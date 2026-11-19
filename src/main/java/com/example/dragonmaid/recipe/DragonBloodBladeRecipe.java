package com.example.dragonmaid.recipe;

import com.example.dragonmaid.item.CompanionBladeItem;
import com.example.dragonmaid.item.DragonBloodBladeItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * 1.2.7：把已有的刀/长剑（CompanionBladeItem）复制成同材质的龙血版本（DragonBloodBladeItem），
 * 保留原物品的全部组件（附魔、耐久、名字等），只换物品本体。
 *
 * <p>1.21.1 映射：{@code assemble(CraftingContainer, RegistryAccess)} 改为
 * {@code assemble(CraftingInput, HolderLookup.Provider)}；原来的「存 NBT → 改 id → ItemStack.of」
 * 改为「新建结果物品 → applyComponents(原物品组件)」，语义等价但走 DataComponents。</p>
 */
public final class DragonBloodBladeRecipe extends ShapelessRecipe {
    private final ShapelessRecipe base;

    private DragonBloodBladeRecipe(ShapelessRecipe source) {
        super(source.getGroup(), source.category(), source.getResultItem(net.minecraft.core.RegistryAccess.EMPTY).copy(),
                source.getIngredients());
        this.base = source;
    }

    public ShapelessRecipe base() {
        return this.base;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack original = input.getItem(slot);
            if (original.getItem() instanceof CompanionBladeItem
                    && !(original.getItem() instanceof DragonBloodBladeItem)) {
                ItemStack result = new ItemStack(this.getResultItem(registries).getItem(), original.getCount());
                result.applyComponents(original.getComponents());
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DRAGON_BLOOD_BLADE.get();
    }

    public static final class Serializer implements RecipeSerializer<DragonBloodBladeRecipe> {
        public static final MapCodec<DragonBloodBladeRecipe> CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(DragonBloodBladeRecipe::new, DragonBloodBladeRecipe::base);
        public static final StreamCodec<RegistryFriendlyByteBuf, DragonBloodBladeRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(DragonBloodBladeRecipe::new, DragonBloodBladeRecipe::base);

        public Serializer() {
        }

        @Override
        public MapCodec<DragonBloodBladeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DragonBloodBladeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
