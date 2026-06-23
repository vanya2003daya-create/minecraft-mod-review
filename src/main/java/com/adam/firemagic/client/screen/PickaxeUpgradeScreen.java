package com.adam.firemagic.client.screen;

import com.adam.firemagic.*;
import com.adam.firemagic.upgrade.PickaxeResourcesData;
import com.adam.firemagic.upgrade.PickaxeResourcesManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

public class PickaxeUpgradeScreen extends Screen {
    private static final int WIDTH = 420;
    private static final int HEIGHT = 280;
    private static final Identifier PACKET_ADD = new Identifier(FireMagicMod.MOD_ID, "add_resources");
    private static final Identifier PACKET_EXTRACT = new Identifier(FireMagicMod.MOD_ID, "extract_resources");
    private static final Identifier PACKET_APPLY = new Identifier(FireMagicMod.MOD_ID, "apply_upgrades");

    private final PlayerEntity player;
    private final ItemStack pickaxeStack;
    private PickaxeResourcesData resourcesData;

    private int x, y;
    private final Map<String, ButtonWidget> addButtons = new HashMap<>();
    private final Map<String, ButtonWidget> extractButtons = new HashMap<>();

    public PickaxeUpgradeScreen(PlayerEntity player, ItemStack pickaxeStack) {
        super(Text.literal("Прокачка кирки"));
        this.player = player;
        this.pickaxeStack = pickaxeStack;
        this.resourcesData = PickaxeResourcesManager.getData(pickaxeStack.copy());
    }

    @Override
    protected void init() {
        super.init();
        this.x = (width - WIDTH) / 2;
        this.y = (height - HEIGHT) / 2;

        // Заголовок
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§6§lПРОКАЧКА КИРКИ"),
                button -> {}
        ).dimensions(x + WIDTH/2 - 100, y + 10, 200, 20).build());

        // Категории
        String[] categories = {"coal", "iron", "gold", "diamond", "emerald"};
        int startY = y + 40;
        int rowHeight = 40;

        for (int i = 0; i < categories.length; i++) {
            String category = categories[i];
            int yPos = startY + i * rowHeight;

            // Название категории
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(getCategoryName(category)),
                    button -> {}
            ).dimensions(x + 20, yPos, 100, 20).build());

            // Кнопка +16
            ButtonWidget addButton = ButtonWidget.builder(
                    Text.literal("+16"),
                    button -> sendAddPacket(category, 16)
            ).dimensions(x + 130, yPos, 50, 20).build();

            // === КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: ПОЛНАЯ БЛОКИРОВКА ПРИ МАКСИМУМЕ ===
            boolean isMaxed = resourcesData.getResource(category) >= getMax(category);
            ButtonWidget extractBtn = ButtonWidget.builder(
                    Text.literal("Извлечь"),
                    button -> sendExtractPacket(category)
            ).dimensions(x + 190, yPos, 70, 20).build();

            // Кнопка "Извлечь" недоступна, если категория заполнена до максимума
            extractBtn.active = !isMaxed && resourcesData.getResource(category) > 0;

            this.addDrawableChild(addButton);
            this.addDrawableChild(extractBtn);
            addButtons.put(category, addButton);
            extractButtons.put(category, extractBtn);
        }

        // Кнопка "Применить"
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§aПрименить"),
                button -> sendApplyPacket()
        ).dimensions(x + WIDTH/2 - 50, y + HEIGHT - 30, 100, 20).build());
    }

    private void sendAddPacket(String category, int amount) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(category);
        buf.writeInt(amount);
        ClientPlayNetworking.send(PACKET_ADD, buf);
    }

    private void sendExtractPacket(String category) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(category);
        ClientPlayNetworking.send(PACKET_EXTRACT, buf);
    }

    private void sendApplyPacket() {
        ClientPlayNetworking.send(PACKET_APPLY, PacketByteBufs.create());
        close();
    }

    public void updateData(PickaxeResourcesData newData) {
        this.resourcesData = newData;
        for (String category : addButtons.keySet()) {
            updateButtons(category);
        }
    }

    private void updateButtons(String category) {
        int current = resourcesData.getResource(category);
        int max = getMax(category);
        boolean isMaxed = (current >= max);

        addButtons.get(category).active = !isMaxed;
        // === ФИНАЛЬНОЕ ИЗМЕНЕНИЕ: ИЗВЛЕЧЕНИЕ ЗАПРЕЩЕНО ПРИ МАКСИМУМЕ ===
        extractButtons.get(category).active = (current > 0) && !isMaxed;
    }

    private int getMax(String category) {
        return switch (category) {
            case "coal" -> 128;
            case "iron" -> 128;
            case "gold" -> 64;
            case "diamond" -> 32;
            case "emerald" -> 16;
            default -> 0;
        };
    }

    private String getCategoryName(String category) {
        return switch (category) {
            case "coal" -> "Уголь";
            case "iron" -> "Железо";
            case "gold" -> "Золото";
            case "diamond" -> "Алмаз";
            case "emerald" -> "Изумруд";
            default -> "";
        };
    }

    private String getResourceName(String category) {
        return switch (category) {
            case "coal" -> "угля";
            case "iron" -> "железа";
            case "gold" -> "золота";
            case "diamond" -> "алмазов";
            case "emerald" -> "изумрудов";
            default -> "ресурсов";
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        // Основной фон
        context.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF3C3C3C);
        context.fill(x, y, x + WIDTH, y + 30, 0xFF222222);

        // Информация
        context.drawText(textRenderer,
                Text.literal("§7Эффекты применяются навсегда после нажатия 'Применить'"),
                x + 20, y + HEIGHT - 50, 0xFFFFFF, false);

        // Отображаем категории
        String[] categories = {"coal", "iron", "gold", "diamond", "emerald"};
        int startY = y + 40;
        int rowHeight = 40;

        for (int i = 0; i < categories.length; i++) {
            String category = categories[i];
            int value = resourcesData.getResource(category);
            int max = getMax(category);
            int yPos = startY + i * rowHeight;

            // Прогресс
            String progress = String.format("§f%d§7/§e%d", value, max);
            context.drawText(textRenderer, Text.literal(progress), x + 270, yPos + 5, 0xFFFFFF, false);

            // Эффекты
            String effect = getEffectText(category, value);
            int color = 0xFFFFFF;
            if (effect.contains("§a")) color = 0x55FF55;
            else if (effect.contains("§c")) color = 0xFF5555;
            else if (effect.contains("§b")) color = 0x55FFFF;
            else if (effect.contains("§6")) color = 0xFFAA00;

            context.drawText(textRenderer, Text.literal(effect), x + 270, yPos + 17, color, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String getEffectText(String category, int value) {
        return switch (category) {
            case "coal" -> value >= 128 ? "§aСпешка II" :
                    value >= 64 ? "§aСпешка I" : "§764 для Спешки I";
            case "iron" -> value >= 128 ? "§cСила I" : "§7128 для Силы I";
            case "diamond" -> value >= 32 ? "§bСопротивление II" :
                    value >= 16 ? "§bСопротивление I" : "§716 для Сопротивления I";
            case "gold" -> value >= 64 ? "§6Огнестойкость" : "§764 для Огнестойкости";
            case "emerald" -> value >= 16 ? "§aУдача I" : "§716 для Удачи I";
            default -> "";
        };
    }

    @Override
    public void renderBackground(DrawContext context) {
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

}