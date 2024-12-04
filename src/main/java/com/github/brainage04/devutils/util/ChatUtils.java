package com.github.brainage04.devutils.util;

import com.github.brainage04.devutils.DevUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentText;

public class ChatUtils {
    public static final String prefix = String.format("[%s] ", DevUtils.MOD_NAME);

    public static void addChatMessage(String message) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;

        if (player != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(prefix + message));
        } else {
            DevUtils.LOGGER.info(message);
        }
    }
}
