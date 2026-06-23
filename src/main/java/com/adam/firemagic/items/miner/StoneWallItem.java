package com.adam.firemagic.items.miner;

import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StoneWallItem extends Item {
    private static final int COOLDOWN_TICKS = 200; // 10 секунд
    private static final int WALL_DISTANCE = 4;
    private static final int WALL_HEIGHT = 5;
    private static final int WALL_WIDTH = 6;

    public StoneWallItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.pass(stack);
        }

        if (user instanceof ServerPlayerEntity serverPlayer) {
            PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

            // === ИСПОЛЬЗУЕМ manaData ДЛЯ ПРОВЕРКИ ===
            if (!manaData.hasMinerSchool()) {
                serverPlayer.setStackInHand(hand, ItemStack.EMPTY);
                serverPlayer.sendMessage(Text.literal("§cПредмет исчез! Нужно изучить школу шахтёра."), true);
                return TypedActionResult.fail(ItemStack.EMPTY);
            }

            // === ПРОДОЛЖАЕМ ИСПОЛЬЗОВАТЬ ТУ ЖЕ ПЕРЕМЕННУЮ ===
            if (serverPlayer.getItemCooldownManager().isCoolingDown(this)) {
                return TypedActionResult.pass(stack);
            }

            if (createStoneWall((ServerWorld) world, serverPlayer)) {
                serverPlayer.getItemCooldownManager().set(this, COOLDOWN_TICKS);
                world.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        net.minecraft.sound.SoundEvents.BLOCK_STONE_PLACE,
                        net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 0.8f);
                return TypedActionResult.success(stack);
            } else {
                serverPlayer.sendMessage(Text.literal("§cНе удалось создать стену!"), true);
                return TypedActionResult.fail(stack);
            }
        }

        return TypedActionResult.pass(stack);
    }

    private boolean createStoneWall(ServerWorld world, ServerPlayerEntity player) {
        // Получаем направление взгляда игрока
        Vec3d lookDir = player.getRotationVec(1.0F);

        // Определяем начальную позицию стены
        BlockPos startPos = BlockPos.ofFloored(
                player.getX() + lookDir.x * WALL_DISTANCE,
                player.getY() + 1, // На уровне глаз
                player.getZ() + lookDir.z * WALL_DISTANCE
        );

        // Определяем ориентацию стены
        Direction.Axis axis = getWallAxis(lookDir);

        // Получаем тип блока в зависимости от высоты
        BlockState wallBlock = getWallBlockByHeight(player);

        boolean wallCreated = false;
        int blocksPlaced = 0;

        // Создаём стену
        for (int y = 0; y < WALL_HEIGHT; y++) {
            for (int w = -WALL_WIDTH / 2; w <= WALL_WIDTH / 2; w++) {
                BlockPos pos;

                if (axis == Direction.Axis.X) {
                    // Стена вдоль оси X
                    pos = startPos.add(w, y, 0);
                } else {
                    // Стена вдоль оси Z
                    pos = startPos.add(0, y, w);
                }

                // Проверяем, можно ли разместить блок
                if (canPlaceBlock(world, pos)) {
                    // Ставим блок (тип зависит от высоты)
                    world.setBlockState(pos, wallBlock, 3);
                    blocksPlaced++;
                    wallCreated = true;
                }
            }
        }

        return wallCreated && blocksPlaced >= 5;
    }

    // ВЫБОР БЛОКА ПО ВЫСОТЕ
    private BlockState getWallBlockByHeight(PlayerEntity player) {
        int y = player.getBlockY();

        if (y < 40) { // Глубоко под землёй - гладкий камень
            return Blocks.SMOOTH_STONE.getDefaultState();
        } else if (y < 60) { // Средняя глубина - булыжник
            return Blocks.COBBLESTONE.getDefaultState();
        } else { // Поверхность - земля
            return Blocks.DIRT.getDefaultState();
        }
    }

    private Direction.Axis getWallAxis(Vec3d lookDir) {
        if (Math.abs(lookDir.z) > Math.abs(lookDir.x)) {
            return Direction.Axis.X;
        } else {
            return Direction.Axis.Z;
        }
    }

    private boolean canPlaceBlock(ServerWorld world, BlockPos pos) {
        net.minecraft.block.BlockState state = world.getBlockState(pos);
        return state.isReplaceable() &&
                !state.isOf(Blocks.WATER) &&
                !state.isOf(Blocks.LAVA) &&
                !state.isOf(Blocks.BEDROCK);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("§6Каменная стена").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("§7• Создаёт стену 5×6 блоков").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("§7• Блок зависит от глубины:").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("§8  Глубоко (Y<20): §fГладкий камень").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("§8  Средняя (20-59): §fБулыжник").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("§8  Поверхность (Y≥60): §fЗемля").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("§7• Перезарядка: §e10 секунд").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("§8Требует школу магии").formatted(Formatting.DARK_GRAY));
    }
}