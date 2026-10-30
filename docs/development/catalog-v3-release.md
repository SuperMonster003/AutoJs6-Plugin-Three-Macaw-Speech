# Catalog v3 release gate (D108, D127, P8.5.2)

Verified on 2026-10-30. This closes the Shared-Assets catalog signature gate for plugin 1.3.0;
the plugin APK, device and release gates keep their own evidence in roadmap P8.5.

## Published assets

- Shared-Assets commit: `714c23b3ff20ac6af4327e6fd72ad99e680a1129` (maintainer signature and promotion), on top of the
  prepared commits `a819131`, `e2f683a` and `6ce8671`.
- Immutable release: [speech-models-v3](https://github.com/SuperMonster003/AutoJs6-Shared-Assets/releases/tag/speech-models-v3),
  tag at the same commit.
- Successful publication and full download verification: [workflow 40992083270](https://github.com/SuperMonster003/AutoJs6-Shared-Assets/actions/runs/40992083270).
- Published at 2026-10-30 10:33:03 Asia/Taipei (02:33:03 UTC).
- 12 new model archives, 1,555,015,680 bytes (1.45 GiB), plus two catalogs and their signatures: 16 assets.
- Release 3 is published in two wire schemas of `catalogVersion` 3 (D127): `catalog-v3.json` with all 30 models for
  1.3.0, and `catalog-v2.json` with the 25 models of the existing kinds for 1.2.0 and earlier (no `speaker`, `segment`,
  `langid` or `tagger` model).
- The 30 models have 32 versions: Kokoro v1.1 and Matcha zh-baker gained a version 2 on their re-pinned upstream assets
  (D158), Kokoro v1.0 int8 was re-pinned before its first release (D157). 3 versions stay upstream-only.

## Signature and bundled bytes

Both catalogs were signed offline by the maintainer with the existing speech-models ECDSA P-256 family key,
`p256-bf5f0f579c7fbef1`. No key rotation or private-key copy was required.

| File | SHA-256 |
| --- | --- |
| `catalog-v3.json` | `23386175cf31a303d5f911fc23c7ece9064dafd399c6d36812ff785453f7ae27` |
| `catalog-v3.sig.json` | `55e7c590ae8d8580db3edaedd06559f7e0a44a8f1279dd6adcc907d70b1bb6ce` |

All 16 release assets were downloaded into an empty directory and checked with
`verify_release.py --downloaded-release --companion catalog-v2.json catalog-v2.sig.json` (every archive by name,
size, SHA-256 and canonical USTAR content) and `verify_catalog.py` with the pinned public key for both catalogs.
The downloaded catalogs and signatures equal, byte for byte, the files on Shared-Assets `main` and the reviewed
`pending/v3` candidates. The plugin bundles `catalog-v3.json` and its signature from that download; the release 2
`catalog-v2.json` it bundled until now is removed.

`CatalogRules.BUNDLED_SCHEMA` is now 3 and `ThreeMacawSpeechContract.CATALOG_VERSION` 3. `locks/speech-catalog.lock`
records both digests and the immutable release provenance, and `BundledCatalogTest` checks the bytes, the version,
the signature and the key ID. `ModelCatalogRepositoryTest` keeps a bundled schema 2 snapshot readable (the 1.3.0 builds
before this gate) and the cache of 1.2.0 (`catalog-v2.json` of release 2 or 3) from blocking the schema 3 refresh.
Debug builds still accept an unsigned development catalog in `filesDir/dev-catalog/` (`SpeechModels.DEV_CATALOG`);
release builds ignore it.

## Validation scope

- Shared-Assets: speech-model-distribution tests 69 / 69 at `714c23b`; release workflow: all steps passed, including
  the companion catalog checks, catalog and archive reproduction, immutable publication and full read-back.
- Plugin: the catalog test classes (`BundledCatalogTest` 5, `ModelCatalogParserTest` 12, `ModelCatalogRepositoryTest` 10,
  `PluginRuntimeInfoTest` 6, `CatalogSignatureVerifierTest` 6) pass with the new snapshot; the full JVM run, the release
  build and the device checks of the final package are recorded in roadmap P8.5.4 and P8.5.5.
