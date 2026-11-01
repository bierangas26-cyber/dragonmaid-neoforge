package com.example.dragonmaid.combat;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DragonChargeEntity;
import com.iafenvoy.iceandfire.entity.FireDragonChargeEntity;
import com.iafenvoy.iceandfire.entity.IceDragonChargeEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonChargeEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3f;

public final class SafeDragonCharges {
    private static final String SAFE = "DragonMaidSafeCharge";
    private static final String AGE = "DragonMaidChargeAge";
    public static final int MAX_AGE_TICKS = 160;

    private SafeDragonCharges() {
    }

    public static boolean isSafe(DragonChargeEntity charge) {
        return charge.getPersistentData().getBoolean("DragonMaidSafeCharge");
    }

    public static DragonChargeEntity launch(DragonBaseEntity dragon, LivingEntity target) {
        if (dragon.level() instanceof ServerLevel serverlevel
            && DragonForms.isHumanoid(dragon)
            && dragon.getDragonStage() >= 5
            && HumanoidTargetGoal.isEnemy(dragon, target)
            && dragon.hasLineOfSight(target)) {
            Vec3 vec31 = dragon.getEyePosition();
            Vec3 vec3 = target.getBoundingBox().getCenter().subtract(vec31).normalize();
            DragonChargeEntity entitydragoncharge = (DragonChargeEntity)(dragon instanceof IceDragonEntity
                ? new IceDragonChargeEntity(
                    (EntityType)IafEntities.ICE_DRAGON_CHARGE.get(), serverlevel, dragon, vec3.x, vec3.y, vec3.z
                )
                : (
                    dragon instanceof LightningDragonEntity
                        ? new LightningDragonChargeEntity(
                            (EntityType)IafEntities.LIGHTNING_DRAGON_CHARGE.get(), serverlevel, dragon, vec3.x, vec3.y, vec3.z
                        )
                        : new FireDragonChargeEntity(
                            (EntityType)IafEntities.FIRE_DRAGON_CHARGE.get(), serverlevel, dragon, vec3.x, vec3.y, vec3.z
                        )
                ));
            entitydragoncharge.setPos(vec31);
            entitydragoncharge.setDeltaMovement(vec3.scale(0.65));
            entitydragoncharge.getPersistentData().putBoolean("DragonMaidSafeCharge", true);
            entitydragoncharge.getPersistentData().putInt("DragonMaidChargeStage", dragon.getDragonStage());
            if (!serverlevel.addFreshEntity(entitydragoncharge)) {
                return null;
            }

            BreathAnimationSync.shot(dragon, 2);
            return entitydragoncharge;
        }

        return null;
    }

    public static void age(DragonChargeEntity charge) {
        if (!charge.level().isClientSide && isSafe(charge)) {
            int i = charge.getPersistentData().getInt("DragonMaidChargeAge") + 1;
            charge.getPersistentData().putInt("DragonMaidChargeAge", i);
            if (i >= 160) {
                charge.discard();
            }
        }
    }

    public static void impact(DragonChargeEntity charge, HitResult hit) {
        if (charge.level() instanceof ServerLevel serverlevel && hit.getType() != Type.MISS) {
            Vec3 vec3 = hit.getLocation().subtract(charge.getDeltaMovement().normalize().scale(0.05));
            if (charge.getOwner() instanceof DragonBaseEntity entitydragonbase && entitydragonbase.isAlive() && !entitydragonbase.isModelDead()) {
                int i = charge.getPersistentData().getInt("DragonMaidChargeStage");

                for (LivingEntity livingentity : serverlevel.getEntitiesOfClass(
                    LivingEntity.class, new AABB(vec3, vec3).inflate(3.0), entity -> HumanoidTargetGoal.isEnemy(entitydragonbase, entity)
                )) {
                    if (!(livingentity.getBoundingBox().getCenter().distanceToSqr(vec3) > 9.0)
                        && serverlevel.clip(new ClipContext(vec3, livingentity.getBoundingBox().getCenter(), Block.COLLIDER, Fluid.NONE, charge)).getType()
                            == Type.MISS) {
                        HumanoidCombat.damage(entitydragonbase, livingentity, i, charge.getDamage() * (float)i);
                    }
                }
            }

            impactParticles(serverlevel, charge, vec3);
            serverlevel.playSound(null, vec3.x, vec3.y, vec3.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.95F);
            charge.discard();
            return;
        }
    }

    private static void impactParticles(ServerLevel level, DragonChargeEntity charge, Vec3 center) {
        boolean flag = charge instanceof IceDragonChargeEntity;
        boolean flag1 = charge instanceof LightningDragonChargeEntity;
        ParticleOptions particleoptions = flag ? ParticleTypes.SNOWFLAKE : (flag1 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.FLAME);
        ParticleOptions particleoptions1 = (ParticleOptions)(flag
            ? new DustParticleOptions(new Vector3f(0.45F, 0.8F, 1.0F), 1.8F)
            : (flag1 ? new DustParticleOptions(new Vector3f(0.7F, 0.4F, 1.0F), 1.8F) : ParticleTypes.LARGE_SMOKE));
        level.sendParticles(particleoptions, center.x, center.y, center.z, 48, 1.1, 1.1, 1.1, 0.2);
        level.sendParticles(particleoptions1, center.x, center.y, center.z, 24, 1.4, 1.4, 1.4, 0.06);

        for (int i = 0; i < 48; i++) {
            double d0 = 1.0 - 2.0 * ((double)i + 0.5) / 48.0;
            double d1 = Math.sqrt(1.0 - d0 * d0);
            double d2 = (double)i * 2.399963229728653;
            double d3 = Math.cos(d2) * d1;
            double d4 = Math.sin(d2) * d1;
            level.sendParticles(
                particleoptions, center.x + d3 * 1.5, center.y + d0 * 1.5, center.z + d4 * 1.5, 0, d3 * 0.4, d0 * 0.4, d4 * 0.4, 1.0
            );
        }

        for (int j = 0; j < 40; j++) {
            double d5 = (double)j * Math.PI * 2.0 / 40.0;
            level.sendParticles(
                particleoptions,
                center.x + Math.cos(d5) * 2.6,
                center.y,
                center.z + Math.sin(d5) * 2.6,
                0,
                Math.cos(d5) * 0.18,
                0.03,
                Math.sin(d5) * 0.18,
                1.0
            );
        }

        if (flag1) {
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 16, 0.7, 0.7, 0.7, 0.25);
        }
    }
}
