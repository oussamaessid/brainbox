# Publication Android

## Préparer un autre PC

1. Cloner le dépôt et ouvrir le projet avec Android Studio.
2. Installer le JDK 17 et le SDK Android 36.
3. Copier la clé d'upload originale dans `keys/brainbox-upload.jks` par un canal privé.
4. Copier `keystore.properties.example` vers `keystore.properties`, puis renseigner les mots de passe.
5. Vérifier que la clé possède l'empreinte SHA-1 suivante :

   `F1:D0:8C:F6:F5:D9:A6:D9:E3:6A:28:C1:6F:EA:70:36:C7:51:35:B6`

   Commande :

   `keytool -list -v -keystore keys/brainbox-upload.jks -alias key0`

La clé, `keystore.properties` et `local.properties` sont volontairement exclus de Git. Le fichier
`app/google-services.json` contient uniquement la configuration publique de l'application Android
Firebase `app.brainbox` et reste versionné pour permettre une compilation identique sur un autre PC.

## Construire et contrôler la release

```shell
./gradlew clean test lint bundleRelease
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
```

Le bundle attendu est `app/build/outputs/bundle/release/app-release.aab`. Son certificat doit avoir la même empreinte SHA-1 que ci-dessus.

## Google Play Console

1. Importer l'AAB dans un test interne.
2. Vérifier `versionCode 6` et `versionName 1.1.0`.
3. Tester installation, consentement UMP, publicités, jeu FR/EN/AR, mode hors-ligne, partage, statistiques, notifications, profil et classements.
4. Publier progressivement en production après validation du test interne.

## Services externes

- Publier d'abord les fichiers `categories_*.json` du dépôt `BrainboxData`.
- Déployer les règles Firestore depuis le dossier `firebase` avec `firebase deploy --only firestore`.
- Configurer un message RGPD dans AdMob, sinon UMP ne peut afficher aucun formulaire.
- Créer une unité publicitaire récompensée avant d'activer le bouton d'indice en production.
