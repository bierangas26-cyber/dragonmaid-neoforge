package com.example.dragonmaid.recipe;

import com.iafenvoy.iceandfire.registry.IafItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public final class ReinforcedDragonHornRecipe extends ShapedRecipe {
    private final ShapedRecipe base;

    private ReinforcedDragonHornRecipe(ShapedRecipe source) {
        super(source.getGroup(), source.category(), source.pattern, source.getResultItem(RegistryAccess.EMPTY).copy(), source.showNotification());
        this.base = source;
    }

    public ShapedRecipe base() {
        return this.base;
    }

    @Override
    public ItemStack assemble(CraftingInput grid, net.minecraft.core.HolderLookup.Provider registries) {
        ItemStack itemstack = ItemStack.EMPTY;

        for (int i = 0; i < grid.size(); i++) {
            if (grid.getItem(i).is(IafItems.DRAGON_HORN.get())) {
                if (!itemstack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                itemstack = grid.getItem(i);
            }
        }

        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            ItemStack result = this.getResultItem(registries).copy();
            if (itemstack.has(DataComponents.CUSTOM_DATA)) {
                result.set(DataComponents.CUSTOM_DATA, itemstack.get(DataComponents.CUSTOM_DATA));
            }
            return result;
        }
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.REINFORCED_DRAGON_HORN.get();
    }

    public static final class Serializer implements RecipeSerializer<ReinforcedDragonHornRecipe> {
        public static final MapCodec<ReinforcedDragonHornRecipe> CODEC =
            RecipeSerializer.SHAPED_RECIPE.codec().xmap(ReinforcedDragonHornRecipe::new, ReinforcedDragonHornRecipe::base);
        public static final StreamCodec<RegistryFriendlyByteBuf, ReinforcedDragonHornRecipe> STREAM_CODEC =
            RecipeSerializer.SHAPED_RECIPE.streamCodec().map(ReinforcedDragonHornRecipe::new, ReinforcedDragonHornRecipe::base);

        public Serializer() {
        }

        @Override
        public MapCodec<ReinforcedDragonHornRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ReinforcedDragonHornRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
