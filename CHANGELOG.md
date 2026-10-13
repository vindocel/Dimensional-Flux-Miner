# Changelog

## 0.2.0 — unreleased (since 0.2.0-beta.2)

### ✨ Added

- 📖 **Dimensional Flux Guide**: a new book (book + steel ingot + Reinforced Eye of Ender, shapeless), handed to every
  new player by the mod's first advancement.
  - A 3D book model that stands upright; using it opens the book in the hand with an animation, it stays half
    open while you read, a leaf turns in your hand with every page and it closes when you leave the guide.
  - The guide is a real book: presentation and clickable contents on the first spread, chapters running page after
    page, page-turn animation and sounds, a violet ribbon back to the contents.
  - 8 chapters: introduction, materials and recipes, Dimensional Crusher, Alloy Smelter, building the Miner (with a
    rotatable 3D view, layer by layer, loose parts or formed), running the Miner, speed and the Efficiency Upgrade,
    Dimensional Death.
  - Live data: crafting recipes from the server, speed tables and matter values from the server configuration and
    datapacks; item tooltips and buttons to the EMI / JEI recipes.
- 🏆 **20 advancements**, from the first Crusher to a whole area drained. The first one gives the guide; the
  hidden "World Breaker" rewards a Dimensional Efficiency Upgrade.

### 🔄 Changed

- 💧 **Corrupted Water no longer waters land plants**: farmland next to it stays dry and sugar cane cannot grow beside
  it. Kelp and seagrass still live in it.
- ☠️ The corruption is now called **Dimensional Death** (*Morte Dimensional*) in game.
- 🏷️ The JAR name now includes the Minecraft version (`dimensionalfluxminer-<minecraft>-<version>.jar`).
- 🚢 Releases: tags may carry the Minecraft line (`v0.2.0-beta.3+1.21.1`), alphas and betas take their version from
  the tag, and a release must come from the branch of its Minecraft line (`mc/<version>`).
- 📘 README rewritten and reorganized.

### 🐛 Fixed

- EMI no longer reports the mod's block and entity tags as untranslated.

### 🧪 Tests

- New GameTests: guide recipe, advancement and reward loading, cobblestone counting, Corrupted Water never hydrating
  farmland or sugar cane (with water controls). New JUnit tests for the "1 000 operations per second for 5 seconds"
  streak. 65 GameTests in total.
