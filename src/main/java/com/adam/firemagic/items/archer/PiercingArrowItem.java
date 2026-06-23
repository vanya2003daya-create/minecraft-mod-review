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

public class PiercingArrowItem extends Item {
    private static final int MANA_COST = 4;
    private static final int RELOAD_COOLDOWN = 160; // 8 секунд

    public PiercingArrowItem(Settings settings) {
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

            // Списание маны
            manaData.setMana(manaData.getMana() - MANA_COST);
            ManaManager.setServerData(serverPlayer, manaData);

            // Установка флага готовности пронзающей стрелы
            manaData.setPiercingArrowReady(true);
            ManaManager.setServerData(serverPlayer, manaData);

            // Сброс флага взрывной стрелы
            if (manaData.isExplosiveArrowReady()) {
                manaData.setExplosiveArrowReady(false);
                ManaManager.setServerData(serverPlayer, manaData);
            }

            // Установка кулдауна
            serverPlayer.getItemCooldownManager().set(this, RELOAD_COOLDOWN);

            // Воспроизведение звука
            world.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.ITEM_CROSSBOW_LOADING_START, SoundCategory.PLAYERS, 1.0f, 1.0f);

            serverPlayer.sendMessage(Text.literal("§bПронзающая стрела §aготова к использованию!"), true);
            System.out.println("[DEBUG] ✅ Пронзающая стрела активирована игроком: " + serverPlayer.getName().getString());
        }

        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<net.minecraft.text.Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(net.minecraft.text.Text.literal(""));
        tooltip.add(net.minecraft.text.Text.literal("§b§lПронзающая стрела").formatted(net.minecraft.util.Formatting.AQUA));
        tooltip.add(net.minecraft.text.Text.literal("§7- Пролетает сквозь §eвсе блоки§7").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(net.minecraft.text.Text.literal("§7- Наносит §c8 HP урона§7 (4 сердца)").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(net.minecraft.text.Text.literal("§7- Исчезает через §610 секунд§7").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(net.minecraft.text.Text.literal("§7- Потребляет: §b4 маны").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(net.minecraft.text.Text.literal("§7- Кулдаун: §68 секунд").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(net.minecraft.text.Text.literal(""));
        tooltip.add(net.minecraft.text.Text.literal("§eПКМ: §aАктивировать пронзающую стрелу").formatted(net.minecraft.util.Formatting.YELLOW));
        tooltip.add(net.minecraft.text.Text.literal("§8(Следующая выпущенная стрела будет пронзающей)").formatted(net.minecraft.util.Formatting.DARK_GRAY));
    }
}