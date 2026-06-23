package com.adam.firemagic.events;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class MinerHeightEffectHandler {
    public static void applyHeightEffects(ServerWorld world, ServerPlayerEntity player) {
        // Проверяем, что игрок в подземелье (высота < 40)
        BlockPos pos = player.getBlockPos();
        if (pos.getY() >= 40) return;

        PlayerManaData data = ManaManager.getServerData(player);

        // Двойная проверка на школу (на всякий случай)
        if (!data.hasMinerSchool()) return;

        // Применяем эффекты в зависимости от глубины
        if (pos.getY() <= 10) {
            // Глубокие шахты: Спешка II + Сопротивление I
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 0, false, false));
        } else if (pos.getY() <= 25) {
            // Средние шахты: Спешка I
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 0, false, false));
        } else {
            // Верхние шахты: Нет эффектов
            player.removeStatusEffect(StatusEffects.HASTE);
            player.removeStatusEffect(StatusEffects.RESISTANCE);
        }
    }
}