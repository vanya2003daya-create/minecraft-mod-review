package com.adam.firemagic.items.enhanced;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterials;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

public class ShadowStepSwordItem extends EnhancedItemBase {
    // Конфигурация
    private static final int MAX_CHARGES = 3;
    private static final float CHARGE_CHANCE = 0.23f; // 23%
    private static final float DAMAGE = 10.5f; // 1.5× алмазного меча
    private static final int MAX_DISTANCE = 6; // Блоков
    private static final double MIN_TELEPORT_DISTANCE = 0.5;
    private static final int COOLDOWN_MS = 1000; // 1 секунда

    // Ключи NBT
    private static final String CHARGES_KEY = "ShadowCharges";
    private static final String LAST_TELEPORT_KEY = "LastTeleportTime";

    public ShadowStepSwordItem() {
        super(ToolMaterials.DIAMOND,
                8, // Базовый урон
                -2.4f,
                new Settings()
                        .maxCount(1)
                        .maxDamage(1561)
                        .fireproof());
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (world.isClient) {
            // Показываем информацию о зарядах
            int charges = getCharges(stack);
            if (charges > 0) {
                player.sendMessage(
                        Text.literal("🌀 Зарядов: " + charges + "/" + MAX_CHARGES)
                                .formatted(Formatting.DARK_PURPLE),
                        true
                );
            }
            return TypedActionResult.pass(stack);
        }

        // Проверяем кулдаун
        if (isOnCooldown(stack)) {
            long remaining = getRemainingCooldown(stack);
            player.sendMessage(
                    Text.literal("⏳ Перезарядка: " + (remaining / 1000.0) + " сек")
                            .formatted(Formatting.RED),
                    true
            );
            return TypedActionResult.fail(stack);
        }

        // Проверяем заряды
        int charges = getCharges(stack);
        if (charges == 0) {
            player.sendMessage(
                    Text.literal("⚡ Нет зарядов для телепортации")
                            .formatted(Formatting.RED),
                    true
            );
            return TypedActionResult.fail(stack);
        }

        try {
            // Логика телепортации как в FireDashItem
            Vec3d lookDir = player.getRotationVector();
            Vec3d horizontalDir = new Vec3d(lookDir.x, 0, lookDir.z).normalize();

            if (horizontalDir.lengthSquared() < 0.01) {
                return TypedActionResult.pass(stack);
            }

            Vec3d startPos = player.getPos();
            Vec3d endPos = startPos.add(horizontalDir.multiply(MAX_DISTANCE));

            // Raycast для определения точки телепортации
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
                player.sendMessage(
                        Text.literal("⚡ Недостаточное расстояние для телепортации")
                                .formatted(Formatting.YELLOW),
                        true
                );
                return TypedActionResult.pass(stack);
            }

            // Проверка безопасной позиции
            if (!isSafeTeleportPosition(world, actualEndPos, player)) {
                player.sendMessage(
                        Text.literal("⚠️ Невозможно телепортироваться в это место")
                                .formatted(Formatting.YELLOW),
                        true
                );
                return TypedActionResult.pass(stack);
            }

            // Успешная телепортация
            player.teleport(actualEndPos.x, actualEndPos.y, actualEndPos.z);

            // Тратим один заряд
            setCharges(stack, charges - 1);
            setLastTeleportTime(stack, world.getTime());
            setCooldown(stack, COOLDOWN_MS);

            // Урон сущностям на пути
            damageEntities(world, startPos, actualEndPos, player);

            // Эффекты телепортации
            spawnTeleportEffects((ServerWorld) world, startPos, actualEndPos);

            // Сообщение игроку
            player.sendMessage(
                    Text.literal("⚡ Телепортирован! Зарядов: " + (charges - 1))
                            .formatted(Formatting.LIGHT_PURPLE),
                    true
            );

            return TypedActionResult.success(stack);

        } catch (Exception e) {
            e.printStackTrace();
            return TypedActionResult.pass(stack);
        }
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.postHit(stack, target, attacker);

        if (!attacker.getWorld().isClient()) {
            // 23% шанс получить заряд
            if (attacker.getRandom().nextFloat() < CHARGE_CHANCE) {
                int currentCharges = getCharges(stack);
                if (currentCharges < MAX_CHARGES) {
                    setCharges(stack, currentCharges + 1);

                    if (attacker instanceof PlayerEntity player) {
                        player.sendMessage(
                                Text.literal("⚡ +1 заряд (" + (currentCharges + 1) + "/" + MAX_CHARGES + ")")
                                        .formatted(Formatting.DARK_PURPLE),
                                true
                        );

                        // Звук получения заряда
                        player.getWorld().playSound(
                                null,
                                player.getBlockPos(),
                                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                SoundCategory.PLAYERS,
                                0.5f,
                                1.0f + player.getRandom().nextFloat() * 0.2f
                        );
                    }
                }
            }
        }

        return result;
    }

    // Проверка безопасной позиции (как в FireDashItem)
    private boolean isSafeTeleportPosition(World world, Vec3d pos, PlayerEntity player) {
        Box playerBox = player.getBoundingBox().offset(pos.subtract(player.getPos()));
        return world.isSpaceEmpty(playerBox) && !world.containsFluid(playerBox);
    }

    // Урон сущностям на пути (адаптировано из FireDashItem)
    private void damageEntities(World world, Vec3d start, Vec3d end, PlayerEntity source) {
        float DAMAGE_RADIUS = 1.5f;

        Box damageBox = new Box(start, end).expand(DAMAGE_RADIUS);
        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class,
                damageBox,
                entity -> entity != source && entity.isAttackable()
        );

        DamageSource damageSource = world.getDamageSources().playerAttack(source);

        for (LivingEntity entity : entities) {
            entity.damage(damageSource, DAMAGE);

            // Визуальный эффект попадания
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        5,
                        0.2, 0.2, 0.2,
                        0.05
                );
            }
        }
    }

    // Эффекты телепортации (адаптировано из FireDashItem)
    private void spawnTeleportEffects(ServerWorld world, Vec3d start, Vec3d end) {
        double distance = start.distanceTo(end);
        int particleCount = Math.min(15, (int) (distance * 3));

        // Частицы по пути
        for (int i = 0; i < particleCount; i++) {
            double t = (double) i / particleCount;
            Vec3d pos = start.lerp(end, t);

            world.spawnParticles(
                    ParticleTypes.PORTAL,
                    pos.x,
                    pos.y + 0.5,
                    pos.z,
                    1,
                    0.1,
                    0.1,
                    0.1,
                    0.05
            );

            // Дополнительные частицы
            if (i % 3 == 0) {
                world.spawnParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        pos.x,
                        pos.y + 0.5,
                        pos.z,
                        1,
                        0.15,
                        0.15,
                        0.15,
                        0.03
                );
            }
        }

        // Больше частиц в начальной и конечной точках
        world.spawnParticles(
                ParticleTypes.ENCHANT,
                start.x,
                start.y,
                start.z,
                10,
                0.3,
                0.3,
                0.3,
                0.1
        );

        world.spawnParticles(
                ParticleTypes.ENCHANT,
                end.x,
                end.y,
                end.z,
                10,
                0.3,
                0.3,
                0.3,
                0.1
        );

        // Звук телепортации
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
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.7f,
                1.0f
        );
    }

    // Работа с зарядами
    private int getCharges(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return Math.min(nbt.getInt(CHARGES_KEY), MAX_CHARGES);
    }

    private void setCharges(ItemStack stack, int charges) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt(CHARGES_KEY, Math.max(0, Math.min(charges, MAX_CHARGES)));
    }

    private void setLastTeleportTime(ItemStack stack, long time) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong(LAST_TELEPORT_KEY, time);
    }

    private long getLastTeleportTime(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return nbt.getLong(LAST_TELEPORT_KEY);
    }

    @Override
    public boolean tryActivateAbility(ItemStack stack, World world, LivingEntity user, Entity target) {
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("🌀 Способность: Шаг Тени").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("  Каждый удар: 23% шанс получить заряд").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("  ПКМ: телепортация на 6 блоков (тратит заряд)").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("  Наносит урон всем врагам на пути").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("⚔️ Урон меча: 8").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("💥 Урон телепортации: " + DAMAGE + " HP").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("🌀 Макс. зарядов: " + MAX_CHARGES).formatted(Formatting.GRAY));
        tooltip.add(Text.literal("📏 Дальность: 6 блоков").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("⚡ Шанс заряда: 23%").formatted(Formatting.GRAY));

        int charges = getCharges(stack);
        tooltip.add(Text.literal("  Зарядов: " + charges + "/" + MAX_CHARGES)
                .formatted(charges == 0 ? Formatting.RED :
                        charges == MAX_CHARGES ? Formatting.GREEN : Formatting.YELLOW));

        if (isOnCooldown(stack)) {
            long remaining = getRemainingCooldown(stack);
            tooltip.add(Text.literal("  Перезарядка: " + (remaining / 1000.0) + " сек")
                    .formatted(Formatting.RED));
        }
    }
}