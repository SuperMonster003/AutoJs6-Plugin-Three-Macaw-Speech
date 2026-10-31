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

- 음성 인식: 마이크 스트리밍 받아쓰기 (부분 결과와 최종 결과를 실시간으로 반환)와 오디오 파일 인식 (WAV, MP3, AAC, Ogg, FLAC 등, 동영상 파일의 소리도 가능)을 지원하며, 중국어, 영어, 중영 혼합, 일본어, 한국어, 광둥어 오프라인 모델을 제공합니다. 프랑스어, 러시아어, 한국어는 말하는 동안 글자가 나오는 스트리밍 모델도 있습니다. 1.3.0에서는 스페인어 오프라인 모델 (문장 부호 포함)과, 언어를 자동으로 판별하는 8개 언어 스트리밍 모델 (아랍어, 인도네시아어, 태국어, 베트남어, 일본어, 러시아어, 중국어, 영어)을 추가했습니다.
- 자동 문장 부호: 문장 부호 모델을 설치하면 스트리밍 모델이 인식한 각 문장에 쉼표, 마침표, 물음표가 붙고 (중국어와 영어), 영어는 대문자도 복원됩니다. SenseVoice는 자체 문장 부호를 붙이므로 바뀌지 않습니다. 문장 부호 모델은 홈 화면의 "문장 부호" 그룹에서 내려받습니다.
- 음성 구간 감지: 내장된 silero VAD v5가 오디오를 음성 구간으로 나누며, 받아쓰기의 자동 문장 끊기에도 사용됩니다.
- 호출어: 키워드를 그대로 입력하면 되고 (예: `小六小六` 또는 `hey macaw`) 모델을 학습할 필요가 없습니다. 호출 후 자동으로 받아쓰기로 넘어갈 수 있습니다.
- 음성 합성: 여러 중국어와 영어 신경망 음성을 제공하며, WAV 파일로 합성하거나 시스템 TTS 엔진으로서 AutoJs6의 `tts` 모듈과 다른 앱에서 사용할 수 있습니다. 프랑스어, 스페인어, 독일어, 러시아어 Piper 음성도 있습니다 (비상업적 이용만). 1.3.0에서는 Kokoro v1.0 다국어 음성 (영어, 스페인어, 프랑스어, 힌디어, 이탈리아어, 브라질 포르투갈어, 중국어)과 아랍어, 한국어 음성 (비상업적 이용만)을 추가했습니다.
- 시스템 음성 입력: 3-Stove Agent 등 다른 앱의 마이크 버튼에서 오프라인 인식을 사용할 수 있습니다. 하단 패널에서 한 문장을 말하면 인식된 텍스트가 그 앱으로 돌아갑니다.
- 디지털 어시스턴트: 3-Macaw Speech를 기본 디지털 어시스턴트 앱으로 선택한 뒤 전원 버튼이나 홈 버튼을 길게 눌러 한 문장을 말하고, 필요하면 인식된 텍스트를 편집해 3-Stove Agent, 3-Stone AI 등 음성 명령을 받는 앱에 넘기거나 복사합니다. 시스템 음성 인식 서비스도 제공합니다. Android 11 이하에서는 어시스턴트로 선택하면 보통 기본 음성 인식 서비스로도 설정되고, Android 12부터는 시스템의 음성 입력 설정에서 따로 선택해야 하며, 이 설정이 없는 휴대전화도 있습니다. 앱이 직접 지정할 수도 있습니다.
- 실행 방법: 전원 버튼을 길게 누르지 않아도 같은 받아쓰기 패널을 열 수 있습니다: 빠른 설정 타일 (잠금 화면에서도 사용 가능), 홈 화면 위젯과 앱 아이콘 바로가기, 끌어서 옮길 수 있는 플로팅 마이크 (기본값 꺼짐), 볼륨 키를 길게 누르거나 두 번 눌러 여는 키 트리거 (키 입력만 다루는 접근성 서비스, 기본값 꺼짐), 블루투스 헤드셋의 음성 키. 평소에는 녹음하지 않으며 패널에서 받아쓰는 몇 초 동안만 마이크를 사용합니다.
- 상시 호출어 (기본값 꺼짐): 켜 두면 호출어 (기본값 `小鹦小鹦`과 `Hey Macaw`, 변경 가능)를 말하기만 해도 같은 받아쓰기 패널이 열리고, 화면이 꺼져 있거나 잠겨 있어도 화면을 켜며, 인식한 텍스트를 읽어 줍니다. 듣는 동안 마이크 표시등이 계속 켜지고 알림이 항상 표시되며 배터리도 더 소모됩니다 (기본값으로 켜져 있는 절전 모드는 조용할 때 호출어 모델을 실행하지 않아 CPU 사용이 훨씬 적습니다). 듣기 시간 (10, 30, 60분 후 자동으로 끄기 또는 항상 켜 두기)과 충전 중에만 듣기를 설정할 수 있습니다. 스크립트, 다른 앱 또는 다른 실행 방법이 마이크를 쓰면 잠시 양보하고 끝나면 다시 듣습니다.
- 화자 인식 (1.3.0): 설정 > 상시 호출어 > 화자에서 이름을 입력하고 문장 3개를 따라 읽으면 자신의 목소리를 등록할 수 있습니다 (여러 사람을 등록할 수 있고, 인식 시험으로 점수도 볼 수 있습니다). "등록된 목소리만"을 켜면 상시 호출어가 먼저 목소리를 비교해 등록된 사람과 일치할 때만 패널을 열기 때문에, 다른 사람이 호출어를 말하면 무시됩니다. 타일, 위젯, 헤드셋 키 등 수동 실행 방법은 영향을 받지 않습니다. 스크립트에서도 `speech.speaker`로 화자를 등록, 확인, 식별할 수 있습니다 (AutoJs6 6.8.0 이상). 화자 인식 모델은 약 26.5 MB이며 "화자" 페이지의 안내에 따라 내려받습니다.
- 오디오 분석 (1.3.0): 체험하기 화면의 파일 인식에서 화자 분리 (누가 언제 말했는지 표시하고, 등록된 목소리는 이름도 보여 줍니다. "화자 분리 + 받아쓰기"는 각 사람의 말을 글자로도 옮깁니다), 언어 식별 (Whisper의 99개 언어), 오디오 태그 (사람 목소리, 음악, 동물, 차량, 경보 등 AudioSet의 527종)도 할 수 있습니다. 모델을 내려받으면 모두 휴대폰에서 오프라인으로 처리합니다 (필요한 모델은 화면에 표시됩니다). 스크립트에서는 `speech.diarize`, `speech.detectLanguage`, `speech.tag`로 사용할 수 있습니다 (AutoJs6 6.8.0 이상).
- 오디오 재생 (1.4.0): 체험하기 화면의 파일 인식에서 선택한 파일을 위치 이동, 0.5배부터 2배까지 속도 변경, 5초 앞뒤 이동과 함께 재생합니다. 재생 중인 문장은 강조되고, 문장을 누르면 그 위치부터 재생하며, 큰 글자의 가사 보기로 전환할 수 있습니다. 플러그인은 최근 파일 하나의 읽기 권한만 유지하며, 결과를 지우거나 다른 파일을 고르면 놓습니다.
- 플러그인 도구 (AutoJs6 6.8.0 이상): `speech_listen` (한 문장을 듣고 인식한 텍스트를 반환), `speech_speak` (텍스트를 소리 내어 읽음), `speech_transcribe` (휴대폰의 오디오 파일을 인식), `speech_synthesize` (텍스트를 WAV 또는 M4A 파일로 합성) 네 가지 도구를 제공합니다. MCP Server 플러그인은 이 도구로 컴퓨터의 AI 어시스턴트가 휴대폰을 통해 듣고 말할 수 있게 하며, 스크립트에서도 `plugins.tools`로 호출할 수 있습니다. 1.3.0에서는 `speech_speaker_verify` (녹음이 등록된 특정 사람인지)와 `speech_speaker_identify` (등록된 사람 중 가장 비슷한 사람)를 추가했으며, 결과에는 이름과 점수만 있습니다. 1.3.0에서는 분석 도구 `speech_diarize` (화자 분리, 파일은 최대 10분), `speech_detect_language` (언어 식별), `speech_tag` (오디오 태그)도 추가했습니다.
- 모델 관리: 서명된 모델 카탈로그에서 내려받고 파일마다 SHA-256 검증을 통과한 뒤에 활성화하며, 로컬 모델 가져오기도 지원합니다. 비상업적 용도로 제한된 모델은 내려받기 전에 라이선스를 안내합니다.
- 독립 화면: 홈 화면에서 상태를 보고 모델을 관리하며, 체험하기 화면에서 받아쓰기, 호출어, 합성을 바로 시험하고 휴대폰의 오디오나 동영상 파일을 글자로 옮겨 텍스트나 SRT 자막으로 저장할 수 있으며, 설정에서 언어, 야간 모드, 테마 색상, 런처 아이콘, 인식과 합성의 기본값을 바꿉니다. 외관은 기본적으로 AutoJs6를 따릅니다.

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

인식, 문장 부호, 호출어, 합성, 화자, 오디오 분석 모델은 각각 6 MB에서 410 MB라서 APK에 포함하지 않습니다. 플러그인은 서명된 모델 카탈로그를 내장하고, AutoJs6 공유 자산 저장소의 변경할 수 없는 릴리스에서 모델을 내려받으며, 미러를 사용할 수 없으면 sherpa-onnx 원본 주소로 전환합니다. 두 경로 모두 모든 파일이 SHA-256 검사를 통과해야 모델을 사용합니다. 1.3.0 카탈로그에는 다음 모델이 있습니다 (크기는 설치 후 차지하는 공간입니다):

| 모델 ID | 용도 | 언어 | 크기 | 라이선스 | 상업적 이용 |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (추천) | 인식 (오프라인, 문장 부호 포함) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | 가능 |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (추천) | 인식 (스트리밍) | zh, en | 199.1 MB | Apache-2.0 | 가능 |
| `asr.zipformer-streaming.en.2023-06-26.int8` (추천) | 인식 (스트리밍) | en | 70.3 MB | Apache-2.0 | 가능 |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | 인식 (스트리밍) | zh | 167.4 MB | 선언 없음 | 비상업적 이용만 |
| `asr.zipformer-streaming.fr.2023-04-14.int8` (추천) | 인식 (스트리밍) | fr | 129.0 MB | Apache-2.0 | 가능 |
| `asr.zipformer-streaming.ru.2025-08-16.int8` (추천) | 인식 (스트리밍) | ru | 28.6 MB | Apache-2.0 | 가능 |
| `asr.zipformer-streaming.ko.2024-06-16.int8` | 인식 (스트리밍) | ko | 140.9 MB | Apache-2.0 | 비상업적 이용만 |
| `asr.nemo-transducer.es.2026-05-03.int8` (추천) | 인식 (오프라인, 문장 부호 포함) | es | 136.5 MB | CC-BY-4.0 | 가능 |
| `asr.zipformer-streaming.ar-en-id-ja-ru-th-vi-zh.2025-02-10.int8` | 인식 (스트리밍) | ar, en, id, ja, ru, th, vi, zh | 338.9 MB | Apache-2.0 | 알 수 없음 |
| `punct.ct-transformer.zh-en.2024-04-12.int8` (추천) | 문장 부호 | zh, en | 75.5 MB | Apache-2.0 | 가능 |
| `punct.cnn-bilstm.en.2024-08-06.int8` (추천) | 문장 부호 | en | 7.6 MB | Apache-2.0 | 가능 |
| `kws.zipformer.zh-en.2025-12-20` (추천) | 호출어 | zh, en | 8.8 MB | Apache-2.0 | 가능 |
| `tts.vits.melo.zh-en` (추천) | 합성 (화자: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | 가능 |
| `tts.vits.icefall.zh-aishell3` (추천) | 합성 (화자: 174, 8 kHz) | zh | 32.8 MB | 선언 없음 | 알 수 없음 |
| `tts.kokoro.multi-lang.v1_1` | 합성 (화자: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | 가능 |
| `tts.matcha.icefall.zh-baker` | 합성 (화자: 1, 22.05 kHz) | zh | 81.0 MB | 선언 없음, MIT (보코더) | 비상업적 이용만 |
| `tts.vits.piper.en-us-lessac-medium` | 합성 (화자: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 비상업적 이용만 |
| `tts.vits.piper.fr-fr-siwis-medium.int8` | 합성 (화자: 1, 22.05 kHz) | fr | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 비상업적 이용만 |
| `tts.vits.piper.es-es-davefx-medium.int8` | 합성 (화자: 1, 22.05 kHz) | es | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 비상업적 이용만 |
| `tts.vits.piper.de-de-thorsten-medium.int8` | 합성 (화자: 1, 22.05 kHz) | de | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 비상업적 이용만 |
| `tts.vits.piper.ru-ru-denis-medium.int8` | 합성 (화자: 1, 22.05 kHz) | ru | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | 비상업적 이용만 |
| `tts.kokoro.multi-lang.v1_0.int8` | 합성 (화자: 49, 24 kHz) | en, es, fr, hi, it, pt, zh | 168.9 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | 가능 |
| `tts.vits.piper.ar-jo-sa-dii-high.int8` | 합성 (화자: 1, 22.05 kHz) | ar | 36.6 MB | CC-BY-NC-SA-4.0, GPL-3.0-or-later (espeak-ng-data) | 비상업적 이용만 |
| `tts.vits.piper.ar-jo-sa-miro-v2-high.int8` | 합성 (화자: 1, 22.05 kHz) | ar | 36.6 MB | CC-BY-NC-ND-4.0, GPL-3.0-or-later (espeak-ng-data) | 비상업적 이용만 |
| `tts.vits.mimic3.ko-ko-kss-low` | 합성 (화자: 1, 22.05 kHz) | ko | 80.8 MB | CC0-1.0, GPL-3.0-or-later (espeak-ng-data) | 비상업적 이용만 |
| `speaker.eres2net.3dspeaker.en-voxceleb` (추천) | 화자 인식 | en, zh | 26.5 MB | Apache-2.0 | 가능 |
| `segment.pyannote.3-0` (추천) | 화자 분할 | 제한 없음 | 6.0 MB | MIT | 비상업적 이용만 |
| `langid.whisper.tiny.int8` (추천) | 언어 식별 | 99개 언어 | 102.8 MB | MIT | 알 수 없음 |
| `langid.whisper.base.int8` | 언어 식별 | 99개 언어 | 159.8 MB | MIT | 알 수 없음 |
| `tagger.ced.mini.int8` (추천) | 오디오 태그 | 제한 없음 | 10.5 MB | CC-BY-4.0, CC-BY-SA-4.0 (AudioSet labels) | 가능 |

비상업적 이용으로 제한되거나 라이선스를 알 수 없는 모델은 내려받기 전에 라이선스를 보여 주며, 스크립트에서 내려받을 때는 `acceptLicense: true`를 명시해야 합니다. 2025 중국어 스트리밍 모델은 가중치에 라이선스 선언이 없고 학습 데이터에 비상업적 이용 전용 말뭉치가 포함되어 있어, sherpa-onnx 원본 주소에서만 내려받고 공유 자산 저장소에는 미러하지 않습니다. 합성 모델 Kokoro와 Piper에는 eSpeak NG의 발음 데이터 (espeak-ng-data, GPL-3.0-or-later)가 포함됩니다. 한국어 스트리밍 모델의 학습 데이터 (KsponSpeech)는 한국 국민만 이용을 신청할 수 있고 국외에서 쓰려면 별도 협약이 필요하므로, 비상업적 이용으로 표시하고 미러하지 않으며 원본 주소에서만 내려받습니다 (내려받기 약 418 MB, 설치 후 141 MB). 프랑스어, 스페인어, 독일어, 러시아어 Piper 음성 4개는 모두 영어 lessac 음성에서 미세 조정한 것이라 lessac과 마찬가지로 비상업적 이용만 가능합니다. 8개 언어 스트리밍 모델 (PengChengStarling)의 가중치는 Apache-2.0이지만 배포자가 학습 데이터를 밝히지 않아 상업적 이용은 알 수 없음으로 표시합니다. 아랍어 dii와 miro 음성은 모두 CC BY-NC-ND 4.0인 합성 음성으로 학습했으며, dii 자체는 CC BY-NC-SA 4.0, miro는 CC BY-NC-ND 4.0입니다. miro는 변경이 허용되지 않으므로 원본 주소에서만 내려받고 공유 자산 저장소에는 미러하지 않습니다. 한국어 KSS 음성의 가중치는 CC0이지만 학습 데이터 (KSS)가 CC BY-NC-SA 4.0이므로 비상업적 이용으로 표시합니다. 화자 분할 모델 (pyannote segmentation 3.0)의 가중치는 MIT이지만 학습 데이터에 연구용으로만 쓸 수 있는 MSDWild가 포함되어 있어 비상업적 이용으로 표시합니다. Whisper 언어 식별 모델 2개의 가중치는 MIT이지만 OpenAI가 학습 데이터를 공개하지 않아 상업적 이용은 알 수 없음으로 표시합니다. CED-mini 오디오 태그 모델의 가중치는 CC BY 4.0이며, 함께 제공되는 AudioSet 클래스 표는 CC BY-SA 4.0입니다.

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

- `speech.listen(options)`는 받아쓰기 세션을 열고 `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error`, `end` 이벤트로 결과를 알리며, `stop()`으로 일찍 끝냅니다. `speech.recognize(path)`는 오디오 파일을 인식하고 (WAV 이외의 형식은 AutoJs6 6.8.0 이상 필요, 16 kHz PCM도 가능), `speech.detectVoice(path)`는 그 안의 음성 구간을 나열합니다.
- `speech.listen`과 `speech.recognize`의 `punctuation` 옵션은 기본값이 true입니다. 모델 자체의 문장 부호 (SenseVoice)는 그대로 두고, 스트리밍 모델의 결과에는 설치된 문장 부호 모델로 문장 부호를 붙이며 (중국어와 영어, 영어는 대소문자도 복원), 해당 언어를 다루는 문장 부호 모델이 없으면 그대로 둡니다. false로 하면 모든 문장 부호를 없앱니다. 문장 부호 모델은 플러그인 홈 화면에서만 관리하며 `speech.models`에는 나오지 않습니다.
- `speech.wake(keywords, options)`는 키워드를 일반 텍스트로 적은 호출어 세션을 엽니다 (최대 50개). `thenListen`을 지정하면 깨어날 때마다 같은 녹음에서 한 라운드의 받아쓰기로 넘어가며, 그 세션은 `wake` 이벤트의 `listen` 속성으로 전달됩니다.
- `speech.synthesize(text, path, options)`는 WAV 파일로 합성합니다 (최대 8000자, 모델, 화자, 속도 선택 가능). `speech.speak(text, options)`와 `tts(text, { engine })`는 시스템 텍스트 음성 변환 엔진으로 읽습니다.
- `speech.models`는 모델 목록, 내려받기 (`install`, 진행률 제공), 가져오기, 삭제와 기본 모델 지정을 맡습니다. `speech.capabilities()`는 플러그인 버전, 지원 기능, 마이크 권한 여부를 돌려줍니다.
- `speech.speaker.enroll(name, files, { replace })`는 화자를 등록합니다 (여러 녹음을 평균하며, 같은 이름은 기본적으로 성문에 더하고 `replace: true`면 새로 등록합니다). `verify(name, file)`는 점수와 일치 여부를 반환하고, `identify(file, { topK })`는 등록된 사람 중 가장 비슷한 사람을 찾아 후보를 나열하며, `list()`와 `remove(name)`로 등록된 사람을 관리합니다. `enroll`, `verify`, `identify`에는 각각 Promise를 반환하는 `...Async` 버전이 있습니다. 결과에는 이름, 점수, 일치 여부, 임계값 (기본값 0.50, `threshold`로 변경)만 있고 성문 벡터는 없습니다. AutoJs6 6.8.0 정식판과 화자 인식 모델이 필요합니다.
- `speech.diarize(file, options)`는 화자를 분리하여 시간순 구간 (`segments`, 각각 `startMillis`, `endMillis`, 0부터 번호를 매긴 `speaker`)과 인원 (`speakers`)을 반환합니다. 옵션 `speakers`로 인원을 지정할 수 있고 (0은 자동 판단, 최대 20), `identify: true`는 등록된 성문과 맞는 화자에 이름을 붙입니다 (`names`). `speech.detectLanguage(file, { windows })`는 음성 앞부분의 몇 개 구간 (각 30초, 기본 3개)을 문장별로 판별합니다. `language`는 가장 오래 말한 언어, `candidates`는 언어별 말한 시간의 비율, `segments`는 문장별 언어입니다. `speech.tag(file, { topK, timeline })`는 10초 구간마다 점수를 매겨 점수가 높은 라벨 (AudioSet의 영어 라벨 527개, 기본 5개)을 반환하며, `timeline: true`면 구간별 라벨도 붙습니다. 셋 모두 `onProgress(done, total)`로 진행 상황을 알리고, Promise를 반환하는 `...Async` 버전이 있습니다. AutoJs6 6.8.0 정식판과 해당 모델이 필요합니다.
- 플러그인 전체에서 동시에 하나의 마이크 세션 (받아쓰기 또는 호출어)만 열 수 있습니다. 마이크 권한이 없으면 호출이 `PERMISSION_DENIED`로 실패하며, 플러그인 홈 화면에서 허용할 수 있습니다. 인식된 텍스트는 AutoJs6 로그에 기록되지 않습니다.
- AutoJs6 6.8.0 이상에서는 스크립트에서도 `plugins.tools.call`로 이 플러그인 도구들를 호출할 수 있습니다 (예: `plugins.tools.call('speech_speak', { text: '안녕하세요' })`). 인수는 MCP Server가 보는 것과 같습니다. 평소 스크립트에서는 `speech` 모듈을 직접 쓰는 편이 더 유연합니다.
- 모든 메서드, 옵션, 이벤트, 오류 코드는 [AutoJs6 문서](https://docs.autojs6.com/#/speech)를 참고하세요. AutoJs6의 샘플 > 3-Macaw Speech에는 파일로 받아쓰기, 호출어와 받아쓰기, TTS 알림, 음성 명령을 Agent에 넘기기, 화자 인식, 화자 분리, 언어 식별, 오디오 태그 등 8개의 샘플이 있습니다.

******

### 호환성

******

- Android 7.0 (API 24) 이상, 대상 API 37. arm64-v8a, armeabi-v7a, x86_64, x86 패키지와 universal 패키지가 있으며, 네이티브 라이브러리는 16 KB 메모리 페이지에 맞춰 정렬되어 16 KB 페이지 에뮬레이터에서 검증했습니다.
- AutoJs6: 스크립트의 `speech` 모듈에는 6.8.0 (내부 버전 5456) 이상이 필요합니다. 플러그인 화면, 시스템 텍스트 음성 변환 엔진, 시스템 음성 입력, 음성 인식 서비스, 디지털 어시스턴트는 AutoJs6에 의존하지 않습니다. WAV가 아닌 오디오 파일 (MP3, M4A 등) 인식과 플러그인 도구에는 AutoJs6 6.8.0 정식판 이상이 필요합니다. 그보다 이전 AutoJs6에서는 스크립트가 WAV만 인식합니다. 스크립트의 `speech.speaker`, `speech.diarize`, `speech.detectLanguage`, `speech.tag`도 AutoJs6 6.8.0 정식판 이상이 필요합니다. 플러그인 안의 "화자" 페이지, "등록된 목소리만", 체험하기 화면의 파일 분석은 AutoJs6에 의존하지 않습니다.
- 백그라운드 듣기: AutoJs6가 포그라운드에 있을 때 시작한 받아쓰기나 호출어 세션은 AutoJs6가 백그라운드로 가거나 화면이 꺼져도 계속 듣습니다 (플러그인이 마이크 포그라운드 서비스와 알림으로 녹음을 유지합니다). AutoJs6가 이미 백그라운드에 있을 때 연 세션은 시스템이 음소거할 수 있으며, 이때 세션은 코드 `PERMISSION_DENIED`인 `warning` 이벤트를 받지만 끝나지 않고, AutoJs6가 포그라운드로 돌아오면 소리가 돌아옵니다. 백그라운드에서 들어야 하는 스크립트는 AutoJs6가 보이는 동안 세션을 여세요.
- 검증한 기기: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), 그리고 API 24, 35, 37 (16 KB 페이지) 에뮬레이터.
- 성능: 저사양 Sony G8441에서 스트리밍 모델은 약 1.7초, SenseVoice는 약 2.8초 만에 로드되고, 받아쓰기는 실시간보다 약 3배 빠르게 처리됩니다. 이런 기기에서 MeloTTS와 Kokoro는 실시간보다 느리게 합성하며 AISHELL-3가 가장 빠릅니다. Sony XQ-DQ72 같은 최신 기기는 약 0.5~1.2초 만에 로드하고 실시간보다 빠르게 합성합니다.
- 메모리: 로드된 인식 모델 하나는 약 210~300 MB의 네이티브 메모리를 사용합니다. 플러그인은 로드된 모델을 기기 메모리의 4분의 1 (384 MB~1.5 GB) 안에서 관리하고, 5분 동안 쓰지 않으면 해제합니다 (설정에서 1~30분으로 변경 가능).
- 디지털 어시스턴트: 시스템이 타사 디지털 어시스턴트 앱 선택을 허용해야 합니다. 호출 방법은 시스템마다 다릅니다: 홈 버튼 길게 누르기 (3버튼 탐색), 화면 아래 모서리에서 대각선 위로 스와이프, 또는 시스템 설정에서 전원 버튼 길게 누르기를 디지털 어시스턴트 호출로 지정. 테스트한 ColorOS와 HyperOS 태블릿 (Android 15)은 전원 버튼과 제스처를 자체 어시스턴트에 쓰므로 실행 방법의 입구를 사용하세요.
- 실행 방법: Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), Xiaomi Pad 23046RP50C (HyperOS, Android 15), API 33과 35 에뮬레이터에서 테스트했습니다. ColorOS와 HyperOS에서는 잠금 화면에서 타일을 탭하면 먼저 잠금 해제를 요구하고, 해제한 뒤 패널을 엽니다. 위젯 추가 요청을 받아들이면서 확인 창을 띄우지 않는 런처가 있으니 그때는 설정의 안내대로 직접 추가하세요. 화면이 꺼지면 휴대폰이 키 입력을 접근성 서비스에 넘기지 않으므로 키 트리거는 화면이 켜져 있을 때만 동작합니다. 블루투스 헤드셋 음성 키: Sony WF-1000XM4와 XQ-DQ72 (Android 13)에서는 길게 누를 때마다 패널이 열리고 헤드셋 마이크로 들었습니다. 다른 앱 (예: Google 앱)도 응답하면 시스템이 먼저 선택 화면을 보여 줍니다. HyperOS 태블릿 (Xiaomi Pad 23046RP50C, Android 15)에서는 선택 화면에서 3-Macaw Speech를 골라도 아무 반응이 없었습니다.
- 상시 호출어: Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), API 35 (16 KB 페이지) 에뮬레이터에서 실측. 화면을 끈 30분 동안의 소비 전력 (Sony XQ-DQ72, Android 13. 기기 전력 프로필에 따른 Android 추정치이며, USB로 연결한 채 충전하지 않는 상태를 모의함): 켜 두고 절전 모드도 켰을 때 (기본값) 17.1 mAh, 플러그인 CPU 시간 235초, 절전 모드를 껐을 때 17.9 mAh, 454초 (평균 CPU 코어 하나의 약 4분의 1)였고 두 경우 모두 그동안 계속 녹음했으며, 추정치 중 약 14 mAh는 녹음 자체의 몫입니다. 이 측정 동안 주변에서 계속 소리가 났기 때문에 절전 모드로 줄어든 것은 주로 CPU 시간입니다. 조용한 방 (Sony G8441, Android 9, 화면을 끈 10분)에서는 플러그인 CPU 시간이 265초에서 26초로 줄었습니다. 상시 호출어를 꺼 두었을 때는 0.006 mAh이며 플러그인이 실행되지 않았습니다. 실제 소비는 기기와 주변 소리에 따라 다릅니다.
- 플러그인 도구: Sony G8441 (Android 9), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), API 24, 35 에뮬레이터에서 테스트 (API 24에서는 모델이 필요 없는 부분만). MCP Server를 통한 호출은 Sony G8441 (Android 9)에서 테스트.
- 문장 부호와 1.2.0에 추가된 모델: Sony XQ-AT72 (Android 12)에서 테스트. 문장 부호를 켜면 중국어, 영어, 중영 혼합 녹음에 문장 부호가 붙고 단어는 껐을 때와 같았습니다. 러시아어와 프랑스어 Piper 음성으로 합성한 문장을 같은 언어의 스트리밍 모델로 다시 인식한 문자 오류율은 각각 7.9%와 0%였습니다. 스페인어와 독일어 음성은 정상적으로 합성되며, 한국어 모델은 불러오기만 확인했습니다 (한국어 테스트 녹음이 없음).
- 1.3.0에 추가된 모델: Sony G8441 (Android 9), XQ-DQ72 (Android 13), API 35 에뮬레이터 (설치, 사용, 삭제)에서 테스트. 스페인어, 아랍어, 한국어 테스트 녹음이 없어 합성한 문장을 인식 모델로 다시 인식했습니다. Kokoro v1.0 스페인어 음성을 NeMo로 다시 인식한 문자 오류율은 0%~6.5%, 아랍어 음성 2개를 8개 언어 모델로 다시 인식하면 모두 3.0% (에뮬레이터에서 miro는 33%), 한국어 음성을 SenseVoice로 다시 인식하면 0%였습니다. 8개 언어 모델의 영어 녹음 문자 오류율은 0%~1.1%입니다. 8개 언어 모델은 로드 후 최대 메모리가 약 600 MiB (G8441)~880 MiB (XQ-DQ72)이고, Kokoro v1.0은 G8441에서 실시간보다 느리게 (오디오 길이의 약 4~6배) 합성하며 XQ-DQ72에서는 거의 실시간입니다.
- 체험하기 화면의 파일 인식: Sony G8441 (Android 9), XQ-AT72 (Android 12), OnePlus OPD2413 (ColorOS, Android 15), API 24, 35 에뮬레이터에서 테스트. OnePlus OPD2413에서 10분 03초 길이의 MP3를 약 21초 만에 인식했습니다 (SenseVoice, 자동 문장 부호 켬). 화면을 떠나도 인식은 계속되며 진행 상황은 알림에 표시됩니다.
- 화자 인식: Sony XQ-AT72 (Android 12), XQ-DQ72 (Android 13), API 24 및 35 (16 KB 페이지) 에뮬레이터에서 실측했습니다 (등록, 확인, 식별, 나열, 삭제, 그리고 "등록된 목소리만"의 통과와 거부. LibriSpeech 낭독자 2명과 합성한 호출어 사용). 같은 사람의 2초 음성은 점수 0.75에서 0.79, 다른 사람은 0.08 미만이며 기본 임계값은 0.50입니다. 컴퓨터에서 영어 화자 40명과 중국어 화자 12명으로 측정한 결과, 2초 음성에서 본인을 거부한 비율은 약 2.5% (영어)와 0% (중국어), 타인을 받아들인 비율은 약 0.05%와 0.9%였습니다. "등록된 목소리만"의 비교는 Sony 기기에서 0.15에서 0.25초 걸립니다.
- 오디오 분석: Sony G8441 (Android 9), OnePlus OPD2413 (ColorOS, Android 15), API 24 및 35 (16 KB 페이지) 에뮬레이터에서 실제로 확인했습니다 (화자 분리와 이름 붙이기, 언어 식별, 태그, 취소, 파일 화면, 도구 3개, AutoJs6 스크립트). LibriSpeech 낭독자 두 명이 번갈아 읽은 42.5초 녹음은 모든 기기에서 2명 7구간으로 나뉘었고, 시간 기준으로 음성의 93%가 올바른 사람에게 배정되었습니다. 등록된 낭독자에게는 이름이 붙었고 (점수 0.85), 다른 한 명에게는 붙지 않았습니다 (0.01). 중국어, 러시아어, 영어 녹음은 모두 올바른 언어로 판정되었고, 음악, 백색 소음, 삐 소리는 각각 맞는 10초 구간에 태그되었습니다. 10분 녹음은 OnePlus OPD2413에서 화자 분리 약 70초, 언어 식별 2.5초, 태그 3.8초, Sony G8441에서는 각각 222초 (최대 메모리 약 470 MiB), 12초, 15초가 걸렸습니다.

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

#### 상시 호출어를 켜면 왜 마이크 표시등이 계속 켜져 있고, 배터리는 얼마나 쓰나요?

호출어를 계속 들으려면 계속 녹음해야 합니다. Android 12부터는 앱이 녹음하는 동안 상태 표시줄에 마이크 표시등 (녹색 점)이 계속 표시되며 앱이 숨길 수 없습니다. 표시등이 켜지지 않는 저전력 핫워드 경로는 사전 설치된 시스템 어시스턴트 전용이라, 기본 디지털 어시스턴트로 선택된 서드파티 앱도 쓸 수 없습니다. 그래서 상시 호출어는 기본값이 꺼짐이며, 켜 두는 동안 녹색 점과 알림이 계속 표시됩니다 (알림에서 바로 끌 수 있습니다). Sony XQ-DQ72에서 화면을 끈 채 30분 동안 측정한 결과, 켜 두었을 때 Android가 추정한 플러그인 소비 전력은 약 17 mAh (배터리 용량의 약 0.4%)였고 꺼 두었을 때는 거의 0이었습니다. 추정치의 대부분은 녹음 자체의 몫입니다. 기본값으로 켜져 있는 절전 모드는 말소리나 다른 소리가 들릴 때만 호출어 모델을 실행해 플러그인 CPU 시간을 절반 (계속 소리가 날 때)에서 10분의 1 (조용할 때)로 줄이지만, 추정치는 약 1 mAh만 줄어듭니다. 아주 작은 소리로 말한 호출어는 절전 모드에서 놓칠 수 있으며, 그럴 때는 설정에서 절전 모드를 끌 수 있습니다. 필요할 때만 켜세요. 듣기 시간을 10, 30, 60분으로 정하거나 충전 중에만 듣기를 켜면 됩니다 (전원을 뽑으면 일시 중지하고 다시 연결하면 계속). 타일, 위젯, 플로팅 마이크 같은 실행 방법은 평소에 녹음하지 않습니다.

#### 호출어를 말해도 "탭하여 말하기" 알림만 나타나는 이유는 무엇인가요?

Android 10부터 백그라운드 앱은 기본 디지털 어시스턴트이거나 다른 앱 위에 표시 권한이 있을 때만 화면을 바로 열 수 있습니다. 3-Macaw Speech가 기본 디지털 어시스턴트가 아니면 상시 호출어를 켤 때 이 권한을 한 번 묻습니다. 허용하지 않으면 호출어를 들었을 때 "탭하여 말하기" 알림이 표시되고, 누르면 패널이 열리며 1분 뒤에 자동으로 사라집니다. 호출어로 패널을 바로 열려면 3-Macaw Speech를 기본 디지털 어시스턴트로 지정하거나 시스템 설정에서 다른 앱 위에 표시를 허용하세요.

#### 상시 호출어를 켤 때 먼저 3-Stove Agent를 끄라는 안내가 나오는 이유는 무엇인가요?

3-Macaw Speech, 3-Stone AI, 3-Stove Agent의 상시 듣기는 함께 쓸 수 없습니다. 한 기기에서 동시에 듣는 앱은 하나뿐이어야 하며, 그렇지 않으면 마이크를 서로 차지하려 합니다. 하나가 켜져 있으면 다른 앱의 스위치에 어떤 앱이 듣고 있는지 표시되고, 그 앱의 설정을 바로 열어 끌 수 있습니다. 재시작이나 업데이트 후에는 상시 호출어가 저절로 녹음을 시작하지 않습니다. 스위치가 켜져 있으면 "탭하여 상시 호출어 다시 시작" 알림이 표시되며, 누르거나 3-Macaw Speech의 홈 화면 또는 설정을 열면 다시 시작합니다. 그사이 듣기 시간이 지났으면 스위치가 꺼집니다.

#### 상시 호출어가 내 목소리에만 반응하게 하려면?

3-Macaw Speech 설정을 열고 상시 호출어 그룹에서 "화자"를 누른 뒤, 안내에 따라 화자 인식 모델 (약 26.5 MB)을 내려받습니다. 그다음 "화자 추가"를 누르고 이름을 입력한 뒤 화면의 문장 3개 (문장당 약 4초)를 따라 읽습니다. 가족도 각자 등록할 수 있습니다. 이제 "등록된 목소리만"을 켜면, 호출어가 들렸을 때 플러그인이 그 직전 약 2초의 오디오를 등록된 성문과 비교해 누군가와 일치하면 (점수 0.50 이상) 패널을 열고, 아니면 아무것도 듣지 못한 것으로 처리합니다 (패널도 알림도 없음). "화자" 페이지의 "인식 시험"으로 내 점수를 볼 수 있습니다. 목소리가 작거나 주변이 시끄럽거나 다른 마이크 (예: 헤드셋)를 쓰면 거부될 수 있으니, 같은 환경에서 녹음을 추가하세요. 이 기능은 주변 사람이 실수로 깨우는 것을 막기 위한 것이며 보안 인증이 아닙니다. 등록된 사람의 녹음을 재생해도 통과할 수 있습니다.

#### AI 어시스턴트는 3-Macaw Speech를 통해 어떻게 듣고 말하나요?

AutoJs6 6.8.0 이상과 MCP Server 플러그인이 필요합니다. 3-Macaw Speech는 공식 플러그인이므로 `speech_listen`, `speech_speak`, `speech_transcribe`는 기본적으로 MCP Server의 도구 목록에 나타납니다 (MCP Server 설정의 "플러그인 도구"에서 끌 수 있음). `speech_synthesize`는 파일을 쓰므로 그룹의 두 번째 스위치도 켜야 하며, 위치를 지정하지 않으면 파일은 AutoJs6 작업 디렉터리의 plugin-outputs/speech에 저장됩니다. `speech_listen`은 한 번에 한 문장을 듣습니다 (최대 60초). 바로 녹음할 수 있을 때 (예: 상시 호출어가 켜져 있거나, 3-Macaw Speech가 기본 디지털 어시스턴트이거나 다른 앱 위에 표시 권한이 있을 때)는 즉시 시작하고, 그렇지 않으면 휴대폰에 "탭하여 말하기" 알림이 나타나며 1분 안에 누르면 시작합니다. 듣는 동안 다른 앱의 상시 대기는 잠시 양보했다가 끝나면 자동으로 다시 시작합니다. 다른 받아쓰기가 진행 중이면 BUSY를, 마이크 권한이나 인식 모델이 없으면 NOT_READY를 반환합니다 (둘 다 플러그인 홈 화면에서 해결할 수 있음). `speech_speak`는 해당 언어의 3-Macaw Speech 음성이 있으면 그 음성으로, 없으면 시스템 음성으로 읽습니다.

#### 전원 버튼 말고 받아쓰기 패널을 여는 방법이 또 있나요?

3-Macaw Speech 설정의 실행 방법 그룹에서 설정합니다: 빠른 설정 타일은 탭해서 추가한 뒤 (Android 13 이상은 시스템이 확인하고, 이전 버전은 직접 추가하는 방법을 보여 줍니다) 빠른 설정에서 음성 명령을 탭합니다. 홈 화면 위젯과 바로가기는 탭하여 말하기 위젯을 홈 화면에 놓거나, 앱 아이콘을 길게 누르고 음성 명령을 선택합니다. 플로팅 마이크는 스위치를 켜고 다른 앱 위에 표시 권한을 허용하면 화면 가장자리에 끌어서 옮길 수 있는 마이크 버튼이 나타납니다. 키 트리거는 시스템 접근성 설정에서 3-Macaw Speech 키 트리거를 켭니다. 기본값은 볼륨 낮추기 키를 약 0.8초 길게 누르기이며, 볼륨 높이기 키 길게 누르기나 볼륨 낮추기 키 두 번 누르기로 바꿀 수 있습니다. 모든 입구가 디지털 어시스턴트와 같은 패널을 열고, 말한 뒤 텍스트를 받을 앱을 선택합니다. 3-Macaw Speech가 기본 디지털 어시스턴트이면 어시스턴트 세션을 거쳐 열고, 아니면 같은 패널을 바로 엽니다.

#### 키 트리거에 왜 접근성 서비스가 필요하며, 안전한가요?

Android에서 시스템보다 먼저 하드웨어 키를 받을 수 있는 것은 접근성 서비스뿐이라서 키 트리거는 접근성 서비스입니다. 키 필터링만 요청하고 화면 이벤트는 구독하지 않으므로 화면 내용을 읽을 수도, 제스처를 실행할 수도 없습니다. 선택한 볼륨 키만 가로채며, 짧게 누르면 평소처럼 볼륨이 바뀌고, 화면이 꺼져 있거나 전화가 오거나 통화 중일 때는 가로채지 않습니다. 스위치가 회색이면 (Android 13 이상에서 설치 파일로 설치한 앱) 먼저 3-Macaw Speech의 앱 정보를 열고 메뉴에서 제한된 설정 허용을 선택하세요.

#### 블루투스 헤드셋의 음성 버튼으로 받아쓰기 패널을 열 수 있나요?

네. 3-Macaw Speech는 블루투스 서비스의 음성 명령 요청 (VOICE_COMMAND)에 응답하므로, 헤드셋의 음성 키나 터치 영역을 길게 누르면 받아쓰기 패널이 열립니다. Android 12 이상에서는 패널이 처음 한 번 근처 기기 권한을 요청합니다. 허용하면 3-Macaw Speech가 헤드셋의 음성 인식을 넘겨받아 헤드셋 마이크로 듣고, 누를 때마다 작동합니다. 허용하지 않으면 휴대전화 마이크로 듣고, 일부 헤드셋 (예: Sony WF-1000XM4)은 두 번에 한 번만 패널을 엽니다. 다른 앱 (Google 앱, 제조사 어시스턴트)도 음성 명령에 응답하면 시스템이 먼저 어떤 앱을 쓸지 묻습니다. HyperOS에서는 선택 화면에서 3-Macaw Speech를 골라도 아무 반응이 없었는데, 이는 시스템 선택 화면의 문제입니다. 쓰지 않으려면 설정의 실행 방법 그룹에서 헤드셋 음성 키를 끄세요.

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

- 마이크: 받아쓰기, 호출어, 체험 화면, 그리고 상시 호출어가 켜져 있는 동안에만 녹음하며, 세션이 끝나거나 스크립트가 종료되면 즉시 마이크를 해제합니다.
- 네트워크: 모델 다운로드와 서명된 모델 카탈로그 새로 고침에만 사용하며, 인식과 합성은 오프라인으로 동작합니다.
- 저장소: 모델은 플러그인 전용 디렉터리에 저장합니다. 가져올 로컬 모델과 체험하기 화면에서 인식할 파일은 모두 시스템 파일 선택기로 고르고 인식 결과도 사용자가 고른 위치에 저장하므로 저장소 권한을 요청하지 않습니다. 마지막 인식 결과는 플러그인 캐시에 남으며 화면에서 지울 수 있습니다.
- 보호된 진입점: 기능 서비스와 활성화 진입점은 AutoJs6 플러그인 권한 (`org.autojs.permission.PLUGIN`)으로 보호되어 다른 앱이 직접 호출할 수 없습니다. 시스템 TTS 엔진과 그 음성 확인, 예시 문장, 설정 진입점은 Android 규칙에 따라 모든 앱에 열려 있으며 음성 합성만 제공합니다.
- 시스템 음성 입력: 시스템 TTS 엔진처럼 모든 앱에 열려 있습니다; 패널이 화면에 있는 동안에만 녹음하고 인식된 텍스트만 돌려주며, 설정에서 끌 수 있습니다.
- 디지털 어시스턴트: 화면 내용을 읽지 않으며 상시 호출어가 꺼져 있으면 백그라운드에서 듣지 않습니다. 패널이 화면에 있는 동안에만 녹음하고, 인식된 텍스트는 앱 버튼을 누르거나 복사할 때만 패널 밖으로 나갑니다(잠금 화면에서는 잠금 해제 후). 음성 인식 서비스는 마이크 권한을 직접 가진 앱을 위해서만 인식합니다.
- 실행 방법: 타일, 위젯, 바로가기, 플로팅 마이크, 키 트리거, 헤드셋 음성 키 자체는 녹음하지 않고 받아쓰기 패널을 열기만 합니다. 키 트리거는 키 입력만 다루는 접근성 서비스입니다: 화면 내용을 읽지 않고 제스처도 실행하지 않으며, 선택한 볼륨 키만 가로챕니다 (짧게 누르면 평소처럼 볼륨이 바뀌고, 화면이 꺼져 있거나 전화가 오거나 통화 중일 때는 가로채지 않습니다). 플로팅 마이크는 다른 앱 위에 표시 권한이 필요하며 무음 알림 하나로 표시를 유지합니다. 부팅 시 자동 시작은 켜 둔 플로팅 마이크를 다시 시작한 뒤 되살리는 데만 씁니다. Android 12 이상에서 헤드셋 음성 키에는 근처 기기 권한이 필요하며, 헤드셋으로 연 패널에서만 요청합니다: 블루투스 서비스가 음성 인식을 패널에 넘기고 헤드셋 마이크를 연결하는 데만 쓰며, 기기를 검색하거나 위치를 읽지 않습니다.
- 상시 호출어: 기본값은 꺼짐이며 플러그인 화면에서만 켤 수 있습니다 (재시작이나 업데이트 후에는 알림을 눌러 다시 시작하며 저절로 녹음하지 않습니다). 호출어는 기기에서 감지하고 오디오를 저장하거나 업로드하지 않으며 인식한 텍스트를 로그에 남기지 않습니다. 켜 두는 동안 계속 녹음하고 마이크 표시등이 계속 켜지며, 알림에서 바로 끌 수 있습니다. 3-Stone AI, 3-Stove Agent의 상시 듣기와는 함께 쓸 수 없습니다 (시스템에서 하나만).
- 성문: 등록에 쓴 녹음은 저장하지 않고, 그로부터 계산한 성문만 저장합니다. 성문은 플러그인 전용 폴더에만 있으며 업로드되지 않고 내보내거나 가져올 수 없습니다. 스크립트와 플러그인 도구가 받는 것은 이름, 점수, 일치 여부뿐입니다. "화자" 페이지에서 한 명씩 삭제할 수 있고, 3-Macaw Speech의 데이터를 지우거나 제거하면 모두 삭제됩니다. "등록된 목소리만"이 비교하는 호출어 직전 약 2초의 오디오는 메모리에서만 처리하고 저장하지 않습니다.
- 플러그인 도구: 도구 서비스도 AutoJs6 플러그인 권한으로 보호되어 AutoJs6만 직접 호출할 수 있습니다. `speech_listen`은 호출마다 한 문장만 녹음하며 녹음 중에는 알림이 표시됩니다 (Android 12부터는 마이크 표시등도). `speech_transcribe`는 AutoJs6가 넘겨준 파일 하나만 읽고, `speech_synthesize`는 오디오를 AutoJs6에 돌려주어 지정한 위치에 쓰게 합니다. 인식한 텍스트와 읽은 텍스트는 로그에 기록하지 않습니다. 분석 도구 3개도 AutoJs6가 넘겨준 파일 하나만 읽으며, `speech_diarize`로 사람을 식별할 때도 이름과 점수만 반환하고 성문은 포함하지 않습니다.

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
tool service: org.autojs.plugin.TOOL_PROVIDER (short name speech: listen, speak, transcribe, synthesize, speaker_verify, speaker_identify, diarize, detect_language, tag; AutoJs6 6.8.0+)
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### 감사의 말

******

3-Macaw Speech는 다음 오픈 소스 프로젝트, 모델, 데이터 위에 만들어졌습니다. 작성자와 관리자 여러분께 감사드립니다:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)와 [k2-fsa (차세대 Kaldi)](https://github.com/k2-fsa) 팀: 인식, 호출어, 음성 구간 검출, 합성의 런타임, 각 모델의 ONNX 내보내기와 Android 라이브러리 (Apache-2.0).
- [icefall](https://github.com/k2-fsa/icefall)로 학습한 모델: Zipformer 스트리밍 인식 모델 (중영 2개 언어 pfluo, 영어 Zengwei, 중국어 2025 yuekai, 프랑스어 shaojieli, 한국어 johnBamma), 개방 어휘 호출어 모델 (pkufool), 그리고 k2-fsa의 AISHELL-3 VITS와 Baker Matcha-TTS 합성 모델.
- [Vosk](https://huggingface.co/alphacep/vosk-model-small-streaming-ru) (Alpha Cephei): 러시아어 소형 스트리밍 인식 모델 (Apache-2.0).
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / Alibaba, FunASR 모델 라이선스 1.1): 다국어 오프라인 인식 모델.
- 문장 부호 모델: [FunASR](https://github.com/modelscope/FunASR) (Alibaba DAMO Academy)의 CT-Transformer 중영 문장 부호 모델과 [Edge-Punct-Casing](https://github.com/frankyoujian/Edge-Punct-Casing) (frankyoujian)의 영어 문장 부호 및 대소문자 모델 (모두 Apache-2.0).
- 합성 모델 [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, 중국어 녹음은 LongMaoData 제공), [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT)와 [HiFi-GAN](https://github.com/jik876/hifi-gan) 보코더 (MIT). 프랑스어 siwis, 스페인어 davefx, 독일어 thorsten, 러시아어 denis 음성은 [piper-voices](https://huggingface.co/rhasspy/piper-voices) (MIT)에서 왔으며 모두 영어 lessac 음성에서 미세 조정했습니다.
- 학습 데이터: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), [Data-Baker 중국어 표준 여성 음성 말뭉치](https://www.data-baker.com/open_source.html) (비상업적 이용만), Lessac Technologies의 Blizzard 2013 데이터 (연구용만), [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (퍼블릭 도메인). 1.2.0에 추가된 프랑스어, 한국어, Piper 모델은 [Common Voice](https://commonvoice.mozilla.org/) 12 프랑스어 (CC0), [KsponSpeech](https://aihub.or.kr/aihubdata/data/view.do?currMenu=115&topMenu=100&aihubDataSe=realm&dataSetSn=123) (AI Hub, 한국 국민만 신청 가능), [SIWIS 프랑스어 음성 합성 데이터베이스](https://datashare.is.ed.ac.uk/handle/10283/2353) (CC BY 4.0, SIWIS database, University of Edinburgh and partners), [Thorsten-Voice](https://github.com/thorstenMueller/Thorsten-Voice) (CC0), [NabuCasa voice-datasets](https://github.com/NabuCasa/voice-datasets)의 davefx와 denis 녹음 (CC0)도 사용합니다.
- 1.3.0에 추가된 모델: [NVIDIA NeMo](https://huggingface.co/nvidia/stt_es_fastconformer_hybrid_large_pc)의 스페인어 FastConformer (CC-BY-4.0, 학습 데이터는 Common Voice 12 (CC0), Multilingual LibriSpeech (CC BY 4.0), VoxPopuli (CC0), Fisher Spanish (LDC)), 8개 언어 스트리밍 모델 [PengChengStarling](https://github.com/yangb05/PengChengStarling) (Apache-2.0), [Kokoro v1.0](https://huggingface.co/hexgrad/Kokoro-82M) (hexgrad, Apache-2.0), OpenVoiceOS의 아랍어 음성 [dii](https://huggingface.co/OpenVoiceOS/phoonnx_ar-SA_dii_espeak)와 [miro](https://huggingface.co/OpenVoiceOS/phoonnx_ar-SA_miro_espeak_V2) ([phoonnx](https://github.com/TigreGotico/phoonnx)로 TigreGotico의 합성 음성 데이터를 학습), Mycroft [Mimic 3](https://github.com/MycroftAI/mimic3-voices)의 한국어 KSS 음성 (CC0, 학습 데이터는 [KSS 데이터셋](https://www.kaggle.com/datasets/bryanpark/korean-single-speaker-speech-dataset), CC BY-NC-SA 4.0).
- 추론 엔진 [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT), 음성 구간 검출 모델 [silero VAD](https://github.com/snakers4/silero-vad) (MIT), 음소 변환의 [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later)와 [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT), 중국어 호출어 읽기 표 [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT).

******

### 개발 로드맵

******

계획, 설계 결정, 진행 상황은 ROADMAP.md의 체크리스트로 관리합니다. 체크되지 않은 항목은 계획이며 현재 버전의 기능이 아닙니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.4.0

_2026/10/30_

- `기능` 체험하기 화면의 파일 인식에서 오디오를 재생할 수 있습니다: 결과 위의 재생 막대에서 위치 이동, 0.5배부터 2배까지 속도 변경, 5초 앞뒤 이동이 가능합니다. 재생 중인 문장은 강조되어 화면에 유지되고, 문장을 누르면 그 위치부터 재생하며, 가사 보기로 전환하면 현재 문장을 크게 표시합니다 (화자, 언어, 소리 태그 결과도 마찬가지입니다). 페이지를 나가도 파일이 남아 있으면 최근 결과를 재생할 수 있습니다: 결과를 지우거나 다른 파일을 고를 때까지 플러그인이 이 파일 하나의 읽기 권한을 유지합니다.
- `개선` 언어 식별이 문장 단위로 바뀌었습니다: 녹음에 여러 언어가 섞여 있으면 문장마다 어떤 언어인지 볼 수 있습니다 (아주 짧은 문장은 이웃 문장에 합칩니다). 체험 화면에 문장별로 표시되고, 스크립트의 speech.detectLanguage와 도구 speech_detect_language의 segments도 문장 단위입니다. 전체 언어는 가장 오래 말한 언어이고 후보의 점수는 말한 시간의 비율입니다. windows 옵션은 그대로입니다. 문장마다 한 번씩 판단하므로 1.3.0보다 조금 느립니다

#### v1.3.0

_2026/10/30_

- `힌트` 스크립트의 speech.speaker, speech.diarize, speech.detectLanguage, speech.tag와 화자 및 분석 도구 5개에는 AutoJs6 6.8.0 정식판 이상이 필요합니다. "화자" 페이지, "등록된 목소리만", 체험하기 화면의 파일 분석은 플러그인 안에서 쓰는 기능이라 AutoJs6 버전과 관계없이 쓸 수 있습니다. 이전 AutoJs6에서는 스크립트의 speech.models.list()에 화자 인식, 화자 분할, 언어 식별, 오디오 태그 모델 (종류 speaker, segment, langid, tagger)도 표시되며, 평소처럼 설치하거나 삭제할 수 있습니다
- `힌트` 긴 오디오를 분석하면 메모리를 많이 씁니다: 10분 녹음의 화자를 분리할 때 실측 최대치가 약 470 MB에서 1.3 GB였기 때문에, 플러그인은 Android에 더 큰 메모리 한도를 요청합니다. 메모리가 적은 휴대폰에서는 먼저 짧은 파일로 해 보세요
- `기능` 상시 호출어에 절전 모드 추가 (설정 > 상시 호출어, 기본값 켜짐): 말소리나 다른 소리가 들릴 때만 호출어 모델을 실행하고 조용할 때는 실행하지 않아, 플러그인 CPU 시간이 절반에서 10분의 1로 줄어듭니다 (조용할수록 적음). 녹음 자체의 소비는 그대로라서 Android의 추정 소비는 조금만 줄어듭니다 (실측은 README 참고). 아주 작게 말한 호출어는 놓칠 수 있으며, 그럴 때는 절전 모드를 끄세요
- `기능` 내려받을 수 있는 모델 추가: 스페인어 오프라인 인식 (NeMo, 문장 부호 포함), 언어를 자동으로 판별하는 8개 언어 스트리밍 인식 (아랍어, 인도네시아어, 태국어, 베트남어, 일본어, 러시아어, 중국어, 영어, PengChengStarling, 설치 후 약 339 MB), Kokoro v1.0 다국어 음성 (영어, 스페인어, 프랑스어, 힌디어, 이탈리아어, 브라질 포르투갈어, 중국어 화자 49명, 각 화자는 자기 언어를 읽음), 아랍어 음성 2개와 한국어 음성 1개. 아랍어와 한국어 음성은 비상업적 이용만 가능하고, 8개 언어 모델은 학습 데이터가 공개되지 않아 상업적 이용 여부를 알 수 없으며, 모두 내려받기 전에 라이선스를 보여 줍니다. 새 음성은 시스템 TTS 엔진에도 언어별로 나타나며, 시스템 음성 입력과 플러그인 도구는 다른 설치된 모델이 지원하지 않는 언어에만 8개 언어 모델을 자동으로 선택합니다
- `기능` 화자 인식과 "등록된 목소리만" (설정 > 상시 호출어): "화자" 페이지에서 이름을 입력하고 문장 3개를 따라 읽으면 자신의 목소리를 등록할 수 있습니다 (여러 사람을 등록할 수 있고, 인식 시험으로 점수도 볼 수 있습니다). "등록된 목소리만"을 켜면 상시 호출어가 먼저 목소리를 비교해 등록된 사람과 일치할 때만 패널을 열기 때문에, 다른 사람이 호출어를 말하면 무시됩니다. 타일, 위젯, 헤드셋 키 등 수동 실행 방법은 영향을 받지 않습니다. 먼저 화자 인식 모델 (약 26.5 MB)을 내려받아야 하며, 페이지에서 안내합니다. 성문 (목소리 특징)은 기기에만 저장되며 업로드하거나 내보내지 않습니다
- `기능` 스크립트에서 speech.speaker로 화자를 등록, 확인, 식별하고, 등록된 사람을 나열하거나 삭제할 수 있습니다. 새 플러그인 도구 speech_speaker_verify (녹음이 등록된 특정 사람인지)와 speech_speaker_identify (등록된 사람 중 가장 비슷한 사람)를 추가했습니다. 결과에는 이름과 점수만 있고 성문은 없습니다
- `기능` 체험하기 화면의 파일 인식에 분석 3가지를 추가했습니다: 화자 분리 (누가 언제 말했는지 표시하고, 등록된 목소리는 이름도 보여 줍니다. "화자 분리 + 받아쓰기"는 각 사람의 말을 글자로도 옮깁니다), 언어 식별 (Whisper의 99개 언어), 오디오 태그 (사람 목소리, 음악, 개 짖는 소리 등 527종의 소리 중 파일에 있는 것). 모두 휴대폰에서 오프라인으로 처리하며, 화면을 벗어나도 계속되고 진행 상황은 알림에 표시됩니다. 먼저 해당 모델을 내려받아야 하며 화면에서 안내합니다: 화자 분할 모델 약 6 MB (화자 인식 모델도 필요), 언어 식별 모델 약 103 MB, 오디오 태그 모델 약 10.5 MB. 화자 분할 모델은 학습 데이터에 연구용으로만 쓸 수 있는 자료가 있어 비상업적 이용으로 표시하고, 언어 식별 모델은 학습 데이터가 공개되지 않아 상업적 이용 여부를 알 수 없습니다
- `기능` 스크립트에서도 speech.diarize, speech.detectLanguage, speech.tag (Promise를 반환하는 ...Async 버전도 있음)로 같은 분석을 하고 진행 상황을 받을 수 있습니다. 새 플러그인 도구 speech_diarize, speech_detect_language, speech_tag도 추가했습니다. 파일 하나는 최대 30분 (도구의 화자 분리는 10분)이며, 분석은 한 번에 하나만 하고 나머지는 차례를 기다립니다
- `수정` Android 7.0에서 모델을 다시 다운로드할 수 있습니다. 모델 목록과 모델은 GitHub에서 받아오는데, 그 인증서가 이제 Android 7.0에 없는 루트 인증서 (Let's Encrypt의 ISRG Root X1)로 발급되어 다운로드와 목록 새로 고침이 인증서 오류로 매번 실패했습니다. 플러그인이 이 루트 인증서를 직접 포함해 시스템 인증서와 함께 사용합니다. Android 7.1 이상은 원래 갖고 있어 영향이 없습니다
- `수정` 일부 휴대폰에서 모델과 파일 크기의 숫자와 단위 사이에 큰 공백이 표시되던 문제를 고쳤습니다 (영어처럼 지역이 지정되지 않은 화면 언어일 때)

#### v1.2.0

_2026/10/23_

- `힌트` 상시 호출어가 추가되었습니다 (기본값 꺼짐). 켜 두면 기기에서 호출어를 계속 듣고, 마이크 표시등이 켜져 있으며, 알림이 항상 표시되고, 배터리도 더 소모됩니다 (화면을 끈 30분 측정값은 README 참고). "듣기 시간" 또는 "충전 중에만 듣기"와 함께 쓰기를 권장합니다
- `힌트` 스크립트에서 WAV가 아닌 오디오를 인식하는 기능과 플러그인 도구 4개에는 AutoJs6 6.8.0 정식판 이상이 필요합니다. 이전 AutoJs6에서는 스크립트가 여전히 WAV만 인식합니다. 상시 호출어, 새 모델, 자동 문장 부호, "파일" 탭은 플러그인 안에서 쓰는 기능이라 AutoJs6 버전과 관계없이 쓸 수 있습니다
- `힌트` 새 권한: 절전 모드 방지 (체험하기 화면에서 파일을 인식하는 동안에만, 최대 60분, 화면이 꺼져도 인식을 계속하기 위해 사용), 미디어 처리 포그라운드 서비스 (Android 15 이상, 파일 인식 중 알림에 진행 상황 표시)
- `기능` 상시 호출어 (설정 > 상시 호출어): 호출어 (기본값 "小鹦小鹦"과 "Hey Macaw", 변경 가능, 감도는 높음, 보통, 낮음 3단계)를 말하면 디지털 어시스턴트와 같은 받아쓰기 패널이 열립니다. 화면이 꺼져 있거나 잠겨 있어도 화면을 켜며, 보내거나 복사하기 전에 잠금을 해제합니다
- `기능` 호출어로 연 패널은 인식한 텍스트를 읽어 줍니다. 해당 언어의 3-Macaw Speech 음성이 있으면 그것을, 없으면 시스템 음성을 사용하며 설정에서 끌 수 있습니다
- `기능` 듣기 시간 (10, 30, 60분 후 자동으로 끄기 또는 항상 켜 두기)과 충전 중에만 듣기 (전원을 뽑으면 일시 중지하고 다시 연결하면 계속)를 추가했습니다. 3-Stove Agent의 같은 옵션과 동일합니다
- `기능` 3-Macaw Speech가 기본 디지털 어시스턴트가 아니면 상시 호출어를 켤 때 다른 앱 위에 표시 권한을 한 번 묻습니다. 허용하지 않으면 호출어를 들었을 때 "탭하여 말하기" 알림이 표시되고, 누르면 패널이 열립니다
- `기능` 스크립트, 다른 앱 또는 이 플러그인의 다른 실행 방법이 마이크를 쓰면 상시 호출어가 잠시 양보하고, 끝나면 다시 듣습니다
- `기능` 3-Stone AI, 3-Stove Agent의 상시 듣기와는 함께 쓸 수 없습니다 (시스템에서 하나만). 상대가 켜져 있으면 먼저 끄도록 안내하고 상대 앱의 설정을 바로 열 수 있습니다
- `기능` 재시작이나 업데이트 후에는 녹음이 자동으로 시작되지 않습니다. 스위치가 켜져 있으면 "탭하여 상시 호출어 다시 시작" 알림이 표시되며, 홈 화면이나 설정을 열어도 다시 시작됩니다
- `기능` 오디오 파일 인식 (speech.recognize, speech.detectVoice)에서 WAV 외에 MP3, AAC (M4A 포함), Ogg (Vorbis, Opus), FLAC, AMR 등 일반적인 형식과 동영상 파일 (MP4, WebM 등)의 소리도 사용 가능; 제한은 그대로 200 MB 또는 30분 (AutoJs6 6.8.0 이상 필요, 이전 버전은 WAV만 가능)
- `기능` 플러그인 도구 4개 (AutoJs6 6.8.0 이상): speech_listen은 한 문장을 듣고 텍스트를 반환하고, speech_speak는 소리 내어 읽으며, speech_transcribe는 휴대폰의 오디오 파일을 인식하고, speech_synthesize는 텍스트를 WAV 또는 M4A 파일로 합성합니다. MCP Server 플러그인은 이 도구로 컴퓨터의 AI 어시스턴트가 휴대폰을 통해 듣고 말하게 하며, 휴대폰의 3-Stove Agent는 직접, 스크립트에서는 plugins.tools로 호출할 수 있습니다. 바로 녹음할 수 없을 때는 먼저 휴대폰에 "탭하여 말하기" 알림을 띄우고, 누르면 듣기 시작합니다
- `기능` 내려받을 수 있는 모델 추가: 프랑스어, 러시아어, 한국어 스트리밍 인식과 프랑스어, 스페인어, 독일어, 러시아어 Piper 음성. Piper 음성과 한국어 모델은 비상업적 이용만 가능하며 내려받기 전에 라이선스를 보여 주고, 한국어 모델은 원본 주소에서만 내려받습니다
- `기능` 자동 문장 부호: 홈 화면의 새 "문장 부호" 그룹에서 문장 부호 모델을 내려받으면 스트리밍 모델이 인식한 중국어와 영어에 쉼표, 마침표, 물음표가 붙고 영어는 대문자도 복원됩니다. SenseVoice는 자체 문장 부호를 붙이므로 바뀌지 않으며, 스크립트에서 punctuation: false를 넘기면 이전처럼 모든 문장 부호를 없앱니다
- `기능` 체험하기 화면에 "파일" 탭 추가: 오디오나 동영상 파일 (MP3, M4A, WAV 등, 최대 200 MB, 30분)을 고르면 스크립트를 쓰지 않아도 그 안의 말을 글자로 옮겨 시간별로 나누어 보여 주며, 전체를 복사하거나 텍스트와 SRT 자막으로 저장할 수 있습니다. 모델, 언어, 자동 문장 부호는 화면에서 고릅니다. 화면을 떠나거나 화면을 꺼도 인식이 계속되고 알림에 진행 상황과 취소 버튼이 표시되며, 마지막 결과는 다음에 열 때도 남아 있습니다

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
