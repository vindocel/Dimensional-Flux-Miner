# Balance reference

Every number of Dimensional Flux Miner in one place. The values are the defaults of `dimensionalfluxminer-server.toml`
and the mod's own data; the in-game guide recomputes the tables from the server's configuration and datapacks.
Code layout is in [ARCHITECTURE.md](ARCHITECTURE.md).

## Speed and energy

Every machine has a speed selector from 1 to 10. The speed is geometric between Rate 1 and Rate 10:

```text
operations(n) = min * (max / min) ^ ((n - 1) / 9)
FE/t(n)       = operations(n) * energyPerOperation
```

The cost of one operation is the same at every level, so a faster level asks for more FE/t in exactly the same
proportion. A machine is charged only for the work it really does; fractions of FE carry over from tick to tick. With
less FE than the level needs, it works proportionally slower and never for free.

| Machine | Cost per operation | Config key |
|---|---|---|
| Miner | 200 000 FE per attempt (filtered and lost attempts included) | `miner.energyPerAttempt` |
| Crusher | 20 000 FE per input item | `crusher.energyPerOperation` |
| Electric Furnace, furnace | 50 000 / 3 FE per item | `electric_furnace.furnaceEnergyPerThreeItems` |
| Electric Furnace, alloy | 30 000 FE per work unit | `electric_furnace.alloyEnergyPerOperation` |

The Efficiency Upgrade halves the Crusher and Electric Furnace costs and leaves the Miner unchanged.

These are the mod's defaults. A modpack can replace the speed curve, the FE/t of every level, or both, with tables of
all ten levels (`speedByLevel`, `powerByLevel`): see the
[Customization Guide](CUSTOMIZATION.md#how-the-ten-speed-levels-work).

### The ten levels

Operations per tick, operations per second and FE/t without the upgrade. The Electric Furnace columns are for a standard
10 s recipe (200 ticks); the furnace column is three working lanes.

| Rate | Miner attempts/s | Miner FE/t | Crusher items/s | Crusher FE/t | Electric Furnace items/s per lane | Furnace FE/t (3 lanes) | Alloy FE/t |
|---|---|---|---|---|---|---|---|
| 1 | 0.25 | 2 500 | 0.10 | 100 | 0.10 | 250 | 150 |
| 2 | 0.65 | 6 458 | 0.29 | 286 | 0.29 | 715 | 429 |
| 3 | 1.67 | 16 681 | 0.82 | 818 | 0.82 | 2 045 | 1 227 |
| 4 | 4.31 | 43 089 | 2.34 | 2 339 | 2.34 | 5 848 | 3 509 |
| 5 | 11.13 | 111 302 | 6.69 | 6 690 | 6.69 | 16 725 | 10 035 |
| 6 | 28.75 | 287 505 | 19.13 | 19 133 | 19.13 | 47 833 | 28 700 |
| 7 | 74.27 | 742 654 | 54.72 | 54 719 | 54.72 | 136 798 | 82 079 |
| 8 | 191.83 | 1 918 350 | 156.49 | 156 494 | 156.49 | 391 234 | 234 740 |
| 9 | 495.53 | 4 955 288 | 447.56 | 447 562 | 447.56 | 1 118 905 | 671 343 |
| 10 | 1 280 | 12 800 000 | 1 280 | 1 280 000 | 1 280 | 3 200 000 | 1 920 000 |

- Miner: 0.0125 attempts per tick at Rate 1 (`miner.minimumProductionRate`) up to 64 per logical cycle at Rate 10
  (`miner.maximumProductionRate`, can be lowered, never raised above 64).
- Crusher: 1 operation every 200 ticks at Rate 1, 64 per tick at Rate 10.
- Electric Furnace: per lane `R(n,T) = (1/T) * (64*T)^((n-1)/9)` for a recipe of `T` ticks, so Rate 10 is always 64 operations
  per lane per tick (192 for the three lanes). The duration of a recipe changes the FE/t, not the cost per operation.
  An alloy recipe without a time uses 100 ticks.

### Energy buffer

```text
buffer = 10 * FE/t of the selected level (with the upgrade applied)
```

Lowering the level never deletes stored FE. Tick accelerators do not raise the buffer shown, but the energy input
also accepts what the extra cycles of one game time use.

## Electric Furnace

| Mode | Behaviour |
|---|---|
| Furnace Only | 3 independent lanes, each with the blast furnace recipe of its item, or the furnace one |
| Alloy + Furnace (default) | runs an alloy when the lanes hold one, otherwise every lane smelts on its own |
| Alloy Only | 1–3 counted ingredients make one operation with a main result and an optional byproduct |

- Furnace energy: `50 000 / 3 FE × input items`, only on the lanes that work (a raw pair 2 → 3 pays 2 items).
- Alloy energy: `30 000 FE × work units`, where the work units are the largest ingredient count of the recipe.
- One job is charged with one profile, never both, and never by the mode button.
- Raw ore bonus: 2 raw → 3 ingots; with the Efficiency Upgrade 1 raw → 2 ingots. Only for raw → ingot recipes with a
  single result.
- Slots: 3 inputs and 3 outputs, at most 64 items each. At Rate 10 the raw bonus is limited by the output: the 2 → 3
  pair makes 21 operations per lane (63 ingots), the upgraded 1 → 2 makes 32 (64 ingots); the rest waits in the input.
- Experience is stored as Liquid Experience, 1 XP = 20 mB. It is output-only, with its own Auto Eject apart from the
  items.

## Ore multipliers

| Path | Ingots per ore |
|---|---|
| Common furnace | 1 |
| Crusher + Electric Furnace | 3 |
| Crusher with upgrade + Electric Furnace | 4.5 |
| Crusher + Electric Furnace with upgrade | 4 |
| Both with upgrade | 6 |

The Crusher gives 2 products per ore, 3 with the upgrade (Stella Arcanum always gives one Stellarite Piece).

## Dimensional Matter

```text
buffer       = 5 000 DM (1 DM = 10 internal units)
one attempt  = 2 DM
one drained column (Basic core) = 64 DM
```

### Matter Injection values

| Dimension | Matter (DM) |
|---|---|
| Overworld | dirt 0.1; grass block 0.2; cobblestone, cobbled deepslate, sand, red sand 0.3; stone, deepslate, granite, diorite, andesite, terracotta (plain and the 16 colors) 0.5 |
| Nether | netherrack, basalt, blackstone, nylium, magma block 0.5; glowstone, soul sand, soul soil 1; nether bricks 1.5 |
| The End | end stone, purpur block 0.5; dragon egg 10 |
| The Other (Allthemodium) | ancient dirt 0.1; ancient stone 0.5 |
| Moon (Ad Astra) | moon cobblestone, moon sand 0.3; moon stone 0.5; soul soil 1 |
| Mars (Ad Astra) | mars cobblestone, mars sand, conglomerate 0.3; mars stone 0.5 |
| Venus (Ad Astra) | venus cobblestone, venus sand 0.3; venus stone, infernal spire block, basalt, blackstone, magma block 0.5 |
| Mercury (Ad Astra) | mercury cobblestone 0.3; mercury stone, basalt, blackstone, magma block 0.5 |
| Glacio (Ad Astra) | permafrost 0.1; glacio cobblestone, cobbled deepslate 0.3; glacio stone, deepslate 0.5 |
| Any dimension | obsidian 1 |
| Ores, only where they generate | 0.5; diamond and uranium 1; emerald, platinum, ancient debris 2 |

Blocks crafted, cut or smelted from accepted blocks are worth the matter spent divided by the items made, in the same
dimension. Values live in `data/<ns>/data_maps/item/matter_value.json`.

### Efficiency Upgrade on the Miner

```text
injected item: bonus = min(base, 0.2 DM); final = base + bonus
an ore the Miner itself can make in that dimension: final <= 2 DM (the cost of the attempt that made it)
filtered ore:  returns min(base + 0.5, 2) DM
```

Examples: dirt 0.1 → 0.2, cobblestone 0.3 → 0.5, stone 0.5 → 0.7, obsidian 1.0 → 1.2, emerald 2.0 → 2.0.
Reinjecting the output can never create matter.

## Ore distribution

- Each family's weight is its real generation score to the power of **0.75**, normalised (about 10 000 in total, at
  least 1 per family). Datapack overrides with `weight` reserve their exact share.
- **The void.** A dimension with fewer than `min_families` families (default 5) loses part of its attempts: they find
  nothing, cost FE and 2 DM, and give no filter recovery. The families share the rest in their own proportions.
- The void follows `curve` (default `geometric`): the full `empty_share` with one family, shrinking to 0 at
  `min_families`. Geometric means the chance to find something is multiplied by the same factor for every family.
  Also `linear` and `step`.
- Default: only the End, `empty_share` 0.97, `min_families` 5, geometric. With 1 to 4 families the void is 97%,
  92.8%, 82.7% and 58.4%; with 5 or more, none. The vanilla End has no ore family and shows "no resources".

## Dimensional Death

- The Basic core surveys the loaded chunks within 300 blocks, 256 columns per tick per Miner and 2 048 per tick per
  dimension; tick accelerators do not raise the budget.
- Per column: up to 3 contiguous water layers and 3 solid blocks, through up to 32 blocks of cover.
- The whole column is checked before anything changes; one protected position denies the column and gives no DM.

### Protections

| Server config key | Default | Effect |
|---|---|---|
| `corruption.enabled` | `true` | When false nothing is corrupted and the Basic core finds no matter |
| `corruption.blockedDimensions` | empty | Dimensions where nothing is corrupted |
| tag `#dimensionalfluxminer:corruption_blocked` (dimension types) | the Ad Astra planets and orbits | Same, by dimension type; the Basic core finds no matter there |
| `corruption.respectSpawnProtection` | `true` | The spawn-protection square of the Overworld is never corrupted |
| `corruption.protectedRegions` | empty | `"<dimension>,<x1>,<z1>,<x2>,<z2>"` rectangles never corrupted |
| `corruption.claimFailurePolicy` | `deny` | When an installed claim mod cannot be queried: `deny` corrupts nothing, `ignore` treats it as absent |

Claims: with FTB Chunks or Open Parties and Claims installed, only unclaimed land or claims where the Miner's owner may
build are corrupted (team members in FTB Chunks, the claim's access rules in Open Parties and Claims). Corrupted Water
never flows or sinks across a claim border, into a protected region, or where corruption is off.

## Dimensional Tanks

| | Value |
|---|---|
| Single tank | 1 chamber of 128 000 mB |
| Dual tank | 2 chambers of 128 000 mB each (A and B) |
| Energy | none |
| Auto Input / Auto Eject | no per-tick limit: as much as the neighbor gives or takes |
| Recipe (tank) | 4 Dimensional Steel Chassis, 2 Dimensional Ports, 2 Reinforced Eyes of Ender, 1 Reinforced Structural Glass |
| Recipe (dual tank) | 2 Dimensional Tanks, 4 Reinforced Eyes of Ender, 2 Pure Steel, 1 Dimensional Steel Chassis |

## Other server settings

| Key | Default | Effect |
|---|---|---|
| `electric_furnace.externalAlloyRecipeTypes` | `enderio:alloy_smelting`, `oritech:foundry` | Recipe types of other mods run as alloys (1–3 counted item ingredients, item results) |
| `tank.personalItem` | `minecraft:diamond` | Item that makes a Dimensional Tank's code personal |
| `tank.anarchyMode` | `false` | A broken personal tank drops as a public one plus the personal item |

## Compatibility

| Mod | What it adds |
|---|---|
| EMI, JEI | Recipe categories, R / U on machine slots |
| Jade | Item, energy and XP display |
| FTB Chunks, Open Parties and Claims | Claim protection against Dimensional Death |
| EnderIO, Oritech | Their alloy recipes in the Electric Furnace |
| Create | Andesite Alloy in the Electric Furnace; asurine corrupts into Corrupted Stone |
| AE2 | Sky stone corrupts into Corrupted Stone |
| Malum, Forbidden Arcanus | Their ores in the Miner and the Crusher |
| Allthemodium | Matter values for The Other |
| Ad Astra | Matter values for each planet; no Dimensional Death on the planets and in orbit |
| Any mod with ore features | Its ores in the Miner (features that declare `targets[].state`); the Crusher and Electric Furnace use the common tags |

Every integration is optional; none is needed to build or run the mod.
