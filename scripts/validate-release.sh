#!/usr/bin/env bash
# Release and branch checks shared by ci.yml and release.yml.
#
#   validate-release.sh branch <branch>   mc/<version> branches must build that Minecraft version
#   validate-release.sh tag               resolves the release from TAG (+ PRERELEASE, TARGET)
#   validate-release.sh artifact          checks the built JAR against VERSION
#
# Tags: v<X.Y.Z>[-alpha.N|-beta.N][+<minecraft>], e.g. v0.3.0-beta.1+1.21.1. gradle.properties holds only the
# base version (mod_version=0.3.0); the channel comes from the tag, so alphas and betas need no version commit.
# A mod_version that already carries a channel (0.2.0-beta.2) must match the tag exactly.
set -euo pipefail

die() { echo "::error::$*" >&2; exit 1; }
property() { sed -n "s/^$1=//p" gradle.properties | head -n 1 | tr -d '\r'; }
output() { if [[ -n "${GITHUB_OUTPUT:-}" ]]; then echo "$1=$2" >> "$GITHUB_OUTPUT"; fi; }

NUM='(0|[1-9][0-9]*)'
BASE_RE="^$NUM\.$NUM\.$NUM$"
CHANNEL_RE="^$NUM\.$NUM\.$NUM-(alpha|beta)\.[1-9][0-9]*$"
MC_RE='[0-9]+\.[0-9]+(\.[0-9]+)?'

[[ -f gradle.properties ]] || die "gradle.properties not found"
mod_version="$(property mod_version)"
minecraft_version="$(property minecraft_version)"
minecraft_range="$(property minecraft_version_range)"
[[ "$minecraft_version" =~ ^$MC_RE$ ]] || die "Invalid minecraft_version: $minecraft_version"
[[ "$mod_version" =~ $BASE_RE || "$mod_version" =~ $CHANNEL_RE ]] \
  || die "Invalid mod_version $mod_version (expected X.Y.Z, or X.Y.Z-alpha.N / X.Y.Z-beta.N)"

# A branch named mc/<version> always builds exactly that Minecraft version.
check_branch() {
  local branch="$1"
  if [[ "$branch" =~ ^mc/(.+)$ ]]; then
    [[ "${BASH_REMATCH[1]}" == "$minecraft_version" ]] \
      || die "Branch $branch builds Minecraft $minecraft_version (gradle.properties); expected ${BASH_REMATCH[1]}"
  fi
}

mode="${1:-}"
case "$mode" in
  branch)
    branch="${2:-}"
    [[ -n "$branch" ]] || die "Branch name missing"
    check_branch "$branch"
    echo "Branch $branch builds Minecraft $minecraft_version (mod_version $mod_version)"
    ;;

  tag)
    [[ -n "${TAG:-}" ]] || die "TAG is empty"
    [[ "$TAG" =~ ^v($NUM\.$NUM\.$NUM)(-(alpha|beta)\.([1-9][0-9]*))?(\+($MC_RE))?$ ]] \
      || die "Invalid tag $TAG (expected v0.3.0, v0.3.0-beta.1 or v0.3.0-beta.1+$minecraft_version)"
    tag_base="${BASH_REMATCH[1]}"
    tag_channel="${BASH_REMATCH[6]}"
    tag_number="${BASH_REMATCH[7]}"
    tag_minecraft="${BASH_REMATCH[9]}"

    version="$tag_base"
    if [[ -n "$tag_channel" ]]; then
      version="$tag_base-$tag_channel.$tag_number"
    fi
    if [[ "$mod_version" =~ $BASE_RE ]]; then
      [[ "$tag_base" == "$mod_version" ]] || die "Tag $TAG is not a release of mod_version $mod_version"
    else
      [[ "$version" == "$mod_version" ]] || die "Tag $TAG does not match mod_version $mod_version"
    fi
    if [[ -n "$tag_minecraft" ]]; then
      [[ "$tag_minecraft" == "$minecraft_version" ]] \
        || die "Tag $TAG is for Minecraft $tag_minecraft but this commit builds $minecraft_version"
    fi
    [[ "$minecraft_range" == "[$minecraft_version]" ]] \
      || die "minecraft_version_range $minecraft_range must be [$minecraft_version] for single-version publishing"

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

    # The release must come from the line it targets: the tagged commit has to be on the selected branch.
    target="${TARGET:-}"
    if [[ -n "$target" && ! "$target" =~ ^[0-9a-f]{40}$ ]]; then
      git show-ref --verify --quiet "refs/remotes/origin/$target" || die "Release target branch $target not found"
      git merge-base --is-ancestor HEAD "refs/remotes/origin/$target" \
        || die "Tag $TAG points to a commit that is not on the release target branch $target"
      check_branch "$target"
    fi

    output version "$version"
    output release_type "$release_type"
    output minecraft_version "$minecraft_version"
    echo "Release $TAG: version $version, $release_type, Minecraft $minecraft_version"
    ;;

  artifact)
    [[ -n "${VERSION:-}" ]] || die "VERSION is empty"
    mapfile -t jars < <(find build/libs -maxdepth 1 -type f -name '*.jar' \
      ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*-dev.jar' | sort)
    [[ "${#jars[@]}" -eq 1 ]] || die "Expected exactly one distribution JAR; got ${#jars[@]}"
    jar="${jars[0]}"
    [[ "$(basename "$jar")" == *"-$minecraft_version-$VERSION.jar" ]] \
      || die "JAR $(basename "$jar") is not named for Minecraft $minecraft_version and version $VERSION"
    metadata="$(unzip -p "$jar" META-INF/neoforge.mods.toml)" || die "NeoForge metadata missing from JAR"
    grep -Fq "version=\"$VERSION\"" <<< "$metadata" || die "Mod version metadata is not $VERSION"
    grep -Fq "$minecraft_range" <<< "$metadata" || die "Minecraft metadata range is not $minecraft_range"
    echo "Artifact $jar: version $VERSION, Minecraft $minecraft_version"
    ;;

  *)
    die "Usage: validate-release.sh branch <branch> | tag | artifact"
    ;;
esac
