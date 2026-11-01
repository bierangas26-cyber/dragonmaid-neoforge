package com.example.dragonmaid.gametest;

import com.example.dragonmaid.learning.KnowledgeUpgrades;
import java.lang.reflect.Method;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 1.21.1 data-component regression: an enchanted book keeps its enchantments in the
 * STORED_ENCHANTMENTS component.  Before the port fix the knowledge table read the removed
 * 1.20.1 "StoredEnchantments" item tag, so every book evaluated to 0 learning experience.
 */
@GameTestHolder("dragonmaid")
@PrefixGameTestTemplate(false)
public final class KnowledgeUpgradeGameTests {
    private KnowledgeUpgradeGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 100)
    public static void enchantedBookYieldsLearningExperience(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().registry(Registries.ENCHANTMENT).orElseThrow();
        Holder<Enchantment> sharpness = enchantments.getHolderOrThrow(Enchantments.SHARPNESS);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(sharpness, 3);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

        helper.assertTrue(KnowledgeUpgrades.isBookCandidate(book), "enchanted book is not recognised as a book candidate");
        KnowledgeUpgrades.BookKind kind = KnowledgeUpgrades.bookKind(book);
        helper.assertTrue(kind == KnowledgeUpgrades.BookKind.REGULAR, "enchanted book classified as " + kind + " instead of REGULAR");

        int experience = bookLearningExperience(book, 1);
        helper.assertTrue(experience > 0, "enchanted book produced " + experience + " learning experience (expected > 0)");

        // A plain book (no stored enchantments) must still be rejected.
        ItemStack plain = new ItemStack(Items.ENCHANTED_BOOK);
        helper.assertTrue(!KnowledgeUpgrades.acceptsBook(plain), "empty enchanted book was accepted");
        helper.assertTrue(bookLearningExperience(plain, 1) == 0, "empty enchanted book produced learning experience");
        helper.succeed();
    }

    /** {@code bookLearningExperience} is package private; the test reads it reflectively. */
    private static int bookLearningExperience(ItemStack stack, int knowledgeLevel) {
        try {
            Method method = KnowledgeUpgrades.class.getDeclaredMethod("bookLearningExperience", ItemStack.class, int.class);
            method.setAccessible(true);
            return ((Integer) method.invoke(null, stack, knowledgeLevel)).intValue();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("KnowledgeUpgrades.bookLearningExperience is gone", exception);
        }
    }
}
