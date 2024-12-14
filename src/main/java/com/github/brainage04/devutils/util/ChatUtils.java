package com.github.brainage04.devutils.util;

import com.github.brainage04.devutils.DevUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.io.File;

public class ChatUtils {
    public enum MessageType {
        INFO,
        WARNING,
        ERROR,
    }

    public static IChatComponent formatMessage(String message, String tooltip, MessageType messageType) {
        IChatComponent iChatComponent = new ChatComponentText(
                message
        );

        if (tooltip != null) {
            iChatComponent.getChatStyle().setChatHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText(tooltip)
                    )
            );
            iChatComponent.getChatStyle().setUnderlined(true);
        }

        switch (messageType) {
            case WARNING:
                iChatComponent.getChatStyle().setColor(EnumChatFormatting.YELLOW);
                break;
            case ERROR:
                iChatComponent.getChatStyle().setColor(EnumChatFormatting.RED);
                break;
        }

        return new ChatComponentTranslation(
                "message.tooltip.format",
                DevUtils.MOD_NAME,
                iChatComponent
        );
    }

    public static IChatComponent formatMessage(String message, MessageType messageType) {
        return formatMessage(message, null, messageType);
    }

    public static void addChatMessage(String message, MessageType messageType) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(formatMessage(message, messageType));
    }

    public static void addChatMessage(String message, String tooltip, MessageType messageType) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(formatMessage(message, tooltip, messageType));
    }

    public static void addAtlasComponent(File output, String message) {
        ChatComponentText atlasComponent = new ChatComponentText(output.getName());
        atlasComponent.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, output.getAbsolutePath()));
        atlasComponent.getChatStyle().setUnderlined(true);

        Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentTranslation(
                        "atlas.save.success",
                        DevUtils.MOD_NAME,
                        message,
                        atlasComponent
                )
        );
    }
}
