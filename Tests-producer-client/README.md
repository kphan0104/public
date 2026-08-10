# tests-producer-client

Client Python compatible macOS et Windows pour envoyer tous les
`originalMessages` d'un flux vers l'API `tests-producer`.

## Structure attendue

```text
racine-projet/
├── flux1/
│   ├── flux1-v1.0.conf
│   ├── flux1-v2.0.conf
│   └── originalMessages/
│       ├── message-sans-extension
│       └── autre-message.msg
├── flux2/
│   ├── flux2-v1.1.conf
│   └── originalMessages/
├── send_original_messages.py
└── generate_flow_topics.py
```

`send_original_messages.py` lit les fichiers sans extension et `.msg` par ordre
alphabétique. Il sélectionne le dernier fichier `flux-vX.Y.conf`, extrait le
topic par défaut de l'input Kafka, puis envoie au serveur le contenu brut, le
nom du flux et ce topic. Un nouveau flux peut donc être testé sans avoir été
ajouté au `flow-topics.yml` de `tests-producer`.

Les fichiers peuvent contenir n'importe quel message texte : leur contenu est
envoyé tel quel dans le champ `originalMessage`. Les fichiers cachés et les
autres extensions sont ignorés.

## Génération de flow-topics.yml

`generate_flow_topics.py` accepte uniquement les configurations de la forme
`flux-v1.0.conf`, sélectionne la version numérique la plus élevée et extrait le
topic par défaut de :

```text
topics => "${KAFKA_TOPIC:integration.events}"
```

Il parcourt tous les dossiers non cachés placés directement dans le répertoire
racine, qu'ils contiennent ou non un dossier `originalMessages`. Un dossier
sans configuration portant son nom est simplement ignoré.

Le script utilise toujours `integration.events`, sans lire `KAFKA_TOPIC` dans
l'environnement.

Depuis le répertoire racine qui contient les dossiers de flux :

```bash
python3 generate_flow_topics.py
```

Il génère un fichier complet `flow-topics.yml` :

```yaml
tests-producer:
  flows:
    topics:
      flux1: integration.events
      flux2: another.events
```

Le même bloc YAML est affiché seul dans le terminal, sans message autour. Il
peut donc être copié directement dans `flow-topics.yml`.

Copier ce fichier à côté de `tests-producer.jar` et de `application.yml`, puis
redémarrer le serveur. Cette liste alimente uniquement le sélecteur `flow` de
Swagger ; elle n'est pas utilisée par `send_original_messages.py`.

## Exécution

Python 3.8 ou supérieur est recommandé. Le script utilise uniquement la
bibliothèque standard de Python : aucune installation avec `pip` n'est
nécessaire.

Renseigner l'URL de `tests-producer` directement en haut de
`send_original_messages.py` :

```python
TESTS_PRODUCER_URL = "http://nom-machine:3000"
```

Sur macOS :

```bash
cd '/chemin/vers/racine-projet'
python3 send_original_messages.py flux1
```

Sur Windows PowerShell :

```powershell
Set-Location 'C:\chemin\vers\racine-projet'
py send_original_messages.py flux1
```

Le script doit être lancé depuis le répertoire qui contient les dossiers de
flux. Le nom du flux est l'unique argument :

```bash
python3 send_original_messages.py flux1
```

Le script s'arrête à la première erreur HTTP. Toutes les erreurs locales
(flux absent, configuration absente, topic introuvable ou originalMessages
absent) sont détectées avant le premier envoi.

Le script appelle l'endpoint `/api/v1/internal/events`, volontairement masqué
de Swagger. Cet endpoint reste néanmoins accessible sur le réseau si son URL
est connue.

## Aide

```bash
python3 send_original_messages.py --help
```
