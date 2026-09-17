# DevUtils icon

## What this is

`docs/icon/icon.png` — the mod's icon: 1024x1024 PNG, 8-bit RGBA, non-interlaced,
175,772 bytes, sha256
`cf3ea9705c5194bb24d11af5090ee7051c3dea896eab5313d319390f7c95152e`.

## How it was made

Two steps: a **real in-game export** from the mod, then a deterministic 1:1 paste.

1. **Export (real client, not a mock).** DevUtils (commit `a04b21d…`, jar sha256
   `3c1f47df3f54319475fed95829ba7b7519a6b6b023e38e5a47c959430119c1e4`) was run in a real
   **Minecraft 1.8.9** client launched via `./gradlew runClient`
   (`provenance/tools/launch-devutils-client.sh`) on an isolated Xvfb display `:223`,
   windowed, **GUI scale 2** at 1600x1000, stock 1.8.9 resources, no resource pack. The
   command `/atlas 256` was typed into the game window with xdotool
   (`provenance/tools/control.py`), producing `runtime/atlas.png|txt|css`, which
   `provenance/tools/record-export.py` copied to
   `exports/b-1600x1000-gui2-256/` and recorded as `proof.json` (8192x5120, 256 px per item,
   610 mapped cells, all populated).
   The export was proven **GUI/window independent** at 64 px and 256 px across
   1280x720/gui1, 1600x1000/gui2 and 1920x1080/gui3 (`independence-proof.json`,
   byte-identical output hashes), so the pixel pitch is a property of the mod, not of the
   window.
2. **Icon.** `provenance/tools/build-icon.py` crops sixteen 256x256 RGBA cells listed in that
   export's `proof.json` and pastes them unmasked into a 4x4 grid at exactly 1:1 (no
   resampling, no recolouring, no model replacement). Every cell of the saved icon is
   byte-identical to the export cell, and the cell pixel hashes match
   `provenance/icon-proof.json`.

Cell identities (legacy 1.8.9 blocks, 4x4 left to right, top to bottom, with their export
source rectangles): GRASS_BLOCK, DIRT, STONE, COBBLESTONE, OAK_WOOD_PLANKS, OAK_WOOD (LOG),
OAK_LEAVES, GLASS, SAND, GRAVEL, BRICKS (BRICK_BLOCK), BOOKSHELF, CRAFTING_TABLE, FURNACE,
CHEST, OBSIDIAN.

No camera and no seed are involved (this is a texture-atlas export, not a world capture) and
there is **no shader pack**: stock 1.8.9 client rendering with default options, which are
recorded verbatim inside `provenance/exports/b-1600x1000-gui2-256/proof.json`.

## Provenance files

`provenance/` mirrors the round-3 authoring tree `round3/atlas3/devutils/`:

| file | what it is |
|---|---|
| `manifest.json` | round-3 entry: method, source export, notes |
| `icon-proof.json` | per-cell proof: export source rectangles, icon rectangles, cell pixel sha256, icon sha256 |
| `independence-proof.json` | the real `/atlas 64` and `/atlas 256` runs across three window/GUI configurations, with geometry and per-file hashes |
| `report.json` | the mod-side root cause fix and its verification (build, client runs, image commands) |
| `exports/b-1600x1000-gui2-256/proof.json` | the export record for the atlas this icon came from: command, window geometry, client options, mapped cells, per-cell boxes and pixel hashes |
| `exports/b-1600x1000-gui2-256/atlas.txt` | the item -> cell mapping text written by the export |
| `tools/build-icon.py` | the icon composer (step 2) |
| `tools/launch-devutils-client.sh`, `tools/devutils-client.init.gradle`, `tools/control.py` | how the client was launched and how `/atlas 256` was typed |
| `tools/record-export.py` | copies the runtime export into `exports/<configuration>/` and writes `proof.json` |
| `tools/report.py`, `tools/finish-evidence.py` | the round-3 report/evidence writers |
| `cleanup.json`, `blockers.json` | round-3 cleanup record (client/server/Xvfb/sink stopped, no blockers) |

## How to regenerate

1. Re-create the export. The raw export PNGs are **no longer present** in the working tree
   (see Notes), so the icon cannot be rebuilt from shipped files alone. Re-run the capture,
   from the mod repository root with the round-3 `atlas3/devutils` directory available (the
   launch script hard-codes that path):

   ```
   bash tools/launch-devutils-client.sh
   python3 tools/control.py command '/atlas 256'          # one quoted argument
   python3 tools/record-export.py b-1600x1000-gui2-256 256
   ```
2. Rebuild the icon, from `docs/icon/provenance` with Pillow:

   ```
   python3 tools/build-icon.py     # rewrites devutils-legacy-blocks-1024.png + icon-proof.json
   ```
   Compare with the sha256 above and with the per-cell hashes in `icon-proof.json`.

## Notes

- **Known gap:** the 8192x5120 `/atlas 256` export PNG and the per-configuration 64/256/512
  export PNGs were deleted from the working tree during disk cleanup; only `proof.json`,
  `atlas.txt`, the client log and the per-cell pixel hashes are left. Identity of the icon is
  therefore still fully auditable (cell hashes + icon hash), but re-running step 1 is needed
  to re-derive it from scratch.
- Deliberately not copied: `server/` (11 MB Minecraft server jar and world), `runtime/`
  (client logs, mods, options, saves), `evidence/` (built mod jar, javap dumps, screenshots,
  run logs), the other six export configurations, each export's `atlas.css`/`client.log`, and
  the 64 px exports.
- The icon is the *legacy* (1.8.9) block atlas by design; it is not the modern atlas used for
  the TextureAtlasGenerator icon.

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
