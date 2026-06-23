package com.adam.firemagic.items.enhanced;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public abstract class BaseEnhancedArmorItem extends ArmorItem {

    public BaseEnhancedArmorItem(ArmorMaterial material, Type type, Settings settings) {
        super(material, type, settings);
    }

    protected boolean isOnCooldown(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return nbt.getLong("Cooldown") > System.currentTimeMillis();
    }

    protected void setCooldown(ItemStack stack, long ms) {
        stack.getOrCreateNbt().putLong("Cooldown", System.currentTimeMillis() + ms);
    }

    protected long getCooldown(ItemStack stack) {
        long end = stack.getOrCreateNbt().getLong("Cooldown");
        return Math.max(0, end - System.currentTimeMillis());
    }

    // ❌ ЗАПРЕТ НАДЕВАТЬ НЕ-ЛУЧНИКУ
    public abstract boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity);
}