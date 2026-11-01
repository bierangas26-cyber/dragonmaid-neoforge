package com.example.dragonmaid.gametest;

import com.example.dragonmaid.item.ModItems;
import com.example.dragonmaid.recipe.ModRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Datapack content must survive a real server start: every shipped recipe has to deserialise with
 * the mod's own serialisers, the root advancement has to exist and the mercy-killing tag has to
 * resolve to the two execution blades.
 */
@GameTestHolder("dragonmaid")
@PrefixGameTestTemplate(false)
public final class DataLoadGameTests {
    /** The 19 recipes shipped in {@code data/dragonmaid/recipe}. */
    private static final List<String> RECIPES = List.of(
        "dragon_breath_emblem",
        "dragon_heart_emblem_necklace",
        "dragon_maid_guide",
        "dragon_recall_altar",
        "guardian_pendant",
        "guardian_tag",
        "icy_drink",
        "kaishaku_blade",
        "lightning_discharge_device",
        "natural_dragon_heart_necklace",
        "reinforced_dragon_horn",
        "service_dragon_armor_body",
        "service_dragon_armor_head",
        "service_dragon_armor_neck",
        "service_dragon_armor_tail",
        "steaming_drink",
        "trinity_dragon_gem",
        "ultimate_dragon_breath_device",
        "warding_emblem"
    );

    private DataLoadGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 100)
    public static void recipesLoaded(GameTestHelper helper) {
        RecipeManager manager = helper.getLevel().getRecipeManager();
        List<String> missing = new ArrayList<>();
        for (String name : RECIPES) {
            if (manager.byKey(ResourceLocation.fromNamespaceAndPath("dragonmaid", name)).isEmpty()) {
                missing.add(name);
            }
        }

        helper.assertTrue(missing.isEmpty(), "missing dragonmaid recipes: " + missing);

        RecipeHolder<?> kaishaku = manager.byKey(ResourceLocation.fromNamespaceAndPath("dragonmaid", "kaishaku_blade")).orElseThrow();
        helper.assertTrue(
            kaishaku.value().getSerializer() == ModRecipes.KAISHAKU_BLADE.get(),
            "dragonmaid:kaishaku_blade used serializer " + kaishaku.value().getSerializer()
        );
        RecipeHolder<?> horn = manager.byKey(ResourceLocation.fromNamespaceAndPath("dragonmaid", "reinforced_dragon_horn")).orElseThrow();
        helper.assertTrue(
            horn.value().getSerializer() == ModRecipes.REINFORCED_DRAGON_HORN.get(),
            "dragonmaid:reinforced_dragon_horn used serializer " + horn.value().getSerializer()
        );
        helper.succeed();
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 100)
    public static void rootAdvancementLoaded(GameTestHelper helper) {
        ResourceLocation root = ResourceLocation.fromNamespaceAndPath("dragonmaid", "root");
        helper.assertTrue(
            helper.getLevel().getServer().getAdvancements().get(root) != null,
            "advancement dragonmaid:root is not loaded"
        );
        for (String name : List.of("closer_companion", "maid_for_hunting", "trinity_of_dragons")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("dragonmaid", name);
            helper.assertTrue(
                helper.getLevel().getServer().getAdvancements().get(id) != null,
                "advancement " + id + " is not loaded"
            );
        }
        helper.succeed();
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 100)
    public static void mercyKillingTagLoaded(GameTestHelper helper) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("dragonmaid", "mercy_killing_tools"));
        var registry = helper.getLevel().registryAccess().registry(Registries.ITEM).orElseThrow();
        var tag = registry.getTag(key).orElse(null);
        helper.assertTrue(tag != null, "item tag dragonmaid:mercy_killing_tools is not loaded");

        List<Item> contents = tag.stream().map(Holder::value).collect(Collectors.toList());
        helper.assertTrue(
            contents.contains(ModItems.KAISHAKU_BLADE.get()),
            "mercy_killing_tools is missing dragonmaid:kaishaku_blade, contents=" + contents
        );
        helper.assertTrue(
            contents.contains(ModItems.TEST_EXECUTION_BLADE.get()),
            "mercy_killing_tools is missing dragonmaid:test_execution_blade, contents=" + contents
        );
        helper.succeed();
    }
}
