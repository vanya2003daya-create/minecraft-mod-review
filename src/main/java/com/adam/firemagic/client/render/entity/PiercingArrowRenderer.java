package com.adam.firemagic.client.render.entity;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.entity.Entity;

public class PiercingArrowRenderer extends EntityRenderer<Entity> {

    public PiercingArrowRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(Entity entity) {
        // Стандартная текстура стрелы
        return new Identifier("textures/entity/projectiles/arrow.png");
    }

    @Override
    public void render(Entity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // НИЧЕГО НЕ ДЕЛАЕМ - стрела будет невидимой, но рендерер существует
        // Это нормально для тестирования логики
    }
}