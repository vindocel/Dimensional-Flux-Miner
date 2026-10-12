# Release automation

Publishing happens only when a **GitHub Release is published** (never on push or on a bare tag).

## Versions

`gradle.properties` holds the **base version of the open update**: `mod_version=0.3.0`. It changes only when an
update is closed and the next one starts. Alphas, betas and the stable release of that update come from the tag,
so they need no version commit:

| Tag | Channel | GitHub "Pre-release" | Built version |
|---|---|---|---|
| `v0.3.0-alpha.1+1.21.1` | alpha | checked | `0.3.0-alpha.1` |
| `v0.3.0-beta.2+1.21.1` | beta | checked | `0.3.0-beta.2` |
| `v0.3.0+1.21.1` | release | unchecked | `0.3.0` |

The `+<minecraft>` suffix is the Minecraft line and must match `minecraft_version` of the tagged commit. It keeps
tags unique when several Minecraft lines ship the same mod version (`v0.3.0+1.21.1` and `v0.3.0+26.2`). Tags
without it (`v0.2.0-beta.2`) are still accepted. A `mod_version` that already carries a channel
(`0.2.0-beta.2`) must match the tag exactly.

The JAR is named `dimensionalfluxminer-<minecraft>-<version>.jar`, e.g. `dimensionalfluxminer-1.21.1-0.3.0-beta.2.jar`.

## Publishing a release

1. Commit and push to the branch of the Minecraft line; wait for CI to pass.
2. On GitHub: **Releases → Draft a new release**.
3. **Choose a tag**: type the new tag (e.g. `v0.3.0-beta.1+1.21.1`), **Target**: the branch of that line.
4. Optionally press **Generate release notes** (commits since the previous tag) and edit the text; it becomes the
   CurseForge/Modrinth changelog.
5. Check **Set as a pre-release** for alpha/beta, leave it unchecked for a stable release, then **Publish**.

`release.yml` then checks out the tag and, in the `management` environment:

1. validates the tag (format, base version, Minecraft suffix, Pre-release flag) and that the tagged commit is on the
   selected target branch, and that an `mc/<version>` target builds that Minecraft version;
2. builds from scratch with the version of the tag and runs the GameTests;
3. checks the JAR (one distribution JAR, file name, version and Minecraft range in `neoforge.mods.toml`);
4. publishes to CurseForge and Modrinth as alpha/beta/release for that Minecraft version.

Any failed check stops the job before anything is uploaded.

## Several Minecraft versions

Each Minecraft line lives on its own branch, `mc/<minecraft_version>` (e.g. `mc/1.21.1`, `mc/26.2`), with its own
`gradle.properties` (`minecraft_version`, `minecraft_version_range`, `neo_version`, mappings, dependencies).
`main` follows the newest line.

- A new branch `mc/<version>` created from another one already has CI and release; nothing else to configure.
  CI fails if the branch name and `minecraft_version` disagree.
- A change meant for several lines is made on the oldest one and carried forward with a pull request
  (`mc/1.21.1` → `mc/26.2`); CI on that pull request builds and tests it on the newer line. Changes for one line
  only stay on its branch.
- To release a line, publish a release whose **Target** is its branch and whose tag carries its Minecraft suffix.
- No automation can decide which code is compatible with which Minecraft version; CI on each branch is what
  proves it.

## Credentials

The `management` environment holds the secrets `CURSEFORGE_TOKEN`, `MODRINTH_TOKEN` and the variables
`ID_DO_CURSEFORGE`, `ID_DO_MODRINTH`. Keep it protected with required reviewers if manual approval is wanted. CI
and pull requests never get publishing credentials.
