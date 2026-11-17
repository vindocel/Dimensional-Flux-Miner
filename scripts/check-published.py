"""Read store APIs before uploading; never publish when lookup fails."""

import json
import os
from pathlib import Path
import re
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode
from urllib.request import Request, urlopen


def fetch_json(url, headers):
    request = Request(url, headers={"User-Agent": "DimensionalFluxMiner/release-check", **headers})
    try:
        with urlopen(request, timeout=30) as response:
            return json.load(response)
    except HTTPError as error:
        raise RuntimeError(f"Store lookup failed (HTTP {error.code}); upload stopped") from None
    except (URLError, TimeoutError, ValueError):
        raise RuntimeError("Store lookup failed; upload stopped") from None


def matches_name(value, publish_version):
    return isinstance(value, str) and value.removeprefix("v") == publish_version.removeprefix("v")


def curseforge_exists(project, minecraft, publish_version, filename):
    result = fetch_json(f"https://api.cfwidget.com/{quote(project, safe='')}", {})
    if not isinstance(result, dict) or result.get("id") != int(project):
        raise ValueError("Invalid CFWidget project")
    files = result.get("files")
    if not isinstance(files, list):
        raise ValueError("Invalid CFWidget file list")
    # Validate the whole response before allowing either store to upload.
    for file in files:
        if (not isinstance(file, dict) or not isinstance(file.get("versions"), list)
                or not all(isinstance(value, str) for value in file["versions"])
                or not isinstance(file.get("display"), str)
                or not isinstance(file.get("name"), str)):
            raise ValueError("Invalid CFWidget file")
    return any(minecraft in file["versions"] and "NeoForge" in file["versions"] and (
        matches_name(file["display"], publish_version) or file["name"] == filename)
        for file in files)


def modrinth_exists(project, token, minecraft, publish_version, filename):
    query = urlencode({"loaders": json.dumps(["neoforge"]), "game_versions": json.dumps([minecraft]),
                       "include_changelog": "false"})
    versions = fetch_json(f"https://api.modrinth.com/v2/project/{quote(project, safe='')}/version?{query}",
                          {"Authorization": token})
    if not isinstance(versions, list):
        raise ValueError("Invalid Modrinth version list")
    for version in versions:
        if minecraft in version["game_versions"] and "neoforge" in version["loaders"] and (
                matches_name(version["version_number"], publish_version) or
                any(file["filename"] == filename for file in version["files"])):
            return True
    return False


def publication_plan(curseforge_id, modrinth_id, token, minecraft, version, mod_id):
    if not all((curseforge_id, modrinth_id, token, minecraft, version, mod_id)):
        raise ValueError("Missing store lookup configuration")
    if not curseforge_id.isdecimal() or not re.fullmatch(r"[A-Za-z0-9_-]+", modrinth_id):
        raise ValueError("Invalid store project ID")
    publish_version = f"v{version}+{minecraft}"
    filename = f"{mod_id}-{minecraft}-{version}.jar"
    # Finish both reads before emitting any outputs.
    curseforge = curseforge_exists(curseforge_id, minecraft, publish_version, filename)
    modrinth = modrinth_exists(modrinth_id, token, minecraft, publish_version, filename)
    return {"curseforge": not curseforge, "modrinth": not modrinth}


def main():
    properties = dict(line.split("=", 1) for line in Path("gradle.properties").read_text().splitlines()
                      if "=" in line and not line.startswith("#"))
    plan = publication_plan(
        os.environ.get("CURSEFORGE_ID", ""),
        *(os.environ.get(key, "") for key in ("MODRINTH_ID", "MODRINTH_TOKEN", "MINECRAFT", "VERSION")),
        properties["mod_id"])
    output = "".join(f"{store}={str(publish).lower()}\n" for store, publish in plan.items())
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as file:
        file.write(output)
    for store, publish in plan.items():
        print(f"{store}: {'missing; publish' if publish else 'already exists; skip'}")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, ValueError, KeyError, OSError) as error:
        # Do not echo HTTP response bodies or authentication headers.
        reason = str(error) if isinstance(error, RuntimeError) else "Invalid store response or configuration."
        print(f"::error::Could not verify existing store versions. {reason} "
              "The temporary CurseForge lookup uses the public CFWidget API without credentials. "
              "No upload was started.", file=sys.stderr)
        sys.exit(1)
