package com.adam.firemagic.entities;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class NecroSkeletonEntity extends SkeletonEntity implements RangedAttackMob {
    private static final int TELEPORT_DISTANCE = 30;
    private static final int TELEPORT_COOLDOWN = 100;

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private LivingEntity owner;
    private int teleportCooldown = 0;

    @Nullable
    private LivingEntity commandedTarget = null;

    // Флаги для безопасного подбора
    private boolean isPickingUp = false;
    private int pickupCooldown = 0;

    public NecroSkeletonEntity(EntityType<? extends SkeletonEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        super.setCustomNameVisible(false);
        super.setCustomName(null);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        this.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.setCanPickUpLoot(true);
    }

    public NecroSkeletonEntity(World world) {
        this(ModEntities.NECRO_SKELETON, world);
    }

    public static DefaultAttributeContainer.Builder createNecroSkeletonAttributes() {
        return SkeletonEntity.createAbstractSkeletonAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_ARMOR, 1.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.clear(goal -> true);
        this.targetSelector.clear(goal -> true);

        // Цели для дальнего боя
        this.goalSelector.add(1, new BowAttackGoal<>(this, 1.0, 20, 15.0f));
        this.goalSelector.add(2, new FollowOwnerGoal(this, 1.1, 20.0f, 3.0f));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));

        // Цели для выбора цели - КАК У ЗОМБИ
        this.targetSelector.add(1, new RevengeForOwnerGoal(this));
        this.targetSelector.add(2, new RevengeForSelfGoal(this));
        this.targetSelector.add(3, new SupportOwnerGoal(this));
        this.targetSelector.add(4, new CommandTargetGoal(this));
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        Hand hand = ProjectileUtil.getHandPossiblyHolding(this, Items.BOW);
        ItemStack itemStack = this.getStackInHand(hand);

        ArrowEntity arrowEntity = this.createArrowProjectile(itemStack, pullProgress);

        double d = target.getX() - this.getX();
        double e = target.getBodyY(0.3333333333333333) - arrowEntity.getY();
        double f = target.getZ() - this.getZ();
        double g = Math.sqrt(d * d + f * f);

        arrowEntity.setVelocity(d, e + g * 0.2, f, 1.6F, (float)(14 - this.getWorld().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.getWorld().spawnEntity(arrowEntity);
    }

    protected ArrowEntity createArrowProjectile(ItemStack arrow, float damageModifier) {
        ArrowEntity arrowEntity = new ArrowEntity(this.getWorld(), this);
        arrowEntity.setDamage(arrowEntity.getDamage() + damageModifier * 0.5F + this.getRandom().nextGaussian() * 0.25);

        if (arrow.hasEnchantments()) {
            int flameLevel = net.minecraft.enchantment.EnchantmentHelper.getLevel(
                    net.minecraft.enchantment.Enchantments.FLAME, arrow);
            if (flameLevel > 0) {
                arrowEntity.setOnFireFor(100);
            }
        }

        if (arrow.getItem() instanceof net.minecraft.item.SpectralArrowItem) {
            arrowEntity.addEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.GLOWING, 200));
        }

        return arrowEntity;
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
    public boolean canEquip(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        try {
            if (item instanceof ArmorItem) return true;
            return item instanceof BowItem || item instanceof CrossbowItem;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    public boolean canPickupItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof ArmorItem;
    }

    // ====== МЕТОД ПОДБОРА (оставляем как было) ======
    @Override
    public void tick() {
        if (pickupCooldown > 0) pickupCooldown--;
        if (isPickingUp && pickupCooldown == 0) {
            performSafePickup();
            isPickingUp = false;
        }

        super.tick();

        if (!this.getWorld().isClient()) {
            // Сбрасываем цель, если она мертва или удалена
            if (this.getTarget() != null && (!this.getTarget().isAlive() || this.getTarget().isRemoved())) {
                this.setTarget(null);
                this.commandedTarget = null;
            }

            // Сбрасываем commandedTarget, если он мертв
            if (this.commandedTarget != null && (!this.commandedTarget.isAlive() || this.commandedTarget.isRemoved())) {
                this.commandedTarget = null;
                this.setTarget(null);
            }

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

                if (distance < 25.0 && this.age % 60 == 0) {
                    this.heal(1.0f);
                }
            }
        }
    }

    public void tryPickupItems() {
        if (pickupCooldown > 0 || isPickingUp) return;

        Vec3d pos = this.getPos();
        Box searchBox = new Box(pos.x - 3, pos.y - 1, pos.z - 3,
                pos.x + 3, pos.y + 1, pos.z + 3);

        for (ItemEntity itemEntity : this.getWorld().getEntitiesByClass(
                ItemEntity.class, searchBox, this::canPickupItemEntity)) {
            if (this.squaredDistanceTo(itemEntity) < 9.0) {
                this.isPickingUp = true;
                this.pickupCooldown = 10;
                return;
            }
        }
    }

    private boolean canPickupItemEntity(ItemEntity itemEntity) {
        return itemEntity.isAlive() &&
                !itemEntity.cannotPickup() &&
                this.canPickupItem(itemEntity.getStack());
    }

    private void performSafePickup() {
        Vec3d pos = this.getPos();
        Box searchBox = new Box(pos.x - 2, pos.y - 1, pos.z - 2,
                pos.x + 2, pos.y + 1, pos.z + 2);

        for (ItemEntity itemEntity : this.getWorld().getEntitiesByClass(
                ItemEntity.class, searchBox, this::canPickupItemEntity)) {
            if (this.squaredDistanceTo(itemEntity) < 4.0) {
                ItemStack stack = itemEntity.getStack().copy();
                EquipmentSlot slot = getEquipmentSlotForItem(stack);

                if (slot != null) {
                    ItemStack currentItem = this.getEquippedStack(slot);
                    if (currentItem.isEmpty() || isBetterItem(stack, currentItem)) {
                        this.equipStack(slot, stack);
                        itemEntity.discard();
                        this.playSound(net.minecraft.sound.SoundEvents.ENTITY_ITEM_PICKUP, 0.2F, 1.0F);
                        if (!currentItem.isEmpty()) this.dropStack(currentItem);
                        break;
                    }
                }
            }
        }
    }

    @Nullable
    private EquipmentSlot getEquipmentSlotForItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ArmorItem) return ((ArmorItem) item).getSlotType();
        else if (item instanceof BowItem || item instanceof CrossbowItem) return EquipmentSlot.MAINHAND;
        return null;
    }

    private boolean isBetterItem(ItemStack newItem, ItemStack currentItem) {
        if (newItem.getItem() instanceof ArmorItem && currentItem.getItem() instanceof ArmorItem) {
            ArmorItem newArmor = (ArmorItem) newItem.getItem();
            ArmorItem currentArmor = (ArmorItem) currentItem.getItem();
            if (newArmor.getProtection() > currentArmor.getProtection()) return true;
            if (newItem.hasEnchantments() && !currentItem.hasEnchantments()) return true;
        }
        if ((newItem.getItem() instanceof BowItem && currentItem.getItem() instanceof BowItem) ||
                (newItem.getItem() instanceof CrossbowItem && currentItem.getItem() instanceof CrossbowItem)) {
            return newItem.hasEnchantments() && !currentItem.hasEnchantments();
        }
        return false;
    }
    // ====== КОНЕЦ МЕТОДА ПОДБОРА ======

    public void setOwner(PlayerEntity player) {
        this.ownerUuid = player.getUuid();
        this.owner = player;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUuid != null && this.getWorld() instanceof ServerWorld) {
            PlayerEntity player = ((ServerWorld) this.getWorld()).getPlayerByUuid(this.ownerUuid);
            if (player instanceof LivingEntity) this.owner = player;
        }
        return this.owner;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof LivingEntity attacker) {
            LivingEntity owner = getOwner();
            if (owner != null && attacker.getUuid().equals(owner.getUuid())) return false;
            if (isAlly(attacker)) return false;
        }
        return super.damage(source, amount);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.ownerUuid != null) nbt.putUuid("Owner", this.ownerUuid);
        if (this.commandedTarget != null) nbt.putUuid("CommandedTarget", this.commandedTarget.getUuid());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("Owner")) this.ownerUuid = nbt.getUuid("Owner");
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

    // ====== ЦЕЛИ КАК У ЗОМБИ ======
    static class FollowOwnerGoal extends Goal {
        private final NecroSkeletonEntity minion;
        private LivingEntity owner;
        private final double speed;
        private final float maxDistance;
        private final float minDistance;
        private int updateCountdownTicks;
        private int cantReachOwnerTicks = 0;

        public FollowOwnerGoal(NecroSkeletonEntity minion, double speed, float maxDistance, float minDistance) {
            this.minion = minion;
            this.speed = speed;
            this.maxDistance = maxDistance;
            this.minDistance = minDistance;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            // Не следовать, если есть цель
            if (minion.getTarget() != null && minion.getTarget().isAlive()) return false;
            LivingEntity owner = minion.getOwner();
            if (owner == null || owner.isSpectator() || !owner.isAlive()) return false;
            double distance = minion.squaredDistanceTo(owner);
            if (distance < (double)(this.minDistance * this.minDistance)) return false;
            this.owner = owner;
            return true;
        }

        @Override
        public boolean shouldContinue() {
            if (minion.getTarget() != null && minion.getTarget().isAlive()) return false;
            if (this.owner == null || !this.owner.isAlive()) return false;
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

            if (--this.updateCountdownTicks <= 0) {
                this.updateCountdownTicks = 10;
                boolean canPath = minion.getNavigation().startMovingTo(this.owner, this.speed);
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
        private final NecroSkeletonEntity minion;
        public RevengeForOwnerGoal(NecroSkeletonEntity minion) { this.minion = minion; }

        @Override
        public boolean canStart() {
            LivingEntity owner = minion.getOwner();
            if (owner == null) return false;
            LivingEntity attacker = owner.getAttacker();
            if (attacker != null && attacker.isAlive() && attacker != minion) {
                if (minion.isAlly(attacker)) return false;
                minion.setTarget(attacker);
                return true;
            }
            return false;
        }
    }

    static class RevengeForSelfGoal extends Goal {
        private final NecroSkeletonEntity minion;
        public RevengeForSelfGoal(NecroSkeletonEntity minion) { this.minion = minion; }

        @Override
        public boolean canStart() {
            LivingEntity attacker = minion.getAttacker();
            if (attacker != null && attacker.isAlive()) {
                LivingEntity owner = minion.getOwner();
                if (owner != null && attacker == owner) return false;
                if (minion.isAlly(attacker)) return false;
                minion.setTarget(attacker);
                return true;
            }
            return false;
        }
    }

    static class SupportOwnerGoal extends Goal {
        private final NecroSkeletonEntity minion;
        private int cooldown = 0;
        public SupportOwnerGoal(NecroSkeletonEntity minion) { this.minion = minion; }

        @Override
        public boolean canStart() {
            if (cooldown > 0) { cooldown--; return false; }
            LivingEntity owner = minion.getOwner();
            if (owner == null || !owner.isAlive()) return false;
            LivingEntity ownerTarget = owner.getAttacking();
            if (ownerTarget != null && ownerTarget.isAlive() && ownerTarget != minion) {
                if (minion.isAlly(ownerTarget)) return false;
                minion.setTarget(ownerTarget);
                cooldown = 20;
                return true;
            }
            return false;
        }
    }

    // НОВАЯ ЦЕЛЬ: атаковать командированную цель от посоха
    static class CommandTargetGoal extends Goal {
        private final NecroSkeletonEntity minion;
        public CommandTargetGoal(NecroSkeletonEntity minion) { this.minion = minion; }

        @Override
        public boolean canStart() {
            return minion.commandedTarget != null &&
                    minion.commandedTarget.isAlive() &&
                    !minion.isAlly(minion.commandedTarget);
        }

        @Override
        public void start() {
            if (minion.commandedTarget != null) {
                minion.setTarget(minion.commandedTarget);
            }
        }

        @Override
        public boolean shouldContinue() {
            return minion.commandedTarget != null &&
                    minion.commandedTarget.isAlive() &&
                    minion.getTarget() == minion.commandedTarget;
        }

        @Override
        public void stop() {
            minion.setTarget(null);
        }
    }
}