3-Macaw Speech runs speech recognition, wake word detection and speech synthesis offline on your phone; once the models are downloaded, all processing stays on the device and no audio is uploaded. It works on its own without AutoJs6 and can also serve as the system text-to-speech engine, voice input and digital assistant. With AutoJs6, scripts can take dictation, transcribe audio files, wait for wake words and read text aloud.

### Getting started

1. Open 3-Macaw Speech and tap "Grant" in the "Microphone permission" row on the home screen.
2. On the home screen, download the models you need in the "Speech recognition", "Wake words" and "Speech synthesis" groups; models marked "Recommended" are a good place to start.
3. Tap the "Try it out" button on the home screen to try dictation, wake words and synthesis.
4. To use it in scripts, enable this plugin in the AutoJs6 "Plugin center".

### Main features

- **Speech recognition**: live dictation from the microphone, plus transcription of audio files and the sound in videos; offline models cover Chinese, English, Japanese, Cantonese and more. After you download a "Punctuation" model, results are punctuated automatically.
- **Wake words**: type the keyword itself, with no model training; after a wake word is heard, it can go on to take one sentence of dictation.
- **Speech synthesis**: neural voices read text aloud or save it as audio files; as the system text-to-speech engine, it also reads aloud for the AutoJs6 `tts` module and other apps.
- **Voice input and digital assistant**: microphone buttons in other apps can use it for offline dictation; as the default digital assistant, long-press the power or Home key, say a sentence, and the text goes to an app that accepts voice commands.
- **Triggers**: entry points such as the "Quick Settings tile", "Home screen widget and shortcut" and "Floating microphone" all open the same dictation sheet; it does not record at other times.
- **Resident wake word**: off by default; when on, saying a wake word opens the dictation sheet, and a wake word can also "Send a broadcast" or "Notify only" instead. While it is on, the microphone indicator stays lit and battery use goes up.
- **Speaker recognition**: enroll your voice on the "Speakers" page, then turn on "Only enrolled voices" so the resident wake word responds only to enrolled people.
- **File transcription and analysis**: the "File" tab of "Try it out" turns audio into text and can "Save as subtitles"; it can also separate speakers, identify the language and tag sounds.
- **Model management**: a model is enabled only after each of its files is verified; when a new version is out, update it right on the model card, or use "Import a local model".
- **Plugin tools**: through the MCP Server plugin or `plugins.tools.call`, listen for a sentence, read text aloud and transcribe audio files.

### Using it in scripts

In AutoJs6 Rhino scripts, these features are available through the global module `speech`; download wake word, speech recognition and speech synthesis models first. The example below listens for a sentence after a wake word, then prints the recognized text and reads it aloud:

```js
let wake = speech.wake(["hey macaw", "小六小六"], { thenListen: true });
wake.on("wake", e => {
    e.listen.on("final", r => {
        console.log(r.text);
        speech.speak(r.text);
    });
});
```

- `speech.recognize` transcribes an audio file and `speech.synthesize` writes text to a WAV file; both block and cannot be called on the UI thread, so use `recognizeAsync` and `synthesizeAsync` in UI scripts.
- Only one dictation or wake session can be open at a time; opening another fails with `SESSION_BUSY` instead of waiting in line.
- Node.js scripts use the same API through `autojs6:speech`; Bun scripts use `host.speech`, which needs the `speech.control` authorization.
- See [AutoJs6 docs > speech](https://docs.autojs6.com/#/speech) for the full usage, including speaker recognition, audio analysis and model management.

### Requirements

- Android 7.0 or later. The plugin's own screens, the system text-to-speech engine, voice input and the digital assistant do not need AutoJs6.
- The `speech` module and the plugin tools need AutoJs6 6.8.0 or later, with this plugin enabled in the "Plugin center".
- The microphone permission is used for dictation and wake word detection and must be granted to this plugin itself; AutoJs6's microphone permission does not replace it. The notification permission is used to show model download progress.
- The overlay permission is optional and lets the "Floating microphone" and the resident wake word open the dictation sheet straight from the background; "Key trigger" must be turned on in the system accessibility settings, and it only filters keys and never reads the screen.
- Models are downloaded over the internet, from a few MB to a few hundred MB each; once downloaded, recognition and synthesis work offline.

### FAQ

**Setting it as the default system text-to-speech engine**

Tap "Open settings" in the "System text-to-speech engine" row on the home screen, then pick 3-Macaw Speech as the preferred engine on the system page. The engine only offers languages covered by installed synthesis models, so if the preview is silent, first download a synthesis model that covers the device language.

**Why the microphone indicator stays on with the resident wake word**

Listening for wake words means recording continuously, and since Android 12 the status bar shows the microphone indicator the whole time; apps cannot hide it. Consider limiting the "Listening time" or turning on "Listen only while charging" in settings.

**When model downloads fail**

The plugin downloads from GitHub first and switches to the upstream address automatically if that fails; interrupted downloads resume where they stopped. If downloads still fail, download the model archive on another device and import it with "Import a local model" on the home screen.

**Why the first dictation in a script takes a moment**

The plugin releases idle models, so the first dictation afterwards has to load the model again, which can take a few seconds on older phones. Setting "Release idle models" to "Never" in settings avoids the wait, at the cost of keeping that memory in use.

See the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech) for the model list, how to set up each entry point and more FAQs.
