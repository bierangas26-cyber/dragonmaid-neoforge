package com.example.dragonmaid.client;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidGroundMount;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.movement.HumanoidFlight;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

final class HumanoidMotion {
    private static final Map<DragonBaseEntity, LocomotionMotion> MOTION = new WeakHashMap<>();

    private HumanoidMotion() {
    }

    static boolean groundMotion(DragonBaseEntity dragon) {
        return dragon.onGround()
            && !dragon.isInWaterOrBubble()
            && !dragon.isInLava()
            && !dragon.isPassenger()
            && !DragonForms.isSeated(dragon)
            && !dragon.isSleeping()
            && !dragon.isModelDead()
            && dragon.getPose() != Pose.CROUCHING;
    }

    static void endTick(Level level) {
        Iterator<Entry<DragonBaseEntity, LocomotionMotion>> iterator = MOTION.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<DragonBaseEntity, LocomotionMotion> entry = iterator.next();
            DragonBaseEntity entitydragonbase = entry.getKey();
            if (!entitydragonbase.isRemoved() && entitydragonbase.level() == level && DragonForms.isHumanoid(entitydragonbase)) {
                update(entitydragonbase, entry.getValue());
            } else {
                iterator.remove();
            }
        }
    }

    static LocomotionMotion get(DragonBaseEntity dragon) {
        LocomotionMotion locomotionmotion = MOTION.computeIfAbsent(dragon, ignored -> new LocomotionMotion());
        update(dragon, locomotionmotion);
        return locomotionmotion;
    }

    private static void update(DragonBaseEntity dragon, LocomotionMotion motion) {
        boolean flag = ClientTailSweepAnimations.ownsPose(dragon);
        // 1.2.7: 飞行时不走地面步态；掉落时间线只在「可能下落」时推进。
        boolean flying = HumanoidFlight.airborne(dragon);
        boolean flag1 = !flying && groundMotion(dragon);
        boolean canFall = !flying
            && !flag
            && !dragon.isPassenger()
            && !HumanoidGroundMount.active(dragon)
            && !dragon.isSleeping()
            && !dragon.isModelDead()
            && !HumanoidLife.isInjured(dragon)
            && !dragon.isInWaterOrBubble()
            && !dragon.isInLava()
            && !dragon.onClimbable()
            && !dragon.isNoGravity()
            && !dragon.isFlying()
            && !dragon.isHovering();
        motion.update(
            dragon.tickCount,
            dragon.getX(),
            dragon.getZ(),
            dragon.yBodyRot,
            DragonForms.modelScale(dragon),
            !flag && flag1,
            !flag && (flag1 || HumanoidWater.active(dragon))
        );
        motion.falling().update(dragon, motion.generation(), canFall);
        if (flag) {
            motion.restartGaitAfterAction();
        }
    }
}
