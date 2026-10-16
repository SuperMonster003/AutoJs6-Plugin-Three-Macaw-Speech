<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-macaw-speech-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Reconocimiento de voz sin conexión, detección de palabras de activación, detección de actividad de voz y síntesis de voz neuronal, utilizable como motor TTS del sistema</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas (Languages)

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ar.md)

******

### Introducción

******

3-Macaw Speech usa [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) para ejecutar sin conexión, en el propio dispositivo, el reconocimiento de voz, la detección de actividad de voz, la detección de palabras de activación y la síntesis de voz neuronal. Todo se calcula localmente y no se envía ningún audio.

El complemento es un APK que se instala por separado. Los scripts de AutoJs6 lo usan mediante el módulo global `speech`; la síntesis de voz también se registra como motor TTS del sistema Android, de modo que el módulo `tts` de AutoJs6 y otras aplicaciones pueden elegirla. Los modelos de reconocimiento y síntesis no se incluyen en el APK, sino que se descargan bajo demanda desde la pantalla del complemento o un script; solo el modelo de detección de actividad de voz viene integrado.

> Esta es una versión preliminar de desarrollo: AutoJs6 detecta el complemento, que descarga y administra modelos, reconoce voz y detecta actividad de voz y palabras de activación, pero la síntesis y las pantallas del complemento aún no se entregan, y la versión de AutoJs6 que ofrece el módulo de script `speech` aún no se ha publicado. El texto siguiente describe los objetivos de diseño de la versión 1.0.0; consulte la hoja de ruta para ver el progreso.

******

### Funciones Destacadas

******

- Reconocimiento de voz: dictado continuo desde el micrófono (resultados parciales y finales en tiempo real) y reconocimiento de archivos WAV, con modelos sin conexión para chino, inglés, chino e inglés mezclados, japonés, coreano y cantonés.
- Detección de actividad de voz: el silero VAD v5 integrado divide el audio en segmentos de voz por sí solo y también cierra automáticamente las frases dictadas.
- Palabras de activación: escriba la palabra clave como texto (por ejemplo `小六小六` o `hey macaw`) sin entrenar ningún modelo; tras la activación, el dictado puede empezar automáticamente.
- Síntesis de voz: varias voces neuronales en chino e inglés, sintetizadas en archivos WAV o usadas como motor TTS del sistema por el módulo `tts` de AutoJs6 y otras aplicaciones.
- Gestión de modelos: descarga desde un catálogo firmado, activación solo después de que cada archivo supere su comprobación SHA-256, e importación de modelos locales; los modelos restringidos a uso no comercial muestran su licencia antes de la descarga.

******

### Uso

******

1. Actualice AutoJs6 a la versión interna 5451 (6.8.0) o posterior.
2. Descargue desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) el APK que corresponda a la ABI del dispositivo (elija `universal` si tiene dudas) e instálelo; después autorícelo y actívelo en el centro de complementos de AutoJs6.
3. Abra la pantalla del complemento para conceder el permiso del micrófono y descargar los modelos necesarios; después use el módulo `speech` en los scripts.

> El complemento admite dispositivos con Android 7.0 (API 24) o posterior. Hay un paquete por ABI y un paquete `universal` para todos los dispositivos.

******

### Modelos

******

Los modelos de reconocimiento, de palabras de activación y de síntesis ocupan entre 30 MB y 350 MB cada uno, por lo que no se incluyen en el APK. El complemento los descarga de publicaciones inmutables del repositorio de recursos compartidos de AutoJs6 y recurre a la dirección original de sherpa-onnx si el espejo no está disponible; en ambos casos cada archivo debe superar su comprobación SHA-256 antes de activar el modelo.

Cada modelo indica su licencia. Los modelos restringidos a uso no comercial (por ejemplo, un modelo de síntesis entrenado con el corpus DataBaker) muestran un aviso antes de la descarga, y los scripts deben pasar `acceptLicense: true` explícitamente.

******

### API De Script

******

Los scripts usan estas capacidades mediante el módulo global `speech` (también disponible como `$speech`; es el borrador de diseño de la 1.0.0, prevalece la documentación de la versión publicada):

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

- `speech.listen(options)` abre una sesión de dictado que informa de los resultados mediante eventos como `partial`, `final` y `end`; `speech.recognize(path)` reconoce un archivo WAV.
- `speech.wake(keywords, options)` abre una sesión de palabras de activación; `thenListen` inicia el dictado automáticamente tras la activación.
- `speech.synthesize(text, path)` sintetiza en un archivo WAV; `speech.speak(text)` y `tts(text, {engine})` hablan mediante el motor TTS del sistema; `speech.models` gestiona los modelos.

******

### Preguntas Frecuentes

******

#### El reconocimiento y la síntesis necesitan Internet?

No. Una vez descargados los modelos, todo se ejecuta en el dispositivo y no se envía ningún audio. El complemento solo se conecta para descargar modelos y actualizar el catálogo.

#### Por qué el complemento solicita el permiso del micrófono por sí mismo?

El audio se graba dentro del proceso del complemento y nunca pasa por AutoJs6. AutoJs6 cede sus capacidades de primer plano al complemento al vincularlo, y el complemento mantiene un servicio en primer plano de tipo micrófono durante una sesión de micrófono, de modo que el dictado continúa cuando AutoJs6 pasa a segundo plano o se apaga la pantalla. Conceda el permiso del micrófono en la pantalla del complemento antes del primer uso.

#### Qué paquete debo instalar?

La mayoría de los teléfonos necesitan `arm64-v8a`. Si no conoce la ABI del dispositivo, instale `universal`: funciona en todos los dispositivos, pero ocupa más.

******

### Permisos Y Seguridad

******

El complemento reduce al máximo sus permisos:

- Micrófono: el audio solo se graba para el dictado, las palabras de activación y la pantalla de prueba, y el micrófono se libera en cuanto termina la sesión o el script.
- Red: solo se usa para descargar modelos y actualizar el catálogo firmado; el reconocimiento y la síntesis funcionan sin conexión.
- Almacenamiento: los modelos se guardan en el directorio privado del complemento y los modelos locales se importan mediante el selector de archivos del sistema, por lo que no se solicita permiso de almacenamiento.
- Entradas protegidas: el servicio de capacidad y la entrada de activación exigen el permiso de complemento de AutoJs6 (`org.autojs.permission.PLUGIN`), así que otras aplicaciones no pueden llamarlos directamente.

******

### Interfaz Del Plugin

******

La siguiente información está dirigida a los desarrolladores del host AutoJs6 y de plugins; el host usa estos identificadores para descubrir el plugin y negociar capacidades:

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

### Hoja De Ruta

******

Los planes, las decisiones de diseño y el progreso se registran como lista de verificación en ROADMAP.md. Los elementos sin marcar son planes, no funciones de la versión actual.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### Historial De Versiones

******

#### v1.0.0

_2026/10/16_

- `Aviso` Versión preliminar de desarrollo: AutoJs6 detecta el complemento, que descarga y administra modelos, reconoce voz en archivos WAV, en PCM suministrado y del micrófono, y detecta actividad de voz y palabras de activación; la síntesis de voz, las pantallas del complemento y el módulo de script speech aún no se entregan
- `Aviso` Requiere AutoJs6 6.8.0 (5451) o posterior (la versión mínima exacta se indicará cuando se integren los cambios del anfitrión)
- `Función` Esqueleto del complemento: protocolo de activación Wake, servicio de identidad del centro de complementos y servicio de capacidad de voz
- `Función` Motor de voz sherpa-onnx 1.13.8 y modelo de detección de actividad de voz silero VAD v5 integrados, con un paquete por ABI para 4 ABI
- `Función` Gestión de modelos: descarga los modelos de voz sin conexión del catálogo firmado desde las publicaciones de los recursos compartidos de AutoJs6, con las publicaciones oficiales de sherpa-onnx como alternativa; cada archivo se verifica con SHA-256 antes de usarse, con descargas reanudables, importación local (tar, tar.bz2, zip), eliminación y modelos predeterminados; los modelos limitados a uso no comercial o sin licencia declarada requieren aceptar antes su licencia
- `Función` Reconocimiento de voz de archivos WAV (enteros de 8, 16, 24 o 32 bits o coma flotante, cualquier frecuencia de muestreo y número de canales, remuestreados a 16 kHz mono en el complemento) con segmentos con marcas de tiempo, además de etiquetas de idioma, emoción y evento para los modelos SenseVoice; los modelos en streaming admiten palabras clave favorecidas
- `Función` Sesiones de dictado en el micrófono o con PCM de 16 kHz suministrado: informan de la disponibilidad, los resultados parciales y finales, los puntos finales, la actividad de voz y el final; el silencio que cierra una frase, el tiempo de espera sin voz y la duración máxima son ajustables; los modelos en streaming actualizan los resultados parciales palabra por palabra, los modelos sin conexión segmento por segmento
- `Función` Detección de actividad de voz: lista los segmentos de voz de un archivo WAV con el silero VAD incluido
- `Función` Palabras de activación: detección de palabras clave de vocabulario abierto; las palabras clave en chino se deletrean en pinyin según su lectura habitual, las palabras en inglés en los fonemas del léxico del modelo, y también se pueden escribir directamente los tokens del modelo; una detección puede iniciar un dictado sobre la misma grabación
- `Función` Durante una sesión de micrófono, el complemento sigue grabando con un servicio en primer plano de tipo micrófono y su notificación, de modo que una sesión iniciada con AutoJs6 en primer plano continúa cuando AutoJs6 pasa a segundo plano o se apaga la pantalla; solo se ejecuta una sesión de micrófono a la vez; sin el permiso del micrófono, las sesiones fallan con PERMISSION_DENIED e indican la pantalla de permisos
- `Función` README, instrucciones del centro de complementos y registros de cambios en 10 idiomas, generados a partir de fuentes JSON
- `Dependencia` Se agrega sherpa-onnx 1.13.8
- `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00
- `Dependencia` Se agrega AndroidX Activity Compose 1.12.2
- `Dependencia` Se agrega AndroidX Core KTX 1.15.0
- `Dependencia` Se agrega Kotlin Coroutines 1.11.0

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación

******

Esta sección está dirigida a desarrolladores que quieran compilar el plugin desde el código fuente.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\verify_native_alignment.py --check
py .python\check_repository.py
```

******

### Licencia

******

El código del proyecto se publica bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). El motor de voz sherpa-onnx usa la licencia Apache-2.0, y onnxruntime y silero VAD usan la licencia MIT; la licencia de cada modelo figura en el catálogo de modelos.

- [Avisos de terceros (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Proyecto sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
