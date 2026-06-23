// 📄 EnhancerInfoBookItem.java (обновленная версия)
package com.adam.firemagic.items.enhanced;

import com.adam.firemagic.client.gui.EnhancerBookScreen;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class EnhancerInfoBookItem extends Item {
    public EnhancerInfoBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            // КЛИЕНТ: открываем GUI напрямую
            MinecraftClient.getInstance().setScreen(new EnhancerBookScreen());
            return TypedActionResult.success(stack);
        }

        // СЕРВЕР: проверяем владельца и школу
        if (user instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound nbt = stack.getOrCreateNbt();

            // Устанавливаем владельца при первом использовании
            if (!nbt.contains("OwnerUUID")) {
                nbt.putUuid("OwnerUUID", serverPlayer.getUuid());
                nbt.putString("OwnerName", serverPlayer.getName().getString());
                stack.setNbt(nbt);
            }

            // Проверяем, принадлежит ли книга игроку
            if (!nbt.getUuid("OwnerUUID").equals(serverPlayer.getUuid())) {
                serverPlayer.sendMessage(Text.literal("§cЭта книга принадлежит другому мастеру!"), true);
                return TypedActionResult.fail(stack);
            }

            // Проверка школы улучшения (опционально)
            PlayerManaData manaData = ManaManager.getServerData(serverPlayer);
            if (!manaData.hasEnhancerSchool()) {
                serverPlayer.sendMessage(Text.literal("§cВы не владеете искусством улучшений!"), true);
                return TypedActionResult.fail(stack);
            }

            // Отправляем пакет для открытия GUI
            ServerPlayNetworking.send(
                    serverPlayer,
                    new Identifier("firemagic", "open_enhancer_book"),
                    PacketByteBufs.empty()
            );
        }

        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, net.minecraft.world.World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        NbtCompound nbt = stack.getNbt();

        if (nbt != null && nbt.contains("OwnerName")) {
            tooltip.add(Text.literal("§7Владелец: §f" + nbt.getString("OwnerName"))
                    .formatted(net.minecraft.util.Formatting.GRAY));
        } else {
            tooltip.add(Text.literal("§7Не принадлежит никому")
                    .formatted(net.minecraft.util.Formatting.GRAY));
        }

        tooltip.add(Text.literal("§6§lКнига рецептов улучшения").formatted(net.minecraft.util.Formatting.GOLD));
        tooltip.add(Text.literal("§7ПКМ: Открыть книгу рецептов").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§7Содержит 3 вкладки с рецептами").formatted(net.minecraft.util.Formatting.GRAY));
    }
}