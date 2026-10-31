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

- Reconnaissance vocale: dictée en continu depuis le microphone (résultats partiels et finaux en temps réel) et reconnaissance de fichiers audio (WAV, MP3, AAC, Ogg, FLAC, etc., ou le son d'un fichier vidéo), avec des modèles hors ligne pour le chinois, l'anglais, le chinois et l'anglais mélangés, le japonais, le coréen et le cantonais. Le français, le russe et le coréen ont aussi des modèles en continu qui affichent le texte pendant que vous parlez. La version 1.3.0 ajoute un modèle hors ligne pour l'espagnol (avec ponctuation) et un modèle en continu pour huit langues qui détecte la langue tout seul: arabe, indonésien, thaï, vietnamien, japonais, russe, chinois et anglais.
- Ponctuation automatique: une fois un modèle de ponctuation installé, chaque phrase reconnue par un modèle en continu reçoit virgules, points et points d'interrogation (chinois et anglais), et le texte anglais retrouve ses majuscules; SenseVoice ajoute sa propre ponctuation et n'est pas modifié. Les modèles de ponctuation se téléchargent depuis le groupe "Ponctuation" de l'écran d'accueil.
- Détection d'activité vocale: le silero VAD v5 intégré découpe l'audio en segments de parole et termine aussi automatiquement les phrases dictées.
- Mots de réveil: saisissez le mot-clé en texte brut (par exemple `小六小六` ou `hey macaw`) sans entraîner de modèle; après le réveil, la dictée peut démarrer automatiquement.
- Synthèse vocale: plusieurs voix neuronales chinoises et anglaises, synthétisées dans des fichiers WAV ou utilisées comme moteur TTS du système par le module `tts` d'AutoJs6 et d'autres applications. Des voix Piper française, espagnole, allemande et russe sont aussi disponibles (usage non commercial uniquement). La version 1.3.0 ajoute les voix multilingues de Kokoro v1.0 (anglais, espagnol, français, hindi, italien, portugais du Brésil et chinois) ainsi que des voix arabes et une voix coréenne (usage non commercial uniquement).
- Saisie vocale du système: les boutons de microphone d'autres applications, comme 3-Stove Agent, peuvent utiliser la reconnaissance hors ligne; vous dites une phrase dans un panneau en bas de l'écran et le texte reconnu revient dans cette application.
- Assistant numérique: choisissez 3-Macaw Speech comme application d'assistant numérique par défaut, puis appuyez longuement sur le bouton Marche/Arrêt ou Accueil, dites une phrase, modifiez le texte reconnu si besoin et transmettez-le à 3-Stove Agent, 3-Stone AI ou une autre application qui reçoit des commandes vocales, ou copiez-le. Il fournit aussi un service de reconnaissance vocale du système: sous Android 11 et versions antérieures, le choisir comme assistant en fait généralement aussi le service de reconnaissance par défaut; à partir d'Android 12, choisissez-le séparément dans les paramètres de saisie vocale du système (certains téléphones ne les proposent pas), et les applications peuvent aussi le désigner directement.
- Déclencheurs: ouvrez le même panneau de dictée sans le bouton Marche/Arrêt: un bloc des Réglages rapides (aussi sur l'écran de verrouillage), un widget et un raccourci de l'icône de l'application, un micro flottant déplaçable (désactivé par défaut), un déclencheur par touche qui maintient ou appuie deux fois sur une touche de volume (un service d'accessibilité qui filtre uniquement les touches, désactivé par défaut) et la touche vocale d'un casque Bluetooth. Rien n'est enregistré avant que le panneau écoute, et seulement pendant les quelques secondes de la dictée.
- Mot de réveil permanent (désactivé par défaut): une fois activé, dire un mot de réveil (`小鹦小鹦` et `Hey Macaw` par défaut, modifiables) ouvre le même panneau de dictée, allume l'écran s'il est éteint ou verrouillé et lit le texte reconnu à voix haute. Pendant l'écoute, le voyant du micro reste allumé, une notification reste affichée et la batterie s'use plus vite (le Mode économie d'énergie, activé par défaut, ne fait pas tourner le modèle du mot de réveil dans le calme et sollicite bien moins le CPU); une durée d'écoute (désactiver après 10, 30 ou 60 minutes, ou toujours active) et Écouter uniquement pendant la charge peuvent être réglées. Quand un script, une autre application ou un autre déclencheur a besoin du micro, il s'efface et reprend l'écoute ensuite.
- Reconnaissance du locuteur (1.3.0): dans Paramètres > Mot de réveil permanent > Locuteurs, saisissez un nom et lisez 3 phrases à voix haute pour enregistrer votre voix (plusieurs personnes peuvent s'enregistrer, et "Essayer la reconnaissance" affiche le score). Avec "Seulement les voix enregistrées" activé, le mot de réveil permanent compare d'abord la voix et n'ouvre le panneau que si elle correspond à une personne enregistrée, si bien que le mot de réveil prononcé par d'autres est ignoré; les déclencheurs manuels comme le bloc, le widget et la touche du casque ne changent pas. Les scripts peuvent enregistrer, vérifier et identifier des locuteurs avec `speech.speaker` (AutoJs6 6.8.0 ou ultérieur). Le modèle de reconnaissance du locuteur fait environ 26,5 Mo; la page "Locuteurs" indique comment le télécharger.
- Analyse audio (1.3.0): la reconnaissance de fichiers d'Essayer peut aussi séparer les locuteurs (qui parle et quand, avec le nom des voix enregistrées, ou "Locuteurs et texte" pour transcrire les paroles de chacun), identifier la langue (les 99 langues de Whisper) et étiqueter les sons (527 types d'AudioSet, comme la parole, la musique, les animaux, les véhicules et les alarmes). Tout fonctionne hors ligne sur le téléphone une fois les modèles téléchargés (l'écran indique lesquels). Les scripts peuvent utiliser `speech.diarize`, `speech.detectLanguage` et `speech.tag` (AutoJs6 6.8.0 ou ultérieur).
- Lecture audio (1.4.0): la reconnaissance de fichiers de la page Essayer lit le fichier avec barre de progression, vitesses de 0.5x à 2x et sauts de 5 secondes. La phrase entendue est surlignée, toucher une phrase lance la lecture à cet endroit et une vue paroles l'affiche en grand. Le plugin ne garde l'accès en lecture qu'au dernier fichier, jusqu'à ce que le résultat soit effacé ou qu'un autre fichier soit choisi.
- Outils du plugin (AutoJs6 6.8.0 ou ultérieur): quatre outils, `speech_listen` (écouter une phrase et renvoyer le texte reconnu), `speech_speak` (lire un texte à voix haute), `speech_transcribe` (transcrire un fichier audio du téléphone) et `speech_synthesize` (synthétiser un texte dans un fichier WAV ou M4A). Le plugin MCP Server s'en sert pour qu'un assistant IA sur un ordinateur puisse écouter et parler à travers le téléphone, et les scripts peuvent aussi les appeler avec `plugins.tools`. 1.3.0 ajoute `speech_speaker_verify` (un enregistrement est-il une personne enregistrée donnée) et `speech_speaker_identify` (la personne enregistrée la plus proche), qui ne renvoient que des noms et des scores. 1.3.0 ajoute aussi trois outils d'analyse: `speech_diarize` (séparer les locuteurs, fichiers de 10 minutes au plus), `speech_detect_language` (identifier la langue) et `speech_tag` (étiqueter les sons).
- Gestion des modèles: téléchargement depuis un catalogue signé, activation seulement après vérification SHA-256 de chaque fichier, et import de modèles locaux; les modèles réservés à un usage non commercial affichent leur licence avant le téléchargement.
- Écrans autonomes: l'écran d'accueil affiche l'état et gère les modèles, Essayer teste directement la dictée, les mots de réveil et la synthèse et transforme en texte un fichier audio ou vidéo du téléphone, à enregistrer en texte brut ou en sous-titres SRT, et les réglages changent la langue, le mode nuit, la couleur du thème, l'icône du lanceur et les valeurs par défaut de reconnaissance et de synthèse; l'apparence suit AutoJs6 par défaut.

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

Les modèles de reconnaissance, de ponctuation, de mot de réveil, de synthèse, de locuteur et d'analyse audio occupent de 6 Mo à 410 Mo chacun, ils ne sont donc pas fournis avec l'APK. Le plugin contient un catalogue de modèles signé et télécharge les modèles depuis les versions immuables du dépôt de ressources partagées d'AutoJs6, en se rabattant sur l'adresse amont de sherpa-onnx lorsque le miroir est indisponible; sur les deux chemins, chaque fichier doit passer sa vérification SHA-256 avant que le modèle soit activé. Le catalogue 1.3.0 contient ces modèles (la taille est l'espace occupé une fois installé):

| ID du modèle | Usage | Langues | Taille | Licence | Usage commercial |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (recommandé) | Reconnaissance (hors ligne, avec ponctuation) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | Autorisé |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (recommandé) | Reconnaissance (en continu) | zh, en | 199.1 MB | Apache-2.0 | Autorisé |
| `asr.zipformer-streaming.en.2023-06-26.int8` (recommandé) | Reconnaissance (en continu) | en | 70.3 MB | Apache-2.0 | Autorisé |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | Reconnaissance (en continu) | zh | 167.4 MB | Aucune déclarée | Non commercial uniquement |
| `asr.zipformer-streaming.fr.2023-04-14.int8` (recommandé) | Reconnaissance (en continu) | fr | 129.0 MB | Apache-2.0 | Autorisé |
| `asr.zipformer-streaming.ru.2025-08-16.int8` (recommandé) | Reconnaissance (en continu) | ru | 28.6 MB | Apache-2.0 | Autorisé |
| `asr.zipformer-streaming.ko.2024-06-16.int8` | Reconnaissance (en continu) | ko | 140.9 MB | Apache-2.0 | Non commercial uniquement |
| `asr.nemo-transducer.es.2026-05-03.int8` (recommandé) | Reconnaissance (hors ligne, avec ponctuation) | es | 136.5 MB | CC-BY-4.0 | Autorisé |
| `asr.zipformer-streaming.ar-en-id-ja-ru-th-vi-zh.2025-02-10.int8` | Reconnaissance (en continu) | ar, en, id, ja, ru, th, vi, zh | 338.9 MB | Apache-2.0 | Inconnu |
| `punct.ct-transformer.zh-en.2024-04-12.int8` (recommandé) | Ponctuation | zh, en | 75.5 MB | Apache-2.0 | Autorisé |
| `punct.cnn-bilstm.en.2024-08-06.int8` (recommandé) | Ponctuation | en | 7.6 MB | Apache-2.0 | Autorisé |
| `kws.zipformer.zh-en.2025-12-20` (recommandé) | Mot de réveil | zh, en | 8.8 MB | Apache-2.0 | Autorisé |
| `tts.vits.melo.zh-en` (recommandé) | Synthèse (locuteurs: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | Autorisé |
| `tts.vits.icefall.zh-aishell3` (recommandé) | Synthèse (locuteurs: 174, 8 kHz) | zh | 32.8 MB | Aucune déclarée | Inconnu |
| `tts.kokoro.multi-lang.v1_1` | Synthèse (locuteurs: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | Autorisé |
| `tts.matcha.icefall.zh-baker` | Synthèse (locuteurs: 1, 22.05 kHz) | zh | 81.0 MB | Aucune déclarée, MIT (vocodeur) | Non commercial uniquement |
| `tts.vits.piper.en-us-lessac-medium` | Synthèse (locuteurs: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non commercial uniquement |
| `tts.vits.piper.fr-fr-siwis-medium.int8` | Synthèse (locuteurs: 1, 22.05 kHz) | fr | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non commercial uniquement |
| `tts.vits.piper.es-es-davefx-medium.int8` | Synthèse (locuteurs: 1, 22.05 kHz) | es | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non commercial uniquement |
| `tts.vits.piper.de-de-thorsten-medium.int8` | Synthèse (locuteurs: 1, 22.05 kHz) | de | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non commercial uniquement |
| `tts.vits.piper.ru-ru-denis-medium.int8` | Synthèse (locuteurs: 1, 22.05 kHz) | ru | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non commercial uniquement |
| `tts.kokoro.multi-lang.v1_0.int8` | Synthèse (locuteurs: 49, 24 kHz) | en, es, fr, hi, it, pt, zh | 168.9 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | Autorisé |
| `tts.vits.piper.ar-jo-sa-dii-high.int8` | Synthèse (locuteurs: 1, 22.05 kHz) | ar | 36.6 MB | CC-BY-NC-SA-4.0, GPL-3.0-or-later (espeak-ng-data) | Non commercial uniquement |
| `tts.vits.piper.ar-jo-sa-miro-v2-high.int8` | Synthèse (locuteurs: 1, 22.05 kHz) | ar | 36.6 MB | CC-BY-NC-ND-4.0, GPL-3.0-or-later (espeak-ng-data) | Non commercial uniquement |
| `tts.vits.mimic3.ko-ko-kss-low` | Synthèse (locuteurs: 1, 22.05 kHz) | ko | 80.8 MB | CC0-1.0, GPL-3.0-or-later (espeak-ng-data) | Non commercial uniquement |
| `speaker.eres2net.3dspeaker.en-voxceleb` (recommandé) | Reconnaissance du locuteur | en, zh | 26.5 MB | Apache-2.0 | Autorisé |
| `segment.pyannote.3-0` (recommandé) | Segmentation des locuteurs | Toutes | 6.0 MB | MIT | Non commercial uniquement |
| `langid.whisper.tiny.int8` (recommandé) | Identification de la langue | 99 langues | 102.8 MB | MIT | Inconnu |
| `langid.whisper.base.int8` | Identification de la langue | 99 langues | 159.8 MB | MIT | Inconnu |
| `tagger.ced.mini.int8` (recommandé) | Étiquetage audio | Toutes | 10.5 MB | CC-BY-4.0, CC-BY-SA-4.0 (AudioSet labels) | Autorisé |

Les modèles réservés à un usage non commercial ou dont la licence est inconnue affichent leur licence avant le téléchargement, et les scripts doivent passer explicitement `acceptLicense: true`. Les poids du modèle chinois en continu de 2025 ne déclarent aucune licence et ses données d'entraînement comprennent des corpus réservés à un usage non commercial: il n'est donc téléchargé que depuis l'adresse amont de sherpa-onnx, sans miroir dans le dépôt de ressources partagées. Les modèles de synthèse Kokoro et Piper incluent les données de prononciation d'eSpeak NG (espeak-ng-data, GPL-3.0-or-later). Les données d'entraînement du modèle coréen en continu (KsponSpeech) ne sont ouvertes qu'aux demandes de ressortissants coréens et leur usage hors de Corée exige un accord distinct: le modèle est donc marqué comme réservé à un usage non commercial et n'est pas non plus mis en miroir, il n'est téléchargé que depuis l'adresse amont (environ 418 Mo à télécharger, 141 Mo une fois installé). Les voix Piper française, espagnole, allemande et russe sont toutes affinées à partir de la voix anglaise lessac et, comme elle, réservées à un usage non commercial. Les poids du modèle en continu à huit langues (PengChengStarling) sont sous Apache-2.0, mais son éditeur ne précise pas les données d'entraînement: son usage commercial est donc marqué comme inconnu. Les voix arabes dii et miro ont été entraînées sur de la parole synthétique sous CC BY-NC-ND 4.0; dii est sous CC BY-NC-SA 4.0 et miro sous CC BY-NC-ND 4.0. Comme miro n'autorise pas les adaptations, elle n'est téléchargée que depuis l'adresse amont, sans miroir dans le dépôt de ressources partagées. Les poids de la voix coréenne KSS sont sous CC0, mais ses données d'entraînement (KSS) sont sous CC BY-NC-SA 4.0: elle est donc marquée comme réservée à un usage non commercial. Les poids du modèle de segmentation des locuteurs (pyannote segmentation 3.0) sont sous MIT, mais ses données d'entraînement comprennent MSDWild, réservé à la recherche: il est donc marqué comme réservé à un usage non commercial. Les poids des deux modèles Whisper d'identification de la langue sont sous MIT, mais OpenAI n'a pas publié les données d'entraînement: leur usage commercial est donc marqué comme inconnu. Les poids du modèle d'étiquetage audio CED-mini sont sous CC BY 4.0, et la table des classes d'AudioSet fournie avec lui est sous CC BY-SA 4.0.

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

- `speech.listen(options)` ouvre une session de dictée qui rend compte par les événements `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` et `end`; `stop()` la termine plus tôt. `speech.recognize(path)` reconnaît un fichier audio (les formats autres que WAV nécessitent AutoJs6 6.8.0 ou ultérieur; le PCM 16 kHz est aussi accepté) et `speech.detectVoice(path)` liste ses segments de parole.
- L'option `punctuation` de `speech.listen` et `speech.recognize` vaut true par défaut: la ponctuation du modèle lui-même (SenseVoice) est conservée, les résultats des modèles en continu reçoivent la ponctuation d'un modèle de ponctuation installé (chinois et anglais, avec les majuscules de l'anglais restaurées) et restent tels quels si aucun modèle de ponctuation ne couvre leur langue; false retire toute la ponctuation. Les modèles de ponctuation se gèrent uniquement sur l'écran d'accueil du plugin et `speech.models` ne les liste pas.
- `speech.wake(keywords, options)` ouvre une session de mot de réveil avec les mots-clés écrits en texte brut (50 au plus); avec `thenListen`, chaque réveil devient un tour de dictée sur le même enregistrement, dont la session est la propriété `listen` de l'événement `wake`.
- `speech.synthesize(text, path, options)` synthétise vers un fichier WAV (8000 caractères au plus, avec modèle, locuteur et vitesse en option); `speech.speak(text, options)` et `tts(text, { engine })` parlent via le moteur de synthèse vocale du système.
- `speech.models` liste, télécharge (`install`, avec progression), importe et supprime les modèles et définit les modèles par défaut; `speech.capabilities()` renvoie la version du plugin, ses fonctions et s'il détient l'autorisation du microphone.
- `speech.speaker.enroll(name, files, { replace })` enregistre un locuteur (les enregistrements sont moyennés; le même nom complète l'empreinte par défaut, `replace: true` recommence), `verify(name, file)` renvoie le score et la correspondance, `identify(file, { topK })` trouve la personne enregistrée la plus proche et liste les candidats, et `list()` et `remove(name)` gèrent les personnes enregistrées; `enroll`, `verify` et `identify` ont chacun une version `...Async` qui renvoie une Promise. Les résultats ne contiennent que des noms, des scores, la correspondance et le seuil (0,50 par défaut, modifiable avec `threshold`), jamais de vecteurs d'empreinte. Nécessite la version finale d'AutoJs6 6.8.0 et le modèle de reconnaissance du locuteur.
- `speech.diarize(file, options)` sépare les locuteurs et renvoie leurs tours de parole dans l'ordre (`segments`, chacun avec `startMillis`, `endMillis` et `speaker` numéroté à partir de 0) et leur nombre (`speakers`); l'option `speakers` fixe leur nombre (0 les compte, 20 au plus), et `identify: true` nomme les locuteurs dont la voix correspond à une empreinte vocale enregistrée (`names`). `speech.detectLanguage(file, { windows })` analyse phrase par phrase les premières fenêtres de parole (de 30 secondes, 3 par défaut): `language` est la langue parlée le plus longtemps, `candidates` donne la part du temps de parole de chaque langue et `segments` la langue de chaque phrase. `speech.tag(file, { topK, timeline })` note des fenêtres de 10 secondes et renvoie les meilleures étiquettes (les 527 étiquettes anglaises d'AudioSet, 5 par défaut); `timeline: true` ajoute les étiquettes de chaque fenêtre. Les trois signalent la progression par `onProgress(done, total)` et ont une version `...Async` qui renvoie une Promise; ils demandent la version finale d'AutoJs6 6.8.0 et les modèles correspondants.
- Le plugin entier n'a qu'une session de microphone à la fois (dictée ou mot de réveil); sans l'autorisation du microphone, les appels échouent avec `PERMISSION_DENIED`, que l'écran d'accueil du plugin permet d'accorder. Le texte reconnu n'est jamais écrit dans le journal d'AutoJs6.
- Avec AutoJs6 6.8.0 ou ultérieur, les scripts peuvent aussi appeler ces outils du plugin avec `plugins.tools.call`, par exemple `plugins.tools.call('speech_speak', { text: 'Bonjour' })`; les arguments sont ceux que voit MCP Server. Pour les scripts courants, le module `speech` reste plus souple.
- La [documentation d'AutoJs6](https://docs.autojs6.com/#/speech) décrit chaque méthode, option, événement et code d'erreur; les Echantillons > 3-Macaw Speech d'AutoJs6 contiennent huit exemples: dictée vers un fichier, mot de réveil et dictée, annonces vocales, commandes vocales transmises à l'Agent, reconnaissance du locuteur, séparation des locuteurs, identification de la langue et étiquetage audio.

******

### Compatibilité

******

- Android 7.0 (API 24) ou ultérieur, API cible 37. Paquets pour arm64-v8a, armeabi-v7a, x86_64 et x86, plus un paquet universal; les bibliothèques natives sont alignées sur des pages mémoire de 16 Ko et ont été vérifiées sur des émulateurs à pages de 16 Ko.
- AutoJs6: le module `speech` des scripts nécessite 6.8.0 (version interne 5456) ou ultérieure; les écrans du plugin, le moteur de synthèse vocale du système, la saisie vocale du système, le service de reconnaissance vocale et l'assistant numérique ne dépendent pas d'AutoJs6. La reconnaissance de fichiers audio autres que WAV (MP3, M4A, etc.) et les outils du plugin demandent la version finale d'AutoJs6 6.8.0 ou ultérieure; avec un AutoJs6 plus ancien, les scripts ne reconnaissent que le WAV. `speech.speaker`, `speech.diarize`, `speech.detectLanguage` et `speech.tag` dans les scripts demandent aussi la version finale d'AutoJs6 6.8.0 ou ultérieure; la page "Locuteurs", "Seulement les voix enregistrées" et les analyses de fichiers d'Essayer du plugin ne dépendent pas d'AutoJs6.
- Écoute en arrière-plan: une session de dictée ou de mot de réveil ouverte pendant qu'AutoJs6 est au premier plan continue d'écouter quand AutoJs6 passe en arrière-plan ou que l'écran s'éteint (le plugin garde l'enregistrement avec un service de premier plan de microphone et sa notification). Une session ouverte alors qu'AutoJs6 est déjà en arrière-plan peut être rendue muette par le système: elle reçoit alors un événement `warning` de code `PERMISSION_DENIED` sans se terminer, et le son revient quand AutoJs6 repasse au premier plan; les scripts qui doivent écouter en arrière-plan ouvrent la session pendant qu'AutoJs6 est visible.
- Appareils testés: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), ainsi que des émulateurs API 24, 35 et 37 (pages de 16 Ko).
- Performances: sur l'entrée de gamme Sony G8441, un modèle en continu se charge en 1,7 seconde environ et SenseVoice en 2,8 secondes environ, et la dictée est traitée environ 3 fois plus vite que le temps réel; MeloTTS et Kokoro synthétisent plus lentement que le temps réel sur ces appareils, AISHELL-3 est le plus rapide. Des appareils plus récents comme le Sony XQ-DQ72 chargent en 0,5 à 1,2 seconde environ et synthétisent plus vite que le temps réel.
- Mémoire: un modèle de reconnaissance chargé occupe environ 210 à 300 Mo de mémoire native. Le plugin limite les modèles chargés au quart de la mémoire de l'appareil (384 Mo à 1,5 Go) et les libère après 5 minutes d'inactivité (de 1 à 30 minutes dans les réglages).
- Assistant numérique: le système doit permettre de choisir une application d'assistant numérique tierce. La façon de l'ouvrir dépend du système: appui long sur le bouton Accueil (navigation à trois boutons), balayage vers le haut depuis un coin inférieur, ou bouton Marche/Arrêt configuré pour ouvrir l'assistant numérique dans les paramètres. Les tablettes ColorOS et HyperOS testées (Android 15) réservent le bouton Marche/Arrêt et les gestes à leur propre assistant; utilisez-y les déclencheurs.
- Déclencheurs: testés sur Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), Xiaomi Pad 23046RP50C (HyperOS, Android 15) et sur des émulateurs API 33 et 35. Sur ColorOS et HyperOS, un appui sur le bloc depuis l'écran de verrouillage demande d'abord de déverrouiller, puis ouvre le panneau; certains lanceurs acceptent la demande d'ajout du widget sans afficher de confirmation, ajoutez-le alors à la main avec les étapes des paramètres. Écran éteint, les téléphones ne transmettent aucune touche aux services d'accessibilité: le déclencheur par touche ne fonctionne qu'écran allumé. Touche vocale d'un casque Bluetooth: avec un Sony WF-1000XM4 et le XQ-DQ72 (Android 13), chaque appui long a ouvert le panneau et la dictée a utilisé le micro du casque; si une autre application (comme l'application Google) répond aussi, le système affiche d'abord un sélecteur. Sur la tablette HyperOS (Xiaomi Pad 23046RP50C, Android 15), le sélecteur n'a rien fait après le choix de 3-Macaw Speech.
- Mot de réveil permanent: testé sur Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15) et un émulateur API 35 (pages de 16 Ko). Consommation écran éteint pendant 30 minutes (Sony XQ-DQ72, Android 13; estimation d'Android d'après le profil énergétique de l'appareil, téléphone branché en USB avec l'état débranché simulé): activé avec le Mode économie d'énergie (par défaut), 17,1 mAh et 235 secondes de temps CPU du plugin; activé sans lui, 17,9 mAh et 454 secondes (environ un quart d'un cœur de CPU en moyenne); enregistrement continu dans les deux cas, et environ 14 mAh de l'estimation reviennent à l'enregistrement lui-même. Pendant ce test, il y avait du bruit en continu autour du téléphone, si bien que le Mode économie d'énergie a surtout réduit le temps CPU; dans une pièce calme (Sony G8441, Android 9, écran éteint pendant 10 minutes), le temps CPU du plugin est passé de 265 à 26 secondes. Désactivé, 0,006 mAh, plugin non lancé. La consommation réelle varie selon l'appareil et les sons alentour.
- Outils du plugin: testés sur Sony G8441 (Android 9), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15) et des émulateurs API 24 et 35 (sur API 24, seulement les parties sans modèle); appels par MCP Server testés sur Sony G8441 (Android 9).
- Ponctuation et modèles ajoutés en 1.2.0: testés sur Sony XQ-AT72 (Android 12). Avec la ponctuation activée, les enregistrements chinois, anglais et mixtes ont reçu une ponctuation et gardé les mêmes mots que sans elle; les phrases synthétisées par les voix Piper russe et française ont été reconnues par le modèle en continu de la même langue avec des taux d'erreur sur les caractères de 7,9 % et 0 %; les voix espagnole et allemande synthétisent normalement, et pour le modèle coréen seul le chargement a été vérifié (aucun enregistrement coréen de test).
- Modèles ajoutés en 1.3.0: testés sur Sony G8441 (Android 9), XQ-DQ72 (Android 13) et un émulateur API 35 (installation, utilisation et suppression). Faute d'enregistrements de test en espagnol, en arabe et en coréen, des phrases synthétisées ont été reconnues: la voix espagnole de Kokoro v1.0 par le modèle NeMo avec des taux d'erreur sur les caractères de 0 % à 6,5 %, les deux voix arabes par le modèle à huit langues avec 3,0 % (33 % pour miro sur l'émulateur), et la voix coréenne par SenseVoice avec 0 %; le modèle à huit langues a reconnu l'enregistrement anglais avec 0 % à 1,1 %. Une fois chargé, le modèle à huit langues culmine à environ 600 Mio (G8441) à 880 Mio (XQ-DQ72) de mémoire; Kokoro v1.0 synthétise plus lentement que le temps réel sur le G8441 (environ 4 à 6 fois la durée de l'audio) et à peu près en temps réel sur le XQ-DQ72.
- Reconnaissance de fichiers dans Essayer: testée sur Sony G8441 (Android 9), XQ-AT72 (Android 12), OnePlus OPD2413 (ColorOS, Android 15) et des émulateurs API 24 et 35. Sur le OnePlus OPD2413, un MP3 de 10 minutes 03 secondes a été reconnu en 21 secondes environ (SenseVoice, ponctuation automatique activée); la reconnaissance continue quand on quitte l'écran, avec la progression dans une notification.
- Reconnaissance du locuteur: testée sur Sony XQ-AT72 (Android 12), XQ-DQ72 (Android 13) et des émulateurs API 24 et 35 (pages de 16 Ko) (inscription, vérification, identification, liste et suppression, ainsi que l'acceptation et le refus de "Seulement les voix enregistrées"; avec deux lecteurs de LibriSpeech et des mots de réveil synthétisés). Des extraits de 2 secondes de la même personne obtiennent de 0,75 à 0,79 et ceux de l'autre personne moins de 0,08, pour un seuil par défaut de 0,50; sur un ordinateur, avec 40 locuteurs anglais et 12 chinois, les extraits de 2 secondes ont été refusés à tort environ 2,5% (anglais) et 0% (chinois) du temps et acceptés à tort environ 0,05% et 0,9%. La vérification de "Seulement les voix enregistrées" prend de 0,15 à 0,25 seconde sur les téléphones Sony.
- Analyse audio: testée sur Sony G8441 (Android 9), OnePlus OPD2413 (ColorOS, Android 15) et des émulateurs API 24 et 35 (pages de 16 Ko) (séparation avec les noms, identification de la langue, étiquetage, annulation, l'écran des fichiers, les trois outils et des scripts AutoJs6). Un enregistrement de 42,5 secondes où deux lecteurs de LibriSpeech parlent à tour de rôle a donné partout 2 locuteurs et 7 tours, avec 93% du temps de parole attribué au bon lecteur; le lecteur enregistré a été nommé (score 0,85) et l'autre non (0,01). Les enregistrements en chinois, en russe et en anglais ont reçu la bonne langue, et la musique, le bruit blanc et les bips ont été étiquetés dans leurs fenêtres de 10 secondes. Un enregistrement de 10 minutes a demandé environ 70 secondes de séparation, 2,5 secondes d'identification de la langue et 3,8 secondes d'étiquetage sur le OnePlus OPD2413, et 222 secondes (mémoire maximale d'environ 470 Mio), 12 secondes et 15 secondes sur le Sony G8441.

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

#### Pourquoi le mot de réveil permanent garde-t-il le voyant du micro allumé, et combien de batterie utilise-t-il?

Écouter un mot de réveil en permanence, c'est enregistrer en permanence: depuis Android 12, la barre d'état affiche le voyant du micro (le point vert) tant qu'une application enregistre, et aucune application ne peut le masquer; les canaux de mots-clés à basse consommation qui n'allument pas le voyant sont réservés aux assistants système préinstallés, pas aux applications tierces, même choisies comme assistant numérique par défaut. Le mot de réveil permanent est donc désactivé par défaut, et tant qu'il est activé le point vert et une notification restent visibles (la notification peut le désactiver). Sur un Sony XQ-DQ72, écran éteint pendant 30 minutes, Android a estimé la consommation du plugin à environ 17 mAh lorsqu'il est activé (environ 0,4% de la batterie) et presque rien lorsqu'il est désactivé. L'essentiel de l'estimation revient à l'enregistrement lui-même: le Mode économie d'énergie, activé par défaut, ne fait tourner le modèle du mot de réveil que lorsque le téléphone entend une voix ou un autre son, ce qui divise le temps CPU du plugin par deux (avec du bruit en continu) à dix (dans le calme), mais ne réduit l'estimation que d'environ 1 mAh. Un mot de réveil prononcé très doucement peut être manqué avec ce mode; il peut alors être désactivé dans les réglages. Activez-le seulement quand vous en avez besoin: réglez la durée d'écoute sur 10, 30 ou 60 minutes, ou activez Écouter uniquement pendant la charge (pause une fois débranché, reprise une fois rebranché); les déclencheurs comme la tuile, le widget et le micro flottant n'enregistrent rien entre-temps.

#### Pourquoi seule une notification "Appuyer pour parler" apparaît-elle après le mot de réveil?

Depuis Android 10, une application en arrière-plan ne peut ouvrir un écran directement que si elle est l'assistant numérique par défaut ou si elle peut se superposer aux autres applications. Si 3-Macaw Speech n'est pas l'assistant numérique par défaut, l'activation du mot de réveil permanent demande une fois cette autorisation; sans elle, un mot de réveil entendu affiche une notification "Appuyer pour parler" qui ouvre le panneau et disparaît au bout d'une minute. Pour que le mot de réveil ouvre directement le panneau, faites de 3-Macaw Speech l'assistant numérique par défaut, ou autorisez-le à se superposer aux autres applications dans les paramètres du système.

#### Pourquoi dois-je désactiver 3-Stove Agent avant d'activer le mot de réveil permanent?

Les écoutes permanentes de 3-Macaw Speech, 3-Stone AI et 3-Stove Agent s'excluent mutuellement: une seule application à la fois peut écouter sur un téléphone, sinon elles se disputeraient le micro. Quand l'une est activée, l'interrupteur de l'autre indique quelle application écoute et ouvre ses paramètres pour la désactiver. Après un redémarrage ou une mise à jour, le mot de réveil permanent ne recommence pas à enregistrer tout seul: tant que l'interrupteur est activé, une notification "Appuyez pour reprendre le mot de réveil permanent" apparaît, et un appui, ou l'ouverture de l'écran d'accueil ou des paramètres de 3-Macaw Speech, le reprend; si la durée d'écoute s'est écoulée entre-temps, l'interrupteur se désactive.

#### Comment faire pour que le mot de réveil permanent ne réponde qu'à ma voix?

Ouvrez les paramètres de 3-Macaw Speech, touchez "Locuteurs" dans le groupe Mot de réveil permanent, téléchargez le modèle de reconnaissance du locuteur (environ 26,5 Mo) comme indiqué, puis touchez "Ajouter un locuteur", saisissez un nom et lisez les 3 phrases affichées, environ 4 secondes chacune; les membres de la famille peuvent aussi s'enregistrer. Activez ensuite "Seulement les voix enregistrées": quand il entend le mot de réveil, le plugin compare les 2 secondes environ qui le précèdent avec les empreintes enregistrées et ouvre le panneau si quelqu'un correspond (score de 0,50 ou plus); sinon il fait comme s'il n'avait rien entendu (ni panneau ni notification). "Essayer la reconnaissance" sur la page "Locuteurs" affiche votre score. Une voix faible, une pièce bruyante ou un autre micro (un casque par exemple) peuvent être refusés; ajoutez alors des enregistrements dans les mêmes conditions. Cela évite que des personnes proches le réveillent par erreur; ce n'est pas un contrôle de sécurité, et un enregistrement d'une personne enregistrée peut aussi passer.

#### Comment un assistant IA écoute-t-il et parle-t-il à travers 3-Macaw Speech?

Il faut AutoJs6 6.8.0 ou ultérieur et le plugin MCP Server. 3-Macaw Speech est un plugin officiel, donc `speech_listen`, `speech_speak` et `speech_transcribe` apparaissent par défaut dans la liste des outils de MCP Server (on peut les désactiver dans "Outils des plugins" des réglages de MCP Server); `speech_synthesize` écrit des fichiers et demande donc aussi le second interrupteur du groupe, et sans emplacement indiqué ses fichiers vont dans plugin-outputs/speech du répertoire de travail d'AutoJs6. `speech_listen` écoute une phrase à la fois (60 secondes au plus): quand il peut enregistrer tout de suite (par exemple avec le mot de réveil permanent activé, ou si 3-Macaw Speech est l'assistant numérique par défaut ou peut se superposer aux autres applications), il commence aussitôt, sinon une notification "Appuyer pour parler" apparaît sur le téléphone et l'écoute commence quand on appuie dessus dans la minute. Pendant l'écoute, l'écoute permanente des autres applications s'efface puis reprend; l'appel renvoie BUSY si une autre dictée est en cours, et NOT_READY sans l'autorisation du microphone ou sans modèle de reconnaissance (l'écran d'accueil du plugin règle les deux). `speech_speak` lit avec une voix de 3-Macaw Speech pour la langue s'il y en a une, sinon avec la voix du système.

#### Comment ouvrir le panneau de dictée sans le bouton Marche/Arrêt?

Dans les paramètres de 3-Macaw Speech, le groupe Déclencheurs les configure: Bloc des Réglages rapides, appuyez pour l'ajouter (Android 13 et versions ultérieures confirment via le système; les versions antérieures affichent les étapes manuelles), puis appuyez sur Commande vocale dans les Réglages rapides; Widget et raccourci de l'écran d'accueil, placez le widget Appuyer pour parler sur l'écran d'accueil, ou appuyez longuement sur l'icône de l'application et choisissez Commande vocale; Micro flottant, activez-le et accordez l'autorisation de se superposer aux autres applications, et un bouton de micro déplaçable apparaît au bord de l'écran; Déclencheur par touche, activez Déclencheur par touche de 3-Macaw Speech dans les paramètres d'accessibilité du système; par défaut, c'est un maintien de la touche de baisse du volume pendant environ 0,8 seconde, que vous pouvez remplacer par un maintien de la hausse du volume ou un double appui sur la baisse du volume. Chaque entrée ouvre le même panneau que l'assistant numérique, où vous choisissez l'application qui reçoit le texte; quand 3-Macaw Speech est l'assistant numérique par défaut, il s'ouvre via la session d'assistant, sinon le même panneau s'ouvre directement.

#### Pourquoi le déclencheur par touche a-t-il besoin d'un service d'accessibilité, et est-ce sûr?

Android ne permet qu'aux services d'accessibilité de voir les touches physiques avant le système, d'où ce service d'accessibilité. Il demande uniquement le filtrage des touches et ne s'abonne à aucun événement d'interface: il ne peut ni lire le contenu de l'écran ni effectuer de gestes; il n'intercepte que la touche de volume choisie, un appui court change toujours le volume, et les touches ne sont pas interceptées quand l'écran est éteint ni pendant un appel. Si son interrupteur est grisé (applications installées depuis un fichier sur Android 13 et versions ultérieures), ouvrez d'abord les infos de l'application 3-Macaw Speech et choisissez Autoriser les paramètres restreints dans son menu.

#### Le bouton vocal d'un casque Bluetooth peut-il ouvrir le panneau de dictée?

Oui. 3-Macaw Speech répond à la demande de commande vocale (VOICE_COMMAND) du service Bluetooth: un appui long sur la touche vocale ou la zone tactile du casque ouvre le panneau de dictée. Depuis Android 12, le panneau demande une fois l'autorisation Appareils à proximité. Accordée, 3-Macaw Speech prend en charge la reconnaissance vocale du casque: il écoute par le micro du casque et chaque appui fonctionne. Refusée, il écoute par le micro du téléphone et certains casques (comme le Sony WF-1000XM4) n'ouvrent le panneau qu'un appui sur deux. Si une autre application répond aussi aux commandes vocales (l'application Google, l'assistant du fabricant), le système demande d'abord laquelle utiliser; sous HyperOS, le sélecteur n'a rien fait après le choix de 3-Macaw Speech, un défaut du sélecteur du système. Pour ne plus l'utiliser, désactivez Touche vocale du casque dans le groupe Déclencheurs des paramètres.

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

- Microphone: l'audio n'est enregistré que pour la dictée, les mots de réveil, l'écran d'essai et tant que le mot de réveil permanent est activé, et le microphone est libéré dès la fin de la session ou du script.
- Réseau: utilisé uniquement pour télécharger les modèles et actualiser le catalogue signé; la reconnaissance et la synthèse restent hors ligne.
- Stockage: les modèles résident dans le répertoire privé du plugin; les modèles locaux à importer et les fichiers à reconnaître dans Essayer sont choisis via le sélecteur de fichiers du système, et vous choisissez où enregistrer une transcription, aucune autorisation de stockage n'est donc demandée. La dernière transcription reste dans le cache du plugin jusqu'à ce que vous l'effaciez à l'écran.
- Entrées protégées: le service de capacité et l'entrée d'activation exigent l'autorisation de plugin d'AutoJs6 (`org.autojs.permission.PLUGIN`), les autres applications ne peuvent donc pas les appeler directement; le moteur TTS du système et ses entrées de vérification des voix, de texte d'exemple et de réglages sont ouverts à toutes les applications, comme l'exige Android, et ne font que synthétiser la parole.
- Saisie vocale du système: comme le moteur de synthèse vocale du système, elle est ouverte à toutes les applications; elle n'enregistre que pendant l'affichage de son panneau, ne renvoie que le texte reconnu et peut être désactivée dans les paramètres.
- Assistant numérique: il ne lit jamais l'écran et n'écoute pas en arrière-plan tant que le mot de réveil permanent est désactivé; il n'enregistre que pendant l'affichage de son panneau, et le texte reconnu ne quitte le panneau que lorsque vous touchez le bouton d'une application ou le copiez (sur l'écran de verrouillage, après le déverrouillage). Le service de reconnaissance vocale ne reconnaît que pour les applications qui détiennent elles-mêmes l'autorisation du microphone.
- Déclencheurs: le bloc, le widget, le raccourci, le micro flottant, le déclencheur par touche et la touche vocale du casque n'enregistrent rien eux-mêmes; ils ouvrent seulement le panneau de dictée. Le déclencheur par touche est un service d'accessibilité qui filtre uniquement les touches: il ne lit jamais le contenu de l'écran, n'effectue aucun geste et n'intercepte que la touche de volume choisie (un appui court change toujours le volume, et les touches ne sont pas interceptées quand l'écran est éteint ni pendant un appel). Le micro flottant a besoin de l'autorisation de se superposer aux autres applications et reste affiché avec une notification silencieuse; le démarrage au boot sert uniquement à rétablir un micro flottant activé après un redémarrage. Depuis Android 12, la touche vocale du casque a besoin d'Appareils à proximité, demandée uniquement dans un panneau ouvert par le casque: cette autorisation sert seulement à ce que le service Bluetooth confie la reconnaissance vocale au panneau et branche le micro du casque; aucune recherche d'appareils, aucune localisation.
- Mot de réveil permanent: désactivé par défaut et activé uniquement depuis les écrans du plugin (après un redémarrage ou une mise à jour, un appui sur sa notification le reprend; rien n'enregistre tout seul). Le mot de réveil est détecté sur l'appareil, aucun son n'est conservé ni envoyé et le texte reconnu n'est jamais journalisé; tant qu'il est activé, il enregistre en permanence, le voyant du micro reste allumé et sa notification peut le désactiver. Il exclut les écoutes permanentes de 3-Stone AI et 3-Stove Agent: le système en autorise une à la fois.
- Empreintes vocales: le plugin ne garde aucun enregistrement d'inscription, seulement l'empreinte calculée à partir de lui; les empreintes restent dans le dossier privé du plugin, ne sont jamais envoyées et ne peuvent être ni exportées ni importées, et les scripts et les outils du plugin ne reçoivent que des noms, des scores et le résultat de la correspondance. La page "Locuteurs" les supprime une par une; effacer les données de 3-Macaw Speech ou le désinstaller les supprime toutes. Les 2 secondes environ avant un mot de réveil que compare "Seulement les voix enregistrées" sont traitées en mémoire uniquement et jamais conservées.
- Outils du plugin: le service d'outils est lui aussi protégé par l'autorisation de plugin d'AutoJs6, seul AutoJs6 peut donc l'appeler directement. `speech_listen` n'enregistre qu'une phrase par appel, avec une notification pendant l'enregistrement (et l'indicateur du microphone à partir d'Android 12); `speech_transcribe` ne lit que le fichier qu'AutoJs6 lui remet, et `speech_synthesize` rend l'audio à AutoJs6, qui l'écrit à l'emplacement demandé. Ni le texte reconnu ni le texte lu ne sont écrits dans les journaux. Les trois outils d'analyse ne lisent eux aussi que le fichier qu'AutoJs6 leur remet, et `speech_diarize` avec `identify` ne renvoie que des noms et des scores, jamais d'empreintes vocales.

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
tool service: org.autojs.plugin.TOOL_PROVIDER (short name speech: listen, speak, transcribe, synthesize, speaker_verify, speaker_identify, diarize, detect_language, tag; AutoJs6 6.8.0+)
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### Remerciements

******

3-Macaw Speech repose sur les projets open source, modèles et données suivants. Merci à leurs auteurs et mainteneurs:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) et l'équipe [k2-fsa (Kaldi nouvelle génération)](https://github.com/k2-fsa): l'environnement d'exécution de la reconnaissance, des mots de réveil, de la détection d'activité vocale et de la synthèse, les exports ONNX des modèles et la bibliothèque Android (Apache-2.0).
- Modèles entraînés avec [icefall](https://github.com/k2-fsa/icefall): les reconnaisseurs Zipformer en continu (bilingue par pfluo, anglais par Zengwei, chinois 2025 par yuekai, français par shaojieli, coréen par johnBamma), le détecteur de mots-clés à vocabulaire ouvert (pkufool), et les modèles de synthèse AISHELL-3 VITS et Baker Matcha-TTS de k2-fsa.
- [Vosk](https://huggingface.co/alphacep/vosk-model-small-streaming-ru) (Alpha Cephei): le petit reconnaisseur russe en continu (Apache-2.0).
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / Alibaba, licence de modèle FunASR 1.1): le reconnaisseur multilingue hors ligne.
- Modèles de ponctuation: le modèle de ponctuation chinois et anglais CT-Transformer de [FunASR](https://github.com/modelscope/FunASR) (Alibaba DAMO Academy) et le modèle de ponctuation et de casse anglais de [Edge-Punct-Casing](https://github.com/frankyoujian/Edge-Punct-Casing) (frankyoujian), tous deux sous Apache-2.0.
- Les modèles de synthèse [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, enregistrements chinois cédés par LongMaoData) et [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT), et le vocodeur [HiFi-GAN](https://github.com/jik876/hifi-gan) (MIT). Les voix française siwis, espagnole davefx, allemande thorsten et russe denis viennent de [piper-voices](https://huggingface.co/rhasspy/piper-voices) (MIT), toutes affinées à partir de la voix anglaise lessac.
- Données d'entraînement: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), le [corpus mandarin standard de Data-Baker](https://www.data-baker.com/open_source.html) (usage non commercial uniquement), les données Blizzard 2013 de Lessac Technologies (recherche uniquement) et [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (domaine public). Les modèles français, coréen et Piper ajoutés en 1.2.0 utilisent aussi [Common Voice](https://commonvoice.mozilla.org/) 12 en français (CC0), [KsponSpeech](https://aihub.or.kr/aihubdata/data/view.do?currMenu=115&topMenu=100&aihubDataSe=realm&dataSetSn=123) (AI Hub, demandes réservées aux ressortissants coréens), la [base de synthèse vocale française SIWIS](https://datashare.is.ed.ac.uk/handle/10283/2353) (CC BY 4.0, SIWIS database, University of Edinburgh and partners), [Thorsten-Voice](https://github.com/thorstenMueller/Thorsten-Voice) (CC0) et les enregistrements davefx et denis de [NabuCasa voice-datasets](https://github.com/NabuCasa/voice-datasets) (CC0).
- Modèles ajoutés en 1.3.0: le FastConformer espagnol de [NVIDIA NeMo](https://huggingface.co/nvidia/stt_es_fastconformer_hybrid_large_pc) (CC-BY-4.0; entraîné sur Common Voice 12 (CC0), Multilingual LibriSpeech (CC BY 4.0), VoxPopuli (CC0) et Fisher Spanish (LDC)), le modèle en continu à huit langues [PengChengStarling](https://github.com/yangb05/PengChengStarling) (Apache-2.0), [Kokoro v1.0](https://huggingface.co/hexgrad/Kokoro-82M) (hexgrad, Apache-2.0), les voix arabes [dii](https://huggingface.co/OpenVoiceOS/phoonnx_ar-SA_dii_espeak) et [miro](https://huggingface.co/OpenVoiceOS/phoonnx_ar-SA_miro_espeak_V2) d'OpenVoiceOS (entraînées avec [phoonnx](https://github.com/TigreGotico/phoonnx) sur des données de parole synthétique de TigreGotico) et la voix coréenne KSS de Mycroft [Mimic 3](https://github.com/MycroftAI/mimic3-voices) (CC0; entraînée sur le [jeu de données KSS](https://www.kaggle.com/datasets/bryanpark/korean-single-speaker-speech-dataset), CC BY-NC-SA 4.0).
- Le moteur d'inférence [ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT), le modèle d'activité vocale [silero VAD](https://github.com/snakers4/silero-vad) (MIT), [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) et [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT) pour les phonèmes, et [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT) pour les lectures des mots de réveil chinois.

******

### Feuille De Route

******

Les plans, décisions de conception et l'avancement sont suivis sous forme de liste à cocher dans ROADMAP.md. Les éléments non cochés sont des intentions, pas des fonctionnalités de la version actuelle.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### Historique Des Versions

******

#### v1.4.0

_2026/10/30_

- `Fonctionnalité` La reconnaissance de fichiers de la page Essayer peut maintenant lire l'audio: un lecteur au-dessus du résultat avec barre de progression, vitesses de 0.5x à 2x et sauts de 5 secondes. La phrase entendue est surlignée et reste visible, toucher une phrase lance la lecture à cet endroit, et une vue paroles affiche la phrase en cours en grand (aussi pour les locuteurs, les langues et les étiquettes de sons). Le dernier résultat reste lisible après avoir quitté la page tant que le fichier existe: le plugin garde l'accès en lecture à ce seul fichier jusqu'à ce que vous effaciez le résultat ou en choisissiez un autre.
- `Amélioration` L'identification de la langue se fait désormais phrase par phrase: quand un enregistrement mêle plusieurs langues, on voit la langue de chaque phrase (les phrases très courtes rejoignent une voisine). L'écran d'essai liste chaque phrase, et les segments de speech.detectLanguage dans les scripts et de l'outil speech_detect_language sont aussi des phrases; la langue globale est celle parlée le plus longtemps et les scores des candidates sont des parts du temps de parole. L'option windows ne change pas. Chaque phrase demande son propre passage, c'est donc un peu plus lent qu'en 1.3.0

#### v1.3.0

_2026/10/30_

- `Note` speech.speaker, speech.diarize, speech.detectLanguage et speech.tag dans les scripts et les cinq outils de locuteur et d'analyse demandent la version finale d'AutoJs6 6.8.0 ou ultérieure; la page "Locuteurs", "Seulement les voix enregistrées" et les analyses de fichiers d'Essayer s'utilisent dans le plugin lui-même et ne dépendent pas de la version d'AutoJs6. Avec un AutoJs6 plus ancien, speech.models.list() dans les scripts liste aussi les modèles de reconnaissance du locuteur, de segmentation des locuteurs, d'identification de la langue et d'étiquetage audio (types speaker, segment, langid et tagger), qui s'installent ou se suppriment comme les autres
- `Note` L'analyse d'un long audio demande beaucoup de mémoire: séparer les locuteurs d'un enregistrement de 10 minutes a atteint environ 470 Mo à 1,3 Go lors des tests, donc le plugin demande à Android une limite de mémoire plus élevée; sur un téléphone avec peu de mémoire, essayez d'abord des fichiers plus courts
- `Fonctionnalité` Mode économie d'énergie pour le mot de réveil permanent (Paramètres > Mot de réveil permanent, activé par défaut): le modèle du mot de réveil ne tourne que lorsque le téléphone entend une voix ou un autre son, jamais dans le silence, si bien que le plugin utilise de deux à dix fois moins de temps CPU (moins encore dans le calme). L'enregistrement lui-même consomme autant, donc l'estimation de la batterie par Android ne baisse que peu (mesure dans le README). Un mot de réveil prononcé très bas peut être manqué; désactivez alors l'économie d'énergie
- `Fonctionnalité` Davantage de modèles à télécharger: reconnaissance hors ligne de l'espagnol (NeMo, avec ponctuation); reconnaissance en continu de huit langues, arabe, indonésien, thaï, vietnamien, japonais, russe, chinois et anglais, qui détecte la langue toute seule (PengChengStarling, environ 339 Mo une fois installé); les voix multilingues de Kokoro v1.0 (49 voix en anglais, espagnol, français, hindi, italien, portugais du Brésil et chinois, chacune dans sa propre langue); deux voix arabes et une voix coréenne. Les voix arabes et coréenne sont réservées à un usage non commercial, et les données d'entraînement du modèle à huit langues ne sont pas publiées, son usage commercial est donc inconnu; la licence s'affiche avant chaque téléchargement. Les nouvelles voix apparaissent aussi par langue dans le moteur TTS du système, et la saisie vocale du système et les outils du plugin ne choisissent le modèle à huit langues que pour les langues qu'aucun autre modèle installé ne couvre
- `Fonctionnalité` Reconnaissance du locuteur et "Seulement les voix enregistrées" (Paramètres > Mot de réveil permanent): sur la page "Locuteurs", saisissez un nom et lisez 3 phrases à voix haute pour enregistrer votre voix (plusieurs personnes peuvent s'enregistrer, et "Essayer la reconnaissance" affiche le score); avec "Seulement les voix enregistrées" activé, le mot de réveil permanent compare d'abord la voix et n'ouvre le panneau que si elle correspond à une personne enregistrée, si bien que le mot de réveil prononcé par d'autres est ignoré. Les déclencheurs manuels comme le bloc, le widget et la touche du casque ne changent pas. Le modèle de reconnaissance du locuteur (environ 26,5 Mo) se télécharge d'abord, la page indique comment; les empreintes vocales restent sur l'appareil et ne sont jamais envoyées ni exportées
- `Fonctionnalité` Les scripts peuvent enregistrer, vérifier et identifier des locuteurs avec speech.speaker, et lister ou supprimer les personnes enregistrées; deux nouveaux outils du plugin, speech_speaker_verify (un enregistrement est-il une personne enregistrée donnée) et speech_speaker_identify (la personne enregistrée la plus proche), ne renvoient que des noms et des scores, jamais d'empreintes vocales
- `Fonctionnalité` La reconnaissance de fichiers d'Essayer gagne trois analyses: Séparer les locuteurs (qui parle et quand, avec le nom des voix enregistrées; "Locuteurs et texte" transcrit en plus les paroles de chacun), Identifier la langue (les 99 langues de Whisper) et Étiqueter les sons (lesquels de 527 types de sons, comme la parole, la musique ou un chien qui aboie, contient le fichier). Elles fonctionnent hors ligne sur le téléphone, continuent quand on quitte l'écran et affichent leur progression dans une notification. Les modèles se téléchargent d'abord, et l'écran explique comment: le modèle de segmentation des locuteurs (environ 6 Mo, plus le modèle de reconnaissance du locuteur), le modèle d'identification de la langue (environ 103 Mo) et le modèle d'étiquetage audio (environ 10,5 Mo). Les données d'entraînement du modèle de segmentation comprennent des données réservées à la recherche, il est donc marqué comme réservé à un usage non commercial; celles des modèles d'identification de la langue ne sont pas publiées, leur usage commercial est donc inconnu
- `Fonctionnalité` Les scripts peuvent faire les mêmes analyses avec speech.diarize, speech.detectLanguage et speech.tag (et leurs versions ...Async qui renvoient une Promise), avec suivi de la progression; trois nouveaux outils du plugin, speech_diarize, speech_detect_language et speech_tag, le font aussi. Un fichier peut durer jusqu'à 30 minutes (10 minutes pour speech_diarize); une seule analyse tourne à la fois et les autres attendent leur tour
- `Correctif` Les téléchargements de modèles fonctionnent de nouveau sur Android 7.0: le catalogue et les modèles sont récupérés depuis GitHub, dont les certificats sont désormais émis sous un certificat racine (ISRG Root X1, de Let's Encrypt) qu'Android 7.0 ne possède pas, si bien que chaque téléchargement et chaque actualisation du catalogue échouait avec une erreur de certificat. Le plugin embarque maintenant ce certificat racine et l'utilise avec ceux du système; Android 7.1 et les versions suivantes l'ont déjà et n'étaient pas concernés
- `Correctif` Sur certains téléphones, la taille des modèles et des fichiers affichait un grand espace entre le nombre et l'unité (quand la langue de l'interface n'avait pas de région, comme l'anglais); elle s'affiche maintenant normalement

#### v1.2.0

_2026/10/23_

- `Note` Nouveau mot de réveil permanent, désactivé par défaut: une fois activé, il écoute en continu le mot de réveil sur l'appareil, le voyant du micro reste allumé, une notification reste affichée et la batterie s'use plus vite (voir le README pour une mesure de 30 minutes écran éteint). Il est conseillé de l'utiliser avec "Durée d'écoute" ou "Écouter uniquement pendant la charge"
- `Note` La reconnaissance d'audio autre que WAV depuis les scripts et les quatre outils du plugin demandent la version finale d'AutoJs6 6.8.0 ou ultérieure; avec un AutoJs6 plus ancien, les scripts ne reconnaissent toujours que le WAV. Le mot de réveil permanent, les nouveaux modèles, la ponctuation automatique et l'onglet "Fichier" s'utilisent dans le plugin lui-même et ne dépendent pas de la version d'AutoJs6
- `Note` Nouvelles autorisations: wakelock (seulement pendant qu'Essayer reconnaît un fichier, 60 minutes au plus, pour que la reconnaissance continue écran éteint) et service de premier plan de traitement multimédia (depuis Android 15, pendant la reconnaissance d'un fichier, avec la progression dans une notification)
- `Fonctionnalité` Mot de réveil permanent (Paramètres > Mot de réveil permanent): dire un mot de réveil ("小鹦小鹦" et "Hey Macaw" par défaut; modifiables, avec une sensibilité élevée, moyenne ou faible) ouvre le même panneau de dictée que l'assistant numérique; écran éteint ou verrouillé, l'écran s'allume, et le déverrouillage n'est demandé qu'avant l'envoi ou la copie
- `Fonctionnalité` Un panneau ouvert par le mot de réveil lit le texte reconnu à voix haute, avec une voix de 3-Macaw Speech de cette langue s'il y en a une, sinon avec la voix du système; cela se désactive dans les paramètres
- `Fonctionnalité` Durée d'écoute (désactiver après 10, 30 ou 60 minutes, ou toujours active) et Écouter uniquement pendant la charge (pause une fois débranché, reprise une fois rebranché), les mêmes options que dans 3-Stove Agent
- `Fonctionnalité` Si 3-Macaw Speech n'est pas l'assistant numérique par défaut, l'activation du mot de réveil permanent demande une fois l'autorisation de se superposer aux autres applications; sans elle, un mot de réveil entendu affiche une notification "Appuyer pour parler" qui ouvre le panneau
- `Fonctionnalité` Quand un script, une autre application ou un autre déclencheur de ce plugin utilise le micro, le mot de réveil permanent s'efface et reprend l'écoute une fois qu'ils ont terminé
- `Fonctionnalité` Une seule écoute permanente à la fois entre 3-Stone AI, 3-Stove Agent et 3-Macaw Speech; si une autre est active, il vous est demandé de la désactiver d'abord et vous pouvez ouvrir directement ses paramètres
- `Fonctionnalité` Aucun enregistrement ne démarre seul après un redémarrage ou une mise à jour: tant que l'interrupteur est activé, une notification "Appuyez pour reprendre le mot de réveil permanent" apparaît, et ouvrir l'écran d'accueil ou les paramètres le reprend aussi
- `Fonctionnalité` La reconnaissance de fichiers audio (speech.recognize, speech.detectVoice) accepte, en plus du WAV, les formats courants MP3, AAC (y compris M4A), Ogg (Vorbis, Opus), FLAC et AMR, ainsi que le son des fichiers vidéo (MP4, WebM, etc.); la limite reste de 200 Mo ou 30 minutes (nécessite AutoJs6 6.8.0 ou ultérieur; les versions antérieures n'acceptent que le WAV)
- `Fonctionnalité` Quatre outils du plugin (AutoJs6 6.8.0 ou ultérieur): speech_listen écoute une phrase et renvoie le texte, speech_speak lit à voix haute, speech_transcribe transcrit un fichier audio du téléphone et speech_synthesize synthétise un texte dans un fichier WAV ou M4A. Le plugin MCP Server s'en sert pour qu'un assistant IA sur un ordinateur écoute et parle à travers le téléphone, 3-Stove Agent les appelle directement sur le téléphone et les scripts peuvent les appeler avec plugins.tools; quand l'enregistrement ne peut pas commencer tout de suite, l'écoute affiche d'abord sur le téléphone une notification "Appuyer pour parler" et commence quand on appuie dessus
- `Fonctionnalité` Davantage de modèles à télécharger: reconnaissance en continu pour le français, le russe et le coréen, et voix Piper pour le français, l'espagnol, l'allemand et le russe; les voix Piper et le modèle coréen sont réservés à un usage non commercial et affichent leur licence avant le téléchargement, et le modèle coréen n'est téléchargé que depuis l'adresse amont
- `Fonctionnalité` Ponctuation automatique: après le téléchargement d'un modèle de ponctuation depuis le nouveau groupe "Ponctuation" de l'écran d'accueil, le texte chinois et anglais reconnu par les modèles en continu reçoit virgules, points et points d'interrogation, et l'anglais retrouve ses majuscules; SenseVoice ajoute sa propre ponctuation et n'est pas modifié; les scripts peuvent toujours passer punctuation: false pour retirer toute la ponctuation
- `Fonctionnalité` Nouvel onglet "Fichier" dans Essayer: choisissez un fichier audio ou vidéo (MP3, M4A, WAV et autres, jusqu'à 200 Mo et 30 minutes) pour obtenir en texte ce qui y est dit sans écrire de script, en segments horodatés que vous pouvez copier ou enregistrer en texte brut ou en sous-titres SRT; le modèle, la langue et la ponctuation automatique se choisissent dans l'onglet; la reconnaissance continue quand vous quittez l'écran ou l'éteignez, et une notification affiche la progression avec un bouton d'annulation; le dernier résultat est toujours là à la prochaine ouverture de l'onglet

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
