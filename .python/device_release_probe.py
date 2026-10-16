# -*- coding: utf-8 -*-
"""Runs the platform-only release probe on devices (roadmap P0.2, R8 item).

Installs the signed, minified release split of one ABI together with the device test APK built with
`-PmacawReleaseProbe=true` (runner SherpaReleaseProbeInstrumentation), runs the probe with `am instrument` and removes
both packages again. The probe loads libsherpa-onnx-jni.so in the release build, checks the version and cuts the test
recording with the bundled VAD; only its one-line verdict is printed.

Prepare the APKs first (signing comes from the local sign.properties):

  .\\gradlew.bat :app:assembleRelease
  .\\gradlew.bat -PmacawReleaseProbe=true :app:assembleDebugAndroidTest

Usage:
  py .python/device_release_probe.py --run <serial>:<abi> [<serial>:<abi> ...]

Exit protocol: one `PASS` / `FAIL` line per run, then RELEASE_PROBE_OK or RELEASE_PROBE_ERROR (exit code 1).
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'io.github.supermonster003.autojs6.plugin.three.macaw.speech'
RUNNER = f'{PACKAGE}.test/{PACKAGE}.SherpaReleaseProbeInstrumentation'
ABIS = ('arm64-v8a', 'armeabi-v7a', 'x86_64', 'x86')


def version_name() -> str:
    text = (ROOT / 'version.properties').read_text(encoding='utf-8')
    return re.search(r'^VERSION_NAME=(.+)$', text, re.M).group(1).strip()


def adb(serial: str, *arguments: str, check: bool = True) -> str:
    result = subprocess.run(['adb', '-s', serial, *arguments], capture_output=True, text=True, encoding='utf-8', errors='replace')
    if check and result.returncode != 0:
        raise RuntimeError(f'adb {" ".join(arguments[:2])} failed: {(result.stdout + result.stderr).strip()[-300:]}')
    return result.stdout + result.stderr


def probe(serial: str, abi: str, release_apk: Path, test_apk: Path) -> tuple[bool, str]:
    adb(serial, 'uninstall', f'{PACKAGE}.test', check=False)
    adb(serial, 'uninstall', PACKAGE, check=False)
    try:
        adb(serial, 'install', '-r', '--abi', abi, str(release_apk))
        adb(serial, 'install', '-r', str(test_apk))
        output = adb(serial, 'shell', 'am', 'instrument', '-w', RUNNER, check=False)
        verdict = next((line.strip() for line in output.splitlines() if line.strip().startswith('RELEASE_RUNTIME_')), None)
        if verdict is None:
            return False, 'no probe verdict: ' + ' '.join(output.split())[:300]
        return verdict.startswith('RELEASE_RUNTIME_OK'), verdict
    finally:
        adb(serial, 'uninstall', f'{PACKAGE}.test', check=False)
        adb(serial, 'uninstall', PACKAGE, check=False)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--run', nargs='+', required=True, metavar='SERIAL:ABI', help='device serial and the split ABI to install')
    args = parser.parse_args()
    release_dir = ROOT / 'app' / 'build' / 'outputs' / 'apk' / 'release'
    test_apks = sorted((ROOT / 'app' / 'build' / 'outputs' / 'apk' / 'androidTest' / 'debug').glob('*.apk'))
    if len(test_apks) != 1:
        print(f'RELEASE_PROBE_ERROR expected one device test APK, found {[p.name for p in test_apks]}')
        return 1
    failures = 0
    for item in args.run:
        serial, _, abi = item.rpartition(':')
        if not serial or abi not in ABIS:
            print(f'RELEASE_PROBE_ERROR invalid run {item!r}, expected <serial>:<{"|".join(ABIS)}>')
            return 1
        release_apk = release_dir / f'autojs6-plugin-three-macaw-speech-v{version_name()}-{abi}.apk'
        if not release_apk.is_file():
            print(f'RELEASE_PROBE_ERROR missing {release_apk.relative_to(ROOT).as_posix()}; run :app:assembleRelease')
            return 1
        sdk = adb(serial, 'shell', 'getprop', 'ro.build.version.sdk').strip()
        model = adb(serial, 'shell', 'getprop', 'ro.product.model').strip()
        try:
            ok, verdict = probe(serial, abi, release_apk, test_apks[0])
        except RuntimeError as error:
            ok, verdict = False, str(error)
        failures += not ok
        print(f'{"PASS" if ok else "FAIL"} {serial} ({model}, API {sdk}) {abi}: {verdict}')
    print('RELEASE_PROBE_OK' if failures == 0 else f'RELEASE_PROBE_ERROR {failures} run(s) failed')
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main())
