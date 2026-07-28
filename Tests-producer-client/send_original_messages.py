#!/usr/bin/env python3
"""Envoie les originalMessages d'un flux vers l'API tests-producer."""

import argparse
import json
import re
import sys
import uuid
from http.client import HTTPConnection, HTTPSConnection, HTTPException
from pathlib import Path
from typing import List, Optional, Tuple
from urllib.parse import urlparse


TESTS_PRODUCER_URL = "http://nom-machine:3000"


CONF_VERSION_PATTERN = re.compile(
    r"-v\.(?P<version>\d+(?:\.\d+)*)\.conf$",
    re.IGNORECASE,
)
TOPIC_PATTERN = re.compile(
    r"""^[ \t]*topics[ \t]*=>[ \t]*
        (?:\[[ \t]*)?
        ["']\$\{
        (?P<variable>[A-Za-z_][A-Za-z0-9_]*)
        :(?P<default_topic>[^}]+)
        \}["']
        (?:[ \t]*\])?
        [ \t]*(?:\#.*)?$
    """,
    re.MULTILINE | re.VERBOSE,
)
KAFKA_TOPIC_PATTERN = re.compile(r"^(?!\.{1,2}$)[A-Za-z0-9._-]+$")


class ClientError(RuntimeError):
    """Erreur fonctionnelle affichable à l'utilisateur."""


class FlowNotFoundError(ClientError):
    pass


class PipelineConfigurationError(ClientError):
    pass


class TopicNotFoundError(ClientError):
    pass


class OriginalMessageError(ClientError):
    pass


class ApiRequestError(ClientError):
    pass


def parse_version(raw_version: str) -> Tuple[int, ...]:
    parts = [int(part) for part in raw_version.split(".")]
    while len(parts) > 1 and parts[-1] == 0:
        parts.pop()
    return tuple(parts)


def find_latest_configuration(flow_directory: Path) -> Tuple[Path, str]:
    versioned_files = []
    for candidate in flow_directory.iterdir():
        if not candidate.is_file():
            continue
        match = CONF_VERSION_PATTERN.search(candidate.name)
        if match is None:
            continue
        raw_version = match.group("version")
        versioned_files.append(
            (parse_version(raw_version), raw_version, candidate)
        )

    if not versioned_files:
        raise PipelineConfigurationError(
            "Aucun fichier versionné '*-v.X.Y.conf' trouvé dans '{}'".format(
                flow_directory
            )
        )

    latest_version = max(item[0] for item in versioned_files)
    latest_files = [
        item for item in versioned_files if item[0] == latest_version
    ]
    if len(latest_files) > 1:
        names = ", ".join(sorted(item[2].name for item in latest_files))
        raise PipelineConfigurationError(
            "Plusieurs configurations portent la dernière version : {}".format(
                names
            )
        )

    _, raw_version, configuration = latest_files[0]
    return configuration, raw_version


def extract_default_topic(configuration: Path) -> str:
    try:
        content = configuration.read_text(encoding="utf-8-sig")
    except (OSError, UnicodeError) as exception:
        raise PipelineConfigurationError(
            "Impossible de lire '{}': {}".format(configuration, exception)
        ) from exception

    matches = list(TOPIC_PATTERN.finditer(content))
    if not matches:
        raise TopicNotFoundError(
            "Aucun 'topics => \"${{VARIABLE:default_topic}}\"' trouvé dans "
            "'{}'".format(configuration)
        )

    topics = {
        match.group("default_topic").strip()
        for match in matches
    }
    if len(topics) > 1:
        raise TopicNotFoundError(
            "Plusieurs topics par défaut différents trouvés dans '{}': {}"
            .format(configuration, ", ".join(sorted(topics)))
        )

    topic = topics.pop()
    if len(topic) > 249 or KAFKA_TOPIC_PATTERN.fullmatch(topic) is None:
        raise TopicNotFoundError(
            "Le topic par défaut '{}' trouvé dans '{}' n'est pas valide"
            .format(topic, configuration)
        )
    return topic


def validate_flow_name(flow_name: str) -> str:
    normalized_name = flow_name.strip()
    if (
        not normalized_name
        or "/" in normalized_name
        or "\\" in normalized_name
        or normalized_name in {".", ".."}
    ):
        raise FlowNotFoundError("Nom de flux invalide : {!r}".format(flow_name))
    return normalized_name


def find_original_messages(flow_directory: Path) -> List[Path]:
    messages_directory = flow_directory / "originalMessages"
    if not messages_directory.is_dir():
        raise OriginalMessageError(
            "Dossier originalMessages introuvable : '{}'".format(
                messages_directory
            )
        )

    messages = sorted(
        (
            candidate
            for candidate in messages_directory.iterdir()
            if candidate.is_file()
            and not candidate.name.startswith(".")
            and (
                candidate.suffix == ""
                or candidate.suffix.lower() == ".msg"
            )
        ),
        key=lambda path: path.name.casefold(),
    )
    if not messages:
        raise OriginalMessageError(
            "Aucun fichier sans extension ou .msg trouvé dans '{}'".format(
                messages_directory
            )
        )
    return messages


def validate_json_file(message: Path) -> None:
    try:
        with message.open("r", encoding="utf-8-sig") as stream:
            value = json.load(stream)
    except (OSError, UnicodeError, json.JSONDecodeError) as exception:
        raise OriginalMessageError(
            "Le fichier '{}' ne contient pas un JSON valide : {}".format(
                message,
                exception,
            )
        ) from exception

    if value is None:
        raise OriginalMessageError(
            "Le fichier '{}' contient null, ce qui est interdit".format(message)
        )


def build_endpoint_url(base_url: str) -> str:
    normalized_url = base_url.strip().rstrip("/")
    parsed_url = urlparse(normalized_url)
    if parsed_url.scheme not in {"http", "https"} or not parsed_url.netloc:
        raise ClientError(
            "TESTS_PRODUCER_URL doit être une URL HTTP(S) valide"
        )
    if normalized_url.endswith("/api/v1/events"):
        return normalized_url
    return normalized_url + "/api/v1/events"


def encode_multipart(
    topic: str,
    flow_name: str,
    message: Path,
) -> Tuple[bytes, str]:
    """Construit le multipart/form-data sans dépendance externe."""
    boundary = "----tests-producer-{}".format(uuid.uuid4().hex)
    boundary_bytes = boundary.encode("ascii")
    body = bytearray()

    def add_text_field(name: str, value: str) -> None:
        body.extend(b"--" + boundary_bytes + b"\r\n")
        body.extend(
            'Content-Disposition: form-data; name="{}"\r\n\r\n'.format(
                name
            ).encode("ascii")
        )
        body.extend(value.encode("utf-8"))
        body.extend(b"\r\n")

    add_text_field("topic", topic)
    add_text_field("flowName", flow_name)

    body.extend(b"--" + boundary_bytes + b"\r\n")
    body.extend(
        b'Content-Disposition: form-data; name="originalMessage"; '
        b'filename="originalMessage.json"\r\n'
    )
    body.extend(b"Content-Type: application/json\r\n\r\n")
    body.extend(message.read_bytes())
    body.extend(b"\r\n--" + boundary_bytes + b"--\r\n")

    return bytes(body), "multipart/form-data; boundary={}".format(boundary)


def send_message(
    endpoint_url: str,
    topic: str,
    flow_name: str,
    message: Path,
) -> dict:
    parsed_url = urlparse(endpoint_url)
    connection_type = (
        HTTPSConnection if parsed_url.scheme == "https" else HTTPConnection
    )
    connection = connection_type(parsed_url.hostname, parsed_url.port)
    request_path = parsed_url.path or "/"
    if parsed_url.query:
        request_path += "?" + parsed_url.query

    try:
        body, content_type = encode_multipart(topic, flow_name, message)
        connection.request(
            "POST",
            request_path,
            body=body,
            headers={
                "Accept": "application/json",
                "Content-Type": content_type,
            },
        )
        response = connection.getresponse()
        status = response.status
        charset = response.headers.get_content_charset() or "utf-8"
        response_text = response.read().decode(charset, errors="replace")
    except (OSError, HTTPException) as exception:
        raise ApiRequestError(
            "Échec de l'envoi de '{}': {}".format(
                message,
                exception,
            )
        ) from exception
    finally:
        connection.close()

    if not 200 <= status < 300:
        details = " - {}".format(response_text[:1000]) if response_text else ""
        raise ApiRequestError(
            "Échec de l'envoi de '{}': HTTP {} {}{}".format(
                message,
                status,
                response.reason,
                details,
            )
        )

    try:
        return json.loads(response_text)
    except json.JSONDecodeError:
        return {"status": status, "body": response_text}


def execute(args: argparse.Namespace) -> int:
    root_directory = args.root.expanduser().resolve()
    if not root_directory.is_dir():
        raise ClientError(
            "Répertoire racine introuvable : '{}'".format(root_directory)
        )

    flow_input = args.flow
    if flow_input is None:
        flow_input = input("Nom du flux : ")
    flow_name = validate_flow_name(flow_input)
    flow_directory = root_directory / flow_name
    if not flow_directory.is_dir():
        raise FlowNotFoundError(
            "Flux '{}' introuvable dans '{}'".format(
                flow_name,
                root_directory,
            )
        )

    configuration, version = find_latest_configuration(flow_directory)
    topic = extract_default_topic(configuration)
    messages = find_original_messages(flow_directory)
    for message in messages:
        validate_json_file(message)

    endpoint_url = build_endpoint_url(TESTS_PRODUCER_URL)

    print("Flux             : {}".format(flow_name))
    print("Configuration     : {} (version {})".format(configuration.name, version))
    print("Topic             : {}".format(topic))
    print("API               : {}".format(endpoint_url))
    print("originalMessages  : {}".format(len(messages)))

    for index, message in enumerate(messages, start=1):
        print("[{}/{}] Envoi de {}...".format(index, len(messages), message.name))
        result = send_message(
            endpoint_url,
            topic,
            flow_name,
            message,
        )
        print("         OK: {}".format(result))

    print("Terminé : {} message(s) envoyé(s).".format(len(messages)))
    return 0


def create_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Envoie les originalMessages d'un flux vers tests-producer"
        )
    )
    parser.add_argument(
        "--root",
        type=Path,
        default=Path.cwd(),
        help="racine contenant les dossiers de flux (défaut: dossier courant)",
    )
    parser.add_argument(
        "--flow",
        help="nom du flux ; demandé interactivement si absent",
    )
    return parser


def main(argv: Optional[List[str]] = None) -> int:
    parser = create_parser()
    args = parser.parse_args(argv)

    try:
        return execute(args)
    except ClientError as exception:
        print("ERREUR: {}".format(exception), file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print("\nInterrompu par l'utilisateur.", file=sys.stderr)
        return 130


if __name__ == "__main__":
    sys.exit(main())
