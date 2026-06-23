package com.adam.firemagic.client.render.entity;  // ← ВАЖНО: правильный пакет!

import com.adam.firemagic.entities.CustomFireballEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class CustomFireballRenderer extends EntityRenderer<CustomFireballEntity> {
    private static final Identifier TEXTURE = new Identifier("firemagic", "textures/entity/fireball.png");

    public CustomFireballRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(CustomFireballEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // Масштабируем файрболл
        matrices.scale(1.0f, 1.0f, 1.0f);

        // Поворачиваем к камере (биллборд)
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));

        // Получаем вершины для рендеринга
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(this.getTexture(entity)));

        // Рисуем квадрат с текстурой
        MatrixStack.Entry entry = matrices.peek();
        var positionMatrix = entry.getPositionMatrix();
        var normalMatrix = entry.getNormalMatrix();

        // Верхний левый угол
        vertexConsumer.vertex(positionMatrix, -0.5f, -0.5f, 0.0f)
                .color(255, 255, 255, 255)
                .texture(0.0f, 1.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .next();

        // Верхний правый угол
        vertexConsumer.vertex(positionMatrix, 0.5f, -0.5f, 0.0f)
                .color(255, 255, 255, 255)
                .texture(1.0f, 1.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .next();

        // Нижний правый угол
        vertexConsumer.vertex(positionMatrix, 0.5f, 0.5f, 0.0f)
                .color(255, 255, 255, 255)
                .texture(1.0f, 0.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .next();

        // Нижний левый угол
        vertexConsumer.vertex(positionMatrix, -0.5f, 0.5f, 0.0f)
                .color(255, 255, 255, 255)
                .texture(0.0f, 0.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .next();

        matrices.pop();
    }

    @Override
    public Identifier getTexture(CustomFireballEntity entity) {
        return TEXTURE;
    }
}