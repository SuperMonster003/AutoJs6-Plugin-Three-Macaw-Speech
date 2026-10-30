package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import org.autojs.plugin.speech.api.SpeechLimits

/**
 * The catalog [SpeechToolService] returns from `IPluginToolProvider.getCatalog` (AutoJs6 plugin tool contract v1, roadmap
 * P7.6, D87): the short name `speech` (D94) and nine tools, which AutoJs6 lists as `speech_listen`, `speech_speak`,
 * `speech_transcribe`, `speech_synthesize`, since roadmap P8.3.5 `speech_speaker_verify` and `speech_speaker_identify`
 * (scores and names only, never a voiceprint), and since roadmap P8.4.4 `speech_diarize`, `speech_detect_language` and
 * `speech_tag` (D152). The document is static (D99): the host caches it per version code, and
 * missing prerequisites (microphone permission, models) are reported by the call, not by dropping tools. The
 * descriptions reach the context of AI models as written, so they stay English and ASCII and name the error codes.
 * zh-CN: [SpeechToolService] 从 `IPluginToolProvider.getCatalog` 返回的目录 (AutoJs6 插件工具契约 v1, 路线图 P7.6, D87): 短名
 * `speech` (D94) 下九个工具, AutoJs6 列为 `speech_listen`, `speech_speak`, `speech_transcribe`, `speech_synthesize`, 路线图 P8.3.5
 * 起的 `speech_speaker_verify` 与 `speech_speaker_identify` (只有分数与名字, 从不含声纹), 以及路线图 P8.4.4 起的 `speech_diarize`,
 * `speech_detect_language` 与 `speech_tag` (D152). 文档是静态的
 * (D99): 宿主按 versionCode 缓存, 缺少的前提 (麦克风权限, 模型) 由调用报告, 不从目录里删工具. 说明原样进入 AI 模型的上下文, 因此保持
 * 英文与 ASCII, 并写明错误码.
 */
internal object SpeechToolCatalog {

    const val SHORT_NAME = "speech"
    const val TOOL_LISTEN = "listen"
    const val TOOL_SPEAK = "speak"
    const val TOOL_TRANSCRIBE = "transcribe"
    const val TOOL_SYNTHESIZE = "synthesize"
    const val TOOL_SPEAKER_VERIFY = "speaker_verify"
    const val TOOL_SPEAKER_IDENTIFY = "speaker_identify"
    const val TOOL_DIARIZE = "diarize"
    const val TOOL_DETECT_LANGUAGE = "detect_language"
    const val TOOL_TAG = "tag"
    val TOOLS = listOf(
        TOOL_LISTEN, TOOL_SPEAK, TOOL_TRANSCRIBE, TOOL_SYNTHESIZE, TOOL_SPEAKER_VERIFY, TOOL_SPEAKER_IDENTIFY, TOOL_DIARIZE, TOOL_DETECT_LANGUAGE, TOOL_TAG,
    )

    /** D95: listening, including at most [NOTIFICATION_WAIT_SECONDS] for a tap on the notification. zh-CN: D95: 听写, 含至多 60 s 等待点按通知. */
    const val LISTEN_TIMEOUT_MS = 120_000L

    /** D96 - D98: the contract ceiling. zh-CN: D96 - D98: 契约上限. */
    const val LONG_TIMEOUT_MS = 300_000L

    /** P8.3.5: one recording of at most 60 s of speech. zh-CN: P8.3.5: 一段至多 60 s 语音的录音. */
    const val SPEAKER_TIMEOUT_MS = 120_000L

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
    const val MAX_SPEAKER_NAME_CHARS = SpeechLimits.MAX_SPEAKER_NAME_CHARS
    const val DEFAULT_TOP_K = SpeechLimits.DEFAULT_IDENTIFY_CANDIDATES
    const val MAX_TOP_K = SpeechLimits.MAX_IDENTIFY_CANDIDATES

    /** P8.4.4: the clustering distance of a diarization (cosine distance, 0 - 2). zh-CN: 分离的聚类距离 (余弦距离, 0 - 2). */
    const val MAX_CLUSTER_THRESHOLD = 2.0

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
    private val THRESHOLD = """"threshold": {"type": "number", "minimum": 0, "maximum": 1, "description": "Cosine score a match needs. Default: the value measured for the speaker model, 0.5 for the published model."}"""
    private val SPEAKER_FILE = """"$PARAM_FILE": {"type": "string", "x-autojs6-type": "file", "description": "An audio file with one person speaking, at least 1 second of speech; the first 60 seconds of speech are used."}"""
    private const val SPEAKER_AUDIO = "Reads WAV, MP3, AAC, M4A, Ogg Vorbis, Ogg Opus, FLAC and the audio track of MP4 or WebM files. Speakers are enrolled on the Speakers screen of 3-Macaw Speech (settings, resident wake word group) or by AutoJs6 scripts with speech.speaker.enroll; voiceprints stay on the phone and are never part of a result."
    private const val SPEAKER_ERRORS = "NOT_READY (no speaker recognition model of 3-Macaw Speech is installed), INPUT_UNREADABLE (unsupported or damaged file, or less than 1 second of speech, AUDIO_TOO_SHORT), LIMIT_EXCEEDED, CANCELLED"
    private const val ANALYSIS_AUDIO = "Reads WAV, MP3, AAC, M4A, Ogg Vorbis, Ogg Opus, FLAC and the audio track of MP4 or WebM files"
    private const val ANALYSIS_ERRORS = "INPUT_UNREADABLE (unsupported or damaged file), LIMIT_EXCEEDED, INVALID_ARGUMENTS, CANCELLED"
    private val SPEED = """"speed": {"type": "number", "minimum": $MIN_SPEED, "maximum": $MAX_SPEED, "default": $DEFAULT_SPEED, "description": "Speaking rate, 1.0 is normal."}"""

    val JSON: String = """{
  "shortName": "$SHORT_NAME",
  "tools": [
    {
      "name": "$TOOL_LISTEN",
      "title": "Listen for speech (3-Macaw Speech)",
      "description": "Listens for one utterance on the phone's microphone and returns what was said, recognized offline by 3-Macaw Speech. Models that do not punctuate their text get punctuation, and English text its capital letters, from an installed punctuation model of 3-Macaw Speech. Listening ends when the speaker pauses for silenceMillis, when nobody speaks for noSpeechSeconds (then text is empty and endReason is no-speech, which is not an error), or after maxSeconds. Wake word listeners of 3-Macaw Speech and of other apps pause while it listens and resume afterwards. Needs the microphone permission of 3-Macaw Speech and an installed speech recognition model for the language. When the plugin cannot use the microphone in the background, it posts a 'Tap to speak' notification showing prompt and waits up to $NOTIFICATION_WAIT_SECONDS seconds for the user to tap it. Errors: NOT_READY (no microphone permission, no recognition model, or no microphone access in the background; the message says what to turn on), BUSY (another dictation is running), INVALID_ARGUMENTS, CANCELLED.",
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
      "description": "Transcribes an audio file on the phone offline with an installed speech recognition model of 3-Macaw Speech and returns the full text with timed segments. Models that do not punctuate their text get punctuation, and English text its capital letters, from an installed punctuation model of 3-Macaw Speech. Reads WAV, MP3, AAC, M4A, Ogg Vorbis, Ogg Opus, FLAC and the audio track of MP4 or WebM files up to 200 MB and 30 minutes; phones transcribe roughly 15 to 30 minutes of audio within the timeout. When the result would be too large, segments keep their times without text and warnings say so. Errors: NOT_READY (no recognition model for the language), INPUT_UNREADABLE (unsupported or damaged file), LIMIT_EXCEEDED (over 200 MB or 30 minutes), INVALID_ARGUMENTS, CANCELLED.",
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
    },
    {
      "name": "$TOOL_SPEAKER_VERIFY",
      "title": "Verify a speaker (3-Macaw Speech)",
      "description": "Checks offline whether the voice in an audio file on the phone is the enrolled speaker of the given name in 3-Macaw Speech and returns the cosine score, the threshold and whether the score reaches it. $SPEAKER_AUDIO Errors: INVALID_ARGUMENTS (no speaker of this name is enrolled, SPEAKER_NOT_FOUND, or a bad argument), $SPEAKER_ERRORS.",
      "inputSchema": {
        "type": "object",
        "properties": {
          $SPEAKER_FILE,
          "name": {"type": "string", "minLength": 1, "maxLength": $MAX_SPEAKER_NAME_CHARS, "description": "The name the speaker was enrolled under."},
          $THRESHOLD
        },
        "required": ["$PARAM_FILE", "name"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "name": {"type": "string"},
          "score": {"type": "number"},
          "matched": {"type": "boolean"},
          "threshold": {"type": "number"},
          "modelId": {"type": "string"},
          $WARNINGS
        },
        "required": ["name", "score", "matched", "threshold", "modelId", "warnings"],
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $SPEAKER_TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_SPEAKER_IDENTIFY",
      "title": "Identify a speaker (3-Macaw Speech)",
      "description": "Finds offline which speaker enrolled in 3-Macaw Speech the voice in an audio file on the phone is closest to. Returns the best candidates with their cosine scores; name is present only when the best score reaches the threshold (matched true). With no speaker enrolled, candidates is empty and a warning says so. $SPEAKER_AUDIO Errors: INVALID_ARGUMENTS, $SPEAKER_ERRORS.",
      "inputSchema": {
        "type": "object",
        "properties": {
          $SPEAKER_FILE,
          $THRESHOLD,
          "topK": {"type": "integer", "minimum": 1, "maximum": $MAX_TOP_K, "default": $DEFAULT_TOP_K, "description": "How many candidates to return, best first."}
        },
        "required": ["$PARAM_FILE"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "name": {"type": "string"},
          "score": {"type": "number"},
          "matched": {"type": "boolean"},
          "threshold": {"type": "number"},
          "modelId": {"type": "string"},
          "candidates": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {"name": {"type": "string"}, "score": {"type": "number"}},
              "required": ["name", "score"],
              "additionalProperties": false
            }
          },
          $WARNINGS
        },
        "required": ["matched", "threshold", "modelId", "candidates", "warnings"],
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $SPEAKER_TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_DIARIZE",
      "title": "Find who spoke when (3-Macaw Speech)",
      "description": "Splits an audio file on the phone into speaker turns offline (speaker diarization) with the speaker segmentation and speaker recognition models of 3-Macaw Speech and returns the turns with the speaker of each, numbered from 0 in order of first appearance. Give speakers when the number of people is known; otherwise they are counted with threshold. With identify true, each speaker's longest turns (up to 10 seconds) are compared with the speakers enrolled in 3-Macaw Speech and names lists the closest one when it scores high enough. $ANALYSIS_AUDIO up to 10 minutes; diarize longer recordings with speech.diarize in an AutoJs6 script or on the Try it out screen of 3-Macaw Speech. Results never hold a voiceprint. Errors: NOT_READY (no speaker segmentation model or no speaker recognition model of 3-Macaw Speech is installed; install both), $ANALYSIS_ERRORS.",
      "inputSchema": {
        "type": "object",
        "properties": {
          "$PARAM_FILE": {"type": "string", "x-autojs6-type": "file", "description": "The audio file, at most 10 minutes long."},
          "speakers": {"type": "integer", "minimum": 0, "maximum": ${SpeechLimits.MAX_DIARIZATION_SPEAKERS}, "default": 0, "description": "The number of speakers when known; 0 counts them."},
          "threshold": {"type": "number", "minimum": 0, "maximum": $MAX_CLUSTER_THRESHOLD, "description": "Clustering distance used to count speakers when speakers is 0; larger values merge more voices into one speaker. Default: the value measured for the speaker model, 0.85 for the published model."},
          "identify": {"type": "boolean", "default": false, "description": "Also name the speakers enrolled in 3-Macaw Speech."}
        },
        "required": ["$PARAM_FILE"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "segments": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {"startMillis": {"type": "integer"}, "endMillis": {"type": "integer"}, "speaker": {"type": "integer"}},
              "required": ["startMillis", "endMillis", "speaker"],
              "additionalProperties": false
            }
          },
          "speakers": {"type": "integer"},
          "durationMillis": {"type": "integer"},
          "modelId": {"type": "string"},
          "speakerModelId": {"type": "string"},
          "names": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {"speaker": {"type": "integer"}, "name": {"type": "string"}, "score": {"type": "number"}},
              "required": ["speaker"],
              "additionalProperties": false
            }
          },
          $WARNINGS
        },
        "required": ["segments", "speakers", "durationMillis", "modelId", "speakerModelId", "warnings"],
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $LONG_TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_DETECT_LANGUAGE",
      "title": "Detect the spoken language (3-Macaw Speech)",
      "description": "Tells offline which language is spoken in an audio file on the phone with a language identification model of 3-Macaw Speech (Whisper, 99 languages). Speech is cut into windows of up to 30 seconds and the first windows are judged sentence by sentence, so a recording that mixes languages shows them all (very short sentences join a neighbor); language is the language spoken longest, candidates gives each language's share of the speaking time and segments the language of each sentence. Languages are two-letter codes such as en, zh or ja. $ANALYSIS_AUDIO up to 200 MB and 30 minutes. Errors: NOT_READY (no language identification model of 3-Macaw Speech is installed), INPUT_UNREADABLE (unsupported or damaged file, or no speech in it, AUDIO_TOO_SHORT), LIMIT_EXCEEDED, INVALID_ARGUMENTS, CANCELLED.",
      "inputSchema": {
        "type": "object",
        "properties": {
          "$PARAM_FILE": {"type": "string", "x-autojs6-type": "file", "description": "The audio file."},
          "windows": {"type": "integer", "minimum": 1, "maximum": ${SpeechLimits.MAX_LANGUAGE_WINDOWS}, "default": ${SpeechLimits.DEFAULT_LANGUAGE_WINDOWS}, "description": "How many speech windows of up to 30 seconds are judged, from the start of the file; each sentence takes its own pass, so more windows take longer."}
        },
        "required": ["$PARAM_FILE"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "language": {"type": "string"},
          "candidates": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {"language": {"type": "string"}, "score": {"type": "number"}},
              "required": ["language", "score"],
              "additionalProperties": false
            }
          },
          "segments": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {"startMillis": {"type": "integer"}, "endMillis": {"type": "integer"}, "language": {"type": "string"}},
              "required": ["startMillis", "endMillis", "language"],
              "additionalProperties": false
            }
          },
          "durationMillis": {"type": "integer"},
          "modelId": {"type": "string"},
          $WARNINGS
        },
        "required": ["language", "candidates", "segments", "durationMillis", "modelId", "warnings"],
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $LONG_TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_TAG",
      "title": "Recognize sounds (3-Macaw Speech)",
      "description": "Recognizes the sounds in an audio file on the phone offline (audio tagging) with an audio tagging model of 3-Macaw Speech and returns the topK most likely of the 527 AudioSet labels, such as Speech, Music, Dog or Siren. The file is scored in windows of 10 seconds; each label keeps its best score and the window where it was best. Labels are English. $ANALYSIS_AUDIO up to 200 MB and 30 minutes. Errors: NOT_READY (no audio tagging model of 3-Macaw Speech is installed), $ANALYSIS_ERRORS.",
      "inputSchema": {
        "type": "object",
        "properties": {
          "$PARAM_FILE": {"type": "string", "x-autojs6-type": "file", "description": "The audio file."},
          "topK": {"type": "integer", "minimum": 1, "maximum": ${SpeechLimits.MAX_TAG_TOP_K}, "default": ${SpeechLimits.DEFAULT_TAG_TOP_K}, "description": "How many labels to return, best first."}
        },
        "required": ["$PARAM_FILE"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "labels": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "label": {"type": "string"}, "mid": {"type": "string"}, "score": {"type": "number"}, "startMillis": {"type": "integer"}, "endMillis": {"type": "integer"}
              },
              "required": ["label", "mid", "score", "startMillis", "endMillis"],
              "additionalProperties": false
            }
          },
          "durationMillis": {"type": "integer"},
          "modelId": {"type": "string"},
          $WARNINGS
        },
        "required": ["labels", "durationMillis", "modelId", "warnings"],
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $LONG_TIMEOUT_MS,
      "maxConcurrency": 1
    }
  ]
}"""

}
