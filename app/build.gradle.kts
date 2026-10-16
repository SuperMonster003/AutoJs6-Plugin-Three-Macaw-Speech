import com.android.build.api.variant.FilterConfiguration
import com.android.build.api.variant.VariantOutputConfiguration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins {
    id("io.github.supermonster003.autojs6-native-alignment")
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"

// Minimum host build (roadmap D28): the P1.3 temporary value 5451, the host build that ships the speech module (VERSION_BUILD
// 3798 + 1 + 1652, equal to SpeechPluginIds.REQUIRED_HOST_VERSION_CODE of speech-api.aar); P6.4 writes the final one. Keep it in
// sync with ThreeMacawSpeechContract.REQUIRED_HOST_VERSION and .readme/common.json.
// zh-CN: 最低宿主构建号 (路线图 D28): P1.3 的临时值 5451, 即提供 speech 模块的宿主构建 (VERSION_BUILD 3798 + 1 + 1652, 与 speech-api.aar
// 的 SpeechPluginIds.REQUIRED_HOST_VERSION_CODE 相同); P6.4 回填最终值. 与 ThreeMacawSpeechContract.REQUIRED_HOST_VERSION 及
// .readme/common.json 保持一致.
val requiredHostVersion = 5451

// The four ABIs of the sherpa-onnx AAR (roadmap D14), in the order PluginInfo.supportedAbis reports them.
// zh-CN: sherpa-onnx AAR 的四个 ABI (路线图 D14), 顺序与 PluginInfo.supportedAbis 一致.
val pluginAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

// libsherpa-onnx-jni.so needs only libonnxruntime.so in every ABI (docs/development/native-abi-report.md); the C and C++ API
// libraries of the AAR serve other languages and are left out of the APK.
// zh-CN: 各 ABI 的 libsherpa-onnx-jni.so 只依赖 libonnxruntime.so (docs/development/native-abi-report.md); AAR 中的 C 与 C++ API
// 库供其他语言使用, 不打入 APK.
val excludedSherpaLibraries = listOf("libsherpa-onnx-c-api.so", "libsherpa-onnx-cxx-api.so")
val packagedNativeLibraries = listOf("libsherpa-onnx-jni.so", "libonnxruntime.so", "libandroidx.graphics.path.so")

val buildTypeDebug = "debug"
val buildTypeRelease = "release"

// `-PmacawReleaseProbe=true` gives the device test APK the platform-only SherpaReleaseProbeInstrumentation runner, which
// drives an installed minified release build (roadmap P0.2, `.python/device_release_probe.py`).
// zh-CN: `-PmacawReleaseProbe=true` 让设备测试 APK 改用纯平台的 SherpaReleaseProbeInstrumentation, 以驱动已安装的 R8 release 构建
// (路线图 P0.2, `.python/device_release_probe.py`).
val useReleaseProbe = when (val value = providers.gradleProperty("macawReleaseProbe").orNull) {
    null, "false" -> false
    "true" -> true
    else -> error("macawReleaseProbe must be either true or false, not $value")
}

fun File.sha256(): String = inputStream().buffered().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

fun readLock(path: String): Map<String, String> {
    val lockFile = rootProject.file(path)
    check(lockFile.isFile) { "Missing $path" }
    val entries = LinkedHashMap<String, String>()
    lockFile.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { line ->
        val key = line.substringBefore('=').trim()
        check(line.contains('=') && entries.put(key, line.substringAfter('=').trim()) == null) { "Malformed or duplicate entry in $path: $line" }
    }
    check(entries["format"] == "1") { "Unsupported lock format in $path" }
    return entries
}

fun verifiedFile(path: String, expected: String?, label: String): File {
    val file = rootProject.file(path)
    check(file.isFile) { "Missing $path ($label)" }
    check(expected != null && expected.matches(Regex("[0-9a-f]{64}"))) { "Invalid SHA-256 for $label" }
    val actual = file.sha256()
    check(actual == expected) { "SHA-256 mismatch for $path: expected $expected, actual $actual" }
    return file
}

// Host contract AARs: staged release artifacts locked by SHA-256 in locks/host-api-aars.lock. The build never reads a sibling
// repository; a missing, extra or modified artifact fails configuration.
// zh-CN: 宿主契约 AAR: 以 SHA-256 锁定在 locks/host-api-aars.lock 中的 release 产物. 构建从不读取兄弟仓库; 产物缺失, 多余或被修改时
// 配置阶段即失败.
val hostApiAars: List<File> = run {
    val entries = readLock("locks/host-api-aars.lock")
    val names = entries.keys.filter { it.endsWith(".file") }.map { it.removeSuffix(".file") }.sorted()
    check(entries.keys == setOf("format") + names.flatMap { listOf("$it.file", "$it.sha256") }) { "Unexpected keys in locks/host-api-aars.lock" }
    names.map { name ->
        val fileName = entries.getValue("$name.file")
        check(fileName == File(fileName).name && fileName.endsWith(".aar") && !fileName.endsWith("-debug.aar")) { "Invalid $name.file" }
        verifiedFile("libs/$fileName", entries["$name.sha256"], name)
    }
}

// The sherpa-onnx runtime (roadmap D13, appendix D Q2 (b)): the official v1.13.8 GitHub Release AAR, pinned in
// locks/sherpa-onnx.lock because Maven Central has no k2-fsa coordinate.
// zh-CN: sherpa-onnx 运行时 (路线图 D13, 附录 D Q2 (b)): 官方 v1.13.8 GitHub Release AAR, Maven Central 没有 k2-fsa 坐标, 因此锁定在
// locks/sherpa-onnx.lock 中.
val sherpaLock = readLock("locks/sherpa-onnx.lock")
val sherpaVersion: String = sherpaLock.getValue("sherpa-onnx.version")
val sherpaAar: File = run {
    val keys = listOf("file", "version", "sha256", "url", "commit", "license").map { "sherpa-onnx.$it" }
    check(sherpaLock.keys == setOf("format") + keys) { "Unexpected keys in locks/sherpa-onnx.lock" }
    val fileName = sherpaLock.getValue("sherpa-onnx.file")
    check(fileName == "sherpa-onnx-$sherpaVersion.aar") { "sherpa-onnx.file must name the locked version" }
    check(sherpaLock.getValue("sherpa-onnx.url").endsWith("/v$sherpaVersion/$fileName")) { "sherpa-onnx.url must be the v$sherpaVersion release asset" }
    check(sherpaLock.getValue("sherpa-onnx.commit").matches(Regex("[0-9a-f]{40}"))) { "sherpa-onnx.commit must be the full tag commit" }
    verifiedFile("libs/$fileName", sherpaLock["sherpa-onnx.sha256"], "sherpa-onnx")
}

// The bundled silero VAD model and the device test recording (roadmap D5 / P0.2), locked in locks/vad-assets.lock.
// zh-CN: 随 APK 的 silero VAD 模型与设备测试录音 (路线图 D5 / P0.2), 锁定在 locks/vad-assets.lock 中.
run {
    val entries = readLock("locks/vad-assets.lock")
    val names = entries.keys.filter { it.endsWith(".path") }.map { it.removeSuffix(".path") }.sorted()
    check(names == listOf("silero-vad", "test-recording")) { "locks/vad-assets.lock must list silero-vad and test-recording" }
    check(entries.keys == setOf("format") + names.flatMap { name -> listOf("path", "sha256", "url", "upstream", "license").map { "$name.$it" } }) {
        "Unexpected keys in locks/vad-assets.lock"
    }
    names.forEach { name -> verifiedFile(entries.getValue("$name.path"), entries["$name.sha256"], name) }
}

run {
    // Device test recordings (roadmap P3.5): androidTest assets only, each pinned to its upstream archive member.
    val entries = readLock("locks/test-audio.lock")
    val names = entries.keys.filter { it.endsWith(".path") }.map { it.removeSuffix(".path") }.sorted()
    check(entries.keys == setOf("format") + names.flatMap { name -> listOf("path", "sha256", "url", "member", "license").map { "$name.$it" } }) {
        "Unexpected keys in locks/test-audio.lock"
    }
    names.forEach { name ->
        val path = entries.getValue("$name.path")
        check(path == "app/src/androidTest/assets/test-audio/$name.wav") { "test-audio $name must live in the androidTest assets" }
        verifiedFile(path, entries["$name.sha256"], "test-audio $name")
    }
}

android {
    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    defaultConfig {
        applicationId = globalApplicationId
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName

        testInstrumentationRunner = if (useReleaseProbe) {
            "$globalApplicationId.SherpaReleaseProbeInstrumentation"
        } else {
            "androidx.test.runner.AndroidJUnitRunner"
        }

        buildConfigField("String", "SHERPA_ONNX_VERSION", "\"$sherpaVersion\"")
        buildConfigField("String", "SUPPORTED_ABIS", "\"${pluginAbis.joinToString(",")}\"")

        resValue("string", "app_name", "3-Macaw Speech")
        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_id", "three-macaw-speech")
        resValue("string", "plugin_engine", "speech")
        resValue("string", "plugin_variant", "default")
        resValue("string", "plugin_requires_host_version", "$requiredHostVersion")
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))
    }

    testOptions { unitTests.isIncludeAndroidResources = true }

    lint {
        abortOnError = true
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        resValues = true
    }

    // One APK per ABI plus the universal APK (roadmap D14): sherpa-onnx and onnxruntime are ABI specific and make up most
    // of the package. zh-CN: 每个 ABI 一个 APK 并附 Universal APK (路线图 D14): sherpa-onnx 与 onnxruntime 与 ABI 相关, 占安装包的大部分.
    splits {
        abi {
            isEnable = true
            reset()
            include(*pluginAbis.toTypedArray())
            isUniversalApk = true
        }
    }

    packaging {
        // Compressed native libraries (roadmap D31, decided in P0.2): stored uncompressed, the arm64-v8a APK would carry
        // 27 MB of sherpa-onnx and onnxruntime and the universal APK 108 MB, far beyond the 20 / 60 MB budget. The installer
        // extracts the compressed libraries, so only their ELF alignment matters on 16 KB page devices, and every library
        // is 16 KB aligned (docs/development/native-abi-report.md).
        // zh-CN: 原生库压缩存储 (路线图 D31, P0.2 决定): 若不压缩, arm64-v8a APK 将包含 27 MB 的 sherpa-onnx 与 onnxruntime,
        // Universal APK 达 108 MB, 远超 20 / 60 MB 预算. 安装器会解压压缩的原生库, 因此 16 KB 页设备上只需 ELF 对齐,
        // 而全部库均为 16 KB 对齐 (docs/development/native-abi-report.md).
        jniLibs {
            useLegacyPackaging = true
            excludes += excludedSherpaLibraries.map { "**/$it" }
        }
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
    }

    bundle {
        language.enableSplit = false
        density.enableSplit = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val architecture = output.filters.find {
                it.filterType == FilterConfiguration.FilterType.ABI
            }?.identifier ?: "universal".takeIf { output.outputType == VariantOutputConfiguration.OutputType.UNIVERSAL }
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    val abiSuffix = architecture?.let { "-$it" }.orEmpty()
                    "${rootProject.name}-v$version$abiSuffix.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    implementation(files(hostApiAars))
    implementation(files(sherpaAar))

    implementation(libs.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Jetpack Compose (roadmap D26): the BOM is the single version source; activity-compose follows it. The screens arrive
    // in roadmap P5. zh-CN: Jetpack Compose (路线图 D26): BOM 是唯一的版本来源, activity-compose 随之; 界面在路线图 P5 加入.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.ui.tooling.preview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    register<Copy>("appendDigestToReleasedFiles") {
        description = "Appends CRC32 digest to released APK files"
        dependsOn("assembleRelease")

        val ext = utils.FILE_EXTENSION_APK
        val src = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease")
        val dst = file("$rootDir/${buildTypeRelease}s")

        from(src)
        into(dst)
        include("*.$ext")
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.FAIL

        eachFile {
            val digest = utils.digestCRC32(file)
            relativePath = RelativePath(true, "${name.removeSuffix(".$ext")}-$digest.$ext")
        }

        doLast { println("Destination: $dst") }
    }
}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}

// The shared gate checks the ELF alignment of every packaged library in all four ABIs (roadmap D14 does not skip the 32-bit
// ABIs); compressed libraries need no ZIP alignment. zh-CN: 共享门禁检查全部四个 ABI 中每个随包库的 ELF 对齐 (路线图 D14
// 不跳过 32 位 ABI); 压缩存储的库无需 ZIP 对齐.
nativeAlignment {
    strictAbis.set(pluginAbis.toSet())
    pageSize.set(16384L)
}

// Fail before collection when credentials, keystore or the actual APK set are incomplete, and pin the native library set and
// its compressed packaging in every split. zh-CN: 签名凭据, 密钥库或实际 APK 集合不完整时, 在收集之前失败, 并固定每个分包中的
// 原生库集合及其压缩存储方式.
val verifySignedReleaseArtifacts = tasks.register("verifySignedReleaseArtifacts") {
    group = "verification"
    dependsOn("assembleRelease")
    doLast {
        val signing = android.buildTypes.getByName(buildTypeRelease).signingConfig
        check(
            signing != null && signing.storeFile?.isFile == true &&
                !signing.storePassword.isNullOrBlank() && !signing.keyAlias.isNullOrBlank() &&
                !signing.keyPassword.isNullOrBlank(),
        ) { "Release signing configuration is missing or incomplete" }
        val directory = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease").get().asFile
        val apks = directory.listFiles { file -> file.isFile && file.extension == "apk" }.orEmpty().associateBy { it.name }
        val prefix = "${rootProject.name}-v${versions.appVersionName}"
        val expected = (pluginAbis + "universal").associateBy { "$prefix-$it.apk" }
        check(apks.keys == expected.keys) { "Unexpected release APK set: ${apks.keys.sorted()}" }
        val buildTools = androidComponents.sdkComponents.sdkDirectory.get().asFile
            .resolve("build-tools/${android.buildToolsVersion}")
        val signerJar = buildTools.resolve("lib/apksigner.jar")
        check(signerJar.isFile) { "Android SDK apksigner is unavailable" }
        expected.forEach { (name, abi) ->
            val apk = apks.getValue(name)
            val result = providers.exec {
                commandLine("java", "-jar", signerJar.absolutePath, "verify", apk.absolutePath)
                isIgnoreExitValue = true
            }.result.get()
            check(result.exitValue == 0) { "Release APK signature verification failed: $name" }
            val abis = if (abi == "universal") pluginAbis else listOf(abi)
            val wanted = abis.flatMap { a -> packagedNativeLibraries.map { "lib/$a/$it" } }.sorted()
            ZipFile(apk).use { zip ->
                val natives = zip.entries().asSequence().filter { it.name.startsWith("lib/") }.toList()
                check(natives.map { it.name }.sorted() == wanted) { "Unexpected native library set in $name: ${natives.map { it.name }.sorted()}" }
                check(natives.all { it.method == ZipEntry.DEFLATED }) { "Native libraries must be compressed in $name (roadmap D31)" }
                check(zip.getEntry("assets/vad/silero_vad_v5.onnx") != null) { "The bundled VAD model is missing from $name" }
            }
            println("Verified $name: ${apk.length()} bytes")
        }
    }
}
tasks.named("appendDigestToReleasedFiles") { dependsOn(verifySignedReleaseArtifacts) }
