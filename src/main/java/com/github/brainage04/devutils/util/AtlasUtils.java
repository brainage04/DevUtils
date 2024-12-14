package com.github.brainage04.devutils.util;

import com.github.brainage04.devutils.DevUtils;
import com.github.brainage04.devutils.mixin_other.IRenderItemMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
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

public class AtlasUtils {
    public static final int columns = 16 * 2;
    public static final int rows = 9 * 2;

    public static int[] framebufferToIntArray(Framebuffer framebuffer) {
        // end framebuffer
        framebuffer.bindFramebuffer(false);

        int width = framebuffer.framebufferWidth;
        int height = framebuffer.framebufferHeight;

        IntBuffer intBuffer = ByteBuffer.allocateDirect(width * height * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, intBuffer);

        int[] pixels = new int[width * height];
        intBuffer.get(pixels);

        return pixels;
    }

    /**
     * Concatenates 1D int arrays in a 2D int array into a single 1D int array.
     * @param intArrays A 2D int array.
     * @param reverse If `true`, the 1D int arrays are concatenated in reverse order. Note: this does not reverse the order of data contained within the 1D int arrays.
     * @return Returns a concatenated 1D int array.
     */
    public static int[] combineIntArrays(int[][] intArrays, boolean reverse) {
        int finalLength = 0;

        for (int[] intArray : intArrays) {
            finalLength += intArray.length;
        }

        int[] finalArray = new int[finalLength];

        if (reverse) {
            int previousLength = finalLength;
            for (int[] intArray : intArrays) {
                previousLength -= intArray.length;
                System.arraycopy(intArray, 0, finalArray, previousLength, intArray.length);
            }
        } else {
            int previousLength = 0;
            for (int[] intArray : intArrays) {
                System.arraycopy(intArray, 0, finalArray, previousLength, intArray.length);
                previousLength = intArray.length;
            }
        }

        return finalArray;
    }

    private static class ErrorWithTooltip {
        String message;
        String tooltip;

        public ErrorWithTooltip(String message, String tooltip) {
            this.message = message;
            this.tooltip = tooltip;
        }
    }

    public static boolean processAtlas(List<ItemStack> itemStacks, int size, String fileName) {
        // use boolean to track if anything is wrong so users don't have to
        // run the command multiple times to figure out everything that is wrong
        boolean shouldReturn = false;

        // track message info to report later
        List<ErrorWithTooltip> errors = new ArrayList<>(3);

        // divide size by the bigger number between columns and rows (columns - for now)
        int maxSize = columns > rows
                ? Math.round((float) Minecraft.getGLMaximumTextureSize() / columns)
                : Math.round((float) Minecraft.getGLMaximumTextureSize() / rows);
        if (size > maxSize) {
            errors.add(
                    new ErrorWithTooltip(
                            "Texture atlas size is too big.",
                            String.format(
                                    "Use a size of %d or less.",
                                    maxSize
                            )
                    )
            );
            shouldReturn = true;
        }

        // not being fullscreen (specifically not matching the aspect ratio of the screen) causes scaling issues
        if (!Minecraft.getMinecraft().isFullScreen()) {
            // for some reason this doesn't work (even though it SHOULD - maybe I just need to wait a few ticks or something?)
            //Minecraft.getMinecraft().toggleFullscreen();
            errors.add(
                    new ErrorWithTooltip(
                            "Minecraft is not fullscreen.",
                            "This causes inconsistencies in texture dimensions\n" +
                                    "(when the aspect ratios of Minecraft and your screen are mismatched).\n" +
                                    "For consistent texture dimensions, please enter fullscreen mode."
                    )
            );
            shouldReturn = true;
        }

/*
        boolean isGuiScaleAuto = Minecraft.getMinecraft().gameSettings.guiScale == 0;
        int oldGuiScale = Minecraft.getMinecraft().gameSettings.guiScale;
        if (!isGuiScaleAuto) {
            Minecraft.getMinecraft().gameSettings.guiScale = 0;

            errors.add(
                    new ErrorWithTooltip(
                            "GUI Scale is not set to Auto.",
                            "This causes incorrect lighting.\n" +
                                    "For lighting consistent with in-game GUIs,\n" +
                                    "set your GUI Scale to Auto."
                    )
            );
            shouldReturn = true;

    }
 */

        if (shouldReturn) {
            ChatUtils.addChatMessage("Errors:", ChatUtils.MessageType.ERROR);

            for (ErrorWithTooltip error : errors) {
                ChatUtils.addChatMessage(
                        error.message,
                        error.tooltip,
                        ChatUtils.MessageType.ERROR
                );
            }

            return false;
        }

        int width = size * columns;
        int height = size * rows;

        int framebufferCount = (int) Math.ceil(
                ((double) itemStacks.size()) / ((double) (columns * rows))
        );
        int framebufferIndex = 0;
        int[][] frameData = new int[framebufferCount][width * height];

        int bufferX = 0;
        int bufferY = 0;

        Framebuffer framebuffer = new Framebuffer(width, height, true);
        framebuffer.bindFramebuffer(true);

        // DON'T ASK I DON'T KNOW
        double scale = 1.0 / 1.2;
        // account for GUI Scale setting
/*
        if (!isGuiScaleAuto) {
            double currentScale = calculateScaleFactor(Minecraft.getMinecraft().gameSettings.guiScale);
            double autoScale = calculateScaleFactor(0); // 0 = auto, 1 = small, 2 = normal, 3 = large
            scale *= autoScale / currentScale;
        }
 */

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0);
        RenderHelper.enableGUIStandardItemLighting();

        for (ItemStack itemStack : itemStacks) {
            // add rendered item to framebuffer
            ((IRenderItemMixin) Minecraft.getMinecraft().getRenderItem()).devUtils$renderItemIntoGUIWithoutEffect(itemStack, bufferX * 16, bufferY * 16);

            // increment counters
            bufferX++;
            if (bufferX == columns) {
                bufferX = 0;
                bufferY++;
            }

            if (bufferY == rows) { // if 19th row reached (framebuffer is 32x18), start new framebuffer
                frameData[framebufferIndex] = AtlasUtils.framebufferToIntArray(framebuffer);
                framebufferIndex++;

                framebuffer = new Framebuffer(width, height, true);
                framebuffer.bindFramebuffer(true);
                bufferY = 0;
            }
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();

        // save final framebuffer
        frameData[framebufferIndex] = AtlasUtils.framebufferToIntArray(framebuffer);
        // stack frames vertically into one frame
        int[] pixels = AtlasUtils.combineIntArrays(frameData, true);
        // save frame as PNG to disk (and remove empty rows)
        AtlasUtils.saveTextureAtlas(pixels, 0, height - (bufferY + 1) * size, width, height * framebufferCount, fileName);

/*
        if (!isGuiScaleAuto) {
            Minecraft.getMinecraft().gameSettings.guiScale = oldGuiScale;
        }
 */

        return true;
    }

    public static void saveTextureAtlas(int[] pixels, int startX, int startY, int endX, int endY, String fileName) {
        int width = endX - startX;
        int height = endY - startY;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[(endY - y - 1) * endX + x]; // Flip vertically
                int alpha = (pixel >> 24) & 0xFF;
                int blue = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int red = pixel & 0xFF;

                // Combine channels into ARGB format
                image.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }

        File output = new File(String.format("%s.png", fileName));
        try {
            ImageIO.write(image, "PNG", output);
            ChatUtils.addAtlasComponent(output, "texture atlas");
        } catch (IOException e) {
            DevUtils.LOGGER.error("Failed to save atlas: {}", e.getMessage());
        }
    }

    public static void saveAtlasFile(String name, String description, String contents) {
        File output = new File(name);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writer.write(contents);
            ChatUtils.addAtlasComponent(output, description);
        } catch (IOException e) {
            DevUtils.LOGGER.error("Failed to save {}: {}", description, e.getMessage());
        }
    }

/*
    public static int calculateScaleFactor(int guiScale) {
        // copied from net.minecraft.client.gui.ScaledResolution
        int scaledWidth = Display.getWidth();
        int scaledHeight = Display.getHeight();
        int scaleFactor = 1;
        boolean bl = Minecraft.getMinecraft().isUnicode();
        if (guiScale == 0) {
            guiScale = 1000;
        }
        while (scaleFactor < guiScale && scaledWidth / (scaleFactor + 1) >= 320 && scaledHeight / (scaleFactor + 1) >= 240) {
            ++scaleFactor;
        }
        if (bl && scaleFactor % 2 != 0 && scaleFactor != 1) {
            --scaleFactor;
        }
        return scaleFactor;
    }
*/
}
