<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-macaw-speech-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Офлайн-распознавание речи, обнаружение слов активации, определение речевой активности и нейросетевой синтез речи с регистрацией в качестве системного движка TTS</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки (Languages)

******

Текущий README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ar.md)

******

### Введение

******

3-Macaw Speech использует [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx), чтобы выполнять на устройстве без подключения к сети распознавание речи, определение речевой активности, обнаружение слов активации и нейросетевой синтез речи. Все вычисления выполняются локально, звук никуда не отправляется.

Плагин устанавливается отдельным APK. Скрипты AutoJs6 используют его через глобальный модуль `speech`; синтез речи также регистрируется как системный движок TTS Android, поэтому его могут выбрать модуль `tts` AutoJs6 и другие приложения. Модели распознавания и синтеза не входят в APK и загружаются по требованию с экрана плагина или из скрипта; встроена только модель определения речевой активности.

> Это предварительная версия для разработки: AutoJs6 обнаруживает плагин, а плагин загружает речевую среду выполнения, но распознавание, слова активации, синтез, загрузка моделей, экраны плагина и модуль скриптов `speech` ещё не готовы. Ниже описаны цели версии 1.0.0; ход работы смотрите в плане развития.

******

### Основные Возможности

******

- Распознавание речи: потоковая диктовка с микрофона (промежуточные и итоговые результаты в реальном времени) и распознавание файлов WAV; офлайн-модели для китайского, английского, смешанной китайско-английской речи, японского, корейского и кантонского.
- Определение речевой активности: встроенный silero VAD v5 сам делит звук на речевые фрагменты и автоматически завершает фразы при диктовке.
- Слова активации: достаточно ввести ключевое слово текстом (например, `小六小六` или `hey macaw`), обучать модель не нужно; после активации диктовка может начаться автоматически.
- Синтез речи: несколько нейросетевых китайских и английских голосов; синтез в файлы WAV или работа в качестве системного движка TTS для модуля `tts` AutoJs6 и других приложений.
- Управление моделями: загрузка из подписанного каталога моделей, включение только после проверки SHA-256 каждого файла, импорт локальных моделей; для моделей только для некоммерческого использования лицензия показывается до загрузки.

******

### Использование

******

1. Обновите AutoJs6 до внутренней версии 5451 (6.8.0) или новее.
2. Скачайте со страницы [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) APK для ABI вашего устройства (если не уверены, выберите `universal`) и установите его, затем разрешите и включите плагин в центре плагинов AutoJs6.
3. Откройте экран плагина, предоставьте доступ к микрофону и загрузите нужные модели, затем используйте модуль `speech` в скриптах.

> Плагин поддерживает устройства с Android 7.0 (API 24) и новее. Для каждого ABI есть отдельный пакет, а пакет `universal` подходит для всех устройств.

******

### Модели

******

Модели распознавания, слов активации и синтеза занимают от 30 МБ до 350 МБ каждая, поэтому они не входят в APK. Плагин загружает их из неизменяемых выпусков общего репозитория ресурсов AutoJs6, а если зеркало недоступно, переходит на исходный адрес sherpa-onnx; в обоих случаях каждый файл должен пройти проверку SHA-256, прежде чем модель будет включена.

Для каждой модели указана лицензия. Для моделей только для некоммерческого использования (например, модели синтеза, обученной на корпусе DataBaker) перед загрузкой показывается предупреждение, а скрипты должны явно передать `acceptLicense: true`.

******

### API Скриптов

******

Скрипты используют эти возможности через глобальный модуль `speech` (также доступен как `$speech`; это проект API версии 1.0.0, приоритет имеет документация выпущенной версии):

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

- `speech.listen(options)` открывает сеанс диктовки, который сообщает результаты событиями `partial`, `final`, `end` и другими; `speech.recognize(path)` распознаёт файл WAV.
- `speech.wake(keywords, options)` открывает сеанс слов активации; `thenListen` автоматически запускает диктовку после активации.
- `speech.synthesize(text, path)` синтезирует речь в файл WAV; `speech.speak(text)` и `tts(text, {engine})` озвучивают текст через системный движок TTS; `speech.models` управляет моделями.

******

### Частые Вопросы

******

#### Нужен ли интернет для распознавания и синтеза?

Нет. После загрузки моделей все вычисления выполняются на устройстве, звук никуда не отправляется. Подключение нужно только для загрузки моделей и обновления каталога.

#### Почему плагин сам запрашивает доступ к микрофону?

Запись ведётся внутри процесса плагина, и звук не проходит через AutoJs6. При привязке AutoJs6 передаёт плагину свои возможности активного приложения, поэтому скрипты могут вести диктовку и в фоне. Перед первым использованием предоставьте доступ к микрофону на экране плагина.

#### Какой пакет установить?

Большинству телефонов подходит `arm64-v8a`. Если вы не знаете ABI устройства, установите `universal`: он работает на всех устройствах, но занимает больше места.

******

### Разрешения И Безопасность

******

Плагин запрашивает минимально необходимые разрешения:

- Микрофон: запись ведётся только для диктовки, слов активации и экрана пробы, а микрофон освобождается сразу после завершения сеанса или скрипта.
- Сеть: используется только для загрузки моделей и обновления подписанного каталога; распознавание и синтез работают без сети.
- Хранилище: модели хранятся в личном каталоге плагина, а локальные модели импортируются через системный выбор файлов, поэтому доступ к хранилищу не запрашивается.
- Защищённые точки входа: служба возможностей и точка активации требуют разрешения плагинов AutoJs6 (`org.autojs.permission.PLUGIN`), поэтому другие приложения не могут вызвать их напрямую.

******

### Интерфейс Плагина

******

Следующая информация предназначена разработчикам хоста AutoJs6 и плагинов; хост использует эти идентификаторы для обнаружения плагина и согласования возможностей:

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

### Дорожная Карта

******

Планы, проектные решения и ход работ ведутся в виде списка с отметками в ROADMAP.md. Неотмеченные пункты являются планами, а не возможностями текущей версии.

- [Открыть ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### История Выпусков

******

#### v1.0.0

_2026/10/16_

- `Подсказка` Предварительная версия для разработки: AutoJs6 обнаруживает плагин, а плагин загружает речевую среду выполнения; распознавание речи, слова активации, синтез речи, загрузка моделей, экраны плагина и модуль скриптов speech ещё не готовы
- `Подсказка` Требуется AutoJs6 6.8.0 (5451) или новее (точная минимальная версия будет указана после внесения изменений в хост)
- `Функция` Каркас плагина: протокол активации Wake, служба идентификации для центра плагинов и служба речевых возможностей
- `Функция` Встроенная речевая среда sherpa-onnx 1.13.8 и модель определения речевой активности silero VAD v5, отдельный пакет для каждого из 4 ABI
- `Функция` README, инструкции для центра плагинов и журналы изменений на 10 языках, создаваемые из исходников JSON
- `Зависимость` Добавлен sherpa-onnx 1.13.8
- `Зависимость` Добавлен Jetpack Compose BOM 2026.09.00
- `Зависимость` Добавлен AndroidX Activity Compose 1.12.2
- `Зависимость` Добавлен AndroidX Core KTX 1.15.0
- `Зависимость` Добавлен Kotlin Coroutines 1.11.0

##### Больше истории выпусков

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка

******

Этот раздел предназначен разработчикам, желающим собрать плагин из исходников.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\verify_native_alignment.py --check
py .python\check_repository.py
```

******

### Лицензия

******

Код проекта распространяется по лицензии [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). Речевая среда sherpa-onnx использует лицензию Apache-2.0, onnxruntime и silero VAD используют лицензию MIT; лицензия каждой модели указана в каталоге моделей.

- [Сведения о сторонних компонентах (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### Ссылки

******

- Проект AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Проект sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
