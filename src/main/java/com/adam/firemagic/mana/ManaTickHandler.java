package com.adam.firemagic.mana;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.effects.MinerHeightEffectHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class ManaTickHandler {
    private static final int REGEN_TICKS = 10; // 0.5 секунды
    private static long lastRegenCheck = 0;
    private static final int EFFECT_TICKS = 40;
    private static long lastEffectCheck = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ManaTickHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        long currentTime = server.getOverworld().getTime();

        // Регенерация маны
        if (currentTime - lastRegenCheck >= REGEN_TICKS) {
            boolean anyRegenerated = false;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                PlayerManaData data = ManaManager.getServerData(player);

                // Регенерируем ману, если она не полная
                if (data.getMana() < data.getMaxMana()) {
                    int oldMana = data.getMana();
                    data.regenerateMana();

                    // 🔴 КРИТИЧЕСКИ ВАЖНО: сохраняем изменения!
                    ManaManager.setServerData(player, data);

                    FireMagicMod.LOGGER.debug("Регенерация маны для " +
                            player.getName().getString() + ": " +
                            oldMana + " → " + data.getMana());

                    anyRegenerated = true;
                }
            }

            if (anyRegenerated) {
                FireMagicMod.LOGGER.debug("Регенерация маны завершена для " +
                        server.getPlayerManager().getPlayerList().size() + " игроков");
            }

            lastRegenCheck = currentTime;
        }
        if (currentTime - lastEffectCheck >= EFFECT_TICKS) {
            for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {
                PlayerManaData data = ManaManager.getServerData(serverPlayer);

                // === ПРОВЕРКА ТОЛЬКО ШКОЛЫ ШАХТЁРА ===
                if (data.hasMinerSchool()) {
                    MinerHeightEffectHandler.applyHeightEffects(
                            (ServerWorld) serverPlayer.getWorld(),
                            serverPlayer
                    );
                }
            }
            lastEffectCheck = currentTime;
        }
    }
}