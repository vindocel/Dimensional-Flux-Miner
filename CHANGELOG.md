# Changelog

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
