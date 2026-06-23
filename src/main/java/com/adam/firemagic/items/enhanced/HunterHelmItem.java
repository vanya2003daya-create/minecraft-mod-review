package com.adam.firemagic.items.enhanced;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public class HunterHelmItem extends BaseEnhancedArmorItem {

    public HunterHelmItem() {
        super(ArmorMaterials.DIAMOND, Type.HELMET,
                new Settings().maxCount(1));
    }

    // ❌ ЗАПРЕТ НАДЕВАТЬ НЕ-ЛУЧНИКУ
    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity) {
        if (entity instanceof ServerPlayerEntity player) {
            PlayerManaData data = ManaManager.getServerData(player);
            return data.hasArcherSchool();
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, net.minecraft.world.World world,
                              List<Text> tooltip,
                              net.minecraft.client.item.TooltipContext context) {

        tooltip.add(Text.literal("§6Шлем охотника"));
        tooltip.add(Text.literal("§7Позволяет чувствовать цели"));
        tooltip.add(Text.literal("§8Только для лучников"));
    }
}