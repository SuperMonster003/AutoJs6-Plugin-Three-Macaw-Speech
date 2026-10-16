<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-macaw-speech-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>離線語音識別, 喚醒詞偵測, 語音活動偵測與神經網絡語音合成, 可註冊為系統 TTS 引擎</p>

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
- 繁體中文 (香港) [zh-Hant-HK] # 目前
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-TW.md)
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

3-Macaw Speech 基於 [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) 在裝置上離線完成語音識別, 語音活動偵測, 喚醒詞偵測與神經網絡語音合成. 全部運算都在本機進行, 音訊不會上傳.

插件是獨立安裝的 APK. AutoJs6 腳本透過全域模組 `speech` 使用這些能力; 合成能力亦註冊為 Android 系統 TTS 引擎, AutoJs6 的 `tts` 模組與其他應用程式都可以選用. 識別與合成模型不隨 APK 提供, 在插件介面或腳本中按需下載, 只有語音活動偵測模型隨套件內置.

> 目前為開發預覽版: 插件可以被 AutoJs6 識別, 可以下載與管理模型, 識別語音, 偵測語音活動與喚醒詞, 合成語音並作為系統 TTS 引擎使用, 插件自己的介面可以管理與試用模型並調整設定; 提供 `speech` 腳本模組的 AutoJs6 版本尚未發佈. 下文描述的是 1.0.0 的設計目標, 進度見開發路線圖.

******

### 功能亮點

******

- 語音識別: 咪高峰串流聽寫 (即時返回部分結果與整句結果) 與 WAV 檔案識別, 提供中文, 英文, 中英混合以及日語, 韓語, 粵語的離線模型.
- 語音活動偵測: 內置 silero VAD v5, 既可單獨切分音訊中的語音段, 亦用於聽寫時的自動斷句.
- 喚醒詞: 直接填寫關鍵詞原文 (例如 `小六小六` 或 `hey macaw`), 無需訓練模型, 喚醒後可自動轉入聽寫.
- 語音合成: 多種中英文神經網絡音色, 可合成到 WAV 檔案, 亦可作為系統 TTS 引擎供 AutoJs6 的 `tts` 模組和其他應用程式使用.
- 系統語音輸入: 其他應用程式 (例如 3-Stove Agent) 的麥克風按鈕可以使用離線識別, 在底部面板中說一句話, 識別出的文字直接交回該應用程式.
- 模型管理: 從已簽署的模型目錄下載, 逐個檔案校驗 SHA-256 後才啟用, 亦可匯入本機模型; 僅限非商業用途的模型在下載前會提示授權.

******

### 使用方法

******

1. 將 AutoJs6 升級到內部版本號 5451 (6.8.0) 或更高版本.
2. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) 下載與裝置 ABI 對應的 APK (不確定時選擇 `universal`) 並安裝, 然後在 AutoJs6 的插件中心授權並啟用.
3. 打開插件介面授予咪高峰權限並下載需要的模型, 然後在腳本中使用 `speech` 模組.

> 插件支援 Android 7.0 (API 24) 及以上的裝置. 每種 ABI 各有一個安裝套件, 另有適用於全部裝置的 `universal` 安裝套件.

******

### 模型

******

識別, 喚醒詞與合成模型的體積在 30 MB 到 350 MB 之間, 因此不隨 APK 提供. 插件從 AutoJs6 共享資源倉庫的不可變發佈下載模型, 鏡像不可用時回退到 sherpa-onnx 的上游地址, 兩條路徑都要逐個檔案校驗 SHA-256 後才啟用.

每個模型都標示了授權. 僅限非商業用途的模型 (例如使用 DataBaker 語料訓練的合成模型) 在下載前會提示, 腳本下載時需要明確傳入 `acceptLicense: true`.

******

### 腳本 API

******

腳本透過全域模組 `speech` (亦可寫作 `$speech`) 使用這些能力 (1.0.0 設計草案, 以發佈版本的文件為準):

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

- `speech.listen(options)` 開啟聽寫會話, 透過 `partial`, `final` 與 `end` 等事件返回結果; `speech.recognize(path)` 識別 WAV 檔案.
- `speech.wake(keywords, options)` 開啟喚醒詞會話; `thenListen` 讓喚醒後自動轉入聽寫.
- `speech.synthesize(text, path)` 合成到 WAV 檔案; `speech.speak(text)` 與 `tts(text, {engine})` 透過系統 TTS 引擎播報; `speech.models` 管理模型.

******

### 常見問題

******

#### 識別與合成需要連網嗎?

不需要. 模型下載完成後, 全部運算都在裝置上進行, 音訊不會上傳. 只有下載模型與重新整理模型目錄時才會連網.

#### 為何由插件自行申請咪高峰權限?

錄音在插件程序內完成, 音訊不經過 AutoJs6 傳輸. AutoJs6 綁定插件時會傳遞前景能力, 插件在麥克風工作階段期間亦會持有麥克風前景服務, 因此工作階段開始後 AutoJs6 退到背景或熄屏也能繼續聽寫. 首次使用前請在插件介面授予麥克風權限.

#### 應該安裝哪個安裝套件?

大多數手機選擇 `arm64-v8a`. 不確定裝置 ABI 時安裝 `universal`, 它適用於全部裝置, 但體積更大.

#### 如何把 3-Macaw Speech 用作系統文字轉語音引擎?

先安裝至少一個合成模型, 然後在系統的文字轉語音設定 (通常位於無障礙或語言設定中) 將偏好引擎設為 3-Macaw Speech. 模型的每個說話人都是一個語音, 名為 `<模型 ID>/<說話人編號>`; 雙語模型另有 `<模型 ID>/<說話人編號>/en-US` 用於英文. AutoJs6 腳本也可以不改系統設定直接選用該引擎: `tts(text, {engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech"})` 或 `speech.speak(text)`.

#### 其他應用程式能用 3-Macaw Speech 做語音輸入嗎?

可以. 安裝識別模型並授予麥克風權限後, 調用系統語音輸入的應用程式 (例如 3-Stove Agent 的麥克風按鈕) 會開啟 3-Macaw Speech 的面板. 裝置上還裝有 Google 等語音輸入應用程式時, Android 會詢問要用哪一個, 並可記住選擇. 不想讓其他應用程式看到這個入口時, 在設定的識別組中關閉作為系統語音輸入開關.

******

### 權限與安全

******

插件在設計上盡量收窄權限範圍:

- 咪高峰: 只在聽寫, 喚醒詞與試聽試說時錄音, 會話結束或腳本退出後立即釋放咪高峰.
- 網絡: 只用於下載模型與重新整理已簽署的模型目錄, 識別與合成不連網.
- 儲存: 模型儲存在插件私有目錄, 匯入本機模型透過系統檔案選擇器完成, 不申請儲存權限.
- 受保護的入口: 能力服務與啟動入口均受 AutoJs6 插件權限 (`org.autojs.permission.PLUGIN`) 保護, 第三方應用程式無法直接調用; 系統 TTS 引擎及其語音檢查, 示例文字與設定入口按 Android 的慣例對所有應用程式開放, 只提供語音合成.
- 系統語音輸入: 與系統 TTS 引擎一樣向所有應用程式開放; 只在面板顯示時錄音, 只傳回識別出的文字, 可在設定中關閉.

******

### 外掛接口

******

以下資訊面向 AutoJs6 宿主與外掛開發者, 宿主透過這些標識發現外掛並完成能力協商:

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

功能規劃, 設計決策與完成情況以可勾選清單維護在 ROADMAP.md 中. 未勾選條目表示規劃意向, 不代表目前版本能力.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/10/16_

- `提示` 開發預覽: 插件可以被 AutoJs6 識別, 可以下載與管理模型, 識別 WAV 語音檔案, 外部 PCM 與麥克風語音, 偵測語音活動與喚醒詞, 合成語音並作為系統 TTS 引擎使用; 可以在插件自己的介面中管理與試用模型; 提供 speech 腳本模組的 AutoJs6 版本尚未發佈
- `提示` 需要 AutoJs6 6.8.0 (5451) 或更高版本 (準確的最低版本號待宿主側改動落地後回填)
- `新增` 插件骨架: Wake 啟動協議, 插件中心身份服務與語音能力服務
- `新增` 內置 sherpa-onnx 1.13.8 語音運行時與 silero VAD v5 語音活動偵測模型, 按 4 種 ABI 分別提供安裝套件
- `新增` 模型管理: 按簽名的模型目錄下載離線語音模型, 優先使用 AutoJs6 共享資源的發佈, 不可用時回退到 sherpa-onnx 官方發佈; 逐個檔案校驗 SHA-256 後啟用, 支援斷點續傳, 本地匯入 (tar, tar.bz2, zip), 刪除與預設模型, 僅限非商業用途或授權未知的模型需先接受授權
- `新增` 語音識別: 識別 WAV 檔案 (8 / 16 / 24 / 32 位元整數或浮點, 任意取樣率與聲道數, 在插件內重新取樣為 16 kHz 單聲道), 結果按語音段帶起止時間, SenseVoice 模型另帶語言, 情緒與事件標籤; 串流模型可使用熱詞
- `新增` 聽寫工作階段: 由插件錄音或接收 16 kHz PCM, 依次報告就緒, 中間結果, 整句結果, 端點, 語音活動與結束; 可調整斷句靜音時長, 無語音逾時與最長時長; 串流模型逐詞更新中間結果, 離線模型按語音段更新
- `新增` 語音活動偵測: 以內置 silero VAD 列出 WAV 檔案中的語音段
- `新增` 喚醒詞: 開放詞表的關鍵詞偵測, 中文關鍵詞按常用讀音拼寫為拼音, 英文單詞按模型詞典拼寫為音素, 也可以直接寫模型 token; 命中後可在同一錄音上自動轉入一輪聽寫
- `新增` 麥克風工作階段期間插件以麥克風前景服務與通知繼續錄音, 在 AutoJs6 處於前景時開始的工作階段在 AutoJs6 退到背景或熄屏後仍可收音; 同一時間只有一個麥克風工作階段; 缺少麥克風權限時報 PERMISSION_DENIED 並指明授權頁面
- `新增` 語音合成: 用已安裝的合成模型 (VITS, Matcha 與 Kokoro, 目錄中有 5 種中英文模型) 把最多 8000 個字元的文字逐句合成為 WAV 檔案, 取樣率與模型一致; 可選擇說話人與語速 (0.5 到 2 倍), 按已完成的字元報告進度, 可隨時取消; 只有一個說話人的模型會忽略所選說話人並給出提示
- `新增` 系統 TTS 引擎: 安裝合成模型後, 可在系統的文字轉語音設定中選擇 3-Macaw Speech, AutoJs6 的 tts 模組與其他應用程式都能用它朗讀; 每個說話人對應一個語音, 雙語模型的每個說話人另有一個英文語音, 語速跟隨系統或應用程式的設定; 未安裝模型時引擎報告缺少語音資料
- `新增` 首頁: 顯示語音運行時版本, 已安裝的模型數量, 麥克風權限 (可一鍵授權), 以及 3-Macaw Speech 是否為偏好的系統 TTS 引擎
- `新增` 首頁的模型按識別, 喚醒詞, 合成與語音活動偵測分組, 顯示大小, 語言與授權, 可下載, 暫停, 繼續, 取消, 刪除與設為預設; 下載進度同時顯示在通知中; 僅限非商業用途或授權未知的模型需先接受授權, 使用流動數據等按用量收費網絡下載前會先詢問
- `新增` 從 tar, tar.bz2 或 zip 壓縮檔或資料夾匯入本機模型: 與目錄模型一致的檔案按該模型登記, 其他檔案在填寫種類, 模型類型, 名稱, 語言與 ID 後作為自訂模型匯入
- `新增` 試聽試說: 即時聽寫 (中間結果顯示為灰字), 可切換模型與語言; 喚醒詞試驗列出命中的關鍵詞與分數; 輸入文字後選擇模型, 說話人與語速試聽合成效果, 或儲存為 WAV 檔案; 離開頁面即釋放麥克風
- `新增` 系統語音輸入: 其他應用程式 (例如 3-Stove Agent) 的麥克風按鈕可以使用 3-Macaw Speech 的離線識別, 在底部面板中說一句話, 識別出的文字直接交回該應用程式; 停頓後自動結束, 也可以提前停止或取消; 沿用設定頁的端點靈敏度與語音活動偵測, 不需要時可在設定的識別組中關閉
- `新增` 設定: 語言, 夜間模式, 主題色與啓動器圖標 (預設跟隨 AutoJs6); 預設識別與合成模型, 端點靈敏度, 語音活動偵測, 說話人, 語速與各語言的預設語音, 作用於試聽試說頁與系統 TTS 引擎 (腳本仍使用文件中的預設值); 閒置模型的釋放時間, 刷新模型目錄與回收未引用的檔案; 系統文字轉語音設定中 3-Macaw Speech 的設定按鈕會打開此頁
- `新增` 關於: 版本, 插件內置的發行歷史, 更新檢查 (每 12 小時至多自動檢查一次, 可忽略某個版本), 以及授權與聲明: 應用程式整體按 GNU GPL 3.0 或更新版本傳播且不提供任何擔保, 內含第三方聲明, GPL 全文, Unicode 使用條款與對應源代碼的位置
- `新增` 啓動器圖標: 可選自適應圖示 (亮色), 自適應圖示 (暗色), 自適應圖示 (自動, 預設) 或透明背景圖示
- `新增` 10 種語言的 README, 插件中心說明與更新日誌, 由 JSON 來源檔案統一生成
- `依賴` 附加 sherpa-onnx 版本 1.13.8
- `依賴` 附加 Jetpack Compose BOM 版本 2026.09.00
- `依賴` 附加 AndroidX Activity Compose 版本 1.12.2
- `依賴` 附加 AndroidX Core KTX 版本 1.15.0
- `依賴` 附加 Kotlin Coroutines 版本 1.11.0
- `依賴` 附加 Material Color Utilities (MaterialKolor) 版本 4.1.1

##### 更多發行歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

******

### 構建

******

本節面向希望從源碼構建外掛的開發者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\verify_native_alignment.py --check
py .python\check_repository.py
```

******

### 許可

******

專案程式碼使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). 語音運行時 sherpa-onnx 使用 Apache-2.0 授權, onnxruntime 與 silero VAD 使用 MIT 授權; 每個模型的授權在模型目錄中逐條列出.

- [第三方聲明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相關連結

******

- AutoJs6 項目: https://github.com/SuperMonster003/AutoJs6
- sherpa-onnx 專案: https://github.com/k2-fsa/sherpa-onnx
