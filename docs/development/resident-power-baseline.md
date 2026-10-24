# Resident wake word power baseline

Roadmap P8.1 (D123, D128). The power use of the resident wake word (P7.4) with the screen off, without and with the
power saving mode, and its hit counts. Measured with the debug build of P8.1 and sherpa-onnx 1.13.8 on the catalog model
`kws.zipformer.zh-en.2025-12-20` (chunk-16-left-64 int8; its keyword spotting numbers are in `kws-baseline.md`).

## Method

- Battery: `py .python/device_resident_run.py battery <serial> 30 on --power <variant>`, the method of P7.4.8: the
  switch on from the debug switch (a visible start, "only while charging" off, no time limit), home, `dumpsys
  batterystats --reset` and `dumpsys battery unplug` (the phone stays on USB; batterystats sees it on battery), the
  screen off for 30 minutes, then the batterystats estimate of the plugin's UID (from the device's power profile, not a
  measured current), the CPU time of the plugin's process (`/proc/<pid>/stat`), its busiest threads
  (`/proc/<pid>/task/*/stat`) and whether it still records. With the voice gate, the `MacawSessions` log says how much of
  the audio reached the keyword spotter.
- Variants (`ResidentPower.variant`, set in the plugin's process by the debug switch or the instrumentation argument
  `macawPower`):
  - `off`: the 1.2.0 wake session (the microphone hands 20 ms frames to the ASR thread, the spotter decodes everything).
  - `gate` (a): a voice gate before the spotter (`VoiceGatedKeywords`). It opens when a 32 ms window is 9 dB louder
    than the room's floor (the quietest window of the last 4 s; the first 0.25 s after a start are not measured).
    Closed, the audio only fills a 3 s lookback; open, the lookback and every window reach the spotter, until 1 s after
    the last rise; then a 0.4 s tail of silence flushes the spotter's last chunk and the spotter starts over.
  - `vad` (a, as first planned): the same gate, also opened by the bundled silero VAD (threshold 0.5, 0.25 s minimum
    speech and silence, its input 8 times louder). Measured on the G8441 only, because the VAD running on every window
    costs about half of what the spotter does (see the results).
  - `step` (b): 200 ms microphone frames instead of 20 ms (5 instead of 50 hand-overs and decodes per second). The
    spotter's onnxruntime already runs one thread (P3.4), the minimum; its 10 ms feature frames and 320 ms chunks are
    fixed by the model.
  - `both` (c): `gate` and `step` together.
- Hits: `py .python/device_resident_run.py phone <serial> --power <variant> --hit-rate 10` with the music volume at 70 %
  (`MACAW_VOLUME_PERCENT=70`, agreed for P7.4.7): the P7.4.7 cases (switch and hit, yielding, time limit, power,
  conflict) plus `theWakeWordIsCountedAtTwoLevels`, the keyword of `kws-zh-6.wav` played 10 times at the full level and
  10 times at a quarter of it (about -12 dB, a soft wake word) through the phone's speaker, 2 s apart; a play counts
  when the hit comes within 3 s of its end (a later one, within 5 s more, counts as late).
- Gate settings: `py .python/device_resident_run.py gate <serial> [plays]` records the room and the keyword (3 plays at
  each level by default, 30 s of the room alone) and spots the same recordings with the plain stream and several gate
  settings: hits, the time the gate opened, how often it opened and how much it decoded.

## Results

### Sony G8441 (Android 9), 2026-10-24: choosing the gate

Debug build of P8.1, the phone on its own speaker in the maintainer's room (room about -62 to -66 dBFS at the
microphone, the keyword about -50 dBFS at the full level and -62 dBFS at a quarter of it). Android 9 gives no
batterystats estimate for an app here, so only the plugin's CPU time counts; 10 minutes with the screen off instead of
30 (`battery BH900ASK9E 10 on --power <variant>`).

| Variant | Plugin CPU in 10 min | ASR thread | Microphone thread | Decoded | Hits, full / quarter |
| --- | --- | --- | --- | --- | --- |
| `off` (1.2.0) | 264.7 s | 242.2 s | 21.7 s | all | 10/10 / 10/10; 10/10 / 9/10; 10/10 / 10/10 |
| `step` (b): 200 ms frames | 234.8 s | 226.7 s | 7.0 s | all | |
| `gate` (a): 9 dB rise, 20 ms frames | 61.2 s | 27.2 s | 32.1 s | 65 s of 600, 16 openings | |
| `both` (c): 9 dB rise, 200 ms frames | 25.8 s | 13.9 s | 9.8 s | 44 s of 600, 10 openings | 10/10 / 10/10 |
| silero VAD alone, 20 ms (first design) | not measured | | | | 10/10 / 2/10; 10/10 / 0/10 |
| silero VAD or 9 dB rise, 200 ms | 145.3 s | 134.4 s | 8.6 s | 37 s of 600, 8 openings | 10/10 / 9/10 |

- The silero VAD alone missed the quiet keyword: run continuously, it fired on a quarter-level keyword (2 to 3 dB over
  the room in RMS) 2.1 to 2.3 s after the playback started or not at all, and the spotter, started over with a lookback
  that late, missed it. An input gain of 8 and a 3 s lookback did not change that.
- With the VAD in the gate the ASR thread still took 134 s although the spotter decoded 6 % of the audio: the VAD on
  every 32 ms window costs about half of what the spotter does on everything. The rise over the room's floor costs one
  pass over the samples, so the power saving mode opens the gate on the rise alone.
- The step alone saves little (11 %): the spotter decodes the same audio. With the gate the decoding is small and the
  20 ms hand-overs dominate (the microphone thread took 32 s), so the two together save the most (90 %). The
  microphone thread's time at 20 ms differs between runs (21.7 s in `off`, 32.1 s in `gate`), probably because a
  lightly loaded CPU runs at a lower clock.
- `gate` recordings (`device_resident_run.py gate`, 3 plays per level, the same recordings for every setting; hits, and
  how often 30 s of the room alone opened the gate):

| Setting | Full | Quarter | Room alone |
| --- | --- | --- | --- |
| plain spotter (1.2.0) | 3/3 | 2/3 | - |
| silero VAD alone | 3/3, opened 1.3 to 1.4 s after the play started | 0/3, opened 2.1 to 2.3 s after it or never | never |
| rise 6 dB | 3/3 | 2/3 | 8 openings, 24.9 s decoded |
| rise 9 dB | 3/3, opened 0.8 to 1.0 s after the play started | 1/3 | 2 openings, 9.6 s decoded |
| rise 12 dB | 3/3 | 0/3, never opened | 1 opening, 4.4 s decoded |

  Three plays a level are few; the live hit counts above (10 plays a level, the gate in the running wake session)
  decide the threshold.

### Hits with the power saving mode on and off (P8.1.4), 2026-10-24

`py .python/device_resident_run.py phone <serial> --power-saving on|off --hit-rate 10`, music volume at 70 %, one phone
at a time. The P7.4.7 cases (switch and hit, yielding, time limit, power, conflict, reboot reminder) and the two-level
count; P7.4.7 recorded `plays=1 ... again=1`, `client hits=1` and `background hit plays=1` on every phone.

| Phone | Power saving | P7.4.7 cases | Full | Quarter |
| --- | --- | --- | --- | --- |
| G8441 (Android 9) | on | all pass (switch and conflict rerun after the maintainer unlocked the phone) | 10/10 | 9/10 |
| G8441 (Android 9) | off | all pass (same rerun) | 10/10 | 8/10 |
| OnePlus OPD2413 (ColorOS, Android 15) | on | all pass but the reboot reminder once (`resumed=false`); 3 reruns pass | 10/10 | 10/10 |
| OnePlus OPD2413 (ColorOS, Android 15) | off | all pass | 10/10 | 10/10 |

- The first G8441 run of the switch and conflict cases found no Compose hierarchy: the battery run before them had
  turned the screen off and the phone locks with a PIN. The battery run now ends with `wm dismiss-keyguard`, which helps
  only without a credential.
- The OnePlus reboot reminder failure did not repeat (power saving on twice and off once); the wake session opened
  normally in its log.
