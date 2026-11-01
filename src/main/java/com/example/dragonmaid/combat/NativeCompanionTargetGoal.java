package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;

public final class NativeCompanionTargetGoal extends Goal {
    private final DragonBaseEntity dragon;
    private LivingEntity selected;
    private int nextCompanionScanTick;

    public NativeCompanionTargetGoal(DragonBaseEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    private boolean available() {
        return !DragonForms.isHumanoid(this.dragon)
            && CompanionDefense.canRespond(this.dragon)
            && WildDragonCombat.canAcquireTargets(this.dragon)
            && this.dragon.canMove();
    }

    private boolean eligible(LivingEntity target) {
        return CompanionDefense.canAssist(this.dragon, target);
    }

    public boolean canUse() {
        if (!this.available()) {
            return false;
        } else {
            LivingEntity livingentity = CompanionDefense.recentAttacker(this.dragon);
            LivingEntity livingentity1 = this.dragon.getTarget();
            if (livingentity1 != null && livingentity1 != livingentity && HumanoidTargetGoal.isEnemy(this.dragon, livingentity1)) {
                return false;
            } else if (this.eligible(livingentity) && this.dragon.hasLineOfSight(livingentity)) {
                this.selected = livingentity;
                return true;
            } else {
                LivingEntity livingentity2 = GuardEscort.owner(this.dragon);
                if (livingentity2 != null && this.dragon.tickCount >= this.nextCompanionScanTick) {
                    this.nextCompanionScanTick = this.dragon.tickCount + 10;
                    double d0 = (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get() + BreathProfile.forDragon(this.dragon).range();
                    this.selected = this.dragon
                        .level()
                        .getEntitiesOfClass(
                            LivingEntity.class,
                            livingentity2.getBoundingBox().inflate(d0),
                            candidate -> {
                                if (candidate instanceof Mob mob
                                    && mob.getTarget() != null
                                    && mob.getTarget().isAlive()
                                    && CompanionRelations.protectedOwners(mob.getTarget()).contains(this.dragon.getOwnerUUID())
                                    && CompanionDefense.isThreat(this.dragon, candidate)
                                    && this.eligible(candidate)
                                    && this.dragon.hasLineOfSight(candidate)) {
                                    return true;
                                }

                                return false;
                            }
                        )
                        .stream()
                        .min(Comparator.comparingDouble(this.dragon::distanceToSqr))
                        .orElse(null);
                    return this.selected != null;
                } else {
                    return false;
                }
            }
        }
    }

    public boolean canContinueToUse() {
        if (this.available() && this.eligible(this.selected) && CompanionDefense.isThreat(this.dragon, this.selected)) {
            LivingEntity livingentity = this.dragon.getTarget();
            return livingentity == null || livingentity == this.selected || !HumanoidTargetGoal.isEnemy(this.dragon, livingentity);
        } else {
            return false;
        }
    }

    public void start() {
        if (this.selected.getVehicle() != this.dragon) {
            this.dragon.setTarget(this.selected);
        }
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if (this.selected.getVehicle() != this.dragon && this.dragon.getTarget() == null && this.dragon.hasLineOfSight(this.selected)) {
            this.dragon.setTarget(this.selected);
        }
    }

    public void stop() {
        if (this.selected != null && this.dragon.getTarget() == this.selected && (this.dragon.getCommand() == 1 || !this.eligible(this.selected))) {
            this.dragon.setTarget(null);
        }

        this.selected = null;
    }
}
