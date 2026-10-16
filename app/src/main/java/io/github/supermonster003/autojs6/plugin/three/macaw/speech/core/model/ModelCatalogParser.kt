package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

/**
 * Parses and validates catalog bytes with every rule of `distribution.validate_catalog` (Shared-Assets
 * `tools/speech-model-distribution`), so a catalog the maintainer's verifier refuses is refused here too: exact field
 * sets, identifiers, sorted unique models and versions, installed paths, upstream mappings covering exactly `files[]`,
 * runtime roles, content-addressed mirror names and URLs, release numbers and the recommended version. Rollback rules
 * between two catalogs live in [ModelCatalogRepository].
 * zh-CN: 以 `distribution.validate_catalog` (Shared-Assets `tools/speech-model-distribution`) 的全部规则解析并校验目录字节, 维护者校验器
 * 拒绝的目录这里同样拒绝: 精确的字段集合, 标识符, 有序且唯一的模型与版本, 安装路径, 恰好覆盖 `files[]` 的上游映射, 运行时角色, 内容寻址
 * 的镜像文件名与 URL, Release 编号与推荐版本. 两个目录之间的回退规则见 [ModelCatalogRepository].
 */
class ModelCatalogParser(private val expectedRepository: String? = CatalogRules.OFFICIAL_REPOSITORY) {

    @Throws(CatalogException::class)
    fun parse(bytes: ByteArray, minimumCatalogVersion: Int = 1): ModelCatalog {
        if (bytes.size > CatalogRules.MAX_CATALOG_BYTES) throw CatalogException("Catalog exceeds its size limit")
        val root = try {
            StrictJson.parseObject(bytes)
        } catch (e: StrictJsonException) {
            throw CatalogException("Invalid catalog JSON: ${e.message}", e)
        }
        return try {
            catalog(root, minimumCatalogVersion)
        } catch (e: InvalidCatalog) {
            throw CatalogException(e.message ?: "Invalid catalog", e)
        }
    }

    private class InvalidCatalog(message: String) : Exception(message)

    private fun fail(message: String): Nothing = throw InvalidCatalog(message)

    private fun catalog(root: Map<String, Any?>, minimumCatalogVersion: Int): ModelCatalog {
        fields(root, listOf("schemaVersion", "catalogVersion", "releaseTag", "generatedAt", "repository", "models"), name = "catalog")
        if (integerOrNull(root["schemaVersion"]) != 1L) fail("Unsupported catalog schemaVersion")
        if (minimumCatalogVersion < 1) fail("minimum catalog version must be at least 1")
        val version = integer(root["catalogVersion"], "catalogVersion", minimumCatalogVersion.toLong()).toInt()
        if (root["releaseTag"] != "speech-models-v$version") fail("releaseTag must match catalogVersion")
        val releaseTag = root["releaseTag"] as String
        val generatedAt = text(root["generatedAt"], "generatedAt", 64)
        val generated = timestamp(generatedAt, "generatedAt")
        val repository = root["repository"] as? String
        if (repository == null || !CatalogRules.REPOSITORY.matches(repository)) fail("repository must be a GitHub owner/repository name")
        if (expectedRepository != null && repository != expectedRepository) fail("Catalog repository differs from the expected repository")
        val modelValues = root["models"] as? List<*> ?: fail("models must be a bounded array")
        if (modelValues.size > CatalogRules.MAX_MODELS) fail("models must be a bounded array")
        val artifacts = HashSet<String>()
        val models = modelValues.map { model(it, repository, version, generated, artifacts) }
        val ids = models.map { it.modelId }
        if (ids != ids.distinct().sorted()) fail("Models must use ascending unique modelId order")
        return ModelCatalog(version, releaseTag, generatedAt, repository, models)
    }

    private fun model(value: Any?, repository: String, catalogVersion: Int, generated: Timestamp, artifacts: MutableSet<String>): CatalogModel {
        val model = fields(value, listOf("modelId", "kind", "displayName", "summary", "recommended", "versions", "recommendedVersionCode"), name = "model")
        val modelId = model["modelId"] as? String
        if (modelId == null || !CatalogRules.MODEL_ID.matches(modelId)) fail("Invalid modelId")
        val kind = model["kind"] as? String
        if (kind !in CatalogRules.KINDS || !modelId.startsWith("$kind.")) fail("modelId must start with its kind")
        val displayName = languageMap(model["displayName"], "displayName", 256)
        val summary = languageMap(model["summary"], "summary", 1024)
        val recommended = boolean(model["recommended"], "recommended")
        val versionValues = model["versions"] as? List<*> ?: fail("Model versions must be a nonempty bounded array")
        if (versionValues.isEmpty() || versionValues.size > CatalogRules.MAX_VERSIONS) fail("Model versions must be a nonempty bounded array")
        val versions = versionValues.map { item ->
            version(item, modelId, kind!!, repository, catalogVersion, generated).also { parsed ->
                parsed.mirror?.let { if (!artifacts.add(it.fileName)) fail("Duplicate mirror archive") }
            }
        }
        val codes = versions.map { it.versionCode }
        if (codes != codes.distinct().sorted()) fail("Versions must use ascending unique versionCode order")
        val recommendedValue = model["recommendedVersionCode"]
        val recommendedCode = if (recommendedValue == null) null else integer(recommendedValue, "recommendedVersionCode").toInt()
        if (recommendedCode != versions.filter { it.isActive }.maxOfOrNull { it.versionCode }) {
            fail("recommendedVersionCode must be the latest active version or null")
        }
        return CatalogModel(modelId, kind!!, displayName, summary, recommended, versions, recommendedCode)
    }

    private fun version(value: Any?, modelId: String, kind: String, repository: String, catalogVersion: Int, generated: Timestamp): CatalogModelVersion {
        val item = fields(
            value,
            listOf("versionCode", "modelType", "languages", "sampleRate", "releasedAt", "license", "runtime", "sourceUrl", "mirror", "upstream", "files"),
            listOf("speakers", "deprecated", "revoked"),
            name = "catalog version",
        )
        val code = integer(item["versionCode"], "versionCode").toInt()
        val files = files(item["files"])
        val filePaths = files.map { it.path }.toSet()
        val directories = directoriesOf(filePaths)
        val upstream = upstream(item["upstream"], filePaths)
        val modelType = item["modelType"] as? String
        val spec = CatalogRules.MODEL_TYPES[modelType]
        if (spec == null || spec.kind != kind) fail("modelType does not belong to the model kind")
        val languages = languages(item["languages"])
        val sampleRate = integer(item["sampleRate"], "sampleRate", 8000, 192000).toInt()
        val speakers = if (kind == "tts") {
            integer(item["speakers"], "speakers", 1, 100000).toInt()
        } else {
            if (item.containsKey("speakers")) fail("speakers is only valid for tts models")
            null
        }
        val license = license(item["license"])
        val (minSherpaOnnx, config) = runtime(item["runtime"], spec, filePaths, directories)
        val deprecated = item["deprecated"]?.let { boolean(it, "deprecated") } ?: false
        val revoked = item["revoked"]?.let { boolean(it, "revoked") } ?: false
        if (item.containsKey("deprecated") && item["deprecated"] == null) fail("deprecated must be a boolean")
        if (item.containsKey("revoked") && item["revoked"] == null) fail("revoked must be a boolean")
        val releasedAt = text(item["releasedAt"], "releasedAt", 64)
        if (timestamp(releasedAt, "releasedAt") > generated) fail("releasedAt cannot be later than the catalog generatedAt")

        val mirror = if (item["mirror"] == null) {
            // Upstream-only version (no archive published, files[] come from upstream[] alone): the source URL alone
            // names the release tag that introduced it.
            val sourceUrl = item["sourceUrl"] as? String ?: ""
            val prefix = "https://github.com/$repository/tree/"
            val tag = if (sourceUrl.startsWith(prefix)) sourceUrl.substring(prefix.length).substringBefore('/') else ""
            val release = CatalogRules.RELEASE_TAG.matchEntire(tag)
            if (release == null || sourceUrl != CatalogRules.sourceUrl(repository, tag, modelId, code)) {
                fail("sourceUrl of an upstream-only version must name its source directory at a release tag")
            }
            if (release.groupValues[1].toLong() > catalogVersion) fail("sourceUrl cannot use a later release than the catalog")
            null
        } else {
            val mirror = fields(item["mirror"], listOf("fileName", "format", "size", "sha256", "url"), name = "mirror")
            if (mirror["format"] != "tar") fail("mirror.format must be tar")
            val mirrorSize = integer(mirror["size"], "mirror size", 1, CatalogRules.MAX_ARCHIVE_BYTES)
            val mirrorSha = sha256(mirror["sha256"], "mirror sha256")
            if (mirrorSize != CatalogRules.archiveSize(files.map { it.size })) fail("mirror.size differs from the canonical size of its files")
            val expectedName = CatalogRules.archiveFileName(modelId, code, mirrorSha)
            if (mirror["fileName"] != expectedName) fail("mirror.fileName must be content addressed")
            val url = text(mirror["url"], "mirror url", 2048)
            val prefix = "https://github.com/$repository/releases/download/"
            val tag = if (url.startsWith(prefix)) url.substring(prefix.length).substringBefore('/') else ""
            val release = CatalogRules.RELEASE_TAG.matchEntire(tag)
            if (release == null || url != CatalogRules.releaseUrl(repository, tag, expectedName)) fail("mirror.url must name this repository's release asset")
            if (release.groupValues[1].toLong() > catalogVersion) fail("mirror.url cannot use a later release than the catalog")
            if (item["sourceUrl"] != CatalogRules.sourceUrl(repository, tag, modelId, code)) fail("sourceUrl must use the release tag that introduced the mirror archive")
            MirrorArchive(expectedName, mirrorSize, mirrorSha, url)
        }

        return CatalogModelVersion(
            versionCode = code,
            modelType = modelType!!,
            languages = languages,
            sampleRate = sampleRate,
            speakers = speakers,
            releasedAt = releasedAt,
            license = license,
            minSherpaOnnx = minSherpaOnnx,
            runtimeConfig = config,
            sourceUrl = item["sourceUrl"] as String,
            mirror = mirror,
            upstream = upstream,
            files = files,
            deprecated = deprecated,
            revoked = revoked,
        )
    }

    private fun files(value: Any?): List<CatalogFile> {
        val items = value as? List<*> ?: fail("files must be an array of 1..${CatalogRules.MAX_FILES} entries")
        if (items.isEmpty() || items.size > CatalogRules.MAX_FILES) fail("files must be an array of 1..${CatalogRules.MAX_FILES} entries")
        val result = ArrayList<CatalogFile>(items.size)
        val folded = HashSet<String>()
        val folders = HashSet<String>()
        var previous: String? = null
        var total = 0L
        for (raw in items) {
            val item = fields(raw, listOf("path", "size", "sha256"), name = "file")
            val path = installPath(item["path"], "path")
            val size = integer(item["size"], "file size", 0, CatalogRules.MAX_FILE_BYTES)
            val sha = sha256(item["sha256"], "file sha256")
            if (previous != null && path <= previous) fail("files must use ascending unique path order")
            previous = path
            val lower = path.lowercase(java.util.Locale.ROOT)
            if (!folded.add(lower)) fail("files contain paths that differ only by case")
            val segments = lower.split('/')
            for (index in 1 until segments.size) folders += segments.subList(0, index).joinToString("/")
            total += size
            result += CatalogFile(path, size, sha)
        }
        if (folded.any { it in folders }) fail("A file path is also used as a directory")
        if (total > CatalogRules.MAX_INSTALL_BYTES) fail("Installed files exceed the size limit")
        return result
    }

    private fun upstream(value: Any?, filePaths: Set<String>): List<UpstreamPart> {
        val parts = value as? List<*> ?: fail("upstream must be an array of 1..8 release assets")
        if (parts.isEmpty() || parts.size > CatalogRules.MAX_UPSTREAM_PARTS) fail("upstream must be an array of 1..8 release assets")
        val targets = HashSet<String>()
        val assets = HashSet<String>()
        val result = parts.map { raw ->
            val format = (raw as? Map<*, *>)?.get("format")
            if (format != "tar.bz2" && format != "file") fail("upstream format must be tar.bz2 or file")
            val archive = format == "tar.bz2"
            val part = fields(raw, listOf("url", "size", "sha256", "format", if (archive) "members" else "path"), name = "upstream asset")
            val url = text(part["url"], "upstream url", 2048)
            val (tag, asset) = upstreamUrl(url)
            if (!assets.add("$tag/$asset")) fail("Duplicate upstream release asset")
            val size = integer(part["size"], "upstream size", 1, CatalogRules.MAX_UPSTREAM_BYTES)
            val sha = sha256(part["sha256"], "upstream sha256")
            if (archive) {
                if (!asset.endsWith(".tar.bz2")) fail("tar.bz2 upstream assets must use the .tar.bz2 extension")
                val members = part["members"] as? Map<*, *> ?: fail("upstream members must map 1..4096 archive members")
                if (members.isEmpty() || members.size > CatalogRules.MAX_FILES) fail("upstream members must map 1..4096 archive members")
                val mapped = LinkedHashMap<String, String>()
                members.forEach { (member, target) ->
                    val memberPath = installPath(member, "upstream member")
                    val targetPath = installPath(target, "path")
                    if (!targets.add(targetPath)) fail("Two upstream members install the same path: $targetPath")
                    mapped[memberPath] = targetPath
                }
                UpstreamPart.TarBz2(url, size, sha, mapped)
            } else {
                val path = installPath(part["path"], "path")
                if (!targets.add(path)) fail("Two upstream members install the same path: $path")
                UpstreamPart.RawFile(url, size, sha, path)
            }
        }
        if (targets != filePaths) fail("Upstream members must cover exactly the files array")
        return result
    }

    private fun upstreamUrl(url: String): Pair<String, String> {
        val prefix = "https://github.com/${CatalogRules.UPSTREAM_REPOSITORY}/releases/download/"
        val parts = if (url.startsWith(prefix) && '?' !in url && '#' !in url) url.substring(prefix.length).split('/') else emptyList()
        if (parts.size != 2 || !CatalogRules.UPSTREAM_TAG.matches(parts[0]) || !CatalogRules.UPSTREAM_ASSET.matches(parts[1])) {
            fail("Upstream URLs must name a k2-fsa/sherpa-onnx GitHub release asset")
        }
        return parts[0] to parts[1]
    }

    private fun license(value: Any?): ModelLicense {
        val license = fields(value, listOf("spdx", "commercialUse", "noticeKey"), name = "license")
        val spdx = license["spdx"] as? String
        if (spdx == null || spdx.length > 256 || !CatalogRules.SPDX.matches(spdx)) {
            fail("license.spdx must be an SPDX expression of license IDs or LicenseRef identifiers")
        }
        val commercialUse = when (license["commercialUse"]) {
            true -> CommercialUse.ALLOWED
            false -> CommercialUse.NOT_ALLOWED
            "unknown" -> CommercialUse.UNKNOWN
            else -> fail("license.commercialUse must be true, false or \"unknown\"")
        }
        val noticeKey = license["noticeKey"] as? String
        if (noticeKey == null || !CatalogRules.NOTICE_KEY.matches(noticeKey)) fail("license.noticeKey must match speech_model_license_*")
        return ModelLicense(spdx, commercialUse, noticeKey)
    }

    private fun runtime(value: Any?, spec: CatalogRules.ModelTypeSpec, filePaths: Set<String>, directories: Set<String>): Pair<String, Map<String, Any>> {
        val runtime = fields(value, listOf("minSherpaOnnx", "config"), name = "runtime")
        val minimum = runtime["minSherpaOnnx"] as? String
        if (minimum == null || !CatalogRules.SEMVER.matches(minimum)) fail("runtime.minSherpaOnnx must be a plain semantic version")
        val config = fields(runtime["config"], spec.required, spec.optional, name = "runtime.config")
        val result = LinkedHashMap<String, Any>()
        config.forEach { (role, item) ->
            when (role) {
                in CatalogRules.VALUE_ROLES -> {
                    if (item !in CatalogRules.VALUE_ROLES.getValue(role)) fail("runtime.config.$role has an unsupported value")
                    result[role] = item as String
                }
                in CatalogRules.LIST_ROLES -> {
                    val list = item as? List<*>
                    if (list == null || list.isEmpty() || list.size > 16 || list.distinct().size != list.size || list.any { it !is String || it !in filePaths }) {
                        fail("runtime.config.$role must list distinct installed files")
                    }
                    result[role] = list.map { it as String }
                }
                in CatalogRules.DIRECTORY_ROLES -> {
                    if (item !is String || item !in directories) fail("runtime.config.$role must name an installed directory")
                    result[role] = item
                }
                else -> {
                    if (item !is String || item !in filePaths) fail("runtime.config.$role must name an installed file")
                    result[role] = item
                }
            }
        }
        return minimum to result
    }

    // Scalars ---------------------------------------------------------------------------------------------------------

    private fun fields(value: Any?, required: List<String>, optional: List<String> = emptyList(), name: String): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        val map = value as? Map<String, Any?> ?: fail("$name must be an object")
        val missing = required.filter { it !in map }
        val unknown = map.keys.filter { it !in required && it !in optional }
        if (missing.isNotEmpty() || unknown.isNotEmpty()) fail("$name has missing fields ${missing.sorted()} or unknown fields ${unknown.sorted()}")
        return map
    }

    private fun integerOrNull(value: Any?): Long? = (value as? JsonNumber)?.longOrNull()

    private fun integer(value: Any?, name: String, minimum: Long = 1, maximum: Long = Int.MAX_VALUE.toLong()): Long {
        val number = integerOrNull(value)
        if (number == null || number < minimum || number > maximum) fail("$name must be an integer in $minimum..$maximum")
        return number
    }

    private fun boolean(value: Any?, name: String): Boolean = value as? Boolean ?: fail("$name must be a boolean")

    private fun text(value: Any?, name: String, maximum: Int = 1024): String {
        val string = value as? String ?: fail("Invalid bounded text: $name")
        val valid = string.isNotEmpty() && !string.first().isWhitespace() && !string.last().isWhitespace() &&
            string.codePointCount(0, string.length) <= maximum && string.none { it.code < 32 || it.code in 127..159 }
        if (!valid) fail("Invalid bounded text: $name")
        return string
    }

    private fun sha256(value: Any?, name: String): String {
        val text = value as? String
        if (text == null || !CatalogRules.SHA256.matches(text)) fail("$name must be a lowercase SHA-256 digest")
        return text
    }

    private fun installPath(value: Any?, name: String): String {
        val path = value as? String
        if (path == null || !CatalogRules.isInstallPath(path)) fail("Invalid $name: $value")
        return path
    }

    private fun languageMap(value: Any?, name: String, maximum: Int): Map<String, String> {
        @Suppress("UNCHECKED_CAST")
        val map = value as? Map<String, Any?>
        if (map == null || !map.keys.containsAll(listOf("en", "zh-CN")) || map.size > 64) fail("$name must contain at least en and zh-CN texts")
        return map.mapValues { (language, content) ->
            if (!CatalogRules.MAP_LANGUAGE.matches(language)) fail("Invalid language tag in $name")
            text(content, name, maximum)
        }
    }

    private fun languages(value: Any?): List<String> {
        val list = value as? List<*>
        if (list == null || list.isEmpty() || list.size > 32 || list.map { it.toString() }.distinct().size != list.size) {
            fail("languages must be a nonempty array of distinct tags")
        }
        return list.map { item ->
            if (item !is String || !CatalogRules.LANGUAGE.matches(item)) fail("Invalid recognition or synthesis language tag")
            item
        }
    }

    private fun timestamp(value: String, name: String): Timestamp = Timestamp.parse(value) ?: fail("Invalid calendar timestamp: $name")

    companion object {

        /** Directory prefixes of installed paths (`validate_files`), the valid targets of `dataDir`. zh-CN: 安装路径的目录前缀. */
        fun directoriesOf(paths: Collection<String>): Set<String> = paths.flatMapTo(HashSet()) { path ->
            val segments = path.split('/')
            (1 until segments.size).map { segments.subList(0, it).joinToString("/") }
        }
    }
}

/**
 * An RFC 3339 timestamp of the catalog (`distribution.timestamp`): calendar date, time, optional fraction and an offset
 * up to 18 hours, ordered by the instant it names. No java.time, which needs API 26.
 * zh-CN: 目录中的 RFC 3339 时间戳 (`distribution.timestamp`): 日期, 时间, 可选小数秒与不超过 18 小时的时差, 按所指时刻排序. 不使用需要
 * API 26 的 java.time.
 */
internal data class Timestamp(val epochSeconds: Long, val nanos: Int) : Comparable<Timestamp> {

    override fun compareTo(other: Timestamp): Int = compareValuesBy(this, other, { it.epochSeconds }, { it.nanos })

    companion object {
        private val PATTERN = Regex(
            "([0-9]{4})-([0-9]{2})-([0-9]{2})T([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])(?:\\.([0-9]{1,9}))?(Z|([+-])(0[0-9]|1[0-8]):([0-5][0-9]))",
        )

        fun parse(value: String): Timestamp? {
            val match = PATTERN.matchEntire(value) ?: return null
            val g = match.groupValues
            val year = g[1].toInt()
            val month = g[2].toInt()
            val day = g[3].toInt()
            if (year < 1 || month !in 1..12 || day < 1 || day > daysInMonth(year, month)) return null
            val offset = if (g[8] == "Z") 0 else {
                val minutes = g[10].toInt() * 60 + g[11].toInt()
                if (minutes > 18 * 60) return null
                if (g[9] == "-") -minutes else minutes
            }
            val seconds = daysFromCivil(year, month, day) * 86400L + g[4].toInt() * 3600L + g[5].toInt() * 60L + g[6].toInt() - offset * 60L
            return Timestamp(seconds, g[7].padEnd(9, '0').ifEmpty { "0" }.toInt())
        }

        private fun daysInMonth(year: Int, month: Int): Int = when (month) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        /** Days since 1970-01-01 in the proleptic Gregorian calendar (Howard Hinnant's algorithm). */
        private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
            val y = if (month <= 2) year - 1 else year
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val doy = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146097L + doe - 719468
        }
    }
}
