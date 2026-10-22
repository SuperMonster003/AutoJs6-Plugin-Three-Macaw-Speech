package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout

import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileProblem

/**
 * The localized hints of a failed file recognition (roadmap D115), shared by the file tab and the result notification.
 * zh-CN: 文件识别失败时的本地化提示 (路线图 D115), 由文件标签与结果通知共用.
 */
internal object FileTexts {

    @StringRes
    fun problem(problem: FileProblem): Int = when (problem) {
        FileProblem.UNSUPPORTED_FORMAT -> R.string.tryout_transcribe_error_format
        FileProblem.LIMIT_EXCEEDED -> R.string.tryout_transcribe_error_limit
        FileProblem.IO_FAILED -> R.string.tryout_transcribe_error_io
        FileProblem.MODEL_MISSING -> R.string.tryout_transcribe_error_model
        FileProblem.ENGINE_FAILED -> R.string.tryout_transcribe_error_engine
    }
}
