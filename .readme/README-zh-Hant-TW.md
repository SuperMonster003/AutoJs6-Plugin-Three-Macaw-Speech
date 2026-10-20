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

外掛是獨立安裝的 APK, 有自己的介面, 用來下載與管理模型, 試聽試說並調整設定. AutoJs6 指令碼透過全域模組 `speech` 使用這些能力; 合成能力也註冊為 Android 系統 TTS 引擎, 辨識能力也以系統語音輸入, 系統語音辨識服務與數位助理的形式提供, 因此其他應用程式也能使用. 辨識與合成模型不隨 APK 提供, 按需下載, 只有語音活動偵測模型隨套件內建.

> 在指令碼中使用 `speech` 模組需要 AutoJs6 6.8.0 (內部版本號 5456) 或更高版本. 系統 TTS 引擎, 系統語音輸入, 語音辨識服務, 數位助理與外掛自己的介面不依賴 AutoJs6, 安裝外掛並下載模型後即可使用.

******

### 功能亮點

******

- 語音辨識: 麥克風串流聽寫 (即時傳回部分結果與整句結果) 與 WAV 檔案辨識, 提供中文, 英文, 中英混合以及日語, 韓語, 粵語的離線模型.
- 語音活動偵測: 內建 silero VAD v5, 既可單獨切分音訊中的語音段, 也用於聽寫時的自動斷句.
- 喚醒詞: 直接填寫關鍵詞原文 (例如 `小六小六` 或 `hey macaw`), 無須訓練模型, 喚醒後可自動轉入聽寫.
- 語音合成: 多種中英文神經網路音色, 可合成到 WAV 檔案, 也可以作為系統 TTS 引擎供 AutoJs6 的 `tts` 模組和其他應用程式使用.
- 系統語音輸入: 其他應用程式 (例如 3-Stove Agent) 的麥克風按鈕可以使用離線辨識, 在底部面板中說一句話, 辨識出的文字直接交回該應用程式.
- 數位助理: 把 3-Macaw Speech 選為預設數位助理應用程式後, 長按電源鍵或 Home 鍵說一句話, 需要時編輯辨識出的文字, 再交給 3-Stove Agent, 3-Stone AI 等接收語音指令的應用程式, 或複製. 它也提供系統語音辨識服務: Android 11 及以下版本選它為助理時一般會一併設為預設辨識服務; Android 12 起需在系統的語音輸入設定中另行選擇 (部分手機沒有這項設定), 應用程式也可以直接指定它.
- 觸發式入口: 不必長按電源鍵也能開啟同一個聽寫面板: 快速設定圖塊 (螢幕鎖定時也能用), 主畫面小工具與應用程式圖示捷徑, 可拖曳的懸浮麥克風 (預設關閉), 以及長按或連按兩下音量鍵的按鍵觸發 (只過濾按鍵的無障礙服務, 預設關閉). 平時不錄音, 只在面板聽寫的幾秒內使用麥克風.
- 模型管理: 從已簽署的模型目錄下載, 逐一檔案驗證 SHA-256 後才啟用, 也可以匯入本機模型; 僅限非商業用途的模型在下載前會提示授權.
- 獨立介面: 首頁顯示執行狀態並管理模型, 試聽試說頁直接試驗聽寫, 喚醒詞與合成, 設定頁調整語言, 夜間模式, 主題色, 啟動器圖示與辨識合成的預設值, 外觀預設跟隨 AutoJs6.

******

### 介面截圖

******

以下截圖來自 Sony XQ-DQ72 (Android 13). 聽寫內容是 sherpa-onnx 測試錄音經手機喇叭播放後辨識的結果, 不含個人資料.

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/home.png?raw=true" alt="首頁: 執行狀態與按用途分組的模型" width="360" />
      <br />
      <sub>首頁: 執行狀態與按用途分組的模型</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/tryout-dictation.png?raw=true" alt="試聽試說: 即時聽寫" width="360" />
      <br />
      <sub>試聽試說: 即時聽寫</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/voice-input.png?raw=true" alt="系統語音輸入: 其他應用程式開啟的底部面板" width="360" />
      <br />
      <sub>系統語音輸入: 其他應用程式開啟的底部面板</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/assistant.png?raw=true" alt="數位助理: 編輯辨識結果並選擇交給哪個應用程式" width="360" />
      <br />
      <sub>數位助理: 編輯辨識結果並選擇交給哪個應用程式</sub>
    </td>
  </tr>
</table>

******

### 安裝與使用

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) 下載與裝置 ABI 對應的 APK 並安裝: 大多數手機選擇 `arm64-v8a`, 不確定時選擇適用於全部裝置但檔案更大的 `universal`.
2. 開啟 3-Macaw Speech, 在首頁授予麥克風權限, 並在辨識, 喚醒詞與合成各群組中下載需要的模型 (目錄推薦的模型是較好的起點, 見下文模型表). Android 13 以上首次下載時也會要求顯示下載進度通知.
3. 在 AutoJs6 中使用: 將 AutoJs6 升級到 6.8.0 (內部版本號 5456) 或更高版本, 在外掛中心啟用 3-Macaw Speech, 然後在指令碼中呼叫 `speech` 模組 (見下文指令碼 API). AutoJs6 的示例程式碼 > 3-Macaw Speech 中有四個可直接執行的範例.
4. 在其他應用程式中使用: 系統 TTS 引擎見下文系統 TTS 設定; 系統語音輸入, 語音辨識服務與數位助理見常見問題.

> 每種 ABI 各有一個安裝套件, 另有適用於全部裝置的 `universal` 安裝套件. 安裝套件不含辨識與合成模型, 模型在外掛介面或指令碼中按需下載, 也可以從本機檔案匯入.

******

### 模型

******

辨識, 喚醒詞與合成模型的大小在 9 MB 到 410 MB 之間, 因此不隨 APK 提供. 外掛內建一份已簽署的模型目錄, 從 AutoJs6 共享資源儲存庫的不可變發布下載模型, 鏡像無法使用時改用 sherpa-onnx 的上游位址; 兩條路徑都要逐一檔案校驗 SHA-256 後才啟用模型. 1.0.0 的目錄包含以下模型 (大小為安裝後佔用的空間):

| 模型 ID | 用途 | 語言 | 大小 | 授權 | 商業用途 |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (推薦) | 辨識 (離線, 含標點) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | 可以 |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (推薦) | 辨識 (串流) | zh, en | 199.1 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.en.2023-06-26.int8` (推薦) | 辨識 (串流) | en | 70.3 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | 辨識 (串流) | zh | 167.4 MB | 未宣告 | 僅限非商業用途 |
| `kws.zipformer.zh-en.2025-12-20` (推薦) | 喚醒詞 | zh, en | 8.8 MB | Apache-2.0 | 可以 |
| `tts.vits.melo.zh-en` (推薦) | 合成 (說話者: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | 可以 |
| `tts.vits.icefall.zh-aishell3` (推薦) | 合成 (說話者: 174, 8 kHz) | zh | 32.8 MB | 未宣告 | 未知 |
| `tts.kokoro.multi-lang.v1_1` | 合成 (說話者: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | 可以 |
| `tts.matcha.icefall.zh-baker` | 合成 (說話者: 1, 22.05 kHz) | zh | 81.0 MB | 未宣告, MIT (聲碼器) | 僅限非商業用途 |
| `tts.vits.piper.en-us-lessac-medium` | 合成 (說話者: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 僅限非商業用途 |

僅限非商業用途或授權未知的模型在下載前會提示授權, 指令碼下載時需要明確傳入 `acceptLicense: true`. 中文串流 2025 模型的權重未宣告授權, 訓練資料含僅限非商業用途的語料, 因此只從 sherpa-onnx 的上游位址下載, 不經共享資源儲存庫鏡像. Kokoro 與 Piper 兩個合成模型附帶 eSpeak NG 的發音資料 (espeak-ng-data, GPL-3.0-or-later).

首頁的匯入本機模型可以從 tar, tar.bz2 或 zip 封存檔或資料夾匯入模型: 與目錄模型一致的檔案按該模型登記, 其他檔案在填寫種類, 模型類型, 名稱與語言後作為自訂模型匯入. 匯入透過系統檔案選擇器完成, 不需要儲存空間權限.

******

### 系統 TTS 設定

******

1. 在外掛首頁的合成群組中下載至少一個合成模型. 想先試試時, 檔案最小的 `tts.vits.icefall.zh-aishell3` (中文, 174 位說話者) 與中英混讀的 `tts.vits.melo.zh-en` 都是推薦模型.
2. 開啟系統的文字轉語音設定, 把偏好的引擎設為 3-Macaw Speech. 外掛首頁狀態卡中的系統文字轉語音引擎一列可以直接開啟該頁面; 手動尋找時, 它通常位於 設定 > 無障礙設定 > 文字轉語音輸出, 或 設定 > 系統 > 語言與輸入設定 > 文字轉語音輸出 (各廠商的名稱略有不同).
3. 輕觸引擎旁的設定按鈕會開啟 3-Macaw Speech 的設定頁, 在合成群組中為各語言選擇預設語音並選擇說話者. 語速由系統設定或要求朗讀的應用程式決定, 不支援音調.
4. 回到系統設定輕觸試聽, 即可用該語言的預設語音朗讀範例文字.

每個模型的每位說話者都是一個語音, 名為 `<模型 ID>/<說話者編號>`, 雙語模型的說話者另有 `<模型 ID>/<說話者編號>/en-US` 用於英文. AutoJs6 指令碼不修改系統設定也能使用該引擎: `tts(text, { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" })`, 或用 `speech.speak(text, { model, speaker })` 直接指定模型與說話者.

******

### 腳本 API

******

AutoJs6 6.8.0 (內部版本號 5456) 起, 指令碼透過全域模組 `speech` (也可寫作 `$speech`) 使用這些能力. 下面三段範例可以分別執行: 聽寫 30 秒並把每句話附加到檔案, 用喚醒詞喚醒後聽一句話, 合成到檔案並朗讀:

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

- `speech.listen(options)` 開啟聽寫工作階段, 透過 `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` 與 `end` 事件傳回結果, `stop()` 提前結束; `speech.recognize(path)` 辨識 WAV 檔案 (也接受 16 kHz PCM), `speech.detectVoice(path)` 列出其中的語音段.
- `speech.wake(keywords, options)` 開啟喚醒詞工作階段, 關鍵字直接寫原文 (最多 50 個); `thenListen` 讓每次喚醒後在同一段錄音上轉入一輪聽寫, 該輪的工作階段由 `wake` 事件的 `listen` 屬性提供.
- `speech.synthesize(text, path, options)` 合成到 WAV 檔案 (最多 8000 個字元, 可選模型, 說話者與語速); `speech.speak(text, options)` 與 `tts(text, { engine })` 經系統 TTS 引擎朗讀.
- `speech.models` 列出, 下載 (`install`, 可回報進度), 匯入, 刪除模型與設定預設模型; `speech.capabilities()` 傳回外掛版本, 支援的功能與麥克風權限狀態.
- 整個外掛同一時間只有一個麥克風工作階段 (聽寫或喚醒); 缺少麥克風權限時呼叫以 `PERMISSION_DENIED` 失敗, 開啟外掛首頁授予即可. 辨識出的文字不會寫入 AutoJs6 的記錄.
- 完整的方法, 選項, 事件與錯誤碼見 [AutoJs6 文件](https://docs.autojs6.com/#/speech); AutoJs6 的示例程式碼 > 3-Macaw Speech 中有聽寫到檔案, 喚醒詞與聽寫, TTS 播報與語音指令交給 Agent 四個範例.

******

### 相容性

******

- Android 7.0 (API 24) 以上, 目標 API 37. arm64-v8a, armeabi-v7a, x86_64 與 x86 各有安裝套件, 另有 universal 安裝套件; 原生程式庫按 16 KB 記憶體分頁對齊, 已在 16 KB 分頁模擬器上驗證.
- AutoJs6: 指令碼中的 `speech` 模組需要 6.8.0 (內部版本號 5456) 或更高版本; 外掛介面, 系統 TTS 引擎, 系統語音輸入, 語音辨識服務與數位助理不依賴 AutoJs6.
- 背景收音: 在 AutoJs6 位於前景時開始的聽寫或喚醒工作階段, AutoJs6 退到背景或螢幕關閉後仍會繼續收音 (外掛以麥克風前景服務與通知保持錄音). AutoJs6 已在背景時才開啟的工作階段可能被系統靜音, 此時工作階段收到錯誤碼為 `PERMISSION_DENIED` 的 `warning` 事件但不會結束, AutoJs6 回到前景後恢復; 需要背景收音的指令碼請在 AutoJs6 可見時開啟工作階段.
- 實測裝置: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 24, 35 與 37 (16 KB 分頁) 模擬器.
- 效能: 在低階的 Sony G8441 上, 串流模型載入約 1.7 秒, SenseVoice 約 2.8 秒, 聽寫處理速度約為即時的 3 倍; MeloTTS 與 Kokoro 在這類裝置上合成慢於即時, AISHELL-3 最快. Sony XQ-DQ72 等較新裝置載入約 0.5 到 1.2 秒, 合成快於即時.
- 記憶體: 一個辨識模型載入後約佔 210 到 300 MB 原生記憶體. 外掛按裝置記憶體的四分之一 (384 MB 到 1.5 GB) 管理已載入的模型, 閒置 5 分鐘後釋放 (可在設定中調整為 1 到 30 分鐘).
- 數位助理: 需要系統允許選擇第三方數位助理應用程式. 喚起方式依系統而定: 長按 Home 鍵 (三按鈕導覽), 從螢幕底部角落斜向上滑, 或在系統設定中把長按電源鍵設為喚起數位助理. 實測的 ColorOS 與 HyperOS 平板 (Android 15) 把電源鍵與手勢留給自家助理, 可以改用觸發式入口.
- 觸發式入口: 實測 Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), Xiaomi Pad 23046RP50C (HyperOS, Android 15), 以及 API 33 與 35 模擬器. ColorOS 與 HyperOS 在螢幕鎖定時點圖塊會先要求解鎖, 解鎖後開啟面板; 部分主畫面接受加入小工具的請求卻不顯示確認, 此時依設定中的步驟手動加入. 螢幕關閉時手機不把按鍵交給無障礙服務, 按鍵觸發只在螢幕開啟時可用.

******

### 常見問題

******

#### 辨識與合成需要連網嗎?

不需要. 模型下載完成後, 全部運算都在裝置上進行, 音訊不會上傳. 只有下載模型與重新整理模型目錄時才會連網.

#### 為什麼由外掛自行申請麥克風權限?

錄音在外掛程序內完成, 音訊不經過 AutoJs6 傳輸. AutoJs6 繫結外掛時會傳遞前景能力, 外掛在麥克風工作階段期間也會持有麥克風前景服務, 因此工作階段開始後 AutoJs6 退到背景或螢幕關閉也能繼續聽寫. 首次使用前請在外掛介面授予麥克風權限.

#### 應該安裝哪個安裝套件?

大多數手機選擇 `arm64-v8a`. 不確定裝置 ABI 時安裝 `universal`, 它適用於全部裝置, 但檔案更大.

#### 為什麼系統設定裡試聽沒有聲音, 或引擎顯示缺少語音資料?

引擎只回報已安裝的合成模型所涵蓋的語言. 沒有安裝合成模型, 或已安裝的模型不涵蓋裝置語言時, 系統會顯示缺少語音資料, 試聽與語速設定也可能無法使用. 在外掛首頁下載一個涵蓋裝置語言的合成模型後重新開啟系統設定即可; 試聽使用該語言的預設語音, 可以在外掛設定的合成群組中變更.

#### 其他應用程式能用 3-Macaw Speech 做語音輸入嗎?

可以. 安裝辨識模型並授予麥克風權限後, 呼叫系統語音輸入的應用程式 (例如 3-Stove Agent 的麥克風按鈕) 會開啟 3-Macaw Speech 的面板. 裝置上還裝有 Google 等語音輸入應用程式時, Android 會詢問要用哪一個, 並可記住選擇. 不想讓其他應用程式看到這個入口時, 在設定的辨識群組中關閉作為系統語音輸入開關.

#### 如何把 3-Macaw Speech 設為預設數位助理?

開啟 3-Macaw Speech 的設定, 輕觸辨識群組中的數位助理, 系統會開啟相應的設定頁, 在其中把 3-Macaw Speech 選為數位助理應用程式 (部分系統的頁面名稱略有不同). 之後長按電源鍵或 Home 鍵, 或使用助理手勢, 即可聽寫. 如果手機的電源鍵已設為其他功能, 可改用 Home 鍵或助理手勢.

#### 為什麼不用喚醒詞常駐監聽?

持續監聽喚醒詞需要一直錄音: Android 12 起只要有應用程式在錄音, 狀態列就會一直顯示麥克風指示燈 (綠點), 應用程式無法隱藏, 常駐服務與持續推論也一直耗電. 系統裡低耗電, 不亮指示燈的熱詞通道只開放給預載的系統助理, 第三方應用程式即使被選為預設數位助理也用不了. 所以 3-Macaw Speech 先提供觸發後才開啟麥克風的入口: 數位助理手勢, 快速設定圖塊, 主畫面小工具與捷徑, 懸浮麥克風與按鍵觸發, 平時不錄音, 只在聽寫的幾秒內亮綠點. 常駐喚醒預計作為預設關閉的選用項目在後續版本提供. 3-Stone AI, 3-Stove Agent 等應用程式可以提供自己的常駐監聽開關 (預設關閉); 它們監聽期間, 數位助理, 語音輸入與語音辨識照常可用, 喚醒詞偵測會暫停並在結束後自動恢復.

#### 不長按電源鍵, 還有哪些方式開啟聽寫面板?

開啟 3-Macaw Speech 的設定, 在觸發方式組中設定: 快速設定圖塊, 點按加入 (Android 13 起由系統確認, 更早的版本依提示手動加入), 之後下拉快速設定點語音指令; 主畫面小工具與捷徑, 把按一下說話小工具放到主畫面, 或長按應用程式圖示選擇語音指令; 懸浮麥克風, 開啟開關並授予在其他應用程式上層顯示的權限, 螢幕邊緣會出現可拖曳的麥克風按鈕; 按鍵觸發, 在系統無障礙設定中開啟 3-Macaw Speech 按鍵觸發, 預設長按音量下鍵約 0.8 秒, 可改為長按音量上鍵或連按兩下音量下鍵. 這些入口都開啟與數位助理相同的面板, 說完後選擇接收文字的應用程式; 3-Macaw Speech 是預設數位助理時經助理工作階段開啟, 不是時直接開啟同款面板.

#### 按鍵觸發為什麼需要無障礙服務, 安全嗎?

Android 只允許無障礙服務在系統處理之前取得實體按鍵, 所以按鍵觸發是一個無障礙服務. 它只申請過濾按鍵, 不訂閱任何介面事件, 不能讀取螢幕內容, 也不能執行手勢; 只攔截所選的音量鍵, 短按照常調整音量, 螢幕關閉, 來電或通話時不攔截. 開關是灰色時 (Android 13 起透過安裝檔安裝的應用程式), 先開啟 3-Macaw Speech 的應用程式資訊頁, 在選單中選擇允許受限制的設定.

#### 藍牙耳機的語音鍵能開啟聽寫面板嗎?

目前不能. 實測 Sony XQ-DQ72 (Android 13) 與 Sony WF-1000XM4: 3-Macaw Speech 是預設數位助理時長按耳機觸控區, 藍牙服務發出的是語音指令請求 (VOICE_COMMAND), 系統把它交給了 Google 應用程式, Google 應用程式隨後開啟了助理設定頁; 3-Macaw Speech 不回應這個請求. 其他耳機與機型尚未實測. 使用耳機時可以改用手機上的圖塊, 小工具, 懸浮麥克風或按鍵觸發.

#### 下載模型失敗怎麼辦?

外掛優先從 AutoJs6 共享資源儲存庫的 GitHub 發布下載, 失敗時自動改用 sherpa-onnx 的上游位址, 中斷的下載可以繼續. 無法存取 GitHub 時, 可以在其他裝置下載模型封存檔 (模型目錄中的鏡像或上游位址), 再透過首頁的匯入本機模型匯入; 與目錄一致的檔案會按目錄模型登記.

#### 模型佔用多少空間, 如何釋放?

各模型安裝後的大小見上文模型表. 首頁可以刪除不再需要的模型; 設定中的回收未參照的檔案會刪除不再需要的下載檔案, 匯入副本與中斷的安裝, 已安裝的模型與暫停的下載會保留. 解除安裝外掛會刪除全部模型.

#### 辨識結果為什麼沒有標點?

串流 Zipformer 模型只輸出文字, 不加標點. SenseVoice 模型按語音段辨識, 會加標點並回報語言, 適合需要完整句子的場合: 在指令碼中以 `model` 選項指定它, 或在設定中把它設為預設辨識模型.

#### 喚醒詞怎麼寫, 為什麼沒有被喚醒?

直接寫關鍵字原文: 中文按常用讀音拼寫為拼音, 英文單字按模型詞典拼寫 (不在詞典中的單字會被拒絕), 破音字可以直接寫模型的拼音 token 來指定讀音. 三到四個音節, 不常見的詞組更不容易誤喚醒. 每個關鍵字可以設定 `threshold` (越低越容易喚醒) 與 `boost`. 先在試聽試說頁的喚醒詞試驗中試試, 再寫進指令碼.

#### 為什麼 APK 按 GPL-3.0 散布, 原始碼在哪裡?

外掛自身的程式碼使用 MPL-2.0. sherpa-onnx 官方 Android 程式庫靜態連結了 eSpeak NG (GPL-3.0-or-later), 所以 APK 整體按 GPL-3.0-or-later 散布. 每個發行版都附有對應原始碼封存檔 (sherpa-onnx 的原始碼與建置指令碼及其全部相依項目), 檔名含 `corresponding-source`, SHA-256 列在 `SHA256SUMS` 中; 外掛原始碼就是本儲存庫在發行標籤處的內容.

******

### 權限與安全

******

外掛在設計上盡量縮小權限範圍:

- 麥克風: 只在聽寫, 喚醒詞與試聽試說時錄音, 工作階段結束或指令碼結束後立即釋放麥克風.
- 網路: 只用於下載模型與重新整理已簽署的模型目錄, 辨識與合成不連網.
- 儲存空間: 模型儲存在外掛私有目錄, 匯入本機模型透過系統檔案選擇器完成, 不申請儲存權限.
- 受保護的入口: 能力服務與啟用入口均受 AutoJs6 外掛權限 (`org.autojs.permission.PLUGIN`) 保護, 第三方應用程式無法直接呼叫; 系統 TTS 引擎及其語音檢查, 範例文字與設定入口依 Android 的慣例對所有應用程式開放, 只提供語音合成.
- 系統語音輸入: 與系統 TTS 引擎一樣向所有應用程式開放; 只在面板顯示時錄音, 只傳回辨識出的文字, 可在設定中關閉.
- 數位助理: 不讀取螢幕內容, 平時不在背景收音; 只在面板顯示時錄音, 辨識出的文字只在你點按應用程式按鈕或複製時離開面板 (鎖定畫面時需先解鎖). 語音辨識服務只為本身擁有麥克風權限的應用程式辨識.
- 觸發式入口: 圖塊, 小工具, 捷徑, 懸浮麥克風與按鍵觸發本身都不錄音, 只負責開啟聽寫面板. 按鍵觸發是只過濾按鍵的無障礙服務: 不讀取螢幕內容, 不執行手勢, 只攔截所選的音量鍵 (短按照常調整音量, 螢幕關閉, 來電或通話時不攔截). 懸浮麥克風需要在其他應用程式上層顯示的權限, 以一則靜音通知保持顯示; 開機自動啟動只用於在重新啟動後恢復已開啟的懸浮麥克風.

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
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### 致謝

******

3-Macaw Speech 建立在以下開源專案, 模型與資料之上, 感謝它們的作者與維護者:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) 與 [k2-fsa (新一代 Kaldi)](https://github.com/k2-fsa) 團隊: 辨識, 喚醒詞, 語音活動偵測與合成的執行階段, 各模型的 ONNX 匯出與 Android 程式庫 (Apache-2.0).
- [icefall](https://github.com/k2-fsa/icefall) 訓練的模型: Zipformer 串流辨識模型 (中英雙語 pfluo, 英文 Zengwei, 中文 2025 yuekai), 開放詞彙喚醒詞模型 (pkufool), 以及 k2-fsa 的 AISHELL-3 VITS 與標貝 Matcha-TTS 合成模型.
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / 阿里巴巴, FunASR 模型授權 1.1): 多語言離線辨識模型.
- [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, 中文錄音由 LongMaoData 授權) 與 [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT) 合成模型, [HiFi-GAN](https://github.com/jik876/hifi-gan) 聲碼器 (MIT).
- 訓練資料: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), [標貝中文標準女聲音庫](https://www.data-baker.com/open_source.html) (僅限非商業用途), Lessac Technologies 的 Blizzard 2013 資料 (僅限研究) 與 [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (公眾領域).
- [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT) 推論引擎, [silero VAD](https://github.com/snakers4/silero-vad) (MIT) 語音活動偵測模型, [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) 與 [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT) 音素轉換, [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT) 中文喚醒詞讀音表.

******

### 開發路線圖

******

功能規劃, 設計決策與完成情況以可勾選清單維護在 ROADMAP.md 中. 未勾選的項目表示規劃意向, 不代表目前版本的能力.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.1.0

_2026/10/20_

- `提示` 新增觸發式入口: 平時不錄音, 點按快速設定圖塊, 主畫面小工具, 捷徑, 懸浮麥克風或按下音量鍵後才開啟與數位助理相同的聽寫面板
- `提示` 新增權限: 在其他應用程式上層顯示 (只用於懸浮麥克風, 預設關閉), 開機自動啟動 (只在懸浮麥克風開啟時於重新啟動後恢復它), 特殊用途前景服務 (只保持懸浮麥克風顯示, 不錄音). 按鍵觸發需要在系統無障礙設定中開啟, 只過濾按鍵
- `新增` 快速設定圖塊 (語音指令): 在設定的觸發方式組中加入 (Android 13 起由系統確認, 更早的版本顯示手動步驟), 下拉快速設定點它即可說話; 螢幕鎖定時可以直接聽寫, 傳送或複製前先解鎖 (ColorOS 與 HyperOS 會先要求解鎖); 長按圖塊開啟觸發方式設定
- `新增` 懸浮麥克風: 預設關閉, 開啟時申請在其他應用程式上層顯示的權限; 可沿螢幕邊緣拖曳的圓形按鈕, 點按即可說話; 按鈕本身不錄音, 以一則靜音通知保持顯示 (可在通知中關閉), 開啟時重新啟動後自動恢復; 聽寫面板顯示期間自動隱藏, 可與 3-Stove Agent 的懸浮球同時顯示
- `新增` 主畫面小工具 (按一下說話) 與長按應用程式圖示出現的捷徑 (語音指令) 都開啟聽寫面板; 設定中可以直接把小工具放到主畫面, 主畫面不支援時顯示手動步驟
- `新增` 按鍵觸發: 只過濾按鍵的無障礙服務, 不讀取螢幕內容, 需要在系統無障礙設定中開啟; 預設長按音量下鍵約 0.8 秒開啟聽寫面板, 可改為長按音量上鍵或連按兩下音量下鍵; 短按照常調整音量, 螢幕關閉, 來電或通話時不攔截按鍵
- `新增` 各入口開啟的面板與長按電源鍵時相同: 3-Macaw Speech 是預設數位助理時經助理工作階段開啟, 不是時直接開啟同款面板; 系統沒有顯示助理工作階段時 (例如在螢幕鎖定時點圖塊後才解鎖) 也直接開啟面板
- `新增` 打包應用透過獨立入口使用語音辨識與模型管理, 麥克風存取遵循各應用本身的錄音授權

#### v1.0.0

_2026/10/20_

- `提示` 首個正式版本. 指令碼中的 speech 模組需要 AutoJs6 6.8.0 (5456) 或更高版本; 外掛介面, 系統 TTS 引擎, 系統語音輸入, 語音辨識服務與數位助理不依賴 AutoJs6
- `提示` 辨識與合成模型不隨安裝套件提供, 需要在外掛介面或指令碼中下載; 應用程式整體依 GNU GPL 3.0 或更新版本傳播 (語音執行階段靜態連結了 eSpeak NG), 對應原始碼隨每個發行版提供
- `新增` 作為 AutoJs6 的語音外掛: 在外掛中心啟用後, 指令碼可以透過 speech 模組使用語音辨識, 喚醒詞, 語音活動偵測, 語音合成與模型管理
- `新增` 內建 sherpa-onnx 1.13.8 語音執行環境與 silero VAD v5 語音活動偵測模型, 依 4 種 ABI 分別提供安裝套件
- `新增` 模型管理: 依簽章的模型目錄下載離線語音模型, 優先使用 AutoJs6 共用資源的發布, 無法使用時改用 sherpa-onnx 官方發布; 逐一檔案驗證 SHA-256 後啟用, 支援續傳, 本機匯入 (tar, tar.bz2, zip), 刪除與預設模型, 僅限非商業用途或授權未知的模型需先接受授權
- `新增` 語音辨識: 辨識 WAV 檔案 (8 / 16 / 24 / 32 位元整數或浮點, 任意取樣率與聲道數, 在外掛內重新取樣為 16 kHz 單聲道), 結果按語音段附起訖時間, SenseVoice 模型另附語言, 情緒與事件標籤; 串流模型可使用熱詞
- `新增` 聽寫工作階段: 由外掛錄音或接收 16 kHz PCM, 依序回報就緒, 中間結果, 整句結果, 端點, 語音活動與結束; 可調整斷句靜音時長, 無語音逾時與最長時長; 串流模型逐詞更新中間結果, 離線模型按語音段更新
- `新增` 語音活動偵測: 以內建 silero VAD 列出 WAV 檔案中的語音段
- `新增` 喚醒詞: 開放詞彙的關鍵詞偵測, 中文關鍵詞依常用讀音拼寫為拼音, 英文單字依模型詞典拼寫為音素, 也可以直接寫模型 token; 命中後可在同一錄音上自動轉入一輪聽寫
- `新增` 麥克風工作階段期間外掛以麥克風前景服務與通知持續錄音, 在 AutoJs6 位於前景時開始的工作階段在 AutoJs6 退到背景或螢幕關閉後仍可收音; 同一時間只有一個麥克風工作階段; 缺少麥克風權限時回報 PERMISSION_DENIED 並指出授權頁面
- `新增` 語音合成: 用已安裝的合成模型 (VITS, Matcha 與 Kokoro, 目錄中有 5 種中英文模型) 把最多 8000 個字元的文字逐句合成為 WAV 檔案, 取樣率與模型一致; 可選擇說話者與語速 (0.5 到 2 倍), 依已完成的字元回報進度, 可隨時取消; 只有一位說話者的模型會忽略所選說話者並給出提示
- `新增` 系統 TTS 引擎: 安裝合成模型後, 可在系統的文字轉語音設定中選擇 3-Macaw Speech, AutoJs6 的 tts 模組與其他應用程式都能用它朗讀; 每位說話者對應一個語音, 雙語模型的每位說話者另有一個英文語音, 語速依系統或應用程式的設定; 未安裝模型時引擎回報缺少語音資料
- `新增` 首頁: 顯示語音執行環境版本, 已安裝的模型數量, 麥克風權限 (可一鍵授權), 以及 3-Macaw Speech 是否為偏好的系統 TTS 引擎
- `新增` 首頁的模型依辨識, 喚醒詞, 合成與語音活動偵測分組, 顯示大小, 語言與授權, 可下載, 暫停, 繼續, 取消, 刪除與設為預設; 下載進度同時顯示在通知中; 僅限非商業用途或授權未知的模型需先接受授權, 使用行動數據等計量付費網路下載前會先詢問
- `新增` 從 tar, tar.bz2 或 zip 壓縮檔或資料夾匯入本機模型: 與目錄模型一致的檔案依該模型登記, 其他檔案在填寫種類, 模型類型, 名稱, 語言與 ID 後作為自訂模型匯入
- `新增` 試聽試說: 即時聽寫 (中間結果以灰字顯示), 可切換模型與語言; 喚醒詞試驗列出命中的關鍵詞與分數; 輸入文字後選擇模型, 說話者與語速試聽合成效果, 或儲存為 WAV 檔案; 離開頁面即釋放麥克風
- `新增` 系統語音輸入: 其他應用程式 (例如 3-Stove Agent) 的麥克風按鈕可以使用 3-Macaw Speech 的離線辨識, 在底部面板中說一句話, 辨識出的文字直接交回該應用程式; 停頓後自動結束, 也可以提前停止或取消; 沿用設定頁面的端點靈敏度與語音活動偵測, 不需要時可在設定的辨識群組中關閉
- `新增` 數位助理: 在系統設定中把 3-Macaw Speech 選為預設數位助理應用程式後, 長按電源鍵或 Home 鍵 (或使用助理手勢) 即可在底部面板中說一句話; 辨識出的文字可以編輯, 再點按鈕交給接收語音指令的應用程式 (例如 3-Stove Agent 或 3-Stone AI), 也可以複製或重說; 點按鈕之前不會傳送任何內容. 鎖定畫面時也能使用, 傳送或複製前會先解鎖. 助理不讀取螢幕內容, 平時不在背景收音. 設定的辨識群組會顯示它是否為預設助理, 並可開啟系統設定進行選擇
- `新增` 系統語音辨識服務: 使用 Android 語音辨識介面 (SpeechRecognizer) 的應用程式可以直接指定 3-Macaw Speech, 使用它的離線辨識並取得中間結果; 把它設為預設辨識服務後, 沒有指定的應用程式也會使用它. Android 11 及以下版本選它為數位助理時一般會一併設為預設; Android 12 起需在系統的語音輸入設定中另行選擇 (部分手機沒有這項設定). 其他應用程式 (例如 3-Stone AI) 藉由 3-Macaw Speech 持續監聽喚醒詞時, 數位助理, 系統語音輸入, 語音辨識服務與試聽試說頁仍可使用麥克風: 期間喚醒詞偵測暫停, 結束後自動恢復
- `新增` 設定: 語言, 夜間模式, 主題色與啟動器圖示 (預設跟隨 AutoJs6); 預設辨識與合成模型, 端點靈敏度, 語音活動偵測, 說話者, 語速與各語言的預設語音, 作用於試聽試說頁與系統 TTS 引擎 (指令碼仍使用文件中的預設值); 閒置模型的釋放時間, 重新整理模型目錄與回收未參照的檔案; 系統文字轉語音設定中 3-Macaw Speech 的設定按鈕會開啟此頁
- `新增` 關於: 版本, 外掛內建的發行歷史, 更新檢查 (每 12 小時至多自動檢查一次, 可忽略某個版本), 以及授權與聲明: 應用程式整體依 GNU GPL 3.0 或更新版本傳播且不提供任何擔保, 內含第三方聲明, GPL 全文, Unicode 使用條款與對應原始碼的位置
- `新增` 啟動器圖示: 可選自適應圖示 (亮色), 自適應圖示 (暗色), 自適應圖示 (自動, 預設) 或透明背景圖示
- `新增` 介面, README, 外掛中心說明與更新日誌支援 10 種語言: 簡體中文, 繁體中文 (香港, 台灣), 英文, 法文, 西班牙文, 日文, 韓文, 俄文與阿拉伯文
- `相依性` 附加 sherpa-onnx 版本 1.13.8
- `相依性` 附加 Jetpack Compose BOM 版本 2026.09.00
- `相依性` 附加 AndroidX Activity Compose 版本 1.12.2
- `相依性` 附加 AndroidX Core KTX 版本 1.15.0
- `相依性` 附加 Kotlin Coroutines 版本 1.11.0
- `相依性` 附加 Material Color Utilities (MaterialKolor) 版本 4.1.1

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
py -I .python\corresponding_source.py build --downloads <directory outside the repository>
```

******

### 授權

******

外掛原始碼使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). sherpa-onnx 官方 Android 程式庫 (Apache-2.0) 靜態連結了 eSpeak NG 與 UCD Tools, 二者使用 GNU GPL 3.0 或更新版本, 因此發布的 APK 整體按 GPL-3.0-or-later 散布 (MPL-2.0 第 3.3 節允許這種組合), 不提供任何擔保. 每個發行版附有對應原始碼 `autojs6-plugin-three-macaw-speech-corresponding-source-sherpa-onnx-v1.13.8-ac68c80b.tar`: sherpa-onnx 1.13.8 的原始碼與建置指令碼, 以及建置時按 URL 與 SHA-256 固定的全部相依封存檔; 其 SHA-256 列在發行版的 `SHA256SUMS` 中, 可以用 `.python/corresponding_source.py` 重新產生與校驗. ONNX Runtime 與 silero VAD 使用 MIT 授權, 各模型的授權見上文模型表. 外掛的關於頁可以查看授權全文與第三方聲明.

- [第三方聲明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: speech 模組: https://docs.autojs6.com/#/speech
- sherpa-onnx 專案: https://github.com/k2-fsa/sherpa-onnx
