# Architecture and development guide

How the code of Dimensional Flux Miner is organised, where each rule lives and how to work on it safely.
Gameplay numbers are in [BALANCE.md](BALANCE.md); releases are in [RELEASING.md](RELEASING.md).

## Packages

Everything lives under `src/main/java/dimensionalfluxminer`.

| Package | Responsibility |
|---|---|
| (root) | `DimensionalFluxMiner` (mod entry point, event wiring) and `DfmConfig` (server config, parsed corruption rules) |
| `registry` | Every deferred register: blocks, items, block entities, menus, fluids, recipes, data maps, tags, sounds, particles, creative tab |
| `machine` | What the single-block machines share: `AbstractMachineBlockEntity`, item queues, neighbour transfer, read-only views, redstone, quick install, tick acceleration bookkeeping |
| `machine.math` | Pure math, covered by JUnit: `RateScale` (Rate 1–10 curve and FE/t), `MinerProductionCurve`, `DynamicEnergyBuffer` (buffer and fractional FE debt), `WorkAccumulator` |
| `machine.side` | Side Configuration: relative faces, routes per transmission (item / energy / redstone), per-machine rules |
| `menu` | `MachineMenu` and the menu sync used by every screen |
| `crusher` | Dimensional Crusher: block, block entity, `CrusherProcessing` (recipe lookup and products), menu |
| `smelter` | Alloy Smelter: block entity plus the pieces below |
| `miner` | Dimensional Flux Miner: multiblock, controller and the pieces below |
| `miner.profile` | Resource Profiles: what the Miner can find in each dimension |
| `miner.survey` | The Basic core's survey of drainable columns |
| `miner.corruption` | Dimensional Death: terrain and water conversion, protection, effects, spawns |
| `recipe` | Recipe types (`crushing`, `ore_crushing`, `alloying`, `tag_smelting`, `tag_blasting`) and the reader of other mods' alloy recipes |
| `content` | Blocks, items and fluids that are not machines (corrupted blocks, Corrupted Water, guide book, Reinforced Eye of Ender) |
| `advancement` | Custom advancement trigger |
| `network` | Payloads (menu sync, Miner assembly outline, formation burst, configuration values set by scripts) |
| `compat` | Optional integrations, never loaded unless the other mod is present: `emi`, `jei`, `jade`, `claims` (FTB Chunks and Open Parties and Claims, by reflection), `kubejs` (server script events, loaded only by KubeJS through `kubejs.plugins.txt`) |
| `mixin` | Corrupted Water on entities and particles, sunlight on corrupted terrain |
| `client` | Renderers, screens, the guide, sounds, particles. Common code reaches it only through `machine.ClientHooks`; the EMI, JEI and Jade plugins are client side and may use it |
| `gametest` | In-world GameTests (see [Tests](#tests)) |

`src/test/java` holds the JUnit tests. They see no Minecraft class, so only pure math is tested there.

## The machines

### Crusher and Smelter

Both extend `AbstractMachineBlockEntity`, which owns the energy buffer, Rate, upgrade slot, the automation of each
material (Auto Input / Auto Eject of items, Auto Eject of fluids through `autoEjectFluidCycle`),
Side Configuration, redstone mode, client sync and drops. Each server tick runs `logicalCycle`; every extra ticker call
of the same game time (tick accelerators) runs one more full cycle. A subclass implements `processCycle`,
`autoInputCycle`, `autoEjectCycle`, `createItemHandler(route)` and `effectivePowerLimit`.

**Alloy Smelter** (`smelter`):

| Class | Responsibility |
|---|---|
| `AlloySmelterBlockEntity` | Slots, modes, lanes, the furnace and alloy processing of each cycle, XP, persistence |
| `SmelterEnergy` | Energy per profile: furnace FE per item, alloy FE per work unit, work units of a recipe, FE/t of a standard recipe |
| `SmelterRecipes` | Recipe caches: furnace recipe per item (cleared when the `RecipeManager` changes), last alloy, alloy ingredient set |
| `SmelterOutputPlanner` | Which of the three outputs a lane or an alloy writes to, and how much room is left |
| `SmelterMode`, `AutoSplit`, `BulkBonus`, `StoredExperience`, `ExperienceFluidHandler` | Mode rules, even spreading, raw-ore bonus, fixed-point XP and its fluid view |

**Dimensional Crusher** (`crusher`): `DimensionalCrusherBlockEntity` runs the cycle and `CrusherProcessing` finds the
products (explicit recipes first, then the common tags).

### Miner

The Miner is a 3×3×3 multiblock. Every part is a `MinerPartBlock`; the parts that need a block entity (ports, chassis)
use `MinerLinkBlockEntity`: the controller answers the capability of each of the nine port faces (`MinerPorts`), and the eight bottom blocks pass
tick acceleration to the controller.

| Class | Responsibility |
|---|---|
| `MinerControllerBlockEntity` | The controller in the terminal: formation and invalidation, ports, the cycle, Auto Input / Eject, visuals, menu, persistence |
| `MinerLayout`, `MinerStructure`, `MinerAssembly`, `MinerStructureEvents` | The 27-block layout, formation check, guided assembly, invalidation on block changes |
| `MinerAttempts` | The attempts of one cycle: pay 2 DM, roll, void, output, filter, filter recovery |
| `MinerOutput` | The 27 output slots: insertion slot, batch change notification, eject, NBT |
| `MatterBuffer` | The DM buffer in fixed point (1 DM = 10 units, 5 000 DM) |
| `MinerMetrics` | Potential and real rate, smoothed load, limiting factor per real tick |
| `MinerPorts` | The nine configurable faces of the lower layer: Item Output, Matter Input and energy faces, saved and synced; `client/render/MinerPortRenderer` paints the ports in code |
| `MatterValues`, `MatterMath`, `MatterVariants` | DM of an item, the Efficiency Upgrade bonus, values of crafted variants |
| `MinerStatus`, `MinerVisualStage` | Status and limiting factor shown on the screen, visual stage for the renderer |

**One cycle of the controller** (`cycle`):

1. Set the energy buffer target and acceleration headroom; run Auto Eject / Auto Input (also while stopped).
2. Check the core, run button, redstone, profile, survey and output room; stop with the matching status otherwise.
3. Work out the attempts of this cycle from `MinerProductionCurve` and the FE available (less FE, fewer attempts).
4. `MinerAttempts.run` performs them; with the Basic core it refills the DM buffer by draining a surveyed column.
5. Charge FE for the attempts executed (`DynamicEnergyBuffer.charge` keeps the fraction), record metrics, eject.

**Resource Profiles** (`miner.profile`): `ProfileBuilder` reads the real ore features of a dimension, `ProfileWeights`
turns generation scores into weights (exponent 0.75), datapack overrides (`ProfileOverride`) reserve exact shares and
`ProfileDimensionRule` + `VoidCurve` add the void of sparse dimensions. `ResourceProfile` is immutable; `ResourceProfiles`
caches one per dimension and rebuilds them on reload; it combines the datapack entries with those of server scripts. `rollIndex` is a binary search over cumulative weights.

**Survey** (`miner.survey`): `MinerSurvey` walks the loaded columns of the 300-block circle in `ColumnOrder`;
`SurveyManager` enforces the per-dimension budget.

### Dimensional Death

| Class | Responsibility |
|---|---|
| `CorruptionService` | Converts a column: terrain, plants, the whole column checked before anything changes |
| `WaterCorruption` | Which water corrupts and how a water position becomes Corrupted Water (also used by the fluid itself) |
| `ProtectionService` | Whether a position may be corrupted: config switches, spawn protection, regions, claims |
| `compat.claims` | `ClaimAdapters` finds the installed claim mods; `FtbChunksAdapter`, `OpenPacAdapter` query them by reflection |
| `CorruptedWaterEffects`, `BlightSpawnManager`, `BlightData` | Effects on entities, extra hostile spawns, persistent corruption data |
| `content.CorruptedWaterFluid` | The fluid: sinks layer by layer, never flows across a claim border |

## Data driven content

| Path | Read by |
|---|---|
| `data/<ns>/dimensional_miner_resources/*.json` | `ResourceProfiles` (overrides and dimension rules) |
| `data/<ns>/data_maps/item/matter_value.json` | `MatterValues` |
| `data/dimensionalfluxminer/tags/block/corrupt*.json` | `CorruptionService` |
| recipe types | `crusher`, `smelter`, `recipe` |
| `data/dimensionalfluxminer/kubejs/recipe_schema/*.json` | KubeJS only: the script form of each recipe type |

Integrations with other mods use only optional data (`neoforge:conditions` with `mod_loaded`, tag entries with
`"required": false`) or code isolated in `compat`.

**Script values (KubeJS).** `compat.kubejs.DfmKubeJSPlugin` posts the `DimensionalFluxMinerEvents` right after the
server scripts load, before the datapacks are read. Each event lands in a layer the mod has without KubeJS:
`DfmConfig.setOverrides` (values above the file, checked by the same spec, sent to clients by
`ConfigOverridesPayload` on login and after every reload, cleared when the server stops or the client leaves),
`ResourceProfiles.setScripted` (entries applied after the datapack ones, void rules before them) and a
`matter_value` data map file in KubeJS's last generated pack. Recipe types need no code: the JSON schemas above.

## Saves and compatibility

Block, item and block entity IDs never change: `ingot_steel` keeps its ID although the item is now Pure Steel. NBT keys
are part of the save format; the extracted classes write the same keys as before (`output`, `dm`, `work_progress`,
`lane_progressN`, `alloy_progress`, `energy_debt`). When a key has to change, read the old one too.

## Tests

```bash
./gradlew build                # compiles and runs the JUnit tests
./gradlew runGameTestServer    # every GameTest must pass
```

| Suite | Covers |
|---|---|
| `MinerGameTests`, `MatterGameTests`, `ProgressionGameTests` | Formation, cycle, DM, filter, profiles, the void, corruption |
| `CrusherGameTests`, `SmelterGameTests` | Recipes, slots, modes, XP, side configuration |
| `EnergyGameTests`, `ThroughputGameTests` | FE per operation, Rate 1–10, buffers, tick acceleration |
| `ProtectionGameTests` | Config protections and claims (a test claim system, and FTB Chunks when present in `run/mods`) |
| `OptimizationGameTests` | Every optimisation gives exactly the result of the plain code |
| `ContentGameTests` | Recipes and advancements load, Corrupted Water and plants, Pure Steel, the guide recipe |
| `BenchmarkGameTests` | Opt-in benchmark: `DFM_BENCHMARK=1 ./gradlew runGameTestServer` logs `BENCH` lines |

The GameTest server loads the mods in `run/mods`; compat tests adapt to the mods present. Corruption tests need
`skyAccess = true`.

## Working on the mod

- Keep names in English everywhere; Portuguese goes only in `pt_br.json`, and both lang files keep the same keys.
- A gameplay change updates the GameTests, `CHANGELOG.md`, `README.md`, [BALANCE.md](BALANCE.md) and, when shown to
  players, `docs/store/description.md` and the guide (`client/screen/GuideContent`, texts in `guide.dimensionalfluxminer.*`).
- No machine slot holds more than 64 items.
- Every machine, new ones included, automates each material on its own: one Side Configuration tab and one toggle
  per material, Auto Eject only for output-only materials, Auto Input only for input-only ones. Cover the independence
  of the toggles with a GameTest.
- Energy always comes from one fixed cost per operation (`DfmConfig`); never add a separate power curve.
- Optimisations never change results; add an equivalence test in `OptimizationGameTests`.
- Client code stays in `client`; common code reaches it only through `ClientHooks`.
- Do not convert line endings of existing files.
