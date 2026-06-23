package com.adam.firemagic.items.necromancer;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class NecromancerSpellBookItem extends Item {
    public NecromancerSpellBookItem(Settings settings) {
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

            // УСТАНАВЛИВАЕМ ШКОЛУ НЕКРОМАНТА
            manaData.setNecromancerSchool(true);
            ManaManager.setServerData(serverPlayer, manaData);

            // === ВЫДАЧА ДОСТИЖЕНИЯ ===
            Identifier advancementId = new Identifier("firemagic", "learn_necromancer_magic");
            Advancement advancement = serverPlayer.getServer().getAdvancementLoader().get(advancementId);

            if (advancement != null) {
                AdvancementProgress progress = serverPlayer.getAdvancementTracker().getProgress(advancement);
                if (!progress.isDone()) {
                    serverPlayer.getAdvancementTracker().grantCriterion(advancement, "learned");
                    serverPlayer.sendMessage(Text.literal("§eВы освоили §5Темное искусство некромантии§e!"), true);
                }
            } else {
                serverPlayer.sendMessage(Text.literal("§eВы освоили §5Темное искусство некромантии§e!"), true);
                FireMagicMod.LOGGER.warn("Достижение learn_necromancer_magic не найдено!");
            }

            // === ВЫДАЧА ПРЕДМЕТОВ НЕКРОМАНТА ===
            // 1. Посох некроманта - главный инструмент
            giveNecromancerStaff(serverPlayer);

            // 2. Рецепты яиц призыва
            grantRecipes(serverPlayer);

            // УДАЛЕНИЕ КНИГИ
            serverPlayer.setStackInHand(hand, ItemStack.EMPTY);
            return TypedActionResult.success(ItemStack.EMPTY);
        }

        return TypedActionResult.success(stack);
    }

    private void giveNecromancerStaff(ServerPlayerEntity player) {
        try {
            // Создаем посох некроманта
            ItemStack staffStack = new ItemStack(FireMagicMod.NECROMANCER_STAFF);

            // Пытаемся положить в инвентарь
            if (player.getInventory().insertStack(staffStack)) {
                player.sendMessage(Text.literal("§aВы получили §5Посох некроманта§a!"), true);
            } else {
                // Если не помещается - выбрасываем на землю
                player.dropItem(staffStack, false);
                player.sendMessage(Text.literal("§eПосох некроманта упал на землю (нет места в инвентаре)"), true);
            }

        } catch (Exception e) {
            FireMagicMod.LOGGER.error("Ошибка при выдаче посоха некроманта: " + e.getMessage());

            // Пытаемся через команду
            try {
                player.getServer().getCommandManager().executeWithPrefix(
                        player.getCommandSource().withSilent(),
                        "give " + player.getName().getString() + " firemagic:necromancer_staff"
                );
                player.sendMessage(Text.literal("§aПосох некроманта выдан через команду"), true);
            } catch (Exception ex) {
                player.sendMessage(Text.literal("§cОшибка при выдаче посоха! Используйте /give"), true);
                FireMagicMod.LOGGER.error("Команда выдачи посоха тоже не сработала: " + ex.getMessage());
            }
        }
    }

    private void grantRecipes(ServerPlayerEntity player) {
        try {
            // Способ 1: через команды (самый надежный)
            String[] recipes = {
                    "firemagic:zombie_minion_egg.json",
                    "firemagic:skeleton_minion_egg",
                    "firemagic:necromancer_staff" // Дублируем рецепт посоха на всякий случай
            };

            for (String recipe : recipes) {
                try {
                    player.getServer().getCommandManager().executeWithPrefix(
                            player.getCommandSource().withSilent(),
                            "recipe give " + player.getName().getString() + " " + recipe
                    );
                } catch (Exception e) {
                    FireMagicMod.LOGGER.warn("Не удалось выдать рецепт " + recipe + ": " + e.getMessage());
                }
            }

            player.sendMessage(Text.literal("§aПолучены рецепты призыва нежити и посоха!"), true);

        } catch (Exception e) {
            FireMagicMod.LOGGER.error("Ошибка при выдаче рецептов: " + e.getMessage());
            player.sendMessage(Text.literal("§eРецепты изучены автоматически"), true);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("§5§lКнига некроманта").formatted(net.minecraft.util.Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("§7Позволяет призывать и контролировать нежить").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§aВы получите: §5Посох некроманта").formatted(net.minecraft.util.Formatting.GREEN));
        tooltip.add(Text.literal("§aРецепты: §eЯйца зомби и скелета").formatted(net.minecraft.util.Formatting.GREEN));
        tooltip.add(Text.literal("§8◇ Лимит: 1 зомби + 1 скелет на игрока").formatted(net.minecraft.util.Formatting.DARK_GRAY));
    }
}