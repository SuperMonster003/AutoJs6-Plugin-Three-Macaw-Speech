package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

/**
 * The stage of a model installation that people see (roadmap D181). `onProgress` counts the download and then the bytes
 * unpacked from it, which suits a fraction but makes an 81 MB model read "162 MB", since mirror archives are not
 * compressed. The screens and the notification show the two stages one after the other instead, each from 0 to 100 %:
 * the download against the size the model is listed with, then the unpacking as a percentage. An operation without
 * [unpacking] bytes, such as copying a document for an import, stays one stage.
 * zh-CN: 用户看到的模型安装阶段 (路线图 D181). `onProgress` 先计下载, 再计解包写出的字节, 用来算比例没有问题, 但镜像归档未压缩,
 * 81 MB 的模型会显示成 "162 MB". 所以界面与通知把两个阶段先后显示, 各自从 0 到 100 %: 下载按模型列出的大小显示, 解包只显示百分比.
 * 没有解包字节的操作 (如导入时复制文档) 仍是一个阶段.
 */
data class InstallProgress(val unpacking: Boolean, val done: Long, val total: Long) {

    /** Share of this stage, or null when its size is unknown. zh-CN: 本阶段的比例, 大小未知时为 null. */
    val fraction: Float? get() = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else null

    val percent: Int get() = ((fraction ?: 0f) * 100).toInt()

    companion object {

        /** [done] of [total] as `onProgress` reports them, the last [unpackBytes] of [total] being the unpacking. */
        fun of(done: Long, total: Long, unpackBytes: Long): InstallProgress {
            val download = total - unpackBytes
            return if (unpackBytes > 0 && done >= download) InstallProgress(true, done - download, unpackBytes) else InstallProgress(false, done, download)
        }
    }
}
