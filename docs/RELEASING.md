# Release automation

The workflow publishes only on **Publish release** (not on tag push or commit).

1. Update `mod_version` in `gradle.properties`, commit and run CI.
2. Create the matching tag from the desired tested commit. Supported tags:
   - `v0.2.0-alpha.1` (GitHub **Pre-release** selected)
   - `v0.2.0-beta.1` (GitHub **Pre-release** selected)
   - `v0.2.0` (GitHub **Pre-release** not selected)
3. Publish the GitHub Release; this triggers `release.yml`.
4. The job builds, runs required GameTests, checks tag/version/channel/Minecraft metadata/JAR and publishes to CurseForge and Modrinth.

**Minecraft versions:** the published version is read from `minecraft_version` in the tagged commit's `gradle.properties`. The build must be genuinely ported and tested for that Minecraft/NeoForge version first; changing the property alone is insufficient. This workflow handles one Minecraft version per release/tag. Tags are globally unique: never reuse a tag for a different Minecraft build. For maintenance branches use `mc/<version>` (e.g. `mc/1.21.1`), with distinct `mod_version` values for separately tagged artifacts.

The `management` environment must contain secrets `CURSEFORGE_TOKEN`, `MODRINTH_TOKEN` and variables `ID_DO_CURSEFORGE`, `ID_DO_MODRINTH`. Keep the environment protected with required reviewers if manual approval is desired. CI has no publishing secrets.

This change does not itself publish any mod or create any tag.
