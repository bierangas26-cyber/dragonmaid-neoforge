package com.example.dragonmaid.block;

import com.example.dragonmaid.menu.RecallMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.example.dragonmaid.compat.net.NetworkHooks;

public final class DragonRecallAltarBlock extends Block {
    private static final VoxelShape COLLISION_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
    private static final VoxelShape OUTLINE_SHAPE = Shapes.or(
        COLLISION_SHAPE,
        new VoxelShape[]{
            Block.box(5.0, 12.0, 5.0, 11.0, 16.0, 11.0),
            Block.box(0.0, 12.0, 2.5, 4.5, 14.0, 13.5),
            Block.box(11.5, 12.0, 2.5, 16.0, 14.0, 13.5)
        }
    );

    public DragonRecallAltarBlock() {
        super(Properties.of().mapColor(MapColor.STONE).strength(4.0F, 12.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion());
    }

    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return OUTLINE_SHAPE;
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        } else {
            if (player instanceof ServerPlayer serverplayer) {
                NetworkHooks.openScreen(
                    serverplayer,
                    new SimpleMenuProvider(
                        (id, inventory, ignored) -> new RecallMenu(id, inventory, pos), Component.translatable("screen.dragonmaid.recall.title")
                    ),
                    pos
                );
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dragonmaid.dragon_recall_altar"));
    }
}
