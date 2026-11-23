# Architecture and development guide

How the code of Dimensional Flux Miner is organised, where each rule lives and how to work on it safely.
Gameplay numbers are in [BALANCE.md](BALANCE.md); releases are in [RELEASING.md](RELEASING.md).

## Minecraft versions (Stonecutter)

The branch builds Minecraft 1.21.1 (NeoForge 21.1.266, Java 21), 26.1.2 (NeoForge 26.1.2.126, Java 25) and 26.3
(NeoForge 26.3.0.103, Java 25) with Stonecutter: `settings.gradle.kts` lists them, `versions/<minecraft>/gradle.properties`
holds each one's own values and `build.gradle.kts` builds each as the Gradle project `:<minecraft>`.

- One `src/main/java` for all of them. Where the API differs, Stonecutter conditions (`//? if >=26.1 {` … `//?} else {`
  … `//?}`, and `>=26.3` for what only 26.3 has) choose the lines; Git holds the 26.1.2 layout (the other versions'
  lines stay commented). Classes that exist on one side only (for example `machine.transfer.Transfer`, `recipe.Recipes`,
  `content.TagBlockEntity`) are wrapped whole in a condition.
- Resources: `src/main/resources` is shared by every version, `src/mc26/resources` by 26.1.2 and 26.3,
  `versions/<minecraft>/src/main/resources` belongs to one version (loot tables, recipes, item models and other formats
  that changed). `src/main/templates/META-INF/neoforge.mods.toml` is shared; its Catalogue logo entry is `mod_logo`.
- Optional integrations without a build for a version are left out of its compile by `build.gradle.kts`: EMI and
  Polymorph exist only on 1.21.1, REI on 1.21.1 and 26.1.2.
- Runs: `:<minecraft>:runClient` uses `run/<minecraft>` for independent mods, settings and saves. `runGameTestServer`
  uses `run/mc-<minecraft>/gametest` (only this mod, like CI) and `runGameTestCompat` `run/mc-<minecraft>/gametest-compat`
  (a copy of the optional mods in its `mods` folder).

On 26.x the gameplay code is the same as on 1.21.1; these layers keep it and the saves unchanged on the new API:

- `machine.transfer`: the machines keep their slot, energy and tank classes (`ItemSlots`, `SlotStorage`,
  `EnergyStore`, `FluidTanks`); `Transfer` exposes them as NeoForge transfer handlers (with a snapshot `Journal`
  per block entity for transactions) and wraps other blocks' handlers the same way.
- Public energy handlers require a configured face. The Jade plugin reads the machines' own buffers directly
  for its energy bars, so generic unsided energy queries do not produce a second bar.
- `content.TagBlockEntity`: block entities still save and load a `CompoundTag` with the same keys, bridged to the new
  value input/output. `content.TagSavedData`: saved data with codecs, moving the old `data/<name>.dat` files once.
- `gametest.DfmGameTests`: registers every `@GameTest` method (the mod's own annotation) as a test function and
  instance; each batch is a test environment that moves the world spawn away while it runs.
- Recipes are a registry read before item components exist: results are `ItemStackTemplate`s. Clients get the
  mod's recipe types from the server (`OnDatapackSyncEvent`, `recipe.Recipes`).
- JEI, REI and EMI (`compat/rei`, `compat/emi`, recipe viewer order EMI, REI, JEI), Jade, AE2 and KubeJS are
  compile-only on every version that has them.
- Data formats are selected per version: 26.1.2 uses loot tables with `conditions` / `functions` and datapack
  format 101; 26.3 uses `condition` / `modifier` entries with `type`. Both use string ingredients.
  1.21.1 keeps its own recipe and resource formats in its version directory.
- Tank items as fluid containers go through `ItemAccess` (`tank.TankItemFluidHandler.forAccess`); every tank item
  uses `client.render.TankItemRenderer`, retaining the existing `creative_tank` special-renderer ID for resource packs.

## Packages

Everything lives under `src/main/java/dimensionalfluxminer`.

| Package | Responsibility |
|---|---|
| (root) | `DimensionalFluxMiner` (mod entry point, event wiring) and `DfmConfig` (server config, parsed corruption rules) |
| `registry` | Every deferred register: blocks, items, data components, block entities, menus, fluids, recipes, data maps, tags, sounds, particles, creative tab. `DfmWood` holds the corrupted wood set (its own `WoodType` and `BlockSetType`, boats, signs, shelf on 26.x; on 1.21.1 the boat type is a `Boat.Type` enum extension in `META-INF/enumextensions.json`) with its flammability, stripping (axe tool-modification event; on 26.3 the `neoforge:transformables` data map in `versions/26.3`) and dispenser behaviour; the blocks copy the properties of pale oak (oak on 1.21.1) and their resources are the vanilla ones renamed |
| `machine` | What the single-block machines share: `AbstractMachineBlockEntity`, item queues, neighbour transfer, read-only views, redstone, quick install, tick acceleration bookkeeping, cable detection (`CableActivity`, also used by the Miner ports, and `CablePortItemHandler`), advancement streaks (`ThroughputStreak`, `FullSpeedStreak`) |
| `machine.math` | Pure math, covered by JUnit: `RateScale` (Rate 1–10 curve and FE/t), `MinerProductionCurve`, `DynamicEnergyBuffer` (buffer and fractional FE debt), `WorkAccumulator` |
| `machine.side` | Side Configuration: relative faces, routes per transmission (item / energy / redstone / experience / fluid), per-machine rules (the Dimensional Tanks too), `NodeInputLanes` (lanes of the network node item inputs on the Electric Furnace Alloy profile) |
| `menu` | `MachineMenu` and the menu sync used by every screen |
| `crusher` | Mechanical Crusher: block, block entity, `CrusherProcessing` (recipe lookup and products), menu |
| `electric_furnace` | Electric Furnace: block entity plus the pieces below |
| `tank` | Dimensional Tanks: channels of the save, block, block entity, chamber fluid handler, menu, item |
| `miner` | Dimensional Flux Miner: multiblock, controller and the pieces below |
| `miner.profile` | Resource Profiles: what the Miner can find in each dimension |
| `miner.survey` | The Basic core's survey of drainable columns |
| `miner.corruption` | Dimensional Death: terrain and water conversion, protection, effects, spawns |
| `recipe` | Recipe types (`crushing`, `ore_crushing`, `alloying`, `tag_smelting`, `tag_blasting`) and the reader of other mods' alloy recipes |
| `content` | Blocks, items and fluids that are not machines (corrupted blocks, Corrupted Water, guide book, Reinforced Eye of Ender) and `Waterlogging`, shared by every waterloggable machine, tank and Miner part |
| `advancement` | Custom advancement trigger, including nearby-player awards after completed tank conversions |
| `network` | Payloads (menu sync, Miner assembly outline, formation burst, effective configuration and script ownership) |
| `compat` | Optional integrations, never loaded unless the other mod is present: `emi`, `jei`, `rei`, `jade`, `claims` (FTB Chunks and Open Parties and Claims, by reflection), `kubejs` (server script events, loaded only by KubeJS through `kubejs.plugins.txt`), `polymorph` (recipe choice on the Crusher and Electric Furnace screens, registered only when Polymorph is loaded), `ae2` (all three versions: `Ae2Power` joins the ME network only through an adjacent energy cell or a quartz fiber facing the side; `Ae2Parts` gives the side screens the part facing the machine; `Ae2TestParts` places cables for the GameTests), always called through `compat.Ae2Compat` |
| `mixin` | Corrupted Water particles, sunlight on corrupted terrain, the dark-screen shader on textures drawn as screen chrome (`GuiGraphicsThemeMixin`) |
| `client` | Renderers, screens, the guide (including live tank conversion diagrams and fluid-amount tooltips), sounds, particles, `ConnectedTextureModel` (connected textures of the loose chassis, glass and vents). Common code reaches it only through `machine.ClientHooks`; the EMI, JEI, REI and Jade plugins are client side and may use it |

**Light / dark screens** (`client/screen`): `GuiTheme` holds the look of the screen being drawn. Gray parts are drawn
inside `GuiTheme.chrome` (panel background, slots, button backgrounds, machine artwork, energy frame); in the dark
look their textures go through the `dark_gui` core shader (gray pixels keep 32% of their brightness, colored pixels
keep their color), and grays drawn with `fill` use `GuiTheme.gray`. Items, liquids and the navy screens are never
drawn inside `chrome`. Icons use one source image each in `icon_white/`; `GuiIcon` draws them centered at native size.
`IconPalette` uses one `icon_palette` pipeline with the desired RGB in vertex color, so arbitrary colors do not create pipelines.
The shader multiplies neutral pixels by that RGB while preserving colored details and grayscale shading. Icons are drawn
white; only the separate furnace terminal and mixed-mode separator layers take the theme color (`IconColor`: blue
#45E5E9 by default, purple #C6A5FF or custom). Only the alloy half receives the terminal overlay in mixed mode; the separator is drawn last.
`CustomColorWindow` edits a draft through HSV sliders and a hex field, showing only the color swatch and code, with explicit Apply/Cancel.
The 18x18 preview uses `ColorSwatch`, sharing the sunken slot bevel with tank channel code colors. `GuiThemes` keeps
`allCustomColor` and per-machine `customColors` alongside the existing preset IDs, preserving scope switches and old files.
The initial custom choice is orange (#EF8B32). An inactive former cyan default is normalized to orange; active custom choices remain intact.
`GuiTheme` caches custom glass gradients/rim and display text colors when RGB changes. Their hue/saturation follow the custom theme color
while retaining the base palette brightness for readable text and dark glass, including neutral and very dark choices.
`ThemeAccent` derives the frame and control shades from the theme color, as the Steelframe screens of Dimensional Flux do:
selected buttons and lit switches use a deep shade (HSL lightness 0.36, top 0.50, bottom 0.24), the hover outline 0.58,
and `MetalFrame` passes every band, ear and rivet color through `ThemeAccent.band` (theme hue, muted saturation, 82% of
the steel lightness; gray choices keep the steel 10% darker). The blue theme keeps the plain steel and the teal controls.
Selected icon buttons keep white icons over the deep fill; disabled controls stay gray.
**Terminal color in the world** (`client/render/TerminalTint`): the animated terminal textures of the Miner, Crusher and
Electric Furnace are neutral gray and sit on tint layer 0 (`neoforge_TintIndex 0` in the OBJ materials, `tintindex` in
the furnace JSON). The Crusher and the Furnace register a block tint source (26.x `BlockTintSources`, 1.21.1 `BlockColor`)
that reads `GuiThemes.terminalRgb(pos)` and returns white while the machine is off; their items use the
`dimensionalfluxminer:theme` item tint (shared color). The Miner's terminal is drawn by `MinerRenderer` with the tint
of its controller position while powered with a core (`MinerVisualStage.themedTerminal`); the no-core yellow and the
off gray keep their art. The colored ring around each screen is part of the tinted terminal layer, so it follows the
color; off machines show it gray. Choosing a color marks the chunks of the machines seen on this client for rebuilding.
The volume controls use editable `icon_white/minus.png` and `plus.png` sprites without font shadows. Label colors come from
`GuiTheme.title/heading/subheading`. Inside `chrome` the mod's own shader is always used (Darkness 1.0 in the light
look). `compat.DarkModeCompat` registers shader exceptions with Dark Mode Everywhere.
`ScreenPaletteMixin` wraps screen extraction and deferred tooltips in `ScreenPaletteGuard`, which temporarily
suspends Dark Mode and Dark Mode Everywhere palette overrides for screens in the mod's package, restoring them in
`finally`. `DarkModeTextureMixin` optionally excludes the mod's namespace from Dark Mode's texture recoloring
during resource loading. These adapters require no external mod dependency.
The Polymorph button is drawn through `GuiTheme.drawOnMachineScreen`. The choice is the player's
own: `GuiThemes` keeps it in `config/dimensionalfluxminer-themes.json` (apply-to-all switch, its color, one color per
machine by world, dimension and position); `ThemeWindow` is the Settings tab's window.
| `gametest` | In-world GameTests (see [Tests](#tests)) |

`src/test/java` holds the JUnit tests. They see no Minecraft class, so only pure math is tested there.

## The machines

`client.config` registers the Mods configuration factory and keeps an opening snapshot separate from the editable draft. `DfmConfigScreen` groups appearance and real server options into four tabs; `ConfigListScreen` returns only validated local lists. `ConfigPreview` maps options to example pictures in `textures/gui/config` (per-language guide tables with en_us fallback; appearance pictures follow the draft value). `ConfigPaint` uses exact-color GUI pipelines on 26.x, with version-specific shaders for RenderPearl on 26.3. `GuiThemes` exposes global preferences without a machine position. Server saves run on the integrated server thread, validate every changed key, reject script-owned keys and broadcast effective values. `DfmConfig` keeps received values and script ownership separate from the local sparse KubeJS overrides; an integrated server never imports its own complete snapshot as overrides. No remote editing packet exists.

### Crusher and Electric Furnace

Both extend `AbstractMachineBlockEntity`, which owns the energy buffer, Rate, upgrade slot, the automation of each
material (Auto Input / Auto Eject of items, Auto Eject of fluids through `autoEjectFluidCycle`),
Side Configuration, redstone mode, client sync and drops. Each server tick runs `logicalCycle`; every extra ticker call
of the same game time (tick accelerators) runs one more full cycle. A subclass implements `processCycle`,
`autoInputCycle`, `autoEjectCycle`, `createItemHandler(route)` and `effectivePowerLimit`.

Every configurable item face exposes a `CablePortItemHandler`: the route's own handler plus probe slots for what the
route lacks, which never move items but record a cable pulling or pushing (`CableActivity`). Every 10 ticks
`scanAutomaticSides` applies the Logistics Networks nodes first, then the active cables (pull = output, push = input,
what the face already did is kept), and locks those faces in the Side Configuration (`cable_sides` in the menu data).

**Electric Furnace** (`electric_furnace`):

| Class | Responsibility |
|---|---|
| `ElectricFurnaceBlockEntity` | Slots, modes, lanes, the furnace and alloy processing of each cycle, XP, persistence |
| `ElectricFurnaceEnergy` | Energy per profile: furnace FE per item, alloy FE per work unit, work units of a recipe, FE/t of a standard recipe |
| `ElectricFurnaceRecipes` | Recipe caches: furnace recipe per item (cleared when the `RecipeManager` changes or a recipe is chosen), last alloy, alloy ingredient set; `options` lists the blast furnace + furnace recipes of an item, same results merged |
| `ElectricFurnaceOutputPlanner` | Which of the three outputs a lane or an alloy writes to, and how much room is left |
| `ElectricFurnaceMode`, `AutoSplit`, `BulkBonus`, `StoredExperience`, `ExperienceFluidHandler` | Mode rules, even spreading, raw-ore bonus, fixed-point XP and its fluid view |

**Mechanical Crusher** (`crusher`): `MechanicalCrusherBlockEntity` runs the cycle and `CrusherProcessing` finds the
products (explicit recipes first, then the common tags).

**Recipe choice** (`recipe.RecipeChoices`): both machines are a `RecipeChoices.Host`. They keep the recipe chosen per
input item (NBT `recipe_choices`, written only when something was chosen) and apply it only while a recipe-selection
integration is installed (`RecipeChoices.enable()`); a version number tells their recipe caches to forget. `recipeOffer`
says what can be chosen now (Crusher: the input's `crushing` / `ore_crushing` recipes; Electric Furnace: the first lane
with more than one furnace result). `compat.polymorph.MachineRecipeData` shows that offer through Polymorph and stores
the pick back in the machine.

### Dimensional Tanks

The tanks have no energy or Rate, so they do not extend `AbstractMachineBlockEntity`; they
reuse its Side Configuration button ids and the `SideConfigWindow`. Liquids have their own Auto Input / Auto Eject;
the ordinary single tank also has an ITEM tab with independent toggles and red/blue container routes.

| Class | Responsibility |
|---|---|
| `TankChannels` | `SavedData` of the Overworld (one per save, seen from every dimension): the chambers of each channel, keyed by tank kind + code; empty channels are not saved |
| `TankCode`, `TankKind` | The three ordered dye colors (stable names, never RGB) and the single / dual / creative kinds with the chamber capacity |
| `DimensionalTankBlockEntity` | An endpoint: code, sides, liquid automation, the channel it points at; syncs the chambers to clients when the channel's version changes (at most every 2 ticks), whichever endpoint changed it |
| `ChamberFluidHandler` | One chamber as a fluid tank (one liquid with its components, simulate never changes anything); looks the channel up on each call, so a code change takes effect at once |
| `TankContainerSlots`, `TankContainerRecipe` | Ordinary single tank: two local slots, one container each; native fluid container exchange or data-driven conversion. The result stays in place and only completed containers can be extracted through a face. Items and completion flags persist in the endpoint NBT, never in the shared channel. Item and fluid changes share the transaction journal. |
| `DimensionalTankBlock`, `DimensionalTankItem`, `TankMenu` | Facing, buckets on the block (the clicked half picks the dual chamber), the code kept on the item (`tank_code` data component, copied by the loot table) |
| `CreativeFluidHandler`, `CreativeTanks` | Creative tank: no channel, the liquid lives in the block entity (`creative_fluid`) and on the item (`creative_tank_fluid`); draining never lowers it, a player's liquid picks it while empty, pipes are voided while empty and refused while full. `CreativeTanks` builds one filled item per source liquid (creative tab, REI entries) |

Container recipes can accept a `fluid_tag` alongside their canonical fluid. A matching fluid already in the tank is
consumed or replenished without changing its type or components; an empty tank uses the canonical fluid.
The recipe viewers list the matching fluid alternatives. Default conversions cover experience bottles,
sponges, concrete powder and an optional nutritious stick recipe guarded by a mod-loaded condition.

The channel padlock requires Shift + left click and selects shared or per-block storage through the same chamber handlers. Switching never moves fluid. Local chambers are persisted in block NBT and in `tank_local_a`/`tank_local_b` item components, alongside `tank_channel_locked`. `DimensionalTankItem.channelLocked(stack)` reads it: an item without that component and without `tank_code` (a new tank, never placed) counts as locked and is placed locked; blocks loaded from NBT without the key stay open. Loot and placement preserve active and inactive local reserves. The ordinary tank item uses a transactional finite handler in local mode. Color/personal-code changes are rejected by the server while locked.
`TankRenderer.State` captures the synchronized channel lock. Individual tanks cover the display inside its outline (front and lid) with one even background slightly darker than the panel (#15171F) and an amber (#E5B34D) padlock, restoring the saved code colors when reconnected. The renderer reuses `icon_white/lock.png` directly through a fixed entity render type, preserving the original pixel artwork without duplicating textures or adding an 8x8 sprite to the mipmapped block atlas. Fluid gauges and dual-chamber lid stripes retain the active liquid colors; creative code panels stay dark.
`TankItemRenderer` shares `TankRenderer.submitItem` with the block geometry: finite fill fractions, both dual chambers, code panels and padlocks appear in every item display context. Creative liquid remains full. Item base models carry only display transforms and particle texture; shell/glass are submitted once by the special renderer.
`tank.TankItemDisplay` reads active local components directly and updates read-only `tank_display_a`/`tank_display_b` channel snapshots on the server for player inventory, offhand, cursor and open-container slots. Ordinary item synchronization delivers changed readings, even with no placed endpoint. Loot seeds the display reading immediately. Snapshots never feed fluid handlers or placement; channel storage remains authoritative, and inactive local reserves stay separate.

`TankContainerSlots.tick` schedules each local slot independently using `tank.containerTicks` (5 by default),
at most once per world game time. The conversion engine remains atomic: neither items nor fluid change until
the delay completes and the full exchange succeeds. Failed conversions retry after another interval. Each slot's
progress is saved in its `containers` NBT and included in transaction snapshots; replacing its item resets only
that slot's timer. World ticks spent unloaded do not advance progress.

`TankRecipeLayout` shares the recipe page dimensions and positions between JEI and REI. Both reuse the screen's
`FluidGauge` artwork at 18 × 64: input liquid on the left, output liquid on the right, an empty gauge on the unused
side and item slots in the center. The fluid ingredient retains its quantity tooltip and recipe lookup behavior.
The tank screen keeps its 30 × 64 gauge; the narrow recipe variant removes central columns without stretching
the borders or level marks. Liquid quads are cropped directly so they work inside translated recipe renderers.
Fluid sampling starts at the sprite's UV bounds, which exclude atlas padding. Raw sprite allocation coordinates
include that padding and must not be used as the source origin for gauge tiles.
The narrow reflection uses a single centered crop; joining separated columns would break its diagonal band.

`client/screen/TankScreen` (reservoirs and code window) and `client/render/TankRenderer` (liquid, gauges and lid stripes
in the liquid's flat color from `FluidColors`, code on the front and lid panels, at the coordinates of the final models) draw the dynamic parts behind the static model's cut-outs.

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
| `TreeCorruption` | All versions: finds the natural tree over a corrupted column and plans its conversion into corrupted wood (logs keep their axis, leaves keep distance) and the swap of its vines for corrupted vines on the same faces |
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
| `src/main/resources/data/dimensionalfluxminer/tags/block/corruptible_{logs,wood,stripped_logs,stripped_wood,leaves,vines}.json`, `corrupted_logs.json` | `TreeCorruption` |
| recipe types | `crusher`, `electric_furnace`, `recipe` |
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
./gradlew :26.1.2:build                # compiles and runs the JUnit tests (also :1.21.1 and :26.3)
./gradlew :26.1.2:runGameTestServer    # every GameTest must pass, on every version
```

| Suite | Covers |
|---|---|
| `MinerGameTests`, `MatterGameTests`, `ProgressionGameTests` | Formation, cycle, DM, filter, profiles, the void, corruption |
| `CrusherGameTests`, `ElectricFurnaceGameTests` | Recipes, slots, modes, XP, side configuration |
| `EnergyGameTests`, `ThroughputGameTests` | FE per operation, Rate 1–10, buffers, tick acceleration |
| `TankGameTests` | Channels by kind and ordered code, dual chambers kept apart, code change and breaking keep the liquid, liquid automation, creative tank (endless, void, no channels, item/save, one per liquid) |
| `ProtectionGameTests` | Config protections and claims (a test claim system, and FTB Chunks when present in `run/mods`) |
| `OptimizationGameTests` | Every optimisation gives exactly the result of the plain code |
| `ContentGameTests` | Recipes and advancements load, Corrupted Water and plants, Pure Steel, the guide recipe |
| `BenchmarkGameTests` | Opt-in benchmark: `DFM_BENCHMARK=1 ./gradlew :26.1.2:runGameTestServer` logs `BENCH` lines |

The GameTest server loads the mods in `run/mods`; compat tests adapt to the mods present. Corruption tests need
`skyAccess = true`. The test world is flat sandstone, with no spawn chunks: a test that needs natural ground lays it
(`TestSupport.naturalGround`), and claims of a test never reach past its own bounds.

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
