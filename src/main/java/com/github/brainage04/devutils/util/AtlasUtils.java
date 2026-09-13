package com.github.brainage04.devutils.util;

import com.github.brainage04.devutils.DevUtils;
import com.github.brainage04.devutils.mixin.TextureMapAccessor;
import com.github.brainage04.devutils.mixin_other.IRenderItemMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.List;

public class AtlasUtils {
    public static final int columns = 16 * 2;
    public static final int rows = 9 * 2;

    public static boolean processAtlas(List<ItemStack> itemStacks, int size, String fileName) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int maxSize = Minecraft.getGLMaximumTextureSize() / Math.max(columns, rows);
        if (size < 1 || size > maxSize) {
            ChatUtils.addChatMessage("Texture atlas size is invalid.",
                    String.format("Use a size between 1 and %d.", maxSize), ChatUtils.MessageType.ERROR);
            return false;
        }
        if (!OpenGlHelper.isFramebufferEnabled() || itemStacks.isEmpty()) {
            ChatUtils.addChatMessage("Atlas export requires framebuffers and at least one item.",
                    ChatUtils.MessageType.ERROR);
            return false;
        }

        int width = size * columns;
        int height = size * rows;
        int outputRows = (itemStacks.size() + columns - 1) / columns;
        BufferedImage image = new BufferedImage(width, outputRows * size, BufferedImage.TYPE_INT_ARGB);
        IntBuffer readback = BufferUtils.createIntBuffer(width * height);
        int[] scanline = new int[width];

        // Snapshot the live texture before selecting a canonical animation frame.
        // Only mip level zero is sampled by the GUI renderer; restore it verbatim
        // afterwards without advancing the world's animation counters.
        TextureMap textures = minecraft.getTextureMapBlocks();
        minecraft.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        int textureWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int textureHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        IntBuffer liveTexture = BufferUtils.createIntBuffer(textureWidth * textureHeight);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, liveTexture);
        Framebuffer framebuffer = new Framebuffer(width, height, true);
        framebuffer.setFramebufferColor(0, 0, 0, 0);

        // A GUI item occupies 16 units. Map exactly 32 x 18 such cells to the
        // export viewport, never to ScaledResolution or the window's projection.
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, columns * 16, rows * 16, 0, 1000, 3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translate(0, 0, -2000);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);

        try {
            minecraft.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            for (TextureAtlasSprite sprite : ((TextureMapAccessor) textures).devUtils$getAnimatedSprites()) {
                for (int frame = 0; frame < sprite.getFrameCount(); frame++) {
                    int[][] data = sprite.getFrameTextureData(frame);
                    if (data != null) {
                        TextureUtil.uploadTextureMipmap(new int[][] {data[0]},
                                sprite.getIconWidth(), sprite.getIconHeight(),
                                sprite.getOriginX(), sprite.getOriginY(), false, false);
                        break;
                    }
                }
            }
            int itemsPerPage = columns * rows;
            for (int first = 0; first < itemStacks.size(); first += itemsPerPage) {
                framebuffer.framebufferClear();
                framebuffer.bindFramebuffer(true);
                int count = Math.min(itemsPerPage, itemStacks.size() - first);
                for (int index = 0; index < count; index++) {
                    ((IRenderItemMixin) minecraft.getRenderItem()).devUtils$renderItemIntoGUIWithoutEffect(
                            itemStacks.get(first + index), index % columns * 16, index / columns * 16);
                }

                readback.clear();
                GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, readback);
                int usedHeight = (count + columns - 1) / columns * size;
                int outputY = first / columns * size;
                for (int y = 0; y < usedHeight; y++) {
                    readback.position((height - y - 1) * width);
                    readback.get(scanline);
                    for (int x = 0; x < width; x++) {
                        int rgba = scanline[x];
                        scanline[x] = (rgba & 0xFF00FF00) | ((rgba & 0xFF) << 16) | ((rgba >> 16) & 0xFF);
                    }
                    image.setRGB(0, outputY + y, width, 1, scanline, 0, width);
                }
            }
        } finally {
            minecraft.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, textureWidth, textureHeight,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, liveTexture);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(matrixMode);
            framebuffer.deleteFramebuffer();
            minecraft.getFramebuffer().bindFramebuffer(true);
        }

        File output = new File(String.format("%s.png", fileName));
        try {
            ImageIO.write(image, "PNG", output);
            ChatUtils.addAtlasComponent(output, "texture atlas");
            return true;
        } catch (IOException e) {
            DevUtils.LOGGER.error("Failed to save atlas: {}", e.getMessage());
            return false;
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

}
