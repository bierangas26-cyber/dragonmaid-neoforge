package com.example.dragonmaid.egg;

import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.registry.IafDragonTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.2: fire dragon egg 的孵化性别提示（灵魂火 + 维度决定 male/female）。
 * 1.2.7: 灵魂沙会把火/灵魂火抬高一格（{@link #incubationPos}），孵化判定与性别提示都要看抬高后的位置。
 */
public final class FireEggHatching {
    private FireEggHatching() {
    }

    public static ExpectedSex expected(DragonEggEntity egg) {
        if (egg.getEggType().getType() != IafDragonTypes.FIRE) {
            return ExpectedSex.RANDOM;
        }

        BlockPos pos = egg.blockPosition();
        Level level = egg.level();
        BlockPos flame = incubationPos(egg, level.getBlockState(pos), pos);
        return expected(level.dimension(), level.getBlockState(flame).is(Blocks.SOUL_FIRE));
    }

    /** 1.2.7: a soul-sand base lifts the flame one block above the egg position. */
    public static BlockPos incubationPos(DragonEggEntity egg, BlockState feet, BlockPos pos) {
        return feet.is(Blocks.SOUL_SAND) && egg.getBoundingBox().maxY > (double) pos.getY() + 1.0 ? pos.above() : pos;
    }

    public static boolean isHeated(DragonEggEntity egg, BlockState state, BlockGetter level, BlockPos pos) {
        BlockPos flame = incubationPos(egg, state, pos);
        BlockState heat = flame.equals(pos) ? state : level.getBlockState(flame);
        return heat.isBurning(level, flame) || heat.is(Blocks.SOUL_FIRE);
    }

    public static ExpectedSex expected(ResourceKey<Level> dimension, boolean soulFire) {
        if (!soulFire) {
            return ExpectedSex.RANDOM;
        } else if (dimension.equals(Level.OVERWORLD) || dimension.equals(Level.END)) {
            return ExpectedSex.MALE;
        } else {
            return dimension.equals(Level.NETHER) ? ExpectedSex.FEMALE : ExpectedSex.RANDOM;
        }
    }

    public enum ExpectedSex {
        MALE,
        FEMALE,
        RANDOM
    }
}
