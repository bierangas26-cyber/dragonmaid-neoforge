package com.example.dragonmaid.client;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

final class HumanoidFallTimeline {
   private static final double MIN_DROP = 1.5;
   private static final int ENTER_TICKS = 7;
   private static final int BRACE_TICKS = 7;
   private static final int LAND_TICKS = 9;
   private static final int RECOVER_TICKS = 18;
   private HumanoidFallTimeline.Phase phase = HumanoidFallTimeline.Phase.NONE;
   private int lastTick = Integer.MIN_VALUE;
   private int generation;
   private int started;
   private double previousY;
   private double highY;

   void update(DragonBaseEntity dragon, int nextGeneration, boolean eligible) {
      int tick = dragon.tickCount;
      double y = dragon.getY();
      if (!eligible) {
         this.reset(tick, y, nextGeneration);
      } else if (tick != this.lastTick) {
         int elapsed = tick - this.lastTick;
         double changeY = y - this.previousY;
         if (this.lastTick != Integer.MIN_VALUE
            && elapsed >= 0
            && elapsed <= 5
            && this.generation == nextGeneration
            && !(Math.abs(changeY) > 4.1 * (double)elapsed)) {
            this.lastTick = tick;
            this.previousY = y;
            boolean descending = this.phase == HumanoidFallTimeline.Phase.ENTER
               || this.phase == HumanoidFallTimeline.Phase.FALL
               || this.phase == HumanoidFallTimeline.Phase.BRACE;
            boolean awaitingContact = descending || this.phase == HumanoidFallTimeline.Phase.NONE && this.highY - y >= 1.5;
            if (!dragon.onGround() || awaitingContact && !hasSupportWithin(dragon, 0.06)) {
               if (this.phase == HumanoidFallTimeline.Phase.LAND || this.phase == HumanoidFallTimeline.Phase.RECOVER || changeY > 0.05) {
                  this.phase = HumanoidFallTimeline.Phase.NONE;
                  this.highY = y;
               }

               this.highY = Math.max(this.highY, y);
               if (this.phase == HumanoidFallTimeline.Phase.NONE && this.highY - y >= 1.5 && changeY < 0.0) {
                  this.start(HumanoidFallTimeline.Phase.ENTER, tick);
               }

               if (this.phase == HumanoidFallTimeline.Phase.ENTER && tick - this.started >= 7) {
                  this.start(HumanoidFallTimeline.Phase.FALL, this.started + 7);
               }

               if ((this.phase == HumanoidFallTimeline.Phase.ENTER || this.phase == HumanoidFallTimeline.Phase.FALL)
                  && changeY < 0.0
                  && nearGround(dragon, Math.max(-changeY / (double)elapsed, -dragon.getDeltaMovement().y))) {
                  this.start(HumanoidFallTimeline.Phase.BRACE, tick);
               }
            } else {
               if (awaitingContact) {
                  this.start(HumanoidFallTimeline.Phase.LAND, tick);
               } else if (this.phase == HumanoidFallTimeline.Phase.LAND && tick - this.started >= 9) {
                  this.start(HumanoidFallTimeline.Phase.RECOVER, this.started + 9);
               } else if (this.phase == HumanoidFallTimeline.Phase.RECOVER && tick - this.started >= 18) {
                  this.phase = HumanoidFallTimeline.Phase.NONE;
               }

               this.highY = y;
            }
         } else {
            this.reset(tick, y, nextGeneration);
         }
      }
   }

   private static boolean nearGround(DragonBaseEntity dragon, double speed) {
      return hasSupportWithin(dragon, Math.min(5.0, 7.0 * speed + 1.6));
   }

   private static boolean hasSupportWithin(DragonBaseEntity dragon, double distance) {
      AABB bounds = dragon.getBoundingBox();
      return Shapes.collide(Axis.Y, bounds, dragon.level().getBlockCollisions(dragon, bounds.expandTowards(0.0, -distance, 0.0)), -distance) > -distance;
   }

   private void start(HumanoidFallTimeline.Phase next, int tick) {
      this.phase = next;
      this.started = tick;
   }

   private void reset(int tick, double y, int nextGeneration) {
      this.phase = HumanoidFallTimeline.Phase.NONE;
      this.lastTick = tick;
      this.previousY = this.highY = y;
      this.generation = nextGeneration;
   }

   HumanoidFallTimeline.Phase phase() {
      return this.phase;
   }

   double age(float partial) {
      return (double)((float)(this.lastTick - this.started) + partial);
   }

   static enum Phase {
      NONE(""),
      ENTER("fall_enter"),
      FALL("fall"),
      BRACE("fall_brace"),
      LAND("land"),
      RECOVER("land_recover");

      final String clip;

      private Phase(String name) {
         this.clip = "animation.dragon_shared." + name;
      }
   }
}
