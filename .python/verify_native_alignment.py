# -*- coding: utf-8 -*-
"""Inspect the native libraries of the sherpa-onnx AAR and of built APKs (roadmap P0.2, D14).

For every `.so` it reads the ELF header and program headers only (standard library, no NDK):

  - ELF class and machine, which must match the ABI directory;
  - the minimum PT_LOAD alignment and the offset / address congruence of every PT_LOAD segment;
  - DT_NEEDED and DT_SONAME from the dynamic segment;
  - for APK entries, the ZIP method and, when stored, the data offset modulo 16384.

The AAR verdict requires a 16 KB PT_LOAD alignment in every ABI (D14 does not skip the 32-bit ABIs).
An APK library must additionally be stored uncompressed at a 16 KB data offset or be compressed (then
the installer extracts it and only the ELF alignment matters); `--expect-stored` / `--expect-compressed`
pin the packaging mode. The default Markdown report is docs/development/native-abi-report.md.

Usage:
  py .python/verify_native_alignment.py                      inspect the locked AAR, rewrite the report
  py .python/verify_native_alignment.py --check              verify that the report matches (writes nothing)
  py .python/verify_native_alignment.py --apk <file> ...     also inspect APKs (printed, not written to the report)

Exit protocol: prints NATIVE_ALIGNMENT_OK on success, NATIVE_ALIGNMENT_ERROR <reason> on failure (exit code 1).
"""

from __future__ import annotations

import argparse
import hashlib
import struct
import sys
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCK = ROOT / 'locks' / 'sherpa-onnx.lock'
REPORT = ROOT / 'docs' / 'development' / 'native-abi-report.md'
PAGE = 16384
ABIS = ['arm64-v8a', 'armeabi-v7a', 'x86_64', 'x86']
MACHINES = {'arm64-v8a': (2, 183), 'armeabi-v7a': (1, 40), 'x86_64': (2, 62), 'x86': (1, 3)}
PT_LOAD, PT_DYNAMIC = 1, 2
DT_NULL, DT_NEEDED, DT_STRTAB, DT_SONAME = 0, 1, 5, 14


@dataclass
class Library:
    path: str
    abi: str
    size: int
    sha256: str
    elf_class: int
    machine: int
    min_align: int
    congruent: bool
    needed: list[str] = field(default_factory=list)
    soname: str = ''
    method: str = ''
    data_offset: int | None = None

    @property
    def elf_ok(self) -> bool:
        return (self.elf_class, self.machine) == MACHINES.get(self.abi) and self.congruent and self.min_align >= PAGE


def parse_elf(data: bytes) -> tuple[int, int, int, bool, list[str], str]:
    """Returns (class, machine, minimum PT_LOAD alignment, congruence, DT_NEEDED, DT_SONAME)."""
    if len(data) < 52 or data[:4] != b'\x7fELF':
        raise ValueError('not an ELF file')
    elf_class, endian = data[4], data[5]
    if endian != 1:
        raise ValueError('big-endian ELF is not an Android library')
    is64 = elf_class == 2
    machine = struct.unpack_from('<H', data, 18)[0]
    if is64:
        phoff = struct.unpack_from('<Q', data, 0x20)[0]
        phentsize, phnum = struct.unpack_from('<HH', data, 0x36)
    else:
        phoff = struct.unpack_from('<I', data, 0x1C)[0]
        phentsize, phnum = struct.unpack_from('<HH', data, 0x2A)
    loads, dynamic = [], None
    for index in range(phnum):
        base = phoff + index * phentsize
        if is64:
            p_type, _flags, p_offset, p_vaddr, _paddr, p_filesz, _memsz, p_align = struct.unpack_from('<IIQQQQQQ', data, base)
        else:
            p_type, p_offset, p_vaddr, _paddr, p_filesz, _memsz, _flags, p_align = struct.unpack_from('<IIIIIIII', data, base)
        if p_type == PT_LOAD:
            loads.append((p_offset, p_vaddr, p_filesz, p_align))
        elif p_type == PT_DYNAMIC:
            dynamic = (p_offset, p_filesz)
    if not loads:
        raise ValueError('no PT_LOAD segment')
    min_align = min(align for _, _, _, align in loads)
    congruent = all(align == 0 or offset % align == vaddr % align for offset, vaddr, _, align in loads)

    def file_offset(address: int) -> int | None:
        for offset, vaddr, filesz, _ in loads:
            if vaddr <= address < vaddr + filesz:
                return offset + address - vaddr
        return None

    needed_offsets, soname_offset, strtab = [], None, None
    if dynamic:
        offset, size = dynamic
        entry = 16 if is64 else 8
        for position in range(offset, offset + size, entry):
            tag, value = struct.unpack_from('<qQ' if is64 else '<iI', data, position)
            if tag == DT_NULL:
                break
            if tag == DT_NEEDED:
                needed_offsets.append(value)
            elif tag == DT_SONAME:
                soname_offset = value
            elif tag == DT_STRTAB:
                strtab = file_offset(value)

    def string(at: int) -> str:
        end = data.index(b'\x00', strtab + at)
        return data[strtab + at:end].decode('utf-8')

    needed = [string(at) for at in needed_offsets] if strtab is not None else []
    soname = string(soname_offset) if strtab is not None and soname_offset is not None else ''
    return elf_class, machine, min_align, congruent, needed, soname


def zip_data_offsets(archive: Path) -> dict[str, int]:
    """Actual data offsets of every entry, read from the local headers."""
    offsets = {}
    with archive.open('rb') as handle, zipfile.ZipFile(archive) as zf:
        for info in zf.infolist():
            handle.seek(info.header_offset)
            header = handle.read(30)
            if header[:4] != b'PK\x03\x04':
                raise ValueError(f'corrupt local header for {info.filename}')
            name_length, extra_length = struct.unpack_from('<HH', header, 26)
            offsets[info.filename] = info.header_offset + 30 + name_length + extra_length
    return offsets


def inspect(archive: Path, prefix: str) -> list[Library]:
    """Inspects `<prefix><abi>/*.so` entries (`jni/` in an AAR, `lib/` in an APK)."""
    libraries = []
    offsets = zip_data_offsets(archive) if prefix == 'lib/' else {}
    with zipfile.ZipFile(archive) as zf:
        for info in sorted(zf.infolist(), key=lambda item: item.filename):
            parts = info.filename.split('/')
            if len(parts) != 3 or parts[0] + '/' != prefix or not parts[2].endswith('.so'):
                continue
            data = zf.read(info)
            elf_class, machine, align, congruent, needed, soname = parse_elf(data)
            library = Library(info.filename, parts[1], len(data), hashlib.sha256(data).hexdigest(),
                              elf_class, machine, align, congruent, needed, soname)
            if prefix == 'lib/':
                library.method = 'stored' if info.compress_type == zipfile.ZIP_STORED else 'deflated'
                library.data_offset = offsets[info.filename]
            libraries.append(library)
    return libraries


def read_lock() -> dict[str, str]:
    entries = {}
    for line in LOCK.read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if line and not line.startswith('#'):
            key, _, value = line.partition('=')
            entries[key.strip()] = value.strip()
    return entries


def render(lock: dict[str, str], libraries: list[Library]) -> str:
    aar = lock['sherpa-onnx.file']
    rows = []
    for lib in libraries:
        verdict = 'ok' if lib.elf_ok else 'FAIL'
        rows.append(f"| `{lib.abi}` | `{Path(lib.path).name}` | {lib.size} | {lib.min_align} | "
                    f"{'yes' if lib.congruent else 'no'} | {', '.join(f'`{n}`' for n in lib.needed) or '-'} | {verdict} |")
    totals = []
    for abi in ABIS:
        selected = [lib for lib in libraries if lib.abi == abi]
        runtime = [lib for lib in selected if Path(lib.path).name in {'libsherpa-onnx-jni.so', 'libonnxruntime.so'}]
        totals.append(f"| `{abi}` | {sum(lib.size for lib in selected)} | {sum(lib.size for lib in runtime)} |")
    jni = {lib.abi: lib.needed for lib in libraries if Path(lib.path).name == 'libsherpa-onnx-jni.so'}
    unused = sorted({Path(lib.path).name for lib in libraries} - {'libsherpa-onnx-jni.so'} -
                    {name for needed in jni.values() for name in needed})
    ok = all(lib.elf_ok for lib in libraries) and {lib.abi for lib in libraries} == set(ABIS)
    lines = [
        '# Native ABI report',
        '',
        'Generated by `py .python/verify_native_alignment.py` from the locked sherpa-onnx AAR (roadmap P0.2, D13 / D14);',
        'do not edit by hand. `--check` fails when this file no longer matches the AAR.',
        '',
        f"- AAR: `libs/{aar}`, sherpa-onnx `{lock['sherpa-onnx.version']}`, SHA-256 `{lock['sherpa-onnx.sha256']}`.",
        f"- Source: {lock['sherpa-onnx.url']}",
        f"- Verdict: {'every library is 16 KB aligned (PT_LOAD alignment >= 16384, offsets congruent)' if ok else 'FAILED'}.",
        f"- Not needed by `libsherpa-onnx-jni.so` in any ABI, excluded from the APK: {', '.join(f'`{n}`' for n in unused) or 'none'}.",
        '',
        '| ABI | Library | Bytes | Min PT_LOAD align | Congruent | DT_NEEDED | 16 KB |',
        '| --- | --- | ---: | ---: | --- | --- | --- |',
        *rows,
        '',
        'Uncompressed bytes per ABI (all libraries / the two the plugin packages):',
        '',
        '| ABI | All | jni + onnxruntime |',
        '| --- | ---: | ---: |',
        *totals,
        '',
    ]
    return '\n'.join(lines)


def describe_apk(apk: Path, expect: str | None) -> list[str]:
    errors = []
    libraries = inspect(apk, 'lib/')
    print(f'APK {apk.name}: {len(libraries)} native libraries')
    for lib in libraries:
        stored_ok = lib.method == 'deflated' or (lib.data_offset is not None and lib.data_offset % PAGE == 0)
        print(f'  {lib.path}: {lib.size} bytes, align {lib.min_align}, {lib.method}, offset {lib.data_offset}'
              f"{'' if stored_ok else ' (not 16 KB aligned in the APK)'}")
        if not lib.elf_ok:
            errors.append(f'{apk.name}: {lib.path} ELF alignment {lib.min_align} or machine mismatch')
        if not stored_ok:
            errors.append(f'{apk.name}: {lib.path} stored at offset {lib.data_offset}')
        if expect and lib.method != expect:
            errors.append(f'{apk.name}: {lib.path} is {lib.method}, expected {expect}')
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--check', action='store_true', help='verify the report without writing it')
    parser.add_argument('--apk', nargs='*', default=[], type=Path, help='APK files to inspect as well')
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--expect-stored', action='store_const', const='stored', dest='expect')
    mode.add_argument('--expect-compressed', action='store_const', const='deflated', dest='expect')
    args = parser.parse_args()
    try:
        lock = read_lock()
        aar = ROOT / 'libs' / lock['sherpa-onnx.file']
        actual = hashlib.sha256(aar.read_bytes()).hexdigest()
        if actual != lock['sherpa-onnx.sha256']:
            raise ValueError(f'{aar.name} SHA-256 {actual} does not match the lock')
        libraries = inspect(aar, 'jni/')
        report = render(lock, libraries)
        errors = [f'{lib.path}: alignment {lib.min_align}, congruent {lib.congruent}' for lib in libraries if not lib.elf_ok]
        if {lib.abi for lib in libraries} != set(ABIS):
            errors.append(f'AAR ABIs {sorted({lib.abi for lib in libraries})} differ from {ABIS}')
        for apk in args.apk:
            errors += describe_apk(apk, args.expect)
        current = REPORT.read_text(encoding='utf-8') if REPORT.is_file() else None
        if args.check:
            if current != report:
                errors.append(f'{REPORT.relative_to(ROOT).as_posix()} is stale; run the script without --check')
        elif current != report:
            REPORT.parent.mkdir(parents=True, exist_ok=True)
            REPORT.write_text(report, encoding='utf-8', newline='\n')
            print(f'Wrote {REPORT.relative_to(ROOT).as_posix()}')
    except (OSError, KeyError, ValueError, zipfile.BadZipFile) as error:
        print(f'NATIVE_ALIGNMENT_ERROR {error}')
        return 1
    if errors:
        for error in errors:
            print(f'NATIVE_ALIGNMENT_ERROR {error}')
        return 1
    print('NATIVE_ALIGNMENT_OK')
    return 0


if __name__ == '__main__':
    sys.exit(main())
