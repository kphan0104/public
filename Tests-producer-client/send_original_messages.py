#!/usr/bin/env python3
"""Envoie les originalMessages d'un flux vers l'API tests-producer."""

import argparse
import json
import sys
from http.client import HTTPConnection, HTTPSConnection, HTTPException
from pathlib import Path
from typing import List, Optional
from urllib.parse import urlencode, urlparse


TESTS_PRODUCER_URL = "http://nom-machine:3000"


class ClientError(RuntimeError):
    """Erreur fonctionnelle affichable à l'utilisateur."""


class FlowNotFoundError(ClientError):
    pass


class OriginalMessageError(ClientError):
    pass


class ApiRequestError(ClientError):
    pass


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


def send_message(
    endpoint_url: str,
    flow_name: str,
    message: Path,
) -> dict:
    parsed_url = urlparse(endpoint_url)
    connection_type = (
        HTTPSConnection if parsed_url.scheme == "https" else HTTPConnection
    )
    connection = connection_type(parsed_url.hostname, parsed_url.port)
    request_path = parsed_url.path or "/"
    request_path += "?" + urlencode({"flow": flow_name})

    try:
        connection.request(
            "POST",
            request_path,
            body=message.read_bytes(),
            headers={
                "Accept": "application/json",
                "Content-Type": "text/plain; charset=utf-8",
            },
        )
        response = connection.getresponse()
        status = response.status
        charset = response.headers.get_content_charset() or "utf-8"
        response_text = response.read().decode(charset, errors="replace")
    except (OSError, HTTPException) as exception:
        raise ApiRequestError(
            "Échec de l'envoi de '{}': {}".format(message, exception)
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
    flow_name = validate_flow_name(args.flow)
    root_directory = Path.cwd().resolve()
    flow_directory = root_directory / flow_name
    if not flow_directory.is_dir():
        raise FlowNotFoundError(
            "Flux '{}' introuvable dans '{}'".format(
                flow_name,
                root_directory,
            )
        )

    messages = find_original_messages(flow_directory)
    endpoint_url = build_endpoint_url(TESTS_PRODUCER_URL)

    print("Flux             : {}".format(flow_name))
    print("API               : {}".format(endpoint_url))
    print("originalMessages  : {}".format(len(messages)))

    for index, message in enumerate(messages, start=1):
        print("[{}/{}] Envoi de {}...".format(index, len(messages), message.name))
        result = send_message(endpoint_url, flow_name, message)
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
        "flow",
        help="nom du flux à traiter",
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
