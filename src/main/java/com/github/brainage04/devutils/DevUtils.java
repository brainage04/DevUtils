package com.github.brainage04.devutils;

import com.github.brainage04.devutils.command.SkyBlockAtlasCommand;
import com.github.brainage04.devutils.command.VanillaAtlasCommand;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = DevUtils.MOD_ID, useMetadata=true)
public class DevUtils {
    public static final String MOD_ID = "devutils";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new VanillaAtlasCommand());
        ClientCommandHandler.instance.registerCommand(new SkyBlockAtlasCommand());
    }
}
