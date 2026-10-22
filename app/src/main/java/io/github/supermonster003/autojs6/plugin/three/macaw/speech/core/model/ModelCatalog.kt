package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import java.util.Locale

/**
 * `license.commercialUse` of a catalog version: `true`, `false` or `"unknown"` (roadmap D11).
 * zh-CN: 目录版本的 `license.commercialUse`: `true`, `false` 或 `"unknown"` (路线图 D11).
 */
enum class CommercialUse(val json: Any) {
    ALLOWED(true),
    NOT_ALLOWED(false),
    UNKNOWN("unknown"),
}

/** License summary of one model version. zh-CN: 单个模型版本的许可摘要. */
data class ModelLicense(val spdx: String, val commercialUse: CommercialUse, val noticeKey: String) {

    /** Roadmap D11: non-commercial and unknown licenses need an explicit acceptance. zh-CN: 路线图 D11: 非商用与未知许可需显式接受. */
    val requiresAcceptance: Boolean get() = commercialUse != CommercialUse.ALLOWED
}

/** One installed file of a version, the final verification basis (roadmap D24). zh-CN: 版本的单个安装文件, 最终校验依据 (路线图 D24). */
data class CatalogFile(val path: String, val size: Long, val sha256: String)

/** A downloadable artifact with its pinned size and SHA-256. zh-CN: 带固定大小与 SHA-256 的可下载资源. */
interface PinnedArtifact {
    val url: String
    val size: Long
    val sha256: String
}

/** The content-addressed Shared-Assets Release archive (canonical uncompressed USTAR). zh-CN: 内容寻址的 Shared-Assets Release 归档. */
data class MirrorArchive(val fileName: String, override val size: Long, override val sha256: String, override val url: String) : PinnedArtifact

/** One sherpa-onnx release asset of the fallback path. zh-CN: 回退路径中的一个 sherpa-onnx Release 资源. */
sealed class UpstreamPart : PinnedArtifact {

    /** A `tar.bz2` whose [members] (archive member to installed path) are installed. zh-CN: 按 [members] 安装成员的 `tar.bz2`. */
    data class TarBz2(override val url: String, override val size: Long, override val sha256: String, val members: Map<String, String>) : UpstreamPart()

    /** A single file installed as [path]. zh-CN: 作为 [path] 安装的单个文件. */
    data class RawFile(override val url: String, override val size: Long, override val sha256: String, val path: String) : UpstreamPart()
}

/**
 * One immutable `(modelId, versionCode)` of the catalog. [runtimeConfig] maps the sherpa-onnx roles to installed paths
 * (a String, or a List for `lexicon`, `ruleFsts` and `ruleFars`).
 * zh-CN: 目录中不可变的 `(modelId, versionCode)`. [runtimeConfig] 把 sherpa-onnx 角色映射到安装路径 (字符串, `lexicon`, `ruleFsts` 与
 * `ruleFars` 为列表).
 */
data class CatalogModelVersion(
    val versionCode: Int,
    val modelType: String,
    val languages: List<String>,
    /** Null for punctuation models, which work on text (schema 2). zh-CN: 标点模型处理文本, 为 null (schema 2). */
    val sampleRate: Int?,
    val speakers: Int?,
    val releasedAt: String,
    val license: ModelLicense,
    val minSherpaOnnx: String,
    val runtimeConfig: Map<String, Any>,
    val sourceUrl: String,
    /** Null for an upstream-only version (no Shared-Assets archive). zh-CN: 仅上游版本 (没有 Shared-Assets 归档) 时为 null. */
    val mirror: MirrorArchive?,
    val upstream: List<UpstreamPart>,
    val files: List<CatalogFile>,
    val deprecated: Boolean,
    val revoked: Boolean,
) {
    val installBytes: Long get() = files.sumOf { it.size }
    val upstreamBytes: Long get() = upstream.sumOf { it.size }

    /** Bytes a download normally transfers: the mirror archive, or the upstream assets without one. zh-CN: 常规下载的字节数. */
    val downloadBytes: Long get() = mirror?.size ?: upstreamBytes
    val isActive: Boolean get() = !deprecated && !revoked
}

/** One catalog model with its versions. zh-CN: 目录中的单个模型及其版本. */
data class CatalogModel(
    val modelId: String,
    val kind: String,
    val displayName: Map<String, String>,
    val summary: Map<String, String>,
    val recommended: Boolean,
    val versions: List<CatalogModelVersion>,
    /**
     * The latest active version this build knows: the catalog's `recommendedVersionCode`, unless roadmap D106 skipped
     * that version.
     * zh-CN: 本构建认识的最新可用版本: 即目录的 `recommendedVersionCode`, 除非该版本被路线图 D106 跳过.
     */
    val recommendedVersionCode: Int?,
) {
    /** The version new installs use, null when every version is deprecated or revoked. zh-CN: 新安装使用的版本. */
    val installableVersion: CatalogModelVersion? get() = versions.firstOrNull { it.versionCode == recommendedVersionCode }

    /** The newest version for display, even when it may no longer be installed. zh-CN: 用于展示的最新版本. */
    val latestVersion: CatalogModelVersion get() = versions.last()

    fun version(code: Int): CatalogModelVersion? = versions.firstOrNull { it.versionCode == code }

    /** [displayName] in [locale], falling back to the same language, then `en`. zh-CN: 按 [locale] 取显示名, 回退到同语言再到 `en`. */
    fun localizedName(locale: Locale): String = localized(displayName, locale)

    fun localizedSummary(locale: Locale): String = localized(summary, locale)
}

/**
 * A validated catalog (wire schema 1 or 2, [schemaVersion]) and the queries of `listModels` and the default model
 * selection; [models] leaves out what roadmap D106 skipped.
 * zh-CN: 已校验的目录 (传输格式 1 或 2, 见 [schemaVersion]), 以及 `listModels` 与默认模型选择所需的查询; [models] 不含按路线图 D106 跳过的条目.
 */
data class ModelCatalog(
    val schemaVersion: Int,
    val catalogVersion: Int,
    val releaseTag: String,
    val generatedAt: String,
    val repository: String,
    val models: List<CatalogModel>,
) {
    fun model(modelId: String): CatalogModel? = models.firstOrNull { it.modelId == modelId }

    /** Models of [kind] (all kinds when null) that handle [language] (any when null). zh-CN: 按种类与语言筛选模型. */
    fun query(kind: String? = null, language: String? = null): List<CatalogModel> = models.filter { model ->
        (kind == null || model.kind == kind) &&
            (language == null || model.latestVersion.languages.any { languageMatches(it, language) })
    }

    /**
     * The model a kind falls back to when nothing is chosen: recommended and installable first, then the first
     * installable one in catalog order.
     * zh-CN: 未选择时某种类回退到的模型: 先取推荐且可安装的, 再按目录顺序取第一个可安装的.
     */
    fun recommendedModel(kind: String, language: String? = null): CatalogModel? {
        val candidates = query(kind, language).filter { it.installableVersion != null }
        return candidates.firstOrNull { it.recommended } ?: candidates.firstOrNull()
    }

    companion object {

        /** `zh` matches `zh`, `zh-CN` and `zh-Hans`; `zh-CN` matches `zh`. zh-CN: 语言按主子标签匹配. */
        fun languageMatches(modelLanguage: String, wanted: String): Boolean {
            val left = modelLanguage.lowercase(Locale.ROOT)
            val right = wanted.lowercase(Locale.ROOT)
            return left == right || left.substringBefore('-') == right.substringBefore('-')
        }
    }
}

internal fun localized(texts: Map<String, String>, locale: Locale): String {
    val tag = locale.toLanguageTag()
    texts[tag]?.let { return it }
    val language = locale.language
    if (language == "zh") {
        // Traditional Chinese has no separate catalog text yet; zh-CN is still closer than English.
        // zh-CN: 目录尚无繁体文本时, zh-CN 仍比英文更接近.
        texts["zh-CN"]?.let { return it }
    }
    texts.entries.firstOrNull { it.key.substringBefore('-').equals(language, ignoreCase = true) }?.let { return it.value }
    return texts["en"] ?: texts.values.first()
}
