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

> 現在は開発プレビュー版です: AutoJs6 はプラグインを検出でき, プラグインはモデルのダウンロードと管理, 音声認識, 音声区間とウェイクワードの検出, 音声合成 (システムの TTS エンジンとしても利用可能) ができます. プラグイン自身の画面では, モデルの管理とお試し, 設定の変更ができます. `speech` スクリプトモジュールを含む AutoJs6 はまだリリースされていません. 以下は 1.0.0 の設計です. 進捗はロードマップを参照してください.

******

### 機能ハイライト

******

- 音声認識: マイクからのストリーミング書き起こし (途中結果と確定結果をリアルタイムに返す) と WAV ファイルの認識. 中国語, 英語, 中英混在, 日本語, 韓国語, 広東語のオフラインモデルを提供します.
- 音声区間検出: 内蔵の silero VAD v5 が音声を発話区間に分割し, 書き起こし時の自動的な文の区切りにも使われます.
- ウェイクワード: キーワードをそのまま入力するだけ (例: `小六小六` や `hey macaw`) でモデルの学習は不要です. 起動後は自動的に書き起こしへ移行できます.
- 音声合成: 複数の中国語と英語のニューラル音声. WAV ファイルへ合成できるほか, システム TTS エンジンとして AutoJs6 の `tts` モジュールや他のアプリから使えます.
- システムの音声入力: 他のアプリ (3-Stove Agent など) のマイクボタンでオフライン認識を使えます. 画面下部のパネルで一文を話すと, 認識されたテキストがそのアプリに戻ります.
- モデル管理: 署名付きモデルカタログからダウンロードし, ファイルごとに SHA-256 を検証してから有効化します. ローカルモデルのインポートにも対応します. 非商用に限られるモデルはダウンロード前にライセンスを表示します.

******

### 使用方法

******

1. AutoJs6 を内部バージョン 5451 (6.8.0) 以降に更新します.
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

録音はプラグインのプロセス内で行われ, 音声が AutoJs6 を経由することはありません. AutoJs6 はプラグインをバインドするときにフォアグラウンドの権限を引き渡し, プラグインはマイクセッションの間マイクのフォアグラウンドサービスを保持するため, セッション開始後に AutoJs6 がバックグラウンドに移っても画面が消えても書き起こしは続きます. 初めて使う前にプラグイン画面でマイクの権限を許可してください.

#### どのパッケージをインストールすればよいですか?

ほとんどのスマートフォンは `arm64-v8a` です. デバイスの ABI が分からない場合は `universal` をインストールしてください. すべてのデバイスで動作しますが, サイズは大きくなります.

#### 3-Macaw Speech をシステムのテキスト読み上げエンジンとして使うには?

まず合成モデルを 1 つ以上インストールし, システムのテキスト読み上げ設定 (多くはユーザー補助または言語の設定内) で優先エンジンを 3-Macaw Speech にします. モデルの話者ごとに `<モデル ID>/<話者番号>` という名前の音声があり, 2 か国語モデルには英語用の `<モデル ID>/<話者番号>/en-US` もあります. AutoJs6 のスクリプトはシステム設定を変えずにこのエンジンを選ぶこともできます: `tts(text, {engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech"})` または `speech.speak(text)`.

#### 他のアプリで 3-Macaw Speech を音声入力に使えますか?

はい. 認識モデルをインストールしてマイクの権限を許可すると, システムの音声入力を呼び出すアプリ (3-Stove Agent のマイクボタンなど) で 3-Macaw Speech のパネルが開きます. Google などほかの音声入力アプリもある場合は, Android がどれを使うか尋ね, 選択を記憶できます. 他のアプリに表示したくない場合は, 設定の認識グループでシステムの音声入力として使うをオフにします.

******

### 権限とセキュリティ

******

プラグインは必要な権限をできるだけ少なくしています:

- マイク: 書き起こし, ウェイクワード, お試し画面でのみ録音し, セッションの終了またはスクリプトの終了と同時にマイクを解放します.
- ネットワーク: モデルのダウンロードと署名付きモデルカタログの更新にのみ使い, 認識と合成はオフラインで動作します.
- ストレージ: モデルはプラグインの専用ディレクトリに保存し, ローカルモデルはシステムのファイル選択画面からインポートするため, ストレージ権限は求めません.
- 保護された入口: 機能サービスと有効化の入口は AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`) で保護されており, 他のアプリから直接呼び出すことはできません. システムの TTS エンジンと, その音声チェック, サンプルテキスト, 設定の入口は Android の決まりどおりすべてのアプリに公開されており, 音声合成だけを提供します.
- システムの音声入力: システムの TTS エンジンと同じくすべてのアプリに開放されています; パネルが表示されている間だけ録音し, 認識されたテキストだけを返します. 設定でオフにできます.

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
minimum host build: 5451
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

- `ヒント` 開発プレビュー: AutoJs6 はプラグインを検出でき, プラグインはモデルのダウンロードと管理, WAV ファイル, 外部 PCM, マイクの音声認識, 音声区間とウェイクワードの検出, 音声合成 (システムの TTS エンジンとしても利用可能) ができます. プラグイン自身の画面でモデルを管理し, 試すこともできます. speech スクリプトモジュールを含む AutoJs6 はまだリリースされていません
- `ヒント` AutoJs6 6.8.0 (5451) 以降が必要です (正確な最低バージョンはホスト側の変更が反映された後に記入されます)
- `機能` プラグインの骨組み: Wake 有効化プロトコル, プラグインセンターの識別サービス, 音声機能サービス
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
- `機能` 設定: 言語, ナイトモード, テーマ色, ランチャーアイコン (デフォルトは AutoJs6 に従う). お試し画面とシステムの TTS エンジンで使うデフォルトの認識モデルと合成モデル, エンドポイントの感度, 音声区間検出, 話者, 速度, 言語ごとのデフォルトの音声 (スクリプトはドキュメントに記載されたデフォルト値を使います). 未使用のモデルを解放するまでの時間, モデルカタログの更新, 残ったファイルの整理. システムのテキスト読み上げ設定で 3-Macaw Speech の設定ボタンを押すと, この画面が開きます
- `機能` アプリについて: バージョン, プラグインに含まれるリリース履歴, 更新の確認 (自動確認は最大で 12 時間に 1 回, 特定のリリースを無視できます), ライセンスと通知. アプリ全体は GNU GPL 3.0 以降で頒布され, いかなる保証もありません. サードパーティの通知, GPL の全文, Unicode 利用規約, 対応するソースコードの場所も確認できます
- `機能` ランチャーアイコン: アダプティブ アイコン (ライト), アダプティブ アイコン (ダーク), アダプティブ アイコン (自動, デフォルト), 透明背景のアイコンから選べます
- `機能` 10 言語の README, プラグインセンターの説明, 変更履歴を JSON ソースから一括生成
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
