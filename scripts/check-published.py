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


def curseforge_exists(project, key, minecraft, publish_version, filename):
    index = 0
    while True:
        query = urlencode({"gameVersion": minecraft, "modLoaderType": 6, "pageSize": 50, "index": index})
        result = fetch_json(f"https://api.curseforge.com/v1/mods/{quote(project, safe='')}/files?{query}",
                            {"x-api-key": key})
        files = result["data"]
        pagination = result["pagination"]
        if not isinstance(files, list):
            raise ValueError("Invalid CurseForge file list")
        for file in files:
            if minecraft in file["gameVersions"] and (
                    matches_name(file["displayName"], publish_version) or file["fileName"] == filename):
                return True
        count = int(pagination["resultCount"])
        total = int(pagination["totalCount"])
        if count != len(files) or count < 0 or total < 0:
            raise ValueError("Invalid CurseForge pagination")
        index += count
        if index >= total:
            return False
        if count == 0:
            raise ValueError("Incomplete CurseForge file list")


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


def publication_plan(curseforge_id, api_key, modrinth_id, token, minecraft, version, mod_id):
    if not all((curseforge_id, api_key, modrinth_id, token, minecraft, version, mod_id)):
        raise ValueError("Missing store lookup configuration")
    if not curseforge_id.isdecimal() or not re.fullmatch(r"[A-Za-z0-9_-]+", modrinth_id):
        raise ValueError("Invalid store project ID")
    publish_version = f"v{version}+{minecraft}"
    filename = f"{mod_id}-{minecraft}-{version}.jar"
    # Finish both reads before emitting any outputs.
    curseforge = curseforge_exists(curseforge_id, api_key, minecraft, publish_version, filename)
    modrinth = modrinth_exists(modrinth_id, token, minecraft, publish_version, filename)
    return {"curseforge": not curseforge, "modrinth": not modrinth}


def main():
    properties = dict(line.split("=", 1) for line in Path("gradle.properties").read_text().splitlines()
                      if "=" in line and not line.startswith("#"))
    plan = publication_plan(
        os.environ.get("CURSEFORGE_ID", ""),
        os.environ.get("CURSEFORGE_API_KEY") or os.environ.get("CURSEFORGE_TOKEN", ""),
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
              "The CurseForge lookup uses CURSEFORGE_TOKEN unless CURSEFORGE_API_KEY is set. "
              "If the Core API rejects that credential, provide its read key as CURSEFORGE_API_KEY. "
              "No upload was started.", file=sys.stderr)
        sys.exit(1)
