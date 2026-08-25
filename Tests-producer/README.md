# tests-producer

Serveur Java 21 qui reçoit un `originalMessage` par REST, construit l'événement
Databus attendu et le publie dans Kafka.

## Versions

- Java 21 ;
- Spring Boot 4.1.0 ;
- Springdoc OpenAPI 3.0.3 ;
- Spring for Apache Kafka 4.1.0 ;
- client Apache Kafka 4.3.1.

Les versions Spring sont gérées par Spring Boot. La propriété Maven
`kafka.version` fixe explicitement le client Kafka.

## Architecture hexagonale

```text
adapter/in/rest
        |
        v
application/port/in  <-  application/service  ->  application/port/out
                                |                          |
                                v                          v
                          domain/model       adapter/out/kafka|json|system
```

- `domain` contient la représentation de l'événement Databus ;
- `application/port/in` expose le cas d'usage de publication ;
- `application/service` orchestre la construction, le calcul de taille et
  l'envoi ;
- `application/port/out` décrit les dépendances vers Kafka, JSON et le hostname ;
- `adapter/in/rest` expose le POST ;
- `adapter/out` implémente les sorties techniques.

Le domaine et le service applicatif ne dépendent pas de Spring.

## Configuration externe obligatoire

Aucun fichier de configuration métier n'est embarqué dans le JAR. Au
démarrage, l'application charge exclusivement `application.yml` et
`flow-topics.yml` placés dans le même dossier que le JAR. Le démarrage échoue
si l'un des deux fichiers est absent, si aucun flux n'est configuré ou si le
token d'administration n'est pas renseigné.

En développement avec Maven, le fichier doit être placé à la racine du projet :

```bash
cp application.yml.example application.yml
cp flow-topics.yml.example flow-topics.yml
chmod 600 application.yml
```

En déploiement, placer les deux fichiers côte à côte :

```text
/opt/tests-producer/
├── tests-producer.jar
├── application.yml
├── flow-topics.yml
├── original-messages/
│   ├── payments.msg
│   └── orders.msg
├── kafka-client-keystore.jks
└── kafka-client-truststore.jks
```

Une localisation différente peut être fournie explicitement avec les deux
fichiers :

```bash
java \
  -Dspring.config.location=file:/chemin/application.yml,file:/chemin/flow-topics.yml \
  -jar tests-producer.jar
```

### Association des flux aux topics

Pour l'API Swagger, le topic n'est pas fourni par l'appelant. Il est déterminé
à partir du flux sélectionné et de `flow-topics.yml` :

```yaml
tests-producer:
  flows:
    topics:
      payments: integration.events
      orders: orders.events
```

Les flux sont triés et affichés sous forme de liste déroulante dans Swagger.
L'API d'administration décrite plus bas permet de créer un flux ou de changer
son topic sans redémarrer l'application. Les endpoints `/events/custom` et
`/raw-events` acceptent quant à eux un topic explicite et ne dépendent pas de
cette liste.

### `originalMessage` proposé selon le flux

Swagger peut préremplir automatiquement l'`originalMessage` lorsque le flux
change. Pour cela, créer un fichier UTF-8 nommé `<flux>.msg` dans le dossier
configuré. Par exemple, le contenu de `original-messages/payments.msg` sera
proposé lorsque le flux `payments` est sélectionné.

Le dossier se configure dans `application.yml` :

```yaml
tests-producer:
  swagger:
    original-messages-directory: /opt/tests-producer/original-messages
```

Il peut aussi être défini avec la variable d'environnement
`TESTS_PRODUCER_ORIGINAL_MESSAGES_DIR`. Un fichier est facultatif : s'il
n'existe pas pour un flux, la zone de texte reste vide. Les messages proposés
restent modifiables avant l'envoi. Après l'ajout ou la modification d'un
fichier via l'API d'administration, il suffit de recharger Swagger pour
récupérer sa valeur ; aucun redémarrage n'est nécessaire.

Un fichier `.msg` peut contenir des marqueurs de timestamp. Ils sont remplacés
uniquement dans le navigateur, au moment où Swagger affiche le message du flux :

```text
{{NOW}}
{{NOW|yyyy-MM-dd HH:mm:ss.SSS|Europe/Paris}}
{{NOW|yyyy-MM-dd'T'HH:mm:ss.SSSXXX|UTC}}
```

`{{NOW}}` produit un timestamp ISO-8601 en UTC. La forme complète accepte un
format puis, facultativement, un fuseau horaire ; sans fuseau, `UTC` est
utilisé. Les éléments de format acceptés sont `yyyy`, `yy`, `MM`, `dd`, `HH`,
`mm`, `ss`, `SSS`, `X`, `XX`, `XXX` et `Z`. Les textes littéraux peuvent être
entourés d'apostrophes, comme le `T` dans l'exemple.

Tous les marqueurs d'un message utilisent le même instant. Le résultat est
visible et reste modifiable dans la zone de texte avant l'envoi. Le timestamp
correspond donc à l'affichage ou au changement du flux, et non au clic sur
`Execute`. Le backend ne remplace jamais ces marqueurs : un appel direct avec
`curl` enverra le texte `{{NOW}}` tel quel. L'endpoint `/raw-events` n'est pas
concerné.

### Kafka SSL avec les JKS

Copier le keystore et le truststore JKS dans un emplacement lisible par
l'application, puis renseigner dans `application.yml` :

```yaml
spring:
  kafka:
    bootstrap-servers:
      - kafka-1.example.net:9093
      - kafka-2.example.net:9093
    properties:
      security.protocol: SSL
      ssl.keystore.type: JKS
      ssl.keystore.location: /opt/tests-producer/kafka-client-keystore.jks
      ssl.keystore.password: mot-de-passe
      ssl.key.password: mot-de-passe-de-la-cle
      ssl.truststore.type: JKS
      ssl.truststore.location: /opt/tests-producer/kafka-client-truststore.jks
      ssl.truststore.password: mot-de-passe
      ssl.endpoint.identification.algorithm: https
```

Les JKS sont utilisés directement par le client Kafka Java. Aucune conversion
en PEM ou PKCS#12 n'est nécessaire.

Si Kafka utilise également SASL, ajouter par exemple :

```yaml
spring:
  kafka:
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: SCRAM-SHA-512
      sasl.jaas.config: >-
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="user" password="password";
```

Les fichiers réels `application.yml`, `flow-topics.yml` et les certificats sont
ignorés par Git.

### Port HTTP

Le port de l'application est entièrement configurable :

```yaml
server:
  port: 8080
```

### Dossier et rotation des logs

Le dossier des logs se configure avec `tests-producer.logs.directory` :

```yaml
tests-producer:
  logs:
    directory: /var/log/tests-producer

logging:
  file:
    name: ${tests-producer.logs.directory}/tests-producer.log
  logback:
    rollingpolicy:
      file-name-pattern: >-
        ${tests-producer.logs.directory}/tests-producer.%d{yyyy-MM-dd}.%i.log.gz
      max-file-size: 10MB
      max-history: 3
      total-size-cap: 1GB
      clean-history-on-start: true
```

La rotation est quotidienne, avec une rotation supplémentaire si un fichier
atteint 10 Mo. Grâce à `max-history: 3`, seuls les trois derniers jours
d'archives sont conservés ; les plus anciens sont supprimés. Le nettoyage est
également exécuté au démarrage.

Dans le modèle fourni, le dossier peut aussi être remplacé par la variable
d'environnement `TESTS_PRODUCER_LOG_DIR`.

## Lancement

En développement :

```bash
cp application.yml.example application.yml
cp flow-topics.yml.example flow-topics.yml
export TESTS_PRODUCER_ADMIN_TOKEN="$(openssl rand -hex 32)"
./mvnw spring-boot:run
```

En déploiement :

```bash
./mvnw clean package
mkdir -p deployment
cp target/tests-producer-0.1.0-SNAPSHOT.jar deployment/tests-producer.jar
cp application.yml.example deployment/application.yml
cp flow-topics.yml.example deployment/flow-topics.yml
# Modifier les deux fichiers YAML avec les vraies valeurs.
export TESTS_PRODUCER_ADMIN_TOKEN="$(openssl rand -hex 32)"
export TESTS_PRODUCER_FLOW_TOPICS_FILE="$PWD/deployment/flow-topics.yml"
java -jar deployment/tests-producer.jar
```

L'API écoute par défaut sur le port `8080`. Le health check est disponible sur
`GET /actuator/health`.

## Swagger UI

L'interface graphique permet de choisir un flux et de saisir directement
l'`originalMessage` dans une zone de texte. Un second endpoint permet de
modifier toutes les valeurs Databus :

```text
http://nom-machine:8080/swagger-ui.html
```

Avec un port configuré à `3000` :

```text
http://nom-machine:3000/swagger-ui.html
```

La description OpenAPI JSON est disponible sur :

```text
http://nom-machine:8080/v3/api-docs
```

Swagger UI utilise cette URL pour charger la description OpenAPI ; elle doit
donc rester accessible. Son bandeau supérieur, le lien visible vers
`/v3/api-docs`, la section `Servers`, l'intitulé `Request body` et la section
`Schemas` sont masqués. La zone de saisie du message reste visible.

Les endpoints restent repliés. Lorsqu'un endpoint est ouvert, ses champs sont
directement éditables sans cliquer sur `Try it out` :

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    default-models-expand-depth: -1
    doc-expansion: list
    try-it-out-enabled: true
```

## Administration des flux

Une seconde interface Swagger, séparée de l'API publique, permet de maintenir
les flux et leurs exemples d'`originalMessage` :

```text
http://nom-machine:8080/admin/swagger-ui.html
```

Les endpoints d'administration n'apparaissent pas dans le Swagger public. Dans
le Swagger d'administration, cliquer sur `Authorize` et saisir le token. Il
sera envoyé dans le header `X-Admin-Token` à chaque requête.

Le token est obligatoire, doit contenir au moins 5 caractères et se configure
de préférence par variable d'environnement :

```bash
export TESTS_PRODUCER_ADMIN_TOKEN="$(openssl rand -hex 32)"
```

La configuration correspondante est :

```yaml
tests-producer:
  admin:
    token: ${TESTS_PRODUCER_ADMIN_TOKEN}
    flow-topics-file: >-
      ${TESTS_PRODUCER_FLOW_TOPICS_FILE:./flow-topics.yml}
```

`flow-topics-file` doit désigner le même fichier que celui chargé au démarrage.
Avec systemd, il est conseillé de définir explicitement les deux variables :

```ini
[Service]
Environment="TESTS_PRODUCER_ADMIN_TOKEN=remplacer-par-un-secret-de-5-caracteres-minimum"
Environment="TESTS_PRODUCER_FLOW_TOPICS_FILE=/opt/tests-producer/flow-topics.yml"
ExecStart=/usr/bin/java -jar /opt/tests-producer/tests-producer.jar --spring.config.location=file:/opt/tests-producer/application.yml,file:/opt/tests-producer/flow-topics.yml
```

Deux opérations sont disponibles :

- `PUT /internal/flows/{flow}?topic=...` crée un flux ou modifie le topic qui
  lui est associé ;
- `PUT /internal/flows/{flow}/original-message` crée ou remplace le fichier
  `<flow>.msg`. Le flux doit exister avant l'enregistrement du message.

Une création retourne `201`, une mise à jour retourne `200`. Les écritures sont
atomiques. Le fichier `flow-topics.yml` est réécrit en triant les flux ; ses
commentaires éventuels ne sont donc pas conservés. Les changements sont pris
en compte immédiatement par les endpoints de publication. Il faut seulement
recharger la page Swagger publique pour actualiser la liste déroulante et les
messages proposés.

Exemples sans Swagger :

```bash
curl --fail-with-body \
  --request PUT \
  --header "X-Admin-Token: ${TESTS_PRODUCER_ADMIN_TOKEN}" \
  'http://localhost:8080/internal/flows/payments?topic=payments.events'

curl --fail-with-body \
  --request PUT \
  --header "X-Admin-Token: ${TESTS_PRODUCER_ADMIN_TOKEN}" \
  --header 'Content-Type: text/plain; charset=utf-8' \
  --data-binary '@./payments.msg' \
  'http://localhost:8080/internal/flows/payments/original-message'
```

## API

### `POST /events`

L'endpoint attend :

- le paramètre obligatoire `flow`, choisi parmi les flux de
  `flow-topics.yml` ;
- un corps `text/plain` obligatoire contenant l'`originalMessage`.

Toutes les autres valeurs Databus utilisent automatiquement leurs valeurs par
défaut. Le topic est trouvé automatiquement à partir de `flow`.

Exemple :

```bash
curl --fail-with-body \
  --request POST \
  --header 'Content-Type: text/plain; charset=utf-8' \
  --data-binary '@./originalMessage.msg' \
  'http://localhost:8080/events?flow=payments'
```

### `POST /events/custom`

Cet endpoint ne consulte pas `flow-topics.yml`. Il reçoit un `topic` obligatoire
saisi librement, un `flow` obligatoire saisi librement et un corps `text/plain`
contenant l'`originalMessage`. Les paramètres Databus optionnels sont
préremplis dans Swagger : `ownerGroup`,
`ownerEntity`, `ownerName`, `providerName`, `providerSource`, `formatVersion`,
`formatType`, `retention`, `location`, `pipelineId` et
`processingDurationMs`.

Les valeurs affichées par défaut sont respectivement `itgp`, `itgp`, `itgp`,
`itgp`, `application`, `1.0.0`, `JSON`, `year`, `MN`, `integrations_tests` et
`100`.

Exemple :

```bash
curl --fail-with-body \
  --request POST \
  --header 'Content-Type: text/plain; charset=utf-8' \
  --data-binary '@./originalMessage.msg' \
  'http://localhost:8080/events/custom?flow=new-flow&topic=new-flow.events'
```

Le contenu texte devient la valeur du champ `originalMessage` dans le message
Kafka, sans être interprété comme du JSON. `timestamp`, `host`, `eventSize` et
`lastStage` restent calculés automatiquement par le serveur.

### `POST /raw-events`

Cet endpoint reçoit uniquement :

- `topic` : topic Kafka obligatoire saisi librement ;
- un corps `text/plain` non vide contenant le message RAW.

Le corps est publié directement comme valeur Kafka, sans enveloppe JSON, sans
`originalMessage` et sans métadonnées Databus :

```bash
curl --fail-with-body \
  --request POST \
  --header 'Content-Type: text/plain; charset=utf-8' \
  --data-binary '@./raw-message' \
  'http://localhost:8080/raw-events?topic=raw.events'
```

Le message RAW conserve exactement les octets reçus et est publié sans clé
Kafka. La limite `tests-producer.publication.max-message-bytes` s'applique aussi
à cet endpoint.

En cas d'erreur Kafka, les trois endpoints retournent un statut `503` avec un
corps `application/problem+json`. Le champ `detail` contient directement le
message de l'exception retournée par le client Kafka, notamment pour les erreurs
d'ACL. La cause et sa stack trace complète sont également écrites dans les logs
du serveur.

Réponse après acquittement par Kafka :

```json
{
  "status": "published",
  "topic": "raw.events",
  "partition": 1,
  "offset": 42,
  "messageSize": 689
}
```

## Message Kafka produit

Les endpoints `/events` et `/events/custom` produisent une
structure JSON imbriquée :

```json
{
  "databus": {
    "flow": {
      "name": "payments",
      "owner": {
        "group": "itgp",
        "entity": "itgp",
        "name": "itgp"
      },
      "provider": {
        "name": "itgp",
        "source": "application"
      },
      "format": {
        "version": "1.0.0",
        "type": "JSON"
      },
      "retention": "year"
    },
    "event": {
      "lineage": {
        "last_stage": 1,
        "stage1": {
          "timestamp": "2026-07-24T10:30:15.123Z",
          "pipeline_id": "integrations_tests",
          "host": "hostname-machine",
          "event_size": 689,
          "location": "MN",
          "processing_duration_ms": 100
        }
      }
    }
  },
  "originalMessage": "2026-07-24 INFO Paiement accepté"
}
```

`event_size` correspond exactement au nombre d'octets UTF-8 du JSON compact
final envoyé dans Kafka. La clé Kafka est le nom du flux.

Le modèle accepte plusieurs stages, exposés directement sous `lineage` avec
les noms `stage1`, `stage2`, etc. `last_stage` est calculé automatiquement à
partir du numéro le plus élevé. Le serveur crée actuellement le `stage1`.

## Tests

```bash
./mvnw test
```
