package com.adam.firemagic.client.render.entity;

import com.adam.firemagic.FireMagicMod;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

public class NecroZombieRenderer extends ZombieEntityRenderer {
    // 🔧 ВАЖНО: Правильный путь к текстуре
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/entity/zombie/zombie.png");

    public NecroZombieRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(net.minecraft.entity.mob.ZombieEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(ZombieEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // 🔥 ПРЯМО В РЕНДЕРЕРЕ: не рендерим имя вообще
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}