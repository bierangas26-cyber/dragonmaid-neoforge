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
      if (entity instanceof DragonBaseEntity targetDragon && targetDragon.isModelDead()) {
         return false;
      }

      if (entity != dragon && entity.isAlive() && entity.level() == dragon.level() && !CompanionRelations.isAlly(dragon, entity)) {
         LivingEntity owner = dragon.getOwner();
         if (entity instanceof Player player
            && (
               player.isCreative()
                  || player.isSpectator()
                  || dragon.getServer() == null
                  || !dragon.getServer().isPvpAllowed()
                  || owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(player)
            )) {
            return false;
         }

         boolean threatened = threatens(entity, dragon)
            || owner != null && (threatens(entity, owner) || recentlyAttacked(owner, entity))
            || CompanionDefense.isThreat(dragon, entity);
         return !(entity instanceof Player) && CompanionRelations.protectedOwners(entity).isEmpty()
            ? entity instanceof Enemy || WildDragonCombat.isHostileWildDragon(dragon, entity) || threatened
            : threatened;
      } else {
         return false;
      }
   }

   private static boolean recent(LivingEntity actor, int timestamp) {
      int age = actor.tickCount - timestamp;
      return age >= 0 && age <= 100;
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
      LivingEntity owner = this.dragon.getOwner();
      int sinceHit = this.dragon.tickCount - this.dragon.getLastHurtByMobTimestamp();
      int selfHit = candidate == this.dragon.getLastHurtByMob() && sinceHit >= 0 && sinceHit <= 40 ? this.dragon.getLastHurtByMobTimestamp() : -1;
      int ownerHit = owner != null && candidate == owner.getLastHurtByMob() && recent(owner, owner.getLastHurtByMobTimestamp()) ? owner.getLastHurtByMobTimestamp() : -1;
      int ownerAttack = owner != null && candidate == owner.getLastHurtMob() && recent(owner, owner.getLastHurtMobTimestamp()) ? owner.getLastHurtMobTimestamp() : -1;
      return new GuardTargetProgress.ThreatState(selfHit, ownerHit, ownerAttack, CompanionDefense.revision(this.dragon, candidate));
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
         LivingEntity target = this.dragon.getTarget();
         if (target == null) {
            this.progress.forgetTarget();
         } else if (!this.mayRetry(target)) {
            this.dragon.setTarget(null);
            this.progress.forgetTarget();
         } else if (this.dragon.tickCount % 10 == 0) {
            if (this.progress
               .shouldAbandon(
                  target.getUUID(), this.dragon.tickCount, this.dragon.position(), this.attackOpportunity(target), !this.dragon.getNavigation().isDone()
               )) {
               this.progress.defer(target.getUUID(), this.dragon.tickCount, this.threats(target));
               this.dragon.setTarget(null);
            }
         }
      }
   }

   private boolean prioritizeOwnerCombat(LivingEntity owner) {
      if (owner != null && owner.isAlive() && !owner.isSpectator() && owner.level() == this.dragon.level()) {
         LivingEntity attacker = owner.getLastHurtByMob();
         if (recent(owner, owner.getLastHurtByMobTimestamp()) && this.canSelect(attacker)) {
            this.dragon.setTarget(attacker);
            return true;
         } else {
            LivingEntity current = this.dragon.getTarget();
            if (this.canSelect(current) && threatens(current, owner)) {
               return true;
            } else {
               if (this.dragon.tickCount % 5 == 0) {
                  LivingEntity threat = this.dragon
                     .level()
                     .getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(8.0), e -> threatens(e, owner) && this.canSelect(e))
                     .stream()
                     .min(Comparator.comparingDouble(owner::distanceToSqr))
                     .orElse(null);
                  if (threat != null) {
                     this.dragon.setTarget(threat);
                     return true;
                  }
               }

               LivingEntity victim = owner.getLastHurtMob();
               if (recent(owner, owner.getLastHurtMobTimestamp()) && this.canSelect(victim)) {
                  this.dragon.setTarget(victim);
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
         boolean var10000;
         label43: {
            if (candidate instanceof Mob mob && mob.getTarget() == this.dragon) {
               var10000 = true;
               break label43;
            }

            var10000 = false;
         }

         boolean attacking = var10000;
         int sinceHit = this.dragon.tickCount - this.dragon.getLastHurtByMobTimestamp();
         boolean justHit = candidate == this.dragon.getLastHurtByMob() && sinceHit >= 0 && sinceHit <= 40;
         return (attacking || justHit) && this.dragon.hasLineOfSight(candidate) && this.mayRetry(candidate);
      } else {
         return false;
      }
   }

   private boolean prioritizeCloseThreat() {
      if (this.isCloseThreat(this.dragon.getTarget())) {
         return true;
      } else {
         LivingEntity attacker = this.dragon.getLastHurtByMob();
         if (this.isCloseThreat(attacker)) {
            this.dragon.setTarget(attacker);
            return true;
         } else if (this.dragon.tickCount % 5 != 0) {
            return false;
         } else {
            LivingEntity threat = this.dragon
               .level()
               .getEntitiesOfClass(LivingEntity.class, this.dragon.getBoundingBox().inflate(4.0), this::isCloseThreat)
               .stream()
               .min(Comparator.comparingDouble(this.dragon::distanceToSqr))
               .orElse(null);
            if (threat == null) {
               return false;
            } else {
               this.dragon.setTarget(threat);
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
         LivingEntity owner = GuardEscort.owner(this.dragon);
         if (this.dragon.getCommand() == 2 && owner == null) {
            this.dragon.setTarget(null);
            this.progress.reset();
         } else {
            LivingEntity current = this.dragon.getTarget();
            if (current != null
               && (
                  !isEnemy(this.dragon, current)
                     || owner == null && !WildDragonCombat.withinTargetRange(this.dragon, current)
                     || !GuardEscort.reachableTarget(this.dragon, current)
               )) {
               this.dragon.setTarget(null);
            }

            this.checkGuardProgress(owner);
            if (!this.prioritizeCloseThreat()) {
               if (!this.prioritizeOwnerCombat(this.dragon.getOwner())) {
                  LivingEntity companionAttacker = CompanionDefense.recentAttacker(this.dragon);
                  if (this.canSelect(companionAttacker)) {
                     this.dragon.setTarget(companionAttacker);
                  } else if (this.dragon.tickCount % 10 == 0) {
                     if (this.dragon.getTarget() == null) {
                        if (owner == null && this.dragon.getCommand() == 0 && CompanionDefense.canRespond(this.dragon)) {
                           LivingEntity threat = this.dragon
                              .level()
                              .getEntitiesOfClass(
                                 LivingEntity.class, this.dragon.getBoundingBox().inflate(24.0), e -> CompanionDefense.isThreat(this.dragon, e) && this.canSelect(e)
                              )
                              .stream()
                              .min(Comparator.comparingDouble(this.dragon::distanceToSqr))
                              .orElse(null);
                           if (threat != null) {
                              this.dragon.setTarget(threat);
                              return;
                           }
                        }

                        if (WildDragonCombat.canAcquireTargets(this.dragon)) {
                           LivingEntity origin = (LivingEntity)(owner == null ? this.dragon : owner);
                           double range = WildDragonCombat.targetRange();
                           LivingEntity wild = this.dragon
                              .level()
                              .getEntitiesOfClass(
                                 DragonBaseEntity.class,
                                 origin.getBoundingBox().inflate(range),
                                 candidate -> WildDragonCombat.isHostileWildDragon(this.dragon, candidate)
                                       && WildDragonCombat.distanceToSqr(this.dragon, origin, candidate) <= range * range
                                       && this.canSelect(candidate)
                              )
                              .stream()
                              .min(Comparator.comparingDouble(candidate -> WildDragonCombat.distanceToSqr(this.dragon, this.dragon, candidate)))
                              .orElse(null);
                           if (wild != null) {
                              this.dragon.setTarget(wild);
                              return;
                           }
                        }

                        if (owner != null) {
                           if (this.dragon.getTarget() != null) {
                              return;
                           }

                           double searchRange = (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get() + BreathProfile.forDragon(this.dragon).range();
                           this.dragon
                              .setTarget(
                                 this.dragon
                                    .level()
                                    .getEntitiesOfClass(
                                       LivingEntity.class,
                                       owner.getBoundingBox().inflate(searchRange),
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
                           if (this.dragon.getCommand() == 0 && DragonForms.state(this.dragon).dragonmaid$getKnowledgeLevel() >= 1) {
                              this.dragon
                                 .setTarget(
                                    this.dragon
                                       .level()
                                       .getEntitiesOfClass(Mob.class, this.dragon.getBoundingBox().inflate(24.0), e -> e instanceof Enemy && this.canSelect(e))
                                       .stream()
                                       .min(Comparator.comparingDouble(this.dragon::distanceToSqr))
                                       .orElse(null)
                                 );
                              if (this.dragon.getTarget() != null) {
                                 return;
                              }
                           }

                           LivingEntity attacker = this.dragon.getLastHurtByMob();
                           if (attacker != null
                              && this.dragon.tickCount - this.dragon.getLastHurtByMobTimestamp() < 200
                              && isEnemy(this.dragon, attacker)
                              && (
                                 WildDragonCombat.isHostileWildDragon(this.dragon, attacker)
                                    ? WildDragonCombat.withinTargetRange(this.dragon, attacker)
                                    : this.dragon.distanceToSqr(attacker) <= 400.0
                              )) {
                              this.dragon.setTarget(attacker);
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
