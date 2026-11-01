package com.example.dragonmaid.movement;

import com.iafenvoy.iceandfire.item.block.IceSpikesBlock;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.PathType;

public final class HumanoidHazards {
    private HumanoidHazards() {
    }

    public static float malus(Mob dragon, PathType type, float original) {
        if (type == PathType.LAVA) {
            return dragon instanceof FireDragonEntity ? 0.0F : -1.0F;
        } else {
            return !(dragon instanceof FireDragonEntity) || type != PathType.DAMAGE_FIRE && type != PathType.DANGER_FIRE ? original : 0.0F;
        }
    }

    public static PathType pathType(Mob dragon, BlockGetter world, int x, int y, int z, PathType original) {
        BlockPos blockpos = new BlockPos(x, y, z);
        boolean flag = !(dragon instanceof IceDragonEntity);
        if (!flag || !spikes(world, blockpos) && !spikes(world, blockpos.below())) {
            if (!(dragon instanceof FireDragonEntity) && world.getFluidState(blockpos).is(FluidTags.LAVA)) {
                return PathType.BLOCKED;
            } else if (original != PathType.WALKABLE
                && original != PathType.WATER
                && original != PathType.WATER_BORDER
                && original != PathType.DANGER_FIRE) {
                return original;
            } else {
                MutableBlockPos mutableblockpos = new MutableBlockPos();

                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        if (i != 0 || j != 0) {
                            for (int k = -1; k <= 0; k++) {
                                mutableblockpos.set(x + i, y + k, z + j);
                                if (flag && spikes(world, mutableblockpos)) {
                                    return PathType.DANGER_OTHER;
                                }

                                if (!(dragon instanceof FireDragonEntity) && world.getFluidState(mutableblockpos).is(FluidTags.LAVA)) {
                                    return PathType.DANGER_FIRE;
                                }
                            }
                        }
                    }
                }

                return original;
            }
        } else {
            return PathType.BLOCKED;
        }
    }

    private static boolean spikes(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof IceSpikesBlock;
    }
}
