package com.example.dragonmaid.gametest;

import com.example.dragonmaid.item.ModItems;
import com.example.dragonmaid.item.ReinforcedDragonHornItem;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.component.DragonHornComponent;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Reinforced dragon horn round trip: capture a tamed dragon into the horn's DRAGON_HORN data
 * component, then release it again and check the UUID survived.
 */
@GameTestHolder("dragonmaid")
@PrefixGameTestTemplate(false)
public final class RecallHornGameTests {
    private static final BlockPos DRAGON_POS = new BlockPos(2, 2, 2);
    private static final BlockPos GROUND_POS = new BlockPos(1, 1, 1);

    private RecallHornGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 300)
    public static void captureAndReleaseKeepsUuid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = GameTestSupport.mockPlayer(helper);
        DragonBaseEntity dragon = null;
        try {
            dragon = GameTestSupport.spawnTamedDragon(helper, owner, DRAGON_POS);
            UUID dragonId = dragon.getUUID();
            helper.assertTrue(dragon.isTame(), "test dragon is not tame");
            helper.assertTrue(dragon.isOwnedBy(owner), "test dragon is not owned by the mock player");

            ReinforcedDragonHornItem horn = (ReinforcedDragonHornItem) ModItems.REINFORCED_DRAGON_HORN.get();
            ItemStack stack = new ItemStack(horn);
            owner.setItemInHand(InteractionHand.MAIN_HAND, stack);

            // --- capture --------------------------------------------------------------
            InteractionResult captured = horn.interactLivingEntity(stack, owner, dragon, InteractionHand.MAIN_HAND);
            helper.assertTrue(captured == InteractionResult.SUCCESS, "capture returned " + captured);
            helper.assertTrue(dragon.isRemoved(), "dragon is still in the level after horn capture");
            helper.assertTrue(stack.has(IafDataComponents.DRAGON_HORN.get()), "horn did not receive the DRAGON_HORN component");

            DragonHornComponent component = stack.get(IafDataComponents.DRAGON_HORN.get());
            helper.assertTrue(component != null, "DRAGON_HORN component is null");
            helper.assertTrue(dragonId.equals(component.entityUuid()), "horn stored uuid " + component.entityUuid() + " instead of " + dragonId);
            helper.assertTrue(
                component.entityType() != null && component.entityType().toString().equals("iceandfire:ice_dragon"),
                "horn stored unexpected entity type " + component.entityType()
            );

            // --- release --------------------------------------------------------------
            helper.setBlock(GROUND_POS, Blocks.STONE.defaultBlockState());
            BlockPos ground = helper.absolutePos(GROUND_POS);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(ground.above()), Direction.UP, ground, false);
            UseOnContext context = new UseOnContext(owner, InteractionHand.MAIN_HAND, hit);
            InteractionResult released = horn.useOn(context);

            helper.assertTrue(released == InteractionResult.SUCCESS, "release returned " + released);
            helper.assertFalse(stack.has(IafDataComponents.DRAGON_HORN.get()), "horn kept the DRAGON_HORN component after release");

            var restored = level.getEntity(dragonId);
            helper.assertTrue(restored != null, "released dragon is not in the level");
            helper.assertTrue(restored instanceof DragonBaseEntity, "released entity is not a dragon: " + restored);
            helper.assertTrue(dragonId.equals(restored.getUUID()), "released dragon uuid changed: " + restored.getUUID());
            helper.assertTrue(restored.blockPosition().getY() >= ground.getY(), "released dragon was placed below the clicked block");
            helper.succeed();
        } finally {
            GameTestSupport.releasePlayer(helper, owner);
        }
    }
}
