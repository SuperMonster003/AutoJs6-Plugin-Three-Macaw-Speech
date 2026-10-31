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

- 語音辨識: 麥克風串流聽寫 (即時傳回部分結果與整句結果) 與音訊檔案辨識 (WAV, MP3, AAC, Ogg, FLAC 等, 也可以是影片檔案中的聲音), 提供中文, 英文, 中英混合以及日語, 韓語, 粵語的離線模型. 法語, 俄語與韓語也有串流模型, 可以邊說邊出字. 1.3.0 新增西班牙語離線模型 (含標點), 以及可自動判斷語言的八語串流模型: 阿拉伯語, 印尼語, 泰語, 越南語, 日語, 俄語, 中文與英文.
- 自動標點: 安裝標點模型後, 串流模型聽寫或辨識出的每句話會自動加上逗號, 句號與問號 (中文與英文), 英文也會恢復大寫; SenseVoice 自帶標點, 不受影響. 標點模型在首頁的 "標點" 分組中下載.
- 語音活動偵測: 內建 silero VAD v5, 既可單獨切分音訊中的語音段, 也用於聽寫時的自動斷句.
- 喚醒詞: 直接填寫關鍵詞原文 (例如 `小六小六` 或 `hey macaw`), 無須訓練模型, 喚醒後可自動轉入聽寫.
- 語音合成: 多種中英文神經網路音色, 可合成到 WAV 檔案, 也可以作為系統 TTS 引擎供 AutoJs6 的 `tts` 模組和其他應用程式使用. 另有法語, 西班牙語, 德語與俄語的 Piper 聲音 (僅限非商業用途). 1.3.0 新增 Kokoro v1.0 多語言聲音 (英語, 西班牙語, 法語, 印地語, 義大利語, 巴西葡萄牙語與中文), 以及阿拉伯語與韓語聲音 (僅限非商業用途).
- 系統語音輸入: 其他應用程式 (例如 3-Stove Agent) 的麥克風按鈕可以使用離線辨識, 在底部面板中說一句話, 辨識出的文字直接交回該應用程式.
- 數位助理: 把 3-Macaw Speech 選為預設數位助理應用程式後, 長按電源鍵或 Home 鍵說一句話, 需要時編輯辨識出的文字, 再交給 3-Stove Agent, 3-Stone AI 等接收語音指令的應用程式, 或複製. 它也提供系統語音辨識服務: Android 11 及以下版本選它為助理時一般會一併設為預設辨識服務; Android 12 起需在系統的語音輸入設定中另行選擇 (部分手機沒有這項設定), 應用程式也可以直接指定它.
- 觸發式入口: 不必長按電源鍵也能開啟同一個聽寫面板: 快速設定圖塊 (螢幕鎖定時也能用), 主畫面小工具與應用程式圖示捷徑, 可拖曳的懸浮麥克風 (預設關閉), 長按或連按兩下音量鍵的按鍵觸發 (只過濾按鍵的無障礙服務, 預設關閉), 以及藍牙耳機的語音鍵. 平時不錄音, 只在面板聽寫的幾秒內使用麥克風.
- 常駐喚醒 (預設關閉): 開啟後說出喚醒詞 (預設 `小鹦小鹦` 與 `Hey Macaw`, 可修改) 即開啟同一個聽寫面板, 螢幕關閉或鎖定時也會點亮螢幕, 並朗讀辨識出的文字. 監聽期間麥克風指示燈常亮, 有一則常駐通知, 也更耗電 (預設開啟的省電模式在安靜時不執行喚醒詞模型, 外掛程式佔用的 CPU 少得多); 可設定監聽時長 (10, 30, 60 分鐘後自動關閉或一直開著) 與只在充電時監聽. 腳本, 其他應用程式或其他入口要用麥克風時自動讓出, 用完後繼續.
- 說話者辨識 (1.3.0): 在設定 > 常駐喚醒 > 說話者中輸入名字並跟讀 3 句話, 即可註冊自己的聲音 (可以註冊多人, 也可以用試驗證查看分數). 開啟 "只聽主人" 後, 常駐喚醒聽到喚醒詞時先比對聲音, 只有與已註冊的人相符才開啟面板, 別人說喚醒詞會被忽略; 圖塊, 小工具, 耳機鍵等手動入口不受影響. 指令碼可以用 `speech.speaker` 註冊, 驗證與辨識說話者 (需要 AutoJs6 6.8.0 及以上). 說話者辨識模型約 26.5 MB, 在 "說話者" 頁面依提示下載.
- 音訊分析 (1.3.0): 試聽試說頁的檔案辨識也可以分離說話者 (標出誰在什麼時候說話, 能用已註冊的聲音認出名字; 選 "分離 + 轉寫" 還會把每個人說的話轉成文字), 辨識語言 (Whisper 的 99 種語言), 以及為聲音加上標籤 (人聲, 音樂, 動物, 車輛, 警報等 AudioSet 的 527 類). 下載模型後全部在手機上離線完成 (頁面會提示需要哪些模型). 指令碼可以用 `speech.diarize`, `speech.detectLanguage` 與 `speech.tag` 呼叫 (需要 AutoJs6 6.8.0 及以上).
- 音訊試聽 (1.4.0): 試聽試說頁的檔案辨識可以播放所選檔案, 能拖動進度, 0.5 倍到 2 倍變速, 前後跳 5 秒. 正在播放的句子會醒目顯示, 點一句就從那裡播放, 還可以切換成大字的歌詞式檢視. 外掛只保留最近一個檔案的讀取權限, 清除結果或換檔案時放棄.
- 外掛工具 (需要 AutoJs6 6.8.0 及以上): 提供 `speech_listen` (聽一句話並傳回辨識出的文字), `speech_speak` (朗讀一段文字), `speech_transcribe` (辨識手機上的音訊檔案) 與 `speech_synthesize` (把文字合成為 WAV 或 M4A 檔案) 四個工具. MCP Server 外掛用它們讓電腦上的 AI 助理透過手機聽和說, 指令碼也可以用 `plugins.tools` 呼叫. 1.3.0 新增 `speech_speaker_verify` (判斷一段錄音是不是某位已註冊的人) 與 `speech_speaker_identify` (在已註冊的人中找出最像的一位), 結果只有名字與分數. 1.3.0 另新增三個分析工具: `speech_diarize` (分離說話者, 檔案最長 10 分鐘), `speech_detect_language` (辨識語言) 與 `speech_tag` (為聲音加上標籤).
- 模型管理: 從已簽署的模型目錄下載, 逐一檔案驗證 SHA-256 後才啟用, 也可以匯入本機模型; 僅限非商業用途的模型在下載前會提示授權.
- 獨立介面: 首頁顯示執行狀態並管理模型, 試聽試說頁直接試驗聽寫, 喚醒詞與合成, 還能把手機上的音訊或影片檔案辨識成文字, 儲存為文字或 SRT 字幕, 設定頁調整語言, 夜間模式, 主題色, 啟動器圖示與辨識合成的預設值, 外觀預設跟隨 AutoJs6.

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

辨識, 標點, 喚醒詞, 合成, 說話者與音訊分析模型的大小在 6 MB 到 410 MB 之間, 因此不隨 APK 提供. 外掛內建一份已簽署的模型目錄, 從 AutoJs6 共享資源儲存庫的不可變發布下載模型, 鏡像無法使用時改用 sherpa-onnx 的上游位址; 兩條路徑都要逐一檔案校驗 SHA-256 後才啟用模型. 1.3.0 的目錄包含以下模型 (大小為安裝後佔用的空間):

| 模型 ID | 用途 | 語言 | 大小 | 授權 | 商業用途 |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (推薦) | 辨識 (離線, 含標點) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | 可以 |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (推薦) | 辨識 (串流) | zh, en | 199.1 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.en.2023-06-26.int8` (推薦) | 辨識 (串流) | en | 70.3 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | 辨識 (串流) | zh | 167.4 MB | 未宣告 | 僅限非商業用途 |
| `asr.zipformer-streaming.fr.2023-04-14.int8` (推薦) | 辨識 (串流) | fr | 129.0 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.ru.2025-08-16.int8` (推薦) | 辨識 (串流) | ru | 28.6 MB | Apache-2.0 | 可以 |
| `asr.zipformer-streaming.ko.2024-06-16.int8` | 辨識 (串流) | ko | 140.9 MB | Apache-2.0 | 僅限非商業用途 |
| `asr.nemo-transducer.es.2026-05-03.int8` (推薦) | 辨識 (離線, 含標點) | es | 136.5 MB | CC-BY-4.0 | 可以 |
| `asr.zipformer-streaming.ar-en-id-ja-ru-th-vi-zh.2025-02-10.int8` | 辨識 (串流) | ar, en, id, ja, ru, th, vi, zh | 338.9 MB | Apache-2.0 | 未知 |
| `punct.ct-transformer.zh-en.2024-04-12.int8` (推薦) | 標點 | zh, en | 75.5 MB | Apache-2.0 | 可以 |
| `punct.cnn-bilstm.en.2024-08-06.int8` (推薦) | 標點 | en | 7.6 MB | Apache-2.0 | 可以 |
| `kws.zipformer.zh-en.2025-12-20` (推薦) | 喚醒詞 | zh, en | 8.8 MB | Apache-2.0 | 可以 |
| `tts.vits.melo.zh-en` (推薦) | 合成 (說話者: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | 可以 |
| `tts.vits.icefall.zh-aishell3` (推薦) | 合成 (說話者: 174, 8 kHz) | zh | 32.8 MB | 未宣告 | 未知 |
| `tts.kokoro.multi-lang.v1_1` | 合成 (說話者: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | 可以 |
| `tts.matcha.icefall.zh-baker` | 合成 (說話者: 1, 22.05 kHz) | zh | 81.0 MB | 未宣告, MIT (聲碼器) | 僅限非商業用途 |
| `tts.vits.piper.en-us-lessac-medium` | 合成 (說話者: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 僅限非商業用途 |
| `tts.vits.piper.fr-fr-siwis-medium.int8` | 合成 (說話者: 1, 22.05 kHz) | fr | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 僅限非商業用途 |
| `tts.vits.piper.es-es-davefx-medium.int8` | 合成 (說話者: 1, 22.05 kHz) | es | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 僅限非商業用途 |
| `tts.vits.piper.de-de-thorsten-medium.int8` | 合成 (說話者: 1, 22.05 kHz) | de | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 僅限非商業用途 |
| `tts.vits.piper.ru-ru-denis-medium.int8` | 合成 (說話者: 1, 22.05 kHz) | ru | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 僅限非商業用途 |
| `tts.kokoro.multi-lang.v1_0.int8` | 合成 (說話者: 49, 24 kHz) | en, es, fr, hi, it, pt, zh | 168.9 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | 可以 |
| `tts.vits.piper.ar-jo-sa-dii-high.int8` | 合成 (說話者: 1, 22.05 kHz) | ar | 36.6 MB | CC-BY-NC-SA-4.0, GPL-3.0-or-later (espeak-ng-data) | 僅限非商業用途 |
| `tts.vits.piper.ar-jo-sa-miro-v2-high.int8` | 合成 (說話者: 1, 22.05 kHz) | ar | 36.6 MB | CC-BY-NC-ND-4.0, GPL-3.0-or-later (espeak-ng-data) | 僅限非商業用途 |
| `tts.vits.mimic3.ko-ko-kss-low` | 合成 (說話者: 1, 22.05 kHz) | ko | 80.8 MB | CC0-1.0, GPL-3.0-or-later (espeak-ng-data) | 僅限非商業用途 |
| `speaker.eres2net.3dspeaker.en-voxceleb` (推薦) | 說話者辨識 | en, zh | 26.5 MB | Apache-2.0 | 可以 |
| `segment.pyannote.3-0` (推薦) | 說話者分割 | 不限 | 6.0 MB | MIT | 僅限非商業用途 |
| `langid.whisper.tiny.int8` (推薦) | 語言辨識 | 99 種語言 | 102.8 MB | MIT | 未知 |
| `langid.whisper.base.int8` | 語言辨識 | 99 種語言 | 159.8 MB | MIT | 未知 |
| `tagger.ced.mini.int8` (推薦) | 音訊標籤 | 不限 | 10.5 MB | CC-BY-4.0, CC-BY-SA-4.0 (AudioSet labels) | 可以 |

僅限非商業用途或授權未知的模型在下載前會提示授權, 指令碼下載時需要明確傳入 `acceptLicense: true`. 中文串流 2025 模型的權重未宣告授權, 訓練資料含僅限非商業用途的語料, 因此只從 sherpa-onnx 的上游位址下載, 不經共享資源儲存庫鏡像. Kokoro 與各個 Piper 合成模型附帶 eSpeak NG 的發音資料 (espeak-ng-data, GPL-3.0-or-later). 韓語串流模型的訓練資料 (KsponSpeech) 只開放韓國國民申請, 在韓國境外使用需要另簽協議, 因此按僅限非商業用途標示, 也不經共享資源儲存庫鏡像, 只從上游位址下載 (下載約 418 MB, 安裝後 141 MB). 法語, 西班牙語, 德語與俄語四個 Piper 聲音都由英文 lessac 聲音微調而來, 與它一樣僅限非商業用途. 八語串流模型 (PengChengStarling) 的權重為 Apache-2.0, 但發布者沒有列出訓練資料, 因此商業用途標示為未知. 阿拉伯語 dii 與 miro 聲音都以 CC BY-NC-ND 4.0 的合成語音訓練, dii 本身為 CC BY-NC-SA 4.0, miro 為 CC BY-NC-ND 4.0; miro 不允許改作, 因此只從上游位址下載, 不經共享資源儲存庫鏡像. 韓語 KSS 聲音的權重為 CC0, 但訓練資料 (KSS) 為 CC BY-NC-SA 4.0, 因此按僅限非商業用途標示. 說話者分割模型 (pyannote segmentation 3.0) 的權重為 MIT, 但訓練資料中的 MSDWild 僅限研究用途, 因此按僅限非商業用途標示. 兩個 Whisper 語言辨識模型的權重為 MIT, 但 OpenAI 沒有公開訓練資料, 因此商業用途標示為未知. CED-mini 音訊標籤模型的權重為 CC BY 4.0, 隨附的 AudioSet 類別表為 CC BY-SA 4.0.

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

- `speech.listen(options)` 開啟聽寫工作階段, 透過 `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` 與 `end` 事件傳回結果, `stop()` 提前結束; `speech.recognize(path)` 辨識音訊檔案 (WAV 以外的格式需要 AutoJs6 6.8.0 以上; 也接受 16 kHz PCM), `speech.detectVoice(path)` 列出其中的語音段.
- `speech.listen` 與 `speech.recognize` 的 `punctuation` 選項預設為 true: 模型自帶標點 (SenseVoice) 時保留, 串流模型的結果由已安裝的標點模型補上標點 (中文與英文, 英文同時恢復大小寫), 沒有涵蓋該語言的標點模型時保持原樣; 設為 false 則去掉所有標點. 標點模型只在外掛首頁管理, 不會出現在 `speech.models` 中.
- `speech.wake(keywords, options)` 開啟喚醒詞工作階段, 關鍵字直接寫原文 (最多 50 個); `thenListen` 讓每次喚醒後在同一段錄音上轉入一輪聽寫, 該輪的工作階段由 `wake` 事件的 `listen` 屬性提供.
- `speech.synthesize(text, path, options)` 合成到 WAV 檔案 (最多 8000 個字元, 可選模型, 說話者與語速); `speech.speak(text, options)` 與 `tts(text, { engine })` 經系統 TTS 引擎朗讀.
- `speech.models` 列出, 下載 (`install`, 可回報進度), 匯入, 刪除模型與設定預設模型; `speech.capabilities()` 傳回外掛版本, 支援的功能與麥克風權限狀態.
- `speech.speaker.enroll(name, files, { replace })` 註冊說話者 (多段錄音取平均; 同名預設追加到原有聲紋, `replace: true` 重新註冊), `verify(name, file)` 回傳分數與是否相符, `identify(file, { topK })` 在已註冊的人中找出最像的一位並列出候選, `list()` 與 `remove(name)` 管理已註冊的人; `enroll`, `verify` 與 `identify` 各有回傳 Promise 的 `...Async` 版本. 結果只有名字, 分數, 是否相符與閾值 (預設 0.50, 可用 `threshold` 調整), 不含聲紋向量. 需要 AutoJs6 6.8.0 正式版與說話者辨識模型.
- `speech.diarize(file, options)` 分離說話者, 傳回按時間排列的輪次 (`segments`, 每項有 `startMillis`, `endMillis` 與從 0 編號的 `speaker`) 與人數 (`speakers`); 選項 `speakers` 指定人數 (0 為自動判斷, 最多 20), `identify: true` 用已註冊的聲紋為說話者認出名字 (`names`). `speech.detectLanguage(file, { windows })` 逐句判斷語音前幾個視窗 (每個 30 秒, 預設 3 個) 的語言: `language` 是說話時長最長的語言, `candidates` 是各語言的時長占比, `segments` 是每句的語言. `speech.tag(file, { topK, timeline })` 按 10 秒的視窗評分, 傳回得分最高的標籤 (AudioSet 的 527 個英文標籤, 預設 5 個), `timeline: true` 附上每個視窗的標籤. 三者都可以用 `onProgress(done, total)` 回報進度, 各有傳回 Promise 的 `...Async` 版本; 需要 AutoJs6 6.8.0 正式版與對應的模型.
- 整個外掛同一時間只有一個麥克風工作階段 (聽寫或喚醒); 缺少麥克風權限時呼叫以 `PERMISSION_DENIED` 失敗, 開啟外掛首頁授予即可. 辨識出的文字不會寫入 AutoJs6 的記錄.
- AutoJs6 6.8.0 及以上, 指令碼也可以透過 `plugins.tools.call` 呼叫這些外掛工具, 例如 `plugins.tools.call('speech_speak', { text: '你好' })`; 參數與 MCP Server 看到的相同. 日常指令碼直接用 `speech` 模組更靈活.
- 完整的方法, 選項, 事件與錯誤碼見 [AutoJs6 文件](https://docs.autojs6.com/#/speech); AutoJs6 的示例程式碼 > 3-Macaw Speech 中有聽寫到檔案, 喚醒詞與聽寫, TTS 播報, 語音指令交給 Agent, 說話者辨識, 說話者分離, 語言辨識與音訊標籤八個範例.

******

### 相容性

******

- Android 7.0 (API 24) 以上, 目標 API 37. arm64-v8a, armeabi-v7a, x86_64 與 x86 各有安裝套件, 另有 universal 安裝套件; 原生程式庫按 16 KB 記憶體分頁對齊, 已在 16 KB 分頁模擬器上驗證.
- AutoJs6: 指令碼中的 `speech` 模組需要 6.8.0 (內部版本號 5456) 或更高版本; 外掛介面, 系統 TTS 引擎, 系統語音輸入, 語音辨識服務與數位助理不依賴 AutoJs6. 辨識非 WAV 音訊檔案 (MP3, M4A 等) 與外掛工具需要 AutoJs6 6.8.0 正式版或更新版本, 較早的 AutoJs6 中指令碼只能辨識 WAV. 指令碼的 `speech.speaker`, `speech.diarize`, `speech.detectLanguage` 與 `speech.tag` 同樣需要 AutoJs6 6.8.0 正式版或更新版本; 外掛內的 "說話者" 頁面, "只聽主人" 與試聽試說頁的檔案分析不依賴 AutoJs6.
- 背景收音: 在 AutoJs6 位於前景時開始的聽寫或喚醒工作階段, AutoJs6 退到背景或螢幕關閉後仍會繼續收音 (外掛以麥克風前景服務與通知保持錄音). AutoJs6 已在背景時才開啟的工作階段可能被系統靜音, 此時工作階段收到錯誤碼為 `PERMISSION_DENIED` 的 `warning` 事件但不會結束, AutoJs6 回到前景後恢復; 需要背景收音的指令碼請在 AutoJs6 可見時開啟工作階段.
- 實測裝置: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 24, 35 與 37 (16 KB 分頁) 模擬器.
- 效能: 在低階的 Sony G8441 上, 串流模型載入約 1.7 秒, SenseVoice 約 2.8 秒, 聽寫處理速度約為即時的 3 倍; MeloTTS 與 Kokoro 在這類裝置上合成慢於即時, AISHELL-3 最快. Sony XQ-DQ72 等較新裝置載入約 0.5 到 1.2 秒, 合成快於即時.
- 記憶體: 一個辨識模型載入後約佔 210 到 300 MB 原生記憶體. 外掛按裝置記憶體的四分之一 (384 MB 到 1.5 GB) 管理已載入的模型, 閒置 5 分鐘後釋放 (可在設定中調整為 1 到 30 分鐘).
- 數位助理: 需要系統允許選擇第三方數位助理應用程式. 喚起方式依系統而定: 長按 Home 鍵 (三按鈕導覽), 從螢幕底部角落斜向上滑, 或在系統設定中把長按電源鍵設為喚起數位助理. 實測的 ColorOS 與 HyperOS 平板 (Android 15) 把電源鍵與手勢留給自家助理, 可以改用觸發式入口.
- 觸發式入口: 實測 Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), Xiaomi Pad 23046RP50C (HyperOS, Android 15), 以及 API 33 與 35 模擬器. ColorOS 與 HyperOS 在螢幕鎖定時點圖塊會先要求解鎖, 解鎖後開啟面板; 部分主畫面接受加入小工具的請求卻不顯示確認, 此時依設定中的步驟手動加入. 螢幕關閉時手機不把按鍵交給無障礙服務, 按鍵觸發只在螢幕開啟時可用. 藍牙耳機語音鍵: Sony WF-1000XM4 配合 XQ-DQ72 (Android 13) 實測每次長按都開啟面板, 並用耳機麥克風收音; 其他應用程式 (例如 Google 應用程式) 也回應時系統先顯示選擇器. 實測 HyperOS 平板 (Xiaomi Pad 23046RP50C, Android 15) 的選擇器在選中 3-Macaw Speech 後沒有反應.
- 常駐喚醒: 實測 Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 35 (16 KB 分頁) 模擬器. 螢幕關閉 30 分鐘耗電 (Sony XQ-DQ72, Android 13; Android 依機型功耗表估算, 手機經 USB 連接, 未充電狀態為模擬): 開啟且省電模式開 (預設) 時 17.1 mAh, 外掛程式 CPU 時間 235 秒; 關掉省電模式時 17.9 mAh, 454 秒 (平均約佔一個 CPU 核心的四分之一); 兩種情況都一直在錄音, 估算中約 14 mAh 算在錄音本身上. 那次測試時周圍一直有聲音, 省電模式減少的主要是 CPU; 在安靜的房間裡 (Sony G8441, Android 9, 螢幕關閉 10 分鐘) 外掛程式 CPU 時間從 265 秒降到 26 秒. 關閉常駐喚醒時 0.006 mAh, 外掛程式沒有在執行. 實際耗電因機型與周圍的聲音而異.
- 外掛工具: 實測 Sony G8441 (Android 9), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 24 與 35 模擬器 (API 24 只測不需要模型的部分); 經 MCP Server 呼叫實測 Sony G8441 (Android 9).
- 標點與 1.2.0 新增模型: 實測 Sony XQ-AT72 (Android 12). 開啟標點時, 中文, 英文與中英混合錄音都加上了標點, 字詞與關閉時相同; 俄語與法語 Piper 聲音合成的句子由同語言的串流模型辨識回來, 字錯率分別為 7.9% 與 0%; 西班牙語與德語聲音合成正常, 韓語模型只驗證了可以載入 (沒有韓語測試錄音).
- 1.3.0 新增模型: 實測 Sony G8441 (Android 9), XQ-DQ72 (Android 13) 與 API 35 模擬器 (安裝, 使用與刪除). 測試錄音裡沒有西班牙語, 阿拉伯語與韓語, 因此用合成的句子由辨識模型讀回: Kokoro v1.0 的西班牙語聲音由 NeMo 讀回, 字錯率 0% 到 6.5%; 兩個阿拉伯語聲音由八語模型讀回, 均為 3.0% (模擬器上 miro 為 33%); 韓語聲音由 SenseVoice 讀回, 0%; 八語模型辨識英文錄音的字錯率為 0% 到 1.1%. 八語模型載入後峰值記憶體約 600 MiB (G8441) 到 880 MiB (XQ-DQ72); Kokoro v1.0 在 G8441 上合成慢於即時 (約為音訊長度的 4 到 6 倍), 在 XQ-DQ72 上接近即時.
- 試聽試說頁辨識檔案: 實測 Sony G8441 (Android 9), XQ-AT72 (Android 12), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 24 與 35 模擬器. 在 OnePlus OPD2413 上, 一段 10 分 03 秒的 MP3 約 21 秒辨識完 (SenseVoice, 開啟自動標點); 離開頁面後辨識照常進行, 通知中顯示進度.
- 說話者辨識: 實測 Sony XQ-AT72 (Android 12), XQ-DQ72 (Android 13), 以及 API 24 與 35 (16 KB 頁) 模擬器 (註冊, 驗證, 辨識, 列出與刪除, 以及 "只聽主人" 的放行與拒絕; 使用 LibriSpeech 的兩位朗讀者與合成的喚醒詞). 同一人的 2 秒片段得分 0.75 到 0.79, 另一人低於 0.08, 預設閾值 0.50; 在電腦上以 40 位英語與 12 位中文說話者測得, 2 秒片段誤拒約 2.5% (英語) 與 0% (中文), 誤收約 0.05% 與 0.9%. "只聽主人" 的比對在 Sony 手機上約 0.15 到 0.25 秒.
- 音訊分析: 實測 Sony G8441 (Android 9), OnePlus OPD2413 (ColorOS, Android 15), 以及 API 24 與 35 (16 KB 頁) 模擬器 (分離與認人, 語言, 標籤, 取消, 檔案頁, 三個工具與 AutoJs6 指令碼). 兩位 LibriSpeech 朗讀者輪流朗讀的 42.5 秒錄音在各機上都分出 2 人 7 段, 按時長計 93% 的語音歸到正確的人; 已註冊的一位被認出 (分數 0.85), 另一位沒有 (0.01). 中文, 俄語與英文錄音的語言都判斷正確, 音樂, 白噪音與提示音都標在對應的 10 秒視窗. 10 分鐘錄音在 OnePlus OPD2413 上分離約 70 秒, 辨識語言 2.5 秒, 加標籤 3.8 秒; 在 Sony G8441 上分別為 222 秒 (峰值記憶體約 470 MiB), 12 秒與 15 秒.

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

#### 常駐喚醒為什麼讓麥克風指示燈一直亮, 耗電多少?

持續監聽喚醒詞需要一直錄音: Android 12 起只要有應用程式在錄音, 狀態列就會一直顯示麥克風指示燈 (綠點), 應用程式無法隱藏; 系統裡低功耗, 不亮指示燈的熱詞通道只開放給預先安裝的系統助理, 第三方應用程式即使被選為預設數位助理也用不了. 所以常駐喚醒預設關閉, 開啟後一直顯示綠點與一則常駐通知 (可在通知中直接關閉). 實測 Sony XQ-DQ72 螢幕關閉 30 分鐘, 開啟時 Android 估算本外掛程式耗電約 17 mAh (約為電池容量的 0.4%), 關閉時幾乎為 0. 估算的大部分算在錄音本身上: 預設開啟的省電模式 (只在聽到說話聲或其他聲音時執行喚醒詞模型) 讓外掛程式的 CPU 時間少一半 (周圍一直有聲音時) 到九成 (安靜時), 估算耗電只少約 1 mAh. 聲音很輕的喚醒詞在省電模式下可能聽不到, 這時可在設定中關掉省電模式. 建議只在需要時開啟: 把監聽時長設為 10, 30 或 60 分鐘, 或開啟只在充電時監聽 (拔掉電源時暫停, 接上後繼續); 不常用時, 圖塊, 小工具, 懸浮麥克風等觸發式入口平時不錄音.

#### 為什麼聽到喚醒詞後只出現 "點此說話" 通知?

Android 10 起, 背景應用程式只有在是預設數位助理, 或者取得了在其他應用程式上層顯示的權限時, 才能直接開啟畫面. 3-Macaw Speech 不是預設數位助理時, 開啟常駐喚醒會詢問一次這項權限; 不授予時, 聽到喚醒詞後發出一則 "點此說話" 通知, 點按即開啟面板, 1 分鐘後自動撤銷. 想讓喚醒詞直接開啟面板, 可以把 3-Macaw Speech 設為預設數位助理, 或在系統設定中允許它在其他應用程式上層顯示.

#### 為什麼開啟常駐喚醒時提示需要先關閉 3-Stove Agent?

3-Macaw Speech, 3-Stone AI 與 3-Stove Agent 的常駐監聽互斥: 一支手機同一時間只能有一個應用程式常駐監聽, 否則它們會爭用麥克風. 其中一個開著時, 另一個的開關會說明是誰在監聽, 並可直接開啟對方的設定去關閉. 重新開機或更新後, 常駐喚醒不會自動開始錄音: 開關仍開著時會發出一則 "點此恢復常駐喚醒" 通知, 點按或開啟 3-Macaw Speech 的首頁, 設定頁都會恢復; 監聽時長在此期間已到則直接關閉.

#### 怎樣讓常駐喚醒只回應自己的聲音?

開啟 3-Macaw Speech 的設定, 在常駐喚醒組中點按 "說話者", 依提示下載說話者辨識模型 (約 26.5 MB), 再點按 "新增說話者", 輸入名字後跟讀畫面上的 3 句話, 每句約 4 秒; 家人也可以各自註冊. 然後開啟 "只聽主人": 聽到喚醒詞時, 外掛取喚醒詞前約 2 秒的錄音與已註冊的聲紋比對, 任一位相符 (分數達到 0.50) 就開啟面板, 否則當作沒聽到 (不開啟面板, 也不發通知). 在 "說話者" 頁面點按 "試驗證" 可以查看自己的分數. 說話聲太小, 周圍很吵或換了麥克風 (例如耳機) 時可能被誤拒, 這時可以在相同的環境下追加錄音. 這項功能用來避免身邊的人誤觸發, 不是安全驗證: 播放已註冊者的錄音也可能通過.

#### AI 助理怎樣透過 3-Macaw Speech 聽和說?

需要 AutoJs6 6.8.0 及以上與 MCP Server 外掛. 3-Macaw Speech 是官方外掛, `speech_listen`, `speech_speak` 與 `speech_transcribe` 預設出現在 MCP Server 的工具清單中 (可在 MCP Server 設定的 "外掛提供的工具" 中關閉); `speech_synthesize` 會寫入檔案, 還要開啟該群組的第二個開關, 未指定位置時檔案儲存在 AutoJs6 工作目錄的 plugin-outputs/speech 下. `speech_listen` 每次聽一句話 (最長 60 秒): 能直接錄音時 (例如開著常駐喚醒, 3-Macaw Speech 是預設數位助理或已取得在其他應用程式上層顯示的權限) 立即開始, 否則手機上出現一則 "點此說話" 通知, 1 分鐘內點按後開始. 聽寫期間其他應用程式的常駐監聽暫時讓出, 聽完自動恢復; 已有聽寫在進行時傳回 BUSY, 缺少麥克風權限或辨識模型時傳回 NOT_READY (開啟外掛首頁處理即可). `speech_speak` 有該語言的 3-Macaw Speech 語音時用它朗讀, 否則用系統語音.

#### 不長按電源鍵, 還有哪些方式開啟聽寫面板?

開啟 3-Macaw Speech 的設定, 在觸發方式組中設定: 快速設定圖塊, 點按加入 (Android 13 起由系統確認, 更早的版本依提示手動加入), 之後下拉快速設定點語音指令; 主畫面小工具與捷徑, 把按一下說話小工具放到主畫面, 或長按應用程式圖示選擇語音指令; 懸浮麥克風, 開啟開關並授予在其他應用程式上層顯示的權限, 螢幕邊緣會出現可拖曳的麥克風按鈕; 按鍵觸發, 在系統無障礙設定中開啟 3-Macaw Speech 按鍵觸發, 預設長按音量下鍵約 0.8 秒, 可改為長按音量上鍵或連按兩下音量下鍵. 這些入口都開啟與數位助理相同的面板, 說完後選擇接收文字的應用程式; 3-Macaw Speech 是預設數位助理時經助理工作階段開啟, 不是時直接開啟同款面板.

#### 按鍵觸發為什麼需要無障礙服務, 安全嗎?

Android 只允許無障礙服務在系統處理之前取得實體按鍵, 所以按鍵觸發是一個無障礙服務. 它只申請過濾按鍵, 不訂閱任何介面事件, 不能讀取螢幕內容, 也不能執行手勢; 只攔截所選的音量鍵, 短按照常調整音量, 螢幕關閉, 來電或通話時不攔截. 開關是灰色時 (Android 13 起透過安裝檔安裝的應用程式), 先開啟 3-Macaw Speech 的應用程式資訊頁, 在選單中選擇允許受限制的設定.

#### 藍牙耳機的語音鍵能開啟聽寫面板嗎?

可以. 3-Macaw Speech 回應藍牙服務發出的語音指令請求 (VOICE_COMMAND), 長按耳機的語音鍵或觸控區即開啟聽寫面板. Android 12 起, 面板第一次會詢問鄰近裝置權限. 允許後 3-Macaw Speech 接管耳機的語音辨識: 用耳機麥克風收音, 每次按鍵都有效. 不允許時用手機麥克風收音, 部分耳機 (例如 Sony WF-1000XM4) 會隔一次按鍵才開啟面板. 手機上還有其他應用程式回應語音指令 (Google 應用程式, 廠商助理) 時, 系統會先讓你選擇; 實測 HyperOS 的選擇器在選中 3-Macaw Speech 後沒有反應, 這是系統選擇器的問題. 不想使用時, 在設定的觸發方式群組中關閉耳機語音鍵.

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

- 麥克風: 只在聽寫, 喚醒詞, 試聽試說與開啟常駐喚醒期間錄音, 工作階段結束或指令碼結束後立即釋放麥克風.
- 網路: 只用於下載模型與重新整理已簽署的模型目錄, 辨識與合成不連網.
- 儲存空間: 模型儲存在外掛私有目錄; 匯入本機模型與在試聽試說頁辨識檔案都透過系統檔案選擇器選取, 辨識結果也由你選擇位置儲存, 因此不申請儲存權限. 最近一次辨識結果保留在外掛快取中, 可在頁面上清除.
- 受保護的入口: 能力服務與啟用入口均受 AutoJs6 外掛權限 (`org.autojs.permission.PLUGIN`) 保護, 第三方應用程式無法直接呼叫; 系統 TTS 引擎及其語音檢查, 範例文字與設定入口依 Android 的慣例對所有應用程式開放, 只提供語音合成.
- 系統語音輸入: 與系統 TTS 引擎一樣向所有應用程式開放; 只在面板顯示時錄音, 只傳回辨識出的文字, 可在設定中關閉.
- 數位助理: 不讀取螢幕內容, 未開啟常駐喚醒時不在背景收音; 只在面板顯示時錄音, 辨識出的文字只在你點按應用程式按鈕或複製時離開面板 (鎖定畫面時需先解鎖). 語音辨識服務只為本身擁有麥克風權限的應用程式辨識.
- 觸發式入口: 圖塊, 小工具, 捷徑, 懸浮麥克風, 按鍵觸發與耳機語音鍵本身都不錄音, 只負責開啟聽寫面板. 按鍵觸發是只過濾按鍵的無障礙服務: 不讀取螢幕內容, 不執行手勢, 只攔截所選的音量鍵 (短按照常調整音量, 螢幕關閉, 來電或通話時不攔截). 懸浮麥克風需要在其他應用程式上層顯示的權限, 以一則靜音通知保持顯示; 開機自動啟動只用於在重新啟動後恢復已開啟的懸浮麥克風. Android 12 起耳機語音鍵需要鄰近裝置權限, 只在耳機開啟的面板中詢問: 只用於讓藍牙服務把語音辨識交給面板並接通耳機麥克風, 不掃描裝置, 不讀取位置.
- 常駐喚醒: 預設關閉, 只能在外掛介面中開啟 (重新開機或更新後需要點按通知恢復, 不會自動錄音). 喚醒詞在本機偵測, 音訊不儲存也不上傳, 辨識出的文字不寫入記錄; 開啟期間一直錄音, 麥克風指示燈常亮, 常駐通知可直接關閉. 與 3-Stone AI, 3-Stove Agent 的常駐監聽互斥, 系統同一時間只允許一個.
- 聲紋: 外掛不儲存註冊用的錄音, 只儲存由它算出的聲紋; 聲紋只存在外掛私有目錄, 不上傳, 不能匯出也不能匯入, 指令碼與外掛工具只拿到名字, 分數與是否相符. 在 "說話者" 頁面可逐一刪除; 清除 3-Macaw Speech 的資料或解除安裝會刪除全部聲紋. "只聽主人" 比對的喚醒詞前約 2 秒錄音只在記憶體中處理, 不儲存.
- 外掛工具: 工具服務同樣受 AutoJs6 外掛權限保護, 只有 AutoJs6 能直接呼叫. `speech_listen` 每次只錄一句話, 錄音期間有通知 (Android 12 起還有麥克風指示燈); `speech_transcribe` 只讀取 AutoJs6 交來的那一個檔案, `speech_synthesize` 把音訊交回 AutoJs6 寫到指定位置. 辨識與朗讀的文字都不寫入記錄. 三個分析工具同樣只讀取 AutoJs6 交來的那一個檔案, `speech_diarize` 認人時只傳回名字與分數, 不含聲紋.

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
tool service: org.autojs.plugin.TOOL_PROVIDER (short name speech: listen, speak, transcribe, synthesize, speaker_verify, speaker_identify, diarize, detect_language, tag; AutoJs6 6.8.0+)
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### 致謝

******

3-Macaw Speech 建立在以下開源專案, 模型與資料之上, 感謝它們的作者與維護者:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) 與 [k2-fsa (新一代 Kaldi)](https://github.com/k2-fsa) 團隊: 辨識, 喚醒詞, 語音活動偵測與合成的執行階段, 各模型的 ONNX 匯出與 Android 程式庫 (Apache-2.0).
- [icefall](https://github.com/k2-fsa/icefall) 訓練的模型: Zipformer 串流辨識模型 (中英雙語 pfluo, 英文 Zengwei, 中文 2025 yuekai, 法語 shaojieli, 韓語 johnBamma), 開放詞彙喚醒詞模型 (pkufool), 以及 k2-fsa 的 AISHELL-3 VITS 與標貝 Matcha-TTS 合成模型.
- [Vosk](https://huggingface.co/alphacep/vosk-model-small-streaming-ru) (Alpha Cephei): 俄語小型串流辨識模型 (Apache-2.0).
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / 阿里巴巴, FunASR 模型授權 1.1): 多語言離線辨識模型.
- 標點模型: [FunASR](https://github.com/modelscope/FunASR) (阿里巴巴達摩院) 的 CT-Transformer 中英文標點模型, 以及 [Edge-Punct-Casing](https://github.com/frankyoujian/Edge-Punct-Casing) (frankyoujian) 的英文標點與大小寫模型 (皆為 Apache-2.0).
- [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, 中文錄音由 LongMaoData 授權) 與 [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT) 合成模型, [HiFi-GAN](https://github.com/jik876/hifi-gan) 聲碼器 (MIT). 法語 siwis, 西班牙語 davefx, 德語 thorsten 與俄語 denis 聲音來自 [piper-voices](https://huggingface.co/rhasspy/piper-voices) (MIT), 皆由英文 lessac 聲音微調.
- 訓練資料: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), [標貝中文標準女聲音庫](https://www.data-baker.com/open_source.html) (僅限非商業用途), Lessac Technologies 的 Blizzard 2013 資料 (僅限研究) 與 [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (公眾領域). 1.2.0 新增的法語, 韓語與 Piper 模型另用到 [Common Voice](https://commonvoice.mozilla.org/) 12 法語 (CC0), [KsponSpeech](https://aihub.or.kr/aihubdata/data/view.do?currMenu=115&topMenu=100&aihubDataSe=realm&dataSetSn=123) (AI Hub, 只開放韓國國民申請), [SIWIS 法語語音合成資料庫](https://datashare.is.ed.ac.uk/handle/10283/2353) (CC BY 4.0, SIWIS database, University of Edinburgh and partners), [Thorsten-Voice](https://github.com/thorstenMueller/Thorsten-Voice) (CC0) 與 [NabuCasa voice-datasets](https://github.com/NabuCasa/voice-datasets) 的 davefx 和 denis 錄音 (CC0).
- 1.3.0 新增的模型: [NVIDIA NeMo](https://huggingface.co/nvidia/stt_es_fastconformer_hybrid_large_pc) 的西班牙語 FastConformer (CC-BY-4.0; 訓練資料為 Common Voice 12 (CC0), Multilingual LibriSpeech (CC BY 4.0), VoxPopuli (CC0) 與 Fisher Spanish (LDC)), 八語串流模型 [PengChengStarling](https://github.com/yangb05/PengChengStarling) (Apache-2.0), [Kokoro v1.0](https://huggingface.co/hexgrad/Kokoro-82M) (hexgrad, Apache-2.0), OpenVoiceOS 的阿拉伯語 [dii](https://huggingface.co/OpenVoiceOS/phoonnx_ar-SA_dii_espeak) 與 [miro](https://huggingface.co/OpenVoiceOS/phoonnx_ar-SA_miro_espeak_V2) 聲音 (以 [phoonnx](https://github.com/TigreGotico/phoonnx) 用 TigreGotico 的合成語音資料訓練), 以及 Mycroft [Mimic 3](https://github.com/MycroftAI/mimic3-voices) 的韓語 KSS 聲音 (CC0; 訓練資料為 [KSS 資料集](https://www.kaggle.com/datasets/bryanpark/korean-single-speaker-speech-dataset), CC BY-NC-SA 4.0).
- [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT) 推論引擎, [silero VAD](https://github.com/snakers4/silero-vad) (MIT) 語音活動偵測模型, [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) 與 [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT) 音素轉換, [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT) 中文喚醒詞讀音表.

******

### 開發路線圖

******

功能規劃, 設計決策與完成情況以可勾選清單維護在 ROADMAP.md 中. 未勾選的項目表示規劃意向, 不代表目前版本的能力.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.4.0

_2026/10/30_

- `新增` 試聽試說頁的檔案辨識可以播放音訊了: 結果上方有播放列, 能拖動進度, 0.5 倍到 2 倍變速, 前後跳 5 秒. 正在播放的句子會醒目顯示並保持可見, 點一句就從那裡播放, 還可以切換成歌詞式檢視, 以大字顯示目前句子 (說話者, 語言和聲音標籤的結果也一樣). 離開頁面後, 只要檔案還在, 最近一次結果仍能播放: 外掛會保留這一個檔案的讀取權限, 直到你清除結果或換一個檔案.
- `優化` 辨識語言改為逐句判斷: 一段錄音裡說了幾種語言時, 能看到每句話說的是哪種語言 (很短的句子併入相鄰的句子). 試聽試說頁列出每一句, 腳本的 speech.detectLanguage 與工具 speech_detect_language 的 segments 也按句提供, 總的語言取說話時長最長的那種, 候選的分數是時長占比; windows 選項不變. 每句都要判斷一次, 所以比 1.3.0 慢一些

#### v1.3.0

_2026/10/30_

- `提示` 指令碼中的 speech.speaker, speech.diarize, speech.detectLanguage, speech.tag 與五個說話者及分析工具需要 AutoJs6 6.8.0 正式版或更新版本; "說話者" 頁面, "只聽主人" 與試聽試說頁的檔案分析在外掛介面中使用, 不依賴 AutoJs6 的版本. 使用較舊的 AutoJs6 時, 指令碼的 speech.models.list() 會多列出說話者辨識, 說話者分割, 語言辨識與音訊標籤模型 (類型為 speaker, segment, langid 與 tagger), 可以照常安裝或刪除
- `提示` 分析長音訊會用到較多記憶體: 分離一段 10 分鐘錄音的說話者, 實測外掛的記憶體峰值約 470 MB 到 1.3 GB, 所以外掛向系統申請了更大的記憶體上限; 記憶體較小的手機建議先分析較短的檔案
- `新增` 常駐喚醒新增省電模式 (設定 > 常駐喚醒, 預設開啟): 只在聽到說話聲或其他聲音時執行喚醒詞模型, 安靜時不執行, 外掛程式佔用的 CPU 少一半到九成 (周圍越安靜越少); 錄音本身的耗電不變, 所以 Android 估算的耗電只略有減少 (實測見 README). 聲音很輕的喚醒詞可能聽不到, 這時可以關閉省電模式
- `新增` 可下載更多模型: 西班牙語離線辨識 (NeMo, 含標點); 自動判斷語言的八語串流辨識, 涵蓋阿拉伯語, 印尼語, 泰語, 越南語, 日語, 俄語, 中文與英文 (PengChengStarling, 安裝後約 339 MB); Kokoro v1.0 多語言聲音 (英語, 西班牙語, 法語, 印地語, 義大利語, 巴西葡萄牙語與中文共 49 個說話人, 每個說話人念自己的語言); 兩個阿拉伯語聲音與一個韓語聲音. 阿拉伯語與韓語聲音僅限非商業用途, 八語模型的訓練資料未公開, 商業用途未知, 下載前都會提示授權. 新聲音也依語言出現在系統 TTS 引擎中; 系統語音輸入與外掛工具只在其他已安裝模型都不涵蓋某種語言時才自動選用八語模型
- `新增` 說話者辨識與 "只聽主人" (設定 > 常駐喚醒): 在 "說話者" 頁面輸入名字並跟讀 3 句話, 即可註冊自己的聲音 (可以註冊多人, 也可以用試驗證查看分數); 開啟 "只聽主人" 後, 常駐喚醒聽到喚醒詞時先比對聲音, 與已註冊的人相符才開啟面板, 別人說喚醒詞會被忽略. 圖塊, 小工具, 耳機鍵等手動入口不受影響. 需要先下載約 26.5 MB 的說話者辨識模型 (頁面會引導); 聲紋只儲存在本機, 不會上傳或匯出
- `新增` 指令碼可以用 speech.speaker 註冊, 驗證與辨識說話者, 並列出或刪除已註冊的人; 新增外掛工具 speech_speaker_verify (判斷一段錄音是不是某位已註冊的人) 與 speech_speaker_identify (在已註冊的人中找出最像的一位), 結果只有名字與分數, 不含聲紋
- `新增` 試聽試說頁的檔案辨識新增三種分析: 分離說話者 (標出誰在什麼時候說話, 可以用已註冊的聲音認出名字; 選 "分離 + 轉寫" 還會把每個人說的話轉成文字), 辨識語言 (Whisper 的 99 種語言) 與音訊標籤 (從人聲, 音樂, 狗叫等 527 類聲音中找出檔案裡有哪些). 都在手機上離線完成, 離開頁面也會繼續, 通知中顯示進度. 需要先下載對應的模型, 頁面會引導: 說話者分割模型約 6 MB (另需說話者辨識模型), 語言辨識模型約 103 MB, 音訊標籤模型約 10.5 MB. 說話者分割模型的訓練資料含僅限研究用途的語料, 按僅限非商業用途標示; 語言辨識模型的訓練資料未公開, 商業用途未知
- `新增` 指令碼可以用 speech.diarize, speech.detectLanguage 與 speech.tag (以及傳回 Promise 的 ...Async 版本) 做同樣的分析, 並可回報進度; 新增外掛工具 speech_diarize, speech_detect_language 與 speech_tag. 一個檔案最長 30 分鐘 (工具的說話者分離為 10 分鐘); 同一時間只做一項分析, 其餘的排隊等候
- `修復` Android 7.0 上又能下載模型了: 模型目錄與模型檔案都從 GitHub 取得, 其證書現在由 Android 7.0 沒有的根證書 (Let's Encrypt 的 ISRG Root X1) 簽發, 之前每次下載與重新整理目錄都因證書錯誤失敗. 插件現在自帶這張根證書, 與系統自帶的一起使用; Android 7.1 及以上本來就有它, 不受影響
- `修復` 部分手機上, 模型與檔案大小的數字和單位之間會隔著一大段空白 (介面語言為英語等沒有指定地區的語言時); 現在顯示正常

#### v1.2.0

_2026/10/23_

- `提示` 新增常駐喚醒, 預設關閉: 開啟後一直在本機監聽喚醒詞, 麥克風指示燈常亮, 有一則常駐通知, 也更耗電 (關閉螢幕 30 分鐘實測見 README). 建議搭配 "監聽時長" 或 "只在充電時監聽" 使用
- `提示` 指令碼辨識非 WAV 音訊與 4 個外掛工具需要 AutoJs6 6.8.0 正式版或更新版本, 舊版 AutoJs6 的指令碼仍只能辨識 WAV; 常駐喚醒, 新模型, 自動標點與 "檔案" 分頁都在外掛介面中使用, 不依賴 AutoJs6 的版本
- `提示` 新增權限: 喚醒鎖 (只在試聽試說頁辨識檔案時使用, 最長 60 分鐘, 讓關閉螢幕後辨識繼續進行), 媒體處理前景服務 (Android 15 起辨識檔案時使用, 在通知中顯示進度)
- `新增` 常駐喚醒 (設定 > 常駐喚醒): 說出喚醒詞 (預設 "小鹦小鹦" 與 "Hey Macaw", 可修改, 靈敏度分高, 中, 低三段) 即開啟與數位助理相同的聽寫面板; 螢幕關閉或鎖定時也會點亮螢幕, 傳送或複製前先解鎖
- `新增` 由喚醒詞開啟的面板會朗讀辨識出的文字, 有該語言的 3-Macaw Speech 語音時使用它, 否則使用系統語音; 可在設定中關閉
- `新增` 監聽時長 (10, 30, 60 分鐘後自動關閉, 或一直開著) 與只在充電時監聽 (拔掉電源時暫停, 接上後繼續), 與 3-Stove Agent 的同名選項一致
- `新增` 3-Macaw Speech 不是預設數位助理時, 開啟常駐喚醒會詢問一次在其他應用程式上層顯示的權限; 不授予時, 聽到喚醒詞後發出 "點此說話" 通知, 點按即開啟面板
- `新增` 腳本, 其他應用程式或本外掛的其他入口使用麥克風時, 常駐喚醒自動讓出, 對方用完後繼續監聽
- `新增` 與 3-Stone AI 和 3-Stove Agent 的常駐監聽互斥, 系統同一時間只能有一個; 對方開著時會提示先關閉, 並可直接開啟對方的設定
- `新增` 重新開機或更新後不會自動錄音: 開關仍開著時發出一則 "點此恢復常駐喚醒" 通知, 開啟首頁或設定頁也會恢復
- `新增` 辨識音訊檔案 (speech.recognize, speech.detectVoice) 除 WAV 外也支援 MP3, AAC (含 M4A), Ogg (Vorbis, Opus), FLAC, AMR 等常見格式, 以及影片檔案 (MP4, WebM 等) 中的聲音; 檔案上限仍為 200 MB 或 30 分鐘 (需要 AutoJs6 6.8.0 以上, 更早的版本只能傳入 WAV)
- `新增` 4 個外掛工具 (需要 AutoJs6 6.8.0 及以上): speech_listen 聽一句話並傳回文字, speech_speak 朗讀, speech_transcribe 辨識手機上的音訊檔案, speech_synthesize 把文字合成為 WAV 或 M4A 檔案. MCP Server 外掛用它們讓電腦上的 AI 助理透過手機聽和說, 手機上的 3-Stove Agent 可以直接呼叫它們, 指令碼也可以透過 plugins.tools 呼叫; 不能直接錄音時, 聽寫先在手機上發出 "點此說話" 通知, 點按後開始
- `新增` 可下載更多模型: 法語, 俄語與韓語的串流辨識, 以及法語, 西班牙語, 德語與俄語的 Piper 聲音; Piper 聲音與韓語模型僅限非商業用途, 下載前會提示授權, 韓語模型只從上游位址下載
- `新增` 自動標點: 在首頁新的 "標點" 分組中下載標點模型後, 串流模型辨識出的中文與英文會自動加上逗號, 句號與問號, 英文也會恢復大寫; SenseVoice 自帶標點, 不受影響; 指令碼照舊可以傳入 punctuation: false 去掉所有標點
- `新增` 試聽試說頁新增 "檔案" 分頁: 選一個音訊或影片檔案 (MP3, M4A, WAV 等, 不超過 200 MB, 長度不超過 30 分鐘), 不寫指令碼也能辨識出裡面說的話, 依時間分段顯示, 可以複製全文或儲存為文字與 SRT 字幕; 模型, 語言與自動標點在頁面上選; 離開頁面或關閉螢幕後辨識照常進行, 通知中顯示進度並可取消; 最近一次結果下次開啟仍在

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
