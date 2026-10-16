<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-macaw-speech-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Reconnaissance vocale hors ligne, détection de mots de réveil, détection d'activité vocale et synthèse vocale neuronale, utilisable comme moteur TTS du système</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues (Languages)

******

Le README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Macaw Speech s'appuie sur [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) pour exécuter hors ligne, sur l'appareil, la reconnaissance vocale, la détection d'activité vocale, la détection de mots de réveil et la synthèse vocale neuronale. Tout est calculé localement et aucun son n'est envoyé.

Le plugin est un APK installé séparément. Les scripts AutoJs6 l'utilisent via le module global `speech`; la synthèse vocale est aussi enregistrée comme moteur TTS du système Android, de sorte que le module `tts` d'AutoJs6 et les autres applications peuvent la choisir. Les modèles de reconnaissance et de synthèse ne sont pas inclus dans l'APK mais téléchargés à la demande depuis l'écran du plugin ou un script; seul le modèle de détection d'activité vocale est intégré.

> Il s'agit d'une préversion de développement: AutoJs6 détecte le plugin et celui-ci charge le moteur vocal, mais la reconnaissance, les mots de réveil, la synthèse, le téléchargement des modèles, les écrans du plugin et le module de script `speech` ne sont pas encore livrés; l'icône actuelle est provisoire et sera remplacée. La suite décrit les objectifs de conception de la 1.0.0; consultez la feuille de route pour l'avancement.

******

### Points Forts

******

- Reconnaissance vocale: dictée en continu depuis le microphone (résultats partiels et finaux en temps réel) et reconnaissance de fichiers WAV, avec des modèles hors ligne pour le chinois, l'anglais, le chinois et l'anglais mélangés, le japonais, le coréen et le cantonais.
- Détection d'activité vocale: le silero VAD v5 intégré découpe l'audio en segments de parole et termine aussi automatiquement les phrases dictées.
- Mots de réveil: saisissez le mot-clé en texte brut (par exemple `小六小六` ou `hey macaw`) sans entraîner de modèle; après le réveil, la dictée peut démarrer automatiquement.
- Synthèse vocale: plusieurs voix neuronales chinoises et anglaises, synthétisées dans des fichiers WAV ou utilisées comme moteur TTS du système par le module `tts` d'AutoJs6 et d'autres applications.
- Gestion des modèles: téléchargement depuis un catalogue signé, activation seulement après vérification SHA-256 de chaque fichier, et import de modèles locaux; les modèles réservés à un usage non commercial affichent leur licence avant le téléchargement.

******

### Utilisation

******

1. Mettez à jour AutoJs6 vers la version interne 5448 (6.8.0) ou ultérieure.
2. Téléchargez depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) l'APK correspondant à l'ABI de l'appareil (choisissez `universal` en cas de doute) et installez-le, puis autorisez-le et activez-le dans le centre des plugins d'AutoJs6.
3. Ouvrez l'écran du plugin pour accorder l'autorisation du microphone et télécharger les modèles nécessaires, puis utilisez le module `speech` dans vos scripts.

> Le plugin prend en charge les appareils sous Android 7.0 (API 24) ou ultérieur. Il existe un paquet par ABI et un paquet `universal` pour tous les appareils.

******

### Modèles

******

Les modèles de reconnaissance, de mots de réveil et de synthèse occupent chacun de 30 Mo à 350 Mo, ils ne sont donc pas inclus dans l'APK. Le plugin les télécharge depuis des publications immuables du dépôt de ressources partagées d'AutoJs6 et se replie sur l'adresse amont de sherpa-onnx si le miroir est indisponible; dans les deux cas, chaque fichier doit passer sa vérification SHA-256 avant l'activation du modèle.

Chaque modèle indique sa licence. Les modèles réservés à un usage non commercial (par exemple un modèle de synthèse entraîné sur le corpus DataBaker) affichent un avertissement avant le téléchargement, et les scripts doivent passer explicitement `acceptLicense: true`.

******

### API De Script

******

Les scripts utilisent ces capacités via le module global `speech` (aussi disponible sous `$speech`; il s'agit du projet de conception de la 1.0.0, la documentation de la version publiée fait foi):

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

- `speech.listen(options)` ouvre une session de dictée qui transmet les résultats par des événements tels que `partial`, `final` et `end`; `speech.recognize(path)` reconnaît un fichier WAV.
- `speech.wake(keywords, options)` ouvre une session de mots de réveil; `thenListen` lance automatiquement la dictée après un réveil.
- `speech.synthesize(text, path)` synthétise dans un fichier WAV; `speech.speak(text)` et `tts(text, {engine})` parlent via le moteur TTS du système; `speech.models` gère les modèles.

******

### FAQ

******

#### La reconnaissance et la synthèse nécessitent-elles Internet?

Non. Une fois les modèles téléchargés, tout s'exécute sur l'appareil et aucun son n'est envoyé. Le plugin ne se connecte que pour télécharger des modèles et actualiser le catalogue.

#### Pourquoi le plugin demande-t-il lui-même l'autorisation du microphone?

L'audio est enregistré dans le processus du plugin et ne transite jamais par AutoJs6. AutoJs6 transmet ses capacités de premier plan au plugin lors de la liaison, les scripts peuvent donc aussi dicter en arrière-plan. Accordez l'autorisation du microphone sur l'écran du plugin avant la première utilisation.

#### Quel paquet installer?

La plupart des téléphones utilisent `arm64-v8a`. Si vous ne connaissez pas l'ABI de l'appareil, installez `universal`: il fonctionne partout mais il est plus volumineux.

******

### Permissions Et Sécurité

******

Le plugin réduit autant que possible ses autorisations:

- Microphone: l'audio n'est enregistré que pour la dictée, les mots de réveil et l'écran d'essai, et le microphone est libéré dès la fin de la session ou du script.
- Réseau: utilisé uniquement pour télécharger les modèles et actualiser le catalogue signé; la reconnaissance et la synthèse restent hors ligne.
- Stockage: les modèles résident dans le répertoire privé du plugin et les modèles locaux sont importés via le sélecteur de fichiers du système, aucune autorisation de stockage n'est donc demandée.
- Entrées protégées: le service de capacité et l'entrée d'activation exigent l'autorisation de plugin d'AutoJs6 (`org.autojs.permission.PLUGIN`), les autres applications ne peuvent donc pas les appeler directement.

******

### Interface Du Plugin

******

Les informations suivantes s'adressent aux développeurs de l'hôte AutoJs6 et de plugins; l'hôte utilise ces identifiants pour découvrir le plugin et négocier les capacités:

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

### Feuille De Route

******

Les plans, décisions de conception et l'avancement sont suivis sous forme de liste à cocher dans ROADMAP.md. Les éléments non cochés sont des intentions, pas des fonctionnalités de la version actuelle.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### Historique Des Versions

******

#### v1.0.0

_2026/10/16_

- `Note` Préversion de développement: AutoJs6 détecte le plugin et celui-ci charge le moteur vocal; la reconnaissance vocale, les mots de réveil, la synthèse vocale, le téléchargement des modèles, les écrans du plugin et le module de script speech ne sont pas encore livrés
- `Note` Nécessite AutoJs6 6.8.0 (5448) ou ultérieur (la version minimale exacte sera renseignée une fois les modifications de l'hôte intégrées)
- `Note` L'icône actuelle est provisoire et sera remplacée
- `Fonctionnalité` Squelette du plugin: protocole d'activation Wake, service d'identité du centre des plugins et service de capacité vocale
- `Fonctionnalité` Moteur vocal sherpa-onnx 1.13.8 et modèle de détection d'activité vocale silero VAD v5 intégrés, avec un paquet par ABI pour 4 ABI
- `Fonctionnalité` README, instructions du centre des plugins et journaux des modifications en 10 langues, générés à partir de sources JSON
- `Dépendance` Ajout de sherpa-onnx 1.13.8
- `Dépendance` Ajout de Jetpack Compose BOM 2026.09.00
- `Dépendance` Ajout de AndroidX Activity Compose 1.12.2
- `Dépendance` Ajout de AndroidX Core KTX 1.15.0
- `Dépendance` Ajout de Kotlin Coroutines 1.11.0

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation

******

Cette section s'adresse aux développeurs souhaitant compiler le plugin depuis les sources.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\verify_native_alignment.py --check
py .python\check_repository.py
```

******

### Licence

******

Le code du projet est publié sous la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). Le moteur vocal sherpa-onnx utilise la licence Apache-2.0, onnxruntime et silero VAD la licence MIT; la licence de chaque modèle figure dans le catalogue des modèles.

- [Mentions tierces (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Projet sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
