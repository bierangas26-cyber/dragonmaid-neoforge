package com.example.dragonmaid.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.example.dragonmaid.compat.RO;
import com.example.dragonmaid.compat.Reg;

public final class ModRecipes {
    private static final Reg<RecipeSerializer<?>> SERIALIZERS = Reg.create(BuiltInRegistries.RECIPE_SERIALIZER, "dragonmaid");
    public static final RO<RecipeSerializer<KaishakuBladeRecipe>> KAISHAKU_BLADE = SERIALIZERS.register(
        "kaishaku_blade", KaishakuBladeRecipe.Serializer::new
    );
    public static final RO<RecipeSerializer<DragonBreathEmblemRecipe>> DRAGON_BREATH_EMBLEM = SERIALIZERS.register(
        "dragon_breath_emblem", DragonBreathEmblemRecipe.Serializer::new
    );
    public static final RO<RecipeSerializer<VerticalAccessoryRecipe>> VERTICAL_ACCESSORY = SERIALIZERS.register(
        "vertical_accessory", VerticalAccessoryRecipe.Serializer::new
    );
    public static final RO<RecipeSerializer<UltimateDragonBreathRecipe>> ULTIMATE_DRAGON_BREATH = SERIALIZERS.register(
        "ultimate_dragon_breath", UltimateDragonBreathRecipe.Serializer::new
    );
    public static final RO<RecipeSerializer<ReinforcedDragonHornRecipe>> REINFORCED_DRAGON_HORN = SERIALIZERS.register(
        "reinforced_dragon_horn", ReinforcedDragonHornRecipe.Serializer::new
    );

    private ModRecipes() {
    }

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
