# -*- coding: utf-8 -*-
"""Collect the corresponding source of the speech runtime into one deterministic archive (roadmap D32, P6.4).

The official sherpa-onnx AAR statically links eSpeak NG and UCD Tools (GPL-3.0-or-later) into
libsherpa-onnx-jni.so, so the APK is conveyed under the GPL-3.0-or-later and every GitHub Release attaches
the corresponding source (GPL-3.0 section 6(d)). The archive holds, byte for byte, the sherpa-onnx v1.13.8
source tree with its build scripts (the GitHub archive of the release tag) and every dependency archive that
its build pins by URL and SHA-256 in `cmake/*.cmake` (kaldifst in the `cmake/` of kaldi-decoder v0.3.0),
plus README.txt, SHA256SUMS and the GPL-3.0 text. The dependency archives keep the file names the cmake
files look for in ~/Downloads, so an offline build finds them there.

`locks/corresponding-source.lock` is the single source of truth: per member the upstream URL, the SHA-256
(the cmake pin for the dependencies; for the sherpa-onnx tag archive, which upstream does not pin, the
digest measured when the lock was written, and the tag commit that GitHub records in the archive's pax
header), the size and the archive path; then the file name, size and SHA-256 of the finished archive.
The archive is a canonical USTAR (regular files only, ascending paths, mode 0644, uid / gid 0, mtime 0,
two zero blocks at the end, as Shared-Assets tools/speech-model-distribution writes the model archives),
so the same inputs always give the same bytes.

Downloaded files are untrusted data: every download goes to its own new, empty directory below
--downloads, nothing in it is executed or extracted, and the checks only read bytes (the pax header of the
sherpa-onnx archive is read with tarfile without extracting). Run this tool with `py -I`.

Usage:
  py -I .python/corresponding_source.py build --downloads <dir> [--out releases]   download, verify, write the archive, compare with the lock
  py -I .python/corresponding_source.py build --downloads <dir> --update-lock     the same, then record the archive (and measured sizes)
  py -I .python/corresponding_source.py verify <archive>                          check an archive against the lock

Exit protocol: prints CORRESPONDING_SOURCE_OK on success, CORRESPONDING_SOURCE_ERROR <reason> on failure (exit 1).
"""
from __future__ import annotations

import argparse
import hashlib
import os
import re
import sys
import tarfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCK = ROOT / "locks" / "corresponding-source.lock"
LEGAL_LOCK = ROOT / "locks" / "legal-texts.lock"
GPL_TEXT = ROOT / "third_party" / "licenses" / "GPL-3.0.txt"
BLOCK = 512
NUL = bytes(1)
MAX_DOWNLOAD = 256 * 1024 * 1024
USER_AGENT = "AutoJs6-Plugin-Three-Macaw-Speech corresponding-source (+https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech)"
SHA256 = re.compile(r"[0-9a-f]{64}")
MEMBER = re.compile(r"[A-Za-z0-9][A-Za-z0-9._+-]*(/[A-Za-z0-9][A-Za-z0-9._+-]*)*")


class SourceError(Exception):
    pass


def require(condition, message):
    if not condition:
        raise SourceError(message)


# Lock --------------------------------------------------------------------------------------------

def read_lock(path=LOCK):
    """Ordered sources and the archive record of the lock file."""
    values, order = {}, []
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        key, separator, value = line.partition("=")
        require(separator and key not in values, f"{path.name}:{number}: malformed or repeated key")
        values[key] = value
        match = re.fullmatch(r"source\.([a-z0-9-]+)\.path", key)
        if match:
            order.append(match.group(1))
    require(values.get("format") == "1", "Unsupported lock format")
    sources = []
    for name in order:
        get = lambda field: values.get(f"source.{name}.{field}", "")
        source = {"name": name, "path": get("path"), "url": get("url"), "sha256": get("sha256"),
                  "size": get("size"), "pin": get("pin"), "license": get("license")}
        require(MEMBER.fullmatch(source["path"]) is not None, f"Invalid archive path of {name}")
        require(source["url"].startswith("https://"), f"Source {name} must be fetched over HTTPS")
        require(SHA256.fullmatch(source["sha256"]) is not None, f"Invalid SHA-256 of {name}")
        require(source["size"] == "" or source["size"].isdigit(), f"Invalid size of {name}")
        require(source["pin"] and source["license"], f"Source {name} needs its pin and license")
        sources.append(source)
    require(sources, "The lock lists no sources")
    paths = [source["path"] for source in sources]
    require(len(set(paths)) == len(paths), "Archive paths repeat")
    return values, sources


def write_lock_records(values, sources, archive_name, archive_size, archive_sha256, path=LOCK):
    """Rewrite the measured sizes and the archive record, keeping every other line as it is."""
    sizes = {f"source.{s['name']}.size": s["measured_size"] for s in sources}
    records = {"archive.file": archive_name, "archive.size": str(archive_size), "archive.sha256": archive_sha256}
    lines, seen = [], set()
    for line in path.read_text(encoding="utf-8").splitlines():
        key = line.partition("=")[0].strip()
        if key in sizes:
            lines.append(f"{key}={sizes[key]}")
            seen.add(key)
        elif key in records:
            lines.append(f"{key}={records[key]}")
            seen.add(key)
        else:
            lines.append(line)
            if re.fullmatch(r"source\.[a-z0-9-]+\.sha256", key):
                size_key = key.removesuffix(".sha256") + ".size"
                if size_key in sizes and size_key not in values:
                    lines.append(f"{size_key}={sizes[size_key]}")
                    seen.add(size_key)
    lines += [f"{key}={value}" for key, value in records.items() if key not in seen]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")


# Downloads ---------------------------------------------------------------------------------------

def sha256_file(path):
    digest, size = hashlib.sha256(), 0
    with open(path, "rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
            size += len(chunk)
    return digest.hexdigest(), size


def fetch(source, downloads):
    """Download one source into its own directory below `downloads` (reused when it already verifies)."""
    directory = downloads / source["name"]
    target = directory / Path(source["path"]).name
    if target.is_file():
        digest, size = sha256_file(target)
        if digest == source["sha256"]:
            return target, size
        raise SourceError(f"{target} exists with another SHA-256; remove its directory and run again")
    require(not directory.exists() or not any(directory.iterdir()), f"{directory} must be new and empty")
    directory.mkdir(parents=True, exist_ok=True)
    partial = target.with_name(target.name + ".part")
    request = urllib.request.Request(source["url"], headers={"User-Agent": USER_AGENT})
    digest, size = hashlib.sha256(), 0
    with urllib.request.urlopen(request, timeout=60) as response, open(partial, "wb") as stream:
        while chunk := response.read(1024 * 1024):
            size += len(chunk)
            require(size <= MAX_DOWNLOAD, f"{source['name']} exceeds {MAX_DOWNLOAD} bytes")
            digest.update(chunk)
            stream.write(chunk)
    require(digest.hexdigest() == source["sha256"],
            f"SHA-256 of {source['name']} is {digest.hexdigest()}, the lock pins {source['sha256']} ({source['pin']})")
    os.replace(partial, target)
    return target, size


def tag_commit(path):
    """The commit that GitHub records in the pax global header (comment) of a tag archive; nothing is extracted."""
    with tarfile.open(path, mode="r:gz") as archive:
        archive.next()
        return archive.pax_headers.get("comment", "")


# Canonical USTAR ---------------------------------------------------------------------------------

def octal(value, width):
    encoded = b"%0*o" % (width - 1, value)
    require(len(encoded) == width - 1, "Value does not fit in a USTAR field")
    return encoded + NUL


def split_name(path):
    raw = path.encode("ascii")
    if len(raw) <= 100:
        return raw, b""
    for index, value in enumerate(raw):
        if value == 0x2F and len(raw) - index - 1 <= 100 and 0 < index <= 155:
            return raw[index + 1:], raw[:index]
    raise SourceError("Path cannot be stored in a USTAR header: " + path)


def tar_header(path, size):
    """One canonical USTAR header: mode 0644, uid / gid 0, mtime 0, empty owner names (as Shared-Assets writes)."""
    name, prefix = split_name(path)
    header = bytearray(BLOCK)
    header[0:len(name)] = name
    header[100:108] = octal(0o644, 8)
    header[108:116] = octal(0, 8)
    header[116:124] = octal(0, 8)
    header[124:136] = octal(size, 12)
    header[136:148] = octal(0, 12)
    header[148:156] = b" " * 8
    header[156:157] = b"0"
    header[257:263] = b"ustar" + NUL
    header[263:265] = b"00"
    header[329:337] = octal(0, 8)
    header[337:345] = octal(0, 8)
    header[345:345 + len(prefix)] = prefix
    header[148:156] = b"%06o" % sum(header) + NUL + b" "
    return bytes(header)


def padding(size):
    return bytes(-size % BLOCK)


def write_archive(path, members):
    """Write (archive path, bytes or file path) members in ascending path order; return (sha256, size)."""
    digest, total, previous = hashlib.sha256(), 0, None
    with open(path, "wb") as stream:
        def emit(chunk):
            nonlocal total
            stream.write(chunk)
            digest.update(chunk)
            total += len(chunk)

        for name, content in members:
            require(previous is None or name > previous, "Archive members must use ascending unique paths")
            previous = name
            if isinstance(content, bytes):
                emit(tar_header(name, len(content)))
                emit(content)
                emit(padding(len(content)))
                continue
            size = content.stat().st_size
            emit(tar_header(name, size))
            with open(content, "rb") as source:
                copied = 0
                while chunk := source.read(1024 * 1024):
                    copied += len(chunk)
                    emit(chunk)
            require(copied == size, "Archive member changed while archiving: " + name)
            emit(padding(size))
        emit(bytes(2 * BLOCK))
    return digest.hexdigest(), total


def read_archive(path):
    """Strictly parse a canonical archive and return [(path, size, sha256)] in archive order."""
    entries, previous = [], None
    with open(path, "rb") as stream:
        def exact(size):
            data = stream.read(size)
            require(len(data) == size, "Archive is truncated")
            return data

        while True:
            header = exact(BLOCK)
            if header == bytes(BLOCK):
                require(exact(BLOCK) == bytes(BLOCK) and not stream.read(1), "Archive must end with exactly two zero blocks")
                return entries
            try:
                name = header[0:100].split(NUL, 1)[0].decode("ascii")
                prefix = header[345:500].split(NUL, 1)[0].decode("ascii")
                size = int(header[124:135].decode("ascii"), 8)
            except (UnicodeError, ValueError) as error:
                raise SourceError("Archive header is not canonical") from error
            member = prefix + "/" + name if prefix else name
            require(header == tar_header(member, size), "Archive header is not canonical: " + member)
            require(previous is None or member > previous, "Archive members are not in ascending path order")
            previous = member
            digest, remaining = hashlib.sha256(), size
            while remaining:
                chunk = exact(min(remaining, 1024 * 1024))
                digest.update(chunk)
                remaining -= len(chunk)
            require(exact(-size % BLOCK) == bytes(-size % BLOCK), "Archive padding must be zero bytes")
            entries.append((member, size, digest.hexdigest()))


# Archive content ---------------------------------------------------------------------------------

def gpl_text():
    expected = re.search(r"^gpl-3\.0\.sha256=([0-9a-f]{64})$", LEGAL_LOCK.read_text(encoding="utf-8"), re.M)
    require(expected is not None, "legal-texts.lock has no GPL-3.0 digest")
    content = GPL_TEXT.read_bytes()
    require(hashlib.sha256(content).hexdigest() == expected.group(1), "third_party/licenses/GPL-3.0.txt differs from its lock")
    return content


def readme(values, sources):
    sherpa = values.get("sherpa-onnx.version", "")
    lines = [
        f"Corresponding source of the speech runtime in 3-Macaw Speech (sherpa-onnx {sherpa})",
        "",
        "3-Macaw Speech (https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech) packages the",
        f"official sherpa-onnx {sherpa} Android AAR (https://github.com/k2-fsa/sherpa-onnx/releases/tag/v{sherpa}).",
        "Its libsherpa-onnx-jni.so statically links eSpeak NG and UCD Tools, which are licensed under the GNU",
        "General Public License version 3 or later, so the APK as a whole is conveyed under the GPL-3.0-or-later",
        "(LICENSE-GPL-3.0.txt). This archive is the corresponding source of that library (GPL-3.0 section 6(d)):",
        "",
        f"- sherpa-onnx/: the source tree of sherpa-onnx at tag v{sherpa} (commit {values.get('sherpa-onnx.commit', '')}),",
        "  as GitHub archives the tag, including its build scripts (build-android-*.sh, cmake/, the Android",
        "  workflow .github/workflows/android.yaml that builds the AAR).",
        "- deps/: every dependency archive that this build pins by URL and SHA-256 in cmake/*.cmake (kaldifst in the",
        "  cmake/ of kaldi-decoder), unchanged and with the file names the cmake files look for in ~/Downloads, so a",
        "  build without network access finds them after you copy deps/* to ~/Downloads.",
        "",
        "ONNX Runtime (libonnxruntime.so, MIT) is a separate prebuilt library; its source is",
        "https://github.com/microsoft/onnxruntime at the version that cmake/onnxruntime-android-*.cmake names.",
        "The source of the plugin itself (MPL-2.0) is its repository at the release tag.",
        "",
        "To build the native library like the official AAR: unpack sherpa-onnx/ and follow build-android-*.sh",
        "for each ABI (an Android NDK is required), or the steps of .github/workflows/android.yaml.",
        "",
        "Members (archive path, size in bytes, SHA-256, where the SHA-256 is pinned, upstream URL, license):",
        "",
    ]
    for source in sources:
        lines += [
            f"{source['path']}",
            f"  size {source['measured_size']}, sha256 {source['sha256']}",
            f"  pinned by {source['pin']}",
            f"  {source['url']}",
            f"  {source['license']}",
            "",
        ]
    lines.append("SHA256SUMS lists the SHA-256 of every member except itself.")
    return ("\n".join(lines) + "\n").encode("ascii")


def members(values, sources, files):
    gpl = gpl_text()
    text = readme(values, sources)
    listed = [("LICENSE-GPL-3.0.txt", gpl), ("README.txt", text)] + [(s["path"], files[s["name"]]) for s in sources]
    sums = "".join(f"{hashlib.sha256(c).hexdigest() if isinstance(c, bytes) else sha256_file(c)[0]}  {name}\n"
                   for name, c in sorted(listed))
    return sorted(listed + [("SHA256SUMS", sums.encode("ascii"))])


def build(arguments):
    values, sources = read_lock()
    downloads = Path(arguments.downloads).resolve()
    require(not downloads.is_relative_to(ROOT) or downloads.is_relative_to(ROOT / ".bak"),
            "--downloads must be outside the repository (or below the ignored .bak/)")
    downloads.mkdir(parents=True, exist_ok=True)
    files = {}
    for source in sources:
        target, size = fetch(source, downloads)
        if source["size"]:
            require(int(source["size"]) == size, f"Size of {source['name']} is {size}, the lock records {source['size']}")
        source["measured_size"] = str(size)
        files[source["name"]] = target
        print(f"  {source['name']}: {size} bytes, SHA-256 verified ({source['pin']})")
    commit = tag_commit(files["sherpa-onnx"])
    require(commit == values.get("sherpa-onnx.commit"), f"The sherpa-onnx archive records commit {commit or '-'}")
    print(f"  sherpa-onnx: tag archive of commit {commit}")
    out = Path(arguments.out).resolve()
    out.mkdir(parents=True, exist_ok=True)
    staging = out / "corresponding-source.tar.part"
    digest, size = write_archive(staging, members(values, sources, files))
    name = f"{values['archive.prefix']}-{digest[:8]}.tar"
    final = out / name
    os.replace(staging, final)
    print(f"  archive: {final.name}, {size} bytes, SHA-256 {digest}")
    if arguments.update_lock:
        write_lock_records(values, sources, name, size, digest)
        print(f"  lock: {LOCK.relative_to(ROOT).as_posix()} updated")
        values, sources = read_lock()
    else:
        require(values.get("archive.sha256") == digest and values.get("archive.size") == str(size)
                and values.get("archive.file") == name, "The archive differs from the lock (run with --update-lock after review)")
    verify_archive(final, values, sources)
    return final


def verify_archive(path, values=None, sources=None):
    if values is None:
        values, sources = read_lock()
    digest, size = sha256_file(path)
    require(values.get("archive.sha256") == digest and values.get("archive.size") == str(size),
            f"{Path(path).name}: SHA-256 {digest} / {size} bytes differ from the lock")
    entries = read_archive(path)
    names = [entry[0] for entry in entries]
    expected = sorted(["LICENSE-GPL-3.0.txt", "README.txt", "SHA256SUMS"] + [s["path"] for s in sources])
    require(names == expected, f"Unexpected members: {names}")
    found = {name: (member_size, member_digest) for name, member_size, member_digest in entries}
    for source in sources:
        member_size, member_digest = found[source["path"]]
        require(member_digest == source["sha256"] and str(member_size) == (source["size"] or source.get("measured_size")),
                f"Member {source['path']} differs from the lock")
    return entries


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    build_parser = commands.add_parser("build")
    build_parser.add_argument("--downloads", required=True, help="new or reused download directory, outside the repository")
    build_parser.add_argument("--out", default=str(ROOT / "releases"), help="output directory (default releases/)")
    build_parser.add_argument("--update-lock", action="store_true", help="record the archive and the measured sizes")
    verify_parser = commands.add_parser("verify")
    verify_parser.add_argument("archive")
    arguments = parser.parse_args(argv)
    try:
        if arguments.command == "build":
            build(arguments)
        else:
            entries = verify_archive(Path(arguments.archive))
            print(f"  {Path(arguments.archive).name}: {len(entries)} members match the lock")
    except (SourceError, OSError, tarfile.TarError) as error:
        print(f"CORRESPONDING_SOURCE_ERROR {error}")
        return 1
    print("CORRESPONDING_SOURCE_OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
