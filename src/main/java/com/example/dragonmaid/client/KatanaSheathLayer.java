package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.SpecialWeapons;
import com.example.dragonmaid.item.DragonBloodBladeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

final class KatanaSheathLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final ResourceLocation DRAGONBONE_KATANA = ResourceLocation.fromNamespaceAndPath("dragonmaid", "dragonbone_katana");
   private static final float THIRD_PERSON_SCALE = 1.0625F;

   static KatanaSheathLayer.Style style(ItemStack weapon) {
      return !(weapon.getItem() instanceof DragonBloodBladeItem) && !DRAGONBONE_KATANA.equals(BuiltInRegistries.ITEM.getKey(weapon.getItem()))
         ? KatanaSheathLayer.Style.STANDARD
         : KatanaSheathLayer.Style.DRAGONBONE;
   }

   KatanaSheathLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   public void render(
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float headYaw,
      float headPitch
   ) {
      if (!player.isInvisible()) {
         ItemStack weapon = player.getMainHandItem();
         if (!SpecialWeapons.isKatana(weapon)) {
            weapon = player.getOffhandItem();
            if (!SpecialWeapons.isKatana(weapon)) {
               return;
            }
         }

         pose.pushPose();

         try {
            ((PlayerModel)this.getParentModel()).leftLeg.translateAndRotate(pose);
            pose.scale(-1.0F, -1.0F, 1.0F);
            renderMounted(pose, buffers, light, LivingEntityRenderer.getOverlayCoords(player, 0.0F), -3.0, style(weapon));
         } finally {
            pose.popPose();
         }
      }
   }

   static void renderMounted(PoseStack pose, MultiBufferSource buffers, int light, int overlay, double offsetX, KatanaSheathLayer.Style style) {
      pose.translate(offsetX / 16.0, -0.025, 0.0);
      pose.mulPose(Axis.XP.rotationDegrees(-30.0F));
      pose.scale(1.0625F, 1.0625F, 1.0625F);
      renderModel(pose, buffers, light, overlay, style);
   }

   static void renderModel(PoseStack pose, MultiBufferSource buffers, int light, int overlay, KatanaSheathLayer.Style style) {
      BakedGeoModel model = (BakedGeoModel)GeckoLibCache.getBakedModels().get(style.model);
      if (model != null) {
         VertexConsumer vertices = buffers.getBuffer(style.type);
         Vector3f normal = new Vector3f();
         Vector4f point = new Vector4f();
         Pose base = pose.last();
         pose.pushPose();

         try {
            Pose matrix = pose.last();
            GeoCube previous = null;

            for (GeoBone root : model.topLevelBones()) {
               for (GeoCube cube : root.getCubes()) {
                  if (previous == null || !previous.rotation().equals(cube.rotation()) || !previous.pivot().equals(cube.pivot())) {
                     matrix.pose().set(base.pose());
                     matrix.normal().set(base.normal());
                     RenderUtil.translateToPivotPoint(pose, cube);
                     RenderUtil.rotateMatrixAroundCube(pose, cube);
                     RenderUtil.translateAwayFromPivotPoint(pose, cube);
                  }

                  previous = cube;

                  for (GeoQuad quad : cube.quads()) {
                     if (quad != null) {
                        matrix.normal().transform(normal.set(quad.normal())).normalize();

                        for (GeoVertex vertex : quad.vertices()) {
                           Vector3f position = vertex.position();
                           matrix.pose().transform(point.set(position.x(), position.y(), position.z(), 1.0F));
                           // 1.21.1: 15 参 vertex(...) 已移除，改用 addVertex + setXxx。
                           vertices.addVertex(point.x, point.y, point.z)
                              .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                              .setUv(vertex.texU(), vertex.texV())
                              .setOverlay(overlay)
                              .setLight(light)
                              .setNormal(normal.x, normal.y, normal.z);
                        }
                     }
                  }
               }
            }
         } finally {
            pose.popPose();
         }
      }
   }

   static enum Style {
      STANDARD("katana_sheath", 0.3F),
      DRAGONBONE("dragonbone_katana_sheath", 0.315625F);

      final ResourceLocation model;
      final RenderType type;
      final float gripOffset;

      private Style(String asset, float gripOffset) {
         this.model = ResourceLocation.fromNamespaceAndPath("dragonmaid", "geo/" + asset + ".geo.json");
         this.type = RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("dragonmaid", "textures/item/" + asset + ".png"));
         this.gripOffset = gripOffset;
      }
   }
}
