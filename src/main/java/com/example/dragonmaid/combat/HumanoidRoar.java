package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import com.iafenvoy.iceandfire.registry.IafItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

public final class HumanoidRoar {
    private static final String NEXT_ROAR = "DragonMaidNextRoar";

    private HumanoidRoar() {
    }

    public static boolean isRoaring(DragonBaseEntity dragon) {
        return (dragon.getAnimation() == DragonBaseEntity.ANIMATION_ROAR || dragon.getAnimation() == DragonBaseEntity.ANIMATION_EPIC_ROAR)
            && dragon.getAnimationTick() < 20;
    }

    public static void tryRoar(DragonBaseEntity dragon) {
        if (dragon.level() instanceof ServerLevel serverlevel
            && DragonForms.isHumanoid(dragon)
            && !TailSweepSync.isBusy(dragon)
            && dragon.isTame()
            && dragon.isAlive()
            && !dragon.isModelDead()
            && dragon.getDragonStage() >= 4
            && !dragon.isOrderedToSit()
            && !dragon.isSleeping()
            && !dragon.isPassenger()
            && !dragon.isNoAi()
            && !GorgonEntity.isStoneMob(dragon)) {
            long j = serverlevel.getGameTime();
            if (dragon.getPersistentData().getLong("DragonMaidNextRoar") > j) {
                return;
            }

            LivingEntity livingentity = dragon.getTarget();
            double d0 = dragon.getDragonStage() >= 5 ? 16.0 : 12.0;
            if (livingentity != null
                && HumanoidTargetGoal.isEnemy(dragon, livingentity)
                && !(dragon.distanceToSqr(livingentity) > d0 * d0)
                && dragon.hasLineOfSight(livingentity)) {
                dragon.getPersistentData().putLong("DragonMaidNextRoar", j + (long)((Integer)DragonMaidConfig.ROAR_COOLDOWN.get()).intValue());
                dragon.setAnimation(dragon.getDragonStage() >= 5 ? DragonBaseEntity.ANIMATION_EPIC_ROAR : DragonBaseEntity.ANIMATION_ROAR);
                dragon.setAnimationTick(0);
                dragon.playSound(dragon.getRoarSound(), 1.5F, dragon.getDragonStage() >= 5 ? 0.85F : 1.0F);
                int i = dragon.getDragonStage() >= 5 ? 300 : 200;

                for (LivingEntity livingentity1 : serverlevel.getEntitiesOfClass(
                    LivingEntity.class, dragon.getBoundingBox().inflate(d0), e -> e != dragon && e.isAlive() && dragon.distanceToSqr(e) <= d0 * d0 && dragon.hasLineOfSight(e)
                )) {
                    if (isAlly(dragon, livingentity1)) {
                        livingentity1.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, i, 0));
                    } else if (HumanoidTargetGoal.isEnemy(dragon, livingentity1)) {
                        if (livingentity1 instanceof DragonBaseEntity) {
                            DragonBaseEntity entitydragonbase = (DragonBaseEntity)livingentity1;
                            if (entitydragonbase.getDragonStage() >= dragon.getDragonStage()) {
                                continue;
                            }
                        }

                        if (!livingentity1.getItemBySlot(EquipmentSlot.HEAD).is((Item)IafItems.EARPLUGS.get())) {
                            livingentity1.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, i, 0));
                        }
                    }
                }

                Vec3 vec3 = dragon.getEyePosition();
                serverlevel.sendParticles(ParticleTypes.POOF, vec3.x, vec3.y, vec3.z, 16, 0.35, 0.2, 0.35, 0.12);
                return;
            }

            return;
        }
    }

    private static boolean isAlly(DragonBaseEntity dragon, LivingEntity living) {
        return CompanionRelations.isAlly(dragon, living);
    }
}
