package com.example.dragonmaid.egg;

import com.iafenvoy.iceandfire.registry.IafBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 1.2: ice dragon egg 的孵化性别提示（龙冰 / 普通冰决定 male/female）。 */
public final class IceEggHatching {
    private IceEggHatching() {
    }

    public static ExpectedSex expected(BlockGetter level, BlockPos eggPos) {
        BlockState support = level.getBlockState(eggPos.below());
        if (support.is(IafBlocks.DRAGON_ICE.get())) {
            return ExpectedSex.MALE;
        } else {
            return support.is(Blocks.ICE) ? ExpectedSex.FEMALE : ExpectedSex.RANDOM;
        }
    }

    public static enum ExpectedSex {
        MALE,
        FEMALE,
        RANDOM;
    }
}
