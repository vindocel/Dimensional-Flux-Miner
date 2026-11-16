#!/usr/bin/env bash
# Release checks shared by ci.yml and release.yml.
#
#   validate-release.sh versions    lists the Minecraft versions Stonecutter builds (one per line)
#   validate-release.sh tag         resolves the release from TAG (+ PRERELEASE, TARGET)
#   validate-release.sh artifact    checks the built JAR against VERSION and MINECRAFT
#
# Every Minecraft version lives on the same branch; Stonecutter builds each one as the Gradle project
# :<minecraft> (versions/<minecraft>). No suffix selects all configured versions; +<minecraft> selects one,
# and +<minecraft>-<minecraft> selects a group, e.g. v0.3.0-beta.1+1.21.1-26.1.2. gradle.properties holds only
# the base version (mod_version=0.3.0); the channel comes from the tag, so alphas and betas need no version commit.
# A mod_version that already carries a channel (0.3.0-beta.2) must match the tag exactly.
set -euo pipefail

die() { echo "::error::$*" >&2; exit 1; }
property() { sed -n "s/^$1=//p" "${2:-gradle.properties}" | head -n 1 | tr -d '\r'; }
output() { if [[ -n "${GITHUB_OUTPUT:-}" ]]; then echo "$1=$2" >> "$GITHUB_OUTPUT"; fi; }

NUM='(0|[1-9][0-9]*)'
BASE_RE="^$NUM\.$NUM\.$NUM$"
CHANNEL_RE="^$NUM\.$NUM\.$NUM-(alpha|beta)\.[1-9][0-9]*$"
MC_RE='[0-9]+\.[0-9]+(\.[0-9]+)?'

[[ -f gradle.properties && -f settings.gradle.kts ]] || die "Run from the repository root"
mod_id="$(property mod_id)"
mod_version="$(property mod_version)"
[[ "$mod_version" =~ $BASE_RE || "$mod_version" =~ $CHANNEL_RE ]] \
  || die "Invalid mod_version $mod_version (expected X.Y.Z, or X.Y.Z-alpha.N / X.Y.Z-beta.N)"

# The versions(...) list of settings.gradle.kts, each with its versions/<minecraft> folder.
configured_versions() {
  local list
  list="$(tr -d '\r' < settings.gradle.kts | sed -n 's/.*versions(\(.*\)).*/\1/p' | head -n 1)"
  [[ -n "$list" ]] || die "No versions(...) list in settings.gradle.kts"
  grep -oE '"[^"]+"' <<< "$list" | tr -d '"'
}

mapfile -t versions < <(configured_versions)
[[ "${#versions[@]}" -gt 0 ]] || die "No Minecraft version configured"
for v in "${versions[@]}"; do
  [[ "$v" =~ ^$MC_RE$ ]] || die "Invalid Minecraft version in settings.gradle.kts: $v"
  props="versions/$v/gradle.properties"
  [[ -f "$props" ]] || die "$props is missing for Minecraft $v"
  [[ "$(property minecraft_version_range "$props")" == "[$v]" ]] \
    || die "minecraft_version_range in $props must be [$v] for single-version publishing"
  [[ "$(property java_version "$props")" =~ ^[0-9]+$ ]] || die "java_version missing in $props"
done

mode="${1:-}"
case "$mode" in
  versions)
    printf '%s\n' "${versions[@]}"
    # JSON list for a GitHub Actions matrix
    json="[$(printf '"%s",' "${versions[@]}" | sed 's/,$//')]"
    output versions "$json"
    ;;

  tag)
    [[ -n "${TAG:-}" ]] || die "TAG is empty"
    release_tag="${TAG%%+*}"
    [[ "$release_tag" =~ ^v($NUM\.$NUM\.$NUM)(-(alpha|beta)\.([1-9][0-9]*))?$ ]] \
      || die "Invalid tag $TAG (expected v0.3.0[-alpha.N|-beta.N][+minecraft-minecraft])"
    tag_base="${BASH_REMATCH[1]}"
    tag_channel="${BASH_REMATCH[6]}"
    tag_number="${BASH_REMATCH[7]}"
    selected=("${versions[@]}")
    if [[ "$TAG" == *+* ]]; then
      selection="${TAG#*+}"
      [[ "$selection" =~ ^$MC_RE(-$MC_RE)*$ ]] \
        || die "Invalid Minecraft selection in $TAG (expected +minecraft or +minecraft-minecraft)"
      IFS='-' read -r -a selected <<< "$selection"
    fi
    declare -A seen=()
    for minecraft in "${selected[@]}"; do
      [[ -z "${seen[$minecraft]:-}" ]] || die "Minecraft $minecraft is repeated in $TAG"
      seen[$minecraft]=true
      found=false
      for v in "${versions[@]}"; do
        [[ "$v" == "$minecraft" ]] && found=true
      done
      [[ "$found" == true ]] \
        || die "Tag $TAG selects Minecraft $minecraft, which Stonecutter does not build (${versions[*]})"
    done

    version="$tag_base"
    if [[ -n "$tag_channel" ]]; then
      version="$tag_base-$tag_channel.$tag_number"
    fi
    if [[ "$mod_version" =~ $BASE_RE ]]; then
      [[ "$tag_base" == "$mod_version" ]] || die "Tag $TAG is not a release of mod_version $mod_version"
    else
      [[ "$version" == "$mod_version" ]] || die "Tag $TAG does not match mod_version $mod_version"
    fi

    release_type=release
    if [[ -n "$tag_channel" ]]; then
      release_type="$tag_channel"
    fi
    if [[ "$release_type" == release && "${PRERELEASE:-false}" != false ]]; then
      die "Stable tag $TAG must not be marked Pre-release on GitHub"
    fi
    if [[ "$release_type" != release && "${PRERELEASE:-false}" != true ]]; then
      die "Alpha/Beta tag $TAG must be marked Pre-release on GitHub"
    fi

    # Alphas and betas come from production; stable releases from main (which receives production when a
    # version is final). The tagged commit has to be on the selected target branch.
    target="${TARGET:-}"
    if [[ -n "$target" && ! "$target" =~ ^[0-9a-f]{40}$ ]]; then
      if [[ "$release_type" == release ]]; then
        [[ "$target" == main ]] || die "Stable tag $TAG must target main (got $target)"
      else
        [[ "$target" == production || "$target" == main ]] \
          || die "Alpha/Beta tag $TAG must target production (got $target)"
      fi
      git show-ref --verify --quiet "refs/remotes/origin/$target" || die "Release target branch $target not found"
      git merge-base --is-ancestor HEAD "refs/remotes/origin/$target" \
        || die "Tag $TAG points to a commit that is not on the release target branch $target"
    fi

    output version "$version"
    output release_type "$release_type"
    matrix='{"include":['
    separator=''
    for minecraft in "${selected[@]}"; do
      java_version="$(property java_version "versions/$minecraft/gradle.properties")"
      matrix+="$separator{\"minecraft\":\"$minecraft\",\"java_version\":\"$java_version\",\"publish_version\":\"v$version+$minecraft\"}"
      separator=','
    done
    matrix+=']}'
    output matrix "$matrix"
    # Retain the single-version outputs for existing callers.
    if [[ "${#selected[@]}" -eq 1 ]]; then
      output minecraft_version "${selected[0]}"
      output java_version "$(property java_version "versions/${selected[0]}/gradle.properties")"
    fi
    echo "Release $TAG: version $version, $release_type, Minecraft ${selected[*]}"
    ;;

  artifact)
    [[ -n "${VERSION:-}" ]] || die "VERSION is empty"
    [[ -n "${MINECRAFT:-}" ]] || die "MINECRAFT is empty"
    libs="versions/$MINECRAFT/build/libs"
    [[ -d "$libs" ]] || die "$libs not found"
    mapfile -t jars < <(find "$libs" -maxdepth 1 -type f -name '*.jar' \
      ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*-dev.jar' | sort)
    [[ "${#jars[@]}" -eq 1 ]] || die "Expected exactly one distribution JAR in $libs; got ${#jars[@]}"
    jar="${jars[0]}"
    [[ "$(basename "$jar")" == "$mod_id-$MINECRAFT-$VERSION.jar" ]] \
      || die "JAR $(basename "$jar") is not named for Minecraft $MINECRAFT and version $VERSION"
    metadata="$(unzip -p "$jar" META-INF/neoforge.mods.toml)" || die "NeoForge metadata missing from JAR"
    grep -Fq "version=\"$VERSION\"" <<< "$metadata" || die "Mod version metadata is not $VERSION"
    grep -Fq "versionRange=\"[$MINECRAFT]\"" <<< "$metadata" || die "Minecraft metadata range is not [$MINECRAFT]"
    output jar "$jar"
    echo "Artifact $jar: version $VERSION, Minecraft $MINECRAFT"
    ;;

  *)
    die "Usage: validate-release.sh versions | tag | artifact"
    ;;
esac
