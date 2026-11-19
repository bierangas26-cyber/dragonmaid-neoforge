package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.KatanaSweep;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class BladeSweepParticle extends TextureSheetParticle {
   private static final int KATANA_SEGMENTS = 24;
   private final double radius;
   private final double halfAngle;
   private final KatanaSweep plane;
   private final boolean legacyWideArc;
   private final BladeSweepParticle.SweepSegment[] katanaArc;

   BladeSweepParticle(ClientLevel level, double x, double y, double z, double yaw, double radius, double angleOrPitch, SpriteSet sprites) {
      super(level, x, y, z);
      boolean tilted = radius < 0.0;
      this.radius = Mth.clamp(Math.abs(radius), 0.1, 16.0);
      this.halfAngle = Math.toRadians(tilted ? 120.0 : Mth.clamp(angleOrPitch, 1.0, 150.0));
      this.plane = tilted ? new KatanaSweep((float)yaw, (float)angleOrPitch) : new KatanaSweep((float)Math.toDegrees(yaw), 0.0F);
      this.legacyWideArc = angleOrPitch > 90.0;
      this.lifetime = !tilted && !this.legacyWideArc ? 7 : 9;
      this.katanaArc = tilted ? this.makeKatanaArc() : null;
      this.pickSprite(sprites);
      this.setBoundingBox(this.getBoundingBox().inflate(this.radius, tilted ? this.radius : 0.4, this.radius));
   }

   public void tick() {
      if (++this.age >= this.lifetime) {
         this.remove();
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void render(VertexConsumer buffer, Camera camera, float partialTick) {
      Vec3 view = camera.getPosition();
      double centerX = this.x - view.x;
      double centerY = this.y - view.y;
      double centerZ = this.z - view.z;
      float time = (float)this.age + partialTick;
      float fade = 1.0F - Mth.clamp((time - 2.0F) / (float)(this.lifetime - 2), 0.0F, 1.0F);
      if (this.katanaArc != null) {
         this.renderKatana(buffer, new Vec3(centerX, centerY, centerZ), fade);
      } else {
         double end = Math.min(1.0, 0.65 + 0.24 * (double)time);
         int segments = this.legacyWideArc ? 24 : 16;

         for (int i = 0; i < segments; i++) {
            double left = (double)i / (double)segments * end;
            double right = (double)(i + 1) / (double)segments * end;
            this.edge(buffer, centerX, centerY, centerZ, left, fade, true);
            this.edge(buffer, centerX, centerY, centerZ, left, fade, false);
            this.edge(buffer, centerX, centerY, centerZ, right, fade, false);
            this.edge(buffer, centerX, centerY, centerZ, right, fade, true);
         }
      }
   }

   private BladeSweepParticle.SweepSegment[] makeKatanaArc() {
      BladeSweepParticle.SweepSegment[] segments = new BladeSweepParticle.SweepSegment[24];
      BladeSweepParticle.SweepPoint left = this.katanaPoint(0.0);

      for (int i = 0; i < segments.length; i++) {
         BladeSweepParticle.SweepPoint right = this.katanaPoint((double)(i + 1) / (double)segments.length);
         BladeSweepParticle.SweepPoint wider = left.taper > right.taper ? left : right;
         Vec3 normal = right.outer.subtract(left.outer).cross(wider.inner.subtract(wider.outer));
         segments[i] = new BladeSweepParticle.SweepSegment(left, right, normal, normal.dot(left.outer));
         left = right;
      }

      return segments;
   }

   private BladeSweepParticle.SweepPoint katanaPoint(double fraction) {
      double angle = (2.0 * fraction - 1.0) * this.halfAngle;
      float taper = (float)Math.sqrt(Math.max(0.0, Math.sin(Math.PI * fraction)));
      Vec3 direction = this.plane.direction(angle);
      double width = this.radius * 0.22 * (double)taper;
      return new BladeSweepParticle.SweepPoint(
         direction.scale(this.radius), direction.scale(this.radius - width).add(this.plane.normal().scale(width * 0.5)), taper
      );
   }

   private void renderKatana(VertexConsumer buffer, Vec3 center, float fade) {
      for (BladeSweepParticle.SweepSegment segment : this.katanaArc) {
         BladeSweepParticle.SweepPoint left = segment.left;
         BladeSweepParticle.SweepPoint right = segment.right;
         if (segment.normal.dot(center) + segment.offset <= 0.0) {
            this.katanaEdge(buffer, center, left, fade, true);
            this.katanaEdge(buffer, center, right, fade, true);
            this.katanaEdge(buffer, center, right, fade, false);
            this.katanaEdge(buffer, center, left, fade, false);
         } else {
            this.katanaEdge(buffer, center, left, fade, false);
            this.katanaEdge(buffer, center, right, fade, false);
            this.katanaEdge(buffer, center, right, fade, true);
            this.katanaEdge(buffer, center, left, fade, true);
         }
      }
   }

   private void katanaEdge(VertexConsumer buffer, Vec3 center, BladeSweepParticle.SweepPoint point, float fade, boolean outer) {
      Vec3 position = outer ? point.outer : point.inner;
      this.vertex(
         buffer,
         center.x + position.x,
         center.y + position.y,
         center.z + position.z,
         outer ? 0.96F : 0.18F,
         outer ? 0.99F : 0.61F,
         fade * point.taper * (outer ? 1.0F : 0.66F)
      );
   }

   private void edge(VertexConsumer buffer, double x, double y, double z, double fraction, float fade, boolean outer) {
      double angle = (2.0 * fraction - 1.0) * this.halfAngle;
      float taper = (float)Math.sqrt(Math.max(0.0, Math.sin(Math.PI * fraction)));
      double width = Math.min(this.radius * 0.35, this.legacyWideArc ? 0.46 : 0.3) * (double)taper;
      double r = outer ? this.radius : this.radius - width;
      double height = Math.sin(angle) * 0.15 + (outer ? width * 0.36 : -width * 0.46);
      Vec3 direction = this.plane.direction(angle);
      Vec3 normal = this.plane.normal();
      float opacity = fade * taper * (outer ? 0.95F : 0.53F);
      float red = outer ? 0.94F : (this.legacyWideArc ? 0.16F : 0.61F);
      float green = outer ? 0.99F : (this.legacyWideArc ? 0.57F : 0.76F);
      this.vertex(
         buffer,
         x + direction.x * r + normal.x * height,
         y + direction.y * r + normal.y * height,
         z + direction.z * r + normal.z * height,
         red,
         green,
         opacity
      );
   }

   private void vertex(VertexConsumer buffer, double x, double y, double z, float red, float green, float alpha) {
      // 1.21.1: vertex()/uv()/color()/uv2()/endVertex() 合并为 addVertex(...) + setXxx 链式调用。
      buffer.addVertex((float)x, (float)y, (float)z)
         .setUv((this.getU0() + this.getU1()) * 0.5F, (this.getV0() + this.getV1()) * 0.5F)
         .setColor(red, green, 1.0F, alpha)
         .setLight(15728880);
   }

   private static record SweepPoint(Vec3 outer, Vec3 inner, float taper) {
   }

   private static record SweepSegment(BladeSweepParticle.SweepPoint left, BladeSweepParticle.SweepPoint right, Vec3 normal, double offset) {
   }
}
