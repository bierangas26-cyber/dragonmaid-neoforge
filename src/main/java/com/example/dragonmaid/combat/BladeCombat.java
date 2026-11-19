package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.item.BladeEnchantments;
import com.example.dragonmaid.item.CompanionBladeItem;
import com.example.dragonmaid.particle.ModParticles;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3f;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class BladeCombat {
   public static final int DRAW_RECOVERY_TICKS = 18;
   private static final double DRAW_EFFECT_DISTANCE_SQR = 4096.0;
   private static final double GUARD_ARC_COS = 0.5;
   private static final float TIMED_PARRY_DAMAGE_TAKEN = 0.1F;
   private static final double LONGSWORD_HALF_ARC = 90.0;
   private static final TagKey<DamageType> EXTRA_PHYSICAL_GUARD_DAMAGE = TagKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("dragonmaid", "longsword_physical_guard")
   );
   private static final DustParticleOptions[] CHARGE_GLOW = new DustParticleOptions[3];
   private static final Map<LivingEntity, LongswordParry> GUARDS;
   private static final ThreadLocal<Boolean> COUNTER_IN_PROGRESS;
   private static final ThreadLocal<BladeCombat.ExplosionHit> EXPLOSION_HIT;

   private BladeCombat() {
   }

   @SubscribeEvent
   public static void beginUse(Start event) {
      LivingEntity wielder = event.getEntity();
      if (SpecialWeapons.isLongsword(event.getItem())) {
         if (LongswordParry.recovering(wielder)) {
            event.setCanceled(true);
         } else if (!wielder.level().isClientSide) {
            int var10000;
            label20: {
               if (wielder instanceof ServerPlayer player && player.connection instanceof LongswordParry.UseSequence use) {
                  var10000 = use.dragonmaid$useItemSequence();
                  break label20;
               }

               var10000 = -1;
            }

            int sequence = var10000;
            GUARDS.put(wielder, new LongswordParry(0L, sequence));
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void hurt(LivingDamageEvent.Pre event) {
      LivingEntity defender = event.getEntity();
      if (defender.level() instanceof ServerLevel level && !(event.getNewDamage() <= 0.0F)) {
         DamageSource source = event.getSource();
         if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof Player player && player.getMainHandItem().getItem() instanceof CompanionBladeItem
            )
          {
            ItemStack held = player.getMainHandItem();
            float amount = event.getNewDamage();
            if (SpecialWeapons.isKatana(held)) {
               amount *= KatanaCharge.lightArmorMultiplier((float)defender.getArmorValue());
            }

            event.setNewDamage(amount + CompanionBladeItem.undeadBonus(held, defender));
         }

         if (defender instanceof DragonBaseEntity dragon) {
            LongswordProjectiles.observeRangedDamage(dragon, source);
         }

         ItemStack blade = defender.getUseItem();
         if (longswordGuardActive(defender) && blocksPhysicalHit(defender, source)) {
            CompanionBladeItem guardBlade = (CompanionBladeItem)blade.getItem();
            LivingEntity attacker = eligibleCounterAttacker(defender, source);
            boolean timedParry = false;
            if (attacker != null) {
               LongswordParry guard = GUARDS.computeIfAbsent(defender, ignored -> new LongswordParry(0L));
               if (guard.spent()) {
                  attacker = null;
               } else {
                  int activeTicks = defender.getTicksUsingItem() - LongswordParry.windupTicks();
                  int window = LongswordParry.windowTicks(blade);
                  timedParry = guard.takeFirstAttacker((long)activeTicks, window, source) != null;
                  if (!timedParry) {
                     float chance = BladeEnchantments.counterChance(blade);
                     if (chance > 0.0F && defender.getRandom().nextFloat() < chance) {
                        guard.spend();
                     } else {
                        attacker = null;
                     }
                  }
               }
            }

            event.setNewDamage(event.getNewDamage() * (timedParry ? 0.1F : 1.0F - guardBlade.guardReduction()));
            if (attacker == null) {
               defender.playSound(SoundEvents.SHIELD_BLOCK, 0.65F, 1.2F);
            } else if (!defender.isSilent()) {
               level.playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.ANVIL_PLACE, defender.getSoundSource(), 0.85F, 1.65F);
            }

            if (attacker != null) {
               int recovery = !timedParry && !BladeCompat.secondChanceTriggersCooldown() ? 0 : LongswordParry.recoveryTicks();
               if (defender instanceof Player player) {
                  LongswordParry.beginPlayerRecovery(player, recovery);
                  if (player instanceof ServerPlayer serverPlayer) {
                     BladeCompat.sendRiposte(serverPlayer, blade.getItem(), GUARDS.get(defender).useSequence(), recovery);
                  }
               } else if (defender instanceof DragonBaseEntity dragon) {
                  DragonShield.of(dragon).beginLongswordRecovery(dragon, recovery);
                  BladeMotion.begin(dragon, 9);
               }

               COUNTER_IN_PROGRESS.set(true);

               try {
                  counter(defender, blade, attacker);
               } finally {
                  COUNTER_IN_PROGRESS.remove();
               }
            } else if (defender instanceof DragonBaseEntity dragon && DragonForms.state(dragon).dragonmaid$getAction() != 9) {
               BladeMotion.begin(dragon, 8);
            }

            return;
         }

         return;
      }
   }

   static boolean longswordGuardActive(LivingEntity defender) {
      return defender.isUsingItem()
         && defender.getUsedItemHand() == InteractionHand.MAIN_HAND
         && defender.getUseItem().getItem() instanceof CompanionBladeItem
         && SpecialWeapons.isLongsword(defender.getUseItem())
         && !LongswordParry.recovering(defender)
         && defender.getTicksUsingItem() >= LongswordParry.windupTicks()
         && (!(defender instanceof DragonBaseEntity dragon) || DragonShield.of(dragon).longswordRaised(dragon));
   }

   static boolean facesIncoming(LivingEntity defender, Vec3 incoming) {
      return incoming.dot(forward(defender)) >= 0.5;
   }

   private static boolean blocksPhysicalHit(LivingEntity defender, DamageSource source) {
      if (!source.is(DamageTypeTags.IS_FALL) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         boolean extraPhysical = source.is(DamageTypeTags.IS_EXPLOSION) || source.is(EXTRA_PHYSICAL_GUARD_DAMAGE);
         if (extraPhysical
            || !source.is(DamageTypeTags.BYPASSES_SHIELD) && (LongswordProjectiles.isProjectile(source) || LongswordParry.meleeAttacker(source) != null)) {
            Vec3 incoming = incomingDirection(defender, source);
            return extraPhysical && incoming.lengthSqr() == 0.0 || facesIncoming(defender, incoming);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean hurtFromExplosion(Entity entity, DamageSource source, float amount, Vec3 center) {
      if (entity instanceof LivingEntity defender && longswordGuardActive(defender) && source.is(DamageTypeTags.IS_EXPLOSION)) {
         BladeCombat.ExplosionHit previous = EXPLOSION_HIT.get();
         EXPLOSION_HIT.set(new BladeCombat.ExplosionHit(defender, source, center));

         boolean var6;
         try {
            var6 = defender.hurt(source, amount);
         } finally {
            if (previous == null) {
               EXPLOSION_HIT.remove();
            } else {
               EXPLOSION_HIT.set(previous);
            }
         }

         return var6;
      }

      return entity.hurt(source, amount);
   }

   private static LivingEntity eligibleCounterAttacker(LivingEntity defender, DamageSource source) {
      LivingEntity attacker = LongswordParry.meleeAttacker(source);
      return COUNTER_IN_PROGRESS.get() == null
            && attacker != null
            && mayHit(defender, attacker)
            && canReachAttacker(defender, attacker, counterReach(defender))
         ? attacker
         : null;
   }

   private static double counterReach(LivingEntity defender) {
      return defender instanceof DragonBaseEntity dragon ? HumanoidCombat.clawReach(dragon) : 4.0;
   }

   private static boolean canReachAttacker(LivingEntity defender, LivingEntity attacker, double reach) {
      return defender.distanceToSqr(attacker) <= reach * reach && defender.hasLineOfSight(attacker);
   }

   private static void counter(LivingEntity defender, ItemStack blade, LivingEntity attacker) {
      double reach = counterReach(defender);
      double base = baseDamage(defender, blade);
      float primary = (float)((base + (double)BladeCompat.damageBonus(blade, attacker)) * 1.5);
      DamageSource damage = attackSource(defender);
      boolean dragonWielder = defender instanceof DragonBaseEntity;
      float primaryBonus = dragonWielder ? CompanionBladeItem.undeadBonus(blade, attacker) : 0.0F;
      LivingEntity struck = attacker.hurt(damage, primary + primaryBonus) ? attacker : null;
      Vec3 direction = horizontal(attacker.position().subtract(defender.position()));
      int sweeping = BladeCompat.sweepingEdge(blade);
      float sweep = Mth.lerp((float)sweeping / (1.0F + (float)sweeping), 0.8F, 1.0F);

      for (LivingEntity enemy : defender.level()
         .getEntitiesOfClass(LivingEntity.class, defender.getBoundingBox().inflate(reach, 1.5, reach), e -> e != attacker && mayHit(defender, e))) {
         if (inArc(defender, enemy, direction, reach, 0.0)) {
            float secondaryBonus = dragonWielder ? CompanionBladeItem.undeadBonus(blade, enemy) : 0.0F;
            if (enemy.hurt(damage, primary * sweep + secondaryBonus) && struck == null) {
               struck = enemy;
            }
         }
      }

      if (struck != null) {
         weaponHit(defender, blade, struck);
      }

      defender.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
      longswordSweepVisual(defender, direction, reach);
   }

   public static void playerDraw(Player player, ItemStack blade, int ticks) {
      if (ticks >= 0) {
         float charge = KatanaCharge.progress((long)ticks);
         draw(player, blade, charge);
         player.getCooldowns().addCooldown(blade.getItem(), 18);
      }
   }

   static void draw(LivingEntity wielder, ItemStack blade, float charge) {
      double reach = KatanaCharge.radius(charge);
      float bonus = Mth.lerp(charge, 1.0F, 2.0F);
      float yaw = wielder instanceof Player ? wielder.getYRot() : wielder.getYHeadRot();
      float pitch = wielder instanceof Player ? wielder.getXRot() : 0.0F;
      Vec3 origin = wielder instanceof Player
         ? wielder.getEyePosition().subtract(0.0, 0.45, 0.0)
         : new Vec3(wielder.getX(), wielder.getY(0.55), wielder.getZ());
      KatanaSweep sweep = new KatanaSweep(yaw, pitch);
      DamageSource source = attackSource(wielder);
      LivingEntity struck = null;
      double base = baseDamage(wielder, blade);

      for (LivingEntity enemy : wielder.level()
         .getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(reach), e -> mayHit(wielder, e) && isIaiTarget(wielder, e))) {
         if (sweep.hits(wielder, origin, enemy.getBoundingBox(), reach)) {
            float damage = (float)(base + (double)BladeCompat.damageBonus(blade, enemy)) * bonus;
            if (wielder instanceof DragonBaseEntity) {
               damage = damage * KatanaCharge.lightArmorMultiplier((float)enemy.getArmorValue()) + CompanionBladeItem.undeadBonus(blade, enemy);
            }

            if (enemy.hurt(source, damage) && struck == null) {
               struck = enemy;
            }
         }
      }

      if (struck != null) {
         weaponHit(wielder, blade, struck);
      }

      if (wielder.level() instanceof ServerLevel level) {
         level.playSound(
            null, wielder.getX(), wielder.getY(), wielder.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, wielder.getSoundSource(), 1.2F, Mth.lerp(charge, 1.2F, 0.85F)
         );

         for (ServerPlayer viewer : level.players()) {
            if (viewer.connection != null && !(viewer.distanceToSqr(origin) > 4096.0)) {
               level.sendParticles(
                  viewer,
                  (SimpleParticleType)BladeCompat.bladeSweepParticle(),
                  true,
                  origin.x,
                  origin.y,
                  origin.z,
                  0,
                  (double)yaw,
                  -reach,
                  (double)pitch,
                  1.0
               );
               if (wielder instanceof ServerPlayer player) {
                  BladeCompat.sendKatanaDraw(viewer, player);
               }
            }
         }
      }
   }

   public static void chargeParticles(LivingEntity wielder, long ticks) {
      if (ticks == 0L || ticks == 20L || ticks == 40L) {
         playChargeCue(wielder, KatanaCharge.stage(ticks));
      }

      long pastFull = ticks - 60L;
      if (ticks >= 0L && (pastFull <= 0L ? ticks % 5L == 0L : pastFull % 12L == 0L)) {
         showChargeParticles(wielder, ticks);
      }
   }

   static void playChargeCue(LivingEntity wielder, int stage) {
      if (wielder.level() instanceof ServerLevel level) {
         SoundSource var10 = wielder.getSoundSource();
         double x = wielder.getX();
         double y = wielder.getY();
         double z = wielder.getZ();
         switch (stage) {
            case 0:
               level.playSound(null, x, y, z, SoundEvents.CHAIN_PLACE, var10, 0.95F, 0.95F);
               break;
            case 1:
               level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_IRON, var10, 1.05F, 1.1F);
               break;
            case 2:
               level.playSound(null, x, y, z, SoundEvents.ANVIL_PLACE, var10, 1.15F, 1.4F);
         }
      }
   }

   static void showChargeParticles(LivingEntity wielder, long ticks) {
      if (wielder.level() instanceof ServerLevel level) {
         float var14 = KatanaCharge.progress(ticks);
         boolean readyFlash = ticks == 60L;
         Vec3 facing = forward(wielder);
         double x = wielder.getX() + facing.z * 0.36 + facing.x * 0.12;
         double z = wielder.getZ() - facing.x * 0.36 + facing.z * 0.12;
         double y = wielder.getY(0.56);
         DustParticleOptions glow = CHARGE_GLOW[KatanaCharge.stage(ticks)];
         level.sendParticles(glow, x, y, z, 3 + (int)(5.0F * var14), 0.13, 0.12, 0.13, 0.02);
         if (readyFlash) {
            level.sendParticles(glow, x, y, z, 16, 0.22, 0.18, 0.22, 0.11);
         }
      }
   }

   private static boolean mayHit(LivingEntity wielder, LivingEntity enemy) {
      if (enemy == wielder || !enemy.isAlive() || enemy.isSpectator() || wielder.isAlliedTo(enemy)) {
         return false;
      } else if (wielder instanceof DragonBaseEntity dragon) {
         return HumanoidTargetGoal.isEnemy(dragon, enemy) && GuardEscort.reachableTarget(dragon, enemy);
      } else {
         if (wielder instanceof Player player) {
            if (CompanionRelations.protectedOwners(enemy).contains(player.getUUID())) {
               return false;
            }

            if (enemy instanceof Player target) {
               return !target.isCreative() && player.canHarmPlayer(target) && player.getServer().isPvpAllowed();
            }
         }

         return enemy.isAttackable();
      }
   }

   private static boolean isIaiTarget(LivingEntity wielder, LivingEntity enemy) {
      if (enemy instanceof Mob mob) {
         EntityType<?> type = enemy.getType();
         if (type == EntityType.VILLAGER) {
            return false;
         } else if (enemy instanceof AbstractVillager
            || type.getCategory().isFriendly() && !(enemy instanceof Enemy) && !(enemy instanceof NeutralMob) && !(enemy instanceof DragonBaseEntity)) {
            if (targets(mob, wielder)) {
               return true;
            } else {
               if (wielder instanceof DragonBaseEntity dragon && targets(mob, dragon.getOwner())) {
                  return true;
               }

               return type == EntityType.DOLPHIN
                  || type == EntityType.FOX
                  || type == EntityType.GOAT
                  || type == EntityType.LLAMA
                  || type == EntityType.TRADER_LLAMA
                  || type == EntityType.PANDA
                  || type == EntityType.PUFFERFISH;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean targets(Mob mob, LivingEntity victim) {
      return victim != null && (mob.getTarget() == victim || mob.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, victim));
   }

   private static boolean inArc(LivingEntity wielder, LivingEntity enemy, Vec3 direction, double reach, double cos) {
      Vec3 delta = enemy.position().subtract(wielder.position());
      return delta.lengthSqr() <= reach * reach && horizontal(delta).dot(direction) >= cos && wielder.hasLineOfSight(enemy);
   }

   private static Vec3 horizontal(Vec3 delta) {
      return new Vec3(delta.x, 0.0, delta.z).normalize();
   }

   static Vec3 incomingDirection(LivingEntity defender, DamageSource source) {
      if (source.is(DamageTypeTags.IS_EXPLOSION)) {
         BladeCombat.ExplosionHit hit = EXPLOSION_HIT.get();
         if (hit != null && hit.defender() == defender && hit.source() == source) {
            return horizontal(hit.center().subtract(defender.position()));
         }
      }

      if (LongswordProjectiles.isProjectile(source) && source.getDirectEntity() != null) {
         Vec3 direction = horizontal(source.getDirectEntity().getDeltaMovement()).reverse();
         if (direction.lengthSqr() > 0.0) {
            return direction;
         }
      }

      Vec3 origin = source.getSourcePosition();
      return origin == null ? Vec3.ZERO : horizontal(origin.subtract(defender.position()));
   }

   private static Vec3 forward(LivingEntity wielder) {
      return Vec3.directionFromRotation(0.0F, wielder instanceof Player ? wielder.getYRot() : wielder.getYHeadRot());
   }

   private static DamageSource attackSource(LivingEntity wielder) {
      return wielder instanceof Player player ? wielder.damageSources().playerAttack(player) : wielder.damageSources().mobAttack(wielder);
   }

   private static double baseDamage(LivingEntity wielder, ItemStack blade) {
      double base = wielder.getAttributeValue(Attributes.ATTACK_DAMAGE);
      return wielder instanceof DragonBaseEntity ? HumanoidCombat.armedDamage(base, blade) : base;
   }

   private static void weaponHit(LivingEntity wielder, ItemStack blade, LivingEntity struck) {
      wielder.setLastHurtMob(struck);
      if (wielder instanceof DragonBaseEntity dragon) {
         HumanoidCombat.applyWeaponHit(dragon, blade, struck);
      } else {
         blade.getItem().hurtEnemy(blade, struck, wielder);
      }
   }

   public static void playerLongswordSweepVisual(Player player) {
      longswordSweepVisual(player, forward(player), player.entityInteractionRange());
   }

   static void longswordSweepVisual(LivingEntity wielder, Vec3 direction, double reach) {
      sweepVisual(wielder, direction, reach, 90.0);
   }

   private static void sweepVisual(LivingEntity wielder, Vec3 direction, double reach, double halfArc) {
      if (wielder.level() instanceof ServerLevel level && Double.isFinite(reach) && !(reach <= 0.0)) {
         Vec3 aim = direction.horizontalDistanceSqr() < 1.0E-6 ? forward(wielder) : direction;
         double yaw = Math.atan2(-aim.x, aim.z);
         level.sendParticles(
            (SimpleParticleType)BladeCompat.bladeSweepParticle(), wielder.getX(), wielder.getY(0.55), wielder.getZ(), 0, yaw, reach, halfArc, 1.0
         );
         return;
      }
   }

   static {
      for (int stage = 0; stage < CHARGE_GLOW.length; stage++) {
         int rgb = KatanaCharge.stageColor(stage);
         CHARGE_GLOW[stage] = new DustParticleOptions(
            new Vector3f((float)(rgb >> 16) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F), 1.2F
         );
      }

      GUARDS = new WeakHashMap<>();
      COUNTER_IN_PROGRESS = new ThreadLocal<>();
      EXPLOSION_HIT = new ThreadLocal<>();
   }

   private static record ExplosionHit(LivingEntity defender, DamageSource source, Vec3 center) {
   }
}