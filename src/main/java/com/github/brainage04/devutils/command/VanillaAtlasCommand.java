package com.github.brainage04.devutils.command;

import com.github.brainage04.devutils.DevUtils;
import com.github.brainage04.devutils.util.AtlasUtils;
import com.github.brainage04.devutils.util.ChatUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VanillaAtlasCommand extends CommandBase {
    private final List<ItemStack> itemStacks = new ArrayList<>();

    @Override
    public String getCommandName() {
        return "atlas";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/atlas <pixels>" +
                "\n<pixels> - the width and height of each individual texture in the atlas";
    }

    private void generateAtlasMappings(int size) {
        int bufferX = 0;
        int bufferY = 0;

        StringBuilder atlasMappingsString = new StringBuilder();
        StringBuilder atlasCssString = new StringBuilder();

        for (ItemStack itemStack : itemStacks) {
            Item item = itemStack.getItem();

            String name1 = itemStack.getDisplayName().toUpperCase().replace(' ', '_');
            String name2 = item.getRegistryName().substring(10).toUpperCase().replace(' ', '_');

            if (itemStack.getMetadata() != 0) {
                name2 += ":" + itemStack.getMetadata();
            }

            atlasMappingsString.append(String.format(
                    "%s (%s): [%d, %d]\n",
                    name1,
                    name2,
                    bufferX,
                    bufferY
            ));
            atlasCssString.append(String.format(
                    ".items-%s, .items-%s { background-position: -%dpx -%dpx; }\n",
                    name1,
                    name2.replace(':', '_'),
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

        File atlasMappingsFile = new File(String.format("%s.txt", getCommandName()));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(atlasMappingsFile))) {
            writer.write(atlasMappingsString.toString());
            ChatUtils.addChatMessage(String.format("Atlas mappings saved to %s", atlasMappingsFile.getAbsolutePath()));
        } catch (IOException e) {
            DevUtils.LOGGER.error("Failed to save atlas mappings: {}", e.getMessage());
        }

        File atlasCssFile = new File(String.format("%s.css", getCommandName()));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(atlasCssFile))) {
            writer.write(atlasCssString.toString());
            ChatUtils.addChatMessage(String.format("Atlas CSS saved to %s", atlasMappingsFile.getAbsolutePath()));
        } catch (IOException e) {
            DevUtils.LOGGER.error("Failed to save atlas CSS: {}", e.getMessage());
        }
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.addChatMessage(getCommandUsage(sender));
            return;
        }

        if (itemStacks.isEmpty()) {
            for (Item item : Item.itemRegistry) {
                // these items do not have models, and therefore should not be in the atlas
                if (item == null
                        || item == Item.getItemFromBlock(Blocks.farmland)
                        || item == Item.getItemFromBlock(Blocks.lit_furnace)
                ) continue;
                List<ItemStack> subItemStacks = new ArrayList<>();
                item.getSubItems(item, null, subItemStacks);
                if (subItemStacks.isEmpty()) {
                    subItemStacks.add(new ItemStack(item));
                }

                // subItemStacks.get(0).getItem().getRegistryName().substring(10).equals("potion")
                if (item instanceof ItemPotion) {
                    DevUtils.LOGGER.info(subItemStacks.toString());
                    itemStacks.addAll(subItemStacks);
                    //itemStacks.add(subItemStacks.get(subItemStacks.size() - 1));
                } else {
                    itemStacks.addAll(subItemStacks);
                }
            }
        }
        DevUtils.LOGGER.info("Item stack count: {}", itemStacks.size());

        int size = Integer.parseInt(args[0]);

        AtlasUtils.processAtlas(itemStacks, size, getCommandName());

        generateAtlasMappings(size);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
