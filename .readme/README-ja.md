<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-macaw-speech-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>オフライン音声認識, ウェイクワード検出, 音声区間検出, ニューラル音声合成を提供し, システムの TTS エンジンとして登録可能</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語 (Languages)

******

現在の README.md は次の言語に対応しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ar.md)

******

### 概要

******

3-Macaw Speech は [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) を使い, 音声認識, 音声区間検出, ウェイクワード検出, ニューラル音声合成をデバイス上でオフライン実行します. すべての処理は端末内で行われ, 音声がアップロードされることはありません.

プラグインは個別にインストールする APK で, モデルのダウンロードと管理, お試し, 設定の変更を行う独自の画面を持ちます. AutoJs6 のスクリプトはグローバルモジュール `speech` からこれらの機能を使います. 音声合成は Android システムのテキスト読み上げエンジンとしても登録され, 音声認識はシステムの音声入力, システムの音声認識サービス, デジタルアシスタントとしても提供されるため, 他のアプリからも使えます. 認識と合成のモデルは APK に含まれず必要に応じてダウンロードし, 音声区間検出モデルだけが同梱されています.

> スクリプトで `speech` モジュールを使うには AutoJs6 6.8.0 (内部バージョン 5456) 以降が必要です. システムのテキスト読み上げエンジン, システムの音声入力, 音声認識サービス, デジタルアシスタントとプラグイン自身の画面は AutoJs6 に依存せず, プラグインをインストールしてモデルをダウンロードすれば使えます.

******

### 機能ハイライト

******

- 音声認識: マイクからのストリーミング書き起こし (途中結果と確定結果をリアルタイムに返す) と WAV ファイルの認識. 中国語, 英語, 中英混在, 日本語, 韓国語, 広東語のオフラインモデルを提供します.
- 音声区間検出: 内蔵の silero VAD v5 が音声を発話区間に分割し, 書き起こし時の自動的な文の区切りにも使われます.
- ウェイクワード: キーワードをそのまま入力するだけ (例: `小六小六` や `hey macaw`) でモデルの学習は不要です. 起動後は自動的に書き起こしへ移行できます.
- 音声合成: 複数の中国語と英語のニューラル音声. WAV ファイルへ合成できるほか, システム TTS エンジンとして AutoJs6 の `tts` モジュールや他のアプリから使えます.
- システムの音声入力: 他のアプリ (3-Stove Agent など) のマイクボタンでオフライン認識を使えます. 画面下部のパネルで一文を話すと, 認識されたテキストがそのアプリに戻ります.
- デジタルアシスタント: 3-Macaw Speech をデフォルトのデジタルアシスタントアプリに選ぶと, 電源ボタンまたはホームボタンの長押しで一文を話し, 必要なら認識されたテキストを編集して 3-Stove Agent や 3-Stone AI など音声コマンドを受け取るアプリに渡すか, コピーできます. システムの音声認識サービスも提供します. Android 11 以前ではアシスタントに選ぶと通常はデフォルトの音声認識サービスにもなり, Android 12 以降はシステムの音声入力設定で別途選びます (この設定がない端末もあります). アプリが直接指定することもできます.
- モデル管理: 署名付きモデルカタログからダウンロードし, ファイルごとに SHA-256 を検証してから有効化します. ローカルモデルのインポートにも対応します. 非商用に限られるモデルはダウンロード前にライセンスを表示します.
- 独立した画面: ホーム画面で状態を表示してモデルを管理し, お試し画面で書き起こし, ウェイクワード, 合成を直接試し, 設定で言語, ナイトモード, テーマカラー, ランチャーアイコン, 認識と合成のデフォルトを変更します. 外観はデフォルトで AutoJs6 に従います.

******

### スクリーンショット

******

スクリーンショットは Sony XQ-DQ72 (Android 13) で撮影しました. 書き起こしのテキストは, sherpa-onnx のテスト録音をスマートフォンのスピーカーで再生してプラグインが認識したもので, 個人情報は含まれていません.

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/home.png?raw=true" alt="ホーム: 状態と用途別のモデル" width="360" />
      <br />
      <sub>ホーム: 状態と用途別のモデル</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/tryout-dictation.png?raw=true" alt="お試し: リアルタイムの書き起こし" width="360" />
      <br />
      <sub>お試し: リアルタイムの書き起こし</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/voice-input.png?raw=true" alt="システムの音声入力: 他のアプリが開いた下部パネル" width="360" />
      <br />
      <sub>システムの音声入力: 他のアプリが開いた下部パネル</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/assistant.png?raw=true" alt="デジタルアシスタント: 結果を編集して渡すアプリを選ぶ" width="360" />
      <br />
      <sub>デジタルアシスタント: 結果を編集して渡すアプリを選ぶ</sub>
    </td>
  </tr>
</table>

******

### インストールと使い方

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) からデバイスの ABI に合った APK をダウンロードしてインストールします. ほとんどのスマートフォンは `arm64-v8a` です. 不明な場合は, すべてのデバイスで動作するもののサイズが大きい `universal` を選びます.
2. 3-Macaw Speech を開き, ホーム画面でマイクの権限を許可して, 認識, ウェイクワード, 合成の各グループで必要なモデルをダウンロードします (カタログの推奨モデルから始めるのがおすすめです. 下記のモデル表を参照). Android 13 以降では最初のダウンロード時にダウンロード進捗の通知の表示も求められます.
3. AutoJs6 で使う: AutoJs6 を 6.8.0 (内部バージョン 5456) 以降に更新し, プラグインセンターで 3-Macaw Speech を有効にして, スクリプトから `speech` モジュールを呼び出します (下記のスクリプト API を参照). AutoJs6 のサンプル > 3-Macaw Speech にすぐに実行できる 4 つのサンプルがあります.
4. 他のアプリで使う: システムのテキスト読み上げエンジンは下記のシステムのテキスト読み上げ設定を, システムの音声入力, 音声認識サービス, デジタルアシスタントはよくある質問を参照してください.

> ABI ごとにパッケージがあり, すべてのデバイス向けの `universal` パッケージもあります. パッケージには認識と合成のモデルは含まれません. モデルはプラグインの画面やスクリプトから必要に応じてダウンロードするか, ローカルファイルからインポートします.

******

### モデル

******

認識, ウェイクワード, 合成のモデルはそれぞれ 9 MB から 410 MB あるため, APK には含まれません. プラグインは署名付きのモデルカタログを内蔵し, AutoJs6 共有アセットリポジトリの変更不可のリリースからモデルをダウンロードします. ミラーを使えない場合は sherpa-onnx の上流アドレスに切り替え, どちらの経路でもすべてのファイルが SHA-256 の検証に合格してからモデルを有効にします. 1.0.0 のカタログには次のモデルがあります (サイズはインストール後の容量です):

| モデル ID | 用途 | 言語 | サイズ | ライセンス | 商用利用 |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (推奨) | 認識 (オフライン, 句読点あり) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | 可 |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (推奨) | 認識 (ストリーミング) | zh, en | 199.1 MB | Apache-2.0 | 可 |
| `asr.zipformer-streaming.en.2023-06-26.int8` (推奨) | 認識 (ストリーミング) | en | 70.3 MB | Apache-2.0 | 可 |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | 認識 (ストリーミング) | zh | 167.4 MB | 宣言なし | 非商用のみ |
| `kws.zipformer.zh-en.2025-12-20` (推奨) | ウェイクワード | zh, en | 8.8 MB | Apache-2.0 | 可 |
| `tts.vits.melo.zh-en` (推奨) | 合成 (話者: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | 可 |
| `tts.vits.icefall.zh-aishell3` (推奨) | 合成 (話者: 174, 8 kHz) | zh | 32.8 MB | 宣言なし | 不明 |
| `tts.kokoro.multi-lang.v1_1` | 合成 (話者: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | 可 |
| `tts.matcha.icefall.zh-baker` | 合成 (話者: 1, 22.05 kHz) | zh | 81.0 MB | 宣言なし, MIT (ボコーダー) | 非商用のみ |
| `tts.vits.piper.en-us-lessac-medium` | 合成 (話者: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 非商用のみ |

非商用に限られるモデルやライセンスが不明なモデルは, ダウンロード前にライセンスを表示します. スクリプトからダウンロードするときは `acceptLicense: true` を明示的に渡す必要があります. 中国語ストリーミング 2025 モデルは重みにライセンスの宣言がなく, 学習データに非商用限定のコーパスを含むため, sherpa-onnx の上流アドレスからのみダウンロードし, 共有アセットリポジトリにはミラーしていません. 合成モデルの Kokoro と Piper には eSpeak NG の発音データ (espeak-ng-data, GPL-3.0-or-later) が含まれます.

ホーム画面のローカルモデルをインポートは, tar, tar.bz2, zip のアーカイブまたはフォルダーを受け付けます. カタログのモデルと一致するファイルはそのモデルとして登録し, それ以外のファイルは種類, モデルタイプ, 名前, 言語を入力するとカスタムモデルとしてインポートします. インポートはシステムのファイル選択画面で行い, ストレージの権限は不要です.

******

### システムのテキスト読み上げ設定

******

1. プラグインのホーム画面の合成グループで, 合成モデルを 1 つ以上ダウンロードします. まず試すなら, 最も小さい `tts.vits.icefall.zh-aishell3` (中国語, 話者 174 人) と中国語と英語を混ぜて読める `tts.vits.melo.zh-en` がどちらも推奨モデルです.
2. システムのテキスト読み上げの設定を開き, 優先するエンジンを 3-Macaw Speech にします. プラグインのホーム画面の状態カードにあるシステムのテキスト読み上げエンジンの行から, このページを直接開けます. 手動で探す場合は, 通常 設定 > ユーザー補助 > テキスト読み上げの出力, または 設定 > システム > 言語と入力 > テキスト読み上げの出力 にあります (名称はメーカーによって異なります).
3. エンジンの横にある設定ボタンで 3-Macaw Speech の設定が開き, 合成グループで言語ごとのデフォルトの音声と話者を選べます. 読み上げ速度はシステムの設定または読み上げを依頼したアプリが決めます. 音の高さには対応していません.
4. システムの設定に戻ってサンプルを再生すると, その言語のデフォルトの音声でサンプルテキストを読み上げます.

各モデルの各話者が 1 つの音声で, 名前は `<モデル ID>/<話者番号>` です. 2 言語モデルの話者には英語用の `<モデル ID>/<話者番号>/en-US` もあります. AutoJs6 のスクリプトはシステムの設定を変えずにこのエンジンを使えます: `tts(text, { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" })`, またはモデルと話者を直接指定する `speech.speak(text, { model, speaker })`.

******

### スクリプト API

******

AutoJs6 6.8.0 (内部バージョン 5456) 以降, スクリプトはグローバルモジュール `speech` (`$speech` でも可) からこれらの機能を使います. 次の 3 つの例はそれぞれ単独で実行できます: 30 秒間書き起こして各文をファイルに追記する, ウェイクワードで起動して一文を聞く, ファイルに合成してから読み上げる:

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

- `speech.listen(options)` は書き起こしセッションを開き, `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error`, `end` イベントで結果を返します. `stop()` で早めに終了します. `speech.recognize(path)` は WAV ファイルを認識し (16 kHz の PCM も可), `speech.detectVoice(path)` はその音声区間を一覧にします.
- `speech.wake(keywords, options)` はキーワードを平文で書いたウェイクワードセッションを開きます (最大 50 個). `thenListen` を指定すると, 起動のたびに同じ録音のまま 1 ラウンドの書き起こしに移り, そのセッションは `wake` イベントの `listen` プロパティで渡されます.
- `speech.synthesize(text, path, options)` は WAV ファイルに合成します (最大 8000 文字, モデル, 話者, 速度を任意で指定). `speech.speak(text, options)` と `tts(text, { engine })` はシステムのテキスト読み上げエンジンで読み上げます.
- `speech.models` はモデルの一覧, ダウンロード (`install`, 進捗あり), インポート, 削除とデフォルトモデルの設定を行います. `speech.capabilities()` はプラグインのバージョン, 対応機能, マイクの権限の有無を返します.
- プラグイン全体で同時に使えるマイクセッションは 1 つ (書き起こしまたはウェイクワード) です. マイクの権限がないと呼び出しは `PERMISSION_DENIED` で失敗し, プラグインのホーム画面で許可できます. 認識したテキストが AutoJs6 のログに書き込まれることはありません.
- すべてのメソッド, オプション, イベント, エラーコードは [AutoJs6 ドキュメント](https://docs.autojs6.com/#/speech) を参照してください. AutoJs6 のサンプル > 3-Macaw Speech には, ファイルへの書き起こし, ウェイクワードと書き起こし, TTS による通知, 音声コマンドを Agent に渡す 4 つのサンプルがあります.

******

### 互換性

******

- Android 7.0 (API 24) 以降, ターゲット API 37. arm64-v8a, armeabi-v7a, x86_64, x86 のパッケージと universal パッケージがあります. ネイティブライブラリは 16 KB のメモリページに整列しており, 16 KB ページのエミュレーターで検証済みです.
- AutoJs6: スクリプトの `speech` モジュールには 6.8.0 (内部バージョン 5456) 以降が必要です. プラグインの画面, システムのテキスト読み上げエンジン, システムの音声入力, 音声認識サービス, デジタルアシスタントは AutoJs6 に依存しません.
- バックグラウンドでの聞き取り: AutoJs6 がフォアグラウンドにあるときに開始した書き起こしやウェイクワードのセッションは, AutoJs6 がバックグラウンドに移ったり画面が消えたりしても聞き取りを続けます (プラグインはマイクのフォアグラウンドサービスとその通知で録音を続けます). AutoJs6 がすでにバックグラウンドにあるときに開いたセッションはシステムにミュートされることがあり, その場合はコード `PERMISSION_DENIED` の `warning` イベントを受け取りますが終了せず, AutoJs6 がフォアグラウンドに戻ると音声が戻ります. バックグラウンドで聞き取る必要があるスクリプトは, AutoJs6 が表示されているうちにセッションを開いてください.
- 検証済みデバイス: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), および API 24, 35, 37 (16 KB ページ) のエミュレーター.
- 性能: ローエンドの Sony G8441 では, ストリーミングモデルの読み込みが約 1.7 秒, SenseVoice が約 2.8 秒で, 書き起こしは実時間の約 3 倍の速さで処理されます. この種のデバイスでは MeloTTS と Kokoro の合成は実時間より遅く, AISHELL-3 が最も速くなります. Sony XQ-DQ72 などの新しいデバイスでは読み込みが約 0.5 から 1.2 秒で, 合成は実時間より速くなります.
- メモリ: 読み込んだ認識モデルは約 210 から 300 MB のネイティブメモリを使います. プラグインは読み込んだモデルをデバイスのメモリの 4 分の 1 (384 MB から 1.5 GB) の範囲で管理し, 5 分間使われないと解放します (設定で 1 から 30 分に変更可能).
- デジタルアシスタント: システムがサードパーティのデジタルアシスタントアプリを選べる必要があります. 電源ボタンの長押しを独自のアシスタントに固定しているシステムでは, ホームボタンやアシスタントのジェスチャーを使ってください. ColorOS と HyperOS での動作はまだ検証していません.

******

### よくある質問

******

#### 認識と合成にインターネット接続は必要ですか?

不要です. モデルをダウンロードした後はすべての処理が端末内で行われ, 音声はアップロードされません. 接続するのはモデルのダウンロードとモデルカタログの更新のときだけです.

#### なぜプラグイン自身がマイクの権限を求めるのですか?

録音はプラグインのプロセス内で行われ, 音声が AutoJs6 を経由することはありません. AutoJs6 はプラグインをバインドするときにフォアグラウンドの権限を引き渡し, プラグインはマイクセッションの間マイクのフォアグラウンドサービスを保持するため, セッション開始後に AutoJs6 がバックグラウンドに移っても画面が消えても書き起こしは続きます. 初めて使う前にプラグイン画面でマイクの権限を許可してください.

#### どのパッケージをインストールすればよいですか?

ほとんどのスマートフォンは `arm64-v8a` です. デバイスの ABI が分からない場合は `universal` をインストールしてください. すべてのデバイスで動作しますが, サイズは大きくなります.

#### システムの設定でサンプルが鳴らない, またはエンジンが音声データがないと表示するのはなぜですか?

エンジンはインストール済みの合成モデルが対応する言語だけを報告します. 合成モデルがない場合や, インストール済みのモデルがデバイスの言語に対応していない場合, システムは音声データがないと表示し, サンプルや読み上げ速度を使えないことがあります. プラグインのホーム画面でデバイスの言語に対応した合成モデルをダウンロードし, システムの設定を開き直してください. サンプルにはその言語のデフォルトの音声が使われ, プラグインの設定の合成グループで変更できます.

#### 他のアプリで 3-Macaw Speech を音声入力に使えますか?

はい. 認識モデルをインストールしてマイクの権限を許可すると, システムの音声入力を呼び出すアプリ (3-Stove Agent のマイクボタンなど) で 3-Macaw Speech のパネルが開きます. Google などほかの音声入力アプリもある場合は, Android がどれを使うか尋ね, 選択を記憶できます. 他のアプリに表示したくない場合は, 設定の認識グループでシステムの音声入力として使うをオフにします.

#### 3-Macaw Speech をデフォルトのデジタルアシスタントにするには?

3-Macaw Speech の設定を開き, 認識グループのデジタルアシスタントをタップすると, システムの設定画面が開きます. そこで 3-Macaw Speech をデジタルアシスタントアプリに選んでください (システムによって画面の名前が少し異なります). その後, 電源ボタンまたはホームボタンの長押し, またはアシスタントのジェスチャーで音声入力できます. 電源ボタンが別の機能に割り当てられている場合は, ホームボタンかジェスチャーを使ってください.

#### アシスタントが常にウェイクワードを聞いていないのはなぜですか?

常に聞くにはマイク, 常駐の通知, マイクのインジケーターをずっとオンにする必要があり, 電池も多く使うため, デジタルアシスタントはアシスタントのボタンを押したときだけ聞きます. 3-Stone AI や 3-Stove Agent などのアプリは独自の常時待ち受けスイッチ (デフォルトはオフ) を提供できます. それが聞いている間もデジタルアシスタント, 音声入力, 音声認識は使え, ウェイクワードの検出は一時停止して, 終わると自動的に再開します. 3-Macaw Speech 自身のウェイクワードは今後のバージョンで予定しています.

#### モデルのダウンロードに失敗したら?

プラグインはまず AutoJs6 共有アセットリポジトリの GitHub リリースからダウンロードし, 失敗すると自動で sherpa-onnx の上流アドレスに切り替えます. 中断したダウンロードは再開できます. GitHub にアクセスできない場合は, 別のデバイスでモデルのアーカイブ (カタログにあるミラーまたは上流のアドレス) をダウンロードし, ホーム画面のローカルモデルをインポートを使ってください. カタログと一致するファイルはカタログのモデルとして登録されます.

#### モデルはどれくらいの容量を使い, どう解放しますか?

各モデルのインストール後のサイズは上記のモデル表のとおりです. 不要になったモデルはホーム画面で削除でき, 設定の参照されていないファイルを整理で, どのモデルにも不要なダウンロードファイル, インポートのコピー, 中断したインストールを削除できます (インストール済みのモデルと一時停止中のダウンロードは残ります). プラグインをアンインストールするとすべてのモデルが削除されます.

#### 認識結果に句読点がないのはなぜですか?

ストリーミングの Zipformer モデルは句読点なしで単語だけを出力します. SenseVoice モデルは音声区間ごとに認識し, 句読点を付けて言語も報告するので, 完全な文が必要な場合に向いています. スクリプトでは `model` オプションで指定するか, 設定でデフォルトの認識モデルにしてください.

#### ウェイクワードはどう書けばよく, 反応しないのはなぜですか?

キーワードはそのまま書きます. 中国語は一般的な読みでピンインに, 英単語はモデルの辞書で綴られます (辞書にない単語は拒否されます). 読みが複数ある漢字は, モデルのピンイントークンを直接書いて読みを指定できます. 3 から 4 音節で, あまり使われない語句ほど誤反応が少なくなります. キーワードごとに `threshold` (低いほど反応しやすい) と `boost` を設定できます. スクリプトに書く前に, お試し画面のウェイクワードで試してみてください.

#### APK が GPL-3.0 で配布されるのはなぜで, ソースはどこにありますか?

プラグイン自身のコードは MPL-2.0 です. sherpa-onnx の公式 Android ライブラリが eSpeak NG (GPL-3.0-or-later) を静的にリンクしているため, APK 全体は GPL-3.0-or-later で配布されます. 各リリースには対応するソースのアーカイブ (sherpa-onnx のソースとビルドスクリプト, およびそのすべての依存関係) が添付され, ファイル名に `corresponding-source` を含み, SHA-256 は `SHA256SUMS` に記載されます. プラグインのソースは, このリポジトリのリリースタグ時点の内容です.

******

### 権限とセキュリティ

******

プラグインは必要な権限をできるだけ少なくしています:

- マイク: 書き起こし, ウェイクワード, お試し画面でのみ録音し, セッションの終了またはスクリプトの終了と同時にマイクを解放します.
- ネットワーク: モデルのダウンロードと署名付きモデルカタログの更新にのみ使い, 認識と合成はオフラインで動作します.
- ストレージ: モデルはプラグインの専用ディレクトリに保存し, ローカルモデルはシステムのファイル選択画面からインポートするため, ストレージ権限は求めません.
- 保護された入口: 機能サービスと有効化の入口は AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`) で保護されており, 他のアプリから直接呼び出すことはできません. システムの TTS エンジンと, その音声チェック, サンプルテキスト, 設定の入口は Android の決まりどおりすべてのアプリに公開されており, 音声合成だけを提供します.
- システムの音声入力: システムの TTS エンジンと同じくすべてのアプリに開放されています; パネルが表示されている間だけ録音し, 認識されたテキストだけを返します. 設定でオフにできます.
- デジタルアシスタント: 画面の内容は読み取らず, 普段はバックグラウンドで聞いていません. パネルが表示されている間だけ録音し, 認識されたテキストはアプリのボタンを押すかコピーしたときにだけパネルの外に出ます (ロック画面ではロック解除の後). 音声認識サービスは, 自身がマイクの権限を持つアプリのためにだけ認識します.

******

### プラグインインターフェース

******

以下の情報は AutoJs6 ホストとプラグイン開発者向けです. ホストはこれらの識別子でプラグインを検出し, 機能ネゴシエーションを行います:

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

### 謝辞

******

3-Macaw Speech は次のオープンソースプロジェクト, モデル, データの上に成り立っています. 作者とメンテナーの皆さんに感謝します:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) と [k2-fsa (次世代 Kaldi)](https://github.com/k2-fsa) チーム: 認識, ウェイクワード, 音声区間検出, 合成のランタイム, 各モデルの ONNX エクスポートと Android ライブラリ (Apache-2.0).
- [icefall](https://github.com/k2-fsa/icefall) で学習したモデル: Zipformer ストリーミング認識モデル (中英 2 言語 pfluo, 英語 Zengwei, 中国語 2025 yuekai), オープン語彙のウェイクワードモデル (pkufool), k2-fsa による AISHELL-3 VITS と標貝 (Baker) Matcha-TTS の合成モデル.
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / Alibaba, FunASR モデルライセンス 1.1): 多言語のオフライン認識モデル.
- 合成モデル [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, 中国語の録音は LongMaoData が提供), [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT) と [HiFi-GAN](https://github.com/jik876/hifi-gan) ボコーダー (MIT).
- 学習データ: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), [Data-Baker 中国語標準女性音声コーパス](https://www.data-baker.com/open_source.html) (非商用のみ), Lessac Technologies の Blizzard 2013 データ (研究用のみ), [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (パブリックドメイン).
- 推論エンジン [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT), 音声区間検出モデル [silero VAD](https://github.com/snakers4/silero-vad) (MIT), 音素変換の [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) と [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT), 中国語ウェイクワードの読みの表 [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT).

******

### 開発ロードマップ

******

計画, 設計上の決定, 進捗は ROADMAP.md のチェックリストで管理しています. 未チェックの項目は計画であり, 現在のバージョンの機能ではありません.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.1.0

_2026/10/20_

- `機能` パッケージ化したアプリが専用の入口から音声認識とモデル管理を利用でき, マイクの利用は各アプリ自身の録音権限に従います

#### v1.0.0

_2026/10/20_

- `ヒント` 最初の正式リリースです. スクリプトの speech モジュールには AutoJs6 6.8.0 (5456) 以降が必要です. プラグインの画面, システムの TTS エンジン, システムの音声入力, 音声認識サービス, デジタルアシスタントは AutoJs6 に依存しません
- `ヒント` 認識と合成のモデルはパッケージに含まれず, プラグインの画面やスクリプトからダウンロードします. アプリ全体は GNU GPL 3.0 以降で頒布され (音声ランタイムが eSpeak NG を静的にリンクしているため), 各リリースに対応するソースコードが付属します
- `機能` AutoJs6 の音声プラグイン: プラグインセンターで有効にすると, スクリプトは speech モジュールから音声認識, ウェイクワード, 音声区間検出, 音声合成, モデル管理を使えます
- `機能` sherpa-onnx 1.13.8 音声ランタイムと silero VAD v5 音声区間検出モデルを内蔵し, 4 つの ABI ごとにパッケージを提供
- `機能` モデル管理: 署名付きモデルカタログのオフライン音声モデルを AutoJs6 共有アセットのリリースからダウンロードし, 利用できない場合は sherpa-onnx 公式リリースに切り替えます. 各ファイルを SHA-256 で検証してから使用し, ダウンロードの再開, ローカルインポート (tar, tar.bz2, zip), 削除, 既定モデルに対応します. 非商用限定またはライセンス未宣言のモデルは, 先にライセンスへの同意が必要です
- `機能` 音声認識: WAV ファイル (8 / 16 / 24 / 32 ビット整数または浮動小数点, 任意のサンプリングレートとチャンネル数, プラグイン内で 16 kHz モノラルにリサンプリング) を認識し, 時刻付きの音声区間を返します. SenseVoice モデルは言語, 感情, イベントのタグも返します. ストリーミングモデルはホットワードに対応します
- `機能` 書き起こしセッション: プラグインによる録音または送られた 16 kHz PCM から, 準備完了, 途中結果, 確定結果, エンドポイント, 音声区間, 終了を順に通知します. 文末とみなす無音の長さ, 無音タイムアウト, 最大時間を調整できます. ストリーミングモデルは単語ごと, オフラインモデルは音声区間ごとに途中結果を更新します
- `機能` 音声区間検出: 内蔵の silero VAD で WAV ファイルの音声区間を列挙します
- `機能` ウェイクワード: 語彙を限定しないキーワード検出です. 中国語のキーワードは一般的な読みのピンインで, 英単語はモデル辞書の音素でつづり, モデルのトークンを直接書くこともできます. 検出後は同じ録音のまま書き起こしを 1 回行えます
- `機能` マイクセッションの間, プラグインはマイクのフォアグラウンドサービスと通知で録音を続けるため, AutoJs6 がフォアグラウンドのときに始めたセッションは AutoJs6 がバックグラウンドに移っても画面が消えても続きます. マイクセッションは同時に 1 つだけです. マイクの権限がない場合は PERMISSION_DENIED となり, 許可画面を示します
- `機能` 音声合成: インストール済みの合成モデル (VITS, Matcha, Kokoro. カタログには中国語と英語のモデルが 5 種類あります) で最大 8000 文字のテキストを文ごとに WAV ファイルへ合成します. サンプリングレートはモデルと同じです. 話者と速度 (0.5 倍から 2 倍) を選べ, 進捗は完了した文字数で通知され, いつでもキャンセルできます. 話者が 1 人のモデルは選んだ話者を無視し, その旨を知らせます
- `機能` システムの TTS エンジン: 合成モデルをインストールすると, システムのテキスト読み上げ設定で 3-Macaw Speech を選べるようになり, AutoJs6 の tts モジュールや他のアプリから読み上げに使えます. 話者ごとに 1 つの音声があり, 2 か国語モデルでは話者ごとに英語の音声が加わります. 読み上げ速度はシステムまたはアプリの設定に従います. モデルがない場合, エンジンは音声データがないと報告します
- `機能` ホーム画面: 音声ランタイムのバージョン, インストール済みのモデルの数, マイクの権限 (ボタンで許可できます), 3-Macaw Speech がシステムの優先 TTS エンジンかどうかを表示します
- `機能` ホーム画面のモデルは認識, ウェイクワード, 合成, 音声区間検出に分けて表示され, サイズ, 言語, ライセンスも分かります. ダウンロード, 一時停止, 再開, キャンセル, 削除, デフォルトに設定ができ, ダウンロードの進捗は通知にも表示されます. 非商用限定またはライセンス未宣言のモデルは, 先にライセンスへの同意が必要です. モバイルデータなどの従量制ネットワークでは, ダウンロードを始める前に確認します
- `機能` ローカルモデルのインポート: tar, tar.bz2, zip のアーカイブまたはフォルダから読み込みます. カタログのモデルと一致するファイルはそのモデルとして登録されます. それ以外のファイルは, 種類, モデルタイプ, 名前, 言語, ID を入力すると独自のモデルとして追加されます
- `機能` お試し: リアルタイムの書き起こし (途中結果は灰色で表示) で, モデルと言語を選べます. ウェイクワードの試験では, 検出したキーワードとスコアを一覧で表示します. 入力したテキストをモデル, 話者, 速度を選んで音声合成し, 再生するか WAV ファイルとして保存できます. 画面を離れるとマイクを解放します
- `機能` システムの音声入力: 他のアプリ (3-Stove Agent など) のマイクボタンで 3-Macaw Speech のオフライン認識を使えます. 画面下部のパネルで一文を話すと, 認識されたテキストがそのままそのアプリに戻ります; 無音で自動的に終わり, 途中で停止や取消もできます; 設定のエンドポイントの感度と音声区間検出に従い, 不要なら設定の認識グループでオフにできます
- `機能` デジタルアシスタント: システム設定で 3-Macaw Speech をデフォルトのデジタルアシスタントアプリに選ぶと, 電源ボタンまたはホームボタンの長押し (またはアシスタントのジェスチャー) で画面下部のパネルが開き, 一文を話せます. 認識されたテキストは編集でき, ボタンで 3-Stove Agent や 3-Stone AI など音声コマンドを受け取るアプリに渡すか, コピーするか, もう一度話せます. ボタンを押すまで何も送信されません. ロック画面でも使え, 送信やコピーの前にロックを解除します. 画面の内容は読み取らず, 普段はバックグラウンドで聞いていません. 設定の認識グループでデフォルトのアシスタントかどうかを確認し, システム設定を開いて選べます
- `機能` システムの音声認識サービス: Android の音声認識 (SpeechRecognizer) を使うアプリは 3-Macaw Speech を直接指定して, そのオフライン認識を途中結果付きで使えます. デフォルトの音声認識サービスにすると, 指定しないアプリもこれを使います. Android 11 以前ではデジタルアシスタントに選ぶと通常はデフォルトにもなり, Android 12 以降はシステムの音声入力設定で別途選びます (この設定がない端末もあります). 他のアプリ (3-Stone AI など) が 3-Macaw Speech でウェイクワードを常に聞いている間も, デジタルアシスタント, システムの音声入力, 音声認識サービス, お試し画面はマイクを使えます. その間ウェイクワードの検出は一時停止し, 終わると自動的に再開します
- `機能` 設定: 言語, ナイトモード, テーマ色, ランチャーアイコン (デフォルトは AutoJs6 に従う). お試し画面とシステムの TTS エンジンで使うデフォルトの認識モデルと合成モデル, エンドポイントの感度, 音声区間検出, 話者, 速度, 言語ごとのデフォルトの音声 (スクリプトはドキュメントに記載されたデフォルト値を使います). 未使用のモデルを解放するまでの時間, モデルカタログの更新, 残ったファイルの整理. システムのテキスト読み上げ設定で 3-Macaw Speech の設定ボタンを押すと, この画面が開きます
- `機能` アプリについて: バージョン, プラグインに含まれるリリース履歴, 更新の確認 (自動確認は最大で 12 時間に 1 回, 特定のリリースを無視できます), ライセンスと通知. アプリ全体は GNU GPL 3.0 以降で頒布され, いかなる保証もありません. サードパーティの通知, GPL の全文, Unicode 利用規約, 対応するソースコードの場所も確認できます
- `機能` ランチャーアイコン: アダプティブ アイコン (ライト), アダプティブ アイコン (ダーク), アダプティブ アイコン (自動, デフォルト), 透明背景のアイコンから選べます
- `機能` 画面, README, プラグインセンターの説明, 変更履歴は 10 言語に対応: 簡体字中国語, 繁体字中国語 (香港, 台湾), 英語, フランス語, スペイン語, 日本語, 韓国語, ロシア語, アラビア語
- `依存関係` sherpa-onnx 1.13.8 を追加
- `依存関係` Jetpack Compose BOM 2026.09.00 を追加
- `依存関係` AndroidX Activity Compose 1.12.2 を追加
- `依存関係` AndroidX Core KTX 1.15.0 を追加
- `依存関係` Kotlin Coroutines 1.11.0 を追加
- `依存関係` Material Color Utilities (MaterialKolor) 4.1.1 を追加

##### その他のリリース履歴は以下を参照

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルド

******

この節はソースからプラグインをビルドしたい開発者向けです.

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

### ライセンス

******

プラグインのソースコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE) です. sherpa-onnx の公式 Android ライブラリ (Apache-2.0) は GNU GPL バージョン 3 以降の eSpeak NG と UCD Tools を静的にリンクしているため, 公開する APK 全体は GPL-3.0-or-later で配布され (MPL-2.0 の第 3.3 節はこの組み合わせを認めています), いかなる保証もありません. 各リリースには対応するソース `autojs6-plugin-three-macaw-speech-corresponding-source-sherpa-onnx-v1.13.8-ac68c80b.tar` が添付されます: sherpa-onnx 1.13.8 のソースとビルドスクリプト, およびそのビルドが URL と SHA-256 で固定するすべての依存アーカイブです. SHA-256 はリリースの `SHA256SUMS` に記載され, `.python/corresponding_source.py` で再生成と検証ができます. ONNX Runtime と silero VAD は MIT ライセンスで, 各モデルのライセンスは上記のモデル表のとおりです. プラグインの情報ページでライセンスの全文とサードパーティの表示を確認できます.

- [サードパーティの表示 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### 関連リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 ドキュメント: speech モジュール: https://docs.autojs6.com/#/speech
- sherpa-onnx プロジェクト: https://github.com/k2-fsa/sherpa-onnx
