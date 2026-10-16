******

### Historial De Versiones

******

# v1.0.0

###### 2026/10/16

* `Aviso` Versión preliminar de desarrollo: AutoJs6 detecta el complemento, que carga el motor de voz y descarga y gestiona modelos; el reconocimiento de voz, las palabras de activación, la síntesis de voz, las pantallas del complemento y el módulo de script speech aún no están disponibles
* `Aviso` Requiere AutoJs6 6.8.0 (5451) o posterior (la versión mínima exacta se indicará cuando se integren los cambios del anfitrión)
* `Función` Esqueleto del complemento: protocolo de activación Wake, servicio de identidad del centro de complementos y servicio de capacidad de voz
* `Función` Motor de voz sherpa-onnx 1.13.8 y modelo de detección de actividad de voz silero VAD v5 integrados, con un paquete por ABI para 4 ABI
* `Función` Gestión de modelos: descarga los modelos de voz sin conexión del catálogo firmado desde las publicaciones de los recursos compartidos de AutoJs6, con las publicaciones oficiales de sherpa-onnx como alternativa; cada archivo se verifica con SHA-256 antes de usarse, con descargas reanudables, importación local (tar, tar.bz2, zip), eliminación y modelos predeterminados; los modelos limitados a uso no comercial o sin licencia declarada requieren aceptar antes su licencia
* `Función` README, instrucciones del centro de complementos y registros de cambios en 10 idiomas, generados a partir de fuentes JSON
* `Dependencia` Se agrega sherpa-onnx 1.13.8
* `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00
* `Dependencia` Se agrega AndroidX Activity Compose 1.12.2
* `Dependencia` Se agrega AndroidX Core KTX 1.15.0
* `Dependencia` Se agrega Kotlin Coroutines 1.11.0
