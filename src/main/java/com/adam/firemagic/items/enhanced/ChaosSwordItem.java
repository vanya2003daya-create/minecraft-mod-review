// 📄 com/adam/firemagic/items/enhanced/ChaosSwordItem.java
package com.adam.firemagic.items.enhanced;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterials;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class ChaosSwordItem extends EnhancedItemBase {
    // Конфигурация меча - ИЗМЕНЕНИЯ ПО БАЛАНСУ
    private static final int HITS_FOR_ABILITY = 5; // КАЖДЫЕ 5 УДАРОВ (было 3)
    private static final float EXPLOSION_POWER = 1.0f; // Сила взрыва
    private static final float DAMAGE_RADIUS = 4.0f; // Радиус урона

    // УРОН ДИНАМИТА: 7 HP в эпицентре, у нас в 2 раза меньше = 3.5 HP
    private static final float DIRECT_HIT_DAMAGE = 3.5f; // Урон в эпицентре (половина динамита)
    private static final float KNOCKBACK_MULTIPLIER = 1.0f; // ОТТАЛКИВАНИЕ УМЕНЬШЕНО В 2 РАЗА (было 2.0)

    private static final int COOLDOWN_MS = 2000; // 2 секунды кулдауна

    public ChaosSwordItem() {
        super(ToolMaterials.NETHERITE,
                8, // Урон (больше алмазного)
                -2.4f, // Скорость атаки
                new Settings()
                        .maxCount(1)
                        .maxDamage(1561) // Прочность незерита
                        .fireproof());
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Показываем информацию о способности при использовании ПКМ
        if (world.isClient) {
            int hits = getHitCounter(stack);
            int hitsLeft = HITS_FOR_ABILITY - (hits % HITS_FOR_ABILITY);

            user.sendMessage(
                    Text.literal("⚡ Счетчик ударов: " + hits + " | До взрыва: " + hitsLeft)
                            .formatted(Formatting.DARK_PURPLE),
                    true
            );

            if (isOnCooldown(stack)) {
                long remaining = getRemainingCooldown(stack);
                user.sendMessage(
                        Text.literal("⏳ Перезарядка: " + (remaining / 1000.0) + " сек")
                                .formatted(Formatting.GRAY),
                        true
                );
            }
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Наносим стандартный урон
        boolean result = super.postHit(stack, target, attacker);

        if (!attacker.getWorld().isClient()) {
            // Увеличиваем счетчик ударов
            incrementHitCounter(stack);
            int hits = getHitCounter(stack);

            // Проверяем, настало ли время для взрыва (теперь каждые 5 ударов)
            if (hits % HITS_FOR_ABILITY == 0 && !isOnCooldown(stack)) {
                activateChaosExplosion(stack, attacker.getWorld(), attacker, target);
                setCooldown(stack, COOLDOWN_MS);
                resetHitCounter(stack);
            }

            // Визуальная индикация прогресса
            if (attacker instanceof PlayerEntity player) {
                int hitsLeft = HITS_FOR_ABILITY - (hits % HITS_FOR_ABILITY);
                if (hitsLeft > 0) {
                    player.sendMessage(
                            Text.literal("⚡ " + hitsLeft + " до взрыва!")
                                    .formatted(Formatting.LIGHT_PURPLE),
                            true
                    );
                }
            }
        }

        return result;
    }

    private void activateChaosExplosion(ItemStack stack, World world, LivingEntity attacker, LivingEntity primaryTarget) {
        if (world.isClient()) return;

        ServerWorld serverWorld = (ServerWorld) world;
        Vec3d explosionPos = primaryTarget.getPos();

        // 1. СОЗДАЕМ СТАНДАРТНЫЙ ВЗРЫВ (СИЛА 1.0 - МАЛЕНЬКИЙ)
        world.createExplosion(
                attacker,
                explosionPos.x,
                explosionPos.y,
                explosionPos.z,
                EXPLOSION_POWER,
                false, // Без огня
                World.ExplosionSourceType.TNT
        );

        // 2. ГАРАНТИРОВАННЫЙ УРОН (КАК У ДИНАМИТА /2)
        float radius = DAMAGE_RADIUS;

        Box damageBox = new Box(
                explosionPos.x - radius,
                explosionPos.y - radius,
                explosionPos.z - radius,
                explosionPos.x + radius,
                explosionPos.y + radius,
                explosionPos.z + radius
        );

        // Находим ВСЕХ живых сущностей в радиусе
        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class,
                damageBox,
                entity -> entity.isAlive() && !entity.isSpectator()
        );

        for (LivingEntity entity : entities) {
            double distance = entity.getPos().distanceTo(explosionPos);
            if (distance > radius) continue;

            // УРОН КАК У ДИНАМИТА (7 HP), НО В 2 РАЗА МЕНЬШЕ
            float baseDamage = calculateDamage(distance, radius);

            // Дополнительный множитель для прямого попадания
            if (entity == primaryTarget) {
                // Прямое попадание = максимальный урон
                baseDamage = DIRECT_HIT_DAMAGE;
            }

            // Наносим урон (включая игрока)
            if (baseDamage > 0.1f) {
                entity.damage(world.getDamageSources().explosion(attacker, attacker), baseDamage);

                // ОТТАЛКИВАНИЕ УМЕНЬШЕНО В 2 РАЗА
                applyKnockback(entity, explosionPos, distance, radius);
            }
        }

        // 3. ВИЗУАЛЬНЫЕ ЭФФЕКТЫ
        spawnChaosParticles(serverWorld, explosionPos);

        // Звук взрыва
        world.playSound(
                null,
                explosionPos.x,
                explosionPos.y,
                explosionPos.z,
                SoundEvents.ENTITY_WITHER_SHOOT,
                SoundCategory.PLAYERS,
                0.8f, // Немного тише
                0.8f + world.random.nextFloat() * 0.4f
        );

        // 4. Сообщение игроку
        if (attacker instanceof PlayerEntity player) {
            player.sendMessage(
                    Text.literal("💥 Взрыв каждые 5 ударов!").formatted(Formatting.DARK_PURPLE),
                    true
            );
        }
    }

    private float calculateDamage(double distance, float radius) {
        // Динамит: 7 HP в эпицентре, у нас 3.5 HP
        // Урон уменьшается линейно с расстоянием
        float damageMultiplier = 1.0f - (float)(distance / radius);

        // Базовый урон: от 0.5 HP на краю до 3.5 HP в эпицентре
        float baseDamage = DIRECT_HIT_DAMAGE * damageMultiplier;

        // Округляем до 0.5
        return Math.max(0.5f, Math.round(baseDamage * 2) / 2.0f);
    }

    private void applyKnockback(LivingEntity entity, Vec3d explosionPos, double distance, float radius) {
        // Расчет вектора от взрыва к сущности
        Vec3d vecToEntity = entity.getPos().subtract(explosionPos).normalize();

        // СИЛА ОТТАЛКИВАНИЯ УМЕНЬШЕНА В 2 РАЗА
        float knockbackStrength = KNOCKBACK_MULTIPLIER * (1.0f - (float)(distance / radius)) * 0.5f;

        Vec3d knockback = vecToEntity.multiply(knockbackStrength);

        // Если это сам игрок - еще меньше
        if (entity instanceof PlayerEntity) {
            knockback = knockback.multiply(0.8); // Еще на 20% меньше для игрока
        }

        entity.addVelocity(knockback.x, Math.min(0.2, knockback.y + 0.2), knockback.z);
        entity.velocityModified = true;
    }

    private void spawnChaosParticles(ServerWorld world, Vec3d pos) {
        // Частицы хаоса
        for (int i = 0; i < 20; i++) { // Меньше частиц
            double offsetX = (world.random.nextDouble() - 0.5) * 2.5;
            double offsetY = (world.random.nextDouble() - 0.5) * 2.5;
            double offsetZ = (world.random.nextDouble() - 0.5) * 2.5;

            world.spawnParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    1,
                    0, 0, 0,
                    0.08
            );

            // Реже дополнительные частицы
            if (world.random.nextFloat() < 0.2f) {
                world.spawnParticles(
                        ParticleTypes.WITCH,
                        pos.x + offsetX * 0.5,
                        pos.y + offsetY * 0.5,
                        pos.z + offsetZ * 0.5,
                        1,
                        0, 0, 0,
                        0.03
                );
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("⚡ Способность: Взрыв Хаоса").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("  Каждые 5 попаданий создает взрыв").formatted(Formatting.GRAY)); // ИЗМЕНЕНО
        tooltip.add(Text.literal("  Урон как у динамита (в 2 раза меньше)").formatted(Formatting.GRAY)); // ИЗМЕНЕНО
        tooltip.add(Text.literal("  ⚠️ Взрыв наносит урон и владельцу").formatted(Formatting.RED));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("⚔️ Урон меча: 8").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("💥 Урон взрыва: 0.5-3.5 HP").formatted(Formatting.GRAY)); // ОБНОВЛЕНО
        tooltip.add(Text.literal("🎯 Радиус: " + DAMAGE_RADIUS + " блоков").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("⏱️ Кулдаун: 2 сек").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("🔁 Частота: каждые 5 ударов").formatted(Formatting.GRAY)); // ДОБАВЛЕНО

        // Информация о прогрессе
        if (!isOnCooldown(stack)) {
            int hits = getHitCounter(stack);
            int hitsLeft = HITS_FOR_ABILITY - (hits % HITS_FOR_ABILITY);
            tooltip.add(Text.literal("  До взрыва: " + hitsLeft + " удара(ов)").formatted(Formatting.LIGHT_PURPLE));
        } else {
            long remaining = getRemainingCooldown(stack);
            tooltip.add(Text.literal("  Перезарядка: " + (remaining / 1000.0) + " сек").formatted(Formatting.RED));
        }
    }

    @Override
    public boolean tryActivateAbility(ItemStack stack, World world, LivingEntity user, Entity target) {
        return false;
    }
}