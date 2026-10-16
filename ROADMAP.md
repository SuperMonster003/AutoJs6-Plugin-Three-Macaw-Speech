# 3-Macaw Speech Roadmap

更新日期: 2026-10-16

本文是 `AutoJs6-Plugin-Three-Macaw-Speech` (显示名 `3-Macaw Speech`; 基于 [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) 为 AutoJs6 脚本提供离线语音识别, 语音活动检测, 唤醒词检测与神经网络语音合成, 并把合成能力注册为 Android 系统 TTS 引擎; 脚本侧挂载为全局模块 `speech`, 有独立启动入口) 的可执行状态表.
以 2026-10-16 的宿主本地代码快照 (`AutoJs6 master@33a1514b74`, `VERSION_NAME=6.8.0` 尚未发布, `VERSION_BUILD=3796` + `VERSION_CODE_OFFSET=1652`, 即 versionCode 5448; 宿主 `tts` 模块已有平台无关的 `TtsBackend` 边界与引擎查询 API), 平台版本插件 `1.9.1`, sherpa-onnx `v1.13.8` (2026-09-10, Apache-2.0), `AutoJs6-Shared-Assets main` (ace-fonts v4 已发布, codegen-models 待发布 v1) 为起点, 每个条目均可独立 Check 并落地, 后续会话按阶段逐步推进.
插件只做 sherpa-onnx 的 Android 集成, 模型管理与宿主契约, 不修改 sherpa-onnx 源码, 不训练模型; 模型文件不入 Git, 经 `AutoJs6-Shared-Assets` 的不可变 Release 分发 (第 1 节 D9).

使用方式:

1. 每次会话开始时, 从 "阶段总览" (第 6 节) 选取一个或多个未完成条目, 优先级按阶段顺序; 单次会话可完成多个小节, 除非单个小节已足够繁杂.
2. 条目完成后勾选 `[x]`, 并在条目后追加证据 (提交 hash / 测试类名 / 设备型号与 API / 模型 ID / 度量值), 证据等级见附录 E. 未勾选条目属于规划意向, 不代表当前版本能力.
3. 条目前缀标明主要落点: `(插件)` 本仓库, `(宿主)` `D:/idea-projects/AutoJs6`, `(资产)` `D:/idea-projects/AutoJs6-Shared-Assets`, `(索引)` `D:/idea-projects/AutoJs6-Official-Plugins-Index`, `(文档)` AutoJs6-Documentation / AutoJs6-TypeScript-Declarations / AutoJs6-Plugin-Ace-Editor / AutoJs6-Plugin-Offline-Docs 四个关联仓库 (均位于 `D:/idea-projects/`), `(设计)` 设计文档, `(测试)`, `(发布)`.
4. 涉及宿主公开契约 (speech 契约, 脚本 API) 的条目, 完成后必须同步宿主 `docs/dev/`, 宿主 `.changelog` (10 种语言, 本轮先只写 zh-Hans, 与 Batch Rename 做法一致) 与本仓库 `.changelog`. 宿主侧提交只落本地, 由维护者合并与推送. `AutoJs6-Shared-Assets` 的推送与签名由维护者执行.
5. 第 1 节 "固定决策" 由维护者 2026-10-16 经三轮选择题确认, 后续阶段不再重新讨论; 第 2 节 "派生技术决策" 可在进入对应阶段前推翻 (推翻点已逐条标注), 之后视同固定; 附录 D "待决事项" 在进入对应阶段前由维护者拍板, 拍板结果回填到第 1 节或第 2 节.
6. 本仓库骨架 (Gradle / Manifest / 资源 / CI) 在 P0.1 按 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成, 其裁剪版即本仓库 `AGENTS.md`; 命名与身份派生按 `AUTOJS6_PLUGIN_THREE_SERIES_RENAME_AGENTS.md` 第 3 节 (Macaw 为 5 字母单词, `ThreeMacaw` 恰好 10 字母); 有独立启动入口 (D7), 因此 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` 与 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` 均适用; 工程约定 (构建, 资源, 文档生成, 提交计数, 推送前检查 `py D:/idea-projects/prepush_check.py`) 以 `AGENTS.md` 与 `D:/idea-projects/COMMON_AGENTS.md` 为准, 本文件只记录 "改什么" 与证据. 所有文案 (10 种语言) 只用 ASCII 标点.
7. 2026-10-16 的首个会话只落盘路线图并 `git init` + 首次提交 (D12); 之后按 `AGENTS.md` 的提交规则工作 (每笔提交前 `VERSION_BUILD = 提交数 + 1`). 与全部官方插件仓库一致, `AGENTS.md` / `CLAUDE.md` 只在本地维护并被 `.gitignore` 忽略, 不入库.

---

## 1. 固定决策

以下决策 D1-D12 已由维护者于 2026-10-16 分三轮确认.

| 编号 | 决策 | 含义 |
| --- | --- | --- |
| D1 | 命名: `AutoJs6-Plugin-Three-Macaw-Speech` | 显示名 `3-Macaw Speech` (Macaw 为金刚鹦鹉, 会听会学舌, 与 Maple / Amber / Folio 同为具象名词), 插件 ID `three-macaw-speech`, 点分包名 `io.github.supermonster003.autojs6.plugin.three.macaw.speech`, 并入 Three 系列. 身份派生表见 5.5. 候选 Vocal / Sonic / Chime 已放弃. |
| D2 | 1.0.0 能力范围: ASR + VAD + TTS + 唤醒词 | 离线语音识别 (流式与文件), 语音活动检测, 神经网络语音合成, 开放词表唤醒词 (KWS, 无需训练). 四项共用一个 sherpa-onnx 运行时, 模型分开下载 (D5). |
| D3 | TTS 接入: 系统 TTS 引擎 + Binder 契约 | 插件实现 Android `TextToSpeechService` 注册为系统引擎, 宿主现有 `tts` 模块通过 engine 选项直接使用, 其他应用也可使用; Binder 契约另提供合成到文件, 进度与取消, 以及模型管理. 宿主 `tts` 模块不新增插件 backend. |
| D4 | 脚本 API 形态: 新增全局模块 `speech` | `speech.recognize` (文件 / PCM), `speech.listen` (返回带 `partial` / `final` / `end` 等事件的会话), `speech.wake` (唤醒词会话), `speech.synthesize` (合成到文件), `speech.speak` (委托宿主 `tts` 并固定 engine 为本插件), `speech.detectVoice`, `speech.models.*`; `$speech` 指向同一对象. 现有 `tts` 模块保持原样. 草案见附录 B. |
| D5 | 模型分发: APK 只含运行时与 VAD, 其余按需下载 | 仿 3-Stone AI: 内置固定版本的模型目录快照, 固定字节数 + SHA-256 校验, 下载到插件私有存储并原子激活; 支持从本地文件导入. 不按语言拆变体 APK, 不在 APK 内置识别或合成模型. |
| D6 | 录音归属: 插件采集, 宿主以 `BIND_INCLUDE_CAPABILITIES` 绑定 | 插件自己申请 `RECORD_AUDIO` 并在进程内采集, 音频不过 Binder; 宿主绑定时传递前台能力, 脚本在后台也能听. 契约同时接受文件 / PCM 输入 (宿主或脚本自备音频). 宿主虽已持有 `RECORD_AUDIO`, 不替插件录音. |
| D7 | 独立界面: 模型管理 + 设置 + 试听试说 + 发行历史 | 有启动器入口 (Icon Studio 亮暗图标), 按 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`; 下载进度, 模型选择, 麦克风测试与 TTS 试听都在界面内完成. 不做聊天或助手界面. |
| D8 | 3-Stove Agent 联动: 1.0.0 脚本级样例, 1.1.0 内置语音助手 | 1.0.0 随宿主附样例 (`speech.wake -> speech.listen -> ai.agent.run`); 插件内置的常驻唤醒, 悬浮麦克风与直调 Stove Agent AIDL 排到 1.1.0 (P7). |
| D9 | 模型来源: `AutoJs6-Shared-Assets` 为主, sherpa-onnx 上游 URL 为备用 | Shared-Assets 新增 `speech-models` 类别 (独立目录 / 清单 / 标签前缀 `speech-models-vN` / 签名密钥), 按模型重打包为内容寻址的不可变 Release 资源; 目录每条可同时记录上游 sherpa-onnx Release URL, 镜像不可用时回退上游, 两条路径最终都按文件级 SHA-256 校验 (D24). |
| D10 | 1.0.0 目录范围: 精选 | 识别: 流式 zh-en zipformer int8, 流式 zh int8 (2025), SenseVoice int8 (zh / en / ja / ko / yue 离线), 流式 en int8; 合成: vits-melo-tts-zh_en, kokoro-multi-lang-v1_1, vits-icefall-zh-aishell3, piper en_US-lessac-medium, matcha-icefall-zh-baker; 唤醒词 zh-en 3M; VAD silero v5. 清单见附录 A, 其他语言按需追加. |
| D11 | 仅限非商业用途的模型: 收录但标注许可并在下载前提示 | 目录每条带 `license` 与 `commercialUse` 字段; `commercialUse=false` 的模型 (如 matcha-icefall-zh-baker 的 DataBaker 语料) 在界面与脚本下载前给出许可提示, 脚本侧需显式 `acceptLicense: true`. |
| D12 | 本次会话交付 | 新建 `D:/idea-projects/AutoJs6-Plugin-Three-Macaw-Speech`, 只写入 `ROADMAP.md`, 设置仓库级提交身份后做一笔 `docs` 提交. 骨架 (Gradle / Manifest / AGENTS.md 裁剪版) 作为 P0 由后续会话完成. |

---

## 2. 派生技术决策

D13-D32 据第 1 节派生, 进入对应阶段前可推翻 (推翻点见各条), 之后视同固定.

| 编号 | 决策 | 含义 | 推翻点 |
| --- | --- | --- | --- |
| D13 | 运行时: sherpa-onnx v1.13.8 官方 Android AAR | 使用上游预编译 AAR (`libsherpa-onnx-jni.so` + `libonnxruntime.so`, 4 ABI; GitHub Release 资源 `sherpa-onnx-1.13.8.aar` 47.8 MB). 优先 Maven Central `com.k2fsa.sherpa.onnx:sherpa-onnx-android` 的同版本坐标 (P0.2 确认版本存在且字节与 Release 一致); 坐标不可用时把 AAR 放入 `libs/` 并在 `locks/` 记录来源, 版本与 SHA-256. 1.0.0 不自行编译 C++. | P0.2 |
| D14 | ABI 拆分: 4 ABI + Universal | `splits.abi.include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")`, `PluginInfo.supportedAbis` 与之一致; P0.2 对 AAR 内每个 `.so` 做 16 KB 页对齐与 ZIP 对齐检查, 不合格时 P0.2 改为 `libs/` 内自行重打包或等待上游修复, 不以 "多数设备 4 KB" 为由跳过. | P0.2 |
| D15 | 音频格式 | 内部统一 16 kHz 单声道 float; 外部 PCM 输入为 16 kHz 单声道 PCM16 LE, 每次 `feed` 不超过 1 MB; 文件输入 1.0.0 只收 WAV (PCM 8 / 16 / 24 / 32-bit 与 float, 任意采样率, 单 / 双声道, 插件内重采样并混为单声道), 其他容器报 `UNSUPPORTED_FORMAT`; 单文件上限 200 MB 或 30 分钟. | P3.3 |
| D16 | 会话模型 | `openSession` / `openWakeSession` 返回会话 Binder, 事件经 oneway 回调 Binder 推送; 每个宿主 Binder token 同时最多 1 个麦克风会话 (听写与唤醒互斥, 唤醒会话提供 "唤醒后自动转听写" 模式以免切换), 外部 PCM 会话最多 4 个; 宿主进程死亡 (`linkToDeath`) 或脚本退出时插件释放会话与麦克风. | P3.2 |
| D17 | 识别管线 | 流式模型 (zipformer transducer online) 走 `OnlineRecognizer` + 端点检测 (sherpa rule1 / rule2 / rule3 默认值可调) + 可选 VAD 门控; 离线模型 (SenseVoice, 离线 zipformer) 走 VAD 切段 + `OfflineRecognizer` 逐段识别. 两类模型在 `speech.listen` 上提供同一事件集, 离线模型的 `partial` 为段级而非字级 (文档注明). 热词 (hotwords) 仅 transducer 模型支持, 其余模型收到热词时返回 `warnings[]` 而非抛错. | P3.2 |
| D18 | 唤醒词 | sherpa-onnx `KeywordSpotter` + zipformer KWS zh-en 3M 模型; 脚本传关键词原文 (`string[]` 或 `{keyword, boost, threshold}[]`, 上限 50 条), 插件进程内转换为模型 token (中文拼音, 英文 BPE). P3.4 先做 spike 确认 1.13.8 的 Kotlin API 是否内置原文转换 (上游已集成 cppinyin 与 ssentencepiece, 附录 D Q4), 不内置则插件内置拼音表 (`pinyin-data`, MIT) 与 BPE 编码. | P3.4 |
| D19 | 系统 TTS 引擎映射 | 每个已安装 TTS 模型展开为一组 `Voice` (模型 x 说话人), Voice 名 `<modelId>/<speakerId>`, locale 由模型语言决定 (双语模型同时报告 `zh-CN` 与 `en-US`); `onSynthesizeText` 在插件内按句切分, 分块合成并经 `SynthesisCallback.audioAvailable` 流式回传 (PCM16, 模型采样率); Android 语速 / 音高映射到 sherpa `speed` (音高不支持时忽略并记录); 无已安装模型时 `onIsLanguageAvailable` 返回 `LANG_MISSING_DATA`, 设置页提示下载. | P4.2 |
| D20 | Binder 合成 | `synthesizeToFile` 输出 WAV (PCM16) 到宿主传入的 `ParcelFileDescriptor`, 支持进度回调与取消; 文本上限 8000 字符 (插件内分句), 单次输出上限 100 MB; `speech.speak` 走宿主 `tts(text, {engine: <插件包名>})`, 宿主 TTS 代码零改动. | P4.3 |
| D21 | 契约模块 `plugin-api/speech-api` | 包 `org.autojs.plugin.speech.api`; 常量 `SpeechPluginActions.SPEECH = "org.autojs.plugin.SPEECH"`, `SpeechPluginIds` (官方 ID / 包名, `ENGINE = "speech"`, `CATEGORY = "speech"`), `SpeechContract.CONTRACT_VERSION = 1`, `SpeechCapabilityKeys`, `SpeechLimits`, `SpeechOptionKeys` / `SpeechResultKeys` / `SpeechModelKeys` / `SpeechErrorCodes`; AIDL `ISpeechPlugin` (`getInfo` 居首), `ISpeechSession`, `ISpeechSessionCallback`, `ISpeechWakeCallback`, `ISpeechProgressCallback`; 请求与结果以 `Bundle` 传输, 不走 JSON 字符串. 草案见附录 C.1. | P1.1 |
| D22 | 宿主绑定与后台麦克风 | `SpeechPluginHost` 基于 `core/plugin/AidlPluginHost.kt`, 绑定 flags 在既有 `BIND_AUTO_CREATE` (+ API 34 `BIND_ALLOW_ACTIVITY_STARTS`) 之上追加 `BIND_INCLUDE_CAPABILITIES` (API 29+) 与 `BIND_IMPORTANT`, 使插件进程继承宿主前台服务的 while-in-use 麦克风能力; P1.2 在 API 31 / 33 / 35 / 36 真机验证熄屏与宿主在后台时 `AudioRecord` 不被静音. 不成立时的备选: 插件自身 microphone 前台服务, 由宿主在前台经 Wake 协议拉起. | P1.2 |
| D23 | 模型存储 | 插件私有 `filesDir/models/<modelId>/<versionCode>/` + `manifest.json` (每文件 SHA-256, 来源, 许可); 下载到 `cacheDir/downloads/<opId>/`, 归档与文件级校验全部通过后原子 rename 激活; 同一 kind 可装多个模型, 每 kind 一个默认 (DataStore); 导入本地文件 (SAF, 目录或 `tar.bz2` / `zip`) 走同一校验, 目录里有的模型按目录身份登记, 没有的以 "用户导入" 身份登记并由用户指定 kind 与模型类型; 删除与回收未引用文件; 不申请存储权限. | P2.3 |
| D24 | 目录与双来源 (D9 细化) | 插件内置发布时的目录快照与 `speech-models` 公钥; 运行时可从 Shared-Assets `main` 的 `speech-models/catalogs/catalog-v1.json` + `.sig.json` 刷新 (ECDSA P-256 / SHA-256 分离签名, 目录版本单调, 拒绝回退); 每条目录含 `mirror` (Shared-Assets Release 不可变 URL, size, sha256), 可选 `upstream` (sherpa-onnx Release URL, size, sha256, 归档成员到安装文件的映射), `files[]` (安装后文件名, size, sha256; 最终校验依据), `license` (`spdx` 或 `LicenseRef-*`, `commercialUse`, 10 语言 notice key), `kind`, `modelType`, `languages`, `sampleRate`, `speakers`, `recommended`. mirror 失败 (HTTP 错误, 超时, 校验失败) 自动回退 upstream; 两条路径都先校验归档 SHA-256 再校验文件级 SHA-256. | P2.1 |
| D25 | Shared-Assets 布局 | 新类别 `speech-models/`: `sources/<modelId>/<versionCode>/` 只放 `source.json` (上游 Release tag, 资源名, 上游 SHA-256, 重打包成员列表, 许可, 摘要), `LICENSE*` 与 `MODEL_CARD.md`, 不入库模型二进制 (单模型 30-350 MB 超过 GitHub 100 MB 文件限制, 仓库明确不用 LFS; 这是与 `codegen-models` "包入库" 的有意偏离, 需维护者确认); `tools/speech-model-distribution/` (Python 3.12 标准库: 从上游归档复现重打包并计算摘要, 生成候选目录, 校验, 发布); 工作流 `publish-speech-models.yml`; 标签 `speech-models-vN`; 独立签名密钥 (维护者离线生成, 仅公钥入库). 草案见附录 C.3. | P2.1 |
| D26 | 界面技术 | Jetpack Compose + Material 3 (与 3-Maple Image / 3-Block Compose 同代); 语言, 夜间模式, 主题色与启动器图标四项按独立设置规范, 默认跟随宿主; 首页 = 状态卡 (运行时版本, 已装模型, 麦克风权限) + 模型列表 (按 kind 分组, 下载 / 导入 / 删除 / 设为默认, 许可提示) + "试听试说" 入口; 设置页与发行历史独立; 更新检查沿用 3-Stone AI 的更新对话框. | P5.1 |
| D27 | 错误码 | 脚本侧异常携带 `code`: `PLUGIN_UNAVAILABLE`, `PLUGIN_DISABLED`, `PLUGIN_INCOMPATIBLE`, `PERMISSION_DENIED` (插件未获麦克风权限), `MODEL_MISSING`, `MODEL_DOWNLOAD_FAILED`, `MODEL_CHECKSUM_MISMATCH`, `MODEL_LICENSE_NOT_ACCEPTED`, `INVALID_OPTIONS`, `UNSUPPORTED_FORMAT`, `LIMIT_EXCEEDED`, `SESSION_BUSY` (麦克风被占用), `SESSION_CANCELLED`, `ENGINE_FAILED`, `IO_FAILED`. 形态沿用 `epub` 模块的 `EpubErrorMapper` 与 `isAvailable()`; 缺插件文案复用宿主 `error_missing_required_plugin_for_module_label`. | P1.4 |
| D28 | `REQUIRES_HOST_VERSION` 回填规则 | 取 "P1 宿主改动全部落地后" 的宿主 versionCode (`VERSION_BUILD + 1652`), 当前基线 5448, 实际值必大于它; P1.3 先写临时值 (当时的 `VERSION_BUILD + 1 + 1652`), P6.4 发布前按宿主最终提交回填并同步 README / 索引清单 / 测试夹具. `PluginInfo.capabilities` 同时上报 `speechContractVersion=1`, `speechFeatures=asr,asrStreaming,vad,kws,tts,ttsSystemEngine`, `speechCatalogVersion=<内置目录版本>`. | P1.3 / P6.4 |
| D29 | 权限 | 插件声明 `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE` (仅独立界面的试听与 1.1.0 助手使用前台服务; 脚本路径依赖 D22 的绑定能力), `INTERNET` (仅模型下载与目录刷新), `POST_NOTIFICATIONS` (下载进度通知); 不申请存储权限. 脚本首次调用麦克风而插件未获 `RECORD_AUDIO` 时报 `PERMISSION_DENIED`, 宿主提示打开插件界面一键申请. | P0.1 |
| D30 | 线程与资源 | 插件进程内 ASR, TTS, 下载各一条专用线程; 模型按需加载, 空闲 5 分钟 (可配置) 后释放; Binder 主路径不做模型加载, `openSession` 立即返回, 加载完成后推送 `ready` 事件, 失败推送 `error`. 多个脚本同时请求同一模型时共享已加载实例. | P3.1 |
| D31 | 体积预算 | 每 ABI release APK <= 20 MB (AAR 单 ABI 约 12-15 MB + VAD 2.2 MB + Compose); Universal <= 60 MB. 超出时先裁 Compose 依赖与 R8 配置, 再考虑 AAR 瘦身. | P0.2 |
| D32 | 版本与许可 | 插件 `VERSION_NAME` 从 1.0.0 开始, `VERSION_BUILD` = 提交数 + 1; 仓库许可 MPL-2.0 (与官方插件一致), `THIRD_PARTY_NOTICES.md` 记录 sherpa-onnx (Apache-2.0), onnxruntime (MIT), silero VAD (MIT), Compose / AndroidX (Apache-2.0), 宿主 AAR (MPL-2.0); 模型许可逐条写在目录中, 不写在仓库许可里. | P0.1 |

---

## 3. 范围与非目标

范围 (1.0.0):

- 脚本 API `speech` 全集 (附录 B) 与 10 种语言的文档, d.ts, 离线文档, Ace 声明同步.
- 流式与文件识别, VAD, 唤醒词, 神经网络 TTS (系统引擎 + Binder), 模型目录与下载 / 导入 / 管理.
- 独立界面: 首页 (模型管理), 设置, 试听试说, 发行历史, 更新检查.
- 宿主契约 `speech-api`, 插件中心注册, 安装向导条目, 官方索引登记; Shared-Assets `speech-models` 类别与 `speech-models-v1` 发布.
- 宿主样例脚本: 听写到文件, 语音命令交给 `ai.agent.run`, 唤醒词 + 听写, TTS 播报.

非目标 (不做或排到 1.1.0 之后, 评估记录见附录 G):

- 内置语音助手 (常驻唤醒, 悬浮麦克风, 直调 3-Stove Agent) (1.1.0, P7).
- 在线识别 / 合成服务对接 (云 ASR / TTS API).
- 标点恢复模型, 说话人识别 / 分离, 语种识别, 音频事件标签 (1.1.0 候选).
- 非 WAV 音频解码 (MP3 / AAC / OGG), 系统音频 / 通话录音抓取.
- 自行编译 sherpa-onnx 或 onnxruntime; NPU (QNN / NNAPI) 加速.
- 宿主 `tts` 模块新增插件 backend (D3 已选系统引擎路线).

---

## 4. 现状诊断

### 4.1 可直接复用的宿主能力

- `tts` 模块: `runtime/api/augment/tts/Tts.kt` (`Augmentable` + `Invokable`, `speak` / `enqueue` / `synthesize` / `ready(engine)` / `engines()` / `voices()` / `languages()`), `runtime/api/tts/TtsBackend.kt` (平台无关边界, 引擎按包名选择), `core/tts/TtsTextChunker.kt`; 第三方系统引擎无需宿主改动即可被 `tts(text, {engine})` 使用 (P4.4 实测).
- 插件发现与绑定: `core/plugin/AidlPluginHost.kt` (`discover` / `selectOrThrow` / `call`, 绑定 flags 在 459-462 行, 已含启用, 授权, 宿主版本门禁与缺插件错误文案); 薄封装先例 `core/plugin/mediainfo/MediainfoPluginHost.kt`.
- 公共契约: `plugin-api/common-plugin-api` (`PluginInfo`, `PluginCapabilityKeys.REQUIRES_HOST_VERSION`, `IPluginInfoProvider`, `AutoJs6HostSettingsContract`); 能力契约模板 `plugin-api/mediainfo-api` (Actions / Ids / CapabilityKeys / OptionKeys / AIDL / consumer-rules); 回调式契约先例 `plugin-api/three-stove-agent-api` (`IThreeStoveAgentRunCallback` 等 oneway 回调).
- 脚本挂载: `ScriptRuntime.kt:1033` `Tts(this, tts).augment(target)`, `:1076` `Mediainfo(this).augment(target)`, `:1079` `Epub(this, epub).augment(target)`; 事件对象先例: `events`, `websocket` 模块的 EventEmitter 用法.
- 错误模型先例: `augment/epub/Epub.kt` 的错误码与 `isAvailable()`.
- 注册表: `core/plugin/center/InstalledPluginRepository.kt` (`queryDeclaredPluginServices` 与 `discoverPackage` 两张表, mediainfo 条目在 222 与 284 行), `core/plugin/center/wizard/PluginInstallWizardCatalog.kt` (`TOOLS` 分类), `AndroidManifest.xml` `<queries>`.
- 下载与模型管理参照: 3-Stone AI (固定版本 HTTPS, 字节数 + SHA-256, SAF 导入, 原子激活, 回收未引用文件, 更新对话框); Shared-Assets `codegen-models/` 与 `tools/codegen-model-distribution/` (目录 schema, 分离签名, 内容寻址文件名, 发布工作流).
- 界面与原生参照: 3-Maple Image / 3-Block Compose (Compose + Material 3, 独立设置规范, Icon Studio 配方), 3-Hanzi CC (4 ABI 拆分, 16 KB 页 CI 任务, JNI 加载检查), MediaInfo (可调用脚本模块与契约模板), 3-Shell Terminal (INFO + 能力 + 管理器三服务).
- 样例对接: `ai.agent.run(task)` (文档 `api/ai.md`), 3-Stove Agent 要求宿主 5298+.

### 4.2 缺口 (需要新建或修改)

- 宿主没有任何语音识别, 唤醒词或 VAD 能力 (`SpeechRecognizer` / `RecognizerIntent` / sherpa / vosk / whisper 在 `app/src/main` 均无引用, 只有 `all_android_classes.json` 索引命中).
- 没有 `speech-api` 契约模块; `InstalledPluginRepository` / 向导目录 / `<queries>` 均无条目; 没有 `speech` 脚本模块, 文档, d.ts 与离线文档.
- `AidlPluginHost` 尚未使用 `BIND_INCLUDE_CAPABILITIES`, 需要新增可选 flag 并验证.
- Shared-Assets 没有语音模型类别, 没有对应 schema, 工具, 工作流与签名公钥.
- 宿主 `tts` 模块对第三方引擎的 `ready(engine)` / `voices()` 行为尚未用自研引擎实测.
- 本仓库尚不存在 (本次会话只落盘路线图).

### 4.3 外部事实

- sherpa-onnx `v1.13.8` 于 2026-09-10 发布 (Apache-2.0), Release 资源含 `sherpa-onnx-1.13.8.aar` (47.8 MB, 4 ABI); Maven Central 存在 `com.k2fsa.sherpa.onnx:sherpa-onnx-android` 坐标, 最新版本号待 P0.2 核实. 上游 Android 预编译示例覆盖流式识别, VAD + 离线识别, 两遍识别, TTS 引擎, 说话人识别, 语种识别, 音频标签, 唤醒词.
- 模型资源挂在上游滚动标签 `asr-models` / `tts-models` / `kws-models` 下, 资源可被替换, 因此 D9 以 Shared-Assets 不可变 Release 为主. 2026-10-16 实测上游归档体积见附录 A.
- 唤醒词为开放词表 (`sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20`), 关键词可带 boost (`:`) 与阈值 (`#`), token 类型支持 BPE, 全拼, 声韵母拼音与音素. SenseVoice 单模型覆盖 zh / en / ja / ko / yue. silero VAD v5 为 2.2 MB.
- Android 后台麦克风政策: 应用在后台只能在持有 microphone 前台服务或被前台进程以 `BIND_INCLUDE_CAPABILITIES` 绑定时访问麦克风 (API 29+); Android 14 起启动 microphone 前台服务本身也受前台可见性限制. D22 以绑定能力为主路径.
- GitHub: 单文件 100 MB 入库限制, Release 资源单个上限 2 GB; Shared-Assets 的既定策略为按类别独立目录 / 清单 / 标签前缀 / 密钥, 不用 LFS, 不作为通用 CDN.
- 平台版本插件当前 `1.9.1`, 建仓时以该值或更新的已发布版本为准.

---

## 5. 目标架构

### 5.1 数据流: 脚本听写与唤醒

```text
speech.listen(options)
  -> augment Speech (参数规范化, 错误映射)
  -> SpeechPluginHost.openSession(options, callback)      --Binder (BIND_INCLUDE_CAPABILITIES)-->  ThreeMacawSpeechService   [插件]
        <- ISpeechSession                                       -> ModelRegistry.acquire(asr 默认模型) (异步加载, ready 事件)
                                                                -> MicrophoneSource (AudioRecord 16 kHz) -> VadGate -> Recognizer (online / offline + VAD 切段)
  <- SpeechSession (EventEmitter): ready, partial, final, endpoint, vad, error, end
speech.wake(keywords, options)
  -> SpeechPluginHost.openWakeSession                       --Binder-->  KeywordSpotter (关键词原文 -> token)
  <- WakeSession: ready, wake {keyword, score}, error, end; options.thenListen 时自动转入听写会话
speech.recognize(file | pcm, options)
  -> SpeechPluginHost.recognizeFile(pfd, options)           --Binder-->  WavDecoder -> VAD 切段 -> OfflineRecognizer (或 online 模型一次跑完)
  <- {text, segments[], language, durationMillis, modelId}
```

### 5.2 数据流: 合成的两条路径

```text
tts("你好", {engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech"})   [宿主 tts 模块, 零改动]
  -> Android TextToSpeech -> ThreeMacawSpeechTtsEngineService (TextToSpeechService)      [插件]
        -> TtsEngineCore: 分句 -> OfflineTts (vits / matcha / kokoro) -> SynthesisCallback.audioAvailable (PCM16, 模型采样率)
speech.speak(text, options)  = 上面的委托 (engine 固定为本插件, options 映射到 tts 选项)
speech.synthesize(text, path, options)
  -> SpeechPluginHost.synthesizeToFile(request, pfd, progress) --Binder--> TtsEngineCore -> WAV 写入宿主给的 pfd
  <- {path, sampleRate, durationMillis, modelId, speakerId}
```

### 5.3 数据流: 模型分发

```text
AutoJs6-Shared-Assets (speech-models)                       sherpa-onnx GitHub Release (asr-models / tts-models / kws-models)
  catalogs/catalog-v1.json + .sig.json  (签名目录)              (上游滚动标签, 备用来源)
  Release speech-models-vN: autojs6-speech-<modelId>-v<versionCode>-<sha8>.tar  (内容寻址, 不可变)
          |                                                              |
          v  mirror (优先)                                                v  upstream (回退)
插件 ModelStore: 下载到 cacheDir/downloads/<opId>/ -> 归档 SHA-256 -> 解包 -> 文件级 SHA-256 -> 原子 rename 到 filesDir/models/<modelId>/<versionCode>/
插件内置: 发布时目录快照 + speech-models 公钥; 运行时刷新目录需签名验证与版本单调
```

### 5.4 包结构

插件 (`app/src/main/java/io/github/supermonster003/autojs6/plugin/three/macaw/speech/`):

```text
ThreeMacawSpeechApplication, WakeActivity
service/
  ThreeMacawSpeechService          ISpeechPlugin.Stub, Bundle <-> core 编解码, 输入边界, 会话表与 linkToDeath
  ThreeMacawSpeechInfoService      IPluginInfoProvider (org.autojs.plugin.INFO)
  PluginRuntimeInfo                PluginInfo 纯数据映射 (供 JVM 测试)
core/
  runtime/SherpaRuntime            AAR 封装, 线程, 模型加载与空闲释放 (D30)
  audio/MicrophoneSource, WavDecoder, Resampler, PcmRing
  asr/StreamingRecognizerSession, OfflineRecognizerSession, VadGate, EndpointPolicy
  kws/KeywordSession, KeywordTokenizer (拼音 / BPE, 视 D18 spike)
  tts/TtsEngineCore, SentenceSplitter, WavWriter
  model/ModelCatalog, CatalogVerifier (签名), ModelStore, Downloader, Importer, LicensePolicy
ttsengine/
  ThreeMacawSpeechTtsEngineService TextToSpeechService (D19), VoiceTable, EngineSettingsActivity 入口
ui/
  HomeActivity, HomeScreen, ModelListScreen, TryoutScreen (试听试说), SettingsActivity, ReleaseHistoryActivity, theme/
data/
  PreferencesStore                 DataStore: 默认模型, 释放时间, 端点参数, 外观四项
```

宿主 (`D:/idea-projects/AutoJs6`):

```text
plugin-api/speech-api/                                      契约 (附录 C.1)
app/src/main/java/org/autojs/autojs/core/plugin/speech/
  SpeechPluginHost, SpeechSessionProxy, SpeechWakeSessionProxy, SpeechErrorMapper
app/src/main/java/org/autojs/autojs/core/plugin/AidlPluginHost.kt   可选 BIND_INCLUDE_CAPABILITIES / BIND_IMPORTANT
app/src/main/java/org/autojs/autojs/runtime/api/augment/speech/
  Speech.kt, SpeechSession.kt, SpeechWakeSession.kt, SpeechModels.kt
app/src/main/java/org/autojs/autojs/runtime/ScriptRuntime.kt        挂载 speech / $speech
app/src/main/java/org/autojs/autojs/core/plugin/center/...          InstalledPluginRepository, PluginInstallWizardCatalog
app/src/main/assets-app/sample/3-Macaw Speech/                       样例脚本
docs/dev/speech-plugin-protocol-v1.md
```

Shared-Assets (`D:/idea-projects/AutoJs6-Shared-Assets`):

```text
speech-models/
  README.md
  sources/<modelId>/<versionCode>/{source.json, MODEL_CARD.md, LICENSE*}   不含模型二进制
  catalogs/catalog-v1.json, catalog-v1.sig.json, history/vN/, pending/vN/
  keys/catalog-signing-public.pem
  schemas/{catalog-v1, signature-v1, model-source-v1}.schema.json
tools/speech-model-distribution/{prepare_sources.py, build_catalog.py, verify_catalog.py, README.md}
.github/workflows/publish-speech-models.yml
```

### 5.5 身份派生表

| 项 | 值 |
| --- | --- |
| `{PROJECT_NAME}` | `AutoJs6-Plugin-Three-Macaw-Speech` |
| `{ROOT_PROJECT_NAME}` | `autojs6-plugin-three-macaw-speech` |
| `{APP_NAME}` (不可翻译) | `3-Macaw Speech` |
| `{APPLICATION_ID}` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.three.macaw.speech` |
| `{PLUGIN_ID}` / `{PLUGIN_ENGINE}` / `{PLUGIN_VARIANT}` | `three-macaw-speech` / `speech` / `default` |
| 身份类 | `ThreeMacawSpeechContract`, `ThreeMacawSpeechService`, `ThreeMacawSpeechInfoService`, `ThreeMacawSpeechTtsEngineService`, `ThreeMacawSpeechApplication`, `Theme.ThreeMacawSpeech`, 测试类同名后缀 |
| 插件中心身份服务 | `ThreeMacawSpeechInfoService`, action `org.autojs.plugin.INFO`, category `three-macaw-speech` |
| 能力服务 | `ThreeMacawSpeechService`, action `org.autojs.plugin.SPEECH`, category `speech`, 受 `org.autojs.permission.PLUGIN` 保护 |
| 系统 TTS 引擎服务 | `ThreeMacawSpeechTtsEngineService`, action `android.intent.action.TTS_SERVICE`, meta-data `android.speech.tts` -> `@xml/tts_engine`, 引擎标签 `3-Macaw Speech` (系统绑定, 不加插件权限) |
| `{CAPABILITY_API}` | `speech-api` (宿主 `plugin-api/speech-api`, AAR 复制到 `libs/speech-api.aar`; 另需 `libs/common-plugin-api.aar`; 全部登记在 `locks/host-api-aars.lock`) |
| 宿主契约包 | `org.autojs.plugin.speech.api` |
| 宿主桥接包 | `org.autojs.autojs.core.plugin.speech` |
| 脚本 API | `speech` (`$speech` 同对象) |
| `{REQUIRES_HOST_VERSION}` | 基线 5448 (宿主 `33a1514b74`), P1.3 写临时值, P6.4 回填 (D28) |
| 向导目录 | `PluginInstallWizardCatalog` `TOOLS` 分类, `official("three.macaw.speech")`, 回退标题 `Three Macaw Speech` (附录 D Q1 可改为新分类) |
| `plugin_description` (zh-Hans) | `离线语音识别, 唤醒词检测, 语音活动检测与神经网络语音合成, 可注册为系统 TTS 引擎` (句尾无点号, 不含 "AutoJs6"; 10 语言在 P0.1 定稿) |
| GitHub 仓库描述 | `Offline speech recognition, wake word detection and neural text-to-speech for AutoJs6 scripts` (功能性描述, 不复述名称) |
| 发布文件名 | `autojs6-plugin-three-macaw-speech-v{VERSION_NAME}-{ABI}-{CRC32}.apk` (4 ABI + `universal`, D14) |
| 索引清单 | `release-manifests/io.github.supermonster003.autojs6.plugin.three.macaw.speech/<versionCode>.json` |
| 图标 | 维护者提供 `three-macaw-ic-launcher-light.png` / `-dark.png` 两张黑白源图, Icon Studio 合成 (P5.5); 源图到位前 P0.1 用临时图案占位并在 README 标注 |
| 许可证 | MPL-2.0 (`LICENSE` 完整文本); `THIRD_PARTY_NOTICES.md` 见 D32 |

---

## 6. 阶段总览

| 阶段 | 版本 | 主题 | 状态 |
| --- | --- | --- | --- |
| P0 | 1.0.0 | 仓库骨架: 构建链, sherpa-onnx AAR 接入, ABI 拆分与 16 KB 校验, 资源, 文档生成器, CI | 未开始 |
| P1 | 宿主 6.8.0 | 宿主契约 `speech-api`, 绑定能力, 注册, 脚本 API `speech` | 未开始 |
| P2 | 资产 + 1.0.0 | Shared-Assets `speech-models` 类别, 精选目录审核, 插件模型存储与目录刷新, `speech-models-v1` 发布 | 未开始 |
| P3 | 1.0.0 | 识别, VAD 与唤醒词: 采集, 流式会话, 文件识别, 唤醒会话, 设备矩阵与性能基线 | 未开始 |
| P4 | 1.0.0 | TTS: 引擎核心, 系统引擎服务, Binder 合成, 宿主 `tts` 互通 | 未开始 |
| P5 | 1.0.0 | 独立界面: 首页与模型管理, 设置, 试听试说, 发行历史, 图标 | 未开始 |
| P6 | 1.0.0 | 文档, 声明, 四仓库同步, 样例, 兼容矩阵, 发布与索引登记 | 未开始 |
| P7 | 1.1.0 | 候选: 内置语音助手, 标点恢复, 说话人识别, 非 WAV 解码, MCP 工具, 更多语言 | 未开始 |

依赖顺序:

```text
P0 (骨架 + AAR)
 -> P1 (宿主契约 + 脚本 API)            P2 (资产类别 + 模型存储) 可与 P1 并行, 只依赖 P0.2 的运行时与 P1.1 的常量草案
     -> P3 (识别 / 唤醒)                依赖 P1 契约, P2.3 模型存储 (设备测试需要已装模型)
     -> P4 (TTS)                        依赖 P2.3; 系统引擎部分不依赖 P1
         -> P5 (界面)                   依赖 P2.3, P3, P4 的核心 (试听试说)
             -> P6 (文档, 发布)         依赖 P1-P5 全部通过与 P2.5 的 speech-models-v1 发布
                 -> P7 (1.1.0)
```

发布门禁 (每个阶段的 "(发布)" 条目都指向本清单):

- R1 `version.properties` 的 `VERSION_NAME` 更新, `VERSION_BUILD` = 提交数 + 1; 10 种语言 `.changelog/lang_*.json` 补齐该版本的 `hint` / `feature` / `fix` / `improvement` / `dependency`, 日期为发布当日.
- R2 `.readme/lang_*.json` (功能, 安装, 模型下载说明, 脚本示例, 兼容性, 常见问题, 第三方致谢), `plugin_instruction` 10 语言, `strings.xml` 描述与 README 一致性校验通过; 界面有变化时重采 `docs/images/screenshots` (API 33 设备或模拟器, 1080 x 2340, 合成素材, 明暗背景可读).
- R3 `py D:/idea-projects/prepush_check.py` 通过 (文档校验, `check_repository.py`, 构建号容差, Gradle 单元测试); 附录 F 设备矩阵中每台在线设备完成冒烟 (安装, 激活, 下载一个识别模型与一个 TTS 模型, `speech.listen` 一次, `speech.wake` 一次, `tts(text, {engine})` 一次, `speech.synthesize` 一次), 记录型号 / API / APK 摘要 / 模型 ID.
- R4 release APK 经 `appendDigestToReleasedFiles` 收集 (4 ABI + Universal), 原生库 16 KB 对齐校验通过, 生成 `SHA256SUMS`, 创建轻量 tag `vX.Y.Z` 与 GitHub Release (APK + SHA256SUMS), 复核下载产物哈希.
- R5 官方索引: `official-repositories.json` 加入 `AutoJs6-Plugin-Three-Macaw-Speech` (首次, 48 -> 49), `release-manifests/.../<versionCode>.json` 登记 (`supportedAbis` 4 项, `requiresHostVersion` 与实际一致), `catalog-icons` 图稿登记, 运行 `tools/generate_official_plugin_index.py` 与索引仓库的 `generate_markdown.py`; Shared-Assets `speech-models-v1` 已发布且插件内置快照与之一致.

---

## P0: 仓库骨架

目标: 可构建, 可测试, 可在 4 个 ABI 上加载 sherpa-onnx 与 VAD 的空壳插件 (无识别能力), 满足 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 全部 MUST.

### P0.1 构建链与身份

- [ ] (插件) 从 3-Hanzi CC (原生 4 ABI) 与 3-Block Compose (Compose, 精简 build-logic) 复制骨架后按 5.5 身份表替换: `settings.gradle.kts` (平台插件 `1.9.1` 或更新的已发布版本, 位于 `includeBuild` 之前), 根 `build.gradle.kts` (`apply false` 声明), `build-logic`, `app/build.gradle.kts` (`resValue` 身份, `appendDigestToReleasedFiles` ABI 版正则), `version.properties` (`VERSION_NAME=1.0.0`).
- [ ] (插件) Manifest: `org.autojs.permission.PLUGIN`, `WAKE_ACTIVITY` 与 `WakeActivity`, `ThreeMacawSpeechInfoService` (INFO), `ThreeMacawSpeechService` (SPEECH, 骨架实现只返回 `getInfo` 与 `getCapabilities`), D29 权限声明, `android:allowBackup="false"`; TTS 引擎服务与界面在 P4 / P5 补齐.
- [ ] (插件) 10 语言 `strings.xml` (`plugin_description`, 首页 / 设置骨架文案), `strings_donottranslate.xml`, 排序与 ASCII 标点扫描; 临时启动器图标 (Icon Studio 以临时图案生成 `mipmap/ic_launcher.png` 系列与 `ic_icon_studio_application`), README 标注 "正式图标待维护者源图".
- [ ] (插件) 复制宿主 `.gitignore` / `sign.properties` / `app/sm003.jks`, `git check-ignore` 验证; `AGENTS.md` 裁剪版 (本地, 忽略); `.readme` / `.changelog` JSON 与模板, `.python/generate_markdown.py` + `--check`, `check_repository.py`.
- [ ] (插件) `LICENSE` (MPL-2.0 全文), `THIRD_PARTY_NOTICES.md` 初稿 (D32), `RIGHTS_AND_TAKEDOWN.md` (沿用 3-Stamp Mail 的短篇双语版本).

### P0.2 sherpa-onnx 接入, ABI 拆分与 16 KB 校验

- [ ] (插件) 核实 Maven Central `com.k2fsa.sherpa.onnx:sherpa-onnx-android` 是否有 `1.13.8` 且 AAR 内 `.so` 与 GitHub Release `sherpa-onnx-1.13.8.aar` 字节一致; 据此在 D13 两条路径中二选一并写入 `locks/` (来源 URL, 版本, SHA-256, 许可).
- [ ] (插件) `splits.abi` 四 ABI + Universal; `verify_native_alignment.py` 检查每个 `.so` 的 ELF `LOAD` 段对齐 (16 KB) 与 APK 内未压缩 / zipalign 状态, 结果写入 `docs/development/native-abi-report.md`; 不合格时按 D14 处理并记录.
- [ ] (插件) R8 / ProGuard: 保留 `com.k2fsa.sherpa.onnx.**` JNI 入口与回调类; release 包在 4 ABI 上 `System.loadLibrary` 成功 (AVD x86 API 24, x86_64 API 35, 真机 arm64, armeabi-v7a 以 32 位 AVD 或真机 32 位模式验证, 做不到时记录 "未执行").
- [ ] (插件) `silero_vad_v5.onnx` 随 APK (assets), `SherpaRuntime` 能创建 `VoiceActivityDetector` 并对内置 `test_silero_vad.wav` 切出预期段数 (instrumentation).
- [ ] (测试) 体积: 每 ABI release APK 与 Universal 实测值写入本节, 对照 D31.

### P0.3 测试与 CI

- [ ] (插件) JVM 单元测试: `PluginRuntimeInfo` 映射 (名称, 描述, 作者, 三个身份字段, 版本, 日期, 4 ABI, 最低宿主版本, 契约版本, features), 选项解析边界.
- [ ] (插件) instrumentation: action + category 只发现一个 Service, 显式 Component 绑定且 descriptor 正确, `getInfo()` 字段, Wake Activity 与 INFO 契约, Manifest exported / permission 审查.
- [ ] (插件) `.github/workflows/build.yml` (JVM, Debug / Release 组装, lint, API 24 x86 与 API 35 x86_64 模拟器契约测试, API 37 16 KB 任务), `markdown.yml` (Windows, `check_markdown.bat`), Python 工具测试.
- [ ] (插件) 平台验收命令 (Temurin 模拟) 通过, 日志只含一段版本决策; `git status --short` 为空, `VERSION_BUILD` 与提交数相差 <= 10.

---

## P1: 宿主契约, 绑定能力与脚本 API

目标: 宿主能发现, 绑定并以 `speech` 模块调用插件; 插件未安装 / 未启用 / 不兼容时合理退化.

### P1.1 契约模块 `plugin-api/speech-api` (D21)

- [ ] (宿主) 常量与 key 对象 (附录 C.1), `SpeechLimits` (会话数, feed 块大小, 文本长度, 文件大小, 关键词数, 热词数), `SpeechErrorCodes`; `consumer-rules.pro`; JVM 测试 (常量稳定性, key 不冲突, 限制值).
- [ ] (宿主) AIDL: `ISpeechPlugin` (`getInfo` 居首, 其后 `getCapabilities`, `listModels`, `installModel`, `cancelOperation`, `removeModel`, `recognizeFile`, `detectVoice`, `openSession`, `openWakeSession`, `synthesizeToFile`), `ISpeechSession`, oneway `ISpeechSessionCallback` / `ISpeechWakeCallback` / `ISpeechProgressCallback`; 每个方法的 nullability, 输入上限, 输出上限, 错误语义, 线程行为与资源所有权写入 `docs/dev/speech-plugin-protocol-v1.md`.
- [ ] (宿主) 生成 `speech-api.aar` 与 `common-plugin-api.aar`, 复制到插件 `libs/` 并锁定 SHA-256 (`locks/host-api-aars.lock`); 插件契约测试断言 action / category / 包名 / 契约版本.

### P1.2 绑定与后台麦克风 (D22)

- [ ] (宿主) `AidlPluginHost` 增加可选绑定参数 `includeCapabilities` (API 29+ 追加 `BIND_INCLUDE_CAPABILITIES`, 可选 `BIND_IMPORTANT`), 默认关闭, 仅 `SpeechPluginHost` 开启; JVM 测试 flags 组合.
- [ ] (宿主) `core/plugin/speech/SpeechPluginHost.kt`: `discover` / `select` / 会话代理 (`SpeechSessionProxy` 持有会话 Binder 与回调 Stub, 宿主侧 `linkToDeath` 插件进程), 操作 ID 与取消, 文件 pfd 的只读 grant.
- [ ] (测试) 真机验证 (API 31 Sony, 33 Sony / Redmi, 35 Xiaomi Pad / OnePlus, 36 Samsung): 宿主脚本在后台 (其他应用在前台, 以及熄屏) 时插件 `AudioRecord` 读到的不是静音帧; 记录每台结果; 不成立的机型按 D22 备选并写入附录 D Q5.

### P1.3 注册, 协议文档, changelog 与版本回填

- [ ] (宿主) `InstalledPluginRepository` 两张表加入 `SpeechPluginHost.ACTION_SPEECH`; `PluginInstallWizardCatalog` 加 `entry(official("three.macaw.speech"), "Three Macaw Speech", TOOLS)` (附录 D Q1); `AndroidManifest.xml` `<queries>` 加包名; R8 规则; 相关 JVM 测试.
- [ ] (插件) `REQUIRES_HOST_VERSION` 临时值 = 当时宿主 `VERSION_BUILD + 1 + 1652` (D28), 同步 README / 测试夹具.
- [ ] (宿主) `docs/dev/speech-plugin-protocol-v1.md` 定稿; 6.8.0 `.changelog/lang_zh-Hans.json` 增加 `speech` 模块与插件条目 (其他语言由维护者同步); 宿主提交只落本地.

### P1.4 脚本 API `speech` (D4, D27)

- [ ] (宿主) `runtime/api/augment/speech/Speech.kt` (`Augmentable`, 方法见附录 B.1), `SpeechSession.kt` / `SpeechWakeSession.kt` (EventEmitter, `stop` / `cancel` / `feed` / `isActive`, 脚本退出时自动关闭), `SpeechModels.kt` (`list` / `install` / `installAsync` / `remove` / `importFile` / `setDefault` / `getDefault`), `SpeechErrorMapper`; `ScriptRuntime` 挂载 `speech` 与 `$speech`.
- [ ] (宿主) 参数规范化与边界: 路径解析 (相对脚本目录), PCM 输入类型 (`ArrayBuffer` / `Int16Array` / `byte[]`), options 校验, `RegExp` 不涉及; `LIMIT_EXCEEDED` 在宿主侧先判.
- [ ] (宿主) 退化: `speech.isAvailable()`; 缺插件抛 `PLUGIN_UNAVAILABLE` 并带安装向导提示; 未启用 / 不兼容走插件中心既有提示; 宿主不保留任何识别或合成实现.
- [ ] (测试) JVM: options 解析, 错误映射, 会话状态机 (ready -> listening -> ended, 重复 stop 幂等); 设备: 用 P0 空壳 + 假模型路径验证 Binder 往返与错误传播 (真实识别在 P3).

---

## P2: 模型目录与分发

目标: 维护者可在 Shared-Assets 复现地生成并签名 `speech-models` 目录与 Release; 插件可下载, 校验, 导入, 管理模型并按签名刷新目录.

### P2.1 Shared-Assets `speech-models` 类别 (D24, D25)

- [ ] (资产) `speech-models/README.md` (布局, 版本与完整性规则, 与 `codegen-models` 的差异: 二进制不入库), `schemas/catalog-v1.schema.json` (附录 C.3), `schemas/signature-v1.schema.json` (复用 codegen 的信封结构), `schemas/model-source-v1.schema.json`.
- [ ] (资产) `tools/speech-model-distribution/`: `prepare_sources.py` (按 `source.json` 下载上游归档到忽略的 `build/`, 校验上游 SHA-256, 按成员列表重打包为 `autojs6-speech-<modelId>-v<versionCode>-<sha8>.tar`, 计算归档与文件级 SHA-256, 可重复运行且字节确定), `build_catalog.py` (生成 `catalogs/pending/vN/catalog-v1.json`), `verify_catalog.py` (schema, 单调版本, 不可变条目, 签名), 单元测试; `README.md` 写明离线签名步骤 (新密钥, 拒绝复用 Ace / codegen 密钥).
- [ ] (资产) `.github/workflows/publish-speech-models.yml`: 输入标签与密钥 ID, 仅上传目录新引入的资源, 发布前核对签名; `markdown.yml` 覆盖新 README.
- [ ] (资产) 维护者离线生成 `speech-models` 签名密钥, 公钥入 `speech-models/keys/catalog-signing-public.pem`, 记录 Key ID 与 SPKI 指纹 (维护者操作, 会话只记录结果).

### P2.2 精选目录审核 (D10, D11)

- [ ] (资产) 附录 A 每条模型的 `source.json`: 上游 Release tag, 资源名, 上游 SHA-256 (下载实测), 重打包成员 (只含 int8 或所需精度, tokens, lexicon, 词典, vocoder, 不含 fp32 重复与测试 wav), 语言, 采样率, 说话人数, `license` 与 `commercialUse`, 模型卡摘要 (10 语言 summary 至少 en / zh-CN).
- [ ] (资产) 许可审核记录 `speech-models/audits/license-audit-v1.json`: 每条模型的权重许可与训练数据许可来源链接, 结论 (`commercialUse` true / false / unknown); `unknown` 的模型按 D11 同样提示.
- [ ] (资产) 生成 `catalogs/pending/v1/catalog-v1.json`, `verify_catalog.py` 通过, 差异审阅后交维护者签名 (P2.5).

### P2.3 插件模型存储 (D23)

- [ ] (插件) `ModelCatalog` (解析, 查询, 推荐模型), `ModelStore` (安装表, 默认模型, 路径解析, 回收), `Downloader` (HTTPS, 超时, 断点续传 `Range`, 进度, 取消, 空间预检, mirror -> upstream 回退), `ArchiveExtractor` (tar / tar.bz2 / zip, 路径穿越防护), `Verifier` (归档与文件级 SHA-256), `Importer` (SAF 目录或归档), `LicensePolicy` (D11 提示与脚本 `acceptLicense`).
- [ ] (插件) Binder `listModels` / `installModel` (返回 opId, 进度回调, 同时只跑一个下载) / `cancelOperation` / `removeModel`; 后台下载以 `FOREGROUND_SERVICE_DATA_SYNC` 前台服务 + 通知进行, 进程被杀后可从断点续传.
- [ ] (测试) JVM: 目录解析与签名验证 (固定向量), 归档解包边界 (路径穿越, 超大成员, 截断), 校验失败 / 空间不足 / 中断 / 损坏恢复 / 原子替换 (本地假 HTTP 服务器); 设备: 真实下载一个小模型 (aishell3 30 MB) 与 mirror 失败回退 upstream 各一次.

### P2.4 目录刷新 (D24)

- [ ] (插件) 内置 `assets/catalog/catalog-v1.json` + `.sig.json` + 公钥 (发布时与 Shared-Assets `main` 完全一致的字节); 刷新: 下载 `main` 的目录与签名 -> 验签 -> 版本单调 -> 原子替换本地缓存; 失败时沿用旧目录并提示; 手动刷新入口在设置页, 不自动联网 (D29).
- [ ] (测试) JVM: 篡改目录 / 篡改签名 / 版本回退 / 公钥不匹配均拒绝; 设备: 一次真实刷新.

### P2.5 `speech-models-v1` 发布 (维护者)

- [ ] (资产) 维护者签名 pending v1 -> 提升到 `catalogs/catalog-v1.json` -> 触发 `publish-speech-models.yml` 上传全部附录 A 资源 -> 核对 Release 资源 SHA-256 与目录一致 -> 插件内置快照更新为该字节.
- [ ] (发布) 记录 Release 链接, 资源数, 总体积, Key ID.

---

## P3: 识别, VAD 与唤醒词

目标: `speech.listen` / `speech.recognize` / `speech.detectVoice` / `speech.wake` 在附录 F 设备上可用, 延迟与资源占用有基线.

### P3.1 音频采集与运行时 (D15, D16, D30)

- [ ] (插件) `MicrophoneSource`: `AudioRecord` (`VOICE_RECOGNITION` 源, 16 kHz, 单声道, 20 ms 帧), 权限检查, 独占与排队 (`SESSION_BUSY`), 读取线程, 静音检测 (连续零帧上报 `warning`), 释放; `SherpaRuntime` 线程与模型生命周期 (加载, 共享, 空闲释放, 内存上报).
- [ ] (插件) 会话表与 `linkToDeath`; 每 token 麦克风会话上限 1, PCM 会话上限 4; 超时 (`timeoutMillis`, `maxDurationMillis`) 与自动结束.
- [ ] (测试) instrumentation: 权限缺失 -> `PERMISSION_DENIED`; 两个会话争用 -> `SESSION_BUSY`; 宿主进程被杀 -> 麦克风 1 秒内释放 (`AudioManager` 活动录音查询).

### P3.2 流式识别会话 (D17)

- [ ] (插件) `StreamingRecognizerSession`: `OnlineRecognizer` + 端点规则 + `partial` / `final` / `endpoint` 事件, 热词 (transducer), `language` 提示 (双语模型忽略), 外部 PCM `feed` 路径 (块大小与速率限制), `finish` 后 flush 最后一段.
- [ ] (插件) 离线模型的 "VAD 切段 + 逐段识别" 会话实现同一回调接口 (段级 `partial`); 模型 kind 不匹配 (把 TTS 模型当 ASR) -> `INVALID_OPTIONS`.
- [ ] (测试) instrumentation (真机): 中文, 英文, 中英混合各一段标准录音 (`test-audio/`, 自录或公开语料, 记录来源与许可) 的词错率粗测; 首字延迟与 RTF 写入 `docs/development/asr-baseline.md`.

### P3.3 文件识别与 VAD (D15)

- [ ] (插件) `WavDecoder` (RIFF 解析, 位深 / 声道 / 采样率, 重采样), `recognizeFile` (VAD 切段 + `OfflineRecognizer`; online 模型则一次跑完), `segments[]` (起止毫秒, 文本, SenseVoice 的语言 / 情感 / 事件标签解析到 `tags`), `detectVoice` 返回语音段.
- [ ] (测试) JVM: WAV 解析边界 (非法头, 截断, 24-bit, float, 双声道); instrumentation: 30 秒与 10 分钟文件, 非 WAV -> `UNSUPPORTED_FORMAT`, 超限 -> `LIMIT_EXCEEDED`.

### P3.4 唤醒词会话 (D18)

- [ ] (插件) spike: 用 1.13.8 Kotlin API 以原文 (中文 "小六小六", 英文 "hey macaw") 创建 `KeywordSpotter` 流, 确认是否需要插件侧 token 化; 结论回填 D18 与附录 D Q4.
- [ ] (插件) `KeywordSession`: 关键词校验 (上限 50, 长度, 字符集), boost / threshold, `wake` 事件 (关键词, 分数, 时间戳), `thenListen` 自动转听写 (复用同一麦克风流, 不重新打开), 连续误唤醒抑制 (最小间隔).
- [ ] (测试) instrumentation: 5 次唤醒命中率与 10 分钟背景音误唤醒次数 (录音回放), 写入 `docs/development/kws-baseline.md`.

### P3.5 设备矩阵与性能基线

- [ ] (测试) 附录 F 每台在线设备: 安装, 激活 (OnePlus 走 ColorOS 激活实测), 下载一个识别模型, `speech.listen` 10 秒, `speech.wake` 一次, 后台熄屏听写 30 秒; 记录内存峰值 (`dumpsys meminfo`), CPU, 电量提示.
- [ ] (测试) 16 KB 页设备 (Samsung API 36 或 API 37 AVD) 一次完整识别.

---

## P4: TTS

目标: 系统设置里能选中 `3-Macaw Speech` 引擎并试听; 宿主 `tts(text, {engine})` 与 `speech.synthesize` 可用.

### P4.1 引擎核心

- [ ] (插件) `TtsEngineCore`: `OfflineTts` 封装 (vits / matcha + vocoder / kokoro 三种配置), 模型加载与释放 (D30), `SentenceSplitter` (中英标点, 最大块长, 保留数字与缩写), `speed` 映射, 说话人选择, `WavWriter`.
- [ ] (测试) instrumentation: 附录 A 五个 TTS 模型各合成一句, 采样率与时长合理, 中英混合在双语模型上不丢字; 单说话人模型传入 speakerId -> 忽略并 `warnings[]`.

### P4.2 系统 TTS 引擎服务 (D3, D19)

- [ ] (插件) `ThreeMacawSpeechTtsEngineService`: `onGetLanguage` / `onIsLanguageAvailable` / `onLoadLanguage` / `onGetVoices` / `onIsValidVoiceName` / `onLoadVoice` / `onGetDefaultVoiceNameFor` / `onSynthesizeText` / `onStop`; `res/xml/tts_engine.xml` (`settingsActivity` 指向插件设置页); Manifest `TTS_SERVICE` intent-filter 与 `android.speech.tts` meta-data; 无模型时 `LANG_MISSING_DATA`.
- [ ] (测试) 系统设置 "文字转语音输出" 可选中引擎, 试听可发声, 语速滑块生效; 第三方应用 (系统 "朗读" 或 TalkBack) 可用; API 28 / 33 / 35 各一台记录.

### P4.3 Binder 合成与 `speech.speak` (D20)

- [ ] (插件) `synthesizeToFile` (请求 Bundle: text, modelId, speakerId, speed; 输出 pfd; 进度回调按句; 取消时删除半写入内容由宿主负责) 与上限校验.
- [ ] (宿主) `speech.synthesize` / `synthesizeAsync` (宿主创建目标文件 pfd, 失败清理), `speech.speak` 委托 `tts` 模块并固定 engine (未安装引擎 -> `PLUGIN_UNAVAILABLE`).
- [ ] (测试) JVM: 请求校验; 设备: 8000 字符长文本合成与取消, 输出 WAV 可被宿主 `media` 播放.

### P4.4 宿主 `tts` 互通

- [ ] (测试) 真机: `tts.engines()` 列出插件, `tts.ready(<包名>)`, `tts.voices()` 含插件 Voice, `tts("你好", {engine})` 与 `tts.synthesize(text, path, {engine})` 成功; 卸载插件后宿主 `tts` 回退系统引擎无异常.

---

## P5: 独立界面

目标: 用户不写脚本也能下载 / 管理模型, 申请权限, 试听试说; 外观与其他独立插件一致.

### P5.1 首页与模型管理 (D26)

- [ ] (插件) `HomeActivity` / `HomeScreen`: 状态卡 (运行时版本, 已装模型数, 麦克风权限与一键申请, 系统 TTS 引擎是否选中), 模型列表 (按 kind 分组, 大小, 语言, 许可标签, 下载 / 暂停 / 取消 / 删除 / 设为默认, 导入入口), 下载进度与通知, D11 许可提示对话框.
- [ ] (测试) instrumentation: 列表渲染 (有 / 无网络, 空目录), 许可提示的确认 / 取消语义, 删除默认模型后的回退.

### P5.2 设置页

- [ ] (插件) 外观四项 (语言, 夜间模式, 主题色, 启动器图标; 默认跟随宿主), 识别 (默认模型, 端点灵敏度, VAD 开关), 合成 (默认模型, 说话人, 语速), 资源 (模型空闲释放时间, 目录刷新, 回收未引用文件), 关于 (版本, 发行历史, 更新检查, 第三方声明); 按 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` 的列表密度, 对话框与保存语义.
- [ ] (测试) API 24 与 35 截图核对, 大字号, RTL (ar), 夜间.

### P5.3 试听试说

- [ ] (插件) `TryoutScreen`: 麦克风实时转写 (partial 灰字 / final 正文), 模型与语言切换, 唤醒词试验 (输入关键词, 显示命中与分数), TTS 试听 (文本框, 模型 / 说话人 / 语速, 播放与保存到文件); 退出页面释放麦克风.
- [ ] (测试) 人工验收 (E4) 两台真机, 截图入 `docs/images/screenshots`.

### P5.4 发行历史与更新检查

- [ ] (插件) `ReleaseHistoryActivity` 读取 `assets/doc/CHANGELOG-<lang>.md`; 更新对话框沿用 3-Stone AI (超时, 取消, 忽略版本, 频率限制, Neutral 按钮打开内置发行历史).

### P5.5 图标

- [ ] (插件) 维护者提供源图 -> `.python/icons/three-macaw-ic-launcher-light.png` / `-dark.png` -> Icon Studio 配方 (几何与颜色按黑白规范) -> 24 项资源 + 四个启动器 alias -> `--check` 确定性通过; 索引 `catalog-icons` 登记图稿 (P6.4).

---

## P6: 文档, 声明, 四仓库同步, 样例与发布

### P6.1 文档 (AutoJs6-Documentation)

- [ ] (文档) `api/speech.md` (模块总览, 方法, options, 事件, 错误码, 模型目录与许可说明, 后台麦克风说明), 类型页 `speechSessionType.md`, `speechWakeSessionType.md`, `speechRecognitionResultType.md`, `speechModelInfoType.md`; 侧栏, 目录与模块表; `tts.md` 增加 "使用 3-Macaw Speech 引擎" 小节; `plugins.md` 插件列表.

### P6.2 声明 (AutoJs6-TypeScript-Declarations, Ace Editor, Offline Docs)

- [ ] (文档) d.ts (附录 B.4 为起点), Ace 声明与补全, Offline Docs 复制文档站 HTML 并重算摘要; 各按其 `AGENTS.md` 处理版本与提交.

### P6.3 README, changelog, 样例与兼容矩阵

- [ ] (插件) `.readme/lang_*.json` 10 语言 (功能, 安装, 模型下载与许可, 系统 TTS 设置, 脚本示例, 兼容性, 常见问题, 致谢 sherpa-onnx / k2-fsa 与模型作者), `plugin_instruction` 10 语言, changelog 1.0.0; 生成与 `--check`.
- [ ] (宿主) `sample/3-Macaw Speech/`: `听写到文件.js`, `语音命令交给 Agent.js` (`speech.wake -> speech.listen -> ai.agent.run`), `唤醒词与听写.js`, `TTS 播报.js`; 样例目录 JSON 登记.
- [ ] (测试) 兼容矩阵 (附录 F) 全部在线设备冒烟记录.

### P6.4 1.0.0 发布

- [ ] (插件) D28 回填 `REQUIRES_HOST_VERSION`; R1-R4.
- [ ] (索引) R5: `official-repositories.json`, `release-manifests`, `catalog-icons`, 索引生成与 Markdown.
- [ ] (发布) 核对 Release 附件 (5 个 APK + SHA256SUMS), 真机安装正式包一次, 记录 E5.

---

## P7: 1.1.0 候选

| 候选 | 说明 | 依赖 |
| --- | --- | --- |
| 内置语音助手 (D8) | 常驻唤醒 (microphone 前台服务, 电量策略), 悬浮麦克风按钮, 识别后经 3-Stove Agent AIDL (`three-stove-agent-api`) 直接下任务, 结果 TTS 播报 | 1.0.0 稳定; Stove Agent 契约评审 |
| 标点恢复 | sherpa-onnx `ct-transformer` 标点模型 (体积较大, 单独目录条目) | P2 目录 v2 |
| 说话人识别 / 验证 | 3D-Speaker 模型, `speech.speaker.*` (注册, 比对), 用于 "只听主人" | 隐私文案 |
| 说话人分离, 语种识别, 音频事件标签 | 上游已有模型与 API | 需求验证 |
| 非 WAV 解码 | `MediaCodec` 解码 MP3 / AAC / OGG 为 PCM | D15 推翻 |
| MCP 工具 | 经 `plugin-tool-api` 暴露 "听一句话" / "朗读" 工具给 MCP Server 与 Agent | 宿主 plugin-tool 发现落地 |
| 更多语言模型 | 日 / 韩 / 粤 (SenseVoice 已含), 俄 / 法 / 西 / 阿 流式模型, 多语 TTS (piper 各语种) | 目录 v2 审核 |
| NPU 加速 | QNN (Qualcomm) 变体 AAR 与模型 | 上游稳定性 |

---

## 附录 A. 1.0.0 精选模型目录 (D10, D11)

上游归档体积为 2026-10-16 从 sherpa-onnx GitHub Release 资源实测 (`tar.bz2`, 多数含 fp32 与 int8 两套及测试 wav); 镜像重打包只含所需精度与文件, 安装后体积在 P2.2 实测后回填. 目录 ID 为草案, P2.2 定稿.

| 目录 ID (草案) | 上游资源 | kind / 类型 | 语言 | 上游归档 | 安装后 (估) | 许可 (待 P2.2 审核) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` | `sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20` | asr, 流式 transducer | zh, en | 487.6 MB | 约 190 MB (encoder.int8 174 MB + decoder 13 MB + joiner.int8 3.1 MB) | Apache-2.0 (icefall); 数据集待审 | 默认流式模型, 支持热词 |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | `sherpa-onnx-streaming-zipformer-zh-int8-2025-06-30` | asr, 流式 transducer | zh | 126.5 MB | 约 160 MB | 待审 | 中文更新模型 |
| `asr.sense-voice.zh-en-ja-ko-yue.2025-09-09.int8` | `sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2025-09-09` | asr, 离线 (VAD 切段) | zh, en, ja, ko, yue | 158.1 MB | P2.2 实测 | SenseVoice 模型许可待审 | 高精度离线, 带情感 / 事件标签 |
| `asr.zipformer-streaming.en.2023-06-26.int8` | `sherpa-onnx-streaming-zipformer-en-2023-06-26-mobile` | asr, 流式 transducer | en | 291.3 MB | 约 70 MB (encoder.int8 68 MB) | Apache-2.0 (icefall); 数据集待审 | 英文轻量 |
| `tts.vits.melo.zh-en` | `vits-melo-tts-zh_en` | tts, vits | zh, en | 159.3 MB | P2.2 实测 | MIT (MeloTTS) 待审 | 1 说话人, 中英混读 |
| `tts.kokoro.multi-lang.v1_1` | `kokoro-multi-lang-v1_1` | tts, kokoro | zh, en 及其他 | 347.9 MB | P2.2 实测 | Apache-2.0 | 103 说话人 |
| `tts.vits.icefall.zh-aishell3` | `vits-icefall-zh-aishell3` | tts, vits | zh | 30.1 MB | P2.2 实测 | Apache-2.0 (AISHELL-3) 待审 | 174 说话人, 轻量, 设备测试首选 |
| `tts.vits.piper.en_US-lessac-medium` | `vits-piper-en_US-lessac-medium` (或 `-int8` 20.0 MB) | tts, vits | en | 64.1 MB | P2.2 实测 | MIT (piper); 数据集待审 | 1 说话人 |
| `tts.matcha.icefall.zh-baker` | `matcha-icefall-zh-baker` + vocoder (`vocos-22khz-univ` 或 `hifigan_v2`) | tts, matcha | zh | 72.0 MB + vocoder | P2.2 实测 | `commercialUse=false` (DataBaker 语料仅限非商用) | D11 提示 |
| `kws.zipformer.zh-en.2025-12-20` | `sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20` | kws | zh, en | 31.4 MB | P2.2 实测 | Apache-2.0 待审 | 开放词表 |
| `vad.silero.v5` | `silero_vad_v5.onnx` | vad | 任意 | 2.2 MB | 随 APK | MIT | 不在下载目录, 仅登记 |

---

## 附录 B. 脚本 API 草案

### B.1 方法

| 方法 | 返回 | 说明 |
| --- | --- | --- |
| `speech.isAvailable()` | `boolean` | 插件已安装, 已启用且版本兼容 |
| `speech.capabilities()` | `object` | `{contractVersion, features[], catalogVersion, engineName}` |
| `speech.listen(options?)` | `SpeechSession` | 打开麦克风听写会话 (或 `source: "external"` 的 PCM 会话) |
| `speech.recognize(source, options?)` | `SpeechRecognitionResult` | 识别 WAV 文件路径或 PCM (`ArrayBuffer` / `Int16Array` + `sampleRate`), 同步 |
| `speech.recognizeAsync(source, options?)` | `Promise<SpeechRecognitionResult>` | 异步变体 |
| `speech.detectVoice(source, options?)` | `{segments: [{startMillis, endMillis}]}` | VAD 切段 |
| `speech.wake(keywords, options?)` | `SpeechWakeSession` | 唤醒词会话 |
| `speech.synthesize(text, path, options?)` | `{path, sampleRate, durationMillis, modelId, speakerId}` | 合成到 WAV 文件, 同步; `synthesizeAsync` 返回 Promise |
| `speech.speak(text, options?)` | 与 `tts()` 相同 | 委托宿主 `tts`, engine 固定为本插件 |
| `speech.models.list(filter?)` | `SpeechModelInfo[]` | `filter: {kind, language, installed}` |
| `speech.models.install(id, options?)` | `SpeechModelInfo` | 同步下载并安装, `options.onProgress(done, total)`, `acceptLicense`; `installAsync` 返回 Promise |
| `speech.models.importFile(path, options?)` | `SpeechModelInfo` | 导入本地目录或归档 |
| `speech.models.remove(id)` | `boolean` | 删除已安装模型 |
| `speech.models.setDefault(kind, id)` / `getDefault(kind)` | `SpeechModelInfo` | 每 kind 默认模型 |
| `speech.models.refreshCatalog()` | `{version, updated}` | 按签名刷新目录 |

### B.2 options

`listen`:

```text
model            string      识别模型 ID, 默认该 kind 的默认模型
language         string      语言提示 (zh / en / ja / ko / yue), 双语模型忽略
source           "mic" | "external"   默认 mic
sampleRate       number      source=external 时的输入采样率 (默认 16000, 只接受 16000)
vad              boolean     麦克风流是否经 VAD 门控 (默认 true)
silenceEndMillis number      端点静音时长 (默认 1200)
maxDurationMillis number     单次会话上限 (默认 300000, 最大 1800000)
timeoutMillis    number      无语音自动结束 (默认 15000, 0 为不限)
hotwords         string[]    热词 (transducer 模型), 上限 200
punctuation      boolean     保留模型输出的标点 (SenseVoice), 默认 true
```

`wake`:

```text
keywords         string[] | {keyword, boost?, threshold?}[]   上限 50
model            string      kws 模型 ID
thenListen       boolean | object   唤醒后自动转听写, object 为 listen 的 options
minIntervalMillis number     连续唤醒最小间隔 (默认 1500)
```

`synthesize` / `speak`:

```text
model            string      TTS 模型 ID
speaker          number | string   说话人 ID 或名称
speed            number      0.5 - 2.0, 默认 1.0
format           "wav"       1.0.0 只支持 wav
```

### B.3 事件与返回对象

`SpeechSession` (EventEmitter):

```text
事件: ready, partial {text, segmentIndex}, final {text, segmentIndex, startMillis, endMillis, tags?}, endpoint, vad {active}, warning {code, message}, error {code, message}, end {reason: "stopped" | "timeout" | "maxDuration" | "cancelled" | "error", text}
方法: stop() (flush 并结束), cancel(), feed(pcm) (仅 external), isActive(), result() (累计文本)
属性: id, modelId, startedAt
```

`SpeechWakeSession` (EventEmitter): `ready`, `wake {keyword, score, timestampMillis, listen?: SpeechSession}`, `warning`, `error`, `end`; 方法 `stop()`, `isActive()`.

`SpeechRecognitionResult`: `{text, segments: [{startMillis, endMillis, text, language?, tags?: {emotion?, event?}}], language, durationMillis, modelId, warnings: [{code, message}]}`.

`SpeechModelInfo`: `{id, kind: "asr" | "tts" | "kws" | "vad", modelType, name, languages[], sizeBytes, installedBytes?, installed, isDefault, recommended, version, license: {spdx, commercialUse, noticeUrl}, speakers?, sampleRate?, source: "catalog" | "imported"}`.

错误: 异常对象带 `code` (D27) 与 `message`; Promise 变体以同样的异常拒绝.

### B.4 d.ts 草案 (节选)

```ts
declare namespace speech {
    function isAvailable(): boolean;
    function listen(options?: SpeechListenOptions): SpeechSession;
    function recognize(source: string | ArrayBuffer | Int16Array, options?: SpeechRecognizeOptions): SpeechRecognitionResult;
    function recognizeAsync(source: string | ArrayBuffer | Int16Array, options?: SpeechRecognizeOptions): Promise<SpeechRecognitionResult>;
    function detectVoice(source: string | ArrayBuffer | Int16Array, options?: SpeechVadOptions): SpeechVoiceSegments;
    function wake(keywords: Array<string | SpeechKeyword>, options?: SpeechWakeOptions): SpeechWakeSession;
    function synthesize(text: string, path: string, options?: SpeechSynthesizeOptions): SpeechSynthesisResult;
    function synthesizeAsync(text: string, path: string, options?: SpeechSynthesizeOptions): Promise<SpeechSynthesisResult>;
    function speak(text: string, options?: SpeechSpeakOptions): TtsUtterance;
    const models: SpeechModels;
}
declare const $speech: typeof speech;
```

### B.5 示例

```js
// 听写到文件
let session = speech.listen({ language: "zh", silenceEndMillis: 1500 });
session.on("partial", (e) => console.log("...", e.text));
session.on("final", (e) => files.append("./dictation.txt", e.text + "\n"));
session.on("end", (e) => console.log("结束:", e.reason));
sleep(30000);
session.stop();

// 唤醒词 -> 听写 -> 交给 3-Stove Agent
let wake = speech.wake(["小六小六"], { thenListen: { silenceEndMillis: 1200 } });
wake.on("wake", (e) => {
    e.listen.on("final", (r) => {
        console.log("指令:", r.text);
        ai.agent.run(r.text);
    });
});

// 合成并播报
speech.synthesize("任务已完成", "./done.wav", { model: "tts.vits.icefall.zh-aishell3", speaker: 10 });
speech.speak("任务已完成");
tts("Task completed", { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" });

// 模型管理
speech.models.list({ kind: "asr" }).forEach((m) => console.log(m.id, m.installed, m.sizeBytes));
speech.models.install("asr.zipformer-streaming.zh-en.2023-02-20.int8", { onProgress: (done, total) => console.log(done, total) });
```

---

## 附录 C. 契约草案

### C.1 `plugin-api/speech-api`

```text
org.autojs.plugin.speech.api
  SpeechPluginActions      SPEECH = "org.autojs.plugin.SPEECH"
  SpeechPluginIds          OFFICIAL_PLUGIN_ID = "three-macaw-speech", OFFICIAL_PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.macaw.speech", ENGINE = "speech", CATEGORY = "speech"
  SpeechContract           CONTRACT_VERSION = 1
  SpeechCapabilityKeys     CONTRACT_VERSION, FEATURES, CATALOG_VERSION, MAX_PCM_SESSIONS, TTS_ENGINE_PACKAGE
  SpeechLimits             MAX_MIC_SESSIONS_PER_TOKEN = 1, MAX_PCM_SESSIONS_PER_TOKEN = 4, MAX_FEED_BYTES = 1 MB, MAX_AUDIO_FILE_BYTES = 200 MB, MAX_AUDIO_MILLIS = 1800000, MAX_SYNTH_TEXT_CHARS = 8000, MAX_SYNTH_OUTPUT_BYTES = 100 MB, MAX_KEYWORDS = 50, MAX_HOTWORDS = 200
  SpeechOptionKeys / SpeechResultKeys / SpeechModelKeys / SpeechErrorCodes   Bundle key 与错误码常量 (与附录 B 字段一一对应)

interface ISpeechPlugin {
    PluginInfo getInfo();
    Bundle getCapabilities();
    Bundle listModels(in Bundle query);
    int installModel(in Bundle request, ISpeechProgressCallback callback);      // 返回 opId
    void cancelOperation(int opId);
    Bundle removeModel(in Bundle request);
    Bundle recognizeFile(in ParcelFileDescriptor audio, in Bundle options);
    Bundle detectVoice(in ParcelFileDescriptor audio, in Bundle options);
    ISpeechSession openSession(in Bundle options, ISpeechSessionCallback callback);
    ISpeechSession openWakeSession(in Bundle options, ISpeechWakeCallback callback);
    int synthesizeToFile(in Bundle request, in ParcelFileDescriptor output, ISpeechProgressCallback callback);   // 返回 opId
}
interface ISpeechSession { void feed(in byte[] pcm16); void finish(); void cancel(); boolean isActive(); Bundle snapshot(); }
oneway interface ISpeechSessionCallback { void onReady(in Bundle info); void onPartial(in Bundle r); void onFinal(in Bundle r); void onEndpoint(); void onVoiceActivity(in Bundle v); void onWarning(in Bundle w); void onError(in Bundle e); void onEnd(in Bundle e); }
oneway interface ISpeechWakeCallback { void onReady(in Bundle info); void onWake(in Bundle hit); void onWarning(in Bundle w); void onError(in Bundle e); void onEnd(in Bundle e); }
oneway interface ISpeechProgressCallback { void onProgress(int opId, long done, long total); void onCompleted(int opId, in Bundle result); void onFailed(int opId, in Bundle error); }
```

AIDL 演进规则: 已发布后保持事务顺序, 新方法追加在末尾, 经 `CONTRACT_VERSION` 协商; 宿主先读 `getCapabilities` 再调用可选方法.

### C.2 系统 TTS 引擎 Manifest 片段

```xml
<service
    android:name=".ttsengine.ThreeMacawSpeechTtsEngineService"
    android:exported="true"
    android:label="@string/app_name">
    <intent-filter>
        <action android:name="android.intent.action.TTS_SERVICE" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    <meta-data android:name="android.speech.tts" android:resource="@xml/tts_engine" />
</service>
```

### C.3 Shared-Assets `speech-models` 目录条目 (草案)

```json
{
  "schemaVersion": 1,
  "catalogVersion": 1,
  "repository": "SuperMonster003/AutoJs6-Shared-Assets",
  "models": [
    {
      "id": "tts.vits.icefall.zh-aishell3",
      "versionCode": 1,
      "kind": "tts",
      "modelType": "vits",
      "name": {"en": "VITS AISHELL-3 (Chinese, 174 speakers)", "zh-CN": "VITS AISHELL-3 (中文, 174 说话人)"},
      "languages": ["zh"],
      "sampleRate": 8000,
      "speakers": 174,
      "recommended": true,
      "license": {"spdx": "Apache-2.0", "commercialUse": true, "noticeKey": "license_apache_2_0"},
      "mirror": {"url": "https://github.com/SuperMonster003/AutoJs6-Shared-Assets/releases/download/speech-models-v1/autojs6-speech-tts.vits.icefall.zh-aishell3-v1-<sha8>.tar", "size": 0, "sha256": "<...>"},
      "upstream": {"url": "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2", "size": 31560000, "sha256": "<...>", "members": {"vits-icefall-zh-aishell3/model.onnx": "model.onnx", "vits-icefall-zh-aishell3/tokens.txt": "tokens.txt", "vits-icefall-zh-aishell3/lexicon.txt": "lexicon.txt"}},
      "files": [{"path": "model.onnx", "size": 0, "sha256": "<...>"}, {"path": "tokens.txt", "size": 0, "sha256": "<...>"}, {"path": "lexicon.txt", "size": 0, "sha256": "<...>"}],
      "runtime": {"minSherpaOnnx": "1.13.8"},
      "status": "active"
    }
  ]
}
```

`sampleRate`, `size` 与 `sha256` 均为 P2.2 实测后填入, 上面的数值仅示意. 签名信封与 `codegen-models/schemas/signature-v1.schema.json` 同构, 密钥独立.

---

## 附录 D. 待决事项

| 编号 | 事项 | 选项 | 拍板时点 |
| --- | --- | --- | --- |
| Q1 | 安装向导分类 | (a) `TOOLS` (与 3-Stone AI / 3-Stove Agent 同列, 宿主零枚举改动); (b) 新分类 `SPEECH` 或 `AUDIO` (需宿主枚举, 10 语言标题, 插件中心分组) | P1.3 前 |
| Q2 | sherpa-onnx AAR 来源 | (a) Maven Central 坐标 (若 1.13.8 存在且字节一致); (b) `libs/` + 锁文件 | P0.2 |
| Q3 | 镜像归档格式 | (a) 未压缩 `tar` (模型本身已是二进制, 解包快); (b) `zip` (Android 原生, 可随机访问); (c) 与上游一致 `tar.bz2` | P2.1 |
| Q4 | 唤醒词原文转 token | (a) 上游 Kotlin API 已内置 (spike 结论); (b) 插件内置拼音表 + BPE 编码; (c) 关键词只接受拼音 / 英文 token (不推荐) | P3.4 spike 后 |
| Q5 | 后台麦克风不成立的机型 | (a) 插件自身 microphone 前台服务; (b) 文档声明限制 | P1.2 实测后 |
| Q6 | 系统 TTS 引擎的双语 Voice 报告方式 | (a) 每个说话人报告 zh-CN 与 en-US 两个 Voice; (b) 只报告 zh-CN 并在 `onIsLanguageAvailable` 对 en 返回 `LANG_AVAILABLE` | P4.2 |
| Q7 | 模型下载是否默认允许计量网络 | (a) 默认询问 (与 3-Stone AI 一致); (b) 默认允许 | P2.3 |
| Q8 | 识别结果是否默认写入宿主控制台日志 | (a) 不记录 (隐私); (b) 仅 `final` 级别 debug 日志 | P1.4 |

---

## 附录 E. 证据等级

- E1 代码与配置: 提交 hash 可追溯, 相关文件在仓库中.
- E2 JVM 单元测试: 测试类名, 用例数, 运行命令.
- E3 instrumentation: 测试类名 + 设备型号 + API 级别 (真机或 AVD) + 日期 + 模型 ID.
- E4 人工验收: 设备, 步骤, 结果, 截图路径 (`.bak/visual/<version>/`, 不入库); 识别类条目另附音频来源与许可.
- E5 发布产物: APK 文件名, SHA-256, tag, GitHub Release 链接, 索引清单文件名; 模型发布另附 Shared-Assets 标签, 资源数与 Key ID.

勾选 `[x]` 的条目至少满足 E1 + (E2 或 E3); 涉及界面的条目另需 E4; "(发布)" 条目需 E5. 未执行的验证 (例如某机型的后台麦克风实测, ColorOS 激活) 必须如实写 "未执行".

---

## 附录 F. 基线快照与设备矩阵

基线 (2026-10-16):

| 项 | 值 |
| --- | --- |
| 宿主 | `AutoJs6 master@33a1514b74`, `VERSION_NAME=6.8.0` (未发布), versionCode 5448 |
| 宿主 TTS | `runtime/api/tts/TtsBackend.kt` 平台无关边界, `tts.engines()` / `ready(engine)` / `voices()` 已有 |
| 宿主绑定 | `AidlPluginHost` flags: `BIND_AUTO_CREATE` (+ API 34 `BIND_ALLOW_ACTIVITY_STARTS`), 尚无 `BIND_INCLUDE_CAPABILITIES` |
| sherpa-onnx | `v1.13.8` (2026-09-10), Apache-2.0, `sherpa-onnx-1.13.8.aar` 47.8 MB (4 ABI); Maven `com.k2fsa.sherpa.onnx:sherpa-onnx-android` 版本待核 |
| 模型上游 | GitHub Release 滚动标签 `asr-models` / `tts-models` / `kws-models`, 体积见附录 A |
| Shared-Assets | `main`, 类别 `ace-fonts` (v4 已发布, ECDSA P-256) 与 `codegen-models` (pending v1), 无语音类别 |
| 平台版本插件 | `1.9.1` |
| 参照插件 | 3-Stone AI (模型下载 / 导入 / 更新对话框), 3-Hanzi CC (4 ABI + 16 KB CI), 3-Maple Image / 3-Block Compose (Compose, 独立设置, Icon Studio), MediaInfo (契约模板), 3-Shell Terminal (三服务), Batch Rename (路线图写法, 宿主只落本地提交) |

设备矩阵 (来自 `COMMON_AGENTS.md`; 2026-10-15 起已连接设备可自由安装 / 卸载官方插件; 手机上的权限对话框由维护者操作):

| 设备 | API | 用途 |
| --- | --- | --- |
| Sony G8441 (BH900ASK9E) | 28 | 低端性能基线 (RTF, 内存), 旧系统 TTS 设置页 |
| Sony XQ-AT72 (QV710AF65F) | 31 | 后台麦克风 (D22) 首测, 常规回归 |
| Sony XQ-DQ72 (QV770340J7) | 33 | 常规回归 (曾因 ART JIT 缺陷 ANR, 与应用无关) |
| Redmi 22120RN86C (bek749scrwv4wo8h) | 33 | 小屏布局, 低内存下的模型加载 |
| Xiaomi Pad 23046RP50C (968e9f18) | 35 | 平板布局, Android 15 前台服务限制, 需先授存储权限 (导入测试) |
| OnePlus OPD2413 (63751f63) | 35 | ColorOS 激活实测, 后台限制最严 |
| Samsung SM-A566B / SM-F971B (localhost:端口) | 36 / 37 | 16 KB 页, 最新系统的麦克风与前台服务政策 |
| AVD (`E:\.android\avd\`) | 24, 35, 37 (16 KB) | CI 同款契约测试, x86 / x86_64 原生库加载, minSdk 边界 |

1.0.0 至少覆盖: API 24 AVD, API 28, API 31, API 33, API 35 真机各一台, 16 KB 页一次, 后台麦克风在 API 31 / 33 / 35 / 36 各一次.

---

## 附录 G. 已评估暂缓

| 事项 | 结论 |
| --- | --- |
| 按语言拆变体 APK 内置识别模型 | 单包超过 150 MB, 更新成本高 (第二轮已否决, D5) |
| 宿主录音经共享内存送 PCM | 增加宿主录音管线与传输复杂度, 插件自采集 + 绑定能力更直接 (第二轮已否决, D6); 契约仍保留外部 PCM 输入 |
| 只注册系统 TTS 引擎, 不做 Binder | 脚本拿不到合成进度, 文件输出与模型管理 (第一轮已否决, D3) |
| `asr` + `tts` 双模块或 `voice` 命名空间 | 维护者选择单一 `speech` 模块 (D4) |
| 直接用 sherpa-onnx 上游 URL 作唯一来源 | 滚动标签资源可被替换, 校验会失败; 改为镜像为主, 上游为备 (D9) |
| 不收录非商用模型 | 维护者选择收录并标注 / 提示 (D11) |
| 在线 ASR / TTS 服务 | 与 "离线" 定位冲突, 且 3-Stone AI 的在线 provider 已覆盖云端 AI; 不纳入 |
| 自编译 sherpa-onnx 以瘦身 | 1.0.0 以官方 AAR 为准, 体积预算 D31 可满足; 仅当 16 KB 对齐不合格时重新评估 (D14) |
| 宿主 `tts` 模块插件 backend | 系统引擎路线已覆盖, 避免宿主重复实现 (D3) |
| 标点恢复, 说话人识别, 语种识别, 音频标签 | 上游有模型, 但 1.0.0 先把四项核心做稳 (P7) |
