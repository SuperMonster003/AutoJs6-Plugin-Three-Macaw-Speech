package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

/**
 * The value rules of the Shared-Assets `speech-models` family (`tools/speech-model-distribution/distribution.py`):
 * identifiers, installed paths, the canonical USTAR layout of mirror archives and the limits, mirrored one to one so
 * the plugin accepts exactly the catalogs and archives the maintainer's tools produce.
 * zh-CN: Shared-Assets `speech-models` 类别的取值规则 (`tools/speech-model-distribution/distribution.py`): 标识符, 安装路径, 镜像归档的
 * 规范 USTAR 布局与各项上限, 逐条对应, 使插件恰好接受维护者工具产出的目录与归档.
 */
object CatalogRules {

    /**
     * Wire schemas the plugin reads (`distribution.CATALOG_SCHEMAS`). Each schema has its own file name, and the plugin
     * fetches the newest one: schema 2 adds the `punct` kind with its model types and upstream members behind `./`
     * (roadmap D106), while `catalog-v1.json` stays frozen for plugins before 1.2.0. Schema 3 adds the `speaker` kind
     * (roadmap P8.3, D127), so `catalog-v2.json` keeps serving 1.2.0 without it.
     * zh-CN: 插件可读的传输格式 (`distribution.CATALOG_SCHEMAS`). 每个格式有自己的文件名, 插件读取最新的一个: schema 2 增加 `punct` 类别及其
     * 模型类型, 以及带 `./` 的上游成员 (路线图 D106); `catalog-v1.json` 冻结, 供 1.2.0 之前的插件使用. schema 3 增加 `speaker` 类别 (路线图 P8.3,
     * D127), `catalog-v2.json` 因此继续为不含它的 1.2.0 服务.
     */
    val SCHEMAS = listOf(1, 2, 3)
    const val SCHEMA_VERSION = 3
    const val CATALOG_FILE = "catalog-v$SCHEMA_VERSION.json"
    const val SIGNATURE_FILE = "catalog-v$SCHEMA_VERSION.sig.json"

    /**
     * Schema of the bundled snapshot: the signed `catalog-v2.json` of release 2 until the maintainer signs release 3,
     * which replaces it in roadmap P8.5.2 (D108, D127); the plugin reads it as the first catalog and refreshes to
     * [CATALOG_FILE].
     * zh-CN: 内置快照的 schema: 维护者签名发布 3 之前为发布 2 的已签名 `catalog-v2.json`, 在路线图 P8.5.2 替换 (D108, D127); 插件把它当作首个目录读取,
     * 刷新时取 [CATALOG_FILE].
     */
    const val BUNDLED_SCHEMA = 2
    const val BUNDLED_CATALOG_FILE = "catalog-v$BUNDLED_SCHEMA.json"
    const val BUNDLED_SIGNATURE_FILE = "catalog-v$BUNDLED_SCHEMA.sig.json"
    const val OFFICIAL_REPOSITORY = "SuperMonster003/AutoJs6-Shared-Assets"
    const val FAMILY_DIRECTORY = "speech-models"
    const val UPSTREAM_REPOSITORY = "k2-fsa/sherpa-onnx"

    const val MAX_CATALOG_BYTES = 8L * 1024 * 1024
    const val MAX_SIGNATURE_BYTES = 64L * 1024
    const val MAX_ARCHIVE_BYTES = Int.MAX_VALUE.toLong()
    const val MAX_FILE_BYTES = Int.MAX_VALUE.toLong()
    const val MAX_UPSTREAM_BYTES = Int.MAX_VALUE.toLong()
    const val MAX_INSTALL_BYTES = 4L * 1024 * 1024 * 1024
    const val MAX_FILES = 4096
    const val MAX_MODELS = 1024
    const val MAX_VERSIONS = 1024
    const val MAX_UPSTREAM_PARTS = 8
    const val MAX_PATH_LENGTH = 240
    const val MAX_PATH_DEPTH = 12
    const val BLOCK = 512

    val REPOSITORY = Regex("[A-Za-z0-9][A-Za-z0-9_.-]*/[A-Za-z0-9][A-Za-z0-9_.-]*")
    val RELEASE_TAG = Regex("speech-models-v([1-9][0-9]{0,9})")
    val MODEL_ID = Regex("(asr|kws|punct|speaker|tts)\\.[a-z0-9][a-z0-9._-]{1,59}")

    /**
     * The shape of a model of a kind this build does not know (roadmap D106): such models are skipped, not refused.
     * zh-CN: 本构建不认识的类别的模型形式 (路线图 D106): 这类模型被跳过, 而不是整份拒绝.
     */
    val FUTURE_MODEL_ID = Regex("([a-z][a-z0-9]{1,15})\\.[a-z0-9][a-z0-9._-]{1,59}")
    val FUTURE_MODEL_TYPE = Regex("[a-z0-9][a-z0-9-]{0,63}")
    val SHA256 = Regex("[0-9a-f]{64}")
    val LANGUAGE = Regex("[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*")
    val MAP_LANGUAGE = Regex("[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*")
    val NOTICE_KEY = Regex("speech_model_license_[a-z0-9_]{1,40}")
    val SPDX = Regex("[A-Za-z0-9.+-]+(?: (?:AND|OR|WITH) [A-Za-z0-9.+-]+)*")
    val SEMVER = Regex("(?:0|[1-9][0-9]{0,3})\\.(?:0|[1-9][0-9]{0,3})\\.(?:0|[1-9][0-9]{0,3})")
    val PATH_SEGMENT = Regex("[A-Za-z0-9_+!](?:[A-Za-z0-9 _.+!-]{0,126}[A-Za-z0-9_+!-])?")
    val UPSTREAM_TAG = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
    val UPSTREAM_ASSET = Regex("[A-Za-z0-9][A-Za-z0-9._+-]{0,254}")

    val KINDS = listOf("asr", "kws", "punct", "speaker", "tts")

    /**
     * Kinds a user may import as a custom model (no catalog runtime roles for punctuation; speaker models need the
     * measured threshold of roadmap P8.3.1). zh-CN: 可作为自定义模型导入的类别 (标点没有目录运行时角色; 说话人模型需要路线图 P8.3.1 实测的阈值).
     */
    val CUSTOM_IMPORT_KINDS = listOf("asr", "kws", "tts")

    /** Punctuation models work on text and carry no `sampleRate`. zh-CN: 标点模型处理文本, 不带 `sampleRate`. */
    const val KIND_PUNCT = "punct"

    /** Speaker embedding models (roadmap P8.3, schema 3). zh-CN: 说话人嵌入模型 (路线图 P8.3, schema 3). */
    const val KIND_SPEAKER = "speaker"

    /**
     * sherpa-onnx 1.13.8 configuration family of every model type, with its required and optional file roles; [values]
     * replaces [VALUE_ROLES] for a type whose value roles take other values (`distribution.MODEL_TYPES` "values").
     * zh-CN: 每种模型类型对应的 sherpa-onnx 1.13.8 配置族, 以及必需与可选的文件角色; 值角色取其他值的类型以 [values] 代替 [VALUE_ROLES]
     * (`distribution.MODEL_TYPES` 的 "values").
     */
    class ModelTypeSpec(val kind: String, val required: List<String>, val optional: List<String>, val values: Map<String, Set<String>>? = null)

    private val TTS_OPTIONAL = listOf("lexicon", "ruleFsts", "ruleFars", "dataDir")
    val MODEL_TYPES: Map<String, ModelTypeSpec> = linkedMapOf(
        "online-transducer" to ModelTypeSpec("asr", listOf("encoder", "decoder", "joiner", "tokens", "modelType"), listOf("bpeVocab")),
        "sense-voice" to ModelTypeSpec("asr", listOf("model", "tokens"), emptyList()),
        // sherpa-onnx OfflineTransducerModelConfig; NeMo transducers since catalog v3 (roadmap D131).
        "offline-transducer" to ModelTypeSpec(
            "asr", listOf("encoder", "decoder", "joiner", "tokens", "modelType"), emptyList(), mapOf("modelType" to setOf(NEMO_TRANSDUCER)),
        ),
        "kws-transducer" to ModelTypeSpec("kws", listOf("encoder", "decoder", "joiner", "tokens", "modelType"), listOf("lexicon")),
        "vits" to ModelTypeSpec("tts", listOf("model", "tokens"), TTS_OPTIONAL),
        "matcha" to ModelTypeSpec("tts", listOf("acousticModel", "vocoder", "tokens"), TTS_OPTIONAL),
        "kokoro" to ModelTypeSpec("tts", listOf("model", "voices", "tokens"), TTS_OPTIONAL),
        // sherpa-onnx OfflinePunctuation (ctTransformer) and OnlinePunctuation (cnnBilstm with its bpeVocab).
        "ct-transformer" to ModelTypeSpec("punct", listOf("model"), emptyList()),
        "cnn-bilstm" to ModelTypeSpec("punct", listOf("model", "bpeVocab"), emptyList()),
        // sherpa-onnx SpeakerEmbeddingExtractor (3D-Speaker, WeSpeaker, NeMo ONNX exports with metadata), roadmap P8.3.
        "speaker-embedding" to ModelTypeSpec("speaker", listOf("model"), emptyList()),
    )
    val LIST_ROLES = setOf("lexicon", "ruleFsts", "ruleFars")
    val DIRECTORY_ROLES = setOf("dataDir")
    val VALUE_ROLES = mapOf("modelType" to setOf("zipformer", "zipformer2"))

    /** The `modelType` value of `offline-transducer` and the sherpa-onnx model type it selects. zh-CN: `offline-transducer` 的 `modelType` 值. */
    const val NEMO_TRANSDUCER = "nemo_transducer"

    /** An upstream archive member may start with this prefix from schema 2 on (archives packed from "."). zh-CN: 上游成员前缀. */
    const val MEMBER_PREFIX = "./"

    /**
     * The first schema that may use a kind, model type or the member prefix (`distribution.SCHEMA_INTRODUCED`); everything
     * else exists since schema 1.
     * zh-CN: 可以使用某个类别, 模型类型或成员前缀的最早 schema (`distribution.SCHEMA_INTRODUCED`); 其余自 schema 1 起即有.
     */
    val SCHEMA_INTRODUCED = mapOf(
        "punct" to 2, "ct-transformer" to 2, "cnn-bilstm" to 2, "offline-transducer" to 2, MEMBER_PREFIX to 2,
        "speaker" to 3, "speaker-embedding" to 3,
    )

    /** `distribution.catalog_file`: the catalog file of a schema, e.g. `catalog-v1.json`. zh-CN: 某个 schema 的目录文件名. */
    fun catalogFile(schema: Int): String = "catalog-v$schema.json"

    fun signatureFile(schema: Int): String = "catalog-v$schema.sig.json"

    /** `install_path`: bounded ASCII, at most 12 segments of the segment pattern, storable in a USTAR header. zh-CN: 安装路径规则. */
    fun isInstallPath(value: String): Boolean {
        if (value.isEmpty() || value.length > MAX_PATH_LENGTH || value.any { it.code > 0x7F }) return false
        val segments = value.split('/')
        if (segments.size > MAX_PATH_DEPTH) return false
        if (segments.any { it == "." || it == ".." || !PATH_SEGMENT.matches(it) }) return false
        return ustarSplit(value) != null
    }

    /** USTAR (name, prefix) of a path, or null when it cannot be stored. zh-CN: 路径在 USTAR 头中的 (name, prefix); 无法存放时为 null. */
    fun ustarSplit(path: String): Pair<String, String>? {
        if (path.length <= 100) return path to ""
        for (index in path.indices) {
            if (path[index] == '/' && path.length - index - 1 <= 100) {
                return if (index in 1..155) path.substring(index + 1) to path.substring(0, index) else null
            }
        }
        return null
    }

    fun padded(size: Long): Long = (size + BLOCK - 1) / BLOCK * BLOCK

    /** Size of the canonical mirror archive of [sizes]. zh-CN: 由文件大小算出的规范镜像归档大小. */
    fun archiveSize(sizes: Iterable<Long>): Long = sizes.sumOf { BLOCK + padded(it) } + 2L * BLOCK

    fun archiveFileName(modelId: String, versionCode: Int, sha256: String): String =
        "autojs6-speech-$modelId-v$versionCode-${sha256.take(8)}.tar"

    fun releaseUrl(repository: String, releaseTag: String, fileName: String): String =
        "https://github.com/$repository/releases/download/$releaseTag/$fileName"

    fun sourceUrl(repository: String, releaseTag: String, modelId: String, versionCode: Int): String =
        "https://github.com/$repository/tree/$releaseTag/$FAMILY_DIRECTORY/sources/$modelId/$versionCode"

    /**
     * The one canonical USTAR header of a mirror member: mode 0644, uid / gid 0, mtime 0, empty owner names
     * (`distribution.tar_header`).
     * zh-CN: 镜像成员唯一的规范 USTAR 头: 权限 0644, uid / gid 0, mtime 0, 属主名为空 (`distribution.tar_header`).
     */
    fun canonicalTarHeader(path: String, size: Long): ByteArray {
        require(size in 0..MAX_FILE_BYTES) { "archive member size out of range" }
        require(isInstallPath(path)) { "invalid archive member path" }
        val (name, prefix) = ustarSplit(path)!!
        val header = ByteArray(BLOCK)
        fun put(offset: Int, text: String) = text.toByteArray(Charsets.US_ASCII).copyInto(header, offset)
        fun octal(offset: Int, width: Int, value: Long) {
            val text = java.lang.Long.toOctalString(value).padStart(width - 1, '0')
            require(text.length == width - 1) { "value does not fit in a USTAR field" }
            put(offset, text)
            header[offset + width - 1] = 0
        }
        put(0, name)
        octal(100, 8, 420)
        octal(108, 8, 0)
        octal(116, 8, 0)
        octal(124, 12, size)
        octal(136, 12, 0)
        put(148, "        ")
        put(156, "0")
        put(257, "ustar")
        header[262] = 0
        put(263, "00")
        octal(329, 8, 0)
        octal(337, 8, 0)
        put(345, prefix)
        val checksum = header.sumOf { it.toInt() and 0xFF }
        put(148, java.lang.Integer.toOctalString(checksum).padStart(6, '0'))
        header[154] = 0
        header[155] = ' '.code.toByte()
        return header
    }
}
