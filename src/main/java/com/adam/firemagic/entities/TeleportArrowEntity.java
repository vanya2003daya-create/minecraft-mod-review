package com.adam.firemagic.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TeleportArrowEntity extends PersistentProjectileEntity {

    public TeleportArrowEntity(EntityType<? extends PersistentProjectileEntity> type, World world) {
        super(type, world);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    public TeleportArrowEntity(World world, LivingEntity owner) {
        this(ModEntities.TELEPORT_ARROW, world);
        this.setOwner(owner);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    @Override
    protected ItemStack asItemStack() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected void onHit(LivingEntity target) {
        teleportPlayer();
        this.discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult hitResult) {
        teleportPlayer();
        this.discard();
    }

    private void teleportPlayer() {
        if (!this.getWorld().isClient() && this.getOwner() instanceof ServerPlayerEntity player) {
            // Просто телепортируем - без проверок!
            player.requestTeleport(this.getX(), this.getY(), this.getZ());

            // Эффекты
            ServerWorld world = (ServerWorld) this.getWorld();
            world.spawnParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY(), this.getZ(),
                    20, 0.5, 0.5, 0.5, 0);

            world.playSound(null, this.getBlockPos(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Частицы при полете (опционально)
        if (this.getWorld().isClient()) {
            for (int i = 0; i < 2; i++) {
                this.getWorld().addParticle(ParticleTypes.END_ROD,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.2,
                        this.getY() + (this.random.nextDouble() - 0.5) * 0.2,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.2,
                        0, 0, 0);
            }
        }
    }
}