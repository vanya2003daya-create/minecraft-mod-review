package com.adam.firemagic.effects;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.List;

public class EnhancerArmorHandler {

    private static int tickCounter = 0;
    private static final String VANGUARD_COOLDOWN = "VanguardCooldown";

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            // Каждые 40 тиков (~2 секунды)
            if (tickCounter % 40 != 0) return;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                handleHunterHelm(player);
            }
        });
    }

    private static void handleHunterHelm(ServerPlayerEntity player) {
        // Проверяем шлем
        if (!(player.getInventory().getArmorStack(3).getItem() instanceof com.adam.firemagic.items.enhanced.HunterHelmItem)) {
            return;
        }

        double radius = 15.0;

        Box box = new Box(
                player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                player.getX() + radius, player.getY() + radius, player.getZ() + radius
        );

        List<LivingEntity> entities = player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                entity -> entity != player && entity.isAlive()
        );

        for (LivingEntity entity : entities) {
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.GLOWING,
                    60, // 3 сек (чуть больше интервала)
                    0,
                    false,
                    false
            ));
        }
    }

}