package com.adam.firemagic.items.archer;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.advancement.Advancement;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ArcherSpellBookItem extends Item {
    public ArcherSpellBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (user instanceof ServerPlayerEntity serverPlayer) {
            PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

            if (manaData.hasMagicSchool()) {
                serverPlayer.sendMessage(Text.literal("§cУ вас уже изучена школа магии!"), true);
                return TypedActionResult.fail(stack);
            }

            // УСТАНАВЛИВАЕМ ШКОЛУ ЛУЧНИКА
            manaData.setArcherSchool(true);
            ManaManager.setServerData(serverPlayer, manaData);

            // === ВЫДАЧА ДОСТИЖЕНИЯ ===
            Advancement advancement = serverPlayer.getServer().getAdvancementLoader()
                    .get(new Identifier("firemagic", "learn_archer_magic"));

            if (advancement != null) {
                serverPlayer.getAdvancementTracker().grantCriterion(advancement, "learned");
                serverPlayer.sendMessage(Text.literal("§eВы освоили §6Искусство лучника§e!"), true);

                // === ВЫДАЧА РЕЦЕПТОВ ВСЕХ СТРЕЛ ===
                serverPlayer.getServer().getCommandManager().executeWithPrefix(
                        serverPlayer.getCommandSource().withSilent(),
                        "recipe give @s firemagic:explosive_arrow"
                );

                serverPlayer.getServer().getCommandManager().executeWithPrefix(
                        serverPlayer.getCommandSource().withSilent(),
                        "recipe give @s firemagic:piercing_arrow"
                );

                serverPlayer.getServer().getCommandManager().executeWithPrefix(
                        serverPlayer.getCommandSource().withSilent(),
                        "recipe give @s firemagic:teleport_arrow"
                );

                // === ВЫДАЧА РЕЦЕПТА КОЛЬЦА (ТОЛЬКО ДЛЯ ЛУЧНИКОВ) ===
                serverPlayer.getServer().getCommandManager().executeWithPrefix(
                        serverPlayer.getCommandSource().withSilent(),
                        "recipe give @s firemagic:archer_ring"
                );

                serverPlayer.getServer().getCommandManager().executeWithPrefix(
                        serverPlayer.getCommandSource().withSilent(),
                        "recipe give @s firemagic:hunter_helm"
                );

                serverPlayer.sendMessage(Text.literal("§aПолучены рецепты стрел, кольца и шлема охотника!"), true);
            } else {
                System.out.println("[ERROR] Достижение learn_archer_magic не найдено!");
            }

            // ВЫДАЧА ЛУКА
            ItemStack bowStack = new ItemStack(FireMagicMod.ARCHER_BOW);
            if (!serverPlayer.getInventory().insertStack(bowStack)) {
                serverPlayer.dropItem(bowStack, false);
            }

            // УДАЛЕНИЕ КНИГИ
            serverPlayer.setStackInHand(hand, ItemStack.EMPTY);
            return TypedActionResult.success(ItemStack.EMPTY);
        }

        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("§6§lКнига лучника").formatted(net.minecraft.util.Formatting.GOLD));
        tooltip.add(Text.literal("§7Даёт доступ к рецептам кольца и стрел").formatted(net.minecraft.util.Formatting.GRAY));
    }
}