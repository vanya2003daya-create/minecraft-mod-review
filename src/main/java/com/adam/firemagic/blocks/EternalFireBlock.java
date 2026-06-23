package com.adam.firemagic.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class EternalFireBlock extends FireBlock {
    private static final int MAX_AGE = 600; // 30 секунд (20 тиков * 30)
    private int age = 0;

    public EternalFireBlock(Settings settings) {
        super(settings);
    }

    // Запретить естественное затухание
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        age++;
        if (age > MAX_AGE) {
            world.removeBlock(pos, false); // Самоуничтожение через 30 секунд
            return;
        }

        // Иногда тухнем (шанс 1% каждый тик)
        if (random.nextInt(100) == 0) {
            world.removeBlock(pos, false);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Предотвратить затухание от воды/дождя
    }

    public boolean canSurvive(BlockState state, WorldView world, BlockPos pos) {
        // Всегда выживает, кроме случаев ручного удаления
        return true;
    }

    public boolean isAffectedByRain() {
        return false; // Неуязвим к дождю
    }

    // Разрешить только ручное тушение
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        // Ограничиваем распространение только соседними блоками
        if (world instanceof ServerWorld) {
            BlockPos downPos = pos.down();
            if (!world.getBlockState(downPos).isSolidBlock(world, downPos)) {
                world.removeBlock(pos, false);
            }
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        // Игнорировать обновления соседей, которые потушили бы обычный огонь
    }

    // Сохранить механику ручного тушения

    public boolean tryFillWithFluid(World world, BlockPos pos, BlockState state, FluidState fluidState) {
        // Разрешить тушение ведром воды
        return fluidState.getFluid() == Fluids.WATER;
    }
}