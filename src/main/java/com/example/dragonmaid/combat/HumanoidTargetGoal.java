package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class HumanoidTargetGoal extends Goal {
    private static final double CLOSE_THREAT_RANGE_SQR = 16.0;
    private static final int RECENT_HIT_TICKS = 40;
    private static final int ASSIST_TICKS = 100;
    private final DragonBaseEntity dragon;
    private final GuardTargetProgress progress = new GuardTargetProgress();

    public HumanoidTargetGoal(DragonBaseEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    public boolean canUse() {
        return DragonForms.isHumanoid(this.dragon);
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void stop() {
        this.dragon.setTarget(null);
        this.progress.reset();
    }

    public static boolean isEnemy(DragonBaseEntity dragon, LivingEntity entity) {
        if (entity instanceof DragonBaseEntity entitydragonbase && entitydragonbase.isModelDead()) {
            return false;
        }

        if (entity != dragon && entity.isAlive() && entity.level() == dragon.level() && !CompanionRelations.isAlly(dragon, entity)) {
            LivingEntity livingentity = dragon.getOwner();
            if (entity instanceof Player player
                && (
                    player.isCreative()
                        || player.isSpectator()
                        || dragon.getServer() == null
                        || !dragon.getServer().isPvpAllowed()
                        || livingentity instanceof Player player1 && !player1.canHarmPlayer(player)
                )) {
                return false;
            }

            boolean flag = threatens(entity, dragon)
                || livingentity != null && (threatens(entity, livingentity) || recentlyAttacked(livingentity, entity))
                || CompanionDefense.isThreat(dragon, entity);
            return !(entity instanceof Player) && CompanionRelations.protectedOwners(entity).isEmpty()
                ? entity instanceof Enemy || WildDragonCombat.isHostileWildDragon(dragon, entity) || flag
                : flag;
        } else {
            return false;
        }
    }

    private static boolean recent(LivingEntity actor, int timestamp) {
        int i = actor.tickCount - timestamp;
        return i >= 0 && i <= 100;
    }

    private static boolean recentlyAttacked(LivingEntity actor, LivingEntity target) {
        return actor.getLastHurtMob() == target && recent(actor, actor.getLastHurtMobTimestamp());
    }

    private static boolean threatens(LivingEntity attacker, LivingEntity victim) {
        return attacker instanceof Mob mob && mob.getTarget() == victim || victim.getLastHurtByMob() == attacker && recent(victim, victim.getLastHurtByMobTimestamp());
    }

    private boolean canSelect(LivingEntity candidate) {
        return candidate != null
            && isEnemy(this.dragon, candidate)
            && GuardEscort.reachableTarget(this.dragon, candidate)
            && (GuardEscort.owner(this.dragon) != null || WildDragonCombat.withinTargetRange(this.dragon, candidate))
            && this.dragon.hasLineOfSight(candidate)
            && this.mayRetry(candidate);
    }

    private boolean attackOpportunity(LivingEntity target) {
        return BreathProfile.forDragon(this.dragon).canHit(this.dragon, target)
            || this.dragon.distanceToSqr(target) <= Math.pow(HumanoidCombat.clawReach(this.dragon), 2.0) && this.dragon.hasLineOfSight(target);
    }

    private GuardTargetProgress.ThreatState threats(LivingEntity candidate) {
        LivingEntity livingentity = this.dragon.getOwner();
        int i = this.dragon.tickCount - this.dragon.getLastHurtByMobTimestamp();
        int j = candidate == this.dragon.getLastHurtByMob() && i >= 0 && i <= 40 ? this.dragon.getLastHurtByMobTimestamp() : -1;
        int k = livingentity != null && candidate == livingentity.getLastHurtByMob() && recent(livingentity, livingentity.getLastHurtByMobTimestamp()) ? livingentity.getLastHurtByMobTimestamp() : -1;
        int l = livingentity != null && candidate == livingentity.getLastHurtMob() && recent(livingentity, livingentity.getLastHurtMobTimestamp()) ? livingentity.getLastHurtMobTimestamp() : -1;
        return new GuardTargetProgress.ThreatState(j, k, l, CompanionDefense.revision(this.dragon, candidate));
    }

    private boolean mayRetry(LivingEntity candidate) {
        return !this.progress.isDeferred(candidate.getUUID(), this.dragon.tickCount)
            ? true
            : this.progress.maySelect(candidate.getUUID(), this.dragon.tickCount, this.attackOpportunity(candidate), this.threats(candidate));
    }

    private void checkGuardProgress(LivingEntity owner) {
        if (owner == null) {
            this.progress.reset();
        } else {
            LivingEntity livingentity = this.dragon.getTarget();
            if (livingentity == null) {
                this.progress.forgetTarget();
            } else if (!this.mayRetry(livingentity)) {
                this.dragon.setTarget(null);
                this.progress.forgetTarget();
            } else if (this.dragon.tickCount % 10 == 0) {
                if (this.progress
                    .shouldAbandon(
                        livingentity.getUUID(),
                        this.dragon.tickCount,
                        this.dragon.position(),
                        this.attackOpportunity(livingentity),
                        !this.dragon.getNavigation().isDone()
                    )) {
                    this.progress.defer(livingentity.getUUID(), this.dragon.tickCount, this.threats(livingentity));
                    this.dragon.setTarget(null);
                }
            }
        }
    }

    private boolean prioritizeOwnerCombat(LivingEntity owner) {
        if (owner != null && owner.isAlive() && !owner.isSpectator() && owner.level() == this.dragon.level()) {
            LivingEntity livingentity = owner.getLastHurtByMob();
            if (recent(owner, owner.getLastHurtByMobTimestamp()) && this.canSelect(livingentity)) {
                this.dragon.setTarget(livingentity);
                return true;
            } else {
                LivingEntity livingentity1 = this.dragon.getTarget();
                if (this.canSelect(livingentity1) && threatens(livingentity1, owner)) {
                    return true;
                } else {
                    if (this.dragon.tickCount % 5 == 0) {
                        LivingEntity livingentity2 = this.dragon
                            .level()
                            .getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(8.0), e -> threatens(e, owner) && this.canSelect(e))
                            .stream()
                            .min(Comparator.comparingDouble(owner::distanceToSqr))
                            .orElse(null);
                        if (livingentity2 != null) {
                            this.dragon.setTarget(livingentity2);
                            return true;
                        }
                    }

                    LivingEntity livingentity3 = owner.getLastHurtMob();
                    if (recent(owner, owner.getLastHurtMobTimestamp()) && this.canSelect(livingentity3)) {
                        this.dragon.setTarget(livingentity3);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
    }

    private boolean isCloseThreat(LivingEntity candidate) {
        if (candidate != null
            && !(this.dragon.distanceToSqr(candidate) > 16.0)
            && isEnemy(this.dragon, candidate)
            && GuardEscort.reachableTarget(this.dragon, candidate)) {
            boolean flag2;
            label43: {
                if (candidate instanceof Mob mob && mob.getTarget() == this.dragon) {
                    flag2 = true;
                    break label43;
                }

                flag2 = false;
            }

            boolean flag = flag2;
            int i = this.dragon.tickCount - this.dragon.getLastHurtByMobTimestamp();
            boolean flag1 = candidate == this.dragon.getLastHurtByMob() && i >= 0 && i <= 40;
            return (flag || flag1) && this.dragon.hasLineOfSight(candidate) && this.mayRetry(candidate);
        } else {
            return false;
        }
    }

    private boolean prioritizeCloseThreat() {
        if (this.isCloseThreat(this.dragon.getTarget())) {
            return true;
        } else {
            LivingEntity livingentity = this.dragon.getLastHurtByMob();
            if (this.isCloseThreat(livingentity)) {
                this.dragon.setTarget(livingentity);
                return true;
            } else if (this.dragon.tickCount % 5 != 0) {
                return false;
            } else {
                LivingEntity livingentity1 = this.dragon
                    .level()
                    .getEntitiesOfClass(LivingEntity.class, this.dragon.getBoundingBox().inflate(4.0), this::isCloseThreat)
                    .stream()
                    .min(Comparator.comparingDouble(this.dragon::distanceToSqr))
                    .orElse(null);
                if (livingentity1 == null) {
                    return false;
                } else {
                    this.dragon.setTarget(livingentity1);
                    return true;
                }
            }
        }
    }

    public void tick() {
        if (!this.dragon.isModelDead()
            && this.dragon.getCommand() != 1
            && !this.dragon.isOrderedToSit()
            && !this.dragon.isSleeping()
            && !this.dragon.isNoAi()
            && !this.dragon.isPassenger()
            && this.dragon.isAlive()
            && !HumanoidLife.isInjured(this.dragon)
            && !DragonForms.state(this.dragon).dragonmaid$isRecoveryRest()
            && !GorgonEntity.isStoneMob(this.dragon)) {
            LivingEntity livingentity = GuardEscort.owner(this.dragon);
            if (this.dragon.getCommand() == 2 && livingentity == null) {
                this.dragon.setTarget(null);
                this.progress.reset();
            } else {
                LivingEntity livingentity1 = this.dragon.getTarget();
                if (livingentity1 != null
                    && (
                        !isEnemy(this.dragon, livingentity1)
                            || livingentity == null && !WildDragonCombat.withinTargetRange(this.dragon, livingentity1)
                            || !GuardEscort.reachableTarget(this.dragon, livingentity1)
                    )) {
                    this.dragon.setTarget(null);
                }

                this.checkGuardProgress(livingentity);
                if (!this.prioritizeCloseThreat()) {
                    if (!this.prioritizeOwnerCombat(this.dragon.getOwner())) {
                        LivingEntity livingentity2 = CompanionDefense.recentAttacker(this.dragon);
                        if (this.canSelect(livingentity2)) {
                            this.dragon.setTarget(livingentity2);
                        } else if (this.dragon.tickCount % 10 == 0) {
                            if (this.dragon.getTarget() == null) {
                                if (WildDragonCombat.canAcquireTargets(this.dragon)) {
                                    LivingEntity livingentity3 = (LivingEntity)(livingentity == null ? this.dragon : livingentity);
                                    double d0 = WildDragonCombat.targetRange();
                                    LivingEntity livingentity4 = this.dragon
                                        .level()
                                        .getEntitiesOfClass(
                                            DragonBaseEntity.class,
                                            livingentity3.getBoundingBox().inflate(d0),
                                            candidate -> WildDragonCombat.isHostileWildDragon(this.dragon, candidate)
                                                    && WildDragonCombat.distanceToSqr(this.dragon, livingentity3, candidate) <= d0 * d0
                                                    && this.canSelect(candidate)
                                        )
                                        .stream()
                                        .min(Comparator.comparingDouble(candidate -> WildDragonCombat.distanceToSqr(this.dragon, this.dragon, candidate)))
                                        .orElse(null);
                                    if (livingentity4 != null) {
                                        this.dragon.setTarget(livingentity4);
                                        return;
                                    }
                                }

                                if (livingentity != null) {
                                    if (this.dragon.getTarget() != null) {
                                        return;
                                    }

                                    double d1 = (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get() + BreathProfile.forDragon(this.dragon).range();
                                    this.dragon
                                        .setTarget(
                                            this.dragon
                                                .level()
                                                .getEntitiesOfClass(
                                                    LivingEntity.class,
                                                    livingentity.getBoundingBox().inflate(d1),
                                                    e -> isEnemy(this.dragon, e)
                                                            && GuardEscort.reachableTarget(this.dragon, e)
                                                            && this.dragon.hasLineOfSight(e)
                                                            && this.mayRetry(e)
                                                )
                                                .stream()
                                                .min(Comparator.comparingDouble(this.dragon::distanceToSqr))
                                                .orElse(null)
                                        );
                                } else {
                                    LivingEntity livingentity5 = this.dragon.getLastHurtByMob();
                                    if (livingentity5 != null
                                        && this.dragon.tickCount - this.dragon.getLastHurtByMobTimestamp() < 200
                                        && isEnemy(this.dragon, livingentity5)
                                        && (
                                            WildDragonCombat.isHostileWildDragon(this.dragon, livingentity5)
                                                ? WildDragonCombat.withinTargetRange(this.dragon, livingentity5)
                                                : this.dragon.distanceToSqr(livingentity5) <= 400.0
                                        )) {
                                        this.dragon.setTarget(livingentity5);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            this.dragon.setTarget(null);
            this.progress.forgetTarget();
        }
    }
}
