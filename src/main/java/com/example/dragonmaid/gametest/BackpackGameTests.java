package com.example.dragonmaid.gametest;

import com.example.dragonmaid.accessory.DragonStorage;
import com.example.dragonmaid.accessory.DragonStorageInventory;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.inventory.DragonBagFeeding;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 1.2 dragon backpack (DragonStorage / DragonBagFeeding) coverage.
 *
 * <p>Covers the three server-side behaviours that the GUI screens only surface:
 * knowledge-gated slot count, pickup-mode cycling/filtering/insertion, and the
 * automatic eating of a hungry dragon from its bag. The accessory screen itself is
 * client code and is not exercised here.</p>
 */
@GameTestHolder("dragonmaid")
@PrefixGameTestTemplate(false)
public final class BackpackGameTests {
    private BackpackGameTests() {
    }

    /** A hungry dragon eats exactly one item out of its unlocked bag slot and gains hunger. */
    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 200)
    public static void bagAutoFeedsHungryDragon(GameTestHelper helper) {
        ServerPlayer owner = GameTestSupport.mockPlayer(helper);
        DragonBaseEntity dragon = null;
        try {
            dragon = GameTestSupport.spawnTamedDragon(helper, owner, GameTestSupport.CENTRE);
            DragonState state = DragonForms.state(dragon);
            state.dragonmaid$setKnowledge(300);
            state.dragonmaid$setKnowledgeLevel(1);
            helper.assertTrue(DragonStorage.unlockedSlots(dragon) >= 1, "no bag slot unlocked at knowledge level 1");

            DragonStorageInventory bag = state.dragonmaid$getStorage();
            helper.assertTrue(bag != null, "dragon bag inventory is null");

            dragon.setHunger(20);
            bag.setItem(0, new ItemStack(Items.BEEF, 3));
            int hungerBefore = dragon.getHunger();

            DragonBagFeeding.tick(dragon);

            helper.assertTrue(dragon.getHunger() > hungerBefore, "hungry dragon did not eat from its bag");
            helper.assertTrue(
                bag.getItem(0).getCount() == 2, "bag should have consumed exactly one beef, got " + bag.getItem(0)
            );
            helper.succeed();
        } finally {
            GameTestSupport.discard(dragon);
            GameTestSupport.releasePlayer(helper, owner);
        }
    }

    /** Pickup mode cycles OFF -> MATCHING -> ALL; filtering and insertion follow the mode. */
    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 200)
    public static void pickupModeCycleAndFiltering(GameTestHelper helper) {
        ServerPlayer owner = GameTestSupport.mockPlayer(helper);
        DragonBaseEntity dragon = null;
        try {
            dragon = GameTestSupport.spawnTamedDragon(helper, owner, GameTestSupport.CENTRE);
            DragonState state = DragonForms.state(dragon);
            state.dragonmaid$setKnowledge(300);
            state.dragonmaid$setKnowledgeLevel(1);
            DragonStorageInventory bag = state.dragonmaid$getStorage();

            helper.assertTrue(DragonStorage.pickupMode(dragon) == DragonStorage.PickupMode.OFF, "pickup mode should default to OFF");
            helper.assertTrue(
                !DragonStorage.canPickup(dragon, new ItemStack(Items.BEEF)), "OFF mode must not accept pickups"
            );

            DragonStorage.cyclePickupMode(dragon);
            helper.assertTrue(
                DragonStorage.pickupMode(dragon) == DragonStorage.PickupMode.MATCHING, "OFF should cycle to MATCHING"
            );
            helper.assertTrue(
                !DragonStorage.canPickup(dragon, new ItemStack(Items.BEEF)),
                "MATCHING mode must reject items whose type is not in the bag"
            );

            bag.setItem(0, new ItemStack(Items.BEEF, 1));
            helper.assertTrue(
                DragonStorage.canPickup(dragon, new ItemStack(Items.BEEF)), "MATCHING mode should accept a stored type"
            );
            helper.assertTrue(
                !DragonStorage.canPickup(dragon, new ItemStack(Items.DIAMOND)),
                "MATCHING mode should reject a foreign type"
            );

            DragonStorage.cyclePickupMode(dragon);
            helper.assertTrue(DragonStorage.pickupMode(dragon) == DragonStorage.PickupMode.ALL, "MATCHING should cycle to ALL");
            helper.assertTrue(
                DragonStorage.canPickup(dragon, new ItemStack(Items.DIAMOND)), "ALL mode should accept any item while room exists"
            );

            ItemStack leftover = DragonStorage.insert(dragon, new ItemStack(Items.DIAMOND, 5));
            helper.assertTrue(leftover.isEmpty(), "ALL mode insert should store the whole stack, leftover " + leftover);
            int diamonds = 0;
            for (int slot = 0; slot < DragonStorage.unlockedSlots(dragon); slot++) {
                if (bag.getItem(slot).is(Items.DIAMOND)) {
                    diamonds += bag.getItem(slot).getCount();
                }
            }
            helper.assertTrue(diamonds == 5, "bag should hold five diamonds, found " + diamonds);

            DragonStorage.cyclePickupMode(dragon);
            helper.assertTrue(DragonStorage.pickupMode(dragon) == DragonStorage.PickupMode.OFF, "ALL should cycle back to OFF");
            helper.succeed();
        } finally {
            GameTestSupport.discard(dragon);
            GameTestSupport.releasePlayer(helper, owner);
        }
    }

    /** A tamed dragon with no knowledge has no bag slots and must not swallow inserted items. */
    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 200)
    public static void knowledgeGatesBagSlots(GameTestHelper helper) {
        ServerPlayer owner = GameTestSupport.mockPlayer(helper);
        DragonBaseEntity dragon = null;
        try {
            dragon = GameTestSupport.spawnTamedDragon(helper, owner, GameTestSupport.CENTRE);
            helper.assertTrue(DragonStorage.unlockedSlots(dragon) == 0, "unlocked slots should be zero without knowledge");

            ItemStack rejected = DragonStorage.insert(dragon, new ItemStack(Items.DIAMOND, 3));
            helper.assertTrue(rejected.getCount() == 3, "locked bag must return the full stack, got " + rejected);

            DragonState state = DragonForms.state(dragon);
            state.dragonmaid$setKnowledge(300);
            state.dragonmaid$setKnowledgeLevel(1);
            helper.assertTrue(
                DragonStorage.unlockedSlots(dragon) == 9, "knowledge level 1 should unlock one row (9 slots)"
            );

            ItemStack accepted = DragonStorage.insert(dragon, new ItemStack(Items.DIAMOND, 3));
            helper.assertTrue(accepted.isEmpty(), "unlocked bag should store the stack, leftover " + accepted);
            helper.succeed();
        } finally {
            GameTestSupport.discard(dragon);
            GameTestSupport.releasePlayer(helper, owner);
        }
    }
}
