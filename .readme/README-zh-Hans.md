<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-macaw-speech-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>离线语音识别, 唤醒词检测, 语音活动检测与神经网络语音合成, 可注册为系统 TTS 引擎</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言 (Languages)

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ar.md)

******

### 简介

******

3-Macaw Speech 基于 [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) 在设备上离线完成语音识别, 语音活动检测, 唤醒词检测与神经网络语音合成. 全部计算都在本机进行, 音频不会上传.

插件是独立安装的 APK. AutoJs6 脚本通过全局模块 `speech` 使用这些能力; 合成能力还注册为 Android 系统 TTS 引擎, AutoJs6 的 `tts` 模块与其他应用都可以选用. 识别与合成模型不随 APK 提供, 在插件界面或脚本中按需下载, 只有语音活动检测模型随包内置.

> 当前为开发预览版: 插件可以被 AutoJs6 识别, 可以下载与管理模型, 识别语音, 检测语音活动与唤醒词, 合成语音并作为系统 TTS 引擎使用, 插件自己的界面可以管理与试用模型并调整设置; 提供 `speech` 脚本模块的 AutoJs6 版本尚未发布. 下文描述的是 1.0.0 的设计目标, 进度见开发路线图.

******

### 功能亮点

******

- 语音识别: 麦克风流式听写 (实时返回部分结果与整句结果) 与 WAV 文件识别, 提供中文, 英文, 中英混合以及日语, 韩语, 粤语的离线模型.
- 语音活动检测: 内置 silero VAD v5, 既可单独切分音频中的语音段, 也用于听写时的自动断句.
- 唤醒词: 直接填写关键词原文 (例如 `小六小六` 或 `hey macaw`), 无需训练模型, 唤醒后可自动转入听写.
- 语音合成: 多种中英文神经网络音色, 可合成到 WAV 文件, 也可以作为系统 TTS 引擎供 AutoJs6 的 `tts` 模块和其他应用使用.
- 系统语音输入: 其他应用 (例如 3-Stove Agent) 的麦克风按钮可以使用离线识别, 在底部弹层中说一句话, 识别出的文字直接交回该应用.
- 数字助理: 把 3-Macaw Speech 选为默认数字助理应用后, 长按电源键或 Home 键说一句话, 需要时编辑识别出的文字, 再交给 3-Stove Agent, 3-Stone AI 等接收语音指令的应用, 或复制. 它也提供系统语音识别服务: Android 11 及以下选它为助理时一般会一并设为默认识别服务; Android 12 起需在系统的语音输入设置里另行选择 (部分手机没有这项设置), 应用也可以直接指定它.
- 模型管理: 从签名的模型目录下载, 逐个文件校验 SHA-256 后才启用, 也可以导入本地模型; 仅限非商业用途的模型在下载前会提示许可.

******

### 使用方法

******

1. 将 AutoJs6 升级到内部版本号 5451 (6.8.0) 或更高版本.
2. 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) 下载与设备 ABI 对应的 APK (不确定时选择 `universal`) 并安装, 然后在 AutoJs6 的插件中心授权并启用.
3. 打开插件界面授予麦克风权限并下载需要的模型, 然后在脚本中使用 `speech` 模块.

> 插件支持 Android 7.0 (API 24) 及以上的设备. 每种 ABI 各有一个安装包, 另有适用于全部设备的 `universal` 安装包.

******

### 模型

******

识别, 唤醒词与合成模型的体积在 30 MB 到 350 MB 之间, 因此不随 APK 提供. 插件从 AutoJs6 共享资源仓库的不可变发布下载模型, 镜像不可用时回退到 sherpa-onnx 的上游地址, 两条路径都要逐个文件校验 SHA-256 后才启用.

每个模型都标注了许可. 仅限非商业用途的模型 (例如使用 DataBaker 语料训练的合成模型) 在下载前会提示, 脚本下载时需要显式传入 `acceptLicense: true`.

******

### 脚本 API

******

脚本通过全局模块 `speech` (也可写作 `$speech`) 使用这些能力 (1.0.0 设计草案, 以发布版本的文档为准):

```javascript
let session = speech.listen({ language: "zh", silenceEndMillis: 1500 });
session.on("partial", (e) => console.log("...", e.text));
session.on("final", (e) => files.append("./dictation.txt", e.text + "\n"));
sleep(30000);
session.stop();

let wake = speech.wake(["hey macaw"], { thenListen: true });
wake.on("wake", (e) => e.listen.on("final", (r) => console.log(r.text)));

speech.synthesize("Task completed", "./done.wav");
tts("Task completed", { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" });
```

- `speech.listen(options)` 打开听写会话, 通过 `partial`, `final` 与 `end` 等事件返回结果; `speech.recognize(path)` 识别 WAV 文件.
- `speech.wake(keywords, options)` 打开唤醒词会话; `thenListen` 让唤醒后自动转入听写.
- `speech.synthesize(text, path)` 合成到 WAV 文件; `speech.speak(text)` 与 `tts(text, {engine})` 通过系统 TTS 引擎播报; `speech.models` 管理模型.

******

### 常见问题

******

#### 识别与合成需要联网吗?

不需要. 模型下载完成后, 全部计算都在设备上进行, 音频不会上传. 只有下载模型与刷新模型目录时才会联网.

#### 为什么由插件自己申请麦克风权限?

录音在插件进程内完成, 音频不经过 AutoJs6 传输. AutoJs6 绑定插件时会传递前台能力, 插件在麦克风会话期间还会持有麦克风前台服务, 因此会话开始后 AutoJs6 退到后台或熄屏也能继续听写. 首次使用前请在插件界面授予麦克风权限.

#### 应该安装哪个安装包?

大多数手机选择 `arm64-v8a`. 不确定设备 ABI 时安装 `universal`, 它适用于全部设备, 但体积更大.

#### 如何把 3-Macaw Speech 用作系统文字转语音引擎?

先安装至少一个合成模型, 然后在系统的文字转语音设置 (通常位于无障碍或语言设置中) 将首选引擎设为 3-Macaw Speech. 模型的每个说话人都是一个语音, 名为 `<模型 ID>/<说话人编号>`; 双语模型另有 `<模型 ID>/<说话人编号>/en-US` 用于英文. AutoJs6 脚本也可以不改系统设置直接选用该引擎: `tts(text, {engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech"})` 或 `speech.speak(text)`.

#### 其他应用能用 3-Macaw Speech 做语音输入吗?

可以. 安装识别模型并授予麦克风权限后, 调用系统语音输入的应用 (例如 3-Stove Agent 的麦克风按钮) 会打开 3-Macaw Speech 的弹层. 设备上还装有 Google 等语音输入应用时, Android 会询问用哪一个, 并可记住选择. 不想让其他应用看到这个入口时, 在设置的识别组中关闭作为系统语音输入开关.

#### 怎样把 3-Macaw Speech 设为默认数字助理?

打开 3-Macaw Speech 的设置, 点按识别组中的数字助理, 系统会打开相应的设置页, 在其中把 3-Macaw Speech 选为数字助理应用 (部分系统的页面名称略有不同). 之后长按电源键或 Home 键, 或使用助理手势, 即可听写. 如果手机的电源键已设为其他功能, 可改用 Home 键或助理手势.

#### 为什么助理不一直监听唤醒词?

一直监听需要始终开着麦克风, 常驻通知与麦克风指示灯, 也更耗电, 所以数字助理只在你按下助理键时才收音. 3-Stone AI, 3-Stove Agent 等应用可以提供自己的常驻监听开关 (默认关闭); 它们监听期间, 数字助理, 语音输入与语音识别照常可用, 唤醒词检测会暂停并在结束后自动恢复. 3-Macaw Speech 自带的唤醒词计划在后续版本提供.

******

### 权限与安全

******

插件在设计上尽量收窄权限面:

- 麦克风: 只在听写, 唤醒词与试听试说时录音, 会话结束或脚本退出后立即释放麦克风.
- 网络: 只用于下载模型与刷新签名的模型目录, 识别与合成不联网.
- 存储: 模型保存在插件私有目录, 导入本地模型通过系统文件选择器完成, 不申请存储权限.
- 受保护的入口: 能力服务与激活入口均受 AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`) 保护, 第三方应用无法直接调用; 系统 TTS 引擎及其语音检查, 示例文本与设置入口按 Android 的约定对所有应用开放, 只提供语音合成.
- 系统语音输入: 与系统 TTS 引擎一样向所有应用开放; 只在弹层显示时录音, 只返回识别出的文字, 可在设置中关闭.
- 数字助理: 不读取屏幕内容, 平时不在后台收音; 只在弹层显示时录音, 识别出的文字只在你点按应用按钮或复制时离开弹层 (锁屏时需先解锁). 语音识别服务只为本身持有麦克风权限的应用识别.

******

### 插件接口

******

以下信息面向 AutoJs6 宿主与插件开发者, 宿主通过这些标识发现插件并完成能力协商:

```text
application id: io.github.supermonster003.autojs6.plugin.three.macaw.speech
plugin id: three-macaw-speech
engine: speech
variant: default
info service: org.autojs.plugin.INFO (category three-macaw-speech)
capability service: org.autojs.plugin.SPEECH (category speech)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 5451
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### 开发路线图

******

功能规划, 设计决策与完成情况以可勾选清单维护在 ROADMAP.md 中. 未勾选条目表示规划意向, 不代表当前版本能力.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.0.0

_2026/10/16_

- `提示` 开发预览: 插件可以被 AutoJs6 识别, 可以下载与管理模型, 识别语音文件, 外部 PCM 与麦克风语音, 检测语音活动与唤醒词, 合成语音并作为系统 TTS 引擎使用; 可以在插件自己的界面中管理与试用模型; 提供 speech 脚本模块的 AutoJs6 版本尚未发布
- `提示` 需要 AutoJs6 6.8.0 (5451) 或更高版本 (准确的最低版本号待宿主侧改动落地后回填)
- `新增` 插件骨架: Wake 激活协议, 插件中心身份服务与语音能力服务
- `新增` 内置 sherpa-onnx 1.13.8 语音运行时与 silero VAD v5 语音活动检测模型, 按 4 种 ABI 分别提供安装包
- `新增` 模型管理: 按签名的模型目录下载离线语音模型, 优先使用 AutoJs6 共享资源的发布, 不可用时回退到 sherpa-onnx 官方发布; 逐个文件校验 SHA-256 后启用, 支持断点续传, 本地导入 (tar, tar.bz2, zip), 删除与默认模型, 仅限非商业用途或许可未知的模型需先接受许可
- `新增` 语音识别: 识别 WAV 文件 (8 / 16 / 24 / 32 位整数或浮点, 任意采样率与声道数, 在插件内重采样为 16 kHz 单声道), 结果按语音段带起止时间, SenseVoice 模型另带语言, 情绪与事件标签; 流式模型可使用热词
- `新增` 听写会话: 由插件录音或接收 16 kHz PCM, 依次报告就绪, 中间结果, 整句结果, 端点, 语音活动与结束; 可调整断句静音时长, 无语音超时与最长时长; 流式模型逐词更新中间结果, 离线模型按语音段更新
- `新增` 语音活动检测: 以内置 silero VAD 列出 WAV 文件中的语音段
- `新增` 唤醒词: 开放词表的关键词检测, 中文关键词按常用读音拼写为拼音, 英文单词按模型词典拼写为音素, 也可以直接写模型 token; 命中后可在同一录音上自动转入一轮听写
- `新增` 麦克风会话期间插件以麦克风前台服务与通知继续录音, 在 AutoJs6 处于前台时开始的会话在 AutoJs6 退到后台或熄屏后仍可收音; 同一时间只有一个麦克风会话; 缺少麦克风权限时报 PERMISSION_DENIED 并指明授权页面
- `新增` 语音合成: 用已安装的合成模型 (VITS, Matcha 与 Kokoro, 目录中有 5 种中英文模型) 把最多 8000 个字符的文本逐句合成为 WAV 文件, 采样率与模型一致; 可选择说话人与语速 (0.5 到 2 倍), 按已完成的字符报告进度, 可随时取消; 只有一个说话人的模型会忽略所选说话人并给出提示
- `新增` 系统 TTS 引擎: 安装合成模型后, 可在系统的文字转语音设置中选择 3-Macaw Speech, AutoJs6 的 tts 模块与其他应用都能用它朗读; 每个说话人对应一个语音, 双语模型的每个说话人另有一个英文语音, 语速跟随系统或应用的设置; 未安装模型时引擎报告缺少语音数据
- `新增` 首页: 显示语音运行时版本, 已安装的模型数量, 麦克风权限 (可一键授权), 以及 3-Macaw Speech 是否为首选的系统 TTS 引擎
- `新增` 首页的模型按识别, 唤醒词, 合成与语音活动检测分组, 显示大小, 语言与许可, 可下载, 暂停, 继续, 取消, 删除与设为默认; 下载进度同时显示在通知中; 仅限非商业用途或许可未知的模型需先确认许可, 使用移动数据等计量网络下载前会先询问
- `新增` 从 tar, tar.bz2 或 zip 归档或文件夹导入本地模型: 与目录模型一致的文件按该模型登记, 其他文件在填写种类, 模型类型, 名称, 语言与 ID 后作为自定义模型导入
- `新增` 试听试说: 实时听写 (中间结果显示为灰字), 可切换模型与语言; 唤醒词试验列出命中的关键词与分数; 输入文本后选择模型, 说话人与语速试听合成效果, 或保存为 WAV 文件; 离开页面即释放麦克风
- `新增` 系统语音输入: 其他应用 (例如 3-Stove Agent) 的麦克风按钮可以使用 3-Macaw Speech 的离线识别, 在底部弹层中说一句话, 识别出的文字直接交回该应用; 停顿后自动结束, 也可以提前停止或取消; 沿用设置页的端点灵敏度与语音活动检测, 不需要时可在设置的识别组中关闭
- `新增` 数字助理: 在系统设置中把 3-Macaw Speech 选为默认数字助理应用后, 长按电源键或 Home 键 (或使用助理手势) 即可在底部弹层中说一句话; 识别出的文字可以编辑, 再点按钮交给接收语音指令的应用 (例如 3-Stove Agent 或 3-Stone AI), 也可以复制或重说; 点按钮之前不会发送任何内容. 锁屏时也能使用, 发送或复制前会先解锁. 助理不读取屏幕内容, 平时不在后台收音. 设置的识别组会显示它是否为默认助理, 并可打开系统设置进行选择
- `新增` 系统语音识别服务: 使用 Android 语音识别接口 (SpeechRecognizer) 的应用可以直接指定 3-Macaw Speech, 使用它的离线识别并获得中间结果; 把它设为默认识别服务后, 没有指定的应用也会使用它. Android 11 及以下选它为数字助理时一般会一并设为默认; Android 12 起需在系统的语音输入设置里另行选择 (部分手机没有这项设置). 其他应用 (例如 3-Stone AI) 借助 3-Macaw Speech 持续监听唤醒词时, 数字助理, 系统语音输入, 语音识别服务与试听试说页仍可使用麦克风: 期间唤醒词检测暂停, 结束后自动恢复
- `新增` 设置: 语言, 夜间模式, 主题色与启动器图标 (默认跟随 AutoJs6); 默认识别与合成模型, 端点灵敏度, 语音活动检测, 说话人, 语速与各语言的默认语音, 作用于试听试说页与系统 TTS 引擎 (脚本仍使用文档中的默认值); 空闲模型的释放时间, 刷新模型目录与回收未引用的文件; 系统文字转语音设置中 3-Macaw Speech 的设置按钮会打开此页
- `新增` 关于: 版本, 插件内置的发行历史, 更新检查 (每 12 小时至多自动检查一次, 可忽略某个版本), 以及许可与声明: 应用整体按 GNU GPL 3.0 或更新版本传播且不提供任何担保, 内含第三方声明, GPL 全文, Unicode 使用条款与对应源码的位置
- `新增` 启动器图标: 可选自适应图标 (亮色), 自适应图标 (暗色), 自适应图标 (自动, 默认) 或透明背景图标
- `新增` 10 种语言的 README, 插件中心说明与更新日志, 由 JSON 源文件统一生成
- `依赖` 附加 sherpa-onnx 版本 1.13.8
- `依赖` 附加 Jetpack Compose BOM 版本 2026.09.00
- `依赖` 附加 AndroidX Activity Compose 版本 1.12.2
- `依赖` 附加 AndroidX Core KTX 版本 1.15.0
- `依赖` 附加 Kotlin Coroutines 版本 1.11.0
- `依赖` 附加 Material Color Utilities (MaterialKolor) 版本 4.1.1

##### 更多发行历史可参阅

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建

******

本节面向希望从源码构建插件的开发者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\verify_native_alignment.py --check
py .python\check_repository.py
```

******

### 许可

******

项目代码使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). 语音运行时 sherpa-onnx 使用 Apache-2.0 许可, onnxruntime 与 silero VAD 使用 MIT 许可; 每个模型的许可在模型目录中逐条列出.

- [第三方声明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
- sherpa-onnx 项目: https://github.com/k2-fsa/sherpa-onnx
