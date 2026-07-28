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
└── send_original_messages.py
```

Le script accepte uniquement les noms de la forme `flux-v1.0.conf`, puis
sélectionne la version numérique la plus élevée. Dans :

```text
topics => "${KAFKA_TOPIC:integration.events}"
```

il utilise toujours `integration.events`, sans lire `KAFKA_TOPIC` dans
l'environnement.

Les fichiers sans extension et `.msg` sont traités par ordre alphabétique. Ils
doivent tous contenir un JSON valide et non nul. Les fichiers cachés et les
autres extensions sont ignorés.

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
(flux absent, configuration absente, topic introuvable ou JSON invalide) sont
détectées avant le premier envoi.

## Aide

```bash
python3 send_original_messages.py --help
```
