package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogFile
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The synthesis loop and the sherpa-onnx configurations (roadmap P4.1): chunks in order with pauses and progress,
 * cancellation between and inside chunks, sink failures that never escape the native callback, and the `vits` /
 * `matcha` / `kokoro` configurations built from `runtime.config` (or inferred for imported models).
 * zh-CN: 合成循环与 sherpa-onnx 配置 (路线图 P4.1): 按顺序带停顿与进度的分段, 段间与段内的取消, 不会从原生回调中抛出的输出失败, 以及由
 * `runtime.config` 构建 (或为导入模型推断) 的 `vits` / `matcha` / `kokoro` 配置.
 */
class TtsSynthesisTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** Answers [perChunk] samples per chunk in two pieces and records what it was asked. zh-CN: 每段分两块返回 [perChunk] 个采样并记录请求. */
    private class FakeGenerator(override val sampleRate: Int = 1000, private val perChunk: Int = 10) : TtsGenerator {
        override val speakerCount = 4
        val calls = ArrayList<Triple<String, Int, Float>>()
        var callbackReturns = ArrayList<Boolean>()

        override fun generate(text: String, speakerId: Int, speed: Float, onSamples: (FloatArray) -> Boolean) {
            calls += Triple(text, speakerId, speed)
            val half = FloatArray(perChunk / 2) { 0.25f }
            for (piece in listOf(half, FloatArray(0), half)) {
                val keep = onSamples(piece)
                callbackReturns += keep
                if (!keep) return
            }
        }
    }

    @Test
    fun `chunks are synthesized in order with pauses and progress`() {
        val generator = FakeGenerator()
        val received = ArrayList<Int>()
        val progress = ArrayList<Pair<Int, Int>>()
        val text = "One. ... Two! Three"
        val outcome = TtsSynthesis.run(generator, text, 2, 1.5f, { false }, { done, total -> progress += done to total }, { received += it.size; true })
        assertEquals(listOf("One.", "Two!", "Three"), generator.calls.map { it.first })
        assertTrue(generator.calls.all { it.second == 2 && it.third == 1.5f })
        val pause = 1000 * TtsSynthesis.PAUSE_MILLIS / 1000
        assertEquals(listOf(5, 5, pause, 5, 5, pause, 5, 5), received)
        assertEquals(TtsSynthesis.Outcome(30L + 2 * pause, 3, cancelled = false), outcome)
        assertEquals(text.length, progress.last().first)
        assertTrue(progress.all { it.second == text.length })
        assertEquals("one report per chunk, the silent one included", 4, progress.size)
    }

    @Test
    fun `a cancel stops between chunks and inside a chunk`() {
        var cancelled = false
        val generator = FakeGenerator()
        val outcome = TtsSynthesis.run(generator, "One. Two. Three.", 0, 1f, { cancelled }, { done, _ -> if (done >= 4) cancelled = true }, { true })
        assertTrue(outcome.cancelled)
        assertEquals(listOf("One."), generator.calls.map { it.first })

        var pieces = 0
        val inside = FakeGenerator()
        val stopped = TtsSynthesis.run(inside, "Only one.", 0, 1f, { pieces >= 1 }, { _, _ -> }, { pieces++; true })
        assertTrue(stopped.cancelled)
        assertEquals("the callback answered stop", listOf(true, false), inside.callbackReturns)
        assertEquals(5L, stopped.samples)
    }

    @Test
    fun `a sink that refuses stops the synthesis`() {
        val generator = FakeGenerator()
        val outcome = TtsSynthesis.run(generator, "One. Two.", 0, 1f, { false }, { _, _ -> }, { false })
        assertTrue(outcome.cancelled)
        assertEquals(listOf(false), generator.callbackReturns)
    }

    @Test
    fun `a sink failure never leaves the callback and is thrown afterwards`() {
        val generator = FakeGenerator()
        val error = assertThrows(SpeechException::class.java) {
            TtsSynthesis.run(generator, "One. Two.", 0, 1f, { false }, { _, _ -> }, { throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "full") })
        }
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, error.code)
        assertEquals("the callback answered stop instead of throwing", listOf(false), generator.callbackReturns)
        assertEquals(1, generator.calls.size)
    }

    @Test
    fun `text without speakable chunks produces no audio`() {
        val generator = FakeGenerator()
        val outcome = TtsSynthesis.run(generator, " ... \u3002 ", 0, 1f, { false }, { _, _ -> }, { true })
        assertEquals(TtsSynthesis.Outcome(0, 0, cancelled = false), outcome)
        assertTrue(generator.calls.isEmpty())
    }

    private fun model(type: String, config: Map<String, Any>, files: List<String>, kind: String = "tts"): InstalledModel {
        val directory = folder.newFolder()
        files.forEach { path -> File(directory, path).apply { parentFile?.mkdirs() }.writeText("x") }
        return InstalledModel(
            modelId = "tts.test.$type", versionCode = 1, source = "catalog", origin = "mirror", kind = kind, modelType = type,
            names = emptyMap(), languages = listOf("zh"), sampleRate = 22050, speakers = 2, license = null, minSherpaOnnx = "1.13.8",
            runtimeConfig = config, catalogVersion = 1, installedAtMillis = 0, files = files.map { CatalogFile(it, 1, "0".repeat(64)) },
            directory = directory,
        )
    }

    @Test
    fun `vits, matcha and kokoro configurations follow runtime config`() {
        val vits = model("vits", mapOf("model" to "model.onnx", "tokens" to "tokens.txt", "lexicon" to listOf("lexicon.txt"), "ruleFsts" to listOf("phone.fst", "date.fst")),
            listOf("model.onnx", "tokens.txt", "lexicon.txt", "phone.fst", "date.fst"))
        val config = TtsEngineCore.config(vits, threads = 3)
        assertEquals(File(vits.directory, "model.onnx").path, config.model.vits.model)
        assertEquals(File(vits.directory, "lexicon.txt").path, config.model.vits.lexicon)
        assertEquals(File(vits.directory, "tokens.txt").path, config.model.vits.tokens)
        assertEquals("", config.model.vits.dataDir)
        assertEquals(listOf("phone.fst", "date.fst").joinToString(",") { File(vits.directory, it).path }, config.ruleFsts)
        assertEquals("", config.ruleFars)
        assertEquals(1, config.maxNumSentences)
        assertEquals(3, config.model.numThreads)
        assertEquals("", config.model.kokoro.model)

        val matcha = model("matcha", mapOf("acousticModel" to "model-steps-3.onnx", "vocoder" to "hifigan_v2.onnx", "tokens" to "tokens.txt", "lexicon" to listOf("lexicon.txt")),
            listOf("model-steps-3.onnx", "hifigan_v2.onnx", "tokens.txt", "lexicon.txt"))
        val matchaConfig = TtsEngineCore.config(matcha).model.matcha
        assertEquals(File(matcha.directory, "model-steps-3.onnx").path, matchaConfig.acousticModel)
        assertEquals(File(matcha.directory, "hifigan_v2.onnx").path, matchaConfig.vocoder)

        val kokoro = model("kokoro", mapOf("model" to "model.onnx", "voices" to "voices.bin", "tokens" to "tokens.txt", "dataDir" to "espeak-ng-data", "lexicon" to listOf("lexicon-us-en.txt", "lexicon-zh.txt")),
            listOf("model.onnx", "voices.bin", "tokens.txt", "espeak-ng-data/phontab", "lexicon-us-en.txt", "lexicon-zh.txt"))
        val kokoroConfig = TtsEngineCore.config(kokoro).model.kokoro
        assertEquals(File(kokoro.directory, "voices.bin").path, kokoroConfig.voices)
        assertEquals(File(kokoro.directory, "espeak-ng-data").path, kokoroConfig.dataDir)
        assertEquals(listOf("lexicon-us-en.txt", "lexicon-zh.txt").joinToString(",") { File(kokoro.directory, it).path }, kokoroConfig.lexicon)
    }

    @Test
    fun `imported models get inferred roles and broken ones fail clearly`() {
        val imported = model("vits", emptyMap(), listOf("en_US-voice.onnx", "tokens.txt", "espeak-ng-data/phontab", "date.fst", "rule.far"))
        val config = TtsEngineCore.config(imported)
        assertEquals(File(imported.directory, "en_US-voice.onnx").path, config.model.vits.model)
        assertEquals(File(imported.directory, "espeak-ng-data").path, config.model.vits.dataDir)
        assertEquals(File(imported.directory, "date.fst").path, config.ruleFsts)
        assertEquals(File(imported.directory, "rule.far").path, config.ruleFars)
        val importedMatcha = model("matcha", emptyMap(), listOf("model-steps-3.onnx", "vocos-22khz.onnx", "tokens.txt", "lexicon.txt"))
        val matcha = TtsEngineCore.config(importedMatcha).model.matcha
        assertTrue(matcha.acousticModel.endsWith("model-steps-3.onnx"))
        assertTrue(matcha.vocoder.endsWith("vocos-22khz.onnx"))
        assertTrue(matcha.lexicon.endsWith("lexicon.txt"))

        val missing = model("vits", mapOf("model" to "model.onnx", "tokens" to "tokens.txt"), listOf("model.onnx"))
        assertEquals(SpeechErrorCodes.MODEL_MISSING, assertThrows(SpeechException::class.java) { TtsEngineCore.config(missing) }.code)
        val unknown = model("zipvoice", mapOf("model" to "model.onnx", "tokens" to "tokens.txt"), listOf("model.onnx", "tokens.txt"))
        assertEquals(SpeechErrorCodes.ENGINE_FAILED, assertThrows(SpeechException::class.java) { TtsEngineCore.config(unknown) }.code)
        val noDataDir = model("vits", mapOf("model" to "model.onnx", "tokens" to "tokens.txt", "dataDir" to "espeak-ng-data"), listOf("model.onnx", "tokens.txt"))
        assertEquals(SpeechErrorCodes.MODEL_MISSING, assertThrows(SpeechException::class.java) { TtsEngineCore.config(noDataDir) }.code)
    }

    @Test
    fun `the native callback declares the method the JNI side calls`() {
        // libsherpa-onnx-jni.so looks up invoke([F)Ljava/lang/Integer; on the callback object (a device crash with a
        // Kotlin 2 lambda showed it): the class must declare exactly that signature.
        val method = TtsEngine.SampleCallback::class.java.getDeclaredMethod("invoke", FloatArray::class.java)
        assertEquals(Int::class.javaObjectType, method.returnType)
        val callback = TtsEngine.SampleCallback { it.size < 3 }
        assertEquals(1, method.invoke(callback, FloatArray(2)))
        assertEquals(0, method.invoke(callback, FloatArray(3)))
    }

    @Test
    fun `speaker names come from speakers txt`() {
        val withNames = model("vits", emptyMap(), listOf("model.onnx", "tokens.txt", "speakers.txt"))
        File(withNames.directory, "speakers.txt").writeText("SSB0005\nSSB0009\n")
        val speakers = TtsEngineCore.speakers(withNames)
        assertEquals(2, speakers.count)
        assertEquals(listOf("SSB0005", "SSB0009"), speakers.names)
        assertFalse(TtsEngineCore.speakers(model("vits", emptyMap(), listOf("model.onnx"))).names.isNotEmpty())
    }
}
