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

插件是獨立安裝的 APK, 有自己的介面, 用來下載與管理模型, 試聽試說並調整設定. AutoJs6 腳本透過全域模組 `speech` 使用這些能力; 合成能力亦註冊為 Android 系統 TTS 引擎, 識別能力亦以系統語音輸入, 系統語音識別服務與數碼助理的形式提供, 因此其他應用程式也能使用. 識別與合成模型不隨 APK 提供, 按需下載, 只有語音活動偵測模型隨套件內置.

> 在腳本中使用 `speech` 模組需要 AutoJs6 6.8.0 (內部版本號 5456) 或更高版本. 系統 TTS 引擎, 系統語音輸入, 語音識別服務, 數碼助理與插件自己的介面不依賴 AutoJs6, 安裝插件並下載模型後即可使用.

******

### 功能亮點

******

- 語音識別: 咪高峰串流聽寫 (即時返回部分結果與整句結果) 與音訊檔案識別 (WAV, MP3, AAC, Ogg, FLAC 等, 亦可以是影片檔案中的聲音), 提供中文, 英文, 中英混合以及日語, 韓語, 粵語的離線模型.
- 語音活動偵測: 內置 silero VAD v5, 既可單獨切分音訊中的語音段, 亦用於聽寫時的自動斷句.
- 喚醒詞: 直接填寫關鍵詞原文 (例如 `小六小六` 或 `hey macaw`), 無需訓練模型, 喚醒後可自動轉入聽寫.
- 語音合成: 多種中英文神經網絡音色, 可合成到 WAV 檔案, 亦可作為系統 TTS 引擎供 AutoJs6 的 `tts` 模組和其他應用程式使用.
- 系統語音輸入: 其他應用程式 (例如 3-Stove Agent) 的麥克風按鈕可以使用離線識別, 在底部面板中說一句話, 識別出的文字直接交回該應用程式.
- 數碼助理: 把 3-Macaw Speech 選為預設數碼助理應用程式後, 長按電源鍵或 Home 鍵說一句話, 需要時編輯識別出的文字, 再交給 3-Stove Agent, 3-Stone AI 等接收語音指令的應用程式, 或複製. 它也提供系統語音識別服務: Android 11 及以下版本選它為助理時一般會一併設為預設識別服務; Android 12 起需在系統的語音輸入設定中另行選擇 (部分手機沒有這項設定), 應用程式也可以直接指定它.
- 觸發式入口: 不必長按電源鍵也能開啟同一個聽寫面板: 快速設定圖塊 (鎖定畫面上也能用), 主畫面小工具與應用程式圖示捷徑, 可拖動的懸浮麥克風 (預設關閉), 長按或連按兩下音量鍵的按鍵觸發 (只過濾按鍵的無障礙服務, 預設關閉), 以及藍牙耳機的語音鍵. 平時不錄音, 只在面板聽寫的幾秒內使用麥克風.
- 常駐喚醒 (預設關閉): 開啟後說出喚醒詞 (預設 `小鹦小鹦` 與 `Hey Macaw`, 可修改) 即開啟同一個聽寫面板, 熄屏或上鎖時也會點亮螢幕, 並朗讀識別出的文字. 監聽期間咪高峰指示燈常亮, 有一則常駐通知, 也更耗電; 可設監聽時長 (10, 30, 60 分鐘後自動關閉或一直開著) 與只在充電時監聽. 腳本, 其他應用程式或其他入口要用咪高峰時自動讓出, 用完後繼續.
- 插件工具 (需要 AutoJs6 6.8.0 及以上): 提供 `speech_listen` (聽一句話並返回識別出的文字), `speech_speak` (朗讀一段文字), `speech_transcribe` (識別手機上的音訊檔案) 與 `speech_synthesize` (把文字合成為 WAV 或 M4A 檔案) 四個工具. MCP Server 插件用它們讓電腦上的 AI 助手通過手機聽和說, 腳本也可以用 `plugins.tools` 調用.
- 模型管理: 從已簽署的模型目錄下載, 逐個檔案校驗 SHA-256 後才啟用, 亦可匯入本機模型; 僅限非商業用途的模型在下載前會提示授權.
- 獨立介面: 首頁顯示運行狀態並管理模型, 試聽試說頁直接試驗聽寫, 喚醒詞與合成, 設定頁調整語言, 夜間模式, 主題色, 啟動器圖示與識別合成的預設值, 外觀預設跟隨 AutoJs6.

******

### 介面截圖

******

以下截圖來自 Sony XQ-DQ72 (Android 13). 聽寫內容是 sherpa-onnx 測試錄音經手機揚聲器播放後識別的結果, 不含個人資料.

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/home.png?raw=true" alt="首頁: 運行狀態與按用途分組的模型" width="360" />
      <br />
      <sub>首頁: 運行狀態與按用途分組的模型</sub>
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
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/assistant.png?raw=true" alt="數碼助理: 編輯識別結果並選擇交給哪個應用程式" width="360" />
      <br />
      <sub>數碼助理: 編輯識別結果並選擇交給哪個應用程式</sub>
    </td>
  </tr>
</table>

******

### 安裝與使用

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) 下載與裝置 ABI 對應的 APK 並安裝: 大多數手機選擇 `arm64-v8a`, 不確定時選擇適用於全部裝置但體積更大的 `universal`.
2. 開啟 3-Macaw Speech, 在首頁授予麥克風權限, 並在識別, 喚醒詞與合成各組中下載需要的模型 (目錄推薦的模型是較好的起點, 見下文模型表). Android 13 及以上首次下載時亦會請求顯示下載進度通知.
3. 在 AutoJs6 中使用: 將 AutoJs6 升級到 6.8.0 (內部版本號 5456) 或更高版本, 在插件中心啟用 3-Macaw Speech, 然後在腳本中調用 `speech` 模組 (見下文腳本 API). AutoJs6 的示例代碼 > 3-Macaw Speech 中有四個可直接運行的範例.
4. 在其他應用程式中使用: 系統 TTS 引擎見下文系統 TTS 設定; 系統語音輸入, 語音識別服務與數碼助理見常見問題.

> 每種 ABI 各有一個安裝套件, 另有適用於全部裝置的 `universal` 安裝套件. 安裝套件不含識別與合成模型, 模型在插件介面或腳本中按需下載, 亦可從本機檔案匯入.

******

### 模型

******

識別, 喚醒詞與合成模型的體積在 9 MB 到 410 MB 之間, 因此不隨 APK 提供. 插件內置一份已簽署的模型目錄, 從 AutoJs6 共享資源倉庫的不可變發佈下載模型, 鏡像不可用時改用 sherpa-onnx 的上游位址; 兩條路徑都要逐個檔案校驗 SHA-256 後才啟用模型. 1.0.0 的目錄包含以下模型 (大小為安裝後佔用的空間):

| 模型 ID | 用途 | 語言 | 大小 | 授權 | 商業用途 |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (推薦) | 識別 (離線, 帶標點) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | 可以 |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (推薦) | 識別 (串流) | zh, en | 199.1 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.en.2023-06-26.int8` (推薦) | 識別 (串流) | en | 70.3 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | 識別 (串流) | zh | 167.4 MB | 未聲明 | 僅限非商業用途 |
| `kws.zipformer.zh-en.2025-12-20` (推薦) | 喚醒詞 | zh, en | 8.8 MB | Apache-2.0 | 可以 |
| `tts.vits.melo.zh-en` (推薦) | 合成 (說話人: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | 可以 |
| `tts.vits.icefall.zh-aishell3` (推薦) | 合成 (說話人: 174, 8 kHz) | zh | 32.8 MB | 未聲明 | 未知 |
| `tts.kokoro.multi-lang.v1_1` | 合成 (說話人: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | 可以 |
| `tts.matcha.icefall.zh-baker` | 合成 (說話人: 1, 22.05 kHz) | zh | 81.0 MB | 未聲明, MIT (聲碼器) | 僅限非商業用途 |
| `tts.vits.piper.en-us-lessac-medium` | 合成 (說話人: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 僅限非商業用途 |

僅限非商業用途或授權未知的模型在下載前會提示授權, 腳本下載時需要明確傳入 `acceptLicense: true`. 中文串流 2025 模型的權重未聲明授權, 訓練資料含僅限非商業用途的語料, 因此只從 sherpa-onnx 的上游位址下載, 不經共享資源倉庫鏡像. Kokoro 與 Piper 兩個合成模型附帶 eSpeak NG 的發音資料 (espeak-ng-data, GPL-3.0-or-later).

首頁的匯入本機模型可以從 tar, tar.bz2 或 zip 封存檔或資料夾匯入模型: 與目錄模型一致的檔案按該模型登記, 其他檔案在填寫種類, 模型類型, 名稱與語言後作為自訂模型匯入. 匯入透過系統檔案選擇器完成, 不需要儲存空間權限.

******

### 系統 TTS 設定

******

1. 在插件首頁的合成組中下載至少一個合成模型. 想先試一試時, 體積最小的 `tts.vits.icefall.zh-aishell3` (中文, 174 個說話人) 與中英混讀的 `tts.vits.melo.zh-en` 都是推薦模型.
2. 開啟系統的文字轉語音設定, 把首選引擎設為 3-Macaw Speech. 插件首頁狀態卡中的系統文字轉語音引擎一行可以直接開啟該頁面; 手動尋找時, 它通常位於 設定 > 無障礙 > 文字轉語音輸出, 或 設定 > 系統 > 語言和輸入法 > 文字轉語音輸出 (各廠商的名稱略有不同).
3. 輕按引擎旁的設定按鈕會開啟 3-Macaw Speech 的設定頁, 在合成組中為各語言選擇預設語音並選擇說話人. 語速由系統設定或請求朗讀的應用程式決定, 音調不受支援.
4. 回到系統設定輕按試聽, 即可用該語言的預設語音朗讀範例文字.

每個模型的每個說話人都是一個語音, 名為 `<模型 ID>/<說話人編號>`, 雙語模型的說話人另有 `<模型 ID>/<說話人編號>/en-US` 用於英文. AutoJs6 腳本不修改系統設定也能使用該引擎: `tts(text, { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" })`, 或用 `speech.speak(text, { model, speaker })` 直接指定模型與說話人.

******

### 腳本 API

******

AutoJs6 6.8.0 (內部版本號 5456) 起, 腳本透過全域模組 `speech` (亦可寫作 `$speech`) 使用這些能力. 下面三段範例可以分別運行: 聽寫 30 秒並把每句話附加到檔案, 用喚醒詞喚醒後聽一句話, 合成到檔案並朗讀:

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

- `speech.listen(options)` 開啟聽寫工作階段, 透過 `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` 與 `end` 事件返回結果, `stop()` 提前結束; `speech.recognize(path)` 識別音訊檔案 (WAV 以外的格式需要 AutoJs6 6.8.0 或以上; 亦接受 16 kHz PCM), `speech.detectVoice(path)` 列出其中的語音段.
- `speech.wake(keywords, options)` 開啟喚醒詞工作階段, 關鍵詞直接寫原文 (至多 50 個); `thenListen` 讓每次喚醒後在同一錄音上轉入一輪聽寫, 該輪的工作階段由 `wake` 事件的 `listen` 屬性給出.
- `speech.synthesize(text, path, options)` 合成到 WAV 檔案 (至多 8000 個字元, 可選模型, 說話人與語速); `speech.speak(text, options)` 與 `tts(text, { engine })` 經系統 TTS 引擎朗讀.
- `speech.models` 列出, 下載 (`install`, 可報告進度), 匯入, 刪除模型與設定預設模型; `speech.capabilities()` 返回插件版本, 支援的功能與麥克風權限狀態.
- 整個插件同一時間只有一個麥克風工作階段 (聽寫或喚醒); 缺少麥克風權限時調用以 `PERMISSION_DENIED` 失敗, 開啟插件首頁授予即可. 識別出的文字不會寫入 AutoJs6 的日誌.
- AutoJs6 6.8.0 及以上, 腳本也可以經 `plugins.tools.call` 調用這四個插件工具, 例如 `plugins.tools.call('speech_speak', { text: '你好' })`; 參數與 MCP Server 看到的相同. 日常腳本直接用 `speech` 模組更靈活.
- 完整的方法, 選項, 事件與錯誤碼見 [AutoJs6 文件](https://docs.autojs6.com/#/speech); AutoJs6 的示例代碼 > 3-Macaw Speech 中有聽寫到檔案, 喚醒詞與聽寫, TTS 播報與語音指令交給 Agent 四個範例.

******

### 相容性

******

- Android 7.0 (API 24) 及以上, 目標 API 37. arm64-v8a, armeabi-v7a, x86_64 與 x86 各有安裝套件, 另有 universal 安裝套件; 原生庫按 16 KB 記憶體分頁對齊, 已在 16 KB 分頁模擬器上驗證.
- AutoJs6: 腳本中的 `speech` 模組需要 6.8.0 (內部版本號 5456) 或更高版本; 插件介面, 系統 TTS 引擎, 系統語音輸入, 語音識別服務與數碼助理不依賴 AutoJs6.
- 背景收音: 在 AutoJs6 處於前景時開始的聽寫或喚醒工作階段, AutoJs6 退到背景或熄屏後仍會繼續收音 (插件以麥克風前景服務與通知保持錄音). AutoJs6 已在背景時才開啟的工作階段可能被系統靜音, 此時工作階段收到錯誤碼為 `PERMISSION_DENIED` 的 `warning` 事件但不會結束, AutoJs6 回到前景後恢復; 需要背景收音的腳本請在 AutoJs6 可見時開啟工作階段.
- 實測裝置: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 24, 35 與 37 (16 KB 分頁) 模擬器.
- 效能: 在低階的 Sony G8441 上, 串流模型載入約 1.7 秒, SenseVoice 約 2.8 秒, 聽寫處理速度約為即時的 3 倍; MeloTTS 與 Kokoro 在這類裝置上合成慢於即時, AISHELL-3 最快. Sony XQ-DQ72 等較新裝置載入約 0.5 到 1.2 秒, 合成快於即時.
- 記憶體: 一個識別模型載入後約佔 210 到 300 MB 原生記憶體. 插件按裝置記憶體的四分之一 (384 MB 到 1.5 GB) 管理已載入的模型, 閒置 5 分鐘後釋放 (可在設定中調整為 1 到 30 分鐘).
- 數碼助理: 需要系統允許選擇第三方數碼助理應用程式. 喚起方式視乎系統: 長按 Home 鍵 (三按鈕導覽), 從螢幕底部角落斜向上滑, 或在系統設定中把長按電源鍵設為喚起數碼助理. 實測的 ColorOS 與 HyperOS 平板電腦 (Android 15) 把電源鍵與手勢留給自家助理, 可以改用觸發式入口.
- 觸發式入口: 實測 Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), Xiaomi Pad 23046RP50C (HyperOS, Android 15), 以及 API 33 與 35 模擬器. ColorOS 與 HyperOS 在鎖定畫面上點圖塊會先要求解鎖, 解鎖後開啟面板; 部分主畫面接受加入小工具的請求卻不顯示確認, 此時按設定中的步驟手動加入. 螢幕關閉時手機不把按鍵交給無障礙服務, 按鍵觸發只在螢幕開啟時可用. 藍牙耳機語音鍵: Sony WF-1000XM4 配合 XQ-DQ72 (Android 13) 實測每次長按都開啟面板, 並用耳機麥克風收音; 其他應用程式 (例如 Google 應用程式) 也回應時系統先顯示選擇器. 實測 HyperOS 平板 (Xiaomi Pad 23046RP50C, Android 15) 的選擇器在選中 3-Macaw Speech 後沒有反應.
- 常駐喚醒: 實測 Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 35 (16 KB 分頁) 模擬器. 螢幕關閉 30 分鐘耗電 (Sony XQ-DQ72, Android 13; Android 按機型功耗表估算, 手機經 USB 連接, 未充電狀態為模擬): 開啟時 17.9 mAh, 插件 CPU 時間 454 秒 (平均約佔一個 CPU 核心的四分之一), 期間一直在錄音; 關閉時 0.006 mAh, 插件沒有在執行. 實際耗電因機型而異.
- 插件工具: 實測 Sony G8441 (Android 9), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 24 與 35 模擬器 (API 24 只測不需要模型的部分); 經 MCP Server 調用實測 Sony G8441 (Android 9).

******

### 常見問題

******

#### 識別與合成需要連網嗎?

不需要. 模型下載完成後, 全部運算都在裝置上進行, 音訊不會上傳. 只有下載模型與重新整理模型目錄時才會連網.

#### 為何由插件自行申請咪高峰權限?

錄音在插件程序內完成, 音訊不經過 AutoJs6 傳輸. AutoJs6 綁定插件時會傳遞前景能力, 插件在麥克風工作階段期間亦會持有麥克風前景服務, 因此工作階段開始後 AutoJs6 退到背景或熄屏也能繼續聽寫. 首次使用前請在插件介面授予麥克風權限.

#### 應該安裝哪個安裝套件?

大多數手機選擇 `arm64-v8a`. 不確定裝置 ABI 時安裝 `universal`, 它適用於全部裝置, 但體積更大.

#### 為甚麼系統設定裏試聽沒有聲音, 或者引擎顯示缺少語音資料?

引擎只報告已安裝的合成模型所涵蓋的語言. 沒有安裝合成模型, 或已安裝的模型不涵蓋裝置語言時, 系統會顯示缺少語音資料, 試聽與語速設定亦可能不可用. 在插件首頁下載一個涵蓋裝置語言的合成模型後重新開啟系統設定即可; 試聽使用該語言的預設語音, 可以在插件設定的合成組中更改.

#### 其他應用程式能用 3-Macaw Speech 做語音輸入嗎?

可以. 安裝識別模型並授予麥克風權限後, 調用系統語音輸入的應用程式 (例如 3-Stove Agent 的麥克風按鈕) 會開啟 3-Macaw Speech 的面板. 裝置上還裝有 Google 等語音輸入應用程式時, Android 會詢問要用哪一個, 並可記住選擇. 不想讓其他應用程式看到這個入口時, 在設定的識別組中關閉作為系統語音輸入開關.

#### 怎樣把 3-Macaw Speech 設為預設數碼助理?

開啟 3-Macaw Speech 的設定, 輕按識別組中的數碼助理, 系統會開啟相應的設定頁, 在其中把 3-Macaw Speech 選為數碼助理應用程式 (部分系統的頁面名稱略有不同). 之後長按電源鍵或 Home 鍵, 或使用助理手勢, 即可聽寫. 如果手機的電源鍵已設為其他功能, 可改用 Home 鍵或助理手勢.

#### 常駐喚醒為甚麼讓咪高峰指示燈一直亮, 耗電多少?

持續監聽喚醒詞需要一直錄音: Android 12 起只要有應用程式在錄音, 狀態列就會一直顯示咪高峰指示燈 (綠點), 應用程式無法隱藏; 系統裏低功耗, 不亮指示燈的熱詞通道只開放給預載的系統助理, 第三方應用程式即使被選為預設數碼助理也用不了. 所以常駐喚醒預設關閉, 開啟後一直顯示綠點與一則常駐通知 (可在通知中直接關閉). 實測 Sony XQ-DQ72 螢幕關閉 30 分鐘, 開啟時 Android 估算本插件耗電約 18 mAh (約為電池容量的 0.4%), 關閉時幾乎為 0. 建議只在需要時開啟: 把監聽時長設為 10, 30 或 60 分鐘, 或開啟只在充電時監聽 (拔掉電源時暫停, 接上後繼續); 不常用時, 圖塊, 小工具, 懸浮咪高峰等觸發式入口平時不錄音.

#### 為甚麼聽到喚醒詞後只出現 "點此說話" 通知?

Android 10 起, 背景應用程式只有在是預設數碼助理, 或者獲得了在其他應用程式上層顯示的權限時, 才能直接開啟畫面. 3-Macaw Speech 不是預設數碼助理時, 開啟常駐喚醒會詢問一次這項權限; 不授予時, 聽到喚醒詞後發出一則 "點此說話" 通知, 按下即開啟面板, 1 分鐘後自動撤銷. 想讓喚醒詞直接開啟面板, 可以把 3-Macaw Speech 設為預設數碼助理, 或在系統設定中允許它在其他應用程式上層顯示.

#### 為甚麼開啟常駐喚醒時提示需要先關閉 3-Stove Agent?

3-Macaw Speech, 3-Stone AI 與 3-Stove Agent 的常駐監聽互斥: 一部手機同一時間只能有一個應用程式常駐監聽, 否則它們會爭用咪高峰. 其中一個開著時, 另一個的開關會說明是誰在監聽, 並可直接開啟對方的設定去關閉. 重新啟動或更新後, 常駐喚醒不會自動開始錄音: 開關仍開著時會發出一則 "點此恢復常駐喚醒" 通知, 按下或開啟 3-Macaw Speech 的主頁, 設定頁都會恢復; 監聽時長在此期間已到則直接關閉.

#### AI 助手怎樣通過 3-Macaw Speech 聽和說?

需要 AutoJs6 6.8.0 及以上與 MCP Server 插件. 3-Macaw Speech 是官方插件, `speech_listen`, `speech_speak` 與 `speech_transcribe` 預設出現在 MCP Server 的工具列表中 (可在 MCP Server 設定的 "插件提供的工具" 中關閉); `speech_synthesize` 會寫入檔案, 還要開啟該分組的第二個開關, 未指定位置時檔案保存在 AutoJs6 工作目錄的 plugin-outputs/speech 下. `speech_listen` 每次聽一句話 (最長 60 秒): 能直接錄音時 (例如開着常駐喚醒, 3-Macaw Speech 是預設數碼助理或已獲得在其他應用程式上層顯示的權限) 立即開始, 否則手機上出現一則 "點此說話" 通知, 1 分鐘內按下後開始. 聽寫期間其他應用程式的常駐監聽暫時讓出, 聽完自動恢復; 已有聽寫在進行時返回 BUSY, 缺少咪高峰權限或識別模型時返回 NOT_READY (開啟插件首頁處理即可). `speech_speak` 有該語言的 3-Macaw Speech 語音時用它朗讀, 否則用系統語音.

#### 不長按電源鍵, 還有哪些方式開啟聽寫面板?

開啟 3-Macaw Speech 的設定, 在觸發方式組中設定: 快速設定圖塊, 點按加入 (Android 13 起由系統確認, 更早的版本按提示手動加入), 之後下拉快速設定點語音指令; 主畫面小工具與捷徑, 把按一下說話小工具放到主畫面, 或長按應用程式圖示選擇語音指令; 懸浮麥克風, 開啟開關並授予在其他應用程式上層顯示的權限, 螢幕邊緣會出現可拖動的麥克風按鈕; 按鍵觸發, 在系統無障礙設定中開啟 3-Macaw Speech 按鍵觸發, 預設長按音量下鍵約 0.8 秒, 可改為長按音量上鍵或連按兩下音量下鍵. 這些入口都開啟與數碼助理相同的面板, 說完後選擇接收文字的應用程式; 3-Macaw Speech 是預設數碼助理時經助理工作階段開啟, 不是時直接開啟同款面板.

#### 按鍵觸發為甚麼需要無障礙服務, 安全嗎?

Android 只允許無障礙服務在系統處理之前取得實體按鍵, 所以按鍵觸發是一個無障礙服務. 它只申請過濾按鍵, 不訂閱任何介面事件, 不能讀取螢幕內容, 也不能執行手勢; 只攔截所選的音量鍵, 短按照常調整音量, 螢幕關閉, 來電或通話時不攔截. 開關是灰色時 (Android 13 起透過安裝檔安裝的應用程式), 先開啟 3-Macaw Speech 的應用程式資料頁, 在選單中選擇允許受限制的設定.

#### 藍牙耳機的語音鍵能開啟聽寫面板嗎?

可以. 3-Macaw Speech 回應藍牙服務發出的語音指令請求 (VOICE_COMMAND), 長按耳機的語音鍵或觸控區即開啟聽寫面板. Android 12 起, 面板第一次會詢問附近的裝置權限. 允許後 3-Macaw Speech 接管耳機的語音識別: 用耳機麥克風收音, 每次按鍵都有效. 不允許時用手機麥克風收音, 部分耳機 (例如 Sony WF-1000XM4) 會隔一次按鍵才開啟面板. 手機上還有其他應用程式回應語音指令 (Google 應用程式, 廠商助理) 時, 系統會先讓你選擇; 實測 HyperOS 的選擇器在選中 3-Macaw Speech 後沒有反應, 這是系統選擇器的問題. 不想使用時, 在設定的觸發方式組中關閉耳機語音鍵.

#### 下載模型失敗怎麼辦?

插件優先從 AutoJs6 共享資源倉庫的 GitHub 發佈下載, 失敗時自動改用 sherpa-onnx 的上游位址, 中斷的下載可以繼續. 無法存取 GitHub 時, 可以在其他裝置下載模型封存檔 (模型目錄中的鏡像或上游位址), 再透過首頁的匯入本機模型匯入; 與目錄一致的檔案會按目錄模型登記.

#### 模型佔用多少空間, 怎樣釋放?

各模型安裝後的大小見上文模型表. 首頁可以刪除不再需要的模型; 設定中的回收未引用的檔案會刪除不再需要的下載檔案, 匯入副本與中斷的安裝, 已安裝的模型與暫停的下載會保留. 解除安裝插件會刪除全部模型.

#### 識別結果為甚麼沒有標點?

串流 Zipformer 模型只輸出文字, 不加標點. SenseVoice 模型按語音段識別, 會加標點並報告語種, 適合需要完整句子的場合: 在腳本中以 `model` 選項指定它, 或在設定中把它設為預設識別模型.

#### 喚醒詞怎麼寫, 為甚麼沒有被喚醒?

直接寫關鍵詞原文: 中文按常用讀音拼寫為拼音, 英文單詞按模型詞典拼寫 (不在詞典中的單詞會被拒絕), 多音字可以直接寫模型的拼音 token 來指定讀音. 三到四個音節, 不常見的詞組更不容易誤喚醒. 每個關鍵詞可以設定 `threshold` (越低越容易喚醒) 與 `boost`. 先在試聽試說頁的喚醒詞試驗中試一試, 再寫進腳本.

#### 為甚麼 APK 按 GPL-3.0 傳播, 原始碼在哪裏?

插件自身的程式碼使用 MPL-2.0. sherpa-onnx 官方 Android 庫靜態連結了 eSpeak NG (GPL-3.0-or-later), 所以 APK 整體按 GPL-3.0-or-later 傳播. 每個發行版都附有對應原始碼封存檔 (sherpa-onnx 的原始碼與建置腳本及其全部依賴), 檔名含 `corresponding-source`, SHA-256 列在 `SHA256SUMS` 中; 插件原始碼就是本倉庫在發行標籤處的內容.

******

### 權限與安全

******

插件在設計上盡量收窄權限範圍:

- 咪高峰: 只在聽寫, 喚醒詞, 試聽試說與開啟常駐喚醒期間錄音, 會話結束或腳本退出後立即釋放咪高峰.
- 網絡: 只用於下載模型與重新整理已簽署的模型目錄, 識別與合成不連網.
- 儲存: 模型儲存在插件私有目錄, 匯入本機模型透過系統檔案選擇器完成, 不申請儲存權限.
- 受保護的入口: 能力服務與啟動入口均受 AutoJs6 插件權限 (`org.autojs.permission.PLUGIN`) 保護, 第三方應用程式無法直接調用; 系統 TTS 引擎及其語音檢查, 示例文字與設定入口按 Android 的慣例對所有應用程式開放, 只提供語音合成.
- 系統語音輸入: 與系統 TTS 引擎一樣向所有應用程式開放; 只在面板顯示時錄音, 只傳回識別出的文字, 可在設定中關閉.
- 數碼助理: 不讀取螢幕內容, 未開啟常駐喚醒時不在背景收音; 只在面板顯示時錄音, 識別出的文字只在你按下應用程式按鈕或複製時離開面板 (上鎖時需先解鎖). 語音識別服務只為本身持有麥克風權限的應用程式識別.
- 觸發式入口: 圖塊, 小工具, 捷徑, 懸浮麥克風, 按鍵觸發與耳機語音鍵本身都不錄音, 只負責開啟聽寫面板. 按鍵觸發是只過濾按鍵的無障礙服務: 不讀取螢幕內容, 不執行手勢, 只攔截所選的音量鍵 (短按照常調整音量, 螢幕關閉, 來電或通話時不攔截). 懸浮麥克風需要在其他應用程式上層顯示的權限, 以一則靜音通知保持顯示; 開機自動啟動只用於在重新啟動後恢復已開啟的懸浮麥克風. Android 12 起耳機語音鍵需要附近的裝置權限, 只在耳機開啟的面板中詢問: 只用於讓藍牙服務把語音識別交給面板並接通耳機麥克風, 不掃描裝置, 不讀取位置.
- 常駐喚醒: 預設關閉, 只能在插件介面中開啟 (重新啟動或更新後需要按通知恢復, 不會自動錄音). 喚醒詞在本機偵測, 音訊不儲存也不上傳, 識別出的文字不寫入日誌; 開啟期間一直錄音, 咪高峰指示燈常亮, 常駐通知可直接關閉. 與 3-Stone AI, 3-Stove Agent 的常駐監聽互斥, 系統同一時間只允許一個.
- 插件工具: 工具服務同樣受 AutoJs6 插件權限保護, 只有 AutoJs6 能直接調用. `speech_listen` 每次只錄一句話, 錄音期間有通知 (Android 12 起還有咪高峰指示燈); `speech_transcribe` 只讀取 AutoJs6 交來的那一個檔案, `speech_synthesize` 把音訊交回 AutoJs6 寫到指定位置. 識別與朗讀的文字都不寫入日誌.

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
tool service: org.autojs.plugin.TOOL_PROVIDER (short name speech: listen, speak, transcribe, synthesize; AutoJs6 6.8.0+)
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### 致謝

******

3-Macaw Speech 建立在以下開源專案, 模型與資料之上, 感謝它們的作者與維護者:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) 與 [k2-fsa (新一代 Kaldi)](https://github.com/k2-fsa) 團隊: 識別, 喚醒詞, 語音活動偵測與合成的運行時, 各模型的 ONNX 匯出與 Android 庫 (Apache-2.0).
- [icefall](https://github.com/k2-fsa/icefall) 訓練的模型: Zipformer 串流識別模型 (中英雙語 pfluo, 英文 Zengwei, 中文 2025 yuekai), 開放詞表喚醒詞模型 (pkufool), 以及 k2-fsa 的 AISHELL-3 VITS 與標貝 Matcha-TTS 合成模型.
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / 阿里巴巴, FunASR 模型授權 1.1): 多語種離線識別模型.
- [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, 中文錄音由 LongMaoData 授權) 與 [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT) 合成模型, [HiFi-GAN](https://github.com/jik876/hifi-gan) 聲碼器 (MIT).
- 訓練資料: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), [標貝中文標準女聲音庫](https://www.data-baker.com/open_source.html) (僅限非商業用途), Lessac Technologies 的 Blizzard 2013 資料 (僅限研究) 與 [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (公有領域).
- [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT) 推理引擎, [silero VAD](https://github.com/snakers4/silero-vad) (MIT) 語音活動偵測模型, [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) 與 [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT) 音素轉換, [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT) 中文喚醒詞讀音表.

******

### 開發路線圖

******

功能規劃, 設計決策與完成情況以可勾選清單維護在 ROADMAP.md 中. 未勾選條目表示規劃意向, 不代表目前版本能力.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.2.0

_2026/10/21_

- `提示` 新增常駐喚醒, 預設關閉: 開啟後一直在本機監聽喚醒詞, 麥克風指示燈常亮, 有一則常駐通知, 也更耗電 (熄屏 30 分鐘實測見 README). 建議配合 "監聽時長" 或 "只在充電時監聽" 使用
- `新增` 常駐喚醒 (設定 > 常駐喚醒): 說出喚醒詞 (預設 "小鹦小鹦" 與 "Hey Macaw", 可修改, 靈敏度分高, 中, 低三檔) 即開啟與數碼助理相同的聽寫面板; 熄屏或上鎖時也會點亮螢幕, 傳送或複製前先解鎖
- `新增` 由喚醒詞開啟的面板會朗讀識別出的文字, 有該語言的 3-Macaw Speech 語音時使用它, 否則使用系統語音; 可在設定中關閉
- `新增` 監聽時長 (10, 30, 60 分鐘後自動關閉, 或一直開著) 與只在充電時監聽 (拔掉電源時暫停, 接上後繼續), 與 3-Stove Agent 的同名選項一致
- `新增` 3-Macaw Speech 不是預設數碼助理時, 開啟常駐喚醒會詢問一次在其他應用程式上層顯示的權限; 不授予時, 聽到喚醒詞後發出 "點此說話" 通知, 點按即開啟面板
- `新增` 指令碼, 其他應用程式或本插件的其他入口使用麥克風時, 常駐喚醒自動讓出, 對方用完後繼續監聽
- `新增` 與 3-Stone AI 和 3-Stove Agent 的常駐監聽互斥, 系統同一時間只能有一個; 對方開著時會提示先關閉, 並可直接開啟對方的設定
- `新增` 重新啟動或更新後不會自動錄音: 開關仍開著時發出一則 "點此恢復常駐喚醒" 通知, 開啟主頁或設定頁也會恢復
- `新增` 識別音訊檔案 (speech.recognize, speech.detectVoice) 除 WAV 外亦支援 MP3, AAC (包括 M4A), Ogg (Vorbis, Opus), FLAC, AMR 等常見格式, 以及影片檔案 (MP4, WebM 等) 中的聲音; 檔案上限仍為 200 MB 或 30 分鐘 (需要 AutoJs6 6.8.0 或以上, 較早的版本只能傳入 WAV)
- `新增` 4 個插件工具 (需要 AutoJs6 6.8.0 及以上): speech_listen 聽一句話並返回文字, speech_speak 朗讀, speech_transcribe 識別手機上的音訊檔案, speech_synthesize 把文字合成為 WAV 或 M4A 檔案. MCP Server 插件用它們讓電腦上的 AI 助手通過手機聽和說, 腳本也可以經 plugins.tools 調用; 不能直接錄音時, 聽寫先在手機上發出 "點此說話" 通知, 按下後開始

#### v1.1.0

_2026/10/21_

- `提示` 新增觸發式入口: 平時不錄音, 點按快速設定圖塊, 主畫面小工具, 捷徑, 懸浮麥克風或按下音量鍵, 藍牙耳機語音鍵後才開啟與數碼助理相同的聽寫面板
- `提示` 新增權限: 在其他應用程式上層顯示 (只用於懸浮麥克風, 預設關閉), 開機自動啟動 (只在懸浮麥克風開啟時於重新啟動後恢復它), 特殊用途前景服務 (只保持懸浮麥克風顯示, 不錄音), 附近的裝置 (Android 12 起只在用耳機語音鍵開啟面板時詢問, 用於接管耳機的語音識別與麥克風). 按鍵觸發需要在系統無障礙設定中開啟, 只過濾按鍵
- `新增` 快速設定圖塊 (語音指令): 在設定的觸發方式組中加入 (Android 13 起由系統確認, 更早的版本顯示手動步驟), 下拉快速設定點它即可說話; 鎖定畫面上可以直接聽寫, 傳送或複製前先解鎖 (ColorOS 與 HyperOS 會先要求解鎖); 長按圖塊開啟觸發方式設定
- `新增` 懸浮麥克風: 預設關閉, 開啟時申請在其他應用程式上層顯示的權限; 可沿螢幕邊緣拖動的圓形按鈕, 點按即可說話; 按鈕本身不錄音, 以一則靜音通知保持顯示 (可在通知中關閉), 開啟時重新啟動後自動恢復; 聽寫面板顯示期間自動隱藏, 可與 3-Stove Agent 的懸浮球同時顯示
- `新增` 主畫面小工具 (按一下說話) 與長按應用程式圖示出現的捷徑 (語音指令) 都開啟聽寫面板; 設定中可以直接把小工具放到主畫面, 主畫面不支援時顯示手動步驟
- `新增` 按鍵觸發: 只過濾按鍵的無障礙服務, 不讀取螢幕內容, 需要在系統無障礙設定中開啟; 預設長按音量下鍵約 0.8 秒開啟聽寫面板, 可改為長按音量上鍵或連按兩下音量下鍵; 短按照常調整音量, 螢幕關閉, 來電或通話時不攔截按鍵
- `新增` 藍牙耳機語音鍵: 長按耳機的語音鍵或觸控區即開啟聽寫面板 (3-Macaw Speech 回應語音指令請求 VOICE_COMMAND). 允許附近的裝置權限後用耳機麥克風收音, 每次按鍵都有效. 其他應用程式也能回應時, 系統會先讓你選擇. 可在設定的觸發方式組中關閉耳機語音鍵
- `新增` 各入口開啟的面板與長按電源鍵時相同: 3-Macaw Speech 是預設數碼助理時經助理工作階段開啟, 不是時直接開啟同款面板; 系統沒有顯示助理工作階段時 (例如在鎖定畫面上點圖塊後才解鎖) 也直接開啟面板
- `新增` 打包應用透過獨立入口使用語音識別與模型管理, 麥克風存取遵循各應用本身的錄音授權
- `修復` 聽寫面板一開啟就開始錄音: 模型仍在載入時 (首次開啟, 或閒置 5 分鐘後) 開頭說的話不再遺失, 你好這樣的短句也不再沒有反應

#### v1.0.0

_2026/10/20_

- `提示` 首個正式版本. 腳本中的 speech 模組需要 AutoJs6 6.8.0 (5456) 或更高版本; 插件介面, 系統 TTS 引擎, 系統語音輸入, 語音識別服務與數碼助理不依賴 AutoJs6
- `提示` 識別與合成模型不隨安裝套件提供, 需要在插件介面或腳本中下載; 應用程式整體按 GNU GPL 3.0 或更新版本傳播 (語音運行時靜態連結了 eSpeak NG), 對應源代碼隨每個發行版提供
- `新增` 作為 AutoJs6 的語音插件: 在插件中心啟用後, 腳本可以透過 speech 模組使用語音識別, 喚醒詞, 語音活動偵測, 語音合成與模型管理
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
- `新增` 數碼助理: 在系統設定中把 3-Macaw Speech 選為預設數碼助理應用程式後, 長按電源鍵或 Home 鍵 (或使用助理手勢) 即可在底部面板中說一句話; 識別出的文字可以編輯, 再按按鈕交給接收語音指令的應用程式 (例如 3-Stove Agent 或 3-Stone AI), 也可以複製或重說; 按下按鈕之前不會傳送任何內容. 上鎖時也能使用, 傳送或複製前會先解鎖. 助理不讀取螢幕內容, 平時不在背景收音. 設定的識別組會顯示它是否為預設助理, 並可開啟系統設定進行選擇
- `新增` 系統語音識別服務: 使用 Android 語音識別介面 (SpeechRecognizer) 的應用程式可以直接指定 3-Macaw Speech, 使用它的離線識別並取得中間結果; 把它設為預設識別服務後, 沒有指定的應用程式也會使用它. Android 11 及以下版本選它為數碼助理時一般會一併設為預設; Android 12 起需在系統的語音輸入設定中另行選擇 (部分手機沒有這項設定). 其他應用程式 (例如 3-Stone AI) 借助 3-Macaw Speech 持續監聽喚醒詞時, 數碼助理, 系統語音輸入, 語音識別服務與試聽試說頁仍可使用麥克風: 期間喚醒詞檢測暫停, 結束後自動恢復
- `新增` 設定: 語言, 夜間模式, 主題色與啓動器圖標 (預設跟隨 AutoJs6); 預設識別與合成模型, 端點靈敏度, 語音活動偵測, 說話人, 語速與各語言的預設語音, 作用於試聽試說頁與系統 TTS 引擎 (腳本仍使用文件中的預設值); 閒置模型的釋放時間, 刷新模型目錄與回收未引用的檔案; 系統文字轉語音設定中 3-Macaw Speech 的設定按鈕會打開此頁
- `新增` 關於: 版本, 插件內置的發行歷史, 更新檢查 (每 12 小時至多自動檢查一次, 可忽略某個版本), 以及授權與聲明: 應用程式整體按 GNU GPL 3.0 或更新版本傳播且不提供任何擔保, 內含第三方聲明, GPL 全文, Unicode 使用條款與對應源代碼的位置
- `新增` 啓動器圖標: 可選自適應圖示 (亮色), 自適應圖示 (暗色), 自適應圖示 (自動, 預設) 或透明背景圖示
- `新增` 介面, README, 插件中心說明與更新日誌支援 10 種語言: 簡體中文, 繁體中文 (香港, 台灣), 英文, 法文, 西班牙文, 日文, 韓文, 俄文與阿拉伯文
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
py -I .python\corresponding_source.py build --downloads <directory outside the repository>
```

******

### 許可

******

插件原始碼使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). sherpa-onnx 官方 Android 庫 (Apache-2.0) 靜態連結了 eSpeak NG 與 UCD Tools, 二者使用 GNU GPL 3.0 或更新版本, 因此發佈的 APK 整體按 GPL-3.0-or-later 傳播 (MPL-2.0 第 3.3 節允許這種組合), 不提供任何擔保. 每個發行版附有對應原始碼 `autojs6-plugin-three-macaw-speech-corresponding-source-sherpa-onnx-v1.13.8-ac68c80b.tar`: sherpa-onnx 1.13.8 的原始碼與建置腳本, 以及建置時按 URL 與 SHA-256 固定的全部依賴封存檔; 其 SHA-256 列在發行版的 `SHA256SUMS` 中, 可以用 `.python/corresponding_source.py` 重新產生與校驗. ONNX Runtime 與 silero VAD 使用 MIT 授權, 各模型的授權見上文模型表. 插件的關於頁可以查看授權全文與第三方聲明.

- [第三方聲明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相關連結

******

- AutoJs6 項目: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: speech 模組: https://docs.autojs6.com/#/speech
- sherpa-onnx 專案: https://github.com/k2-fsa/sherpa-onnx
