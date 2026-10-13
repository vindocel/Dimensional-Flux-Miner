<div align="center">

# ⛏️ Dimensional Flux Miner

**A miner that never digs.**
It tunes into the frequency of the dimension it stands in and makes that dimension's ores appear out of nothing.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.255-E07A2F?style=for-the-badge)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-5382A1?style=for-the-badge)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-EPL--2.0-9366DE?style=for-the-badge)](LICENSE)
[![CI](https://img.shields.io/github/actions/workflow/status/vindocel/Dimensional-Flux-Miner/ci.yml?style=for-the-badge&label=CI)](https://github.com/vindocel/Dimensional-Flux-Miner/actions/workflows/ci.yml)

</div>

---

## 📚 Contents

- [✨ Overview](#-overview)
- [🚀 Getting started](#-getting-started)
- [📖 Dimensional Flux Guide](#-dimensional-flux-guide)
- [⛏️ Dimensional Flux Miner](#%EF%B8%8F-dimensional-flux-miner)
- [⚙️ Dimensional Crusher](#%EF%B8%8F-dimensional-crusher)
- [🔥 Alloy Smelter](#-alloy-smelter)
- [⚡ Speed and the Efficiency Upgrade](#-speed-and-the-efficiency-upgrade)
- [☠️ Dimensional Death](#%EF%B8%8F-dimensional-death)
- [🏆 Advancements](#-advancements)
- [🔌 Compatibility](#-compatibility)
- [🧩 Configuration and datapacks](#-configuration-and-datapacks)
- [🛠️ Building from source](#%EF%B8%8F-building-from-source)
- [🚢 Continuous integration and releases](#-continuous-integration-and-releases)
- [📜 License and credits](#-license-and-credits)

---

## ✨ Overview

| | |
|---|---|
| ⛏️ **Dimensional Flux Miner** | A 3×3×3 multiblock that turns energy (FE) and **Dimensional Matter (DM)** into the ores that really generate in its dimension, without breaking a single block of the world. |
| ⚙️ **Dimensional Crusher** | Doubles your ores (triples them with the upgrade) and grinds ingots, raw ores and gems into dust. |
| 🔥 **Alloy Smelter** | A three-lane electric furnace that also makes alloys, with a raw-ore bonus and stored experience. |
| ⚡ **Speed 1–10** | No speed upgrades to craft: every machine has a speed selector, limited only by your power generation. One machine from early game to end game. |
| ☠️ **Dimensional Death** | The price of the Basic core's drain: the land, the water and the life around it rot forever. |
| 📖 **Dimensional Flux Guide** | An animated book with the whole mod explained, in game. |

| | |
|---|---|
| Minecraft | **1.21.1** |
| NeoForge | **21.1.255** |
| Java | **21** |
| Mod ID | `dimensionalfluxminer` |
| License | EPL-2.0 (see [`LICENSE`](LICENSE)) |

> 🔋 The mod generates **no energy**: power the machines with generators from any other mod.

---

## 🚀 Getting started

1. 🧱 Build a **Dimensional Crusher** and an **Alloy Smelter** (vanilla items only).
2. ⚙️ In the Crusher, turn iron ingots into **Iron Dust**, coal into **Coal Dust** and calcite into **Calcite Grains**.
3. 🔥 In the Smelter, combine the three into **Steel Ingots** (each one also leaves a **Slag**; nine slag make a Slag Aggregate that turns back into steel).
4. 👁️ With steel, craft the **Reinforced Eye of Ender**, the Miner parts and a **Dimensional Core**.
5. ⛏️ Build the Miner, insert the core, connect power and press the power button.

---

## 📖 Dimensional Flux Guide

Every new player receives the guide with the mod's first advancement; it is also crafted from a **book + steel ingot + Reinforced Eye of Ender** (shapeless).

- 🎞️ **Animated book**: using it opens the book in the hand with the model's own animation; it stays half open while you read, a leaf turns in your hand with every page, and it closes when you leave.
- 📄 **Real pages**: the presentation and the clickable **contents** open the book; every chapter starts on a left page and the pages flow one after another, with a page-turn animation and sound. The violet ribbon goes back to the contents.
- 🧊 **Interactive 3D Miner**: rotate the assembly, look at one layer at a time, switch between loose parts and the formed machine, hover a part to isolate it.
- 🧾 **Live data**: crafting recipes come from the server, the speed tables and matter values follow the server configuration and datapacks.
- 🔎 Item tooltips everywhere and buttons that open the machine recipes in EMI / JEI.

All text lives in the language files (`guide.dimensionalfluxminer.*`), in English and Brazilian Portuguese.

---

## ⛏️ Dimensional Flux Miner

### 🧱 Building it

27 blocks. The **Miner Terminal** faces the front of the machine.

| Layer | Parts |
|---|---|
| 1 — bottom | 8 **Steel Chassis** around, the **Miner Terminal** in the front middle |
| 2 — middle | **Quantum Core** in the center, **Dimensional Ports** left / right / back, **Reinforced Structural Glass** in front, **Ventilation Units** in the corners |
| 3 — top | **Reinforced Structural Glass** in the center, front and sides, **Ventilation Units** in the corners and the back middle |

Looking at the terminal, the three identical ports become **Energy Input** (left), **Item Output** (right) and **Matter Input** (back) when the structure forms.

> 💡 Place the terminal first and right-click it: every click places the next missing part from your inventory (bottom layer first) and outlines where the others go, colored by part, red where a block is in the way. The structure forms with the last part.

### 💎 Cores: where the matter comes from

| Core | Mode | How it works |
|---|---|---|
| **Basic Dimensional Core** (Rare) | 🌀 **Dimensional Drain** | Surveys the loaded chunks within 300 blocks and harvests the dimensional energy of the land, column by column (**64 DM** per column). The drained land falls to the **Dimensional Death**. |
| **Stabilized Dimensional Core** (Epic) | 💉 **Matter Injection** | Turns the items pushed into the Matter Input into DM at once. Never touches the environment. |

Matter values (Matter Injection): dirt **0.1 DM**, cobblestone **0.3**, stone family **0.5** (each only in its own dimension); ores **0.5** in any dimension, diamond and uranium **1**, emerald, platinum and ancient debris **2**. All data driven.

### 🎛️ Using it

- 🔌 **Power button**: a new Miner starts off and only this button starts it; redstone can stop it but never start it.
- 🖥️ **Terminal screen**: status, core, **limiting factor** (energy, no matter, output full, nothing to drain, redstone) and the DM stored.
- 🧲 **Filter**: an allow-list of ore families; with nothing selected the filter is off.
- 🔁 **Auto Input / Auto Eject** (Settings window): work with the inventories touching the ports, even while the machine is off.
- 🔴 **Redstone Control**: a signal on any bottom-layer block becomes an extra lock.
- ⚡ **Quick install**: sneak + right-click with an Efficiency Upgrade or a core on any part installs it.

| Specification | Value |
|---|---|
| Energy buffer | 10 × the FE/t of the selected speed |
| DM buffer | 5 000 DM — every roll costs 2 DM |
| Output | 27 slots — a roll with no room is lost (time and FE, not DM); OUTPUT BLOCKED only when nothing it keeps fits |
| Item I/O | no limit per tick — energy is the only limit |

<details>
<summary>🔬 Production details</summary>

- Production follows `R(P) = Rmin·(P/Pmin)^k` (1/80 → 64 resources per logical cycle); FE is charged through the inverse curve for the work actually done.
- The limiting factor is the strongest limit met by any cycle of the last tick, accelerated ones included; the screen also shows the potential and real production of the tick.
- The animation and the sound bridge pauses of up to 1 s, so they never flicker.
- Auto Input pulls repeatedly until the DM buffer or the source runs out, so a single-slot bulk or creative storage feeds it at full speed. Only a Redstone Control halt freezes Auto Input / Auto Eject.
- With the Efficiency Upgrade, every injected item gives +0.5 DM and every ore rejected by the filter returns `min(base + 0.5, 2) DM`.
</details>

---

## ⚙️ Dimensional Crusher

- 🪨 **Ores**: 2 products per ore (**3** with the Efficiency Upgrade, which also halves FE). Works with ores of any mod (`c:ores/<m>` → `c:raw_materials/<m>` → `c:gems/<m>`).
- 🧂 **Dust**: ingots, raw materials and gems of any mod become one dust of the same material whenever a `c:dusts/<m>` item exists. Metal dusts smelt back into ingots (furnace or blast furnace).
- 📦 1 input slot and 4 output slots of 64 items; a product continues in the next free slot and the machine stops only when no output can take it.
- 🧭 **Side Configuration** for items and energy; the front is locked and the top face always picks up dropped items.
- 🔴 Redstone Control: Ignored / Normal / Inverted.

---

## 🔥 Alloy Smelter

| Mode | Behavior |
|---|---|
| **Furnace Only** | 3 independent lanes, each with the blast furnace recipe of its item (or the furnace one) |
| **Alloy + Furnace** (default) | runs an alloy when the lanes hold one, otherwise every lane smelts on its own |
| **Alloy Only** | 1–3 counted ingredients make one operation with a main result and an optional byproduct (steel + slag) |

- 🎁 **Raw ore bonus**: 2 raw → 3 ingots, or 1 raw → 2 ingots with the Efficiency Upgrade.
- 🔀 **Auto Split** spreads items evenly over the free lanes, wherever they come from.
- ✨ Recipe XP is stored as **Liquid Experience** (`#c:experience`, 1 XP = 20 mB): pipes extract it from the EXPERIENCE faces, and breaking the machine drops it as orbs.
- 🧪 Also runs the alloy recipes of other mods (EnderIO and Oritech by default).

> 💡 **Crusher + Smelter**: 1 ore → **3** ingots without upgrades, **4.5** with the upgrade in the Crusher, **4** with it in the Smelter, **6** with both — against 1 ingot in a common furnace.

<details>
<summary>🔬 Lane and output details</summary>

- Three outputs shared by every mode: lane *n* smelts into output *n*; when that slot holds another item the lane uses a slot that already holds its result, or a free slot whose own lane is idle or makes the same item. An alloy puts its main result in output 1 and its byproduct in output 2. Every output stays extractable in every mode.
- In Alloy + Furnace mode an alloy ingredient that can also be smelted (for example iron dust) is spread like any furnace item, unless another lane holds one of its alloy partners (for example coal dust).
- In ALLOY mode, lanes 1/2/3 can be routed individually (input-only) in the Side Configuration.
- Emits light level 14 while working; the ingredients being processed are rendered inside the chamber.
</details>

---

## ⚡ Speed and the Efficiency Upgrade

There is **no speed upgrade to craft**. Every machine has a **speed selector from 1 to 10** (the tab with colored strips): each level uses 2 to 2.5 times more energy and works faster in the same proportion. Your power generation is the only limit, so the same machine follows you from the first ingot to the last dragon. A machine only spends the energy of the work it really does.

| Machine | Speed 1 | Speed 10 |
|---|---|---|
| ⛏️ Miner | 500 FE/t — 0.25 ores/s | 1 500 000 FE/t — 1 280 ores/s |
| ⚙️ Crusher | 250 FE/t — 1 item every 10 s | 500 000 FE/t — 1 280 items/s |
| 🔥 Alloy Smelter | 600 FE/t — 0.1 items/s per lane | 600 000 FE/t — 3 840 items/s per lane |

Intermediate levels are geometric; the guide shows the full tables computed from the server configuration.

**Dimensional Efficiency Upgrade** (one per machine):

| Machine | Effect |
|---|---|
| ⚙️ Crusher | half the energy, 3 products per ore instead of 2 |
| 🔥 Alloy Smelter | half the energy, raw bonus 1 → 2 instead of 2 → 3 |
| ⛏️ Miner | energy unchanged; +0.5 DM per injected item; ores rejected by the filter return DM |

---

## ☠️ Dimensional Death

Dimensional energy is the glue that holds the world together. Where the Basic core's drain harvests it, the land falls apart.

- 🟣 **Terrain**: dirt, grass and the stone family become corrupted; sands and gravels become **Corrupted Sediment** (falls like sand, smelts into glass). It reaches the ground through trees and buildings (up to 32 blocks of cover). Weeds become corrupted grass and flowers become wither roses.
- 💧 **Corrupted Water**: flows, makes infinite sources and puts out fire, with a purple underwater view and purple bubbles; it sinks layer by layer but never spreads sideways. **No land plant can drink from it** (no farmland, no sugar cane); kelp and seagrass survive in it.
- 🧟 **Creatures**: it hurts players and peaceful mobs and heals hostile ones (the Warden one heart every half second; never the Ender Dragon or the Wither). Hostiles do not burn in daylight on corrupted terrain and more of them appear near the corruption.

> 🌿 Want to mine without hurting the land? Use the **Stabilized Dimensional Core**.

---

## 🏆 Advancements

| | Advancement | How |
|---|---|---|
| 📖 | **Dimensional Flux** | join the world — gives the guide |
| ⚙️ | Ground and Sifted | build a Dimensional Crusher |
| 🔥 | Electric Fire | build an Alloy Smelter |
| 🔩 | Forged in the Flux | get a Steel Ingot |
| ♻️ | Nothing Is Lost | turn Slag Aggregate back into steel |
| 👁️ | Unbreakable Gaze | craft a Reinforced Eye of Ender |
| 🏰 | To the Stronghold | reach a stronghold following a Reinforced Eye |
| 🧱 | Piece by Piece | form the Miner |
| 💎 | First Extraction | receive the first ore from the Miner |
| 🧲 | Ore Surgeon | discard an ore with the filter |
| 🌡️ | Redline *(challenge)* | set any machine to speed 10 |
| ⏱️ | A Thousand per Second *(challenge)* | keep 1 000+ operations per second for 5 seconds |
| ✨ | Absolute Efficiency *(challenge)* | craft the Efficiency Upgrade |
| 🧹 | Neat Freak | make DM by Matter Injection |
| 🪨 | From Trash to Treasure | inject 10 000 cobblestone |
| 🌀 | Drain Engaged | drain the first column |
| 🟣 | Scorched Earth | step on corrupted dirt or grass |
| 💧 | Purple Plunge | get into corrupted water |
| 🤫 | *secret* | … |
| 🤫 | *secret (challenge)* | … — rewards an Efficiency Upgrade |

---

## 🔌 Compatibility

- 🔧 **Pipes and cables**: every machine exposes the standard NeoForge capabilities (items, energy, fluid) per face.
- 📘 **EMI / JEI** (optional): Crushing, Alloy Smelter (Furnace) and Alloy Smelter (Alloy) categories; clicking a machine's work chamber opens its recipes, and R / U work on the items inside machine slots.
- 🔍 **Jade**: input / output items, the machines' own animated energy bar and the Smelter's stored XP in mB.
- ⏩ **Tick accelerators** (e.g. Soul Surge): every extra ticker call runs one more complete cycle at full FE cost; on the Miner only the eight bottom Steel Chassis are receivers.
- 🌍 **Ores of other mods**: the Miner reads the ore features of other mods that declare `targets[].state`; the Crusher and Smelter work through the common tags.

<details>
<summary>🔬 More compatibility details</summary>

- **Tick acceleration**: the energy buffer stays 10 × the FE/t of the speed, but the energy input also accepts what the extra cycles of one game time use, so a source that refills once per game time keeps an accelerated machine at full power. A machine whose accelerated cycles worked in the last tick always shows as powered and running. Covered by GameTests and observed in game with Soul Surge.
- **Claims**: corruption goes through an adapter registry (`ProtectionService`). No claim mod adapter ships yet (FTB Chunks and Open Parties and Claims are planned), so corruption currently respects only the built-in blacklist.
- **Recipe viewers**: with both installed, only the EMI plugin runs. The Alloy Smelter categories use the machine's own processing element.
- **Jade**: every machine answers the no-face (`null` side) capability query with read-only views; text too long for a bar slides from side to side. Taking Smelter results by hand collects the stored XP, as from a vanilla furnace.
- **Alloys of other mods**: recipe types listed in `smelter.externalAlloyRecipeTypes` (default `enderio:alloy_smelting`, `oritech:foundry`) are read through their own codecs: 1–3 counted item ingredients, item results (a second result becomes the byproduct), experience and time. Recipes with fluids or more ingredients are skipped, and so are plain furnace copies. No other mod is needed to build or run.
- **Resource Profiles**: vein size defaults to 8 when a mod's ore feature does not declare it; mods that add no ores contribute nothing.
</details>

---

## 🧩 Configuration and datapacks

The server config `dimensionalfluxminer-server.toml` sets the FE extremes of each machine and the Miner production extremes; every intermediate speed level is recalculated from them. The Miner maximum can be lowered but never raised above 64 per cycle.

<details>
<summary>📂 Datapack reference</summary>

| Path | Purpose |
|---|---|
| `data/<ns>/dimensional_miner_resources/*.json` | Resource Profile overrides: `resource`, `dimension`, `family`, `weight` (share of the profile, 0–1], `enabled`, `crusher_output` |
| `data/<ns>/data_maps/item/matter_value.json` | DM value of substrates (`dm`, optional `dimensions`) |
| `data/dimensionalfluxminer/tags/block/corruptible_stone.json` | Stone-family blocks converted to corrupted stone |
| `data/dimensionalfluxminer/tags/block/corruptible_sediment.json` | Sands and gravels converted to corrupted sediment |
| `data/dimensionalfluxminer/tags/block/corruptible_weeds.json` | Weeds converted to corrupted grass |
| `data/dimensionalfluxminer/tags/block/corruption_ground.json` | Natural ground: the corruption looks through every other block above it |
| `data/dimensionalfluxminer/tags/block/corruption_blacklist.json` | Blocks never corrupted |
| `data/dimensionalfluxminer/tags/entity_type/*.json` | Dimensional Death spawn blacklist, light-bypass eligibility, Corrupted Water heal blacklist and heart-heal list |
| recipe types `crushing`, `ore_crushing`, `alloying` | Machine recipes |
| recipe types `tag_smelting`, `tag_blasting` | Furnace / blast furnace recipes whose result is the first item of `result_tag` |
</details>

---

## 🛠️ Building from source

```bash
./gradlew build                # jar in build/libs/dimensionalfluxminer-<minecraft>-<version>.jar
./gradlew test                 # JUnit: rate curves, production curve, buffers, bulk bonus, auto-split, layout
./gradlew runGameTestServer    # in-world GameTests for the machines, the corruption and the content
./gradlew runClient            # development client
./gradlew runServer            # development server
```

On Windows use `gradlew.bat`. The Gradle wrapper provisions Gradle itself; a JDK 21 is required (the foojay toolchain resolver can download one).

---

## 🚢 Continuous integration and releases

| Workflow | When | What |
|---|---|---|
| 🧪 **CI** (`ci.yml`) | every push to `main`, `1.21.x`, `mc/**` and every pull request | checks that an `mc/<version>` branch builds that Minecraft version, runs `./gradlew build` and the GameTests on Java 21; no publishing credentials |
| 📦 **Release** (`release.yml`) | a GitHub Release is published | validates the tag and its target branch, builds the tagged commit with the tag's version, runs the GameTests, checks the jar and uploads it to CurseForge and Modrinth with the release body as changelog |

- 🏷️ Tags look like `v0.3.0-beta.1+1.21.1`: `gradle.properties` keeps the base version, the channel (alpha, beta, release) comes from the tag.
- 🌿 Each Minecraft line lives on an `mc/<version>` branch.
- 📘 Full release and multi-version workflow: [docs/RELEASING.md](docs/RELEASING.md).

---

## 📜 License and credits

- 📄 Code and original assets: **EPL-2.0** — see [`LICENSE`](LICENSE).
- 🎨 A few machine screen sprites (panel background, slots, button background and some icons) come from an MIT-licensed project; the file list and the license text are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) (also packaged in the mod jar). Everything else was drawn for this mod.
