# Model downloads on the shared download kit

Roadmap P12.2 (1.6.1), host plugin-center roadmap P13.2 (D22, D26, D27, T31, T32, appendix A Q51 A). Since 1.6.1,
`core/model/Downloader` keeps its contract (`fetch(artifact, directory, cancel, onProgress): File`, the cancellation of
`CancelToken`, the error codes) and downloads through the download kit of the AutoJs6 host (`plugin-api/download-kit`,
package `org.autojs.plugin.downloadkit`), the verified store and resumable fetcher of the host's Plugin Center, which the
host's app update, code generation models and Ace fonts and the official plugins Ace Editor and 3-Stone AI share. The
release AAR is staged as `libs/download-kit.aar` and locked in `locks/host-api-aars.lock` (see `libs/README.md`).

## Layout

| Path | Content |
| --- | --- |
| `cacheDir/downloads/store/<sha256>/file` | A verified download (renamed from `file.part` after the size and SHA-256 matched) |
| `cacheDir/downloads/store/<sha256>/file.part` | An unfinished download |
| `cacheDir/downloads/store/<sha256>/meta.json` | The kit's metadata: URL, size, SHA-256, the server's validators (`ETag`, `Last-Modified`) and the URL the bytes came from |
| `cacheDir/downloads/<sha256>.part`, `<sha256>.bin` | Files of 1.6.0 and earlier, imported on the next fetch of their artifact |

`Downloader` holds one `DownloadStore` and one `DownloadFetcher` per download directory (the production
`SpeechModels` has one `ModelManager`, so one of each per process): concurrent fetches of the same file share one
download only within one fetcher. `ModelInstaller` discards the store entry once the model is installed, or when the
archive does not unpack to the catalog files. `ModelManager.downloadedBytes`, `discardDownload`, `recycle` and the
start-up recycling go through `Downloader.downloadedBytes`, `discard` and `leftovers`, which cover both layouts. The
store's own retention (`cleanup`) is not used: paused downloads stay until they are recycled or a week old, as before.

## Network policy and trust

The kit opens its connections through `DownloadFetcher(open = ...)`. `Downloader` answers with a
`TransportUrlConnection`, an `HttpURLConnection` whose request goes through the plugin's `HttpTransport`: the production
`UrlConnectionTransport` keeps the policy of roadmap D9 / D24 (HTTPS only, entry hosts `github.com` and
`raw.githubusercontent.com`, every redirect hop checked by hand against the GitHub host suffixes) and the platform trust
configuration (`network_security_config` with the pinned ISRG Root X1 of roadmap P5.1 for API 24 - 25, unchanged). The
headers the kit sets (`Range`, `If-Range`, `Accept-Encoding: identity`) are sent on every hop; the answer gives the kit
its status, lengths, `Content-Range`, validators and the final URL (the host in failure details). The kit's timeouts and
redirect switch are ignored: the transport has its own (15 s connect, 30 s read, at most 5 redirects).

## Behaviour

- A verified file is reused without a request; its SHA-256 is computed again on every reuse.
- An unfinished file continues with `Range` after a dropped connection, a cancellation or a killed process, with
  `If-Range` when the server gave a strong validator (GitHub's release asset storage does); a `206` whose
  `Content-Range` or validator does not match starts over, a `200` starts over, a `416` drops the part.
- The bytes on disk continue on another URL of the same SHA-256 (for example after a catalog moves an archive) without
  `If-Range`; the final SHA-256 decides.
- A resumed file that fails its size or SHA-256 is downloaded whole once more, only once per fetch.
- Connection failures and server errors (5xx, 408, 429) are retried after 2 s and 5 s, each retry continuing the part.
- The SHA-256 is computed while downloading; the finished file is not read again.
- Cancelling: `CancelToken.cancel()` leaves the download within about 100 ms; when no other caller wants the file,
  `Downloader` also closes the open connection, so a read blocked on a stalled server returns at once (as before).

### Error codes of `fetch`

| Outcome of the kit | Exception (code) |
| --- | --- |
| Cancelled, or the token was cancelled meanwhile | `OperationCancelledException` (`SESSION_CANCELLED`) |
| `HTTP_STATUS` that a retry cannot fix (404 and other 4xx except 408 / 429 / 416) | `HttpStatusException` (`MODEL_DOWNLOAD_FAILED`): `ModelInstaller` falls back to the upstream assets |
| `HTTP_STATUS` after the retries (5xx, 408, 429) or 416 | `ModelException` (`MODEL_DOWNLOAD_FAILED`) |
| `CHECKSUM_MISMATCH`; `SIZE_MISMATCH` of a longer file | `IntegrityException` (`MODEL_CHECKSUM_MISMATCH`) |
| `SIZE_MISMATCH` of a shorter file after the retries (a cut connection) | `ModelException` (`MODEL_DOWNLOAD_FAILED`) |
| `NETWORK`, `SECURITY`, `OTHER` (including a URL the network policy refuses) | `ModelException` (`MODEL_DOWNLOAD_FAILED`) |
| The download directory cannot be created | `ModelException` (`IO_FAILED`) |

Failure messages name the final host and the number of retries.

### Files of 1.6.0 and earlier (Q51 A)

Before each fetch, `<sha256>.bin` (a verified file the earlier downloader did not install yet) and then `<sha256>.part`
are handed to `DownloadStore.importPartial`. An imported `.bin` has the full length, so the fetch checks its SHA-256
without a request; an imported `.part` continues with `Range`. A file that is empty, longer than the catalog size, or
shorter than what the store already holds is deleted; a file whose entry is downloading stays for the next fetch.
`remainingBytes` (free space checks) and `downloadedBytes` (the home screen's paused state) count these files before
they are imported.

## Tests

- JVM `DownloaderTest` (19): full download and progress, dropped connection, killed process, server ignoring `Range`,
  404, corrupt and oversized bodies, cancellation, the production policy (also through the kit), `If-Range` and a file
  replaced on the server, another URL of the same file, a resumed file failing its SHA-256 (whole once more, then
  refused), server errors retried, concurrent fetches sharing one download, the `.part` / `.bin` of 1.6.0 and unusable
  ones, discarding and recycling. `ModelManagerTest` (32) covers the installer and manager on the store layout.
- Device `ModelDownloadDeviceTest`, run with `py .python/device_download_run.py local|github <serial>`:
  - `local`: a server on the PC reached through `adb reverse` serves a 3,000,000 byte blob with a strong ETag; the
    plugin forbids cleartext through `HttpURLConnection`, so these cases use a socket transport of the test, and the
    kit, its coroutines, the store on the device, `Downloader` and the import are the production code. Cases: a
    connection cut at 1,000,000 bytes continues on its own (`Range` with `If-Range`), a download paused at about 1 MB
    continues in the next downloader, a corrupt body is refused with `MODEL_CHECKSUM_MISMATCH` (also after a resume,
    then after one whole download) and nothing is kept, the `.part` of 1.6.0 continues with `Range` and its verified
    `.bin` is reused without a request.
  - `github`: the production transport downloads the smallest mirror archive of the bundled catalog (about 6 MB),
    pauses after 2 MB and continues.

### Device runs (2026-11-17, debug build 1.6.1 of the P12.2 commit)

| Device | local | github | Note |
| --- | --- | --- | --- |
| AVD API 33 (google_apis x86_64, temporary `pc2_p13_macaw_api33`) | 4 / 4 PASS | PASS: `autojs6-speech-segment.pyannote.3-0-v1-6441ef8a.tar` (5994496 B) via `release-assets.githubusercontent.com`, paused at 2074240 B, resumed, SHA-256 verified | |
| AVD API 24 (default x86_64, temporary `pc2_p13_macaw_api24`) | 4 / 4 PASS | PASS: same archive, paused at 2018752 B, resumed, SHA-256 verified | `TrustAnchorDeviceTest` PASS: catalog, release asset, releases page and official index hosts through the pinned ISRG Root X1 |

Requests the local server saw (identical on both AVDs except the bytes the slow answer had queued when the client left):

```
/blob/corrupt range=- if-range=- -> 200 3000000 B
/blob/corrupt range=bytes=1000000- if-range=- -> 206 2000000 B
/blob/corrupt range=- if-range=- -> 200 3000000 B
/blob/drop range=- if-range=- -> 200 1000000 B
/blob/drop range=bytes=1000000- if-range="macaw-blob-v1" -> 206 2000000 B
/blob/legacy range=bytes=1500000- if-range=- -> 206 1500000 B
/blob/slow range=- if-range=- -> 200 2424832 B
/blob/slow range=bytes=1114112- if-range="macaw-blob-v1" -> 206 1885888 B
```

`/blob/legacy-verified` saw no request: the verified `.bin` of 1.6.0 was reused.
