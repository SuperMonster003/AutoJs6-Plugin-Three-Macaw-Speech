# -*- coding: utf-8 -*-
"""Write the SHA256SUMS of a release (roadmap R4, P6.4): the five APKs and the corresponding source archive.

`.\\gradlew.bat :app:appendDigestToReleasedFiles` copies the signed APKs to releases/ with a CRC32 suffix
(`autojs6-plugin-three-macaw-speech-v<version>-<abi>-<crc32>.apk`); several builds of one version may sit there side
by side. This tool takes, for each of the four ABIs and universal, the newest APK of the version in version.properties
(or the files given with --apk), checks that the CRC32 suffix matches the file, checks the corresponding source archive
of locks/corresponding-source.lock (corresponding_source.verify), and writes releases/SHA256SUMS in the format of
`sha256sum` (two spaces, file names sorted). Nothing is signed and no secret is read.

Usage:
  py .python/release_sums.py                 write releases/SHA256SUMS for the newest APK set
  py .python/release_sums.py --check         compare releases/SHA256SUMS with the files (writes nothing)

Exit protocol: prints RELEASE_SUMS_OK on success, RELEASE_SUMS_ERROR <reason> on failure (exit code 1).
"""
from __future__ import annotations

import argparse
import hashlib
import re
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import corresponding_source  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
RELEASES = ROOT / "releases"
SPLITS = ["arm64-v8a", "armeabi-v7a", "x86_64", "x86", "universal"]
PREFIX = "autojs6-plugin-three-macaw-speech"


class SumsError(Exception):
    pass


def version_name():
    match = re.search(r"^VERSION_NAME=(.+)$", (ROOT / "version.properties").read_text(encoding="utf-8"), re.M)
    if not match:
        raise SumsError("VERSION_NAME is missing from version.properties")
    return match.group(1).strip()


def crc32(path):
    value = 0
    with open(path, "rb") as stream:
        while chunk := stream.read(1024 * 1024):
            value = zlib.crc32(chunk, value)
    return f"{value:08x}"


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def newest_apks(version):
    chosen = []
    for split in SPLITS:
        pattern = re.compile(rf"{re.escape(PREFIX)}-v{re.escape(version)}-{re.escape(split)}-([0-9a-f]{{8}})\.apk")
        candidates = [p for p in RELEASES.glob("*.apk") if pattern.fullmatch(p.name)]
        if not candidates:
            raise SumsError(f"No {split} APK of v{version} in releases/ (run appendDigestToReleasedFiles)")
        chosen.append(max(candidates, key=lambda p: p.stat().st_mtime))
    return chosen


def release_files():
    apks = newest_apks(version_name())
    for apk in apks:
        suffix = apk.stem.rsplit("-", 1)[1]
        if crc32(apk) != suffix:
            raise SumsError(f"{apk.name}: the CRC32 suffix does not match the file")
    values, sources = corresponding_source.read_lock()
    archive = RELEASES / values["archive.file"]
    if not archive.is_file():
        raise SumsError(f"{archive.name} is missing (py -I .python/corresponding_source.py build --downloads <dir>)")
    corresponding_source.verify_archive(archive, values, sources)
    return apks + [archive]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="compare releases/SHA256SUMS instead of writing it")
    arguments = parser.parse_args(argv)
    try:
        files = release_files()
        lines = "".join(f"{sha256(p)}  {p.name}\n" for p in sorted(files, key=lambda p: p.name))
        target = RELEASES / "SHA256SUMS"
        if arguments.check:
            if not target.is_file() or target.read_text(encoding="ascii") != lines:
                raise SumsError("releases/SHA256SUMS differs from the release files")
        else:
            target.write_text(lines, encoding="ascii", newline="\n")
        for path in sorted(files, key=lambda p: p.name):
            print(f"  {path.name}: {path.stat().st_size} bytes")
    except (SumsError, corresponding_source.SourceError, OSError) as error:
        print(f"RELEASE_SUMS_ERROR {error}")
        return 1
    print("RELEASE_SUMS_OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
