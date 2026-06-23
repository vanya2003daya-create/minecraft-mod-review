package com.adam.firemagic.upgrade;

import com.adam.firemagic.items.miner.MinerPickaxeItem;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class PickaxeResourcesManager {
    private static final String TAG_KEY = "firemagicmod_pickaxe_data";

    public static PickaxeResourcesData getData(ItemStack stack) {
        PickaxeResourcesData data = new PickaxeResourcesData();
        if (stack.hasNbt() && stack.getNbt().contains(TAG_KEY)) {
            NbtCompound nbt = stack.getNbt().getCompound(TAG_KEY);
            data = PickaxeResourcesData.fromNbt(nbt);
        }
        return data;
    }

    public static void setData(ItemStack stack, PickaxeResourcesData data) {
        NbtCompound nbt = new NbtCompound();
        stack.getOrCreateNbt().put(TAG_KEY, data.toNbt());
    }

    public static void applyPermanentEffects(ServerPlayerEntity player, PickaxeResourcesData data) {
        removePickaxeEffects(player);

        // Применяем новые эффекты (БЕЗ параметра replace)
        if (data.getResource("coal") >= 64) {
            int amplifier = data.getResource("coal") >= 128 ? 1 : 0;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, -1, amplifier, false, false));
        }

        if (data.getResource("iron") >= 128) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, -1, 0, false, false));
        }

        if (data.getResource("diamond") >= 16) {
            int amplifier = data.getResource("diamond") >= 32 ? 1 : 0;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, -1, amplifier, false, false));
        }

        if (data.getResource("gold") >= 64) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, -1, 0, false, false));
        }

        // Зачарование кирки (изумруды)
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.getItem() instanceof MinerPickaxeItem) {
            // Сбрасываем зачарования (правильный способ для 1.20.1)
            NbtList emptyEnchantments = new NbtList();
            mainHand.getOrCreateNbt().put("Enchantments", emptyEnchantments);

            // Добавляем Удачу I при 16+ изумрудах
            if (data.getResource("emerald") >= 16) {
                mainHand.addEnchantment(Enchantments.FORTUNE, 1);
            }
        }
    }

    public static void removePickaxeEffects(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.HASTE);
        player.removeStatusEffect(StatusEffects.STRENGTH);
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
    }
}