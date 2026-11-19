package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.item.BladeEnchantments;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafDamageTypes;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

public final class LongswordParry {
   public static final float PRIMARY_MULTIPLIER = 1.5F;
   public static final float SECONDARY_RATIO = 0.8F;
   private static final Map<Player, Long> PLAYER_RECOVERY = Collections.synchronizedMap(new WeakHashMap<>());
   private final long effectiveAt;
   private final int useSequence;
   private boolean spent;

   public LongswordParry(long effectiveAt) {
      this(effectiveAt, -1);
   }

   LongswordParry(long effectiveAt, int useSequence) {
      this.effectiveAt = effectiveAt;
      this.useSequence = useSequence;
   }

   int useSequence() {
      return this.useSequence;
   }

   boolean spent() {
      return this.spent;
   }

   void spend() {
      this.spent = true;
   }

   public static int windupTicks() {
      return BladeCompat.guardDelayTicks();
   }

   public static int windowTicks(ItemStack longsword) {
      return BladeCompat.parryWindowTicks() + BladeEnchantments.parryBonusTicks(longsword);
   }

   public static int recoveryTicks() {
      return BladeCompat.parryCooldownTicks();
   }

   public static boolean recovering(LivingEntity wielder) {
      if (wielder instanceof DragonBaseEntity dragon) {
         return DragonShield.of(dragon).longswordRecovering(dragon);
      } else {
         return wielder instanceof Player player ? player.level().getGameTime() < PLAYER_RECOVERY.getOrDefault(player, Long.MIN_VALUE) : false;
      }
   }

   public static void beginPlayerRecovery(Player player, int ticks) {
      if (ticks > 0) {
         PLAYER_RECOVERY.put(player, player.level().getGameTime() + (long)ticks);
         if (player.isUsingItem() && SpecialWeapons.isLongsword(player.getUseItem())) {
            player.stopUsingItem();
         }
      }
   }

   public LivingEntity takeFirstAttacker(long now, int windowTicks, DamageSource source) {
      long elapsed = now - this.effectiveAt;
      if (!this.spent && elapsed >= 0L && elapsed < (long)windowTicks) {
         LivingEntity attacker = meleeAttacker(source);
         if (attacker == null) {
            return null;
         } else {
            this.spend();
            return attacker;
         }
      } else {
         return null;
      }
   }

   static LivingEntity meleeAttacker(DamageSource source) {
      if (source.getDirectEntity() instanceof LivingEntity attacker
         && source.getEntity() == attacker
         && !source.is(DamageTypeTags.IS_PROJECTILE)
         && !source.is(DamageTypeTags.IS_EXPLOSION)
         && !source.is(DamageTypeTags.IS_FIRE)
         && !source.is(DamageTypeTags.IS_LIGHTNING)
         && !source.is(DamageTypeTags.IS_FREEZING)
         && !source.is(DamageTypeTags.IS_FALL)
         && !source.is(DamageTypes.THORNS)
         && !source.is(DamageTypes.MAGIC)
         && !source.is(DamageTypes.INDIRECT_MAGIC)
         && !source.is(DamageTypes.DRAGON_BREATH)
         && !source.is(IafDamageTypes.DRAGON_FIRE_TYPE)
         && !source.is(IafDamageTypes.DRAGON_ICE_TYPE)
         && !source.is(IafDamageTypes.DRAGON_LIGHTNING_TYPE)) {
         return attacker;
      }

      return null;
   }

   public interface UseSequence {
      int dragonmaid$useItemSequence();
   }
}