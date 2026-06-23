package com.adam.firemagic.network;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.items.miner.MinerPickaxeItem;
import com.adam.firemagic.upgrade.PickaxeResourcesData;
import com.adam.firemagic.upgrade.PickaxeResourcesManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ServerPacketHandler {
    public static void register() {
        // Добавление ресурсов
        ServerPlayNetworking.registerGlobalReceiver(
                new Identifier(FireMagicMod.MOD_ID, "add_resources"),
                (server, player, handler, buf, responseSender) -> {
                    String category = buf.readString(32767);
                    int amount = buf.readInt();
                    server.execute(() -> handleAddResources(handler.getPlayer(), category, amount));
                }
        );

        // Извлечение ресурсов
        ServerPlayNetworking.registerGlobalReceiver(
                new Identifier(FireMagicMod.MOD_ID, "extract_resources"),
                (server, player, handler, buf, responseSender) -> {
                    String category = buf.readString(32767);
                    server.execute(() -> handleExtractResources(handler.getPlayer(), category));
                }
        );

        // Применение улучшений
        ServerPlayNetworking.registerGlobalReceiver(
                new Identifier(FireMagicMod.MOD_ID, "apply_upgrades"),
                (server, player, handler, buf, responseSender) -> {
                    server.execute(() -> handleApplyUpgrades(handler.getPlayer()));
                }
        );
    }

    private static void handleAddResources(ServerPlayerEntity player, String category, int amount) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof MinerPickaxeItem)) return;

        PickaxeResourcesData resourcesData = PickaxeResourcesManager.getData(stack);
        int current = resourcesData.getResource(category);
        int max = getMax(category);

        if (current >= max) {
            player.sendMessage(Text.literal("§cДостигнут максимум для " + getCategoryName(category).toLowerCase() + "!"), true);
            return;
        }

        Item resourceItem = getResourceItem(category);
        if (resourceItem == null) return;

        int available = player.getInventory().count(resourceItem);
        if (available < amount) {
            player.sendMessage(Text.literal("§cНедостаточно " + getResourceName(category) + "!"), true);
            return;
        }

        // Удаляем ресурсы из инвентаря
        int toRemove = amount;
        for (int i = 0; i < player.getInventory().size() && toRemove > 0; i++) {
            ItemStack invStack = player.getInventory().getStack(i);
            if (invStack.isOf(resourceItem)) {
                int remove = Math.min(invStack.getCount(), toRemove);
                invStack.decrement(remove);
                toRemove -= remove;
            }
        }

        // Обновляем данные кирки
        int newAmount = Math.min(current + amount, max);
        resourcesData.setResource(category, newAmount);
        PickaxeResourcesManager.setData(stack, resourcesData);

        // Синхронизация с клиентом
        syncDataWithClient(player, resourcesData);
    }

    private static void handleExtractResources(ServerPlayerEntity player, String category) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof MinerPickaxeItem)) return;

        PickaxeResourcesData resourcesData = PickaxeResourcesManager.getData(stack);
        int amount = resourcesData.getResource(category);

        if (amount <= 0) return;

        Item resourceItem = getResourceItem(category);
        if (resourceItem == null) return;

        // Возвращаем ресурсы в инвентарь
        ItemStack resourceStack = new ItemStack(resourceItem, amount);
        if (!player.getInventory().insertStack(resourceStack)) {
            player.dropItem(resourceStack, false);
        }

        // Обнуляем ресурсы в кирке
        resourcesData.setResource(category, 0);
        PickaxeResourcesManager.setData(stack, resourcesData);

        // Синхронизация с клиентом
        syncDataWithClient(player, resourcesData);
    }

    private static void handleApplyUpgrades(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof MinerPickaxeItem)) return;

        PickaxeResourcesData resourcesData = PickaxeResourcesManager.getData(stack);

        // Сначала удаляем старые эффекты
        PickaxeResourcesManager.removePickaxeEffects(player);

        // Применяем новые эффекты и зачарования
        PickaxeResourcesManager.applyPermanentEffects(player, resourcesData);

        player.sendMessage(Text.literal("§aУлучшения успешно применены!"), true);
    }

    private static void syncDataWithClient(ServerPlayerEntity player, PickaxeResourcesData data) {
        PacketByteBuf buf = PacketByteBufs.create();
        data.write(buf);
        ServerPlayNetworking.send(player, new Identifier(FireMagicMod.MOD_ID, "sync_pickaxe_data"), buf);
    }

    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    private static int getMax(String category) {
        return switch (category) {
            case "coal" -> 128;
            case "iron" -> 128;
            case "gold" -> 64;
            case "diamond" -> 32;
            case "emerald" -> 16;
            default -> 0;
        };
    }

    private static String getCategoryName(String category) {
        return switch (category) {
            case "coal" -> "Уголь";
            case "iron" -> "Железо";
            case "gold" -> "Золото";
            case "diamond" -> "Алмаз";
            case "emerald" -> "Изумруд";
            default -> "";
        };
    }

    private static String getResourceName(String category) {
        return switch (category) {
            case "coal" -> "угля";
            case "iron" -> "железа";
            case "gold" -> "золота";
            case "diamond" -> "алмазов";
            case "emerald" -> "изумрудов";
            default -> "ресурсов";
        };
    }

    private static Item getResourceItem(String category) {
        return switch (category) {
            case "coal" -> Items.COAL;
            case "iron" -> Items.IRON_INGOT;
            case "gold" -> Items.GOLD_INGOT;
            case "diamond" -> Items.DIAMOND;
            case "emerald" -> Items.EMERALD;
            default -> null;
        };
    }
}