package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.LongswordParry;
import com.example.dragonmaid.combat.SpecialWeapons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "dragonmaid",
   value = {Dist.CLIENT}
)
public final class PlayerBladeAnimation {
   private static LocalPlayer player;
   private static ItemStack charging = ItemStack.EMPTY;
   private static int chargeTicks;
   private static PlayerBladeAnimation.Playback playback;

   private PlayerBladeAnimation() {
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      {
         LocalPlayer local = Minecraft.getInstance().player;
         if (ready(local)) {
            ItemStack main = local.getMainHandItem();
            if (isCharging(local)) {
               charging = main;
               chargeTicks = local.getTicksUsingItem();
               playback = null;
            } else if (!charging.isEmpty()) {
               if (main == charging) {
                  transition(local, main, chargeTicks, true);
               }

               charging = ItemStack.EMPTY;
            }

            if (playback != null && (main != playback.stack || playback.finished(local.tickCount))) {
               playback = null;
            }
         }
      }
   }

   public static void release(Player releasing, ItemStack stack, int ticks) {
      if (releasing instanceof LocalPlayer local && local == Minecraft.getInstance().player && ready(local)) {
         transition(local, stack, ticks, false);
      }
   }

   static void startLongswordGuard(LocalPlayer local) {
      if (ready(local) && playback != null && !playback.katana) {
         playback = null;
      }
   }

   public static void riposte(int slot, int itemId, int useSequence, int recoveryTicks) {
      LocalPlayer local = Minecraft.getInstance().player;
      if (ready(local)) {
         LongswordParry.beginPlayerRecovery(local, recoveryTicks);
         if (local.getInventory().selected == slot) {
            ItemStack main = local.getMainHandItem();
            if (SpecialWeapons.isLongsword(main) && Item.getId(main.getItem()) == itemId && LongswordParryOverlay.playerRiposte(local, useSequence)) {
               PlayerBladeMotion.Clip clip = PlayerBladeMotion.INSTANCE.get(PlayerBladeMotion.Action.RIPOSTE);
               if (clip != null) {
                  playback = new PlayerBladeAnimation.Playback(main, local.tickCount, clip, null, false, -1);
               }
            }
         }
      }
   }

   private static void transition(LocalPlayer local, ItemStack stack, int ticks, boolean cancel) {
      charging = ItemStack.EMPTY;
      PlayerBladeMotion motions = PlayerBladeMotion.INSTANCE;
      PlayerBladeMotion.Clip charge = motions.get(PlayerBladeMotion.Action.CHARGE);
      PlayerBladeMotion.Clip action = motions.get(cancel ? PlayerBladeMotion.Action.CANCEL : PlayerBladeMotion.Action.RELEASE);
      if (charge != null && action != null) {
         float seconds = (float)ticks / 20.0F;
         float earliest = seconds < 0.38F ? (cancel ? action.seconds() : 0.135F) : 0.0F;
         playback = new PlayerBladeAnimation.Playback(
            stack,
            local.tickCount,
            action.continueFrom(charge.sample(seconds), earliest),
            cancel ? null : motions.get(PlayerBladeMotion.Action.RECOVER),
            true,
            cancel ? -1 : ticks
         );
      }
   }

   private static boolean ready(LocalPlayer local) {
      if (player != local || local == null || !local.isAlive() || local.isSpectator()) {
         player = local;
         charging = ItemStack.EMPTY;
         playback = null;
      }

      return local != null && local.isAlive() && !local.isSpectator();
   }

   private static boolean isCharging(LocalPlayer local) {
      return local.isUsingItem()
         && local.getUsedItemHand() == InteractionHand.MAIN_HAND
         && SpecialWeapons.isKatana(local.getUseItem())
         && SpecialWeapons.isKatana(local.getMainHandItem());
   }

   static PlayerBladeMotion.Pose katana(LocalPlayer local, float partialTick) {
      if (!ready(local)) {
         return null;
      } else if (isCharging(local)) {
         PlayerBladeMotion motion = PlayerBladeMotion.INSTANCE;
         PlayerBladeMotion.Clip charge = motion.get(PlayerBladeMotion.Action.CHARGE);
         if (charge == null) {
            return null;
         } else {
            float seconds = ((float)local.getTicksUsingItem() + partialTick) / 20.0F;
            PlayerBladeMotion.Clip held = motion.get(PlayerBladeMotion.Action.READY);
            return seconds >= charge.seconds() && held != null ? held.sample(0.0F) : charge.sample(seconds);
         }
      } else {
         return current(local, partialTick, true);
      }
   }

   static PlayerBladeMotion.Pose longsword(LocalPlayer local, float partialTick) {
      return ready(local) ? current(local, partialTick, false) : null;
   }

   static float katanaChargeAge(LocalPlayer local, float partialTick) {
      if (isCharging(local)) {
         return (float)local.getTicksUsingItem() + partialTick;
      } else {
         return playback != null && playback.releaseChargeTicks >= 60 && playback.stack == local.getMainHandItem()
            ? (float)(playback.releaseChargeTicks + local.tickCount - playback.startTick) + partialTick
            : -1.0F;
      }
   }

   static boolean isKatanaSwinging(LocalPlayer local) {
      return player == local
         && playback != null
         && playback.releaseChargeTicks >= 0
         && playback.stack == local.getMainHandItem()
         && (float)(local.tickCount - playback.startTick) < 20.0F * playback.clip.seconds();
   }

   private static PlayerBladeMotion.Pose current(LocalPlayer local, float partialTick, boolean katana) {
      return playback != null && playback.katana == katana && local.getMainHandItem() == playback.stack ? playback.sample(local.tickCount, partialTick) : null;
   }

   private static record Playback(
      ItemStack stack, int startTick, PlayerBladeMotion.Clip clip, PlayerBladeMotion.Clip recovery, boolean katana, int releaseChargeTicks
   ) {
      boolean finished(int tick) {
         return (float)(tick - this.startTick) >= 20.0F * (this.clip.seconds() + (this.recovery == null ? 0.0F : this.recovery.seconds()));
      }

      PlayerBladeMotion.Pose sample(int tick, float partialTick) {
         float seconds = ((float)(tick - this.startTick) + partialTick) / 20.0F;
         if (seconds < this.clip.seconds()) {
            return this.clip.sample(seconds);
         } else {
            float recoveryTime = seconds - this.clip.seconds();
            return this.recovery != null && recoveryTime < this.recovery.seconds() ? this.recovery.sample(recoveryTime) : null;
         }
      }
   }
}
