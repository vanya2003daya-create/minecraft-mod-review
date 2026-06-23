package com.adam.firemagic.items.archer;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExplosiveArrowManager {
    private static final Map<UUID, UUID> explosiveArrows = new HashMap<>();
    public static final float EXPLOSION_POWER = 1.8f; // Сила взрыва

    public static void markArrowAsExplosive(UUID arrowUuid, UUID ownerUuid) {
        explosiveArrows.put(arrowUuid, ownerUuid);
        System.out.println("[DEBUG] ✅ Зарегистрирована взрывная стрела. UUID: " + arrowUuid);
    }

    public static boolean isExplosiveArrow(UUID arrowUuid) {
        boolean isExplosive = explosiveArrows.containsKey(arrowUuid);
        if (isExplosive) {
            System.out.println("[DEBUG] 🔍 Проверка: стрела " + arrowUuid + " - ВЗРЫВНАЯ");
        }
        return isExplosive;
    }

    public static UUID getOwnerUuid(UUID arrowUuid) {
        return explosiveArrows.get(arrowUuid);
    }

    public static void handleExplosiveArrowCollision(PersistentProjectileEntity projectile, World world, Vec3d impactPos) {
        if (world.isClient() || projectile.isRemoved()) {
            System.out.println("[DEBUG] ❌ Клиентская сторона или снаряд удалён");
            return;
        }

        UUID projectileUuid = projectile.getUuid();
        if (!isExplosiveArrow(projectileUuid)) {
            System.out.println("[DEBUG] ❌ Снаряд не взрывной: " + projectileUuid);
            return;
        }

        System.out.println("[DEBUG] 💥 Активация взрыва в точке: " + impactPos);

        try {
            // 🔥 ГЛАВНОЕ ИЗМЕНЕНИЕ: создаём взрыв БЕЗ ИСТОЧНИКА (null), чтобы наносил урон ВСЕМ
            world.createExplosion(
                    null, // ← КРИТИЧЕСКИ ВАЖНО: null вместо владельца!
                    impactPos.x,
                    impactPos.y,
                    impactPos.z,
                    EXPLOSION_POWER,
                    false, // создаёт огонь
                    World.ExplosionSourceType.TNT // ← В 1.20.1 это правильный enum (не ломает блоки)
            );

            System.out.println("[DEBUG] ✅ Взрыв успешно создан в точке: " + impactPos);

            // 🌪️ ЧАСТИЦЫ (рабочий код для 1.20.1)
            if (world instanceof ServerWorld serverWorld) {
                for (int i = 0; i < 20; i++) {
                    double offsetX = (world.random.nextDouble() - 0.5) * 2.0;
                    double offsetY = (world.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (world.random.nextDouble() - 0.5) * 2.0;

                    serverWorld.spawnParticles(
                            net.minecraft.particle.ParticleTypes.FLAME,
                            impactPos.x + offsetX,
                            impactPos.y + offsetY,
                            impactPos.z + offsetZ,
                            1,
                            0.0, 0.0, 0.0,
                            0.05
                    );

                    serverWorld.spawnParticles(
                            net.minecraft.particle.ParticleTypes.SMOKE,
                            impactPos.x + offsetX * 0.5,
                            impactPos.y + offsetY * 0.5,
                            impactPos.z + offsetZ * 0.5,
                            1,
                            0.0, 0.0, 0.0,
                            0.02
                    );
                }
            }

        } catch (Exception e) {
            System.out.println("[ERROR] ❌ Ошибка при создании взрыва: " + e.getMessage());
            e.printStackTrace();
        }

        // Удаляем снаряд из системы
        explosiveArrows.remove(projectileUuid);
        System.out.println("[DEBUG] 🗑️ Снаряд удалён из системы");
    }

    // Вспомогательный метод для расчёта урона
    private static float calculateExplosionDamage(float power, float distance) {
        // Формула урона от расстояния (упрощённая)
        float maxDamage = power * 3.0f; // 2.5f * 4 = 10 урона в эпицентре
        float minDamage = maxDamage * 0.2f; // минимальный урон на краю радиуса

        if (distance <= 1.0f) {
            return maxDamage;
        }

        float damage = maxDamage - (maxDamage - minDamage) * (distance / (power * 2.0f));
        return Math.max(minDamage, damage);
    }
}