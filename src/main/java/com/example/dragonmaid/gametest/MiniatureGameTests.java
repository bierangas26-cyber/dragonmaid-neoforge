package com.example.dragonmaid.gametest;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryInventory;
import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.item.ModItems;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 1.2 Faerie's Wish: equipping the miniature accessory must shrink a grown, knowledge-unlocked
 * dragon (small + worn), qualify it for the shoulder perch, and removing the charm must restore it.
 */
@GameTestHolder("dragonmaid")
@PrefixGameTestTemplate(false)
public final class MiniatureGameTests {
    private MiniatureGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 200)
    public static void faerieWishTogglesMiniature(GameTestHelper helper) {
        ServerPlayer owner = GameTestSupport.mockPlayer(helper);
        DragonBaseEntity dragon = null;
        try {
            dragon = GameTestSupport.spawnTamedDragon(helper, owner, new BlockPos(2, 2, 2));
            // Grow to stage 5 (100 days) so the miniature form is the "mature mini" variant.
            dragon.setAgeInTicks(2400000);
            DragonState state = DragonForms.state(dragon);
            state.dragonmaid$setKnowledge(300);
            state.dragonmaid$setKnowledgeLevel(1);
            helper.assertTrue(!MiniatureForm.isSmall(dragon), "dragon unexpectedly starts miniature");
            helper.assertTrue(DragonAccessories.unlockedSlots(dragon) >= 1, "no accessory slot was unlocked");

            DragonAccessoryInventory inventory = state.dragonmaid$getAccessories();
            helper.assertTrue(inventory != null, "dragon accessory inventory is null");
            inventory.setItem(0, new ItemStack(ModItems.MINIATURE_CHARM.get()));
            DragonAccessories.refresh(dragon);

            helper.assertTrue(MiniatureForm.isSmall(dragon), "Faerie's Wish did not shrink the dragon");
            helper.assertTrue(
                ((MiniatureForm.Holder)dragon).dragonmaid$miniWorn(), "miniature state was not marked as worn"
            );
            helper.assertTrue(MiniatureForm.isShoulderMini(dragon), "miniature dragon does not qualify for the shoulder perch");
            helper.assertTrue(MiniatureForm.isMatureMini(dragon), "stage-five dragon should be a mature mini");

            inventory.removeItemNoUpdate(0);
            DragonAccessories.refresh(dragon);
            helper.assertTrue(!MiniatureForm.isSmall(dragon), "removing the charm did not restore the dragon size");
            helper.succeed();
        } finally {
            GameTestSupport.discard(dragon);
            GameTestSupport.releasePlayer(helper, owner);
        }
    }
}
