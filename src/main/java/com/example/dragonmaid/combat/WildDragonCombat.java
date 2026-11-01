package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import java.util.UUID;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class WildDragonCombat {
    private WildDragonCombat() {
    }

    public static double targetRange() {
        return (Double)DragonMaidConfig.WILD_DRAGON_TARGET_RANGE.get();
    }

    private static boolean hasValidOwner(DragonBaseEntity dragon) {
        UUID uuid = dragon.getOwnerUUID();
        return uuid != null && (uuid.getMostSignificantBits() != 0L || uuid.getLeastSignificantBits() != 0L);
    }

    public static boolean isHostileWildDragon(DragonBaseEntity dragon, LivingEntity candidate) {
        if (dragon.isTame()
            && hasValidOwner(dragon)
            && candidate instanceof DragonBaseEntity entitydragonbase
            && entitydragonbase != dragon
            && entitydragonbase.level() == dragon.level()
            && entitydragonbase.isAlive()
            && !entitydragonbase.isRemoved()
            && !entitydragonbase.isTame()
            && entitydragonbase.getOwnerUUID() == null
            && !entitydragonbase.isModelDead()
            && !GorgonEntity.isStoneMob(entitydragonbase)
            && !CompanionRelations.isAlly(dragon, entitydragonbase)) {
            return true;
        }

        return false;
    }

    public static boolean canAcquireTargets(DragonBaseEntity dragon) {
        return !dragon.level().isClientSide
            && dragon.isTame()
            && hasValidOwner(dragon)
            && dragon.isAlive()
            && !dragon.isRemoved()
            && !dragon.isModelDead()
            && !HumanoidLife.isInjured(dragon)
            && !DragonForms.state(dragon).dragonmaid$isRecoveryRest()
            && dragon.getCommand() != 1
            && !dragon.isOrderedToSit()
            && !dragon.isSleeping()
            && !dragon.isNoAi()
            && !dragon.isPassenger()
            && !GorgonEntity.isStoneMob(dragon);
    }

    public static double distanceToSqr(DragonBaseEntity dragon, LivingEntity origin, LivingEntity target) {
        if (!isHostileWildDragon(dragon, target)) {
            return origin.distanceToSqr(target);
        } else {
            AABB aabb = target.getBoundingBox();
            double d0 = Mth.clamp(origin.getX(), aabb.minX, aabb.maxX);
            double d1 = Mth.clamp(origin.getY(), aabb.minY, aabb.maxY);
            double d2 = Mth.clamp(origin.getZ(), aabb.minZ, aabb.maxZ);
            return origin.distanceToSqr(d0, d1, d2);
        }
    }

    public static boolean withinTargetRange(DragonBaseEntity dragon, LivingEntity target) {
        double d0 = isHostileWildDragon(dragon, target) ? targetRange() : 24.0;
        return distanceToSqr(dragon, dragon, target) <= d0 * d0;
    }
}
