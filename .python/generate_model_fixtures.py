"""Regenerate the JVM test fixtures of the model store (roadmap P2.3 / P2.4) with the Shared-Assets reference tools.

The fixtures are produced by the very code the maintainer uses for speech-models (distribution.py for the catalog
rules and the canonical mirror archives, crypto.py with OpenSSL for the detached signature), so the plugin's Kotlin
parser, verifier and extractor are tested against the reference implementation instead of against themselves:

- a small synthetic catalog-v1.json (three models: commercial / non-commercial / unknown license, a long USTAR path
  with a space and "!", an upstream tar.bz2 written by Python's tarfile in GNU format with extra members, and a raw
  upstream file), its catalog-v1.sig.json and the throwaway public key;
- the canonical mirror archives and the upstream assets the catalog pins;
- bzip2 vectors written by libbzip2 (through Python's bz2 module): multi-block, long runs, every byte value,
  concatenated streams and an empty stream, with the SHA-256 of their plain content in bz2-vectors.json.

The signing key is generated for each run and its private half is deleted, so a rerun changes the key, the signature
and the sizes stay the same. Not part of the build or of the pre-push check: run it by hand after changing the
fixtures, then run :app:testDebugUnitTest.

    py .python/generate_model_fixtures.py --tools <Shared-Assets>/tools/speech-model-distribution

zh-CN: 使用 Shared-Assets 参考工具重新生成模型存储 (路线图 P2.3 / P2.4) 的 JVM 测试夹具: 目录规则与规范镜像归档来自 distribution.py,
分离签名来自 crypto.py (OpenSSL), 因此插件的 Kotlin 解析器, 校验器与解包器是对照参考实现测试的. 签名密钥每次运行重新生成并删除私钥.
不属于构建与推送前检查; 修改夹具后手动运行, 再运行 :app:testDebugUnitTest.
"""
from __future__ import annotations

import argparse
import bz2
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import random
import shutil
import sys
import tarfile
import tempfile

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "app/src/test/resources/model-fixtures"
REPOSITORY = "SuperMonster003/AutoJs6-Shared-Assets"
UPSTREAM = "https://github.com/k2-fsa/sherpa-onnx/releases/download"


def load(tools: Path, name: str, key: str):
    spec = importlib.util.spec_from_file_location(key, tools / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules[key] = module
    spec.loader.exec_module(module)
    return module


def blob(seed: int, size: int) -> bytes:
    generator = random.Random(seed)
    return bytes(generator.getrandbits(8) for _ in range(size))


def text(seed: int, lines: int) -> bytes:
    generator = random.Random(seed)
    return "".join(f"token{generator.randrange(10_000)} {index}\n" for index in range(lines)).encode("ascii")


def gnu_tar_bz2(members: list[tuple[str, bytes | None]]) -> bytes:
    """A GNU format tar.bz2 like the sherpa-onnx releases: directories first, then files (None marks a directory)."""
    buffer = io.BytesIO()
    with tarfile.open(fileobj=buffer, mode="w", format=tarfile.GNU_FORMAT) as archive:
        for name, content in members:
            info = tarfile.TarInfo(name)
            info.mtime = 1_700_000_000
            info.uname = info.gname = "runner"
            if content is None:
                info.type = tarfile.DIRTYPE
                info.mode = 0o755
                archive.addfile(info)
            else:
                info.size = len(content)
                info.mode = 0o644
                archive.addfile(info, io.BytesIO(content))
    return bz2.compress(buffer.getvalue(), compresslevel=9)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--tools", type=Path, required=True, help="Shared-Assets tools/speech-model-distribution")
    args = parser.parse_args()
    distribution = load(args.tools, "distribution", "autojs6_speech_distribution")
    crypto = load(args.tools, "crypto", "autojs6_speech_crypto")

    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    OUTPUT.mkdir(parents=True)
    long_dir = "espeak-ng-data/voices/!v/" + "x" * 90
    models = [
        {
            "modelId": "asr.test.noncommercial", "kind": "asr", "modelType": "sense-voice", "upstreamName": "noncommercial.tar.bz2",
            "top": "sherpa-onnx-noncommercial", "license": {"spdx": "LicenseRef-NonCommercial", "commercialUse": False,
                                                             "noticeKey": "speech_model_license_non_commercial"},
            "files": {"model.int8.onnx": blob(11, 4000), "tokens.txt": text(12, 40)},
            "extra": {"test_wavs/0.wav": blob(13, 700), "model.onnx": blob(14, 900)},
            "config": {"model": "model.int8.onnx", "tokens": "tokens.txt"}, "sampleRate": 16000,
        },
        {
            "modelId": "kws.test.unknown", "kind": "kws", "modelType": "kws-transducer", "upstreamName": "unknown.tar.bz2",
            "top": "sherpa-onnx-kws-test", "license": {"spdx": "LicenseRef-NoLicenseDeclared", "commercialUse": "unknown",
                                                        "noticeKey": "speech_model_license_unknown"},
            "files": {"decoder.onnx": blob(21, 1500), "encoder.int8.onnx": blob(22, 2500), "joiner.int8.onnx": blob(23, 800),
                      "tokens.txt": text(24, 30)},
            "extra": {"README.md": b"kws test\n"},
            "config": {"encoder": "encoder.int8.onnx", "decoder": "decoder.onnx", "joiner": "joiner.int8.onnx",
                       "tokens": "tokens.txt", "modelType": "zipformer2"}, "sampleRate": 16000,
        },
        {
            "modelId": "tts.test.tiny", "kind": "tts", "modelType": "matcha", "upstreamName": "tiny-model.tar.bz2",
            "top": "tiny-model", "license": {"spdx": "MIT", "commercialUse": True, "noticeKey": "speech_model_license_commercial"},
            "files": {"model-steps-3.onnx": blob(31, 3000), "tokens.txt": text(32, 50), "lexicon.txt": text(33, 80),
                      "espeak-ng-data/phontab": blob(34, 300), f"{long_dir}/Mr serious": b"name Mr serious\n"},
            "raw": ("vocoder-models/hifigan_test.onnx", "hifigan_test.onnx", blob(35, 1200)),
            "extra": {"model-steps-3.fp32.onnx": blob(36, 1000), "test.wav": blob(37, 400)},
            "config": {"acousticModel": "model-steps-3.onnx", "vocoder": "hifigan_test.onnx", "tokens": "tokens.txt",
                       "lexicon": ["lexicon.txt"], "dataDir": "espeak-ng-data"}, "sampleRate": 22050, "speakers": 1,
        },
    ]
    catalog_models = []
    for model in models:
        files = dict(model["files"])
        upstream_members = sorted(set(list(files) + list(model["extra"])))
        directories = sorted({"/".join(path.split("/")[:index]) for path in upstream_members for index in range(1, path.count("/") + 1)})
        members = [(f"{model['top']}", None)] + [(f"{model['top']}/{d}", None) for d in directories]
        members += [(f"{model['top']}/{path}", files.get(path, model["extra"].get(path))) for path in upstream_members]
        archive = gnu_tar_bz2(members)
        (OUTPUT / model["upstreamName"]).write_bytes(archive)
        tag = "tts-models" if model["kind"] == "tts" else ("kws-models" if model["kind"] == "kws" else "asr-models")
        upstream = [{"format": "tar.bz2", "url": f"{UPSTREAM}/{tag}/{model['upstreamName']}", "size": len(archive),
                     "sha256": hashlib.sha256(archive).hexdigest(), "members": {f"{model['top']}/{p}": p for p in sorted(files)}}]
        if "raw" in model:
            url_path, path, content = model["raw"]
            (OUTPUT / Path(url_path).name).write_bytes(content)
            files[path] = content
            upstream.append({"format": "file", "url": f"{UPSTREAM}/{url_path}", "size": len(content),
                             "sha256": hashlib.sha256(content).hexdigest(), "path": path})
        records = [{"path": p, "size": len(files[p]), "sha256": hashlib.sha256(files[p]).hexdigest()} for p in sorted(files)]
        stream = io.BytesIO()
        digest, size = distribution.write_archive(stream, [(r["path"], r["size"], (lambda c=files[r["path"]]: io.BytesIO(c))) for r in records])
        name = distribution.archive_file_name(model["modelId"], 1, digest)
        (OUTPUT / name).write_bytes(stream.getvalue())
        assert distribution.read_archive(OUTPUT / name) == records
        version = {
            "versionCode": 1, "modelType": model["modelType"], "languages": ["zh", "en"], "sampleRate": model["sampleRate"],
            "releasedAt": "2026-10-16T00:00:00Z", "license": model["license"],
            "runtime": {"minSherpaOnnx": "1.13.8", "config": model["config"]},
            "sourceUrl": distribution.source_url(REPOSITORY, "speech-models-v1", model["modelId"], 1),
            "mirror": {"fileName": name, "format": "tar", "size": size, "sha256": digest,
                       "url": distribution.release_url(REPOSITORY, "speech-models-v1", name)},
            "upstream": upstream, "files": records,
        }
        if "speakers" in model:
            version["speakers"] = model["speakers"]
        catalog_models.append({
            "modelId": model["modelId"], "kind": model["kind"],
            "displayName": {"en": f"Test {model['modelId']}", "zh-CN": f"测试 {model['modelId']}"},
            "summary": {"en": "Synthetic test model.", "zh-CN": "合成的测试模型."},
            "recommended": model["kind"] != "asr", "versions": [version], "recommendedVersionCode": 1,
        })
    catalog = {"schemaVersion": 1, "catalogVersion": 1, "releaseTag": "speech-models-v1", "generatedAt": "2026-10-16T01:00:00Z",
               "repository": REPOSITORY, "models": catalog_models}
    distribution.validate_catalog(catalog, expected_repository=REPOSITORY)
    catalog_path = OUTPUT / "catalog-v1.json"
    catalog_path.write_bytes(distribution.canonical_json_bytes(catalog))

    with tempfile.TemporaryDirectory(prefix="macaw-fixture-key-") as directory:
        private = Path(directory) / "private.pem"
        key_id = crypto.generate_key_pair(private, OUTPUT / "catalog-signing-public.pem", Path(directory) / "public.der")
        crypto.sign_catalog(catalog_path, private, OUTPUT / "catalog-v1.sig.json", key_id, expected_repository=REPOSITORY)
        crypto.verify_catalog_material(catalog_path, OUTPUT / "catalog-v1.sig.json", OUTPUT / "catalog-signing-public.pem", key_id,
                                       expected_repository=REPOSITORY)

    hostile = OUTPUT / "hostile"
    hostile.mkdir()
    (hostile / "oversized.tar.bz2").write_bytes(gnu_tar_bz2([("top", None), ("top/model.onnx", blob(41, 4096))]))
    (hostile / "duplicate.tar.bz2").write_bytes(gnu_tar_bz2([("top/model.onnx", b"tiny"), ("top/model.onnx", b"tiny")]))
    buffer = io.BytesIO()
    with tarfile.open(fileobj=buffer, mode="w", format=tarfile.GNU_FORMAT) as archive:
        link = tarfile.TarInfo("top/model.onnx")
        link.type, link.linkname = tarfile.SYMTYPE, "/etc/passwd"
        archive.addfile(link)
    (hostile / "symlink.tar.bz2").write_bytes(bz2.compress(buffer.getvalue()))

    vectors = {}
    generator = random.Random(7)
    plain = {
        "random-all-bytes": bytes(generator.getrandbits(8) for _ in range(150_000)),
        "long-runs": b"".join(bytes([value]) * length for value, length in
                              [(65, 3), (66, 4), (67, 5), (68, 259), (69, 260), (70, 1000), (0, 70_000), (255, 4), (71, 1)]),
        "single-byte-megabyte": b"z" * 1_048_576,
        "text-level1-multiblock": b"".join(f"line {index} {index * index % 9973}\n".encode() for index in range(40_000)),
        "empty": b"",
    }
    levels = {"random-all-bytes": 1, "long-runs": 9, "single-byte-megabyte": 9, "text-level1-multiblock": 1, "empty": 9}
    bz2_dir = OUTPUT / "bz2"
    bz2_dir.mkdir()
    for name, content in plain.items():
        (bz2_dir / f"{name}.bz2").write_bytes(bz2.compress(content, compresslevel=levels[name]))
        vectors[f"{name}.bz2"] = {"size": len(content), "sha256": hashlib.sha256(content).hexdigest()}
    first, second = b"first stream\n" * 1000, b"second stream\n" * 1000
    (bz2_dir / "concatenated.bz2").write_bytes(bz2.compress(first) + bz2.compress(second))
    vectors["concatenated.bz2"] = {"size": len(first + second), "sha256": hashlib.sha256(first + second).hexdigest()}
    (OUTPUT / "bz2-vectors.json").write_bytes(distribution.canonical_json_bytes(vectors))
    for path in sorted(OUTPUT.rglob("*")):
        if path.is_file():
            print(f"{path.relative_to(OUTPUT).as_posix()}  {path.stat().st_size}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
