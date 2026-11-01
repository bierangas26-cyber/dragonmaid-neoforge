package com.example.dragonmaid.learning;

import com.example.dragonmaid.DragonMaidConfig;
import com.iafenvoy.iceandfire.registry.IafItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class KnowledgeMaterials {
    private KnowledgeMaterials() {
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty()
            && (stack.is(Items.BOOK) || stack.is(Items.EXPERIENCE_BOTTLE) || stack.is((Item)IafItems.MANUSCRIPT.get()));
    }

    public static int experience(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        } else if (stack.is(Items.BOOK)) {
            return (Integer)DragonMaidConfig.BOOK_EXPERIENCE.get();
        } else if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return (Integer)DragonMaidConfig.EXPERIENCE_BOTTLE_EXPERIENCE.get();
        } else {
            return stack.is((Item)IafItems.MANUSCRIPT.get()) ? (Integer)DragonMaidConfig.MANUSCRIPT_EXPERIENCE.get() : 0;
        }
    }

    public static boolean accepts(ItemStack stack, int knowledgeLevel) {
        return accepts(stack) || KnowledgeUpgrades.bookLearningExperience(stack, knowledgeLevel) > 0;
    }

    public static int experience(ItemStack stack, int knowledgeLevel) {
        return accepts(stack) ? experience(stack) : KnowledgeUpgrades.bookLearningExperience(stack, knowledgeLevel);
    }
}
