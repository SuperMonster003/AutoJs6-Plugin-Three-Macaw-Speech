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
7. 2026-10-16 的首个会话只落盘路线图并 `git init` + 首次提交 (D12); 同日 P0 完成骨架, sherpa-onnx 接入与测试 / CI (提交 `9a901ec`, `343591f`, `a828ea1`, `02b2391`, `d44a8ce`, `b62b98d`, 证据见 P0 各条), 之后按 `AGENTS.md` 的提交规则工作 (每笔提交前 `VERSION_BUILD = 提交数 + 1`). 与全部官方插件仓库一致, `AGENTS.md` / `CLAUDE.md` 只在本地维护并被 `.gitignore` 忽略, 不入库.

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
| D10 | 1.0.0 目录范围: 精选 | 识别: 流式 zh-en zipformer int8, 流式 zh int8 (2025; 2026-10-16 维护者决定保留目录条目但仅走上游 URL, 不镜像), SenseVoice int8 官方 2024-07-17 导出 (zh / en / ja / ko / yue 离线, 带标点, FunASR 模型许可 1.1 可商用; 2026-10-16 维护者决定以此替换 2025-09-09 粤语微调版), 流式 en int8; 合成: vits-melo-tts-zh_en, kokoro-multi-lang-v1_1, vits-icefall-zh-aishell3, piper en_US-lessac-medium, matcha-icefall-zh-baker; 唤醒词 zh-en 3M; VAD silero v5. 清单见附录 A, 其他语言按需追加. 两项决定的证据: Shared-Assets `89f04a6` (附录 D Q10 / Q11). |
| D11 | 仅限非商业用途的模型: 收录但标注许可并在下载前提示 | 目录每条带 `license` 与 `commercialUse` 字段; `commercialUse=false` 的模型 (如 matcha-icefall-zh-baker 的 DataBaker 语料) 在界面与脚本下载前给出许可提示, 脚本侧需显式 `acceptLicense: true`. |
| D12 | 本次会话交付 | 新建 `D:/idea-projects/AutoJs6-Plugin-Three-Macaw-Speech`, 只写入 `ROADMAP.md`, 设置仓库级提交身份后做一笔 `docs` 提交. 骨架 (Gradle / Manifest / AGENTS.md 裁剪版) 作为 P0 由后续会话完成. |

---

## 2. 派生技术决策

D13-D32 据第 1 节派生, 进入对应阶段前可推翻 (推翻点见各条), 之后视同固定.

| 编号 | 决策 | 含义 | 推翻点 |
| --- | --- | --- | --- |
| D13 | 运行时: sherpa-onnx v1.13.8 官方 Android AAR | 使用上游预编译 AAR (`libsherpa-onnx-jni.so` + `libonnxruntime.so`, 4 ABI; GitHub Release 资源 `sherpa-onnx-1.13.8.aar` 47.8 MB). 优先 Maven Central `com.k2fsa.sherpa.onnx:sherpa-onnx-android` 的同版本坐标 (P0.2 确认版本存在且字节与 Release 一致); 坐标不可用时把 AAR 放入 `libs/` 并在 `locks/` 记录来源, 版本与 SHA-256. 1.0.0 不自行编译 C++. P0.2 结论 (2026-10-16): Maven Central 没有任何 k2-fsa 坐标, 采用 `libs/sherpa-onnx-1.13.8.aar` + `locks/sherpa-onnx.lock` (附录 D Q2 (b)); AAR 另含的 `libsherpa-onnx-c-api.so` / `libsherpa-onnx-cxx-api.so` 不被 JNI 库依赖, 不打入 APK. | P0.2 |
| D14 | ABI 拆分: 4 ABI + Universal | `splits.abi.include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")`, `PluginInfo.supportedAbis` 与之一致; P0.2 对 AAR 内每个 `.so` 做 16 KB 页对齐与 ZIP 对齐检查, 不合格时 P0.2 改为 `libs/` 内自行重打包或等待上游修复, 不以 "多数设备 4 KB" 为由跳过. P0.2 结论: AAR 内 16 个 `.so` (含 32 位) 的 PT_LOAD 对齐均为 16384, 无需重打包; 原生库按 D31 压缩存储, 由安装器解压, 因此只需 ELF 对齐. | P0.2 |
| D15 | 音频格式 | 内部统一 16 kHz 单声道 float; 外部 PCM 输入为 16 kHz 单声道 PCM16 LE, 每次 `feed` 不超过 1 MB; 文件输入 1.0.0 只收 WAV (PCM 8 / 16 / 24 / 32-bit 与 float, 任意采样率, 单 / 双声道, 插件内重采样并混为单声道), 其他容器报 `UNSUPPORTED_FORMAT`; 单文件上限 200 MB 或 30 分钟. | P3.3 |
| D16 | 会话模型 | `openSession` / `openWakeSession` 返回会话 Binder, 事件经 oneway 回调 Binder 推送; 每个宿主 Binder token 同时最多 1 个麦克风会话 (听写与唤醒互斥, 唤醒会话提供 "唤醒后自动转听写" 模式以免切换), 外部 PCM 会话最多 4 个; 宿主进程死亡 (`linkToDeath`) 或脚本退出时插件释放会话与麦克风. | P3.2 |
| D17 | 识别管线 | 流式模型 (zipformer transducer online) 走 `OnlineRecognizer` + 端点检测 (sherpa rule1 / rule2 / rule3 默认值可调) + 可选 VAD 门控; 离线模型 (SenseVoice, 离线 zipformer) 走 VAD 切段 + `OfflineRecognizer` 逐段识别. 两类模型在 `speech.listen` 上提供同一事件集, 离线模型的 `partial` 为段级而非字级 (文档注明). 热词 (hotwords) 仅 transducer 模型支持, 其余模型收到热词时返回 `warnings[]` 而非抛错. | P3.2 |
| D18 | 唤醒词 | sherpa-onnx `KeywordSpotter` + zipformer KWS zh-en 3M 模型; 脚本传关键词原文 (`string[]` 或 `{keyword, boost, threshold}[]`, 上限 50 条), 插件进程内转换为模型 token (中文拼音, 英文 BPE). P3.4 先做 spike 确认 1.13.8 的 Kotlin API 是否内置原文转换 (上游已集成 cppinyin 与 ssentencepiece, 附录 D Q4), 不内置则插件内置拼音表 (`pinyin-data`, MIT) 与 BPE 编码. | P3.4 |
| D19 | 系统 TTS 引擎映射 | 每个已安装 TTS 模型展开为一组 `Voice` (模型 x 说话人), Voice 名 `<modelId>/<speakerId>`, locale 由模型语言决定 (双语模型同时报告 `zh-CN` 与 `en-US`); `onSynthesizeText` 在插件内按句切分, 分块合成并经 `SynthesisCallback.audioAvailable` 流式回传 (PCM16, 模型采样率); Android 语速 / 音高映射到 sherpa `speed` (音高不支持时忽略并记录); 无已安装模型时 `onIsLanguageAvailable` 返回 `LANG_MISSING_DATA`, 设置页提示下载. | P4.2 |
| D20 | Binder 合成 | `synthesizeToFile` 输出 WAV (PCM16) 到宿主传入的 `ParcelFileDescriptor`, 支持进度回调与取消; 文本上限 8000 字符 (插件内分句), 单次输出上限 100 MB; `speech.speak` 走宿主 `tts(text, {engine: <插件包名>})`, 宿主 TTS 代码零改动. | P4.3 |
| D21 | 契约模块 `plugin-api/speech-api` | 包 `org.autojs.plugin.speech.api`; 常量 `SpeechPluginActions.SPEECH = "org.autojs.plugin.SPEECH"`, `SpeechPluginIds` (官方 ID / 包名, `ENGINE = "speech"`, `CATEGORY = "speech"`), `SpeechContract.CONTRACT_VERSION = 1`, `SpeechCapabilityKeys`, `SpeechLimits`, `SpeechOptionKeys` / `SpeechResultKeys` / `SpeechModelKeys` / `SpeechErrorCodes`; AIDL `ISpeechPlugin` (`getInfo` 居首), `ISpeechSession`, `ISpeechSessionCallback`, `ISpeechWakeCallback`, `ISpeechProgressCallback`; 请求与结果以 `Bundle` 传输, 不走 JSON 字符串. 草案见附录 C.1. | P1.1 |
| D22 | 宿主绑定与后台麦克风 | `SpeechPluginHost` 基于 `core/plugin/AidlPluginHost.kt`, 绑定 flags 在既有 `BIND_AUTO_CREATE` (+ API 34 `BIND_ALLOW_ACTIVITY_STARTS`) 之上追加 `BIND_INCLUDE_CAPABILITIES` (API 29+) 与 `BIND_IMPORTANT`, 使插件进程继承宿主前台服务的 while-in-use 麦克风能力; P1.2 在 API 31 / 33 / 35 / 36 真机验证熄屏与宿主在后台时 `AudioRecord` 不被静音. 不成立时的备选: 插件自身 microphone 前台服务, 由宿主在前台经 Wake 协议拉起. | P1.2 |
| D23 | 模型存储 | 插件私有 `filesDir/models/<modelId>/<versionCode>/` + `manifest.json` (每文件 SHA-256, 来源, 许可); 下载到 `cacheDir/downloads/<opId>/`, 归档与文件级校验全部通过后原子 rename 激活; 同一 kind 可装多个模型, 每 kind 一个默认 (DataStore); 导入本地文件 (SAF, 目录或 `tar.bz2` / `zip`) 走同一校验, 目录里有的模型按目录身份登记, 没有的以 "用户导入" 身份登记并由用户指定 kind 与模型类型; 删除与回收未引用文件; 不申请存储权限. P2.3 实现时的细化 (`8119610`): 清单为版本目录内的 `.manifest.json` (安装路径不以点开头, 不会冲突), 暂存目录改名到位即提交; 下载按归档 SHA-256 存为 `cacheDir/downloads/<sha256>.part` (而非 `<opId>/`), 被杀进程可按内容续传; 默认模型与许可接受记录写在 `filesDir/models/.state.json` (原子写入), 不用 DataStore; 每个模型只保留一个版本. | P2.3 |
| D24 | 目录与双来源 (D9 细化) | 插件内置发布时的目录快照与 `speech-models` 公钥; 运行时可从 Shared-Assets `main` 的 `speech-models/catalogs/catalog-v1.json` + `.sig.json` 刷新 (ECDSA P-256 / SHA-256 分离签名, 目录版本单调, 拒绝回退); 每条目录含 `mirror` (Shared-Assets Release 不可变 URL, size, sha256; 2026-10-16 起可为 null, 表示维护者决定不镜像的仅上游条目, 见附录 D Q10), `upstream[]` (sherpa-onnx Release URL, size, sha256, 归档成员到安装文件的映射; 须完整覆盖 `files[]`, 是仅上游条目的唯一来源), `files[]` (安装后文件名, size, sha256; 最终校验依据), `license` (`spdx` 或 `LicenseRef-*`, `commercialUse`, 10 语言 notice key), `kind`, `modelType`, `languages`, `sampleRate`, `speakers`, `recommended`. mirror 失败 (HTTP 错误, 超时, 校验失败) 自动回退 upstream; 两条路径都先校验归档 SHA-256 再校验文件级 SHA-256. | P2.1 |
| D25 | Shared-Assets 布局 | 新类别 `speech-models/`: `sources/<modelId>/<versionCode>/` 只放 `source.json` (上游 Release tag, 资源名, 上游 SHA-256, 重打包成员列表, 许可, 摘要), `LICENSE*` 与 `MODEL_CARD.md`, 不入库模型二进制 (单模型 30-350 MB 超过 GitHub 100 MB 文件限制, 仓库明确不用 LFS; 这是与 `codegen-models` "包入库" 的有意偏离, 需维护者确认); `tools/speech-model-distribution/` (Python 3.12 标准库: 从上游归档复现重打包并计算摘要, 生成候选目录, 校验, 发布); 工作流 `publish-speech-models.yml`; 标签 `speech-models-vN`; 独立签名密钥 (维护者离线生成, 仅公钥入库). 草案见附录 C.3. | P2.1 |
| D26 | 界面技术 | Jetpack Compose + Material 3 (与 3-Maple Image / 3-Block Compose 同代); 语言, 夜间模式, 主题色与启动器图标四项按独立设置规范, 默认跟随宿主; 首页 = 状态卡 (运行时版本, 已装模型, 麦克风权限) + 模型列表 (按 kind 分组, 下载 / 导入 / 删除 / 设为默认, 许可提示) + "试听试说" 入口; 设置页与发行历史独立; 更新检查沿用 3-Stone AI 的更新对话框. | P5.1 |
| D27 | 错误码 | 脚本侧异常携带 `code`: `PLUGIN_UNAVAILABLE`, `PLUGIN_DISABLED`, `PLUGIN_INCOMPATIBLE`, `PERMISSION_DENIED` (插件未获麦克风权限), `MODEL_MISSING`, `MODEL_DOWNLOAD_FAILED`, `MODEL_CHECKSUM_MISMATCH`, `MODEL_LICENSE_NOT_ACCEPTED`, `INVALID_OPTIONS`, `UNSUPPORTED_FORMAT`, `LIMIT_EXCEEDED`, `SESSION_BUSY` (麦克风被占用), `SESSION_CANCELLED`, `ENGINE_FAILED`, `IO_FAILED`. 形态沿用 `epub` 模块的 `EpubErrorMapper` 与 `isAvailable()`; 缺插件文案复用宿主 `error_missing_required_plugin_for_module_label`. | P1.4 |
| D28 | `REQUIRES_HOST_VERSION` 回填规则 | 取 "P1 宿主改动全部落地后" 的宿主 versionCode (`VERSION_BUILD + 1652`), 当前基线 5448, 实际值必大于它; P1.3 先写临时值 (当时的 `VERSION_BUILD + 1 + 1652`), P6.4 发布前按宿主最终提交回填并同步 README / 索引清单 / 测试夹具. `PluginInfo.capabilities` 同时上报 `speechContractVersion=1`, `speechFeatures=asr,asrStreaming,vad,kws,tts,ttsSystemEngine`, `speechCatalogVersion=<内置目录版本>`. P2.3 起 `speechFeatures` 先上报契约追加的 `models` (模型管理), 其余功能随 P3 / P4 逐项加入; P2.4 起 `speechCatalogVersion=1`. | P1.3 / P6.4 |
| D29 | 权限 | 插件声明 `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE` (仅独立界面的试听与 1.1.0 助手使用前台服务; 脚本路径依赖 D22 的绑定能力), `INTERNET` (仅模型下载与目录刷新), `POST_NOTIFICATIONS` (下载进度通知); P2.3 追加 `FOREGROUND_SERVICE_DATA_SYNC` (模型下载, 导入与目录刷新期间的数据同步前台服务 `ModelOperationService`, 未导出); 不申请存储权限. 脚本首次调用麦克风而插件未获 `RECORD_AUDIO` 时报 `PERMISSION_DENIED`, 宿主提示打开插件界面一键申请. | P0.1 |
| D30 | 线程与资源 | 插件进程内 ASR, TTS, 下载各一条专用线程; 模型按需加载, 空闲 5 分钟 (可配置) 后释放; Binder 主路径不做模型加载, `openSession` 立即返回, 加载完成后推送 `ready` 事件, 失败推送 `error`. 多个脚本同时请求同一模型时共享已加载实例. | P3.1 |
| D31 | 体积预算 | 每 ABI release APK <= 20 MB (AAR 单 ABI 约 12-15 MB + VAD 2.2 MB + Compose); Universal <= 60 MB. 超出时先裁 Compose 依赖与 R8 配置, 再考虑 AAR 瘦身. P0.2 决定: 原生库压缩存储 (`packaging.jniLibs.useLegacyPackaging = true`, 安装时解压到 `nativeLibraryDir`); 若不压缩, arm64-v8a 包内仅 sherpa-onnx + onnxruntime 就有 27.0 MB, Universal 达 108 MB, 必然超预算. 代价是设备上同时占用 APK 与解压后的库 (arm64-v8a 约 12 + 27 MB). 实测见 P0.2. | P0.2 |
| D32 | 版本与许可 | 插件 `VERSION_NAME` 从 1.0.0 开始, `VERSION_BUILD` = 提交数 + 1; 仓库许可 MPL-2.0 (与官方插件一致), `THIRD_PARTY_NOTICES.md` 记录 sherpa-onnx (Apache-2.0), onnxruntime (MIT), silero VAD (MIT), Compose / AndroidX (Apache-2.0), 宿主 AAR (MPL-2.0); 模型许可逐条写在目录中, 不写在仓库许可里. 2026-10-16 许可复核 (维护者选推荐项): 官方 AAR 4 个 ABI 的 `libsherpa-onnx-jni.so` 静态链接 eSpeak NG 与 UCD Tools (GPL-3.0-or-later), 仍保留官方 AAR; 插件源码保持 MPL-2.0, APK 整体按 GPL-3.0-or-later 传播 (MPL-2.0 第 3.3 节允许); `THIRD_PARTY_NOTICES.md` 列出 JNI 库内全部 11 个静态组件, GPL 小节与 Unicode 数据小节; 对应源码随每个 Release 附件提供 (P6.4), GPL 全文与声明进关于页 (P5.2), sherpa-onnx 移除 eSpeak NG 后迁移 (附录 D Q9). 证据见 P0.1. | P0.1 |

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

- sherpa-onnx `v1.13.8` 于 2026-09-10 发布 (Apache-2.0, tag 提交 `11afbd009a7f8c08f4bcf2fc1b265d0df4670fbf`), Release 资源含 `sherpa-onnx-1.13.8.aar` (50129134 字节, 4 ABI, 内置 onnxruntime 1.28.2). P0.2 核实 (2026-10-16): Maven Central 没有 `com.k2fsa.sherpa.onnx:sherpa-onnx-android` (该名称只出现在 JitPack 构建日志的本地安装步骤中); JitPack 的 `com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.8` 是对 Release 资源的转存, SHA-256 相同. 上游 Android 预编译示例覆盖流式识别, VAD + 离线识别, 两遍识别, TTS 引擎, 说话人识别, 语种识别, 音频标签, 唤醒词.
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
| `plugin_description` (zh-Hans) | `离线语音识别, 唤醒词检测, 语音活动检测与神经网络语音合成, 可注册为系统 TTS 引擎` (句尾无点号, 不含 "AutoJs6"; P0.1 定稿, 10 语言见 `values*/strings.xml`) |
| GitHub 仓库描述 | `Offline speech recognition, wake word detection and neural text-to-speech for AutoJs6 scripts` (功能性描述, 不复述名称) |
| 发布文件名 | `autojs6-plugin-three-macaw-speech-v{VERSION_NAME}-{ABI}-{CRC32}.apk` (4 ABI + `universal`, D14) |
| 索引清单 | `release-manifests/io.github.supermonster003.autojs6.plugin.three.macaw.speech/<versionCode>.json` |
| 图标 | 维护者提供 `three-macaw-ic-launcher-light.png` / `-dark.png` 两张黑白源图, Icon Studio 合成 (P5.5); 源图到位前 P0.1 用临时图案占位并在 README 标注 |
| 许可证 | 源码 MPL-2.0 (`LICENSE` 完整文本); APK 整体按 GPL-3.0-or-later 传播 (运行时静态链接 eSpeak NG); `THIRD_PARTY_NOTICES.md` 见 D32 |

---

## 6. 阶段总览

| 阶段 | 版本 | 主题 | 状态 |
| --- | --- | --- | --- |
| P0 | 1.0.0 | 仓库骨架: 构建链, sherpa-onnx AAR 接入, ABI 拆分与 16 KB 校验, 资源, 文档生成器, CI | 已完成 (2026-10-16; 工作流待首次推送后的远端运行) |
| P1 | 宿主 6.8.0 | 宿主契约 `speech-api`, 绑定能力, 注册, 脚本 API `speech` | 已完成 (2026-10-16 宿主本地提交 `b91aef068` .. `51b6bd3b7`, `bd410162c`; 后台麦克风按附录 D Q5 (a) 在 P3.1 补插件前台服务) |
| P2 | 资产 + 1.0.0 | Shared-Assets `speech-models` 类别, 精选目录审核, 插件模型存储与目录刷新, `speech-models-v1` 发布 | 进行中 (P2.1-P2.4 已完成 2026-10-16; P2.5 签名与发布待维护者) |
| P3 | 1.0.0 | 识别, VAD 与唤醒词: 采集, 流式会话, 文件识别, 唤醒会话, 设备矩阵与性能基线 | 未开始 |
| P4 | 1.0.0 | TTS: 引擎核心, 系统引擎服务, Binder 合成, 宿主 `tts` 互通 | 未开始 |
| P5 | 1.0.0 | 独立界面: 首页与模型管理, 设置, 试听试说, 发行历史, 图标 | 进行中 (P5.5 定稿图稿已于 2026-10-16 替换; 启动器 alias 随 P5.1) |
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

- [x] (插件) 从 3-Hanzi CC (原生 4 ABI) 与 3-Block Compose (Compose, 精简 build-logic) 复制骨架后按 5.5 身份表替换: `settings.gradle.kts` (平台插件 `1.9.1` 或更新的已发布版本, 位于 `includeBuild` 之前), 根 `build.gradle.kts` (`apply false` 声明), `build-logic`, `app/build.gradle.kts` (`resValue` 身份, `appendDigestToReleasedFiles` ABI 版正则), `version.properties` (`VERSION_NAME=1.0.0`). 证据: 提交 `9a901ec`; 骨架直接取自同规范下最新建仓的 Batch Rename P0 状态 (`8e6c640`, 其 build-logic 即 3-Block Compose 精简版), ABI 拆分与发布收集取自 3-Hanzi CC; platform-versions 与 native-alignment 均为 1.9.1 (Gradle Plugin Portal 可解析), Gradle 9.5.0, 根脚本以 `gradle.agp.version` / `gradle.kotlin.version` 声明 application 与 Compose 编译器插件 (`apply false`); `PluginRuntimeInfoTest` 冻结 `resValue` 身份与 `rootProject.name`. 偏差: 输出文件名由 `onVariants` 设为 `autojs6-plugin-three-macaw-speech-v{VERSION_NAME}-{ABI}.apk` (Universal 为 `-universal`), `appendDigestToReleasedFiles` 只追加 CRC32, 不再用正则改名; `verifySignedReleaseArtifacts` 先校验签名, 5 个 APK 的名称, 原生库集合与压缩方式; 2026-10-16 本地 `:app:appendDigestToReleasedFiles` 收集到 `releases/` 下 5 个签名 APK (如 `autojs6-plugin-three-macaw-speech-v1.0.0-arm64-v8a-efa438ea.apk`). 本提交的导出树单独 `assembleDebug` 可构建.
- [x] (插件) Manifest: `org.autojs.permission.PLUGIN`, `WAKE_ACTIVITY` 与 `WakeActivity`, `ThreeMacawSpeechInfoService` (INFO), `ThreeMacawSpeechService` (SPEECH, 骨架实现只返回 `getInfo` 与 `getCapabilities`), D29 权限声明, `android:allowBackup="false"`; TTS 引擎服务与界面在 P4 / P5 补齐. 证据: 提交 `343591f`; `service.ThreeMacawSpeechInfoService` (INFO, category `three-macaw-speech`) 与 `service.ThreeMacawSpeechService` (`org.autojs.plugin.SPEECH`, category `speech`) 均 exported 且受 PLUGIN 权限保护, 服务带 `requiresHostVersion` meta-data (5448 占位); D29 五项权限, 无存储权限; `allowBackup=false` + `dataExtractionRules` + `fullBackupContent=false`; `NATIVE_PAGE_ALIGNMENT=16384`; 无启动器入口. `ManifestContractTest` (JVM) 与 `ThreeMacawSpeechContractTest` (5 台设备, 见 P0.3) 验证. 偏差: 宿主契约 `ISpeechPlugin` 在 P1.1 才有, 插件内不得复制 AIDL, 因此能力服务暂返回只携带 `org.autojs.plugin.speech.api.ISpeechPlugin` 描述符的占位 Binder (拒绝全部事务, 与 Batch Rename P0 同法); `getInfo` 由 INFO 服务提供, `getCapabilities` 的内容在 `PluginInfo.capabilities` 中 (`speechContractVersion=1`, `speechFeatures=[]`, `speechCatalogVersion=0`; 键名按 D28 暂存于 `ThreeMacawSpeechContract`, P1.1 改用 `SpeechCapabilityKeys`). AndroidX 合并进来的 `ProfileInstallReceiver` 受系统 `DUMP` 权限保护, 设备测试显式核对.
- [x] (插件) 10 语言 `strings.xml` (`plugin_description`, 首页 / 设置骨架文案), `strings_donottranslate.xml`, 排序与 ASCII 标点扫描; 临时启动器图标 (Icon Studio 以临时图案生成 `mipmap/ic_launcher.png` 系列与 `ic_icon_studio_application`), README 标注 "正式图标待维护者源图". 证据: 提交 `343591f` (字符串与图标), `a828ea1` (README 标注); 11 个 values 目录各 11 条字符串 (描述, 3 个首页分组, 发行历史, 设置与外观四项), 术语沿用 3-Maple Image; `StringResourceParityTest`, `ApplicationTextPunctuationTest` 与 `check_repository.py` 通过. Icon Studio 1.3 配方 (`neutral-v1`, Three 底色 `#FAFAFA` / `#212121`) 从临时 "麦克风 + 声波" 源图 `.python/icons/three-macaw-ic-launcher-light.png` / `-dark.png` 生成 17 项资源, `--check` 通过. 偏差: 不可翻译项全部由 `resValue` 生成, 因此没有 `strings_donottranslate.xml`; 启动器四个 alias 随 P5 界面加入, 本阶段配方 `launcher=false`.
- [x] (插件) 复制宿主 `.gitignore` / `sign.properties` / `app/sm003.jks`, `git check-ignore` 验证; `AGENTS.md` 裁剪版 (本地, 忽略); `.readme` / `.changelog` JSON 与模板, `.python/generate_markdown.py` + `--check`, `check_repository.py`. 证据: 提交 `9a901ec` (`.gitignore`, 以 Batch Rename 的插件版为准, 含宿主规则并补 `*.keystore`, `*.pre-platform-versions.bak`, `.kotlin/`), `a828ea1` (文档源与工具); 签名文件与宿主字节一致, `check_repository.py` 用 `git check-ignore` 确认 `AGENTS.md`, `CLAUDE.md`, `sign.properties`, `app/sm003.jks`, `local.properties` 与迁移备份均被忽略; `generate_markdown.py --check` 为 36 个产物; 本地 `AGENTS.md` 已按本仓库裁剪.
- [x] (插件) `LICENSE` (MPL-2.0 全文), `THIRD_PARTY_NOTICES.md` 初稿 (D32), `RIGHTS_AND_TAKEDOWN.md` (沿用 3-Stamp Mail 的短篇双语版本). 证据: 提交 `a828ea1`; `THIRD_PARTY_NOTICES.md` 列出 sherpa-onnx 1.13.8 (Apache-2.0, tag 提交), onnxruntime 1.28.2 (MIT), silero VAD v5 (MIT), 测试录音 (MIT, 不进 APK), 宿主 AAR (MPL-2.0) 与 AndroidX; `HostApiAarLockTest` / `NativeRuntimeLockTest` 保证其与锁文件一致. 2026-10-16 补充 (D32 许可复核, 与本条记录同一提交): `libsherpa-onnx-jni.so` 与 `libsherpa-onnx-c-api.so` 在 4 个 ABI 上都命中 11/11 条 eSpeak NG 源码字符串 (error.c 状态码文案如 `Wrong version of espeak-ng-data`, dictionary.c 的 `Can't read dictionary file: '%s'`, `/usr/share/espeak-ng-data`, `ESPEAK_DATA_PATH`), piper-phonemize (`Failed to set eSpeak-ng voice`) 与 sherpa 胶水 (`piper-phonemize-lexicon.cc`) 也全部命中, `libonnxruntime.so` 与 `libsherpa-onnx-cxx-api.so` 为 0; 静态链接且符号隐藏 (动态符号表无 `espeak_*` 导出, 先前 "含 espeak 符号" 是 `OfflineSpeaker` 的大小写误匹配); 2026-10-16 构建的 release universal APK 中 4 个 `libsherpa-onnx-jni.so` 结果相同. 来源: sherpa-onnx `v1.13.8` 默认开启 `SHERPA_ONNX_ENABLE_TTS`, 由 `cmake/espeak-ng-for-piper.cmake` 构建 csukuangfj/espeak-ng `ed530aa` 的 `espeak-ng` 与 `ucd`, 由 `cmake/piper-phonemize.cmake` 构建 csukuangfj/piper-phonemize `f3ff95a` (该提交为 MIT, 分叉 master 后来改为 GPL-3.0); 构建路径 `_deps/<name>-src` 另证 kaldi-decoder, kaldifst, OpenFst, simple-sentencepiece. 上游 AAR, `LICENSE` 与 README 都没有第三方许可清单; k2-fsa/sherpa-onnx#3731 (2026-07-08 开启, 未关闭) 计划在 2.0.0 移除 eSpeak NG 与 piper-phonemize, #3969 中上游建议只用 ASR 的用户以 `-DSHERPA_ONNX_ENABLE_TTS=OFF` 自编译. 维护者 2026-10-16 四项均选推荐项: 保留官方 AAR 并声明 GPL, 对应源码随插件 Release 附件提供 (P6.4), GPL 全文先链接 gnu.org 并在关于页内置 (P5.2), 一并补齐其余 8 个宽松许可静态组件. `THIRD_PARTY_NOTICES.md` 因此新增静态组件表 (11 项, 版本取自 sherpa-onnx `v1.13.8` 与 kaldi-decoder `v0.3.0` 的 cmake 固定值), GPL 小节与 Unicode 数据小节.

### P0.2 sherpa-onnx 接入, ABI 拆分与 16 KB 校验

- [x] (插件) 核实 Maven Central `com.k2fsa.sherpa.onnx:sherpa-onnx-android` 是否有 `1.13.8` 且 AAR 内 `.so` 与 GitHub Release `sherpa-onnx-1.13.8.aar` 字节一致; 据此在 D13 两条路径中二选一并写入 `locks/` (来源 URL, 版本, SHA-256, 许可). 证据: 2026-10-16 核实, `https://repo1.maven.org/maven2/com/k2fsa/` 返回 404, Maven Central 搜索 `g:com.k2fsa.sherpa.onnx` 无结果 (只有第三方 `com.bihe0832.android:lib-sherpa-onnx`, 不采用); JitPack 的 `com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.8` 构建日志显示其直接下载 Release 资源, 下载件 SHA-256 与 Release 一致 (`633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96`), 但 JitPack 按需重建, 不是不可变仓库. 结论: 附录 D Q2 选 (b), 提交 `9a901ec` 入库 `libs/sherpa-onnx-1.13.8.aar` (50129134 字节, 与 OpenCV / Paddle OCR 插件入库 50 MB 级 AAR 的先例一致), `locks/sherpa-onnx.lock` 记录 URL, 版本, SHA-256, tag 提交 `11afbd009a7f8c08f4bcf2fc1b265d0df4670fbf` 与 Apache-2.0, 配置阶段校验; `NativeRuntimeLockTest` (JVM) 覆盖.
- [x] (插件) `splits.abi` 四 ABI + Universal; `verify_native_alignment.py` 检查每个 `.so` 的 ELF `LOAD` 段对齐 (16 KB) 与 APK 内未压缩 / zipalign 状态, 结果写入 `docs/development/native-abi-report.md`; 不合格时按 D14 处理并记录. 证据: 提交 `9a901ec` (拆分与打包), `02b2391` (脚本, 报告与 `.python/tests/test_verify_native_alignment.py`); AAR 内 16 个 `.so` 的 PT_LOAD 最小对齐均为 16384 且偏移同余, 包括 armeabi-v7a 与 x86; `libsherpa-onnx-jni.so` 只依赖 `libonnxruntime.so`, 因此 c-api / cxx-api 被排除. APK 内原生库按 D31 压缩 (DEFLATED), 安装器解压, 不要求 ZIP 对齐; 共享门禁 `verifyDebugNativePageAlignment` 以 `strictAbis` = 全部 4 ABI, `pageSize` = 16384 检查 5 个 APK 全部 `ok`, release 5 个 APK 经 `verify_native_alignment.py --apk --expect-compressed` 通过. 合并清单的 `org.autojs.plugin.contract.SUPPORTED_ABIS` 由 native-alignment 插件写为 `arm64-v8a,armeabi-v7a,x86_64,x86`.
- [x] (插件) R8 / ProGuard: 保留 `com.k2fsa.sherpa.onnx.**` JNI 入口与回调类; release 包在 4 ABI 上 `System.loadLibrary` 成功 (AVD x86 API 24, x86_64 API 35, 真机 arm64, armeabi-v7a 以 32 位 AVD 或真机 32 位模式验证, 做不到时记录 "未执行"). 证据: 提交 `9a901ec` (`-keep class com.k2fsa.sherpa.onnx.** { *; }`, AAR 无 consumer 规则, JNI 按名称读字段与调构造函数; 另 `-dontwarn kotlinx.parcelize.Parcelize`), `d44a8ce` (纯平台探针 `SherpaReleaseProbeInstrumentation` 与 `.python/device_release_probe.py`); 2026-10-16 签名的 R8 release 分包逐 ABI 安装后由探针加载 `libsherpa-onnx-jni.so`, 读到 sherpa-onnx 1.13.8 / onnxruntime 1.28.2, 并用随包 VAD 切出 15 段 (语音采样合计 798336, 与参考一致): AVD_API_24 (x86, API 24), AVD_API_33 (x86_64, API 33), AVD_API_35_16K (x86_64, API 35, 16 KB 页), AVD_API_37.1_16K (x86_64, API 37, 16 KB 页), Sony XQ-AT72 (QV710AF65F, API 31) 的 arm64-v8a 与 armeabi-v7a (`adb install --abi armeabi-v7a`, 32 位进程) 全部 `PASS`. 偏差与未执行: "x86_64 API 35" 在本机以 16 KB 页镜像执行, 4 KB 页的 API 35 由 CI 契约任务覆盖 (待首次推送); 额外尝试在 x86_64 AVD 上经 native bridge 翻译运行 arm64 分包时, onnxruntime 在静态初始化 (`pthread_once`) 内崩溃, 属于翻译层场景, x86 设备会安装原生 x86 / x86_64 分包, 不计入验收; 16 KB 页 arm64 真机 (Samsung SM-A566B) 未执行.
- [x] (插件) `silero_vad_v5.onnx` 随 APK (assets), `SherpaRuntime` 能创建 `VoiceActivityDetector` 并对内置 `test_silero_vad.wav` 切出预期段数 (instrumentation). 证据: 提交 `9a901ec` (`app/src/main/assets/vad/silero_vad_v5.onnx`, 2313101 字节, 与 silero-vad `v5.0` 的 `files/silero_vad.onnx` 为同一 Git blob; 测试录音只在 androidTest 资产, 与 silero-vad `tests/data/test.wav` 为同一 blob, 均 MIT, `locks/vad-assets.lock`), `343591f` (`SherpaRuntime.createVad` / `detectSegments`, 参数经 `VadSettings` 校验), `d44a8ce` (`SherpaRuntimeDeviceTest`). 预期段数取自 sherpa-onnx 1.13.8 自身: 其 Windows x64 Python wheel 以同一模型与默认参数 (窗口 512, 阈值 0.5, 最短静音 / 语音 0.25 秒, 最长语音 5 秒) 逐窗口处理 60 秒录音得 15 段; 设备测试要求段数相同且每段起止误差 <= 1024 采样, 在 P0.3 所列 5 个目标上通过 (2026-10-16).
- [x] (测试) 体积: 每 ABI release APK 与 Universal 实测值写入本节, 对照 D31. 证据: 2026-10-16 签名 release (`verifySignedReleaseArtifacts`): arm64-v8a 12211085 字节 (11.6 MiB), armeabi-v7a 11387123 (10.9 MiB), x86_64 13408924 (12.8 MiB), x86 13637723 (13.0 MiB), universal 43804454 (41.8 MiB), 均在 D31 (每 ABI <= 20 MB, Universal <= 60 MB) 之内. 注意: 骨架尚无 Compose 界面代码, R8 移除了未用的 Compose 类, P5 加入界面后须重测.

### P0.3 测试与 CI

- [x] (插件) JVM 单元测试: `PluginRuntimeInfo` 映射 (名称, 描述, 作者, 三个身份字段, 版本, 日期, 4 ABI, 最低宿主版本, 契约版本, features), 选项解析边界. 证据: 提交 `d44a8ce`; `:app:testDebugUnitTest` 7 个类 34 项全部通过: `PluginRuntimeInfoTest` (6, 含映射全部字段, 4 ABI 顺序, 5448, 契约版本 1, 空 features, 目录版本 0, ABI 列表解析拒绝空项与重复项), `VadSettingsTest` (4, VAD 参数边界含 NaN / 无穷, 采样到毫秒换算), `ManifestContractTest` (7), `StringResourceParityTest` (6), `ApplicationTextPunctuationTest` (2), `HostApiAarLockTest` (5), `NativeRuntimeLockTest` (4).
- [x] (插件) instrumentation: action + category 只发现一个 Service, 显式 Component 绑定且 descriptor 正确, `getInfo()` 字段, Wake Activity 与 INFO 契约, Manifest exported / permission 审查. 证据: 提交 `d44a8ce`; `ThreeMacawSpeechContractTest` (6 项: Wake 契约与 `SUPPORTED_ABIS` meta-data, 无启动器入口, INFO 发现 / 绑定 / 真实 Parcel 编组的 `getInfo()` 与 capabilities, SPEECH 占位 Binder 的描述符与拒绝事务, 重复绑定, 导出组件与请求权限审查) 与 `SherpaRuntimeDeviceTest` (4 项: 已装分包的原生库集合与压缩, 版本, VAD 参考切分, 静音无语音段); 2026-10-16 `connectedDebugAndroidTest` 每台 10/10 通过: AVD_API_24 (x86, API 24), AVD_API_33 (x86_64, API 33), AVD_API_35_16K (x86_64, API 35, 16 KB 页), AVD_API_37.1_16K (x86_64, API 37, 16 KB 页), Sony XQ-AT72 (QV710AF65F, arm64-v8a, API 31). 未执行: ColorOS (OnePlus OPD2413) 的真实激活验证.
- [x] (插件) `.github/workflows/build.yml` (JVM, Debug / Release 组装, lint, API 24 x86 与 API 35 x86_64 模拟器契约测试, API 37 16 KB 任务), `markdown.yml` (Windows, `check_markdown.bat`), Python 工具测试. 证据: 提交 `b62b98d`; `build.yml` 含仓库契约, 文档 / 原生报告 `--check`, `python -m unittest discover -s .python/tests`, JVM, Debug / androidTest / Release 组装, `lintDebug`, release 分包原生打包检查, 以及 API 24 x86, API 35 x86_64, API 35 x86_64 16 KB (断言 `PAGE_SIZE`) 与 API 37.1 16 KB (按 3-Stove Agent 先例不阻塞) 的设备测试矩阵; 另有 `markdown.yml` (Windows) 与 `icon-studio.yml` (Windows / Linux). 本地 `lintDebug` 0 错误 (警告: 骨架字符串未使用 10, 图标同形 2, 依赖与 Gradle 新版本提示 3, `enableOnBackInvokedCallback` 1, `ChromeOsAbiSupport` 1 为拆分配置下的误报). 未执行: 远端工作流运行 (仓库尚未推送).
- [x] (插件) 平台验收命令 (Temurin 模拟) 通过, 日志只含一段版本决策; `git status --short` 为空, `VERSION_BUILD` 与提交数相差 <= 10. 证据: `.\gradlew.bat --no-daemon '-Djava.vendor=Eclipse Adoptium' '-Djava.vendor.version=Temurin-21.0.12.1+1' :app:assembleDebug :app:testDebugUnitTest` 通过, 日志只有一段 `Version information for IDE platform and Gradle plugins` (Temurin 21.0.12.1+1, AGP 9.3.3, Kotlin 2.3.20, Gradle 9.5.0), 无 `checkAarMetadata` 错误; 7 笔 P0 提交每笔前 `VERSION_BUILD = 提交数 + 1`, 本提交后 `VERSION_BUILD=8` 等于提交数, 工作区干净, `py D:/idea-projects/prepush_check.py` 通过.

---

## P1: 宿主契约, 绑定能力与脚本 API

目标: 宿主能发现, 绑定并以 `speech` 模块调用插件; 插件未安装 / 未启用 / 不兼容时合理退化.

### P1.1 契约模块 `plugin-api/speech-api` (D21)

- [x] (宿主) 常量与 key 对象 (附录 C.1), `SpeechLimits` (会话数, feed 块大小, 文本长度, 文件大小, 关键词数, 热词数), `SpeechErrorCodes`; `consumer-rules.pro`; JVM 测试 (常量稳定性, key 不冲突, 限制值). 证据: 宿主提交 `1d6280bf8` (本地, 未推送); `plugin-api/speech-api` (包 `org.autojs.plugin.speech.api`) 含 `SpeechPluginActions` (`SPEECH = "org.autojs.plugin.SPEECH"`, `SERVICE_CATEGORY = "speech"`), `SpeechPluginIds` (官方 ID / 包名, `ENGINE`, `CATEGORY`, `REQUIRED_HOST_VERSION_CODE = 5451`), `SpeechContract` (`CONTRACT_VERSION = 1`, 事件, 结束原因, 会话状态, 音频来源, 模型种类与来源, 功能, 格式, 宿主侧超时), `SpeechCapabilityKeys`, `SpeechLimits`, `SpeechOptionKeys` / `SpeechResultKeys` / `SpeechModelKeys`, `SpeechErrorCodes` 与 `consumer-rules.pro`; `:plugin-api:speech-api:testDebugUnitTest` 中 `SpeechContractTest` (7 项: 字面值快照, 键唯一, 闭合集合, 限制值与分层关系, 错误码与 encode / decode) 通过. 偏差: D27 的错误码集合补充宿主侧的 `SESSION_CLOSED`, `TIMEOUT` 与 `INTERNAL`; 能力键另有 `speechEngineName`, `speechEngineVersion` 与 `speechMicrophoneGranted` (仅 `getCapabilities`).
- [x] (宿主) AIDL: `ISpeechPlugin` (`getInfo` 居首, 其后 `getCapabilities`, `listModels`, `installModel`, `cancelOperation`, `removeModel`, `recognizeFile`, `detectVoice`, `openSession`, `openWakeSession`, `synthesizeToFile`), `ISpeechSession`, oneway `ISpeechSessionCallback` / `ISpeechWakeCallback` / `ISpeechProgressCallback`; 每个方法的 nullability, 输入上限, 输出上限, 错误语义, 线程行为与资源所有权写入 `docs/dev/speech-plugin-protocol-v1.md`. 证据: 宿主提交 `1d6280bf8` (AIDL), `51b6bd3b7` (协议文档); `SpeechAidlOrderTest` 冻结 5 个接口的事务顺序与 oneway 标记. 偏差 (D21 推翻点 P1.1 内的追加): `ISpeechPlugin` 在附录 C.1 的 11 个方法之后追加 `importModel`, `setDefaultModel`, `refreshCatalog` (对应附录 B 的 `models.importFile` / `setDefault` / `refreshCatalog`); `openWakeSession` 增加听写回调参数 `listenCallback`, `ISpeechSession` 增加 `finishRound(discard)`, 用于 D16 的 "唤醒后自动转听写" 轮次 (唤醒命中带 `round`, 宿主按轮次配对两个 oneway 通道); 每个应答与完成回调都带 `contractVersion`.
- [x] (宿主) 生成 `speech-api.aar` 与 `common-plugin-api.aar`, 复制到插件 `libs/` 并锁定 SHA-256 (`locks/host-api-aars.lock`); 插件契约测试断言 action / category / 包名 / 契约版本. 证据: 2026-10-16 `:plugin-api:speech-api:assembleRelease` (宿主 `1d6280bf8` 的源码, 至 `51b6bd3b7` 未变) 产出 `speech-api.aar` (SHA-256 `74f29a089c7da86c07920a956455b18e8039def5f006c7fddf78af1db33cd1f2`), 同轮重建的 `common-plugin-api.aar` 与已暂存的 `ed7a7194...` 字节一致; 锁文件, `libs/README.md` 与 `THIRD_PARTY_NOTICES.md` 已同步, `HostApiAarLockTest` (5) 固定产物集合为 `common-plugin-api` 与 `speech-api`. 新增 JVM `SpeechContractAlignmentTest` (4 项: action, category, INFO, 插件 ID / 引擎 / 变体, 包名, `ISpeechPlugin.DESCRIPTOR`, 契约版本, 能力键, 最低宿主构建号, Manifest), 设备 `ThreeMacawSpeechContractTest` 改用 `SpeechPluginActions` 发现能力服务并断言 `ISpeechPlugin.DESCRIPTOR`; `:app:testDebugUnitTest` 8 个类 38 项通过, Sony XQ-AT72 (QV710AF65F, API 31) 设备测试 10/10 通过. 能力服务仍是只带描述符的占位 Binder, `ISpeechPlugin.Stub` 随 P3 / P4 实现.

### P1.2 绑定与后台麦克风 (D22)

- [x] (宿主) `AidlPluginHost` 增加可选绑定参数 `includeCapabilities` (API 29+ 追加 `BIND_INCLUDE_CAPABILITIES`, 可选 `BIND_IMPORTANT`), 默认关闭, 仅 `SpeechPluginHost` 开启; JVM 测试 flags 组合. 证据: 宿主提交 `b91aef068`; 新参数 `includeCapabilities` 与 `important` 默认 false, flags 由纯函数 `AidlPluginHost.bindFlags` 计算 (专用租约与连接池两条绑定路径共用); `AidlPluginHostBindFlagsTest` (6 项: 框架常量值, 未开启时仅 `BIND_AUTO_CREATE`, API 28 / 29 边界, speech 组合, API 34 的 `BIND_ALLOW_ACTIVITY_STARTS` 与外部服务 flag) 通过.
- [x] (宿主) `core/plugin/speech/SpeechPluginHost.kt`: `discover` / `select` / 会话代理 (`SpeechSessionProxy` 持有会话 Binder 与回调 Stub, 宿主侧 `linkToDeath` 插件进程), 操作 ID 与取消, 文件 pfd 的只读 grant. 证据: 宿主提交 `1d6280bf8`; `SpeechPluginHost` (选择按引擎 `speech` / 变体 `default`, 校验契约版本与 `requiresHostVersion >= 5451`, 按功能拒绝未声明的调用, 缺失 / 停用 / 不兼容分别映射 `PLUGIN_UNAVAILABLE` / `PLUGIN_DISABLED` / `PLUGIN_INCOMPATIBLE`), `SpeechSessionProxy` 与 `SpeechWakeSessionProxy` (每个会话一个专用绑定租约, `linkToDeath` 插件进程, 死亡时合成 `error(SESSION_CLOSED)` + `end(error)`), `SpeechEventGate` (唯一 `end`), `SpeechProgressBinder` (操作 id, 调用方取消或超时时 `cancelOperation`), `SpeechAudioInput` (只读 pfd, 宿主先查 WAV 头与 200 MiB) 与 `SpeechOutputFile` (合成输出暂存在目标旁, 校验后原子改名); JVM `SpeechErrorMapperTest` (4), `SpeechEventGateTest` (3), `SpeechAudioTest` (4) 通过; 设备往返见 P1.4 测试条目 (假插件 `test-apps/speech-provider-conformance`, 独立 uid, 含进程死亡用例).
- [x] (测试) 真机验证 (API 31 Sony, 33 Sony / Redmi, 35 Xiaomi Pad / OnePlus, 36 Samsung): 宿主脚本在后台 (其他应用在前台, 以及熄屏) 时插件 `AudioRecord` 读到的不是静音帧; 记录每台结果; 不成立的机型按 D22 备选并写入附录 D Q5. 证据: 宿主提交 `bd410162c` 的 `test-apps/speech-provider-conformance/run_microphone_probe.py` (脚本经 `RunIntentActivity` 启动, 不用 instrumentation, 宿主保持真实进程状态; 假插件在独立 uid 进程用 `AudioRecord` 每 500 ms 报告一次是否有声或被静音; 前台, HOME 退到后台, 熄屏各 8 秒, 每段采样宿主与插件进程的 procState / capability), 2026-10-16, AutoJs6 均未开前台服务: Sony XQ-AT72 (QV710AF65F, API 31), AutoJs6 无障碍服务已绑定: 三段均有声, 插件进程的 procState / capability 与宿主一致 (后台均为 `5/LCMN`, 熄屏均为 `6/LCMN`), 即 `BIND_INCLUDE_CAPABILITIES` 生效; OnePlus OPD2413 (63751f63, API 35), 无障碍服务未启用: 前台有声 (宿主 `2/LCMNFUA`, 插件 `3/LCMNFUA`), 后台与熄屏被静音 (两个进程均为 `15/-------`; 首轮后台 15 个与熄屏 14 个事件全部被静音, 最终版复测后台 15 个中 14 个被静音, 仅切换瞬间 1 个有声, 熄屏 15 个全部被静音); Xiaomi Pad 23046RP50C (968e9f18, API 35), 无障碍服务已启用但系统一直未能绑定: 首轮与最终版复测相同, 前台有声 (`2/LCMNFUA`, `3/LCMNFUA`), 后台 15 个事件中 14 个被静音 (仅切换瞬间 1 个有声), 熄屏 15 个全部被静音 (两个进程均为 `15/-------`). 复测由维护者解锁两台设备后运行最终版探针 (`bd410162c`), 退出码 0. 结论: 插件能录音当且仅当 AutoJs6 自身持有麦克风能力 (界面可见, 无障碍服务已绑定或 microphone 前台服务); 宿主在后台且无此能力时插件同样被静音, 按 D22 备选处理, 附录 D Q5 定为 (a), 实现列入 P3.1. 未执行: API 33 (Sony XQ-DQ72 QV770340J7 本轮离线, Redmi 22120RN86C 保留旧宿主未改动), API 36 (Samsung 未连接); 两者留给 P3.5 设备矩阵 (1.0.0 要求 API 31 / 33 / 35 / 36 各测一次后台麦克风).

### P1.3 注册, 协议文档, changelog 与版本回填

- [x] (宿主) `InstalledPluginRepository` 两张表加入 `SpeechPluginHost.ACTION_SPEECH`; `PluginInstallWizardCatalog` 加 `entry(official("three.macaw.speech"), "Three Macaw Speech", TOOLS)` (附录 D Q1); `AndroidManifest.xml` `<queries>` 加包名; R8 规则; 相关 JVM 测试. 证据: 宿主提交 `1d6280bf8`; 两张表, 引擎到 action 的映射 (`PluginCenterViewModel`), 向导 `TOOLS` 条目 (不进内置配置), `<queries>` 中的 SPEECH intent 与包名; R8 由 `speech-api` 的 `consumer-rules.pro` 保留契约类; `PluginCenterSpeechRegistrationTest` (5) 与更新条目数为 49 的 `PluginInstallWizardCatalogTest` 通过.
- [x] (插件) `REQUIRES_HOST_VERSION` 临时值 = 当时宿主 `VERSION_BUILD + 1 + 1652` (D28), 同步 README / 测试夹具. 证据: 开工时宿主 `VERSION_BUILD = 3798` (维护者 `bc65c8256`), 临时值 3798 + 1 + 1652 = 5451; 宿主提交 `7ceef07da` 把 `VERSION_BUILD` 提为 3799 (versionCode 5451), 契约常量 `SpeechPluginIds.REQUIRED_HOST_VERSION_CODE` 同为 5451. 插件 `app/build.gradle.kts`, `ThreeMacawSpeechContract.REQUIRED_HOST_VERSION`, `.readme/common.json` 与生成的 README / changelog (10 种语言) 同步, `PluginRuntimeInfoTest` 与 `SpeechContractAlignmentTest` 断言 5451.
- [x] (宿主) `docs/dev/speech-plugin-protocol-v1.md` 定稿; 6.8.0 `.changelog/lang_zh-Hans.json` 增加 `speech` 模块与插件条目 (其他语言由维护者同步); 宿主提交只落本地. 证据: 宿主提交 `51b6bd3b7` (本地, 未推送): 协议文档 (发现与绑定, 逐方法的线程 / 超时 / 上限 / 结果 / 错误, 描述符, 会话与操作的资源所有权, 唤醒轮次, 限制, 错误码, 后台麦克风, 脚本路径); changelog 增加 "3-Macaw Speech 插件" 与 "speech 模块" 两条 feature 并重新生成 zh-Hans 的 Markdown. 维护者同一文件中未提交的条目移动未被带入该提交.

### P1.4 脚本 API `speech` (D4, D27)

- [x] (宿主) `runtime/api/augment/speech/Speech.kt` (`Augmentable`, 方法见附录 B.1), `SpeechSession.kt` / `SpeechWakeSession.kt` (EventEmitter, `stop` / `cancel` / `feed` / `isActive`, 脚本退出时自动关闭), `SpeechModels.kt` (`list` / `install` / `installAsync` / `remove` / `importFile` / `setDefault` / `getDefault`), `SpeechErrorMapper`; `ScriptRuntime` 挂载 `speech` 与 `$speech`. 证据: 宿主提交 `7ceef07da` (`SpeechErrorMapper` 在 `1d6280bf8`); 附录 B.1 全部方法 (另有 `models.refreshCatalog`), 会话属性 `id`, `modelId`, `source`, `round`, `startedAt`, `state`, `reason`, `isEnded` 与 `result()`, 唤醒会话的 `thenListen` 轮次以 `wake` 事件的 `listen` 交给脚本 (轮次事件在命中派发后才投递), `speech.SpeechError` 构造器; `SpeechService` 在脚本退出时静默取消全部会话. 附录 D Q8 按 (a): 识别文本不写宿主日志.
- [x] (宿主) 参数规范化与边界: 路径解析 (相对脚本目录), PCM 输入类型 (`ArrayBuffer` / `Int16Array` / `byte[]`), options 校验, `RegExp` 不涉及; `LIMIT_EXCEEDED` 在宿主侧先判. 证据: 宿主提交 `7ceef07da`; `SpeechScriptArguments` (纯 Kotlin: 未知键按名拒绝, 形态与范围为 `INVALID_OPTIONS`, 会话时长, 热词, 关键词, 文本与 token 长度为 `LIMIT_EXCEEDED`, 拒绝 URI 路径), `SpeechCalls.pcmBytes` (`ArrayBuffer`, 类型数组视图, `byte[]`; 大端缓冲区转小端), 脚本 PCM 由宿主包成临时 WAV 交给 `recognizeFile`; JVM `SpeechScriptArgumentsTest` (10) 与 `SpeechScriptValuesTest` (4) 通过.
- [x] (宿主) 退化: `speech.isAvailable()`; 缺插件抛 `PLUGIN_UNAVAILABLE` 并带安装向导提示; 未启用 / 不兼容走插件中心既有提示; 宿主不保留任何识别或合成实现. 证据: 宿主提交 `1d6280bf8` / `7ceef07da`; 缺插件时信息为宿主本地化的 `error_missing_required_plugin_for_module_label` ("3-Macaw Speech", 按 D27 复用, 不另加向导链接), 未启用 / 未授权为 `PLUGIN_DISABLED`, 版本不足或不应答契约 (如 P0 骨架) 为 `PLUGIN_INCOMPATIBLE`; 宿主中没有识别或合成代码. 设备证据见下一条.
- [x] (测试) JVM: options 解析, 错误映射, 会话状态机 (ready -> listening -> ended, 重复 stop 幂等); 设备: 用 P0 空壳 + 假模型路径验证 Binder 往返与错误传播 (真实识别在 P3). 证据: JVM (宿主 `7ceef07da`) `SpeechSessionTest` (8 项: `pending` -> `active` -> `ended`, 事件顺序与载荷, 唯一终结 `end`, 经端口的 `stop` / `cancel` 及结束后幂等, `feed` 规则, 溢出, 脚本退出静默, 唤醒轮次先到后到两种顺序), `SpeechScriptArgumentsTest` (10), `SpeechErrorMapperTest` (4) 等; 2026-10-16 `:app:testAppDebugUnitTest` 选定的 speech 与插件中心测试 101 项全部通过. 设备 (宿主 `1d6280bf8` 的 `test-apps/speech-provider-conformance` 假插件, 独立 uid, 与宿主同签名): `SpeechScriptApiTest` 4 项在 Sony XQ-AT72 (QV710AF65F, API 31) 与 Xiaomi Pad 23046RP50C (968e9f18, API 35) 上均通过, 覆盖能力, 模型 (列表, 默认, 带进度安装, 非商用许可, 删除, 设默认, 刷新目录), WAV 与 PCM 识别, 假模型 `asr.missing` 的 `MODEL_MISSING`, 非 WAV 的 `UNSUPPORTED_FORMAT`, 缺文件的 `IO_FAILED`, 语音活动, 合成到暂存文件 (无残留), `Async` 形态, 外部 PCM 会话, 带 `thenListen` 的唤醒轮次, 以及插件进程被杀后的 `error(SESSION_CLOSED)` + `end(error)`. 未装假插件时: 两台都看到 `PLUGIN_UNAVAILABLE` (含 "3-Macaw Speech" 的本地化文案); 在 QV710AF65F 上只装本仓库 P0 骨架 (占位 Binder, 要求宿主 5451) 时看到 `PLUGIN_INCOMPATIBLE`. 参数错误 (`INVALID_OPTIONS`) 与超长文本 (`LIMIT_EXCEEDED`) 在到达插件前由宿主报告. 未执行: Sony XQ-DQ72 (QV770340J7, API 33) 本轮离线, Redmi 22120RN86C 保留旧宿主未改动, Samsung API 36 / 37 未连接.

---

## P2: 模型目录与分发

目标: 维护者可在 Shared-Assets 复现地生成并签名 `speech-models` 目录与 Release; 插件可下载, 校验, 导入, 管理模型并按签名刷新目录.

### P2.1 Shared-Assets `speech-models` 类别 (D24, D25)

- [x] (资产) `speech-models/README.md` (布局, 版本与完整性规则, 与 `codegen-models` 的差异: 二进制不入库), `schemas/catalog-v1.schema.json` (附录 C.3), `schemas/signature-v1.schema.json` (复用 codegen 的信封结构), `schemas/model-source-v1.schema.json`. 证据: Shared-Assets 提交 `c7d1adf` (本地, 待主会话推送); README 扩写为用途, 与 codegen 的差异, 布局, 镜像归档格式 (附录 D Q3 定为未压缩 tar), 目录条目字段, 版本与完整性规则, v1 现状表, 保留的密钥小节, 以及维护者签名, 提升, 本地复现与触发工作流的逐条命令 (P2.5). 三个 schema 由校验器常量生成, `test_repository` 断言必填字段, 每个 modelType 的 `runtime.config` 角色与校验器一致, 签名 schema 与 codegen 仅 `$id` / `title` 不同. 相对附录 C.3 草案的偏差: `upstream` 改为数组 (matcha 需要第二个上游资源 `vocoder-models/hifigan_v2.onnx`); 模型级字段与 `versions[]` 分离 (同 codegen); 新增 `runtime.config` (sherpa-onnx 1.13.8 文件角色) 与 `sourceUrl`; `commercialUse` 取 true / false / `"unknown"`. 详见附录 C.3 末尾.
- [x] (资产) `tools/speech-model-distribution/`: `prepare_sources.py` (按 `source.json` 下载上游归档到忽略的 `build/`, 校验上游 SHA-256, 按成员列表重打包为 `autojs6-speech-<modelId>-v<versionCode>-<sha8>.tar`, 计算归档与文件级 SHA-256, 可重复运行且字节确定), `build_catalog.py` (生成 `catalogs/pending/vN/catalog-v1.json`), `verify_catalog.py` (schema, 单调版本, 不可变条目, 签名), 单元测试; `README.md` 写明离线签名步骤 (新密钥, 拒绝复用 Ace / codegen 密钥). 证据: Shared-Assets 提交 `c7d1adf` (本地, 待主会话推送); 另加 `verify_release.py` (发布资源校验与远端标签 / Release 不存在预检), `sign_catalog.py` (维护者签名, 拒绝 Git 工作树内的私钥与 Ace / codegen 密钥) 和 `check_docs.py`, 共用 `distribution.py` / `preparation.py` / `crypto.py`; `verify_catalog.py` 不带签名参数时校验未签名候选, `--source-root` 要求与来源重建的字节一致. 下载只接受 `k2-fsa/sherpa-onnx` 的 GitHub Release URL, 重定向限于 GitHub 资源主机, 支持断点续传, 缓存先校验大小与 SHA-256; 选中成员以流式写入编号临时文件, 成员名不落盘. 单元测试 52 项 (`python -m unittest discover -s tools/speech-model-distribution/tests`, Python 3.12 与 3.13 均通过, 无网络, 临时 OpenSSL 密钥); `py D:/idea-projects/prepush_check.py` 7/7 通过 (含 codegen 63 项与 Ace 测试). 字节确定: 2026-10-16 两次独立离线重建 10 个归档, SHA-256 全部相同 (每次约 8.5 分钟).
- [x] (资产) `.github/workflows/publish-speech-models.yml`: 输入标签与密钥 ID, 仅上传目录新引入的资源, 发布前核对签名; `markdown.yml` 覆盖新 README. 证据: Shared-Assets 提交 `c7d1adf` (本地, 待主会话推送); 工作流依次为默认分支检查, 工具测试, 签名输入齐全且标签一致, 当前与历史目录验签, 由来源重建目录并 `cmp`, 从固定上游重建 `release-assets.txt` 所列归档并 `verify_release.py`, 认证后确认标签与 Release 均不存在, 建标签 + 草稿 Release + 发布, 最后全部下载回读校验 (environment `speech-models-release`, action 固定到与 codegen 相同的提交); `test_release` 断言步骤顺序, 权限与内嵌 bash 语法. `markdown.yml` 新增 `check_docs.py` 步骤 (Linux / Windows), 检查 speech 文档的 ASCII 标点, 相对链接, 以及 README 记录的 Key ID / SPKI 指纹与公钥一致. 未执行: 远端工作流运行 (随 P2.5 由维护者触发).
- [x] (资产) 维护者离线生成 `speech-models` 签名密钥, 公钥入 `speech-models/keys/catalog-signing-public.pem`, 记录 Key ID 与 SPKI 指纹 (维护者操作, 会话只记录结果). 证据: 维护者提交 `003d427` (已推送); Key ID `p256-bf5f0f579c7fbef1`, SPKI DER SHA-256 `bf5f0f579c7fbef188e81a02257fc3336189020273b2c69c69092cc402214f68`, 私钥在仓库外的 `D:/offline-keys/`; `test_crypto` 核对该公钥为 P-256 且 Key ID 与指纹一致, 并确认 Ace / codegen 公钥被拒绝.

### P2.2 精选目录审核 (D10, D11)

- [x] (资产) 附录 A 每条模型的 `source.json`: 上游 Release tag, 资源名, 上游 SHA-256 (下载实测), 重打包成员 (只含 int8 或所需精度, tokens, lexicon, 词典, vocoder, 不含 fp32 重复与测试 wav), 语言, 采样率, 说话人数, `license` 与 `commercialUse`, 模型卡摘要 (10 语言 summary 至少 en / zh-CN). 证据: Shared-Assets 提交 `c7d1adf` (本地, 待主会话推送), `speech-models/sources/<modelId>/1/` 共 10 条 (各含 `source.json`, `MODEL_CARD.md`, `LICENSE*`). 上游 11 个资源 (含 `vocoder-models/hifigan_v2.onnx`) 共 1857832275 字节, 2026-10-16 下载一次并缓存在被忽略的 `build/speech-models/upstream/`, 其中 6 个资源的 GitHub API digest 与实测 SHA-256 一致 (其余 5 个上游未公布 digest). 采样率与说话人数取自各 ONNX 的元数据. 成员选择以 sherpa-onnx 1.13.8 自身配置 (`OnlineRecognizer.kt`, `OfflineRecognizer.kt`, `generate-tts-apk-script.py`) 为准: int8 encoder / joiner + float32 decoder, 不含测试 wav 与 fp32 重复; 1.12.15 起不再使用的 jieba `dict/` 一律省略; aishell3 省略 172.3 MB (180717014 字节) 的可选 `rule.far`; melo 的 `model.int8.onnx` 是 133 字节的 Git LFS 指针, 不收录; matcha 声码器选 `hifigan_v2` (3.6 MiB, MIT) 而非 sherpa APK 默认的 `vocos-22khz-univ` (51.4 MiB); KWS 取 chunk-16-left-64. 偏差: piper 的目录 ID 改为 `tts.vits.piper.en-us-lessac-medium` (目录 ID 只允许小写); summary 只写 en / zh-CN.
- [x] (资产) 许可审核记录 `speech-models/audits/license-audit-v1.json`: 每条模型的权重许可与训练数据许可来源链接, 结论 (`commercialUse` true / false / unknown); `unknown` 的模型按 D11 同样提示. 证据: Shared-Assets 提交 `c7d1adf` (本地, 待主会话推送); 规则: 发布者或其点名的训练数据限制商用为 false (优先), 权重未声明许可为 unknown, 其余为 true; 每条附权重与数据的证据链接与待决问题. 结论: true 5 条 (中英流式, 英文流式, melo, kokoro, KWS); false 4 条 (中文流式 2025: 训练集含 MagicData CC BY-NC-ND 4.0, KeSpeech, WenetSpeech 等非商用语料且权重未声明许可; SenseVoice 2025-09-09: 由 WenetSpeech-Yue (CC BY-NC 4.0) 微调; piper lessac: Lessac Blizzard 2013 数据仅限研究; matcha baker: 标贝语料仅限非商用); unknown 1 条 (aishell3: 数据为 Apache-2.0, 但权重未声明许可). kokoro 与 piper 附带的 `espeak-ng-data` 为 GPL-3.0-or-later, 来源目录附 GPL 全文. silero VAD 作为随 APK 资产登记 (MIT). 当时待维护者确认的两项 (中文流式 2025 的上游仓库受限访问且无许可声明, 签名前需确认可以镜像; SenseVoice 是否改用官方 2024-07-17 导出) 已于 2026-10-16 拍板, 见下方第 4 条与附录 D Q10 / Q11.
- [x] (资产) 生成 `catalogs/pending/v1/catalog-v1.json`, `verify_catalog.py` 通过, 差异审阅后交维护者签名 (P2.5). 证据: Shared-Assets 提交 `c7d1adf` (本地, 待主会话推送); `build_catalog.py --catalog-version 1 --generated-at 2026-10-16T03:30:00Z` 生成 10 个模型与 10 个新归档 (`release-assets.txt`, 合计 1461609984 字节), 目录 258990 字节; `verify_catalog.py ... --expected-repository SuperMonster003/AutoJs6-Shared-Assets --source-root speech-models/sources` 通过, `verify_release.py` 对重建的 10 个归档通过, GNU tar 可正常列出 (0644, 属主 0/0, mtime 0). 签名与提升由维护者按 Shared-Assets `speech-models/README.md` 的命令执行 (P2.5); 重建产物保留在被忽略的 `build/speech-models/release-v1/`. 该候选已被下一条按两项维护者决定重新生成.
- [x] (资产) 2026-10-16 维护者对上述两个待决项拍板, 已落地于 Shared-Assets 提交 `89f04a6` (已推送). 决定 1 (附录 D Q10): 中文流式 2025 `asr.zipformer-streaming.zh.2025-06-30.int8` 保留目录条目但不镜像, 只走上游 URL (权重无许可, 原始 Hugging Face 仓库 gated, 训练集含 MagicData / KeSpeech / WenetSpeech 非商用语料, 不镜像可避免由维护者再分发); `files[]`, `upstream[]` 与许可提示 (commercialUse=false) 不变. 实现: 目录条目 `mirror` 由必填对象改为 `object | null` (字段仍必填, null 即仅上游), `source.json` 新增必填布尔 `mirror` (false 时 `archive` 固定 null, `files[]` 仍由上游成员实测且须被 `upstream[]` 完整覆盖), 校验器对无 mirror 条目跳过归档校验并要求 `sourceUrl` 指向不晚于目录版本的发布标签, `prepare_sources.py` 只测量不打包, `build_catalog.py` 不列入 `release-assets.txt` (`preparation.json` 新增 `upstreamOnlyVersions`), `verify_release.py` 与发布工作流不重建也不上传, 已发布版本的 mirror 不可增删 (历史不可变); 单元测试 52 -> 56 项 (无 mirror 条目的正反用例). 决定 2 (附录 D Q11): SenseVoice 改用 sherpa-onnx 官方 int8 导出, 新目录 ID `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (上游 `asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17.tar.bz2` 163002883 字节, SHA-256 `7d1efa2138a65b0b488df37f8b89e3d91a60676e416f515b952358d83dfd347e`, 与 GitHub API digest 一致; 成员 `LICENSE`, `README.md`, `model.int8.onnx`, `tokens.txt`, 与 1.13.8 `OfflineRecognizer` 的 sense-voice 配置 (model + tokens) 一致, 安装后 239549910 字节, 镜像归档 239554048 字节, SHA-256 `7d9de5b77708d26f08bdd4fcf29d58a2654608eeab1f3ea1fea3bbeef52f19e5`; 许可 `LicenseRef-FunASR-Model-License-1.1`, commercialUse=true, 来源目录 `LICENSE` 含 FunASR 许可全文, `recommended` 沿用 false), 删除 2025-09-09 来源目录 (pending 未签名, 不走 revoked), 许可审核 `openQuestions` 改为已决定并新增 `decisions` / `distribution` 字段. 重新生成的 pending v1: `generatedAt` 2026-10-16T05:30:00Z, `catalog-v1.json` 258885 字节, SHA-256 `4ece91b79de565e2ed9c334f89751488f9d7b6684889efab4197a17ad56cf16f`, 10 个模型 = 9 个镜像 + 1 个仅上游, `release-assets.txt` 9 行合计 1296362496 字节; `verify_catalog.py --source-root` 通过, 离线二次复现 9 个归档 SHA-256 一致且 `verify_release.py` 通过, 56 项单元测试, `check_docs.py` 与 `prepush_check.py` 7/7 通过. 插件侧: P2.3 的下载器遇到 `mirror` 为 null 时直接走 upstream, P2.4 内置快照改为上述目录字节. 2026-10-16 维护者另决定把新 SenseVoice 设为推荐 (`recommended=true`): Shared-Assets 提交 `62d4a30` 重新生成 pending v1 (258884 字节, SHA-256 `312332e29aa6dd0a1dac1ec1641b571330ec2ba9edca460bbf7420b08d97fa46`, generatedAt 2026-10-16T06:15:00Z, 其余内容不变), 插件内置快照与锁文件同步更新 (`5aa07e3`).

### P2.3 插件模型存储 (D23)

- [x] (插件) `ModelCatalog` (解析, 查询, 推荐模型), `ModelStore` (安装表, 默认模型, 路径解析, 回收), `Downloader` (HTTPS, 超时, 断点续传 `Range`, 进度, 取消, 空间预检, mirror -> upstream 回退), `ArchiveExtractor` (tar / tar.bz2 / zip, 路径穿越防护), `Verifier` (归档与文件级 SHA-256), `Importer` (SAF 目录或归档), `LicensePolicy` (D11 提示与脚本 `acceptLicense`). 证据: 提交 `8119610`, 包 `core/model` (纯 JVM): `StrictJson` (严格 JSON, 拒绝重复键, 非有限数, BOM 与孤立代理项); `CatalogRules` + `ModelCatalogParser` 逐条移植 Shared-Assets `distribution.validate_catalog` (含 `89f04a6` 的 `mirror: null` 仅上游条目; 缺省的 `mirror` 键同样按仅上游读取, 这是唯一比参考校验器宽松之处, 提交 `8119610` 之后的一笔修正), `ModelCatalog` 按 kind / 语言 / 已安装查询, 推荐模型先取推荐且可安装的, 再按目录顺序; `ModelStore` (`filesDir/models/<modelId>/<versionCode>/` + `.manifest.json`, `.staging/` 写好并校验后改名即激活, 替换时旧目录先进 `.trash/`, 启动时 `reconcile` 恢复或清理, 删除时一并清掉默认设置, 启动时回收不再被目录引用或超过一周的下载残留); `Downloader` (`HttpURLConnection`, 无新依赖; 连接 15 s / 读取 30 s 超时, 每一跳手动重定向且只走 HTTPS 的 GitHub 主机, `Range` 续传, 服务器忽略 `Range` 时从头开始, 连接失败重试 3 次, HTTP 错误与完整性失败不重试以便立即回退); `ModelInstaller` (镜像优先, 取消与空间不足以外的任何镜像失败都回退上游; 空间预检 = 待下载 + 安装字节 + 32 MiB, Android 上取 `StorageManager.getAllocatableBytes`; 进度 = 已下载 + 已写出字节); `ArchiveExtractor` + `TarReader` (镜像逐字节核对规范 USTAR 头部; 上游 GNU / POSIX tar 按 Python `tarfile` 规则读取 (校验和, base-256, GNU 长名, pax), 只写出 `members` 映射的成员, 超大成员写入前拒绝, 截断报错; 导入先写编号块文件, 路径校验后才改名, 总量与条目数受限); `Bzip2InputStream` (自带解码器, 校验块 CRC 与流 CRC, 支持拼接流, 拒绝随机化块); `Verifier` (归档大小 + SHA-256, 安装后逐文件 SHA-256); `Importer` (tar / tar.bz2 / zip 或目录树; 按大小与 SHA-256 匹配某个目录版本即以目录身份, 路径与元数据登记, 否则须给出 id / kind / modelType / name / languages, id 不得采用目录形式 `<kind>.<name>`, 单一顶层目录被去掉); `LicensePolicy` (`commercialUse` 为 false 或 `"unknown"` 时需 `acceptLicense`, 按模型, 版本与 SPDX 表达式记住接受), 三个 `noticeKey` 的 10 语言字符串与 `LicenseNotices` 映射 (界面用于 P5.1). 偏差: (1) 不引入 Apache Commons Compress: 1.26 起经 commons-io 的构建器依赖 `java.nio.file` (API 26, 插件 minSdk 24), 1.25 及以前带 CVE-2024-25710 / CVE-2024-26308, 因此自带约 330 行的 bzip2 解码器, 由 libbzip2 写出的向量与文件级 SHA-256 兜底; 本阶段没有新增运行时依赖, changelog 依赖行与 `THIRD_PARTY_NOTICES.md` 不变. (2) 下载文件以归档 SHA-256 命名, 默认模型与许可接受不用 DataStore, 清单名 `.manifest.json`, 每个模型只保留一个版本 (见 D23 注记). (3) 目录外导入的模型没有 `runtime.config`, 文件角色由 P3 / P4 加载时按 sherpa-onnx 命名推断; SAF 目录树只有 `ImportTree` 接口与本地目录实现 (`FileImportTree`), SAF 适配随 P5.1 的文件选择器加入, Binder 的 `importModel` 只接收归档描述符. (4) `listModels` 还列出随 APK 的 `vad.silero.v5` (kind `vad`, 已安装, 默认); 契约的 `commercialUse` 是布尔值, 目录的 `"unknown"` 在 Bundle 中读作 false (同样需要 `acceptLicense`), `noticeUrl` 取该版本的 `sourceUrl`; `modelType` 报告目录值 (`online-transducer`, `sense-voice`, `vits` 等). (5) 附录 D Q7 (计量网络) 只对脚本路径作答, 见该条.
- [x] (插件) Binder `listModels` / `installModel` (返回 opId, 进度回调, 同时只跑一个下载) / `cancelOperation` / `removeModel`; 后台下载以 `FOREGROUND_SERVICE_DATA_SYNC` 前台服务 + 通知进行, 进程被杀后可从断点续传. 证据: 提交 `8119610`; `SpeechPluginBinder` (`ISpeechPlugin.Stub`, 取代占位 Binder): `getInfo`, `getCapabilities` (契约版本, `speechFeatures=[models]`, 当前目录版本, `sherpa-onnx` / 1.13.8, 麦克风权限), `listModels` (kind / language / installed 过滤), `installModel` (未知 ID 与无可装版本同步抛 `MODEL_MISSING`, 许可等其余失败经回调), `cancelOperation` (排队中的立即以 `SESSION_CANCELLED` 结束, 运行中的关闭连接), `removeModel` (`removed`; 使用中返回 `SESSION_BUSY` 的钩子 `ModelManager.isModelInUse` 留给 P3), 以及契约追加的 `importModel` (先把宿主描述符的副本复制到 `cacheDir/imports/`), `setDefaultModel`, `refreshCatalog`; 长操作由 `ModelManager` 在单一线程上按序执行, 进度约每阶段 200 次, 每个应答与完成都带 `contractVersion`, 错误信息为英文且不超过 4 KiB, 请求的契约版本不受支持时报 `PLUGIN_INCOMPATIBLE`. `recognizeFile` / `detectVoice` 应答 `ENGINE_FAILED`, `openSession` / `openWakeSession` / `synthesizeToFile` 抛 `ENGINE_FAILED`, 分别由 P3.2 / P3.3 / P3.4 / P4.3 替换 (插件未声明这些功能, 宿主不会调用). `ModelOperationService` (未导出, `foregroundServiceType="dataSync"`, Manifest 追加 `FOREGROUND_SERVICE_DATA_SYNC`, 见 D29) 在有操作排队或运行时以静默进度通知保持进程, 队列清空即停止, Android 15 的 `onTimeout` 只停止服务, 被拒启动时操作照常继续并记录警告; 进程被杀后部分文件保留, 下次 `installModel` 以 `Range` 续传 (不自动重启下载). 设备: Sony G8441 (BH900ASK9E, API 28) 下载期间 `dumpsys activity services` 显示 `ModelOperationService` `isForeground=true` (渠道 `model_operations`), 结束后停止. 未执行: 宿主在后台经绑定发起下载时 API 31+ 能否启动前台服务, 留给 P3.5 设备矩阵; `POST_NOTIFICATIONS` 的申请随 P5.1 界面.
- [x] (测试) JVM: 目录解析与签名验证 (固定向量), 归档解包边界 (路径穿越, 超大成员, 截断), 校验失败 / 空间不足 / 中断 / 损坏恢复 / 原子替换 (本地假 HTTP 服务器); 设备: 真实下载一个小模型 (aishell3 30 MB) 与 mirror 失败回退 upstream 各一次. 证据: 提交 `8119610`; `:app:testDebugUnitTest` 17 个类 107 项全部通过 (Temurin 验收命令同样通过, 连续复跑 3 次无不稳定), 新增 `StrictJsonTest` (5), `Bzip2InputStreamTest` (5: libbzip2 写出的多块, 长游程, 1 MiB 游程, 全字节值, 拼接流与空流向量; 截断与翻转比特), `ArchiveExtractorTest` (9: 规范镜像含超过 100 字节且带空格与 `!` 的路径, 上游只写映射成员, 超大 / 重复 / 符号链接成员, 截断, 非规范头部与尾随数据, 导入的路径穿越与链接, zip 炸弹与条目数, 格式识别, GNU 长名与 pax), `ModelCatalogParserTest` (7: 内置目录 10 个模型含 1 个仅上游, 合成夹具, 24 条规则反例, `mirror: null`, 时间戳), `CatalogSignatureVerifierTest` (6: Shared-Assets `crypto.py` 经 OpenSSL 签名的固定向量, 测试内生成 P-256 密钥按 `crypto.py` 信封签名, 篡改目录 / 篡改签名 / 他人密钥 / 错误 keyId / 非规范 base64 与 DER / 多余字段 / P-384 均拒绝), `DownloaderTest` (8: 本地 `HttpServer` 假 GitHub, 进度, 断线后 `Range` 续传, 被杀进程留下的部分文件续传, 忽略 `Range`, 404 不重试, 损坏与超长响应, 取消, 正式网络策略), `ModelManagerTest` (16: 镜像安装, 镜像 404 与损坏时回退上游, 仅上游版本, 全部来源失败, 空间不足, 许可与记住, 取消后续传, 串行执行与未开始即取消, 列表 / 默认 / 删除, 损坏的安装被识别并修复, 两次改名之间中断后恢复旧副本, 按内容导入目录模型, 目录外导入与路径穿越, 目录树导入, 回收过期下载). 夹具由 `.python/generate_model_fixtures.py` 调用 Shared-Assets `c7d1adf` 的 `distribution.py` / `crypto.py` 生成 (合成目录与签名, 规范镜像 tar, Python `tarfile` 写的 GNU tar.bz2, libbzip2 向量, 恶意归档). 设备 (`ModelInstallDeviceTest`, `-Pandroid.testInstrumentationRunnerArguments.macawNetwork=true`, 2026-10-16, 模型 `tts.vits.icefall.zh-aishell3`): Sony XQ-DQ72 (QV770340J7, API 33, arm64-v8a) 未传 `acceptLicense` 时得到 `MODEL_LICENSE_NOT_ACCEPTED` 且不联网; 镜像 `speech-models-v1` 尚不存在, HTTP 404 后回退 sherpa-onnx `tts-models/vits-icefall-zh-aishell3.tar.bz2` (31559701 字节); 下载约 4.26 MB 后取消 (`SESSION_CANCELLED`, 部分文件 4370562 字节), 再次安装按 `Range` 只下载其余 27189139 字节, 22.3 s 完成 (186 次进度回调), 8 个文件共 32762508 字节, 逐文件 SHA-256 全部一致, `listModels` 显示已安装, `removeModel` 删除. Sony G8441 (BH900ASK9E, API 28, arm64-v8a) 同一流程: 部分文件 4550945 字节, 续传 27008756 字节, 47.2 s, SHA-256 全部一致. 两台的 11 项设备测试 (含更新后的 `ThreeMacawSpeechContractTest`: 经真实 Parcel 的 `getCapabilities` / `listModels` / `removeModel` / `recognizeFile` 应答与新的权限集合) 均通过; release 分包 (R8) 构建与签名校验通过, arm64-v8a 12483736 字节 (D31 之内). 未执行: 镜像路径的真实下载 (Release 待 P2.5); OnePlus OPD2413 (63751f63) 因 ColorOS 弹出 USB 安装确认页, 按规则未代点, 已中止.

### P2.4 目录刷新 (D24)

- [x] (插件) 内置 `assets/catalog/catalog-v1.json` + `.sig.json` + 公钥 (发布时与 Shared-Assets `main` 完全一致的字节); 刷新: 下载 `main` 的目录与签名 -> 验签 -> 版本单调 -> 原子替换本地缓存; 失败时沿用旧目录并提示; 手动刷新入口在设置页, 不自动联网 (D29). 证据: 提交 `8119610`; `app/src/main/assets/catalog/` 内置 Shared-Assets `62d4a30` 的 `speech-models/catalogs/pending/v1/catalog-v1.json` 原字节 (258884 字节, SHA-256 `312332e29aa6dd0a1dac1ec1641b571330ec2ba9edca460bbf7420b08d97fa46`; 首版内置为 `89f04a6` 的 258885 字节 `4ece91b79de565e2ed9c334f89751488f9d7b6684889efab4197a17ad56cf16f`, 2026-10-16 随 SenseVoice 推荐标记更新, 插件提交 `5aa07e3`) 与 `keys/catalog-signing-public.pem` 原字节, 锁定在 `locks/speech-catalog.lock` (`.gitattributes` 保持原字节); `ModelCatalogConfiguration` 固定仓库, Key ID `p256-bf5f0f579c7fbef1` 与 SPKI SHA-256, 公钥与固定值不符即拒绝. `ModelCatalogRepository`: 没有签名的内置快照作为首个目录并标注 `UNSIGNED_BUNDLED`, 内置 `.sig.json` 存在时必须验签 (附录 D Q12); 刷新从 `raw.githubusercontent.com/SuperMonster003/AutoJs6-Shared-Assets/main/speech-models/catalogs/` 取目录 (上限 8 MiB) 与签名 (64 KiB), 对原始字节验签 (`CatalogSignatureVerifier`, 逐条对应 `crypto._signature_bytes`, P-256 曲线核对), 解析校验 (含仓库名), 不得早于内置目录, 当前目录与曾接受的最高版本 (`catalog.accepted`, 删除损坏的缓存也不能借此回退), 同版本须同字节 (唯一例外: 已签名目录可替换同版本的未签名内置快照), 先写签名再以改名提交目录; 网络失败报 `IO_FAILED`, 签名 / 格式 / 回退报 `MODEL_CHECKSUM_MISMATCH` (协议为 `refreshCatalog` 指定的码), 失败时目录不变. 只有 Binder `refreshCatalog` 触发 (D29 不自动联网), 设置页按钮归 P5.2; `PluginInfo.capabilities` 的 `speechCatalogVersion` 为 1, `getCapabilities` 报告当前目录版本. 偏差: P2.5 之前没有 `.sig.json` 可内置 (附录 D Q12); 维护者提升后若目录字节不变, 一次刷新只把信任升级为已签名 (`updated=false`), P2.5 再把内置快照换成签名目录与签名的原字节.
- [x] (测试) JVM: 篡改目录 / 篡改签名 / 版本回退 / 公钥不匹配均拒绝; 设备: 一次真实刷新. 证据: 提交 `8119610`; `ModelCatalogRepositoryTest` (7: 未签名快照的标注, 内置签名必须有效, 新版本验签后缓存并跨实例保留, 已签名同版本替换未签名快照, 篡改目录 / 篡改签名 / 他人密钥 / 回退 / 同版本不同字节 / 低于内置版本均拒绝且目录不变, 网络失败, 损坏缓存被丢弃后接受状态仍阻止回退), `BundledCatalogTest` (5: 锁定字节, 版本与 `ThreeMacawSpeechContract.CATALOG_VERSION` 一致, 固定公钥, 一旦存在 `.sig.json` 必须验签通过且 keyId 与公钥一致, 目录中每个 noticeKey 都有 10 语言字符串). 设备: QV770340J7 (API 33) 与 BH900ASK9E (API 28) 各做一次真实刷新, Shared-Assets `main` 上尚无签名目录, 返回 `IO_FAILED` (`Catalog refresh failed: HTTP 404`), 仍使用内置 v1 (`UNSIGNED_BUNDLED`). 未执行: 成功的真实刷新 (待 P2.5 提升签名目录).

### P2.5 `speech-models-v1` 发布 (维护者)

- [ ] (资产) 维护者签名 pending v1 -> 提升到 `catalogs/catalog-v1.json` -> 触发 `publish-speech-models.yml` 上传全部附录 A 资源 -> 核对 Release 资源 SHA-256 与目录一致 -> 插件内置快照更新为该字节.
- [ ] (发布) 记录 Release 链接, 资源数, 总体积, Key ID.

---

## P3: 识别, VAD 与唤醒词

目标: `speech.listen` / `speech.recognize` / `speech.detectVoice` / `speech.wake` 在附录 F 设备上可用, 延迟与资源占用有基线.

### P3.1 音频采集与运行时 (D15, D16, D30)

- [ ] (插件) `MicrophoneSource`: `AudioRecord` (`VOICE_RECOGNITION` 源, 16 kHz, 单声道, 20 ms 帧), 权限检查, 独占与排队 (`SESSION_BUSY`), 读取线程, 静音检测 (连续零帧上报 `warning`), 释放; `SherpaRuntime` 线程与模型生命周期 (加载, 共享, 空闲释放, 内存上报).
- [ ] (插件) 会话表与 `linkToDeath`; 每 token 麦克风会话上限 1, PCM 会话上限 4; 超时 (`timeoutMillis`, `maxDurationMillis`) 与自动结束.
- [ ] (插件) 后台麦克风备选 (D22, 附录 D Q5 (a)): 麦克风会话 (含唤醒) 开启时插件启动 `microphone` 类型前台服务 (通知标明正在录音), 会话结束即停止; 须在插件仍继承宿主麦克风能力时启动 (宿主界面可见, 或宿主自身持有能力), 之后宿主退到后台或熄屏不被静音; 宿主已在后台且无能力时启动被拒的报错 (错误码与文案) 在实现时实测确定, 同步协议文档与 `api/speech.md`. P1.2 实测见上文: API 35 上宿主在后台时插件读到静音帧.
- [ ] (测试) 真机: 宿主界面可见时开启麦克风会话, 退到后台与熄屏各 8 秒仍有声 (阶段划分同宿主 `run_microphone_probe.py`); 宿主已在后台时开启的结果与报错.
- [ ] (测试) instrumentation: 权限缺失 -> `PERMISSION_DENIED`; 两个会话争用 -> `SESSION_BUSY`; 宿主进程被杀 -> 麦克风 1 秒内释放 (`AudioManager` 活动录音查询).

### P3.2 流式识别会话 (D17)

- [ ] (插件) `StreamingRecognizerSession`: `OnlineRecognizer` + 端点规则 + `partial` / `final` / `endpoint` 事件, 热词 (transducer), `language` 提示 (双语模型忽略), 外部 PCM `feed` 路径 (块大小与速率限制), `finish` 后 flush 最后一段. P2.3 起 Binder 的 `openSession` / `openWakeSession` 以 `ENGINE_FAILED` 占位, 本阶段替换并在 `ThreeMacawSpeechContract.FEATURES` 加入对应功能; 模型取 `ModelManager.defaultModel(kind)`, 文件路径由 `InstalledModel.runtimeConfig` 的角色经 `InstalledModel.resolve(path)` 得到, 加载中的模型经 `ModelManager.isModelInUse` 阻止删除.
- [ ] (插件) 离线模型的 "VAD 切段 + 逐段识别" 会话实现同一回调接口 (段级 `partial`); 模型 kind 不匹配 (把 TTS 模型当 ASR) -> `INVALID_OPTIONS`.
- [ ] (测试) instrumentation (真机): 中文, 英文, 中英混合各一段标准录音 (`test-audio/`, 自录或公开语料, 记录来源与许可) 的词错率粗测; 首字延迟与 RTF 写入 `docs/development/asr-baseline.md`.

### P3.3 文件识别与 VAD (D15)

- [ ] (插件) `WavDecoder` (RIFF 解析, 位深 / 声道 / 采样率, 重采样), `recognizeFile` (VAD 切段 + `OfflineRecognizer`; online 模型则一次跑完), `segments[]` (起止毫秒, 文本, SenseVoice 的语言 / 情感 / 事件标签解析到 `tags`), `detectVoice` 返回语音段. P2.3 起这两个方法应答 `ENGINE_FAILED` 占位, 本阶段替换.
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

- [ ] (插件) `synthesizeToFile` (请求 Bundle: text, modelId, speakerId, speed; 输出 pfd; 进度回调按句; 取消时删除半写入内容由宿主负责) 与上限校验. P2.3 起该方法抛 `ENGINE_FAILED` 占位, 本阶段替换.
- [ ] (宿主) `speech.synthesize` / `synthesizeAsync` (宿主创建目标文件 pfd, 失败清理), `speech.speak` 委托 `tts` 模块并固定 engine (未安装引擎 -> `PLUGIN_UNAVAILABLE`).
- [ ] (测试) JVM: 请求校验; 设备: 8000 字符长文本合成与取消, 输出 WAV 可被宿主 `media` 播放.

### P4.4 宿主 `tts` 互通

- [ ] (测试) 真机: `tts.engines()` 列出插件, `tts.ready(<包名>)`, `tts.voices()` 含插件 Voice, `tts("你好", {engine})` 与 `tts.synthesize(text, path, {engine})` 成功; 卸载插件后宿主 `tts` 回退系统引擎无异常.

---

## P5: 独立界面

目标: 用户不写脚本也能下载 / 管理模型, 申请权限, 试听试说; 外观与其他独立插件一致.

### P5.1 首页与模型管理 (D26)

- [ ] (插件) `HomeActivity` / `HomeScreen`: 状态卡 (运行时版本, 已装模型数, 麦克风权限与一键申请, 系统 TTS 引擎是否选中), 模型列表 (按 kind 分组, 大小, 语言, 许可标签, 下载 / 暂停 / 取消 / 删除 / 设为默认, 导入入口), 下载进度与通知, D11 许可提示对话框. P2.3 已备好: `ModelManager` (列表, 安装, 导入, 删除, 默认, 刷新, 活动回调), `ModelOperationService` 通知, `LicenseNotices` 的 10 语言提示; 本阶段补 SAF 目录树的 `ImportTree` 适配与文件选择器, `POST_NOTIFICATIONS` 申请, 计量网络询问 (附录 D Q7).
- [ ] (测试) instrumentation: 列表渲染 (有 / 无网络, 空目录), 许可提示的确认 / 取消语义, 删除默认模型后的回退.

### P5.2 设置页

- [ ] (插件) 外观四项 (语言, 夜间模式, 主题色, 启动器图标; 默认跟随宿主), 识别 (默认模型, 端点灵敏度, VAD 开关), 合成 (默认模型, 说话人, 语速), 资源 (模型空闲释放时间, 目录刷新, 回收未引用文件), 关于 (版本, 发行历史, 更新检查, 第三方声明: APK 内置 `THIRD_PARTY_NOTICES.md` 的内容, GPL-3.0 全文, Unicode 使用条款与对应源码的位置, 满足 GPL-3.0 第 4 条与第 5(d) 条, D32); 按 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` 的列表密度, 对话框与保存语义.
- [ ] (测试) API 24 与 35 截图核对, 大字号, RTL (ar), 夜间.

### P5.3 试听试说

- [ ] (插件) `TryoutScreen`: 麦克风实时转写 (partial 灰字 / final 正文), 模型与语言切换, 唤醒词试验 (输入关键词, 显示命中与分数), TTS 试听 (文本框, 模型 / 说话人 / 语速, 播放与保存到文件); 退出页面释放麦克风.
- [ ] (测试) 人工验收 (E4) 两台真机, 截图入 `docs/images/screenshots`.

### P5.4 发行历史与更新检查

- [ ] (插件) `ReleaseHistoryActivity` 读取 `assets/doc/CHANGELOG-<lang>.md`; 更新对话框沿用 3-Stone AI (超时, 取消, 忽略版本, 频率限制, Neutral 按钮打开内置发行历史).

### P5.5 图标

- [x] (插件) 维护者提供源图 -> `.python/icons/three-macaw-ic-launcher-light.png` / `-dark.png` -> Icon Studio 配方 (几何与颜色按黑白规范) -> 重算资源 -> `--check` 确定性通过. 证据: 与本条记录同一提交. 源图是维护者 2026-10-16 经两轮比较选定的定稿 (浅色版按亮度分段色调映射, 不是反色; 暗色版提亮声波), 字节原样复制; 两张均为 830 x 830 RGBA, alpha 逐像素相同, 全部像素 R=G=B, 透明占 64.3%, 不透明像素灰阶浅色 39 - 78 (40 级), 暗色 134 - 253 (100 级). 内容地址 `726b2b45...` (light, 亮色主题) 与 `da03ee67...` (dark, 暗色主题) 放入 `.icons/assets/`; 配方 `baseline` 仍引用 P0 占位图, 两份占位资产按工作台应用时的做法保留. 生成内核仍是仓库自带的 1.3.0 `icon_studio_runtime.py` (未升级), 未使用工作台. 几何沿用 Optical geometry v1 默认值 (normalized, scale 1.0, 偏移 0 / 0; 原值并非为占位图调整): 视觉尺寸 V = 0.5206 (目标 0.52), 列表圆最大半径 190.2 / 216 px, 自适应前景最大半径 126.6 / 132 px, 无错误与提醒; 着墨质心偏右 8.1 px 属鹦鹉身体的自然重心, 按规范 4.2 不强制归零. 色调由 `mask` 改为 `grayscale` (锚点字段仍为 `#272727` / `#d8d8d8`, 底色 `#fafafa` / `#212121`): `mask` 会把翅膀, 喙与声波的灰阶层次压成单一颜色, 按黑白规范第 2 节 (不把灰阶原稿涂成纯色) 与 3-Stamp Mail 的先例保留灰阶; 输出亮色灰阶 37 - 82, 暗色 137 - 255, 两主题 alpha 相同. `py .python/generate_icon_studio.py` 重写 9 张 PNG, `--check` 校验 17 项 (16 项输出 + Manifest 图标引用) 通过, 再次生成无字节变化. 亮暗底对照图 (插件中心与系统圆形 36 / 48 / 64 / 96 / 432 px, 圆角方形, `mask` 对比, 15 个 Three 插件 48 / 64 px 横向比较) 已交维护者审阅; 合成图不等于真机验收. 10 种语言 changelog 删除 "当前图标为临时图案" 提示, README 与插件中心说明的同类句子一并删除后重新生成. 偏差: 原计划的 "24 项资源 + 四个启动器 alias" 依赖首个界面的启动器入口, 移到下一条.
- [ ] (插件) 启动器资源与四个 alias: P5.1 的 `HomeActivity` 加入 MAIN / LAUNCHER alias 时, 配方改为 `launcher=true` 并补充 `ic_launcher_system*` 输出与 auto XML (从同一配方渲染, 不另调几何; 自适应前景已满足 33 dp 安全圆), `--check` 确定性通过; 索引 `catalog-icons` 登记图稿 (P6.4).

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
- [ ] (发布) 对应源码 (D32, GPL-3.0 第 6(d) 条): 按 sherpa-onnx `v1.13.8` 及 kaldi-decoder `v0.3.0` 的 cmake 固定 URL 与 SHA-256 收集 sherpa-onnx 源码与构建脚本, eSpeak NG, piper-phonemize, kaldi-native-fbank, kaldi-decoder, kaldifst, OpenFst, Eigen, simple-sentencepiece, nlohmann/json, hclust-cpp, 逐个校验上游 SHA-256 后打成确定性归档, 与 APK 并列作为每个 Release 的附件, 归档 SHA-256 写入锁文件与 SHA256SUMS; README 许可小节写明 APK 整体按 GPL-3.0-or-later 传播. 更换 AAR 版本时重做.

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

上游归档体积为 2026-10-16 从 sherpa-onnx GitHub Release 资源实测 (`tar.bz2`, 多数含 fp32 与 int8 两套及测试 wav); 镜像重打包只含所需精度与文件, "安装后" 与 "镜像" 为 P2.2 实测 (2026-10-16, Shared-Assets `c7d1adf`; SenseVoice 与中文流式 2025 两行按同日两项维护者决定由 `89f04a6` 更新). 表中体积单位 MB 均按 1048576 字节计. 目录 ID 已于 P2.2 定稿, 许可结论与证据见 Shared-Assets `speech-models/audits/license-audit-v1.json`.

| 目录 ID | 上游资源 | kind / 类型 | 语言 | 上游归档 | 安装后 (实测) | 许可 (P2.2 审核) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` | `sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20` | asr, 流式 transducer | zh, en | 487.6 MB | 189.8 MB (encoder.int8 173.5 + decoder 13.2 + joiner.int8 3.1); 镜像 189.9 MB | Apache-2.0, commercialUse=true (训练数据未公开) | 默认流式模型, 支持热词 (`bpe.vocab`) |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | `sherpa-onnx-streaming-zipformer-zh-int8-2025-06-30` | asr, 流式 transducer | zh | 126.5 MB | 159.6 MB (encoder.int8 153.7); 仅上游, 不镜像 | 权重未声明许可, commercialUse=false (MagicData / KeSpeech / WenetSpeech 仅限非商用) | 中文更新模型; 2026-10-16 维护者决定不镜像 (权重无许可且上游仓库 gated), 目录 `mirror` 为 null, 只从 sherpa-onnx Release 资源安装 (附录 D Q10) |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` | `sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17` | asr, 离线 (VAD 切段) | zh, en, ja, ko, yue | 155.5 MB | 228.5 MB (model.int8 228.2); 镜像 228.5 MB | FunASR 模型许可 1.1, commercialUse=true (注明出处并保留模型名) | 官方导出, 支持标点与 ITN; 2026-10-16 维护者决定替换 2025-09-09 粤语微调版 (WenetSpeech-Yue CC BY-NC 4.0 微调, 无标点, 来源目录已删除; 附录 D Q11); 已设为推荐 (`62d4a30`) |
| `asr.zipformer-streaming.en.2023-06-26.int8` | `sherpa-onnx-streaming-zipformer-en-2023-06-26-mobile` | asr, 流式 transducer | en | 291.3 MB | 67.1 MB (encoder.int8 64.8); 镜像 67.1 MB | Apache-2.0, commercialUse=true (LibriSpeech, CC BY 4.0) | 英文轻量; 归档无 `bpe.vocab`, 不支持热词 |
| `tts.vits.melo.zh-en` | `vits-melo-tts-zh_en` | tts, vits | zh, en | 159.3 MB | 169.2 MB; 镜像 169.2 MB | MIT, commercialUse=true | 1 说话人, 中英混读, 44.1 kHz |
| `tts.kokoro.multi-lang.v1_1` | `kokoro-multi-lang-v1_1` | tts, kokoro | zh, en | 347.9 MB | 386.9 MB (model 310.5 + voices 51.3 + espeak-ng-data); 镜像 387.2 MB | Apache-2.0 + GPL-3.0-or-later (espeak-ng-data), commercialUse=true | 103 说话人 (100 中文, 3 英文), 24 kHz; int8 版 (140.2 MB) 可作后续候选 |
| `tts.vits.icefall.zh-aishell3` | `vits-icefall-zh-aishell3` | tts, vits | zh | 30.1 MB | 31.2 MB (不含 172.3 MB 的可选 `rule.far`); 镜像 31.3 MB | 权重未声明许可, commercialUse=unknown (AISHELL-3 为 Apache-2.0) | 174 说话人, 8 kHz, 轻量, 设备测试首选 |
| `tts.vits.piper.en-us-lessac-medium` | `vits-piper-en_US-lessac-medium` | tts, vits | en | 64.1 MB | 77.4 MB (model 60.2 + espeak-ng-data); 镜像 77.7 MB | MIT + GPL-3.0-or-later (espeak-ng-data), commercialUse=false (Lessac Blizzard 2013 仅限研究) | 1 说话人, 22.05 kHz; `-int8` 版 (20.0 MB) 可作后续候选 |
| `tts.matcha.icefall.zh-baker` | `matcha-icefall-zh-baker` + `vocoder-models/hifigan_v2.onnx` | tts, matcha | zh | 72.0 MB + 3.6 MB | 77.2 MB (acoustic 72.1 + hifigan_v2 3.6); 镜像 77.2 MB | 权重未声明许可 + MIT (声码器), commercialUse=false (标贝语料仅限非商用) | D11 提示, 22.05 kHz |
| `kws.zipformer.zh-en.2025-12-20` | `sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20` | kws | zh, en | 31.4 MB | 8.4 MB (chunk-16 int8 encoder / joiner + en.phone 3.2); 镜像 8.4 MB | Apache-2.0, commercialUse=true (训练数据未公开) | 开放词表 |
| `vad.silero.v5` | `silero_vad_v5.onnx` | vad | 任意 | 2.2 MB | 随 APK | MIT | 不在下载目录, 只在许可审核中登记 |

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

`sampleRate`, `size` 与 `sha256` 均为 P2.2 实测后填入, 上面的数值仅示意. 签名信封与 `codegen-models/schemas/signature-v1.schema.json` 同构, 密钥独立. P2.1 定稿 (2026-10-16) 的正式结构见 Shared-Assets `speech-models/schemas/catalog-v1.schema.json`, 相对上面草案的差异: 模型级 `modelId` / `kind` / `displayName` (草案的 `name`) / `summary` / `recommended` 与不可变的 `versions[]` 分离 (同 codegen); `upstream` 为数组, 元素为带 `members` 的 `tar.bz2` 或带 `path` 的单文件 `file`; 新增 `runtime.config` (sherpa-onnx 文件角色, 如 `encoder`, `tokens`, `lexicon[]`, `ruleFsts[]`, `dataDir`, transducer 的 `modelType`) 与 `sourceUrl`; `license.commercialUse` 取 true / false / `"unknown"`; 草案的 `status` 由 `deprecated` / `revoked` 取代. 2026-10-16 起 `versions[].mirror` 可为 `null` (维护者决定不镜像的仅上游版本, 附录 D Q10; 字段仍必填, schema 为 `oneOf [null, object]`), 此时 `upstream[]` 须完整覆盖 `files[]`, 该版本不出现在 `release-assets.txt` 中, 发布工作流不重建也不上传; 已发布版本的 `mirror` 与其他字段一样不可变.

---

## 附录 D. 待决事项

| 编号 | 事项 | 选项 | 拍板时点 |
| --- | --- | --- | --- |
| Q1 | 安装向导分类 | (a) `TOOLS` (与 3-Stone AI / 3-Stove Agent 同列, 宿主零枚举改动); (b) 新分类 `SPEECH` 或 `AUDIO` (需宿主枚举, 10 语言标题, 插件中心分组) | 已定 (a) `TOOLS` (2026-10-16 按推荐项, P1.3) |
| Q2 | sherpa-onnx AAR 来源 | (a) Maven Central 坐标 (若 1.13.8 存在且字节一致); (b) `libs/` + 锁文件 | P0.2 已定 (b): Maven Central 无坐标, JitPack 只是同字节转存且按需重建 (D13) |
| Q3 | 镜像归档格式 | (a) 未压缩 `tar` (模型本身已是二进制, 解包快); (b) `zip` (Android 原生, 可随机访问); (c) 与上游一致 `tar.bz2` | P2.1 已定 (a): 规范 USTAR (只含普通文件, 按路径排序, 0644, uid / gid 0, mtime 0, 末尾两个零块), 由标准库写出, 字节与 Python / zlib / bzip2 版本无关, 体积可由 `files[]` 算出; zip 需要确定的 deflate 或存储模式, tar.bz2 解压慢 (Shared-Assets `c7d1adf`) |
| Q4 | 唤醒词原文转 token | (a) 上游 Kotlin API 已内置 (spike 结论); (b) 插件内置拼音表 + BPE 编码; (c) 关键词只接受拼音 / 英文 token (不推荐) | P3.4 spike 后 |
| Q5 | 后台麦克风不成立的机型 | (a) 插件自身 microphone 前台服务; (b) 文档声明限制 | 已定 (a) (2026-10-16 按推荐项, P1.2 实测: API 35 上宿主在后台且自身无麦克风能力时插件读到静音帧); 实现列入 P3.1, D29 的前台服务用途随之扩展到脚本的麦克风会话 |
| Q6 | 系统 TTS 引擎的双语 Voice 报告方式 | (a) 每个说话人报告 zh-CN 与 en-US 两个 Voice; (b) 只报告 zh-CN 并在 `onIsLanguageAvailable` 对 en 返回 `LANG_AVAILABLE` | P4.2 |
| Q7 | 模型下载是否默认允许计量网络 | (a) 默认询问 (与 3-Stone AI 一致); (b) 默认允许 | 脚本路径已按实现确定 (P2.3): Binder `installModel` 不检查计量网络, 脚本显式调用即视为同意; 界面下载按钮的默认行为待维护者在 P5.1 之前于 (a) / (b) 中拍板, 推荐 (a) |
| Q8 | 识别结果是否默认写入宿主控制台日志 | (a) 不记录 (隐私); (b) 仅 `final` 级别 debug 日志 | 已定 (a) 不记录 (2026-10-16 按推荐项, P1.4) |
| Q9 | 运行时内 eSpeak NG (GPL-3.0-or-later) 的处理 | (a) 保留官方 AAR, 声明 GPL 并随 Release 提供对应源码; (b) 自编译 `SHERPA_ONNX_ENABLE_TTS=OFF` (1.0.0 失去 TTS, 推翻 D2 / D3 / D13); (c) 自编译保留 TTS 但剔除 eSpeak NG (需改 sherpa-onnx 源码, piper 不可用); (d) 暂停发布等 sherpa-onnx 2.0.0 | 已定 (a) (2026-10-16 按推荐项, D32); sherpa-onnx 发布移除 eSpeak NG 的版本 (k2-fsa/sherpa-onnx#3731) 后重新评估: 迁移并删去 GPL 小节与对应源码附件, 同时核对 piper / kokoro 改用 lexicon 后的目录变化 |
| Q10 | 中文流式 2025 模型 (`asr.zipformer-streaming.zh.2025-06-30.int8`) 是否镜像 | (a) 镜像到 `speech-models-vN` Release; (b) 保留目录条目但仅走上游 URL, 不镜像; (c) 移出目录 | 已定 (b) (2026-10-16 维护者决定): 权重无许可, 原始 Hugging Face 仓库 gated, 训练集含 MagicData / KeSpeech / WenetSpeech 非商用语料, 不镜像可避免由维护者再分发; 目录 `mirror` 为 null, `files[]` / `upstream[]` / 许可提示不变, 校验与发布工作流跳过该条目 (Shared-Assets `89f04a6`, P2.2) |
| Q11 | SenseVoice 采用哪个导出 | (a) 2025-09-09 粤语微调版 (WenetSpeech-Yue CC BY-NC 4.0, 无标点, commercialUse=false); (b) 官方 2024-07-17 int8 导出 (FunASR 模型许可 1.1 可商用, 带标点) | 已定 (b) (2026-10-16 维护者决定): 新目录 ID `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8`, 2025-09-09 来源目录删除 (pending 未签名), D10 与附录 A 已更新 (Shared-Assets `89f04a6`, P2.2); 维护者同日定为推荐 (`recommended=true`, Shared-Assets `62d4a30`) |
| Q12 | `speech-models-v1` 签名之前内置目录如何使用 | (a) 内置 pending 快照 (无签名) 作为首个目录来源, 允许加载并标注 "未签名快照"; 刷新得到的目录必须验签, 版本单调, 原子替换; 内置 `.sig.json` 存在时同样必须验签; 已签名目录可替换同版本的未签名快照; (b) 签名到位前不内置目录 (模型列表为空, 只能刷新); (c) 内置目录也必须带签名, 缺签名即拒绝加载 | 暂按 (a) 实现 (P2.4, 主会话给出的默认策略), 待维护者确认: APK 本身的签名保护内置字节, 网络来源的目录一律验签; 仓库测试 `BundledCatalogTest` 在 `.sig.json` 出现后强制验签通过且 keyId 与内置公钥一致; P2.5 提升后内置快照换成签名目录与签名的原字节 |

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
| sherpa-onnx | `v1.13.8` (2026-09-10), Apache-2.0, `sherpa-onnx-1.13.8.aar` 47.8 MB (4 ABI, onnxruntime 1.28.2); Maven Central 无官方坐标, AAR 入库 `libs/` 并以 SHA-256 锁定 (P0.2) |
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
