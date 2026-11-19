package com.example.dragonmaid.client;

import com.example.dragonmaid.block.RecallAltarBlockEntity;
import com.example.dragonmaid.recall.RecallContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "dragonmaid",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class RecallAltarRenderer implements BlockEntityRenderer<RecallAltarBlockEntity> {
   private static final ModelResourceLocation GEM_MODEL = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("dragonmaid", "block/dragon_recall_altar_gem"));
   private static final float GEM_PIVOT_Y = 0.803125F;
   private final BlockRenderDispatcher blocks;

   private RecallAltarRenderer(Context context) {
      this.blocks = context.getBlockRenderDispatcher();
   }

   @SubscribeEvent
   public static void registerModel(RegisterAdditional event) {
      event.register(GEM_MODEL);
   }

   @SubscribeEvent
   public static void registerRenderer(RegisterRenderers event) {
      event.registerBlockEntityRenderer((BlockEntityType)RecallContent.ALTAR_ENTITY.get(), RecallAltarRenderer::new);
   }

   public void render(RecallAltarBlockEntity altar, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
      BakedModel model = this.blocks.getBlockModelShaper().getModelManager().getModel(GEM_MODEL);
      pose.pushPose();
      pose.translate(0.5, (double)altar.gemHeight(partialTick), 0.5);
      pose.mulPose(Axis.YP.rotation(-altar.gemYaw(partialTick) - (float) (Math.PI / 2)));
      pose.translate(-0.5, -0.803125F, -0.5);
      this.blocks
         .getModelRenderer()
         .renderModel(pose.last(), buffers.getBuffer(Sheets.solidBlockSheet()), null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, null);
      pose.popPose();
   }
}
