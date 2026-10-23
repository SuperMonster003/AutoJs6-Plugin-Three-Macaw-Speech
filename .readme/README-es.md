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

El complemento es un APK que se instala por separado, con pantallas propias para descargar y gestionar modelos, probarlos y cambiar los ajustes. Los scripts de AutoJs6 lo usan mediante el módulo global `speech`; la síntesis de voz también se registra como motor de texto a voz del sistema Android, y el reconocimiento también se ofrece como entrada de voz del sistema, servicio de reconocimiento de voz del sistema y asistente digital, de modo que otras aplicaciones también pueden usarlos. Los modelos de reconocimiento y síntesis no se incluyen en el APK, sino que se descargan cuando hacen falta; solo el modelo de detección de actividad de voz viene integrado.

> El módulo `speech` en los scripts necesita AutoJs6 6.8.0 (versión interna 5456) o posterior. El motor de texto a voz del sistema, la entrada de voz del sistema, el servicio de reconocimiento de voz, el asistente digital y las pantallas del complemento no dependen de AutoJs6 y funcionan en cuanto el complemento está instalado y los modelos descargados.

******

### Funciones Destacadas

******

- Reconocimiento de voz: dictado continuo desde el micrófono (resultados parciales y finales en tiempo real) y reconocimiento de archivos de audio (WAV, MP3, AAC, Ogg, FLAC y otros, o el sonido de un archivo de vídeo), con modelos sin conexión para chino, inglés, chino e inglés mezclados, japonés, coreano y cantonés. El francés, el ruso y el coreano también tienen modelos en streaming que muestran el texto mientras hablas.
- Puntuación automática: con un modelo de puntuación instalado, cada frase que reconoce un modelo en streaming recibe comas, puntos y signos de interrogación (chino e inglés), y el texto en inglés recupera las mayúsculas; SenseVoice añade su propia puntuación y no se modifica. Los modelos de puntuación se descargan desde el grupo "Puntuación" de la pantalla de inicio.
- Detección de actividad de voz: el silero VAD v5 integrado divide el audio en segmentos de voz por sí solo y también cierra automáticamente las frases dictadas.
- Palabras de activación: escriba la palabra clave como texto (por ejemplo `小六小六` o `hey macaw`) sin entrenar ningún modelo; tras la activación, el dictado puede empezar automáticamente.
- Síntesis de voz: varias voces neuronales en chino e inglés, sintetizadas en archivos WAV o usadas como motor TTS del sistema por el módulo `tts` de AutoJs6 y otras aplicaciones. También hay voces Piper en francés, español, alemán y ruso (solo uso no comercial).
- Entrada de voz del sistema: los botones de micrófono de otras apps, como 3-Stove Agent, pueden usar el reconocimiento sin conexión; dices una frase en un panel inferior y el texto reconocido vuelve a esa app.
- Asistente digital: elige 3-Macaw Speech como app de asistente digital predeterminada, mantén pulsado el botón de encendido o el de inicio, di una frase, edita el texto reconocido si hace falta y entrégalo a 3-Stove Agent, 3-Stone AI u otra app que reciba comandos de voz, o cópialo. También ofrece un servicio de reconocimiento de voz del sistema: en Android 11 y versiones anteriores, elegirlo como asistente suele convertirlo también en el servicio de reconocimiento predeterminado; desde Android 12, elígelo aparte en los ajustes de entrada de voz del sistema (algunos teléfonos no los tienen), y las apps también pueden indicarlo directamente.
- Formas de activación: abre el mismo panel de dictado sin el botón de encendido: un icono de Ajustes rápidos (también en la pantalla de bloqueo), un widget y un acceso directo del icono de la app, un micrófono flotante que puedes arrastrar (desactivado por defecto), un activador por tecla que mantiene pulsada o pulsa dos veces una tecla de volumen (un servicio de accesibilidad que solo filtra teclas, desactivado por defecto) y la tecla de voz de unos auriculares Bluetooth. No se graba nada hasta que el panel escucha, y solo durante los pocos segundos del dictado.
- Palabra de activación permanente (desactivada por defecto): una vez activa, decir una palabra de activación (`小鹦小鹦` y `Hey Macaw` por defecto, editables) abre el mismo panel de dictado, enciende la pantalla si está apagada o bloqueada y lee en voz alta el texto reconocido. Mientras escucha, el indicador del micrófono sigue encendido, hay una notificación fija y la batería se gasta más rápido; se puede fijar un tiempo de escucha (desactivar tras 10, 30 o 60 minutos, o siempre activa) y Escuchar solo mientras se carga. Cuando un script, otra app u otra forma de activación necesita el micrófono, se aparta y vuelve a escuchar después.
- Herramientas del complemento (AutoJs6 6.8.0 o posterior): cuatro herramientas, `speech_listen` (escucha una frase y devuelve el texto reconocido), `speech_speak` (lee un texto en voz alta), `speech_transcribe` (transcribe un archivo de audio del teléfono) y `speech_synthesize` (sintetiza texto en un archivo WAV o M4A). El complemento MCP Server las usa para que un asistente de IA en el ordenador escuche y hable a través del teléfono, y los scripts también pueden llamarlas con `plugins.tools`.
- Gestión de modelos: descarga desde un catálogo firmado, activación solo después de que cada archivo supere su comprobación SHA-256, e importación de modelos locales; los modelos restringidos a uso no comercial muestran su licencia antes de la descarga.
- Pantallas propias: la pantalla de inicio muestra el estado y gestiona los modelos, Probar ensaya directamente el dictado, las palabras de activación y la síntesis y convierte en texto un archivo de audio o vídeo del teléfono, que se puede guardar como texto plano o subtítulos SRT, y los ajustes cambian el idioma, el modo nocturno, el color del tema, el icono del lanzador y los valores predeterminados de reconocimiento y síntesis; la apariencia sigue a AutoJs6 de forma predeterminada.

******

### Capturas de pantalla

******

Las capturas proceden de un Sony XQ-DQ72 (Android 13). El texto dictado es lo que el complemento reconoció de una grabación de prueba de sherpa-onnx reproducida por el altavoz del teléfono; no se muestran datos personales.

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/home.png?raw=true" alt="Inicio: estado y modelos agrupados por uso" width="360" />
      <br />
      <sub>Inicio: estado y modelos agrupados por uso</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/tryout-dictation.png?raw=true" alt="Probar: dictado en directo" width="360" />
      <br />
      <sub>Probar: dictado en directo</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/voice-input.png?raw=true" alt="Entrada de voz del sistema: el panel que abrió otra aplicación" width="360" />
      <br />
      <sub>Entrada de voz del sistema: el panel que abrió otra aplicación</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/assistant.png?raw=true" alt="Asistente digital: editar el resultado y elegir la aplicación que lo recibe" width="360" />
      <br />
      <sub>Asistente digital: editar el resultado y elegir la aplicación que lo recibe</sub>
    </td>
  </tr>
</table>

******

### Instalación y uso

******

1. Descargue desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) el APK que corresponda a la ABI del dispositivo e instálelo: la mayoría de los teléfonos usan `arm64-v8a`; si tiene dudas, elija `universal`, que funciona en todos los dispositivos pero ocupa más.
2. Abra 3-Macaw Speech, conceda el permiso del micrófono en la pantalla de inicio y descargue los modelos que necesite en los grupos de reconocimiento, palabra de activación y síntesis (los modelos que recomienda el catálogo son un buen punto de partida; consulte la tabla de modelos más abajo). En Android 13 y posteriores, la primera descarga también pide mostrar notificaciones de progreso.
3. En AutoJs6: actualice AutoJs6 a 6.8.0 (versión interna 5456) o posterior, active 3-Macaw Speech en el centro de complementos y llame al módulo `speech` en los scripts (consulte API de scripts más abajo). Los ejemplos de AutoJs6 incluyen cuatro scripts listos para ejecutar en Muestras > 3-Macaw Speech.
4. En otras aplicaciones: consulte Texto a voz del sistema más abajo para el motor de texto a voz, y las preguntas frecuentes para la entrada de voz del sistema, el servicio de reconocimiento de voz y el asistente digital.

> Hay un paquete por ABI y un paquete `universal` para todos los dispositivos. Los paquetes no contienen modelos de reconocimiento ni de síntesis; los modelos se descargan cuando hacen falta desde las pantallas del complemento o un script, o se importan desde archivos locales.

******

### Modelos

******

Los modelos de reconocimiento, puntuación, palabra de activación y síntesis ocupan entre 7 MB y 410 MB cada uno, por lo que no se incluyen en el APK. El complemento lleva un catálogo de modelos firmado y descarga los modelos de las versiones inmutables del repositorio de recursos compartidos de AutoJs6, y recurre a la dirección original de sherpa-onnx cuando el espejo no está disponible; en ambos caminos cada archivo debe superar su comprobación SHA-256 antes de activar el modelo. El catálogo 1.2.0 contiene estos modelos (el tamaño es el espacio que ocupan una vez instalados):

| ID del modelo | Uso | Idiomas | Tamaño | Licencia | Uso comercial |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (recomendado) | Reconocimiento (sin conexión, con puntuación) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | Permitido |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (recomendado) | Reconocimiento (en streaming) | zh, en | 199.1 MB | Apache-2.0 | Permitido |
| `asr.zipformer-streaming.en.2023-06-26.int8` (recomendado) | Reconocimiento (en streaming) | en | 70.3 MB | Apache-2.0 | Permitido |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | Reconocimiento (en streaming) | zh | 167.4 MB | Ninguna declarada | Solo no comercial |
| `asr.zipformer-streaming.fr.2023-04-14.int8` (recomendado) | Reconocimiento (en streaming) | fr | 129.0 MB | Apache-2.0 | Permitido |
| `asr.zipformer-streaming.ru.2025-08-16.int8` (recomendado) | Reconocimiento (en streaming) | ru | 28.6 MB | Apache-2.0 | Permitido |
| `asr.zipformer-streaming.ko.2024-06-16.int8` | Reconocimiento (en streaming) | ko | 140.9 MB | Apache-2.0 | Solo no comercial |
| `punct.ct-transformer.zh-en.2024-04-12.int8` (recomendado) | Puntuación | zh, en | 75.5 MB | Apache-2.0 | Permitido |
| `punct.cnn-bilstm.en.2024-08-06.int8` (recomendado) | Puntuación | en | 7.6 MB | Apache-2.0 | Permitido |
| `kws.zipformer.zh-en.2025-12-20` (recomendado) | Palabra de activación | zh, en | 8.8 MB | Apache-2.0 | Permitido |
| `tts.vits.melo.zh-en` (recomendado) | Síntesis (hablantes: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | Permitido |
| `tts.vits.icefall.zh-aishell3` (recomendado) | Síntesis (hablantes: 174, 8 kHz) | zh | 32.8 MB | Ninguna declarada | Desconocido |
| `tts.kokoro.multi-lang.v1_1` | Síntesis (hablantes: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | Permitido |
| `tts.matcha.icefall.zh-baker` | Síntesis (hablantes: 1, 22.05 kHz) | zh | 81.0 MB | Ninguna declarada, MIT (vocoder) | Solo no comercial |
| `tts.vits.piper.en-us-lessac-medium` | Síntesis (hablantes: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Solo no comercial |
| `tts.vits.piper.fr-fr-siwis-medium.int8` | Síntesis (hablantes: 1, 22.05 kHz) | fr | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Solo no comercial |
| `tts.vits.piper.es-es-davefx-medium.int8` | Síntesis (hablantes: 1, 22.05 kHz) | es | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Solo no comercial |
| `tts.vits.piper.de-de-thorsten-medium.int8` | Síntesis (hablantes: 1, 22.05 kHz) | de | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Solo no comercial |
| `tts.vits.piper.ru-ru-denis-medium.int8` | Síntesis (hablantes: 1, 22.05 kHz) | ru | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Solo no comercial |

Los modelos restringidos al uso no comercial o con licencia desconocida muestran su licencia antes de la descarga, y los scripts deben pasar `acceptLicense: true` de forma explícita. Los pesos del modelo chino en streaming de 2025 no declaran licencia y sus datos de entrenamiento incluyen corpus solo para uso no comercial, por lo que se descarga solo desde la dirección original de sherpa-onnx y no se replica en el repositorio de recursos compartidos. Los modelos de síntesis Kokoro y Piper incluyen los datos de pronunciación de eSpeak NG (espeak-ng-data, GPL-3.0-or-later). Los datos de entrenamiento del modelo coreano en streaming (KsponSpeech) solo admiten solicitudes de ciudadanos coreanos y su uso fuera de Corea requiere un acuerdo aparte, por lo que el modelo se marca como de uso no comercial y tampoco se replica: se descarga solo desde la dirección original (unos 418 MB de descarga, 141 MB una vez instalado). Las voces Piper en francés, español, alemán y ruso se ajustaron a partir de la voz inglesa lessac y, como ella, son solo para uso no comercial.

Importar un modelo local, en la pantalla de inicio, acepta un archivo tar, tar.bz2 o zip o una carpeta: los archivos que coinciden con un modelo del catálogo se registran como ese modelo, y los demás se convierten en un modelo personalizado tras indicar su clase, tipo de modelo, nombre e idiomas. La importación usa el selector de archivos del sistema y no necesita permiso de almacenamiento.

******

### Texto a voz del sistema

******

1. Descargue al menos un modelo de síntesis en el grupo de síntesis de la pantalla de inicio del complemento. Para una primera prueba, el más pequeño, `tts.vits.icefall.zh-aishell3` (chino, 174 hablantes), y el modelo de chino e inglés `tts.vits.melo.zh-en` están recomendados.
2. Abra los ajustes de texto a voz del sistema y elija 3-Macaw Speech como motor preferido. La fila Motor de texto a voz del sistema de la tarjeta de estado, en la pantalla de inicio del complemento, abre esa página directamente; si no, suele estar en Ajustes > Accesibilidad > Salida de texto a voz o Ajustes > Sistema > Idiomas e introducción de texto > Salida de texto a voz (los nombres varían según el fabricante).
3. El botón de ajustes junto al motor abre los ajustes de 3-Macaw Speech, cuyo grupo de síntesis define la voz predeterminada de cada idioma y el hablante. La velocidad la decide el ajuste del sistema o la aplicación que pide la lectura; el tono no es compatible.
4. De vuelta en los ajustes del sistema, toque Escuchar un ejemplo para oír la voz predeterminada de ese idioma.

Cada hablante de cada modelo es una voz llamada `<id del modelo>/<número del hablante>`; los hablantes de los modelos bilingües también tienen `<id del modelo>/<número del hablante>/en-US` para el inglés. Los scripts de AutoJs6 pueden usar el motor sin cambiar el ajuste del sistema: `tts(text, { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" })`, o `speech.speak(text, { model, speaker })` para indicar directamente el modelo y el hablante.

******

### API De Script

******

Desde AutoJs6 6.8.0 (versión interna 5456), los scripts usan estas funciones mediante el módulo global `speech` (también disponible como `$speech`). Los tres ejemplos siguientes se ejecutan por separado: un dictado de 30 segundos que añade cada frase a un archivo, una frase tras una palabra de activación, y una síntesis a un archivo seguida de su lectura:

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

- `speech.listen(options)` abre una sesión de dictado que informa mediante los eventos `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` y `end`; `stop()` la termina antes. `speech.recognize(path)` reconoce un archivo de audio (los formatos distintos de WAV requieren AutoJs6 6.8.0 o posterior; también acepta PCM de 16 kHz) y `speech.detectVoice(path)` lista sus segmentos de voz.
- La opción `punctuation` de `speech.listen` y `speech.recognize` es true por defecto: se conserva la puntuación del propio modelo (SenseVoice), los resultados de los modelos en streaming reciben la puntuación de un modelo de puntuación instalado (chino e inglés, con las mayúsculas del inglés restauradas) y quedan igual si ningún modelo de puntuación cubre su idioma; false quita toda la puntuación. Los modelos de puntuación se gestionan solo en la pantalla de inicio del complemento y `speech.models` no los lista.
- `speech.wake(keywords, options)` abre una sesión de palabra de activación con las palabras clave escritas como texto (50 como máximo); con `thenListen`, cada activación se convierte en una ronda de dictado sobre la misma grabación, cuya sesión es la propiedad `listen` del evento `wake`.
- `speech.synthesize(text, path, options)` sintetiza a un archivo WAV (8000 caracteres como máximo, con modelo, hablante y velocidad opcionales); `speech.speak(text, options)` y `tts(text, { engine })` hablan mediante el motor de texto a voz del sistema.
- `speech.models` lista, descarga (`install`, con progreso), importa y elimina modelos y define los modelos predeterminados; `speech.capabilities()` devuelve la versión del complemento, sus funciones y si tiene el permiso del micrófono.
- Todo el complemento tiene una sola sesión de micrófono a la vez (dictado o palabra de activación); sin el permiso del micrófono, las llamadas fallan con `PERMISSION_DENIED`, que se concede en la pantalla de inicio del complemento. El texto reconocido nunca se escribe en el registro de AutoJs6.
- Con AutoJs6 6.8.0 o posterior, los scripts también pueden llamar a estas cuatro herramientas del complemento con `plugins.tools.call`, por ejemplo `plugins.tools.call('speech_speak', { text: 'Hola' })`; los argumentos son los mismos que ve MCP Server. Para los scripts de todos los días, el módulo `speech` es más flexible.
- La [documentación de AutoJs6](https://docs.autojs6.com/#/speech) describe cada método, opción, evento y código de error; Muestras > 3-Macaw Speech de AutoJs6 contiene cuatro ejemplos: dictado a un archivo, palabra de activación y dictado, avisos por voz y comandos de voz entregados al Agent.

******

### Compatibilidad

******

- Android 7.0 (API 24) o posterior, API de destino 37. Paquetes para arm64-v8a, armeabi-v7a, x86_64 y x86, además de un paquete universal; las bibliotecas nativas están alineadas a páginas de memoria de 16 KB y se verificaron en emuladores con páginas de 16 KB.
- AutoJs6: el módulo `speech` de los scripts necesita 6.8.0 (versión interna 5456) o posterior; las pantallas del complemento, el motor de texto a voz del sistema, la entrada de voz del sistema, el servicio de reconocimiento de voz y el asistente digital no dependen de AutoJs6.
- Escucha en segundo plano: una sesión de dictado o de palabra de activación iniciada con AutoJs6 en primer plano sigue escuchando cuando AutoJs6 pasa a segundo plano o se apaga la pantalla (el complemento mantiene la grabación con un servicio en primer plano de micrófono y su notificación). Una sesión abierta cuando AutoJs6 ya está en segundo plano puede ser silenciada por el sistema: recibe entonces un evento `warning` con el código `PERMISSION_DENIED` pero no termina, y el audio vuelve cuando AutoJs6 regresa al primer plano; los scripts que necesitan escuchar en segundo plano deben abrir la sesión mientras AutoJs6 está visible.
- Dispositivos probados: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), y emuladores con API 24, 35 y 37 (páginas de 16 KB).
- Rendimiento: en el Sony G8441, de gama baja, un modelo en streaming se carga en unos 1,7 segundos y SenseVoice en unos 2,8 segundos, y el dictado se procesa unas 3 veces más rápido que el tiempo real; MeloTTS y Kokoro sintetizan más despacio que el tiempo real en estos dispositivos, y AISHELL-3 es el más rápido. Dispositivos más recientes como el Sony XQ-DQ72 cargan en unos 0,5 a 1,2 segundos y sintetizan más rápido que el tiempo real.
- Memoria: un modelo de reconocimiento cargado ocupa unos 210 a 300 MB de memoria nativa. El complemento mantiene los modelos cargados dentro de una cuarta parte de la memoria del dispositivo (de 384 MB a 1,5 GB) y los libera tras 5 minutos de inactividad (de 1 a 30 minutos en los ajustes).
- Asistente digital: el sistema debe permitir elegir una app de asistente digital de terceros. La forma de abrirlo depende del sistema: mantener pulsado el botón de inicio (navegación con tres botones), deslizar hacia arriba desde una esquina inferior, o configurar en los ajustes del sistema el botón de encendido para abrir el asistente digital. Las tabletas ColorOS y HyperOS probadas (Android 15) reservan el botón de encendido y los gestos para su propio asistente; usa allí las formas de activación.
- Formas de activación: probadas en Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), Xiaomi Pad 23046RP50C (HyperOS, Android 15) y en emuladores API 33 y 35. En ColorOS y HyperOS, tocar el icono en la pantalla de bloqueo pide primero desbloquear y después abre el panel; algunos lanzadores aceptan la solicitud de añadir el widget sin mostrar ninguna confirmación, así que añádelo a mano con los pasos de los ajustes. Con la pantalla apagada, los teléfonos no pasan teclas a los servicios de accesibilidad, por lo que el activador por tecla solo funciona con la pantalla encendida. Tecla de voz de los auriculares Bluetooth: con unos Sony WF-1000XM4 y el XQ-DQ72 (Android 13), cada pulsación larga abrió el panel y el dictado usó el micrófono de los auriculares; si otra app (como la app de Google) también responde, el sistema muestra antes un selector. En la tableta HyperOS (Xiaomi Pad 23046RP50C, Android 15), el selector no hizo nada tras elegir 3-Macaw Speech.
- Palabra de activación permanente: probada en Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15) y un emulador API 35 (páginas de 16 KB). Consumo con la pantalla apagada durante 30 minutos (Sony XQ-DQ72, Android 13; estimación de Android según el perfil de energía del dispositivo, con el teléfono conectado por USB y el estado sin cargar simulado): activada, 17,9 mAh y 454 segundos de CPU del complemento (alrededor de un cuarto de un núcleo de CPU de media), grabando todo el tiempo; desactivada, 0,006 mAh, sin el complemento en ejecución. El consumo real varía según el dispositivo.
- Herramientas del complemento: probadas en Sony G8441 (Android 9), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15) y en emuladores con API 24 y 35 (en API 24, solo las partes que no necesitan modelo); las llamadas a través de MCP Server se probaron en Sony G8441 (Android 9).
- Puntuación y modelos añadidos en 1.2.0: probados en Sony XQ-AT72 (Android 12). Con la puntuación activada, las grabaciones en chino, en inglés y mezcladas recibieron puntuación y conservaron las mismas palabras que sin ella; las frases sintetizadas por las voces Piper en ruso y francés se reconocieron de nuevo con el modelo en streaming del mismo idioma con tasas de error de caracteres del 7,9 % y del 0 %; las voces en español y alemán sintetizan con normalidad, y del modelo coreano solo se comprobó que carga (no hay grabación de prueba en coreano).

******

### Preguntas Frecuentes

******

#### El reconocimiento y la síntesis necesitan Internet?

No. Una vez descargados los modelos, todo se ejecuta en el dispositivo y no se envía ningún audio. El complemento solo se conecta para descargar modelos y actualizar el catálogo.

#### Por qué el complemento solicita el permiso del micrófono por sí mismo?

El audio se graba dentro del proceso del complemento y nunca pasa por AutoJs6. AutoJs6 cede sus capacidades de primer plano al complemento al vincularlo, y el complemento mantiene un servicio en primer plano de tipo micrófono durante una sesión de micrófono, de modo que el dictado continúa cuando AutoJs6 pasa a segundo plano o se apaga la pantalla. Conceda el permiso del micrófono en la pantalla del complemento antes del primer uso.

#### Qué paquete debo instalar?

La mayoría de los teléfonos necesitan `arm64-v8a`. Si no conoce la ABI del dispositivo, instale `universal`: funciona en todos los dispositivos, pero ocupa más.

#### Por qué el ejemplo de los ajustes del sistema no suena, o el motor indica que faltan datos de voz?

El motor solo informa de los idiomas de los modelos de síntesis instalados. Sin un modelo de síntesis, o cuando los modelos instalados no cubren el idioma del dispositivo, el sistema muestra que faltan datos de voz y el ejemplo y la velocidad pueden no estar disponibles. Descargue un modelo de síntesis para el idioma del dispositivo en la pantalla de inicio del complemento y vuelva a abrir los ajustes del sistema; el ejemplo usa la voz predeterminada de ese idioma, que se cambia en el grupo de síntesis de los ajustes del complemento.

#### Pueden otras apps usar 3-Macaw Speech para la entrada de voz?

Sí. Con un modelo de reconocimiento instalado y el permiso de micrófono concedido, las apps que abren la entrada de voz del sistema (por ejemplo, el botón de micrófono de 3-Stove Agent) muestran el panel de 3-Macaw Speech. Si también hay otra app de entrada de voz, como Google, Android pregunta cuál usar y puede recordar la elección. Para ocultar esta entrada a otras apps, desactiva 'Usar como entrada de voz del sistema' en el grupo Reconocimiento de los ajustes.

#### Cómo hago que 3-Macaw Speech sea el asistente digital predeterminado?

Abre los ajustes de 3-Macaw Speech y toca Asistente digital en el grupo de reconocimiento: se abren los ajustes del sistema, donde eliges 3-Macaw Speech como app de asistente digital (en algunos sistemas la página tiene un nombre algo distinto). Después mantén pulsado el botón de encendido o el de inicio, o usa el gesto del asistente, para dictar. Si el botón de encendido de tu teléfono tiene otra función asignada, usa el botón de inicio o el gesto del asistente.

#### Por qué la palabra de activación permanente mantiene encendido el indicador del micrófono, y cuánta batería gasta?

Escuchar una palabra de activación todo el tiempo significa grabar todo el tiempo: desde Android 12, la barra de estado muestra el indicador del micrófono (el punto verde) mientras una app graba, y ninguna app puede ocultarlo; los canales de palabras clave de bajo consumo que no encienden el indicador solo están abiertos a los asistentes del sistema preinstalados, no a apps de terceros, aunque una sea el asistente digital predeterminado. Por eso la palabra de activación permanente está desactivada por defecto, y mientras está activa se ven el punto verde y una notificación (que puede desactivarla). En un Sony XQ-DQ72 con la pantalla apagada durante 30 minutos, Android estimó el consumo del complemento en unos 18 mAh con ella activada (alrededor del 0,4% de la batería) y casi nada con ella desactivada. Actívala solo cuando la necesites: fija el tiempo de escucha en 10, 30 o 60 minutos, o activa Escuchar solo mientras se carga (se pausa al desconectar el cargador y continúa al volver a conectarlo); las formas de activación como el mosaico, el widget y el micrófono flotante no graban nada mientras tanto.

#### Por qué tras la palabra de activación solo aparece una notificación "Toca para hablar"?

Desde Android 10, una app en segundo plano solo puede abrir una pantalla directamente si es el asistente digital predeterminado o puede mostrarse sobre otras apps. Si 3-Macaw Speech no es el asistente digital predeterminado, al activar la palabra de activación permanente se pide una vez ese permiso; sin él, al oír la palabra de activación aparece una notificación "Toca para hablar" que abre el panel y desaparece al cabo de un minuto. Para que la palabra de activación abra el panel directamente, haz que 3-Macaw Speech sea el asistente digital predeterminado o permite que se muestre sobre otras apps en los ajustes del sistema.

#### Por qué se me pide desactivar 3-Stove Agent antes de activar la palabra de activación permanente?

Las escuchas permanentes de 3-Macaw Speech, 3-Stone AI y 3-Stove Agent se excluyen entre sí: en un teléfono solo puede escuchar una app a la vez, o competirían por el micrófono. Mientras una está activa, el interruptor de la otra indica qué app escucha y abre sus ajustes para que la desactives allí. Tras un reinicio o una actualización, la palabra de activación permanente no empieza a grabar por sí sola: con el interruptor activado aparece una notificación "Toca para reanudar la palabra de activación permanente", y al tocarla, o al abrir la pantalla de inicio o los ajustes de 3-Macaw Speech, se reanuda; si el tiempo de escucha terminó mientras tanto, el interruptor se desactiva.

#### Cómo escucha y habla un asistente de IA a través de 3-Macaw Speech?

Necesita AutoJs6 6.8.0 o posterior y el complemento MCP Server. 3-Macaw Speech es un complemento oficial, así que `speech_listen`, `speech_speak` y `speech_transcribe` aparecen por defecto en la lista de herramientas de MCP Server (se pueden desactivar en "Herramientas de plugins" de los ajustes de MCP Server); `speech_synthesize` escribe archivos, por lo que también necesita el segundo interruptor del grupo, y si no se indica un lugar sus archivos se guardan en plugin-outputs/speech del directorio de trabajo de AutoJs6. `speech_listen` escucha una frase cada vez (60 segundos como máximo): cuando puede grabar enseguida (por ejemplo, con la palabra de activación permanente activa, o si 3-Macaw Speech es el asistente digital predeterminado o puede mostrarse sobre otras apps) empieza al momento; si no, aparece en el teléfono una notificación "Toca para hablar" y la escucha empieza al tocarla en menos de 1 minuto. Mientras escucha, la escucha permanente de otras apps cede el micrófono y se reanuda después; devuelve BUSY si ya hay otro dictado en curso, y NOT_READY sin el permiso del micrófono o sin un modelo de reconocimiento (ambos se resuelven en la pantalla principal del complemento). `speech_speak` lee con una voz de 3-Macaw Speech para el idioma si la hay, y si no con la voz del sistema.

#### De qué otras formas puedo abrir el panel de dictado sin el botón de encendido?

En los ajustes de 3-Macaw Speech, el grupo Formas de activación las configura: Icono de Ajustes rápidos, toca para añadirlo (Android 13 y posteriores lo confirman con el sistema; las versiones anteriores muestran los pasos manuales) y después toca Comando de voz en Ajustes rápidos; Widget y acceso directo de la pantalla de inicio, pon el widget Toca para hablar en la pantalla de inicio, o mantén pulsado el icono de la app y elige Comando de voz; Micrófono flotante, actívalo y concede el permiso para mostrarse sobre otras apps, y aparecerá un botón de micrófono que puedes arrastrar en el borde de la pantalla; Activador por tecla, activa Activador por tecla de 3-Macaw Speech en los ajustes de accesibilidad del sistema; por defecto es mantener pulsada la tecla de bajar volumen unos 0,8 segundos, y puedes cambiarlo a mantener pulsada la de subir volumen o pulsar dos veces la de bajar volumen. Todas las entradas abren el mismo panel que el asistente digital, donde eliges la app que recibe el texto; si 3-Macaw Speech es el asistente digital predeterminado se abre a través de la sesión del asistente, y si no, el mismo panel se abre directamente.

#### Por qué el activador por tecla necesita un servicio de accesibilidad, y es seguro?

Android solo permite a los servicios de accesibilidad ver las teclas físicas antes que el sistema, por eso el activador por tecla es un servicio de accesibilidad. Solo pide filtrar teclas y no se suscribe a ningún evento de la interfaz, así que no puede leer el contenido de la pantalla ni realizar gestos; solo intercepta la tecla de volumen elegida, una pulsación corta sigue cambiando el volumen, y las teclas no se interceptan con la pantalla apagada ni durante una llamada. Si su interruptor está en gris (apps instaladas desde un archivo en Android 13 y posteriores), abre antes la información de la app 3-Macaw Speech y elige Permitir ajustes restringidos en su menú.

#### Puede el botón de voz de unos auriculares Bluetooth abrir el panel de dictado?

Sí. 3-Macaw Speech responde a la solicitud de comando de voz (VOICE_COMMAND) del servicio Bluetooth: mantener pulsada la tecla de voz o la zona táctil de los auriculares abre el panel de dictado. Desde Android 12, el panel pide una vez el permiso Dispositivos cercanos. Si lo concedes, 3-Macaw Speech se encarga del reconocimiento de voz de los auriculares: escucha por su micrófono y cada pulsación funciona. Si no, escucha por el micrófono del teléfono y algunos auriculares (como los Sony WF-1000XM4) solo abren el panel una pulsación de cada dos. Si otra app también responde a los comandos de voz (la app de Google, el asistente del fabricante), el sistema pregunta primero cuál usar; en HyperOS el selector no hizo nada tras elegir 3-Macaw Speech, un fallo del selector del sistema. Para dejar de usarla, desactiva Tecla de voz de los auriculares en el grupo Formas de activación de los ajustes.

#### Qué hago si falla la descarga de un modelo?

El complemento descarga primero de las versiones de GitHub del repositorio de recursos compartidos de AutoJs6 y cambia por sí solo a la dirección original de sherpa-onnx si eso falla; las descargas interrumpidas se reanudan. Sin acceso a GitHub, descargue el archivo del modelo en otro dispositivo (la dirección del espejo o la original del catálogo) y use Importar un modelo local en la pantalla de inicio; los archivos que coinciden con el catálogo se registran como el modelo del catálogo.

#### Cuánto espacio ocupan los modelos y cómo lo libero?

La tabla de modelos de arriba indica el tamaño instalado de cada modelo. La pantalla de inicio elimina los modelos que ya no necesite, y Limpiar archivos sin referencias, en los ajustes, borra los archivos descargados que ningún modelo necesita, las copias de importación y las instalaciones interrumpidas, y conserva los modelos instalados y las descargas en pausa. Desinstalar el complemento elimina todos los modelos.

#### Por qué el resultado no tiene puntuación?

Los modelos Zipformer en streaming producen palabras sin puntuación. El modelo SenseVoice reconoce la voz segmento a segmento, añade puntuación e indica el idioma, lo que conviene para frases completas: indíquelo con la opción `model` en los scripts o conviértalo en el modelo de reconocimiento predeterminado en los ajustes.

#### Cómo se escribe una palabra de activación y por qué no se activa?

Escriba la palabra clave como texto: el chino se deletrea en pinyin según su lectura habitual, las palabras inglesas según el diccionario del modelo (las que no están en él se rechazan), y los tokens pinyin del modelo pueden escribirse directamente para elegir la lectura de un carácter con varias lecturas. Tres o cuatro sílabas y una expresión poco común provocan menos activaciones por error. Cada palabra clave admite un `threshold` (más bajo, se activa con más facilidad) y un `boost`. Pruébela en la prueba de palabra de activación de la pantalla Probar antes de escribirla en un script.

#### Por qué el APK se distribuye bajo la GPL-3.0 y dónde está el código fuente?

El código propio del complemento está bajo MPL-2.0. La biblioteca oficial de sherpa-onnx para Android enlaza estáticamente eSpeak NG (GPL-3.0-or-later), así que el APK en su conjunto se distribuye bajo la GPL-3.0-or-later. Cada versión adjunta el archivo del código fuente correspondiente (el código fuente y los scripts de compilación de sherpa-onnx con todas sus dependencias), cuyo nombre contiene `corresponding-source` y cuyo SHA-256 figura en `SHA256SUMS`; el código fuente del complemento es este repositorio en la etiqueta de la versión.

******

### Permisos Y Seguridad

******

El complemento reduce al máximo sus permisos:

- Micrófono: el audio solo se graba para el dictado, las palabras de activación, la pantalla de prueba y mientras la palabra de activación permanente está activa, y el micrófono se libera en cuanto termina la sesión o el script.
- Red: solo se usa para descargar modelos y actualizar el catálogo firmado; el reconocimiento y la síntesis funcionan sin conexión.
- Almacenamiento: los modelos se guardan en el directorio privado del complemento; los modelos locales que se importan y los archivos que se reconocen en Probar se eligen mediante el selector de archivos del sistema, y tú eliges dónde guardar una transcripción, por lo que no se solicita permiso de almacenamiento. La última transcripción queda en la caché del complemento hasta que la borras en la pantalla.
- Entradas protegidas: el servicio de capacidad y la entrada de activación exigen el permiso de complemento de AutoJs6 (`org.autojs.permission.PLUGIN`), así que otras aplicaciones no pueden llamarlos directamente; el motor TTS del sistema y sus entradas de comprobación de voces, texto de ejemplo y ajustes están abiertos a todas las aplicaciones, como exige Android, y solo sintetizan voz.
- Entrada de voz del sistema: como el motor de texto a voz del sistema, está abierta a todas las apps; solo graba mientras su panel está en pantalla, solo devuelve el texto reconocido y se puede desactivar en los ajustes.
- Asistente digital: nunca lee la pantalla y no escucha en segundo plano mientras la palabra de activación permanente está desactivada; solo graba mientras su panel está en pantalla, y el texto reconocido solo sale del panel cuando tocas el botón de una app o lo copias (en la pantalla de bloqueo, después de desbloquear). El servicio de reconocimiento de voz solo reconoce para apps que tienen ellas mismas el permiso del micrófono.
- Formas de activación: el icono, el widget, el acceso directo, el micrófono flotante, el activador por tecla y la tecla de voz de los auriculares no graban nada por sí mismos; solo abren el panel de dictado. El activador por tecla es un servicio de accesibilidad que solo filtra teclas: nunca lee el contenido de la pantalla, no realiza gestos y solo intercepta la tecla de volumen elegida (una pulsación corta sigue cambiando el volumen, y las teclas no se interceptan con la pantalla apagada ni durante una llamada). El micrófono flotante necesita el permiso para mostrarse sobre otras apps y sigue en pantalla con una notificación silenciosa; el inicio al arrancar solo sirve para recuperar un micrófono flotante activado tras reiniciar. Desde Android 12, la tecla de voz de los auriculares necesita Dispositivos cercanos, que se pide solo en un panel abierto por los auriculares: solo sirve para que el servicio Bluetooth pase el reconocimiento de voz al panel y conecte el micrófono de los auriculares; no busca dispositivos ni lee la ubicación.
- Palabra de activación permanente: desactivada por defecto y solo se activa desde las pantallas del plugin (tras un reinicio o una actualización se reanuda tocando su notificación; nada graba por sí solo). La palabra de activación se detecta en el dispositivo, no se guarda ni se sube audio y el texto reconocido nunca se registra; mientras está activa graba todo el tiempo, el indicador del micrófono sigue encendido y su notificación puede desactivarla. Excluye las escuchas permanentes de 3-Stone AI y 3-Stove Agent: el sistema permite una a la vez.
- Herramientas del complemento: el servicio de herramientas también está protegido por el permiso de complemento de AutoJs6, así que solo AutoJs6 puede llamarlo directamente. `speech_listen` graba una sola frase por llamada, con una notificación mientras graba (y el indicador del micrófono desde Android 12); `speech_transcribe` solo lee el archivo que le entrega AutoJs6, y `speech_synthesize` devuelve el audio a AutoJs6, que lo escribe en el lugar indicado. Ni el texto reconocido ni el leído se escriben en los registros.

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
tool service: org.autojs.plugin.TOOL_PROVIDER (short name speech: listen, speak, transcribe, synthesize; AutoJs6 6.8.0+)
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### Agradecimientos

******

3-Macaw Speech se basa en los siguientes proyectos de código abierto, modelos y datos. Gracias a sus autores y mantenedores:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) y el equipo de [k2-fsa (Kaldi de nueva generación)](https://github.com/k2-fsa): el entorno de ejecución del reconocimiento, las palabras de activación, la detección de actividad de voz y la síntesis, las exportaciones ONNX de los modelos y la biblioteca para Android (Apache-2.0).
- Modelos entrenados con [icefall](https://github.com/k2-fsa/icefall): los reconocedores Zipformer en streaming (bilingüe de pfluo, inglés de Zengwei, chino 2025 de yuekai, francés de shaojieli, coreano de johnBamma), el detector de palabras clave de vocabulario abierto (pkufool), y los modelos de síntesis AISHELL-3 VITS y Baker Matcha-TTS de k2-fsa.
- [Vosk](https://huggingface.co/alphacep/vosk-model-small-streaming-ru) (Alpha Cephei): el reconocedor ruso pequeño en streaming (Apache-2.0).
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / Alibaba, licencia de modelo FunASR 1.1): el reconocedor multilingüe sin conexión.
- Modelos de puntuación: el modelo de puntuación en chino e inglés CT-Transformer de [FunASR](https://github.com/modelscope/FunASR) (Alibaba DAMO Academy) y el modelo de puntuación y mayúsculas en inglés de [Edge-Punct-Casing](https://github.com/frankyoujian/Edge-Punct-Casing) (frankyoujian), ambos Apache-2.0.
- Los modelos de síntesis [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, grabaciones en chino cedidas por LongMaoData) y [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT), y el vocoder [HiFi-GAN](https://github.com/jik876/hifi-gan) (MIT). Las voces francesa siwis, española davefx, alemana thorsten y rusa denis proceden de [piper-voices](https://huggingface.co/rhasspy/piper-voices) (MIT), todas ajustadas a partir de la voz inglesa lessac.
- Datos de entrenamiento: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), el [corpus de mandarín estándar de Data-Baker](https://www.data-baker.com/open_source.html) (solo uso no comercial), los datos Blizzard 2013 de Lessac Technologies (solo investigación) y [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (dominio público). Los modelos francés, coreano y Piper añadidos en 1.2.0 usan además [Common Voice](https://commonvoice.mozilla.org/) 12 en francés (CC0), [KsponSpeech](https://aihub.or.kr/aihubdata/data/view.do?currMenu=115&topMenu=100&aihubDataSe=realm&dataSetSn=123) (AI Hub, solicitudes solo de ciudadanos coreanos), la [base de datos de síntesis de voz francesa SIWIS](https://datashare.is.ed.ac.uk/handle/10283/2353) (CC BY 4.0, SIWIS database, University of Edinburgh and partners), [Thorsten-Voice](https://github.com/thorstenMueller/Thorsten-Voice) (CC0) y las grabaciones davefx y denis de [NabuCasa voice-datasets](https://github.com/NabuCasa/voice-datasets) (CC0).
- El motor de inferencia [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT), el modelo de actividad de voz [silero VAD](https://github.com/snakers4/silero-vad) (MIT), [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) y [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT) para los fonemas, y [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT) para las lecturas de las palabras de activación en chino.

******

### Hoja De Ruta

******

Los planes, las decisiones de diseño y el progreso se registran como lista de verificación en ROADMAP.md. Los elementos sin marcar son planes, no funciones de la versión actual.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### Historial De Versiones

******

#### v1.2.0

_2026/10/21_

- `Aviso` Nueva palabra de activación permanente, desactivada por defecto: mientras está activa escucha la palabra de activación en el dispositivo, el indicador del micrófono sigue encendido, hay una notificación fija y la batería se gasta más rápido (consulta el README para una medición de 30 minutos con la pantalla apagada). Se recomienda usarla con "Tiempo de escucha" o "Escuchar solo mientras se carga"
- `Función` Palabra de activación permanente (Ajustes > Palabra de activación permanente): al decir una palabra de activación ("小鹦小鹦" y "Hey Macaw" por defecto; editables, con sensibilidad alta, media o baja) se abre el mismo panel de dictado que con el asistente digital; con la pantalla apagada o bloqueada la enciende, y solo hay que desbloquear antes de enviar o copiar
- `Función` Un panel abierto por la palabra de activación lee en voz alta el texto reconocido, con una voz de 3-Macaw Speech de ese idioma si la hay o, si no, con la voz del sistema; se puede desactivar en los ajustes
- `Función` Tiempo de escucha (desactivar tras 10, 30 o 60 minutos, o siempre activa) y Escuchar solo mientras se carga (se pausa al desconectar el cargador y continúa al volver a conectarlo), las mismas opciones que en 3-Stove Agent
- `Función` Si 3-Macaw Speech no es el asistente digital predeterminado, al activar la palabra de activación permanente se pide una vez el permiso para mostrarse sobre otras apps; sin él, al oír la palabra de activación aparece una notificación "Toca para hablar" que abre el panel
- `Función` Cuando un script, otra app u otra forma de activación de este plugin usa el micrófono, la palabra de activación permanente se aparta y vuelve a escuchar cuando terminan
- `Función` Solo puede haber una escucha permanente a la vez entre 3-Stone AI, 3-Stove Agent y 3-Macaw Speech; si otra está activa, se te pide desactivarla primero y puedes abrir sus ajustes directamente
- `Función` Tras un reinicio o una actualización no se graba nada por sí solo: con el interruptor activado aparece una notificación "Toca para reanudar la palabra de activación permanente", y abrir la pantalla de inicio o los ajustes también la reanuda
- `Función` El reconocimiento de archivos de audio (speech.recognize, speech.detectVoice) admite, además de WAV, formatos comunes como MP3, AAC (incluido M4A), Ogg (Vorbis, Opus), FLAC y AMR, así como el sonido de archivos de vídeo (MP4, WebM y otros); el límite sigue siendo 200 MB o 30 minutos (requiere AutoJs6 6.8.0 o posterior; las versiones anteriores solo aceptan WAV)
- `Función` Cuatro herramientas del complemento (AutoJs6 6.8.0 o posterior): speech_listen escucha una frase y devuelve el texto, speech_speak lee en voz alta, speech_transcribe transcribe un archivo de audio del teléfono y speech_synthesize sintetiza texto en un archivo WAV o M4A. El complemento MCP Server las usa para que un asistente de IA en el ordenador escuche y hable a través del teléfono, y los scripts pueden llamarlas con plugins.tools; cuando no se puede grabar enseguida, la escucha muestra primero en el teléfono una notificación "Toca para hablar" y empieza al tocarla
- `Función` Más modelos para descargar: reconocimiento en streaming para francés, ruso y coreano, y voces Piper para francés, español, alemán y ruso; las voces Piper y el modelo coreano son solo para uso no comercial y muestran su licencia antes de la descarga, y el modelo coreano se descarga solo desde la dirección original
- `Función` Puntuación automática: después de descargar un modelo de puntuación desde el nuevo grupo "Puntuación" de la pantalla de inicio, el texto en chino e inglés que reconocen los modelos en streaming recibe comas, puntos y signos de interrogación, y el inglés recupera las mayúsculas; SenseVoice añade su propia puntuación y no se modifica; los scripts pueden seguir pasando punctuation: false para quitar toda la puntuación
- `Función` Nueva pestaña "Archivo" en Probar: elige un archivo de audio o vídeo (MP3, M4A, WAV y más, hasta 200 MB y 30 minutos) y obtén como texto lo que se dice en él sin escribir un script, en segmentos con sus tiempos que puedes copiar o guardar como texto plano o subtítulos SRT; el modelo, el idioma y la puntuación automática se eligen en la pestaña; el reconocimiento sigue al salir de la pantalla o apagarla, y una notificación muestra el progreso con un botón para cancelar; el último resultado sigue ahí la próxima vez que abras la pestaña

#### v1.1.0

_2026/10/21_

- `Aviso` Nuevas formas de activación: no se graba nada entretanto; tocar el icono de Ajustes rápidos, el widget, el acceso directo o el micrófono flotante, o pulsar una tecla de volumen o la tecla de voz de unos auriculares Bluetooth, abre el mismo panel de dictado que el asistente digital
- `Aviso` Nuevos permisos: mostrarse sobre otras apps (solo para el micrófono flotante, desactivado por defecto), inicio al arrancar (solo para recuperar un micrófono flotante activado tras reiniciar), un servicio en primer plano de uso especial (solo mantiene el micrófono flotante en pantalla, nunca graba) y dispositivos cercanos (desde Android 12, se pide solo cuando la tecla de voz de los auriculares abre el panel, para encargarse del reconocimiento de voz y el micrófono de los auriculares). El activador por tecla se activa en los ajustes de accesibilidad del sistema y solo filtra teclas
- `Función` Icono de Ajustes rápidos (Comando de voz): añádelo en el grupo Formas de activación de los ajustes (Android 13 y posteriores lo confirman con el sistema; las versiones anteriores muestran los pasos manuales) y tócalo en Ajustes rápidos para hablar; en la pantalla de bloqueo puedes dictar directamente y solo desbloqueas antes de enviar o copiar (ColorOS y HyperOS piden desbloquear primero); mantener pulsado el icono abre los ajustes de Formas de activación
- `Función` Micrófono flotante: desactivado por defecto, al activarlo pide el permiso para mostrarse sobre otras apps; un botón redondo que arrastras por el borde de la pantalla y tocas para hablar; el botón no graba nada por sí mismo, sigue en pantalla con una notificación silenciosa (desde la que se puede desactivar) y vuelve tras reiniciar mientras esté activado; se oculta mientras se muestra el panel de dictado y puede mostrarse junto a la bola flotante de 3-Stove Agent
- `Función` El widget de la pantalla de inicio (Toca para hablar) y el acceso directo de mantener pulsado el icono de la app (Comando de voz) abren el panel de dictado; los ajustes pueden poner el widget en la pantalla de inicio y muestran los pasos manuales cuando el lanzador no lo admite
- `Función` Activador por tecla: un servicio de accesibilidad que solo filtra teclas y nunca lee el contenido de la pantalla, que se activa en los ajustes de accesibilidad del sistema; por defecto, mantener pulsada la tecla de bajar volumen unos 0,8 segundos abre el panel de dictado, y puede cambiarse a mantener pulsada la de subir volumen o pulsar dos veces la de bajar volumen; una pulsación corta sigue cambiando el volumen, y las teclas no se interceptan con la pantalla apagada ni durante una llamada
- `Función` Tecla de voz de los auriculares Bluetooth: mantener pulsada la tecla de voz o la zona táctil de los auriculares abre el panel de dictado (3-Macaw Speech responde a la solicitud de comando de voz VOICE_COMMAND). Con Dispositivos cercanos permitido escucha por el micrófono de los auriculares y cada pulsación funciona. Si otra app también responde, el sistema pregunta primero cuál usar. Se desactiva con Tecla de voz de los auriculares en el grupo Formas de activación de los ajustes
- `Función` Todas las entradas abren el mismo panel que mantener pulsado el botón de encendido: a través de la sesión del asistente si 3-Macaw Speech es el asistente digital predeterminado, y si no, el mismo panel directamente; cuando el sistema no muestra la sesión del asistente (por ejemplo, tras tocar el icono en la pantalla de bloqueo y desbloquear), el panel también se abre directamente
- `Función` Las aplicaciones empaquetadas usan una entrada separada para reconocimiento y modelos, con su propio permiso de micrófono
- `Corrección` El panel de dictado graba en cuanto se abre: mientras el modelo aún se carga (la primera vez, o tras 5 minutos sin uso) ya no se pierden las primeras palabras y una frase corta como hola ya no se queda sin respuesta

#### v1.0.0

_2026/10/20_

- `Aviso` Primera versión. El módulo speech de los scripts necesita AutoJs6 6.8.0 (5456) o posterior; las pantallas del complemento, el motor de texto a voz del sistema, la entrada de voz del sistema, el servicio de reconocimiento de voz y el asistente digital no dependen de AutoJs6
- `Aviso` Los modelos de reconocimiento y síntesis no forman parte del paquete y se descargan desde las pantallas del complemento o un script; la aplicación en su conjunto se distribuye bajo la GNU GPL 3.0 o posterior (el entorno de voz enlaza estáticamente eSpeak NG), y cada versión incluye el código fuente correspondiente
- `Función` Complemento de voz de AutoJs6: una vez activado en el centro de complementos, los scripts usan el reconocimiento de voz, las palabras de activación, la detección de actividad de voz, la síntesis de voz y la gestión de modelos mediante el módulo speech
- `Función` Motor de voz sherpa-onnx 1.13.8 y modelo de detección de actividad de voz silero VAD v5 integrados, con un paquete por ABI para 4 ABI
- `Función` Gestión de modelos: descarga los modelos de voz sin conexión del catálogo firmado desde las publicaciones de los recursos compartidos de AutoJs6, con las publicaciones oficiales de sherpa-onnx como alternativa; cada archivo se verifica con SHA-256 antes de usarse, con descargas reanudables, importación local (tar, tar.bz2, zip), eliminación y modelos predeterminados; los modelos limitados a uso no comercial o sin licencia declarada requieren aceptar antes su licencia
- `Función` Reconocimiento de voz de archivos WAV (enteros de 8, 16, 24 o 32 bits o coma flotante, cualquier frecuencia de muestreo y número de canales, remuestreados a 16 kHz mono en el complemento) con segmentos con marcas de tiempo, además de etiquetas de idioma, emoción y evento para los modelos SenseVoice; los modelos en streaming admiten palabras clave favorecidas
- `Función` Sesiones de dictado en el micrófono o con PCM de 16 kHz suministrado: informan de la disponibilidad, los resultados parciales y finales, los puntos finales, la actividad de voz y el final; el silencio que cierra una frase, el tiempo de espera sin voz y la duración máxima son ajustables; los modelos en streaming actualizan los resultados parciales palabra por palabra, los modelos sin conexión segmento por segmento
- `Función` Detección de actividad de voz: lista los segmentos de voz de un archivo WAV con el silero VAD incluido
- `Función` Palabras de activación: detección de palabras clave de vocabulario abierto; las palabras clave en chino se deletrean en pinyin según su lectura habitual, las palabras en inglés en los fonemas del léxico del modelo, y también se pueden escribir directamente los tokens del modelo; una detección puede iniciar un dictado sobre la misma grabación
- `Función` Durante una sesión de micrófono, el complemento sigue grabando con un servicio en primer plano de tipo micrófono y su notificación, de modo que una sesión iniciada con AutoJs6 en primer plano continúa cuando AutoJs6 pasa a segundo plano o se apaga la pantalla; solo se ejecuta una sesión de micrófono a la vez; sin el permiso del micrófono, las sesiones fallan con PERMISSION_DENIED e indican la pantalla de permisos
- `Función` Síntesis de voz: los modelos de síntesis instalados (VITS, Matcha y Kokoro; el catálogo tiene 5 modelos en chino e inglés) convierten hasta 8000 caracteres de texto en un archivo WAV frase por frase, con la frecuencia de muestreo del modelo; se pueden elegir el hablante y la velocidad (de 0.5 a 2 veces), el progreso cuenta los caracteres terminados y la síntesis se puede cancelar en cualquier momento; un modelo con un solo hablante ignora el hablante elegido y lo indica
- `Función` Motor TTS del sistema: con un modelo de síntesis instalado, 3-Macaw Speech se puede elegir en los ajustes de texto a voz del sistema, y el módulo tts de AutoJs6 y otras aplicaciones leen en voz alta con él; cada hablante es una voz, los modelos bilingües añaden una voz en inglés para cada hablante y la velocidad de habla sigue el ajuste del sistema o de la aplicación; sin un modelo instalado, el motor indica que faltan datos de voz
- `Función` Pantalla de inicio: muestra la versión del motor de voz, el número de modelos instalados, el permiso del micrófono (con un botón para concederlo) y si 3-Macaw Speech es el motor TTS preferido del sistema
- `Función` Modelos en la pantalla de inicio, agrupados en reconocimiento, palabras de activación, síntesis y detección de actividad de voz, con su tamaño, idiomas y licencia: descargar, pausar, reanudar, cancelar, eliminar y establecer como predeterminado; el progreso de la descarga también aparece en una notificación; los modelos limitados a uso no comercial o sin licencia declarada piden aceptar antes su licencia, y una descarga con datos móviles u otra red de uso medido pide confirmación antes de empezar
- `Función` Importación de un modelo local desde un archivo tar, tar.bz2 o zip o desde una carpeta: los archivos que coinciden con un modelo del catálogo se registran como ese modelo, y los demás se convierten en un modelo propio después de indicar su clase, tipo de modelo, nombre, idiomas e ID
- `Función` Probar: dictado en directo (resultados parciales en gris) con elección de modelo e idioma, prueba de palabras de activación que lista las detecciones con su palabra clave y su puntuación, y síntesis de voz de su texto con elección de modelo, hablante y velocidad, para reproducirla o guardarla como archivo WAV; al salir de la pantalla se libera el micrófono
- `Función` Entrada de voz del sistema: los botones de micrófono de otras apps, como 3-Stove Agent, pueden usar el reconocimiento sin conexión de 3-Macaw Speech: di una frase en un panel inferior y el texto reconocido vuelve directamente a esa app; termina tras una pausa, o puedes detenerla o cancelarla antes; sigue la sensibilidad de fin de frase y la detección de actividad de voz de los ajustes y se puede desactivar en su grupo Reconocimiento
- `Función` Asistente digital: tras elegir 3-Macaw Speech como app de asistente digital predeterminada en los ajustes del sistema, mantén pulsado el botón de encendido o el de inicio (o usa el gesto del asistente) y di una frase en un panel inferior; el texto reconocido se puede editar y luego un botón lo entrega a una app que recibe comandos de voz, como 3-Stove Agent o 3-Stone AI, o lo copias o vuelves a hablar; no se envía nada hasta que tocas un botón. También funciona en la pantalla de bloqueo y desbloquea antes de enviar o copiar. Nunca lee la pantalla y no escucha en segundo plano. El grupo de reconocimiento de los ajustes indica si es el asistente predeterminado y abre los ajustes del sistema para elegirlo
- `Función` Servicio de reconocimiento de voz del sistema: las apps que usan el reconocedor de voz de Android (SpeechRecognizer) pueden indicar 3-Macaw Speech directamente y obtener su reconocimiento sin conexión, con resultados parciales; cuando es el servicio de reconocimiento predeterminado, también lo usan las apps que no indican ninguno. En Android 11 y versiones anteriores, elegirlo como asistente digital suele convertirlo también en el predeterminado; desde Android 12, elígelo aparte en los ajustes de entrada de voz del sistema (algunos teléfonos no los tienen). Cuando otra app mantiene la escucha de una palabra de activación a través de 3-Macaw Speech (3-Stone AI, por ejemplo), el asistente digital, la entrada de voz del sistema, el servicio de reconocimiento de voz y la pantalla de prueba siguen pudiendo usar el micrófono: mientras tanto la detección de la palabra de activación se pausa y se reanuda sola después
- `Función` Ajustes: idioma, modo nocturno, color del tema e icono del lanzador (siguen a AutoJs6 de forma predeterminada); modelos de reconocimiento y síntesis predeterminados, sensibilidad de fin de frase, detección de actividad de voz, hablante, velocidad y voz predeterminada por idioma para la pantalla de prueba y el motor TTS del sistema (los scripts mantienen los valores predeterminados documentados); cuándo se liberan los modelos inactivos, actualización del catálogo y limpieza de archivos sobrantes; el botón de ajustes de 3-Macaw Speech en los ajustes de texto a voz del sistema abre esta pantalla
- `Función` Acerca de: versión, historial de versiones incluido en el complemento, búsqueda de actualizaciones (automática como máximo una vez cada 12 horas, se puede ignorar una versión) y licencias y avisos: la aplicación en su conjunto se distribuye bajo la GNU GPL 3.0 o posterior sin ninguna garantía, con los avisos de terceros, el texto completo de la GPL, las condiciones de uso de Unicode y dónde está el código fuente correspondiente
- `Función` Icono del lanzador: icono adaptable (claro), icono adaptable (oscuro), icono adaptable (automático, predeterminado) o icono con fondo transparente
- `Función` Pantallas, README, instrucciones del centro de complementos y registro de cambios en 10 idiomas: chino simplificado, chino tradicional (Hong Kong, Taiwán), inglés, francés, español, japonés, coreano, ruso y árabe
- `Dependencia` Se agrega sherpa-onnx 1.13.8
- `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00
- `Dependencia` Se agrega AndroidX Activity Compose 1.12.2
- `Dependencia` Se agrega AndroidX Core KTX 1.15.0
- `Dependencia` Se agrega Kotlin Coroutines 1.11.0
- `Dependencia` Se agrega Material Color Utilities (MaterialKolor) 4.1.1

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
py -I .python\corresponding_source.py build --downloads <directory outside the repository>
```

******

### Licencia

******

El código fuente del complemento está bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). La biblioteca oficial de sherpa-onnx para Android (Apache-2.0) enlaza estáticamente eSpeak NG y UCD Tools, que están bajo la GNU GPL versión 3 o posterior, así que el APK publicado en su conjunto se distribuye bajo la GPL-3.0-or-later (la sección 3.3 de la MPL-2.0 permite esta combinación), sin ninguna garantía. Cada versión adjunta el código fuente correspondiente `autojs6-plugin-three-macaw-speech-corresponding-source-sherpa-onnx-v1.13.8-ac68c80b.tar`: el código fuente y los scripts de compilación de sherpa-onnx 1.13.8 y cada archivo de dependencia que su compilación fija por URL y SHA-256; su SHA-256 figura en el `SHA256SUMS` de la versión, y `.python/corresponding_source.py` lo reconstruye y lo comprueba. ONNX Runtime y silero VAD están bajo la licencia MIT; la licencia de cada modelo figura en la tabla de modelos de arriba. La página Acerca de del complemento muestra los textos completos de las licencias y los avisos de terceros.

- [Avisos de terceros (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentación de AutoJs6: el módulo speech: https://docs.autojs6.com/#/speech
- Proyecto sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
