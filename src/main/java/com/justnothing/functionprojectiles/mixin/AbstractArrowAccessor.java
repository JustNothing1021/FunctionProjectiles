package com.justnothing.functionprojectiles.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {

    @Invoker("isInGround")
    boolean callIsInGround();

    @Invoker("setInGround")
    void callSetInGround(boolean inGround);

    @Invoker("findHitEntity")
    EntityHitResult callFindHitEntity(Vec3 startVec, Vec3 endVec);

    @Invoker("onHitEntity")
    void callOnHitEntity(EntityHitResult hitResult);
}
