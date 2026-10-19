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

Le plugin est un APK installé séparément, avec ses propres écrans pour télécharger et gérer les modèles, les essayer et modifier les réglages. Les scripts AutoJs6 l'utilisent via le module global `speech`; la synthèse vocale s'enregistre aussi comme moteur de synthèse vocale du système Android, et la reconnaissance est aussi proposée comme saisie vocale du système, service de reconnaissance vocale du système et assistant numérique, si bien que les autres applications peuvent aussi s'en servir. Les modèles de reconnaissance et de synthèse ne sont pas fournis avec l'APK mais téléchargés à la demande; seul le modèle de détection d'activité vocale est intégré.

> Le module `speech` dans les scripts nécessite AutoJs6 6.8.0 (version interne 5456) ou ultérieure. Le moteur de synthèse vocale du système, la saisie vocale du système, le service de reconnaissance vocale, l'assistant numérique et les écrans du plugin ne dépendent pas d'AutoJs6 et fonctionnent dès que le plugin est installé et les modèles téléchargés.

******

### Points Forts

******

- Reconnaissance vocale: dictée en continu depuis le microphone (résultats partiels et finaux en temps réel) et reconnaissance de fichiers WAV, avec des modèles hors ligne pour le chinois, l'anglais, le chinois et l'anglais mélangés, le japonais, le coréen et le cantonais.
- Détection d'activité vocale: le silero VAD v5 intégré découpe l'audio en segments de parole et termine aussi automatiquement les phrases dictées.
- Mots de réveil: saisissez le mot-clé en texte brut (par exemple `小六小六` ou `hey macaw`) sans entraîner de modèle; après le réveil, la dictée peut démarrer automatiquement.
- Synthèse vocale: plusieurs voix neuronales chinoises et anglaises, synthétisées dans des fichiers WAV ou utilisées comme moteur TTS du système par le module `tts` d'AutoJs6 et d'autres applications.
- Saisie vocale du système: les boutons de microphone d'autres applications, comme 3-Stove Agent, peuvent utiliser la reconnaissance hors ligne; vous dites une phrase dans un panneau en bas de l'écran et le texte reconnu revient dans cette application.
- Assistant numérique: choisissez 3-Macaw Speech comme application d'assistant numérique par défaut, puis appuyez longuement sur le bouton Marche/Arrêt ou Accueil, dites une phrase, modifiez le texte reconnu si besoin et transmettez-le à 3-Stove Agent, 3-Stone AI ou une autre application qui reçoit des commandes vocales, ou copiez-le. Il fournit aussi un service de reconnaissance vocale du système: sous Android 11 et versions antérieures, le choisir comme assistant en fait généralement aussi le service de reconnaissance par défaut; à partir d'Android 12, choisissez-le séparément dans les paramètres de saisie vocale du système (certains téléphones ne les proposent pas), et les applications peuvent aussi le désigner directement.
- Gestion des modèles: téléchargement depuis un catalogue signé, activation seulement après vérification SHA-256 de chaque fichier, et import de modèles locaux; les modèles réservés à un usage non commercial affichent leur licence avant le téléchargement.
- Écrans autonomes: l'écran d'accueil affiche l'état et gère les modèles, Essayer teste directement la dictée, les mots de réveil et la synthèse, et les réglages changent la langue, le mode nuit, la couleur du thème, l'icône du lanceur et les valeurs par défaut de reconnaissance et de synthèse; l'apparence suit AutoJs6 par défaut.

******

### Captures d'écran

******

Les captures proviennent d'un Sony XQ-DQ72 (Android 13). Le texte dicté est ce que le plugin a reconnu d'un enregistrement de test de sherpa-onnx diffusé par le haut-parleur du téléphone; aucune donnée personnelle n'est affichée.

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/home.png?raw=true" alt="Accueil: état et modèles regroupés par usage" width="360" />
      <br />
      <sub>Accueil: état et modèles regroupés par usage</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/tryout-dictation.png?raw=true" alt="Essayer: dictée en direct" width="360" />
      <br />
      <sub>Essayer: dictée en direct</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/voice-input.png?raw=true" alt="Saisie vocale du système: le panneau ouvert par une autre application" width="360" />
      <br />
      <sub>Saisie vocale du système: le panneau ouvert par une autre application</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/assistant.png?raw=true" alt="Assistant numérique: modifier le résultat et choisir l'application qui le reçoit" width="360" />
      <br />
      <sub>Assistant numérique: modifier le résultat et choisir l'application qui le reçoit</sub>
    </td>
  </tr>
</table>

******

### Installation et utilisation

******

1. Téléchargez depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) l'APK correspondant à l'ABI de l'appareil et installez-le: la plupart des téléphones utilisent `arm64-v8a`; en cas de doute, choisissez `universal`, qui fonctionne sur tous les appareils mais est plus volumineux.
2. Ouvrez 3-Macaw Speech, accordez l'autorisation du microphone sur l'écran d'accueil et téléchargez les modèles nécessaires dans les groupes reconnaissance, mot de réveil et synthèse (les modèles recommandés par le catalogue sont un bon point de départ, voir le tableau des modèles ci-dessous). Sous Android 13 et versions ultérieures, le premier téléchargement demande aussi d'afficher les notifications de progression.
3. Dans AutoJs6: mettez à jour AutoJs6 vers 6.8.0 (version interne 5456) ou ultérieure, activez 3-Macaw Speech dans le centre des plugins et appelez le module `speech` dans vos scripts (voir API de script ci-dessous). Les exemples d'AutoJs6 contiennent quatre scripts prêts à l'emploi sous Echantillons > 3-Macaw Speech.
4. Dans d'autres applications: voir Synthèse vocale du système ci-dessous pour le moteur de synthèse vocale, et la FAQ pour la saisie vocale du système, le service de reconnaissance vocale et l'assistant numérique.

> Il existe un paquet par ABI et un paquet `universal` pour tous les appareils. Les paquets ne contiennent aucun modèle de reconnaissance ou de synthèse; les modèles sont téléchargés à la demande depuis les écrans du plugin ou un script, ou importés depuis des fichiers locaux.

******

### Modèles

******

Les modèles de reconnaissance, de mot de réveil et de synthèse occupent de 8 Mo à 390 Mo chacun, ils ne sont donc pas fournis avec l'APK. Le plugin contient un catalogue de modèles signé et télécharge les modèles depuis les versions immuables du dépôt de ressources partagées d'AutoJs6, en se rabattant sur l'adresse amont de sherpa-onnx lorsque le miroir est indisponible; sur les deux chemins, chaque fichier doit passer sa vérification SHA-256 avant que le modèle soit activé. Le catalogue 1.0.0 contient ces modèles (la taille est l'espace occupé une fois installé):

| ID du modèle | Usage | Langues | Taille | Licence | Usage commercial |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (recommandé) | Reconnaissance (hors ligne, avec ponctuation) | zh, en, ja, ko, yue | 228.5 MB | FunASR Model License 1.1 | Autorisé |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (recommandé) | Reconnaissance (en continu) | zh, en | 189.8 MB | Apache-2.0 | Autorisé |
| `asr.zipformer-streaming.en.2023-06-26.int8` (recommandé) | Reconnaissance (en continu) | en | 67.1 MB | Apache-2.0 | Autorisé |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | Reconnaissance (en continu) | zh | 159.6 MB | Aucune déclarée | Non commercial uniquement |
| `kws.zipformer.zh-en.2025-12-20` (recommandé) | Mot de réveil | zh, en | 8.4 MB | Apache-2.0 | Autorisé |
| `tts.vits.melo.zh-en` (recommandé) | Synthèse (locuteurs: 1, 44.1 kHz) | zh, en | 169.2 MB | MIT | Autorisé |
| `tts.vits.icefall.zh-aishell3` (recommandé) | Synthèse (locuteurs: 174, 8 kHz) | zh | 31.2 MB | Aucune déclarée | Inconnu |
| `tts.kokoro.multi-lang.v1_1` | Synthèse (locuteurs: 103, 24 kHz) | zh, en | 386.9 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | Autorisé |
| `tts.matcha.icefall.zh-baker` | Synthèse (locuteurs: 1, 22.05 kHz) | zh | 77.2 MB | Aucune déclarée, MIT (vocodeur) | Non commercial uniquement |
| `tts.vits.piper.en-us-lessac-medium` | Synthèse (locuteurs: 1, 22.05 kHz) | en | 77.4 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non commercial uniquement |

Les modèles réservés à un usage non commercial ou dont la licence est inconnue affichent leur licence avant le téléchargement, et les scripts doivent passer explicitement `acceptLicense: true`. Les poids du modèle chinois en continu de 2025 ne déclarent aucune licence et ses données d'entraînement comprennent des corpus réservés à un usage non commercial: il n'est donc téléchargé que depuis l'adresse amont de sherpa-onnx, sans miroir dans le dépôt de ressources partagées. Les modèles de synthèse Kokoro et Piper incluent les données de prononciation d'eSpeak NG (espeak-ng-data, GPL-3.0-or-later).

Importer un modèle local, sur l'écran d'accueil, accepte une archive tar, tar.bz2 ou zip ou un dossier: les fichiers qui correspondent à un modèle du catalogue sont enregistrés comme ce modèle, les autres deviennent un modèle personnalisé une fois son type, son type de modèle, son nom et ses langues saisis. L'import passe par le sélecteur de fichiers du système et ne demande aucune autorisation de stockage.

******

### Synthèse vocale du système

******

1. Téléchargez au moins un modèle de synthèse dans le groupe synthèse de l'écran d'accueil du plugin. Pour un premier essai, le plus petit, `tts.vits.icefall.zh-aishell3` (chinois, 174 locuteurs), et le modèle chinois et anglais `tts.vits.melo.zh-en` sont tous deux recommandés.
2. Ouvrez les réglages de synthèse vocale du système et choisissez 3-Macaw Speech comme moteur préféré. La ligne Moteur de synthèse vocale du système de la carte d'état, sur l'écran d'accueil du plugin, ouvre directement cette page; sinon, elle se trouve généralement sous Paramètres > Accessibilité > Synthèse vocale ou Paramètres > Système > Langues et saisie > Synthèse vocale (les noms varient selon le fabricant).
3. Le bouton de réglages à côté du moteur ouvre les réglages de 3-Macaw Speech, dont le groupe synthèse définit la voix par défaut de chaque langue et le locuteur. Le débit vient du réglage du système ou de l'application qui demande la lecture; la hauteur n'est pas prise en charge.
4. De retour dans les réglages du système, touchez Écouter un exemple pour entendre la voix par défaut de cette langue.

Chaque locuteur de chaque modèle est une voix nommée `<id du modèle>/<numéro du locuteur>`; les locuteurs des modèles bilingues ont aussi `<id du modèle>/<numéro du locuteur>/en-US` pour l'anglais. Les scripts AutoJs6 peuvent utiliser le moteur sans changer le réglage du système: `tts(text, { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" })`, ou `speech.speak(text, { model, speaker })` pour nommer directement le modèle et le locuteur.

******

### API De Script

******

À partir d'AutoJs6 6.8.0 (version interne 5456), les scripts utilisent ces fonctions via le module global `speech` (aussi disponible sous `$speech`). Les trois exemples ci-dessous s'exécutent séparément: une dictée de 30 secondes qui ajoute chaque phrase à un fichier, une phrase après un mot de réveil, et une synthèse vers un fichier suivie d'une lecture:

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

- `speech.listen(options)` ouvre une session de dictée qui rend compte par les événements `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` et `end`; `stop()` la termine plus tôt. `speech.recognize(path)` reconnaît un fichier WAV (le PCM 16 kHz est aussi accepté) et `speech.detectVoice(path)` liste ses segments de parole.
- `speech.wake(keywords, options)` ouvre une session de mot de réveil avec les mots-clés écrits en texte brut (50 au plus); avec `thenListen`, chaque réveil devient un tour de dictée sur le même enregistrement, dont la session est la propriété `listen` de l'événement `wake`.
- `speech.synthesize(text, path, options)` synthétise vers un fichier WAV (8000 caractères au plus, avec modèle, locuteur et vitesse en option); `speech.speak(text, options)` et `tts(text, { engine })` parlent via le moteur de synthèse vocale du système.
- `speech.models` liste, télécharge (`install`, avec progression), importe et supprime les modèles et définit les modèles par défaut; `speech.capabilities()` renvoie la version du plugin, ses fonctions et s'il détient l'autorisation du microphone.
- Le plugin entier n'a qu'une session de microphone à la fois (dictée ou mot de réveil); sans l'autorisation du microphone, les appels échouent avec `PERMISSION_DENIED`, que l'écran d'accueil du plugin permet d'accorder. Le texte reconnu n'est jamais écrit dans le journal d'AutoJs6.
- La [documentation d'AutoJs6](https://docs.autojs6.com/#/speech) décrit chaque méthode, option, événement et code d'erreur; les Echantillons > 3-Macaw Speech d'AutoJs6 contiennent quatre exemples: dictée vers un fichier, mot de réveil et dictée, annonces vocales et commandes vocales transmises à l'Agent.

******

### Compatibilité

******

- Android 7.0 (API 24) ou ultérieur, API cible 37. Paquets pour arm64-v8a, armeabi-v7a, x86_64 et x86, plus un paquet universal; les bibliothèques natives sont alignées sur des pages mémoire de 16 Ko et ont été vérifiées sur des émulateurs à pages de 16 Ko.
- AutoJs6: le module `speech` des scripts nécessite 6.8.0 (version interne 5456) ou ultérieure; les écrans du plugin, le moteur de synthèse vocale du système, la saisie vocale du système, le service de reconnaissance vocale et l'assistant numérique ne dépendent pas d'AutoJs6.
- Écoute en arrière-plan: une session de dictée ou de mot de réveil ouverte pendant qu'AutoJs6 est au premier plan continue d'écouter quand AutoJs6 passe en arrière-plan ou que l'écran s'éteint (le plugin garde l'enregistrement avec un service de premier plan de microphone et sa notification). Une session ouverte alors qu'AutoJs6 est déjà en arrière-plan peut être rendue muette par le système: elle reçoit alors un événement `warning` de code `PERMISSION_DENIED` sans se terminer, et le son revient quand AutoJs6 repasse au premier plan; les scripts qui doivent écouter en arrière-plan ouvrent la session pendant qu'AutoJs6 est visible.
- Appareils testés: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), ainsi que des émulateurs API 24, 35 et 37 (pages de 16 Ko).
- Performances: sur l'entrée de gamme Sony G8441, un modèle en continu se charge en 1,7 seconde environ et SenseVoice en 2,8 secondes environ, et la dictée est traitée environ 3 fois plus vite que le temps réel; MeloTTS et Kokoro synthétisent plus lentement que le temps réel sur ces appareils, AISHELL-3 est le plus rapide. Des appareils plus récents comme le Sony XQ-DQ72 chargent en 0,5 à 1,2 seconde environ et synthétisent plus vite que le temps réel.
- Mémoire: un modèle de reconnaissance chargé occupe environ 210 à 300 Mo de mémoire native. Le plugin limite les modèles chargés au quart de la mémoire de l'appareil (384 Mo à 1,5 Go) et les libère après 5 minutes d'inactivité (de 1 à 30 minutes dans les réglages).
- Assistant numérique: le système doit permettre de choisir une application d'assistant numérique tierce. Certains systèmes réservent l'appui long sur le bouton Marche/Arrêt à leur propre assistant; utilisez alors le bouton Accueil ou le geste d'assistant. ColorOS et HyperOS n'ont pas encore été vérifiés.

******

### FAQ

******

#### La reconnaissance et la synthèse nécessitent-elles Internet?

Non. Une fois les modèles téléchargés, tout s'exécute sur l'appareil et aucun son n'est envoyé. Le plugin ne se connecte que pour télécharger des modèles et actualiser le catalogue.

#### Pourquoi le plugin demande-t-il lui-même l'autorisation du microphone?

L'audio est enregistré dans le processus du plugin et ne transite jamais par AutoJs6. AutoJs6 transmet ses capacités de premier plan au plugin lors de la liaison, et le plugin conserve un service de premier plan de type microphone pendant une session au microphone, si bien que la dictée continue quand AutoJs6 passe en arrière-plan ou que l'écran s'éteint. Accordez l'autorisation du microphone dans l'écran du plugin avant la première utilisation.

#### Quel paquet installer?

La plupart des téléphones utilisent `arm64-v8a`. Si vous ne connaissez pas l'ABI de l'appareil, installez `universal`: il fonctionne partout mais il est plus volumineux.

#### Pourquoi l'exemple des réglages du système reste-t-il muet, ou le moteur signale-t-il des données vocales manquantes?

Le moteur ne déclare que les langues des modèles de synthèse installés. Sans modèle de synthèse, ou si les modèles installés ne couvrent pas la langue de l'appareil, le système affiche des données vocales manquantes et l'exemple ainsi que le débit peuvent être indisponibles. Téléchargez un modèle de synthèse pour la langue de l'appareil sur l'écran d'accueil du plugin et rouvrez les réglages du système; l'exemple utilise la voix par défaut de cette langue, modifiable dans le groupe synthèse des réglages du plugin.

#### D'autres applications peuvent-elles utiliser 3-Macaw Speech pour la saisie vocale?

Oui. Une fois un modèle de reconnaissance installé et le microphone autorisé, les applications qui lancent la saisie vocale du système (par exemple le bouton de microphone de 3-Stove Agent) ouvrent le panneau de 3-Macaw Speech. Si une autre application de saisie vocale comme Google est installée, Android demande laquelle utiliser et peut mémoriser ce choix. Pour masquer cette entrée aux autres applications, désactivez 'Utiliser comme saisie vocale du système' dans le groupe Reconnaissance des paramètres.

#### Comment faire de 3-Macaw Speech l'assistant numérique par défaut?

Ouvrez les paramètres de 3-Macaw Speech et touchez Assistant numérique dans le groupe reconnaissance: les paramètres du système s'ouvrent, et vous y choisissez 3-Macaw Speech comme application d'assistant numérique (le nom de la page peut varier légèrement selon le système). Ensuite, appuyez longuement sur le bouton Marche/Arrêt ou Accueil, ou utilisez le geste de l'assistant, pour dicter. Si le bouton Marche/Arrêt de votre téléphone est réglé sur une autre fonction, utilisez le bouton Accueil ou le geste de l'assistant.

#### Pourquoi l'assistant n'écoute-t-il pas un mot de réveil en permanence?

Écouter en permanence garde allumés le microphone, une notification permanente et le voyant du microphone, et consomme plus de batterie; l'assistant numérique n'écoute donc que lorsque vous appuyez sur le bouton de l'assistant. Des applications comme 3-Stone AI et 3-Stove Agent peuvent proposer leur propre interrupteur d'écoute permanente (désactivé par défaut); pendant qu'elles écoutent, l'assistant numérique, la saisie vocale et la reconnaissance vocale fonctionnent toujours, et la détection du mot de réveil se met en pause puis reprend d'elle-même ensuite. Un mot de réveil intégré à 3-Macaw Speech est prévu dans une version ultérieure.

#### Que faire si le téléchargement d'un modèle échoue?

Le plugin télécharge d'abord depuis les versions GitHub du dépôt de ressources partagées d'AutoJs6 et passe de lui-même à l'adresse amont de sherpa-onnx en cas d'échec; les téléchargements interrompus reprennent. Sans accès à GitHub, téléchargez l'archive du modèle sur un autre appareil (adresse du miroir ou amont du catalogue) et utilisez Importer un modèle local sur l'écran d'accueil; les fichiers conformes au catalogue sont enregistrés comme le modèle du catalogue.

#### Quelle place occupent les modèles, et comment la libérer?

Le tableau des modèles ci-dessus donne la taille installée de chaque modèle. L'écran d'accueil supprime les modèles dont vous n'avez plus besoin, et Nettoyer les fichiers non référencés, dans les réglages, supprime les fichiers téléchargés dont aucun modèle n'a besoin, les copies d'import et les installations interrompues, en gardant les modèles installés et les téléchargements en pause. La désinstallation du plugin supprime tous les modèles.

#### Pourquoi le résultat n'a-t-il pas de ponctuation?

Les modèles Zipformer en continu produisent des mots sans ponctuation. Le modèle SenseVoice reconnaît la parole segment par segment, ajoute la ponctuation et indique la langue, ce qui convient aux phrases complètes: nommez-le avec l'option `model` dans les scripts ou faites-en le modèle de reconnaissance par défaut dans les réglages.

#### Comment écrire un mot de réveil, et pourquoi ne se déclenche-t-il pas?

Écrivez le mot-clé en texte brut: le chinois est épelé en pinyin selon sa lecture courante, les mots anglais selon le dictionnaire du modèle (les mots absents sont refusés), et les jetons pinyin du modèle peuvent être écrits directement pour choisir la lecture d'un caractère à plusieurs lectures. Trois à quatre syllabes et une expression peu courante déclenchent moins de réveils par erreur. Chaque mot-clé accepte un `threshold` (plus bas, il se déclenche plus facilement) et un `boost`. Essayez-le dans l'essai de mot de réveil de l'écran Essayer avant de l'écrire dans un script.

#### Pourquoi l'APK est-il distribué sous GPL-3.0, et où est le code source?

Le code propre du plugin est sous MPL-2.0. La bibliothèque Android officielle de sherpa-onnx lie statiquement eSpeak NG (GPL-3.0-or-later), donc l'APK dans son ensemble est distribué sous GPL-3.0-or-later. Chaque version joint l'archive du code source correspondant (le code source et les scripts de compilation de sherpa-onnx avec toutes leurs dépendances), dont le nom contient `corresponding-source` et dont le SHA-256 figure dans `SHA256SUMS`; le code source du plugin est ce dépôt à l'étiquette de la version.

******

### Permissions Et Sécurité

******

Le plugin réduit autant que possible ses autorisations:

- Microphone: l'audio n'est enregistré que pour la dictée, les mots de réveil et l'écran d'essai, et le microphone est libéré dès la fin de la session ou du script.
- Réseau: utilisé uniquement pour télécharger les modèles et actualiser le catalogue signé; la reconnaissance et la synthèse restent hors ligne.
- Stockage: les modèles résident dans le répertoire privé du plugin et les modèles locaux sont importés via le sélecteur de fichiers du système, aucune autorisation de stockage n'est donc demandée.
- Entrées protégées: le service de capacité et l'entrée d'activation exigent l'autorisation de plugin d'AutoJs6 (`org.autojs.permission.PLUGIN`), les autres applications ne peuvent donc pas les appeler directement; le moteur TTS du système et ses entrées de vérification des voix, de texte d'exemple et de réglages sont ouverts à toutes les applications, comme l'exige Android, et ne font que synthétiser la parole.
- Saisie vocale du système: comme le moteur de synthèse vocale du système, elle est ouverte à toutes les applications; elle n'enregistre que pendant l'affichage de son panneau, ne renvoie que le texte reconnu et peut être désactivée dans les paramètres.
- Assistant numérique: il ne lit jamais l'écran et n'écoute pas en arrière-plan; il n'enregistre que pendant l'affichage de son panneau, et le texte reconnu ne quitte le panneau que lorsque vous touchez le bouton d'une application ou le copiez (sur l'écran de verrouillage, après le déverrouillage). Le service de reconnaissance vocale ne reconnaît que pour les applications qui détiennent elles-mêmes l'autorisation du microphone.

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
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### Remerciements

******

3-Macaw Speech repose sur les projets open source, modèles et données suivants. Merci à leurs auteurs et mainteneurs:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) et l'équipe [k2-fsa (Kaldi nouvelle génération)](https://github.com/k2-fsa): l'environnement d'exécution de la reconnaissance, des mots de réveil, de la détection d'activité vocale et de la synthèse, les exports ONNX des modèles et la bibliothèque Android (Apache-2.0).
- Modèles entraînés avec [icefall](https://github.com/k2-fsa/icefall): les reconnaisseurs Zipformer en continu (bilingue par pfluo, anglais par Zengwei, chinois 2025 par yuekai), le détecteur de mots-clés à vocabulaire ouvert (pkufool), et les modèles de synthèse AISHELL-3 VITS et Baker Matcha-TTS de k2-fsa.
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / Alibaba, licence de modèle FunASR 1.1): le reconnaisseur multilingue hors ligne.
- Les modèles de synthèse [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, enregistrements chinois cédés par LongMaoData) et [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT), et le vocodeur [HiFi-GAN](https://github.com/jik876/hifi-gan) (MIT).
- Données d'entraînement: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), le [corpus mandarin standard de Data-Baker](https://www.data-baker.com/open_source.html) (usage non commercial uniquement), les données Blizzard 2013 de Lessac Technologies (recherche uniquement) et [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (domaine public).
- Le moteur d'inférence [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT), le modèle d'activité vocale [silero VAD](https://github.com/snakers4/silero-vad) (MIT), [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) et [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT) pour les phonèmes, et [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT) pour les lectures des mots de réveil chinois.

******

### Feuille De Route

******

Les plans, décisions de conception et l'avancement sont suivis sous forme de liste à cocher dans ROADMAP.md. Les éléments non cochés sont des intentions, pas des fonctionnalités de la version actuelle.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### Historique Des Versions

******

#### v1.0.0

_2026/10/20_

- `Note` Première version. Le module speech des scripts nécessite AutoJs6 6.8.0 (5456) ou ultérieur; les écrans du plugin, le moteur de synthèse vocale du système, la saisie vocale du système, le service de reconnaissance vocale et l'assistant numérique ne dépendent pas d'AutoJs6
- `Note` Les modèles de reconnaissance et de synthèse ne font pas partie du paquet et se téléchargent depuis les écrans du plugin ou un script; l'application dans son ensemble est distribuée sous GNU GPL 3.0 ou ultérieure (l'environnement vocal lie statiquement eSpeak NG), et chaque version est accompagnée du code source correspondant
- `Fonctionnalité` Plugin vocal d'AutoJs6: une fois activé dans le centre des plugins, les scripts utilisent la reconnaissance vocale, les mots de réveil, la détection d'activité vocale, la synthèse vocale et la gestion des modèles via le module speech
- `Fonctionnalité` Moteur vocal sherpa-onnx 1.13.8 et modèle de détection d'activité vocale silero VAD v5 intégrés, avec un paquet par ABI pour 4 ABI
- `Fonctionnalité` Gestion des modèles: télécharge les modèles vocaux hors ligne du catalogue signé depuis les publications des ressources partagées d'AutoJs6, avec repli sur les publications officielles de sherpa-onnx; chaque fichier est vérifié par SHA-256 avant usage, avec reprise des téléchargements, import local (tar, tar.bz2, zip), suppression et modèles par défaut; les modèles réservés à un usage non commercial ou sans licence déclarée exigent d'accepter leur licence au préalable
- `Fonctionnalité` Reconnaissance vocale des fichiers WAV (entiers sur 8, 16, 24 ou 32 bits ou flottants, toute fréquence d'échantillonnage et tout nombre de canaux, rééchantillonnés en 16 kHz mono dans le plugin) avec des segments horodatés, ainsi que des étiquettes de langue, d'émotion et d'événement pour les modèles SenseVoice; les modèles en continu acceptent des mots favoris
- `Fonctionnalité` Sessions de dictée sur le microphone ou sur du PCM 16 kHz fourni: elles signalent la disponibilité, les résultats partiels et finaux, les fins d'énoncé, l'activité vocale et la fin; le silence qui termine une phrase, le délai sans parole et la durée maximale sont réglables; les modèles en continu actualisent les résultats partiels mot par mot, les modèles hors ligne segment par segment
- `Fonctionnalité` Détection d'activité vocale: liste les segments de parole d'un fichier WAV avec le silero VAD intégré
- `Fonctionnalité` Mots de réveil: détection de mots clés à vocabulaire ouvert; les mots clés chinois sont épelés en pinyin selon leur lecture usuelle, les mots anglais en phonèmes du lexique du modèle, et les jetons du modèle peuvent être écrits directement; une détection peut lancer une dictée sur le même enregistrement
- `Fonctionnalité` Pendant une session au microphone, le plugin continue d'enregistrer avec un service de premier plan de type microphone et sa notification, si bien qu'une session commencée alors qu'AutoJs6 est au premier plan continue quand AutoJs6 passe en arrière-plan ou que l'écran s'éteint; une seule session au microphone à la fois; sans l'autorisation du microphone, les sessions échouent avec PERMISSION_DENIED en indiquant l'écran d'autorisation
- `Fonctionnalité` Synthèse vocale: les modèles de synthèse installés (VITS, Matcha et Kokoro; le catalogue compte 5 modèles chinois et anglais) transforment jusqu'à 8000 caractères de texte en fichier WAV phrase par phrase, à la fréquence d'échantillonnage du modèle; le locuteur et la vitesse (de 0.5 à 2 fois) se choisissent, la progression compte les caractères traités et la synthèse peut être annulée à tout moment; un modèle à un seul locuteur ignore le locuteur choisi et le signale
- `Fonctionnalité` Moteur TTS du système: une fois un modèle de synthèse installé, 3-Macaw Speech peut être choisi dans les réglages de synthèse vocale du système, et le module tts d'AutoJs6 comme les autres applications lisent à voix haute avec lui; chaque locuteur est une voix, les modèles bilingues ajoutent une voix anglaise pour chaque locuteur et la vitesse d'élocution suit le réglage du système ou de l'application; sans modèle installé, le moteur signale des données vocales manquantes
- `Fonctionnalité` Écran d'accueil: affiche la version du moteur vocal, le nombre de modèles installés, l'autorisation du microphone (avec un bouton pour l'accorder) et indique si 3-Macaw Speech est le moteur TTS préféré du système
- `Fonctionnalité` Modèles sur l'écran d'accueil, classés en reconnaissance, mots de réveil, synthèse et détection d'activité vocale, avec leur taille, leurs langues et leur licence: télécharger, suspendre, reprendre, annuler, supprimer et définir par défaut; la progression du téléchargement s'affiche aussi dans une notification; les modèles réservés à un usage non commercial ou sans licence déclarée demandent d'abord d'accepter leur licence, et un téléchargement sur données mobiles ou sur un autre réseau facturé à l'usage demande confirmation avant de commencer
- `Fonctionnalité` Import d'un modèle local depuis une archive tar, tar.bz2 ou zip ou depuis un dossier: les fichiers qui correspondent à un modèle du catalogue sont enregistrés comme ce modèle, les autres deviennent un modèle personnalisé une fois sa catégorie, son type de modèle, son nom, ses langues et son ID renseignés
- `Fonctionnalité` Essayer: dictée en direct (résultats partiels en gris) avec choix du modèle et de la langue, essai des mots de réveil qui liste les détections avec leur mot clé et leur score, et synthèse vocale de votre texte avec choix du modèle, du locuteur et de la vitesse, à écouter ou à enregistrer en fichier WAV; quitter l'écran libère le microphone
- `Fonctionnalité` Saisie vocale du système: les boutons de microphone d'autres applications, comme 3-Stove Agent, peuvent utiliser la reconnaissance hors ligne de 3-Macaw Speech: dites une phrase dans un panneau en bas de l'écran et le texte reconnu revient directement dans cette application; la saisie se termine après une pause, ou vous pouvez l'arrêter ou l'annuler plus tôt; elle suit la sensibilité de fin de phrase et la détection d'activité vocale des paramètres et peut être désactivée dans leur groupe Reconnaissance
- `Fonctionnalité` Assistant numérique: après avoir choisi 3-Macaw Speech comme application d'assistant numérique par défaut dans les paramètres du système, appuyez longuement sur le bouton Marche/Arrêt ou Accueil (ou utilisez le geste de l'assistant) et dites une phrase dans un panneau en bas de l'écran; le texte reconnu peut être modifié, puis un bouton le transmet à une application qui reçoit des commandes vocales, comme 3-Stove Agent ou 3-Stone AI, ou vous le copiez ou parlez à nouveau; rien n'est envoyé avant que vous touchiez un bouton. Il fonctionne aussi sur l'écran de verrouillage et déverrouille avant d'envoyer ou de copier. Il ne lit jamais l'écran et n'écoute pas en arrière-plan. Le groupe reconnaissance des paramètres indique s'il est l'assistant par défaut et ouvre les paramètres du système pour le choisir
- `Fonctionnalité` Service de reconnaissance vocale du système: les applications qui utilisent la reconnaissance vocale d'Android (SpeechRecognizer) peuvent désigner 3-Macaw Speech directement et obtenir sa reconnaissance hors ligne, avec des résultats partiels; quand il est le service de reconnaissance par défaut, les applications qui n'en désignent aucun l'utilisent aussi. Sous Android 11 et versions antérieures, le choisir comme assistant numérique en fait généralement aussi le service par défaut; à partir d'Android 12, choisissez-le séparément dans les paramètres de saisie vocale du système (certains téléphones ne les proposent pas). Quand une autre application garde l'écoute d'un mot de réveil via 3-Macaw Speech (3-Stone AI, par exemple), l'assistant numérique, la saisie vocale du système, le service de reconnaissance vocale et l'écran d'essai peuvent toujours utiliser le microphone: la détection du mot de réveil est mise en pause pendant ce temps et reprend d'elle-même ensuite
- `Fonctionnalité` Paramètres: langue, mode nuit, couleur du thème et icône du lanceur (qui suivent AutoJs6 par défaut); modèles de reconnaissance et de synthèse par défaut, sensibilité de fin de phrase, détection d'activité vocale, locuteur, vitesse et voix par défaut par langue pour l'écran d'essai et le moteur TTS du système (les scripts gardent les valeurs par défaut documentées); délai avant la libération des modèles inactifs, actualisation du catalogue et nettoyage des fichiers restants; le bouton de réglages de 3-Macaw Speech dans les réglages de synthèse vocale du système ouvre cet écran
- `Fonctionnalité` À propos: version, historique des versions inclus dans le plugin, recherche de mises à jour (automatique au plus une fois toutes les 12 heures, une version peut être ignorée) ainsi que licences et mentions: l'application dans son ensemble est distribuée sous la GNU GPL 3.0 ou ultérieure, sans aucune garantie, avec les mentions de tiers, le texte complet de la GPL, les conditions d'utilisation d'Unicode et l'emplacement de la source correspondante
- `Fonctionnalité` Icône du lanceur: icône adaptative (claire), icône adaptative (sombre), icône adaptative (automatique, par défaut) ou icône à fond transparent
- `Fonctionnalité` Écrans, README, instructions du centre des plugins et journal des modifications en 10 langues: chinois simplifié, chinois traditionnel (Hong Kong, Taïwan), anglais, français, espagnol, japonais, coréen, russe et arabe
- `Dépendance` Ajout de sherpa-onnx 1.13.8
- `Dépendance` Ajout de Jetpack Compose BOM 2026.09.00
- `Dépendance` Ajout de AndroidX Activity Compose 1.12.2
- `Dépendance` Ajout de AndroidX Core KTX 1.15.0
- `Dépendance` Ajout de Kotlin Coroutines 1.11.0
- `Dépendance` Ajout de Material Color Utilities (MaterialKolor) 4.1.1

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
py -I .python\corresponding_source.py build --downloads <directory outside the repository>
```

******

### Licence

******

Le code source du plugin est sous [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). La bibliothèque Android officielle de sherpa-onnx (Apache-2.0) lie statiquement eSpeak NG et UCD Tools, sous GNU GPL version 3 ou ultérieure, donc l'APK publié dans son ensemble est distribué sous GPL-3.0-or-later (la section 3.3 de la MPL-2.0 permet cette combinaison), sans aucune garantie. Chaque version joint le code source correspondant `autojs6-plugin-three-macaw-speech-corresponding-source-sherpa-onnx-v1.13.8-ac68c80b.tar`: le code source et les scripts de compilation de sherpa-onnx 1.13.8 et chaque archive de dépendance que sa compilation fixe par URL et SHA-256; son SHA-256 figure dans le `SHA256SUMS` de la version, et `.python/corresponding_source.py` le reconstruit et le vérifie. ONNX Runtime et silero VAD sont sous licence MIT; la licence de chaque modèle figure dans le tableau des modèles ci-dessus. La page À propos du plugin affiche le texte complet des licences et les mentions tierces.

- [Mentions tierces (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentation d'AutoJs6: le module speech: https://docs.autojs6.com/#/speech
- Projet sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
