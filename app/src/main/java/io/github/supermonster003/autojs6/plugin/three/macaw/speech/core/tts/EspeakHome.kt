package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts

import java.io.File

/**
 * The one espeak-ng data directory of the process (roadmap D175). sherpa-onnx starts espeak-ng once, with the data
 * directory of the first model that needs it, and espeak-ng keeps reading voices and dictionaries from that path; a
 * model given another directory later still reads the first. Once that directory is deleted, by an update or a removal,
 * every model that needs espeak-ng in this process loses most of its text without any error (measured on the G8441:
 * 6330 samples instead of 85461). So engines get a fixed [Link] to the data of one installed model, which [leave] moves
 * to another model before that data goes. Every catalog model ships the same data, so any of them serves all of them.
 * Pure JVM; the link itself is Android's.
 * zh-CN: 进程唯一的 espeak-ng 数据目录 (路线图 D175). sherpa-onnx 只启动一次 espeak-ng, 用的是第一个需要它的模型的数据目录, 此后 espeak-ng 一直从该路径读取语音与
 * 词典; 之后的模型即使传入别的目录, 读的仍是第一个. 该目录一旦因更新或删除而消失, 本进程里所有需要 espeak-ng 的模型都会丢掉大部分文字且不报任何错 (G8441 实测
 * 6330 个样本, 正常为 85461). 因此引擎拿到的是指向某个已安装模型数据的固定 [Link], 该数据被删除之前由 [leave] 把链接移到另一个模型. 全部目录模型自带的数据相同,
 * 任何一份都能服务全部模型. 纯 JVM; 链接本身由 Android 层实现.
 */
class EspeakHome(private val link: Link) {

    /** A symbolic link that moves atomically. zh-CN: 可原子移动的符号链接. */
    interface Link {
        val path: File

        /** Where it points, null when it does not exist. zh-CN: 链接的目标, 不存在时为 null. */
        fun target(): File?

        /** False when the file system refuses. zh-CN: 文件系统拒绝时返回 false. */
        fun point(target: File): Boolean
    }

    /**
     * The data directory to give sherpa-onnx for a model whose own data is [own]: the link, pointed at [own] unless it
     * already leads to complete data; [own] itself when no link can be made, the behavior before roadmap D175.
     * zh-CN: 为自带数据位于 [own] 的模型提供给 sherpa-onnx 的数据目录: 即该链接, 除非它已通向完整的数据, 否则先指向 [own]; 无法创建链接时为 [own] 本身, 即路线图
     * D175 之前的行为.
     */
    fun dataDir(own: File): File = synchronized(this) {
        val current = link.target()
        if (current != null && complete(current)) link.path else if (link.point(own)) link.path else own
    }

    /**
     * Moves the link away from [directory], which is about to be deleted, to the first of [others] with complete data.
     * Without one it stays and leads nowhere, which nothing reads, and the next [dataDir] repairs it.
     * zh-CN: 把链接从即将删除的 [directory] 移到 [others] 中第一份完整的数据. 没有时链接保持原样, 指向已不存在的位置; 此时没有模型读取它, 下一次 [dataDir] 会修复它.
     */
    fun leave(directory: File, others: () -> List<File>) = synchronized(this) {
        val current = link.target() ?: return@synchronized
        if (!current.path.startsWith(directory.path + File.separator)) return@synchronized
        others().firstOrNull(::complete)?.let(link::point)
        Unit
    }

    private fun complete(data: File): Boolean = File(data, COMPLETE_MARK).isFile

    private companion object {
        /** sherpa-onnx refuses a data directory without it. zh-CN: 缺少它时 sherpa-onnx 拒绝该数据目录. */
        const val COMPLETE_MARK = "phontab"
    }
}
