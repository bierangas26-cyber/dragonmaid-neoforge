package com.example.dragonmaid.gametest;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryInventory;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.item.ModItems;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Accessory effects: a warding emblem in the first unlocked slot must raise the dragon's ARMOR
 * attribute by the item's documented bonus (dragon stage clamped to 1..5, plus one).
 */
@GameTestHolder("dragonmaid")
@PrefixGameTestTemplate(false)
public final class AccessoryGameTests {
    private AccessoryGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 200)
    public static void wardingEmblemRaisesArmor(GameTestHelper helper) {
        ServerPlayer owner = GameTestSupport.mockPlayer(helper);
        DragonBaseEntity dragon = null;
        try {
            dragon = GameTestSupport.spawnTamedDragon(helper, owner, new BlockPos(2, 2, 2));
            DragonState state = DragonForms.state(dragon);

            // Unlock the first accessory slot: the knowledge *level* is normalised against the raw
            // knowledge total (level 1 needs 300 knowledge), so both have to be raised.
            state.dragonmaid$setKnowledge(300);
            state.dragonmaid$setKnowledgeLevel(1);
            helper.assertTrue(DragonAccessories.unlockedSlots(dragon) >= 1, "no accessory slot was unlocked");

            AttributeInstance armor = dragon.getAttribute(Attributes.ARMOR);
            helper.assertTrue(armor != null, "ice dragon has no ARMOR attribute");
            double before = armor.getValue();

            DragonAccessoryInventory inventory = state.dragonmaid$getAccessories();
            helper.assertTrue(inventory != null, "dragon accessory inventory is null");

            ItemStack emblem = new ItemStack(ModItems.WARDING_EMBLEM.get());
            double itemBonus = ModItems.WARDING_EMBLEM.get().armorBonus(emblem, dragon);
            helper.assertTrue(itemBonus > 0.0D, "warding emblem reports no armor bonus for this dragon");

            inventory.setItem(0, emblem);
            DragonAccessories.refresh(dragon);

            double after = armor.getValue();
            helper.assertTrue(
                after >= before + itemBonus - 1.0E-6D,
                "ARMOR did not rise: before=" + before + " after=" + after + " expected bonus=" + itemBonus
            );

            // Removing the emblem must give the armor back.
            inventory.removeItemNoUpdate(0);
            DragonAccessories.refresh(dragon);
            double restored = armor.getValue();
            helper.assertTrue(
                Math.abs(restored - before) < 1.0E-6D,
                "ARMOR did not return to its base value: before=" + before + " after-removal=" + restored
            );
            helper.succeed();
        } finally {
            GameTestSupport.discard(dragon);
            GameTestSupport.releasePlayer(helper, owner);
        }
    }
}
