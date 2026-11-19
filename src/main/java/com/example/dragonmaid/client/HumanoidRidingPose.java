package com.example.dragonmaid.client;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.combat.BladeMotion;
import com.example.dragonmaid.combat.DragonShield;
import com.example.dragonmaid.combat.HumanoidRoar;
import com.example.dragonmaid.config.HumanoidRidingConfig;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidGroundMount;
import com.example.dragonmaid.movement.HumanoidFlight;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "dragonmaid",
   value = {Dist.CLIENT}
)
public final class HumanoidRidingPose {
   static final String BEING_CARRIED = "animation.dragon_female.bridal.being_carried";
   static final String CARRY_PLAYER = "animation.dragon_female.bridal.carry_player";
   private static final float PLAYER_RENDER_SCALE = 0.9375F;
   private static final HumanoidRidingPose.PartPose HELD_HEAD = new HumanoidRidingPose.PartPose(9.83F, -2.883F, -10.0F, 9.616F, 54.554F, 46.509F);
   private static final HumanoidRidingPose.PartPose HELD_BODY = new HumanoidRidingPose.PartPose(9.83F, -2.883F, -10.0F, -55.0F, 90.0F, 0.0F);
   private static final HumanoidRidingPose.PartPose HELD_RIGHT_ARM = new HumanoidRidingPose.PartPose(8.192F, -1.736F, -5.0F, 87.495F, -11.818F, 107.877F);
   private static final HumanoidRidingPose.PartPose HELD_LEFT_ARM = new HumanoidRidingPose.PartPose(8.192F, -1.736F, -15.0F, 129.444F, 51.453F, -169.359F);
   private static final HumanoidRidingPose.PartPose HELD_RIGHT_LEG = new HumanoidRidingPose.PartPose(0.0F, 4.0F, -8.1F, -93.563F, 85.758F, -44.961F);
   private static final HumanoidRidingPose.PartPose HELD_LEFT_LEG = new HumanoidRidingPose.PartPose(0.0F, 4.0F, -11.9F, 84.602F, 85.758F, 135.039F);
   private static final HumanoidRidingPose.PartPose CARRIER_HEAD = new HumanoidRidingPose.PartPose(0.0F, -0.03F, -0.607F, -4.781F, 11.815F, 2.114F);
   private static final HumanoidRidingPose.PartPose CARRIER_BODY = new HumanoidRidingPose.PartPose(0.0F, 0.231F, -2.084F, 10.0F, 0.0F, 0.0F);
   private static final HumanoidRidingPose.PartPose CARRIER_RIGHT_ARM = new HumanoidRidingPose.PartPose(-5.0F, 2.2F, -1.736F, -29.7F, -44.224F, -2.586F);
   private static final HumanoidRidingPose.PartPose CARRIER_LEFT_ARM = new HumanoidRidingPose.PartPose(5.0F, 2.2F, -1.736F, -63.548F, 24.822F, -58.922F);

   private HumanoidRidingPose() {
   }

   private static DragonBaseEntity mount(AbstractClientPlayer player) {
      if (player.getVehicle() instanceof DragonBaseEntity dragon && HumanoidGroundMount.active(dragon)) {
         return dragon;
      }

      return null;
   }

   private static DragonBaseEntity carriedDragon(AbstractClientPlayer player, float partial) {
      DragonBaseEntity dragon = mount(player);
      return dragon != null && appearance(dragon, partial) == HumanoidRidingConfig.Appearance.PLAYER_CARRIES_DRAGON ? dragon : null;
   }

   static HumanoidRidingConfig.Appearance appearance(DragonBaseEntity dragon, float partial) {
      HumanoidRidingConfig.Appearance configured = HumanoidRidingConfig.appearance(dragon);
      if (configured == HumanoidRidingConfig.Appearance.PIGGYBACK) {
         return configured;
      } else if (configured == HumanoidRidingConfig.Appearance.PLAYER_CARRIES_DRAGON && HumanoidFlight.phase(dragon) != HumanoidFlight.Phase.GROUND) {
         return HumanoidRidingConfig.Appearance.PIGGYBACK;
      } else {
         return combatPose(dragon, partial) ? HumanoidRidingConfig.Appearance.PIGGYBACK : configured;
      }
   }

   private static boolean combatPose(DragonBaseEntity dragon, float partial) {
      if (!dragon.isModelDead() && !dragon.isSleeping()) {
         int action = DragonForms.state(dragon).dragonmaid$getAction();
         if (action == 1
            || action == 2
            || action == 3
            || action == 5
            || action == 6
            || action == 14
            || action == 15
            || DragonShield.isRaised(dragon)
            || dragon.swinging
            || dragon.getAttackAnim(partial) > 0.0F
            || HumanoidRoar.isRoaring(dragon)
            || ClientTailSweepAnimations.frame(dragon, partial) != null) {
            return true;
         } else if (!(dragon instanceof LightningDragonEntity)) {
            BreathTimeline.Frame breath = ClientBreathAnimations.frame(dragon, partial);
            return breath != null && breath.weight() > 0.0F;
         } else if (BladeMotion.frame(dragon, partial) != null) {
            return true;
         } else {
            IceFemaleRenderer[] stages = ClientRegistration.dragonRenderers.get(FemaleDragonAssets.of(dragon));
            if (stages == null) {
               return false;
            } else {
               int stage = FemaleDragonAssets.stageIndex(MiniatureForm.humanoidModelStage(dragon));
               IceFemaleRenderer renderer = stages[stage];
               return ((IceFemaleModel)renderer.getGeoModel()).lightningBreathFinishing((IceFemaleAnimatable)renderer.getAnimatable(), dragon, partial);
            }
         }
      } else {
         return false;
      }
   }

   public static Vec3 carrierEyes(AbstractClientPlayer player, float partial) {
      DragonBaseEntity dragon = carriedDragon(player, partial);
      return dragon == null ? null : dragon.getPosition(partial).add(0.0, (double)player.getEyeHeight(Pose.STANDING), 0.0);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void hideOccupiedHands(RenderHandEvent event) {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer player = minecraft.player;
      if (player != null && minecraft.getCameraEntity() == player && carriedDragon(player, event.getPartialTick()) != null) {
         event.setCanceled(true);
      }
   }

   public static boolean placePlayer(AbstractClientPlayer player, PoseStack pose, float partial) {
      DragonBaseEntity dragon = mount(player);
      if (dragon == null) {
         return false;
      } else {
         HumanoidRidingConfig.Appearance appearance = appearance(dragon, partial);
         float yaw = Mth.rotLerp(partial, dragon.yBodyRotO, dragon.yBodyRot);
         Vec3 offset = dragon.getPosition(partial).subtract(player.getPosition(partial));
         if (appearance == HumanoidRidingConfig.Appearance.PIGGYBACK) {
            offset = offset.add(Vec3.directionFromRotation(0.0F, yaw).scale(-0.42 * (double)DragonForms.modelScale(dragon)))
               .add(0.0, (double)DragonForms.height(dragon) * 0.62 - 0.7, 0.0);
         }

         pose.translate(offset.x, offset.y + (player.isCrouching() ? 0.125 : 0.0), offset.z);
         pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
         return true;
      }
   }

   public static HumanoidRidingPose.PlayerParts animatePlayer(PlayerModel<?> model, AbstractClientPlayer player, float partial) {
      DragonBaseEntity dragon = mount(player);
      if (dragon == null) {
         return null;
      } else {
         HumanoidRidingPose.PlayerParts previous = new HumanoidRidingPose.PlayerParts(model);
         switch (appearance(dragon, partial)) {
            case PLAYER_CARRIES_DRAGON:
               CARRIER_HEAD.apply(model.head, 0.0F, 0.0F);
               CARRIER_BODY.apply(model.body, 0.0F, 0.0F);
               CARRIER_RIGHT_ARM.apply(model.rightArm, 0.0F, 0.0F);
               CARRIER_LEFT_ARM.apply(model.leftArm, 0.0F, 0.0F);
               LocomotionMotion motion = HumanoidMotion.get(dragon);
               float step = Mth.cos((float)motion.walkTick(partial) * 0.34F) * 0.65F * motion.weight(partial);
               model.rightLeg.setPos(-1.9F, 12.0F, 0.0F);
               model.leftLeg.setPos(1.9F, 12.0F, 0.0F);
               model.rightLeg.setRotation(step, 0.0F, 0.07F);
               model.leftLeg.setRotation(-step, 0.0F, -0.07F);
               break;
            case DRAGON_CARRIES_PLAYER:
               float growth = DragonForms.modelScale(dragon) / 0.9375F - 1.0F;
               float up = (HELD_RIGHT_LEG.y - 24.0F) * growth;
               float forward = HELD_BODY.z * growth;
               HELD_HEAD.apply(model.head, up, forward);
               HELD_BODY.apply(model.body, up, forward);
               HELD_RIGHT_ARM.apply(model.rightArm, up, forward);
               HELD_LEFT_ARM.apply(model.leftArm, up, forward);
               HELD_RIGHT_LEG.apply(model.rightLeg, up, forward);
               HELD_LEFT_LEG.apply(model.leftLeg, up, forward);
               break;
            case PIGGYBACK:
               model.rightArm.setRotation(-1.18F, -0.15F, 0.15F);
               model.leftArm.setRotation(-1.18F, 0.15F, -0.15F);
         }

         model.hat.copyFrom(model.head);
         model.jacket.copyFrom(model.body);
         model.rightSleeve.copyFrom(model.rightArm);
         model.leftSleeve.copyFrom(model.leftArm);
         model.rightPants.copyFrom(model.rightLeg);
         model.leftPants.copyFrom(model.leftLeg);
         return previous;
      }
   }

   private static record PartPose(float x, float y, float z, float pitch, float yaw, float roll) {
      void apply(ModelPart part, float verticalOffset, float forwardOffset) {
         part.setPos(this.x, this.y + verticalOffset, this.z + forwardOffset);
         part.setRotation(this.pitch * (float) (Math.PI / 180.0), this.yaw * (float) (Math.PI / 180.0), this.roll * (float) (Math.PI / 180.0));
      }
   }

   public static final class PlayerParts {
      private final ModelPart[] parts;
      private final net.minecraft.client.model.geom.PartPose[] poses;

      private PlayerParts(PlayerModel<?> model) {
         this.parts = new ModelPart[]{
            model.head,
            model.body,
            model.rightArm,
            model.leftArm,
            model.rightLeg,
            model.leftLeg,
            model.hat,
            model.jacket,
            model.rightSleeve,
            model.leftSleeve,
            model.rightPants,
            model.leftPants
         };
         this.poses = new net.minecraft.client.model.geom.PartPose[this.parts.length];

         for (int i = 0; i < this.parts.length; i++) {
            this.poses[i] = this.parts[i].storePose();
         }
      }

      public void restore() {
         for (int i = 0; i < this.parts.length; i++) {
            net.minecraft.client.model.geom.PartPose saved = this.poses[i];
            this.parts[i].setPos(saved.x, saved.y, saved.z);
            this.parts[i].setRotation(saved.xRot, saved.yRot, saved.zRot);
         }
      }
   }
}
