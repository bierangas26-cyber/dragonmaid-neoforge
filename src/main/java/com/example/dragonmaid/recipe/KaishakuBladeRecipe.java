package com.example.dragonmaid.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public final class KaishakuBladeRecipe extends ShapedRecipe {
    private final ShapedRecipe base;

    private KaishakuBladeRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(RegistryAccess.EMPTY).copy(), recipe.showNotification());
        this.base = recipe;
    }

    public ShapedRecipe base() {
        return this.base;
    }

    @Override
    public boolean matches(CraftingInput grid, Level level) {
        for (int i = 0; i <= grid.width() - this.getWidth(); i++) {
            for (int j = 0; j <= grid.height() - this.getHeight(); j++) {
                if (this.matchesAt(grid, i, j)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matchesAt(CraftingInput grid, int offsetX, int offsetY) {
        for (int i = 0; i < grid.height(); i++) {
            for (int j = 0; j < grid.width(); j++) {
                int k = j - offsetX;
                int l = i - offsetY;
                Ingredient ingredient = k >= 0 && k < this.getWidth() && l >= 0 && l < this.getHeight()
                    ? (Ingredient)this.getIngredients().get(l * this.getWidth() + k)
                    : Ingredient.EMPTY;
                if (!ingredient.test(grid.getItem(i * grid.width() + j))) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.KAISHAKU_BLADE.get();
    }

    public static final class Serializer implements RecipeSerializer<KaishakuBladeRecipe> {
        public static final MapCodec<KaishakuBladeRecipe> CODEC =
            RecipeSerializer.SHAPED_RECIPE.codec().xmap(KaishakuBladeRecipe::new, KaishakuBladeRecipe::base);
        public static final StreamCodec<RegistryFriendlyByteBuf, KaishakuBladeRecipe> STREAM_CODEC =
            RecipeSerializer.SHAPED_RECIPE.streamCodec().map(KaishakuBladeRecipe::new, KaishakuBladeRecipe::base);

        public Serializer() {
        }

        @Override
        public MapCodec<KaishakuBladeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, KaishakuBladeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
