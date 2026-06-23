package com.adam.firemagic.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PiercingArrowEntity extends PersistentProjectileEntity {
    private final Set<UUID> hitEntities = new HashSet<>();
    private int lifeTicks = 0;
    private static final int MAX_LIFETIME = 160; // 8 секунд
    private static final float DAMAGE = 22.0f; // 8 сердец

    // Конструктор для регистрации
    public PiercingArrowEntity(EntityType<? extends PersistentProjectileEntity> type, World world) {
        super(type, world);
        this.setNoClip(true);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    // Конструктор для создания в игре
    public PiercingArrowEntity(World world, LivingEntity owner) {
        this(ModEntities.PIERCING_ARROW, world);
        this.setOwner(owner);
        this.setNoClip(true);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    // ========== ВАЖНО: Реализуем абстрактный метод ==========
    @Override
    protected ItemStack asItemStack() {
        return new ItemStack(Items.ARROW); // Возвращаем обычную стрелу для отображения
    }

    @Override
    public void tick() {
        if (this.getWorld().isClient()) {
            // Клиентская часть: только частицы
            spawnTrailParticles();
            super.tick();
            return;
        }

        // Серверная часть: логика
        lifeTicks++;

        // Проверяем время жизни
        if (lifeTicks >= MAX_LIFETIME) {
            removeWithEffects();
            return;
        }

        // Сохраняем предыдущую позицию для проверки столкновений
        Vec3d previousPos = this.getPos();

        // Двигаем стрелу стандартным способом
        super.tick();

        // Проверяем столкновения с сущностями
        checkEntityCollisions(previousPos, this.getPos());

        // Визуальные эффекты на сервере
        if (lifeTicks % 2 == 0) {
            spawnServerParticles();
        }
    }

    private void checkEntityCollisions(Vec3d startPos, Vec3d endPos) {
        Box collisionBox = new Box(startPos, endPos).expand(0.3);

        this.getWorld().getOtherEntities(this, collisionBox, entity ->
                entity instanceof LivingEntity &&
                        entity.isAlive() &&
                        entity != this.getOwner() &&
                        !entity.isSpectator() &&
                        !hitEntities.contains(entity.getUuid())
        ).forEach(entity -> {
            if (entity.getBoundingBox().intersects(collisionBox)) {
                handleEntityHit((LivingEntity) entity);
            }
        });
    }

    private void handleEntityHit(LivingEntity entity) {
        DamageSource damageSource = this.getWorld().getDamageSources().arrow(this, this.getOwner());

        if (entity.damage(damageSource, DAMAGE)) {
            spawnHitEffects(entity.getPos());
            hitEntities.add(entity.getUuid());

            System.out.println("[DEBUG] Пронзающая стрела поразила: " +
                    entity.getType().getTranslationKey());
        }
    }

    private void spawnTrailParticles() {
        this.getWorld().addParticle(ParticleTypes.CRIT,
                this.getX(), this.getY(), this.getZ(),
                0, 0, 0);
    }

    private void spawnServerParticles() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0, 0, 0, 0);
        }
    }

    private void spawnHitEffects(Vec3d pos) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null,
                    this.getBlockPos(),
                    SoundEvents.ENTITY_ARROW_HIT,
                    net.minecraft.sound.SoundCategory.NEUTRAL,
                    0.5f, 1.0f);

            serverWorld.spawnParticles(ParticleTypes.CRIT,
                    pos.x, pos.y + 0.5, pos.z,
                    3, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void removeWithEffects() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    5, 0.1, 0.1, 0.1, 0.01);
        }
        this.discard();
        System.out.println("[DEBUG] Пронзающая стрела удалена по таймеру");
    }

    // ========== Отключаем стандартное поведение ==========

    protected void onHit(HitResult hitResult) {
        // Ничего не делаем - стрела игнорирует столкновения
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        // Ничего не делаем - урон обрабатывается в checkEntityCollisions
        // ОБРАТИТЕ ВНИМАНИЕ: Этот метод вызывается системой Minecraft
        // Мы оставляем его пустым, чтобы отменить стандартное поведение
    }

    @Override
    protected void onBlockHit(net.minecraft.util.hit.BlockHitResult blockHitResult) {
        // Ничего не делаем - стрела пролетает сквозь блоки
    }
}