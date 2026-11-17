"""Device runs of the model downloader on the shared download kit (host plugin-center roadmap P13.2): runs
ModelDownloadDeviceTest and prints one line per case plus the requests the local server saw.

    py .python/device_download_run.py local <serial>    a local HTTP server on this PC, reached by the device through
                                                        `adb reverse`: dropped connection, paused download, SHA-256
                                                        mismatch, files of 1.6.0 (no network needed)
    py .python/device_download_run.py github <serial>   the smallest mirror archive of the bundled catalog (about 6 MB)
                                                        from GitHub with the production transport, paused and resumed

Install first: `.\\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest`, then
`py .python/device_speech_run.py install <serial>`. The server serves the 3,000,000 byte blob of DownloaderTest
(byte i = (i * 31 + i // 7) & 0xff) with a strong ETag; `Range` and `If-Range` follow RFC 9110. Per path: `drop` cuts
its first answer after 1,000,000 bytes, `slow` sends 64 KiB every 50 ms, `corrupt` flips byte 2,000,000.
"""
import re
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_speech_run as speech  # noqa: E402

SIZE = 3_000_000
BLOB = bytes(((i * 31 + i // 7) & 0xFF) for i in range(SIZE))
CORRUPT = BLOB[:2_000_000] + bytes([(BLOB[2_000_000] + 1) & 0xFF]) + BLOB[2_000_001:]
ETAG = '"macaw-blob-v1"'
DROP_AFTER = 1_000_000
CHUNK = 64 * 1024
TEST = "ModelDownloadDeviceTest"


class Server(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self):
        super().__init__(("127.0.0.1", 0), Handler)
        self.lock = threading.Lock()
        self.counts = {}
        self.records = []


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):
        pass

    def do_GET(self):
        server = self.server
        path = urlsplit(self.path).path
        case = path.rsplit("/", 1)[-1]
        range_header = self.headers.get("Range")
        if_range = self.headers.get("If-Range")
        with server.lock:
            count = server.counts[path] = server.counts.get(path, 0) + 1
        body = CORRUPT if case == "corrupt" else BLOB
        start, status = 0, 200
        match = re.fullmatch(r"bytes=(\d+)-", range_header or "")
        if match and (if_range is None or if_range == ETAG):
            start, status = int(match.group(1)), 206
            if start >= len(body):
                self.send_response(416)
                self.send_header("Content-Length", "0")
                self.send_header("Connection", "close")
                self.end_headers()
                self.record(path, range_header, if_range, 416, 0)
                return
        payload = body[start:]
        self.send_response(status)
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("ETag", ETAG)
        self.send_header("Accept-Ranges", "bytes")
        if status == 206:
            self.send_header("Content-Range", f"bytes {start}-{len(body) - 1}/{len(body)}")
        self.send_header("Connection", "close")
        self.end_headers()
        limit = len(payload)
        if case == "drop" and count == 1:
            limit = max(0, DROP_AFTER - start)
        sent = 0
        try:
            while sent < limit:
                chunk = payload[sent:min(limit, sent + CHUNK)]
                self.wfile.write(chunk)
                self.wfile.flush()
                sent += len(chunk)
                if case == "slow":
                    time.sleep(0.05)
        except OSError:
            pass
        finally:
            self.record(path, range_header, if_range, status, sent)
            if sent < len(payload):
                self.close_connection = True
                try:
                    self.connection.shutdown(2)
                except OSError:
                    pass

    def record(self, path, range_header, if_range, status, sent):
        with self.server.lock:
            self.server.records.append((path, range_header, if_range, status, sent))


def local(serial):
    server = Server()
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    speech.adb(serial, "reverse", f"tcp:{port}", f"tcp:{port}")
    try:
        ok = speech.run_test(serial, TEST, [f"macawDownloadServer=http://127.0.0.1:{port}"])
    finally:
        speech.adb(serial, "reverse", "--remove", f"tcp:{port}", check=False)
        server.shutdown()
    print(f"{speech.label(serial)}: local server requests (path, Range, If-Range, status, body bytes):")
    for path, range_header, if_range, status, sent in server.records:
        print(f"    {path} range={range_header or '-'} if-range={if_range or '-'} -> {status} {sent} B")
    return ok


def github(serial):
    return speech.run_test(serial, f"{TEST}#realGitHubDownloadPausesAndContinues", ["macawDownloadGitHub=true"])


def main():
    if len(sys.argv) != 3 or sys.argv[1] not in ("local", "github"):
        raise SystemExit(__doc__)
    ok = local(sys.argv[2]) if sys.argv[1] == "local" else github(sys.argv[2])
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
