package com.adam.firemagic;

import com.adam.firemagic.blocks.ModBlocks;
import com.adam.firemagic.client.gui.EnhancerBookScreen;
import com.adam.firemagic.client.render.entity.*;
import com.adam.firemagic.entities.ModEntities;
import com.adam.firemagic.items.enhanced.EnhancerRecipeManager;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import com.adam.firemagic.network.ClientPacketHandler;
import com.adam.firemagic.network.ManaSyncPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public class FireMagicModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Регистрация прозрачных блоков
        BlockRenderLayerMap.INSTANCE.putBlock(
                ModBlocks.ETERNAL_FIRE,
                RenderLayer.getCutout()
        );
        ClientPacketHandler.register();

        System.out.println("[DEBUG] FireMagicModClient инициализирован!");

        // ========== Регистрация рендерера для пронзающей стрелы ==========
        EntityRendererRegistry.register(ModEntities.PIERCING_ARROW, PiercingArrowRenderer::new);
        System.out.println("[DEBUG] Рендерер для пронзающей стрелы зарегистрирован!");

        // Регистрация рендерера для огненного шара
        EntityRendererRegistry.register(ModEntities.CUSTOM_FIREBALL, CustomFireballRenderer::new);
        System.out.println("[DEBUG] CustomFireballRenderer зарегистрирован!");

        EntityRendererRegistry.register(ModEntities.TELEPORT_ARROW, TeleportArrowRenderer::new);
        System.out.println("[DEBUG] TeleportArrowRenderer зарегистрирован!");
        EntityRendererRegistry.register(ModEntities.NECRO_ZOMBIE, NecroZombieRenderer::new);
        EntityRendererRegistry.register(ModEntities.NECRO_SKELETON, NecroSkeletonRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(
                FireMagicMod.id("open_enhancer_book"),
                (client, handler, buf, responseSender) -> {
                    // Открываем GUI в основном потоке игры
                    client.execute(() -> {
                        if (client.player != null) {
                            MinecraftClient.getInstance().setScreen(new EnhancerBookScreen());
                        }
                    });
                }
        );
        EnhancerRecipeManager.init();

        // Регистрация предикатов для лука (анимация натяжения)
        registerBowPredicates();

        // Регистрация сетевых обработчиков
        registerNetworkHandlers();
    }

    private void registerBowPredicates() {
        // Предикат "pull" (сила натяжения лука)
        ModelPredicateProviderRegistry.register(
                FireMagicMod.ARCHER_BOW,
                new Identifier("pull"),
                (stack, world, entity, seed) -> {
                    if (entity == null || entity.getActiveItem() != stack) {
                        return 0.0F;
                    }

                    // Расчёт силы натяжения (0.0 до 1.0)
                    int useTicks = stack.getMaxUseTime() - entity.getItemUseTimeLeft();
                    float pull = useTicks / 20.0F;
                    return Math.min(pull, 1.0F);
                }
        );

        // Предикат "pulling" (идёт ли натяжение)
        ModelPredicateProviderRegistry.register(
                FireMagicMod.ARCHER_BOW,
                new Identifier("pulling"),
                (stack, world, entity, seed) ->
                        entity != null && entity.getActiveItem() == stack ? 1.0F : 0.0F
        );

        System.out.println("[DEBUG] Предикаты для лука зарегистрированы!");
    }

    private void registerNetworkHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(
                ManaSyncPacket.ID,
                (client, handler, buf, responseSender) -> {
                    int mana = buf.readInt();
                    int maxMana = buf.readInt();
                    boolean hasMinerSchool = buf.readBoolean();
                    boolean hasArcherSchool = buf.readBoolean();

                    client.execute(() -> {
                        PlayerManaData data = new PlayerManaData();
                        data.setMana(mana);
                        data.setMaxMana(maxMana);
                        data.setMinerSchool(hasMinerSchool);
                        data.setArcherSchool(hasArcherSchool);

                        ManaManager.setClientData(data);
                        System.out.println("[DEBUG] Мана синхронизирована: " + mana + "/" + maxMana +
                                ", шахтёр: " + hasMinerSchool + ", лучник: " + hasArcherSchool);
                    });
                }
        );
    }
}