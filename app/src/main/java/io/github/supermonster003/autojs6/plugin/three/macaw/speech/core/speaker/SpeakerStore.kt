package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.JsonNumber
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJsonException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * The voiceprints of the plugin (roadmap D125): `speakers.json` in [directory] (`filesDir/speakers/`, so clearing the
 * plugin's data removes them), with a format version, the previous good file as `speakers.json.bak` and atomic writes.
 * A file that cannot be read falls back to the backup; when both fail it is moved aside as `speakers.json.corrupt` and
 * the store starts empty, so one damaged write never blocks the feature. A file of a newer format (a downgraded plugin)
 * is kept untouched: the store reads as empty and refuses writes with `IO_FAILED`. Thread-safe, pure JVM.
 * zh-CN: 插件的声纹 (路线图 D125): [directory] (`filesDir/speakers/`, 清除插件数据即删除) 中的 `speakers.json`, 带格式版本, 上一个完好的文件
 * 保存为 `speakers.json.bak`, 原子写入. 无法读取的文件回退到备份; 两者都失败时将其移到 `speakers.json.corrupt` 并以空库开始, 一次损坏的写入不会
 * 让功能无法使用. 较新格式的文件 (插件被降级) 保持不动: 读作空库, 写入以 `IO_FAILED` 拒绝. 线程安全, 纯 JVM.
 */
class SpeakerStore(private val directory: File, private val clock: () -> Long = System::currentTimeMillis, private val log: (String) -> Unit = {}) {

    private val file = File(directory, FILE)
    private val backup = File(directory, "$FILE.bak")
    private val corrupt = File(directory, "$FILE.corrupt")

    private var cache: List<Voiceprint>? = null
    private var newerFormat = false

    @Synchronized
    fun list(): List<Voiceprint> = load()

    @Synchronized
    fun get(name: String): Voiceprint? = load().firstOrNull { it.name == name }

    /** Voiceprints that [modelId] can score. zh-CN: [modelId] 可以打分的声纹. */
    @Synchronized
    fun of(modelId: String): List<Voiceprint> = load().filter { it.modelId == modelId }

    /**
     * Adds [embeddings] to [name] (a new voiceprint, or the earlier recordings of the same model unless [replace])
     * and writes the store at once; nothing changes when the write fails.
     * zh-CN: 把 [embeddings] 加入 [name] (新声纹, 或除非 [replace], 否则并入同一模型的已有录音) 并立即写入; 写入失败时不做任何改动.
     */
    @Synchronized
    fun enroll(name: String, modelId: String, modelVersion: Int, embeddings: List<FloatArray>, replace: Boolean): Voiceprint {
        val current = load()
        val earlier = current.firstOrNull { it.name == name }
        if (earlier == null && current.size >= SpeechLimits.MAX_SPEAKERS) {
            throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "At most ${SpeechLimits.MAX_SPEAKERS} speakers can be enrolled")
        }
        val kept = earlier?.takeUnless { replace }
        if (kept != null && kept.modelId != modelId) {
            throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "$name was enrolled with ${kept.modelId}; enroll again with replace to use $modelId")
        }
        if (kept != null && kept.dim != embeddings.first().size) {
            throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "$name was enrolled with another version of $modelId; enroll again with replace")
        }
        val mean = SpeakerMath.merge(kept?.mean, kept?.samples ?: 0, embeddings)
        val voiceprint = Voiceprint(name, modelId, modelVersion, (kept?.samples ?: 0) + embeddings.size, mean, clock())
        save(current.filter { it.name != name } + voiceprint)
        return voiceprint
    }

    @Synchronized
    fun remove(name: String): Boolean {
        val current = load()
        if (current.none { it.name == name }) return false
        save(current.filter { it.name != name })
        return true
    }

    private fun load(): List<Voiceprint> {
        cache?.let { return it }
        val read = read(file) ?: read(backup)?.also { log("Speakers: $FILE could not be read, using the backup") } ?: run {
            if (file.isFile && !newerFormat) {
                corrupt.delete()
                if (file.renameTo(corrupt)) log("Speakers: $FILE could not be read and was moved aside")
            }
            emptyList()
        }
        return read.sortedBy { it.name }.also { cache = it }
    }

    /** The voiceprints of [source], or null when it is missing or unreadable. zh-CN: [source] 中的声纹; 缺失或无法读取时为 null. */
    private fun read(source: File): List<Voiceprint>? {
        if (!source.isFile) return null
        return try {
            if (source.length() > MAX_FILE_BYTES) throw StrictJsonException("too large")
            val root = StrictJson.parseObject(source.readBytes())
            val format = (root["format"] as? JsonNumber)?.longOrNull() ?: throw StrictJsonException("no format")
            if (format > FORMAT_VERSION) {
                newerFormat = true
                log("Speakers: $FILE has format $format, newer than $FORMAT_VERSION; kept untouched")
                return emptyList()
            }
            if (format != FORMAT_VERSION.toLong()) throw StrictJsonException("format $format")
            val speakers = root["speakers"] as? List<*> ?: throw StrictJsonException("no speakers")
            speakers.map { item -> voiceprint(item as? Map<*, *> ?: throw StrictJsonException("speaker is not an object")) }
                .also { list -> if (list.map { it.name }.toSet().size != list.size) throw StrictJsonException("duplicate names") }
        } catch (e: StrictJsonException) {
            log("Speakers: ${source.name} is unreadable (${e.message})")
            null
        } catch (e: IOException) {
            log("Speakers: ${source.name} is unreadable (${e.message})")
            null
        } catch (e: SpeechException) {
            // A stored name the rules refuse. zh-CN: 存储的名字不符合规则.
            log("Speakers: ${source.name} is unreadable (${e.message})")
            null
        } catch (e: RuntimeException) {
            log("Speakers: ${source.name} is unreadable (${e.javaClass.simpleName})")
            null
        }
    }

    private fun voiceprint(item: Map<*, *>): Voiceprint {
        fun number(key: String): Long = (item[key] as? JsonNumber)?.longOrNull() ?: throw StrictJsonException("$key is not an integer")
        val name = SpeakerRules.name(item["name"] as? String ?: throw StrictJsonException("no name"))
        val mean = (item["mean"] as? List<*> ?: throw StrictJsonException("no mean")).map { value ->
            ((value as? JsonNumber)?.toDouble() ?: throw StrictJsonException("mean holds a non-number")).toFloat().also {
                if (!it.isFinite()) throw StrictJsonException("mean holds a non-finite number")
            }
        }.toFloatArray()
        if (mean.isEmpty() || mean.size > MAX_DIM) throw StrictJsonException("mean has ${mean.size} dimensions")
        val samples = number("samples").takeIf { it in 1..Int.MAX_VALUE } ?: throw StrictJsonException("samples out of range")
        return Voiceprint(
            name = name,
            modelId = item["model"] as? String ?: throw StrictJsonException("no model"),
            modelVersion = number("modelVersion").toInt(),
            samples = samples.toInt(),
            mean = mean,
            updatedAt = number("updatedAt"),
        )
    }

    private fun save(voiceprints: List<Voiceprint>) {
        if (newerFormat) throw SpeechException(SpeechErrorCodes.IO_FAILED, "The speakers were saved by a newer version of 3-Macaw Speech")
        val json = linkedMapOf(
            "format" to FORMAT_VERSION,
            "speakers" to voiceprints.sortedBy { it.name }.map { v ->
                linkedMapOf(
                    "name" to v.name,
                    "model" to v.modelId,
                    "modelVersion" to v.modelVersion,
                    "samples" to v.samples,
                    "updatedAt" to v.updatedAt,
                    "mean" to v.mean.map { JsonNumber(it.toString()) },
                )
            },
        )
        directory.mkdirs()
        val temporary = File(directory, ".$FILE.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(temporary).use {
                it.write(StrictJson.write(json))
                it.flush()
                it.fd.sync()
            }
            // The previous good file becomes the backup before the new one takes its place.
            if (file.isFile && read(file) != null) {
                backup.delete()
                file.copyTo(backup, overwrite = true)
            }
            if (!temporary.renameTo(file)) {
                // Windows (JVM tests) does not replace on rename.
                file.delete()
                if (!temporary.renameTo(file)) throw SpeechException(SpeechErrorCodes.IO_FAILED, "Cannot write the speakers")
            }
        } catch (e: IOException) {
            throw SpeechException(SpeechErrorCodes.IO_FAILED, "Cannot write the speakers: ${e.message}", e)
        } finally {
            temporary.delete()
        }
        cache = voiceprints.sortedBy { it.name }
    }

    companion object {
        const val FILE = "speakers.json"

        /** Format of `speakers.json`; a later plugin that changes it bumps this. zh-CN: `speakers.json` 的格式版本. */
        const val FORMAT_VERSION = 1

        /** 50 voiceprints of 512 dimensions take about 0.5 MiB. zh-CN: 50 个 512 维声纹约 0.5 MiB. */
        private const val MAX_FILE_BYTES = 4L * 1024 * 1024
        private const val MAX_DIM = 4096
    }
}
