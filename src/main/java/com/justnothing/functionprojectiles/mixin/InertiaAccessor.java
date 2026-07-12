package com.justnothing.functionprojectiles.mixin;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ThrowableProjectile.class)
public interface InertiaAccessor {

    @Invoker("applyInertia")
    void callApplyInertia();
}
