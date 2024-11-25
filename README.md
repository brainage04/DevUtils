# About
This mod features a collection of helpful utility commands for
developers creating Minecraft-related products. As of right now,
this mod features:
- /atlas \<pixels\> - generates a 2D texture atlas (atlas[0-9].png)
and mapping (atlas.txt) where each texture is assigned a width and
height (in pixels) of the specified argument for \<pixels\>
(only 32 textures wide - for now).
This atlas uses 3D models for blocks and orders items as they are
ordered in the creative search tab (by numeric ID, support for
alphabetical/other ordering will be added as a later stage)

# Backstory
I created this mod because I wanted a 1.8.9 texture atlas that used
3D models for blocks for a Minecraft-related website I'm currently
developing. I found some, however I couldn't easily find mappings
for these atlases. As such, I decided to create my own, the proper
way - using functions from Minecraft's code base.