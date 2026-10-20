<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-macaw-speech-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>오프라인 음성 인식, 호출어 감지, 음성 구간 감지와 신경망 음성 합성을 제공하며 시스템 TTS 엔진으로 등록 가능</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어 (Languages)

******

현재 README.md 는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ar.md)

******

### 소개

******

3-Macaw Speech는 [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)를 사용해 음성 인식, 음성 구간 감지, 호출어 감지, 신경망 음성 합성을 기기에서 오프라인으로 실행합니다. 모든 처리는 기기 안에서 이루어지며 음성은 업로드되지 않습니다.

플러그인은 따로 설치하는 APK이며, 모델을 내려받고 관리하고, 체험하고, 설정을 바꾸는 자체 화면이 있습니다. AutoJs6 스크립트는 전역 모듈 `speech`로 이 기능을 사용합니다. 음성 합성은 Android 시스템 텍스트 음성 변환 엔진으로도 등록되고, 음성 인식은 시스템 음성 입력, 시스템 음성 인식 서비스, 디지털 어시스턴트로도 제공되므로 다른 앱에서도 사용할 수 있습니다. 인식과 합성 모델은 APK에 포함되지 않고 필요할 때 내려받으며, 음성 구간 검출 모델만 내장되어 있습니다.

> 스크립트에서 `speech` 모듈을 사용하려면 AutoJs6 6.8.0 (내부 버전 5456) 이상이 필요합니다. 시스템 텍스트 음성 변환 엔진, 시스템 음성 입력, 음성 인식 서비스, 디지털 어시스턴트와 플러그인 자체 화면은 AutoJs6에 의존하지 않으며, 플러그인을 설치하고 모델을 내려받으면 바로 사용할 수 있습니다.

******

### 기능 하이라이트

******

- 음성 인식: 마이크 스트리밍 받아쓰기 (부분 결과와 최종 결과를 실시간으로 반환)와 WAV 파일 인식을 지원하며, 중국어, 영어, 중영 혼합, 일본어, 한국어, 광둥어 오프라인 모델을 제공합니다.
- 음성 구간 감지: 내장된 silero VAD v5가 오디오를 음성 구간으로 나누며, 받아쓰기의 자동 문장 끊기에도 사용됩니다.
- 호출어: 키워드를 그대로 입력하면 되고 (예: `小六小六` 또는 `hey macaw`) 모델을 학습할 필요가 없습니다. 호출 후 자동으로 받아쓰기로 넘어갈 수 있습니다.
- 음성 합성: 여러 중국어와 영어 신경망 음성을 제공하며, WAV 파일로 합성하거나 시스템 TTS 엔진으로서 AutoJs6의 `tts` 모듈과 다른 앱에서 사용할 수 있습니다.
- 시스템 음성 입력: 3-Stove Agent 등 다른 앱의 마이크 버튼에서 오프라인 인식을 사용할 수 있습니다. 하단 패널에서 한 문장을 말하면 인식된 텍스트가 그 앱으로 돌아갑니다.
- 디지털 어시스턴트: 3-Macaw Speech를 기본 디지털 어시스턴트 앱으로 선택한 뒤 전원 버튼이나 홈 버튼을 길게 눌러 한 문장을 말하고, 필요하면 인식된 텍스트를 편집해 3-Stove Agent, 3-Stone AI 등 음성 명령을 받는 앱에 넘기거나 복사합니다. 시스템 음성 인식 서비스도 제공합니다. Android 11 이하에서는 어시스턴트로 선택하면 보통 기본 음성 인식 서비스로도 설정되고, Android 12부터는 시스템의 음성 입력 설정에서 따로 선택해야 하며, 이 설정이 없는 휴대전화도 있습니다. 앱이 직접 지정할 수도 있습니다.
- 모델 관리: 서명된 모델 카탈로그에서 내려받고 파일마다 SHA-256 검증을 통과한 뒤에 활성화하며, 로컬 모델 가져오기도 지원합니다. 비상업적 용도로 제한된 모델은 내려받기 전에 라이선스를 안내합니다.
- 독립 화면: 홈 화면에서 상태를 보고 모델을 관리하며, 체험하기 화면에서 받아쓰기, 호출어, 합성을 바로 시험하고, 설정에서 언어, 야간 모드, 테마 색상, 런처 아이콘, 인식과 합성의 기본값을 바꿉니다. 외관은 기본적으로 AutoJs6를 따릅니다.

******

### 스크린샷

******

스크린샷은 Sony XQ-DQ72 (Android 13)에서 찍었습니다. 받아쓴 텍스트는 sherpa-onnx 테스트 녹음을 휴대전화 스피커로 재생해 플러그인이 인식한 결과이며, 개인 정보는 없습니다.

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/home.png?raw=true" alt="홈: 상태와 용도별 모델" width="360" />
      <br />
      <sub>홈: 상태와 용도별 모델</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/tryout-dictation.png?raw=true" alt="체험하기: 실시간 받아쓰기" width="360" />
      <br />
      <sub>체험하기: 실시간 받아쓰기</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/voice-input.png?raw=true" alt="시스템 음성 입력: 다른 앱이 연 하단 패널" width="360" />
      <br />
      <sub>시스템 음성 입력: 다른 앱이 연 하단 패널</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/assistant.png?raw=true" alt="디지털 어시스턴트: 결과를 편집하고 받을 앱 선택" width="360" />
      <br />
      <sub>디지털 어시스턴트: 결과를 편집하고 받을 앱 선택</sub>
    </td>
  </tr>
</table>

******

### 설치와 사용

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases)에서 기기 ABI에 맞는 APK를 내려받아 설치합니다. 대부분의 휴대전화는 `arm64-v8a`이며, 확실하지 않으면 모든 기기에서 동작하지만 크기가 더 큰 `universal`을 선택합니다.
2. 3-Macaw Speech를 열고 홈 화면에서 마이크 권한을 허용한 뒤, 인식, 호출어, 합성 그룹에서 필요한 모델을 내려받습니다 (카탈로그의 추천 모델로 시작하면 좋습니다. 아래 모델 표 참고). Android 13 이상에서는 처음 내려받을 때 다운로드 진행 알림 표시 권한도 요청합니다.
3. AutoJs6에서 사용: AutoJs6를 6.8.0 (내부 버전 5456) 이상으로 업데이트하고, 플러그인 센터에서 3-Macaw Speech를 활성화한 뒤 스크립트에서 `speech` 모듈을 호출합니다 (아래 스크립트 API 참고). AutoJs6의 샘플 > 3-Macaw Speech에 바로 실행할 수 있는 샘플 4개가 있습니다.
4. 다른 앱에서 사용: 시스템 텍스트 음성 변환 엔진은 아래 시스템 텍스트 음성 변환 설정을, 시스템 음성 입력, 음성 인식 서비스, 디지털 어시스턴트는 자주 묻는 질문을 참고하세요.

> ABI마다 패키지가 하나씩 있고, 모든 기기용 `universal` 패키지도 있습니다. 패키지에는 인식과 합성 모델이 없으며, 모델은 플러그인 화면이나 스크립트에서 필요할 때 내려받거나 로컬 파일에서 가져옵니다.

******

### 모델

******

인식, 호출어, 합성 모델은 각각 9 MB에서 410 MB라서 APK에 포함하지 않습니다. 플러그인은 서명된 모델 카탈로그를 내장하고, AutoJs6 공유 자산 저장소의 변경할 수 없는 릴리스에서 모델을 내려받으며, 미러를 사용할 수 없으면 sherpa-onnx 원본 주소로 전환합니다. 두 경로 모두 모든 파일이 SHA-256 검사를 통과해야 모델을 사용합니다. 1.0.0 카탈로그에는 다음 모델이 있습니다 (크기는 설치 후 차지하는 공간입니다):

| 모델 ID | 용도 | 언어 | 크기 | 라이선스 | 상업적 이용 |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (추천) | 인식 (오프라인, 문장 부호 포함) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | 가능 |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (추천) | 인식 (스트리밍) | zh, en | 199.1 MB | Apache-2.0 | 가능 |
| `asr.zipformer-streaming.en.2023-06-26.int8` (추천) | 인식 (스트리밍) | en | 70.3 MB | Apache-2.0 | 가능 |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | 인식 (스트리밍) | zh | 167.4 MB | 선언 없음 | 비상업적 이용만 |
| `kws.zipformer.zh-en.2025-12-20` (추천) | 호출어 | zh, en | 8.8 MB | Apache-2.0 | 가능 |
| `tts.vits.melo.zh-en` (추천) | 합성 (화자: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | 가능 |
| `tts.vits.icefall.zh-aishell3` (추천) | 합성 (화자: 174, 8 kHz) | zh | 32.8 MB | 선언 없음 | 알 수 없음 |
| `tts.kokoro.multi-lang.v1_1` | 합성 (화자: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | 가능 |
| `tts.matcha.icefall.zh-baker` | 합성 (화자: 1, 22.05 kHz) | zh | 81.0 MB | 선언 없음, MIT (보코더) | 비상업적 이용만 |
| `tts.vits.piper.en-us-lessac-medium` | 합성 (화자: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 비상업적 이용만 |

비상업적 이용으로 제한되거나 라이선스를 알 수 없는 모델은 내려받기 전에 라이선스를 보여 주며, 스크립트에서 내려받을 때는 `acceptLicense: true`를 명시해야 합니다. 2025 중국어 스트리밍 모델은 가중치에 라이선스 선언이 없고 학습 데이터에 비상업적 이용 전용 말뭉치가 포함되어 있어, sherpa-onnx 원본 주소에서만 내려받고 공유 자산 저장소에는 미러하지 않습니다. 합성 모델 Kokoro와 Piper에는 eSpeak NG의 발음 데이터 (espeak-ng-data, GPL-3.0-or-later)가 포함됩니다.

홈 화면의 로컬 모델 가져오기는 tar, tar.bz2, zip 압축 파일이나 폴더를 받습니다. 카탈로그 모델과 일치하는 파일은 그 모델로 등록하고, 나머지 파일은 종류, 모델 유형, 이름, 언어를 입력하면 사용자 지정 모델로 가져옵니다. 가져오기는 시스템 파일 선택기로 이루어지며 저장소 권한이 필요 없습니다.

******

### 시스템 텍스트 음성 변환 설정

******

1. 플러그인 홈 화면의 합성 그룹에서 합성 모델을 하나 이상 내려받습니다. 먼저 써 보려면 가장 작은 `tts.vits.icefall.zh-aishell3` (중국어, 화자 174명)와 중국어와 영어를 섞어 읽는 `tts.vits.melo.zh-en`이 모두 추천 모델입니다.
2. 시스템의 텍스트 음성 변환 설정을 열고 기본 엔진을 3-Macaw Speech로 지정합니다. 플러그인 홈 화면 상태 카드의 시스템 텍스트 음성 변환 엔진 행에서 이 페이지를 바로 열 수 있습니다. 직접 찾을 때는 보통 설정 > 접근성 > 텍스트 음성 변환 출력, 또는 설정 > 시스템 > 언어 및 입력 > 텍스트 음성 변환 출력에 있습니다 (제조사마다 이름이 조금 다릅니다).
3. 엔진 옆의 설정 버튼을 누르면 3-Macaw Speech 설정이 열리고, 합성 그룹에서 언어별 기본 음성과 화자를 고를 수 있습니다. 말하기 속도는 시스템 설정이나 읽기를 요청한 앱이 정하며, 음높이는 지원하지 않습니다.
4. 시스템 설정으로 돌아가 예시 듣기를 누르면 그 언어의 기본 음성으로 예시 텍스트를 읽습니다.

각 모델의 각 화자가 하나의 음성이며 이름은 `<모델 ID>/<화자 번호>`입니다. 2개 언어 모델의 화자에게는 영어용 `<모델 ID>/<화자 번호>/en-US`도 있습니다. AutoJs6 스크립트는 시스템 설정을 바꾸지 않고도 이 엔진을 쓸 수 있습니다: `tts(text, { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" })`, 또는 모델과 화자를 직접 지정하는 `speech.speak(text, { model, speaker })`.

******

### 스크립트 API

******

AutoJs6 6.8.0 (내부 버전 5456)부터 스크립트는 전역 모듈 `speech` (`$speech`로도 사용 가능)로 이 기능을 사용합니다. 아래 세 예시는 각각 따로 실행할 수 있습니다: 30초 동안 받아써 문장마다 파일에 덧붙이기, 호출어로 깨운 뒤 한 문장 듣기, 파일로 합성한 뒤 읽어 주기:

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

- `speech.listen(options)`는 받아쓰기 세션을 열고 `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error`, `end` 이벤트로 결과를 알리며, `stop()`으로 일찍 끝냅니다. `speech.recognize(path)`는 WAV 파일을 인식하고 (16 kHz PCM도 가능), `speech.detectVoice(path)`는 그 안의 음성 구간을 나열합니다.
- `speech.wake(keywords, options)`는 키워드를 일반 텍스트로 적은 호출어 세션을 엽니다 (최대 50개). `thenListen`을 지정하면 깨어날 때마다 같은 녹음에서 한 라운드의 받아쓰기로 넘어가며, 그 세션은 `wake` 이벤트의 `listen` 속성으로 전달됩니다.
- `speech.synthesize(text, path, options)`는 WAV 파일로 합성합니다 (최대 8000자, 모델, 화자, 속도 선택 가능). `speech.speak(text, options)`와 `tts(text, { engine })`는 시스템 텍스트 음성 변환 엔진으로 읽습니다.
- `speech.models`는 모델 목록, 내려받기 (`install`, 진행률 제공), 가져오기, 삭제와 기본 모델 지정을 맡습니다. `speech.capabilities()`는 플러그인 버전, 지원 기능, 마이크 권한 여부를 돌려줍니다.
- 플러그인 전체에서 동시에 하나의 마이크 세션 (받아쓰기 또는 호출어)만 열 수 있습니다. 마이크 권한이 없으면 호출이 `PERMISSION_DENIED`로 실패하며, 플러그인 홈 화면에서 허용할 수 있습니다. 인식된 텍스트는 AutoJs6 로그에 기록되지 않습니다.
- 모든 메서드, 옵션, 이벤트, 오류 코드는 [AutoJs6 문서](https://docs.autojs6.com/#/speech)를 참고하세요. AutoJs6의 샘플 > 3-Macaw Speech에는 파일로 받아쓰기, 호출어와 받아쓰기, TTS 알림, 음성 명령을 Agent에 넘기기 등 4개의 샘플이 있습니다.

******

### 호환성

******

- Android 7.0 (API 24) 이상, 대상 API 37. arm64-v8a, armeabi-v7a, x86_64, x86 패키지와 universal 패키지가 있으며, 네이티브 라이브러리는 16 KB 메모리 페이지에 맞춰 정렬되어 16 KB 페이지 에뮬레이터에서 검증했습니다.
- AutoJs6: 스크립트의 `speech` 모듈에는 6.8.0 (내부 버전 5456) 이상이 필요합니다. 플러그인 화면, 시스템 텍스트 음성 변환 엔진, 시스템 음성 입력, 음성 인식 서비스, 디지털 어시스턴트는 AutoJs6에 의존하지 않습니다.
- 백그라운드 듣기: AutoJs6가 포그라운드에 있을 때 시작한 받아쓰기나 호출어 세션은 AutoJs6가 백그라운드로 가거나 화면이 꺼져도 계속 듣습니다 (플러그인이 마이크 포그라운드 서비스와 알림으로 녹음을 유지합니다). AutoJs6가 이미 백그라운드에 있을 때 연 세션은 시스템이 음소거할 수 있으며, 이때 세션은 코드 `PERMISSION_DENIED`인 `warning` 이벤트를 받지만 끝나지 않고, AutoJs6가 포그라운드로 돌아오면 소리가 돌아옵니다. 백그라운드에서 들어야 하는 스크립트는 AutoJs6가 보이는 동안 세션을 여세요.
- 검증한 기기: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), 그리고 API 24, 35, 37 (16 KB 페이지) 에뮬레이터.
- 성능: 저사양 Sony G8441에서 스트리밍 모델은 약 1.7초, SenseVoice는 약 2.8초 만에 로드되고, 받아쓰기는 실시간보다 약 3배 빠르게 처리됩니다. 이런 기기에서 MeloTTS와 Kokoro는 실시간보다 느리게 합성하며 AISHELL-3가 가장 빠릅니다. Sony XQ-DQ72 같은 최신 기기는 약 0.5~1.2초 만에 로드하고 실시간보다 빠르게 합성합니다.
- 메모리: 로드된 인식 모델 하나는 약 210~300 MB의 네이티브 메모리를 사용합니다. 플러그인은 로드된 모델을 기기 메모리의 4분의 1 (384 MB~1.5 GB) 안에서 관리하고, 5분 동안 쓰지 않으면 해제합니다 (설정에서 1~30분으로 변경 가능).
- 디지털 어시스턴트: 시스템에서 타사 디지털 어시스턴트 앱을 선택할 수 있어야 합니다. 전원 버튼 길게 누르기를 자체 어시스턴트에 고정한 시스템에서는 홈 버튼이나 어시스턴트 제스처를 사용하세요. ColorOS와 HyperOS에서의 동작은 아직 검증하지 않았습니다.

******

### 자주 묻는 질문

******

#### 인식과 합성에 인터넷 연결이 필요한가요?

필요하지 않습니다. 모델을 내려받은 뒤에는 모든 처리가 기기에서 이루어지며 음성은 업로드되지 않습니다. 모델을 내려받거나 모델 카탈로그를 새로 고칠 때만 연결합니다.

#### 왜 플러그인이 직접 마이크 권한을 요청하나요?

녹음은 플러그인 프로세스 안에서 이루어지며 오디오는 AutoJs6 를 거치지 않습니다. AutoJs6 는 플러그인을 바인딩할 때 포그라운드 권한을 넘겨주고, 플러그인은 마이크 세션 동안 마이크 포그라운드 서비스를 유지하므로 세션이 시작된 뒤 AutoJs6 가 백그라운드로 가거나 화면이 꺼져도 받아쓰기가 계속됩니다. 처음 사용하기 전에 플러그인 화면에서 마이크 권한을 허용하세요.

#### 어떤 설치 파일을 설치해야 하나요?

대부분의 휴대전화는 `arm64-v8a`입니다. 기기 ABI를 모르면 `universal`을 설치하세요. 모든 기기에서 동작하지만 용량이 더 큽니다.

#### 시스템 설정의 예시 듣기에서 소리가 나지 않거나 엔진이 음성 데이터가 없다고 표시하는 이유는 무엇인가요?

엔진은 설치된 합성 모델이 지원하는 언어만 알립니다. 합성 모델이 없거나 설치된 모델이 기기 언어를 지원하지 않으면 시스템에 음성 데이터가 없다고 표시되고, 예시 듣기와 말하기 속도를 쓸 수 없을 수 있습니다. 플러그인 홈 화면에서 기기 언어를 지원하는 합성 모델을 내려받은 뒤 시스템 설정을 다시 여세요. 예시 듣기는 그 언어의 기본 음성을 사용하며, 플러그인 설정의 합성 그룹에서 바꿀 수 있습니다.

#### 다른 앱에서 3-Macaw Speech로 음성 입력을 할 수 있나요?

네. 인식 모델을 설치하고 마이크 권한을 허용하면, 시스템 음성 입력을 호출하는 앱 (예: 3-Stove Agent의 마이크 버튼) 에서 3-Macaw Speech 패널이 열립니다. Google 등 다른 음성 입력 앱도 설치되어 있으면 Android가 어느 앱을 쓸지 묻고 선택을 기억할 수 있습니다. 다른 앱에 보이지 않게 하려면 설정의 인식 그룹에서 시스템 음성 입력으로 사용을 끄세요.

#### 3-Macaw Speech를 기본 디지털 어시스턴트로 설정하려면 어떻게 하나요?

3-Macaw Speech 설정을 열고 인식 그룹의 디지털 어시스턴트를 탭하면 시스템 설정이 열립니다. 거기서 3-Macaw Speech를 디지털 어시스턴트 앱으로 선택하세요(시스템에 따라 화면 이름이 조금 다를 수 있습니다). 그다음 전원 버튼이나 홈 버튼을 길게 누르거나 어시스턴트 동작으로 받아쓰기를 시작합니다. 전원 버튼이 다른 기능으로 설정되어 있으면 홈 버튼이나 동작을 사용하세요.

#### 어시스턴트가 호출어를 항상 듣지 않는 이유는 무엇인가요?

항상 들으려면 마이크, 상주 알림, 마이크 표시등을 계속 켜 두어야 하고 배터리도 더 쓰기 때문에, 디지털 어시스턴트는 어시스턴트 버튼을 누를 때만 듣습니다. 3-Stone AI, 3-Stove Agent 같은 앱은 자체 상시 대기 스위치(기본값 꺼짐)를 제공할 수 있습니다. 그 앱이 듣는 동안에도 디지털 어시스턴트, 음성 입력, 음성 인식은 그대로 사용할 수 있으며, 호출어 감지는 일시 중지되었다가 끝나면 자동으로 다시 시작됩니다. 3-Macaw Speech 자체 호출어는 이후 버전에서 제공할 예정입니다.

#### 모델 내려받기에 실패하면 어떻게 하나요?

플러그인은 먼저 AutoJs6 공유 자산 저장소의 GitHub 릴리스에서 내려받고, 실패하면 자동으로 sherpa-onnx 원본 주소로 전환합니다. 중단된 내려받기는 이어서 받을 수 있습니다. GitHub에 접속할 수 없으면 다른 기기에서 모델 압축 파일 (카탈로그의 미러 또는 원본 주소)을 내려받은 뒤 홈 화면의 로컬 모델 가져오기를 사용하세요. 카탈로그와 일치하는 파일은 카탈로그 모델로 등록됩니다.

#### 모델은 공간을 얼마나 차지하고, 어떻게 확보하나요?

각 모델의 설치 후 크기는 위의 모델 표에 있습니다. 홈 화면에서 더 이상 필요 없는 모델을 삭제할 수 있고, 설정의 참조되지 않는 파일 정리로 어떤 모델에도 필요 없는 내려받기 파일, 가져오기 사본, 중단된 설치를 지울 수 있습니다 (설치된 모델과 일시 중지한 내려받기는 남습니다). 플러그인을 제거하면 모든 모델이 삭제됩니다.

#### 인식 결과에 문장 부호가 없는 이유는 무엇인가요?

스트리밍 Zipformer 모델은 문장 부호 없이 단어만 출력합니다. SenseVoice 모델은 음성 구간 단위로 인식하며 문장 부호를 붙이고 언어도 알려 주므로, 완전한 문장이 필요할 때 알맞습니다. 스크립트에서는 `model` 옵션으로 지정하거나 설정에서 기본 인식 모델로 지정하세요.

#### 호출어는 어떻게 쓰고, 왜 깨어나지 않나요?

키워드를 일반 텍스트로 씁니다. 중국어는 흔한 읽기로 병음 철자가 되고, 영어 단어는 모델 사전으로 철자가 되며 (사전에 없는 단어는 거부됩니다), 읽기가 여러 개인 한자는 모델의 병음 토큰을 직접 써서 읽기를 지정할 수 있습니다. 3~4음절이고 흔하지 않은 표현일수록 잘못 깨어나는 일이 적습니다. 키워드마다 `threshold` (낮을수록 쉽게 깨어남)와 `boost`를 지정할 수 있습니다. 스크립트에 쓰기 전에 체험하기 화면의 호출어 시험에서 먼저 시험해 보세요.

#### APK가 GPL-3.0으로 배포되는 이유와 소스 위치는요?

플러그인 자체 코드는 MPL-2.0입니다. sherpa-onnx 공식 Android 라이브러리가 eSpeak NG (GPL-3.0-or-later)를 정적으로 링크하므로 APK 전체는 GPL-3.0-or-later로 배포됩니다. 모든 릴리스에는 대응 소스 압축 파일 (sherpa-onnx 소스와 빌드 스크립트, 그리고 모든 의존성)이 첨부되며, 파일 이름에 `corresponding-source`가 들어가고 SHA-256은 `SHA256SUMS`에 있습니다. 플러그인 소스는 릴리스 태그 시점의 이 저장소입니다.

******

### 권한과 보안

******

플러그인은 권한 범위를 최대한 좁게 유지합니다:

- 마이크: 받아쓰기, 호출어, 체험 화면에서만 녹음하며, 세션이 끝나거나 스크립트가 종료되면 즉시 마이크를 해제합니다.
- 네트워크: 모델 다운로드와 서명된 모델 카탈로그 새로 고침에만 사용하며, 인식과 합성은 오프라인으로 동작합니다.
- 저장소: 모델은 플러그인 전용 디렉터리에 저장하고 로컬 모델은 시스템 파일 선택기로 가져오므로 저장소 권한을 요청하지 않습니다.
- 보호된 진입점: 기능 서비스와 활성화 진입점은 AutoJs6 플러그인 권한 (`org.autojs.permission.PLUGIN`)으로 보호되어 다른 앱이 직접 호출할 수 없습니다. 시스템 TTS 엔진과 그 음성 확인, 예시 문장, 설정 진입점은 Android 규칙에 따라 모든 앱에 열려 있으며 음성 합성만 제공합니다.
- 시스템 음성 입력: 시스템 TTS 엔진처럼 모든 앱에 열려 있습니다; 패널이 화면에 있는 동안에만 녹음하고 인식된 텍스트만 돌려주며, 설정에서 끌 수 있습니다.
- 디지털 어시스턴트: 화면 내용을 읽지 않으며 평소에는 백그라운드에서 듣지 않습니다. 패널이 화면에 있는 동안에만 녹음하고, 인식된 텍스트는 앱 버튼을 누르거나 복사할 때만 패널 밖으로 나갑니다(잠금 화면에서는 잠금 해제 후). 음성 인식 서비스는 마이크 권한을 직접 가진 앱을 위해서만 인식합니다.

******

### 플러그인 인터페이스

******

다음 정보는 AutoJs6 호스트와 플러그인 개발자를 위한 것으로, 호스트는 이 식별자들로 플러그인을 발견하고 기능을 협상합니다:

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

### 감사의 말

******

3-Macaw Speech는 다음 오픈 소스 프로젝트, 모델, 데이터 위에 만들어졌습니다. 작성자와 관리자 여러분께 감사드립니다:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)와 [k2-fsa (차세대 Kaldi)](https://github.com/k2-fsa) 팀: 인식, 호출어, 음성 구간 검출, 합성의 런타임, 각 모델의 ONNX 내보내기와 Android 라이브러리 (Apache-2.0).
- [icefall](https://github.com/k2-fsa/icefall)로 학습한 모델: Zipformer 스트리밍 인식 모델 (중영 2개 언어 pfluo, 영어 Zengwei, 중국어 2025 yuekai), 개방 어휘 호출어 모델 (pkufool), 그리고 k2-fsa의 AISHELL-3 VITS와 Baker Matcha-TTS 합성 모델.
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / Alibaba, FunASR 모델 라이선스 1.1): 다국어 오프라인 인식 모델.
- 합성 모델 [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, 중국어 녹음은 LongMaoData 제공), [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT)와 [HiFi-GAN](https://github.com/jik876/hifi-gan) 보코더 (MIT).
- 학습 데이터: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), [Data-Baker 중국어 표준 여성 음성 말뭉치](https://www.data-baker.com/open_source.html) (비상업적 이용만), Lessac Technologies의 Blizzard 2013 데이터 (연구용만), [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (퍼블릭 도메인).
- 추론 엔진 [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT), 음성 구간 검출 모델 [silero VAD](https://github.com/snakers4/silero-vad) (MIT), 음소 변환의 [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later)와 [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT), 중국어 호출어 읽기 표 [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT).

******

### 개발 로드맵

******

계획, 설계 결정, 진행 상황은 ROADMAP.md의 체크리스트로 관리합니다. 체크되지 않은 항목은 계획이며 현재 버전의 기능이 아닙니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.1.0

_2026/10/20_

- `기능` 패키징된 앱이 별도 진입점으로 음성 인식과 모델 관리를 사용하며 마이크 접근은 각 앱의 녹음 권한을 따름

#### v1.0.0

_2026/10/20_

- `힌트` 첫 정식 릴리스입니다. 스크립트의 speech 모듈에는 AutoJs6 6.8.0 (5456) 이상이 필요합니다. 플러그인 화면, 시스템 TTS 엔진, 시스템 음성 입력, 음성 인식 서비스, 디지털 어시스턴트는 AutoJs6에 의존하지 않습니다
- `힌트` 인식과 합성 모델은 패키지에 포함되지 않으며 플러그인 화면이나 스크립트에서 내려받습니다. 앱 전체는 GNU GPL 3.0 이상으로 배포되며 (음성 런타임이 eSpeak NG를 정적으로 링크하기 때문), 모든 릴리스에 대응 소스 코드가 함께 제공됩니다
- `기능` AutoJs6의 음성 플러그인: 플러그인 센터에서 활성화하면 스크립트가 speech 모듈로 음성 인식, 호출어, 음성 구간 검출, 음성 합성, 모델 관리를 사용할 수 있습니다
- `기능` sherpa-onnx 1.13.8 음성 런타임과 silero VAD v5 음성 구간 감지 모델을 내장하고 4가지 ABI별로 설치 파일 제공
- `기능` 모델 관리: 서명된 모델 카탈로그의 오프라인 음성 모델을 AutoJs6 공유 리소스 릴리스에서 다운로드하고, 사용할 수 없으면 sherpa-onnx 공식 릴리스로 전환합니다. 모든 파일을 SHA-256으로 검증한 뒤 사용하며, 이어받기, 로컬 가져오기 (tar, tar.bz2, zip), 삭제, 기본 모델을 지원합니다. 비상업적 용도로 제한되거나 라이선스가 밝혀지지 않은 모델은 먼저 라이선스에 동의해야 합니다
- `기능` 음성 인식: WAV 파일 (8 / 16 / 24 / 32 비트 정수 또는 부동소수점, 모든 샘플링 레이트와 채널 수, 플러그인 안에서 16 kHz 모노로 리샘플링) 을 인식하고 시작과 끝 시간이 있는 음성 구간을 반환하며, SenseVoice 모델은 언어, 감정, 이벤트 태그도 반환합니다. 스트리밍 모델은 핫워드를 지원합니다
- `기능` 받아쓰기 세션: 플러그인이 직접 녹음하거나 전달받은 16 kHz PCM 으로 준비, 중간 결과, 최종 결과, 끝점, 음성 구간, 종료를 차례로 알립니다. 문장 끝으로 볼 무음 길이, 무음 시간 초과, 최대 시간을 조정할 수 있으며, 스트리밍 모델은 단어마다, 오프라인 모델은 음성 구간마다 중간 결과를 갱신합니다
- `기능` 음성 구간 감지: 내장 silero VAD 로 WAV 파일의 음성 구간을 나열합니다
- `기능` 호출어: 어휘 제한이 없는 키워드 감지입니다. 중국어 키워드는 일반적인 읽기의 병음으로, 영어 단어는 모델 사전의 음소로 표기하며, 모델 토큰을 직접 쓸 수도 있습니다. 감지 후 같은 녹음에서 받아쓰기를 한 번 이어서 할 수 있습니다
- `기능` 마이크 세션 동안 플러그인은 마이크 포그라운드 서비스와 알림으로 녹음을 계속하므로, AutoJs6 가 포그라운드에 있을 때 시작한 세션은 AutoJs6 가 백그라운드로 가거나 화면이 꺼져도 계속됩니다. 마이크 세션은 한 번에 하나만 실행되며, 마이크 권한이 없으면 PERMISSION_DENIED 를 보고하고 권한 화면을 알려 줍니다
- `기능` 음성 합성: 설치된 합성 모델 (VITS, Matcha, Kokoro. 카탈로그에는 중국어와 영어 모델 5 종이 있습니다)로 최대 8000 자의 텍스트를 문장 단위로 WAV 파일에 합성하며, 샘플링 레이트는 모델과 같습니다. 화자와 속도 (0.5 배에서 2 배)를 고를 수 있고, 진행률은 완료된 글자 수로 알리며, 언제든 취소할 수 있습니다. 화자가 한 명인 모델은 선택한 화자를 무시하고 이를 알려 줍니다
- `기능` 시스템 TTS 엔진: 합성 모델을 설치하면 시스템의 텍스트 음성 변환 설정에서 3-Macaw Speech 를 선택할 수 있고, AutoJs6 의 tts 모듈과 다른 앱이 이를 사용해 읽어 줍니다. 화자마다 음성이 하나씩 있고 이중 언어 모델에는 화자마다 영어 음성이 더해지며, 말하기 속도는 시스템 또는 앱 설정을 따릅니다. 설치된 모델이 없으면 엔진은 음성 데이터가 없다고 보고합니다
- `기능` 홈 화면: 음성 런타임 버전, 설치된 모델 수, 마이크 권한 (버튼으로 바로 허용 가능), 3-Macaw Speech 가 시스템의 기본 TTS 엔진인지 여부를 표시합니다
- `기능` 홈 화면의 모델은 인식, 호출어, 합성, 음성 구간 감지로 나뉘어 표시되며 크기, 언어, 라이선스를 확인할 수 있습니다. 다운로드, 일시중지, 재개, 취소, 삭제, 기본값으로 설정을 할 수 있고, 다운로드 진행률은 알림에도 표시됩니다. 비상업적 용도로 제한되거나 라이선스가 밝혀지지 않은 모델은 먼저 라이선스에 동의해야 합니다. 모바일 데이터 같은 종량제 네트워크에서는 다운로드를 시작하기 전에 확인합니다
- `기능` 로컬 모델 가져오기: tar, tar.bz2, zip 압축 파일이나 폴더에서 가져옵니다. 카탈로그의 모델과 일치하는 파일은 그 모델로 등록되고, 그 밖의 파일은 종류, 모델 유형, 이름, 언어, ID 를 입력하면 사용자 모델로 추가됩니다
- `기능` 체험하기: 실시간 받아쓰기 (중간 결과는 회색으로 표시)에서 모델과 언어를 고를 수 있고, 호출어 체험은 감지된 키워드와 점수를 목록으로 보여 줍니다. 입력한 텍스트를 모델, 화자, 속도를 골라 음성으로 합성한 뒤 재생하거나 WAV 파일로 저장할 수 있습니다. 화면을 벗어나면 마이크를 해제합니다
- `기능` 시스템 음성 입력: 3-Stove Agent 등 다른 앱의 마이크 버튼에서 3-Macaw Speech의 오프라인 인식을 사용할 수 있습니다. 하단 패널에서 한 문장을 말하면 인식된 텍스트가 바로 그 앱으로 돌아갑니다; 멈추면 자동으로 끝나며 중간에 중지하거나 취소할 수도 있습니다; 설정의 엔드포인트 민감도와 음성 구간 감지를 따르며, 필요 없으면 설정의 인식 그룹에서 끌 수 있습니다
- `기능` 디지털 어시스턴트: 시스템 설정에서 3-Macaw Speech를 기본 디지털 어시스턴트 앱으로 선택하면 전원 버튼이나 홈 버튼을 길게 누르거나 어시스턴트 동작으로 하단 패널을 열어 한 문장을 말할 수 있습니다. 인식된 텍스트는 편집할 수 있고, 버튼을 눌러 3-Stove Agent나 3-Stone AI처럼 음성 명령을 받는 앱에 넘기거나, 복사하거나, 다시 말할 수 있습니다. 버튼을 누르기 전에는 아무것도 보내지 않습니다. 잠금 화면에서도 사용할 수 있으며 보내거나 복사하기 전에 잠금을 해제합니다. 화면 내용을 읽지 않으며 평소에는 백그라운드에서 듣지 않습니다. 설정의 인식 그룹에서 기본 어시스턴트인지 확인하고 시스템 설정을 열어 선택할 수 있습니다
- `기능` 시스템 음성 인식 서비스: Android 음성 인식(SpeechRecognizer)을 쓰는 앱은 3-Macaw Speech를 직접 지정해 중간 결과와 함께 오프라인 인식을 사용할 수 있으며, 기본 음성 인식 서비스로 설정하면 지정하지 않은 앱도 이를 사용합니다. Android 11 이하에서는 디지털 어시스턴트로 선택하면 보통 기본 서비스로도 설정되고, Android 12부터는 시스템의 음성 입력 설정에서 따로 선택해야 하며, 이 설정이 없는 휴대전화도 있습니다. 다른 앱(예: 3-Stone AI)이 3-Macaw Speech로 호출어를 계속 듣는 동안에도 디지털 어시스턴트, 시스템 음성 입력, 음성 인식 서비스, 체험 화면은 마이크를 사용할 수 있으며, 그동안 호출어 감지는 일시 중지되었다가 끝나면 자동으로 다시 시작됩니다
- `기능` 설정: 언어, 야간 모드, 테마 색상, 런처 아이콘 (기본값은 AutoJs6 따르기). 체험하기 화면과 시스템 TTS 엔진에 쓰이는 기본 인식 모델과 합성 모델, 엔드포인트 민감도, 음성 구간 감지, 화자, 속도, 언어별 기본 음성 (스크립트는 문서에 나온 기본값을 그대로 사용). 유휴 모델을 해제하는 시간, 모델 카탈로그 새로 고침, 남은 파일 정리. 시스템의 텍스트 음성 변환 설정에서 3-Macaw Speech 의 설정 버튼을 누르면 이 화면이 열립니다
- `기능` 정보: 버전, 플러그인에 포함된 릴리스 기록, 업데이트 확인 (자동 확인은 최대 12시간마다 한 번, 특정 릴리스를 무시할 수 있음), 라이선스 및 고지. 앱 전체는 GNU GPL 3.0 이상으로 배포되며 어떠한 보증도 제공하지 않습니다. 서드파티 고지, GPL 전문, Unicode 이용 약관, 대응 소스 코드의 위치도 확인할 수 있습니다
- `기능` 런처 아이콘: 적응형 아이콘 (밝게), 적응형 아이콘 (어둡게), 적응형 아이콘 (자동, 기본값), 투명 배경 아이콘 중에서 고를 수 있습니다
- `기능` 화면, README, 플러그인 센터 안내, 변경 기록이 10개 언어를 지원합니다: 중국어 간체, 중국어 번체 (홍콩, 대만), 영어, 프랑스어, 스페인어, 일본어, 한국어, 러시아어, 아랍어
- `의존성` sherpa-onnx 1.13.8 추가
- `의존성` Jetpack Compose BOM 2026.09.00 추가
- `의존성` AndroidX Activity Compose 1.12.2 추가
- `의존성` AndroidX Core KTX 1.15.0 추가
- `의존성` Kotlin Coroutines 1.11.0 추가
- `의존성` Material Color Utilities (MaterialKolor) 4.1.1 추가

##### 더 많은 릴리스 기록은 다음을 참고

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드

******

이 절은 소스에서 플러그인을 빌드하려는 개발자를 위한 것입니다.

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

### 라이선스

******

플러그인 소스 코드는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE)입니다. sherpa-onnx 공식 Android 라이브러리 (Apache-2.0)가 GNU GPL 버전 3 이상인 eSpeak NG와 UCD Tools를 정적으로 링크하므로, 배포하는 APK 전체는 GPL-3.0-or-later로 배포되며 (MPL-2.0 3.3절이 이 조합을 허용합니다) 어떠한 보증도 없습니다. 모든 릴리스에는 대응 소스 `autojs6-plugin-three-macaw-speech-corresponding-source-sherpa-onnx-v1.13.8-ac68c80b.tar`가 첨부됩니다: sherpa-onnx 1.13.8의 소스와 빌드 스크립트, 그리고 그 빌드가 URL과 SHA-256으로 고정하는 모든 의존성 압축 파일입니다. SHA-256은 릴리스의 `SHA256SUMS`에 있으며, `.python/corresponding_source.py`로 다시 만들고 검증할 수 있습니다. ONNX Runtime과 silero VAD는 MIT 라이선스이며, 각 모델의 라이선스는 위의 모델 표에 있습니다. 플러그인 정보 페이지에서 라이선스 전문과 타사 고지를 볼 수 있습니다.

- [제3자 고지 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### 관련 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 문서: speech 모듈: https://docs.autojs6.com/#/speech
- sherpa-onnx 프로젝트: https://github.com/k2-fsa/sherpa-onnx
