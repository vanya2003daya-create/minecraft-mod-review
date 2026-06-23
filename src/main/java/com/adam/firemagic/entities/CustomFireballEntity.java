package com.adam.firemagic.entities;

import com.adam.firemagic.blocks.ModBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class CustomFireballEntity extends ProjectileEntity {
    // МОЩНЫЕ ПАРАМЕТРЫ (немного уменьшены для баланса)
    private static final float EXPLOSION_POWER = 3.5f;       // Большой радиус взрыва
    private static final float DIRECT_HIT_DAMAGE = 14.0f;    // 7 сердец за прямое попадание (было 18 = 9 сердец)
    private static final float MAX_DAMAGE = 16.0f;          // 8 сердец максимум от взрыва (было 20 = 10 сердец)
    private static final float DAMAGE_RADIUS = 7.0f;        // Большой радиус урона

    private boolean hasCollided = false;
    private final Random random = new Random();

    public CustomFireballEntity(EntityType<? extends ProjectileEntity> type, World world) {
        super(type, world);
    }

    public CustomFireballEntity(World world, LivingEntity owner) {
        super(ModEntities.CUSTOM_FIREBALL, world);
        this.setOwner(owner);
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker() {}

    @Override
    public void tick() {
        // Сохраняем старое положение
        Vec3d oldPos = this.getPos();
        // Добавляем скорость к позиции
        Vec3d newPos = oldPos.add(this.getVelocity());
        this.setPosition(newPos);

        // Частицы на клиенте
        if (this.getWorld().isClient()) {
            spawnFlightParticles();
        }

        // Проверка коллизий только на сервере
        if (!this.getWorld().isClient) {
            HitResult hitResult = ProjectileUtil.getCollision(this, entity -> {
                // Проверяем, что сущность жива и не является хозяином
                return !entity.isSpectator() && entity.isAlive() && entity != this.getOwner();
            });

            // Если попали во что-то - обрабатываем столкновение
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.onCollision(hitResult);
            }
        }

        // Автоматическое удаление через 4 секунды (80 тиков)
        if (this.age++ > 80) {
            this.discard();
        }
    }

    // Красивые частицы при полете
    private void spawnFlightParticles() {
        World world = this.getWorld();

        // Огненные частицы
        for (int i = 0; i < 3; i++) {
            world.addParticle(ParticleTypes.FLAME,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                    this.getY() + (this.random.nextDouble() - 0.5) * 0.3,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.3,
                    0, 0, 0);
        }

        // Дымовые частицы каждые 4 тика
        if (this.age % 4 == 0) {
            world.addParticle(ParticleTypes.SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0);
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        // Защита от повторных столкновений
        if (hitResult.getType() == HitResult.Type.MISS || hasCollided) {
            return;
        }
        hasCollided = true;

        // Только на сервере
        if (this.getWorld().isClient) return;

        Vec3d explosionPos = hitResult.getPos();
        LivingEntity directTarget = null;

        // ✅ КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: безопасная проверка типа
        if (hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            // Проверяем, что это LivingEntity, а не ItemEntity или что-то еще
            if (entity instanceof LivingEntity) {
                directTarget = (LivingEntity) entity;
            }
        }

        // ✅ МОЩНЫЙ ВЗРЫВ (как раньше)
        createGuaranteedExplosion(explosionPos, directTarget);

        // ✅ МНОГО ОГНЯ (как раньше)
        spawnCustomFire(explosionPos);

        // Удаляем файрболл
        this.discard();
    }

    // ✅ МОЩНЫЙ ВЗРЫВ С ГАРАНТИРОВАННЫМ УРОНОМ
    private void createGuaranteedExplosion(Vec3d explosionPos, LivingEntity directTarget) {
        // 1. СТАНДАРТНЫЙ ВЗРЫВ (для визуальных эффектов и физики)
        this.getWorld().createExplosion(
                this.getOwner(), // Источник - хозяин файрболла
                explosionPos.x,
                explosionPos.y,
                explosionPos.z,
                EXPLOSION_POWER,
                true, // С огнем
                World.ExplosionSourceType.TNT
        );

        // 2. ✅ ГАРАНТИРОВАННЫЙ УРОН В УПОР (оптимизированная версия)
        if (!this.getWorld().isClient) {
            float radius = DAMAGE_RADIUS;

            // Создаем область поиска (оптимизация для сети)
            Box damageBox = new Box(
                    explosionPos.x - radius,
                    explosionPos.y - radius,
                    explosionPos.z - radius,
                    explosionPos.x + radius,
                    explosionPos.y + radius,
                    explosionPos.z + radius
            );

            // Находим ВСЕХ живых сущностей в радиусе
            List<LivingEntity> entities = this.getWorld().getEntitiesByClass(
                    LivingEntity.class,
                    damageBox,
                    entity -> entity.isAlive() && !entity.isSpectator()
            );

            // Наносим урон каждой сущности
            for (LivingEntity entity : entities) {
                double distance = entity.getPos().distanceTo(explosionPos);
                if (distance > radius) continue;

                // Рассчитываем базовый урон от взрыва
                float baseDamage = calculateBaseDamage(distance, radius);

                // Дополнительный урон за прямое попадание
                if (entity == directTarget) {
                    baseDamage += DIRECT_HIT_DAMAGE * 0.7f; // 70% от прямого урона
                    entity.setOnFireFor(6); // Поджигаем на 6 секунд
                }

                // Наносим урон (тип "магия" - учитывает броню)
                if (baseDamage > 0) {
                    entity.damage(this.getWorld().getDamageSources().magic(), baseDamage);

                    // Отладочное сообщение (можно убрать)
                    System.out.println("[FIREBALL] Нанесено " + baseDamage + " урона " +
                            entity.getName().getString() + " (дистанция: " +
                            String.format("%.1f", distance) + " блоков)");
                }
            }
        }

        // Визуальные эффекты взрыва
        spawnExplosionParticles(explosionPos);
    }

    // ✅ ФОРМУЛА УРОНА С ГАРАНТИЕЙ В УПОР
    private float calculateBaseDamage(double distance, float maxRadius) {
        // Гарантированный минимум урона в упор (2 сердца)
        float minDamageInCenter = 4.0f;

        // Максимальный урон в эпицентре (8 сердец)
        float maxDamage = MAX_DAMAGE;

        // Квадратичная зависимость урона от расстояния
        float factor = (float) Math.max(0.0, 1.0 - (distance / maxRadius));
        factor = factor * factor; // Квадрат для резкого падения урона

        float damage = maxDamage * factor;

        // ✅ ГАРАНТИЯ: даже в упор будет минимум 2 сердца урона
        if (distance < 0.5) {
            damage = Math.max(damage, minDamageInCenter);
        }

        return damage;
    }

    // ✅ ВИЗУАЛЬНЫЕ ЭФФЕКТЫ ВЗРЫВА
    private void spawnExplosionParticles(Vec3d pos) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // Огненные частицы
            for (int i = 0; i < 30; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 2.5;
                double offsetY = (this.random.nextDouble() - 0.5) * 2.5;
                double offsetZ = (this.random.nextDouble() - 0.5) * 2.5;

                serverWorld.spawnParticles(
                        ParticleTypes.FLAME,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        1,
                        0.0, 0.0, 0.0,
                        0.06
                );
            }

            // Дымовые частицы
            for (int i = 0; i < 25; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
                double offsetY = (this.random.nextDouble() - 0.5) * 2.0;
                double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;

                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        1,
                        0.0, 0.0, 0.0,
                        0.03
                );
            }
        }
    }

    // ✅ СОЗДАНИЕ МНОЖЕСТВА ОГНЯ
    private void spawnCustomFire(Vec3d explosionPos) {
        int fireCount = 15 + this.random.nextInt(10); // 15-25 блоков огня

        for (int i = 0; i < fireCount; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 12.0;
            double offsetZ = (this.random.nextDouble() - 0.5) * 12.0;
            double offsetY = (this.random.nextDouble() - 0.5) * 4.0;

            BlockPos firePos = new BlockPos(
                    (int)(explosionPos.x + offsetX),
                    (int)(explosionPos.y + offsetY),
                    (int)(explosionPos.z + offsetZ)
            );

            if (canPlaceFire(firePos)) {
                this.getWorld().setBlockState(firePos, ModBlocks.ETERNAL_FIRE.getDefaultState());
            }
        }

        // Дополнительно: круг огня вокруг эпицентра
        createFireCircle(explosionPos);
    }

    // ✅ КРУГ ОГНЯ ВОКРУГ ВЗРЫВА
    private void createFireCircle(Vec3d center) {
        int points = 12 + this.random.nextInt(8); // 12-20 точек
        double radius = 4.0 + this.random.nextDouble() * 3.0; // Радиус 4-7 блоков

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);

            // Пробуем разместить огонь на разной высоте
            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                BlockPos firePos = new BlockPos((int)x, (int)center.y + yOffset, (int)z);
                if (canPlaceFire(firePos)) {
                    this.getWorld().setBlockState(firePos, ModBlocks.ETERNAL_FIRE.getDefaultState());
                    break;
                }
            }
        }
    }

    // Проверка, можно ли разместить огонь
    private boolean canPlaceFire(BlockPos pos) {
        World world = this.getWorld();
        return world.getBlockState(pos).isAir() &&
                world.getBlockState(pos.down()).isSolidBlock(world, pos.down());
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        this.onCollision(blockHitResult);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        this.onCollision(entityHitResult);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
    }
}