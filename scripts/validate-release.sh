#!/usr/bin/env bash
set -euo pipefail

die() { echo "::error::$*" >&2; exit 1; }
property() { sed -n "s/^$1=//p" gradle.properties | head -n 1 | tr -d '\r'; }

[[ -f gradle.properties ]] || die "gradle.properties not found"
[[ -n "${TAG:-}" ]] || die "TAG is empty"
[[ "$TAG" =~ ^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-(alpha|beta)\.([1-9][0-9]*))?$ ]] || die "Invalid tag: $TAG"

mod_version="$(property mod_version)"
minecraft_version="$(property minecraft_version)"
minecraft_range="$(property minecraft_version_range)"
neo_version="$(property neo_version)"
[[ "${TAG#v}" == "$mod_version" ]] || die "Tag $TAG does not match mod_version $mod_version"
[[ "$minecraft_version" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]] || die "Invalid Minecraft version: $minecraft_version"
[[ "$minecraft_range" == "[$minecraft_version]" ]] || die "Minecraft range $minecraft_range must equal [$minecraft_version] for single-version publishing"
[[ -n "$neo_version" ]] || die "neo_version missing"

release_type=release
if [[ "$mod_version" =~ -alpha\.[1-9][0-9]*$ ]]; then
  release_type=alpha
elif [[ "$mod_version" =~ -beta\.[1-9][0-9]*$ ]]; then
  release_type=beta
fi

if [[ "$release_type" == release && "${PRERELEASE:-false}" != false ]]; then
  die "Stable tag must not be marked prerelease in GitHub"
fi
if [[ "$release_type" != release && "${PRERELEASE:-false}" != true ]]; then
  die "Alpha/Beta tags must be marked prerelease in GitHub"
fi

mapfile -t jars < <(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*-dev.jar' | sort)
[[ "${#jars[@]}" -eq 1 ]] || die "Expected exactly one distribution JAR; got ${#jars[@]}"
jar="${jars[0]}"
metadata="$(unzip -p "$jar" META-INF/neoforge.mods.toml)" || die "NeoForge metadata missing from JAR"
grep -Fq "version=\"$mod_version\"" <<< "$metadata" || die "Mod version metadata mismatch"
grep -Fq "$minecraft_range" <<< "$metadata" || die "Minecraft metadata range mismatch"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "release_type=$release_type"
    echo "minecraft_version=$minecraft_version"
    echo "mod_version=$mod_version"
  } >> "$GITHUB_OUTPUT"
fi
echo "Validated $TAG as $release_type for Minecraft $minecraft_version ($jar)"
