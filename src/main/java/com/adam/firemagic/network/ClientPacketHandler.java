package com.adam.firemagic.network;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.client.screen.PickaxeUpgradeScreen;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import com.adam.firemagic.upgrade.PickaxeResourcesData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class ClientPacketHandler {
    public static void register() {
        // Обработчик открытия GUI
        ClientPlayNetworking.registerGlobalReceiver(
                new Identifier(FireMagicMod.MOD_ID, "open_pickaxe_gui"),
                (client, handler, buf, responseSender) -> {
                    ItemStack stack = buf.readItemStack();
                    client.execute(() -> {
                        if (MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().setScreen(
                                    new PickaxeUpgradeScreen(MinecraftClient.getInstance().player, stack)
                            );
                        }
                    });
                }
        );

        // Обработчик синхронизации данных (для кнопок)
        ClientPlayNetworking.registerGlobalReceiver(
                new Identifier(FireMagicMod.MOD_ID, "sync_pickaxe_data"),
                (client, handler, buf, responseSender) -> {
                    PickaxeResourcesData data = PickaxeResourcesData.read(buf);
                    client.execute(() -> {
                        if (MinecraftClient.getInstance().currentScreen instanceof PickaxeUpgradeScreen screen) {
                            screen.updateData(data);
                        }
                    });
                }
        );

        // ✅ ДОБАВЛЕНО: Обработчик синхронизации маны
        ClientPlayNetworking.registerGlobalReceiver(
                ManaSyncPacket.ID, // Используем тот же ID
                (client, handler, buf, responseSender) -> {
                    // Читаем все данные на клиенте
                    int mana = buf.readInt();
                    int maxMana = buf.readInt();
                    boolean hasMiner = buf.readBoolean();
                    boolean hasArcher = buf.readBoolean();
                    boolean hasNecromancer = buf.readBoolean();
                    boolean hasZombie = buf.readBoolean();
                    boolean hasSkeleton = buf.readBoolean();
                    boolean hasPhantom = buf.readBoolean();

                    // Обновляем клиентские данные
                    client.execute(() -> {
                        PlayerManaData clientData = new PlayerManaData();
                        clientData.setMana(mana);
                        clientData.setMaxMana(maxMana);
                        clientData.setMinerSchool(hasMiner);
                        clientData.setArcherSchool(hasArcher);
                        clientData.setNecromancerSchool(hasNecromancer);

                        // На клиенте мы не храним ID мобов, только знаем что они есть
                        ManaManager.setClientData(clientData);

                        FireMagicMod.LOGGER.info("[CLIENT] Получены данные маны: " +
                                mana + "/" + maxMana +
                                ", некромант=" + hasNecromancer);
                    });
                }
        );
    }
}