package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.KatanaCharge;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderHandEvent;

final class KatanaChargeSparks {
   private static final float DURATION = 9.0F;
   private static final RenderType SPARK = RenderType.text(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/particle/glitter_7.png"));

   private KatanaChargeSparks() {
   }

   static void render(RenderHandEvent event, HumanoidArm mainArm, float chargeAge) {
      if (!(chargeAge < 0.0F)) {
         boolean full = chargeAge >= 60.0F;
         int stage = KatanaCharge.stage((long)chargeAge);
         float age = chargeAge - (float)(full ? 60 : stage * 20);
         if (!(age < 0.0F) && !(age >= 9.0F)) {
            int color = KatanaCharge.stageColor(stage);
            float side = mainArm == HumanoidArm.LEFT ? 1.0F : -1.0F;
            Pose matrix = event.getPoseStack().last();
            VertexConsumer vertices = event.getMultiBufferSource().getBuffer(SPARK);
            float coreDuration = full ? 6.0F : 4.0F;
            if (age < coreDuration) {
               int alpha = Math.round(255.0F * (1.0F - Math.max(0.0F, age - 1.0F) / (coreDuration - 1.0F)));
               spark(vertices, matrix, side * 0.28F, -0.17F, (full ? 0.075F : 0.045F) + 0.008F * age, color, alpha);
            }

            float time = age / 9.0F;
            float spread = 0.035F + 0.16F * time;
            int alpha = Math.round(245.0F * (1.0F - time));
            int count = full ? 8 : 5;

            for (int i = 0; i < count; i++) {
               float angle = (float)i * (float) (Math.PI * 2) / (float)count + 0.17F;
               float x = side * (0.28F + Mth.cos(angle) * spread);
               float y = -0.17F + Mth.sin(angle) * spread * 0.68F + 0.045F * time;
               spark(vertices, matrix, x, y, (0.018F + 0.006F * (float)(i % 2)) * (1.0F - 0.5F * time), color, alpha);
            }
         }
      }
   }

   private static void spark(VertexConsumer vertices, Pose matrix, float x, float y, float size, int color, int alpha) {
      vertex(vertices, matrix, x - size, y - size, 0, 1, color, alpha);
      vertex(vertices, matrix, x + size, y - size, 1, 1, color, alpha);
      vertex(vertices, matrix, x + size, y + size, 1, 0, color, alpha);
      vertex(vertices, matrix, x - size, y + size, 0, 0, color, alpha);
   }

   private static void vertex(VertexConsumer vertices, Pose matrix, float x, float y, int u, int v, int color, int alpha) {
      // 1.21.1: 顶点链接口改为 addVertex(...) + setXxx。
      vertices.addVertex(matrix.pose(), x, y, -1.1F)
         .setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha)
         .setUv((float)u, (float)v)
         .setLight(15728880);
   }
}
