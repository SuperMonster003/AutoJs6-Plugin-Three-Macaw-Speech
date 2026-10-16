******

### Historique Des Versions

******

# v1.0.0

###### 2026/10/16

* `Note` Préversion de développement: AutoJs6 détecte le plugin, qui télécharge et gère les modèles, reconnaît la parole dans des fichiers WAV, du PCM fourni et au microphone, et détecte l'activité vocale et les mots de réveil; la synthèse vocale, les écrans du plugin et le module de script speech ne sont pas encore livrés
* `Note` Nécessite AutoJs6 6.8.0 (5451) ou ultérieur (la version minimale exacte sera renseignée une fois les modifications de l'hôte intégrées)
* `Fonctionnalité` Squelette du plugin: protocole d'activation Wake, service d'identité du centre des plugins et service de capacité vocale
* `Fonctionnalité` Moteur vocal sherpa-onnx 1.13.8 et modèle de détection d'activité vocale silero VAD v5 intégrés, avec un paquet par ABI pour 4 ABI
* `Fonctionnalité` Gestion des modèles: télécharge les modèles vocaux hors ligne du catalogue signé depuis les publications des ressources partagées d'AutoJs6, avec repli sur les publications officielles de sherpa-onnx; chaque fichier est vérifié par SHA-256 avant usage, avec reprise des téléchargements, import local (tar, tar.bz2, zip), suppression et modèles par défaut; les modèles réservés à un usage non commercial ou sans licence déclarée exigent d'accepter leur licence au préalable
* `Fonctionnalité` Reconnaissance vocale des fichiers WAV (entiers sur 8, 16, 24 ou 32 bits ou flottants, toute fréquence d'échantillonnage et tout nombre de canaux, rééchantillonnés en 16 kHz mono dans le plugin) avec des segments horodatés, ainsi que des étiquettes de langue, d'émotion et d'événement pour les modèles SenseVoice; les modèles en continu acceptent des mots favoris
* `Fonctionnalité` Sessions de dictée sur le microphone ou sur du PCM 16 kHz fourni: elles signalent la disponibilité, les résultats partiels et finaux, les fins d'énoncé, l'activité vocale et la fin; le silence qui termine une phrase, le délai sans parole et la durée maximale sont réglables; les modèles en continu actualisent les résultats partiels mot par mot, les modèles hors ligne segment par segment
* `Fonctionnalité` Détection d'activité vocale: liste les segments de parole d'un fichier WAV avec le silero VAD intégré
* `Fonctionnalité` Mots de réveil: détection de mots clés à vocabulaire ouvert; les mots clés chinois sont épelés en pinyin selon leur lecture usuelle, les mots anglais en phonèmes du lexique du modèle, et les jetons du modèle peuvent être écrits directement; une détection peut lancer une dictée sur le même enregistrement
* `Fonctionnalité` Pendant une session au microphone, le plugin continue d'enregistrer avec un service de premier plan de type microphone et sa notification, si bien qu'une session commencée alors qu'AutoJs6 est au premier plan continue quand AutoJs6 passe en arrière-plan ou que l'écran s'éteint; une seule session au microphone à la fois; sans l'autorisation du microphone, les sessions échouent avec PERMISSION_DENIED en indiquant l'écran d'autorisation
* `Fonctionnalité` README, instructions du centre des plugins et journaux des modifications en 10 langues, générés à partir de sources JSON
* `Dépendance` Ajout de sherpa-onnx 1.13.8
* `Dépendance` Ajout de Jetpack Compose BOM 2026.09.00
* `Dépendance` Ajout de AndroidX Activity Compose 1.12.2
* `Dépendance` Ajout de AndroidX Core KTX 1.15.0
* `Dépendance` Ajout de Kotlin Coroutines 1.11.0
