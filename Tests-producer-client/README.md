# tests-producer-client

Client Python compatible macOS et Windows pour envoyer tous les
`originalMessages` d'un flux vers l'API `tests-producer`.

## Structure attendue

```text
racine-projet/
├── flux1/
│   ├── flux1-v.1.0.conf
│   ├── flux1-v.2.0.conf
│   └── originalMessages/
│       ├── message-sans-extension
│       └── autre-message.msg
├── flux2/
│   ├── flux2-v.1.1.conf
│   └── originalMessages/
└── send_original_messages.py
```

Le script sélectionne la version numérique la plus élevée du `.conf`. Dans :

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

Sur macOS :

```bash
export TESTS_PRODUCER_URL='http://nom-machine:3000'
python3 send_original_messages.py --root '/chemin/vers/racine-projet'
```

Sur Windows PowerShell :

```powershell
$env:TESTS_PRODUCER_URL = 'http://nom-machine:3000'
py send_original_messages.py --root 'C:\chemin\vers\racine-projet'
```

Le nom du flux est demandé dans le terminal :

```text
Nom du flux : flux1
```

Il peut aussi être fourni pour automatiser l'exécution :

```bash
python3 send_original_messages.py --root . --flow flux1
```

Par défaut, les variables proxy du poste sont ignorées, ce qui évite le proxy
local d'entreprise vu sur `127.0.0.1`. Utiliser `--use-system-proxy` uniquement
si l'accès à l'API nécessite réellement le proxy du poste.

Le script s'arrête à la première erreur HTTP. Toutes les erreurs locales
(flux absent, configuration absente, topic introuvable ou JSON invalide) sont
détectées avant le premier envoi.

## Options

```bash
python3 send_original_messages.py --help
```
