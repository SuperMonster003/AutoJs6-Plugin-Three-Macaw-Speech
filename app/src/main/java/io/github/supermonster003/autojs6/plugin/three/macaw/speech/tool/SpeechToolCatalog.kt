package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

/**
 * The catalog [SpeechToolService] returns from `IPluginToolProvider.getCatalog` (AutoJs6 plugin tool contract v1, roadmap
 * P7.6, D87): the short name `speech` (D94) and four tools, which AutoJs6 lists as `speech_listen`, `speech_speak`,
 * `speech_transcribe` and `speech_synthesize`. The document is static (D99): the host caches it per version code, and
 * missing prerequisites (microphone permission, models) are reported by the call, not by dropping tools. The
 * descriptions reach the context of AI models as written, so they stay English and ASCII and name the error codes.
 * zh-CN: [SpeechToolService] 从 `IPluginToolProvider.getCatalog` 返回的目录 (AutoJs6 插件工具契约 v1, 路线图 P7.6, D87): 短名
 * `speech` (D94) 下四个工具, AutoJs6 列为 `speech_listen`, `speech_speak`, `speech_transcribe` 与 `speech_synthesize`. 文档是静态的
 * (D99): 宿主按 versionCode 缓存, 缺少的前提 (麦克风权限, 模型) 由调用报告, 不从目录里删工具. 说明原样进入 AI 模型的上下文, 因此保持
 * 英文与 ASCII, 并写明错误码.
 */
internal object SpeechToolCatalog {

    const val SHORT_NAME = "speech"
    const val TOOL_LISTEN = "listen"
    const val TOOL_SPEAK = "speak"
    const val TOOL_TRANSCRIBE = "transcribe"
    const val TOOL_SYNTHESIZE = "synthesize"
    val TOOLS = listOf(TOOL_LISTEN, TOOL_SPEAK, TOOL_TRANSCRIBE, TOOL_SYNTHESIZE)

    /** D95: listening, including at most [NOTIFICATION_WAIT_SECONDS] for a tap on the notification. zh-CN: D95: 听写, 含至多 60 s 等待点按通知. */
    const val LISTEN_TIMEOUT_MS = 120_000L

    /** D96 - D98: the contract ceiling. zh-CN: D96 - D98: 契约上限. */
    const val LONG_TIMEOUT_MS = 300_000L

    const val NOTIFICATION_WAIT_SECONDS = 60

    const val MIN_SILENCE_MILLIS = 300
    const val MAX_SILENCE_MILLIS = 5000
    const val DEFAULT_SILENCE_MILLIS = 1200
    const val MIN_NO_SPEECH_SECONDS = 1
    const val MAX_NO_SPEECH_SECONDS = 30
    const val DEFAULT_NO_SPEECH_SECONDS = 8
    const val MIN_MAX_SECONDS = 1
    const val MAX_MAX_SECONDS = 60
    const val DEFAULT_MAX_SECONDS = 30
    const val MAX_HOTWORDS = 100
    const val MAX_HOTWORD_CHARS = 128
    const val MAX_PROMPT_CHARS = 200

    /** D96: `TextToSpeech.getMaxSpeechInputLength()`. zh-CN: D96: Android 单次朗读上限. */
    const val MAX_SPEAK_CHARS = 4000

    /** D98: the synthesis limit of the speech contract (D20). zh-CN: D98: 语音契约的合成上限 (D20). */
    const val MAX_SYNTHESIZE_CHARS = 8000

    const val MIN_SPEED = 0.5
    const val MAX_SPEED = 2.0
    const val DEFAULT_SPEED = 1.0
    const val MAX_LANGUAGE_CHARS = 35
    const val MAX_MODEL_CHARS = 128
    const val MAX_VOICE_CHARS = 257

    const val PARAM_FILE = "file"
    const val OUTPUT_AUDIO = "audio"
    const val FORMAT_WAV = "wav"
    const val FORMAT_M4A = "m4a"
    val FORMATS = listOf(FORMAT_WAV, FORMAT_M4A)
    const val MIME_WAV = "audio/wav"
    const val MIME_M4A = "audio/mp4"

    const val END_ENDPOINT = "endpoint"
    const val END_NO_SPEECH = "no-speech"
    const val END_MAX_DURATION = "max-duration"

    const val ENGINE_PLUGIN = "plugin"
    const val ENGINE_SYSTEM = "system"

    private val WARNINGS = """"warnings": {"type": "array", "items": {"type": "object", "properties": {"code": {"type": "string"}, "message": {"type": "string"}}, "required": ["code", "message"], "additionalProperties": false}}"""
    private val LANGUAGE = """"language": {"type": "string", "minLength": 1, "maxLength": $MAX_LANGUAGE_CHARS"""
    private val VOICE = """"voice": {"type": "string", "minLength": 3, "maxLength": $MAX_VOICE_CHARS"""
    private val SPEED = """"speed": {"type": "number", "minimum": $MIN_SPEED, "maximum": $MAX_SPEED, "default": $DEFAULT_SPEED, "description": "Speaking rate, 1.0 is normal."}"""

    val JSON: String = """{
  "shortName": "$SHORT_NAME",
  "tools": [
    {
      "name": "$TOOL_LISTEN",
      "title": "Listen for speech (3-Macaw Speech)",
      "description": "Listens for one utterance on the phone's microphone and returns what was said, recognized offline by 3-Macaw Speech. Listening ends when the speaker pauses for silenceMillis, when nobody speaks for noSpeechSeconds (then text is empty and endReason is no-speech, which is not an error), or after maxSeconds. Wake word listeners of 3-Macaw Speech and of other apps pause while it listens and resume afterwards. Needs the microphone permission of 3-Macaw Speech and an installed speech recognition model for the language. When the plugin cannot use the microphone in the background, it posts a 'Tap to speak' notification showing prompt and waits up to $NOTIFICATION_WAIT_SECONDS seconds for the user to tap it. Errors: NOT_READY (no microphone permission, no recognition model, or no microphone access in the background; the message says what to turn on), BUSY (another dictation is running), INVALID_ARGUMENTS, CANCELLED.",
      "inputSchema": {
        "type": "object",
        "properties": {
          $LANGUAGE, "description": "BCP 47 language tag such as en or zh-CN; picks an installed recognition model that speaks it. Default: the dictation model chosen in 3-Macaw Speech."},
          "silenceMillis": {"type": "integer", "minimum": $MIN_SILENCE_MILLIS, "maximum": $MAX_SILENCE_MILLIS, "default": $DEFAULT_SILENCE_MILLIS, "description": "A pause this long after speech ends the utterance."},
          "noSpeechSeconds": {"type": "integer", "minimum": $MIN_NO_SPEECH_SECONDS, "maximum": $MAX_NO_SPEECH_SECONDS, "default": $DEFAULT_NO_SPEECH_SECONDS, "description": "Stop when nobody speaks for this long."},
          "maxSeconds": {"type": "integer", "minimum": $MIN_MAX_SECONDS, "maximum": $MAX_MAX_SECONDS, "default": $DEFAULT_MAX_SECONDS, "description": "Longest utterance."},
          "hotwords": {"type": "array", "items": {"type": "string", "minLength": 1, "maxLength": $MAX_HOTWORD_CHARS}, "maxItems": $MAX_HOTWORDS, "description": "Words or names to favor, such as contact or product names. Only some models support them; the others ignore them with a warning."},
          "prompt": {"type": "string", "maxLength": $MAX_PROMPT_CHARS, "description": "Short text shown in the 'Tap to speak' notification, such as the question being asked."}
        },
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "text": {"type": "string"},
          "language": {"type": "string"},
          "modelId": {"type": "string"},
          "durationMillis": {"type": "integer"},
          "endReason": {"type": "string", "enum": ["$END_ENDPOINT", "$END_NO_SPEECH", "$END_MAX_DURATION"]},
          $WARNINGS
        },
        "required": ["text", "modelId", "durationMillis", "endReason", "warnings"],
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $LISTEN_TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_SPEAK",
      "title": "Read text aloud (3-Macaw Speech)",
      "description": "Reads text aloud on the phone through Android text-to-speech. 3-Macaw Speech's own engine speaks when one of its installed voices speaks the language; otherwise the system default text-to-speech engine does. With wait true (default) the call returns when reading ends, with the playing time and the engine used; with wait false it returns as soon as reading starts. Cancelling the call or its timeout stops reading at once. Plays as assistant audio at the current volume. Errors: NOT_READY (no engine can speak the language), BUSY (another speak call is reading), INVALID_ARGUMENTS, CANCELLED.",
      "inputSchema": {
        "type": "object",
        "properties": {
          "text": {"type": "string", "minLength": 1, "maxLength": $MAX_SPEAK_CHARS, "description": "The text to read."},
          $LANGUAGE, "description": "BCP 47 language tag such as en or zh-CN. Default: guessed from the text."},
          $VOICE, "description": "A voice of 3-Macaw Speech as <modelId>/<speaker>, where speaker is a number or a name of the model. Only applies when 3-Macaw Speech's own engine reads."},
          $SPEED,
          "wait": {"type": "boolean", "default": true, "description": "Return when reading ends (true) or as soon as it starts (false)."}
        },
        "required": ["text"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "engine": {"type": "string", "enum": ["$ENGINE_PLUGIN", "$ENGINE_SYSTEM"]},
          "enginePackage": {"type": "string"},
          "language": {"type": "string"},
          "voice": {"type": "string"},
          "completed": {"type": "boolean"},
          "durationMillis": {"type": "integer"},
          $WARNINGS
        },
        "required": ["engine", "enginePackage", "language", "completed", "warnings"],
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $LONG_TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_TRANSCRIBE",
      "title": "Transcribe an audio file (3-Macaw Speech)",
      "description": "Transcribes an audio file on the phone offline with an installed speech recognition model of 3-Macaw Speech and returns the full text with timed segments. Reads WAV, MP3, AAC, M4A, Ogg Vorbis, Ogg Opus, FLAC and the audio track of MP4 or WebM files up to 200 MB and 30 minutes; phones transcribe roughly 15 to 30 minutes of audio within the timeout. When the result would be too large, segments keep their times without text and warnings say so. Errors: NOT_READY (no recognition model for the language), INPUT_UNREADABLE (unsupported or damaged file), LIMIT_EXCEEDED (over 200 MB or 30 minutes), INVALID_ARGUMENTS, CANCELLED.",
      "inputSchema": {
        "type": "object",
        "properties": {
          "$PARAM_FILE": {"type": "string", "x-autojs6-type": "file", "description": "The audio file."},
          $LANGUAGE, "description": "BCP 47 language tag such as en or zh-CN; picks an installed recognition model that speaks it and is passed to models that detect several languages."},
          "model": {"type": "string", "minLength": 1, "maxLength": $MAX_MODEL_CHARS, "description": "The id of an installed recognition model, overriding the choice by language."}
        },
        "required": ["$PARAM_FILE"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "text": {"type": "string"},
          "segments": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {"startMillis": {"type": "integer"}, "endMillis": {"type": "integer"}, "text": {"type": "string"}},
              "required": ["startMillis", "endMillis"],
              "additionalProperties": false
            }
          },
          "language": {"type": "string"},
          "modelId": {"type": "string"},
          "durationMillis": {"type": "integer"},
          $WARNINGS
        },
        "required": ["text", "segments", "modelId", "durationMillis", "warnings"],
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $LONG_TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_SYNTHESIZE",
      "title": "Synthesize speech to a file (3-Macaw Speech)",
      "description": "Turns text into speech offline with an installed text-to-speech model of 3-Macaw Speech and writes it to the output audio: WAV (16-bit mono PCM, default) or M4A (AAC, mono, 64 kbps). Name the output file with the extension of the format. The output holds at most 8 MiB, about 3 minutes of WAV or 15 minutes of M4A. Choose a voice as <modelId>/<speaker>, or let language pick the default voice of a model that speaks it. Errors: NOT_READY (no text-to-speech model of 3-Macaw Speech speaks the language), LIMIT_EXCEEDED (the audio would be larger than 8 MiB), INVALID_ARGUMENTS (for example an unknown voice), CANCELLED.",
      "inputSchema": {
        "type": "object",
        "properties": {
          "text": {"type": "string", "minLength": 1, "maxLength": $MAX_SYNTHESIZE_CHARS, "description": "The text to speak."},
          $LANGUAGE, "description": "BCP 47 language tag such as en or zh-CN; picks a model that speaks it when voice is absent. Default: guessed from the text."},
          $VOICE, "description": "A voice of 3-Macaw Speech as <modelId>/<speaker>, where speaker is a number or a name of the model."},
          $SPEED,
          "format": {"type": "string", "enum": ["$FORMAT_WAV", "$FORMAT_M4A"], "default": "$FORMAT_WAV", "description": "wav (audio/wav) or m4a (audio/mp4)."}
        },
        "required": ["text"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "format": {"type": "string", "enum": ["$FORMAT_WAV", "$FORMAT_M4A"]},
          "mimeType": {"type": "string"},
          "modelId": {"type": "string"},
          "voice": {"type": "string"},
          "sampleRate": {"type": "integer"},
          "durationMillis": {"type": "integer"},
          "bytes": {"type": "integer"},
          $WARNINGS
        },
        "required": ["format", "mimeType", "modelId", "voice", "sampleRate", "durationMillis", "bytes", "warnings"],
        "additionalProperties": false
      },
      "outputs": [
        {"name": "$OUTPUT_AUDIO", "kind": "file", "mimeType": "$MIME_WAV", "description": "The speech: audio/wav, or audio/mp4 when format is m4a."}
      ],
      "risk": "write",
      "timeoutMs": $LONG_TIMEOUT_MS,
      "maxConcurrency": 1
    }
  ]
}"""

}
