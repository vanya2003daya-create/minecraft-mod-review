package com.adam.firemagic.effects;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class MinerHeightEffectHandler {

    public static void applyHeightEffects(ServerWorld world, ServerPlayerEntity player) {
        if (world.isClient) return;

        PlayerManaData manaData = ManaManager.getServerData(player);
        if (!manaData.hasMagicSchool()) return;

        int y = player.getBlockY();

        // Убираем ВСЕ предыдущие эффекты скорости/замедления
        // (убираем только те, что даём мы, оставляем другие эффекты нетронутыми)
        player.removeStatusEffect(StatusEffects.SPEED);
        player.removeStatusEffect(StatusEffects.SLOWNESS);

        // ПРИМЕНЯЕМ НОВЫЕ ЭФФЕКТЫ ПО ТВОЕЙ ЛОГИКЕ:
        if (y < 20) {
            // ГЛУБОКО (Y < 10) → СКОРОСТЬ II
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED,
                    100,    // 5 секунд (20 тиков/сек × 5)
                    0,      // Уровень II (индекс 1)
                    false,  // Нет амбьента
                    false,  // Не показывать в инвентаре
                    true    // Показывать иконку
            ));

        } else if (y < 55) {
            // СРЕДНЯЯ ГЛУБИНА (10 ≤ Y < 55) → СКОРОСТЬ I
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED,
                    100,
                    0,      // Уровень I (индекс 0)
                    false,
                    false,
                    true
            ));

        } else {
            // ПОВЕРХНОСТЬ (Y ≥ 55) → ЗАМЕДЛЕНИЕ I
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS,
                    100,
                    0,      // Уровень I
                    false,
                    false,
                    true
            ));
        }
    }
}