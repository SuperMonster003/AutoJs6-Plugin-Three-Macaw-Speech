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

プラグインは個別にインストールする APK です. AutoJs6 のスクリプトはグローバルモジュール `speech` からこれらの機能を使います. 音声合成は Android のシステム TTS エンジンとしても登録されるため, AutoJs6 の `tts` モジュールや他のアプリからも選択できます. 認識モデルと合成モデルは APK に含まれず, プラグイン画面またはスクリプトから必要に応じてダウンロードします. 同梱されるのは音声区間検出モデルのみです.

> 現在は開発プレビュー版です: AutoJs6 はプラグインを検出でき, プラグインは音声ランタイムを読み込みますが, 認識, ウェイクワード, 合成, モデルのダウンロード, プラグイン画面, `speech` スクリプトモジュールはまだ提供されていません. 現在のアイコンは仮のもので, 後で差し替えられます. 以下は 1.0.0 の設計目標です. 進捗はロードマップを参照してください.

******

### 機能ハイライト

******

- 音声認識: マイクからのストリーミング書き起こし (途中結果と確定結果をリアルタイムに返す) と WAV ファイルの認識. 中国語, 英語, 中英混在, 日本語, 韓国語, 広東語のオフラインモデルを提供します.
- 音声区間検出: 内蔵の silero VAD v5 が音声を発話区間に分割し, 書き起こし時の自動的な文の区切りにも使われます.
- ウェイクワード: キーワードをそのまま入力するだけ (例: `小六小六` や `hey macaw`) でモデルの学習は不要です. 起動後は自動的に書き起こしへ移行できます.
- 音声合成: 複数の中国語と英語のニューラル音声. WAV ファイルへ合成できるほか, システム TTS エンジンとして AutoJs6 の `tts` モジュールや他のアプリから使えます.
- モデル管理: 署名付きモデルカタログからダウンロードし, ファイルごとに SHA-256 を検証してから有効化します. ローカルモデルのインポートにも対応します. 非商用に限られるモデルはダウンロード前にライセンスを表示します.

******

### 使用方法

******

1. AutoJs6 を内部バージョン 5448 (6.8.0) 以降に更新します.
2. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) からデバイスの ABI に合った APK をダウンロードしてインストールし (不明な場合は `universal` を選択), AutoJs6 のプラグインセンターで許可して有効にします.
3. プラグイン画面でマイクの権限を許可し, 必要なモデルをダウンロードしてから, スクリプトで `speech` モジュールを使います.

> プラグインは Android 7.0 (API 24) 以降のデバイスに対応します. ABI ごとのパッケージに加えて, すべてのデバイスで使える `universal` パッケージがあります.

******

### モデル

******

認識, ウェイクワード, 合成の各モデルは 30 MB から 350 MB あるため, APK には含まれません. プラグインは AutoJs6 共有アセットリポジトリの変更不可のリリースからモデルをダウンロードし, ミラーが使えない場合は sherpa-onnx の上流アドレスに切り替えます. どちらの経路でも, すべてのファイルが SHA-256 検証に合格してからモデルを有効にします.

すべてのモデルにライセンスが表示されます. 非商用に限られるモデル (例: DataBaker コーパスで学習した合成モデル) はダウンロード前に通知され, スクリプトからダウンロードする場合は `acceptLicense: true` を明示的に渡す必要があります.

******

### スクリプト API

******

スクリプトはグローバルモジュール `speech` (`$speech` とも書けます) からこれらの機能を使います (1.0.0 の設計案です. リリース版のドキュメントが優先されます):

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

- `speech.listen(options)` は書き起こしセッションを開き, `partial`, `final`, `end` などのイベントで結果を返します. `speech.recognize(path)` は WAV ファイルを認識します.
- `speech.wake(keywords, options)` はウェイクワードセッションを開きます. `thenListen` を指定すると起動後に自動で書き起こしへ移行します.
- `speech.synthesize(text, path)` は WAV ファイルへ合成します. `speech.speak(text)` と `tts(text, {engine})` はシステム TTS エンジンで読み上げます. `speech.models` はモデルを管理します.

******

### よくある質問

******

#### 認識と合成にインターネット接続は必要ですか?

不要です. モデルをダウンロードした後はすべての処理が端末内で行われ, 音声はアップロードされません. 接続するのはモデルのダウンロードとモデルカタログの更新のときだけです.

#### なぜプラグイン自身がマイクの権限を求めるのですか?

録音はプラグインのプロセス内で行われ, 音声が AutoJs6 を経由することはありません. AutoJs6 はプラグインをバインドするときにフォアグラウンドの権限を引き渡すため, スクリプトはバックグラウンドでも書き起こしできます. 初めて使う前にプラグイン画面でマイクの権限を許可してください.

#### どのパッケージをインストールすればよいですか?

ほとんどのスマートフォンは `arm64-v8a` です. デバイスの ABI が分からない場合は `universal` をインストールしてください. すべてのデバイスで動作しますが, サイズは大きくなります.

******

### 権限とセキュリティ

******

プラグインは必要な権限をできるだけ少なくしています:

- マイク: 書き起こし, ウェイクワード, お試し画面でのみ録音し, セッションの終了またはスクリプトの終了と同時にマイクを解放します.
- ネットワーク: モデルのダウンロードと署名付きモデルカタログの更新にのみ使い, 認識と合成はオフラインで動作します.
- ストレージ: モデルはプラグインの専用ディレクトリに保存し, ローカルモデルはシステムのファイル選択画面からインポートするため, ストレージ権限は求めません.
- 保護された入口: 機能サービスと有効化の入口は AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`) で保護されており, 他のアプリから直接呼び出すことはできません.

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
minimum host build: 5448
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### 開発ロードマップ

******

計画, 設計上の決定, 進捗は ROADMAP.md のチェックリストで管理しています. 未チェックの項目は計画であり, 現在のバージョンの機能ではありません.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/10/16_

- `ヒント` 開発プレビュー: AutoJs6 はプラグインを検出でき, プラグインは音声ランタイムを読み込みます. 音声認識, ウェイクワード, 音声合成, モデルのダウンロード, プラグイン画面, speech スクリプトモジュールはまだ提供されていません
- `ヒント` AutoJs6 6.8.0 (5448) 以降が必要です (正確な最低バージョンはホスト側の変更が反映された後に記入されます)
- `ヒント` 現在のアイコンは仮のもので, 後で差し替えられます
- `機能` プラグインの骨組み: Wake 有効化プロトコル, プラグインセンターの識別サービス, 音声機能サービス
- `機能` sherpa-onnx 1.13.8 音声ランタイムと silero VAD v5 音声区間検出モデルを内蔵し, 4 つの ABI ごとにパッケージを提供
- `機能` 10 言語の README, プラグインセンターの説明, 変更履歴を JSON ソースから一括生成
- `依存関係` sherpa-onnx 1.13.8 を追加
- `依存関係` Jetpack Compose BOM 2026.09.00 を追加
- `依存関係` AndroidX Activity Compose 1.12.2 を追加
- `依存関係` AndroidX Core KTX 1.15.0 を追加
- `依存関係` Kotlin Coroutines 1.11.0 を追加

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
```

******

### ライセンス

******

プロジェクトのコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE) で公開されています. 音声ランタイム sherpa-onnx は Apache-2.0 ライセンス, onnxruntime と silero VAD は MIT ライセンスです. 各モデルのライセンスはモデルカタログに個別に記載されています.

- [サードパーティの表示 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### 関連リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
- sherpa-onnx プロジェクト: https://github.com/k2-fsa/sherpa-onnx
