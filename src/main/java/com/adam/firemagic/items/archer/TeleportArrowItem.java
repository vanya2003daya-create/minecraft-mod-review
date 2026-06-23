// items/TeleportArrowItem.java
package com.adam.firemagic.items.archer;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class TeleportArrowItem extends Item {
    public TeleportArrowItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        PlayerManaData manaData = ManaManager.getServerData(player);

        // Проверка школы лучника
        if (!manaData.hasArcherSchool()) {
            player.sendMessage(Text.literal("§cВы не изучали школу лучника!"), true);
            return TypedActionResult.fail(stack);
        }

        // Проверка маны (4 единицы)
        if (!manaData.consumeMana(4)) {
            player.sendMessage(Text.literal("§cНедостаточно маны! Нужно 4 единицы."), true);
            return TypedActionResult.fail(stack);
        }

        // Устанавливаем флаг, что следующая стрела будет телепортирующей
        manaData.setTeleportArrowReady(true);
        ManaManager.setServerData(player, manaData);

        // НЕ УДАЛЯЕМ ПРЕДМЕТ!
        // Просто устанавливаем кулдаун на сам предмет
        player.getItemCooldownManager().set(this, 8 * 20); // 8 секунд в тиках

        player.sendMessage(Text.literal("§aТелепортирующая стрела активирована! Следующий выстрел будет телепортировать."), true);
        player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

        return TypedActionResult.success(stack);
    }
}