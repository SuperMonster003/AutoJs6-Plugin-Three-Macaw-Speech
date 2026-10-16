package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes

/**
 * Failure of a model or catalog operation with the contract error code the host shows the script (roadmap D27,
 * `SpeechErrorCodes`). Messages are English, as the protocol requires.
 * zh-CN: 模型或目录操作的失败, 带宿主展示给脚本的契约错误码 (路线图 D27, `SpeechErrorCodes`). 按协议要求, 信息为英文.
 */
open class ModelException(code: String, message: String, cause: Throwable? = null) : SpeechException(code, message, cause)

/**
 * A catalog that is malformed, wrongly signed, rolled back or not ours; reported as `MODEL_CHECKSUM_MISMATCH`, the code
 * the protocol names for signature failures of `refreshCatalog`.
 * zh-CN: 格式错误, 签名不符, 版本回退或来源不符的目录; 报告为 `MODEL_CHECKSUM_MISMATCH`, 即协议为 `refreshCatalog` 签名失败指定的错误码.
 */
open class CatalogException(message: String, cause: Throwable? = null) :
    ModelException(SpeechErrorCodes.MODEL_CHECKSUM_MISMATCH, message, cause)

class CatalogSignatureException(message: String, cause: Throwable? = null) : CatalogException(message, cause)

class CatalogRollbackException(message: String) : CatalogException(message)

/** Size or SHA-256 differs from the pinned value. zh-CN: 大小或 SHA-256 与固定值不同. */
class IntegrityException(message: String) : ModelException(SpeechErrorCodes.MODEL_CHECKSUM_MISMATCH, message)

/** A malformed, truncated or unsafe archive. zh-CN: 格式错误, 被截断或不安全的归档. */
class ArchiveFormatException(message: String, cause: Throwable? = null) : ModelException(SpeechErrorCodes.UNSUPPORTED_FORMAT, message, cause)

/** The operation was cancelled by `cancelOperation`. zh-CN: 操作被 `cancelOperation` 取消. */
class OperationCancelledException : ModelException(SpeechErrorCodes.SESSION_CANCELLED, "The operation was cancelled")

/** Not enough free space for the download and the installed files. zh-CN: 下载与安装文件所需的可用空间不足. */
class InsufficientSpaceException(val required: Long, val available: Long) :
    ModelException(SpeechErrorCodes.IO_FAILED, "Not enough free space: $required bytes needed, $available bytes available")

/**
 * An HTTP answer other than the expected one; the downloader falls back to the next source on any of them.
 * zh-CN: 非预期的 HTTP 应答; 下载器遇到任何此类应答都会回退到下一个来源.
 */
class HttpStatusException(val status: Int, val url: String) :
    ModelException(SpeechErrorCodes.MODEL_DOWNLOAD_FAILED, "HTTP $status from ${redactQuery(url)}")

internal fun redactQuery(url: String): String = url.substringBefore('?')

/**
 * An import whose files match no catalog version and that names no custom identity: the screens then ask for kind,
 * model type, name, languages and id (roadmap P5.1); scripts see the same `INVALID_OPTIONS`.
 * zh-CN: 文件不匹配任何目录版本且未给出自定义身份的导入: 界面随后询问种类, 模型类型, 名称, 语言与 ID (路线图 P5.1); 脚本看到的仍是 `INVALID_OPTIONS`.
 */
class NoCatalogMatchException : ModelException(
    SpeechErrorCodes.INVALID_OPTIONS,
    "The import matches no catalog model; model, kind, modelType, name and languages are required",
)
