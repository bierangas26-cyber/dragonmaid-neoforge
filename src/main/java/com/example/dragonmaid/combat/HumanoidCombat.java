package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.github.alexthe666.iceandfire.entity.props.EntityData;
import com.github.alexthe666.iceandfire.entity.props.EntityDataProvider;
import com.iafenvoy.iceandfire.registry.IafDamageTypes;
import com.iafenvoy.iceandfire.registry.IafSounds;
import com.iafenvoy.iceandfire.registry.IafDamageTypes.CustomEntityDamageSource;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HumanoidCombat {
    private static final Map<DragonBaseEntity, Integer> FORGE_SOUND_TICKS = new WeakHashMap<>();

    private HumanoidCombat() {
    }

    public static void stopBreath(DragonBaseEntity dragon) {
        dragon.setBreathingFire(false);
        if (dragon instanceof LightningDragonEntity entitylightningdragon) {
            entitylightningdragon.setHasLightningTarget(false);
        }
    }

    public static double clawReach(DragonBaseEntity dragon) {
        return switch (dragon.getDragonStage()) {
            case 4 -> 2.3;
            case 5 -> 2.5;
            default -> 2.2;
        };
    }

    public static void claw(DragonBaseEntity dragon, double reach) {
        if (dragon.level() instanceof ServerLevel serverlevel) {
            dragon.swing(InteractionHand.MAIN_HAND);
            Vec3 vec31 = dragon.getTarget() == null ? dragon.getLookAngle() : dragon.getTarget().position().subtract(dragon.position()).normalize();

            for (LivingEntity livingentity : serverlevel.getEntitiesOfClass(
                LivingEntity.class, dragon.getBoundingBox().inflate(reach), e -> HumanoidTargetGoal.isEnemy(dragon, e)
            )) {
                Vec3 vec3 = livingentity.position().subtract(dragon.position());
                if (!(vec3.lengthSqr() > reach * reach)
                    && !(vec3.normalize().dot(vec31) < 0.0)
                    && dragon.hasLineOfSight(livingentity)
                    && livingentity.hurt(serverlevel.damageSources().mobAttack(dragon), (float)dragon.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                    livingentity.knockback(0.5 + (double)dragon.getDragonStage() * 0.1, -vec3.x, -vec3.z);
                }
            }

            serverlevel.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                dragon.getX() + vec31.x,
                dragon.getY() + (double)dragon.getBbHeight() * 0.55,
                dragon.getZ() + vec31.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0
            );
        }
    }

    public static void breath(DragonBaseEntity dragon, LivingEntity target, boolean damageTick) {
        if (dragon.level() instanceof ServerLevel serverlevel) {
            BreathProfile breathprofile = BreathProfile.forDragon(dragon);
            if (!(dragon instanceof LightningDragonEntity)) {
                if (damageTick) {
                    MovingBreaths.emit(dragon, target, breathprofile);
                }
            } else {
                Vec3 vec3 = dragon.getEyePosition();
                Vec3 vec31 = target.getBoundingBox().getCenter().subtract(vec3).normalize();
                Vec3 vec32 = vec3.add(vec31.scale(Math.min(breathprofile.range(), vec3.distanceTo(target.getBoundingBox().getCenter()) + 0.5)));
                Vec3 vec33 = serverlevel.clip(new ClipContext(vec3, vec32, Block.COLLIDER, Fluid.NONE, dragon)).getLocation();
                if (dragon instanceof LightningDragonEntity entitylightningdragon) {
                    if (!dragon.isBreathingFire() || dragon.tickCount % 5 == 0) {
                        dragon.playSound(IafSounds.LIGHTNINGDRAGON_BREATH.get(), 2.0F, 1.0F);
                    }

                    dragon.setBreathingFire(true);
                    entitylightningdragon.setLightningTargetVec((float)vec33.x, (float)vec33.y, (float)vec33.z);
                    entitylightningdragon.setHasLightningTarget(true);
                }

                if (damageTick) {
                    double d0 = 0.35;

                    for (LivingEntity livingentity : serverlevel.getEntitiesOfClass(
                        LivingEntity.class, new AABB(vec3, vec33).inflate(d0), e -> HumanoidTargetGoal.isEnemy(dragon, e)
                    )) {
                        AABB aabb = livingentity.getBoundingBox().inflate(d0);
                        if ((aabb.contains(vec3) || !aabb.clip(vec3, vec33).isEmpty()) && dragon.hasLineOfSight(livingentity)) {
                            damage(dragon, livingentity, breathprofile.stage());
                        }
                    }
                }
            }
        }
    }

    public static boolean damage(DragonBaseEntity dragon, LivingEntity enemy, int stage) {
        return damage(dragon, enemy, stage, breathDamageAmount(dragon, stage));
    }

    public static float breathDamageAmount(DragonBaseEntity dragon, int stage) {
        return (float)stage * (dragon instanceof LightningDragonEntity ? 1.5F : 3.0F) * DragonAccessories.breathDamageMultiplier(dragon);
    }

    public static boolean damage(DragonBaseEntity dragon, LivingEntity enemy, int stage, float amount) {
        if (dragon.level() instanceof ServerLevel && HumanoidTargetGoal.isEnemy(dragon, enemy)) {
            CustomEntityDamageSource customentitydamagesource = dragon instanceof IceDragonEntity
                ? IafDamageTypes.causeDragonIceDamage(dragon)
                : (
                    dragon instanceof LightningDragonEntity
                        ? IafDamageTypes.causeDragonLightningDamage(dragon)
                        : IafDamageTypes.causeDragonFireDamage(dragon)
                );
            if (!enemy.hurt(customentitydamagesource, amount)) {
                return false;
            } else {
                if (dragon instanceof FireDragonEntity) {
                    enemy.igniteForSeconds((float)stage);
                }

                if (dragon instanceof IceDragonEntity) {
                    EntityDataProvider.getCapability(enemy).ifPresent(data -> data.frozenData.setFrozen(enemy, stage * 40));
                    enemy.setTicksFrozen(Math.max(enemy.getTicksFrozen(), enemy.getTicksRequiredToFreeze() + stage * 20));
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stage * 40, 1));
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public static void forgeStream(DragonBaseEntity dragon, Vec3 end) {
        if (dragon.level() instanceof ServerLevel serverlevel) {
            int k = dragon instanceof LightningDragonEntity ? 7 : 5;
            if (dragon.tickCount % k == 0 && !Integer.valueOf(dragon.tickCount).equals(FORGE_SOUND_TICKS.get(dragon))) {
                FORGE_SOUND_TICKS.put(dragon, dragon.tickCount);
                dragon.playSound(
                    dragon instanceof LightningDragonEntity
                        ? IafSounds.LIGHTNINGDRAGON_BREATH.get()
                        : (dragon instanceof IceDragonEntity ? IafSounds.ICEDRAGON_BREATH.get() : IafSounds.FIREDRAGON_BREATH.get()),
                    4.0F,
                    1.0F
                );
            }

            if (dragon instanceof LightningDragonEntity entitylightningdragon) {
                dragon.setBreathingFire(true);
                entitylightningdragon.setLightningTargetVec((float)end.x, (float)end.y, (float)end.z);
                entitylightningdragon.setHasLightningTarget(true);
            } else {
                if (dragon.tickCount % 3 != 0) {
                    return;
                }

                Vec3 vec3 = HumanoidWater.breathOrigin(dragon);
                ParticleOptions particleoptions = dragon instanceof IceDragonEntity ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME;
                int i = Math.max(1, (int)Math.ceil(vec3.distanceTo(end) * 4.0));

                for (int j = 0; j <= i; j++) {
                    Vec3 vec31 = vec3.lerp(end, (double)j / (double)i);
                    serverlevel.sendParticles(particleoptions, vec31.x, vec31.y, vec31.z, 1, 0.035, 0.035, 0.035, 0.001);
                }
            }
        }
    }
}
