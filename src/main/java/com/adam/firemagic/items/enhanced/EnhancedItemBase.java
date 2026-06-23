// 📄 com/adam/firemagic/items/enhanced/EnhancedItemBase.java
package com.adam.firemagic.items.enhanced;

import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.nbt.NbtCompound;

public abstract class EnhancedItemBase extends SwordItem {
    // Счетчик для активации способностей
    private static final String HIT_COUNTER_KEY = "HitCounter";
    private static final String COOLDOWN_KEY = "Cooldown";

    public EnhancedItemBase(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    // Метод для увеличения счетчика ударов
    protected void incrementHitCounter(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        int hits = nbt.getInt(HIT_COUNTER_KEY) + 1;
        nbt.putInt(HIT_COUNTER_KEY, hits);
    }

    // Получение текущего счетчика
    protected int getHitCounter(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return nbt.getInt(HIT_COUNTER_KEY);
    }

    // Сброс счетчика
    protected void resetHitCounter(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt(HIT_COUNTER_KEY, 0);
    }

    // Проверка кулдауна
    protected boolean isOnCooldown(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return nbt.getLong(COOLDOWN_KEY) > System.currentTimeMillis();
    }

    // Установка кулдауна (в миллисекундах)
    protected void setCooldown(ItemStack stack, long cooldownMs) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong(COOLDOWN_KEY, System.currentTimeMillis() + cooldownMs);
    }

    // Получение оставшегося времени кулдауна
    protected long getRemainingCooldown(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        long cooldownEnd = nbt.getLong(COOLDOWN_KEY);
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }

    // Абстрактный метод для активации способности (переопределяется в каждом предмете)
    public abstract boolean tryActivateAbility(ItemStack stack, net.minecraft.world.World world,
                                               net.minecraft.entity.LivingEntity user,
                                               net.minecraft.entity.Entity target);
}