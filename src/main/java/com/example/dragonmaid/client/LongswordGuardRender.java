package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.SpecialWeapons;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(
   modid = "dragonmaid",
   value = {Dist.CLIENT}
)
public final class LongswordGuardRender {
   private static final Quaternionf SHEATH_TO_GRIP = Axis.of(new Vector3f(1.0F, 0.0F, -1.0F).normalize()).rotationDegrees(180.0F);

   private LongswordGuardRender() {
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void render(RenderHandEvent event) {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer player = minecraft.player;
      if (player != null && minecraft.getCameraEntity() == player && !player.isScoping()) {
         if (SpecialWeapons.isKatana(player.getMainHandItem())) {
            PlayerBladeMotion.Pose pose = PlayerBladeAnimation.katana(player, event.getPartialTick());
            if (event.getHand() == InteractionHand.MAIN_HAND) {
               if (pose != null) {
                  renderKatana(minecraft, player, event, pose);
               }

               KatanaChargeSparks.render(event, player.getMainArm(), PlayerBladeAnimation.katanaChargeAge(player, event.getPartialTick()));
            }

            if (pose != null) {
               event.setCanceled(true);
            }
         } else if (event.getHand() == InteractionHand.MAIN_HAND && SpecialWeapons.isLongsword(event.getItemStack())) {
            PlayerBladeMotion.Pose riposte = PlayerBladeAnimation.longsword(player, event.getPartialTick());
            if (riposte != null || player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND && SpecialWeapons.isLongsword(player.getUseItem())) {
               boolean left = player.getMainArm() == HumanoidArm.LEFT;
               float side = left ? -1.0F : 1.0F;
               PoseStack posex = event.getPoseStack();
               posex.pushPose();

               label106: {
                  try {
                     if (riposte == null) {
                        posex.translate(side * 0.56F, -0.52F - 0.6F * event.getEquipProgress(), -0.72F);
                        posex.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
                        posex.scale(0.4F, 0.4F, 0.4F);
                        posex.translate(side * -0.5F, 0.2F, 0.0F);
                        posex.mulPose(Axis.YP.rotationDegrees(side * 30.0F));
                        posex.mulPose(Axis.XP.rotationDegrees(-80.0F));
                        posex.mulPose(Axis.YP.rotationDegrees(side * 60.0F));
                        posex.translate(0.0F, 0.25F, 0.125F);
                        posex.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
                        posex.mulPose(Axis.ZP.rotationDegrees(side * 25.0F));
                        posex.scale(3.4F, 3.4F, 1.7F);
                        minecraft.getItemRenderer()
                           .renderStatic(
                              player,
                              event.getItemStack(),
                              ItemDisplayContext.NONE,
                              left,
                              posex,
                              event.getMultiBufferSource(),
                              player.level(),
                              event.getPackedLight(),
                              OverlayTexture.NO_OVERLAY,
                              player.getId()
                           );
                        break label106;
                     }

                     posex.translate(0.0F, -0.6F * event.getEquipProgress(), 0.0F);
                     riposte.grip().apply(posex, side);
                     renderBlade(minecraft, player, event, side, 1.36F, 0.265625F);
                     event.setCanceled(true);
                  } finally {
                     posex.popPose();
                  }

                  return;
               }

               event.setCanceled(true);
            }
         }
      }
   }

   private static void renderKatana(Minecraft minecraft, LocalPlayer player, RenderHandEvent event, PlayerBladeMotion.Pose motion) {
      float side = player.getMainArm() == HumanoidArm.LEFT ? -1.0F : 1.0F;
      KatanaSheathLayer.Style sheath = KatanaSheathLayer.style(player.getMainHandItem());
      PoseStack pose = event.getPoseStack();
      pose.pushPose();

      try {
         motion.grip().apply(pose, side);
         pose.pushPose();

         try {
            renderBlade(minecraft, player, event, side, 0.68F, sheath.gripOffset);
         } finally {
            pose.popPose();
         }

         motion.mainArm().apply(pose, side);
         renderArm(minecraft, player, event, side < 0.0F);
      } finally {
         pose.popPose();
      }

      pose.pushPose();

      try {
         motion.scabbard().apply(pose, side);
         pose.pushPose();

         try {
            pose.scale(side * 0.68F, 0.68F, 0.68F);
            pose.mulPose(SHEATH_TO_GRIP);
            KatanaSheathLayer.renderModel(pose, event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, sheath);
         } finally {
            pose.popPose();
         }

         motion.supportArm().apply(pose, side);
         renderArm(minecraft, player, event, side > 0.0F);
      } finally {
         pose.popPose();
      }
   }

   private static void renderBlade(Minecraft minecraft, LocalPlayer player, RenderHandEvent event, float side, float scale, float gripOffset) {
      PoseStack pose = event.getPoseStack();
      pose.scale(side, 1.0F, 1.0F);
      pose.mulPose(Axis.ZP.rotationDegrees(45.0F));
      pose.scale(scale, scale, 0.68F);
      pose.translate(gripOffset, gripOffset, 0.0F);
      minecraft.getItemRenderer()
         .renderStatic(
            player,
            player.getMainHandItem(),
            ItemDisplayContext.NONE,
            side < 0.0F,
            pose,
            event.getMultiBufferSource(),
            player.level(),
            event.getPackedLight(),
            OverlayTexture.NO_OVERLAY,
            player.getId()
         );
   }

   private static void renderArm(Minecraft minecraft, LocalPlayer player, RenderHandEvent event, boolean left) {
      if (!player.isInvisible() && minecraft.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
         boolean var7 = player.getSkin().model() == PlayerSkin.Model.SLIM;
         PoseStack pose = event.getPoseStack();
         pose.translate(0.0, 0.046875, 0.0);
         pose.scale(0.675F, 0.73F, 0.675F);
         pose.translate((double)(left ? -1 : 1) * (var7 ? 5.5 : 6.0) / 16.0, -(var7 ? 12.5 : 12.0) / 16.0, 0.0);
         if (left) {
            renderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
         } else {
            renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
         }
      }
   }
}
