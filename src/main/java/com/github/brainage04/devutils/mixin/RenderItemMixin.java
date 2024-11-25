package com.github.brainage04.devutils.mixin;

import com.github.brainage04.devutils.mixin_interface.IRenderItemMixin;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderItem.class)
public abstract class RenderItemMixin implements IRenderItemMixin {
    @Shadow protected abstract void renderModel(IBakedModel model, ItemStack stack);

    @Shadow @Final private ItemModelMesher itemModelMesher;

    @Shadow @Final private TextureManager textureManager;

    @Shadow protected abstract void setupGuiTransform(int xPosition, int yPosition, boolean isGui3d);

    @Unique
    public void devUtils$renderItemWithoutEffect(ItemStack stack, IBakedModel model) {
        if (stack != null) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5f, 0.5f, 0.5f);
            if (model.isBuiltInRenderer()) {
                GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f);
                GlStateManager.translate(-0.5f, -0.5f, -0.5f);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                GlStateManager.enableRescaleNormal();
                TileEntityItemStackRenderer.instance.renderByItem(stack);
            } else {
                GlStateManager.translate(-0.5f, -0.5f, -0.5f);
                this.renderModel(model, stack);
            }
            GlStateManager.popMatrix();
        }
    }

    @Unique
    public void devUtils$renderItemIntoGUIWithoutEffect(ItemStack stack, int x, int y) {
        IBakedModel ibakedmodel = this.itemModelMesher.getItemModel(stack);
        GlStateManager.pushMatrix();
        this.textureManager.bindTexture(TextureMap.locationBlocksTexture);
        this.textureManager.getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        this.setupGuiTransform(x, y, ibakedmodel.isGui3d());
        ibakedmodel = ForgeHooksClient.handleCameraTransforms(ibakedmodel, ItemCameraTransforms.TransformType.GUI);
        this.devUtils$renderItemWithoutEffect(stack, ibakedmodel);
        GlStateManager.disableAlpha();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        this.textureManager.bindTexture(TextureMap.locationBlocksTexture);
        this.textureManager.getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
    }
}
