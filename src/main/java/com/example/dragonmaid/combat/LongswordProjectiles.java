package com.example.dragonmaid.combat;

import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.entity.DragonTridentFlight;
import com.example.dragonmaid.item.BladeEnchantments;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DragonChargeEntity;
import com.iafenvoy.iceandfire.entity.HydraBreathEntity;
import com.iafenvoy.iceandfire.entity.MultipartPartEntity;
import com.iafenvoy.iceandfire.entity.SeaSerpentBubblesEntity;
import com.iafenvoy.iceandfire.registry.IafDamageTypes;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class LongswordProjectiles {
   private static final String REFLECTED = "DragonMaidWingGuardReflected";
   private static final String DEFLECTOR = "DragonMaidLongswordDeflector";
   private static final String CHARGE_DAMAGE = "DragonMaidDeflectedChargeDamage";
   private static final float WING_GUARD_CHANCE = 0.5F;
   private static final Map<Projectile, LongswordProjectiles.DeflectionAttempt> ATTEMPTS = new WeakHashMap<>();

   private LongswordProjectiles() {
   }

   static boolean isProjectile(DamageSource source) {
      return !source.is(DamageTypeTags.IS_EXPLOSION) && (source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile);
   }

   static void observeRangedDamage(DragonBaseEntity dragon, DamageSource source) {
      if (source.getEntity() instanceof LivingEntity attacker
         && (
            isProjectile(source)
               || source.is(DamageTypes.SONIC_BOOM)
               || source.is(DamageTypes.INDIRECT_MAGIC)
               || source.is(IafDamageTypes.DRAGON_FIRE_TYPE)
               || source.is(IafDamageTypes.DRAGON_ICE_TYPE)
               || source.is(IafDamageTypes.DRAGON_LIGHTNING_TYPE)
         )) {
         recordShot(dragon, attacker);
      }
   }

   private static void recordShot(DragonBaseEntity dragon, LivingEntity attacker) {
      if (SpecialWeapons.wingGuard(dragon) && SpecialWeapons.trained(dragon) && canReturnTo(dragon, attacker)) {
         DragonShield.of(dragon).recordRangedHit(dragon.level().getGameTime());
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void impact(ProjectileImpactEvent event) {
      Projectile shot = event.getProjectile();
      if (shot.level() instanceof ServerLevel level
         && !shot.isRemoved()
         && !shot.noPhysics
         && !(shot instanceof FishingHook)
         && event.getRayTraceResult() instanceof EntityHitResult hit) {
         Entity struck = hit.getEntity();
         if (struck instanceof MultipartPartEntity part) {
            struck = part.getParent();
         }

         if (struck instanceof LivingEntity defender) {
            LivingEntity reflector = reflectedBy(level, shot);
            if (reflector != null && !canReturnTo(reflector, defender)) {
               event.setCanceled(true);
               return;
            }

            if (reflector != null && hitWithReturnedIafProjectile(shot, hit.getEntity(), defender, reflector)) {
               event.setCanceled(true);
               return;
            }

            if (!BladeCombat.longswordGuardActive(defender)) {
               return;
            }

            boolean var10000;
            label66: {
               if (defender instanceof DragonBaseEntity dragon && SpecialWeapons.wingGuard(dragon)) {
                  var10000 = true;
                  break label66;
               }

               var10000 = false;
            }

            boolean wingGuard = var10000;
            Vec3 motion = shot.getDeltaMovement();
            Vec3 incoming = new Vec3(-motion.x, 0.0, -motion.z);
            if (incoming.lengthSqr() < 1.0E-6) {
               incoming = new Vec3(shot.getX() - defender.getX(), 0.0, shot.getZ() - defender.getZ());
            }

            if (!BladeCombat.facesIncoming(defender, incoming.normalize())) {
               return;
            }

            if (wingGuard && defender instanceof DragonBaseEntity dragon && shot.getOwner() instanceof LivingEntity attacker) {
               recordShot(dragon, attacker);
            }

            if (tryDeflect(level, shot, defender, wingGuard)) {
               event.setCanceled(true);
               return;
            }

            if (!wingGuard) {
               return;
            }

            event.setCanceled(true);
            if (shot instanceof AbstractArrow) {
               Vec3 away = incoming.normalize();
               shot.setDeltaMovement(away.x * 0.15 + away.z * 0.25, -0.2, away.z * 0.15 - away.x * 0.25);
               shot.hasImpulse = true;
               shot.hurtMarked = true;
            } else {
               shot.discard();
            }

            blocked(defender);
            return;
         }

         return;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void directAttack(LivingIncomingDamageEvent event) {
      DamageSource source = event.getSource();
      LivingEntity defender = event.getEntity();
      if (!(event.getAmount() <= 0.0F) && defender.level() instanceof ServerLevel level) {
         Projectile shot = source.getDirectEntity() instanceof Projectile projectile ? projectile : null;
         LivingEntity reflector = shot == null ? null : reflectedBy(level, shot);
         if (reflector != null && !canReturnTo(reflector, defender)) {
            event.setCanceled(true);
         } else if (isProjectile(source)
            && !source.is(DamageTypeTags.BYPASSES_SHIELD)
            && BladeCombat.longswordGuardActive(defender)
            && BladeCombat.facesIncoming(defender, BladeCombat.incomingDirection(defender, source))) {
            boolean var13;
            label61: {
               if (defender instanceof DragonBaseEntity dragon && SpecialWeapons.wingGuard(dragon)) {
                  var13 = true;
                  break label61;
               }

               var13 = false;
            }

            boolean wingGuard = var13;
            boolean reflected = shot != null && tryDeflect(level, shot, defender, wingGuard);
            if (reflected || wingGuard) {
               if (defender instanceof DragonBaseEntity dragon) {
                  observeRangedDamage(dragon, source);
               }

               if (!reflected) {
                  blocked(defender);
               }

               event.setCanceled(true);
            }
         }
      }
   }

   private static boolean tryDeflect(ServerLevel level, Projectile shot, LivingEntity defender, boolean wingGuard) {
      if (!shot.isRemoved()
         && !shot.noPhysics
         && !(shot instanceof FishingHook)
         && !shot.getPersistentData().getBoolean("DragonMaidWingGuardReflected")
         && shot.getOwner() instanceof LivingEntity attacker
         && canReturnTo(defender, attacker)) {
         float chance = BladeEnchantments.deflectionChance(defender.getUseItem()) + (wingGuard ? 0.5F : 0.0F);
         if (chance <= 0.0F) {
            return false;
         } else {
            LongswordProjectiles.DeflectionAttempt last = ATTEMPTS.get(shot);
            if (last != null && last.tick() == level.getGameTime() && last.defender().equals(defender.getUUID())) {
               return false;
            } else {
               ATTEMPTS.put(shot, new LongswordProjectiles.DeflectionAttempt(defender.getUUID(), level.getGameTime()));
               if (!(defender.getRandom().nextFloat() >= chance) && reflect(level, shot, defender, attacker)) {
                  markReflected(shot, defender);
                  if (!defender.isSilent()) {
                     level.playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, defender.getSoundSource(), 1.0F, 1.35F);
                  }

                  playGuardImpact(defender);
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

   public static boolean canReturnTo(LivingEntity defender, LivingEntity attacker) {
      if (attacker != defender && attacker.isAlive() && !attacker.isSpectator() && attacker.level() == defender.level() && !defender.isAlliedTo(attacker)) {
         Player owner = defender instanceof Player player ? player : null;
         if (defender instanceof DragonBaseEntity dragon) {
            if (CompanionRelations.isAlly(dragon, attacker)) {
               return false;
            }

            if (dragon.getOwner() instanceof Player playerx) {
               owner = playerx;
            }
         } else if (owner != null && CompanionRelations.protectedOwners(attacker).contains(owner.getUUID())) {
            return false;
         }

         if (attacker instanceof Player playerx
            && (playerx.isCreative() || defender.getServer() == null || !defender.getServer().isPvpAllowed() || owner != null && !owner.canHarmPlayer(playerx))) {
            return false;
         }

         return true;
      } else {
         return false;
      }
   }

   private static LivingEntity reflectedBy(ServerLevel level, Projectile shot) {
      CompoundTag data = shot.getPersistentData();
      if (!data.getBoolean("DragonMaidWingGuardReflected")) {
         return null;
      } else {
         return (
               data.hasUUID("DragonMaidLongswordDeflector")
                  ? level.getEntity(data.getUUID("DragonMaidLongswordDeflector"))
                  : (shot instanceof ThrownTrident ? null : shot.getOwner())
            ) instanceof LivingEntity living
            ? living
            : null;
      }
   }

   private static void markReflected(Projectile shot, LivingEntity defender) {
      shot.getPersistentData().putBoolean("DragonMaidWingGuardReflected", true);
      shot.getPersistentData().putUUID("DragonMaidLongswordDeflector", defender.getUUID());
   }

   private static boolean reflect(ServerLevel level, Projectile shot, LivingEntity defender, LivingEntity attacker) {
      if (shot instanceof ShulkerBullet) {
         ShulkerBullet returned = new ShulkerBullet(level, defender, attacker, Axis.Y);
         returned.setPos(shot.position());
         markReflected(returned, defender);
         if (!level.addFreshEntity(returned)) {
            return false;
         } else {
            shot.discard();
            return true;
         }
      } else {
         Vec3 aim = attacker.getBoundingBox().getCenter().subtract(shot.position());
         if (aim.lengthSqr() < 1.0E-6) {
            return false;
         } else {
            float speed = (float)Math.max(1.4, shot.getDeltaMovement().length());
            if (shot instanceof AbstractArrow && !shot.isNoGravity()) {
               double travel = Math.min(20.0, aim.horizontalDistance() / (double)speed);
               aim = aim.add(0.0, 0.025 * travel * travel, 0.0);
            }

            Vec3 direction = aim.normalize();
            if (shot instanceof DragonChargeEntity charge && attacker instanceof DragonBaseEntity dragon) {
               shot.getPersistentData().putFloat("DragonMaidDeflectedChargeDamage", charge.getDamage() * (float)dragon.getDragonStage());
            }

            if (shot instanceof DragonTridentFlight trident) {
               trident.markReflectedBy(defender);
            }

            if (!(shot instanceof ThrownTrident) && !(shot instanceof ThrownEnderpearl)) {
               if (shot instanceof AbstractArrow arrow) {
                  Pickup pickup = arrow.pickup;
                  arrow.setOwner(defender);
                  arrow.pickup = pickup;
               } else {
                  shot.setOwner(defender);
               }
            }

            if (shot instanceof AbstractHurtingProjectile powered) {
               Vec3 acceleration = direction.scale(0.1);
               powered.accelerationPower = 0.1;
               
               
            }

            shot.shoot(aim.x, aim.y, aim.z, speed, 0.0F);
            shot.hasImpulse = true;
            shot.hurtMarked = true;
            return true;
         }
      }
   }

   private static boolean hitWithReturnedIafProjectile(Projectile shot, Entity struck, LivingEntity victim, LivingEntity reflector) {
      if (shot instanceof DragonChargeEntity charge) {
         CompoundTag data = shot.getPersistentData();
         if (!data.contains("DragonMaidDeflectedChargeDamage")) {
            return false;
         }

         DamageSource damage = new DamageSource(charge.causeDamage(reflector).typeHolder(), shot, reflector);
         struck.hurt(damage, data.getFloat("DragonMaidDeflectedChargeDamage"));
      } else if (shot instanceof HydraBreathEntity) {
         if (struck.hurt(shot.damageSources().mobProjectile(shot, reflector), 2.0F)) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60));
         }
      } else {
         if (!(shot instanceof SeaSerpentBubblesEntity)) {
            return false;
         }

         struck.hurt(shot.damageSources().mobProjectile(shot, reflector), 6.0F);
      }

      shot.discard();
      return true;
   }

   private static void blocked(LivingEntity defender) {
      defender.playSound(SoundEvents.SHIELD_BLOCK, 0.8F, 1.15F);
      playGuardImpact(defender);
   }

   private static void playGuardImpact(LivingEntity defender) {
      if (defender instanceof DragonBaseEntity dragon && DragonForms.state(dragon).dragonmaid$getAction() != 9) {
         BladeMotion.begin(dragon, 8);
      }
   }

   private static record DeflectionAttempt(UUID defender, long tick) {
   }
}
