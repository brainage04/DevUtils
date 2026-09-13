# About
This mod features a collection of helpful utility commands for developers creating Minecraft-related products. As of right now, this mod features:

1. /atlas \<pixels\> - generates vanilla atlas files
   - \<pixels\> - the width and height of each individual texture in the atlas
2. /skyblockatlas \<pixels\> \<fullAtlas\> - generates Hypixel SkyBlock atlas files
    - \<pixels\> - the width and height of each individual texture in the atlas
   - \<fullAtlas\> - generates the full (~2,545 items) atlas if true, generates only the bazaar (~351 items) atlas if false

These atlases use 3D models for blocks and orders items by numeric ID (where applicable).

Exports use exactly the requested pixel pitch in a 32-column grid, independent of window resolution, fullscreen mode, and GUI scale. Framebuffers must be enabled; the maximum per-item size is the OpenGL maximum texture size divided by 32. For example, `/atlas 256` produces native 256×256 cells on hardware supporting an 8192-pixel texture.

Animated textures (including the clock and compass) use their first loaded frame for reproducible exports. The live texture pixels and animation timing are preserved after export. Only unused rows after the last item are omitted; the mappings and CSS refer directly to the exported cells.

# Backstory
I created this mod because I wanted a 1.8.9 texture atlas that used 3D models for blocks for a Minecraft-related website I'm currently developing. I found some, however I couldn't easily find mappings for these atlases. As such, I decided to create my own, the proper way - using functions from Minecraft's code base.