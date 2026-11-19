package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.SpecialWeapons;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

public final class PlayerKatanaPose {
   private static final float WINDUP_TICKS = 7.0F;
   private static final float CUT_TICKS = 4.0F;
   private static final float HOLD_TICKS = 2.0F;
   private static final float RECOVERY_TICKS = 9.0F;
   private static final Map<AbstractClientPlayer, Integer> DRAWS = new WeakHashMap<>();
   private static final PlayerKatanaPose.Pose READY = new PlayerKatanaPose.Pose(0.5F, -0.82F, -0.63F, -0.92F, -0.33F, 0.29F, 0.4F);
   private static final PlayerKatanaPose.Pose FOLLOW_THROUGH = new PlayerKatanaPose.Pose(-0.42F, -1.03F, 0.57F, 1.05F, -0.5F, -0.12F, 0.3F);

   private PlayerKatanaPose() {
   }

   public static void startDraw(int entityId, UUID uuid) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.getEntity(entityId) instanceof AbstractClientPlayer player && player.getUUID().equals(uuid)) {
         DRAWS.put(player, player.tickCount);
      }
   }

   public static void apply(PlayerModel<?> model, AbstractClientPlayer player, float ageInTicks, float headPitch) {
      if (player.isAlive() && !player.isSpectator()) {
         Integer started = DRAWS.get(player);
         float drawAge = started == null ? -1.0F : ageInTicks - (float)started.intValue();
         float cutEnd = 6.0F;
         if (drawAge >= 0.0F && drawAge < cutEnd + 9.0F) {
            float sweep = ease(drawAge / 4.0F);
            float weight = 1.0F - ease((drawAge - cutEnd) / 9.0F);
            pose(model, player, READY.blend(FOLLOW_THROUGH, sweep), weight, headPitch);
         } else if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND && SpecialWeapons.isKatana(player.getUseItem())) {
            float ticks = (float)player.getTicksUsingItem() + Mth.clamp(ageInTicks - (float)player.tickCount, 0.0F, 1.0F);
            pose(model, player, READY, ease(ticks / 7.0F), headPitch);
         }
      }
   }

   private static float ease(float time) {
      float t = Mth.clamp(time, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private static void pose(PlayerModel<?> model, AbstractClientPlayer player, PlayerKatanaPose.Pose pose, float weight, float headPitch) {
      boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
      float mirror = rightHanded ? 1.0F : -1.0F;
      float aim = Mth.clamp(headPitch * (float) (Math.PI / 180.0), -0.9F, 0.9F);
      model.body.yRot = model.body.yRot + mirror * pose.torso * weight;
      setArm(rightHanded ? model.rightArm : model.leftArm, pose.mainX + aim, mirror * pose.mainY, mirror * pose.mainZ, weight);
      setArm(rightHanded ? model.leftArm : model.rightArm, pose.otherX + aim * 0.3F, mirror * pose.otherY, mirror * pose.otherZ, weight);
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftSleeve.copyFrom(model.leftArm);
      model.jacket.copyFrom(model.body);
   }

   private static void setArm(ModelPart arm, float x, float y, float z, float weight) {
      arm.xRot = Mth.lerp(weight, arm.xRot, x);
      arm.yRot = Mth.lerp(weight, arm.yRot, y);
      arm.zRot = Mth.lerp(weight, arm.zRot, z);
   }

   private static record Pose(float torso, float mainX, float mainY, float mainZ, float otherX, float otherY, float otherZ) {
      PlayerKatanaPose.Pose blend(PlayerKatanaPose.Pose end, float time) {
         return new PlayerKatanaPose.Pose(
            Mth.lerp(time, this.torso, end.torso),
            Mth.lerp(time, this.mainX, end.mainX),
            Mth.lerp(time, this.mainY, end.mainY),
            Mth.lerp(time, this.mainZ, end.mainZ),
            Mth.lerp(time, this.otherX, end.otherX),
            Mth.lerp(time, this.otherY, end.otherY),
            Mth.lerp(time, this.otherZ, end.otherZ)
         );
      }
   }
}
