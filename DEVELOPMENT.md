# Better Glass - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients
need only Pandorical. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API)
and `fabric.mod.json` (Java).

## Key Files

| File | Responsibility |
|------|---------------|
| `Main.java` | Entry point; the cracked blocks and the shard, and shipping the assets |
| `GlassShardLoot.java` | Vanilla glass and panes broken without silk touch drop shards |
| `CrackedGlassBlock.java`, `CrackedGlassPaneBlock.java` | The cracked kinds, which are glass and a pane in every behaviour but one |
| `Fragile.java` | That one: cracked glass set in place now and then shatters a moment later, and gives way under anything standing on it too long |
| `generate_assets.py` | Every texture, model, blockstate, recipe and loot table under `src/main/resources` |

## Joined glass

The joining is drawn by Pandorical's `ConnectedFaces` context model, which picks pieces by
neighbour as the chunk is built. This mod only ships the pieces, named after the block they are
for, which for vanilla's glass means `assets/minecraft/models/block`. Pandorical's DEVELOPMENT.md
has the naming.

Each frame piece repeats the faces of the frameless core exactly, vertex for vertex, so the frame
lands on top of the sheet instead of fighting it for depth. That only holds while the two share
geometry: a frame piece and its core are generated from the same numbers, and should stay that way.

## Art

`generate_assets.py` draws every texture from scratch (none are cut out of the vanilla jar) and
writes everything else beside them. It clears `src/main/resources/assets` and `data` before
writing, so anything hand-placed there is lost: put it in the script. Re-run it after changing it.

## Tests

`xvfb-run -a ./gradlew runClientGameTest` runs `GlassBreaksIntoShards` (shard counts, silk touch,
tinted glass, how often cracked glass gives way, how long it bears weight), and `Showcase`, which builds a row of windows and leaves its
pictures in `build/run/clientGameTest/screenshots`.
