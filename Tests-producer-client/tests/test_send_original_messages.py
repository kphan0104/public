import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import send_original_messages as client


class FakeHeaders:
    def get_content_charset(self):
        return "utf-8"


class FakeResponse:
    status = 201
    reason = "Created"
    headers = FakeHeaders()

    def read(self):
        return b'{"status":"published"}'


class FakeConnection:
    def __init__(self):
        self.sent_request = None
        self.closed = False

    def request(self, method, path, body, headers):
        self.sent_request = {
            "method": method,
            "path": path,
            "body": body,
            "content_type": headers["Content-Type"],
        }

    def getresponse(self):
        return FakeResponse()

    def close(self):
        self.closed = True


class ClientTest(unittest.TestCase):
    def setUp(self):
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary_directory.name)
        self.flow = self.root / "payments"
        self.messages = self.flow / "originalMessages"
        self.messages.mkdir(parents=True)

    def tearDown(self):
        self.temporary_directory.cleanup()

    def write_configuration(self, name, topic="integration.events"):
        path = self.flow / name
        path.write_text(
            'input {\n  kafka {\n    topics => '
            '"${KAFKA_TOPIC:' + topic + '}"\n  }\n}\n',
            encoding="utf-8",
        )
        return path

    def test_selects_latest_numeric_configuration_version(self):
        self.write_configuration("payments-v.1.9.conf", "old.events")
        expected = self.write_configuration(
            "payments-v.1.10.conf",
            "latest.events",
        )

        configuration, version = client.find_latest_configuration(self.flow)

        self.assertEqual(expected, configuration)
        self.assertEqual("1.10", version)
        self.assertEqual(
            "latest.events",
            client.extract_default_topic(configuration),
        )

    def test_uses_default_topic_even_if_environment_variable_exists(self):
        configuration = self.write_configuration(
            "payments-v.2.0.conf",
            "default.events",
        )
        previous_value = os.environ.get("KAFKA_TOPIC")
        os.environ["KAFKA_TOPIC"] = "environment.events"
        try:
            topic = client.extract_default_topic(configuration)
        finally:
            if previous_value is None:
                os.environ.pop("KAFKA_TOPIC", None)
            else:
                os.environ["KAFKA_TOPIC"] = previous_value

        self.assertEqual("default.events", topic)

    def test_raises_when_topic_is_missing(self):
        configuration = self.flow / "payments-v.1.0.conf"
        configuration.write_text("input { kafka {} }", encoding="utf-8")

        with self.assertRaises(client.TopicNotFoundError):
            client.extract_default_topic(configuration)

    def test_finds_only_supported_original_message_files(self):
        (self.messages / "first").write_text('{"id": 1}', encoding="utf-8")
        (self.messages / "second.msg").write_text(
            '{"id": 2}',
            encoding="utf-8",
        )
        (self.messages / "ignored.json").write_text(
            '{"id": 3}',
            encoding="utf-8",
        )
        (self.messages / ".hidden").write_text('{"id": 4}', encoding="utf-8")

        messages = client.find_original_messages(self.flow)

        self.assertEqual(["first", "second.msg"], [item.name for item in messages])

    def test_rejects_invalid_json_before_sending(self):
        message = self.messages / "invalid.msg"
        message.write_text("{invalid}", encoding="utf-8")

        with self.assertRaises(client.OriginalMessageError):
            client.validate_json_file(message)

    def test_sends_multipart_request(self):
        message = self.messages / "message.msg"
        message.write_text('{"id": 42}', encoding="utf-8")
        connection = FakeConnection()

        with patch.object(
            client,
            "HTTPConnection",
            return_value=connection,
        ) as connection_factory:
            result = client.send_message(
                "http://server:3000/api/v1/events",
                "integration.events",
                "payments",
                message,
            )

        self.assertEqual({"status": "published"}, result)
        connection_factory.assert_called_once_with("server", 3000)
        self.assertEqual("POST", connection.sent_request["method"])
        self.assertEqual("/api/v1/events", connection.sent_request["path"])
        self.assertTrue(connection.closed)
        self.assertTrue(
            connection.sent_request["content_type"].startswith(
                "multipart/form-data; boundary="
            )
        )
        body = connection.sent_request["body"]
        self.assertIn(b'name="topic"\r\n\r\nintegration.events', body)
        self.assertIn(b'name="flowName"\r\n\r\npayments', body)
        self.assertIn(b'name="originalMessage"', body)
        self.assertIn(b'filename="originalMessage.json"', body)
        self.assertIn(b"Content-Type: application/json", body)
        self.assertIn(b'{"id": 42}', body)


if __name__ == "__main__":
    unittest.main()
