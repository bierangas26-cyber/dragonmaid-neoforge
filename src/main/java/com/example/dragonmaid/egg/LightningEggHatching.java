package com.example.dragonmaid.egg;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 1.2: lightning dragon egg 的红石信号强度提示（>=8 为 female，1..7 为 male，0 为 random）。 */
public final class LightningEggHatching {
    private LightningEggHatching() {
    }

    public static int signal(Level level, BlockPos eggPos) {
        int maximum = sample(level, eggPos);
        return maximum == 15 ? 15 : Math.max(maximum, sample(level, eggPos.below()));
    }

    private static int sample(Level level, BlockPos pos) {
        int maximum = level.getBestNeighborSignal(pos);
        if (maximum >= 15) {
            return 15;
        } else {
            BlockState state = level.getBlockState(pos);

            for (Direction direction : Direction.values()) {
                maximum = Math.max(maximum, state.getSignal(level, pos, direction));
                maximum = Math.max(maximum, state.getDirectSignal(level, pos, direction));
                if (maximum >= 15) {
                    return 15;
                }
            }

            return Math.max(0, maximum);
        }
    }
}
