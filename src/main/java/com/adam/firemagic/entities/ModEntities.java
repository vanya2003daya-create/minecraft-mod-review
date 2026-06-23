package com.adam.firemagic.entities;

import com.adam.firemagic.FireMagicMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModEntities {
    // Сущность кастомного файербола (у вас уже есть)
    public static final EntityType<CustomFireballEntity> CUSTOM_FIREBALL =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(FireMagicMod.MOD_ID, "custom_fireball"),
                    FabricEntityTypeBuilder.<CustomFireballEntity>create(
                                    SpawnGroup.MISC,
                                    CustomFireballEntity::new
                            )
                            .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
                            .trackRangeBlocks(32)  // Оптимизация для сети
                            .trackedUpdateRate(10) // Реже обновляем позицию в сети
                            .build()
            );

    // Сущность пронзающей стрелы
    public static final EntityType<PiercingArrowEntity> PIERCING_ARROW = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(FireMagicMod.MOD_ID, "piercing_arrow"),
            FabricEntityTypeBuilder.<PiercingArrowEntity>create(SpawnGroup.MISC, PiercingArrowEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeBlocks(128)
                    .trackedUpdateRate(2)
                    .build()
    );

    // Сущность телепортирующей стрелы
    public static final EntityType<TeleportArrowEntity> TELEPORT_ARROW =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(FireMagicMod.MOD_ID, "teleport_arrow"),
                    FabricEntityTypeBuilder.<TeleportArrowEntity>create(SpawnGroup.MISC, TeleportArrowEntity::new)
                            .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
                            .trackRangeBlocks(4)
                            .trackedUpdateRate(20)
                            .build()
            );

    // 🔧 ИСПРАВЛЕНИЕ: Используем лямбда-выражение для конструктора
    public static final EntityType<NecroZombieEntity> NECRO_ZOMBIE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(FireMagicMod.MOD_ID, "necro_zombie"),
            FabricEntityTypeBuilder.<NecroZombieEntity>create(SpawnGroup.MONSTER,
                            (EntityType<NecroZombieEntity> entityType, World world) -> new NecroZombieEntity(entityType, world))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .trackRangeBlocks(64)           // 🔧 УВЕЛИЧИТЬ С 8 ДО 64
                    .trackedUpdateRate(3)           // 🔧 Чаще обновлять
                    .forceTrackedVelocityUpdates(true) // 🔧 Важно для отслеживания
                    .build()
    );
    public static final EntityType<NecroSkeletonEntity> NECRO_SKELETON = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(FireMagicMod.MOD_ID, "necro_skeleton"),
            FabricEntityTypeBuilder.<NecroSkeletonEntity>create(SpawnGroup.MONSTER,
                            (EntityType<NecroSkeletonEntity> entityType, World world) -> new NecroSkeletonEntity(entityType, world))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(3)
                    .forceTrackedVelocityUpdates(true)
                    .build()
    );

    public static void init() {
        FireMagicMod.LOGGER.info("Custom fireball entity registered!)");
        FireMagicMod.LOGGER.info("Piercing arrow entity registered!");
        FireMagicMod.LOGGER.info("Teleport arrow entity registered!");
        FireMagicMod.LOGGER.info("Necro zombie entity registered!"); // 🔧 ДОБАВЛЕНО
    }

    // 🔧 ДОБАВЛЕНО: Метод для регистрации атрибутов (вызывается отдельно)
    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(NECRO_ZOMBIE, NecroZombieEntity.createNecroZombieAttributes());
        FabricDefaultAttributeRegistry.register(NECRO_SKELETON, NecroSkeletonEntity.createNecroSkeletonAttributes());
    }
}