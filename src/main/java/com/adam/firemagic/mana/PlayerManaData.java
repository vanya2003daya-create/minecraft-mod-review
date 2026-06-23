package com.adam.firemagic.mana;

import com.adam.firemagic.FireMagicMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.Entity;

import java.util.UUID;

public class PlayerManaData {
    private int mana = 20;
    private int maxMana = 20;
    private long lastRegenTime = 0;

    // Школы магии
    private boolean hasMinerSchool = false;
    private boolean hasArcherSchool = false;
    private boolean hasNecromancerSchool = false;
    private boolean hasEnhancerSchool = false; // ✅ ДОБАВЛЕНО: Школа улучшения

    private long lastRingCheck = 0;
    private long lastCurseTime = 0;

    // Временные состояния (не сохраняются в NBT)
    private boolean explosiveArrowReady = false;
    private boolean piercingArrowReady = false;
    private boolean teleportArrowReady = false;

    // Данные некроманта
    private UUID zombieMinionId = null;
    private UUID skeletonMinionId = null;
    private UUID phantomMinionId = null;
    private long lastMinionTeleportTime = 0;

    public PlayerManaData() {}

    public PlayerManaData(NbtCompound nbt) {
        this.mana = nbt.getInt("mana");
        this.maxMana = nbt.getInt("maxMana");
        this.lastRegenTime = nbt.getLong("lastRegenTime");
        this.hasMinerSchool = nbt.getBoolean("hasMinerSchool");
        this.hasArcherSchool = nbt.getBoolean("hasArcherSchool");
        this.hasNecromancerSchool = nbt.getBoolean("hasNecromancerSchool");
        this.hasEnhancerSchool = nbt.getBoolean("hasEnhancerSchool"); // ✅ ДОБАВЛЕНО

        // 🔴 ИСПРАВЛЕНИЕ БАГА: проверяем на повреждённые данные
        if (this.maxMana <= 0) {
            this.maxMana = 20; // Стандартное значение
        }

        if (this.mana <= 0 && this.maxMana > 0) {
            this.mana = this.maxMana; // Начинаем с полной маны!
            FireMagicMod.LOGGER.warn("⚠️ Исправлены повреждённые данные маны: " +
                    this.mana + "/" + this.maxMana);
        }

        // Загрузка данных некроманта
        if (nbt.contains("zombieMinionIdMost") && nbt.contains("zombieMinionIdLeast")) {
            this.zombieMinionId = new UUID(nbt.getLong("zombieMinionIdMost"),
                    nbt.getLong("zombieMinionIdLeast"));
        }
        if (nbt.contains("skeletonMinionIdMost") && nbt.contains("skeletonMinionIdLeast")) {
            this.skeletonMinionId = new UUID(nbt.getLong("skeletonMinionIdMost"),
                    nbt.getLong("skeletonMinionIdLeast"));
        }
        if (nbt.contains("phantomMinionIdMost") && nbt.contains("phantomMinionIdLeast")) {
            this.phantomMinionId = new UUID(nbt.getLong("phantomMinionIdMost"),
                    nbt.getLong("phantomMinionIdLeast"));
        }

        this.lastMinionTeleportTime = nbt.getLong("lastMinionTeleportTime");
    }

    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }
    public long getLastRegenTime() { return lastRegenTime; }
    public long getLastRingCheck() { return lastRingCheck; }
    public void setLastRingCheck(long time) { this.lastRingCheck = time; }

    public void setMana(int mana) {
        this.mana = Math.max(0, Math.min(mana, maxMana));
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
        if (this.mana > maxMana) this.mana = maxMana;
    }

    public void setLastRegenTime(long time) { this.lastRegenTime = time; }

    // Школа шахтёра
    public boolean hasMinerSchool() { return hasMinerSchool; }
    public void setMinerSchool(boolean value) {
        this.hasMinerSchool = value;
        if (value) {
            this.hasArcherSchool = false;
            this.hasNecromancerSchool = false;
            this.hasEnhancerSchool = false; // ✅ ДОБАВЛЕНО
        }
    }

    // Школа лучника
    public boolean hasArcherSchool() { return hasArcherSchool; }
    public void setArcherSchool(boolean value) {
        this.hasArcherSchool = value;
        if (value) {
            this.hasMinerSchool = false;
            this.hasNecromancerSchool = false;
            this.hasEnhancerSchool = false; // ✅ ДОБАВЛЕНО
        }
    }

    // Школа некроманта
    public boolean hasNecromancerSchool() { return hasNecromancerSchool; }
    public void setNecromancerSchool(boolean value) {
        this.hasNecromancerSchool = value;
        if (value) {
            this.hasMinerSchool = false;
            this.hasArcherSchool = false;
            this.hasEnhancerSchool = false; // ✅ ДОБАВЛЕНО
        }
    }

    // ✅ ДОБАВЛЕНО: Школа улучшения
    public boolean hasEnhancerSchool() { return hasEnhancerSchool; }
    public void setEnhancerSchool(boolean value) {
        this.hasEnhancerSchool = value;
        if (value) {
            this.hasMinerSchool = false;
            this.hasArcherSchool = false;
            this.hasNecromancerSchool = false;
        }
    }

    // Проверка любой школы
    public boolean hasMagicSchool() {
        return hasMinerSchool || hasArcherSchool ||
                hasNecromancerSchool || hasEnhancerSchool;
    }

    // Получение названия текущей школы (для отладки)
    public String getCurrentSchoolName() {
        if (hasMinerSchool()) return "Miner";
        if (hasArcherSchool()) return "Archer";
        if (hasNecromancerSchool()) return "Necromancer";
        if (hasEnhancerSchool()) return "Enhancer";
        return "None";
    }

    public boolean consumeMana(int amount) {
        if (mana >= amount) {
            mana -= amount;
            return true;
        }
        return false;
    }

    public void regenerateMana() {
        if (mana < maxMana) mana++;
    }

    public void resetOnDeath() {
        this.hasMinerSchool = false;
        this.hasArcherSchool = false;
        this.hasNecromancerSchool = false;
        this.hasEnhancerSchool = false; // ✅ ДОБАВЛЕНО
        this.explosiveArrowReady = false;
        this.piercingArrowReady = false;
        this.teleportArrowReady = false;

        // Очистка мобов при смерти
        this.zombieMinionId = null;
        this.skeletonMinionId = null;
        this.phantomMinionId = null;
    }

    // Геттеры/сеттеры для временных флагов стрел
    public boolean isExplosiveArrowReady() { return explosiveArrowReady; }
    public void setExplosiveArrowReady(boolean ready) { this.explosiveArrowReady = ready; }

    public boolean isPiercingArrowReady() { return piercingArrowReady; }
    public void setPiercingArrowReady(boolean ready) { this.piercingArrowReady = ready; }

    public boolean isTeleportArrowReady() { return teleportArrowReady; }
    public void setTeleportArrowReady(boolean ready) { this.teleportArrowReady = ready; }

    // Методы для управления мобами некроманта

    // Зомби
    public boolean hasZombieMinion() {
        return zombieMinionId != null;
    }

    public UUID getZombieMinionId() {
        return zombieMinionId;
    }

    public void setZombieMinionId(UUID id) {
        this.zombieMinionId = id;
    }

    public void clearZombieMinion() {
        this.zombieMinionId = null;
    }

    public boolean isZombieMinionAlive(ServerWorld world) {
        if (zombieMinionId == null) return false;
        Entity entity = world.getEntity(zombieMinionId);
        return entity != null && entity.isAlive();
    }

    // Скелет
    public boolean hasSkeletonMinion() {
        return skeletonMinionId != null;
    }

    public UUID getSkeletonMinionId() {
        return skeletonMinionId;
    }

    public void setSkeletonMinionId(UUID id) {
        this.skeletonMinionId = id;
    }

    public void clearSkeletonMinion() {
        this.skeletonMinionId = null;
    }

    public boolean isSkeletonMinionAlive(ServerWorld world) {
        if (skeletonMinionId == null) return false;
        Entity entity = world.getEntity(skeletonMinionId);
        return entity != null && entity.isAlive();
    }

    // Призрак
    public boolean hasPhantomMinion() {
        return phantomMinionId != null;
    }

    public long getLastCurseTime() {
        return lastCurseTime;
    }

    public void setLastCurseTime(long time) {
        this.lastCurseTime = time;
    }

    public boolean canUseCurse(long currentWorldTime) {
        return currentWorldTime - lastCurseTime >= 1200; // 60 секунд
    }

    public boolean isPhantomMinionAlive(ServerWorld world) {
        if (phantomMinionId == null) return false;
        Entity entity = world.getEntity(phantomMinionId);
        return entity != null && entity.isAlive();
    }

    // Телепорт мобов (кулдаун)
    public long getLastMinionTeleportTime() {
        return lastMinionTeleportTime;
    }

    public void setLastMinionTeleportTime(long time) {
        this.lastMinionTeleportTime = time;
    }

    public boolean canTeleportMinions(long currentTime) {
        return currentTime - lastMinionTeleportTime >= 200; // 10 секунд (200 тиков)
    }

    // Общий счетчик мобов
    public int getActiveMinionCount() {
        int count = 0;
        if (zombieMinionId != null) count++;
        if (skeletonMinionId != null) count++;
        if (phantomMinionId != null) count++;
        return count;
    }

    // Очистка мертвых мобов
    public void cleanupDeadMinions(ServerWorld world) {
        if (zombieMinionId != null && !isZombieMinionAlive(world)) {
            zombieMinionId = null;
        }
        if (skeletonMinionId != null && !isSkeletonMinionAlive(world)) {
            skeletonMinionId = null;
        }
        if (phantomMinionId != null && !isPhantomMinionAlive(world)) {
            phantomMinionId = null;
        }
    }

    public NbtCompound writeToNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("mana", mana);
        nbt.putInt("maxMana", maxMana);
        nbt.putLong("lastRegenTime", lastRegenTime);
        nbt.putBoolean("hasMinerSchool", hasMinerSchool);
        nbt.putBoolean("hasArcherSchool", hasArcherSchool);
        nbt.putBoolean("hasNecromancerSchool", hasNecromancerSchool);
        nbt.putBoolean("hasEnhancerSchool", hasEnhancerSchool); // ✅ ДОБАВЛЕНО
        nbt.putLong("LastCurseTime", lastCurseTime);

        // Сохранение данных некроманта
        if (zombieMinionId != null) {
            nbt.putLong("zombieMinionIdMost", zombieMinionId.getMostSignificantBits());
            nbt.putLong("zombieMinionIdLeast", zombieMinionId.getLeastSignificantBits());
        }
        if (skeletonMinionId != null) {
            nbt.putLong("skeletonMinionIdMost", skeletonMinionId.getMostSignificantBits());
            nbt.putLong("skeletonMinionIdLeast", skeletonMinionId.getLeastSignificantBits());
        }
        if (phantomMinionId != null) {
            nbt.putLong("phantomMinionIdMost", phantomMinionId.getMostSignificantBits());
            nbt.putLong("phantomMinionIdLeast", phantomMinionId.getLeastSignificantBits());
        }

        nbt.putLong("lastMinionTeleportTime", lastMinionTeleportTime);

        return nbt;
    }
}