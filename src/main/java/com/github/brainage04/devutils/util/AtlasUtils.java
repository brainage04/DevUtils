package com.github.brainage04.devutils.util;

import com.github.brainage04.devutils.DevUtils;
import com.github.brainage04.devutils.mixin_other.IRenderItemMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.List;

public class AtlasUtils {
    public static final int columns = 16 * 2;
    public static final int rows = 9 * 2;

    public static void processAtlas(List<ItemStack> itemStacks, int size, String fileName) {
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

        double scale = 1.0 / 1.2; // DON'T ASK I DON'T KNOW

        int displayWidth = Display.getWidth();
        int displayHeight = Display.getHeight();
        double desiredAspectRatio = 16.0 / 9.0;
        double actualAspectRatio = ((double) displayWidth) / ((double) displayHeight);
        double scaleY = desiredAspectRatio / actualAspectRatio;
        DevUtils.LOGGER.info("Screen width: {}, Screen height: {}", displayWidth, displayHeight);
        DevUtils.LOGGER.info("Desired aspect ratio: {}, Actual aspect ratio: {}", desiredAspectRatio, actualAspectRatio);

        for (ItemStack itemStack : itemStacks) {
            // add rendered item to framebuffer
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();

            GlStateManager.scale(scale, scale * scaleY, 1.0);
            ((IRenderItemMixin) Minecraft.getMinecraft().getRenderItem()).devUtils$renderItemIntoGUIWithoutEffect(itemStack, bufferX * 16, bufferY * 16);

            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();

            // increment counters
            bufferX++;
            if (bufferX == columns) {
                bufferX = 0;
                bufferY++;
            }

            if (bufferY == 18) { // if 19th row reached (framebuffer is 32x18), start new framebuffer
                frameData[framebufferIndex] = AtlasUtils.framebufferToIntArray(framebuffer);
                framebufferIndex++;

                framebuffer = new Framebuffer(width, height, true);
                framebuffer.bindFramebuffer(true);
                bufferY = 0;
            }
        }

        // save final framebuffer
        frameData[framebufferIndex] = AtlasUtils.framebufferToIntArray(framebuffer);
        // stack frames vertically into one frame
        int[] pixels = AtlasUtils.combineIntArrays(frameData, true);
        // save frame as PNG to disk
        AtlasUtils.saveTextureAtlas(pixels, width, height * framebufferCount, fileName);
    }

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

    public static void saveTextureAtlas(int[] pixels, int width, int height, String fileName) {
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

        File output = new File(String.format("%s.png", fileName));
        try {
            ImageIO.write(image, "PNG", output);
            ChatUtils.addChatMessage(String.format("Atlas saved to %s", output.getAbsolutePath()));
        } catch (IOException e) {
            DevUtils.LOGGER.error("Failed to save atlas: {}", e.getMessage());
        }
    }
}
