# -*- coding: utf-8 -*-
"""Unit tests for .python/verify_native_alignment.py on synthetic ELF files, AARs and APKs (roadmap P0.2 / P0.3)."""

from __future__ import annotations

import io
import struct
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import verify_native_alignment as tool  # noqa: E402


def elf(is64: bool = True, machine: int = 183, align: int = 16384, needed: tuple[str, ...] = ('liblog.so',),
        soname: str = 'libdemo.so', congruent: bool = True) -> bytes:
    """A minimal little-endian shared object: two PT_LOAD segments, a PT_DYNAMIC segment and a string table."""
    strings = b'\x00' + b''.join(name.encode() + b'\x00' for name in (*needed, soname))
    offsets, position = [], 1
    for name in (*needed, soname):
        offsets.append(position)
        position += len(name) + 1
    header_size, entry_size = (64, 56) if is64 else (52, 32)
    phnum = 3
    strtab_offset = header_size + entry_size * phnum
    dynamic_offset = strtab_offset + len(strings)
    dynamic_offset += (-dynamic_offset) % 16
    tags = [(1, offset) for offset in offsets[:-1]] + [(14, offsets[-1]), (5, strtab_offset), (0, 0)]
    dynamic = b''.join(struct.pack('<qQ' if is64 else '<iI', tag, value) for tag, value in tags)
    size = dynamic_offset + len(dynamic)
    second_vaddr = align * 4 + (0 if congruent else 4096)
    loads = [(0, 0, size), (0, second_vaddr, size)]
    if is64:
        head = b'\x7fELF' + bytes([2, 1, 1]) + bytes(9) + struct.pack('<HHIQQQIHHHHHH', 3, machine, 1, 0, header_size, 0, 0, header_size, entry_size, phnum, 64, 0, 0)
        program = b''.join(struct.pack('<IIQQQQQQ', 1, 5, offset, vaddr, vaddr, filesz, filesz, align) for offset, vaddr, filesz in loads)
        program += struct.pack('<IIQQQQQQ', 2, 6, dynamic_offset, dynamic_offset, dynamic_offset, len(dynamic), len(dynamic), 8)
    else:
        head = b'\x7fELF' + bytes([1, 1, 1]) + bytes(9) + struct.pack('<HHIIIIIHHHHHH', 3, machine, 1, 0, header_size, 0, 0, header_size, entry_size, phnum, 40, 0, 0)
        program = b''.join(struct.pack('<IIIIIIII', 1, offset, vaddr, vaddr, filesz, filesz, 5, align) for offset, vaddr, filesz in loads)
        program += struct.pack('<IIIIIIII', 2, dynamic_offset, dynamic_offset, dynamic_offset, len(dynamic), len(dynamic), 6, 4)
    body = head + program + strings
    body += bytes(dynamic_offset - len(body))
    return body + dynamic


class ParseElfTest(unittest.TestCase):

    def test_reads_alignment_needed_and_soname(self):
        elf_class, machine, align, congruent, needed, soname = tool.parse_elf(elf(needed=('libonnxruntime.so', 'liblog.so')))
        self.assertEqual((2, 183, 16384, True), (elf_class, machine, align, congruent))
        self.assertEqual(['libonnxruntime.so', 'liblog.so'], needed)
        self.assertEqual('libdemo.so', soname)

    def test_reads_32_bit_libraries(self):
        elf_class, machine, align, congruent, needed, _ = tool.parse_elf(elf(is64=False, machine=40, align=4096))
        self.assertEqual((1, 40, 4096, True), (elf_class, machine, align, congruent))
        self.assertEqual(['liblog.so'], needed)

    def test_detects_incongruent_segments(self):
        self.assertFalse(tool.parse_elf(elf(congruent=False))[3])

    def test_rejects_non_elf_input(self):
        with self.assertRaises(ValueError):
            tool.parse_elf(b'not an elf file at all, but long enough to be parsed as one' * 2)


class InspectArchivesTest(unittest.TestCase):

    def test_aar_libraries_are_judged_by_machine_and_alignment(self):
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, 'w') as archive:
            archive.writestr('classes.jar', b'')
            archive.writestr('jni/arm64-v8a/libgood.so', elf())
            archive.writestr('jni/x86/libwrongmachine.so', elf(is64=False, machine=40))
            archive.writestr('jni/armeabi-v7a/libsmall.so', elf(is64=False, machine=40, align=4096))
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'demo.aar'
            path.write_bytes(buffer.getvalue())
            verdicts = {lib.path: lib.elf_ok for lib in tool.inspect(path, 'jni/')}
        self.assertEqual({'jni/arm64-v8a/libgood.so': True, 'jni/x86/libwrongmachine.so': False, 'jni/armeabi-v7a/libsmall.so': False}, verdicts)

    def test_apk_entries_report_method_and_data_offset(self):
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, 'w') as archive:
            archive.writestr(zipfile.ZipInfo('lib/x86_64/libstored.so'), elf(machine=62), compress_type=zipfile.ZIP_STORED)
            archive.writestr(zipfile.ZipInfo('lib/x86_64/libdeflated.so'), elf(machine=62), compress_type=zipfile.ZIP_DEFLATED)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'demo.apk'
            path.write_bytes(buffer.getvalue())
            libraries = {lib.path: lib for lib in tool.inspect(path, 'lib/')}
            errors = tool.describe_apk(path, 'deflated')
        stored = libraries['lib/x86_64/libstored.so']
        self.assertEqual('stored', stored.method)
        self.assertEqual(len('lib/x86_64/libstored.so') + 30, stored.data_offset)
        self.assertEqual('deflated', libraries['lib/x86_64/libdeflated.so'].method)
        # The stored entry is neither 16 KB aligned in the ZIP nor compressed as expected.
        self.assertTrue(any('stored at offset' in error for error in errors))
        self.assertTrue(any('expected deflated' in error for error in errors))


class RepositoryReportTest(unittest.TestCase):

    def test_the_committed_report_matches_the_locked_aar(self):
        lock = tool.read_lock()
        libraries = tool.inspect(tool.ROOT / 'libs' / lock['sherpa-onnx.file'], 'jni/')
        self.assertEqual(16, len(libraries))
        self.assertTrue(all(lib.elf_ok for lib in libraries))
        self.assertEqual(tool.render(lock, libraries), tool.REPORT.read_text(encoding='utf-8'))


if __name__ == '__main__':
    unittest.main()
