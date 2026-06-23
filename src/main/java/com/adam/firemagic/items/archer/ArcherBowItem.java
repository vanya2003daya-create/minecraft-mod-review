package com.adam.firemagic.items.archer;

import com.adam.firemagic.entities.PiercingArrowEntity;
import com.adam.firemagic.entities.TeleportArrowEntity;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ArcherBowItem extends BowItem {

    public ArcherBowItem(Settings settings) {
        super(settings);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient()) return;

        if (!(user instanceof ServerPlayerEntity serverPlayer)) return;

        PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

        // Проверка школы лучника
        if (!manaData.hasArcherSchool()) {
            serverPlayer.sendMessage(Text.literal("§cВы не можете использовать этот лук без школы лучников!"), true);
            return;
        }

        int charge = this.getMaxUseTime(stack) - remainingUseTicks;
        float pullProgress = BowItem.getPullProgress(charge);

        if (pullProgress < 0.1f) return;

        // === ПОДДЕРЖКА ВАНИЛЬНЫХ СТРЕЛ С ЧАРАМИ ===
        // Получаем тип снаряда (стрела, снежок и т.д.)
        ItemStack projectileStack = serverPlayer.getProjectileType(stack);
        boolean hasProjectile = !projectileStack.isEmpty() || serverPlayer.getAbilities().creativeMode;

        // Проверка магических стрел
        boolean useExplosiveArrow = manaData.isExplosiveArrowReady();
        boolean usePiercingArrow = manaData.isPiercingArrowReady();
        boolean useTeleportArrow = manaData.isTeleportArrowReady();

        // Если нет ни обычных стрел, ни магических
        if (!hasProjectile && !useExplosiveArrow && !usePiercingArrow && !useTeleportArrow) {
            serverPlayer.sendMessage(Text.literal("§cНет стрел для выстрела!"), true);
            return;
        }

        // Расчет начальной позиции
        Vec3d lookVec = serverPlayer.getRotationVec(1.0f);
        Vec3d spawnPos = serverPlayer.getEyePos()
                .subtract(0, 0.1, 0)
                .add(lookVec.multiply(0.5));

        ArrowEntity arrow = null;
        PiercingArrowEntity piercingArrow = null;
        TeleportArrowEntity teleportArrow = null;

        if (useExplosiveArrow) {
            // Логика взрывной стрелы
            arrow = new ArrowEntity(world, serverPlayer);
            arrow.setPosition(spawnPos);

            // Применяем зачарования к взрывной стреле
            applyEnchantmentsToArrow(arrow, stack, pullProgress);

            // Отмечаем как взрывную
            ExplosiveArrowManager.markArrowAsExplosive(arrow.getUuid(), serverPlayer.getUuid());

            serverPlayer.sendMessage(Text.literal("§eВыпустили §cвзрывную стрелу§e!"), true);
            manaData.setExplosiveArrowReady(false);

        } else if (usePiercingArrow) {
            // Логика пронзающей стрелы - отдельный класс
            piercingArrow = new PiercingArrowEntity(world, serverPlayer);
            piercingArrow.setPosition(spawnPos);

            serverPlayer.sendMessage(Text.literal("§eВыпустили §bпронзающую стрелу§e!"), true);
            manaData.setPiercingArrowReady(false);

        } else if (useTeleportArrow) {
            // Логика стрелы-телепорта
            teleportArrow = new TeleportArrowEntity(world, serverPlayer);
            teleportArrow.setPosition(spawnPos);

            serverPlayer.sendMessage(Text.literal("§eВыпустили §dстрелу телепортации§e!"), true);
            manaData.setTeleportArrowReady(false);

        } else {
            // === ОБЫЧНАЯ ВАНИЛЬНАЯ СТРЕЛА С ПОДДЕРЖКОЙ ВСЕХ ЧАР ===
            arrow = createVanillaArrow(world, serverPlayer, stack, projectileStack);
            arrow.setPosition(spawnPos);

            // Применяем все зачарования
            applyEnchantmentsToArrow(arrow, stack, pullProgress);

            // Расходуем стрелу (с учетом Бесконечности)
            if (!serverPlayer.getAbilities().creativeMode) {
                int infinityLevel = EnchantmentHelper.getLevel(Enchantments.INFINITY, stack);
                boolean hasInfinity = infinityLevel > 0;

                // Бесконечность не работает со стрелами зелья
                boolean isTippedArrow = projectileStack.isOf(Items.TIPPED_ARROW);

                if (!hasInfinity || isTippedArrow) {
                    projectileStack.decrement(1);
                    if (projectileStack.isEmpty()) {
                        serverPlayer.getInventory().removeOne(projectileStack);
                    }
                }
            }
        }

        // Устанавливаем скорость для всех типов стрел
        float speed = pullProgress * 3.0f;

        if (arrow != null) {
            arrow.setVelocity(lookVec.x, lookVec.y, lookVec.z, speed, 1.0f);
            world.spawnEntity(arrow);
        } else if (piercingArrow != null) {
            piercingArrow.setVelocity(lookVec.x, lookVec.y, lookVec.z, speed, 1.0f);
            world.spawnEntity(piercingArrow);
        } else if (teleportArrow != null) {
            teleportArrow.setVelocity(lookVec.x, lookVec.y, lookVec.z, speed, 1.0f);
            world.spawnEntity(teleportArrow);
        }

        // Звук выстрела
        world.playSound(null,
                serverPlayer.getBlockPos(),
                SoundEvents.ENTITY_ARROW_SHOOT,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f / (world.getRandom().nextFloat() * 0.4f + 1.2f) + pullProgress * 0.5f);

        // Статистика
        serverPlayer.incrementStat(Stats.USED.getOrCreateStat(this));

        // Сохраняем изменения маны
        ManaManager.setServerData(serverPlayer, manaData);
    }

    /**
     * Создаёт ванильную стрелу с поддержкой типов снарядов
     */
    private ArrowEntity createVanillaArrow(World world, LivingEntity shooter, ItemStack bowStack, ItemStack projectileStack) {
        ArrowEntity arrow = new ArrowEntity(world, shooter);

        // Если это стрела зелья - инициализируем из стака
        if (projectileStack.isOf(Items.TIPPED_ARROW) ||
                (projectileStack.isOf(Items.ARROW) && projectileStack.hasNbt() && projectileStack.getNbt().contains("Potion"))) {
            arrow = new ArrowEntity(world, shooter);
            arrow.initFromStack(projectileStack); // Копируем эффекты зелья
        }

        return arrow;
    }

    /**
     * Применяет все зачарования лука к стреле (как в ванильном майнкрафте)
     */
    private void applyEnchantmentsToArrow(ArrowEntity arrow, ItemStack bowStack, float pullProgress) {
        // 1. Критический удар при полной зарядке
        if (pullProgress >= 1.0F) {
            arrow.setCritical(true);
        }

        // 2. Зачарование "Мощность" (Power)
        int powerLevel = EnchantmentHelper.getLevel(Enchantments.POWER, bowStack);
        if (powerLevel > 0) {
            arrow.setDamage(arrow.getDamage() + (double)powerLevel * 0.5 + 0.5);
        }

        // 3. Зачарование "Отдача" (Punch)
        int punchLevel = EnchantmentHelper.getLevel(Enchantments.PUNCH, bowStack);
        if (punchLevel > 0) {
            arrow.setPunch(punchLevel);
        }

        // 4. Зачарование "Пламя" (Flame)
        int flameLevel = EnchantmentHelper.getLevel(Enchantments.FLAME, bowStack);
        if (flameLevel > 0) {
            arrow.setOnFireFor(100);
        }

        // 5. Зачарование "Небесная кара" (из мода?) - если есть
        // Для совместимости с модами можно добавить проверку других чар
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Проверка наличия стрел (ванильных или магических)
        boolean hasVanillaArrows = !user.getProjectileType(stack).isEmpty();
        boolean hasMagicalArrows = false;

        if (user instanceof ServerPlayerEntity serverPlayer) {
            PlayerManaData manaData = ManaManager.getServerData(serverPlayer);
            hasMagicalArrows = manaData.isExplosiveArrowReady() ||
                    manaData.isPiercingArrowReady() ||
                    manaData.isTeleportArrowReady();
        }

        if (!hasVanillaArrows && !hasMagicalArrows && !user.getAbilities().creativeMode) {
            if (!world.isClient()) {
                user.sendMessage(Text.literal("§cНет стрел для выстрела!"), true);
            }
            return TypedActionResult.fail(stack);
        }

        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000; // Стандартное время для лука
    }

    // === ПОДДЕРЖКА ЗАЧАРОВАНИЙ ДЛЯ ЛУКА ===

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantability() {
        return 25; // Высокая вероятность хороших чар (как у лука)
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(Items.STRING); // Можно починить нитью
    }
}