3-Macaw Speech effectue hors ligne, sur le téléphone, la reconnaissance vocale, la détection de mots de réveil et la synthèse vocale; une fois les modèles téléchargés, tous les calculs restent sur l'appareil et aucun son n'est envoyé. Il fonctionne seul, sans AutoJs6, et peut aussi servir de moteur de synthèse vocale du système, de saisie vocale et d'assistant numérique. Avec AutoJs6, les scripts peuvent dicter, transcrire des fichiers audio, attendre des mots de réveil et lire du texte à voix haute.

### Prise en main

1. Ouvrez 3-Macaw Speech et touchez "Accorder" sur la ligne "Autorisation du microphone" de l'écran d'accueil.
2. Sur l'écran d'accueil, téléchargez les modèles nécessaires dans les groupes "Reconnaissance vocale", "Mots de réveil" et "Synthèse vocale"; les modèles marqués "Recommandé" sont un bon point de départ.
3. Touchez le bouton "Essayer" de l'écran d'accueil pour essayer la dictée, les mots de réveil et la synthèse.
4. Pour l'utiliser dans des scripts, activez ce plugin dans le "Centre des plugins" d'AutoJs6.

### Fonctionnalités principales

- **Reconnaissance vocale**: dictée en direct depuis le microphone, ainsi que transcription de fichiers audio et du son des vidéos; des modèles hors ligne couvrent le chinois, l'anglais, le japonais, le cantonais, etc. Après le téléchargement d'un modèle "Ponctuation", les résultats sont ponctués automatiquement.
- **Mots de réveil**: saisissez directement le mot-clé, sans entraîner de modèle; après un mot de réveil, une phrase peut être dictée dans la foulée.
- **Synthèse vocale**: des voix neuronales lisent le texte ou l'enregistrent dans des fichiers audio; en tant que moteur de synthèse vocale du système, il lit aussi pour le module `tts` d'AutoJs6 et les autres applications.
- **Saisie vocale et assistant numérique**: les boutons de microphone des autres applications peuvent l'utiliser pour dicter hors ligne; une fois défini comme assistant numérique par défaut, un appui long sur la touche marche/arrêt ou Accueil permet de dire une phrase, dont le texte va à une application qui accepte les commandes vocales.
- **Déclencheurs**: des points d'entrée comme "Bloc des Réglages rapides", "Widget et raccourci de l'écran d'accueil" et "Micro flottant" ouvrent tous le même panneau de dictée; rien n'est enregistré le reste du temps.
- **Mot de réveil permanent**: désactivé par défaut; une fois activé, dire un mot de réveil ouvre le panneau de dictée, et un mot de réveil peut aussi "Envoyer une diffusion" ou "Notifier seulement". Pendant ce temps, le voyant du microphone reste allumé et la batterie s'use plus vite.
- **Reconnaissance du locuteur**: enregistrez votre voix sur la page "Locuteurs", puis activez "Seulement les voix enregistrées" pour que le mot de réveil permanent ne réponde qu'aux personnes enregistrées.
- **Transcription et analyse de fichiers**: l'onglet "Fichier" de "Essayer" transforme l'audio en texte et propose "Enregistrer en sous-titres"; il peut aussi séparer les locuteurs, identifier la langue et étiqueter les sons.
- **Gestion des modèles**: un modèle n'est activé qu'après la vérification de chacun de ses fichiers; quand une nouvelle version sort, mettez-le à jour directement depuis sa carte, ou utilisez "Importer un modèle local".
- **Outils du plugin**: via le plugin MCP Server ou `plugins.tools.call`, écoutez une phrase, lisez du texte à voix haute et transcrivez des fichiers audio.

### Utilisation dans les scripts

Dans les scripts Rhino d'AutoJs6, ces fonctions passent par le module global `speech`; téléchargez d'abord des modèles de mots de réveil, de reconnaissance vocale et de synthèse vocale. L'exemple suivant écoute une phrase après un mot de réveil, puis affiche le texte reconnu et le lit à voix haute:

```js
let wake = speech.wake(["hey macaw", "小六小六"], { thenListen: true });
wake.on("wake", e => {
    e.listen.on("final", r => {
        console.log(r.text);
        speech.speak(r.text);
    });
});
```

- `speech.recognize` transcrit un fichier audio et `speech.synthesize` enregistre du texte dans un fichier WAV; ces deux méthodes sont bloquantes et ne peuvent pas être appelées sur le thread UI, utilisez donc `recognizeAsync` et `synthesizeAsync` dans les scripts d'interface.
- Une seule session de dictée ou de réveil peut être ouverte à la fois; en ouvrir une autre échoue avec `SESSION_BUSY`, sans file d'attente.
- Les scripts Node.js utilisent la même API via `autojs6:speech`; les scripts Bun utilisent `host.speech`, qui nécessite l'autorisation `speech.control`.
- L'utilisation complète, dont la reconnaissance du locuteur, l'analyse audio et la gestion des modèles, est décrite dans [Documentation AutoJs6 > speech](https://docs.autojs6.com/#/speech).

### Prérequis

- Android 7.0 ou version ultérieure. Les écrans du plugin, le moteur de synthèse vocale du système, la saisie vocale et l'assistant numérique ne nécessitent pas AutoJs6.
- Le module `speech` et les outils du plugin nécessitent AutoJs6 6.8.0 ou version ultérieure, avec ce plugin activé dans le "Centre des plugins".
- L'autorisation du microphone sert à la dictée et à la détection des mots de réveil; elle doit être accordée à ce plugin lui-même, celle d'AutoJs6 ne la remplace pas. L'autorisation des notifications sert à afficher la progression des téléchargements de modèles.
- L'autorisation de superposition est facultative et permet au "Micro flottant" et au mot de réveil permanent d'ouvrir le panneau de dictée directement depuis l'arrière-plan; le "Déclencheur par touche" s'active dans les paramètres d'accessibilité du système, filtre seulement les touches et ne lit jamais l'écran.
- Les modèles se téléchargent par Internet, de quelques Mo à quelques centaines de Mo chacun; une fois téléchargés, la reconnaissance et la synthèse fonctionnent hors ligne.

### Questions fréquentes

**Définir le moteur de synthèse vocale par défaut du système**

Touchez "Ouvrir les paramètres" sur la ligne "Moteur de synthèse vocale du système" de l'écran d'accueil, puis choisissez 3-Macaw Speech comme moteur préféré dans la page du système. Le moteur ne propose que les langues couvertes par les modèles de synthèse installés; si l'écoute d'essai reste muette, téléchargez d'abord un modèle de synthèse qui couvre la langue de l'appareil.

**Raisons pour lesquelles le voyant du microphone reste allumé avec le mot de réveil permanent**

Écouter les mots de réveil impose d'enregistrer en continu, et depuis Android 12 la barre d'état affiche le voyant du microphone pendant tout ce temps, sans que les applications puissent le masquer. Pensez à limiter la "Durée d'écoute" ou à activer "Écouter uniquement pendant la charge" dans les paramètres.

**Cas où le téléchargement d'un modèle échoue**

Le plugin télécharge d'abord depuis GitHub et passe automatiquement à l'adresse d'origine en cas d'échec; un téléchargement interrompu reprend là où il s'était arrêté. Si cela échoue encore, téléchargez l'archive du modèle sur un autre appareil, puis importez-la avec "Importer un modèle local" sur l'écran d'accueil.

**Raisons de l'attente lors de la première dictée d'un script**

Le plugin libère les modèles inactifs, si bien que la première dictée suivante doit recharger le modèle, ce qui peut prendre quelques secondes sur les téléphones anciens. Régler "Libérer les modèles inactifs" sur "Jamais" dans les paramètres supprime cette attente, au prix d'une mémoire occupée en permanence.

La liste des modèles, la configuration de chaque point d'entrée et d'autres questions fréquentes se trouvent sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech).
