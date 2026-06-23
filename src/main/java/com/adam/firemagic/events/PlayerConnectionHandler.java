package com.adam.firemagic.events;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import com.adam.firemagic.upgrade.PickaxeResourcesManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerConnectionHandler {

    public static void register() {
        // Сохраняем данные при выходе игрока
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) {
                savePlayerManaData(player);
            }
        });

        // Загружаем данные при входе игрока
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) {
                loadPlayerManaData(player);
            }
        });
    }

    private static void savePlayerManaData(ServerPlayerEntity player) {
        // Сохраняем в PersistentState
        PlayerManaData manaData = ManaManager.getServerData(player);
        // Данные уже сохранятся через markDirty()
    }

    private static void loadPlayerManaData(ServerPlayerEntity player) {
        // Автоматически загружается через getServerData()
        PlayerManaData manaData = ManaManager.getServerData(player);

        // Синхронизируем с клиентом
        ManaManager.setServerData(player, manaData);

        // Отладочное сообщение
        System.out.println("[FireMagic] Загружены данные игрока " + player.getName().getString() +
                ": магия=" + manaData.hasMagicSchool() +
                ", мана=" + manaData.getMana() + "/" + manaData.getMaxMana());
    }
}