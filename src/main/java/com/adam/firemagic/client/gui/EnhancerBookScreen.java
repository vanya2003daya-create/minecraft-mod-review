package com.adam.firemagic.client.gui;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.items.enhanced.EnhancerRecipeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnhancerBookScreen extends Screen {
    // Размеры GUI
    private static final int GUI_WIDTH = 350;
    private static final int GUI_HEIGHT = 240;
    private int x, y;

    // Константы для отрисовки
    private static final int SLOT_SIZE = 20;
    private static final int RECIPES_PER_ROW = 2;
    private static final int RECIPES_PER_COLUMN = 3;
    private static final int RECIPES_PER_PAGE = RECIPES_PER_ROW * RECIPES_PER_COLUMN;

    private enum Tab {
        ARMOR(0, Text.literal("🛡️ БРОНЯ"), 0xFF4A90E2),
        SWORDS(1, Text.literal("⚔️ МЕЧИ"), 0xFFE24A4A),
        BOWS(2, Text.literal("🏹 ЛУКИ"), 0xFF4AE24A);

        final int index;
        final Text title;
        final int color;

        Tab(int index, Text title, int color) {
            this.index = index;
            this.title = title;
            this.color = color;
        }
    }

    private Tab currentTab = Tab.ARMOR;
    private List<RecipeEntry> currentRecipes = new ArrayList<>();
    private List<RecipeEntry> filteredRecipes = new ArrayList<>();
    private int scrollOffset = 0;
    private TextFieldWidget searchField;
    private String searchText = "";

    public EnhancerBookScreen() {
        super(Text.literal("📘 Книга Улучшений"));
    }

    @Override
    protected void init() {
        super.init();

        // Центрируем GUI
        this.x = (this.width - GUI_WIDTH) / 2;
        this.y = (this.height - GUI_HEIGHT) / 2;

        // Поле поиска
        this.searchField = new TextFieldWidget(
                this.textRenderer,
                x + 20, y + 20, GUI_WIDTH - 40, 20,
                Text.literal("Поиск...")
        );
        this.searchField.setChangedListener(text -> {
            this.searchText = text.toLowerCase();
            updateRecipes();
        });
        this.addDrawableChild(this.searchField);

        // Кнопки вкладок
        int tabY = y + 50;
        for (Tab tab : Tab.values()) {
            ButtonWidget tabButton = ButtonWidget.builder(
                            tab.title,
                            button -> {
                                this.currentTab = tab;
                                this.scrollOffset = 0;
                                updateRecipes();
                            }
                    )
                    .dimensions(x + 20 + tab.index * 100, tabY, 90, 24)
                    .build();

            this.addDrawableChild(tabButton);
        }

        // Кнопка прокрутки вверх
        ButtonWidget upButton = ButtonWidget.builder(
                        Text.literal("↑"),
                        button -> scrollUp()
                )
                .dimensions(x + GUI_WIDTH - 30, y + 90, 20, 20)
                .build();
        this.addDrawableChild(upButton);

        // Кнопка прокрутки вниз
        ButtonWidget downButton = ButtonWidget.builder(
                        Text.literal("↓"),
                        button -> scrollDown()
                )
                .dimensions(x + GUI_WIDTH - 30, y + GUI_HEIGHT - 30, 20, 20)
                .build();
        this.addDrawableChild(downButton);

        // Кнопка закрытия
        ButtonWidget closeButton = ButtonWidget.builder(
                        Text.literal("✕"),
                        button -> this.close()
                )
                .dimensions(x + GUI_WIDTH - 25, y + 5, 20, 20)
                .build();
        this.addDrawableChild(closeButton);

        updateRecipes();
    }

    private void updateRecipes() {
        currentRecipes.clear();
        switch (currentTab) {
            case ARMOR:
                currentRecipes.addAll(EnhancerRecipeManager.getArmorRecipes());
                break;
            case SWORDS:
                currentRecipes.addAll(EnhancerRecipeManager.getSwordRecipes());
                break;
            case BOWS:
                currentRecipes.addAll(EnhancerRecipeManager.getBowRecipes());
                break;
        }

        // Применяем фильтр поиска
        filteredRecipes = new ArrayList<>(currentRecipes);
    }

    private void scrollUp() {
        if (scrollOffset > 0) scrollOffset--;
    }

    private void scrollDown() {
        int maxPages = (filteredRecipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE;
        int currentPage = scrollOffset / RECIPES_PER_PAGE;
        if (currentPage < maxPages - 1) {
            scrollOffset = Math.min(scrollOffset + RECIPES_PER_PAGE, filteredRecipes.size() - RECIPES_PER_PAGE);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Темный полупрозрачный фон
        renderBackground(context);

        // Основной фон GUI
        context.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFF2C2C2C);
        context.fill(x + 1, y + 1, x + GUI_WIDTH - 1, y + GUI_HEIGHT - 1, 0xFF3C3C3C);

        // Заголовок
        context.drawText(
                textRenderer,
                "📘 КНИГА УЛУЧШЕНИЙ",
                x + GUI_WIDTH / 2 - textRenderer.getWidth("📘 КНИГА УЛУЧШЕНИЙ") / 2,
                y + 8,
                0xFFE6CC80,
                false
        );

        // Подсветка активной вкладки
        context.fill(
                x + 20 + currentTab.index * 100, y + 74,
                x + 110 + currentTab.index * 100, y + 76,
                currentTab.color
        );

        // Сетка рецептов
        int startX = x + 20;
        int startY = y + 90;
        int recipeWidth = 150;
        int recipeHeight = 40;

        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int recipeIndex = scrollOffset + i;
            if (recipeIndex >= filteredRecipes.size()) break;

            int row = i / RECIPES_PER_ROW;
            int col = i % RECIPES_PER_ROW;

            int recipeX = startX + col * (recipeWidth + 20);
            int recipeY = startY + row * (recipeHeight + 15);

            renderRecipe(context, filteredRecipes.get(recipeIndex), recipeX, recipeY, mouseX, mouseY);
        }

        // Информация о количестве рецептов
        String infoText = String.format("Рецепты: %d/%d", filteredRecipes.size(), currentRecipes.size());
        context.drawText(
                textRenderer,
                infoText,
                x + 20,
                y + GUI_HEIGHT - 25,
                0xAAAAAA,
                false
        );

        // Постраничная навигация
        if (filteredRecipes.size() > RECIPES_PER_PAGE) {
            int currentPage = (scrollOffset / RECIPES_PER_PAGE) + 1;
            int totalPages = (filteredRecipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE;
            String pageText = String.format("Страница %d/%d", currentPage, totalPages);
            context.drawText(
                    textRenderer,
                    pageText,
                    x + GUI_WIDTH - textRenderer.getWidth(pageText) - 60,
                    y + GUI_HEIGHT - 25,
                    0xAAAAAA,
                    false
            );
        }

        // Рендер всех виджетов
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderRecipe(DrawContext context, RecipeEntry recipe, int x, int y, int mouseX, int mouseY) {
        int width = 150;
        boolean hasSecondResource = !recipe.resource2.isEmpty();

        // Фон рецепта
        context.fill(x, y, x + width, y + 40, 0xFF4A4A4A);
        context.fill(x + 1, y + 1, x + width - 1, y + 39, 0xFF555555);

        // Входной предмет (Алмазный меч)
        int currentX = x + 5;
        context.drawItem(recipe.input, currentX, y + 10);

        // Плюсик
        currentX += 25;
        context.drawText(textRenderer, "+", currentX, y + 18, 0xFFFFFF, false);

        // Первый ресурс
        currentX += 15;
        context.drawItem(recipe.resource1, currentX, y + 10);

        // Отображаем количество первого ресурса
        if (recipe.resource1.getCount() > 1) {
            context.drawText(textRenderer, String.valueOf(recipe.resource1.getCount()),
                    currentX, y + 10, 0xFFFFFF, false);
        }

        // Если есть второй ресурс
        if (hasSecondResource) {
            // Второй плюсик
            currentX += 25;
            context.drawText(textRenderer, "+", currentX, y + 18, 0xFFFFFF, false);

            // Второй ресурс
            currentX += 15;
            context.drawItem(recipe.resource2, currentX, y + 10);

            // Отображаем количество второго ресурса
            if (recipe.resource2.getCount() > 1) {
                context.drawText(textRenderer, String.valueOf(recipe.resource2.getCount()),
                        currentX, y + 10, 0xFFFFFF, false);
            }
        }

        // Стрелочка
        currentX += 25;
        context.drawText(textRenderer, "→", currentX, y + 18, 0xFFD700, false);

        // Результат
        currentX += 15;
        context.drawItem(recipe.output, currentX, y + 10);

        // Название результата (сокращенное)
        String resultName = recipe.output.getName().getString();
        if (resultName.length() > 12) {
            resultName = resultName.substring(0, 12) + "...";
        }
        context.drawText(
                textRenderer,
                resultName,
                x + 5,
                y + 2,
                0xFFD700,
                false
        );

        // Проверка наведения
        boolean hovered = mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + 40;

        if (hovered) {
            // Подсветка
            context.fill(x, y, x + width, y + 40, 0x40FFFFFF);

            // Показываем тултип
            context.drawTooltip(textRenderer, recipe.getDetailedTooltip(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount > 0) {
            scrollUp();
        } else {
            scrollDown();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Вспомогательный класс для рецепта
    public static class RecipeEntry {
        public final ItemStack input;
        public final ItemStack resource1;
        public final ItemStack resource2;
        public final ItemStack output;
        public final Text description;

        public RecipeEntry(ItemStack input, ItemStack resource, ItemStack output, Text description) {
            this.input = input.copy();
            this.resource1 = resource.copy();
            this.resource2 = ItemStack.EMPTY;
            this.output = output.copy();
            this.description = description;
        }

        public RecipeEntry(ItemStack input, ItemStack resource1, ItemStack resource2,
                           ItemStack output, Text description) {
            this.input = input.copy();
            this.resource1 = resource1.copy();
            this.resource2 = resource2.copy();
            this.output = output.copy();
            this.description = description;
        }

        public List<Text> getDetailedTooltip() {
            List<Text> tooltip = new ArrayList<>();

            // Заголовок
            tooltip.add(Text.literal("§6§l" + output.getName().getString()));
            tooltip.add(Text.literal("§7" + description.getString()));
            tooltip.add(Text.literal("§8══════════════════════"));

            // Требования
            tooltip.add(Text.literal("§e§lТребуемые материалы:"));
            tooltip.add(Text.literal(" §f• " + input.getCount() + "× §b" + input.getName().getString()));
            tooltip.add(Text.literal(" §f• " + resource1.getCount() + "× §b" + resource1.getName().getString()));

            if (!resource2.isEmpty()) {
                tooltip.add(Text.literal(" §f• " + resource2.getCount() + "× §b" + resource2.getName().getString()));
            }

            // Если меч имеет зачарования, показываем их
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(output);
            if (!enchantments.isEmpty()) {
                tooltip.add(Text.literal("§a§lДополнительные свойства:"));
                enchantments.forEach((enchantment, level) -> {
                    tooltip.add(Text.literal(" §7• " + enchantment.getName(level).getString()));
                });
            }

            // Показываем схему крафта в зависимости от меча
            tooltip.add(Text.literal("§8══════════════════════"));
            tooltip.add(Text.literal("§6§lСхема крафта:"));

            if (output.getItem() == FireMagicMod.CHAOS_SWORD) {
                tooltip.add(Text.literal("§f   Q Q "));
                tooltip.add(Text.literal("§f   QDQ "));
                tooltip.add(Text.literal("§f    S  "));
                tooltip.add(Text.literal("§7Где: Q=Кварц, D=Алм.меч, S=Незеритовый скрап"));
            } else if (output.getItem() == FireMagicMod.SHADOW_STEP_SWORD) {
                tooltip.add(Text.literal("§f   E "));
                tooltip.add(Text.literal("§f   S "));
                tooltip.add(Text.literal("§f   E "));
                tooltip.add(Text.literal("§7Где: E=Эндер-перл, S=Алм.меч"));
            }

            // Инструкция
            tooltip.add(Text.literal("§8══════════════════════"));
            tooltip.add(Text.literal("§eКак создать:"));
            tooltip.add(Text.literal(" §7Поместите предметы на верстак"));
            tooltip.add(Text.literal(" §7в указанном выше порядке"));

            return tooltip;
        }
    }
}