package com.adam.firemagic.items.archer;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArcherRingItem extends Item {

    private static final Map<UUID, Long> ACTIVE_RINGS = new HashMap<>();
    private static final int MANA_COST = 10;
    private static final int DURATION_TICKS = 30 * 20; // 30 секунд
    private static final int COOLDOWN_TICKS = 5 * 20;  // 5 секунд для теста
    private static final int RADIUS = 30;

    public ArcherRingItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;

        // Проверка маны (БЕЗ ПРОВЕРКИ ШКОЛЫ - может использовать любой)
        PlayerManaData manaData = ManaManager.getServerData(serverPlayer);
        if (manaData.getMana() < MANA_COST) {
            serverPlayer.sendMessage(Text.literal("§cНедостаточно маны! Нужно " + MANA_COST + " единиц"), true);
            return TypedActionResult.fail(stack);
        }

        // Проверка кулдауна предмета
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        // Трата маны
        manaData.setMana(manaData.getMana() - MANA_COST);
        ManaManager.setServerData(serverPlayer, manaData);

        // Активация эффектов
        activateRingEffects(serverPlayer);

        // Установка кулдауна предмета
        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        // Звук активации
        world.playSound(
                null,
                user.getX(),
                user.getY(),
                user.getZ(),
                SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
        );

        return TypedActionResult.success(stack);
    }

    private void activateRingEffects(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        UUID playerId = player.getUuid();

        // 1. Даём эффект скорости
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED,
                DURATION_TICKS,
                0,
                false,
                true,
                true
        ));

        // 2. Активируем подсветку сущностей
        long endTime = world.getTime() + DURATION_TICKS;
        ACTIVE_RINGS.put(playerId, endTime);

        // 3. Визуальные эффекты при активации
        spawnActivationParticles(world, player);

        player.sendMessage(Text.literal("§aКольцо активировано! Сущности подсвечены на 30 секунд"), true);
    }

    private void spawnActivationParticles(ServerWorld world, PlayerEntity player) {
        for (int i = 0; i < 20; i++) {
            double x = player.getX() + (world.random.nextDouble() - 0.5) * 2;
            double y = player.getY() + world.random.nextDouble() * 2;
            double z = player.getZ() + (world.random.nextDouble() - 0.5) * 2;

            world.spawnParticles(
                    ParticleTypes.GLOW,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0.1
            );
        }
    }

    public static void tickActiveRing(ServerPlayerEntity player, long currentTime) {
        if (player == null || !player.isAlive()) {
            clearPlayerRing(player.getUuid());
            return;
        }

        Long endTime = ACTIVE_RINGS.get(player.getUuid());
        if (endTime == null) return;

        if (currentTime >= endTime) {
            ACTIVE_RINGS.remove(player.getUuid());
            player.sendMessage(Text.literal("§7Действие кольца закончилось"), true);
            return;
        }

        if (currentTime % 5 == 0) {
            highlightEntities(player.getServerWorld(), player);
        }
    }

    private static void highlightEntities(ServerWorld world, ServerPlayerEntity player) {
        Box box = new Box(
                player.getX() - RADIUS,
                player.getY() - RADIUS,
                player.getZ() - RADIUS,
                player.getX() + RADIUS,
                player.getY() + RADIUS,
                player.getZ() + RADIUS
        );

        for (Entity entity : world.getOtherEntities(player, box)) {
            if (entity instanceof LivingEntity livingEntity && entity != player) {
                livingEntity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.GLOWING,
                        40,
                        0,
                        false,
                        false,
                        false
                ));

                if (world.getTime() % 10 == 0) {
                    spawnHighlightParticles(world, livingEntity);
                }
            }
        }
    }

    private static void spawnHighlightParticles(ServerWorld world, LivingEntity entity) {
        double x = entity.getX();
        double y = entity.getY() + entity.getHeight();
        double z = entity.getZ();

        boolean isHostile = !(entity instanceof PlayerEntity);

        world.spawnParticles(
                isHostile ? ParticleTypes.ANGRY_VILLAGER : ParticleTypes.HAPPY_VILLAGER,
                x, y, z,
                1,
                entity.getWidth() / 2, 0, entity.getWidth() / 2,
                0.05
        );
    }

    public static boolean hasActiveRing(ServerPlayerEntity player) {
        return ACTIVE_RINGS.containsKey(player.getUuid());
    }

    public static void clearPlayerRing(UUID playerId) {
        ACTIVE_RINGS.remove(playerId);
    }

    public static boolean isRingActiveForPlayer(PlayerEntity player) {
        return player != null && ACTIVE_RINGS.containsKey(player.getUuid());
    }

    public static int getRemainingTime(PlayerEntity player) {
        if (player == null) return 0;
        Long endTime = ACTIVE_RINGS.get(player.getUuid());
        if (endTime == null) return 0;
        return (int) Math.max(0, endTime - player.getWorld().getTime());
    }

    public static void deactivateRing(PlayerEntity player) {
        if (player != null) {
            ACTIVE_RINGS.remove(player.getUuid());
            player.removeStatusEffect(StatusEffects.GLOWING);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("§6Кольцо Соколиного Взора").formatted(net.minecraft.util.Formatting.GOLD));
        tooltip.add(Text.literal("§7ПКМ: §fПодсвечивает всех сущностей").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§7в радиусе 30 блоков").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§7Скорость I на 30 сек").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§cСтоимость: 10 маны").formatted(net.minecraft.util.Formatting.RED));
        tooltip.add(Text.literal("§7Могут использовать все, но крафт").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§7доступен только лучникам").formatted(net.minecraft.util.Formatting.GRAY));
    }
}