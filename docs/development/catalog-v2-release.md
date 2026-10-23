# Catalog v2 release gate (D108)

Verified on 2026-10-23. This closes the Shared-Assets catalog signature gate for plugin 1.2.0;
the remaining plugin APK, device and release gates retain their own evidence and status.

## Published assets

- Shared-Assets commit: `2901e5579f94923b26a69f32df4e3a9931ed67c9`, including the candidate preparation commit `710484e`.
- Immutable release: [speech-models-v2](https://github.com/SuperMonster003/AutoJs6-Shared-Assets/releases/tag/speech-models-v2).
- Successful publication and full download verification: [workflow 39942763456](https://github.com/SuperMonster003/AutoJs6-Shared-Assets/actions/runs/39942763456).
- Published at 2026-10-23 18:58:51 Asia/Taipei (10:58:51 UTC).
- 8 new model archives, 388,244,480 bytes (370.3 MiB), plus `catalog-v2.json` and `catalog-v2.sig.json`: 10 assets.
- The 19-model catalog retains all ten v1 models unchanged. The new Korean model remains upstream-only.
- Every published asset name, size and GitHub SHA-256 digest matches the signed catalog or the reviewed catalog/signature bytes.

## Signature and bundled bytes

The existing offline signature of the reviewed candidate was verified and retained byte for byte.
The signing key is the existing speech-models ECDSA P-256 family key, `p256-bf5f0f579c7fbef1`.
No key rotation or private-key copy was required.

| File | SHA-256 |
| --- | --- |
| `catalog-v2.json` | `d5a975fa65161ed73d1582cd4118af764b82e803d26c6e40dc64ac607cedfb8c` |
| `catalog-v2.sig.json` | `e4b3a58fa88e6317f432c45661a58f7cb4243aced533ab03e1042555f6c0a697` |

The published catalog and signature were downloaded independently, compared byte for byte with
the plugin's bundled files, and verified with the pinned public key. The catalog bytes are unchanged
from the reviewed pending candidate; the signature is now bundled, and `locks/speech-catalog.lock`
records both digests and the immutable release provenance.

`BundledCatalogTest` requires the signature, locks its bytes, rejects pending catalog provenance,
and verifies the signature and key ID. Removing the signature can no longer pass the repository gate.
Runtime fallback behavior for development snapshots is unchanged.

## Validation scope

- Shared-Assets pre-push checks: 7/7, including all three asset-family tool suites.
- Release workflow: all steps passed, including current/historical signatures, catalog reproduction,
  archive reproduction, immutable publication, and full archive download/content verification.
- Plugin pre-push checks: 7/7. JVM: 403 tests across 58 suites, zero failures/errors/skips;
  all 5 `BundledCatalogTest` cases passed.
- Plugin validation used an isolated checkout based on remote `0003dcfd3e7e0ea227511d4016cda83e54cf093e`,
  excluding the primary checkout's unrelated unpublished commits and working changes.
- No device tests or plugin APK release were performed by this catalog publication task.
