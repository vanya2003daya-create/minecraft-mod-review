package com.adam.firemagic.entities;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Predicate;

public class NecroZombieEntity extends ZombieEntity {
    private static final int TELEPORT_DISTANCE = 30;
    private static final int TELEPORT_COOLDOWN = 100;

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private LivingEntity owner;
    private int teleportCooldown = 0;

    @Nullable
    private LivingEntity commandedTarget = null;

    private static final Predicate<ItemEntity> CAN_PICKUP_PREDICATE = (itemEntity) -> {
        ItemStack stack = itemEntity.getStack();
        Item item = stack.getItem();
        return item instanceof SwordItem ||
                item instanceof PickaxeItem ||
                item instanceof AxeItem ||
                item instanceof ShovelItem ||
                item instanceof HoeItem ||
                item instanceof ArmorItem;
    };

    public NecroZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        super.setCustomNameVisible(false);
        super.setCustomName(null);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
    }

    public NecroZombieEntity(World world) {
        this(ModEntities.NECRO_ZOMBIE, world);
    }

    public static DefaultAttributeContainer.Builder createNecroZombieAttributes() {
        return ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0)
                .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS, 0.0)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 2.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.clear(goal -> true);
        this.targetSelector.clear(goal -> true);

        this.goalSelector.add(0, new PickupItemGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(2, new FollowOwnerGoal(this, 1.1, 20.0f, 3.0f));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeForOwnerGoal(this));
        this.targetSelector.add(2, new RevengeForSelfGoal(this));
        this.targetSelector.add(3, new SupportOwnerGoal(this));
    }

    private boolean isAlly(LivingEntity entity) {
        if (entity == null) return false;
        if (entity == this.getOwner()) return true;

        if (entity instanceof NecroZombieEntity otherZombie) {
            LivingEntity thisOwner = this.getOwner();
            LivingEntity otherOwner = otherZombie.getOwner();
            return thisOwner != null && thisOwner == otherOwner;
        }

        if (entity instanceof NecroSkeletonEntity otherSkeleton) {
            LivingEntity thisOwner = this.getOwner();
            LivingEntity otherOwner = otherSkeleton.getOwner();
            return thisOwner != null && thisOwner == otherOwner;
        }

        return false;
    }

    public void setCommandedTarget(@Nullable LivingEntity target) {
        this.commandedTarget = target;
        if (target != null) {
            this.setTarget(target);
        } else {
            this.setTarget(null);
        }
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        super.setCustomNameVisible(false);
    }

    @Override
    public void setCustomName(@Nullable net.minecraft.text.Text name) {
        super.setCustomName(null);
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    public boolean canPickupItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem ||
                item instanceof PickaxeItem ||
                item instanceof AxeItem ||
                item instanceof ShovelItem ||
                item instanceof HoeItem ||
                item instanceof ArmorItem;
    }

    static class PickupItemGoal extends Goal {
        private final NecroZombieEntity zombie;
        private ItemEntity targetItem;
        private int cooldown;

        public PickupItemGoal(NecroZombieEntity zombie) {
            this.zombie = zombie;
            this.cooldown = 0;
        }

        @Override
        public boolean canStart() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }

            Vec3d pos = zombie.getPos();
            Box searchBox = new Box(pos.x - 2, pos.y - 1, pos.z - 2,
                    pos.x + 2, pos.y + 1, pos.z + 2);

            this.targetItem = zombie.getWorld().getEntitiesByClass(
                            ItemEntity.class,
                            searchBox,
                            CAN_PICKUP_PREDICATE
                    ).stream()
                    .filter(item -> item.isAlive() && !item.cannotPickup())
                    .filter(item -> zombie.squaredDistanceTo(item) < 4.0)
                    .findFirst()
                    .orElse(null);

            return this.targetItem != null;
        }

        @Override
        public boolean shouldContinue() {
            return this.targetItem != null &&
                    this.targetItem.isAlive() &&
                    !this.targetItem.cannotPickup() &&
                    zombie.squaredDistanceTo(this.targetItem) < 9.0;
        }

        @Override
        public void start() {
            zombie.getNavigation().startMovingTo(this.targetItem, 1.0);
        }

        @Override
        public void stop() {
            this.targetItem = null;
            zombie.getNavigation().stop();
            this.cooldown = 20;
        }

        @Override
        public void tick() {
            if (this.targetItem != null && zombie.squaredDistanceTo(this.targetItem) < 2.0) {
                ItemStack stack = this.targetItem.getStack();

                if (zombie.canPickupItem(stack)) {
                    EquipmentSlot slot = getSlotForItem(stack);
                    ItemStack currentItem = zombie.getEquippedStack(slot);

                    if (currentItem.isEmpty() || isBetterItem(stack, currentItem)) {
                        zombie.equipStack(slot, stack.copy());
                        this.targetItem.discard();
                        zombie.playSound(net.minecraft.sound.SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);

                        if (!currentItem.isEmpty()) {
                            zombie.dropStack(currentItem.copy());
                        }
                    }
                }

                this.stop();
            }
        }

        private EquipmentSlot getSlotForItem(ItemStack stack) {
            Item item = stack.getItem();

            if (item instanceof SwordItem ||
                    item instanceof PickaxeItem ||
                    item instanceof AxeItem ||
                    item instanceof ShovelItem ||
                    item instanceof HoeItem) {
                return EquipmentSlot.MAINHAND;
            } else if (item instanceof ArmorItem) {
                return ((ArmorItem) item).getSlotType();
            }

            return EquipmentSlot.MAINHAND;
        }

        private boolean isBetterItem(ItemStack newItem, ItemStack currentItem) {
            if (newItem.getItem() instanceof SwordItem && currentItem.getItem() instanceof SwordItem) {
                return getSwordDamage(newItem) > getSwordDamage(currentItem);
            } else if (newItem.getItem() instanceof ArmorItem && currentItem.getItem() instanceof ArmorItem) {
                return getArmorProtection(newItem) > getArmorProtection(currentItem);
            }
            return true;
        }

        private float getSwordDamage(ItemStack stack) {
            if (stack.getItem() instanceof SwordItem sword) {
                return sword.getAttackDamage();
            }
            return 0;
        }

        private int getArmorProtection(ItemStack stack) {
            if (stack.getItem() instanceof ArmorItem armor) {
                return armor.getProtection();
            }
            return 0;
        }
    }

    public void setOwner(PlayerEntity player) {
        this.ownerUuid = player.getUuid();
        this.owner = player;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUuid != null && this.getWorld() instanceof ServerWorld) {
            PlayerEntity player = ((ServerWorld) this.getWorld()).getPlayerByUuid(this.ownerUuid);
            if (player instanceof LivingEntity) {
                this.owner = player;
            }
        }
        return this.owner;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient()) {
            LivingEntity owner = getOwner();
            if (owner != null) {
                double distance = this.squaredDistanceTo(owner);

                if (distance > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                    if (teleportCooldown <= 0) {
                        this.teleport(owner.getX(), owner.getY(), owner.getZ());
                        teleportCooldown = TELEPORT_COOLDOWN;
                    } else {
                        teleportCooldown--;
                    }
                } else {
                    teleportCooldown = 0;
                }

                if (!owner.isAlive()) {
                    this.kill();
                }
            }

            if (owner != null && this.squaredDistanceTo(owner) < 25.0 && this.age % 40 == 0) {
                this.heal(1.0f);
            }

            if (this.commandedTarget != null &&
                    (!this.commandedTarget.isAlive() || this.commandedTarget.isRemoved())) {
                this.commandedTarget = null;
                this.setTarget(null);
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof LivingEntity attacker) {
            LivingEntity owner = getOwner();

            if (owner != null && attacker.getUuid().equals(owner.getUuid())) {
                return false;
            }
        }

        return super.damage(source, amount);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.ownerUuid != null) {
            nbt.putUuid("Owner", this.ownerUuid);
        }
        if (this.commandedTarget != null) {
            nbt.putUuid("CommandedTarget", this.commandedTarget.getUuid());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("Owner")) {
            this.ownerUuid = nbt.getUuid("Owner");
        }
        if (nbt.containsUuid("CommandedTarget") && this.getWorld() instanceof ServerWorld) {
            UUID targetUuid = nbt.getUuid("CommandedTarget");
            Entity entity = ((ServerWorld) this.getWorld()).getEntity(targetUuid);
            if (entity instanceof LivingEntity) {
                this.commandedTarget = (LivingEntity) entity;
                this.setTarget(this.commandedTarget);
            }
        }
    }

    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropEquipment(source, lootingMultiplier, allowDrops);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);
    }

    static class FollowOwnerGoal extends Goal {
        private final NecroZombieEntity minion;
        private LivingEntity owner;
        private final double speed;
        private final float maxDistance;
        private final float minDistance;
        private int updateCountdownTicks;
        private int cantReachOwnerTicks = 0;

        public FollowOwnerGoal(NecroZombieEntity minion, double speed, float maxDistance, float minDistance) {
            this.minion = minion;
            this.speed = speed;
            this.maxDistance = maxDistance;
            this.minDistance = minDistance;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (minion.getTarget() != null && minion.getTarget().isAlive()) {
                return false;
            }

            LivingEntity owner = minion.getOwner();
            if (owner == null || owner.isSpectator() || !owner.isAlive()) {
                return false;
            }

            double distance = minion.squaredDistanceTo(owner);
            if (distance < (double)(this.minDistance * this.minDistance)) {
                return false;
            }

            this.owner = owner;
            return true;
        }

        @Override
        public boolean shouldContinue() {
            if (minion.getTarget() != null && minion.getTarget().isAlive()) {
                return false;
            }

            if (this.owner == null || !this.owner.isAlive()) {
                return false;
            }

            double distance = minion.squaredDistanceTo(this.owner);
            return distance > (double)(this.minDistance * this.minDistance);
        }

        @Override
        public void start() {
            this.updateCountdownTicks = 0;
            this.cantReachOwnerTicks = 0;
        }

        @Override
        public void tick() {
            if (this.owner == null || !this.owner.isAlive()) return;

            minion.getLookControl().lookAt(this.owner, 10.0F, (float)minion.getMaxLookPitchChange());

            double distance = minion.squaredDistanceTo(this.owner);
            double currentSpeed = this.speed;

            if (--this.updateCountdownTicks <= 0) {
                this.updateCountdownTicks = 10;

                boolean canPath = minion.getNavigation().startMovingTo(this.owner, currentSpeed);

                if (!canPath) {
                    this.cantReachOwnerTicks++;

                    if (this.cantReachOwnerTicks > 100) {
                        this.cantReachOwnerTicks = 0;

                        if (minion.getWorld() instanceof ServerWorld serverWorld) {
                            minion.teleport(
                                    this.owner.getX() + (minion.getRandom().nextDouble() - 0.5) * 3,
                                    this.owner.getY(),
                                    this.owner.getZ() + (minion.getRandom().nextDouble() - 0.5) * 3
                            );
                        }
                    }
                } else {
                    this.cantReachOwnerTicks = 0;
                }
            }

            if (distance < (double)(this.minDistance * this.minDistance)) {
                minion.getNavigation().stop();
                this.updateCountdownTicks = 0;
            }
        }

        @Override
        public void stop() {
            this.owner = null;
            minion.getNavigation().stop();
            this.updateCountdownTicks = 0;
            this.cantReachOwnerTicks = 0;
        }
    }

    static class RevengeForOwnerGoal extends Goal {
        private final NecroZombieEntity minion;

        public RevengeForOwnerGoal(NecroZombieEntity minion) {
            this.minion = minion;
        }

        @Override
        public boolean canStart() {
            LivingEntity owner = minion.getOwner();
            if (owner == null) {
                return false;
            }

            LivingEntity attacker = owner.getAttacker();
            if (attacker != null && attacker.isAlive() && attacker != minion) {
                if (minion.isAlly(attacker)) {
                    return false;
                }
                minion.setTarget(attacker);
                return true;
            }

            return false;
        }
    }

    static class RevengeForSelfGoal extends Goal {
        private final NecroZombieEntity minion;

        public RevengeForSelfGoal(NecroZombieEntity minion) {
            this.minion = minion;
        }

        @Override
        public boolean canStart() {
            LivingEntity attacker = minion.getAttacker();

            if (attacker != null && attacker.isAlive()) {
                LivingEntity owner = minion.getOwner();
                if (owner != null && attacker == owner) {
                    return false;
                }

                if (minion.isAlly(attacker)) {
                    return false;
                }

                minion.setTarget(attacker);
                return true;
            }

            return false;
        }
    }

    static class SupportOwnerGoal extends Goal {
        private final NecroZombieEntity minion;
        private int cooldown = 0;

        public SupportOwnerGoal(NecroZombieEntity minion) {
            this.minion = minion;
        }

        @Override
        public boolean canStart() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }

            LivingEntity owner = minion.getOwner();
            if (owner == null || !owner.isAlive()) {
                return false;
            }

            LivingEntity ownerTarget = owner.getAttacking();
            if (ownerTarget != null && ownerTarget.isAlive() && ownerTarget != minion) {
                if (minion.isAlly(ownerTarget)) {
                    return false;
                }
                minion.setTarget(ownerTarget);
                return true;
            }

            return false;
        }
    }
}