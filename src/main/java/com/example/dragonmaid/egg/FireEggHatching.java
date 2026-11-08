package com.example.dragonmaid.egg;

import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.registry.IafDragonTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** 1.2: fire dragon egg 的孵化性别提示（灵魂火 + 维度决定 male/female）。 */
public final class FireEggHatching {
    private FireEggHatching() {
    }

    public static ExpectedSex expected(DragonEggEntity egg) {
        return egg.getEggType().getType() != IafDragonTypes.FIRE
            ? ExpectedSex.RANDOM
            : expected(egg.level().dimension(), egg.level().getBlockState(egg.blockPosition()).is(Blocks.SOUL_FIRE));
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

    public static enum ExpectedSex {
        MALE,
        FEMALE,
        RANDOM;
    }
}
