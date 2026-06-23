package com.adam.firemagic.network;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.mana.PlayerManaData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ManaSyncPacket {
    public static final Identifier ID = new Identifier("firemagic", "mana_sync");

    // МЕТОД sendTo ДЛЯ СЕРВЕРА
    public static void sendTo(ServerPlayerEntity player, PlayerManaData data) {
        if (player == null || data == null) return;

        PacketByteBuf buf = PacketByteBufs.create();

        // Записываем ВСЕ данные
        buf.writeInt(data.getMana());
        buf.writeInt(data.getMaxMana());
        buf.writeBoolean(data.hasMinerSchool());
        buf.writeBoolean(data.hasArcherSchool());
        buf.writeBoolean(data.hasNecromancerSchool()); // ✅ ДОБАВЛЕНО

        // Флаги наличия мобов (для HUD/индикаторов)
        buf.writeBoolean(data.hasZombieMinion());
        buf.writeBoolean(data.hasSkeletonMinion());
        buf.writeBoolean(data.hasPhantomMinion());

        ServerPlayNetworking.send(player, ID, buf);

        FireMagicMod.LOGGER.info("[SERVER] Отправлены данные маны для " + player.getName().getString() +
                ": мана=" + data.getMana() +
                ", некромант=" + data.hasNecromancerSchool() +
                ", зомби=" + data.hasZombieMinion() +
                ", скелет=" + data.hasSkeletonMinion());
    }
}