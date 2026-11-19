package com.example.dragonmaid.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.BufferedReader;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;

final class PlayerBladeMotion extends SimplePreparableReloadListener<Map<PlayerBladeMotion.Action, PlayerBladeMotion.Clip>> {
   static final PlayerBladeMotion INSTANCE = new PlayerBladeMotion();
   private static final ResourceLocation RESOURCE = ResourceLocation.fromNamespaceAndPath("dragonmaid", "player_motions/player_first_person.motion.json");
   private Map<PlayerBladeMotion.Action, PlayerBladeMotion.Clip> clips = Map.of();
   private static final String[] OBJECTS = new String[]{"RightGrip", "Scabbard", "RightForearm", "LeftForearm"};

   protected Map<PlayerBladeMotion.Action, PlayerBladeMotion.Clip> prepare(ResourceManager resources, ProfilerFiller profiler) {
      try {
         EnumMap var24;
         try (BufferedReader reader = resources.getResourceOrThrow(RESOURCE).openAsReader()) {
            JsonObject source = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("clips");
            EnumMap<PlayerBladeMotion.Action, PlayerBladeMotion.Clip> result = new EnumMap<>(PlayerBladeMotion.Action.class);

            for (PlayerBladeMotion.Action action : PlayerBladeMotion.Action.values()) {
               JsonObject entry = source.getAsJsonObject(action.key);
               float seconds = entry.get("reviewDurationSeconds").getAsFloat();
               JsonArray keys = entry.getAsJsonArray("keyframes");
               if (!(seconds > 0.0F) || !(seconds <= 10.0F) || keys.size() < 2 || keys.size() > 64) {
                  throw new IllegalArgumentException(action.key);
               }

               PlayerBladeMotion.Frame[] frames = new PlayerBladeMotion.Frame[keys.size()];
               float previous = -1.0F;

               for (int index = 0; index < frames.length; index++) {
                  JsonObject key = keys.get(index).getAsJsonObject();
                  float phase = key.get("phase").getAsFloat();
                  if (!Float.isFinite(phase) || phase <= previous || phase > 1.0F || index == 0 && phase != 0.0F || index == frames.length - 1 && phase != 1.0F
                     )
                   {
                     throw new IllegalArgumentException(action.key + " keyframe " + index);
                  }

                  PlayerBladeMotion.Transform[] parts = new PlayerBladeMotion.Transform[action == PlayerBladeMotion.Action.RIPOSTE ? 1 : OBJECTS.length];

                  for (int i = 0; i < parts.length; i++) {
                     JsonObject part = key.getAsJsonObject("objects").getAsJsonObject(OBJECTS[i]);
                     parts[i] = new PlayerBladeMotion.Transform(vector(part.getAsJsonArray("position")), vector(part.getAsJsonArray("rotationZYXDegrees")));
                  }

                  frames[index] = new PlayerBladeMotion.Frame(phase, new PlayerBladeMotion.Pose(parts));
                  previous = phase;
               }

               result.put(action, new PlayerBladeMotion.Clip(seconds, frames));
            }

            var24 = result;
         }

         return var24;
      } catch (Exception var23) {
         LoggerFactory.getLogger(PlayerBladeMotion.class).warn("Could not load first-person blade motions", var23);
         return Map.of();
      }
   }

   protected void apply(Map<PlayerBladeMotion.Action, PlayerBladeMotion.Clip> prepared, ResourceManager resources, ProfilerFiller profiler) {
      this.clips = prepared;
   }

   PlayerBladeMotion.Clip get(PlayerBladeMotion.Action action) {
      return this.clips.get(action);
   }

   private static Vector3f vector(JsonArray values) {
      if (values.size() != 3) {
         throw new IllegalArgumentException("Expected three components");
      } else {
         Vector3f vector = new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
         if (!vector.isFinite()) {
            throw new IllegalArgumentException("Non-finite motion component");
         } else {
            return vector;
         }
      }
   }

   static enum Action {
      RIPOSTE("longsword_riposte"),
      CHARGE("katana_charge"),
      READY("katana_ready"),
      RELEASE("katana_release"),
      RECOVER("katana_recover"),
      CANCEL("katana_cancel");

      final String key;

      private Action(String key) {
         this.key = key;
      }
   }

   static record Clip(float seconds, PlayerBladeMotion.Frame[] frames) {
      PlayerBladeMotion.Pose sample(float time) {
         float phase = Mth.clamp(time / this.seconds, 0.0F, 1.0F);

         for (int index = 1; index < this.frames.length; index++) {
            PlayerBladeMotion.Frame next = this.frames[index];
            if (!(phase > next.phase)) {
               PlayerBladeMotion.Frame previous = this.frames[index - 1];
               return previous.pose.interpolate(next.pose, (phase - previous.phase) / (next.phase - previous.phase));
            }
         }

         return this.frames[this.frames.length - 1].pose;
      }

      PlayerBladeMotion.Clip continueFrom(PlayerBladeMotion.Pose start, float earliestKeySeconds) {
         int first = 1;

         while (first < this.frames.length - 1 && this.frames[first].phase * this.seconds < earliestKeySeconds - 1.0E-5F) {
            first++;
         }

         PlayerBladeMotion.Frame[] result = new PlayerBladeMotion.Frame[this.frames.length - first + 1];
         result[0] = new PlayerBladeMotion.Frame(0.0F, start);

         for (int index = first; index < this.frames.length; index++) {
            int target = index - first + 1;
            result[target] = new PlayerBladeMotion.Frame(this.frames[index].phase, this.frames[index].pose.near(result[target - 1].pose));
         }

         return new PlayerBladeMotion.Clip(this.seconds, result);
      }
   }

   static record Frame(float phase, PlayerBladeMotion.Pose pose) {
   }

   static record Pose(PlayerBladeMotion.Transform[] parts) {
      PlayerBladeMotion.Pose interpolate(PlayerBladeMotion.Pose next, float fraction) {
         PlayerBladeMotion.Transform[] result = new PlayerBladeMotion.Transform[this.parts.length];

         for (int i = 0; i < this.parts.length; i++) {
            result[i] = this.parts[i].interpolate(next.parts[i], fraction);
         }

         return new PlayerBladeMotion.Pose(result);
      }

      PlayerBladeMotion.Pose near(PlayerBladeMotion.Pose previous) {
         PlayerBladeMotion.Transform[] result = new PlayerBladeMotion.Transform[this.parts.length];

         for (int i = 0; i < this.parts.length; i++) {
            result[i] = this.parts[i].near(previous.parts[i]);
         }

         return new PlayerBladeMotion.Pose(result);
      }

      PlayerBladeMotion.Transform grip() {
         return this.parts[0];
      }

      PlayerBladeMotion.Transform scabbard() {
         return this.parts[1];
      }

      PlayerBladeMotion.Transform mainArm() {
         return this.parts[2];
      }

      PlayerBladeMotion.Transform supportArm() {
         return this.parts[3];
      }
   }

   static record Transform(Vector3f position, Vector3f rotation) {
      void apply(PoseStack pose, float side) {
         pose.translate(side * this.position.x, this.position.y, this.position.z);
         pose.mulPose(Axis.ZP.rotationDegrees(side * this.rotation.z));
         pose.mulPose(Axis.YP.rotationDegrees(side * this.rotation.y));
         pose.mulPose(Axis.XP.rotationDegrees(this.rotation.x));
      }

      PlayerBladeMotion.Transform interpolate(PlayerBladeMotion.Transform next, float fraction) {
         return new PlayerBladeMotion.Transform(
            new Vector3f(this.position).lerp(next.position, fraction), new Vector3f(this.rotation).lerp(next.rotation, fraction)
         );
      }

      PlayerBladeMotion.Transform near(PlayerBladeMotion.Transform previous) {
         return new PlayerBladeMotion.Transform(
            this.position,
            new Vector3f(
               previous.rotation.x + Mth.wrapDegrees(this.rotation.x - previous.rotation.x),
               previous.rotation.y + Mth.wrapDegrees(this.rotation.y - previous.rotation.y),
               previous.rotation.z + Mth.wrapDegrees(this.rotation.z - previous.rotation.z)
            )
         );
      }
   }
}
