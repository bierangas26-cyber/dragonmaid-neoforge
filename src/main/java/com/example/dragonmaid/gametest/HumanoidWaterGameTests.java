package com.example.dragonmaid.gametest;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 1.2 humanoid water handling ({@link HumanoidWater} / HumanoidMoveControl).
 *
 * <p>Locks the ported 1.2 behaviour: a humanoid ice dragon that is submerged dives
 * (breath origin moves off the eye, custom travel takes over), while a dry humanoid
 * dragon falls back to the vanilla path (no active water travel, no dive breath).</p>
 */
@GameTestHolder("dragonmaid")
@PrefixGameTestTemplate(false)
public final class HumanoidWaterGameTests {
    private HumanoidWaterGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 200)
    public static void humanoidIceDragonSwimsAndDives(GameTestHelper helper) {
        ServerPlayer owner = GameTestSupport.mockPlayer(helper);
        // IAF CE 2.2 registers synced data attachments; a mock connection must negotiate the channel
        // or the entity tick throws "Payload neoforge:sync_attachments may not be sent to the client".
        GameTestSupport.allowClientPayload(owner, ResourceLocation.fromNamespaceAndPath("neoforge", "sync_attachments"));
        // Flood the whole 5x5x5 template so a hatchling-sized dragon ends up submerged.
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 5; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState());
                }
            }
        }

        DragonBaseEntity dragon = GameTestSupport.spawnTamedDragon(helper, owner, GameTestSupport.CENTRE);
        DragonState state = DragonForms.state(dragon);
        state.dragonmaid$setKnowledge(300);
        state.dragonmaid$setKnowledgeLevel(1);
        state.dragonmaid$setHumanoid(true);

        // isInWater() is a per-tick flag; let the entity tick twice before asserting water state.
        helper.runAfterDelay(2L, () -> {
            try {
                helper.assertTrue(dragon instanceof IceDragonEntity, "fixture should spawn an ice dragon");
                helper.assertTrue(HumanoidWater.active(dragon), "humanoid dragon in water should be active");
                helper.assertTrue(HumanoidWater.submerged(dragon), "a flooded 5x5x5 box should submerge the dragon");
                helper.assertTrue(HumanoidWater.canDive(dragon), "ice dragons can dive");

                Vec3 eye = dragon.getEyePosition();
                Vec3 breath = HumanoidWater.breathOrigin(dragon);
                helper.assertTrue(
                    breath.distanceToSqr(eye) > 1.0E-6D, "submerged dive breath should not start at the eye"
                );

                helper.assertTrue(HumanoidWater.travel(dragon, Vec3.ZERO), "humanoid water travel should own the tick");
                helper.assertTrue(dragon.fallDistance == 0.0F, "water travel should reset fall distance");
                helper.succeed();
            } finally {
                GameTestSupport.discard(dragon);
                GameTestSupport.releasePlayer(helper, owner);
            }
        });
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 200)
    public static void dryHumanoidFallsBackToVanillaTravel(GameTestHelper helper) {
        ServerPlayer owner = GameTestSupport.mockPlayer(helper);
        DragonBaseEntity dragon = null;
        try {
            dragon = GameTestSupport.spawnTamedDragon(helper, owner, GameTestSupport.CENTRE);
            DragonState state = DragonForms.state(dragon);
            state.dragonmaid$setKnowledge(300);
            state.dragonmaid$setKnowledgeLevel(1);
            state.dragonmaid$setHumanoid(true);

            helper.assertTrue(!HumanoidWater.active(dragon), "a dry humanoid dragon must not be water-active");
            helper.assertTrue(!HumanoidWater.submerged(dragon), "a dry dragon cannot be submerged");
            helper.assertTrue(HumanoidWater.wander(dragon) == null, "dry humanoid dragons get no dive wander target");
            helper.assertTrue(!HumanoidWater.travel(dragon, Vec3.ZERO), "dry humanoid dragons use vanilla travel");

            Vec3 eye = dragon.getEyePosition();
            helper.assertTrue(
                HumanoidWater.breathOrigin(dragon).distanceToSqr(eye) < 1.0E-9D,
                "dry humanoid breath should start at the eye position"
            );
            helper.succeed();
        } finally {
            GameTestSupport.discard(dragon);
            GameTestSupport.releasePlayer(helper, owner);
        }
    }
}
