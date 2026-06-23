package com.adam.firemagic.items.necromancer;

import com.adam.firemagic.entities.NecroZombieEntity;
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
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class ZombieMinionEggItem extends Item {
    private static final int MANA_COST = 19; // Исправлено: 20 маны
    private static final int COOLDOWN_TICKS = 1200; // 60 секунд

    public ZombieMinionEggItem(Settings settings) {
        super(settings.maxCount(1).maxDamageIfAbsent(1)); // Максимум 1 в слоте
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();

        if (world.isClient() || player == null) {
            return ActionResult.PASS;
        }

        // 🔧 Проверка ServerPlayerEntity
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.FAIL;
        }

        ServerWorld serverWorld = (ServerWorld) world;

        // Проверяем, является ли игрок некромантом
        PlayerManaData manaData = ManaManager.getServerData(serverPlayer);
        if (!manaData.hasNecromancerSchool()) {
            serverPlayer.sendMessage(Text.literal("§cТолько некроманты могут использовать это!"), true);
            return ActionResult.FAIL;
        }

        // Проверка маны
        if (manaData.getMana() < MANA_COST) {
            serverPlayer.sendMessage(Text.literal("§cНедостаточно маны! Нужно " + MANA_COST + " маны"), true);
            return ActionResult.FAIL;
        }

        // Проверка кулдауна на предмете
        if (serverPlayer.getItemCooldownManager().isCoolingDown(this)) {
            serverPlayer.sendMessage(Text.literal("§cЯйцо зомби перезаряжается!"), true);
            return ActionResult.FAIL;
        }

        // Проверяем лимит зомби
        if (manaData.hasZombieMinion()) {
            // Проверяем, жив ли существующий зомби
            Entity existingZombie = serverWorld.getEntity(manaData.getZombieMinionId());
            if (existingZombie != null && existingZombie.isAlive()) {
                serverPlayer.sendMessage(Text.literal("§cУ вас уже есть живой зомби-прислужник!"), true);
                return ActionResult.FAIL;
            } else {
                // Зомби умер, очищаем UUID
                manaData.clearZombieMinion();
                ManaManager.setServerData(serverPlayer, manaData);
            }
        }

        // Определяем позицию спавна
        BlockPos blockPos = context.getBlockPos();
        Direction direction = context.getSide();
        BlockPos spawnPos = blockPos.offset(direction);

        // Спавним кастомного зомби
        NecroZombieEntity zombie = new NecroZombieEntity(serverWorld);
        zombie.setOwner(serverPlayer);
        zombie.refreshPositionAndAngles(
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                player.getYaw(),
                player.getPitch()
        );

        // Регистрируем зомби
        if (serverWorld.spawnEntity(zombie)) {
            // Устанавливаем кулдаун на предмет (60 секунд)
            serverPlayer.getItemCooldownManager().set(this, COOLDOWN_TICKS);

            // Сохраняем UUID зомби в данных игрока
            manaData.setZombieMinionId(zombie.getUuid());
            manaData.setMana(manaData.getMana() - MANA_COST);
            ManaManager.setServerData(serverPlayer, manaData);

            // Игровое событие
            world.emitGameEvent(serverPlayer, GameEvent.ENTITY_PLACE, blockPos);

            // Сообщение об успехе
            serverPlayer.sendMessage(Text.literal("§aЗомби призван! Мана: -" + MANA_COST), true);

            // Яйцо НЕ расходуется, только устанавливается кулдаун
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        // 🔧 Проверка ServerPlayerEntity
        if (user instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.literal("§7Используйте яйцо на земле для призыва зомби"), true);
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("§2Яйцо призыва зомби").formatted(net.minecraft.util.Formatting.DARK_GREEN));
        tooltip.add(Text.literal("§7Призывает зомби-прислужника").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§7Стоимость: §b" + MANA_COST + " маны").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§7Кулдаун: §e60 секунд").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§8◇ Не атакует своего хозяина").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        tooltip.add(Text.literal("§8◇ Защищает хозяина от врагов").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        tooltip.add(Text.literal("§8◇ Лимит: 1 зомби на некроманта").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        tooltip.add(Text.literal("§8◇ Не расходуется при использовании").formatted(net.minecraft.util.Formatting.DARK_GRAY));
    }
}