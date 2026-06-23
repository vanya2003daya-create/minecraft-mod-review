package com.adam.firemagic.client.render.entity;

import com.adam.firemagic.FireMagicMod;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.SkeletonEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

public class NecroSkeletonRenderer extends SkeletonEntityRenderer {
    private static final Identifier TEXTURE = new Identifier(FireMagicMod.MOD_ID, "textures/entity/necro_skeleton.png");

    public NecroSkeletonRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    public Identifier getTexture(net.minecraft.entity.mob.SkeletonEntity entity) {
        return TEXTURE;
    }

    public void render(SkeletonEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // 🔥 ПРЯМО В РЕНДЕРЕРЕ: не рендерим имя вообще
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}