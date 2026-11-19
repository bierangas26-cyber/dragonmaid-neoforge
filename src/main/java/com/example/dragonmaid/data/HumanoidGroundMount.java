package com.example.dragonmaid.data;

import com.example.dragonmaid.movement.HumanoidFlight;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.Vec3;

/**
 * 1.2.7: grounding rules for "player rides a humanoid dragon" (as opposed to the dragon riding the
 * player's shoulder), including re-attaching the rider after a relog.
 *
 * <p>Port note: upstream gated on {@code DragonForms.withinManagementReach} +
 * {@code hasManagementPermission}; this build exposes the single {@code DragonForms.canManage}
 * predicate, which is the union of both, so the checks below collapse into one call.
 */
public final class HumanoidGroundMount {
    private static final ThreadLocal<RestoringRider> RESTORING = new ThreadLocal<>();

    private HumanoidGroundMount() {
    }

    public static boolean restoreRider(ServerPlayer player, Entity vehicle, boolean force) {
        if (!(vehicle instanceof DragonBaseEntity dragon) || !DragonForms.isHumanoid(dragon)) {
            return player.startRiding(vehicle, force);
        }

        RestoringRider previous = RESTORING.get();
        RESTORING.set(new RestoringRider(player, dragon));
        try {
            return player.startRiding(dragon, force);
        } finally {
            if (previous == null) {
                RESTORING.remove();
            } else {
                RESTORING.set(previous);
            }
        }
    }

    public static boolean isRestoringRider(Player player, DragonBaseEntity dragon) {
        RestoringRider restoring = RESTORING.get();
        return restoring != null && restoring.player() == player && restoring.dragon() == dragon;
    }

    public static boolean eligible(DragonBaseEntity dragon, Entity passenger) {
        return passenger instanceof Player player && canRemainMounted(dragon, player) && DragonForms.canManage(dragon, player);
    }

    public static boolean canRemainMounted(DragonBaseEntity dragon, Entity passenger) {
        return passenger instanceof Player player
            && DragonForms.isHumanoid(dragon)
            && DragonForms.canManage(dragon, player)
            && dragon.getDragonStage() >= 3
            && !dragon.isPassenger()
            && !dragon.isNoAi()
            && !GorgonEntity.isStoneMob(dragon);
    }

    public static boolean active(DragonBaseEntity dragon) {
        return DragonForms.isHumanoid(dragon) && dragon.getControllingPassenger() instanceof Player;
    }

    public static boolean mount(DragonBaseEntity dragon, Player player) {
        if (!eligible(dragon, player)
            || HumanoidFlight.airborne(dragon)
            || dragon.isVehicle()
            || player.isPassenger()
            || !player.startRiding(dragon)) {
            return false;
        }

        dragon.setInSittingPose(false);
        dragon.setOrderedToSit(false);
        dragon.sleepProgress = 0.0F;
        dragon.prevAnimationProgresses[1] = 0.0F;
        dragon.setControlState((byte) 0);
        dragon.getNavigation().stop();
        dragon.setTarget(null);
        dragon.burningTarget = null;
        dragon.positionRider(player);
        return true;
    }

    public static double seatHeight(DragonBaseEntity dragon) {
        return (double) DragonForms.height(dragon) * 0.58;
    }

    public static Vec3 dismount(DragonBaseEntity dragon, LivingEntity passenger) {
        double distance = (double) (dragon.getBbWidth() + passenger.getBbWidth()) * 0.5 + 0.2;
        for (int quarterTurn : new int[]{1, -1, 2, 0}) {
            Vec3 offset = Vec3.directionFromRotation(0.0F, dragon.getYRot() + (float) quarterTurn * 90.0F).scale(distance);
            BlockPos position = BlockPos.containing(dragon.getX() + offset.x, dragon.getY(), dragon.getZ() + offset.z);
            Vec3 landing = DismountHelper.findSafeDismountLocation(passenger.getType(), dragon.level(), position, true);
            if (landing != null) {
                return landing;
            }
        }

        return dragon.position();
    }

    private record RestoringRider(ServerPlayer player, DragonBaseEntity dragon) {
    }
}
