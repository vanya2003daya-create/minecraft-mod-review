package com.adam.firemagic.items.enhanced;

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

public class EnhancerSpellBookItem extends Item {
    public EnhancerSpellBookItem(Settings settings) {
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

            // Проверяем, есть ли уже школа магии
            if (manaData.hasMagicSchool()) {
                serverPlayer.sendMessage(Text.literal("§cУ вас уже изучена школа магии!"), true);
                return TypedActionResult.fail(stack);
            }

            // УСТАНАВЛИВАЕМ ШКОЛУ УЛУЧШЕНИЯ
            manaData.setEnhancerSchool(true);
            ManaManager.setServerData(serverPlayer, manaData);

            // === ВЫДАЧА ДОСТИЖЕНИЯ ===
            Identifier advancementId = new Identifier("firemagic", "learn_enhancer_magic");
            Advancement advancement = serverPlayer.getServer().getAdvancementLoader().get(advancementId);

            if (advancement != null) {
                AdvancementProgress progress = serverPlayer.getAdvancementTracker().getProgress(advancement);
                if (!progress.isDone()) {
                    serverPlayer.getAdvancementTracker().grantCriterion(advancement, "learned");
                    FireMagicMod.LOGGER.info("Достижение выдано игроку: " + serverPlayer.getName().getString());
                }
            } else {
                FireMagicMod.LOGGER.warn("Достижение learn_enhancer_magic не найдено!");
            }

            try {
                // СПИСОК РЕЦЕПТОВ ДЛЯ ВЫДАЧИ
                String[] recipes = {
                        "firemagic:chaos_sword",
                        "firemagic:shadow_step_sword"  // ДОБАВЛЕН МЕЧ ТЕЛЕПОРТАЦИИ
                };

                for (String recipe : recipes) {
                    serverPlayer.getServer().getCommandManager().executeWithPrefix(
                            serverPlayer.getCommandSource().withSilent(),
                            "recipe give " + serverPlayer.getName().getString() + " " + recipe
                    );
                }

            } catch (Exception e) {
                FireMagicMod.LOGGER.error("Ошибка при выдаче рецептов: " + e.getMessage());

                // Запасной способ
                serverPlayer.sendMessage(Text.literal("§eРецепты изучены автоматически"), true);
            }

            // === ВЫДАЧА ИНФОРМАЦИОННОЙ КНИГИ ===
            ItemStack infoBook = new ItemStack(FireMagicMod.ENHANCER_INFO_BOOK);

            // Добавляем NBT с информацией о владельце
            infoBook.getOrCreateNbt().putUuid("OwnerUUID", serverPlayer.getUuid());
            infoBook.getOrCreateNbt().putString("OwnerName", serverPlayer.getName().getString());

            // Даем игроку информационную книгу
            if (!serverPlayer.getInventory().insertStack(infoBook)) {
                serverPlayer.dropItem(infoBook, false);
            }

            serverPlayer.sendMessage(Text.literal("§aПолучена §6Книга рецептов улучшения§a!"), true);

            // УДАЛЕНИЕ КНИГИ СПЕЦИАЛИЗАЦИИ
            serverPlayer.setStackInHand(hand, ItemStack.EMPTY);
            return TypedActionResult.success(ItemStack.EMPTY);
        }

        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("§6§lКнига Мастера Улучшений").formatted(net.minecraft.util.Formatting.GOLD));
        tooltip.add(Text.literal("§7Позволяет улучшать оружие и броню").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§8◇ Доступные улучшения: Хаос, Телепортация").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        tooltip.add(Text.literal("§8◇ Используйте книгу рецептов для просмотра").formatted(net.minecraft.util.Formatting.DARK_GRAY));
    }
}