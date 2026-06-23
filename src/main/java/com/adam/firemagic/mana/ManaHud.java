package com.adam.firemagic.mana;

import com.adam.firemagic.FireMagicMod;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class ManaHud implements HudRenderCallback {
    public static final Identifier MANA_FULL = new Identifier(FireMagicMod.MOD_ID, "textures/gui/mana_full.png");
    public static final Identifier MANA_EMPTY = new Identifier(FireMagicMod.MOD_ID, "textures/gui/mana_empty.png");

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.options.hudHidden) return;
        if (client.player.isCreative() || client.player.isSpectator()) return;

        PlayerManaData data = ManaManager.getClientData();
        int mana = data.getMana();
        int maxMana = data.getMaxMana();

        // НОВАЯ ПОЗИЦИЯ: между голодом и сердцами
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // Центрируем по горизонтали
        int x = screenWidth / 2 - 9 * 5; // Центр минус половина ширины (10 сердец по 9 пикселей)

        // Позиция: выше голода, но ниже сердец
        int y = screenHeight - 49 - 20; // На 20 пикселей выше полоски голода

        // Альтернативная позиция (раскомментируйте если нужно):
        // int y = screenHeight - 39 - 30; // Еще выше, между сердцами и голодом

        int perRow = 10;
        for (int i = 0; i < maxMana; i++) {
            int row = i / perRow;
            int col = i % perRow;
            int xPos = x + col * 9;
            int yPos = y - row * 9; // Смещаем вверх для новых рядов

            Identifier texture = (i < mana) ? MANA_FULL : MANA_EMPTY;
            context.drawTexture(texture, xPos, yPos, 0, 0, 8, 8, 8, 8);
        }
    }
}