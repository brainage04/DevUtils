package com.github.brainage04.devutils.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class DebugCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "sr";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {

    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
