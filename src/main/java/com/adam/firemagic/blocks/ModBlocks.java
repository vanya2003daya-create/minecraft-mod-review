package com.adam.firemagic.blocks;

import com.adam.firemagic.FireMagicMod;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final EternalFireBlock ETERNAL_FIRE = new EternalFireBlock(
            FabricBlockSettings.copyOf(Blocks.FIRE)
                    .luminance(state -> 15 - state.get(EternalFireBlock.AGE))
                    .ticksRandomly()
                    .sounds(BlockSoundGroup.WOOL) // Тише, чем обычный огонь
                    .replaceable()
                    .noCollision()
                    .breakInstantly()
    );

    public static void register() {
        Registry.register(Registries.BLOCK,
                new Identifier(FireMagicMod.MOD_ID, "eternal_fire"),
                ETERNAL_FIRE
        );

        // Регистрация ItemBlock для творческого инвентаря (опционально)
        Registry.register(Registries.ITEM,
                new Identifier(FireMagicMod.MOD_ID, "eternal_fire"),
                new BlockItem(ETERNAL_FIRE, new Item.Settings())
        );
    }
}