package com.adam.firemagic.items.necromancer;

import com.adam.firemagic.entities.NecroSkeletonEntity;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class SkeletonMinionEggItem extends Item {
    private static final int MANA_COST = 19; // Исправлено: 20 маны
    private static final int COOLDOWN_TICKS = 1200; // 60 секунд

    public SkeletonMinionEggItem(Settings settings) {
        super(settings.maxCount(1).maxDamageIfAbsent(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        Direction direction = context.getSide();

        if (world.isClient() || player == null) {
            return ActionResult.PASS;
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

        // Проверка школы некроманта
        if (!manaData.hasNecromancerSchool()) {
            player.sendMessage(Text.literal("§cТолько некроманты могут призывать скелетов!"), true);
            return ActionResult.FAIL;
        }

        // Проверка кулдауна на предмете
        if (serverPlayer.getItemCooldownManager().isCoolingDown(this)) {
            serverPlayer.sendMessage(Text.literal("§cЯйцо скелета перезаряжается!"), true);
            return ActionResult.FAIL;
        }

        // Проверка лимита (не более 1 скелета)
        if (manaData.hasSkeletonMinion()) {
            // Проверяем, жив ли существующий скелет
            Entity existingSkeleton = ((ServerWorld) world).getEntity(manaData.getSkeletonMinionId());
            if (existingSkeleton != null && existingSkeleton.isAlive()) {
                player.sendMessage(Text.literal("§cУ вас уже есть живой скелет!"), true);
                return ActionResult.FAIL;
            } else {
                // Скелет умер, очищаем ID
                manaData.clearSkeletonMinion();
            }
        }

        // Проверка маны
        if (manaData.getMana() < MANA_COST) {
            player.sendMessage(Text.literal("§cНедостаточно маны! Нужно " + MANA_COST + " маны"), true);
            return ActionResult.FAIL;
        }

        // Позиция для спавна
        BlockPos spawnPos = blockPos.offset(direction);

        // Создаем скелета
        NecroSkeletonEntity skeleton = new NecroSkeletonEntity(world);
        skeleton.setOwner(player);
        skeleton.refreshPositionAndAngles(
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                player.getYaw(),
                player.getPitch()
        );

        // Спавним
        if (world.spawnEntity(skeleton)) {
            // Устанавливаем кулдаун на предмет (60 секунд)
            serverPlayer.getItemCooldownManager().set(this, COOLDOWN_TICKS);

            // Сохраняем UUID скелета
            manaData.setSkeletonMinionId(skeleton.getUuid());

            // Тратим ману
            manaData.setMana(manaData.getMana() - MANA_COST);
            ManaManager.setServerData(serverPlayer, manaData);

            // Сообщение об успехе
            player.sendMessage(Text.literal("§aСкелет призван! Мана: -" + MANA_COST), true);

            // Яйцо НЕ расходуется, только устанавливается кулдаун
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("§5Яйцо скелета-прислужника").formatted(net.minecraft.util.Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("§7Использование: ПКМ по блоку для призыва").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§7Стоимость: §b" + MANA_COST + " маны").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§7Кулдаун: §e60 секунд").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§8◇ Максимум 1 скелет на игрока").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        tooltip.add(Text.literal("§8◇ Не расходуется при использовании").formatted(net.minecraft.util.Formatting.DARK_GRAY));
    }
}