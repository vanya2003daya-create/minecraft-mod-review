package com.adam.firemagic.items.necromancer;

import com.adam.firemagic.entities.NecroSkeletonEntity;
import com.adam.firemagic.entities.NecroZombieEntity;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class NecromancerStaffItem extends Item {
    private static final int TELEPORT_COOLDOWN_TICKS = 200; // 10 секунд
    private static final double RAYCAST_DISTANCE = 50.0;

    public NecromancerStaffItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        // 🔧 ИСПРАВЛЕНИЕ: Используем ServerPlayerEntity через проверку
        if (!(user instanceof net.minecraft.server.network.ServerPlayerEntity)) {
            return TypedActionResult.fail(stack);
        }

        net.minecraft.server.network.ServerPlayerEntity serverPlayer = (net.minecraft.server.network.ServerPlayerEntity) user;
        PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

        if (!manaData.hasNecromancerSchool()) {
            serverPlayer.sendMessage(Text.literal("§cТолько некроманты могут использовать это!"), true);
            return TypedActionResult.fail(stack);
        }

        if (user.isSneaking()) {
            return teleportMinions(serverPlayer, world, stack);
        } else {
            return selectAttackTarget(serverPlayer, world, stack);
        }
    }

    private TypedActionResult<ItemStack> teleportMinions(net.minecraft.server.network.ServerPlayerEntity user, World world, ItemStack stack) {
        ServerWorld serverWorld = (ServerWorld) world;
        PlayerManaData manaData = ManaManager.getServerData(user);
        boolean teleportedAny = false;

        if (manaData.hasZombieMinion()) {
            Entity zombie = serverWorld.getEntity(manaData.getZombieMinionId());
            if (zombie != null && zombie.isAlive()) {
                zombie.teleport(user.getX(), user.getY(), user.getZ());
                teleportedAny = true;
                // world.sendEntityStatus(user, (byte) 35); // ⚠️ Этот метод может быть недоступен
            }
        }

        if (manaData.hasSkeletonMinion()) {
            Entity skeleton = serverWorld.getEntity(manaData.getSkeletonMinionId());
            if (skeleton != null && skeleton.isAlive()) {
                skeleton.teleport(user.getX(), user.getY(), user.getZ());
                teleportedAny = true;
            }
        }

        // TODO: Добавить скелета и призрака позже

        if (teleportedAny) {
            user.sendMessage(Text.literal("§aВсе прислужники телепортированы к вам!"), true);
            user.getItemCooldownManager().set(this, TELEPORT_COOLDOWN_TICKS);
            return TypedActionResult.success(stack);
        } else {
            user.sendMessage(Text.literal("§7У вас нет живых прислужников"), true);
            return TypedActionResult.fail(stack);
        }
    }

    private TypedActionResult<ItemStack> selectAttackTarget(net.minecraft.server.network.ServerPlayerEntity user, World world, ItemStack stack) {
        HitResult hit = raycast(user, RAYCAST_DISTANCE);

        if (hit.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hit).getEntity();

            if (target instanceof LivingEntity && target != user) {
                commandMinionsToAttack(user, (LivingEntity) target, world);
                user.sendMessage(Text.literal("§cПриказано атаковать " + target.getName().getString() + "!"), true);
                user.getItemCooldownManager().set(this, 20);
                return TypedActionResult.success(stack);
            }
        }

        user.sendMessage(Text.literal("§7Нацельтесь на врага, чтобы отдать приказ атаки"), true);
        return TypedActionResult.fail(stack);
    }

    private void commandMinionsToAttack(net.minecraft.server.network.ServerPlayerEntity owner, LivingEntity target, World world) {
        if (!(world instanceof ServerWorld)) return;

        ServerWorld serverWorld = (ServerWorld) world;
        PlayerManaData manaData = ManaManager.getServerData(owner);

        // 🔧 Не атаковать своих
        if (target == owner) {
            owner.sendMessage(Text.literal("§cНельзя приказывать атаковать себя!"), false);
            return;
        }

        if (target instanceof NecroZombieEntity zombie && zombie.getOwner() == owner) {
            owner.sendMessage(Text.literal("§cНельзя приказывать атаковать своего зомби!"), false);
            return;
        }

        if (target instanceof NecroSkeletonEntity skeleton && skeleton.getOwner() == owner) {
            owner.sendMessage(Text.literal("§cНельзя приказывать атаковать своего скелета!"), false);
            return;
        }

        // Командуем зомби
        if (manaData.hasZombieMinion()) {
            Entity zombie = serverWorld.getEntity(manaData.getZombieMinionId());
            if (zombie instanceof NecroZombieEntity) {
                ((NecroZombieEntity) zombie).setCommandedTarget(target);
            }
        }

        // Командуем скелета
        if (manaData.hasSkeletonMinion()) {
            Entity skeleton = serverWorld.getEntity(manaData.getSkeletonMinionId());
            if (skeleton instanceof NecroSkeletonEntity) {
                ((NecroSkeletonEntity) skeleton).setCommandedTarget(target);
            }
        }
    }

    private HitResult raycast(PlayerEntity player, double maxDistance) {
        Vec3d start = player.getEyePos();
        Vec3d rotation = player.getRotationVec(1.0F);
        Vec3d end = start.add(rotation.multiply(maxDistance));

        // Raycast по сущностям
        EntityHitResult entityHit = ProjectileUtil.raycast(
                player,
                start,
                end,
                player.getBoundingBox().stretch(rotation.multiply(maxDistance)).expand(1.0),
                entity -> !entity.isSpectator() && entity.canHit(),
                maxDistance * maxDistance
        );

        if (entityHit != null) {
            return entityHit;
        }

        // Raycast по блокам
        return player.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("§5Посох некроманта").formatted(net.minecraft.util.Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("§7ПКМ: Приказать атаковать цель").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§7Shift+ПКМ: Телепортировать прислужников").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§8◇ Кулдаун телепортации: 10 сек").formatted(net.minecraft.util.Formatting.DARK_GRAY));
    }
}