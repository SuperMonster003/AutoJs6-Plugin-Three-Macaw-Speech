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

- Reconocimiento de voz: dictado continuo desde el micrófono (resultados parciales y finales en tiempo real) y reconocimiento de archivos WAV, con modelos sin conexión para chino, inglés, chino e inglés mezclados, japonés, coreano y cantonés.
- Detección de actividad de voz: el silero VAD v5 integrado divide el audio en segmentos de voz por sí solo y también cierra automáticamente las frases dictadas.
- Palabras de activación: escriba la palabra clave como texto (por ejemplo `小六小六` o `hey macaw`) sin entrenar ningún modelo; tras la activación, el dictado puede empezar automáticamente.
- Síntesis de voz: varias voces neuronales en chino e inglés, sintetizadas en archivos WAV o usadas como motor TTS del sistema por el módulo `tts` de AutoJs6 y otras aplicaciones.
- Entrada de voz del sistema: los botones de micrófono de otras apps, como 3-Stove Agent, pueden usar el reconocimiento sin conexión; dices una frase en un panel inferior y el texto reconocido vuelve a esa app.
- Asistente digital: elige 3-Macaw Speech como app de asistente digital predeterminada, mantén pulsado el botón de encendido o el de inicio, di una frase, edita el texto reconocido si hace falta y entrégalo a 3-Stove Agent, 3-Stone AI u otra app que reciba comandos de voz, o cópialo. También ofrece un servicio de reconocimiento de voz del sistema: en Android 11 y versiones anteriores, elegirlo como asistente suele convertirlo también en el servicio de reconocimiento predeterminado; desde Android 12, elígelo aparte en los ajustes de entrada de voz del sistema (algunos teléfonos no los tienen), y las apps también pueden indicarlo directamente.
- Gestión de modelos: descarga desde un catálogo firmado, activación solo después de que cada archivo supere su comprobación SHA-256, e importación de modelos locales; los modelos restringidos a uso no comercial muestran su licencia antes de la descarga.
- Pantallas propias: la pantalla de inicio muestra el estado y gestiona los modelos, Probar ensaya directamente el dictado, las palabras de activación y la síntesis, y los ajustes cambian el idioma, el modo nocturno, el color del tema, el icono del lanzador y los valores predeterminados de reconocimiento y síntesis; la apariencia sigue a AutoJs6 de forma predeterminada.

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

Los modelos de reconocimiento, palabra de activación y síntesis ocupan entre 9 MB y 410 MB cada uno, por lo que no se incluyen en el APK. El complemento lleva un catálogo de modelos firmado y descarga los modelos de las versiones inmutables del repositorio de recursos compartidos de AutoJs6, y recurre a la dirección original de sherpa-onnx cuando el espejo no está disponible; en ambos caminos cada archivo debe superar su comprobación SHA-256 antes de activar el modelo. El catálogo 1.0.0 contiene estos modelos (el tamaño es el espacio que ocupan una vez instalados):

| ID del modelo | Uso | Idiomas | Tamaño | Licencia | Uso comercial |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (recomendado) | Reconocimiento (sin conexión, con puntuación) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | Permitido |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (recomendado) | Reconocimiento (en streaming) | zh, en | 199.1 MB | Apache-2.0 | Permitido |
| `asr.zipformer-streaming.en.2023-06-26.int8` (recomendado) | Reconocimiento (en streaming) | en | 70.3 MB | Apache-2.0 | Permitido |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | Reconocimiento (en streaming) | zh | 167.4 MB | Ninguna declarada | Solo no comercial |
| `kws.zipformer.zh-en.2025-12-20` (recomendado) | Palabra de activación | zh, en | 8.8 MB | Apache-2.0 | Permitido |
| `tts.vits.melo.zh-en` (recomendado) | Síntesis (hablantes: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | Permitido |
| `tts.vits.icefall.zh-aishell3` (recomendado) | Síntesis (hablantes: 174, 8 kHz) | zh | 32.8 MB | Ninguna declarada | Desconocido |
| `tts.kokoro.multi-lang.v1_1` | Síntesis (hablantes: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | Permitido |
| `tts.matcha.icefall.zh-baker` | Síntesis (hablantes: 1, 22.05 kHz) | zh | 81.0 MB | Ninguna declarada, MIT (vocoder) | Solo no comercial |
| `tts.vits.piper.en-us-lessac-medium` | Síntesis (hablantes: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Solo no comercial |

Los modelos restringidos al uso no comercial o con licencia desconocida muestran su licencia antes de la descarga, y los scripts deben pasar `acceptLicense: true` de forma explícita. Los pesos del modelo chino en streaming de 2025 no declaran licencia y sus datos de entrenamiento incluyen corpus solo para uso no comercial, por lo que se descarga solo desde la dirección original de sherpa-onnx y no se replica en el repositorio de recursos compartidos. Los modelos de síntesis Kokoro y Piper incluyen los datos de pronunciación de eSpeak NG (espeak-ng-data, GPL-3.0-or-later).

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

- `speech.listen(options)` abre una sesión de dictado que informa mediante los eventos `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` y `end`; `stop()` la termina antes. `speech.recognize(path)` reconoce un archivo WAV (también acepta PCM de 16 kHz) y `speech.detectVoice(path)` lista sus segmentos de voz.
- `speech.wake(keywords, options)` abre una sesión de palabra de activación con las palabras clave escritas como texto (50 como máximo); con `thenListen`, cada activación se convierte en una ronda de dictado sobre la misma grabación, cuya sesión es la propiedad `listen` del evento `wake`.
- `speech.synthesize(text, path, options)` sintetiza a un archivo WAV (8000 caracteres como máximo, con modelo, hablante y velocidad opcionales); `speech.speak(text, options)` y `tts(text, { engine })` hablan mediante el motor de texto a voz del sistema.
- `speech.models` lista, descarga (`install`, con progreso), importa y elimina modelos y define los modelos predeterminados; `speech.capabilities()` devuelve la versión del complemento, sus funciones y si tiene el permiso del micrófono.
- Todo el complemento tiene una sola sesión de micrófono a la vez (dictado o palabra de activación); sin el permiso del micrófono, las llamadas fallan con `PERMISSION_DENIED`, que se concede en la pantalla de inicio del complemento. El texto reconocido nunca se escribe en el registro de AutoJs6.
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
- Asistente digital: el sistema debe permitir elegir una aplicación de asistente digital de terceros. Algunos sistemas reservan la pulsación larga del botón de encendido para su propio asistente; en ese caso use el botón de inicio o el gesto de asistente. ColorOS y HyperOS aún no se han verificado.

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

#### Por qué el asistente no escucha una palabra de activación todo el tiempo?

Escuchar todo el tiempo mantiene encendidos el micrófono, una notificación permanente y el indicador del micrófono, y gasta más batería, así que el asistente digital solo escucha cuando pulsas el botón del asistente. Apps como 3-Stone AI y 3-Stove Agent pueden ofrecer su propio interruptor de escucha permanente (desactivado por defecto); mientras escuchan, el asistente digital, la entrada de voz y el reconocimiento de voz siguen funcionando, y la detección de la palabra de activación se pausa y se reanuda sola después. Una palabra de activación propia de 3-Macaw Speech está prevista para una versión posterior.

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

- Micrófono: el audio solo se graba para el dictado, las palabras de activación y la pantalla de prueba, y el micrófono se libera en cuanto termina la sesión o el script.
- Red: solo se usa para descargar modelos y actualizar el catálogo firmado; el reconocimiento y la síntesis funcionan sin conexión.
- Almacenamiento: los modelos se guardan en el directorio privado del complemento y los modelos locales se importan mediante el selector de archivos del sistema, por lo que no se solicita permiso de almacenamiento.
- Entradas protegidas: el servicio de capacidad y la entrada de activación exigen el permiso de complemento de AutoJs6 (`org.autojs.permission.PLUGIN`), así que otras aplicaciones no pueden llamarlos directamente; el motor TTS del sistema y sus entradas de comprobación de voces, texto de ejemplo y ajustes están abiertos a todas las aplicaciones, como exige Android, y solo sintetizan voz.
- Entrada de voz del sistema: como el motor de texto a voz del sistema, está abierta a todas las apps; solo graba mientras su panel está en pantalla, solo devuelve el texto reconocido y se puede desactivar en los ajustes.
- Asistente digital: nunca lee la pantalla y no escucha en segundo plano; solo graba mientras su panel está en pantalla, y el texto reconocido solo sale del panel cuando tocas el botón de una app o lo copias (en la pantalla de bloqueo, después de desbloquear). El servicio de reconocimiento de voz solo reconoce para apps que tienen ellas mismas el permiso del micrófono.

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
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### Agradecimientos

******

3-Macaw Speech se basa en los siguientes proyectos de código abierto, modelos y datos. Gracias a sus autores y mantenedores:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) y el equipo de [k2-fsa (Kaldi de nueva generación)](https://github.com/k2-fsa): el entorno de ejecución del reconocimiento, las palabras de activación, la detección de actividad de voz y la síntesis, las exportaciones ONNX de los modelos y la biblioteca para Android (Apache-2.0).
- Modelos entrenados con [icefall](https://github.com/k2-fsa/icefall): los reconocedores Zipformer en streaming (bilingüe de pfluo, inglés de Zengwei, chino 2025 de yuekai), el detector de palabras clave de vocabulario abierto (pkufool), y los modelos de síntesis AISHELL-3 VITS y Baker Matcha-TTS de k2-fsa.
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / Alibaba, licencia de modelo FunASR 1.1): el reconocedor multilingüe sin conexión.
- Los modelos de síntesis [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, grabaciones en chino cedidas por LongMaoData) y [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT), y el vocoder [HiFi-GAN](https://github.com/jik876/hifi-gan) (MIT).
- Datos de entrenamiento: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), el [corpus de mandarín estándar de Data-Baker](https://www.data-baker.com/open_source.html) (solo uso no comercial), los datos Blizzard 2013 de Lessac Technologies (solo investigación) y [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (dominio público).
- El motor de inferencia [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT), el modelo de actividad de voz [silero VAD](https://github.com/snakers4/silero-vad) (MIT), [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) y [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT) para los fonemas, y [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT) para las lecturas de las palabras de activación en chino.

******

### Hoja De Ruta

******

Los planes, las decisiones de diseño y el progreso se registran como lista de verificación en ROADMAP.md. Los elementos sin marcar son planes, no funciones de la versión actual.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### Historial De Versiones

******

#### v1.1.0

_2026/10/20_

- `Función` Las aplicaciones empaquetadas usan una entrada separada para reconocimiento y modelos, con su propio permiso de micrófono

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
