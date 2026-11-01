package com.example.dragonmaid.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public final class DragonBreathEmblemRecipe extends ShapedRecipe {
    private final ShapedRecipe base;
    private final NonNullList<Ingredient> placementIngredients = NonNullList.create();

    private DragonBreathEmblemRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(RegistryAccess.EMPTY).copy(), recipe.showNotification());
        this.base = recipe;
        this.placementIngredients.addAll(recipe.getIngredients());
        String[] astring = new String[]{"fire_dragon_blood", "ice_dragon_blood", "lightning_dragon_blood"};

        for (int i = 0; i < astring.length; i++) {
            this.placementIngredients
                .set(
                    new int[]{3, 5, 7}[i],
                    Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("iceandfire", astring[i])))
                );
        }
    }

    public ShapedRecipe base() {
        return this.base;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.placementIngredients;
    }

    @Override
    public boolean matches(CraftingInput grid, Level level) {
        if (!super.matches(grid, level)) {
            return false;
        } else {
            int i = 0;

            for (int j = 0; j < grid.size(); j++) {
                ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(grid.getItem(j).getItem());
                if (resourcelocation != null && resourcelocation.getNamespace().equals("iceandfire")) {
                    String s = resourcelocation.getPath();

                    int k = switch (s) {
                        case "fire_dragon_blood" -> 1;
                        case "ice_dragon_blood" -> 2;
                        case "lightning_dragon_blood" -> 4;
                        default -> 0;
                    };
                    if ((i & k) != 0) {
                        return false;
                    }

                    i |= k;
                }
            }

            return i == 7;
        }
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DRAGON_BREATH_EMBLEM.get();
    }

    public static final class Serializer implements RecipeSerializer<DragonBreathEmblemRecipe> {
        public static final MapCodec<DragonBreathEmblemRecipe> CODEC =
            RecipeSerializer.SHAPED_RECIPE.codec().xmap(DragonBreathEmblemRecipe::new, DragonBreathEmblemRecipe::base);
        public static final StreamCodec<RegistryFriendlyByteBuf, DragonBreathEmblemRecipe> STREAM_CODEC =
            RecipeSerializer.SHAPED_RECIPE.streamCodec().map(DragonBreathEmblemRecipe::new, DragonBreathEmblemRecipe::base);

        public Serializer() {
        }

        @Override
        public MapCodec<DragonBreathEmblemRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DragonBreathEmblemRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
