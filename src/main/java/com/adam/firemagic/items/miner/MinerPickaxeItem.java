package com.adam.firemagic.items.miner;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class MinerPickaxeItem extends PickaxeItem {
    public MinerPickaxeItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, 7, attackSpeed, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (user instanceof ServerPlayerEntity serverPlayer) {
            PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

            // === ПРОВЕРКА ТОЛЬКО ШКОЛЫ ШАХТЁРА ===
            if (!manaData.hasMinerSchool()) {
                serverPlayer.setStackInHand(hand, ItemStack.EMPTY);
                serverPlayer.sendMessage(Text.literal("§cКирка исчезла! Нужно изучить школу шахтёра."), true);
                return TypedActionResult.fail(ItemStack.EMPTY);
            }
            openUpgradeScreen(serverPlayer, stack);
        }

        return TypedActionResult.success(stack);
    }

    private void openUpgradeScreen(ServerPlayerEntity player, ItemStack stack) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeItemStack(stack);
        ServerPlayNetworking.send(player, new Identifier(FireMagicMod.MOD_ID, "open_pickaxe_gui"), buf);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity serverPlayer) {
            PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

            // === ПРОВЕРКА ТОЛЬКО ШКОЛЫ ШАХТЁРА ===
            if (!manaData.hasMinerSchool()) {
                serverPlayer.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                serverPlayer.sendMessage(Text.literal("§cКирка исчезла! Нужно изучить школу шахтёра."), true);
                return false;
            }
        }

        if (attacker.getRandom().nextFloat() < 0.5f) {
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS,
                    60,
                    1
            ));
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient() && miner instanceof ServerPlayerEntity player) {
            PlayerManaData manaData = ManaManager.getServerData(player);

            // === ПРОВЕРКА ТОЛЬКО ШКОЛЫ ШАХТЁРА ===
            if (!manaData.hasMinerSchool()) {
                player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                player.sendMessage(Text.literal("§cКирка исчезла! Нужно изучить школу шахтёра."), true);
                return false;
            }

            if (isOreBlock(state)) {
                int newMana = Math.min(manaData.getMana() + 2, manaData.getMaxMana());
                manaData.setMana(newMana);
                ManaManager.setServerData(player, manaData);
            }
        }
        return super.postMine(stack, world, state, pos, miner);
    }

    private boolean isOreBlock(BlockState state) {
        String name = state.getBlock().getTranslationKey().toLowerCase();
        return name.contains("ore") || name.contains("руд");
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("§6§lКирка «Рудокоп»").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("§7• Урон: §e10").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("§7• При ударе: §c50% шанс замедления II").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("§7• Добыча руд: §b+2 маны").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("§7• Прокачка: §dПКМ для открытия GUI").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("§8Требует школу шахтёра").formatted(Formatting.DARK_GRAY)); // ИЗМЕНЕНО: школу шахтёра
    }
}