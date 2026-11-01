package com.example.dragonmaid.recipe;

import com.example.dragonmaid.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public final class UltimateDragonBreathRecipe extends ShapedRecipe {
    private final ShapedRecipe base;

    private UltimateDragonBreathRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(RegistryAccess.EMPTY).copy(), recipe.showNotification());
        this.base = recipe;
        if (recipe.getWidth() != 3 || recipe.getHeight() != 2) {
            throw new IllegalArgumentException("The ultimate dragon breath recipe requires its canonical two-row pattern");
        }
    }

    public ShapedRecipe base() {
        return this.base;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public boolean matches(CraftingInput grid, Level level) {
        if (this.canCraftInDimensions(grid.width(), grid.height()) && grid.getItem(4).is(ModItems.DRAGON_BREATH_EMBLEM.get())) {
            for (int i = 0; i < grid.size(); i++) {
                ItemStack itemstack = grid.getItem(i);

                boolean flag = switch (i) {
                    case 1 -> itemstack.is(ModItems.LIGHTNING_DISCHARGE_DEVICE.get());
                    default -> itemstack.isEmpty();
                    case 3 -> itemstack.is(ModItems.STEAMING_DRINK.get());
                    case 4 -> itemstack.is(ModItems.DRAGON_BREATH_EMBLEM.get());
                    case 5 -> itemstack.is(ModItems.ICY_DRINK.get());
                };
                if (!flag) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ULTIMATE_DRAGON_BREATH.get();
    }

    public static final class Serializer implements RecipeSerializer<UltimateDragonBreathRecipe> {
        public static final MapCodec<UltimateDragonBreathRecipe> CODEC =
            RecipeSerializer.SHAPED_RECIPE.codec().xmap(UltimateDragonBreathRecipe::new, UltimateDragonBreathRecipe::base);
        public static final StreamCodec<RegistryFriendlyByteBuf, UltimateDragonBreathRecipe> STREAM_CODEC =
            RecipeSerializer.SHAPED_RECIPE.streamCodec().map(UltimateDragonBreathRecipe::new, UltimateDragonBreathRecipe::base);

        public Serializer() {
        }

        @Override
        public MapCodec<UltimateDragonBreathRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UltimateDragonBreathRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
