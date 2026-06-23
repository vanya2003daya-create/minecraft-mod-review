package com.adam.firemagic.mana;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.network.ManaSyncPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

public class ManaManager {

    // === СЕРВЕРНАЯ ЧАСТЬ ===
    public static class ManaState extends PersistentState {
        private final java.util.HashMap<java.util.UUID, PlayerManaData> playerMana = new java.util.HashMap<>();

        @Override
        public NbtCompound writeNbt(NbtCompound nbt) {
            NbtCompound players = new NbtCompound();
            for (var entry : playerMana.entrySet()) {
                players.put(entry.getKey().toString(), entry.getValue().writeToNbt());
            }
            nbt.put("players", players);
            return nbt;
        }

        public void readNbt(NbtCompound nbt) {
            if (nbt.contains("players")) {
                NbtCompound players = nbt.getCompound("players");
                for (String key : players.getKeys()) {
                    PlayerManaData data = new PlayerManaData(players.getCompound(key));
                    playerMana.put(java.util.UUID.fromString(key), data);
                }
                FireMagicMod.LOGGER.info("📂 Загружены данные маны для " +
                        playerMana.size() + " игроков");
            } else {
                FireMagicMod.LOGGER.warn("⚠️ Файл маны пуст или повреждён");
            }
        }

        public PlayerManaData getOrCreate(java.util.UUID id) {
            return playerMana.computeIfAbsent(id, k -> {
                FireMagicMod.LOGGER.info("🆕 Созданы новые данные маны для UUID: " + id);
                return new PlayerManaData();
            });
        }

        public void setPlayerData(java.util.UUID id, PlayerManaData data) {
            playerMana.put(id, data);
            markDirty(); // Помечаем для сохранения
            FireMagicMod.LOGGER.debug("💾 Данные маны сохранены для UUID: " + id);
        }
    }

    private static ManaState getManaState(ServerPlayerEntity player) {
        return player.getServerWorld().getPersistentStateManager().getOrCreate(
                nbt -> {
                    ManaState state = new ManaState();
                    state.readNbt(nbt);
                    return state;
                },
                ManaState::new,
                "firemagic_mana"
        );
    }

    public static PlayerManaData getServerData(ServerPlayerEntity player) {
        PlayerManaData data = getManaState(player).getOrCreate(player.getUuid());

        // 🔴 ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА
        if (data.getMana() == 0 && data.getMaxMana() > 0) {
            FireMagicMod.LOGGER.warn("🛠️ Обнаружена мана=0, исправляем...");
            data.setMana(data.getMaxMana());
        }

        return data;
    }

    public static void setServerData(ServerPlayerEntity player, PlayerManaData data) {
        // Проверяем данные перед сохранением
        if (data.getMana() < 0) {
            FireMagicMod.LOGGER.error("❌ Попытка сохранить отрицательную ману!");
            data.setMana(0);
        }

        if (data.getMaxMana() <= 0) {
            FireMagicMod.LOGGER.error("❌ Попытка сохранить maxMana <= 0!");
            data.setMaxMana(20);
        }

        getManaState(player).setPlayerData(player.getUuid(), data);
        ManaSyncPacket.sendTo(player, data);
    }

    // 🔴 НОВЫЙ МЕТОД: принудительная синхронизация
    public static void forceSync(ServerPlayerEntity player) {
        PlayerManaData data = getServerData(player);
        ManaSyncPacket.sendTo(player, data);
        FireMagicMod.LOGGER.info("🔁 Принудительная синхронизация для " +
                player.getName().getString());
    }

    // === КЛИЕНТСКАЯ ЧАСТЬ ===
    private static PlayerManaData clientData = new PlayerManaData();

    public static PlayerManaData getClientData() {
        return clientData;
    }

    public static void setClientData(PlayerManaData data) {
        clientData = data;
        FireMagicMod.LOGGER.debug("📱 Клиентские данные обновлены: " +
                data.getMana() + "/" + data.getMaxMana());
    }
}