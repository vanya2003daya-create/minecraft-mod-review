package com.adam.firemagic.items;

import com.adam.firemagic.blocks.ModBlocks;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

public class FireDashItem extends Item {
    private static final int MANA_COST = 5;
    private static final int MAX_DISTANCE = 10;
    private static final int COOLDOWN_TICKS = 60;
    private static final float DAMAGE = 2.0f;
    private static final float DAMAGE_RADIUS = 1.5f;
    private static final double MIN_TELEPORT_DISTANCE = 0.5;
    private static final double STEP_SIZE = 0.5;

    public FireDashItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        // === ПРОВЕРКА ШКОЛЫ МАГИИ ===
        PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

        // Проверка перезарядки
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }

        // Проверка маны
        if (manaData.getMana() < MANA_COST) {
            player.sendMessage(Text.translatable("message.firemagic.insufficient_mana"), true);
            return TypedActionResult.fail(stack);
        }

        try {
            Vec3d lookDir = player.getRotationVector();
            Vec3d horizontalDir = new Vec3d(lookDir.x, 0, lookDir.z).normalize();

            if (horizontalDir.lengthSquared() < 0.01) {
                return TypedActionResult.pass(stack);
            }

            Vec3d startPos = player.getPos();
            Vec3d endPos = startPos.add(horizontalDir.multiply(MAX_DISTANCE));

            BlockHitResult hitResult = world.raycast(new RaycastContext(
                    startPos,
                    endPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player
            ));

            Vec3d actualEndPos = (hitResult.getType() == HitResult.Type.BLOCK)
                    ? hitResult.getPos().subtract(horizontalDir.multiply(0.5))
                    : endPos;

            if (startPos.distanceTo(actualEndPos) < MIN_TELEPORT_DISTANCE) {
                return TypedActionResult.pass(stack);
            }

            if (!isSafeTeleportPosition(world, actualEndPos, player)) {
                return TypedActionResult.pass(stack);
            }

            // Успешная телепортация
            player.teleport(actualEndPos.x, actualEndPos.y, actualEndPos.z);
            manaData.setMana(manaData.getMana() - MANA_COST);
            ManaManager.setServerData(serverPlayer, manaData);

            createFireTrail((ServerWorld) world, startPos, actualEndPos, horizontalDir);
            damageEntities(world, startPos, actualEndPos, player);
            spawnEffects((ServerWorld) world, startPos, actualEndPos);

        } catch (Exception e) {
            e.printStackTrace();
            return TypedActionResult.pass(stack);
        }

        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        return TypedActionResult.success(stack);
    }

    private boolean isSafeTeleportPosition(World world, Vec3d pos, PlayerEntity player) {
        Box playerBox = player.getBoundingBox().offset(pos.subtract(player.getPos()));
        return world.isSpaceEmpty(playerBox) && !world.containsFluid(playerBox);
    }

    private void createFireTrail(ServerWorld world, Vec3d start, Vec3d end, Vec3d direction) {
        double distance = start.distanceTo(end);
        int steps = (int) (distance / STEP_SIZE);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3d point = start.lerp(end, t);
            BlockPos firePos = findValidFirePosition(world, point);

            if (firePos != null && world.getBlockState(firePos).isReplaceable()) {
                world.setBlockState(firePos, ModBlocks.ETERNAL_FIRE.getDefaultState(), 3);
            }
        }
    }

    private BlockPos findValidFirePosition(World world, Vec3d position) {
        for (int yOffset = 0; yOffset > -5; yOffset--) {
            BlockPos groundPos = BlockPos.ofFloored(position.x, position.y + yOffset, position.z);
            BlockPos firePos = groundPos.up();

            if (world.getBlockState(firePos).isAir() &&
                    ModBlocks.ETERNAL_FIRE.getDefaultState().canPlaceAt(world, firePos)) {
                return firePos;
            }
        }
        return null;
    }

    private void damageEntities(World world, Vec3d start, Vec3d end, PlayerEntity source) {
        Box damageBox = new Box(start, end).expand(DAMAGE_RADIUS);
        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class,
                damageBox,
                entity -> entity != source && entity.isAttackable()
        );

        for (LivingEntity entity : entities) {
            entity.damage(world.getDamageSources().magic(), DAMAGE);
        }
    }

    private void spawnEffects(ServerWorld world, Vec3d start, Vec3d end) {
        double distance = start.distanceTo(end);
        int particleCount = Math.min(15, (int) (distance * 3));

        for (int i = 0; i < particleCount; i++) {
            double t = (double) i / particleCount;
            Vec3d pos = start.lerp(end, t);
            world.spawnParticles(
                    ParticleTypes.FLAME,
                    pos.x,
                    pos.y + 0.5,
                    pos.z,
                    1,
                    0.1,
                    0.1,
                    0.1,
                    0.01
            );
        }

        world.spawnParticles(
                ParticleTypes.SMOKE,
                start.x,
                start.y,
                start.z,
                8,
                0.3,
                0.3,
                0.3,
                0.1
        );

        world.spawnParticles(
                ParticleTypes.SMOKE,
                end.x,
                end.y,
                end.z,
                8,
                0.3,
                0.3,
                0.3,
                0.1
        );

        world.playSound(
                null,
                start.x,
                start.y,
                start.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.7f,
                1.2f
        );

        world.playSound(
                null,
                end.x,
                end.y,
                end.z,
                SoundEvents.BLOCK_FIRE_AMBIENT,
                SoundCategory.PLAYERS,
                0.5f,
                1.5f
        );
    }
}