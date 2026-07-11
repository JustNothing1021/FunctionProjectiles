package com.justnothing.functionprojectiles.mixin;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketEntityAccessor {

    @Accessor("ITEM")
    static TrackedData<ItemStack> getItemTrackedData() {
        throw new AssertionError();
    }
}
