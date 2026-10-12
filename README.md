# Dimensional Flux Miner

A NeoForge mod built around a 3×3×3 multiblock miner that **never mines blocks from the world**. It turns FE and
Dimensional Matter (DM) into ore blocks that really generate in the current dimension. Two single-block machines
support it: the **Dimensional Crusher** and the **Alloy Smelter**.

| | |
|---|---|
| Minecraft | **1.21.1** |
| NeoForge | **21.1.255** |
| Java | **21** |
| Mod ID | `dimensionalfluxminer` |
| License | EPL-2.0 (see `LICENSE`) |

## Content

### Dimensional Flux Miner (multiblock)

27 blocks: 8 Steel Chassis, 9 Ventilation Units, 5 Reinforced Structural Glass, 3 Dimensional Ports, 1 Quantum
Core and 1 Miner Terminal. The terminal's facing defines the front of the machine. The three identical ports get
their role when the structure forms: **Energy Input**, **Matter Input** and **Item Output**. The structure forms
when the last block is placed, or when you right-click the terminal. Once formed, the terminal opens the GUI.
While the structure is incomplete, each right-click on the terminal places the next missing part from your
inventory (bottom layer first, through normal block placement) and outlines where the remaining parts go, colored
by part, red where another block is in the way.

- **Cores** (installed in the terminal GUI and never exposed to automation):
  - *Basic Dimensional Core* (Rare) runs the **Dimensional Drain**. A progressive survey of loaded chunks
    (300-block circular radius) finds drainable columns. Corrupting one column, following its topography with
    3 terrain blocks plus up to 3 water layers, yields **64 DM**.
  - *Stabilized Dimensional Core* (Epic) runs **Matter Injection**. Substrates pushed into the Matter Input port
    are converted to DM immediately. This core never touches the environment.
- **Manual power**: the power button left of the terminal screen (it stays pressed while the machine is on). A new
  machine starts off, and redstone can never start it. With *Redstone Control* ON, a signal on any of the nine
  lower-layer blocks is an additional gate. The terminal screen shows the status, the core, the limiting factor and
  the DM buffer, with the energy bar under it. The limiting factor is the strongest limit met by any cycle of the last
  tick, accelerated ones included (energy, no matter, output full, nothing to drain,
  redstone), and the potential / real production count every cycle of the tick.
- **Flux Rate 1–10**: 500 → 1 500 000 FE/t (geometric). Production follows `R(P) = Rmin·(P/Pmin)^k`
  (1/80 → 64 resources per logical cycle). FE is charged through the inverse curve for the work actually done.
- **Energy buffer** = 10 × the FE/t of the selected Rate. Lowering the Rate never deletes stored FE.
- **DM buffer**: 5 000 DM, stored in fixed point. Each roll costs 2 DM.
- **Output buffer**: 27 slots (9×3), output-only. A rolled result with no room is lost: the roll still takes its
  time and FE (not its DM), so every result keeps its own chance and pace and a nearly full output never produces
  the ore that still fits any faster. Only when no result it keeps can fit any more does it report OUTPUT BLOCKED,
  before it spends anything. The animation and the sound bridge short pauses (1 s) so they never flicker.
- **Item I/O**: no item limit per tick, for automation or pipes. Energy is the only limit: an accelerated
  Miner fed fast enough runs every cycle it is given, at the cost of server load.
- **Filter**: an allow-list of Resource Profile families, opened from the filter button left of the terminal screen (it stays pressed while the filter is on). An empty
  selection means the filter is OFF. With the **Efficiency Upgrade**, rejected results return
  `min(base + 0.5, 2) DM`.
- **Auto Input / Auto Eject** switches (default OFF, in the Settings window) act on the inventory directly adjacent
  to each port whenever they are on, also while the machine is off (the DM buffer fills before it starts); only a
  Redstone Control halt freezes them. Auto Input pulls repeatedly from the source until the DM buffer or the
  source runs out, so a single-slot bulk or creative storage feeds it at full speed.
- **Quick install**: sneak + right click with an Efficiency Upgrade on any machine, or on any block of a formed
  Miner, puts it in the upgrade slot; with a Dimensional Core on the Miner (or its terminal), in the core slot.

### Dimensional Crusher

- Rate 1–10: 250 → 500 000 FE/t, from 1 operation per 200 ticks up to 64 operations per tick.
- Explicit `dimensionalfluxminer:crushing` recipes, then `dimensionalfluxminer:ore_crushing`. Any other ore is
  resolved through `c:ores/<m>` → `c:raw_materials/<m>` → `c:gems/<m>`, with 2 products per ore (3 with the
  Efficiency Upgrade, which also halves FE). No fake "raw" items are ever created.
- Ingots (`c:ingots/<m>`), raw materials (`c:raw_materials/<m>`) and gems (`c:gems/<m>`) of any mod are crushed
  into one dust of the same material when a `c:dusts/<m>` item exists (this mod's dust first, then any other).
- Metal dusts smelt (furnace or blast furnace) into ingots: iron, gold and copper into the vanilla ingots; tin, lead,
  silver, uranium, aluminum, nickel, osmium and zinc into the first item of `c:ingots/<m>`, only when an installed
  mod provides that ingot (recipe types `tag_smelting` / `tag_blasting`). Gem and coal dusts have no ingot.
- Per-transmission **Side Configuration** (ITEM / ENERGY). The front is locked. The top face is a fixed item
  input that always picks up dropped items. Input and output can share one face.
- One input slot and four output slots, 64 items per slot. As in a vanilla inventory, a product whose slot is full
  continues in the next free output slot; the machine stops only when no output slot can take it. Grinding
  particles are bounded.
- Redstone Control is a whole-block mode (Ignored / Normal / Inverted), shared with the Alloy Smelter.

### Alloy Smelter

- Three modes, cycled from the mode tab: **Furnace Only**, **Alloy + Furnace** (the default) and **Alloy Only**.
  - Furnace: 3 independent lanes, each using the blast furnace recipe of its item, or its furnace recipe when
    there is none. Raw → Ingot uses the bulk bonus: 2 → 3 without the upgrade, 1 → 2 with it.
  - Alloy: `dimensionalfluxminer:alloying` recipes. 1–3 counted ingredients make one combined operation that
    produces a main result and an optional byproduct (for example steel + slag).
  - Alloy + Furnace: when the lanes together hold an alloy recipe it runs; otherwise every lane smelts on its own.
- **Auto Split** ON keeps every smeltable item spread evenly over its lanes and the empty ones (Raw pairs stay
  together without the upgrade), whatever put it there: GUI, automation or items already loaded. In Alloy +
  Furnace mode an alloy ingredient that can also be smelted (for example iron dust) is spread like any furnace item,
  unless another lane holds one of its alloy partners (for example coal dust): then it stays in one lane for the alloy.
- Machine slots behave like vanilla slots: left click takes or inserts a stack, right click takes half or inserts
  one item, Shift sends to the inventory, and in Creative a middle click picks a full stack of the item (Crusher
  slots too).
- Three output slots shared by every mode: lane *n* smelts into output *n*; when that slot holds another item, the
  lane uses a slot that already holds its result, or a free slot whose own lane is idle or makes the same item (a
  free slot stays reserved for the lane above it). An alloy puts its main result in the first output and its
  byproduct in the second.
  Every output stays extractable in every mode, so switching modes never strands items.
- Clicking the Crusher grinding chamber or the Alloy Smelter heat chamber opens the matching recipes in EMI / JEI.
  The Crusher mill turns only while the machine really processes; its two bars follow the operation's progress.
- Rate 1–10: 600 → 600 000 FE/t. Lane speed is `R(n,T) = (1/T)·(192T)^((n-1)/9)`, up to 192 items per tick
  per lane, limited in practice by the slots: every input and output holds 64 items, and a full output stops
  its lane until space is freed.
- Side Configuration adds the EXPERIENCE transmission. In ALLOY mode, lanes 1/2/3 can be routed individually
  (input-only).
- Recipe XP is stored internally and exposed as the fluid `dimensionalfluxminer:liquid_experience`
  (`#c:experience`, 1 XP = 20 mB, output-only; no bucket and no world block). It is not shown in the GUI:
  pipes extract it from the EXPERIENCE faces, and when the machine breaks the stored XP drops as experience orbs.
- Emits light level 14 while ACTIVE. The ingredients being processed are rendered inside the chamber.

### Dimensional Blight

Corruption converts dirt, grass and the stone family into corrupted variants, and sands and gravels into
Corrupted Sediment (it falls like sand, counts as sand and smelts into glass). It reaches the ground through
anything covering it (trees, buildings, machines; at most 32 blocks of cover). Weeds (short and tall grass,
ferns, dead bushes) become corrupted grass, flowers become wither roses, and water becomes Corrupted Water.

Corrupted Water behaves exactly like water (it flows, forms infinite sources, is in the `minecraft:water` fluid
tag, hydrates farmland and puts out fire; entities treat it as water: splash, swimming, air, underwater sounds;
mobs that breathe underwater, like drowned and guardians, breathe in it),
with a purple underwater view, and sinks by itself: it turns the water directly below it into
Corrupted Water, layer by layer, but never spreads sideways. Kelp and seagrass stay alive in it (they are swapped
for identical copies holding Corrupted Water, with the vanilla drops). It hurts players and passive mobs
(2 HP every 40 ticks) and heals monsters (+1 HP every 20 ticks; the Warden one heart every 10 ticks; never the
Ender Dragon or the Wither). Hostiles do not burn in daylight while
standing on corrupted terrain. A centralized spawn manager adds bounded hostile spawn pressure
(`4 · density²` attempts every 40 ticks, at most 64 per pass) without changing the mob cap.

## Building

```bash
./gradlew build
```

On Windows, use `gradlew.bat build`. The jar is written to
`build/libs/dimensionalfluxminer-<minecraft>-<version>.jar`.
The Gradle wrapper provisions Gradle itself. A JDK 21 is required; the foojay toolchain resolver can also
download one.

## Tests

```bash
./gradlew test                 # JUnit: rate curves, production curve, buffers, bulk bonus, auto-split, layout
./gradlew runGameTestServer    # in-world GameTests for the three machines and the corruption rules
```

## Continuous integration and releases

- **CI** (`.github/workflows/ci.yml`): every push to `main`, `1.21.x` or `mc/**` and every pull request
  checks that an `mc/<version>` branch builds that Minecraft version, then runs `./gradlew build` and the
  GameTests on Java 21. It uses no publishing credentials.
- **Release** (`.github/workflows/release.yml`): runs only when a GitHub Release is published. Tags look like
  `v0.3.0-beta.1+1.21.1`: `gradle.properties` keeps the base version and the channel (alpha, beta, release)
  comes from the tag. In the `management` environment the job validates the tag and its target branch, builds
  the tagged commit from scratch with that version, runs the GameTests, checks the jar and uploads it to
  CurseForge and Modrinth for the Minecraft version of that commit, with the release body as changelog.
- Each Minecraft line lives on an `mc/<version>` branch. See [docs/RELEASING.md](docs/RELEASING.md) for the full
  release and multi-version workflow.
- Gradle caching uses the open-source `basic` provider of `gradle/actions/setup-gradle`.

## Development

```bash
./gradlew runClient
./gradlew runServer
```

The server config `dimensionalfluxminer-server.toml` sets the FE extremes of each machine and the Miner
production extremes. Intermediate Rate levels are always recalculated from those values. The Miner maximum
can be lowered but never raised above 64 per cycle.

### Datapack customization

| Path | Purpose |
|---|---|
| `data/<ns>/dimensional_miner_resources/*.json` | Resource Profile overrides: `resource`, `dimension`, `family`, `weight` (share of the profile, 0–1], `enabled`, `crusher_output` |
| `data/<ns>/data_maps/item/matter_value.json` | DM value of substrates (`dm`, optional `dimensions`) |
| `data/dimensionalfluxminer/tags/block/corruptible_stone.json` | Stone-family blocks converted to corrupted stone |
| `data/dimensionalfluxminer/tags/block/corruptible_sediment.json` | Sands and gravels converted to corrupted sediment |
| `data/dimensionalfluxminer/tags/block/corruptible_weeds.json` | Weeds converted to corrupted grass |
| `data/dimensionalfluxminer/tags/block/corruption_ground.json` | Natural ground: the corruption looks through every other block above it |
| `data/dimensionalfluxminer/tags/block/corruption_blacklist.json` | Blocks never corrupted |
| `data/dimensionalfluxminer/tags/entity_type/*.json` | Blight spawn blacklist, light-bypass eligibility, Corrupted Water heal blacklist and heart-heal list (one heart every 10 ticks) |
| recipe types `crushing`, `ore_crushing`, `alloying` | Machine recipes |
| recipe types `tag_smelting`, `tag_blasting` | Furnace / blast furnace recipes whose result is the first item of `result_tag` |

## Compatibility

- All machines expose the standard NeoForge capabilities (items, energy, fluid) per face, so universal pipes
  that support several capabilities on one connection can use a single face.
- **External tick acceleration** (for example Soul Surge) works through the block entity ticker. Every extra
  ticker call in the same game time runs one more complete logical cycle. On the Miner, only the eight lower
  Steel Chassis are receivers. Each extra cycle costs a
  full cycle of FE. The energy buffer stays 10 × the FE/t of the Rate, but the energy input also accepts, above it,
  what the extra cycles of one game time use (measured from the last game time), so a source that refills once per
  game time keeps an accelerated machine at full power. A machine whose accelerated cycles did work in the last
  tick always shows as powered and running, even when its buffer reads 0 at that moment. This applies to the Miner,
  the Crusher and the Alloy Smelter; it is covered by GameTests that call the ticker directly and was observed in
  game with Soul Surge.
- Claim protection uses an adapter registry (`ProtectionService`). **No claim mod adapter ships yet** (FTB Chunks
  and Open Parties and Claims are planned), so corruption currently respects only the built-in blacklist.
- **Recipe viewers**: EMI and JEI (optional, compile-only APIs) show Crushing, Alloy Smelter (Furnace) and Alloy
  Smelter (Alloy). The Alloy Smelter categories use the machine's own processing element. With both installed, only
  the EMI plugin runs. The recipe / usage keys (R / U) also work on the items shown in the machine slots.
- **Jade**: every machine answers the no-face (`null` side) capability query with read-only views, so Jade shows
  its input / output items. An optional Jade plugin shows the energy with the machines' own animated energy bar
  (laid horizontally, in place of Jade's energy line) and, for the Electric Smelter, the stored XP in Jade's fluid
  bar filled with Liquid Experience, in mB. Text too long for a bar slides from side to side. Taking
  Electric Smelter results by hand collects the stored XP, as from a vanilla furnace.
- **Resource Profile** worldgen analysis reads vanilla ore features and ore features of other mods that declare
  `targets[].state` in their configuration (their vein size defaults to 8 when it is not part of the
  configuration). Mods that add no ores contribute nothing.
- **Alloys of other mods**: the Alloy Smelter also runs the recipes of the recipe types listed in
  `smelter.externalAlloyRecipeTypes` (default `enderio:alloy_smelting` and `oritech:foundry`), read through their own
  codecs: 1 to 3 counted item ingredients, item results (a second result becomes the byproduct), experience and time.
  Recipes with fluids or more than 3 ingredients are skipped, and so are plain furnace copies some alloy machines keep
  in their own type (the Alloy Smelter smelts those itself, with its raw ore bonus). No other mod is needed to build
  or run; the converted alloys also show in the EMI / JEI Alloy Smelter category.

## Third-party assets

The machine screen textures come from an MIT-licensed project; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
for the file list and the license text (also packaged in the mod jar).
