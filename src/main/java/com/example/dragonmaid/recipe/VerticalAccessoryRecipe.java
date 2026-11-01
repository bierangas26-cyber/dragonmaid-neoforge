package com.example.dragonmaid.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public final class VerticalAccessoryRecipe extends ShapedRecipe {
    private final ShapedRecipe base;

    private VerticalAccessoryRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(RegistryAccess.EMPTY).copy(), recipe.showNotification());
        this.base = recipe;
        if (recipe.getWidth() != 1 || recipe.getHeight() != 3 || recipe.getIngredients().stream().anyMatch(Ingredient::isEmpty)) {
            throw new IllegalArgumentException("A vertical accessory recipe requires three nonempty rows in one column");
        }
    }

    public ShapedRecipe base() {
        return this.base;
    }

    @Override
    public boolean matches(CraftingInput grid, Level level) {
        if (!this.canCraftInDimensions(grid.width(), grid.height())) {
            return false;
        } else {
            int i = -1;
            int j = 0;

            for (int k = 0; k < grid.size(); k++) {
                if (!grid.getItem(k).isEmpty()) {
                    if (i < 0) {
                        i = k;
                    }

                    if (k != i + j * grid.width() || ++j > 3) {
                        return false;
                    }
                }
            }

            return j == 3 && this.matchesIngredients(grid, i, 0, 0);
        }
    }

    private boolean matchesIngredients(CraftingInput grid, int first, int row, int used) {
        if (row == 3) {
            return true;
        } else {
            ItemStack itemstack = grid.getItem(first + row * grid.width());

            for (int i = 0; i < 3; i++) {
                int j = 1 << i;
                if ((used & j) == 0 && ((Ingredient)this.getIngredients().get(i)).test(itemstack) && this.matchesIngredients(grid, first, row + 1, used | j)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.VERTICAL_ACCESSORY.get();
    }

    public static final class Serializer implements RecipeSerializer<VerticalAccessoryRecipe> {
        public static final MapCodec<VerticalAccessoryRecipe> CODEC =
            RecipeSerializer.SHAPED_RECIPE.codec().xmap(VerticalAccessoryRecipe::new, VerticalAccessoryRecipe::base);
        public static final StreamCodec<RegistryFriendlyByteBuf, VerticalAccessoryRecipe> STREAM_CODEC =
            RecipeSerializer.SHAPED_RECIPE.streamCodec().map(VerticalAccessoryRecipe::new, VerticalAccessoryRecipe::base);

        public Serializer() {
        }

        @Override
        public MapCodec<VerticalAccessoryRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VerticalAccessoryRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
