package com.github.brainage04.devutils;

import com.github.brainage04.devutils.mixin_interface.IRenderItemMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
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
import java.util.Arrays;
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
        // these items do not have models, and therefore should not be in the atlas
        List<String> blacklistedItems = new ArrayList<>(Arrays.asList("FARMLAND", "LIT_FURNACE"));

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
            // begin framebuffer
            int framebufferIndex = 0;
            int size = Integer.parseInt(args[0]);

            int itemsPerRow = 16 * 2;
            int itemsPerColumn = 9 * 2;
            int width = size * itemsPerRow;
            int height = size * itemsPerColumn;
            int bufferX = 0;
            int bufferY = 0;
            int bufferYcarry = 0;
            Framebuffer framebuffer = new Framebuffer(width, height, true);
            framebuffer.bindFramebuffer(true);

            File file = new File("atlas.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Item item : Item.itemRegistry) {
                    if (item == null) continue;
                    List<ItemStack> subItemStacks = new ArrayList<>();
                    item.getSubItems(item, null, subItemStacks);
                    if (subItemStacks.isEmpty()) {
                        subItemStacks.add(new ItemStack(item));
                    }

                    for (ItemStack subItemStack : subItemStacks) {
                        Item subItem = subItemStack.getItem();

                        String name1 = subItemStack.getDisplayName().toUpperCase().replace(' ', '_');
                        String name2 = subItem.getRegistryName().substring(10).toUpperCase().replace(' ', '_');

                        if (blacklistedItems.contains(name2)) {
                            continue;
                        }

                        if (subItemStack.getMetadata() != 0) {
                            name2 += ":" + subItemStack.getMetadata();
                        }

                        writer.write(String.format(
                                "%s (%s): [%d, %d]",
                                name1,
                                name2,
                                bufferX,
                                bufferY + bufferYcarry * (itemsPerColumn - 2)
                        ));
                        writer.newLine();

                        // add rendered item to framebuffer
                        GlStateManager.pushMatrix();
                        RenderHelper.enableGUIStandardItemLighting();

                        if (subItemStack.hasEffect()) {
                            ((IRenderItemMixin) Minecraft.getMinecraft().getRenderItem()).devUtils$renderItemIntoGUIWithoutEffect(subItemStack, bufferX * 16, bufferY * 16);
                        } else {
                            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(subItemStack, bufferX * 16, bufferY * 16);
                        }

                        RenderHelper.disableStandardItemLighting();
                        GlStateManager.popMatrix();

                        // increment counters
                        bufferX++;
                        if (bufferX == itemsPerRow) {
                            bufferX = 0;
                            bufferY++;
                        }

                        if (bufferY == itemsPerColumn - 2) {
                            saveCurrentFramebuffer(framebuffer, width, height, framebufferIndex);
                            framebufferIndex++;
                            framebuffer = new Framebuffer(width, height, true);
                            framebuffer.bindFramebuffer(true);
                            bufferY = 0;
                            bufferYcarry++;
                        }
                    }
                }

                LOGGER.info("Item identifiers saved to {}", file.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Failed to save item identifiers: {}", e.getMessage());
            }

            saveCurrentFramebuffer(framebuffer, width, height, framebufferIndex);
        }

        private static void saveCurrentFramebuffer(Framebuffer framebuffer, int width, int height, int framebufferIndex) {
            // end framebuffer
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

            File output = new File(String.format("atlas%d.png", framebufferIndex));
            try {
                ImageIO.write(image, "PNG", output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }
    }
}
