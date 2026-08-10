#!/usr/bin/env python3
"""Génère flow-topics.yml depuis les pipelines Logstash versionnés."""

import json
import re
import sys
from pathlib import Path
from typing import Dict, Tuple


OUTPUT_FILE_NAME = "flow-topics.yml"

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


class GenerationError(RuntimeError):
    pass


def parse_version(raw_version: str) -> Tuple[int, ...]:
    parts = [int(part) for part in raw_version.split(".")]
    while len(parts) > 1 and parts[-1] == 0:
        parts.pop()
    return tuple(parts)


def find_flow_directories(root_directory: Path):
    flows = sorted(
        (
            candidate
            for candidate in root_directory.iterdir()
            if candidate.is_dir()
            and not candidate.name.startswith(".")
        ),
        key=lambda path: path.name.casefold(),
    )
    if not flows:
        raise GenerationError(
            "Aucun dossier de flux trouvé dans '{}'".format(root_directory)
        )
    return flows


def find_latest_configuration(flow_directory: Path) -> Tuple[Path, str]:
    pattern = re.compile(
        r"^{}-v(?P<version>\d+(?:\.\d+)*)\.conf$".format(
            re.escape(flow_directory.name)
        ),
        re.IGNORECASE,
    )
    versioned_files = []
    for candidate in flow_directory.iterdir():
        if not candidate.is_file():
            continue
        match = pattern.fullmatch(candidate.name)
        if match is None:
            continue
        raw_version = match.group("version")
        versioned_files.append(
            (parse_version(raw_version), raw_version, candidate)
        )

    if not versioned_files:
        raise GenerationError(
            "Aucun fichier '{}-vX.Y.conf' trouvé dans '{}'".format(
                flow_directory.name,
                flow_directory,
            )
        )

    latest_version = max(item[0] for item in versioned_files)
    latest_files = [
        item for item in versioned_files if item[0] == latest_version
    ]
    if len(latest_files) > 1:
        names = ", ".join(sorted(item[2].name for item in latest_files))
        raise GenerationError(
            "Plusieurs configurations portent la dernière version pour '{}': "
            "{}".format(flow_directory.name, names)
        )

    _, raw_version, configuration = latest_files[0]
    return configuration, raw_version


def extract_default_topic(configuration: Path) -> str:
    try:
        content = configuration.read_text(encoding="utf-8-sig")
    except (OSError, UnicodeError) as exception:
        raise GenerationError(
            "Impossible de lire '{}': {}".format(configuration, exception)
        ) from exception

    topics = {
        match.group("default_topic").strip()
        for match in TOPIC_PATTERN.finditer(content)
    }
    if not topics:
        raise GenerationError(
            "Aucun topics => \"${{VARIABLE:topic}}\" trouvé dans '{}'".format(
                configuration
            )
        )
    if len(topics) > 1:
        raise GenerationError(
            "Plusieurs topics différents trouvés dans '{}': {}".format(
                configuration,
                ", ".join(sorted(topics)),
            )
        )

    topic = topics.pop()
    if len(topic) > 249 or KAFKA_TOPIC_PATTERN.fullmatch(topic) is None:
        raise GenerationError(
            "Le topic '{}' trouvé dans '{}' n'est pas valide".format(
                topic,
                configuration,
            )
        )
    return topic


def collect_flow_topics(root_directory: Path) -> Dict[str, str]:
    flow_topics = {}
    for flow_directory in find_flow_directories(root_directory):
        configuration, version = find_latest_configuration(flow_directory)
        topic = extract_default_topic(configuration)
        flow_topics[flow_directory.name] = topic
        print(
            "{} -> {} ({} v{})".format(
                flow_directory.name,
                topic,
                configuration.name,
                version,
            )
        )
    return flow_topics


def render_yaml(flow_topics: Dict[str, str]) -> str:
    lines = [
        "tests-producer:",
        "  flows:",
        "    topics:",
    ]
    for flow, topic in sorted(flow_topics.items()):
        lines.append(
            "      {}: {}".format(
                json.dumps(flow, ensure_ascii=False),
                json.dumps(topic, ensure_ascii=False),
            )
        )
    return "\n".join(lines) + "\n"


def main() -> int:
    root_directory = Path.cwd().resolve()
    try:
        flow_topics = collect_flow_topics(root_directory)
        output_file = root_directory / OUTPUT_FILE_NAME
        output_file.write_text(render_yaml(flow_topics), encoding="utf-8")
    except (GenerationError, OSError) as exception:
        print("ERREUR: {}".format(exception), file=sys.stderr)
        return 1

    print(
        "{} flux écrits dans '{}'".format(len(flow_topics), output_file)
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
