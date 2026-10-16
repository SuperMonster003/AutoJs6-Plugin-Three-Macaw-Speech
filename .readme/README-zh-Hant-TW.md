<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-macaw-speech-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>離線語音辨識, 喚醒詞偵測, 語音活動偵測與神經網路語音合成, 可註冊為系統 TTS 引擎</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言 (Languages)

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ar.md)

******

### 簡介

******

3-Macaw Speech 基於 [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) 在裝置上離線完成語音辨識, 語音活動偵測, 喚醒詞偵測與神經網路語音合成. 全部運算都在本機進行, 音訊不會上傳.

外掛是獨立安裝的 APK. AutoJs6 指令碼透過全域模組 `speech` 使用這些能力; 合成能力也註冊為 Android 系統 TTS 引擎, AutoJs6 的 `tts` 模組與其他應用程式都可以選用. 辨識與合成模型不隨 APK 提供, 在外掛介面或指令碼中按需下載, 只有語音活動偵測模型隨套件內建.

> 目前為開發預覽版: 外掛可以被 AutoJs6 辨識並載入語音執行環境, 但辨識, 喚醒詞, 合成, 模型下載, 外掛介面與 `speech` 指令碼模組尚未交付. 下文描述的是 1.0.0 的設計目標, 進度見開發路線圖.

******

### 功能亮點

******

- 語音辨識: 麥克風串流聽寫 (即時傳回部分結果與整句結果) 與 WAV 檔案辨識, 提供中文, 英文, 中英混合以及日語, 韓語, 粵語的離線模型.
- 語音活動偵測: 內建 silero VAD v5, 既可單獨切分音訊中的語音段, 也用於聽寫時的自動斷句.
- 喚醒詞: 直接填寫關鍵詞原文 (例如 `小六小六` 或 `hey macaw`), 無須訓練模型, 喚醒後可自動轉入聽寫.
- 語音合成: 多種中英文神經網路音色, 可合成到 WAV 檔案, 也可以作為系統 TTS 引擎供 AutoJs6 的 `tts` 模組和其他應用程式使用.
- 模型管理: 從已簽署的模型目錄下載, 逐一檔案驗證 SHA-256 後才啟用, 也可以匯入本機模型; 僅限非商業用途的模型在下載前會提示授權.

******

### 使用方法

******

1. 將 AutoJs6 升級到內部版本號 5451 (6.8.0) 或更高版本.
2. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) 下載與裝置 ABI 對應的 APK (不確定時選擇 `universal`) 並安裝, 然後在 AutoJs6 的外掛中心授權並啟用.
3. 開啟外掛介面授予麥克風權限並下載需要的模型, 然後在指令碼中使用 `speech` 模組.

> 外掛支援 Android 7.0 (API 24) 及以上的裝置. 每種 ABI 各有一個安裝套件, 另有適用於全部裝置的 `universal` 安裝套件.

******

### 模型

******

辨識, 喚醒詞與合成模型的大小在 30 MB 到 350 MB 之間, 因此不隨 APK 提供. 外掛從 AutoJs6 共享資源儲存庫的不可變發布下載模型, 鏡像無法使用時改用 sherpa-onnx 的上游位址, 兩條路徑都要逐一檔案驗證 SHA-256 後才啟用.

每個模型都標示了授權. 僅限非商業用途的模型 (例如使用 DataBaker 語料訓練的合成模型) 在下載前會提示, 指令碼下載時需要明確傳入 `acceptLicense: true`.

******

### 腳本 API

******

指令碼透過全域模組 `speech` (也可寫作 `$speech`) 使用這些能力 (1.0.0 設計草案, 以發布版本的文件為準):

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

- `speech.listen(options)` 開啟聽寫工作階段, 透過 `partial`, `final` 與 `end` 等事件傳回結果; `speech.recognize(path)` 辨識 WAV 檔案.
- `speech.wake(keywords, options)` 開啟喚醒詞工作階段; `thenListen` 讓喚醒後自動轉入聽寫.
- `speech.synthesize(text, path)` 合成到 WAV 檔案; `speech.speak(text)` 與 `tts(text, {engine})` 透過系統 TTS 引擎播報; `speech.models` 管理模型.

******

### 常見問題

******

#### 辨識與合成需要連網嗎?

不需要. 模型下載完成後, 全部運算都在裝置上進行, 音訊不會上傳. 只有下載模型與重新整理模型目錄時才會連網.

#### 為什麼由外掛自行申請麥克風權限?

錄音在外掛程序內完成, 音訊不經過 AutoJs6 傳輸. AutoJs6 繫結外掛時會傳遞前景能力, 因此指令碼在背景也能聽寫. 首次使用前請在外掛介面授予麥克風權限.

#### 應該安裝哪個安裝套件?

大多數手機選擇 `arm64-v8a`. 不確定裝置 ABI 時安裝 `universal`, 它適用於全部裝置, 但檔案更大.

******

### 權限與安全

******

外掛在設計上盡量縮小權限範圍:

- 麥克風: 只在聽寫, 喚醒詞與試聽試說時錄音, 工作階段結束或指令碼結束後立即釋放麥克風.
- 網路: 只用於下載模型與重新整理已簽署的模型目錄, 辨識與合成不連網.
- 儲存空間: 模型儲存在外掛私有目錄, 匯入本機模型透過系統檔案選擇器完成, 不申請儲存權限.
- 受保護的入口: 能力服務與啟用入口均受 AutoJs6 外掛權限 (`org.autojs.permission.PLUGIN`) 保護, 第三方應用程式無法直接呼叫.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者, 主程式透過這些識別碼發現外掛並完成能力協商:

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

### 開發路線圖

******

功能規劃, 設計決策與完成情況以可勾選清單維護在 ROADMAP.md 中. 未勾選的項目表示規劃意向, 不代表目前版本的能力.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/10/16_

- `提示` 開發預覽: 外掛可以被 AutoJs6 辨識並載入語音執行環境, 語音辨識, 喚醒詞, 語音合成, 模型下載, 外掛介面與 speech 指令碼模組尚未交付
- `提示` 需要 AutoJs6 6.8.0 (5451) 或更高版本 (確切的最低版本號待宿主端變更完成後回填)
- `新增` 外掛骨架: Wake 啟用協定, 外掛中心身分服務與語音能力服務
- `新增` 內建 sherpa-onnx 1.13.8 語音執行環境與 silero VAD v5 語音活動偵測模型, 依 4 種 ABI 分別提供安裝套件
- `新增` 10 種語言的 README, 外掛中心說明與更新日誌, 由 JSON 來源檔案統一產生
- `相依性` 附加 sherpa-onnx 版本 1.13.8
- `相依性` 附加 Jetpack Compose BOM 版本 2026.09.00
- `相依性` 附加 AndroidX Activity Compose 版本 1.12.2
- `相依性` 附加 AndroidX Core KTX 版本 1.15.0
- `相依性` 附加 Kotlin Coroutines 版本 1.11.0

##### 更多發行歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置

******

本節面向希望從原始碼建置外掛的開發者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\verify_native_alignment.py --check
py .python\check_repository.py
```

******

### 授權

******

專案程式碼使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). 語音執行環境 sherpa-onnx 使用 Apache-2.0 授權, onnxruntime 與 silero VAD 使用 MIT 授權; 每個模型的授權在模型目錄中逐條列出.

- [第三方聲明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
- sherpa-onnx 專案: https://github.com/k2-fsa/sherpa-onnx
