package com.adam.firemagic.client.render.entity;

import com.adam.firemagic.entities.TeleportArrowEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class TeleportArrowRenderer extends EntityRenderer<TeleportArrowEntity> {
    private final ItemRenderer itemRenderer;
    private static final Identifier TEXTURE = new Identifier("textures/entity/projectiles/arrow.png");

    public TeleportArrowRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TeleportArrowEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {

        // Проверяем, не должна ли стрела быть удалена или уже удалена
        if (entity == null || entity.isRemoved()) {
            return;
        }

        // Проверяем скорость - если стрела почти остановилась, не рендерим
        double velocityLength = entity.getVelocity().length();
        if (velocityLength < 0.1) {
            return;
        }

        matrices.push();

        // Поворачиваем стрелу в направлении полета
        // Вместо getPitch() и getYaw() используем значения из стрелы
        float pitch = entity.getPitch();
        float yawDegrees = entity.getYaw();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDegrees - 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pitch));

        // Добавляем вращение для визуализации полета телепортирующей стрелы
        float rotationSpeed = 15.0f; // Скорость вращения
        float rotationAngle = (entity.age + tickDelta) * rotationSpeed;
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationAngle));

        // Масштабируем
        float scale = 0.5F;
        matrices.scale(scale, scale, scale);

        // Создаем ItemStack стрелы
        ItemStack arrowStack = new ItemStack(Items.ARROW);

        // Рендерим модель стрелы
        itemRenderer.renderItem(arrowStack, ModelTransformationMode.GROUND,
                light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers,
                entity.getWorld(), 0);

        matrices.pop();

        // Вызываем родительский метод для тени и т.д.
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(TeleportArrowEntity entity) {
        return TEXTURE;
    }
}