package com.adam.firemagic.items.enhanced;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.client.gui.EnhancerBookScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class EnhancerRecipeManager {
    private static final List<EnhancerBookScreen.RecipeEntry> ARMOR_RECIPES = new ArrayList<>();
    private static final List<EnhancerBookScreen.RecipeEntry> SWORD_RECIPES = new ArrayList<>();
    private static final List<EnhancerBookScreen.RecipeEntry> BOW_RECIPES = new ArrayList<>();

    public static void init() {
        ARMOR_RECIPES.clear();
        SWORD_RECIPES.clear();
        BOW_RECIPES.clear();

        // ========= МЕЧИ =========
        ItemStack chaosSword = FireMagicMod.CHAOS_SWORD.getDefaultStack();
        chaosSword.setCustomName(Text.literal("Chaos Sword").formatted(Formatting.DARK_PURPLE));

        addSwordRecipe(
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.QUARTZ, 4),
                new ItemStack(Items.NETHERITE_SCRAP, 1),
                chaosSword,
                Text.literal("4 Quartz + 1 Netherite Scrap").formatted(Formatting.GRAY)
        );

        ItemStack shadowSword = FireMagicMod.SHADOW_STEP_SWORD.getDefaultStack();
        shadowSword.setCustomName(Text.literal("Shadow Step Sword").formatted(Formatting.DARK_PURPLE));

        addSwordRecipe(
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.ENDER_PEARL, 2),
                ItemStack.EMPTY,
                shadowSword,
                Text.literal("2 Ender Pearls").formatted(Formatting.GRAY)
        );

        // ========= БРОНЯ =========

        // 🪖 Hunter Helm
        ItemStack hunterHelm = FireMagicMod.HUNTER_HELM.getDefaultStack();
        hunterHelm.setCustomName(Text.literal("Hunter Helm").formatted(Formatting.GREEN));

        addArmorRecipe(
                new ItemStack(Items.DIAMOND_HELMET),
                new ItemStack(Items.REDSTONE, 2),
                hunterHelm,
                Text.literal("Reveals nearby enemies").formatted(Formatting.GRAY)
        );



    }

    private static void addArmorRecipe(ItemStack input, ItemStack resource, ItemStack output, Text description) {
        ARMOR_RECIPES.add(new EnhancerBookScreen.RecipeEntry(input, resource, output, description));
    }

    private static void addSwordRecipe(ItemStack input, ItemStack r1, ItemStack r2, ItemStack output, Text desc) {
        SWORD_RECIPES.add(new EnhancerBookScreen.RecipeEntry(input, r1, r2, output, desc));
    }

    public static List<EnhancerBookScreen.RecipeEntry> getArmorRecipes() {
        return new ArrayList<>(ARMOR_RECIPES);
    }

    public static List<EnhancerBookScreen.RecipeEntry> getSwordRecipes() {
        return new ArrayList<>(SWORD_RECIPES);
    }

    public static List<EnhancerBookScreen.RecipeEntry> getBowRecipes() {
        return new ArrayList<>(BOW_RECIPES);
    }
}