package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.MapOptionSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechOptions
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P7.6.2 (D89, D95, D100, D101 E2): the routes of `speech_listen` to the microphone, the foreground service rule,
 * the conversion of the arguments into dictation options, the end reasons, the notification time, the `NOT_READY`
 * message and the shape of the result.
 * zh-CN: 路线图 P7.6.2 (D89, D95, D100, D101 E2): `speech_listen` 取得麦克风的途径, 前台服务规则, 参数到听写选项的转换, 结束原因, 通知时长,
 * `NOT_READY` 信息与结果的形态.
 */
class ListenRulesTest {

    @Test
    fun `routes follow D89 in order`() {
        assertEquals(emptyList<ListenRoute>(), ListenRules.routes(MicrophoneAccess(sdk = 34)))
        assertEquals(listOf(ListenRoute.NOTIFY), ListenRules.routes(MicrophoneAccess(notificationsAllowed = true, sdk = 34)))
        assertEquals(
            listOf(ListenRoute.DIRECT, ListenRoute.OPEN, ListenRoute.NOTIFY),
            ListenRules.routes(MicrophoneAccess(residentRunning = true, assistantActive = true, notificationsAllowed = true, sdk = 35)),
        )
        listOf(
            MicrophoneAccess(residentRunning = true, sdk = 34),
            MicrophoneAccess(microphoneForeground = true, sdk = 34),
            MicrophoneAccess(screenVisible = true, sdk = 34),
        ).forEach { assertEquals(it.toString(), listOf(ListenRoute.DIRECT), ListenRules.routes(it)) }
        assertEquals("the assistant opens screens", listOf(ListenRoute.OPEN), ListenRules.routes(MicrophoneAccess(assistantActive = true, sdk = 34)))
        assertEquals("so does the overlay permission", listOf(ListenRoute.OPEN), ListenRules.routes(MicrophoneAccess(overlayGranted = true, sdk = 30)))
        assertEquals("Android 10 records in the background but opens no screen", listOf(ListenRoute.DIRECT), ListenRules.routes(MicrophoneAccess(sdk = 29)))
        assertEquals("Android 9 does both", listOf(ListenRoute.DIRECT, ListenRoute.OPEN), ListenRules.routes(MicrophoneAccess(sdk = 28)))
        assertEquals(listOf(ListenRoute.DIRECT, ListenRoute.OPEN, ListenRoute.NOTIFY), ListenRules.routes(MicrophoneAccess(notificationsAllowed = true, sdk = 24)))
    }

    @Test
    fun `only a direct dictation in the resident wake word's service goes without the microphone foreground service`() {
        val resident = MicrophoneAccess(residentRunning = true, screenVisible = true, sdk = 34)
        assertFalse(ListenRules.foregroundService(ListenRoute.DIRECT, resident))
        assertTrue(ListenRules.foregroundService(ListenRoute.OPEN, resident))
        assertTrue(ListenRules.foregroundService(ListenRoute.NOTIFY, resident))
        assertTrue(ListenRules.foregroundService(ListenRoute.DIRECT, MicrophoneAccess(screenVisible = true, sdk = 34)))
        assertTrue(ListenRules.foregroundService(ListenRoute.DIRECT, MicrophoneAccess(microphoneForeground = true, sdk = 34)))
        assertTrue(ListenRules.foregroundService(ListenRoute.DIRECT, MicrophoneAccess(sdk = 28)))
    }

    @Test
    fun `arguments become dictation options that the runtime accepts`() {
        val request = ListenRequest("fr-CA", 300, 1000, 60_000, listOf("AutoJs6", "Macaw"), "Which file?")
        assertEquals(
            mapOf(
                SpeechOptionKeys.MODEL to "sherpa-onnx-fr", SpeechOptionKeys.SILENCE_END_MILLIS to 300L, SpeechOptionKeys.TIMEOUT_MILLIS to 1000L,
                SpeechOptionKeys.MAX_DURATION_MILLIS to 60_000L, SpeechOptionKeys.HOTWORDS to listOf("AutoJs6", "Macaw"),
            ),
            ListenRules.options(request, "sherpa-onnx-fr"),
        )
        val defaults = ListenRules.options(ListenRequest(language = "de"), "m")
        assertFalse("no hotwords, no key", defaults.containsKey(SpeechOptionKeys.HOTWORDS))
        assertFalse("D95: the language only chooses the model", defaults.containsKey(SpeechOptionKeys.LANGUAGE))
        assertEquals(setOf(SpeechOptionKeys.MODEL, SpeechOptionKeys.SILENCE_END_MILLIS, SpeechOptionKeys.TIMEOUT_MILLIS, SpeechOptionKeys.MAX_DURATION_MILLIS), defaults.keys)

        listOf(
            ListenRequest(), request,
            ListenRequest(silenceMillis = 5000, noSpeechMillis = 30_000, maxMillis = 1000, hotwords = List(SpeechToolCatalog.MAX_HOTWORDS) { "w$it".padEnd(128, 'x') }),
        ).forEach { given ->
            val parsed = SpeechOptions.listen(MapOptionSource(ListenRules.options(given, "model-a")))
            assertEquals("model-a", parsed.model)
            assertEquals(given.silenceMillis, parsed.silenceEndMillis)
            assertEquals(given.noSpeechMillis, parsed.timeoutMillis)
            assertEquals(given.maxMillis, parsed.maxDurationMillis)
            assertEquals(given.hotwords, parsed.hotwords)
        }
    }

    @Test
    fun `end reasons of D95`() {
        assertEquals(SpeechToolCatalog.END_MAX_DURATION, ListenRules.endReason(SpeechContract.REASON_MAX_DURATION, heard = true))
        assertEquals(SpeechToolCatalog.END_MAX_DURATION, ListenRules.endReason(SpeechContract.REASON_MAX_DURATION, heard = false))
        assertEquals(SpeechToolCatalog.END_NO_SPEECH, ListenRules.endReason(SpeechContract.REASON_TIMEOUT, heard = false))
        assertEquals(SpeechToolCatalog.END_ENDPOINT, ListenRules.endReason(SpeechContract.REASON_TIMEOUT, heard = true))
        assertEquals(SpeechToolCatalog.END_ENDPOINT, ListenRules.endReason(SpeechContract.REASON_STOPPED, heard = true))
        assertEquals("a finish without text is still the endpoint", SpeechToolCatalog.END_ENDPOINT, ListenRules.endReason(SpeechContract.REASON_STOPPED, heard = false))
        assertEquals(SpeechToolCatalog.END_ENDPOINT, ListenRules.endReason(null, heard = false))
    }

    @Test
    fun `the notification waits at most a minute and leaves time to listen`() {
        assertEquals(60_000L, ListenRules.notificationWait(remainingMillis = 120_000, maxMillis = 30_000))
        assertEquals(55_000L, ListenRules.notificationWait(remainingMillis = 120_000, maxMillis = 60_000))
        assertEquals(15_000L, ListenRules.notificationWait(remainingMillis = 50_000, maxMillis = 30_000))
        assertEquals(ListenRules.MIN_TAP_MILLIS, ListenRules.notificationWait(remainingMillis = 40_000, maxMillis = 30_000))
        assertEquals("too little time skips it", 0L, ListenRules.notificationWait(remainingMillis = 39_999, maxMillis = 30_000))
        assertEquals(0L, ListenRules.notificationWait(remainingMillis = 0, maxMillis = 1000))
        assertEquals(0L, ListenRules.notificationWait(remainingMillis = -5, maxMillis = 1000))
    }

    @Test
    fun `a language no installed model covers gives a warning`() {
        assertNull(ListenRules.languageWarning(null, "m", listOf("en")))
        assertNull(ListenRules.languageWarning("en-GB", "m", listOf("en")))
        assertNull(ListenRules.languageWarning("ZH-hans", "m", listOf("zh", "en")))
        val warning = ListenRules.languageWarning("fr", "sherpa-onnx-en", listOf("en"))!!
        assertEquals(SpeechErrorCodes.MODEL_MISSING, warning.code)
        assertEquals("No installed recognition model covers the language 'fr'; sherpa-onnx-en listened instead", warning.message)
        assertEquals(mapOf("code" to warning.code, "message" to warning.message), warning.toJson())
    }

    @Test
    fun `the NOT_READY message names every route tried and only the missing remedies`() {
        val nothing = MicrophoneAccess(sdk = 34)
        assertEquals(
            "PERMISSION_DENIED: 3-Macaw Speech cannot use the microphone in the background (no route to the microphone is open). To fix this, " +
                "turn on the resident wake word, make 3-Macaw Speech the default digital assistant, allow it to display over other apps or allow its notifications",
            ListenRules.unavailable(emptyList(), nothing),
        )
        val tried = listOf(
            ListenRoute.OPEN to "the start screen did not open from the background within 2000 ms",
            ListenRoute.NOTIFY to "the \"Tap to speak\" notification was not tapped within 60 s",
        )
        assertEquals(
            "PERMISSION_DENIED: 3-Macaw Speech cannot use the microphone in the background (start screen: the start screen did not open from the background " +
                "within 2000 ms; notification: the \"Tap to speak\" notification was not tapped within 60 s). To fix this, turn on the resident wake word or " +
                "make 3-Macaw Speech the default digital assistant",
            ListenRules.unavailable(tried, MicrophoneAccess(overlayGranted = true, notificationsAllowed = true, sdk = 34)),
        )
        assertTrue(
            ListenRules.unavailable(listOf(ListenRoute.DIRECT to "Android silences the microphone"), MicrophoneAccess(assistantActive = true, overlayGranted = true, notificationsAllowed = true, sdk = 34))
                .endsWith("(direct: Android silences the microphone). To fix this, turn on the resident wake word"),
        )
    }

    @Test
    fun `the result has the catalog's keys and keeps the model warnings first`() {
        val model = ToolWarning(SpeechErrorCodes.MODEL_MISSING, "No installed recognition model covers the language 'fr'; m listened instead")
        val hotwords = ToolWarning(SpeechErrorCodes.INVALID_OPTIONS, "m does not support hotwords; 2 hotwords were ignored")
        val heard = ListenOutcome.Heard("open the door", "en", SpeechToolCatalog.END_ENDPOINT, 2350, listOf(hotwords, model))
        val result = ListenRules.result(heard, "en", "m", listOf(model))
        assertEquals(listOf("text", "language", "modelId", "durationMillis", "endReason", "warnings"), result.keys.toList())
        assertEquals(listOf(model.toJson(), hotwords.toJson()), result["warnings"])
        assertEquals(
            """{"text":"open the door","language":"en","modelId":"m","durationMillis":2350,"endReason":"endpoint","warnings":[""" +
                """{"code":"MODEL_MISSING","message":"No installed recognition model covers the language 'fr'; m listened instead"},""" +
                """{"code":"INVALID_OPTIONS","message":"m does not support hotwords; 2 hotwords were ignored"}]}""",
            String(StrictJson.write(result), Charsets.UTF_8).trimEnd(),
        )
        val silent = ListenRules.result(ListenOutcome.Heard("", null, SpeechToolCatalog.END_NO_SPEECH, 8000), null, "m", emptyList())
        assertEquals("the language is optional", listOf("text", "modelId", "durationMillis", "endReason", "warnings"), silent.keys.toList())
        assertEquals("", silent["text"])
        assertEquals(emptyList<Any>(), silent["warnings"])
    }

}
