package com.example.dragonmaid.gametest;

import com.example.dragonmaid.DragonMaid;
import com.example.dragonmaid.menu.RecallMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The recall altar must open its menu for a server player when used with an empty hand.
 */
@GameTestHolder("dragonmaid")
@PrefixGameTestTemplate(false)
public final class AltarInteractionGameTests {
    private static final BlockPos ALTAR_POS = new BlockPos(1, 1, 1);

    private AltarInteractionGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 200)
    public static void altarOpensRecallMenu(GameTestHelper helper) {
        ServerPlayer player = GameTestSupport.mockPlayer(helper);
        try {
            // The altar opens a menu with extra data, which NeoForge sends as a custom payload.
            GameTestSupport.allowClientPayload(player, ResourceLocation.fromNamespaceAndPath("neoforge", "advanced_open_screen"));
            GameTestSupport.allowClientPayload(player, ResourceLocation.fromNamespaceAndPath("dragonmaid", "compat"));
            helper.setBlock(ALTAR_POS, DragonMaid.DRAGON_RECALL_ALTAR.get());
            BlockPos pos = helper.absolutePos(ALTAR_POS);

            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            BlockState state = helper.getLevel().getBlockState(pos);

            InteractionResult result = state.useWithoutItem(helper.getLevel(), player, hit);
            helper.assertTrue(result.consumesAction(), "using the altar returned " + result);
            helper.assertTrue(
                player.containerMenu instanceof RecallMenu,
                "the recall menu was not opened, active menu is " + player.containerMenu
            );
            helper.assertTrue(
                ((RecallMenu) player.containerMenu).altar.equals(pos),
                "recall menu was opened for the wrong altar position"
            );
            helper.succeed();
        } finally {
            GameTestSupport.releasePlayer(helper, player);
        }
    }
}
