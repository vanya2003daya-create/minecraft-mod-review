package com.adam.firemagic.events;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NecromancerCurseHandler {
    private static final long COOLDOWN_TICKS = 1200; // 60 секунд * 20 тиков
    private static final Map<UUID, Long> lastCurseTime = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                PlayerManaData manaData = ManaManager.getServerData(player);

                if (manaData.hasNecromancerSchool()) {
                    // Проверяем, атаковали ли игрока
                    checkAndApplyCurse(player, manaData);
                }
            }
        });
    }

    private static void checkAndApplyCurse(ServerPlayerEntity player, PlayerManaData manaData) {
        long currentTime = player.getWorld().getTime();
        UUID playerId = player.getUuid();

        // Проверяем кулдаун
        if (lastCurseTime.containsKey(playerId)) {
            long timeSinceLastCurse = currentTime - lastCurseTime.get(playerId);
            if (timeSinceLastCurse < COOLDOWN_TICKS) {
                return; // Кулдаун не прошел
            }
        }

        // Ищем ближайших мобов, которые недавно атаковали игрока
        Box searchBox = player.getBoundingBox().expand(10); // 10 блоков вокруг
        for (Entity entity : player.getWorld().getOtherEntities(player, searchBox)) {
            if (entity instanceof LivingEntity attacker && attacker.getAttacking() == player) {
                // Нашли атакующего
                applyBlindnessCurse(player, attacker);
                lastCurseTime.put(playerId, currentTime);

                // Сообщение игроку
                player.sendMessage(net.minecraft.text.Text.literal("§5Ваше проклятие ослепило " + attacker.getName().getString() + "!"), true);
                break; // Применяем только к одному атакующему
            }
        }
    }

    private static void applyBlindnessCurse(PlayerEntity caster, LivingEntity target) {
        // Ослепление на 5 секунд (100 тиков)
        target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.BLINDNESS,
                100, // 5 секунд * 20 тиков
                0,   // Уровень I
                false, // не частицы
                true,  // видимый эффект
                true   // можно показать иконку
        ));

        // Дополнительный слабый эффект (по желанию)
        target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.WEAKNESS,
                100,
                0,
                false,
                false,
                true
        ));
    }

    // Метод для сброса кулдауна при смерти
    public static void onPlayerDeath(PlayerEntity player) {
        lastCurseTime.remove(player.getUuid());
    }
}