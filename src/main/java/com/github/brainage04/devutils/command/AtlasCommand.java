package com.github.brainage04.devutils.command;

import com.github.brainage04.devutils.DevUtils;
import com.github.brainage04.devutils.util.AtlasUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AtlasCommand extends CommandBase {
    private final List<ItemStack> itemStacks = new ArrayList<>();

    @Override
    public String getCommandName() {
        return "atlas";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.atlas.usage";
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

        AtlasUtils.saveAtlasFile(String.format("%s.txt", getCommandName()), "atlas mappings", atlasMappingsString.toString());
        AtlasUtils.saveAtlasFile(String.format("%s.css", getCommandName()), "atlas CSS", atlasCssString.toString());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) {
            throw new WrongUsageException(getCommandUsage(sender));
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

        if (!AtlasUtils.processAtlas(itemStacks, size, getCommandName())) {
            return;
        }

        generateAtlasMappings(size);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
