package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.ThreeMacawSpeechInfoService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.ThreeMacawSpeechService
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.speech.api.ISpeechPlugin
import org.autojs.plugin.speech.api.SpeechPluginActions
import org.autojs.plugin.speech.api.SpeechPluginIds
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The host-facing activation and discovery contract against the installed APK (roadmap P0.3, AGENTS 14.2): the Wake
 * activity, discovery of exactly one component per action and category, explicit binding with the expected Binder
 * descriptors, `getInfo()` marshalled through a real Parcel with the transaction code the host uses, the refusing
 * placeholder capability Binder, repeated bindings, the absent launcher entry and the exported / permission audit.
 *
 * The services run in the plugin's own process, so the calls below use `transact` with explicit Parcels instead of
 * `asInterface`, which would return the local object and skip marshalling.
 *
 * zh-CN: 针对已安装 APK 验证面向宿主的激活与发现契约 (路线图 P0.3, AGENTS 14.2): Wake Activity, 每组 action 与 category 恰好
 * 发现一个组件, 显式绑定与 Binder 描述符, 以宿主使用的事务号经真实 Parcel 编组的 `getInfo()`, 拒绝调用的占位能力 Binder,
 * 重复绑定, 不存在的启动器入口, 以及 exported / permission 审查. 服务与测试同进程, 因此下文用显式 Parcel 调用 `transact`, 不用
 * 会直接返回本地对象而跳过编组的 `asInterface`.
 */
@RunWith(AndroidJUnit4::class)
class ThreeMacawSpeechContractTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val packageName: String
        get() = context.packageName

    @Test
    fun wakeActivityFollowsTheHostActivationContract() {
        val metaData = requireNotNull(context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData)
        assertEquals(".WakeActivity", metaData.getString("org.autojs.plugin.WAKE_ACTIVITY"))
        assertEquals(context.getString(R.string.plugin_author), metaData.getString("org.autojs.plugin.info.AUTHOR"))
        assertEquals(16384, metaData.getInt(PluginCapabilityKeys.NATIVE_PAGE_ALIGNMENT, -1))
        // Written by the shared native-alignment plugin from the merged native libraries and the ABI splits.
        assertEquals(BuildConfig.SUPPORTED_ABIS, metaData.getString(PluginCapabilityKeys.SUPPORTED_ABIS))

        val component = ComponentName(packageName, "$packageName.WakeActivity")
        val activityInfo = context.packageManager.getActivityInfo(component, 0)
        assertTrue(activityInfo.exported)
        assertTrue(activityInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, activityInfo.permission)
        assertEquals(android.R.style.Theme_NoDisplay, activityInfo.theme)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH != 0)

        val matches = queryActivities(Intent("org.autojs.plugin.action.WAKE").addCategory(Intent.CATEGORY_DEFAULT).setPackage(packageName))
        assertEquals("the WAKE action resolves exactly one activity", listOf(component.className), matches)
    }

    @Test
    fun thereIsNoLauncherEntryBeforeTheScreensLand() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)
        assertEquals("roadmap P5 adds the launcher entry", emptyList<String>(), queryActivities(launcher))
    }

    @Test
    fun infoServiceAnswersTheSpeechIdentity() {
        val serviceInfo = discoverSingleService(PluginActions.INFO, ThreeMacawSpeechContract.INFO_CATEGORY, ThreeMacawSpeechInfoService::class.java.name)
        assertEquals(ThreeMacawSpeechContract.REQUIRED_HOST_VERSION.toString(), serviceInfo.metaData?.get(PluginCapabilityKeys.REQUIRES_HOST_VERSION)?.toString())
        withBoundService(serviceInfo) { binder ->
            assertEquals(IPluginInfoProvider.DESCRIPTOR, binder.interfaceDescriptor)
            val info = transactPluginInfo(binder)
            assertIdentity(info)
            val capabilities = requireNotNull(info.capabilities)
            assertEquals(
                setOf(
                    PluginCapabilityKeys.REQUIRES_HOST_VERSION, ThreeMacawSpeechContract.CAPABILITY_CONTRACT_VERSION,
                    ThreeMacawSpeechContract.CAPABILITY_FEATURES, ThreeMacawSpeechContract.CAPABILITY_CATALOG_VERSION,
                ),
                capabilities.keySet(),
            )
            assertEquals(ThreeMacawSpeechContract.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
            assertEquals(1, capabilities.getInt(ThreeMacawSpeechContract.CAPABILITY_CONTRACT_VERSION))
            assertArrayEquals(emptyArray<String>(), capabilities.getStringArray(ThreeMacawSpeechContract.CAPABILITY_FEATURES))
            assertEquals(0, capabilities.getInt(ThreeMacawSpeechContract.CAPABILITY_CATALOG_VERSION, -1))
        }
    }

    @Test
    fun capabilityServiceIsDiscoverableAndRejectsCallsUntilTheContractLands() {
        // Discovered with the literals of the staged host contract speech-api.aar (roadmap P1.1).
        val serviceInfo = discoverSingleService(
            SpeechPluginActions.SPEECH, SpeechPluginActions.SERVICE_CATEGORY, ThreeMacawSpeechService::class.java.name,
        )
        assertEquals(SpeechPluginActions.PLUGIN_PERMISSION, serviceInfo.permission)
        assertEquals(SpeechPluginIds.REQUIRED_HOST_VERSION_CODE.toString(), serviceInfo.metaData?.get(PluginCapabilityKeys.REQUIRES_HOST_VERSION)?.toString())
        withBoundService(serviceInfo) { binder ->
            assertEquals(ISpeechPlugin.DESCRIPTOR, binder.interfaceDescriptor)
            // Placeholder until roadmap P1.1: every transaction is refused instead of answered with guesses.
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(ThreeMacawSpeechContract.CAPABILITY_DESCRIPTOR)
                assertFalse(binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0))
            } finally {
                data.recycle()
                reply.recycle()
            }
        }
    }

    @Test
    fun repeatedBindingsKeepAnsweringTheSameIdentity() {
        val serviceInfo = discoverSingleService(PluginActions.INFO, ThreeMacawSpeechContract.INFO_CATEGORY, ThreeMacawSpeechInfoService::class.java.name)
        val versions = (1..3).map {
            var versionCode = -1L
            withBoundService(serviceInfo) { binder -> versionCode = transactPluginInfo(binder).versionCode }
            versionCode
        }
        assertEquals(1, versions.toSet().size)
        assertTrue(versions.first() > 0)
    }

    @Test
    fun everyExportedComponentRequiresThePluginPermission() {
        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.GET_PERMISSIONS,
        )
        val exported = packageInfo.activities.orEmpty().filter { it.exported }.map { it.name to it.permission } +
            packageInfo.services.orEmpty().filter { it.exported }.map { it.name to it.permission } +
            packageInfo.receivers.orEmpty().filter { it.exported }.map { it.name to it.permission } +
            packageInfo.providers.orEmpty().filter { it.exported }.map { it.name to it.readPermission }
        val (library, own) = exported.partition { it.first in LIBRARY_COMPONENTS }
        assertEquals(
            setOf("$packageName.WakeActivity", ThreeMacawSpeechInfoService::class.java.name, ThreeMacawSpeechService::class.java.name),
            own.map { it.first }.toSet(),
        )
        own.forEach { (name, permission) -> assertEquals("$name must require the plugin permission", PLUGIN_PERMISSION, permission) }
        library.forEach { (name, permission) -> assertEquals("$name keeps its system protection", LIBRARY_COMPONENTS.getValue(name), permission) }
        assertEquals(
            setOf(
                PLUGIN_PERMISSION, "android.permission.RECORD_AUDIO", "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_MICROPHONE", "android.permission.INTERNET", "android.permission.POST_NOTIFICATIONS",
            ),
            packageInfo.requestedPermissions.orEmpty().toSet() - MERGED_PLATFORM_PERMISSIONS,
        )
        assertTrue("backups stay disabled", context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP == 0)
    }

    private fun assertIdentity(info: PluginInfo) {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        assertEquals("3-Macaw Speech", info.name)
        assertEquals(context.getString(R.string.app_name), info.name)
        assertEquals(context.getString(R.string.plugin_description), info.description)
        assertEquals(ThreeMacawSpeechContract.INSTRUCTION_REFERENCE, info.instruction)
        assertTrue("the instruction resource exists", context.resources.getIdentifier("plugin_instruction", "raw", packageName) != 0)
        assertEquals("SuperMonster003", info.author)
        assertEquals(ThreeMacawSpeechContract.PLUGIN_ID, info.id)
        assertEquals(context.getString(R.string.plugin_id), info.id)
        assertEquals(ThreeMacawSpeechContract.ENGINE, info.engine)
        assertEquals(ThreeMacawSpeechContract.VARIANT, info.variant)
        assertEquals(packageInfo.versionName, info.versionName)
        assertEquals(packageInfo.versionCodeCompat(), info.versionCode)
        assertEquals(context.getString(R.string.plugin_version_date), info.versionDate)
        // Roadmap D14: the ABIs the plugin is built for, in the split order, whatever split is installed.
        assertArrayEquals(arrayOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"), requireNotNull(info.supportedAbis))
    }

    private fun PackageInfo.versionCodeCompat(): Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }

    /** `getInfo()` is the first transaction of IPluginInfoProvider. */
    private fun transactPluginInfo(binder: IBinder): PluginInfo {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(IPluginInfoProvider.DESCRIPTOR)
            assertTrue("getInfo() transaction must be handled", binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0))
            reply.readException()
            assertTrue("getInfo() must not return null", reply.readInt() != 0)
            return pluginInfoCreator.createFromParcel(reply)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun queryActivities(intent: Intent): List<String> {
        @Suppress("DEPRECATION")
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.name }
    }

    private fun discoverSingleService(action: String, category: String, expectedClassName: String): ServiceInfo {
        val intent = Intent(action).addCategory(category).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
        assertEquals("$action / $category must resolve exactly one service: ${matches.map { it.serviceInfo.name }}", 1, matches.size)
        val serviceInfo = matches.single().serviceInfo
        assertEquals(packageName, serviceInfo.packageName)
        assertEquals(expectedClassName, serviceInfo.name)
        assertTrue("$expectedClassName must be exported", serviceInfo.exported)
        assertTrue("$expectedClassName must be enabled", serviceInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, serviceInfo.permission)
        assertEquals("services run in the plugin's default process", packageName, serviceInfo.processName)
        return serviceInfo
    }

    private fun withBoundService(serviceInfo: ServiceInfo, block: (IBinder) -> Unit) {
        val binderReference = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binderReference.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit

            override fun onNullBinding(name: ComponentName) {
                connected.countDown()
            }
        }
        val explicit = Intent().setComponent(ComponentName(serviceInfo.packageName, serviceInfo.name))
        assertTrue("bindService returned false", context.bindService(explicit, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("timed out waiting for ${serviceInfo.name}", connected.await(10, TimeUnit.SECONDS))
            block(requireNotNull(binderReference.get()) { "${serviceInfo.name} returned a null binding" })
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"

        /**
         * Added by AndroidX to the merged Manifest on its own (core 1.9+ declares the dynamic receiver permission).
         * zh-CN: AndroidX 自行加入合并 Manifest 的权限 (core 1.9+ 声明的动态接收器权限).
         */
        val MERGED_PLATFORM_PERMISSIONS: Set<String> = setOf("$PACKAGE.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")

        const val PACKAGE = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"

        /**
         * Exported library components merged in by AndroidX, with the system permission that protects them: Compose pulls in
         * profileinstaller, whose receiver only answers holders of DUMP (adb, the system).
         * zh-CN: AndroidX 合并进来的导出库组件及保护它们的系统权限: Compose 引入 profileinstaller, 其接收器只响应持有 DUMP 的调用方
         * (adb 与系统).
         */
        val LIBRARY_COMPONENTS: Map<String, String> = mapOf("androidx.profileinstaller.ProfileInstallReceiver" to "android.permission.DUMP")

        /** The Parcelize-generated `CREATOR` is a JVM static field that Kotlin sources cannot name directly. */
        @Suppress("UNCHECKED_CAST")
        val pluginInfoCreator: Parcelable.Creator<PluginInfo> =
            PluginInfo::class.java.getField("CREATOR").get(null) as Parcelable.Creator<PluginInfo>
    }

}
