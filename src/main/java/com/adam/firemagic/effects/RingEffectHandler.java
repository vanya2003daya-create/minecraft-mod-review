package com.adam.firemagic.effects;

import com.adam.firemagic.items.archer.ArcherRingItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class RingEffectHandler {
    private static final int RING_TICK_INTERVAL = 5; // Проверка каждые 5 тиков для производительности

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(RingEffectHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        long currentTime = server.getOverworld().getTime();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Проверяем только если у игрока есть активное кольцо
            if (ArcherRingItem.hasActiveRing(player)) {
                ArcherRingItem.tickActiveRing(player, currentTime);
            }
        }
    }
}