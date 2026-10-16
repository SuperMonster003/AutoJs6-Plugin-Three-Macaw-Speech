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

> 当前为开发预览版: 插件可以被 AutoJs6 识别并加载语音运行时, 但识别, 唤醒词, 合成, 模型下载, 插件界面与 `speech` 脚本模块尚未交付; 当前图标为临时图案, 正式图标待替换. 下文描述的是 1.0.0 的设计目标, 进度见开发路线图.

******

### 功能亮点

******

- 语音识别: 麦克风流式听写 (实时返回部分结果与整句结果) 与 WAV 文件识别, 提供中文, 英文, 中英混合以及日语, 韩语, 粤语的离线模型.
- 语音活动检测: 内置 silero VAD v5, 既可单独切分音频中的语音段, 也用于听写时的自动断句.
- 唤醒词: 直接填写关键词原文 (例如 `小六小六` 或 `hey macaw`), 无需训练模型, 唤醒后可自动转入听写.
- 语音合成: 多种中英文神经网络音色, 可合成到 WAV 文件, 也可以作为系统 TTS 引擎供 AutoJs6 的 `tts` 模块和其他应用使用.
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

录音在插件进程内完成, 音频不经过 AutoJs6 传输. AutoJs6 绑定插件时会传递前台能力, 因此脚本在后台也能听写. 首次使用前请在插件界面授予麦克风权限.

#### 应该安装哪个安装包?

大多数手机选择 `arm64-v8a`. 不确定设备 ABI 时安装 `universal`, 它适用于全部设备, 但体积更大.

******

### 权限与安全

******

插件在设计上尽量收窄权限面:

- 麦克风: 只在听写, 唤醒词与试听试说时录音, 会话结束或脚本退出后立即释放麦克风.
- 网络: 只用于下载模型与刷新签名的模型目录, 识别与合成不联网.
- 存储: 模型保存在插件私有目录, 导入本地模型通过系统文件选择器完成, 不申请存储权限.
- 受保护的入口: 能力服务与激活入口均受 AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`) 保护, 第三方应用无法直接调用.

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

- `提示` 开发预览: 插件可以被 AutoJs6 识别并加载语音运行时, 语音识别, 唤醒词, 语音合成, 模型下载, 插件界面与 speech 脚本模块尚未交付
- `提示` 需要 AutoJs6 6.8.0 (5451) 或更高版本 (准确的最低版本号待宿主侧改动落地后回填)
- `提示` 当前图标为临时图案, 正式图标待替换
- `新增` 插件骨架: Wake 激活协议, 插件中心身份服务与语音能力服务
- `新增` 内置 sherpa-onnx 1.13.8 语音运行时与 silero VAD v5 语音活动检测模型, 按 4 种 ABI 分别提供安装包
- `新增` 10 种语言的 README, 插件中心说明与更新日志, 由 JSON 源文件统一生成
- `依赖` 附加 sherpa-onnx 版本 1.13.8
- `依赖` 附加 Jetpack Compose BOM 版本 2026.09.00
- `依赖` 附加 AndroidX Activity Compose 版本 1.12.2
- `依赖` 附加 AndroidX Core KTX 版本 1.15.0
- `依赖` 附加 Kotlin Coroutines 版本 1.11.0

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
