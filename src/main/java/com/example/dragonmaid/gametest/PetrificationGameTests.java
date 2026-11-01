package com.example.dragonmaid.gametest;

import com.example.dragonmaid.item.ModItems;
import com.example.dragonmaid.petrification.DragonPetrification;
import com.example.dragonmaid.petrification.PetrificationLedger;
import com.example.dragonmaid.recall.RecallLedger;
import com.example.dragonmaid.recall.RecallTracking;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Petrification round trip through the production facade ({@link DragonPetrification}, which
 * forwards to {@code RecallDragonPetrification} — the same entry point the gorgon mixins use):
 * a tamed dragon becomes a stone statue, the dragon leaves the level, the statue keeps the dragon
 * snapshot in its DragonMaidPetrification tag and the remedy restores the very same dragon UUID.
 */
@GameTestHolder("dragonmaid")
@PrefixGameTestTemplate(false)
public final class PetrificationGameTests {
    private static final BlockPos DRAGON_POS = new BlockPos(2, 2, 2);
    private static final String MARKER = "DragonMaidPetrification";

    private PetrificationGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 300)
    public static void statueRoundTripKeepsDragon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = GameTestSupport.mockPlayer(helper);
        DragonBaseEntity dragon = null;
        StoneStatueEntity statue = null;
        try {
            dragon = GameTestSupport.spawnTamedDragon(helper, owner, DRAGON_POS);
            UUID dragonId = dragon.getUUID();
            helper.assertTrue(DragonPetrification.isEligibleDragon(dragon), "tamed dragon is not petrification eligible");

            statue = DragonPetrification.buildStatue(dragon);
            helper.assertTrue(statue != null, "buildStatue returned null");
            // IAF's StoneStatueEntity.buildStatueEntity() does not copy the target position: the real
            // gorgon flow calls absMoveTo(target) before addFreshEntity() (GorgonEntity#aiStep), so the
            // test has to do the same or the statue would be added at the level origin.
            BlockPos statuePos = helper.absolutePos(DRAGON_POS);
            statue.moveTo(statuePos.getX() + 0.5D, statuePos.getY(), statuePos.getZ() + 0.5D, 0.0F, 0.0F);

            CompoundTag marker = statue.getTrappedTag().getCompound(MARKER);
            helper.assertTrue(marker.hasUUID("Dragon"), "statue payload has no dragon uuid");
            helper.assertTrue(dragonId.equals(marker.getUUID("Dragon")), "statue payload kept the wrong dragon uuid");
            helper.assertTrue(marker.hasUUID("Token"), "statue payload has no transfer token");

            boolean added = DragonPetrification.addStatue(level, statue);
            helper.assertTrue(added, "addStatue refused to place the statue (statueRemoved=" + statue.isRemoved()
                + ", dragonRemoved=" + dragon.isRemoved() + ", pos=" + statue.position() + ")");
            helper.assertTrue(dragon.isRemoved(), "dragon is still in the level after petrification");
            PetrificationLedger ledger = PetrificationLedger.get(level);
            helper.assertTrue(ledger.active(dragonId), "petrification ledger has no active entry for the dragon");

            // --- restore ---------------------------------------------------------------
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PETRIFICATION_REMEDY.get()));
            owner.teleportTo(level, statue.getX(), statue.getY(), statue.getZ(), 0.0F, 0.0F);
            InteractionResult restored = DragonPetrification.restore(owner, InteractionHand.MAIN_HAND, statue);
            helper.assertTrue(restored.consumesAction(), "restore returned " + restored);

            var entity = level.getEntity(dragonId);
            helper.assertTrue(entity instanceof DragonBaseEntity, "restored dragon is missing from the level: " + entity);
            helper.assertTrue(!ledger.active(dragonId), "petrification ledger is still active after a successful restore");
            helper.succeed();
        } finally {
            GameTestSupport.discard(dragon);
            if (statue != null && !statue.isRemoved()) {
                statue.discard();
            }
            GameTestSupport.releasePlayer(helper, owner);
        }
    }

    /**
     * Regression for the 1.21.1 persistent-data rename: NeoForge saves {@code Entity#getPersistentData()}
     * under {@code NeoForgeData} (Entity#saveWithoutId), so the recall token written for a dragon must be
     * readable again from the saved tag.  Before the port fix {@code RecallTracking} still used the 1.20.1
     * key {@code ForgeData}, which silently broke {@code RecallPersistence.checkpoint} (statue placement
     * rolled back) and any dragon recreated from a snapshot.
     */
    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 100)
    public static void savedSnapshotKeepsRecallToken(GameTestHelper helper) {
        ServerPlayer owner = GameTestSupport.mockPlayer(helper);
        DragonBaseEntity dragon = null;
        try {
            dragon = GameTestSupport.spawnTamedDragon(helper, owner, DRAGON_POS);
            RecallLedger.Entry entry = RecallLedger.get(helper.getLevel()).entry(dragon.getUUID());
            helper.assertTrue(entry != null, "the dragon has no recall ledger entry after joining the level");

            CompoundTag saved = dragon.saveWithoutId(new CompoundTag());
            helper.assertTrue(
                saved.getCompound("NeoForgeData").hasUUID("DragonMaidRecallToken"),
                "saved entity tag carries no recall token under NeoForgeData; keys=" + saved.getAllKeys()
            );
            helper.assertTrue(
                RecallTracking.matches(saved, entry),
                "RecallTracking.matches(savedTag, entry) is false: the snapshot check reads a different key than Entity#saveWithoutId writes"
            );

            CompoundTag stamped = new CompoundTag();
            RecallTracking.stamp(stamped, entry);
            helper.assertTrue(
                stamped.getCompound("NeoForgeData").hasUUID("DragonMaidRecallToken"),
                "RecallTracking.stamp(CompoundTag) wrote the token under a key the entity loader does not read"
            );
            helper.assertTrue(RecallTracking.matches(stamped, entry), "a freshly stamped snapshot does not verify");
            helper.succeed();
        } finally {
            GameTestSupport.discard(dragon);
            GameTestSupport.releasePlayer(helper, owner);
        }
    }
}
