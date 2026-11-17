"""Exercise release selection and artifact checks without Git or store access."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import zipfile


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/validate-release.sh"
BASH = shutil.which("bash") or "C:/Program Files/Git/bin/bash.exe"
VERSIONS = ["1.21.1", "26.1.2", "26.3"]


class ReleaseChecks(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="dfm-release-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.write("gradle.properties", "mod_id=dimensionalfluxminer\nmod_version=0.2.8\n")
        self.write("settings.gradle.kts", 'versions("1.21.1", "26.1.2", "26.3")\n')
        for version in VERSIONS:
            java = "21" if version == "1.21.1" else "25"
            self.write(f"versions/{version}/gradle.properties",
                       f"minecraft_version_range=[{version}]\njava_version={java}\n")

    def write(self, name, text):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def run_check(self, mode="tag", **values):
        env = os.environ.copy()
        for key in ("TAG", "PRERELEASE", "TARGET", "VERSION", "MINECRAFT", "GITHUB_OUTPUT"):
            env.pop(key, None)
        output = self.root / "outputs.txt"
        output.write_text("", encoding="utf-8")
        env.update(values, GITHUB_OUTPUT=output.as_posix())
        result = subprocess.run([BASH, SCRIPT.as_posix(), mode], cwd=self.root,
                                env=env, text=True, capture_output=True)
        outputs = dict(line.split("=", 1) for line in output.read_text().splitlines())
        return result, outputs

    def assert_selection(self, tag, expected, version="0.2.8", channel="release"):
        result, outputs = self.run_check(TAG=tag, PRERELEASE=str(channel != "release").lower())
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(outputs["version"], version)
        self.assertEqual(outputs["release_type"], channel)
        entries = json.loads(outputs["matrix"])["include"]
        self.assertEqual(entries, [
            {"minecraft": mc, "java_version": "21" if mc == "1.21.1" else "25",
             "publish_version": f"v{version}+{mc}"} for mc in expected
        ])
        if len(expected) == 1:
            self.assertEqual(outputs["minecraft_version"], expected[0])
        else:
            self.assertNotIn("minecraft_version", outputs)

    def test_all_stable(self):
        self.assert_selection("v0.2.8", VERSIONS)

    def test_all_beta(self):
        self.assert_selection("v0.2.8-beta.1", VERSIONS, "0.2.8-beta.1", "beta")

    def test_all_alpha(self):
        self.assert_selection("v0.2.8-alpha.2", VERSIONS, "0.2.8-alpha.2", "alpha")

    def test_single_version(self):
        for mc in VERSIONS:
            with self.subTest(mc=mc):
                self.assert_selection(f"v0.2.8+{mc}", [mc])

    def test_selected_group(self):
        self.assert_selection("v0.2.8+1.21.1-26.1.2", VERSIONS[:2])

    def test_beta_group_and_order(self):
        self.assert_selection("v0.2.8-beta.3+26.3-1.21.1", ["26.3", "1.21.1"],
                              "0.2.8-beta.3", "beta")

    def test_explicit_all(self):
        self.assert_selection("v0.2.8+1.21.1-26.1.2-26.3", VERSIONS)

    def test_future_target_is_included_automatically(self):
        self.write("settings.gradle.kts", 'versions("1.21.1", "26.1.2", "26.3", "27.1")\n')
        self.write("versions/27.1/gradle.properties", "minecraft_version_range=[27.1]\njava_version=25\n")
        self.assert_selection("v0.2.8", VERSIONS + ["27.1"])

    def test_invalid_tags(self):
        for tag in ("v0.2.8+", "v0.2.8+26.3-", "v0.2.8+-26.3", "v0.2.8+26.3--1.21.1",
                    "v0.2.8+26.3+1.21.1", "v0.2.8+26.3-26.3", "v0.2.8+1.21.1-9.9.9",
                    "v0.2.8+26.3 other", "v0.2.8-beta.0", "v0.2.8-rc.1", "v00.2.8",
                    "0.2.8", "v0.2.9", "v0.2.8+26.3;echo", "v0.2.8+26.3-$(echo)"):
            with self.subTest(tag=tag):
                result, outputs = self.run_check(TAG=tag, PRERELEASE="false")
                self.assertNotEqual(result.returncode, 0, tag)
                self.assertNotIn("matrix", outputs)

    def test_prerelease_flag(self):
        for tag, flag in (("v0.2.8", "true"), ("v0.2.8-beta.1", "false"),
                          ("v0.2.8-alpha.1+1.21.1-26.3", "false")):
            result, _ = self.run_check(TAG=tag, PRERELEASE=flag)
            self.assertNotEqual(result.returncode, 0)

    def test_pinned_channel_must_match(self):
        self.write("gradle.properties", "mod_id=dimensionalfluxminer\nmod_version=0.2.8-beta.1\n")
        self.assert_selection("v0.2.8-beta.1", VERSIONS, "0.2.8-beta.1", "beta")
        result, _ = self.run_check(TAG="v0.2.8-beta.2", PRERELEASE="true")
        self.assertNotEqual(result.returncode, 0)

    def test_wrong_target_branch(self):
        result, _ = self.run_check(TAG="v0.2.8", PRERELEASE="false", TARGET="production")
        self.assertNotEqual(result.returncode, 0)

    def test_ci_version_list_is_unchanged(self):
        result, outputs = self.run_check("versions")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(outputs["versions"]), VERSIONS)

    def jar(self, version, mc):
        path = self.root / f"versions/{mc}/build/libs/dimensionalfluxminer-{mc}-{version}.jar"
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("META-INF/neoforge.mods.toml",
                             f'version="{version}"\nversionRange="[{mc}]"\n')
        return path

    def test_beta_artifact(self):
        self.jar("0.2.8-beta.1", "26.3")
        result, _ = self.run_check("artifact", VERSION="0.2.8-beta.1", MINECRAFT="26.3")
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_wrong_artifact_version_and_extra_jar(self):
        self.jar("0.2.8", "1.21.1")
        result, _ = self.run_check("artifact", VERSION="0.2.8-beta.1", MINECRAFT="1.21.1")
        self.assertNotEqual(result.returncode, 0)
        self.jar("0.2.8-beta.1", "1.21.1")
        result, _ = self.run_check("artifact", VERSION="0.2.8", MINECRAFT="1.21.1")
        self.assertNotEqual(result.returncode, 0)


if __name__ == "__main__":
    unittest.main()
