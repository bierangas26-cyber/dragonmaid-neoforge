package com.example.dragonmaid.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.fml.ModList;

public final class HumanoidWallOcclusion {
   private static final boolean SHADERS = ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");
   private static boolean shadowApiAvailable = SHADERS;

   private HumanoidWallOcclusion() {
   }

   public static boolean hidden(AABB bounds, double x, double y, double z) {
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      Camera camera = minecraft.gameRenderer.getMainCamera();
      Vec3 eye = camera.getPosition();
      if (level != null && camera.isInitialized() && camera.getEntity() != null && !(eye.distanceToSqr(x, y, z) > 1.0E-6) && !bounds.contains(eye) && !inShadowPass()) {
         Vec3 center = bounds.getCenter();
         double distance = eye.distanceToSqr(center);
         if (!(distance < 36.0) && !(distance > 2304.0)) {
            BlockHitResult hit = level.clip(new ClipContext(eye, center, Block.VISUAL, Fluid.NONE, camera.getEntity()));
            if (hit.getType() == Type.BLOCK && !hit.isInside()) {
               BlockPos pos = hit.getBlockPos();
               Axis axis = hit.getDirection().getAxis();
               Axis u = axis == Axis.X ? Axis.Y : Axis.X;
               Axis v = axis == Axis.Z ? Axis.Y : Axis.Z;
               int slice = pos.get(axis);
               double plane = (double)slice + 0.5;
               double eyeA = eye.get(axis);
               double eyeU = eye.get(u);
               double eyeV = eye.get(v);
               double minA = bounds.min(axis);
               double maxA = bounds.max(axis);
               if (eyeA < plane && plane < minA || eyeA > plane && plane > maxA) {
                  double t0 = (plane - eyeA) / (minA - eyeA);
                  double t1 = (plane - eyeA) / (maxA - eyeA);
                  double tMin = Math.min(t0, t1);
                  double tMax = Math.max(t0, t1);
                  int minU = lower(eyeU, bounds.min(u), tMin, tMax);
                  int maxU = upper(eyeU, bounds.max(u), tMin, tMax);
                  int minV = lower(eyeV, bounds.min(v), tMin, tMax);
                  int maxV = upper(eyeV, bounds.max(v), tMin, tMax);
                  if (((long)(maxU - minU) + 1L) * ((long)(maxV - minV) + 1L) > 64L) {
                     return false;
                  } else {
                     MutableBlockPos block = new MutableBlockPos();

                     for (int col = minU; col <= maxU; col++) {
                        for (int row = minV; row <= maxV; row++) {
                           block.set(axis == Axis.X ? slice : col, axis == Axis.Y ? slice : (axis == Axis.X ? col : row), axis == Axis.Z ? slice : row);
                           if (!level.hasChunkAt(block)) {
                              return false;
                           }

                           BlockState state = level.getBlockState(block);
                           if (state.getRenderShape() != RenderShape.MODEL
                              || state.hasBlockEntity()
                              || state.getLightBlock(level, block) != level.getMaxLightLevel()
                              || !state.isSolidRender(level, block)
                              || !state.isCollisionShapeFullBlock(level, block)) {
                              return false;
                           }
                        }
                     }

                     return true;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static int lower(double eye, double edge, double tMin, double tMax) {
      return Mth.floor(eye + (edge - eye) * (edge < eye ? tMax : tMin) - 1.0E-4);
   }

   private static int upper(double eye, double edge, double tMin, double tMax) {
      return Mth.floor(eye + (edge - eye) * (edge < eye ? tMin : tMax) + 1.0E-4);
   }

   private static boolean inShadowPass() {
      if (!SHADERS) {
         return false;
      } else {
         if (shadowApiAvailable) {
            try {
               // 1.21.1 本仓不直接依赖 Iris（libs 无 iris/oculus），这里用反射调同名的 IrisApi 入口。
               Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
               Object instance = api.getMethod("getInstance").invoke(null);
               return (Boolean)api.getMethod("isRenderingShadowPass").invoke(instance);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError var1) {
               shadowApiAvailable = false;
            }
         }

         return true;
      }
   }
}
