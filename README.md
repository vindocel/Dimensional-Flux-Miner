<div align="center">

# ⛏️ Dimensional Flux Miner

**A miner that never digs.**
It tunes into the frequency of the dimension it stands in and makes that dimension's ores appear out of nothing.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.255-E07A2F?style=for-the-badge)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-5382A1?style=for-the-badge)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-EPL--2.0-9366DE?style=for-the-badge)](LICENSE)
[![CI](https://img.shields.io/github/actions/workflow/status/vindocel/Dimensional-Flux-Miner/ci.yml?style=for-the-badge&label=CI)](https://github.com/vindocel/Dimensional-Flux-Miner/actions/workflows/ci.yml)

[![Visit the showcase](https://img.shields.io/badge/Visit%20the%20showcase-vindocel.github.io%2FShowcase--DFM-8A5CF5?style=for-the-badge)](https://vindocel.github.io/Showcase-DFM/)

**See every machine, the Dimensional Death and the guide in action on the [showcase site](https://vindocel.github.io/Showcase-DFM/).**

</div>

---

## 📚 Contents

- [✨ Overview](#-overview)
- [🚀 Getting started](#-getting-started)
- [📖 Dimensional Flux Guide](#-dimensional-flux-guide)
- [⛏️ Dimensional Flux Miner](#%EF%B8%8F-dimensional-flux-miner)
- [⚙️ Mechanical Crusher](#%EF%B8%8F-mechanical-crusher)
- [🔥 Electric Furnace](#-electric-furnace)
- [🛢️ Dimensional Tanks](#%EF%B8%8F-dimensional-tanks)
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
| ⚙️ **Mechanical Crusher** | Doubles your ores (triples them with the upgrade) and grinds ingots, raw ores and gems into dust. |
| 🔥 **Electric Furnace** | A three-lane electric furnace that also makes alloys, with a raw-ore bonus and stored experience. |
| 🛢️ **Dimensional Tanks** | Liquid tanks paired by a three-color code: every tank with the same code, in any dimension, holds the same liquid. |
| ⚡ **Speed 1–10** | No speed upgrades to craft: every machine has a speed selector, limited only by your power generation. One machine from early game to end game. |
| ☠️ **Dimensional Death** | The price of the Basic core's drain: the land, the water and the life around it rot forever. |
| 📖 **Dimensional Flux Guide** | An animated book with the whole mod explained, in game. |
| 🌙 **Light or dark screens** | A Settings tab on every machine screen (left side) switches it between light and dark, for one machine or all of them; only your own game changes. |

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

1. 🧱 Build a **Mechanical Crusher** and an **Electric Furnace** (vanilla items only).
2. ⚙️ In the Crusher, turn iron ingots into **Iron Dust**, coal into **Coal Dust** and calcite into **Calcite Grains**.
3. 🔥 In the Electric Furnace, combine the three into **Pure Steel** (each one also leaves a **Slag**; nine slag make a Slag Aggregate that turns back into steel).
4. 👁️ With Pure Steel, craft the **Reinforced Eye of Ender**, the Miner parts and a **Dimensional Core**.
5. ⛏️ Build the Miner, insert the core, connect power and press the power button.

---

## 📖 Dimensional Flux Guide

Your first **Pure Steel** earns you the guide (through the "Forged in the Flux" advancement); more copies are crafted from a **book + Pure Steel + Reinforced Eye of Ender** (shapeless).

- 🎞️ **Animated book**: using it opens the book in the hand with the model's own animation; it stays half open while you read, a leaf turns in your hand with every page, and it closes when you leave.
- 📄 **Real pages**: the presentation and the clickable **contents** open the book; every chapter starts on a left page and the pages flow one after another, with a page-turn animation and sound. The violet ribbon goes back to the contents.
- 🧊 **Interactive 3D Miner**: rotate the assembly, look at one layer at a time, switch between loose parts and the formed machine, hover a part to isolate it.
- 🧾 **Live data**: crafting recipes come from the server, the speed tables and matter values follow the server configuration and datapacks.
- 🔎 Item tooltips everywhere and buttons that open the machine recipes in EMI / JEI. The first time a chapter names an item, the word is underlined: hover it to see the item's icon and name.

All text lives in the language files (`guide.dimensionalfluxminer.*`), in English and Brazilian Portuguese.

---

## ⛏️ Dimensional Flux Miner

### 🧱 Building it

27 blocks. The **Miner Terminal** faces the front of the machine.

| Layer | Parts |
|---|---|
| 1 — bottom | 7 **Dimensional Ports** around, a **Steel Chassis** in the center, the **Miner Terminal** in the front middle |
| 2 — middle | **Quantum Core** in the center, **Reinforced Structural Glass** in front and on both sides, **Steel Chassis** in the corners and at the back |
| 3 — top | **Reinforced Structural Glass** in the center, front and sides, **Ventilation Units** in the corners and the back middle |

The ports of the bottom layer have **nine configurable faces**: three on each side and three at the back. In the **Side Configuration** you pick one face for the **Item Output** and, with the Stabilized core, one for the **Matter Input** (both may share a face). **Energy** enters through any face by default, and a port appears where a cable touches it; pick the energy faces by hand and they stay visible. Until a core is inserted, only the Core slot can be used.

> 💡 Hold the terminal and the whole machine shows as see-through blocks where it would be built. Once the terminal is down, the missing parts stay visible as see-through blocks (red outline where a block is in the way) until the machine forms. Right-click the terminal: every click places the next missing part from your inventory (bottom layer first); with no part to place, the click hides or shows the preview. The structure forms with the last part.

### 💎 Cores: where the matter comes from

| Core | Mode | How it works |
|---|---|---|
| **Basic Dimensional Core** (Rare) | 🌀 **Dimensional Drain** | Surveys the loaded chunks within 300 blocks and harvests the dimensional energy of the land, column by column (**64 DM** per column). The drained land falls to the **Dimensional Death**. |
| **Stabilized Dimensional Core** (Epic) | 💉 **Matter Injection** | Turns the items pushed into the Matter Input into DM at once. Never touches the environment. |

Matter Injection only takes matter of the Miner's own dimension. All values are data driven:

| Dimension | Accepted matter |
|---|---|
| 🌍 Overworld | dirt **0.1 DM**, grass block **0.2**, cobblestone, cobbled deepslate, sand and red sand **0.3**, stone, deepslate, granite, diorite, andesite and every terracotta **0.5** |
| 🔥 Nether | netherrack, basalt, blackstone, nylium and magma block **0.5**, glowstone, soul sand and soul soil **1**, nether bricks **1.5** |
| 🌌 The End | end stone and purpur block **0.5**, dragon egg **10** |
| 🧩 The Other (Allthemodium) | ancient dirt **0.1**, ancient stone **0.5** |
| ✨ Any dimension | obsidian **1** |

Ores count only in a dimension where that exact ore generates (Overworld diamond ore is refused in the Nether): **0.5 DM**, diamond and uranium **1**, emerald, platinum and ancient debris **2**. The Basic core only finds land to drain in the Overworld; the Nether and the End need the Stabilized core.

🕳️ **The void of the End**: where few ore families are born, part of the attempts find nothing (they still cost FE and DM). By default only the End: 97% with one family, shrinking geometrically to none at five families. The terminal's details show the void chance.

### 🎛️ Using it

- 🔌 **Power button**: a new Miner starts off and only this button starts it; redstone can stop it but never start it.
- 🖥️ **Terminal screen**: status, core, **limiting factor** (energy, no matter, output full, nothing to drain, redstone) and the DM stored.
- 🧲 **Filter**: an allow-list of ore families; with nothing selected the filter is off.
- 🔁 **Auto Input / Auto Eject** (Side Configuration): work with the inventories touching the item ports, even while the machine is off.
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
- With the Efficiency Upgrade, every injected item gives `+ min(value, 0.2)` DM (an ore the Miner can make never more than 2 DM in total) and every ore rejected by the filter returns `min(base + 0.5, 2) DM`.
</details>

---

## ⚙️ Mechanical Crusher

- 🪨 **Ores**: 2 products per ore (**3** with the Efficiency Upgrade, which also halves FE). Works with ores of any mod (`c:ores/<m>` → `c:raw_materials/<m>` → `c:gems/<m>`).
- 🧂 **Dust**: ingots, raw materials and gems of any mod become one dust of the same material whenever a `c:dusts/<m>` item exists. Metal dusts smelt back into ingots (furnace or blast furnace).
- 📦 1 input slot and 4 output slots of 64 items; a product continues in the next free slot and the machine stops only when no output can take it.
- 🧭 **Side Configuration** for items and energy; the front is locked and the top face always picks up dropped items.
- 🔴 Redstone Control: Ignored / Normal / Inverted.

---

## 🔥 Electric Furnace

| Mode | Behavior |
|---|---|
| **Furnace Only** | 3 independent lanes, each with the blast furnace recipe of its item (or the furnace one) |
| **Alloy + Furnace** (default) | runs an alloy when the lanes hold one, otherwise every lane smelts on its own |
| **Alloy Only** | 1–3 counted ingredients make one operation with a main result and an optional byproduct (steel + slag) |

- 🎁 **Raw ore bonus**: 2 raw → 3 ingots, or 1 raw → 2 ingots with the Efficiency Upgrade.
- 🔀 **Auto Split** spreads items evenly over the free lanes, wherever they come from.
- ✨ Recipe XP is stored as **Liquid Experience** (`#c:experience`, 1 XP = 20 mB): pipes extract it from the EXPERIENCE faces, and breaking the machine drops it as orbs.
- 🔁 **Automation per material** (Side Configuration): items have Auto Input and Auto Eject; Liquid Experience, which only leaves the machine, has its own Auto Eject, switched on and off apart from the items.
- 🧪 Also runs the alloy recipes of other mods (EnderIO and Oritech by default).

> 💡 **Crusher + Electric Furnace**: 1 ore → **3** ingots without upgrades, **4.5** with the upgrade in the Crusher, **4** with it in the Electric Furnace, **6** with both — against 1 ingot in a common furnace.

<details>
<summary>🔬 Lane and output details</summary>

- Three outputs shared by every mode: lane *n* smelts into output *n*; when that slot holds another item the lane uses a slot that already holds its result, or a free slot whose own lane is idle or makes the same item. An alloy puts its main result in output 1 and its byproduct in output 2. Every output stays extractable in every mode.
- In Alloy + Furnace mode an alloy ingredient that can also be smelted (for example iron dust) is spread like any furnace item, unless another lane holds one of its alloy partners (for example coal dust).
- In ALLOY mode, lanes 1/2/3 can be routed individually (input-only) in the Side Configuration.
- Emits light level 14 while working; the ingredients being processed are rendered inside the chamber.
</details>

---

## 🛢️ Dimensional Tanks

| Tank | Chambers |
|---|---|
| **Dimensional Tank** | one chamber of **128 000 mB** |
| **Dual Dimensional Tank** | two separate chambers of **128 000 mB** each: **A** (right side of the front) and **B** (left side) |

- 🎨 **Dimensional Code**: three of 16 colors on the front panel, picked on the screen (no dye needed) and read left to right; the order counts. Every tank with the same kind and code, in any dimension, shows and moves the same liquid, with the others loaded or not. A single and a dual tank never share, even with the same colors; dual tanks share A with A and B with B.
- 🔁 Changing the code (click the code on the screen) points the tank at another channel: no liquid moves, is copied or is lost. A broken tank keeps its code on the item, and the liquid stays in the channel.
- 💎 **Personal codes**: a diamond on the tank makes its code a channel of its own for you, apart from the public one with the same colors; sneak and use it with an empty hand to make it public again (the diamond comes back).
- 🪣 Buckets and fluid containers: right click the block with a filled one to empty it, with an empty one to take the liquid out (on the dual tank, the right side is A and the left side is B), or click the reservoir on the screen.
- 🔧 Pipes: every face but the front carries liquid; the single tank starts as input/output on all of them, the dual tank with A on the right side and B on the left. Each face serves one chamber only, and a full chamber never spills into the other. Liquids have their own Auto Input and Auto Eject in the Side Configuration.
- ⚡ No energy.
- 🧰 Recipes: tank = 4 Dimensional Steel Chassis + 2 Dimensional Ports + 2 Reinforced Eyes of Ender + 1 Reinforced Structural Glass; dual tank = 2 Dimensional Tanks + 4 Reinforced Eyes of Ender + 2 Pure Steel + 1 Dimensional Steel Chassis.

---

## ⚡ Speed and the Efficiency Upgrade

There is **no speed upgrade to craft**. Every machine has a **speed selector from 1 to 10** (the tab with colored strips): every operation costs the same energy at every level, so a higher level works faster and asks for more FE/t in the same proportion, with no discount and no penalty. Your power generation is the only limit, so the same machine follows you from the first ingot to the last dragon. A machine only spends the energy of the work it really does.

| Machine | Cost per operation | Speed 1 | Speed 10 |
|---|---|---|---|
| ⛏️ Miner | 200 000 FE per attempt | 2 500 FE/t — 0.25 ores/s | 12 800 000 FE/t — 1 280 ores/s |
| ⚙️ Crusher | 20 000 FE per item | 100 FE/t — 1 item every 10 s | 1 280 000 FE/t — 1 280 items/s |
| 🔥 Electric Furnace (furnace) | 50 000 / 3 FE per item | 250 FE/t (3 lanes) — 0.1 items/s per lane | 3 200 000 FE/t — 1 280 items/s per lane, 3 840 items/s in total |
| 🔥 Electric Furnace (alloy) | 30 000 FE per execution | 150 FE/t | 1 920 000 FE/t — 1 280 alloys/s |

The Electric Furnace values are for a standard recipe (10 s); the Efficiency Upgrade halves the Crusher and Electric Furnace costs. Intermediate levels are geometric. The guide shows the full tables computed from the server configuration.

**Dimensional Efficiency Upgrade** (one per machine):

| Machine | Effect |
|---|---|
| ⚙️ Crusher | half the energy, 3 products per ore instead of 2 |
| 🔥 Electric Furnace | half the energy, raw bonus 1 → 2 instead of 2 → 3 |
| ⛏️ Miner | energy unchanged; up to +0.2 DM per injected item; ores rejected by the filter return DM |

---

## ☠️ Dimensional Death

Dimensional energy is the glue that holds the world together. Where the Basic core's drain harvests it, the land falls apart.

- 🟣 **Terrain**: dirt, grass and the stone family (calcite, dripstone and modded stones such as Create's asurine and AE2's sky stone included) become corrupted; every terracotta becomes **Corrupted Terracotta**; sands and gravels become **Corrupted Sediment** (falls like sand, smelts into glass). It reaches the ground through trees and buildings (up to 32 blocks of cover). Weeds become corrupted grass and flowers become wither roses.
- 💧 **Corrupted Water**: flows, makes infinite sources and puts out fire, with a purple underwater view and purple bubbles; it sinks layer by layer but never spreads sideways. **No land plant can drink from it** (no farmland, no sugar cane); kelp and seagrass survive in it.
- 🧟 **Creatures**: it hurts players and peaceful mobs and heals hostile ones (the Warden one heart every half second; never the Ender Dragon or the Wither). Hostiles do not burn in daylight on corrupted terrain and more of them appear near the corruption.

> 🌿 Want to mine without hurting the land? Use the **Stabilized Dimensional Core**.

---

## 🏆 Advancements

| | Advancement | How |
|---|---|---|
| 📖 | **Dimensional Flux** | join the world |
| ⚙️ | Ground and Sifted | build a Mechanical Crusher |
| 🔥 | Electric Fire | build an Electric Furnace |
| 🔩 | Forged in the Flux | get your first Pure Steel (rewards the guide) |
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

- 🔧 **Pipes and cables**: every machine exposes the standard NeoForge capabilities (items, energy, fluid) per face. A cable that keeps pulling items from a face (or pushing into it) sets that face's item route by itself, on the Miner's ports, the Crusher and the Electric Furnace.
- 📘 **EMI / JEI** (optional): Crushing, Electric Furnace (Furnace) and Electric Furnace (Alloy) categories; clicking a machine's work chamber opens its recipes, and R / U work on the items inside machine slots.
- 🔍 **Jade**: input / output items, the machines' own animated energy bar and the Electric Furnace's stored XP in mB.
- 🧪 **Cognition**: the Experience Obelisk takes the Electric Furnace's Liquid Experience directly.
- 🔀 **Polymorph** (optional): when an item in the Crusher or the Electric Furnace has more than one recipe, a button near the outputs lets you pick the result; the pick is kept per item on that machine.
- ⏩ **Tick accelerators** (e.g. Soul Surge): every extra ticker call runs one more complete cycle at full FE cost; on the Miner only the eight bottom blocks around the terminal (the seven ports and the center chassis) are receivers.
- 🌍 **Ores of other mods**: the Miner reads the ore features of other mods that declare `targets[].state`; the Crusher and Electric Furnace work through the common tags.

<details>
<summary>🔬 More compatibility details</summary>

- **Tick acceleration**: the energy buffer stays 10 × the FE/t of the speed, but the energy input also accepts what the extra cycles of one game time use, so a source that refills once per game time keeps an accelerated machine at full power. A machine whose accelerated cycles worked in the last tick always shows as powered and running. Covered by GameTests and observed in game with Soul Surge.
- **Claims**: corruption respects **FTB Chunks** and **Open Parties and Claims** when they are installed (optional, no dependency). The Basic Core only corrupts unclaimed land or claims where the Miner's owner may build (team members in FTB Chunks; the claim's own access rules in Open Parties and Claims); a claim of someone else, or a check that fails, is never corrupted and gives no DM. A column is checked as a whole before anything changes, and Corrupted Water never flows or sinks across a claim border. Server config: `corruption.enabled`, `corruption.blockedDimensions`, `corruption.respectSpawnProtection`, `corruption.protectedRegions` and `corruption.claimFailurePolicy` (`deny` by default). Tested with FTB Chunks 2101.1.22; the Open Parties and Claims adapter follows its public API but was not run against the mod.
- **Recipe viewers**: with both installed, only the EMI plugin runs. The Electric Furnace categories use the machine's own processing element.
- **Jade**: every machine answers the no-face (`null` side) capability query with read-only views; text too long for a bar slides from side to side. Taking Electric Furnace results by hand collects the stored XP, as from a vanilla furnace.
- **Alloys of other mods**: recipe types listed in `electric_furnace.externalAlloyRecipeTypes` (default `enderio:alloy_smelting`, `oritech:foundry`) are read through their own codecs: 1–3 counted item ingredients, item results (a second result becomes the byproduct), experience and time. Recipes with fluids or more ingredients are skipped, and so are plain furnace copies. No other mod is needed to build or run.
- **Resource Profiles**: vein size defaults to 8 when a mod's ore feature does not declare it; mods that add no ores contribute nothing.
</details>

---

## 🧩 Configuration and datapacks

Modpack creators, modders and server administrators can customize machine settings, dimensional resource profiles, Matter values, recipes, corruption tags and compatibility.

Everything can also be done from a KubeJS server script (optional). For supported options, limits, JSON/TOML/KubeJS
examples and datapack instructions, see the **[Complete Customization Guide](docs/CUSTOMIZATION.md)**.

---

## 🛠️ Building from source

```bash
./gradlew build                # jar in build/libs/dimensionalfluxminer-<minecraft>-<version>.jar
./gradlew test                 # JUnit: rate curves, production curve, buffers, bulk bonus, auto-split, matter math, ore weights, void curve, layout
./gradlew runGameTestServer    # in-world GameTests for the machines, the corruption and the content
./gradlew runClient            # development client
./gradlew runServer            # development server
```

On Windows use `gradlew.bat`. The Gradle wrapper provisions Gradle itself; a JDK 21 is required (the foojay toolchain resolver can download one).

- 🧭 Code layout, tests and development rules: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
- ⚖️ Every balance number (speed and energy tables, matter, ore distribution, protections, server config): [docs/BALANCE.md](docs/BALANCE.md).

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
