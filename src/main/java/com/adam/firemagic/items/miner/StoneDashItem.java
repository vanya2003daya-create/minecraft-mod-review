package com.adam.firemagic.items.miner;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StoneDashItem extends Item {
    private static final int COOLDOWN_TICKS = 200; // 10 секунд
    private static final float DAMAGE = 20.0f; // 10 сердец урона

    public StoneDashItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.pass(stack);
        }

        if (user instanceof ServerPlayerEntity serverPlayer) {
            PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

            if (!manaData.hasMinerSchool()) {
                serverPlayer.setStackInHand(hand, ItemStack.EMPTY);
                serverPlayer.sendMessage(Text.literal("§cПредмет исчез! Нужно изучить школу шахтёра."), true);
                return TypedActionResult.fail(ItemStack.EMPTY);
            }

            if (serverPlayer.getItemCooldownManager().isCoolingDown(this)) {
                return TypedActionResult.pass(stack);
            }

            boolean success;
            int y = serverPlayer.getBlockY();

            if (y < 55) {
                // ✅ СИЛЬНАЯ ВЕРСИЯ: 10 блоков с разрушением
                success = performStrongStoneDash((ServerWorld) world, serverPlayer, 10, 3);
            } else {
                // ✅ СЛАБАЯ ВЕРСИЯ: 5 блоков без разрушения (старая логика)
                success = performWeakStoneDash((ServerWorld) world, serverPlayer, 5);
            }

            if (success) {
                serverPlayer.getItemCooldownManager().set(this, COOLDOWN_TICKS);
                return TypedActionResult.success(stack);
            } else {
                serverPlayer.sendMessage(Text.literal("§cНе удалось использовать рывок!"), true);
                return TypedActionResult.fail(stack);
            }
        }

        return TypedActionResult.pass(stack);
    }

    // ✅ СИЛЬНАЯ ВЕРСИЯ (под землей)
    private boolean performStrongStoneDash(ServerWorld world, ServerPlayerEntity player, int maxDistance, int breakRadius) {
        try {
            Vec3d startPos = player.getPos();
            Vec3d lookDir = player.getRotationVec(1.0F);

            // Целевая позиция
            Vec3d targetPos = startPos.add(lookDir.multiply(maxDistance));

            // ✅ 1. ТЕЛЕПОРТАЦИЯ (как в вашей оригинальной версии)
            player.teleport(targetPos.x, targetPos.y, targetPos.z);

            // ✅ 2. РАЗРУШЕНИЕ БЛОКОВ
            breakBlocksAlongPath(world, startPos, targetPos, player, breakRadius);

            // ✅ 3. УРОН
            damageEntities(world, startPos, targetPos, player);

            // ✅ 4. ЗВУКИ: и каменный взрыв И телепорт
            spawnStrongEffects(world, startPos, targetPos);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ СЛАБАЯ ВЕРСИЯ (поверхность) - ВОЗВРАЩАЕМ СТАРУЮ ЛОГИКУ
    private boolean performWeakStoneDash(ServerWorld world, ServerPlayerEntity player, int maxDistance) {
        try {
            Vec3d startPos = player.getPos();
            Vec3d lookDir = player.getRotationVec(1.0F);

            // ✅ ИСПОЛЬЗУЕМ СТАРЫЙ МЕТОД ПОИСКА БЕЗОПАСНОЙ ПОЗИЦИИ
            Vec3d safePos = findSafePositionForWeakDash(world, startPos, lookDir, maxDistance, player);

            if (safePos == null || startPos.distanceTo(safePos) < 1.0) {
                return false;
            }

            // ✅ ТЕЛЕПОРТАЦИЯ (как раньше)
            player.teleport(safePos.x, safePos.y, safePos.z);

            // ✅ УРОН
            damageEntities(world, startPos, safePos, player);

            // ✅ ЗВУКИ: телепорт как в сильной версии
            spawnWeakEffects(world, startPos, safePos);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ СТАРЫЙ МЕТОД ПОИСКА БЕЗОПАСНОЙ ПОЗИЦИИ (чтобы не проваливался)
    private Vec3d findSafePositionForWeakDash(World world, Vec3d startPos, Vec3d direction, int maxDistance, PlayerEntity player) {
        // Нормализуем горизонтальное направление
        Vec3d horizontalDir = new Vec3d(direction.x, 0, direction.z).normalize();
        if (horizontalDir.lengthSquared() < 0.01) {
            return null;
        }

        double step = 0.5;
        double totalDistance = 0;
        Vec3d lastSafePos = startPos;

        while (totalDistance <= maxDistance) {
            Vec3d checkPos = startPos.add(horizontalDir.multiply(totalDistance));

            // Проверяем несколько ключевых точек по высоте
            boolean collisionFound = false;

            // Проверяем точки от ног до головы
            for (double heightCheck = 0.0; heightCheck <= 1.8; heightCheck += 0.5) {
                Vec3d checkPoint = checkPos.add(0, heightCheck, 0);
                BlockPos blockPos = BlockPos.ofFloored(checkPoint);
                BlockState blockState = world.getBlockState(blockPos);

                // Если блок не воздух - это препятствие
                if (!blockState.isAir() && blockState.isSolidBlock(world, blockPos)) {
                    collisionFound = true;
                    break;
                }
            }

            if (collisionFound) {
                // Нашли препятствие - останавливаемся на предыдущей позиции
                break;
            }

            // Проверяем bounding box игрока в этой позиции
            Box playerBox = player.getBoundingBox().offset(checkPos.subtract(startPos));
            if (world.isSpaceEmpty(playerBox) && !world.containsFluid(playerBox)) {
                lastSafePos = checkPos;
            } else {
                // Если bounding box пересекается с чем-то, останавливаемся
                break;
            }

            totalDistance += step;
        }

        // Если не сдвинулись с места
        if (lastSafePos.equals(startPos)) {
            return null;
        }

        return lastSafePos;
    }

    // Разрушение блоков на пути (только для сильной версии)
    private void breakBlocksAlongPath(ServerWorld world, Vec3d start, Vec3d end, ServerPlayerEntity player, int radius) {
        double stepSize = 0.7;
        double distance = start.distanceTo(end);
        int steps = Math.max(1, (int) (distance / stepSize));

        int radiusHalf = radius / 2;

        // Разрушаем блоки по всему пути
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3d currentPos = new Vec3d(
                    start.x + (end.x - start.x) * t,
                    start.y + (end.y - start.y) * t,
                    start.z + (end.z - start.z) * t
            );

            BlockPos center = BlockPos.ofFloored(currentPos);

            // Разрушаем блоки в кубе с заданным радиусом
            for (int dx = -radiusHalf; dx <= radiusHalf; dx++) {
                for (int dy = -radiusHalf; dy <= radiusHalf; dy++) { // Вертикальный радиус тоже
                    for (int dz = -radiusHalf; dz <= radiusHalf; dz++) {
                        BlockPos targetPos = center.add(dx, dy, dz);

                        // Не разрушаем блоки слишком далеко от пути
                        double distanceFromPath = Math.sqrt(dx*dx + dy*dy + dz*dz);
                        if (distanceFromPath <= radiusHalf + 0.5) {
                            breakBlockAtPos(world, targetPos, player);
                        }
                    }
                }
            }
        }
    }

    private void breakBlockAtPos(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        BlockState blockState = world.getBlockState(pos);

        if (!canBreakBlock(blockState, world, pos, player)) {
            return;
        }

        world.breakBlock(pos, true, player);

        // Эффекты разрушения
        world.spawnParticles(
                ParticleTypes.CLOUD,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                2, 0.2, 0.2, 0.2, 0.05
        );
    }

    private boolean canBreakBlock(BlockState blockState, World world, BlockPos pos, PlayerEntity player) {
        if (blockState.isAir()) return false;
        if (blockState.isOf(Blocks.WATER)) return false;
        if (blockState.isOf(Blocks.LAVA)) return false;
        if (blockState.isOf(Blocks.BEDROCK)) return false;
        if (blockState.isOf(Blocks.OBSIDIAN)) return false;
        if (blockState.isOf(Blocks.ENDER_CHEST)) return false;
        if (blockState.isOf(Blocks.SPAWNER)) return false;
        if (blockState.isOf(Blocks.BARRIER)) return false;

        float hardness = blockState.getHardness(world, pos);
        return hardness >= 0 && hardness <= 50.0f;
    }

    // Урон сущностям (одинаковый для обеих версий)
    private void damageEntities(World world, Vec3d start, Vec3d end, PlayerEntity source) {
        float DAMAGE_RADIUS = 2.5f;

        Box damageBox = new Box(start, end).expand(DAMAGE_RADIUS);
        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class,
                damageBox,
                entity -> entity != source && entity.isAttackable()
        );

        // Тип урона: "взрыв" для частичного игнорирования брони
        DamageSource damageSource = world.getDamageSources().explosion(source, source);

        for (LivingEntity entity : entities) {
            entity.damage(damageSource, DAMAGE);
        }
    }

    // ✅ ЭФФЕКТЫ ДЛЯ СИЛЬНОЙ ВЕРСИИ (и телепорт и каменный взрыв)
    private void spawnStrongEffects(ServerWorld world, Vec3d start, Vec3d end) {
        // Частицы по пути
        double distance = start.distanceTo(end);
        int particleCount = (int) (distance * 2);

        for (int i = 0; i < particleCount; i++) {
            double t = (double) i / particleCount;
            Vec3d pos = start.lerp(end, t);

            world.spawnParticles(
                    ParticleTypes.CRIT,
                    pos.x, pos.y + 0.5, pos.z,
                    1, 0.1, 0.1, 0.1, 0.1
            );

            // Добавляем частицы разрушения
            if (i % 3 == 0) {
                world.spawnParticles(
                        ParticleTypes.CLOUD,
                        pos.x, pos.y + 0.5, pos.z,
                        1, 0.2, 0.2, 0.2, 0.05
                );
            }
        }

        // ✅ ЗВУКИ: И КАМЕННЫЙ ВЗРЫВ И ТЕЛЕПОРТ
        world.playSound(null, start.x, start.y, start.z,
                SoundEvents.BLOCK_STONE_BREAK,
                SoundCategory.PLAYERS, 0.8f, 1.0f);

        world.playSound(null, end.x, end.y, end.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.7f, 1.2f);
    }

    // ✅ ЭФФЕКТЫ ДЛЯ СЛАБОЙ ВЕРСИИ (телепорт как в сильной)
    private void spawnWeakEffects(ServerWorld world, Vec3d start, Vec3d end) {
        // Простые частицы
        double distance = start.distanceTo(end);
        int particleCount = (int) (distance * 2);

        for (int i = 0; i < particleCount; i++) {
            double t = (double) i / particleCount;
            Vec3d pos = start.lerp(end, t);

            world.spawnParticles(
                    ParticleTypes.CLOUD,
                    pos.x, pos.y + 0.5, pos.z,
                    1, 0.1, 0.1, 0.1, 0.02
            );
        }

        // ✅ ЗВУК ТЕЛЕПОРТА КАК В СИЛЬНОЙ ВЕРСИИ
        world.playSound(null, end.x, end.y, end.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.7f, 1.2f);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("§6Каменный рывок").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("§7• Версия зависит от высоты:").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("§8  Под землей (Y<55): §fРазрушает блоки, 10 блоков").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("§8  На поверхности (Y≥55): §fБез разрушения, 5 блоков").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("§7• Урон: §c10 сердец ⚔️").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("§7• Перезарядка: §e10 секунд").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("§8Требует школу шахтёра").formatted(Formatting.DARK_GRAY));
    }
}