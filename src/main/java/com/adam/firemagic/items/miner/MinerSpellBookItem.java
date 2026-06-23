package com.adam.firemagic.items.miner;

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

public class MinerSpellBookItem extends Item {
    public MinerSpellBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayerEntity player = (ServerPlayerEntity) user;
            PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

            // === ОБЩАЯ ПРОВЕРКА: ЕСТЬ ЛИ УЖЕ КАКАЯ-ТО ШКОЛА ===
            if (manaData.hasMagicSchool()) {
                serverPlayer.sendMessage(Text.literal("§cУ вас уже изучена школа магии!"), true);
                return TypedActionResult.fail(stack);
            }

            // === УСТАНОВКА КОНКРЕТНОЙ ШКОЛЫ ШАХТЁРА ===
            manaData.setMinerSchool(true);
            ManaManager.setServerData(serverPlayer, manaData);

            // Даём достижение
            Advancement advancement = player.getServer().getAdvancementLoader().get(
                    new Identifier("firemagic", "learn_miner_magic")
            );
            if (advancement != null) {
                player.getAdvancementTracker().grantCriterion(advancement, "learned_magic");
                System.out.println("[DEBUG] Выдано достижение " + player.getName().getString());
                player.getServer().getCommandManager().executeWithPrefix(
                        player.getCommandSource().withSilent(),
                        "recipe give @s firemagic:stone_wall_spell"
                );
                player.getServer().getCommandManager().executeWithPrefix(
                        player.getCommandSource().withSilent(),
                        "recipe give @s firemagic:stone_dash_spell"
                );
            }



            // Даём кирку, если её нет
            boolean hasPick = false;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack itemStack = player.getInventory().getStack(i);
                if (itemStack.getItem() == FireMagicMod.MINER_PICK) {
                    hasPick = true;
                    break;
                }
            }

            if (!hasPick) {
                player.giveItemStack(new ItemStack(FireMagicMod.MINER_PICK));
                System.out.println("[DEBUG] Выдана кирка " + player.getName().getString());
            }

            // Удаляем книгу (если не в креативе)
            if (!player.isCreative()) {
                stack.decrement(1);
            }

            player.sendMessage(Text.literal("§eВы освоили §6Мастерство шахтёра§e!"), true);
            player.sendMessage(Text.literal("§7Вы получили кирку «Рудокоп» и доступ к новым заклинаниям"), true);
            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }
}