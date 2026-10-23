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

插件是独立安装的 APK, 有自己的界面, 用来下载与管理模型, 试听试说并调整设置. AutoJs6 脚本通过全局模块 `speech` 使用这些能力; 合成能力还注册为 Android 系统 TTS 引擎, 识别能力还以系统语音输入, 系统语音识别服务与数字助理的形式提供, 因此其他应用也能使用. 识别与合成模型不随 APK 提供, 按需下载, 只有语音活动检测模型随包内置.

> 在脚本中使用 `speech` 模块需要 AutoJs6 6.8.0 (内部版本号 5456) 或更高版本. 系统 TTS 引擎, 系统语音输入, 语音识别服务, 数字助理与插件自己的界面不依赖 AutoJs6, 安装插件并下载模型后即可使用.

******

### 功能亮点

******

- 语音识别: 麦克风流式听写 (实时返回部分结果与整句结果) 与音频文件识别 (WAV, MP3, AAC, Ogg, FLAC 等, 也可以是视频文件中的声音), 提供中文, 英文, 中英混合以及日语, 韩语, 粤语的离线模型. 法语, 俄语与韩语也有流式模型, 可以边说边出字.
- 自动标点: 装上标点模型后, 流式模型听写或识别出的每句话会自动加上逗号, 句号与问号 (中文与英文), 英文还会恢复大写; SenseVoice 自带标点, 不受影响. 标点模型在首页的 "标点" 分组里下载.
- 语音活动检测: 内置 silero VAD v5, 既可单独切分音频中的语音段, 也用于听写时的自动断句.
- 唤醒词: 直接填写关键词原文 (例如 `小六小六` 或 `hey macaw`), 无需训练模型, 唤醒后可自动转入听写.
- 语音合成: 多种中英文神经网络音色, 可合成到 WAV 文件, 也可以作为系统 TTS 引擎供 AutoJs6 的 `tts` 模块和其他应用使用. 另有法语, 西班牙语, 德语与俄语的 Piper 声音 (仅限非商业用途).
- 系统语音输入: 其他应用 (例如 3-Stove Agent) 的麦克风按钮可以使用离线识别, 在底部弹层中说一句话, 识别出的文字直接交回该应用.
- 数字助理: 把 3-Macaw Speech 选为默认数字助理应用后, 长按电源键或 Home 键说一句话, 需要时编辑识别出的文字, 再交给 3-Stove Agent, 3-Stone AI 等接收语音指令的应用, 或复制. 它也提供系统语音识别服务: Android 11 及以下选它为助理时一般会一并设为默认识别服务; Android 12 起需在系统的语音输入设置里另行选择 (部分手机没有这项设置), 应用也可以直接指定它.
- 触发式入口: 不必长按电源键也能打开同一个听写弹层: 快速设置磁贴 (锁屏上也能用), 桌面小部件与应用图标快捷方式, 可拖动的悬浮麦克风 (默认关闭), 长按或双击音量键的按键触发 (只过滤按键的无障碍服务, 默认关闭), 以及蓝牙耳机的语音键. 平时不录音, 只在弹层听写的几秒内使用麦克风.
- 常驻唤醒 (默认关闭): 开启后说出唤醒词 (默认 `小鹦小鹦` 与 `Hey Macaw`, 可修改) 即打开同一个听写弹层, 熄屏或锁屏时也会点亮屏幕, 并朗读识别出的文字. 监听期间麦克风指示灯常亮, 有一条常驻通知, 也更耗电; 可设监听时长 (10, 30, 60 分钟后自动关闭或一直开着) 与只在充电时监听. 脚本, 其他应用或其他入口要用麦克风时自动让出, 用完后继续.
- 插件工具 (需要 AutoJs6 6.8.0 及以上): 提供 `speech_listen` (听一句话并返回识别出的文字), `speech_speak` (朗读一段文字), `speech_transcribe` (识别手机上的音频文件) 与 `speech_synthesize` (把文字合成为 WAV 或 M4A 文件) 四个工具. MCP Server 插件用它们让电脑上的 AI 助手通过手机听和说, 脚本也可以用 `plugins.tools` 调用.
- 模型管理: 从签名的模型目录下载, 逐个文件校验 SHA-256 后才启用, 也可以导入本地模型; 仅限非商业用途的模型在下载前会提示许可.
- 独立界面: 首页显示运行状态并管理模型, 试听试说页直接试验听写, 唤醒词与合成, 还能把手机上的音频或视频文件识别成文字, 保存为文本或 SRT 字幕, 设置页调整语言, 夜间模式, 主题色, 启动器图标与识别合成的默认值, 外观默认跟随 AutoJs6.

******

### 界面截图

******

以下截图来自 Sony XQ-DQ72 (Android 13). 听写内容是 sherpa-onnx 测试录音经手机扬声器播放后识别的结果, 不含个人数据.

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/home.png?raw=true" alt="首页: 运行状态与按用途分组的模型" width="360" />
      <br />
      <sub>首页: 运行状态与按用途分组的模型</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/tryout-dictation.png?raw=true" alt="试听试说: 实时听写" width="360" />
      <br />
      <sub>试听试说: 实时听写</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/voice-input.png?raw=true" alt="系统语音输入: 其他应用打开的底部弹层" width="360" />
      <br />
      <sub>系统语音输入: 其他应用打开的底部弹层</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/assistant.png?raw=true" alt="数字助理: 编辑识别结果并选择交给哪个应用" width="360" />
      <br />
      <sub>数字助理: 编辑识别结果并选择交给哪个应用</sub>
    </td>
  </tr>
</table>

******

### 安装与使用

******

1. 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) 下载与设备 ABI 对应的 APK 并安装: 大多数手机选择 `arm64-v8a`, 不确定时选择适用于全部设备但体积更大的 `universal`.
2. 打开 3-Macaw Speech, 在首页授予麦克风权限, 并在识别, 唤醒词与合成各组中下载需要的模型 (目录推荐的模型是较好的起点, 见下文模型表). Android 13 及以上首次下载时还会请求显示下载进度通知.
3. 在 AutoJs6 中使用: 将 AutoJs6 升级到 6.8.0 (内部版本号 5456) 或更高版本, 在插件中心启用 3-Macaw Speech, 然后在脚本中调用 `speech` 模块 (见下文脚本 API). AutoJs6 的示例代码 > 3-Macaw Speech 中有四个可直接运行的示例.
4. 在其他应用中使用: 系统 TTS 引擎见下文系统 TTS 设置; 系统语音输入, 语音识别服务与数字助理见常见问题.

> 每种 ABI 各有一个安装包, 另有适用于全部设备的 `universal` 安装包. 安装包不含识别与合成模型, 模型在插件界面或脚本中按需下载, 也可以从本地文件导入.

******

### 模型

******

识别, 标点, 唤醒词与合成模型的体积在 7 MB 到 410 MB 之间, 因此不随 APK 提供. 插件内置一份签名的模型目录, 从 AutoJs6 共享资源仓库的不可变发布下载模型, 镜像不可用时回退到 sherpa-onnx 的上游地址; 两条路径都要逐个文件校验 SHA-256 后才启用模型. 1.2.0 的目录包含以下模型 (大小为安装后占用的空间):

| 模型 ID | 用途 | 语言 | 大小 | 许可 | 商业用途 |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (推荐) | 识别 (离线, 带标点) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | 可以 |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (推荐) | 识别 (流式) | zh, en | 199.1 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.en.2023-06-26.int8` (推荐) | 识别 (流式) | en | 70.3 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | 识别 (流式) | zh | 167.4 MB | 未声明 | 仅限非商业用途 |
| `asr.zipformer-streaming.fr.2023-04-14.int8` (推荐) | 识别 (流式) | fr | 129.0 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.ru.2025-08-16.int8` (推荐) | 识别 (流式) | ru | 28.6 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.ko.2024-06-16.int8` | 识别 (流式) | ko | 140.9 MB | Apache-2.0 | 仅限非商业用途 |
| `punct.ct-transformer.zh-en.2024-04-12.int8` (推荐) | 标点 | zh, en | 75.5 MB | Apache-2.0 | 可以 |
| `punct.cnn-bilstm.en.2024-08-06.int8` (推荐) | 标点 | en | 7.6 MB | Apache-2.0 | 可以 |
| `kws.zipformer.zh-en.2025-12-20` (推荐) | 唤醒词 | zh, en | 8.8 MB | Apache-2.0 | 可以 |
| `tts.vits.melo.zh-en` (推荐) | 合成 (说话人: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | 可以 |
| `tts.vits.icefall.zh-aishell3` (推荐) | 合成 (说话人: 174, 8 kHz) | zh | 32.8 MB | 未声明 | 未知 |
| `tts.kokoro.multi-lang.v1_1` | 合成 (说话人: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | 可以 |
| `tts.matcha.icefall.zh-baker` | 合成 (说话人: 1, 22.05 kHz) | zh | 81.0 MB | 未声明, MIT (声码器) | 仅限非商业用途 |
| `tts.vits.piper.en-us-lessac-medium` | 合成 (说话人: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 仅限非商业用途 |
| `tts.vits.piper.fr-fr-siwis-medium.int8` | 合成 (说话人: 1, 22.05 kHz) | fr | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 仅限非商业用途 |
| `tts.vits.piper.es-es-davefx-medium.int8` | 合成 (说话人: 1, 22.05 kHz) | es | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 仅限非商业用途 |
| `tts.vits.piper.de-de-thorsten-medium.int8` | 合成 (说话人: 1, 22.05 kHz) | de | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 仅限非商业用途 |
| `tts.vits.piper.ru-ru-denis-medium.int8` | 合成 (说话人: 1, 22.05 kHz) | ru | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 仅限非商业用途 |

仅限非商业用途或许可未知的模型在下载前会提示许可, 脚本下载时需要显式传入 `acceptLicense: true`. 中文流式 2025 模型的权重未声明许可, 训练数据含仅限非商业用途的语料, 因此只从 sherpa-onnx 的上游地址下载, 不经共享资源仓库镜像. Kokoro 与各个 Piper 合成模型附带 eSpeak NG 的发音数据 (espeak-ng-data, GPL-3.0-or-later). 韩语流式模型的训练数据 (KsponSpeech) 只向韩国国民开放申请, 在韩国境外使用需要另签协议, 因此按仅限非商业用途标注, 也不经共享资源仓库镜像, 只从上游地址下载 (下载约 418 MB, 安装后 141 MB). 法语, 西班牙语, 德语与俄语四个 Piper 声音都由英文 lessac 声音微调而来, 与它一样仅限非商业用途.

首页的导入本地模型可以从 tar, tar.bz2 或 zip 归档或文件夹导入模型: 与目录模型一致的文件按该模型登记, 其他文件在填写种类, 模型类型, 名称与语言后作为自定义模型导入. 导入通过系统文件选择器完成, 不需要存储权限.

******

### 系统 TTS 设置

******

1. 在插件首页的合成组中下载至少一个合成模型. 想先试一试时, 体积最小的 `tts.vits.icefall.zh-aishell3` (中文, 174 个说话人) 与中英混读的 `tts.vits.melo.zh-en` 都是推荐模型.
2. 打开系统的文字转语音设置, 把首选引擎设为 3-Macaw Speech. 插件首页状态卡中的系统文字转语音引擎一行可以直接打开该页面; 手动查找时, 它通常位于 设置 > 无障碍 > 文字转语音输出, 或 设置 > 系统 > 语言和输入法 > 文字转语音输出 (各厂商的名称略有不同).
3. 点按引擎旁的设置按钮会打开 3-Macaw Speech 的设置页, 在合成组中为各语言选择默认语音并选择说话人. 语速由系统设置或请求朗读的应用决定, 音调不受支持.
4. 回到系统设置点按试听, 即可用该语言的默认语音朗读示例文本.

每个模型的每个说话人都是一个语音, 名为 `<模型 ID>/<说话人编号>`, 双语模型的说话人另有 `<模型 ID>/<说话人编号>/en-US` 用于英文. AutoJs6 脚本不修改系统设置也能使用该引擎: `tts(text, { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" })`, 或用 `speech.speak(text, { model, speaker })` 直接指定模型与说话人.

******

### 脚本 API

******

AutoJs6 6.8.0 (内部版本号 5456) 起, 脚本通过全局模块 `speech` (也可写作 `$speech`) 使用这些能力. 下面三段示例可以分别运行: 听写 30 秒并把每句话追加到文件, 用唤醒词唤醒后听一句话, 合成到文件并朗读:

```javascript
// 1. Dictation: partial text while you speak, one final per sentence, 30 seconds in all
let session = speech.listen({ silenceEndMillis: 1500, timeoutMillis: 0 });
session.on("partial", (e) => console.log("...", e.text));
session.on("final", (e) => files.append("./dictation.txt", e.text + "\n"));
setTimeout(() => session.stop(), 30000);

// 2. A wake word, then one round of dictation on the same recording
let wake = speech.wake(["hey macaw", "小六小六"], { thenListen: true });
wake.on("wake", (e) => e.listen.on("final", (r) => {
    console.log(e.keyword, r.text);
    wake.stop();
}));

// 3. Synthesis to a WAV file, and speech through the system TTS engine
let result = speech.synthesize("Task completed", "./done.wav", { model: "tts.vits.melo.zh-en" });
console.log(result.durationMillis, result.sampleRate);
speech.speak("任务完成");
tts("Task completed", { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" });
```

- `speech.listen(options)` 打开听写会话, 通过 `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` 与 `end` 事件返回结果, `stop()` 提前结束; `speech.recognize(path)` 识别音频文件 (WAV 以外的格式需要 AutoJs6 6.8.0 及以上; 也接受 16 kHz PCM), `speech.detectVoice(path)` 列出其中的语音段.
- `speech.listen` 与 `speech.recognize` 的 `punctuation` 选项默认为 true: 模型自带标点 (SenseVoice) 时保留, 流式模型的结果由已安装的标点模型补上标点 (中文与英文, 英文同时恢复大小写), 没有覆盖该语言的标点模型时保持原样; 设为 false 去掉所有标点. 标点模型只在插件首页管理, 不出现在 `speech.models` 中.
- `speech.wake(keywords, options)` 打开唤醒词会话, 关键词直接写原文 (至多 50 个); `thenListen` 让每次唤醒后在同一录音上转入一轮听写, 该轮的会话由 `wake` 事件的 `listen` 属性给出.
- `speech.synthesize(text, path, options)` 合成到 WAV 文件 (至多 8000 个字符, 可选模型, 说话人与语速); `speech.speak(text, options)` 与 `tts(text, { engine })` 经系统 TTS 引擎朗读.
- `speech.models` 列出, 下载 (`install`, 可报告进度), 导入, 删除模型与设置默认模型; `speech.capabilities()` 返回插件版本, 支持的功能与麦克风权限状态.
- 整个插件同一时间只有一个麦克风会话 (听写或唤醒); 缺少麦克风权限时调用以 `PERMISSION_DENIED` 失败, 打开插件首页授予即可. 识别出的文字不会写入 AutoJs6 的日志.
- AutoJs6 6.8.0 及以上, 脚本也可以经 `plugins.tools.call` 调用这四个插件工具, 例如 `plugins.tools.call('speech_speak', { text: '你好' })`; 参数与 MCP Server 看到的相同. 日常脚本直接用 `speech` 模块更灵活.
- 完整的方法, 选项, 事件与错误码见 [AutoJs6 文档](https://docs.autojs6.com/#/speech); AutoJs6 的示例代码 > 3-Macaw Speech 中有听写到文件, 唤醒词与听写, TTS 播报与语音命令交给 Agent 四个示例.

******

### 兼容性

******

- Android 7.0 (API 24) 及以上, 目标 API 37. arm64-v8a, armeabi-v7a, x86_64 与 x86 各有安装包, 另有 universal 安装包; 原生库按 16 KB 内存页对齐, 已在 16 KB 页模拟器上验证.
- AutoJs6: 脚本中的 `speech` 模块需要 6.8.0 (内部版本号 5456) 或更高版本; 插件界面, 系统 TTS 引擎, 系统语音输入, 语音识别服务与数字助理不依赖 AutoJs6.
- 后台收音: 在 AutoJs6 处于前台时开始的听写或唤醒会话, AutoJs6 退到后台或熄屏后仍会继续收音 (插件以麦克风前台服务与通知保持录音). AutoJs6 已在后台时才打开的会话可能被系统静音, 此时会话收到错误码为 `PERMISSION_DENIED` 的 `warning` 事件但不会结束, AutoJs6 回到前台后恢复; 需要后台收音的脚本请在 AutoJs6 可见时开启会话.
- 实测设备: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 24, 35 与 37 (16 KB 页) 模拟器.
- 性能: 在低端的 Sony G8441 上, 流式模型加载约 1.7 秒, SenseVoice 约 2.8 秒, 听写处理速度约为实时的 3 倍; MeloTTS 与 Kokoro 在这类设备上合成慢于实时, AISHELL-3 最快. Sony XQ-DQ72 等较新设备加载约 0.5 到 1.2 秒, 合成快于实时.
- 内存: 一个识别模型加载后约占 210 到 300 MB 原生内存. 插件按设备内存的四分之一 (384 MB 到 1.5 GB) 管理已加载的模型, 空闲 5 分钟后释放 (可在设置中调整为 1 到 30 分钟).
- 数字助理: 需要系统允许选择第三方数字助理应用. 唤起方式依系统而定: 长按 Home 键 (三键导航), 从屏幕底部角落斜向上滑, 或在系统设置中把长按电源键设为唤起数字助理. 实测的 ColorOS 与 HyperOS 平板 (Android 15) 把电源键与手势留给自家助理, 可以改用触发式入口.
- 触发式入口: 实测 Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), Xiaomi Pad 23046RP50C (HyperOS, Android 15), 以及 API 33 与 35 模拟器. ColorOS 与 HyperOS 在锁屏上点磁贴会先要求解锁, 解锁后打开弹层; 部分桌面接受添加小部件的请求却不显示确认, 此时按设置中的步骤手动添加. 熄屏时手机不把按键交给无障碍服务, 按键触发只在亮屏时可用. 蓝牙耳机语音键: Sony WF-1000XM4 配合 XQ-DQ72 (Android 13) 实测每次长按都打开弹层, 并用耳机麦克风收音; 其他应用 (例如 Google 应用) 也应答时系统先显示选择器. 实测 HyperOS 平板 (Xiaomi Pad 23046RP50C, Android 15) 的选择器在选中 3-Macaw Speech 后没有反应.
- 常驻唤醒: 实测 Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 35 (16 KB 页) 模拟器. 熄屏 30 分钟耗电 (Sony XQ-DQ72, Android 13; Android 按机型功耗表估算, 手机经 USB 连接, 未充电状态为模拟): 开启时 17.9 mAh, 插件 CPU 时间 454 秒 (平均约占一个 CPU 核心的四分之一), 期间一直在录音; 关闭时 0.006 mAh, 插件没有在运行. 实际耗电因机型而异.
- 插件工具: 实测 Sony G8441 (Android 9), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 24 与 35 模拟器 (API 24 只测不需要模型的部分); 经 MCP Server 调用实测 Sony G8441 (Android 9).
- 标点与 1.2.0 新增模型: 实测 Sony XQ-AT72 (Android 12). 开启标点时, 中文, 英文与中英混合录音都加上了标点, 字词与关闭时相同; 俄语与法语 Piper 声音合成的句子由同语言的流式模型识别回来, 字错率分别为 7.9% 与 0%; 西班牙语与德语声音合成正常, 韩语模型只验证了能加载 (没有韩语测试录音).

******

### 常见问题

******

#### 识别与合成需要联网吗?

不需要. 模型下载完成后, 全部计算都在设备上进行, 音频不会上传. 只有下载模型与刷新模型目录时才会联网.

#### 为什么由插件自己申请麦克风权限?

录音在插件进程内完成, 音频不经过 AutoJs6 传输. AutoJs6 绑定插件时会传递前台能力, 插件在麦克风会话期间还会持有麦克风前台服务, 因此会话开始后 AutoJs6 退到后台或熄屏也能继续听写. 首次使用前请在插件界面授予麦克风权限.

#### 应该安装哪个安装包?

大多数手机选择 `arm64-v8a`. 不确定设备 ABI 时安装 `universal`, 它适用于全部设备, 但体积更大.

#### 为什么系统设置里试听没有声音, 或者引擎显示缺少语音数据?

引擎只报告已安装的合成模型所覆盖的语言. 没有安装合成模型, 或已安装的模型不覆盖设备语言时, 系统会显示缺少语音数据, 试听与语速设置也可能不可用. 在插件首页下载一个覆盖设备语言的合成模型后重新打开系统设置即可; 试听使用该语言的默认语音, 可以在插件设置的合成组中更改.

#### 其他应用能用 3-Macaw Speech 做语音输入吗?

可以. 安装识别模型并授予麦克风权限后, 调用系统语音输入的应用 (例如 3-Stove Agent 的麦克风按钮) 会打开 3-Macaw Speech 的弹层. 设备上还装有 Google 等语音输入应用时, Android 会询问用哪一个, 并可记住选择. 不想让其他应用看到这个入口时, 在设置的识别组中关闭作为系统语音输入开关.

#### 怎样把 3-Macaw Speech 设为默认数字助理?

打开 3-Macaw Speech 的设置, 点按识别组中的数字助理, 系统会打开相应的设置页, 在其中把 3-Macaw Speech 选为数字助理应用 (部分系统的页面名称略有不同). 之后长按电源键或 Home 键, 或使用助理手势, 即可听写. 如果手机的电源键已设为其他功能, 可改用 Home 键或助理手势.

#### 常驻唤醒为什么让麦克风指示灯一直亮, 耗电多少?

持续监听唤醒词需要一直录音: Android 12 起只要有应用在录音, 状态栏就会一直显示麦克风指示灯 (绿点), 应用无法隐藏; 系统里低功耗, 不亮指示灯的热词通道只开放给预装的系统助理, 第三方应用即使被选为默认数字助理也用不了. 所以常驻唤醒默认关闭, 开启后一直显示绿点与一条常驻通知 (可在通知中直接关闭). 实测 Sony XQ-DQ72 熄屏 30 分钟, 开启时 Android 估算本插件耗电约 18 mAh (约为电池容量的 0.4%), 关闭时几乎为 0. 建议只在需要时开启: 把监听时长设为 10, 30 或 60 分钟, 或打开只在充电时监听 (拔掉电源时暂停, 接上后继续); 不常用时, 磁贴, 小部件, 悬浮麦克风等触发式入口平时不录音.

#### 为什么听到唤醒词后只出现 "点此说话" 通知?

Android 10 起, 后台应用只有在是默认数字助理, 或者获得了在其他应用上层显示的权限时, 才能直接打开界面. 3-Macaw Speech 不是默认数字助理时, 开启常驻唤醒会询问一次悬浮窗权限; 不授予时, 听到唤醒词后发出一条 "点此说话" 通知, 点按即打开弹层, 1 分钟后自动撤销. 想让唤醒词直接打开弹层, 可以把 3-Macaw Speech 设为默认数字助理, 或在系统设置中允许它在其他应用上层显示.

#### 为什么开启常驻唤醒时提示需要先关闭 3-Stove Agent?

3-Macaw Speech, 3-Stone AI 与 3-Stove Agent 的常驻监听互斥: 一部手机同一时刻只能有一个应用常驻监听, 否则它们会争用麦克风. 其中一个开着时, 另一个的开关会说明是谁在监听, 并可直接打开对方的设置去关闭. 重启或更新后, 常驻唤醒不会自动开始录音: 开关仍开着时会发一条 "点此恢复常驻唤醒" 通知, 点按或打开 3-Macaw Speech 的首页, 设置页都会恢复; 监听时长在此期间已到则直接关闭.

#### AI 助手怎样通过 3-Macaw Speech 听和说?

需要 AutoJs6 6.8.0 及以上与 MCP Server 插件. 3-Macaw Speech 是官方插件, `speech_listen`, `speech_speak` 与 `speech_transcribe` 默认出现在 MCP Server 的工具列表中 (可在 MCP Server 设置的 "插件提供的工具" 中关闭); `speech_synthesize` 会写入文件, 还要打开该组的第二个开关, 未指定位置时文件保存在 AutoJs6 工作目录的 plugin-outputs/speech 下. `speech_listen` 每次听一句话 (最长 60 秒): 能直接录音时 (例如开着常驻唤醒, 3-Macaw Speech 是默认数字助理或已获得悬浮窗权限) 立即开始, 否则手机上出现一条 "点此说话" 通知, 1 分钟内点按后开始. 听写期间其他应用的常驻监听暂时让出, 听完自动恢复; 已有听写在进行时返回 BUSY, 缺少麦克风权限或识别模型时返回 NOT_READY (打开插件首页处理即可). `speech_speak` 有该语言的 3-Macaw Speech 语音时用它朗读, 否则用系统语音.

#### 不长按电源键, 还有哪些方式打开听写弹层?

打开 3-Macaw Speech 的设置, 在触发方式组中设置: 快速设置磁贴, 点按添加 (Android 13 起由系统确认, 更早的版本按提示手动添加), 之后下拉快速设置点语音指令; 桌面小部件与快捷方式, 把按一下说话小部件放到桌面, 或长按应用图标选择语音指令; 悬浮麦克风, 打开开关并授予悬浮窗权限, 屏幕边缘会出现可拖动的麦克风按钮; 按键触发, 在系统无障碍设置中开启 3-Macaw Speech 按键触发, 默认长按音量下键约 0.8 秒, 可改为长按音量上键或双击音量下键. 这些入口都打开与数字助理相同的弹层, 说完后选择接收文字的应用; 3-Macaw Speech 是默认数字助理时经助理会话打开, 不是时直接打开同款弹层.

#### 按键触发为什么需要无障碍服务, 安全吗?

Android 只允许无障碍服务在系统处理之前拿到实体按键, 所以按键触发是一个无障碍服务. 它只申请过滤按键, 不订阅任何界面事件, 不能读取屏幕内容, 也不能执行手势; 只拦截所选的音量键, 短按照常调节音量, 熄屏, 来电或通话时不拦截. 开关是灰色时 (Android 13 起通过安装包安装的应用), 先打开 3-Macaw Speech 的应用信息页, 在菜单中选择允许受限制的设置.

#### 蓝牙耳机的语音键能打开听写弹层吗?

能. 3-Macaw Speech 应答蓝牙服务发出的语音命令请求 (VOICE_COMMAND), 长按耳机的语音键或触控区即打开听写弹层. Android 12 起, 弹层第一次会询问附近的设备权限. 允许后 3-Macaw Speech 接管耳机的语音识别: 用耳机麦克风收音, 每次按键都有效. 不允许时用手机麦克风收音, 部分耳机 (例如 Sony WF-1000XM4) 会隔一次按键才打开弹层. 手机上还有其他应用应答语音命令 (Google 应用, 厂商助理) 时, 系统会先让你选择; 实测 HyperOS 的选择器在选中 3-Macaw Speech 后没有反应, 这是系统选择器的问题. 不想使用时, 在设置的触发方式组中关闭耳机语音键.

#### 下载模型失败怎么办?

插件优先从 AutoJs6 共享资源仓库的 GitHub 发布下载, 失败时自动改用 sherpa-onnx 的上游地址, 中断的下载可以继续. 无法访问 GitHub 时, 可以在其他设备下载模型归档 (模型目录中的镜像或上游地址), 再通过首页的导入本地模型导入; 与目录一致的文件会按目录模型登记.

#### 模型占用多少空间, 怎样释放?

各模型安装后的大小见上文模型表. 首页可以删除不再需要的模型; 设置中的回收未引用的文件会删除不再需要的下载文件, 导入副本与中断的安装, 已安装的模型与暂停的下载会保留. 卸载插件会删除全部模型.

#### 识别结果为什么没有标点?

流式 Zipformer 模型只输出文字, 不加标点. SenseVoice 模型按语音段识别, 会加标点并报告语种, 适合需要完整句子的场合: 在脚本中以 `model` 选项指定它, 或在设置中把它设为默认识别模型.

#### 唤醒词怎么写, 为什么没有被唤醒?

直接写关键词原文: 中文按常用读音拼写为拼音, 英文单词按模型词典拼写 (不在词典中的单词会被拒绝), 多音字可以直接写模型的拼音 token 来指定读音. 三到四个音节, 不常见的词组更不容易误唤醒. 每个关键词可以设置 `threshold` (越低越容易唤醒) 与 `boost`. 先在试听试说页的唤醒词试验中试一试, 再写进脚本.

#### 为什么 APK 按 GPL-3.0 传播, 源码在哪里?

插件自身的代码使用 MPL-2.0. sherpa-onnx 官方 Android 库静态链接了 eSpeak NG (GPL-3.0-or-later), 所以 APK 整体按 GPL-3.0-or-later 传播. 每个发行版都附有对应源码归档 (sherpa-onnx 的源码与构建脚本及其全部依赖), 文件名含 `corresponding-source`, SHA-256 列在 `SHA256SUMS` 中; 插件源码就是本仓库在发行标签处的内容.

******

### 权限与安全

******

插件在设计上尽量收窄权限面:

- 麦克风: 只在听写, 唤醒词, 试听试说与开启常驻唤醒期间录音, 会话结束或脚本退出后立即释放麦克风.
- 网络: 只用于下载模型与刷新签名的模型目录, 识别与合成不联网.
- 存储: 模型保存在插件私有目录; 导入本地模型与在试听试说页识别文件都通过系统文件选择器选择, 识别结果也由你选位置保存, 因此不申请存储权限. 最近一次识别结果留在插件缓存里, 可在页面上清除.
- 受保护的入口: 能力服务与激活入口均受 AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`) 保护, 第三方应用无法直接调用; 系统 TTS 引擎及其语音检查, 示例文本与设置入口按 Android 的约定对所有应用开放, 只提供语音合成.
- 系统语音输入: 与系统 TTS 引擎一样向所有应用开放; 只在弹层显示时录音, 只返回识别出的文字, 可在设置中关闭.
- 数字助理: 不读取屏幕内容, 未开启常驻唤醒时不在后台收音; 只在弹层显示时录音, 识别出的文字只在你点按应用按钮或复制时离开弹层 (锁屏时需先解锁). 语音识别服务只为本身持有麦克风权限的应用识别.
- 触发式入口: 磁贴, 小部件, 快捷方式, 悬浮麦克风, 按键触发与耳机语音键本身都不录音, 只负责打开听写弹层. 按键触发是只过滤按键的无障碍服务: 不读取屏幕内容, 不执行手势, 只拦截所选的音量键 (短按照常调节音量, 熄屏, 来电或通话时不拦截). 悬浮麦克风需要悬浮窗权限, 以一条静默通知保持显示; 开机自启只用于在重启后恢复已开启的悬浮麦克风. Android 12 起耳机语音键需要附近的设备权限, 只在耳机打开的弹层中询问: 只用于让蓝牙服务把语音识别交给弹层并接通耳机麦克风, 不扫描设备, 不读取位置.
- 常驻唤醒: 默认关闭, 只能在插件界面中开启 (重启或更新后需要点按通知恢复, 不会自动录音). 唤醒词在本机检测, 音频不保存也不上传, 识别出的文字不写日志; 开启期间一直录音, 麦克风指示灯常亮, 常驻通知可直接关闭. 与 3-Stone AI, 3-Stove Agent 的常驻监听互斥, 系统同一时刻只允许一个.
- 插件工具: 工具服务同样受 AutoJs6 插件权限保护, 只有 AutoJs6 能直接调用. `speech_listen` 每次只录一句话, 录音期间有通知 (Android 12 起还有麦克风指示灯); `speech_transcribe` 只读取 AutoJs6 交来的那一个文件, `speech_synthesize` 把音频交回 AutoJs6 写到指定位置. 识别与朗读的文字都不写日志.

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
tool service: org.autojs.plugin.TOOL_PROVIDER (short name speech: listen, speak, transcribe, synthesize; AutoJs6 6.8.0+)
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### 致谢

******

3-Macaw Speech 建立在以下开源项目, 模型与数据之上, 感谢它们的作者与维护者:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) 与 [k2-fsa (新一代 Kaldi)](https://github.com/k2-fsa) 团队: 识别, 唤醒词, 语音活动检测与合成的运行时, 各模型的 ONNX 导出与 Android 库 (Apache-2.0).
- [icefall](https://github.com/k2-fsa/icefall) 训练的模型: Zipformer 流式识别模型 (中英双语 pfluo, 英文 Zengwei, 中文 2025 yuekai, 法语 shaojieli, 韩语 johnBamma), 开放词表唤醒词模型 (pkufool), 以及 k2-fsa 的 AISHELL-3 VITS 与标贝 Matcha-TTS 合成模型.
- [Vosk](https://huggingface.co/alphacep/vosk-model-small-streaming-ru) (Alpha Cephei): 俄语小型流式识别模型 (Apache-2.0).
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / 阿里巴巴, FunASR 模型许可 1.1): 多语种离线识别模型.
- 标点模型: [FunASR](https://github.com/modelscope/FunASR) (阿里巴巴达摩院) 的 CT-Transformer 中英文标点模型, 以及 [Edge-Punct-Casing](https://github.com/frankyoujian/Edge-Punct-Casing) (frankyoujian) 的英文标点与大小写模型 (均为 Apache-2.0).
- [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, 中文录音由 LongMaoData 授权) 与 [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT) 合成模型, [HiFi-GAN](https://github.com/jik876/hifi-gan) 声码器 (MIT). 法语 siwis, 西班牙语 davefx, 德语 thorsten 与俄语 denis 声音来自 [piper-voices](https://huggingface.co/rhasspy/piper-voices) (MIT), 均由英文 lessac 声音微调.
- 训练数据: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), [标贝中文标准女声音库](https://www.data-baker.com/open_source.html) (仅限非商业用途), Lessac Technologies 的 Blizzard 2013 数据 (仅限研究) 与 [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (公有领域). 1.2.0 新增的法语, 韩语与 Piper 模型另用到 [Common Voice](https://commonvoice.mozilla.org/) 12 法语 (CC0), [KsponSpeech](https://aihub.or.kr/aihubdata/data/view.do?currMenu=115&topMenu=100&aihubDataSe=realm&dataSetSn=123) (AI Hub, 只向韩国国民开放申请), [SIWIS 法语语音合成库](https://datashare.is.ed.ac.uk/handle/10283/2353) (CC BY 4.0, SIWIS database, University of Edinburgh and partners), [Thorsten-Voice](https://github.com/thorstenMueller/Thorsten-Voice) (CC0) 与 [NabuCasa voice-datasets](https://github.com/NabuCasa/voice-datasets) 的 davefx 和 denis 录音 (CC0).
- [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT) 推理引擎, [silero VAD](https://github.com/snakers4/silero-vad) (MIT) 语音活动检测模型, [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) 与 [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT) 音素转换, [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT) 中文唤醒词读音表.

******

### 开发路线图

******

功能规划, 设计决策与完成情况以可勾选清单维护在 ROADMAP.md 中. 未勾选条目表示规划意向, 不代表当前版本能力.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.2.0

_2026/10/21_

- `提示` 新增常驻唤醒, 默认关闭: 开启后一直在本机监听唤醒词, 麦克风指示灯常亮, 有一条常驻通知, 也更耗电 (熄屏 30 分钟实测见 README). 建议配合 "监听时长" 或 "只在充电时监听" 使用
- `新增` 常驻唤醒 (设置 > 常驻唤醒): 说出唤醒词 (默认 "小鹦小鹦" 与 "Hey Macaw", 可修改, 灵敏度分高, 中, 低三档) 即打开与数字助理相同的听写弹层; 熄屏或锁屏时也会点亮屏幕, 发送或复制前先解锁
- `新增` 由唤醒词打开的弹层会朗读识别出的文字, 有该语言的 3-Macaw Speech 语音时用它, 否则用系统语音; 可在设置中关闭
- `新增` 监听时长 (10, 30, 60 分钟后自动关闭, 或一直开着) 与只在充电时监听 (拔掉电源时暂停, 接上后继续), 与 3-Stove Agent 的同名选项一致
- `新增` 3-Macaw Speech 不是默认数字助理时, 开启常驻唤醒会询问一次悬浮窗权限; 不授予时, 听到唤醒词后发出 "点此说话" 通知, 点按即打开弹层
- `新增` 脚本, 其他应用或本插件的其他入口使用麦克风时, 常驻唤醒自动让出, 对方用完后继续监听
- `新增` 与 3-Stone AI 和 3-Stove Agent 的常驻监听互斥, 系统同一时刻只能有一个; 对方开着时会提示先关闭, 并可直接打开对方的设置
- `新增` 重启或更新后不会自动录音: 开关仍开着时发一条 "点此恢复常驻唤醒" 通知, 打开首页或设置页也会恢复
- `新增` 识别音频文件 (speech.recognize, speech.detectVoice) 除 WAV 外也支持 MP3, AAC (含 M4A), Ogg (Vorbis, Opus), FLAC, AMR 等常见格式, 以及视频文件 (MP4, WebM 等) 中的声音; 文件上限仍为 200 MB 或 30 分钟 (需要 AutoJs6 6.8.0 及以上, 更早的版本只能传入 WAV)
- `新增` 4 个插件工具 (需要 AutoJs6 6.8.0 及以上): speech_listen 听一句话并返回文字, speech_speak 朗读, speech_transcribe 识别手机上的音频文件, speech_synthesize 把文字合成为 WAV 或 M4A 文件. MCP Server 插件用它们让电脑上的 AI 助手通过手机听和说, 脚本也可以经 plugins.tools 调用; 不能直接录音时, 听写先在手机上发出 "点此说话" 通知, 点按后开始
- `新增` 可下载更多模型: 法语, 俄语与韩语的流式识别, 以及法语, 西班牙语, 德语与俄语的 Piper 声音; Piper 声音与韩语模型仅限非商业用途, 下载前会提示许可, 韩语模型只从上游地址下载
- `新增` 自动标点: 在首页新的 "标点" 分组里下载标点模型后, 流式模型识别出的中文与英文会自动加上逗号, 句号与问号, 英文还会恢复大写; SenseVoice 自带标点, 不受影响; 脚本照旧可以传入 punctuation: false 去掉所有标点
- `新增` 试听试说页新增 "文件" 标签: 选一个音频或视频文件 (MP3, M4A, WAV 等, 不超过 200 MB, 时长不超过 30 分钟), 不写脚本也能识别出里面说的话, 按时间分段显示, 可以复制全文或保存为文本与 SRT 字幕; 模型, 语言与自动标点在页面上选; 离开页面或熄屏后识别照常进行, 通知里显示进度并可取消; 最近一次结果下次打开仍在

#### v1.1.0

_2026/10/21_

- `提示` 新增触发式入口: 平时不录音, 点按快速设置磁贴, 桌面小部件, 快捷方式, 悬浮麦克风或按下音量键, 蓝牙耳机语音键后才打开与数字助理相同的听写弹层
- `提示` 新增权限: 悬浮窗 (只用于悬浮麦克风, 默认关闭), 开机自启 (只在悬浮麦克风开启时于重启后恢复它), 特殊用途前台服务 (只保持悬浮麦克风显示, 不录音), 附近的设备 (Android 12 起只在用耳机语音键打开弹层时询问, 用于接管耳机的语音识别与麦克风). 按键触发需要在系统无障碍设置中开启, 只过滤按键
- `新增` 快速设置磁贴 (语音指令): 在设置的触发方式组中添加 (Android 13 起由系统确认, 更早的版本显示手动步骤), 下拉快速设置点它即可说话; 锁屏上可以直接听写, 发送或复制前先解锁 (ColorOS 与 HyperOS 会先要求解锁); 长按磁贴打开触发方式设置
- `新增` 悬浮麦克风: 默认关闭, 开启时申请悬浮窗权限; 可沿屏幕边缘拖动的圆形按钮, 点按即可说话; 按钮本身不录音, 以一条静默通知保持显示 (可在通知中关闭), 开启时重启后自动恢复; 听写弹层显示期间自动隐藏, 可与 3-Stove Agent 的悬浮球同时显示
- `新增` 桌面小部件 (按一下说话) 与长按应用图标出现的快捷方式 (语音指令) 都打开听写弹层; 设置中可以直接把小部件放到桌面, 桌面不支持时显示手动步骤
- `新增` 按键触发: 只过滤按键的无障碍服务, 不读取屏幕内容, 需要在系统无障碍设置中开启; 默认长按音量下键约 0.8 秒打开听写弹层, 可改为长按音量上键或双击音量下键; 短按照常调节音量, 熄屏, 来电或通话时不拦截按键
- `新增` 蓝牙耳机语音键: 长按耳机的语音键或触控区即打开听写弹层 (3-Macaw Speech 应答语音命令请求 VOICE_COMMAND). 允许附近的设备权限后用耳机麦克风收音, 每次按键都有效. 其他应用也能应答时, 系统会先让你选择. 可在设置的触发方式组中关闭耳机语音键
- `新增` 各入口打开的弹层与长按电源键时相同: 3-Macaw Speech 是默认数字助理时经助理会话打开, 不是时直接打开同款弹层; 系统没有显示助理会话时 (例如在锁屏上点磁贴后才解锁) 也直接打开弹层
- `新增` 打包应用通过独立入口使用语音识别与模型管理, 麦克风访问遵循各应用自身的录音授权
- `修复` 听写弹层一打开就开始录音: 模型仍在加载时 (首次打开, 或空闲 5 分钟后) 开头说的话不再丢失, 你好这样的短句也不再没有反应

#### v1.0.0

_2026/10/20_

- `提示` 首个正式版本. 脚本中的 speech 模块需要 AutoJs6 6.8.0 (5456) 或更高版本; 插件界面, 系统 TTS 引擎, 系统语音输入, 语音识别服务与数字助理不依赖 AutoJs6
- `提示` 识别与合成模型不随安装包提供, 需要在插件界面或脚本中下载; 应用整体按 GNU GPL 3.0 或更新版本传播 (语音运行时静态链接了 eSpeak NG), 对应源码随每个发行版提供
- `新增` 作为 AutoJs6 的语音插件: 在插件中心启用后, 脚本可以通过 speech 模块使用语音识别, 唤醒词, 语音活动检测, 语音合成与模型管理
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
- `新增` 界面, README, 插件中心说明与更新日志支持 10 种语言: 简体中文, 繁体中文 (香港, 台湾), 英语, 法语, 西班牙语, 日语, 韩语, 俄语与阿拉伯语
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
py -I .python\corresponding_source.py build --downloads <directory outside the repository>
```

******

### 许可

******

插件源码使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). sherpa-onnx 官方 Android 库 (Apache-2.0) 静态链接了 eSpeak NG 与 UCD Tools, 二者使用 GNU GPL 3.0 或更新版本, 因此发布的 APK 整体按 GPL-3.0-or-later 传播 (MPL-2.0 第 3.3 节允许这种组合), 不提供任何担保. 每个发行版附有对应源码 `autojs6-plugin-three-macaw-speech-corresponding-source-sherpa-onnx-v1.13.8-ac68c80b.tar`: sherpa-onnx 1.13.8 的源码与构建脚本, 以及构建时按 URL 与 SHA-256 固定的全部依赖归档; 其 SHA-256 列在发行版的 `SHA256SUMS` 中, 可以用 `.python/corresponding_source.py` 重新生成与校验. ONNX Runtime 与 silero VAD 使用 MIT 许可, 各模型的许可见上文模型表. 插件的关于页可以查看许可全文与第三方声明.

- [第三方声明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文档: speech 模块: https://docs.autojs6.com/#/speech
- sherpa-onnx 项目: https://github.com/k2-fsa/sherpa-onnx
