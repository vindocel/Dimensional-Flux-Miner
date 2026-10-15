# Changelog

## 0.2.1 — unreleased (since 0.2.0-beta.4)

### ✨ Added

- 🟪 **Corrupted Terracotta**: Dimensional Death now turns every terracotta (plain, colored and glazed) into it; it
  counts as terracotta in recipes and tags. Calcite, dripstone blocks, Create's asurine and AE2's sky stone now
  corrupt into Corrupted Stone.
- 🛡️ **Claims protect against Dimensional Death.** With **FTB Chunks** or **Open Parties and Claims** installed (both
  optional), the Basic Core corrupts only unclaimed land or claims its owner may build in; a claim of someone else, or a
  check that fails or cannot decide, is left untouched and gives no DM. Every block of a column (terrain, water,
  plants, both halves of tall plants) is checked before anything changes, and an authorized column still gives 64 DM.
  Corrupted Water no longer flows or sinks across a claim border, even long after the Miner ran.
  - New server config: `corruption.enabled` (off = the Basic Core gets no matter), `corruption.blockedDimensions`,
    `corruption.respectSpawnProtection` (on), `corruption.protectedRegions` and `corruption.claimFailurePolicy`
    (`deny`: no corruption while an installed claim mod cannot be queried; `ignore`).
- 🕳️ **The void of the End.** Where few ore families are born (by default only the End), part of the Miner's attempts
  find nothing: they still cost their energy and 2 DM, and the ores share the rest (an End with a single modded ore no
  longer gives 100% of it). The void is 97% with one family and shrinks geometrically: about 93%, 83% and 58% with two,
  three and four, none from five families on. Datapacks set it per dimension with an entry
  `{"dimension", "empty_share", "min_families", "curve": "geometric" | "linear" | "step"}` in
  `dimensional_miner_resources`; the vanilla End, with no ores, still shows no resources. The Miner's details show the
  void chance.
- 🎚️ **Speed and energy of every level for modpacks.** New optional server config tables of all ten levels:
  `miner.speedByLevel`, `crusher.speedByLevel` and `smelter.speedByLevel` set the operations per tick of each level,
  and `miner.powerByLevel`, `crusher.powerByLevel`, `smelter.furnacePowerByLevel` and `smelter.alloyPowerByLevel` set
  its FE/t. They are empty by default, so nothing changes for players; a table that does not list the ten levels is
  ignored with a warning. Guide tables and machine screens follow them.
- 🟣 **Every terrain conversion of Dimensional Death is a datapack tag.** New tags `corruptible_dirt` (dirt →
  Corrupted Dirt), `corruptible_grass` (grass block → Corrupted Grass) and `corruptible_flowers` (flowers → wither
  rose) join the stone, sediment, terracotta and weed tags: the tag a block is in chooses the corrupted block that
  replaces it. Defaults are unchanged; only water stays fixed.
- 🔁 **Automation per material on the Alloy Smelter.** Items and Liquid Experience are automated apart: the item tab
  of the Side Configuration keeps OFF / Auto Input / Auto Eject / both, and the experience tab has its own Auto Eject,
  switched on and off by itself (liquid XP is only extracted, so it has no Auto Input). Existing Smelters keep ejecting
  XP if their Auto Eject was on.
- 📚 New documentation: the [Customization Guide](docs/CUSTOMIZATION.md) (every server option, datapack format,
  limit and example), [docs/BALANCE.md](docs/BALANCE.md) (every balance number) and
  [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) (code layout, tests, development rules). The README links to them.

### 🔄 Changed

- ⚡ **Fixed energy cost per operation.** Every operation costs the same FE at every speed, so a faster speed asks
  for more FE/t in exactly the same proportion (no discount, no penalty; the Miner's double cost at speeds 6–9 is gone).
  Production per speed is unchanged.
  - Miner: 200 000 FE per attempt (filtered and lost attempts included): 2 500 FE/t at speed 1 up to 12 800 000 FE/t
    at speed 10.
  - Crusher: 20 000 FE per input item (10 000 with the upgrade), not per product: 100 → 1 280 000 FE/t.
  - Alloy Smelter: the cost follows the job really done. Smelting: 50 000 / 3 FE per item, only on the lanes that
    work (250 FE/t at speed 1 with three lanes). Alloy: 30 000 FE per execution of one unit of each ingredient; a
    recipe asking for two units of an ingredient costs twice that (150 FE/t at speed 1 for a 10 s recipe). The
    upgrade halves both.
  - Fractions of FE are carried from tick to tick (no rounding drift), and the work in progress and that fraction
    survive a save and reload. Without enough FE a machine slows down in proportion and never works for free.
  - **Server config migration:** the old `miner/crusher/smelter.minimumFEPerTick` and `maximumFEPerTick` keys are
    replaced by `miner.energyPerAttempt`, `crusher.energyPerOperation`, `smelter.alloyEnergyPerOperation` and
    `smelter.furnaceEnergyPerThreeItems`. A custom power curve cannot be carried over: NeoForge removes the old keys
    (with a warning in the log) and the new costs start at their defaults. `miner.minimumProductionRate` and
    `maximumProductionRate` are kept.
- 🌀 **Dimensional Matter with the Efficiency Upgrade.** Every item in the Matter Input now gives `+ min(value, 0.2 DM)`
  instead of a flat +0.5 DM: dirt 0.1 → 0.2, cobblestone 0.3 → 0.5, stone 0.5 → 0.7, obsidian 1.0 → 1.2. Base values,
  the 5 000 DM buffer and the 2 DM per attempt are unchanged.
  - An ore the Miner can make in that dimension never gives back more than 2 DM (emerald and ancient debris stay at
    2 DM with the upgrade), so reinjecting the output can never create matter.
  - Recipe variants with the upgrade are worth their share of the upgraded block, so cutting a block into slabs no
    longer multiplies the bonus (a stone slab: 0.2 → 0.3 DM, two slabs never beat one stone).
  - A variant worth less than 0.1 DM per piece is no longer rounded up to 0.1 DM: coarse dirt (2 dirt + 2 gravel → 4)
    is not matter any more.
  - The filter recovery keeps its own rule: `min(value + 0.5, 2) DM`.
- ⚖️ **Ore distribution of the Miner.** Family weights now use the generation score to the power of 0.75 instead of a
  square root: every family is still present, but rare and endgame ores no longer get an outsized share. Vanilla
  Overworld diamond 7.6% → 5.3%, emerald 6.5% → 4.2%, iron 26.5% → 34.4%; Nether ancient debris 7.6% → 3.2%. Datapack
  share overrides are unchanged.
- 🚀 **Performance** (same production, energy, DM and corruption): the Miner's attempt loop allocates half as much and
  runs about twice as fast at speed 10 (one slot search per result instead of a simulated and a real insertion, one
  "changed" mark per cycle, a cached production curve, rolls by binary search with identical results); the survey
  checks columns without building a full plan and keeps its queue unboxed; claim checks ask a chunk-based claim mod
  once per chunk and read the server rules parsed once; the Alloy Smelter remembers furnace recipes per item, which
  makes automated insertion about five times cheaper.
- 🧩 Internal cleanup with no gameplay change: the Miner's output, matter buffer, attempts and metrics, the Alloy
  Smelter's energy, recipe caches and output planning, and the Corrupted Water conversion now live in their own
  classes. Saves, NBT and IDs are unchanged.

### 🐛 Fixed

- Alloy Smelter speed: speed 10 is **64 operations per lane per tick** (192 for the three furnace lanes, 64 standard
  alloys), no longer 192 per lane. Speed 10 uses 3 200 000 FE/t smelting with three lanes and 1 920 000 FE/t on alloys.
- Alloy Smelter slots hold at most **64 items** each, inputs and outputs alike, like every other machine slot.

### 🧪 Tests

- New GameTest suites for the energy per operation and the ten speeds, the level tables, the Smelter's 64 per lane,
  Dimensional Matter with the upgrade, ore distribution and the void, claims and server protections, the corruption
  tags, the exact equivalence of every optimization, and the Smelter's automation per material (including old saves);
  JUnit tests for the matter math, the ore weights, the void curve and the level tables. An opt-in benchmark
  (`DFM_BENCHMARK=1`). 125 GameTests in total.

## 0.2.0 — unreleased (since 0.2.0-beta.2)

### ✨ Added

- 📖 **Dimensional Flux Guide**: a new book, earned with your first Pure Steel (more copies: book + Pure Steel +
  Reinforced Eye of Ender, shapeless).
  - A 3D book model that stands upright; using it opens the book in the hand with an animation, it stays half
    open while you read, a leaf turns in your hand with every page and it closes when you leave the guide.
  - The guide is a real book: presentation and clickable contents on the first spread, chapters running page after
    page, page-turn animation and sounds, a violet ribbon back to the contents.
  - 9 chapters: introduction, materials and recipes, Dimensional Crusher, Alloy Smelter, building the Miner (with a
    rotatable 3D view, layer by layer, loose parts or formed), running the Miner, dimensions (what each dimension
    accepts and which core works there), speed and the Efficiency Upgrade, Dimensional Death.
  - Live data: crafting recipes from the server, speed tables and matter values from the server configuration and
    datapacks; item tooltips and buttons to the EMI / JEI recipes.
- 🏆 **20 advancements**, from the first Crusher to half of the drain area corrupted. "Forged in the Flux" (first Pure Steel)
  gives the guide; the hidden "World Breaker" rewards a Dimensional Efficiency Upgrade.
- ⛏️ The Miner now finds the ores of **Malum** (soulstone, brilliance, blazing quartz, cthonic gold, natural quartz)
  and all ores of **Forbidden Arcanus** (runic stone and its variants, Stella Arcanum); the Crusher doubles them
  into their own drops, except Stella Arcanum, which always gives a single Stellarite Piece.
- 🧱 New matter for the Stabilized core: in the Nether, glowstone, soul sand and soul soil (1 DM), nether bricks
  (1.5 DM), nylium and magma blocks (0.5 DM); **obsidian** (1 DM) in any dimension; with **Allthemodium**, ancient stone
  (0.5 DM) and ancient dirt (0.1 DM) in The Other.
  In the End, purpur blocks (0.5 DM) and the dragon egg (10 DM). Blocks made from accepted blocks in a stonecutter, a furnace or a crafting grid
  (polished, smooth, bricks, cracked, mossy, stairs, slabs, walls..., modded ones included) are accepted in the same
  dimension, worth the blocks spent divided by the items made.
- 🔥 With **Create** installed, the Alloy Smelter makes **Andesite Alloy** from one andesite and one iron or zinc nugget.

### 🔄 Changed

- ⚡ **New energy costs.** Miner: 2 500 FE/t at speed 1 up to 5 000 000 FE/t at speed 10; speeds 1–5 follow the
  geometric scale and speeds 6–9 cost double, so running fast is paid for (production per speed is unchanged).
  Crusher: 1 500 → 1 000 000 FE/t. Alloy Smelter: 2 500 → 2 000 000 FE/t. Existing worlds keep the values in their
  server config.
- 🔩 The mod's steel is now **Pure Steel** (*Aço Puro*): it no longer shares the common steel tags, and the mod's
  recipes only take Pure Steel, so steel from other mods cannot skip the Crusher and the Smelter.
- 💧 **Corrupted Water no longer waters land plants**: farmland next to it stays dry and sugar cane cannot grow beside
  it. Kelp and seagrass still live in it.
- ☠️ The corruption is now called **Dimensional Death** (*Morte Dimensional*) in game.
- 🏷️ The JAR name now includes the Minecraft version (`dimensionalfluxminer-<minecraft>-<version>.jar`).
- 🚢 Releases: tags may carry the Minecraft line (`v0.2.0-beta.3+1.21.1`), alphas and betas take their version from
  the tag, and a release must come from the branch of its Minecraft line (`mc/<version>`).
- 📘 README rewritten and reorganized.
- 🧹 Texture cleanup: each machine now reads its static textures from a single atlas holding only the regions its
  model uses (Alloy Smelter, Crusher, Miner); animated and state-dependent textures stay separate. Unused textures,
  models and OBJ geometry were removed (196 → 138 textures).

### 🐛 Fixed

- The Matter Input no longer takes ores of another dimension: an ore only becomes DM in a dimension where that exact ore
  generates (Overworld diamond ore is refused in the Nether), through the port and through the automatic input alike.
- A newly built Miner no longer lights up and plays its power-on sound before it receives any energy.
- EMI no longer reports the mod's block and entity tags as untranslated.

### 🧪 Tests

- New GameTests: guide recipe, advancement and reward loading, cobblestone counting, Corrupted Water never hydrating
  farmland or sugar cane (with water controls), an unpowered new Miner staying gray, Pure Steel not being common steel,
  Malum and Forbidden Arcanus ores, Create andesite alloy, matter accepted only in its own dimension. New JUnit tests for the "1 000 operations per second for
  5 seconds" streak. 70 GameTests in total.
