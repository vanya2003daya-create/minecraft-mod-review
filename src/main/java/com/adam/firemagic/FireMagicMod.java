package com.adam.firemagic;

import com.adam.firemagic.blocks.ModBlocks;
import com.adam.firemagic.effects.RingEffectHandler;
import com.adam.firemagic.entities.ModEntities;
import com.adam.firemagic.events.NecromancerCurseHandler;
import com.adam.firemagic.items.archer.*;
import com.adam.firemagic.items.enhanced.*;
import com.adam.firemagic.items.miner.MinerPickaxeItem;
import com.adam.firemagic.items.miner.MinerSpellBookItem;
import com.adam.firemagic.items.miner.StoneDashItem;
import com.adam.firemagic.items.miner.StoneWallItem;
import com.adam.firemagic.items.necromancer.NecromancerSpellBookItem;
import com.adam.firemagic.items.necromancer.NecromancerStaffItem;
import com.adam.firemagic.items.necromancer.SkeletonMinionEggItem;
import com.adam.firemagic.items.necromancer.ZombieMinionEggItem;
import com.adam.firemagic.mana.ManaHud;
import com.adam.firemagic.mana.ManaTickHandler;
import com.adam.firemagic.items.*;
import com.adam.firemagic.events.PlayerConnectionHandler;
import com.adam.firemagic.events.PlayerDeathHandler;
import com.adam.firemagic.network.ClientPacketHandler;
import com.adam.firemagic.network.ServerPacketHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adam.firemagic.effects.EnhancerArmorHandler;


public class FireMagicMod implements ModInitializer {
    public static final String MOD_ID = "firemagic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // === Предметы школы огня ===
    public static final Item FIREBALL = new FireballItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    public static final Item FIRE_DASH_ITEM = new FireDashItem(new Item.Settings().maxCount(1).fireproof());
    public static final Item FIRE_SWORD = new FireSwordItem(ToolMaterials.IRON, 4, -2.4f, new Item.Settings().maxCount(1).maxDamage(1500).fireproof().rarity(Rarity.EPIC));

    // === Предметы школы шахтёра ===
    public static final Item MINER_PICK = new MinerPickaxeItem(ToolMaterials.NETHERITE, 7, -2.8f, new Item.Settings().maxCount(1).fireproof().rarity(Rarity.UNCOMMON));
    public static final Item MINER_SPELL_BOOK = new MinerSpellBookItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof());
    public static final Item STONE_WALL = new StoneWallItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    public static final Item STONE_DASH = new StoneDashItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));

    // === Предметы школы лучника ===
    public static final Item ARCHER_SPELL_BOOK = new ArcherSpellBookItem(new Item.Settings().maxCount(1));
    public static final Item ARCHER_BOW = new ArcherBowItem(new Item.Settings().maxDamage(384));
    public static final Item EXPLOSIVE_ARROW = new ExplosiveArrowItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final Item PIERCING_ARROW = new PiercingArrowItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final Item TELEPORT_ARROW = new TeleportArrowItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final Item ARCHER_RING = new ArcherRingItem(new Item.Settings().rarity(Rarity.RARE));

    // === Предметы школы некроманта ===
    public static final Item NECROMANCER_SPELL_BOOK = new NecromancerSpellBookItem(new Item.Settings().maxCount(1));
    public static final Item ZOMBIE_MINION_EGG = new ZombieMinionEggItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    public static final Item NECROMANCER_STAFF = new NecromancerStaffItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final Item SKELETON_MINION_EGG = new SkeletonMinionEggItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));

    // === ПРЕДМЕТЫ ШКОЛЫ УЛУЧШЕНИЯ (ДОБАВЛЕНО) ===
    public static final Item ENHANCER_SPELL_BOOK = new EnhancerSpellBookItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    // 🔴 ИСПРАВЛЕНО: Используем EnhancerInfoBookItem вместо EnchantedBookItem
    public static final Item ENHANCER_INFO_BOOK = new EnhancerInfoBookItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    public static final Item CHAOS_SWORD = new ChaosSwordItem();
    public static final Item SHADOW_STEP_SWORD = new ShadowStepSwordItem();

    public static final Item HUNTER_HELM = new HunterHelmItem();

    @Override
    public void onInitialize() {
        // Регистрация предметов
        registerItems();
        NecromancerCurseHandler.register();
        EnhancerRecipeManager.init();
        EnhancerRecipeManager.init();
        // Регистрация в табах
        registerItemGroups();
        ModEntities.registerAttributes();

        // Остальные компоненты
        registerModComponents();

        // Сетевые пакеты
        ServerPacketHandler.register();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPacketHandler.register();
        }

        // Обработчик смерти
        ServerPlayerEvents.AFTER_RESPAWN.register(new PlayerDeathHandler());

        LOGGER.info("Fire Magic Mod initialized with all schools including Enhancer!");
    }

    private void registerItems() {
        // Школа огня
        Registry.register(Registries.ITEM, id("fireball"), FIREBALL);
        Registry.register(Registries.ITEM, id("fire_dash"), FIRE_DASH_ITEM);
        Registry.register(Registries.ITEM, id("fire_sword"), FIRE_SWORD);

        // Школа шахтёра
        Registry.register(Registries.ITEM, id("miner_pick"), MINER_PICK);
        Registry.register(Registries.ITEM, id("miner_spell_book"), MINER_SPELL_BOOK);
        Registry.register(Registries.ITEM, id("stone_wall"), STONE_WALL);
        Registry.register(Registries.ITEM, id("stone_dash"), STONE_DASH);

        // Школа лучника
        Registry.register(Registries.ITEM, id("archer_spell_book"), ARCHER_SPELL_BOOK);
        Registry.register(Registries.ITEM, id("archer_bow"), ARCHER_BOW);
        Registry.register(Registries.ITEM, id("explosive_arrow"), EXPLOSIVE_ARROW);
        Registry.register(Registries.ITEM, id("piercing_arrow"), PIERCING_ARROW);
        Registry.register(Registries.ITEM, id("teleport_arrow"), TELEPORT_ARROW);
        Registry.register(Registries.ITEM, id("archer_ring"), ARCHER_RING);

        // Школа некроманта
        Registry.register(Registries.ITEM, id("necromancer_spell_book"), NECROMANCER_SPELL_BOOK);
        Registry.register(Registries.ITEM, id("zombie_minion_egg"), ZOMBIE_MINION_EGG);
        Registry.register(Registries.ITEM, id("necromancer_staff"), NECROMANCER_STAFF);
        Registry.register(Registries.ITEM, id("skeleton_minion_egg"), SKELETON_MINION_EGG);

        // ✅ ШКОЛА УЛУЧШЕНИЯ (ДОБАВЛЕНО)
        Registry.register(Registries.ITEM, id("enhancer_spell_book"), ENHANCER_SPELL_BOOK);
        Registry.register(Registries.ITEM, id("enhancer_info_book"), ENHANCER_INFO_BOOK);
        Registry.register(Registries.ITEM, id("chaos_sword"), CHAOS_SWORD);
        Registry.register(Registries.ITEM, id("shadow_step_sword"), SHADOW_STEP_SWORD);

        Registry.register(Registries.ITEM, id("hunter_helm"), HUNTER_HELM);
    }

    private void registerItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            // Школа огня
            entries.add(FIREBALL);
            entries.add(FIRE_DASH_ITEM);
            entries.add(FIRE_SWORD);

            // Школа шахтёра
            entries.add(MINER_PICK);
            entries.add(MINER_SPELL_BOOK);
            entries.add(STONE_DASH);
            entries.add(STONE_WALL);

            // Школа лучника
            entries.add(ARCHER_SPELL_BOOK);
            entries.add(ARCHER_BOW);
            entries.add(EXPLOSIVE_ARROW);
            entries.add(PIERCING_ARROW);
            entries.add(TELEPORT_ARROW);
            entries.add(ARCHER_RING);

            // Школа некроманта
            entries.add(NECROMANCER_SPELL_BOOK);
            entries.add(NECROMANCER_STAFF);
            entries.add(ZOMBIE_MINION_EGG);
            entries.add(SKELETON_MINION_EGG);

            // ✅ ШКОЛА УЛУЧШЕНИЯ (ДОБАВЛЕНО)
            entries.add(ENHANCER_SPELL_BOOK);
            entries.add(ENHANCER_INFO_BOOK);
            entries.add(CHAOS_SWORD);
            entries.add(SHADOW_STEP_SWORD);
            entries.add(HUNTER_HELM);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(MINER_PICK);
            entries.add(MINER_SPELL_BOOK);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(EXPLOSIVE_ARROW);
            entries.add(PIERCING_ARROW);
            entries.add(TELEPORT_ARROW);
        });
    }

    private void registerModComponents() {
        ModBlocks.register();
        ModEntities.init();
        ModEntities.registerAttributes();
        PlayerConnectionHandler.register();
        ManaTickHandler.register();
        RingEffectHandler.register();
        EnhancerArmorHandler.register();

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            HudRenderCallback.EVENT.register(new ManaHud());

        }
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}