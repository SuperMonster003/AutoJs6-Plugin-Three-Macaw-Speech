package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioFiles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.EngineCache
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.EngineKey
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaEngines
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SpeakerEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerAudio
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerMatch
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerMath
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerScore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerStore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.Voiceprint
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.File

/**
 * Speaker recognition (roadmap P8.3, D132 - D141): voiceprints in `filesDir/speakers/` ([SpeakerStore]), embeddings from
 * the leased [SpeakerEngine] of a `speaker` model, the speech of every recording cut by the bundled VAD
 * ([SpeakerAudio]). Every call runs on the calling thread: a Binder thread of the host, the verification thread of the
 * resident wake word or a worker of the speaker screen. Names and scores are never logged.
 * zh-CN: 说话人识别 (路线图 P8.3, D132 - D141): 声纹保存在 `filesDir/speakers/` ([SpeakerStore]), 嵌入来自所租用的 `speaker` 模型的
 * [SpeakerEngine], 每段录音的语音由随包 VAD 截取 ([SpeakerAudio]). 每个调用都在调用线程上运行: 宿主的 Binder 线程, 常驻唤醒的验证线程或说话人
 * 页面的工作线程. 不记录名字与分数.
 */
internal class SpeakerRecognition(private val runtime: SpeechRuntime, val store: SpeakerStore) {

    /** One enrollment: the stored [voiceprint] and the speech of this call. zh-CN: 一次注册: 保存的 [voiceprint] 与本次调用的语音时长. */
    class Enrollment(val voiceprint: Voiceprint, val speechMillis: Long)

    /** A recording: decoded samples of the plugin, or a descriptor of the host. zh-CN: 一段录音: 插件自己的采样, 或宿主的描述符. */
    sealed class Recording(val label: String) {
        class Samples(val samples: FloatArray, label: String) : Recording(label)
        class Descriptor(val audio: ParcelFileDescriptor, label: String) : Recording(label)
    }

    /**
     * Enrolls [name] from [recordings], all or nothing (roadmap D134): every recording is decoded and embedded before the
     * store changes. [model] defaults to the default speaker model.
     * zh-CN: 以 [recordings] 注册 [name], 要么全部成功要么不变 (路线图 D134): 所有录音都解码并提取嵌入后才修改声纹库. [model] 默认取默认说话人模型.
     */
    fun enroll(name: String, recordings: List<Recording>, replace: Boolean, model: String?): Enrollment {
        val checked = SpeakerRules.name(name)
        SpeakerRules.recordings(recordings.size)
        val installed = runtime.resolve(SpeechContract.KIND_SPEAKER, model)
        val started = SystemClock.elapsedRealtime()
        return lease(installed).use { lease ->
            var speech = 0L
            val embeddings = recordings.map { recording ->
                val cut = speech(recording)
                speech += cut.speechMillis
                lease.engine.embed(cut.samples)
            }
            val voiceprint = store.enroll(checked, installed.modelId, installed.versionCode, embeddings, replace)
            Log.i(TAG, "Enrolled ${recordings.size} recording(s), $speech ms of speech, ${voiceprint.samples} sample(s), ${SystemClock.elapsedRealtime() - started} ms")
            Enrollment(voiceprint, speech)
        }
    }

    /**
     * Compares [recording] with the voiceprint of [name] (`SPEAKER_NOT_FOUND` without one), scored by the model that made
     * it; [threshold] defaults to that model's value (roadmap D138).
     * zh-CN: 以生成 [name] 声纹的模型将 [recording] 与该声纹比较 (没有声纹时为 `SPEAKER_NOT_FOUND`); [threshold] 默认取该模型的值 (路线图 D138).
     */
    fun verify(name: String, recording: Recording, threshold: Double?): SpeakerMatch {
        val checked = SpeakerRules.name(name)
        val voiceprint = store.get(checked) ?: throw SpeechException(SpeechErrorCodes.SPEAKER_NOT_FOUND, "No speaker of this name is enrolled")
        val limit = SpeakerRules.threshold(threshold, voiceprint.modelId)
        val installed = runtime.resolve(SpeechContract.KIND_SPEAKER, voiceprint.modelId)
        return lease(installed).use { lease ->
            if (lease.engine.dim != voiceprint.dim) {
                throw SpeechException(SpeechErrorCodes.SPEAKER_NOT_FOUND, "The voiceprint belongs to another version of ${voiceprint.modelId}; enroll again with replace")
            }
            val score = SpeakerMath.cosine(lease.engine.embed(speech(recording).samples), voiceprint.mean)
            val best = SpeakerScore(voiceprint.name, score)
            SpeakerMatch(best, limit, listOf(best), installed.modelId)
        }
    }

    /**
     * Finds the closest of the voiceprints of the default speaker model (roadmap D133, D141): no candidates without any,
     * then [SpeakerMatch.best] is null and nothing is decoded.
     * zh-CN: 在默认说话人模型的声纹中找出最接近的 (路线图 D133, D141): 没有声纹时候选为空, [SpeakerMatch.best] 为 null, 也不解码录音.
     */
    fun identify(recording: Recording, threshold: Double?, topK: Int?): SpeakerMatch {
        val k = SpeakerRules.topK(topK)
        val installed = runtime.resolve(SpeechContract.KIND_SPEAKER, null)
        val limit = SpeakerRules.threshold(threshold, installed.modelId)
        val voiceprints = store.of(installed.modelId)
        if (voiceprints.isEmpty()) return SpeakerMatch(null, limit, emptyList(), installed.modelId)
        return lease(installed).use { lease ->
            val candidates = SpeakerMath.rank(lease.engine.embed(speech(recording).samples), voiceprints, k)
            SpeakerMatch(candidates.firstOrNull(), limit, candidates, installed.modelId)
        }
    }

    /**
     * The check of "only enrolled voices" (roadmap D135, D139): [window] is about two seconds of microphone audio before a
     * wake word hit, mostly the wake word itself, so the speech the VAD finds is scored when it lasts a second and the
     * whole window otherwise. Null when the check cannot run (no speaker model, no voiceprint of it): the hit passes.
     * zh-CN: "只听主人" 的检查 (路线图 D135, D139): [window] 是唤醒词命中前约两秒的麦克风音频, 主要是唤醒词本身, 因此 VAD 找到的语音满一秒时对其打分,
     * 否则对整个窗口打分. 无法检查 (没有说话人模型, 没有其声纹) 时为 null, 命中照常放行.
     */
    fun gate(window: FloatArray): SpeakerMatch? {
        val installed = runCatching { runtime.resolve(SpeechContract.KIND_SPEAKER, null) }.getOrNull() ?: return null
        val voiceprints = store.of(installed.modelId).takeIf { it.isNotEmpty() } ?: return null
        val cut = SherpaEngines.vad(runtime.context.assets, SpeechRuntime.SESSION_VAD).use { vad -> SpeakerAudio.speech(window, vad) }
        val samples = if (cut.speechMillis >= SpeakerRules.MIN_SPEECH_MILLIS) cut.samples else window
        return lease(installed).use { lease ->
            val candidates = SpeakerMath.rank(lease.engine.embed(samples), voiceprints, 1)
            SpeakerMatch(candidates.firstOrNull(), SpeakerRules.threshold(installed.modelId), candidates, installed.modelId)
        }
    }

    fun list(): List<Voiceprint> = store.list().sortedBy { it.name }

    fun remove(name: String): Boolean = store.remove(SpeakerRules.name(name))

    /** Whether the default speaker model is installed and has a voiceprint (roadmap D139). zh-CN: 默认说话人模型已安装且有声纹 (路线图 D139). */
    fun ready(): Boolean = runCatching { store.of(runtime.resolve(SpeechContract.KIND_SPEAKER, null).modelId).isNotEmpty() }.getOrDefault(false)

    private fun lease(model: InstalledModel): EngineCache.Lease<SpeakerEngine> =
        runtime.engines.acquire(EngineKey(model.modelId, model.versionCode, "speaker"), model.installedBytes) { SherpaEngines.speaker(model) }

    /** The speech of one recording, at least one second (`AUDIO_TOO_SHORT`). zh-CN: 一段录音的语音, 至少一秒 (`AUDIO_TOO_SHORT`). */
    private fun speech(recording: Recording): SpeakerAudio.Speech = SherpaEngines.vad(runtime.context.assets, SpeechRuntime.SESSION_VAD).use { vad ->
        val cut = when (recording) {
            is Recording.Samples -> SpeakerAudio.speech(recording.samples, vad)
            is Recording.Descriptor -> AudioFiles.open(recording.audio).use { reader -> SpeakerAudio.speech(reader, vad, recording.label) }
        }
        SpeakerAudio.requireEnough(cut, recording.label)
    }

    companion object {
        private const val TAG = "MacawSpeakers"

        @Volatile
        private var instance: SpeakerRecognition? = null

        fun get(context: Context): SpeakerRecognition = instance ?: synchronized(this) {
            instance ?: SpeechRuntime.get(context).let { runtime ->
                SpeakerRecognition(runtime, SpeakerStore(File(runtime.context.filesDir, "speakers"), log = { Log.w(TAG, it) }))
            }.also { instance = it }
        }
    }
}
