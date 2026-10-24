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
  - `gate` (a): a voice gate before the spotter (`VoiceGatedKeywords`). It opens when the bundled silero VAD
    (threshold 0.5, 0.25 s minimum speech and silence, its input 8 times louder) hears speech or when a 32 ms window is
    9 dB louder than the room's floor (the quietest window of the last 4 s). Closed, the audio only fills a 3 s
    lookback; open, the lookback and every window reach the spotter, until 1 s after the last trigger; then a 0.4 s
    tail of silence flushes the spotter's last chunk and the spotter starts over.
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
