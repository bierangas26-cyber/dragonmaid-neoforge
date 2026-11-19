package com.example.dragonmaid.client;

import com.example.dragonmaid.entity.DragonTridentFlight;
import com.iafenvoy.iceandfire.render.model.TideTridentModel;
import com.iafenvoy.iceandfire.render.entity.TideTridentEntityRenderer;
import com.iafenvoy.iceandfire.registry.IafItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class DragonTridentRenderer extends ThrownTridentRenderer {
   private final TideTridentModel tide = new TideTridentModel();
   private final ItemRenderer items;

   DragonTridentRenderer(Context context) {
      super(context);
      this.items = context.getItemRenderer();
   }

   public void render(ThrownTrident shot, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      ItemStack stack = shot instanceof DragonTridentFlight flight ? flight.displayStack() : ItemStack.EMPTY;
      if (!stack.isEmpty() && !stack.is(Items.TRIDENT)) {
         pose.pushPose();

         try {
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, shot.yRotO, shot.getYRot()) - 90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, shot.xRotO, shot.getXRot()) + 90.0F));
            if (stack.is((Item)IafItems.TIDE_TRIDENT.get())) {
               VertexConsumer vertices = ItemRenderer.getFoilBuffer(buffers, this.tide.renderType(TideTridentEntityRenderer.TRIDENT), false, shot.isFoil());
               this.tide.renderToBuffer(pose, vertices, light, OverlayTexture.NO_OVERLAY);
            } else {
               BakedModel model = this.items.getModel(stack, shot.level(), null, shot.getId());
               if (!model.isGui3d() && !model.isCustomRenderer()) {
                  pose.mulPose(Axis.ZP.rotationDegrees(-135.0F));
               }

               this.items.render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
            }
         } finally {
            pose.popPose();
         }
      } else {
         super.render(shot, yaw, partial, pose, buffers, light);
      }
   }
}
