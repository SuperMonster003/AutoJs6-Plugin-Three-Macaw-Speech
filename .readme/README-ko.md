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

플러그인은 별도로 설치하는 APK입니다. AutoJs6 스크립트는 전역 모듈 `speech`로 이 기능을 사용합니다. 음성 합성은 Android 시스템 TTS 엔진으로도 등록되므로 AutoJs6의 `tts` 모듈과 다른 앱에서도 선택할 수 있습니다. 인식 모델과 합성 모델은 APK에 포함되지 않고 플러그인 화면이나 스크립트에서 필요할 때 내려받으며, 음성 구간 감지 모델만 기본으로 포함됩니다.

> 현재는 개발 미리보기 버전입니다: AutoJs6가 플러그인을 인식하고 플러그인은 음성 런타임을 불러오지만, 인식, 호출어, 합성, 모델 다운로드, 플러그인 화면, `speech` 스크립트 모듈은 아직 제공되지 않습니다. 현재 아이콘은 임시 디자인이며 이후 교체됩니다. 아래 내용은 1.0.0의 설계 목표이며, 진행 상황은 로드맵을 참고하세요.

******

### 기능 하이라이트

******

- 음성 인식: 마이크 스트리밍 받아쓰기 (부분 결과와 최종 결과를 실시간으로 반환)와 WAV 파일 인식을 지원하며, 중국어, 영어, 중영 혼합, 일본어, 한국어, 광둥어 오프라인 모델을 제공합니다.
- 음성 구간 감지: 내장된 silero VAD v5가 오디오를 음성 구간으로 나누며, 받아쓰기의 자동 문장 끊기에도 사용됩니다.
- 호출어: 키워드를 그대로 입력하면 되고 (예: `小六小六` 또는 `hey macaw`) 모델을 학습할 필요가 없습니다. 호출 후 자동으로 받아쓰기로 넘어갈 수 있습니다.
- 음성 합성: 여러 중국어와 영어 신경망 음성을 제공하며, WAV 파일로 합성하거나 시스템 TTS 엔진으로서 AutoJs6의 `tts` 모듈과 다른 앱에서 사용할 수 있습니다.
- 모델 관리: 서명된 모델 카탈로그에서 내려받고 파일마다 SHA-256 검증을 통과한 뒤에 활성화하며, 로컬 모델 가져오기도 지원합니다. 비상업적 용도로 제한된 모델은 내려받기 전에 라이선스를 안내합니다.

******

### 사용 방법

******

1. AutoJs6를 내부 버전 5448 (6.8.0) 이상으로 업데이트합니다.
2. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases)에서 기기 ABI에 맞는 APK를 내려받아 설치하고 (확실하지 않으면 `universal` 선택), AutoJs6 플러그인 센터에서 승인하고 활성화합니다.
3. 플러그인 화면에서 마이크 권한을 허용하고 필요한 모델을 내려받은 뒤, 스크립트에서 `speech` 모듈을 사용합니다.

> 플러그인은 Android 7.0 (API 24) 이상의 기기를 지원합니다. ABI별 설치 파일과 모든 기기에서 쓸 수 있는 `universal` 설치 파일이 있습니다.

******

### 모델

******

인식, 호출어, 합성 모델은 각각 30 MB에서 350 MB에 달하므로 APK에 포함되지 않습니다. 플러그인은 AutoJs6 공유 자산 저장소의 변경할 수 없는 릴리스에서 모델을 내려받고, 미러를 쓸 수 없으면 sherpa-onnx 원본 주소로 전환합니다. 두 경로 모두 모든 파일이 SHA-256 검증을 통과해야 모델을 활성화합니다.

모든 모델에는 라이선스가 표시됩니다. 비상업적 용도로 제한된 모델 (예: DataBaker 말뭉치로 학습한 합성 모델)은 내려받기 전에 안내가 표시되며, 스크립트에서 내려받을 때는 `acceptLicense: true`를 명시적으로 전달해야 합니다.

******

### 스크립트 API

******

스크립트는 전역 모듈 `speech` (`$speech`로도 사용 가능)로 이 기능을 사용합니다 (1.0.0 설계 초안이며, 출시 버전의 문서가 우선합니다):

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

- `speech.listen(options)`는 받아쓰기 세션을 열고 `partial`, `final`, `end` 등의 이벤트로 결과를 전달합니다. `speech.recognize(path)`는 WAV 파일을 인식합니다.
- `speech.wake(keywords, options)`는 호출어 세션을 엽니다. `thenListen`을 지정하면 호출 후 자동으로 받아쓰기로 넘어갑니다.
- `speech.synthesize(text, path)`는 WAV 파일로 합성합니다. `speech.speak(text)`와 `tts(text, {engine})`는 시스템 TTS 엔진으로 읽어 줍니다. `speech.models`는 모델을 관리합니다.

******

### 자주 묻는 질문

******

#### 인식과 합성에 인터넷 연결이 필요한가요?

필요하지 않습니다. 모델을 내려받은 뒤에는 모든 처리가 기기에서 이루어지며 음성은 업로드되지 않습니다. 모델을 내려받거나 모델 카탈로그를 새로 고칠 때만 연결합니다.

#### 왜 플러그인이 직접 마이크 권한을 요청하나요?

녹음은 플러그인 프로세스 안에서 이루어지며 음성이 AutoJs6를 거치지 않습니다. AutoJs6는 플러그인을 바인딩할 때 포그라운드 권한을 넘겨주므로 스크립트는 백그라운드에서도 받아쓰기를 할 수 있습니다. 처음 사용하기 전에 플러그인 화면에서 마이크 권한을 허용하세요.

#### 어떤 설치 파일을 설치해야 하나요?

대부분의 휴대전화는 `arm64-v8a`입니다. 기기 ABI를 모르면 `universal`을 설치하세요. 모든 기기에서 동작하지만 용량이 더 큽니다.

******

### 권한과 보안

******

플러그인은 권한 범위를 최대한 좁게 유지합니다:

- 마이크: 받아쓰기, 호출어, 체험 화면에서만 녹음하며, 세션이 끝나거나 스크립트가 종료되면 즉시 마이크를 해제합니다.
- 네트워크: 모델 다운로드와 서명된 모델 카탈로그 새로 고침에만 사용하며, 인식과 합성은 오프라인으로 동작합니다.
- 저장소: 모델은 플러그인 전용 디렉터리에 저장하고 로컬 모델은 시스템 파일 선택기로 가져오므로 저장소 권한을 요청하지 않습니다.
- 보호된 진입점: 기능 서비스와 활성화 진입점은 AutoJs6 플러그인 권한 (`org.autojs.permission.PLUGIN`)으로 보호되어 다른 앱이 직접 호출할 수 없습니다.

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
minimum host build: 5448
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### 개발 로드맵

******

계획, 설계 결정, 진행 상황은 ROADMAP.md의 체크리스트로 관리합니다. 체크되지 않은 항목은 계획이며 현재 버전의 기능이 아닙니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.0.0

_2026/10/16_

- `힌트` 개발 미리보기: AutoJs6가 플러그인을 인식하고 플러그인은 음성 런타임을 불러옵니다. 음성 인식, 호출어, 음성 합성, 모델 다운로드, 플러그인 화면, speech 스크립트 모듈은 아직 제공되지 않습니다
- `힌트` AutoJs6 6.8.0 (5448) 이상이 필요합니다 (정확한 최소 버전은 호스트 쪽 변경이 반영된 뒤 기입됩니다)
- `힌트` 현재 아이콘은 임시 디자인이며 이후 교체됩니다
- `기능` 플러그인 골격: Wake 활성화 프로토콜, 플러그인 센터 식별 서비스, 음성 기능 서비스
- `기능` sherpa-onnx 1.13.8 음성 런타임과 silero VAD v5 음성 구간 감지 모델을 내장하고 4가지 ABI별로 설치 파일 제공
- `기능` 10개 언어의 README, 플러그인 센터 안내, 변경 기록을 JSON 원본에서 일괄 생성
- `의존성` sherpa-onnx 1.13.8 추가
- `의존성` Jetpack Compose BOM 2026.09.00 추가
- `의존성` AndroidX Activity Compose 1.12.2 추가
- `의존성` AndroidX Core KTX 1.15.0 추가
- `의존성` Kotlin Coroutines 1.11.0 추가

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
```

******

### 라이선스

******

프로젝트 코드는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE)으로 배포됩니다. 음성 런타임 sherpa-onnx는 Apache-2.0 라이선스, onnxruntime과 silero VAD는 MIT 라이선스를 사용하며, 각 모델의 라이선스는 모델 카탈로그에 개별적으로 표시됩니다.

- [제3자 고지 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### 관련 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
- sherpa-onnx 프로젝트: https://github.com/k2-fsa/sherpa-onnx
