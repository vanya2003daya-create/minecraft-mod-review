package com.adam.firemagic.mixin;

import com.adam.firemagic.items.archer.ExplosiveArrowManager;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin {

    // Для столкновений с сущностями
    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void onEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;

        if (!projectile.getWorld().isClient() && ExplosiveArrowManager.isExplosiveArrow(projectile.getUuid())) {
            System.out.println("[DEBUG] 💥 Столкновение со сущностью!");
            ExplosiveArrowManager.handleExplosiveArrowCollision(projectile, projectile.getWorld(), hitResult.getEntity().getPos());
            projectile.discard();
            ci.cancel();
        }
    }

    // Для столкновений с блоками
    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void onBlockHit(BlockHitResult hitResult, CallbackInfo ci) {
        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;

        if (!projectile.getWorld().isClient() && ExplosiveArrowManager.isExplosiveArrow(projectile.getUuid())) {
            System.out.println("[DEBUG] 💥 Столкновение с блоком!");
            ExplosiveArrowManager.handleExplosiveArrowCollision(projectile, projectile.getWorld(), hitResult.getPos());
            projectile.discard();
            ci.cancel();
        }
    }
}