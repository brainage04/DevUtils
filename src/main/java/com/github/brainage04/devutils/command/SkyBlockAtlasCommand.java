package com.github.brainage04.devutils.command;

import com.github.brainage04.devutils.DevUtils;
import com.github.brainage04.devutils.util.AtlasUtils;
import com.github.brainage04.devutils.util.ChatUtils;
import com.github.brainage04.devutils.util.SkyBlockData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SkyBlockAtlasCommand extends CommandBase {
    private static final String configPath = String.format("%s.json", DevUtils.MOD_ID);
    private static final File config = new File(configPath);

    private final List<ItemStack> itemStacksFull = new ArrayList<>();
    private final List<ItemStack> itemStacksBazaar = new ArrayList<>();

    @Override
    public String getCommandName() {
        return "skyblockatlas";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/skyblockatlas <pixels> <fullAtlas>" +
                "\n<pixels> - the width and height of each individual texture in the atlas" +
                "\n<fullAtlas> - generates the full (~2,545 items) atlas if true, generates only the bazaar (~351 items) atlas if false";
    }

    private void generateAtlasMappings(SkyBlockData.NameSkinValuePair[] pairs, int size, String fileName) {
        int bufferX = 0;
        int bufferY = 0;

        StringBuilder atlasMappingsString = new StringBuilder();
        StringBuilder atlasCssString = new StringBuilder();

        for (SkyBlockData.NameSkinValuePair pair : pairs) {
            String name = pair.name;

            atlasMappingsString.append(String.format(
                    "%s: [%d, %d]\n",
                    name,
                    bufferX,
                    bufferY
            ));
            atlasCssString.append(String.format(
                    ".items-%s { background-position: -%dpx -%dpx; }\n",
                    name,
                    bufferX * size,
                    bufferY * size
            ));

            // increment counters
            bufferX++;
            if (bufferX == AtlasUtils.columns) {
                bufferX = 0;
                bufferY++;
            }
        }

        File atlasMappingsFile = new File(String.format("%s.txt", fileName));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(atlasMappingsFile))) {
            writer.write(atlasMappingsString.toString());
            ChatUtils.addChatMessage(String.format("Atlas mappings saved to %s", atlasMappingsFile.getAbsolutePath()));
        } catch (IOException e) {
            DevUtils.LOGGER.error("Failed to save atlas mappings: {}", e.getMessage());
        }

        File atlasCssFile = new File(String.format("%s.css", fileName));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(atlasCssFile))) {
            writer.write(atlasCssString.toString());
            ChatUtils.addChatMessage(String.format("Atlas CSS saved to %s", atlasCssFile.getAbsolutePath()));
        } catch (IOException e) {
            DevUtils.LOGGER.error("Failed to save atlas CSS: {}", e.getMessage());
        }
    }

    private ItemStack generateSkull(String skinValue) {
        ItemStack skull = new ItemStack(Items.skull, 1, 3);

        // example: {SkullOwner:{Id:"00000000-0000-0000-0000-000000000000",Properties:{textures:[{Value:"base64"}]}}}
        // create SkullOwner object and Id key/value
        NBTTagCompound skullOwner = new NBTTagCompound();
        // Id value MUST be unique - identical values will cause all skulls with the same Id to have the same texture
        // EVEN if the texture URLs are different
        UUID uuid = UUID.randomUUID();
        skullOwner.setString("Id", uuid.toString());

        // create Properties object and textures array
        NBTTagCompound properties = new NBTTagCompound();
        NBTTagList textures = new NBTTagList();

        // create texture object with Value key/value
        NBTTagCompound texture = new NBTTagCompound();
        texture.setString("Value", skinValue);

        // stitch everything together
        textures.appendTag(texture);
        properties.setTag("textures", textures);
        skullOwner.setTag("Properties", properties);

        // finally, add NBT to skull itemstack
        skull.setTagCompound(new NBTTagCompound());
        skull.getTagCompound().setTag("SkullOwner", skullOwner);

        return skull;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 2) {
            ChatUtils.addChatMessage(getCommandUsage(sender));
            return;
        }

        if (!config.exists()) {
            ChatUtils.addChatMessage(
                    "WARNING: The texture atlas generated by this command may contain mostly Steve heads " +
                            "the first time it is generated using a fresh Minecraft install. " +
                            "This is most likely due to a rate limiting issue. " +
                            "Running the command again should correctly generate the texture atlas. " +
                            "This warning will not appear again."
            );

            try {
                if (config.createNewFile()) {
                    DevUtils.LOGGER.info("{} created.", configPath);
                } else {
                    DevUtils.LOGGER.info("{} already exists.", configPath);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        int size = Integer.parseInt(args[0]);
        boolean fullAtlas = Boolean.parseBoolean(args[1]);

        List<ItemStack> itemStacks;
        SkyBlockData.NameSkinValuePair[] pairs;
        String fileName = getCommandName();

        if (fullAtlas) {
            itemStacks = itemStacksFull;
            pairs = SkyBlockData.allPairs;
            fileName += "_full";
        } else {
            itemStacks = itemStacksBazaar;
            pairs = SkyBlockData.bazaarPairs;
            fileName += "_bazaar";
        }

        if (itemStacks.isEmpty()) {
            for (SkyBlockData.NameSkinValuePair pair : pairs) {
                itemStacks.add(generateSkull(pair.skinValue));
            }
        }
        DevUtils.LOGGER.info("Item stack count: {}", itemStacks.size());

        AtlasUtils.processAtlas(itemStacks, size, fileName);
        generateAtlasMappings(pairs, size, fileName);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
