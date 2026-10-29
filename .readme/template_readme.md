<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="{{ repo_url }}/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="{{ repo_url }}/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="{{ icon_alt }}" border="0" width="128" />
    </picture>
  </p>

  <p>{{ text_plugin_synopsis }}</p>

  <p>
    <a href="{{ repo_url }}/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/{{ repo_slug }}?label=Release"/></a>
    <a href="{{ repo_url }}/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/{{ repo_slug }}?color=A24232&label=Issues"/></a>
    <a href="{{ license_url }}"><img alt="GitHub License" src="https://img.shields.io/github/license/{{ repo_slug }}?color=534BAE&label=License"/></a>
  </p>
</div>

******

### {{ h3_languages_with_ascii }}

******

{{ p_languages_all_supported_for_readme }}:

{{ placeholder_ul_languages_all_supported }}

******

### {{ h3_introduction }}

******

{{ p_introduction_what }}

{{ p_introduction_how }}

> {{ p_status_note }}

******

### {{ h3_features }}

******

{{ placeholder_features }}

******

### {{ h3_screenshots }}

******

{{ p_screenshots_intro }}

<table>
  <tr>
    <td align="center">
      <img src="{{ repo_url }}/blob/master/docs/images/screenshots/home.png?raw=true" alt="{{ screenshot_home_caption }}" width="360" />
      <br />
      <sub>{{ screenshot_home_caption }}</sub>
    </td>
    <td align="center">
      <img src="{{ repo_url }}/blob/master/docs/images/screenshots/tryout-dictation.png?raw=true" alt="{{ screenshot_tryout_caption }}" width="360" />
      <br />
      <sub>{{ screenshot_tryout_caption }}</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="{{ repo_url }}/blob/master/docs/images/screenshots/voice-input.png?raw=true" alt="{{ screenshot_voice_caption }}" width="360" />
      <br />
      <sub>{{ screenshot_voice_caption }}</sub>
    </td>
    <td align="center">
      <img src="{{ repo_url }}/blob/master/docs/images/screenshots/assistant.png?raw=true" alt="{{ screenshot_assistant_caption }}" width="360" />
      <br />
      <sub>{{ screenshot_assistant_caption }}</sub>
    </td>
  </tr>
</table>

******

### {{ h3_usage }}

******

{{ placeholder_usage_steps }}

> {{ p_usage_note }}

******

### {{ h3_models }}

******

{{ p_models_intro }}

{{ placeholder_models_table }}

{{ p_models_license }}

{{ p_models_import }}

******

### {{ h3_tts_setup }}

******

{{ placeholder_tts_setup_steps }}

{{ p_tts_setup_note }}

******

### {{ h3_script_api }}

******

{{ p_script_api_intro }}:

```javascript
// 1. Dictation: partial text while you speak, one final per sentence, 30 seconds in all
let session = speech.listen({ silenceEndMillis: 1500, timeoutMillis: 0 });
session.on("partial", (e) => console.log("...", e.text));
session.on("final", (e) => files.append("./dictation.txt", e.text + "\n"));
setTimeout(() => session.stop(), 30000);

// 2. A wake word, then one round of dictation on the same recording
let wake = speech.wake(["hey macaw", "小六小六"], { thenListen: true });
wake.on("wake", (e) => e.listen.on("final", (r) => {
    console.log(e.keyword, r.text);
    wake.stop();
}));

// 3. Synthesis to a WAV file, and speech through the system TTS engine
let result = speech.synthesize("Task completed", "./done.wav", { model: "tts.vits.melo.zh-en" });
console.log(result.durationMillis, result.sampleRate);
speech.speak("任务完成");
tts("Task completed", { engine: "{{ plugin_application_id }}" });
```

{{ placeholder_api_points }}

******

### {{ h3_compatibility }}

******

{{ placeholder_compatibility_points }}

******

### {{ h3_faq }}

******

{{ placeholder_faq }}

******

### {{ h3_security }}

******

{{ p_security_intro }}

{{ placeholder_security_points }}

******

### {{ h3_plugin_interface }}

******

{{ p_plugin_interface }}:

```text
application id: {{ plugin_application_id }}
plugin id: {{ plugin_id }}
engine: {{ plugin_engine }}
variant: {{ plugin_variant }}
info service: {{ info_action }} (category {{ info_category }})
capability service: {{ discovery_action }} (category {{ discovery_category }})
wake action: {{ wake_action }}
tool service: {{ tool_action }} (short name {{ tool_short_name }}: listen, speak, transcribe, synthesize, speaker_verify, speaker_identify, diarize, detect_language, tag; AutoJs6 6.8.0+)
minimum host build: {{ required_host_version_code }}
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx {{ sherpa_onnx_version }}; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### {{ h3_acknowledgements }}

******

{{ p_acknowledgements_intro }}

{{ placeholder_acknowledgements }}

******

### {{ h3_roadmap }}

******

{{ p_roadmap }}

- [{{ text_link_roadmap }}]({{ roadmap_url }})

******

### {{ h3_release_history }}

******

{{ placeholder_latest_release_history }}

##### {{ h5_for_more_release_history }}

* {{ placeholder_read_more_in_changelog_md }}

******

### {{ h3_build }}

******

{{ p_build_intro }}

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\verify_native_alignment.py --check
py .python\check_repository.py
py -I .python\corresponding_source.py build --downloads <directory outside the repository>
```

******

### {{ h3_license }}

******

{{ p_license }}

- [{{ text_link_third_party }}]({{ third_party_url }})

******

### {{ h3_links }}

******

- {{ text_link_autojs6 }}: {{ autojs6_url }}
- {{ text_link_docs }}: {{ docs_url }}
- {{ text_link_sherpa_onnx }}: {{ sherpa_onnx_url }}
