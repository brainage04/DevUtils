package com.github.brainage04.devutils;

import com.github.brainage04.devutils.mixin_interface.IRenderItemMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

@Mod(modid = DevUtils.MOD_ID, useMetadata=true)
public class DevUtils {
    public static final String MOD_ID = "devutils";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new CaptureCommand());
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static class CaptureCommand extends CommandBase {
        List<ItemStack> itemStacks = new ArrayList<>();

        @Override
        public String getCommandName() {
            return "atlas";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/atlas <pixels> - the width and height of each individual texture in the atlas";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (itemStacks.isEmpty()) {
                for (Item item : Item.itemRegistry) {
                    // these items do not have models, and therefore should not be in the atlas
                    if (item == null || item == Item.getItemFromBlock(Blocks.farmland) || item == Item.getItemFromBlock(Blocks.lit_furnace)) continue;
                    List<ItemStack> subItemStacks = new ArrayList<>();
                    item.getSubItems(item, null, subItemStacks);
                    if (subItemStacks.isEmpty()) {
                        subItemStacks.add(new ItemStack(item));
                    }

                    itemStacks.addAll(subItemStacks);
                }
            }
            LOGGER.info("Item stack count: {}", itemStacks.size());

            // begin framebuffer
            int size = Integer.parseInt(args[0]);

            // todo: implement algorithm that adjusts rendering for non-16:9 aspect ratios
            int columns = 16 * 2;
            int rows = 9 * 2 + 2;

            int width = size * columns;
            int height = size * rows;
            int bufferX = 0;
            int bufferY = 0;

            LOGGER.info("Columns: {}", columns);
            LOGGER.info("Rows: {}", rows);

            Framebuffer framebuffer = new Framebuffer(width, height, true);
            framebuffer.bindFramebuffer(true);

            File file = new File("atlas.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (ItemStack itemStack : itemStacks) {
                    Item subItem = itemStack.getItem();

                    String name1 = itemStack.getDisplayName().toUpperCase().replace(' ', '_');
                    String name2 = subItem.getRegistryName().substring(10).toUpperCase().replace(' ', '_');

                    if (itemStack.getMetadata() != 0) {
                        name2 += ":" + itemStack.getMetadata();
                    }

                    writer.write(String.format(
                            "%s (%s): [%d, %d]",
                            name1,
                            name2,
                            bufferX,
                            bufferY
                    ));
                    writer.newLine();

                    // add rendered item to framebuffer
                    GlStateManager.pushMatrix();
                    RenderHelper.enableGUIStandardItemLighting();

                    // scaled down by 1/9ths
                    GlStateManager.scale(1.0 / 1.2, (1.0 / 1.2) * 8 / 9, 1.0);

                    ((IRenderItemMixin) Minecraft.getMinecraft().getRenderItem()).devUtils$renderItemIntoGUIWithoutEffect(itemStack, bufferX * 16, bufferY * 16);

                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.popMatrix();

                    // increment counters
                    bufferX++;
                    if (bufferX == columns) {
                        bufferX = 0;
                        bufferY++;
                    }
                }

                LOGGER.info("Item identifiers saved to {}", file.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Failed to save item identifiers: {}", e.getMessage());
            }

            framebuffer.bindFramebuffer(false);

            IntBuffer buffer = ByteBuffer.allocateDirect(width * height * 4)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer();
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            int[] pixels = new int[width * height];
            buffer.get(pixels);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[(height - y - 1) * width + x]; // Flip vertically
                    int alpha = (pixel >> 24) & 0xFF;
                    int blue = (pixel >> 16) & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int red = pixel & 0xFF;

                    // Combine channels into ARGB format
                    image.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
                }
            }

            File output = new File("atlas.png");
            try {
                ImageIO.write(image, "PNG", output);
                LOGGER.info("Atlas saved to {}", output.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("Could not write image to file!");
            }
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }
    }
}
