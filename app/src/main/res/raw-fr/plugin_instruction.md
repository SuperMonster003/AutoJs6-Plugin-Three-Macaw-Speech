3-Macaw Speech s'appuie sur [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) pour exécuter hors ligne, sur l'appareil, la reconnaissance vocale, la détection d'activité vocale, la détection de mots de réveil et la synthèse vocale neuronale. Tout est calculé localement et aucun son n'est envoyé.

> Il s'agit d'une préversion de développement: AutoJs6 détecte le plugin, qui télécharge et gère les modèles, reconnaît la parole, détecte l'activité vocale et les mots de réveil et synthétise la parole, y compris comme moteur TTS du système, mais les écrans du plugin ne sont pas encore livrés, et la version d'AutoJs6 qui fournit le module de script `speech` n'est pas encore publiée. Le texte ci-dessous décrit les objectifs de conception de la version 1.0.0; consultez la feuille de route pour l'avancement.

### Points Forts

- Reconnaissance vocale: dictée en continu depuis le microphone (résultats partiels et finaux en temps réel) et reconnaissance de fichiers WAV, avec des modèles hors ligne pour le chinois, l'anglais, le chinois et l'anglais mélangés, le japonais, le coréen et le cantonais.
- Détection d'activité vocale: le silero VAD v5 intégré découpe l'audio en segments de parole et termine aussi automatiquement les phrases dictées.
- Mots de réveil: saisissez le mot-clé en texte brut (par exemple `小六小六` ou `hey macaw`) sans entraîner de modèle; après le réveil, la dictée peut démarrer automatiquement.
- Synthèse vocale: plusieurs voix neuronales chinoises et anglaises, synthétisées dans des fichiers WAV ou utilisées comme moteur TTS du système par le module `tts` d'AutoJs6 et d'autres applications.
- Gestion des modèles: téléchargement depuis un catalogue signé, activation seulement après vérification SHA-256 de chaque fichier, et import de modèles locaux; les modèles réservés à un usage non commercial affichent leur licence avant le téléchargement.

L'avancement et la conception sont disponibles sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech).
