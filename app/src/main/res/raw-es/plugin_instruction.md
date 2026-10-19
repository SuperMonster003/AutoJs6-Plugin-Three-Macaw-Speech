3-Macaw Speech usa [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) para ejecutar sin conexión, en el propio dispositivo, el reconocimiento de voz, la detección de actividad de voz, la detección de palabras de activación y la síntesis de voz neuronal. Todo se calcula localmente y no se envía ningún audio.

> El módulo `speech` en los scripts necesita AutoJs6 6.8.0 (versión interna 5456) o posterior. El motor de texto a voz del sistema, la entrada de voz del sistema, el servicio de reconocimiento de voz, el asistente digital y las pantallas del complemento no dependen de AutoJs6 y funcionan en cuanto el complemento está instalado y los modelos descargados.

### Funciones Destacadas

- Reconocimiento de voz: dictado continuo desde el micrófono (resultados parciales y finales en tiempo real) y reconocimiento de archivos WAV, con modelos sin conexión para chino, inglés, chino e inglés mezclados, japonés, coreano y cantonés.
- Detección de actividad de voz: el silero VAD v5 integrado divide el audio en segmentos de voz por sí solo y también cierra automáticamente las frases dictadas.
- Palabras de activación: escriba la palabra clave como texto (por ejemplo `小六小六` o `hey macaw`) sin entrenar ningún modelo; tras la activación, el dictado puede empezar automáticamente.
- Síntesis de voz: varias voces neuronales en chino e inglés, sintetizadas en archivos WAV o usadas como motor TTS del sistema por el módulo `tts` de AutoJs6 y otras aplicaciones.
- Entrada de voz del sistema: los botones de micrófono de otras apps, como 3-Stove Agent, pueden usar el reconocimiento sin conexión; dices una frase en un panel inferior y el texto reconocido vuelve a esa app.
- Asistente digital: elige 3-Macaw Speech como app de asistente digital predeterminada, mantén pulsado el botón de encendido o el de inicio, di una frase, edita el texto reconocido si hace falta y entrégalo a 3-Stove Agent, 3-Stone AI u otra app que reciba comandos de voz, o cópialo. También ofrece un servicio de reconocimiento de voz del sistema: en Android 11 y versiones anteriores, elegirlo como asistente suele convertirlo también en el servicio de reconocimiento predeterminado; desde Android 12, elígelo aparte en los ajustes de entrada de voz del sistema (algunos teléfonos no los tienen), y las apps también pueden indicarlo directamente.
- Gestión de modelos: descarga desde un catálogo firmado, activación solo después de que cada archivo supere su comprobación SHA-256, e importación de modelos locales; los modelos restringidos a uso no comercial muestran su licencia antes de la descarga.
- Pantallas propias: la pantalla de inicio muestra el estado y gestiona los modelos, Probar ensaya directamente el dictado, las palabras de activación y la síntesis, y los ajustes cambian el idioma, el modo nocturno, el color del tema, el icono del lanzador y los valores predeterminados de reconocimiento y síntesis; la apariencia sigue a AutoJs6 de forma predeterminada.

### Instalación y uso

1. Descargue desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) el APK que corresponda a la ABI del dispositivo e instálelo: la mayoría de los teléfonos usan `arm64-v8a`; si tiene dudas, elija `universal`, que funciona en todos los dispositivos pero ocupa más.
2. Abra 3-Macaw Speech, conceda el permiso del micrófono en la pantalla de inicio y descargue los modelos que necesite en los grupos de reconocimiento, palabra de activación y síntesis (los modelos que recomienda el catálogo son un buen punto de partida; consulte la tabla de modelos más abajo). En Android 13 y posteriores, la primera descarga también pide mostrar notificaciones de progreso.
3. En AutoJs6: actualice AutoJs6 a 6.8.0 (versión interna 5456) o posterior, active 3-Macaw Speech en el centro de complementos y llame al módulo `speech` en los scripts (consulte API de scripts más abajo). Los ejemplos de AutoJs6 incluyen cuatro scripts listos para ejecutar en Muestras > 3-Macaw Speech.
4. En otras aplicaciones: consulte Texto a voz del sistema más abajo para el motor de texto a voz, y las preguntas frecuentes para la entrada de voz del sistema, el servicio de reconocimiento de voz y el asistente digital.

Consulte la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech) para las instrucciones completas, la lista de modelos y las preguntas frecuentes, y la [documentación de AutoJs6](https://docs.autojs6.com/#/speech) para la API de scripts.
