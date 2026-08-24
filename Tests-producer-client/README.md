# tests-producer-client

Outil Python standard pour générer le fichier `flow-topics.yml` utilisé par
`tests-producer`.

## Structure attendue

```text
racine-projet/
├── flux1/
│   ├── flux1-v1.0.conf
│   └── flux1-v2.0.conf
├── flux2/
│   └── flux2-v1.1.conf
└── generate_flow_topics.py
```

## Génération de flow-topics.yml

`generate_flow_topics.py` accepte uniquement les configurations de la forme
`flux-v1.0.conf`, sélectionne la version numérique la plus élevée et extrait le
topic par défaut de :

```text
topics => "${KAFKA_TOPIC:integration.events}"
```

Il parcourt tous les dossiers non cachés placés directement dans le répertoire
racine. Un dossier sans configuration portant son nom est ignoré.

Depuis le répertoire racine qui contient les dossiers de flux :

```bash
python3 generate_flow_topics.py
```

Le script utilise uniquement la bibliothèque standard de Python et ne nécessite
aucune installation avec `pip`.

Il génère un fichier complet `flow-topics.yml` :

```yaml
tests-producer:
  flows:
    topics:
      flux1: integration.events
      flux2: another.events
```

Si la dernière configuration d'un flux contient plusieurs topics distincts,
les clés sont numérotées :

```yaml
tests-producer:
  flows:
    topics:
      flux11: first.events
      flux12: second.events
```

Le même bloc YAML est affiché seul dans le terminal. Copier le fichier à côté
de `tests-producer.jar` et de `application.yml`, puis redémarrer le serveur.
