# -*- coding: utf-8 -*-
"""Unit tests for .python/corresponding_source.py without network access (roadmap D32, P6.4)."""

from __future__ import annotations

import hashlib
import io
import sys
import tarfile
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import corresponding_source as tool  # noqa: E402

LOCK_TEXT = """# comment stays
format=1
sherpa-onnx.version=1.13.8
sherpa-onnx.commit=0123456789abcdef0123456789abcdef01234567
source.alpha.path=deps/alpha-1.0.tar.gz
source.alpha.url=https://example.org/alpha-1.0.tar.gz
source.alpha.sha256=%s
source.alpha.pin=example cmake/alpha.cmake
source.alpha.license=MIT
archive.prefix=example-corresponding-source
""" % ("a" * 64)


class CorrespondingSourceTest(unittest.TestCase):

    def test_headers_are_canonical_ustar_that_tarfile_reads(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "x.tar"
            payload = Path(directory) / "payload.bin"
            payload.write_bytes(bytes(range(256)) * 5)
            long_name = "deps/" + "n" * 90 + "/" + "f" * 60 + ".tar.gz"
            digest, size = tool.write_archive(path, [("README.txt", b"hello"), (long_name, payload)])
            self.assertEqual(digest, hashlib.sha256(path.read_bytes()).hexdigest())
            self.assertEqual(size % tool.BLOCK, 0)
            entries = tool.read_archive(path)
            self.assertEqual([e[0] for e in entries], ["README.txt", long_name])
            self.assertEqual(entries[1][2], hashlib.sha256(payload.read_bytes()).hexdigest())
            with tarfile.open(path) as archive:
                member = archive.getmember(long_name)
                self.assertEqual((member.mode, member.uid, member.gid, member.mtime), (0o644, 0, 0, 0))
                self.assertEqual(archive.extractfile("README.txt").read(), b"hello")

    def test_the_same_members_give_the_same_bytes(self):
        with tempfile.TemporaryDirectory() as directory:
            first, second = Path(directory) / "1.tar", Path(directory) / "2.tar"
            members = [("A.txt", b"a"), ("b/c.txt", b"c" * 700)]
            self.assertEqual(tool.write_archive(first, members), tool.write_archive(second, members))
            self.assertEqual(first.read_bytes(), second.read_bytes())

    def test_unsorted_or_altered_archives_are_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "x.tar"
            with self.assertRaises(tool.SourceError):
                tool.write_archive(path, [("b.txt", b"b"), ("a.txt", b"a")])
            tool.write_archive(path, [("a.txt", b"a")])
            data = bytearray(path.read_bytes())
            data[100:108] = tool.octal(0o755, 8)
            path.write_bytes(bytes(data))
            with self.assertRaises(tool.SourceError):
                tool.read_archive(path)

    def test_the_lock_is_parsed_in_order_and_records_are_written_back(self):
        with tempfile.TemporaryDirectory() as directory:
            lock = Path(directory) / "test.lock"
            lock.write_text(LOCK_TEXT, encoding="utf-8")
            values, sources = tool.read_lock(lock)
            self.assertEqual([s["name"] for s in sources], ["alpha"])
            self.assertEqual(sources[0]["size"], "")
            sources[0]["measured_size"] = "42"
            tool.write_lock_records(values, sources, "example-corresponding-source-abcdef01.tar", 2048, "b" * 64, lock)
            values, sources = tool.read_lock(lock)
            self.assertEqual(sources[0]["size"], "42")
            self.assertEqual(values["archive.file"], "example-corresponding-source-abcdef01.tar")
            self.assertEqual(values["archive.size"], "2048")
            self.assertTrue(lock.read_text(encoding="utf-8").startswith("# comment stays\n"))

    def test_the_lock_refuses_plain_http_and_bad_digests(self):
        with tempfile.TemporaryDirectory() as directory:
            lock = Path(directory) / "test.lock"
            lock.write_text(LOCK_TEXT.replace("https://", "http://"), encoding="utf-8")
            with self.assertRaises(tool.SourceError):
                tool.read_lock(lock)
            lock.write_text(LOCK_TEXT.replace("a" * 64, "A" * 64), encoding="utf-8")
            with self.assertRaises(tool.SourceError):
                tool.read_lock(lock)

    def test_the_tag_commit_comes_from_the_pax_header(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "tag.tar.gz"
            with tarfile.open(path, "w:gz", format=tarfile.PAX_FORMAT, pax_headers={"comment": "c" * 40}) as archive:
                info = tarfile.TarInfo("project-1.0/README")
                info.size = 2
                archive.addfile(info, io.BytesIO(b"hi"))
            self.assertEqual(tool.tag_commit(path), "c" * 40)

    def test_the_repository_lock_lists_the_eleven_runtime_sources(self):
        values, sources = tool.read_lock()
        self.assertEqual(len(sources), 11)
        self.assertEqual(sources[0]["name"], "sherpa-onnx")
        self.assertTrue(all(s["size"] for s in sources))
        self.assertTrue(values["archive.file"].startswith(values["archive.prefix"] + "-" + values["archive.sha256"][:8]))


if __name__ == "__main__":
    unittest.main()
