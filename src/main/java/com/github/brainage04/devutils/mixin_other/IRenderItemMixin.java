package com.github.brainage04.devutils.mixin_other;

import net.minecraft.item.ItemStack;

public interface IRenderItemMixin {
    void devUtils$renderItemIntoGUIWithoutEffect(ItemStack stack, int x, int y);
}
