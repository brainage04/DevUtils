package com.github.brainage04.devutils.command;

import com.github.brainage04.devutils.DevUtils;
import com.github.brainage04.devutils.util.AtlasUtils;
import com.github.brainage04.devutils.util.ChatUtils;
import com.github.brainage04.devutils.util.SkyBlockData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.File;
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
        return "commands.skyblockatlas.usage";
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

        AtlasUtils.saveAtlasFile(String.format("%s.txt", fileName), "atlas mappings", atlasMappingsString.toString());
        AtlasUtils.saveAtlasFile(String.format("%s.css", fileName), "atlas CSS", atlasCssString.toString());
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
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 2) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        int size = Integer.parseInt(args[0]);
        String atlasType = args[1];

        List<ItemStack> itemStacks;
        SkyBlockData.NameSkinValuePair[] pairs;

        switch (atlasType) {
            case "full":
                itemStacks = itemStacksFull;
                pairs = SkyBlockData.allPairs;
                break;
            case "bazaar":
                itemStacks = itemStacksBazaar;
                pairs = SkyBlockData.bazaarPairs;
                break;
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }

        String fileName = String.format(
                "%s_%s",
                getCommandName(),
                atlasType
        );

        if (itemStacks.isEmpty()) {
            for (SkyBlockData.NameSkinValuePair pair : pairs) {
                itemStacks.add(generateSkull(pair.skinValue));
            }
        }
        DevUtils.LOGGER.info("Item stack count: {}", itemStacks.size());

        if (!AtlasUtils.processAtlas(itemStacks, size, fileName)) {
            return;
        }

        generateAtlasMappings(pairs, size, fileName);

        if (!config.exists()) {
            ChatUtils.addChatMessage(
                    "The texture atlas generated by this command may contain mostly Steve heads " +
                            "the first time it is generated using a fresh Minecraft install. " +
                            "This is most likely due to a rate limiting issue. " +
                            "Running the command again should correctly generate the texture atlas. " +
                            "This warning will not appear again.",
                    ChatUtils.MessageType.WARNING
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
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
