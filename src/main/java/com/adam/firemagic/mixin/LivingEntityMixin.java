package com.adam.firemagic.mixin;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyArg(
            method = "damage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"
            ),
            index = 1
    )
    private float modifyDamageAmount(DamageSource source, float amount) {
        Entity attacker = source.getAttacker();

        if (attacker instanceof ServerPlayerEntity serverPlayer &&
                !serverPlayer.getWorld().isClient()) {

            PlayerManaData manaData = ManaManager.getServerData(serverPlayer);
            ItemStack mainHand = serverPlayer.getMainHandStack();

            // Проверяем школу лучника и меч
            if (manaData.hasArcherSchool() &&
                    mainHand.getItem() instanceof SwordItem) {

                // Снижаем урон на 25% БЕЗ РЕКУРСИИ
                return amount * 0.75f;
            }
        }

        return amount; // Нет изменений
    }
}