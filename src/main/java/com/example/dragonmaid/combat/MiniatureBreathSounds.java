package com.example.dragonmaid.combat;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.registry.IafSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

final class MiniatureBreathSounds {
   private MiniatureBreathSounds() {
   }

   static void launch(DragonBaseEntity dragon, Vec3 mouth) {
      if (dragon.level() instanceof ServerLevel level && !dragon.isSilent()) {
         level.playSound(
            null,
            mouth.x,
            mouth.y,
            mouth.z,
            dragon instanceof LightningDragonEntity
               ? IafSounds.LIGHTNINGDRAGON_BREATH.get()
               : (dragon instanceof IceDragonEntity ? IafSounds.ICEDRAGON_BREATH.get() : IafSounds.FIREDRAGON_BREATH.get()),
            dragon.getSoundSource(),
            1.5F,
            1.2F
         );
         if (!(dragon instanceof LightningDragonEntity)) {
            level.playSound(
               null,
               mouth.x,
               mouth.y,
               mouth.z,
               SoundEvents.BLAZE_SHOOT,
               dragon.getSoundSource(),
               1.5F,
               0.95F + dragon.getRandom().nextFloat() * 0.1F
            );
         }

         return;
      }
   }

   static void impact(DragonBaseEntity dragon, Vec3 point, float volume) {
      if (dragon.level() instanceof ServerLevel level && !dragon.isSilent()) {
         level.playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE, dragon.getSoundSource(), volume, 1.35F);
         return;
      }
   }
}
