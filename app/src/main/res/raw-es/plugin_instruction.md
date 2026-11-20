3-Macaw Speech realiza sin conexión, en el teléfono, el reconocimiento de voz, la detección de palabras de activación y la síntesis de voz; una vez descargados los modelos, todo el cálculo se hace en el dispositivo y no se sube ningún audio. Funciona por sí solo, sin AutoJs6, y también puede servir como motor de texto a voz del sistema, entrada de voz y asistente digital. Con AutoJs6, los scripts pueden dictar, transcribir archivos de audio, esperar palabras de activación y leer texto en voz alta.

### Primeros pasos

1. Abre 3-Macaw Speech y toca "Conceder" en la fila "Permiso de micrófono" de la pantalla de inicio.
2. En la pantalla de inicio, descarga los modelos que necesites en los grupos "Reconocimiento de voz", "Palabras de activación" y "Síntesis de voz"; los modelos marcados como "Recomendado" son un buen punto de partida.
3. Toca el botón "Probar" de la pantalla de inicio para probar el dictado, las palabras de activación y la síntesis.
4. Para usarlo en scripts, activa este complemento en el "Centro de plugins" de AutoJs6.

### Funciones principales

- **Reconocimiento de voz**: dictado en tiempo real desde el micrófono y transcripción de archivos de audio y del sonido de vídeos; hay modelos sin conexión para chino, inglés, japonés, cantonés y más. Tras descargar un modelo de "Puntuación", los resultados se puntúan automáticamente.
- **Palabras de activación**: escribe la palabra clave tal cual, sin entrenar modelos; tras oír una palabra de activación, puede dictar una frase a continuación.
- **Síntesis de voz**: voces neuronales leen texto en voz alta o lo guardan en archivos de audio; como motor de texto a voz del sistema, también lee para el módulo `tts` de AutoJs6 y otras apps.
- **Entrada de voz y asistente digital**: los botones de micrófono de otras apps pueden usarlo para dictar sin conexión; como asistente digital predeterminado, mantén pulsada la tecla de encendido o Inicio, di una frase y el texto pasa a una app que acepte comandos de voz.
- **Formas de activación**: accesos como "Icono de Ajustes rápidos", "Widget y acceso directo de la pantalla de inicio" y "Micrófono flotante" abren el mismo panel de dictado; el resto del tiempo no se graba nada.
- **Palabra de activación permanente**: desactivada por defecto; al activarla, decir una palabra de activación abre el panel de dictado, y cada palabra también puede "Enviar una difusión" o "Solo notificar". Mientras está activa, el indicador del micrófono sigue encendido y se gasta más batería.
- **Reconocimiento del hablante**: registra tu voz en la página "Hablantes" y activa "Solo voces registradas" para que la palabra de activación permanente solo responda a las personas registradas.
- **Transcripción y análisis de archivos**: la pestaña "Archivo" de "Probar" convierte audio en texto y permite "Guardar como subtítulos"; también separa hablantes, identifica el idioma y etiqueta sonidos.
- **Gestión de modelos**: un modelo solo se activa tras verificar cada uno de sus archivos; cuando sale una versión nueva, actualízalo desde su tarjeta, o usa "Importar un modelo local".
- **Herramientas del complemento**: mediante el complemento MCP Server o `plugins.tools.call`, escucha una frase, lee texto en voz alta y transcribe archivos de audio.

### Uso en scripts

En los scripts Rhino de AutoJs6, estas funciones se usan con el módulo global `speech`; descarga antes modelos de palabras de activación, reconocimiento de voz y síntesis de voz. El siguiente ejemplo escucha una frase tras una palabra de activación y luego muestra el texto reconocido y lo lee en voz alta:

```js
let wake = speech.wake(["hey macaw", "小六小六"], { thenListen: true });
wake.on("wake", e => {
    e.listen.on("final", r => {
        console.log(r.text);
        speech.speak(r.text);
    });
});
```

- `speech.recognize` transcribe un archivo de audio y `speech.synthesize` guarda texto en un archivo WAV; ambos bloquean y no se pueden llamar en el hilo de UI, así que en scripts de interfaz usa `recognizeAsync` y `synthesizeAsync`.
- Solo puede haber una sesión de dictado o de activación abierta a la vez; abrir otra falla con `SESSION_BUSY` en lugar de esperar turno.
- Los scripts de Node.js usan la misma API mediante `autojs6:speech`; los de Bun usan `host.speech`, que requiere la autorización `speech.control`.
- El uso completo, incluidos el reconocimiento del hablante, el análisis de audio y la gestión de modelos, está en [Documentación de AutoJs6 > speech](https://docs.autojs6.com/#/speech).

### Requisitos

- Android 7.0 o posterior. Las pantallas del complemento, el motor de texto a voz del sistema, la entrada de voz y el asistente digital no necesitan AutoJs6.
- El módulo `speech` y las herramientas del complemento necesitan AutoJs6 6.8.0 o posterior, con este complemento activado en el "Centro de plugins".
- El permiso de micrófono se usa para el dictado y la detección de palabras de activación, y debe concederse a este mismo complemento; el de AutoJs6 no lo sustituye. El permiso de notificaciones se usa para mostrar el progreso de descarga de los modelos.
- El permiso de superposición es opcional y permite que el "Micrófono flotante" y la palabra de activación permanente abran el panel de dictado directamente desde segundo plano; el "Activador por tecla" se activa en los ajustes de accesibilidad del sistema, solo filtra teclas y nunca lee la pantalla.
- Los modelos se descargan por Internet y ocupan desde unos pocos MB hasta varios cientos de MB cada uno; una vez descargados, el reconocimiento y la síntesis funcionan sin conexión.

### Preguntas frecuentes

**Configuración como motor de texto a voz predeterminado del sistema**

Toca "Abrir ajustes" en la fila "Motor de texto a voz del sistema" de la pantalla de inicio y elige 3-Macaw Speech como motor preferido en la página del sistema. El motor solo ofrece los idiomas que cubren los modelos de síntesis instalados; si la prueba no suena, descarga primero un modelo de síntesis que cubra el idioma del dispositivo.

**Motivos por los que el indicador del micrófono sigue encendido con la palabra de activación permanente**

Escuchar palabras de activación exige grabar continuamente, y desde Android 12 la barra de estado muestra el indicador del micrófono todo el tiempo; las apps no pueden ocultarlo. Conviene limitar el "Tiempo de escucha" o activar "Escuchar solo mientras se carga" en los ajustes.

**Casos en que falla la descarga de un modelo**

El complemento descarga primero desde GitHub y, si falla, cambia automáticamente a la dirección original; las descargas interrumpidas se reanudan donde se quedaron. Si aun así no se puede descargar, descarga el paquete del modelo en otro dispositivo e impórtalo con "Importar un modelo local" en la pantalla de inicio.

**Motivos de la espera en el primer dictado de un script**

El complemento libera los modelos inactivos, así que el primer dictado posterior debe volver a cargar el modelo, lo que puede tardar unos segundos en teléfonos antiguos. Poner "Liberar modelos inactivos" en "Nunca" en los ajustes evita la espera, a cambio de mantener esa memoria ocupada.

La lista de modelos, cómo configurar cada acceso y más preguntas frecuentes están en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech).
