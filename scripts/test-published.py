"""Store lookup regression tests use mocked responses and never access accounts."""

import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from urllib.error import HTTPError

SPEC = importlib.util.spec_from_file_location("published", Path(__file__).with_name("check-published.py"))
published = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(published)
MC = "1.21.1"
VERSION = "0.2.8"
NAME = "v0.2.8+1.21.1"
FILE = "dimensionalfluxminer-1.21.1-0.2.8.jar"


def cf_page(files, total=None):
    return {"data": files, "pagination": {"resultCount": len(files),
                                          "totalCount": len(files) if total is None else total}}


def cf_file(name=NAME, filename=FILE, mc=MC):
    return {"displayName": name, "fileName": filename, "gameVersions": [mc, "NeoForge"]}


def mr_version(name=NAME, filename=FILE, mc=MC, loader="neoforge"):
    return {"version_number": name, "game_versions": [mc], "loaders": [loader],
            "files": [{"filename": filename}]}


class PublishedChecks(unittest.TestCase):
    def plan(self):
        return published.publication_plan("123", "read-key", "project", "token", MC, VERSION,
                                          "dimensionalfluxminer")

    @patch.object(published, "fetch_json")
    def test_both_missing(self, fetch):
        fetch.side_effect = [cf_page([]), []]
        self.assertEqual(self.plan(), {"curseforge": True, "modrinth": True})

    @patch.object(published, "fetch_json")
    def test_both_exist(self, fetch):
        fetch.side_effect = [cf_page([cf_file()]), [mr_version()]]
        self.assertEqual(self.plan(), {"curseforge": False, "modrinth": False})

    @patch.object(published, "fetch_json")
    def test_only_modrinth_missing(self, fetch):
        fetch.side_effect = [cf_page([cf_file()]), []]
        self.assertEqual(self.plan(), {"curseforge": False, "modrinth": True})

    @patch.object(published, "fetch_json")
    def test_only_curseforge_missing(self, fetch):
        fetch.side_effect = [cf_page([]), [mr_version()]]
        self.assertEqual(self.plan(), {"curseforge": True, "modrinth": False})

    @patch.object(published, "fetch_json")
    def test_legacy_display_name_uses_filename(self, fetch):
        fetch.side_effect = [cf_page([cf_file(name="Dimensional Flux Miner 0.2.8")]),
                             [mr_version(name="older-title")]]
        self.assertEqual(self.plan(), {"curseforge": False, "modrinth": False})

    @patch.object(published, "fetch_json")
    def test_names_without_v(self, fetch):
        fetch.side_effect = [cf_page([cf_file(name=NAME[1:], filename="other.jar")]),
                             [mr_version(name=NAME[1:], filename="other.jar")]]
        self.assertEqual(self.plan(), {"curseforge": False, "modrinth": False})

    @patch.object(published, "fetch_json")
    def test_other_minecraft_or_loader_does_not_match(self, fetch):
        fetch.side_effect = [cf_page([cf_file(mc="26.3")]), [mr_version(loader="fabric")]]
        self.assertEqual(self.plan(), {"curseforge": True, "modrinth": True})

    @patch.object(published, "fetch_json")
    def test_other_channel_does_not_match(self, fetch):
        fetch.side_effect = [cf_page([cf_file(name="v0.2.8-beta.1+1.21.1", filename="beta.jar")]),
                             [mr_version(name="v0.2.8-beta.1+1.21.1", filename="beta.jar")]]
        self.assertEqual(self.plan(), {"curseforge": True, "modrinth": True})

    @patch.object(published, "fetch_json")
    def test_curseforge_pagination(self, fetch):
        fetch.side_effect = [cf_page([cf_file(name="old", filename="old.jar")], total=2),
                             cf_page([cf_file()], total=2), []]
        self.assertEqual(self.plan(), {"curseforge": False, "modrinth": True})
        self.assertIn("index=1", fetch.call_args_list[1].args[0])
        self.assertEqual(fetch.call_args_list[0].args[1], {"x-api-key": "read-key"})
        self.assertIn("gameVersion=1.21.1", fetch.call_args_list[0].args[0])
        self.assertIn("modLoaderType=6", fetch.call_args_list[0].args[0])

    @patch.object(published, "fetch_json")
    def test_lookup_failure_does_not_produce_plan(self, fetch):
        for responses in ([RuntimeError("unavailable")], [cf_page([]), RuntimeError("unavailable")]):
            fetch.side_effect = responses
            with self.assertRaises(RuntimeError):
                self.plan()

    @patch.object(published, "fetch_json")
    def test_truncated_page_stops_upload(self, fetch):
        fetch.return_value = cf_page([], total=2)
        with self.assertRaises(ValueError):
            self.plan()

    @patch.object(published, "fetch_json")
    def test_missing_key_never_queries(self, fetch):
        with self.assertRaises(ValueError):
            published.publication_plan("123", "", "project", "token", MC, VERSION, "dimensionalfluxminer")
        fetch.assert_not_called()

    @patch.object(published, "urlopen")
    def test_http_error_does_not_expose_token_or_body(self, request):
        request.side_effect = HTTPError("https://example.test", 401, "private response", {}, None)
        with self.assertRaisesRegex(RuntimeError, "HTTP 401") as error:
            published.fetch_json("https://example.test", {"Authorization": "private-token"})
        self.assertNotIn("private", str(error.exception))

    @patch.object(published, "publication_plan")
    @patch("builtins.print")
    def test_existing_token_is_reused_by_default(self, printing, plan):
        plan.return_value = {"curseforge": False, "modrinth": True}
        with tempfile.TemporaryDirectory(prefix="dfm-store-check-") as folder:
            output = Path(folder) / "outputs.txt"
            with patch.dict(os.environ, {"CURSEFORGE_TOKEN": "existing-upload-key",
                                         "GITHUB_OUTPUT": str(output)}, clear=True):
                published.main()
            self.assertEqual(plan.call_args.args[1], "existing-upload-key")
            self.assertEqual(output.read_text(), "curseforge=false\nmodrinth=true\n")

    @patch.object(published, "publication_plan")
    @patch("builtins.print")
    def test_optional_read_key_takes_precedence(self, printing, plan):
        plan.return_value = {"curseforge": False, "modrinth": False}
        with tempfile.TemporaryDirectory(prefix="dfm-store-check-") as folder:
            with patch.dict(os.environ, {"CURSEFORGE_TOKEN": "upload-key",
                                         "CURSEFORGE_API_KEY": "read-key",
                                         "GITHUB_OUTPUT": str(Path(folder) / "outputs.txt")}, clear=True):
                published.main()
            self.assertEqual(plan.call_args.args[1], "read-key")


if __name__ == "__main__":
    unittest.main()
