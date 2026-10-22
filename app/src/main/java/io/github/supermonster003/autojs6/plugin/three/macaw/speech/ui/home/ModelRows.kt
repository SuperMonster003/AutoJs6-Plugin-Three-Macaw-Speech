package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CommercialUse
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelStore

/** A model operation the screens started and still track. zh-CN: 界面发起并仍在跟踪的模型操作. */
internal data class TrackedOperation(
    val opId: Int,
    val modelId: String?,
    val kind: Kind,
    val started: Boolean = false,
    val done: Long = 0,
    val total: Long = 0,
) {
    enum class Kind { DOWNLOAD, IMPORT, REFRESH, RECYCLE }
}

/** What a model row shows and offers. zh-CN: 模型行显示与提供的内容. */
internal sealed class RowState {
    data object Installed : RowState()
    data object Available : RowState()

    /** A download that stopped with [downloadedBytes] on disk; installing again resumes it. zh-CN: 已停止的下载, 再次安装即续传. */
    data class Paused(val downloadedBytes: Long) : RowState()

    data class Queued(val opId: Int) : RowState()
    data class Running(val opId: Int, val done: Long, val total: Long) : RowState()
}

internal enum class LicenseTag { COMMERCIAL, NON_COMMERCIAL, UNKNOWN }

/** One row of the home model list (roadmap P5.1). zh-CN: 首页模型列表的一行 (路线图 P5.1). */
internal data class ModelRow(
    val id: String,
    val kind: String,
    val name: String,
    val languages: List<String>,
    val sizeBytes: Long,
    val installedBytes: Long?,
    val license: LicenseTag,
    val licenseSpdx: String,
    val noticeKey: String?,
    val noticeUrl: String?,
    val isDefault: Boolean,
    val recommended: Boolean,
    val speakers: Int?,
    val sampleRate: Int?,
    val version: Int,
    val modelType: String,
    val imported: Boolean,
    /** Part of the APK (the silero VAD): never downloaded or removed. zh-CN: APK 内置 (silero VAD): 不下载也不删除. */
    val builtin: Boolean,
    val state: RowState,
)

/** A kind group of the list; [installed] counts its installed rows. zh-CN: 列表中按种类的分组; [installed] 为已安装行数. */
internal data class ModelGroup(val kind: String, val rows: List<ModelRow>) {
    val installed: Int get() = rows.count { it.state == RowState.Installed }
}

/**
 * Builds the grouped rows from `ModelManager.list()` (roadmap D26): recognition, its punctuation (D107), wake words,
 * synthesis, then the bundled VAD; inside a group the default model first, then installed, recommended, catalog order.
 * Pure Kotlin.
 * zh-CN: 由 `ModelManager.list()` 生成分组的行 (路线图 D26): 识别, 识别的标点 (D107), 唤醒词, 合成, 最后是内置 VAD; 组内默认模型在前, 然后依次为
 * 已安装, 推荐, 目录顺序.
 * 纯 Kotlin.
 */
internal object ModelRows {
    val KIND_ORDER = listOf("asr", "punct", "kws", "tts", "vad")

    fun build(
        entries: List<ModelEntry>,
        operations: Collection<TrackedOperation>,
        builtinIds: Set<String>,
        downloadedBytes: (String) -> Long,
    ): List<ModelGroup> {
        val byModel = operations.filter { it.kind == TrackedOperation.Kind.DOWNLOAD && it.modelId != null }.associateBy { it.modelId!! }
        val rows = entries.mapIndexed { index, entry -> index to row(entry, byModel[entry.id], entry.id in builtinIds, downloadedBytes) }
        return KIND_ORDER.mapNotNull { kind ->
            val group = rows.filter { it.second.kind == kind }.sortedWith(
                compareByDescending<Pair<Int, ModelRow>> { it.second.isDefault && it.second.state == RowState.Installed }
                    .thenByDescending { it.second.state == RowState.Installed }
                    .thenByDescending { it.second.recommended }
                    .thenBy { it.first },
            ).map { it.second }
            if (group.isEmpty()) null else ModelGroup(kind, group)
        }
    }

    private fun row(entry: ModelEntry, operation: TrackedOperation?, builtin: Boolean, downloadedBytes: (String) -> Long): ModelRow {
        val state = when {
            entry.installed && operation == null -> RowState.Installed
            operation != null && !operation.started -> RowState.Queued(operation.opId)
            operation != null -> RowState.Running(operation.opId, operation.done, operation.total)
            builtin -> RowState.Installed
            else -> downloadedBytes(entry.id).takeIf { it > 0 }?.let(RowState::Paused) ?: RowState.Available
        }
        return ModelRow(
            id = entry.id,
            kind = entry.kind,
            name = entry.name,
            languages = entry.languages,
            sizeBytes = entry.sizeBytes,
            installedBytes = entry.installedBytes,
            license = when (entry.commercialUse) {
                CommercialUse.ALLOWED -> LicenseTag.COMMERCIAL
                CommercialUse.NOT_ALLOWED -> LicenseTag.NON_COMMERCIAL
                CommercialUse.UNKNOWN -> LicenseTag.UNKNOWN
            },
            licenseSpdx = entry.licenseSpdx,
            noticeKey = entry.noticeKey,
            noticeUrl = entry.noticeUrl,
            isDefault = entry.isDefault,
            recommended = entry.recommended,
            speakers = entry.speakers,
            sampleRate = entry.sampleRate,
            version = entry.version,
            modelType = entry.modelType,
            imported = entry.source == ModelStore.SOURCE_IMPORTED,
            builtin = builtin,
            state = state,
        )
    }

    /** Installed models a user installed (catalog or imported), without the bundled VAD. zh-CN: 用户安装的模型数, 不含内置 VAD. */
    fun installedCount(groups: List<ModelGroup>): Int = groups.sumOf { group -> group.rows.count { !it.builtin && it.state == RowState.Installed } }
}
