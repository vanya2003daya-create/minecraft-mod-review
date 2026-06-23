package com.adam.firemagic.items;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class FireSwordItem extends SwordItem {
    private static final int FIRE_DURATION = 3; // секунды
    private static final int MANA_RESTORE = 1;

    public FireSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.postHit(stack, target, attacker);


        // Только на сервере и для игроков
        if (!attacker.getWorld().isClient() && attacker instanceof ServerPlayerEntity player) {
            // Обычный поджог (оранжевый огонь)
            target.setOnFireFor(FIRE_DURATION);

            // Восстановление маны
            PlayerManaData manaData = ManaManager.getServerData(player);
            int current = manaData.getMana();
            int max = manaData.getMaxMana();

            if (current < max) {
                manaData.setMana(current + MANA_RESTORE);
                ManaManager.setServerData(player, manaData);
            }
        }

        return result;
    }
}