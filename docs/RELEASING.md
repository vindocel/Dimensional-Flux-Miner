# Release automation

Publishing happens only when a **GitHub Release is published** (never on a push or on a bare tag).

## Branches

| Branch | What it holds |
|---|---|
| `production` | Day-to-day work for every Minecraft version. Every commit goes here; it may be unstable. Alphas and betas are released from it. |
| `main` | Stable versions only. It receives a merge from `production` when a version is final, and stable releases are published from it. |

CI (`.github/workflows/ci.yml`) builds every Minecraft version and runs its GameTests on every push to `production`
or `main` and on every pull request.

## Tags and Minecraft selection

`gradle.properties` holds the base version of the open update (`mod_version=0.2.9`). Alpha, beta and stable
versions come from the GitHub Release tag; changing the channel needs no version commit.

| GitHub tag | Minecraft versions selected | Store versions |
|---|---|---|
| `v0.2.9` | All configured versions | `v0.2.9+1.21.1`, `v0.2.9+26.1.2`, `v0.2.9+26.3` |
| `v0.2.9-beta.1` | All configured versions | `v0.2.9-beta.1+1.21.1`, `v0.2.9-beta.1+26.1.2`, `v0.2.9-beta.1+26.3` |
| `v0.2.9+1.21.1` | 1.21.1 only | `v0.2.9+1.21.1` |
| `v0.2.9+1.21.1-26.1.2` | 1.21.1 and 26.1.2 | `v0.2.9+1.21.1`, `v0.2.9+26.1.2` |
| `v0.2.9-beta.2+26.1.2-26.3` | 26.1.2 and 26.3 | `v0.2.9-beta.2+26.1.2`, `v0.2.9-beta.2+26.3` |

No Minecraft suffix means every version in `settings.gradle.kts`. After `+`, list one or more Minecraft versions
separated by `-`. Unknown, repeated or malformed versions stop validation. New configured targets are automatically
included by a tag without a Minecraft suffix.

Alpha and beta releases must have **Pre-release** checked on GitHub; stable releases must leave it unchecked.
Alpha/beta target `production`; stable releases target `main`. The tag's base version must match `mod_version`.
If `mod_version` already includes a channel, the mod version and channel must match as well.

Each store upload has its own name and version number, `v<mod-version>+<minecraft>`, independent of the GitHub
Release title or the combined tag. The JAR keeps the existing name
`dimensionalfluxminer-<minecraft>-<mod-version>.jar`; its metadata contains the mod version without the Minecraft suffix.

## Publishing a release

1. Commit and push to `production` (to `main` after the merge, for a stable release); wait for CI to pass.
2. On GitHub, open **Releases â†’ Draft a new release**.
3. Choose a tag using the table above, and select `production` for alpha/beta or `main` for stable.
4. Write the release notes. The same text becomes the changelog of each selected store upload.
5. Set **Pre-release** to match the channel, then publish the GitHub Release.

A **Re-run jobs** action uses the original commit and workflow, so it does not pick up later fixes.
After committing and pushing a workflow fix, publish a new release tag pointing to the corrected commit.
For example, `v0.2.9-beta.2` publishes beta.2 store versions; it does not update beta.1.

The workflow resolves the selection, then builds, runs GameTests and validates each selected JAR in separate jobs.
Only after **every selected build passes** can the store upload jobs start. The validated artifacts are transferred
between jobs and checked again before upload. Only the upload jobs use the `management` environment and store secrets.

## Completing missing store uploads

Before uploading each Minecraft version, `scripts/check-published.py` queries CurseForge and Modrinth independently.
It recognizes the exact store version name (with or without its leading `v`) or the existing JAR filename, for the
selected Minecraft version and NeoForge loader. CurseForge metadata temporarily comes from the public
[CFWidget API](https://cfwidget.com/#api), without a read key; Modrinth still uses its official API.
CFWidget caches responses for up to one hour. A recently uploaded file may therefore be absent: avoid
individual/grouped releases or reruns for the same store version within that window, and verify the
CurseForge file list before retrying. A missing result is not a guarantee against duplicate uploads.

If `v0.2.9+1.21.1` was already uploaded and a later GitHub Release uses `v0.2.9`, that version is skipped in each store
where it already exists. The 26.1.2 and 26.3 uploads still proceed. If a version exists on only one store, the workflow
uploads it only to the other store. Existing files, versions and changelogs are not replaced.

A store lookup error stops that target's uploads instead of treating it as missing. Publication jobs for the same
store version are serialized across different GitHub tags. Only versions returned by the store API can be detected;
files still awaiting approval or otherwise hidden from the API may not appear yet. No remote publication is atomic
across stores; a rerun checks again and completes the remaining uploads.

## Several Minecraft versions (Stonecutter)

Every Minecraft version lives on the same branch. **Stonecutter** builds each one as the Gradle project
`:<minecraft>`, listed in `settings.gradle.kts` (`versions(...)`) with its own `versions/<minecraft>/gradle.properties`
(NeoForge, Java, Parchment, optional mod APIs, Catalogue logo entry). The shared values stay in the root
`gradle.properties`.

- **Code**: one `src/main/java` for all versions. Where the Minecraft API differs, Stonecutter conditions choose
  the lines of each version:

  ```java
  //? if >=26.1 {
  import net.minecraft.resources.Identifier;
  //?} else {
  /*import net.minecraft.resources.ResourceLocation;
  *///?}
  ```

  The code of the active version is plain, the others stay commented. Git always holds the sources in the
  `26.1.2` layout (`vcsVersion` in `settings.gradle.kts`, `stonecutter active` in `stonecutter.gradle.kts`).
- **Resources**: `src/main/resources` holds what every version shares; `src/mc26/resources` what 26.1.2 and 26.3
  share (their data and asset formats); `versions/<minecraft>/src/main/resources` what only that version has. A file
  lives in exactly one of these places for each version.
- **Optional integrations** without a build for a version stay out of its compilation (`build.gradle.kts`): EMI and
  Polymorph exist only on 1.21.1, REI on 1.21.1 and 26.1.2.
- **Development clients**: run `./gradlew :<minecraft>:runClient` to use `run/<minecraft>` with that version's own
  mods, settings and worlds. No active-version switch is needed for this terminal command.
- **Switching the edited version** (IDE): run the Gradle task
  `Set active project to <minecraft>`; run `Reset active project` (back to 26.1.2) before committing. Building or
  testing any version does not need a switch: `./gradlew :1.21.1:build` works from any active version.
- To add a Minecraft version, add it to `versions(...)` in `settings.gradle.kts` and create
  `versions/<minecraft>/gradle.properties`. CI builds and tests it from the next push.
- No automation can decide which code is compatible with which Minecraft version; CI on every configured version is
  what proves it.

## Credentials

The `management` environment holds:

| Kind | Name | Purpose |
|---|---|---|
| Secret | `CURSEFORGE_TOKEN` | Existing CurseForge author upload token |
| Secret | `MODRINTH_TOKEN` | Modrinth version lookup and upload |
| Variable | `ID_DO_CURSEFORGE` | Numeric CurseForge project ID |
| Variable | `ID_DO_MODRINTH` | Modrinth project ID or slug |

`CURSEFORGE_TOKEN` is used only for uploads through `X-Api-Token`. The temporary CFWidget metadata
lookup sends no credentials. An official CurseForge Core API key has been requested; switching back to
the official API will require a code/workflow change and a separate `CURSEFORGE_API_KEY` read secret
in `management`. Adding that secret alone does not switch this workflow.
Keep credentials in GitHub secrets, never in source or release notes.

The [Modrinth version-list API](https://docs.modrinth.com/api/operations/getprojectversions/) uses the existing token.
CI and build jobs do not receive store secrets. Keep `management` protected with required reviewers if desired.

## Checking locally before a tag

```bash
./gradlew :1.21.1:build :26.1.2:build :26.3:build
./gradlew :1.21.1:runGameTestServer :26.1.2:runGameTestServer :26.3:runGameTestServer
bash scripts/validate-release.sh versions
TAG=v0.2.9 PRERELEASE=false bash scripts/validate-release.sh tag
TAG=v0.2.9-beta.1+1.21.1-26.1.2 PRERELEASE=true bash scripts/validate-release.sh tag
python3 -B scripts/test-release.py
python3 -B scripts/test-published.py
```

On Windows use `gradlew.bat` and run the script from Git Bash.
