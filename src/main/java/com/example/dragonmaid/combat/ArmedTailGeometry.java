package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.world.phys.Vec3;

public final class ArmedTailGeometry {
   private static final Map<String, ArmedTailGeometry.Rig> RIGS = load("armed_tail_geometry");
   private static final Map<String, ArmedTailGeometry.Rig> WATERFOWL_RIGS = load("waterfowl_tail_geometry");

   private ArmedTailGeometry() {
   }

   private static Map<String, ArmedTailGeometry.Rig> load(String file) {
      try {
         Map var17;
         try (InputStream stream = ArmedTailGeometry.class.getResourceAsStream("/data/dragonmaid/combat/" + file + ".json")) {
            if (stream == null) {
               return Map.of();
            }

            JsonObject root = (JsonObject)new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, ArmedTailGeometry.Rig> rigs = new HashMap<>();

            for (Entry<String, JsonElement> e : root.entrySet()) {
               ArmedTailGeometry.Rig rig = (ArmedTailGeometry.Rig)new Gson().fromJson(e.getValue(), ArmedTailGeometry.Rig.class);
               if (rig.samples.length != 11) {
                  throw new IllegalArgumentException("Invalid armed tail samples");
               }

               int length = rig.samples[0].length;
               if (length == 0 || length % 4 != 0 || rig.trail < 0 || rig.trail >= length / 4) {
                  throw new IllegalArgumentException("Invalid tail cube index");
               }

               for (double[] frame : rig.samples) {
                  if (frame.length != length) {
                     throw new IllegalArgumentException("Changing tail topology");
                  }

                  for (int i = 0; i < length; i++) {
                     if (!Double.isFinite(frame[i]) || i % 4 == 3 && frame[i] <= 0.0) {
                        throw new IllegalArgumentException("Invalid tail coordinate");
                     }
                  }
               }

               rigs.put(e.getKey(), rig);
            }

            var17 = Map.copyOf(rigs);
         }

         return var17;
      } catch (Exception var15) {
         LogUtils.getLogger().error("Tail geometry {} unavailable; skipping this skill", file, var15);
         return Map.of();
      }
   }

   public static double reach(DragonBaseEntity dragon) {
      return HumanoidTailSweep.reach(dragon) * 1.4;
   }

   private static ArmedTailGeometry.Rig rig(DragonBaseEntity dragon, boolean waterfowl) {
      String species = dragon instanceof FireDragonEntity ? "fire" : (dragon instanceof LightningDragonEntity ? "lightning" : "ice");
      int stage = Math.max(3, Math.min(5, MiniatureForm.humanoidModelStage(dragon)));
      return (waterfowl ? WATERFOWL_RIGS : RIGS).get(species + "_" + (dragon.isMale() ? "male" : "female") + "_" + stage);
   }

   public static boolean available(DragonBaseEntity dragon) {
      return available(dragon, false);
   }

   static boolean available(DragonBaseEntity dragon, boolean waterfowl) {
      return rig(dragon, waterfowl) != null;
   }

   public static List<TailSweepGeometry.Frame> frames(DragonBaseEntity dragon, float yaw) {
      return frames(dragon, yaw, false);
   }

   static List<TailSweepGeometry.Frame> frames(DragonBaseEntity dragon, float yaw, boolean waterfowl) {
      ArmedTailGeometry.Rig rig = rig(dragon, waterfowl);
      if (rig == null) {
         return List.of();
      } else {
         double scale = (double)DragonForms.modelScale(dragon) / 16.0;
         double angle = Math.toRadians((double)yaw);
         double cos = Math.cos(angle);
         double sin = Math.sin(angle);
         List<TailSweepGeometry.Frame> frames = new ArrayList<>(11);

         for (double[] sample : rig.samples) {
            List<TailSweepGeometry.Sphere> spheres = new ArrayList<>(sample.length / 4 + 7);

            for (int i = 0; i < sample.length; i += 4) {
               Vec3 center = new Vec3(
                  dragon.getX() + (-cos * sample[i] + sin * sample[i + 2]) * scale,
                  dragon.getY() + sample[i + 1] * scale + 0.01 * (double)DragonForms.modelScale(dragon),
                  dragon.getZ() + (-sin * sample[i] - cos * sample[i + 2]) * scale
               );
               spheres.add(new TailSweepGeometry.Sphere(center, sample[i + 3] * scale));
            }

            Vec3 tip = spheres.get(rig.trail).center();
            Vec3 radial = new Vec3(tip.x - dragon.getX(), 0.0, tip.z - dragon.getZ()).normalize();
            double outer = Math.max(0.0, reach(dragon) - 0.55 - tip.subtract(dragon.position()).horizontalDistance());
            Vec3 edge = tip.add(radial.scale(outer));

            for (int i = 0; i < 7; i++) {
               spheres.add(new TailSweepGeometry.Sphere(tip.lerp(edge, (double)i / 6.0), 0.55));
            }

            frames.add(new TailSweepGeometry.Frame(List.copyOf(spheres), edge));
         }

         return List.copyOf(frames);
      }
   }

   private static record Rig(int trail, double[][] samples) {
   }
}
