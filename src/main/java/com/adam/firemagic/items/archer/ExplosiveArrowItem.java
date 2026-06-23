package com.adam.firemagic.items.archer;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ExplosiveArrowItem extends Item {
    private static final int MANA_COST = 3;
    private static final int RELOAD_COOLDOWN = 100; // 5 секунд

    public ExplosiveArrowItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (user instanceof ServerPlayerEntity serverPlayer) {
            if (serverPlayer.getItemCooldownManager().isCoolingDown(this)) {
                serverPlayer.sendMessage(Text.literal("§cПодождите немного перед следующим использованием..."), true);
                return TypedActionResult.fail(stack);
            }

            PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

            if (!manaData.hasArcherSchool()) {
                serverPlayer.sendMessage(Text.literal("§cВы не из школы лучников!"), true);
                return TypedActionResult.fail(stack);
            }

            if (manaData.getMana() < MANA_COST) {
                serverPlayer.sendMessage(Text.literal("§cНедостаточно маны! Нужно: " + MANA_COST), true);
                return TypedActionResult.fail(stack);
            }

            // ✅ Списание маны
            manaData.setMana(manaData.getMana() - MANA_COST);
            ManaManager.setServerData(serverPlayer, manaData);

            // ✅ Установка временного флага (не сохраняется между сессиями)
            manaData.setExplosiveArrowReady(true);
            ManaManager.setServerData(serverPlayer, manaData);

            // Установка кулдауна
            serverPlayer.getItemCooldownManager().set(this, RELOAD_COOLDOWN);

            // Стрела НЕ исчезает!
            world.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.5f, 1.5f);

            serverPlayer.sendMessage(Text.literal("§eВзрывная стрела §aготова к использованию!"), true);
        }

        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<net.minecraft.text.Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(net.minecraft.text.Text.literal(""));
        tooltip.add(net.minecraft.text.Text.literal("§c§lВзрывная стрела").formatted(net.minecraft.util.Formatting.RED));
        tooltip.add(net.minecraft.text.Text.literal("§7- При попадании создает взрыв").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(net.minecraft.text.Text.literal("§7- Потребляет: §b3 маны").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(net.minecraft.text.Text.literal("§7- Кулдаун: §65 секунд").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(net.minecraft.text.Text.literal(""));
        tooltip.add(net.minecraft.text.Text.literal("§eПКМ: §aАктивировать взрывную стрелу").formatted(net.minecraft.util.Formatting.YELLOW));
        tooltip.add(net.minecraft.text.Text.literal("§8(Следующая выпущенная стрела будет взрывной)").formatted(net.minecraft.util.Formatting.DARK_GRAY));
    }
}