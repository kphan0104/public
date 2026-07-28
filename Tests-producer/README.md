# tests-producer

Serveur Java 21 qui reçoit un `originalMessage` par REST, construit l'événement
Databus attendu et le publie dans Kafka.

## Versions

- Java 21 ;
- Spring Boot 4.1.0 ;
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

Aucun `application.yml` n'est embarqué dans le JAR. Au démarrage, l'application
charge exclusivement le fichier `application.yml` placé dans le même dossier
que le JAR. Le démarrage échoue si ce fichier est absent.

En développement avec Maven, le fichier doit être placé à la racine du projet :

```bash
cp application.yml.example application.yml
chmod 600 application.yml
```

En déploiement, placer les deux fichiers côte à côte :

```text
/opt/tests-producer/
├── tests-producer.jar
├── application.yml
├── kafka-client-keystore.jks
└── kafka-client-truststore.jks
```

Une localisation différente peut être fournie explicitement avec
`-Dspring.config.location=file:/chemin/application.yml`.

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

Le fichier réel `application.yml` et les fichiers de certificats sont ignorés
par Git.

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
./mvnw spring-boot:run
```

En déploiement :

```bash
./mvnw clean package
mkdir -p deployment
cp target/tests-producer-0.1.0-SNAPSHOT.jar deployment/tests-producer.jar
cp application.yml.example deployment/application.yml
# Modifier deployment/application.yml avec les vraies valeurs.
java -jar deployment/tests-producer.jar
```

L'API écoute par défaut sur le port `8080`. Le health check est disponible sur
`GET /actuator/health`.

## API

### `POST /api/v1/events`

L'endpoint attend une requête `multipart/form-data` avec trois parties
obligatoires :

- `topic` : nom du topic Kafka ;
- `flowName` : nom du flux ;
- `originalMessage` : fichier contenant le message texte à publier.

Exemple :

```bash
curl --fail-with-body \
  --request POST \
  --form 'topic=integration.events' \
  --form 'flowName=payments' \
  --form 'originalMessage=@./originalMessage.msg;type=text/plain' \
  http://localhost:8080/api/v1/events
```

Il ne faut pas ajouter manuellement le header `Content-Type` de la requête :
`curl` génère le type multipart et sa boundary. Le contenu texte du fichier
devient la valeur du champ `originalMessage` dans le message Kafka, sans être
interprété comme du JSON.

Réponse après acquittement par Kafka :

```json
{
  "status": "published",
  "topic": "integration.events",
  "partition": 1,
  "offset": 42,
  "eventSize": 689,
  "timestamp": "2026-07-24T10:30:15.123Z"
}
```

## Message Kafka produit

Le message utilise une structure JSON imbriquée :

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
