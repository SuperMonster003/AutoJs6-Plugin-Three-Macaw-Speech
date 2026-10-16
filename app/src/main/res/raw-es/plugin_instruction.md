3-Macaw Speech usa [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) para ejecutar sin conexión, en el propio dispositivo, el reconocimiento de voz, la detección de actividad de voz, la detección de palabras de activación y la síntesis de voz neuronal. Todo se calcula localmente y no se envía ningún audio.

> Esta es una versión preliminar de desarrollo: AutoJs6 detecta el complemento, que descarga y administra modelos, reconoce voz, detecta actividad de voz y palabras de activación y sintetiza voz, también como motor TTS del sistema; sus propias pantallas administran los modelos, permiten probarlos y reúnen los ajustes. La versión de AutoJs6 que ofrece el módulo de script `speech` aún no se ha publicado. El texto siguiente describe el diseño de la versión 1.0.0; consulte la hoja de ruta para ver el progreso.

### Funciones Destacadas

- Reconocimiento de voz: dictado continuo desde el micrófono (resultados parciales y finales en tiempo real) y reconocimiento de archivos WAV, con modelos sin conexión para chino, inglés, chino e inglés mezclados, japonés, coreano y cantonés.
- Detección de actividad de voz: el silero VAD v5 integrado divide el audio en segmentos de voz por sí solo y también cierra automáticamente las frases dictadas.
- Palabras de activación: escriba la palabra clave como texto (por ejemplo `小六小六` o `hey macaw`) sin entrenar ningún modelo; tras la activación, el dictado puede empezar automáticamente.
- Síntesis de voz: varias voces neuronales en chino e inglés, sintetizadas en archivos WAV o usadas como motor TTS del sistema por el módulo `tts` de AutoJs6 y otras aplicaciones.
- Entrada de voz del sistema: los botones de micrófono de otras apps, como 3-Stove Agent, pueden usar el reconocimiento sin conexión; dices una frase en un panel inferior y el texto reconocido vuelve a esa app.
- Gestión de modelos: descarga desde un catálogo firmado, activación solo después de que cada archivo supere su comprobación SHA-256, e importación de modelos locales; los modelos restringidos a uso no comercial muestran su licencia antes de la descarga.

El progreso y el diseño están disponibles en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech).
