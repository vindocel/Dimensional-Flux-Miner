# 🧩 Customization Guide

How to change Dimensional Flux Miner with a server configuration file and datapacks: what each option does, where
its limits are, what cannot be changed without editing the code, and how to check that a change really took effect.

This guide is for modpack creators, server administrators and modders. You do not need to know the mod's code to use
it, but you should know how to edit a text file and how to put a folder inside a world save.

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.255 |
| Mod ID | `dimensionalfluxminer` |
| Datapack format | `48` |

Other references: gameplay numbers in [BALANCE.md](BALANCE.md), code layout in [ARCHITECTURE.md](ARCHITECTURE.md),
the player-facing overview in the [README](../README.md).

> [!NOTE]
> Every option, field and limit in this guide was checked against the mod's code. If something is not listed here,
> assume it is **not** configurable.

---

## 📚 Contents

**Part I: Server configuration**

1. [Four ways to change the mod](#1-four-ways-to-change-the-mod)
2. [The server configuration file](#2-the-server-configuration-file)
   - [2.1 Dimensional Flux Miner](#21-dimensional-flux-miner)
   - [2.2 Dimensional Crusher](#22-dimensional-crusher)
   - [2.3 Alloy Smelter](#23-alloy-smelter)
   - [2.4 Dimensional Death](#24-dimensional-death)
   - [2.5 Complete example](#25-complete-example)

**Part II: Datapacks**

3. [Datapack basics](#3-datapack-basics)
4. [Resource Profiles: the ores the Miner makes](#4-resource-profiles-the-ores-the-miner-makes)
5. [Dimensional Matter values](#5-dimensional-matter-values)
6. [Recipes](#6-recipes)
7. [Dimensional Death block tags](#7-dimensional-death-block-tags)
8. [Entity tags](#8-entity-tags)
9. [Compatibility with other mods](#9-compatibility-with-other-mods)

**Part III: Limits**

10. [What can and cannot be changed](#10-what-can-and-cannot-be-changed)
11. [Internal limits and invalid values](#11-internal-limits-and-invalid-values)

**Part IV: Examples and troubleshooting**

12. [A complete example datapack](#12-a-complete-example-datapack)
13. [Applying and checking changes](#13-applying-and-checking-changes)
14. [Troubleshooting](#14-troubleshooting)

---

# Part I: Server configuration

## 1. Four ways to change the mod

| Mechanism | What it is | What it changes in this mod | Needs a restart? |
|---|---|---|---|
| **Server configuration** | The file `dimensionalfluxminer-server.toml` | Energy costs, speed and FE/t of every level, alloy recipe types of other mods, Dimensional Death switches and protections | No, in most cases (see [2](#when-a-change-is-applied)) |
| **Datapack** | A folder or zip of JSON files loaded with the world | Ores the Miner makes, Dimensional Matter values, recipes, block and entity tags | No: `/reload` |
| **Resource pack** | A folder or zip of client assets | Textures, models, sounds and translations only. No gameplay value can be changed with it | No: `F3 + T` |
| **Java code** | Editing and rebuilding the mod | Everything else: the number of levels, buffers, slots, Dimensional Matter rules, corruption depth and radius, effects | Rebuild the mod |

> [!WARNING]
> Never edit the files inside the mod's `.jar`. Your changes are lost on every update, they cannot be shared as a
> separate file, and a broken file can stop the mod from loading. Everything a datapack can do, it does from outside
> the jar.

## 2. The server configuration file

### Where it is

NeoForge creates `dimensionalfluxminer-server.toml` the first time the mod loads a world or a server starts. It is a
**server** configuration: the server's copy is the one that counts, and players joining a server receive its values
(the in-game guide shows the server's numbers).

| Environment | Main file (shared by every world of the instance) | Override for one world |
|---|---|---|
| Singleplayer | `<instance>/config/dimensionalfluxminer-server.toml` | `<instance>/saves/<world>/serverconfig/dimensionalfluxminer-server.toml` |
| Dedicated server | `<server>/config/dimensionalfluxminer-server.toml` | `<server>/<level-name>/serverconfig/dimensionalfluxminer-server.toml` |
| Development (this repository) | `run/config/dimensionalfluxminer-server.toml` | `run/saves/<world>/serverconfig/` or `run/world/serverconfig/` |

A file placed in a world's `serverconfig` folder overrides the main file for that world only. NeoForge writes a
`readme.txt` there that explains the same rule.

### When a change is applied

- The mod reads its values every time it needs them and listens to NeoForge's configuration reload events, so a saved
  change is used as soon as NeoForge reloads the file. If you edit the file while the game or server is stopped, it is
  read at the next start. On a production server, restarting after editing is the safest way to be sure.
- Changes never require a new world. Machines already placed use the new values on their next cycle.
- One exception: `smelter.externalAlloyRecipeTypes` is read when the recipes are loaded, so run `/reload` (or restart)
  after changing it.

### What happens with an invalid value

NeoForge checks every entry against its type and range when it loads the file. An entry that is out of range or of the
wrong type is corrected back to its default and a warning is written to the log. Read the log after editing. Section
[11](#11-internal-limits-and-invalid-values) lists the extra checks the mod makes itself.

### All options

| Key | Type | Default | Minimum | Maximum |
|---|---|---|---|---|
| `miner.minimumProductionRate` | decimal | `0.0125` | `0.0001` | `64.0` |
| `miner.maximumProductionRate` | decimal | `64.0` | `0.0001` | `64.0` |
| `miner.energyPerAttempt` | integer (FE) | `200000` | `1` | `1000000000` |
| `crusher.energyPerOperation` | integer (FE) | `20000` | `1` | `1000000000` |
| `smelter.alloyEnergyPerOperation` | integer (FE) | `30000` | `1` | `1000000000` |
| `smelter.furnaceEnergyPerThreeItems` | integer (FE) | `50000` | `1` | `1000000000` |
| `smelter.externalAlloyRecipeTypes` | list of recipe type IDs | `["enderio:alloy_smelting", "oritech:foundry"]` | empty list | n/a |
| `miner.speedByLevel`, `crusher.speedByLevel`, `smelter.speedByLevel` | empty, or a list of exactly 10 decimals (operations per tick) | `[]` | `0.0001` each | `64` each |
| `miner.powerByLevel`, `crusher.powerByLevel`, `smelter.furnacePowerByLevel`, `smelter.alloyPowerByLevel` | empty, or a list of exactly 10 integers (FE/t) | `[]` | `1` each | `64000000000` each |
| `corruption.enabled` | true / false | `true` | n/a | n/a |
| `corruption.blockedDimensions` | list of dimension IDs | `[]` | n/a | n/a |
| `corruption.respectSpawnProtection` | true / false | `true` | n/a | n/a |
| `corruption.protectedRegions` | list of `"<dimension>,<x1>,<z1>,<x2>,<z2>"` | `[]` | n/a | n/a |
| `corruption.claimFailurePolicy` | `"deny"` or `"ignore"` | `"deny"` | n/a | n/a |

### How the ten speed levels work

Every machine has a speed selector from 1 to 10.

**The default curve (what a player gets without changing anything).** Only the two ends of the speed range exist as
numbers, and the eight levels in between are computed from them:

```text
operations(n) = minimum × (maximum / minimum) ^ ((n − 1) / 9)        n = 1 … 10
FE/t(n)       = operations(n) × energy per operation                  rounded to the nearest FE
buffer(n)     = 10 × FE/t(n)
```

So by default the cost of one operation is the same at every level: a faster level does more operations per tick and
asks for more FE/t in exactly the same proportion. A machine only pays for the operations it really performs, and with
less FE than the level needs it works proportionally slower.

**Level tables (for modpacks).** Each machine also accepts two optional tables that set every level by hand:

| Table | Replaces | Unit |
|---|---|---|
| `<machine>.speedByLevel` | The speed curve | Operations per tick of each level (the Smelter: per lane, for a 10-second recipe) |
| `<machine>.powerByLevel` (Smelter: `furnacePowerByLevel` and `alloyPowerByLevel`) | The fixed cost per operation | FE/t of each level at full speed, without the Efficiency Upgrade |

- **A table must list all ten levels**, from level 1 to level 10. An empty table (`[]`, the default) keeps the
  default curve. A table with any other number of entries is ignored as a whole, the default curve is used and the log
  says `Config <key> lists N levels instead of 10`. A level is never half configured.
- Each table works alone or together with the other:

  | Speed table | Power table | Speed of level n | FE/t of level n | Cost of one operation at level n |
  |---|---|---|---|---|
  | no | no | default curve | speed × cost | the configured cost, same at every level |
  | yes | no | `speedByLevel[n]` | speed × cost | the configured cost, same at every level |
  | no | yes | default curve | `powerByLevel[n]` | `powerByLevel[n] / speed(n)` |
  | yes | yes | `speedByLevel[n]` | `powerByLevel[n]` | `powerByLevel[n] / speedByLevel[n]` |

- With a power table, a machine still pays only for the work it really does, at the cost of the level it is running at.
- The levels do not have to grow: any value in range is accepted for any level, and the order is yours to balance.
- The energy buffer is still 10 × the FE/t of the selected level, and the Efficiency Upgrade still halves the Crusher
  and Smelter costs.
- The guide's speed tables and the machines' screens show the configured values.

### 2.1 Dimensional Flux Miner

| Key | What it does | Effect of raising it |
|---|---|---|
| `miner.minimumProductionRate` | Mining attempts per tick at speed 1 (`0.0125` = one attempt every 4 seconds) | Speed 1 and every middle level get faster and use more FE/t |
| `miner.maximumProductionRate` | Mining attempts per tick at speed 10 | Speed 10 and the middle levels get faster. It can be lowered, but never above `64` |
| `miner.energyPerAttempt` | FE paid by every attempt: attempts that find an ore, attempts lost because the output is full, attempts discarded by the filter and attempts lost to the void | Every level uses more FE/t; production does not change |
| `miner.speedByLevel` | Attempts per tick of each level (10 values, each at most 64). Replaces the two rates above | |
| `miner.powerByLevel` | FE/t of each level (10 values). Replaces `energyPerAttempt` | |

**Interactions.**

- `minimumProductionRate` must be lower than `maximumProductionRate`. If it is not, both are ignored and the
  defaults are used (the file is not rewritten, so check your values).
- The FE/t of speed 10 is `maximumProductionRate × energyPerAttempt`. The defaults give `64 × 200 000 = 12 800 000 FE/t`.
- With `speedByLevel`, the two rates are not used. With `powerByLevel`, `energyPerAttempt` is not used.
- The Efficiency Upgrade never changes the Miner's energy.

**Example: a cheaper and slower Miner.**

```toml
[miner]
	minimumProductionRate = 0.01
	maximumProductionRate = 32.0
	energyPerAttempt = 100000
```

Speed 1 then makes 0.2 attempts per second for 1 000 FE/t, and speed 10 makes 640 attempts per second for 3 200 000 FE/t.

**Example: every level by hand.** Cheap early levels and an expensive top level (the cost per attempt of level 10 is
`20 000 000 / 64 = 312 500 FE`, of level 1 `500 / 0.0125 = 40 000 FE`):

```toml
[miner]
	speedByLevel = [0.0125, 0.03, 0.08, 0.2, 0.5, 1.5, 4.0, 10.0, 25.0, 64.0]
	powerByLevel = [500, 1500, 5000, 15000, 50000, 200000, 700000, 2000000, 6000000, 20000000]
```

**Not configurable** (fixed in the code): the number of levels (10), the hard cap of 64 attempts per logical cycle, the energy
buffer of 10 × FE/t, the 27 output slots, the 5 000 DM buffer, the cost of 2 DM per attempt and the 64 DM of a
drained column. The cores have no configuration. The Basic core's drain is controlled by the Dimensional Death options
([2.4](#24-dimensional-death)).

### 2.2 Dimensional Crusher

| Key | What it does | Effect of raising it |
|---|---|---|
| `crusher.energyPerOperation` | FE paid per **input item** crushed, not per product | Every level uses more FE/t; speed does not change |
| `crusher.speedByLevel` | Operations per tick of each level (10 values, each at most 64) | |
| `crusher.powerByLevel` | FE/t of each level without the upgrade (10 values). Replaces `energyPerOperation` | |

- The default speed is 1 operation every 200 ticks at speed 1 up to 64 operations per tick at speed 10. With the
  default cost that is 100 FE/t at speed 1 and 1 280 000 FE/t at speed 10. `speedByLevel` replaces it.
- The Efficiency Upgrade halves the cost per operation, also with a power table. This factor is fixed.
- How many products an ore gives (2, or 3 with the upgrade) is **not** in the configuration file. It comes from the
  `ore_crushing` recipes, which a datapack can change ([6.2](#62-ore-crushing)). Ores handled by the common-tag
  fallback always give 2, or 3 with the upgrade.

```toml
[crusher]
	energyPerOperation = 40000
```

```toml
[crusher]
	speedByLevel = [0.01, 0.03, 0.1, 0.3, 1.0, 2.0, 4.0, 8.0, 16.0, 32.0]
	powerByLevel = [200, 600, 2000, 6000, 20000, 40000, 80000, 160000, 320000, 640000]
```

### 2.3 Alloy Smelter

The Smelter has two energy profiles, and each job is charged with the profile of the work it really does:

| Profile | Used by | Cost | Key |
|---|---|---|---|
| **Furnace** | Each lane that smelts an item with a blast furnace or furnace recipe | `furnaceEnergyPerThreeItems / 3` FE per input item, only on the lanes that work | `smelter.furnaceEnergyPerThreeItems` |
| **Alloy** | One execution of an alloy recipe | `alloyEnergyPerOperation × work units`, where the work units are the largest ingredient count of the recipe | `smelter.alloyEnergyPerOperation` |

The three modes use those profiles like this:

| Mode | What runs | What it pays |
|---|---|---|
| Furnace Only | Every lane smelts on its own | Furnace profile |
| Alloy Only | Only alloy recipes | Alloy profile |
| Alloy + Furnace (default) | An alloy when the lanes hold one, otherwise each lane smelts | The profile of the job that ran; never both for one job |

- Defaults: 50 000 / 3 FE per smelted item and 30 000 FE per alloy work unit. At speed 10, three lanes smelting a
  standard 10-second recipe use 3 200 000 FE/t, and alloys use 1 920 000 FE/t.
- The Efficiency Upgrade halves both profiles, also with power tables. This factor is fixed.
- The default speed: at speed 10 every lane runs 64 operations per tick, for a recipe of any duration. The duration of
  a recipe changes how fast lower levels are and the FE/t, not the cost of one operation.

**Level tables of the Smelter.**

| Key | What it sets |
|---|---|
| `smelter.speedByLevel` | Operations per lane per tick of each level **for a standard 10-second (200-tick) recipe**. A recipe of another duration keeps the proportion of the default curve: `speedByLevel[n] × R(n, T) / R(n, 200)`, never above 64 per lane |
| `smelter.furnacePowerByLevel` | FE/t of each level with the three lanes smelting standard recipes. The cost of one item is `furnacePowerByLevel[n] / (3 × lane speed)`. Replaces `furnaceEnergyPerThreeItems` |
| `smelter.alloyPowerByLevel` | FE/t of each level running a standard alloy (one unit of each ingredient, 200 ticks). The cost of one work unit is `alloyPowerByLevel[n] / lane speed`. Replaces `alloyEnergyPerOperation` |

```toml
[smelter]
	speedByLevel = [0.005, 0.02, 0.06, 0.2, 0.6, 2.0, 6.0, 16.0, 32.0, 64.0]
	furnacePowerByLevel = [300, 1200, 3600, 12000, 36000, 120000, 360000, 960000, 1920000, 3840000]
	alloyPowerByLevel = [200, 800, 2400, 8000, 24000, 80000, 240000, 640000, 1280000, 2560000]
```

**Alloy recipes of other mods.** `smelter.externalAlloyRecipeTypes` lists recipe **types** whose recipes the Smelter
also runs as alloys:

```toml
[smelter]
	externalAlloyRecipeTypes = ["enderio:alloy_smelting", "oritech:foundry"]
```

- An entry is a recipe type ID (`<mod id>:<type>`), not a recipe ID. Types of mods that are not installed are ignored.
- A recipe is taken only when it has 1 to 3 item ingredients (counts allowed) and item results. A second result becomes
  the byproduct, always with a 100% chance. Recipes with fluids, more than 3 ingredients or no item result are skipped,
  and so are plain copies of furnace recipes.
- The mod reads the recipe's own data, so it can only find the fields it knows: inputs in `inputs`, `ingredients` or
  `input0` to `input2`; results in `output`, `result`, `results` or `outputs`; `experience`; the time in `time`,
  `processing_ticks`, `processingTime` or `duration` (100 ticks when none is found). Other recipe formats are skipped.
  Whether a given type works therefore depends on the other mod's format. The two defaults are the ones the mod was
  written for.
- The mod's own `alloying` recipes are always tried first.
- An empty list (`[]`) turns the feature off.

### 2.4 Dimensional Death

These options control the corruption caused by the **Basic Dimensional Core**. The Stabilized core never corrupts
anything and is not affected by them.

| Key | What it does |
|---|---|
| `corruption.enabled` | `false` stops all corruption. The Basic core then finds no land to drain and gets no Dimensional Matter: it never gets matter without corrupting. Corrupted Water also stops spreading |
| `corruption.blockedDimensions` | Dimensions where nothing is ever corrupted and Corrupted Water does not spread, e.g. `["minecraft:overworld"]` |
| `corruption.respectSpawnProtection` | Never corrupts inside the spawn protection: a square around the world spawn, in the Overworld only. On a dedicated server its radius is `spawn-protection` from `server.properties` (0 protects nothing). In singleplayer and LAN worlds the radius is always Minecraft's built-in 16 blocks |
| `corruption.protectedRegions` | Rectangles that are never corrupted. Each entry is `"<dimension>,<x1>,<z1>,<x2>,<z2>"` in block coordinates, both corners included, from the bottom to the top of the world |
| `corruption.claimFailurePolicy` | When FTB Chunks or Open Parties and Claims is installed but cannot be queried: `"deny"` stops all corruption until it works again, `"ignore"` acts as if it were not installed |

```toml
[corruption]
	enabled = true
	blockedDimensions = ["minecraft:the_nether"]
	respectSpawnProtection = true
	protectedRegions = ["minecraft:overworld,-500,-500,500,500", "minecraft:overworld,2000,1000,2400,1300"]
	claimFailurePolicy = "deny"
```

**Claims** (no option needed). When FTB Chunks or Open Parties and Claims is installed, the Basic core only corrupts
unclaimed land or claims where the Miner's owner may build. A claim of someone else, or a claim check that fails, is
never corrupted and gives no Dimensional Matter. Every block of a column is checked before anything changes. Corrupted
Water never flows or sinks across a claim border or into a protected region.

**Not configurable** (fixed in the code):

- the survey radius of 300 blocks, in loaded chunks only;
- the survey budget of 256 columns per tick per Miner and 2 048 per tick per dimension;
- the depth of a column: up to 3 water layers and 3 solid blocks, through up to 32 blocks of cover;
- the 64 DM of a column;
- Corrupted Water damage and healing;
- the hostile spawns around corrupted land.

Which blocks and creatures are affected is controlled by tags, see [7](#7-dimensional-death-block-tags) and
[8](#8-entity-tags).

> [!NOTE]
> Turning the corruption off or protecting an area never reverts land that is already corrupted.

### 2.5 Complete example

The full file with its default values:

```toml
[miner]
	minimumProductionRate = 0.0125
	maximumProductionRate = 64.0
	energyPerAttempt = 200000
	speedByLevel = []
	powerByLevel = []

[crusher]
	energyPerOperation = 20000
	speedByLevel = []
	powerByLevel = []

[smelter]
	externalAlloyRecipeTypes = ["enderio:alloy_smelting", "oritech:foundry"]
	alloyEnergyPerOperation = 30000
	furnaceEnergyPerThreeItems = 50000
	speedByLevel = []
	furnacePowerByLevel = []
	alloyPowerByLevel = []

[corruption]
	enabled = true
	blockedDimensions = []
	respectSpawnProtection = true
	protectedRegions = []
	claimFailurePolicy = "deny"
```

To go back to the defaults, delete the file (or the line) and NeoForge writes it again with the default values.

---

# Part II: Datapacks

## 3. Datapack basics

### Structure

A datapack is a folder (or a `.zip` of that folder) with a `pack.mcmeta` file and a `data` folder:

```text
My_DFM_Datapack/
├── pack.mcmeta
└── data/
    ├── my_modpack/                          ← your own namespace: new files
    │   ├── dimensional_miner_resources/
    │   └── recipe/
    └── dimensionalfluxminer/                ← the mod's namespace: files that extend or replace the mod's own
        ├── data_maps/item/matter_value.json
        ├── recipe/
        └── tags/
```

`pack.mcmeta` for Minecraft 1.21.1 (data pack format `48`):

```json
{
  "pack": {
    "pack_format": 48,
    "description": "My Dimensional Flux Miner tweaks"
  }
}
```

### Which namespace to use

| Content | Namespace of the folder | Why |
|---|---|---|
| Resource Profile entries | Any, prefer your own (`data/my_modpack/dimensional_miner_resources/`) | Every file of every namespace is read |
| Dimensional Matter values | Always `data/dimensionalfluxminer/data_maps/item/matter_value.json` | The path is the data map's ID |
| New recipes | Your own (`data/my_modpack/recipe/`) | A new ID never collides with the mod's |
| Replacing or removing a mod recipe | `data/dimensionalfluxminer/recipe/<same path>` | Only the same ID replaces it |
| The mod's tags | `data/dimensionalfluxminer/tags/<block or entity_type>/<name>.json` | The path is the tag's ID |
| Common tags (`c:`) | `data/c/tags/...` | The same rule |

### Installing and enabling

1. Put the folder or zip in the world's `datapacks` folder:
   - singleplayer: `<instance>/saves/<world>/datapacks/`
   - dedicated server: `<server>/<level-name>/datapacks/`
2. In a running world, run `/datapack list` to see it, then `/datapack enable "file/My_DFM_Datapack"` if it is not
   enabled yet. A new datapack is usually enabled automatically on the next load.
3. Run `/reload`, or restart.

For a new singleplayer world you can also add it in the **Data Packs** screen when creating the world.

### What `/reload` does

`/reload` reloads every datapack of the server. For this mod it reloads:

- the Resource Profiles: profiles are rebuilt the next time a Miner needs them, and filter selections of families that
  no longer exist are dropped;
- the Dimensional Matter values (also sent to the clients, so the guide follows);
- every recipe, including the conversion of other mods' alloy recipes;
- every tag.

It does **not** reload:

- the server configuration file (section [2](#when-a-change-is-applied));
- world generation. Ores that physically generate and dimension definitions only change in newly generated chunks,
  or after a restart for new dimensions;
- blocks that are already in the world. Land corrupted before a tag change stays corrupted, and land that was skipped
  can now be corrupted.

### Priority and conflicts

- Datapacks are applied in the order shown by `/datapack list`. A pack enabled later has higher priority, and the mod's
  own data has the lowest. Use `/datapack enable "file/<name>" last` (or `first`) to choose.
- **Same file path, replaced:** recipes and Resource Profile files. The file of the highest-priority pack is the only
  one used.
- **Same file path, merged:** tags and data maps. The lists of every pack are combined, unless a file sets
  `"replace": true`, which drops what lower-priority packs defined.
- Two packs that change the same thing in different files (two recipes for the same input, two Resource Profile
  entries for the same ore) do not override each other in a defined order. Avoid it: change one ID in one place.

## 4. Resource Profiles: the ores the Miner makes

A **Resource Profile** is the list of ore families the Miner can produce in one dimension and the chance of each.

> [!IMPORTANT]
> A Resource Profile only changes what the **Miner** produces. It never changes which ores generate in the world.

### 4.1 How the mod builds a profile

When a Miner first needs the profile of its dimension, the mod reads the dimension's real world generation:

1. It collects the ore features of every biome the dimension can generate (the underground ore and underground
   decoration steps).
2. It reads each ore feature: vanilla ore features directly, and features of other mods when their configuration lists
   `targets[].state`. The vein size is the feature's `size`, or 8 when the size is not part of its data.
3. Each feature gets a raw score: `vein size × placements per chunk × rarity chance × share of biomes that have it`,
   split evenly among the blocks it places.
4. Each placed block's item joins a **family**:

   | Ore | Family key |
   |---|---|
   | Vanilla ore blocks | `minecraft:coal`, `minecraft:iron`, `minecraft:copper`, `minecraft:gold`, `minecraft:redstone`, `minecraft:lapis`, `minecraft:diamond`, `minecraft:emerald`, `minecraft:quartz`, `minecraft:nether_gold`, `minecraft:ancient_debris` |
   | Items in `c:ores/<material>` | `c:<material>` (or `minecraft:<material>` when the material is one of the vanilla names above) |
   | Anything else | Not an ore: ignored |

5. Every family needs a Crusher product (see [4.5](#45-families-without-a-crusher-product)). A family without one is
   left out and a warning is logged.
6. Datapack entries are applied (see below), then the weights are normalized, then the void is applied.

So any mod whose ores are in the common tag `c:ores/<material>` and use standard ore features appears in the
profile with no datapack at all.

### 4.2 Weights and normalization

```text
weight basis = raw score ^ 0.75
weight       = round(basis / sum of bases × 10 000)        at least 1 per family
```

- The weights of a profile add up to about **10 000**, so a weight of 500 is about a 5% chance per attempt.
- The exponent 0.75 keeps rare ores present without inflating them. It is fixed in the code.
- Inside a family, the variant (for example normal or deepslate ore) is chosen by its own generation score.
- The Miner's filter window shows each family's key and weight in its tooltip.

### 4.3 Entry fields

Files go in `data/<namespace>/dimensional_miner_resources/<name>.json`. There are two kinds of entry.

**Resource entry**

| Field | Required | Type | Meaning | When omitted |
|---|---|---|---|---|
| `resource` | Yes | Item ID | The ore item the entry is about | Entry rejected |
| `dimension` | To add an ore: yes. To change or remove: no | Dimension ID | The dimension the entry applies to | Applies to every dimension, but can only change families already present there |
| `family` | No | Family key (`namespace:path`) | The family the ore joins | The ore's own family ([4.1](#41-how-the-mod-builds-a-profile)), or the item ID when it has none |
| `weight` | No | Number from 0 to 1 | Exact share of the profile reserved for the family (0.05 = 5%) | The family keeps its generated weight |
| `enabled` | No | true / false | `false` removes the ore from the profile; a family left with no ore disappears | `true` |
| `crusher_output` | No | Item ID | The family's Crusher product, also used by the Crusher for that ore item | Resolved from recipes and tags |

Rules worth knowing:

- **`weight` is a share, not a raw weight.** The families with a share get exactly it; the others split the rest in
  their own proportions. If the shares add up to more than 1, they are capped at 100%. `weight: 0` does not remove a
  family (it keeps the minimum weight of 1); use `enabled: false`.
- **An ore added by a datapack and not generated in the dimension has no raw score**, so without `weight` it gets the
  minimum weight of 1 (about 0.01%). Always give `weight` to a family you add.
- An ore added to a family that already exists joins it with a variant weight of 1 raw point. That is often much less
  than the generated variants, and it cannot be set.
- The same ore listed by several entries: the result depends on loading order, which is not guaranteed. Keep one
  entry per ore and dimension.
- An unknown `resource` item logs a warning and the entry is skipped. An unknown `crusher_output` item is ignored.
- These files do **not** support `neoforge:conditions`. An entry for an ore of a mod that is not installed is simply
  skipped with a warning, which is harmless.

**Dimension rule (the void)**: an entry that has `empty_share`.

| Field | Required | Type | Meaning | When omitted |
|---|---|---|---|---|
| `dimension` | Yes | Dimension ID | The dimension | Entry rejected |
| `empty_share` | Yes | Number from 0 to 0.9999 | Share of the attempts that find nothing when the dimension has a single family | Entry rejected |
| `min_families` | No | Integer from 1 to 1000 | From this many families on, there is no void | `5` |
| `curve` | No | `"geometric"`, `"linear"` or `"step"` | How the void shrinks from 1 family to `min_families` | `"geometric"` |

- Attempts lost to the void cost their FE and 2 DM and give no filter recovery. The families share the rest of the
  attempts in their own proportions.
- `geometric`: the chance of finding something is multiplied by the same factor for every extra family. `linear`: the
  void shrinks by the same amount per family. `step`: the full void below `min_families`, none from it on.
- The mod ships one rule, for the End:
  `data/dimensionalfluxminer/dimensional_miner_resources/the_end.json` with `empty_share` 0.97, `min_families` 5,
  `geometric` (97%, 92.8%, 82.7% and 58.4% void with 1 to 4 families).
- Only one rule per dimension is used. To change the End's rule, replace that file by putting a file with the same
  path in your datapack. `"empty_share": 0` turns the void off.

### 4.4 File formats

A file can hold one entry, a list, or an object with an `entries` list. All three are equivalent:

```json
{ "resource": "minecraft:diamond_ore", "dimension": "minecraft:overworld", "weight": 0.03 }
```

```json
[
  { "resource": "minecraft:diamond_ore", "dimension": "minecraft:overworld", "weight": 0.03 },
  { "resource": "minecraft:emerald_ore", "dimension": "minecraft:overworld", "enabled": false }
]
```

```json
{
  "entries": [
    { "resource": "minecraft:diamond_ore", "dimension": "minecraft:overworld", "weight": 0.03 },
    { "resource": "minecraft:emerald_ore", "dimension": "minecraft:overworld", "enabled": false }
  ]
}
```

An invalid entry is skipped with a warning naming the file. The other entries of the file still load.

### 4.5 Families without a Crusher product

Every family must have a Crusher product, because everything the Miner makes must be processable. The product is
found in this order:

1. `crusher_output` of a datapack entry;
2. otherwise what the Crusher makes from the family's first ore: a `crushing` or `ore_crushing` recipe, or the
   common tags `c:ores/<m>` → `c:raw_materials/<m>`, otherwise `c:gems/<m>`.

When nothing is found, the family is left out of the profile and the log says:
`Resource family <key> in <dimension> has no resolvable Crusher output; add a crusher_output override. Family excluded.`

### 4.6 Examples

> The IDs of the form `examplemod:...` are placeholders: replace them with the real IDs of the mod you use. All the
> other IDs are real.

**1. Add an ore of another mod to a dimension.** Create adds zinc ore in the Overworld, and its tag `c:ores/zinc`
already makes it appear. To add it where it does not generate (here, the Nether) with a 4% share:

```json
{
  "resource": "create:zinc_ore",
  "dimension": "minecraft:the_nether",
  "weight": 0.04
}
```

The family key is `c:zinc` (from the tag) and the Crusher product is `create:raw_zinc` through `c:raw_materials/zinc`.

**2. Add an ore to a custom dimension.** A modded dimension with no ore features the mod can read gets no profile at
all, so each ore must be added. Shares are needed, since nothing generates there:

```json
[
  { "resource": "minecraft:iron_ore", "dimension": "examplemod:crystal_caves", "weight": 0.6 },
  { "resource": "minecraft:gold_ore", "dimension": "examplemod:crystal_caves", "weight": 0.3 },
  { "resource": "minecraft:diamond_ore", "dimension": "examplemod:crystal_caves", "weight": 0.1 }
]
```

**3. Remove an ore from a dimension.** `enabled: false` removes an ore item. A family disappears only when all of its
ores are gone, so remove both emerald ores:

```json
[
  { "resource": "minecraft:emerald_ore", "dimension": "minecraft:overworld", "enabled": false },
  { "resource": "minecraft:deepslate_emerald_ore", "dimension": "minecraft:overworld", "enabled": false }
]
```

Without `dimension`, the same entries remove emerald from every dimension.

**4. Control the share of a family.** Diamonds in the Overworld at exactly 2% of the attempts (about 200 of 10 000),
the other families sharing the remaining 98%:

```json
{
  "resource": "minecraft:diamond_ore",
  "dimension": "minecraft:overworld",
  "weight": 0.02
}
```

**5. Put the variants of one ore in the right family.** A mod whose ores are not tagged `c:ores/<material>` would make
one family per ore item. Give them the same `family` so they become one family with two variants:

```json
[
  { "resource": "examplemod:tin_ore", "dimension": "minecraft:overworld", "family": "c:tin", "weight": 0.05 },
  { "resource": "examplemod:deepslate_tin_ore", "dimension": "minecraft:overworld", "family": "c:tin" }
]
```

Use the key the mod would choose itself (`c:<material>`), so the entries merge with any tagged ore of the same material.
Untagged ores also have no common-tag Crusher product, so this family still needs one, otherwise it is left out:
add `crusher_output` (example 6) or an `ore_crushing` recipe.

**6. Choose the Crusher product with `crusher_output`.**

```json
{
  "resource": "examplemod:tin_ore",
  "dimension": "minecraft:overworld",
  "family": "c:tin",
  "weight": 0.05,
  "crusher_output": "examplemod:raw_tin"
}
```

The Crusher then gives 2 `examplemod:raw_tin` per ore (3 with the upgrade) whenever no recipe and no common tag
already handle that ore. For a different count, use an `ore_crushing` recipe ([6.2](#62-ore-crushing)).

**7. Change the void of a dimension.** A lighter void in the End, which disappears from three families on:

```json
{
  "dimension": "minecraft:the_end",
  "empty_share": 0.8,
  "min_families": 3,
  "curve": "linear"
}
```

Save it as `data/dimensionalfluxminer/dimensional_miner_resources/the_end.json` so it replaces the mod's own rule.

## 5. Dimensional Matter values

The Stabilized core turns the items pushed into its Matter Input into **Dimensional Matter (DM)**. The value of each
item comes from the data map `dimensionalfluxminer:matter_value`.

### 5.1 File and format

The file is always `data/dimensionalfluxminer/data_maps/item/matter_value.json`. The copies of every datapack are
merged: a datapack only needs the entries it adds or changes.

```json
{
  "replace": false,
  "values": {
    "minecraft:cobblestone": { "dm": 0.3, "dimensions": ["minecraft:overworld"] },
    "#c:ores/tin": { "dm": 1.0 }
  },
  "remove": ["minecraft:dirt"]
}
```

| Field | Where | Meaning |
|---|---|---|
| `replace` | Top level, optional | `true` drops every value defined by lower-priority packs (the mod's included) before applying this file |
| `values` | Top level, required | Keys are item IDs or item tags (`#namespace:path`), values are the entry objects |
| `remove` | Top level, optional | Items or tags whose value is removed |
| `dm` | Entry, required | The value in DM, from 0 to 5000 |
| `dimensions` | Entry, optional | The dimensions where the item is accepted. Missing or empty: every dimension |
| `neoforge:conditions` | Entry, optional | Load the entry only when the conditions pass, e.g. `neoforge:mod_loaded` |

An entry can also be written as `{ "value": { "dm": 1.0 }, "replace": true }`. For this data map it behaves the same
as the short form: the later value replaces the earlier one.

### 5.2 Precision

The mod stores DM in integer units of **0.1 DM** (1 DM = 10 units). Every `dm` is rounded to the nearest 0.1:

| `dm` written | Stored | Notes |
|---|---|---|
| `0.3` | 0.3 DM | |
| `0.25` | 0.3 DM | rounded half up |
| `0.04` | 0 | **not accepted** as matter |
| `0` | 0 | **not accepted** as matter |

Use values that are multiples of 0.1. A value of 0 (or one that rounds to 0) makes the Matter Input refuse the item.

### 5.3 Rules the data map cannot change

- **Ores.** An ore (an item in `#c:ores`, or a vanilla ore block) is accepted only in a dimension whose Resource Profile
  contains that exact item. Its `dimensions` field is ignored. An ore with no entry is worth 0.5 DM.
- **Ores the Miner can make** never give more than 2 DM, the cost of the attempt that made them, even with the
  Efficiency Upgrade.
- **Variants.** Plain blocks made from accepted items in a stonecutter, a furnace or a crafting table (stairs, slabs,
  polished, bricks, mossy...) are accepted automatically in the same dimension, worth the matter spent divided by the
  items made, rounded down. An item with its own entry keeps its entry. Blocks with a block entity and anything made
  from ores never count.
- **Efficiency Upgrade.** Each injected item gives `value + min(value, 0.2 DM)`. For example cobblestone 0.3 → 0.5,
  obsidian 1.0 → 1.2.
- **Filter recovery.** With the upgrade, an ore discarded by the Miner's filter returns `min(value + 0.5, 2) DM`, where
  the value is the ore's data map value or 0.5.
- **Fixed in the code:** the 5 000 DM buffer, the 2 DM per attempt and the 64 DM per drained column. A datapack cannot
  change them. An item worth more than the free room of the buffer waits until there is room.

### 5.4 Examples

**1. Cobblestone worth more.**

```json
{
  "values": {
    "minecraft:cobblestone": { "dm": 0.4, "dimensions": ["minecraft:overworld"] }
  }
}
```

Keep `dimensions`: an entry replaces the whole previous value, so leaving it out would make cobblestone accepted
everywhere.

**2. Obsidian worth less, still accepted everywhere.**

```json
{
  "values": {
    "minecraft:obsidian": { "dm": 0.5 }
  }
}
```

**3. A material only accepted in the Nether.**

```json
{
  "values": {
    "minecraft:crying_obsidian": { "dm": 1.5, "dimensions": ["minecraft:the_nether"] }
  }
}
```

**4. A material accepted in every dimension.**

```json
{
  "values": {
    "minecraft:tuff": { "dm": 0.5 }
  }
}
```

**5. Materials of an optional mod.** The condition avoids load errors when the mod is missing:

```json
{
  "values": {
    "create:limestone": {
      "neoforge:conditions": [
        { "type": "neoforge:mod_loaded", "modid": "create" }
      ],
      "dm": 0.5,
      "dimensions": ["minecraft:overworld"]
    }
  }
}
```

**6. Ore values by tag.**

```json
{
  "values": {
    "#c:ores/tin": { "dm": 1.0 },
    "#c:ores/lead": { "dm": 1.0 }
  }
}
```

Remember that ores are only accepted where they generate, whatever `dimensions` says. To check where an ore is accepted,
check the dimension's Resource Profile.

**7. Remove a value.**

```json
{
  "values": {},
  "remove": ["minecraft:dragon_egg"]
}
```

## 6. Recipes

### 6.1 Recipe types of the mod

| Type | Used by | Fields |
|---|---|---|
| `dimensionalfluxminer:crushing` | Crusher, fixed result | `ingredient`, `result` |
| `dimensionalfluxminer:ore_crushing` | Crusher, ore yield (2, or 3 with the upgrade) | `ingredient`, `result` **or** `result_tag`, `base_count`, `efficiency_upgrade_count` |
| `dimensionalfluxminer:alloying` | Alloy Smelter, alloy profile | `ingredients`, `result`, `byproduct`, `processing_ticks`, `experience` |
| `dimensionalfluxminer:tag_smelting` | Furnace and the Smelter's lanes | `ingredient`, `result_tag`, `experience`, `cookingtime` |
| `dimensionalfluxminer:tag_blasting` | Blast furnace and the Smelter's lanes | `ingredient`, `result_tag`, `experience`, `cookingtime` |

The mod's own recipes are in `data/dimensionalfluxminer/recipe/`. Their IDs follow the path:
`ore_processing/iron.json` is the recipe `dimensionalfluxminer:ore_processing/iron`.

**What the Crusher does with an item**, in order:

1. a `crushing` recipe;
2. an `ore_crushing` recipe;
3. an item in `c:ores/<m>` gives 2 (3 with the upgrade) of the first item of `c:raw_materials/<m>`, or of `c:gems/<m>`;
4. a Resource Profile `crusher_output` for that ore item, 2 (3);
5. an item in `c:ingots/<m>`, `c:raw_materials/<m>` or `c:gems/<m>` gives 1 item of `c:dusts/<m>`.

"First item of a tag" means vanilla first, then this mod, then the other mods in alphabetical order of their IDs.

**What a Smelter lane does with an item:** the item's blast furnace recipe, otherwise its furnace recipe (vanilla
recipes, other mods' recipes and `tag_smelting` / `tag_blasting` alike). The raw ore bonus (2 raw → 3 ingots, 1 → 2
with the upgrade) applies only when the input is in `c:raw_materials/<m>` and the single result is in `c:ingots/<m>`.

### 6.2 Ore crushing

| Field | Required | Type | Meaning |
|---|---|---|---|
| `ingredient` | Yes | Ingredient (`{"item": ...}`, `{"tag": ...}` or a list of them) | The ore |
| `result` | One of the two | Item stack (`{"id": ...}`) | A fixed product. Its `count` is ignored: the counts below decide |
| `result_tag` | One of the two | Item tag ID | The product is the first item of the tag, so it follows whatever mod provides it |
| `base_count` | No | Integer ≥ 1 | Products per ore without the upgrade. Default `2` |
| `efficiency_upgrade_count` | No | Integer ≥ 1 | Products per ore with the upgrade. Default `3` |

Use `result_tag` when several mods may provide the product (raw tin, for example) and `result` for one specific item.
A recipe with both fields, or with neither, fails to load. A `result_tag` with no items makes the recipe inactive
until a mod fills the tag.

**Add an ore of another mod** (`data/my_modpack/recipe/ore_crushing/zinc.json`):

```json
{
  "neoforge:conditions": [
    { "type": "neoforge:mod_loaded", "modid": "create" }
  ],
  "type": "dimensionalfluxminer:ore_crushing",
  "ingredient": { "tag": "c:ores/zinc" },
  "result_tag": "c:raw_materials/zinc",
  "base_count": 2,
  "efficiency_upgrade_count": 3
}
```

**Replace an existing one**: diamonds give 1, or 2 with the upgrade. Same path as the mod's recipe:
`data/dimensionalfluxminer/recipe/ore_processing/diamond.json`

```json
{
  "type": "dimensionalfluxminer:ore_crushing",
  "ingredient": { "tag": "c:ores/diamond" },
  "result": { "id": "minecraft:diamond" },
  "base_count": 1,
  "efficiency_upgrade_count": 2
}
```

### 6.3 Alloying

| Field | Required | Type | Meaning |
|---|---|---|---|
| `ingredients` | Yes | List of 1 to 3 `{"ingredient": <ingredient>, "count": <n>}` | What the three lanes must hold. `count` defaults to 1 |
| `result` | Yes | Item stack `{"id": ..., "count": ...}` | Main result, into output 1 |
| `byproduct` | No | `{"id": ..., "count": ..., "chance": ...}` | Second result, into output 2. `count` defaults to 1, `chance` from 0 to 1 defaults to 1 and is rolled on every operation |
| `processing_ticks` | No | Integer ≥ 1 | Duration at the reference speed. Default `100` |
| `experience` | No | Number ≥ 0 | XP per operation, stored as Liquid Experience. Default `0` |

How a recipe is matched:

- each ingredient must be in a **different** lane, in any order;
- every lane that holds an item must be used by the recipe, so an extra item in a third lane prevents a two-ingredient
  alloy;
- one operation consumes `count` of each ingredient;
- an ingredient count above 64 can never be met, since a lane holds at most 64 items;
- the energy of one operation is `alloyEnergyPerOperation × the largest count`.

A list with 0 or more than 3 ingredients fails to load. When several alloy recipes match the same lanes, which one runs
is not defined, so avoid overlaps.

**Three ingredients, a main result and a byproduct with a 25% chance**
(`data/my_modpack/recipe/alloying/gilded_blackstone.json`):

```json
{
  "type": "dimensionalfluxminer:alloying",
  "ingredients": [
    { "ingredient": { "item": "minecraft:blackstone" }, "count": 1 },
    { "ingredient": { "tag": "c:nuggets/gold" }, "count": 4 },
    { "ingredient": { "item": "minecraft:magma_cream" }, "count": 1 }
  ],
  "result": { "id": "minecraft:gilded_blackstone", "count": 1 },
  "byproduct": { "id": "minecraft:gold_nugget", "count": 1, "chance": 0.25 },
  "processing_ticks": 200,
  "experience": 0.5
}
```

This recipe has 4 work units (its largest count), so with the default settings one operation costs 120 000 FE.

### 6.4 Crushing, tag smelting and tag blasting

**`crushing`**: a fixed result, no ore bonus. The Efficiency Upgrade still halves the energy, but not the count.

| Field | Required | Type |
|---|---|---|
| `ingredient` | Yes | Ingredient |
| `result` | Yes | Item stack `{"id": ..., "count": ...}` |

```json
{
  "type": "dimensionalfluxminer:crushing",
  "ingredient": { "item": "minecraft:blaze_rod" },
  "result": { "id": "minecraft:blaze_powder", "count": 3 }
}
```

Because `crushing` is checked first, a `crushing` recipe on an ore replaces its ore yield.

**`tag_smelting` and `tag_blasting`**: furnace and blast furnace recipes whose result is the first item of a tag (in
the tag's own order), resolved when the recipe is used. The mod uses them to smelt its dusts into whichever ingot the
installed mods provide.

| Field | Required | Type | Default |
|---|---|---|---|
| `ingredient` | Yes | Ingredient | |
| `result_tag` | Yes | Item tag ID | |
| `experience` | No | Number | `0` |
| `cookingtime` | No | Integer (ticks) | `200` (smelting), `100` (blasting) |

The recipe does nothing while the tag is empty. Add the same condition the mod uses so it does not even load:

```json
{
  "neoforge:conditions": [
    { "type": "neoforge:not", "value": { "type": "neoforge:tag_empty", "tag": "c:ingots/tin" } }
  ],
  "type": "dimensionalfluxminer:tag_blasting",
  "ingredient": { "item": "dimensionalfluxminer:dust_tin" },
  "result_tag": "c:ingots/tin",
  "experience": 0.7,
  "cookingtime": 100
}
```

### 6.5 Replacing and removing recipes

- **Replace**: put a file with the **same path** as the original, in the original's namespace. For example
  `data/dimensionalfluxminer/recipe/alloying/steel.json` replaces the Pure Steel recipe. The crafting recipes of the
  machines (`alloy_smelter`, `dimensional_crusher`, `steel_chassis`, ...) are vanilla shaped recipes and can be
  replaced the same way.
- **Add**: use your own namespace, `data/my_modpack/recipe/...`.
- **Remove**: replace the file with a recipe whose condition never passes. NeoForge then skips it:

  ```json
  {
    "neoforge:conditions": [
      { "type": "neoforge:false" }
    ]
  }
  ```

- **Avoid conflicts**: two recipes of the same type that accept the same input are not chosen in a defined order.
  Replace the existing recipe instead of adding a second one.

> [!WARNING]
> Removing the recipe of an item the progression needs (Pure Steel, the cores, the Miner parts) can make the Miner
> impossible to build. Replace it instead.

## 7. Dimensional Death block tags

The corruption of the Basic core is driven by block tags in `data/dimensionalfluxminer/tags/block/`.

| Tag | What it controls | Kind |
|---|---|---|
| `dimensionalfluxminer:corruptible_dirt` | Becomes **Corrupted Dirt** (default: dirt) | Conversion |
| `dimensionalfluxminer:corruptible_grass` | Becomes **Corrupted Grass** (default: grass block) | Conversion |
| `dimensionalfluxminer:corruptible_stone` | Becomes **Corrupted Stone** | Conversion |
| `dimensionalfluxminer:corruptible_sediment` | Becomes **Corrupted Sediment** (sands, gravels) | Conversion |
| `dimensionalfluxminer:corruptible_terracotta` | Becomes **Corrupted Terracotta** | Conversion |
| `dimensionalfluxminer:corruptible_weeds` | A plant standing on corrupted ground becomes a corrupted grass plant | Conversion (vegetation) |
| `dimensionalfluxminer:corruptible_flowers` | A plant standing on corrupted ground becomes a wither rose (default: `#minecraft:flowers`) | Conversion (vegetation) |
| `dimensionalfluxminer:corruption_ground` | Where a column's ground starts: the corruption looks through anything else above it (trees, buildings, machines), up to 32 blocks | Classification |
| `dimensionalfluxminer:corruption_blacklist` | Never converted | Protection of block types |
| `dimensionalfluxminer:corrupted_terrain` | Already corrupted: never converted again; the only ground for the extra hostile spawns; undead do not burn in daylight on it; corrupted grass plants can stand on it | Classification |

**Choosing what a block becomes.** Each conversion tag names its result, so adding a block to a tag is how you
choose which corrupted block replaces it:

| Add the block to | It becomes |
|---|---|
| `corruptible_dirt` | `dimensionalfluxminer:corrupted_dirt` |
| `corruptible_grass` | `dimensionalfluxminer:corrupted_grass` |
| `corruptible_stone` | `dimensionalfluxminer:corrupted_stone` |
| `corruptible_sediment` | `dimensionalfluxminer:corrupted_sediment` (falls like sand) |
| `corruptible_terracotta` | `dimensionalfluxminer:corrupted_terracotta` |

Put a block in **one** conversion tag only. If it is in several, the first match wins, in this order: sediment, dirt,
grass, stone, terracotta. To stop a block from being corrupted, remove it from its tag (with `"replace": true`, see
example 5) or add it to `corruption_blacklist`, which always wins.

Rules fixed in the code:

- Water, kelp and seagrass become Corrupted Water and its plants. This is the only conversion without a tag.
- Blocks with a block entity are never converted.
- Only full blocks in the top 3 solid layers are converted.
- The blacklist, block entities and already corrupted blocks are checked before any conversion tag.

`corruption_ground` already includes every block conversion tag (`corruptible_dirt`, `corruptible_grass`,
`corruptible_stone`, `corruptible_sediment`, `corruptible_terracotta`), so a block added to any of them is also
treated as ground.

> [!IMPORTANT]
> `corruption_blacklist` protects **block types** everywhere. It is not territorial protection: to protect places, use
> claims or the server options of [2.4](#24-dimensional-death).

Tag files are **merged** with the mod's own tags. Keep `"replace": false` to add entries; `"replace": true` drops every
entry of lower-priority packs, the mod's included. Use `{"id": "...", "required": false}` for blocks of optional
mods: without it, a missing block makes the whole tag fail to load.

**1. Let a stone of another mod be corrupted** (`data/dimensionalfluxminer/tags/block/corruptible_stone.json`):

```json
{
  "replace": false,
  "values": [
    { "id": "create:limestone", "required": false },
    { "id": "create:scoria", "required": false }
  ]
}
```

**2. Add a sand or sediment** (`corruptible_sediment.json`):

```json
{
  "replace": false,
  "values": [
    "minecraft:suspicious_sand",
    { "id": "examplemod:ash_sand", "required": false }
  ]
}
```

Note that `minecraft:suspicious_sand` has a block entity, so it would still be skipped. It is only here to show the
rule; prefer plain blocks.

**3. Protect a block type** (`corruption_blacklist.json`):

```json
{
  "replace": false,
  "values": [
    "minecraft:calcite",
    { "id": "examplemod:sacred_stone", "required": false }
  ]
}
```

**Corrupt other dirts into Corrupted Dirt and Corrupted Grass** (`corruptible_dirt.json` and `corruptible_grass.json`):

```json
{
  "replace": false,
  "values": [
    "minecraft:coarse_dirt",
    "minecraft:rooted_dirt"
  ]
}
```

```json
{
  "replace": false,
  "values": [
    "minecraft:podzol",
    "minecraft:mycelium"
  ]
}
```

**4. Recognize a new kind of ground**, so the corruption stops on it instead of looking through it
(`corruption_ground.json`):

```json
{
  "replace": false,
  "values": [
    "minecraft:mud",
    "minecraft:moss_block"
  ]
}
```

This only changes where the column starts. Mud and moss are not converted, because they are in no conversion tag.

**5. Replace a tag completely**: only these blocks become Corrupted Stone, whatever the mod lists
(`corruptible_stone.json`):

```json
{
  "replace": true,
  "values": [
    "minecraft:stone",
    "minecraft:deepslate"
  ]
}
```

## 8. Entity tags

In `data/dimensionalfluxminer/tags/entity_type/`:

| Tag | What it controls | Default |
|---|---|---|
| `dimensionalfluxminer:blight_light_bypass_eligible` | Monsters that may spawn in light during the extra spawns around corrupted land | zombie, zombie villager, skeleton, stray, spider, creeper, enderman, witch |
| `dimensionalfluxminer:blight_spawn_blacklist` | Never spawned by those extra spawns | Ender Dragon, Wither, Warden |
| `dimensionalfluxminer:corrupted_water_heal_blacklist` | Monsters Corrupted Water never heals | Ender Dragon, Wither |
| `dimensionalfluxminer:corrupted_water_heart_heal` | Monsters healed one heart (2 HP) every 10 ticks instead of 1 HP every 20 ticks | Warden |

Who is hurt or healed is **not** a tag:

- players (outside Creative and Spectator) and every non-monster mob take 2 HP every 40 ticks in Corrupted Water;
- monsters are healed.

The extra spawns run every 40 ticks near players, with the biome's normal monster list, the mob cap and the normal
spawn rules. The tags above only choose among them. Damage, healing amounts, intervals and spawn rates are fixed in
the code.

**Example**: a modded monster may spawn in light, and a modded boss is never healed.

`blight_light_bypass_eligible.json`:

```json
{
  "replace": false,
  "values": [
    { "id": "examplemod:shade", "required": false }
  ]
}
```

`corrupted_water_heal_blacklist.json`:

```json
{
  "replace": false,
  "values": [
    { "id": "examplemod:ancient_titan", "required": false }
  ]
}
```

## 9. Compatibility with other mods

The mod has no scripting integration of its own. Everything below uses standard Minecraft and NeoForge data.

| Goal | How |
|---|---|
| Ores of another mod in the Miner | Automatic when they are in `c:ores/<material>` and use standard ore features. Otherwise a Resource Profile entry ([4.6](#46-examples)) |
| Process another mod's ores | Automatic through `c:ores/<m>` → `c:raw_materials/<m>` / `c:gems/<m>`. For other counts or products, an `ore_crushing` recipe ([6.2](#62-ore-crushing)) |
| Dust of another mod's material | Automatic when a `c:dusts/<m>` item exists. To smelt a dust back, a `tag_smelting` / `tag_blasting` recipe ([6.4](#64-crushing-tag-smelting-and-tag-blasting)) |
| Canonical products | When a tag has several items, the mod picks vanilla first, then itself, then the others alphabetically. To force one item, use `result` instead of `result_tag` |
| Alloys of another mod | Add its recipe type to `smelter.externalAlloyRecipeTypes` ([2.3](#23-alloy-smelter)), or write `alloying` recipes |
| A custom dimension | Resource Profile entries with its `dimension` ID, matter values with `dimensions` |
| Another mod's blocks or creatures in the corruption | The tags of [7](#7-dimensional-death-block-tags) and [8](#8-entity-tags) with `"required": false` |
| Claims | FTB Chunks and Open Parties and Claims are detected automatically |

**Identifiers** are always `namespace:path`:

- items: `create:zinc_ore`;
- blocks: `create:limestone`;
- dimensions: `minecraft:the_nether`, `allthemodium:the_other`;
- tags: `#c:ores/zinc` in data maps, `"tag": "c:ores/zinc"` in ingredients, `c:raw_materials/zinc` in `result_tag`.

**When the other mod may be missing:**

| File | Safe way |
|---|---|
| Recipes | `"neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "<mod>" }]` at the top of the recipe |
| Data map entries | The same `neoforge:conditions` inside the entry |
| Tags | `{ "id": "<id>", "required": false }` |
| Resource Profile entries | No conditions. An entry with an unknown item is skipped with a warning |
| Server configuration | Types and dimensions of missing mods are ignored |

Tools that write recipes, tags or data maps in the standard formats (KubeJS, CraftTweaker and similar) work with
these formats like any datapack would, but the mod has no direct integration with them and they are not tested.

---

# Part III: Limits

## 10. What can and cannot be changed

**Legend.**

- ✅ supported directly
- 🟡 supported partially (see the notes)
- ❌ not supported by that mechanism
- 🔧 needs a code change

| Feature | TOML | Datapack | Code | Notes |
|---|---|---|---|---|
| Energy per operation of each machine | ✅ | ❌ | | One cost per machine (two for the Smelter), or a cost per level through a power table |
| FE/t of each level | ✅ | ❌ | | `powerByLevel` tables, all ten levels |
| Speed of each level, every machine | ✅ | ❌ | | `speedByLevel` tables, all ten levels; the Miner also through its two rates. Never above 64 per tick |
| Speed curve | ✅ | ❌ | | Geometric by default; any shape through a speed table |
| Number of levels | ❌ | ❌ | 🔧 | Always 10 |
| Operations per tick | ✅ | 🟡 | | Speed tables; a Smelter recipe's duration changes the lower levels |
| Products per ore | ❌ | 🟡 | 🔧 | `ore_crushing` counts; the tag fallback is fixed at 2 / 3 |
| Efficiency Upgrade effects | ❌ | 🟡 | 🔧 | Ore counts by recipe. The energy halving, the raw bonus and the DM bonus are fixed |
| Buffer sizes (energy, DM) | ❌ | ❌ | 🔧 | Energy 10 × FE/t, DM 5 000 |
| DM per mining attempt | ❌ | ❌ | 🔧 | 2 DM |
| DM value of each item | ❌ | ✅ | | `matter_value` data map |
| Accepted matter per dimension | ❌ | 🟡 | | `dimensions` for non-ores; ores follow the Resource Profile |
| Ores the Miner makes | ❌ | ✅ | | Resource Profiles |
| Family chances | ❌ | 🟡 | 🔧 | Shares per family and the void by datapack; the 0.75 exponent is fixed |
| Crusher outputs | ❌ | ✅ | | Recipes, tags, `crusher_output` |
| Alloy recipes | 🟡 | ✅ | | TOML only picks other mods' recipe types |
| Crafting recipes | ❌ | ✅ | | Vanilla recipe files |
| Which blocks corrupt, and into what | ❌ | ✅ | | One tag per corrupted result (dirt, grass, stone, sediment, terracotta) plus weeds and flowers; only water is fixed. New corrupted result blocks need code |
| Corruption blacklist | ❌ | ✅ | | `corruption_blacklist` |
| Corruption radius, depth, speed | ❌ | ❌ | 🔧 | 300 blocks, 3 + 3 layers, survey budget |
| Corruption on / off, per dimension, regions | ✅ | ❌ | | Section [2.4](#24-dimensional-death) |
| Claim protection | 🟡 | ❌ | 🔧 | Automatic; TOML only sets the failure policy. Other claim mods need code |
| Affected creatures | ❌ | 🟡 | 🔧 | Exceptions by tag; damage, healing and categories are fixed |
| Corrupted Water | 🟡 | ❌ | 🔧 | Stopped by the corruption switches; its behaviour is fixed |
| Models, textures, sounds, translations | ❌ | ❌ | | Resource pack |
| Animations | ❌ | ❌ | 🔧 | |
| Ports, slots and automation | ❌ | ❌ | 🔧 | Side Configuration in game; slot counts fixed |
| Other mods' compatibility | 🟡 | ✅ | 🔧 | Data and tags; new claim systems or recipe formats need code |

## 11. Internal limits and invalid values

### Limits fixed in the code

| Limit | Value |
|---|---|
| Speed levels | 1 to 10 |
| Miner attempts per logical cycle | 64 at most (`maximumProductionRate` cannot exceed it) |
| Crusher speed (default curve) | 1 operation / 200 ticks to 64 operations / tick |
| Smelter speed (default curve) | 64 operations per lane per tick at speed 10 |
| Speed tables | exactly 10 values, each from 0.0001 to 64 operations per tick |
| Power tables | exactly 10 values, each from 1 to 64 000 000 000 FE/t |
| Energy per operation (config) | 1 to 1 000 000 000 FE |
| Energy buffer | 10 × the FE/t of the selected speed |
| Slots | 64 items each. Crusher: 1 input, 4 outputs. Smelter: 3 inputs, 3 outputs. Miner: 27 outputs |
| DM buffer | 5 000 DM |
| DM per attempt | 2 DM |
| DM per drained column | 64 DM |
| DM precision | 0.1 DM; `dm` from 0 to 5 000 |
| Survey | radius 300 blocks, loaded chunks, 256 columns / tick / Miner, 2 048 / tick / dimension |
| Corrupted column | up to 3 water layers + 3 solid blocks, through up to 32 cover blocks |
| Alloy recipe | 1 to 3 ingredients; `processing_ticks` ≥ 1; `chance` 0 to 1 |
| Profile weights | about 10 000 in total, at least 1 per family; `weight` 0 to 1 |
| Void | `empty_share` 0 to 0.9999; `min_families` 1 to 1000 |
| Multiblock | fixed 3 × 3 × 3 shape of 27 blocks |
| Extra hostile spawns | every 40 ticks, at most 64 attempts per dimension, within 128 blocks of a player and at least 24 away |

### What happens with a wrong value

| Where | Wrong value | Result |
|---|---|---|
| TOML | Out of range or wrong type | **Corrected**: NeoForge restores the default and logs a warning |
| TOML | `minimumProductionRate` ≥ `maximumProductionRate` | **Replaced at run time**: both defaults are used; the file keeps your values |
| TOML | Unknown recipe type or dimension in a list | **Ignored** |
| TOML | A level table with fewer or more than 10 entries | **Table ignored**: the default curve is used and the log says `lists N levels instead of 10` |
| Resource Profile | Invalid field or value | **Entry rejected**: a warning names the file; the other entries load |
| Resource Profile | Unknown item | **Entry skipped** with a warning |
| Resource Profile | Broken JSON | **File rejected**: `Couldn't parse data file` |
| Data map | `dm` outside 0 to 5 000, a field of the wrong type | **File rejected**: `Could not read data map ...` and none of that file's values apply |
| Data map | Unknown item ID | **Entry skipped**: `Object with ID ... doesn't exist` |
| Data map | `dm` that rounds to 0 | **Accepted but useless**: the Matter Input refuses the item |
| Recipe | Missing field, wrong type, both or neither of `result` / `result_tag`, 0 or 4+ ingredients | **Recipe rejected**: `Parsing error loading recipe ...` |
| Tag | Missing entry without `"required": false` | **Tag rejected** completely, which can disable the corruption of every block of that tag |

---

# Part IV: Examples and troubleshooting

## 12. A complete example datapack

This pack:

- gives the Overworld more diamonds;
- makes cobblestone worth more;
- adds an alloy recipe;
- lets calcite-like modded stone corrupt.

```text
My_DFM_Datapack/
├── pack.mcmeta
└── data/
    ├── my_modpack/
    │   ├── dimensional_miner_resources/
    │   │   └── overworld.json
    │   └── recipe/
    │       └── alloying/
    │           └── gilded_blackstone.json
    └── dimensionalfluxminer/
        ├── data_maps/
        │   └── item/
        │       └── matter_value.json
        └── tags/
            └── block/
                └── corruptible_stone.json
```

`pack.mcmeta`:

```json
{
  "pack": {
    "pack_format": 48,
    "description": "My modpack's Dimensional Flux Miner tweaks"
  }
}
```

`data/my_modpack/dimensional_miner_resources/overworld.json`:

```json
{
  "entries": [
    { "resource": "minecraft:diamond_ore", "dimension": "minecraft:overworld", "weight": 0.04 }
  ]
}
```

`data/dimensionalfluxminer/data_maps/item/matter_value.json`:

```json
{
  "values": {
    "minecraft:cobblestone": { "dm": 0.4, "dimensions": ["minecraft:overworld"] }
  }
}
```

`data/my_modpack/recipe/alloying/gilded_blackstone.json`:

```json
{
  "type": "dimensionalfluxminer:alloying",
  "ingredients": [
    { "ingredient": { "item": "minecraft:blackstone" }, "count": 1 },
    { "ingredient": { "tag": "c:nuggets/gold" }, "count": 4 },
    { "ingredient": { "item": "minecraft:magma_cream" }, "count": 1 }
  ],
  "result": { "id": "minecraft:gilded_blackstone", "count": 1 },
  "byproduct": { "id": "minecraft:gold_nugget", "count": 1, "chance": 0.25 },
  "processing_ticks": 200,
  "experience": 0.5
}
```

`data/dimensionalfluxminer/tags/block/corruptible_stone.json`:

```json
{
  "replace": false,
  "values": [
    { "id": "create:limestone", "required": false }
  ]
}
```

JSON files cannot hold comments: keep explanations in a separate README of your pack.

## 13. Applying and checking changes

| Change | Apply with | How to check |
|---|---|---|
| Server configuration | Save the file (or restart) | The guide's speed tables and the machines' power show the new FE/t; the log shows corrections |
| Install a datapack | Copy to `datapacks/`, then `/reload` | `/datapack list` shows it under "enabled" |
| Resource Profile | `/reload` | Open a Miner's filter window in that dimension: each family's key and weight are in the tooltip. The terminal's details show the void chance |
| Recipes | `/reload` | EMI or JEI show the Crusher and Alloy Smelter categories. Typing the ID in `/recipe give @s <id>` also helps: the command only suggests and accepts recipes that loaded |
| DM values | `/reload` | The guide's matter table lists every non-ore entry with its value with and without the upgrade. The Miner's details show the last injected item and the DM it gave |
| Tags | `/reload` | Press F3 and look at a block: its tags are listed on the right; or try the corruption in a test world |

- **Logs**: `logs/latest.log` (and `logs/debug.log`). Search for `dimensionalfluxminer`, `Couldn't parse`,
  `Parsing error loading recipe`, `Invalid dimensional miner`, `Could not read data map`, `doesn't exist` and
  `Couldn't load tag`.
- **Restart** after changing dimensions or world generation, and on a server when you are not sure a configuration
  reload happened.
- **Existing worlds**: changes never rewrite blocks already placed. Corrupted land stays corrupted, ores already in the
  Miner's output stay, and DM already in a buffer keeps its amount.
- **Go back**: disable the pack (`/datapack disable "file/<name>"`) and `/reload`; delete the configuration file to
  regenerate the defaults. Keep a copy of every file before editing it.

## 14. Troubleshooting

| Problem | Likely causes | What to check |
|---|---|---|
| An ore is missing from a profile | No `c:ores/<m>` tag and a non-standard ore feature; an `enabled: false` somewhere; no Crusher product | Log line `has no resolvable Crusher output`; add a Resource Profile entry with `dimension` and `weight` |
| An added ore never comes out | No `weight`, so the family got the minimum weight of 1 | Filter tooltip weight; add `weight` |
| An ore appears but the Crusher refuses it | No recipe, no `c:raw_materials/<m>` or `c:gems/<m>` item, no `crusher_output` | EMI / JEI Crushing category; add an `ore_crushing` recipe |
| `crusher_output` has no effect | Unknown item ID (ignored silently); a recipe or a common tag already handles the ore (they come first) | Check the ID; use an `ore_crushing` recipe to override |
| A recipe is not recognized | Wrong `type`, invalid field, false condition, wrong folder (`recipes` instead of `recipe`), two recipes for the same input | `Parsing error loading recipe` in the log; `/recipe give @s <id>` |
| A tag does not apply | Wrong folder (`blocks` instead of `block`), missing entry without `"required": false`, block with a block entity, block also in the blacklist or in an earlier conversion tag | `Couldn't load tag` in the log; F3 block tags |
| A DM value is still the old one | File not at `data/dimensionalfluxminer/data_maps/item/matter_value.json`; a higher-priority pack sets it; the whole file failed on one bad entry; no `/reload` | `Could not read data map` in the log; `/datapack list` order |
| A material is refused in a dimension | `dimensions` does not list it; it is an ore that does not generate there; its value rounds to 0; the DM buffer is full | Its entry; the dimension's profile; the DM bar |
| The configuration is not loaded | Edited the wrong copy (a world's `serverconfig` overrides `config/`); invalid value corrected; server not reloaded | Both paths of [2](#where-it-is); the log |
| A level table has no effect | It does not list exactly 10 values; a value out of range made NeoForge reset the list | `lists N levels instead of 10` or a correction warning in the log |
| A datapack has invalid JSON | Missing comma or quote, comments in the file | The log names the file; validate it with any JSON validator |
| Two datapacks conflict | Same recipe path in both (the higher one wins); different files for the same input or ore (undefined) | `/datapack list` order; keep one definition per ID |
| Errors when an optional mod is missing | Recipe without `neoforge:mod_loaded`; tag entry without `"required": false`; data map entry without a condition | The log line names the missing ID |
