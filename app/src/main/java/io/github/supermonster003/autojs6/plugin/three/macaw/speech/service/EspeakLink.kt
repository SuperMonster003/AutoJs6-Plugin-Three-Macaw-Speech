package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.system.ErrnoException
import android.system.Os
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.EspeakHome
import java.io.File
import java.util.UUID

/**
 * [EspeakHome.Link] as a symbolic link: `android.system.Os`, because `java.nio.file.Files` needs API 26 and minSdk is
 * 24. The new link replaces the old one by rename, so a reader never finds it missing.
 * zh-CN: 以符号链接实现的 [EspeakHome.Link]: 使用 `android.system.Os`, 因为 `java.nio.file.Files` 需要 API 26 而 minSdk 为 24. 新链接经改名替换旧链接,
 * 读取方因此不会碰到链接缺失.
 */
internal class EspeakLink(override val path: File) : EspeakHome.Link {

    override fun target(): File? = try {
        File(Os.readlink(path.path))
    } catch (_: ErrnoException) {
        null
    }

    override fun point(target: File): Boolean {
        val temporary = File(path.parentFile, "${path.name}.${UUID.randomUUID()}.tmp")
        return try {
            path.parentFile?.mkdirs()
            Os.symlink(target.path, temporary.path)
            Os.rename(temporary.path, path.path)
            true
        } catch (e: ErrnoException) {
            Log.w(TAG, "Cannot link the espeak-ng data: ${e.message}")
            temporary.delete()
            false
        }
    }

    private companion object {
        const val TAG = "MacawEspeak"
    }
}
