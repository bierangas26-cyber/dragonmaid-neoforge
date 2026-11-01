package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public record BreathProfile(int stage, int duration, double range, double halfAngle, double speed) {
    public static final int EMIT_INTERVAL = 14;

    public static BreathProfile forDragon(DragonBaseEntity dragon) {
        BreathProfile breathprofile = forStage(dragon.getDragonStage());
        return dragon instanceof LightningDragonEntity ? breathprofile : fireIceForStage(breathprofile.stage);
    }

    public static BreathProfile fireIceForStage(int stage) {
        int i = Mth.clamp(stage, 3, 5);
        int j = i == 3 ? 1 : 15;
        return new BreathProfile(
            i,
            j,
            (Double)DragonMaidConfig.FIRE_ICE_BREATH_RANGE.get()
                * (14.0 + (double)(i - 3) * 4.0)
                / 22.0
                * (Double)DragonMaidConfig.HUMANOID_BREATH_RANGE_MULTIPLIER.get(),
            Math.toRadians((double)(6 + (i - 3) * 3)),
            (Double)DragonMaidConfig.BREATH_SPEED.get()
        );
    }

    public boolean inRange(DragonBaseEntity dragon, LivingEntity target) {
        return HumanoidWater.breathOrigin(dragon).distanceToSqr(this.aimContact(dragon, target)) <= this.range * this.range;
    }

    private Vec3 aimContact(DragonBaseEntity dragon, LivingEntity target) {
        Vec3 vec3 = HumanoidWater.breathOrigin(dragon);
        AABB aabb = target.getBoundingBox();
        Vec3 vec31 = aabb.getCenter();
        if (!WildDragonCombat.isHostileWildDragon(dragon, target)) {
            return vec31;
        } else {
            return aabb.contains(vec3) ? vec3 : aabb.clip(vec3, vec31).orElse(vec31);
        }
    }

    public boolean canHit(DragonBaseEntity dragon, LivingEntity target) {
        Vec3 vec3 = HumanoidWater.breathOrigin(dragon);
        Vec3 vec31 = this.aimContact(dragon, target);
        return vec3.distanceToSqr(vec31) <= this.range * this.range
            && dragon.level().clip(new ClipContext(vec3, target.getBoundingBox().getCenter(), Block.COLLIDER, Fluid.NONE, dragon)).getType() == Type.MISS;
    }

    public static BreathProfile forStage(int stage) {
        int i = Mth.clamp(stage, 3, 5);
        return new BreathProfile(
            i,
            12 + (i - 3) * 6,
            (Double)DragonMaidConfig.BREATH_RANGE.get() * (0.625 + (double)(i - 3) * 0.1875) * (Double)DragonMaidConfig.HUMANOID_BREATH_RANGE_MULTIPLIER.get(),
            Math.toRadians((double)(12 + (i - 3) * 6)),
            (Double)DragonMaidConfig.BREATH_SPEED.get()
        );
    }
}
