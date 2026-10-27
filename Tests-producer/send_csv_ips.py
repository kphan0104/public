#!/usr/bin/env python3
"""Remplace ${IP} dans un message modèle et publie un message par entrée CSV."""

import argparse
import csv
import json
import shutil
import subprocess
import sys
from pathlib import Path
from urllib.parse import urlencode


API_URL = "http://localhost:8080/raw-events"
TOPIC = "mon-topic"
IP_TOKEN = "${IP}"

# Coller ici le contenu du fichier JSON, en conservant le token ${IP}.
# Le préfixe r conserve les antislashs du JSON (par exemple \n ou \" ).
ORIGINAL_MESSAGE = r'''{
  "source_ip": "${IP}",
  "message": "Test de connexion"
}'''


def publish_message(message, ip, line_number):
    url = "{}?{}".format(API_URL, urlencode({"topic": TOPIC}))
    result = subprocess.run(
        [
            "curl",
            "--noproxy", "*",
            "--silent",
            "--show-error",
            "--request", "POST",
            "--header", "Content-Type: text/plain; charset=utf-8",
            "--data-binary", "@-",
            "--write-out", "\n%{http_code}",
            url,
        ],
        input=message.encode("utf-8"),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if result.returncode != 0:
        raise RuntimeError(
            "Ligne {} ({}) : curl a échoué : {}".format(
                line_number, ip, result.stderr.decode("utf-8", errors="replace").strip()
            )
        )

    body, _, status = result.stdout.decode("utf-8", errors="replace").rpartition("\n")
    if not status.isdigit() or not 200 <= int(status) < 300:
        raise RuntimeError(
            "Ligne {} ({}) : HTTP {}\n{}".format(line_number, ip, status, body)
        )


def main():
    parser = argparse.ArgumentParser(
        description="Remplace ${IP} dans un message et envoie un POST /raw-events par entrée CSV."
    )
    parser.add_argument("csv_file", type=Path, help="Chemin du CSV, avec ou sans en-tête ip,hostname")
    args = parser.parse_args()

    if shutil.which("curl") is None:
        raise RuntimeError("La commande curl doit être installée sur le poste.")

    template = ORIGINAL_MESSAGE
    if IP_TOKEN not in template:
        raise ValueError("ORIGINAL_MESSAGE doit contenir le token {}.".format(IP_TOKEN))
    json.loads(template)

    sent = 0
    first_entry = True
    with args.csv_file.open("r", encoding="utf-8-sig", newline="") as csv_file:
        reader = csv.reader(csv_file, strict=True)
        for row in reader:
            if not row or all(not value.strip() for value in row):
                continue
            ip = row[0].strip()
            if first_entry and ip.lower() == "ip":
                first_entry = False
                continue
            first_entry = False
            if not ip:
                raise ValueError("Ligne {} : IP manquante.".format(reader.line_num))
            message = template.replace(IP_TOKEN, ip)
            publish_message(message, ip, reader.line_num)
            sent += 1
            print("[{}] {} envoyé".format(sent, ip), flush=True)

    print("{} messages envoyés sur le topic {}.".format(sent, TOPIC))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError, csv.Error) as error:
        print("Erreur : {}".format(error), file=sys.stderr)
        sys.exit(1)
