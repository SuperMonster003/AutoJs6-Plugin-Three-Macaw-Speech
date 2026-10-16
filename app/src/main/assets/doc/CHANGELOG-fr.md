******

### Historique Des Versions

******

# v1.0.0

###### 2026/10/16

* `Note` Préversion de développement: AutoJs6 détecte le plugin, qui charge le moteur vocal et télécharge et gère les modèles; la reconnaissance vocale, les mots de réveil, la synthèse vocale, les écrans du plugin et le module de script speech ne sont pas encore livrés
* `Note` Nécessite AutoJs6 6.8.0 (5451) ou ultérieur (la version minimale exacte sera renseignée une fois les modifications de l'hôte intégrées)
* `Fonctionnalité` Squelette du plugin: protocole d'activation Wake, service d'identité du centre des plugins et service de capacité vocale
* `Fonctionnalité` Moteur vocal sherpa-onnx 1.13.8 et modèle de détection d'activité vocale silero VAD v5 intégrés, avec un paquet par ABI pour 4 ABI
* `Fonctionnalité` Gestion des modèles: télécharge les modèles vocaux hors ligne du catalogue signé depuis les publications des ressources partagées d'AutoJs6, avec repli sur les publications officielles de sherpa-onnx; chaque fichier est vérifié par SHA-256 avant usage, avec reprise des téléchargements, import local (tar, tar.bz2, zip), suppression et modèles par défaut; les modèles réservés à un usage non commercial ou sans licence déclarée exigent d'accepter leur licence au préalable
* `Fonctionnalité` README, instructions du centre des plugins et journaux des modifications en 10 langues, générés à partir de sources JSON
* `Dépendance` Ajout de sherpa-onnx 1.13.8
* `Dépendance` Ajout de Jetpack Compose BOM 2026.09.00
* `Dépendance` Ajout de AndroidX Activity Compose 1.12.2
* `Dépendance` Ajout de AndroidX Core KTX 1.15.0
* `Dépendance` Ajout de Kotlin Coroutines 1.11.0
