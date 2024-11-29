package com.github.brainage04.devutils.command;

import com.github.brainage04.devutils.DevUtils;
import com.github.brainage04.devutils.util.AtlasUtils;
import com.github.brainage04.devutils.util.SkyBlockSkinValues;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.List;

public class SkyBlockAtlasCommand extends CommandBase {
    private final List<ItemStack> itemStacks = new ArrayList<>();

    @Override
    public String getCommandName() {
        return "skyblockatlas";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/skyblockatlas <pixels>" +
                "\n<pixels>- the width and height of each individual texture in the atlas";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        // (THIS WILL TAKE A WHILE TO RUN - BE PATIENT)
        // (IT WILL TAKE EVEN LONGER TO RUN FOR THE FIRST TIME)
        if (itemStacks.isEmpty()) {
            for (String skinValue : SkyBlockSkinValues.skinValues) {
                ItemStack skull = new ItemStack(Items.skull, 1, 3);

                // example: {SkullOwner:{Id:"00000000-0000-0000-0000-000000000000",Properties:{textures:[{Value:"base64"}]}}}
                // create SkullOwner object and Id key/value
                NBTTagCompound skullOwner = new NBTTagCompound();
                skullOwner.setString("Id", "00000000-0000-0000-0000-000000000000");

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

                // add "stitched-together" NBT to skull itemstack
                skull.setTagCompound(new NBTTagCompound());
                skull.getTagCompound().setTag("SkullOwner", skullOwner);

                itemStacks.add(skull);
            }
        }
        DevUtils.LOGGER.info("Item stack count: {}", itemStacks.size());

        if (args.length != 1) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(getCommandUsage(Minecraft.getMinecraft().thePlayer));
            return;
        }

        int size = Integer.parseInt(args[0]);

        AtlasUtils.processAtlas(itemStacks, size, getCommandName());
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
