package com.example.dragonmaid.brewing;

import com.example.dragonmaid.item.ModItems;
import com.iafenvoy.iceandfire.registry.IafItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

/**
 * 1.21.1 / NeoForge 迁移：
 * - `BrewingRecipeRegistry.addRecipe(...)` 静态入口已删除（该类现在是 record）；改监听
 *   `RegisterBrewingRecipesEvent`（game bus），走 `event.getBuilder().addRecipe(IBrewingRecipe)`。
 * - `PotionUtils` 已删除（1.21 药水数据改为 DataComponents.POTION_CONTENTS / PotionContents）。
 */
@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class RemedyBrewing extends BrewingRecipe {
    private RemedyBrewing() {
        super(
            Ingredient.of(new ItemStack[]{mundaneBottle()}),
            Ingredient.of(new ItemLike[]{(ItemLike)IafItems.GORGON_HEAD.get()}),
            new ItemStack((ItemLike)ModItems.PETRIFICATION_REMEDY.get())
        );
    }

    @SubscribeEvent
    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(new RemedyBrewing());
    }

    public static ItemStack mundaneBottle() {
        return PotionContents.createItemStack(Items.POTION, Potions.MUNDANE);
    }

    public boolean isInput(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }

        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.MUNDANE) && contents.customEffects().isEmpty();
    }
}
