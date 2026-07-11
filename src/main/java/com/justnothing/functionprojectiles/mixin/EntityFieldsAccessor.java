package com.justnothing.functionprojectiles.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityFieldsAccessor {

    @Accessor("velocityDirty")
    void setVelocityDirty(boolean dirty);
}
