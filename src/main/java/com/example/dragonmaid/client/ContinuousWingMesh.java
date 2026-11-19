package com.example.dragonmaid.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.animation.state.BoneSnapshot;

final class ContinuousWingMesh {
   final String id;
   final String modelHash;
   final ResourceLocation model;
   final ContinuousWingMesh.Rig[] rig;
   final ContinuousWingMesh.Wing[] wings;
   final Map<String, int[]> hidden;
   final Map<String, Integer> counts;

   private ContinuousWingMesh(
      String id,
      ResourceLocation model,
      String hash,
      ContinuousWingMesh.Rig[] rig,
      ContinuousWingMesh.Wing[] wings,
      Map<String, int[]> hidden,
      Map<String, Integer> counts
   ) {
      this.id = id;
      this.model = model;
      this.modelHash = hash;
      this.rig = rig;
      this.wings = wings;
      this.hidden = hidden;
      this.counts = counts;
   }

   static ContinuousWingMesh parse(JsonObject json) {
      require("dragonmaid.continuous_wing.v1".equals(json.get("schema").getAsString()), "schema");
      float[] tex = floats(json.getAsJsonArray("texture_size"), 2);
      require(tex[0] == 128.0F && tex[1] == 128.0F, "texture dimensions");
      ArrayList<ContinuousWingMesh.Rig> rig = new ArrayList<>();
      HashSet<String> unique = new HashSet<>();

      for (JsonElement item : json.getAsJsonArray("rig")) {
         JsonObject b = item.getAsJsonObject();
         String name = b.get("name").getAsString();
         require(unique.add(name), "duplicate bone");
         rig.add(
            new ContinuousWingMesh.Rig(
               name,
               b.get("parent").isJsonNull() ? null : b.get("parent").getAsString(),
               floats(b.getAsJsonArray("origin"), 3),
               floats(b.getAsJsonArray("rotation"), 3)
            )
         );
      }

      ArrayList<ContinuousWingMesh.Wing> wings = new ArrayList<>();
      HashSet<String> sides = new HashSet<>();

      for (JsonElement item : json.getAsJsonArray("wings")) {
         JsonObject w = item.getAsJsonObject();
         require(sides.add(w.get("side").getAsString()), "duplicate side");
         ArrayList<ContinuousWingMesh.Anchor> anchors = new ArrayList<>();

         for (JsonElement a : w.getAsJsonArray("anchors")) {
            JsonObject j = a.getAsJsonObject();
            String name = j.get("bone").getAsString();
            require(unique.contains(name), "anchor bone");
            anchors.add(new ContinuousWingMesh.Anchor(name, floats(j.getAsJsonArray("bone_local"), 3)));
         }

         require(anchors.size() == 16, "anchor count");
         ArrayList<ContinuousWingMesh.Point> points = new ArrayList<>();

         for (JsonElement v : w.getAsJsonArray("vertices")) {
            JsonObject j = v.getAsJsonObject();
            int[] indices = ints(j.getAsJsonArray("anchors"), 3, anchors.size());
            float[] weights = floats(j.getAsJsonArray("weights"), 3);
            float total = weights[0] + weights[1] + weights[2];
            require((double)Math.abs(total - 1.0F) < 1.0E-5, "weights sum");

            for (float f : weights) {
               require(f >= 0.0F && f <= 1.0F, "negative weight");
            }

            points.add(new ContinuousWingMesh.Point(indices, weights));
         }

         require(points.size() > 2 && points.size() <= 4096, "vertex count");
         ArrayList<ContinuousWingMesh.Face> faces = new ArrayList<>();

         for (JsonElement t : w.getAsJsonArray("triangles")) {
            JsonObject j = t.getAsJsonObject();
            int[] v = ints(j.getAsJsonArray("v"), 3, points.size());
            require(v[0] != v[1] && v[0] != v[2] && v[1] != v[2], "degenerate topology");
            faces.add(new ContinuousWingMesh.Face(v, uv(j.getAsJsonArray("uv_front")), uv(j.getAsJsonArray("uv_back"))));
         }

         require(!faces.isEmpty() && faces.size() <= 8192, "face count");
         wings.add(
            new ContinuousWingMesh.Wing(
               anchors.toArray(ContinuousWingMesh.Anchor[]::new),
               points.toArray(ContinuousWingMesh.Point[]::new),
               faces.toArray(ContinuousWingMesh.Face[]::new)
            )
         );
      }

      require(sides.equals(Set.of("left", "right")), "both wings required");
      HashMap<String, int[]> hidden = new HashMap<>();
      HashMap<String, Integer> counts = new HashMap<>();

      for (Entry<String, JsonElement> e : json.getAsJsonObject("hide_cube_counts").entrySet()) {
         int count = e.getValue().getAsInt();
         require(unique.contains(e.getKey()) && count > 0, "membrane bone/cubes");
         int[] indices = ints(json.getAsJsonObject("hide_cube_indices").getAsJsonArray(e.getKey()), -1, count);
         require(indices.length > 0, "empty replacement");
         HashSet<Integer> distinct = new HashSet<>();

         for (int i : indices) {
            require(distinct.add(i), "duplicate replacement");
         }

         counts.put(e.getKey(), count);
         hidden.put(e.getKey(), indices);
      }

      require(hidden.size() == 10, "membrane mapping");
      return new ContinuousWingMesh(
         json.get("profile").getAsString(),
         ResourceLocation.parse(json.get("model").getAsString()),
         json.get("model_sha256").getAsString(),
         rig.toArray(ContinuousWingMesh.Rig[]::new),
         wings.toArray(ContinuousWingMesh.Wing[]::new),
         Map.copyOf(hidden),
         Map.copyOf(counts)
      );
   }

   static void require(boolean value, String message) {
      if (!value) {
         throw new IllegalArgumentException("Continuous wing: " + message);
      }
   }

   private static float[] floats(JsonArray a, int size) {
      require(a.size() == size, "vector size");
      float[] out = new float[size];

      for (int i = 0; i < size; i++) {
         out[i] = a.get(i).getAsFloat();
         require(Float.isFinite(out[i]), "nonfinite number");
      }

      return out;
   }

   private static int[] ints(JsonArray a, int size, int bound) {
      require(size < 0 || a.size() == size, "indices size");
      int[] out = new int[a.size()];

      for (int i = 0; i < out.length; i++) {
         out[i] = a.get(i).getAsInt();
         require(out[i] >= 0 && out[i] < bound, "index range");
      }

      return out;
   }

   private static float[] uv(JsonArray a) {
      require(a.size() == 3, "UV corners");
      float[] out = new float[6];

      for (int i = 0; i < 3; i++) {
         float[] v = floats(a.get(i).getAsJsonArray(), 2);

         for (int j = 0; j < 2; j++) {
            require(v[j] >= 0.0F && (double)v[j] <= 128.0001, "UV range");
            out[i * 2 + j] = v[j] / 128.0F;
         }
      }

      return out;
   }

   ContinuousWingMesh.Bound bind(BakedGeoModel baked) {
      ArrayList<GeoBone> bones = new ArrayList<>();

      for (GeoBone b : baked.topLevelBones()) {
         flatten(b, bones);
      }

      require(baked.topLevelBones().size() == 1, "one complete root");
      require(bones.size() == this.rig.length, "rig bone count");
      HashMap<String, GeoBone> names = new HashMap<>();

      for (GeoBone b : bones) {
         require(names.put(b.getName(), b) == null, "duplicate baked bone");
      }

      for (ContinuousWingMesh.Rig expected : this.rig) {
         GeoBone bone = names.get(expected.name);
         require(bone != null, "missing bone " + expected.name);
         require(Objects.equals(expected.parent, bone.getParent() == null ? null : bone.getParent().getName()), "parent " + expected.name);
         require(
            near(bone.getPivotX(), expected.pivot[0]) && near(bone.getPivotY(), expected.pivot[1]) && near(bone.getPivotZ(), expected.pivot[2]),
            "pivot " + expected.name
         );
         BoneSnapshot bind = bone.getInitialSnapshot();
         require(bind != null, "missing bind pose " + expected.name);
         require(
            near(bind.getRotX(), (float)Math.toRadians((double)expected.rotation[0]))
               && near(bind.getRotY(), (float)Math.toRadians((double)expected.rotation[1]))
               && near(bind.getRotZ(), (float)Math.toRadians((double)expected.rotation[2])),
            "rotation " + expected.name
         );
      }

      Set<GeoCube> cubes = Collections.newSetFromMap(new IdentityHashMap<>());

      for (Entry<String, int[]> e : this.hidden.entrySet()) {
         GeoBone bone = names.get(e.getKey());
         require(bone.getCubes().size() == this.counts.get(e.getKey()), "cube count " + e.getKey());

         for (int i : e.getValue()) {
            cubes.add((GeoCube)bone.getCubes().get(i));
         }
      }

      return new ContinuousWingMesh.Bound(this, bones, names, cubes);
   }

   private static void flatten(GeoBone b, List<GeoBone> out) {
      out.add(b);

      for (GeoBone c : b.getChildBones()) {
         flatten(c, out);
      }
   }

   private static boolean near(float a, float b) {
      return (double)Math.abs(a - b) < 1.0E-4;
   }

   static record Anchor(String bone, float[] local) {
   }

   static final class Bound {
      final ContinuousWingMesh mesh;
      final GeoBone[] bones;
      final int[] parents;
      final int[] usedBones;
      final Matrix4f[] matrices;
      final int[][] anchorBones;
      final float[][] anchors;
      final float[][] positions;
      private final float[][] drawPositions;
      final Set<GeoCube> hiddenCubes;
      final GeoBone[] membraneBones;
      private final Vector3f temp = new Vector3f();
      private final Vector4f drawPosition = new Vector4f();
      private final float[] previousTransforms;
      private final float[][] faceNormals;
      private boolean positionsValid;

      Bound(ContinuousWingMesh mesh, List<GeoBone> bones, Map<String, GeoBone> names, Set<GeoCube> cubes) {
         this.mesh = mesh;
         this.bones = bones.toArray(GeoBone[]::new);
         this.hiddenCubes = cubes;
         this.membraneBones = mesh.hidden.keySet().stream().map(names::get).toArray(GeoBone[]::new);
         IdentityHashMap<GeoBone, Integer> indices = new IdentityHashMap<>();

         for (int i = 0; i < this.bones.length; i++) {
            indices.put(this.bones[i], i);
         }

         this.parents = new int[bones.size()];
         this.matrices = new Matrix4f[bones.size()];

         for (int i = 0; i < this.parents.length; i++) {
            this.parents[i] = this.bones[i].getParent() == null ? -1 : indices.get(this.bones[i].getParent());
            this.matrices[i] = new Matrix4f();
            ContinuousWingMesh.require(this.parents[i] < i, "parent order");
         }

         this.anchorBones = new int[2][];
         this.anchors = new float[2][];
         this.positions = new float[2][];
         this.drawPositions = new float[2][];

         for (int w = 0; w < 2; w++) {
            ContinuousWingMesh.Wing wing = mesh.wings[w];
            this.anchorBones[w] = new int[wing.anchors.length];
            this.anchors[w] = new float[wing.anchors.length * 3];
            this.positions[w] = new float[wing.points.length * 3];
            this.drawPositions[w] = new float[this.positions[w].length];

            for (int a = 0; a < wing.anchors.length; a++) {
               this.anchorBones[w][a] = indices.get(names.get(wing.anchors[a].bone));
            }
         }

         BitSet ancestors = new BitSet(bones.size());

         for (int[] wing : this.anchorBones) {
            for (int anchor : wing) {
               for (int i = anchor; i >= 0 && !ancestors.get(i); i = this.parents[i]) {
                  ancestors.set(i);
               }
            }
         }

         this.usedBones = ancestors.stream().toArray();
         this.previousTransforms = new float[this.usedBones.length * 9];
         this.faceNormals = new float[][]{new float[mesh.wings[0].faces.length * 3], new float[mesh.wings[1].faces.length * 3]};
      }

      boolean update() {
         for (GeoBone bone : this.membraneBones) {
            if (bone.isHidden()) {
               return false;
            }

            for (GeoBone parent = bone.getParent(); parent != null; parent = parent.getParent()) {
               if (parent.isHidingChildren()) {
                  return false;
               }
            }
         }

         boolean changed = !this.positionsValid;
         int cursor = 0;

         for (int i : this.usedBones) {
            GeoBone bone = this.bones[i];
            changed |= this.remember(cursor++, bone.getPosX());
            changed |= this.remember(cursor++, bone.getPosY());
            changed |= this.remember(cursor++, bone.getPosZ());
            changed |= this.remember(cursor++, bone.getRotX());
            changed |= this.remember(cursor++, bone.getRotY());
            changed |= this.remember(cursor++, bone.getRotZ());
            changed |= this.remember(cursor++, bone.getScaleX());
            changed |= this.remember(cursor++, bone.getScaleY());
            changed |= this.remember(cursor++, bone.getScaleZ());
         }

         if (!changed) {
            return true;
         } else {
            this.positionsValid = false;

            for (int i : this.usedBones) {
               GeoBone b = this.bones[i];
               Matrix4f m = this.matrices[i];
               if (this.parents[i] < 0) {
                  m.identity();
               } else {
                  m.set(this.matrices[this.parents[i]]);
               }

               m.translate(-b.getPosX() / 16.0F, b.getPosY() / 16.0F, b.getPosZ() / 16.0F)
                  .translate(b.getPivotX() / 16.0F, b.getPivotY() / 16.0F, b.getPivotZ() / 16.0F)
                  .rotateZYX(b.getRotZ(), b.getRotY(), b.getRotX())
                  .scale(b.getScaleX(), b.getScaleY(), b.getScaleZ())
                  .translate(-b.getPivotX() / 16.0F, -b.getPivotY() / 16.0F, -b.getPivotZ() / 16.0F);
               if (!m.isFinite()) {
                  return false;
               }
            }

            for (int w = 0; w < 2; w++) {
               ContinuousWingMesh.Wing wing = this.mesh.wings[w];
               float[] a = this.anchors[w];
               float[] p = this.positions[w];

               for (int i = 0; i < wing.anchors.length; i++) {
                  ContinuousWingMesh.Anchor anchor = wing.anchors[i];
                  GeoBone bone = this.bones[this.anchorBones[w][i]];
                  this.matrices[this.anchorBones[w][i]]
                     .transformPosition(
                        (bone.getPivotX() + anchor.local[0]) / 16.0F,
                        (bone.getPivotY() + anchor.local[1]) / 16.0F,
                        (bone.getPivotZ() + anchor.local[2]) / 16.0F,
                        this.temp
                     );
                  a[i * 3] = this.temp.x;
                  a[i * 3 + 1] = this.temp.y;
                  a[i * 3 + 2] = this.temp.z;
               }

               for (int i = 0; i < wing.points.length; i++) {
                  ContinuousWingMesh.Point v = wing.points[i];

                  for (int axis = 0; axis < 3; axis++) {
                     float sum = 0.0F;

                     for (int k = 0; k < 3; k++) {
                        sum += a[v.anchors[k] * 3 + axis] * v.weights[k];
                     }

                     if (!Float.isFinite(sum)) {
                        return false;
                     }

                     p[i * 3 + axis] = sum;
                  }
               }

               updateNormals(wing.faces, p, this.faceNormals[w]);
            }

            this.positionsValid = true;
            return true;
         }
      }

      private boolean remember(int index, float value) {
         boolean changed = this.previousTransforms[index] != value;
         this.previousTransforms[index] = value;
         return changed;
      }

      private static void updateNormals(ContinuousWingMesh.Face[] faces, float[] p, float[] normals) {
         int index = 0;

         for (ContinuousWingMesh.Face face : faces) {
            int a = face.vertices[0] * 3;
            int b = face.vertices[1] * 3;
            int c = face.vertices[2] * 3;
            float x1 = p[b] - p[a];
            float y1 = p[b + 1] - p[a + 1];
            float z1 = p[b + 2] - p[a + 2];
            float x2 = p[c] - p[a];
            float y2 = p[c + 1] - p[a + 1];
            float z2 = p[c + 2] - p[a + 2];
            float nx = y1 * z2 - z1 * y2;
            float ny = z1 * x2 - x1 * z2;
            float nz = x1 * y2 - y1 * x2;
            float length = (float)Math.sqrt((double)(nx * nx + ny * ny + nz * nz));
            if ((double)length < 1.0E-10) {
               nz = 0.0F;
               ny = 0.0F;
               nx = 0.0F;
            } else {
               nx /= length;
               ny /= length;
               nz /= length;
            }

            normals[index++] = nx;
            normals[index++] = ny;
            normals[index++] = nz;
         }
      }

      void draw(PoseStack pose, VertexConsumer out, int light, int overlay, float red, float green, float blue, float alpha) {
         Pose matrix = pose.last();
         BufferBuilder batch = DragonVertexBatch.target(out);
         int color = DragonVertexBatch.color(red, green, blue, alpha);

         for (int w = 0; w < 2; w++) {
            float[] p = this.positions[w];
            float[] draw = this.drawPositions[w];

            for (int i = 0; i < p.length; i += 3) {
               matrix.pose().transform(this.drawPosition.set(p[i], p[i + 1], p[i + 2], 1.0F));
               draw[i] = this.drawPosition.x;
               draw[i + 1] = this.drawPosition.y;
               draw[i + 2] = this.drawPosition.z;
            }

            float[] normals = this.faceNormals[w];
            int normalIndex = 0;

            for (ContinuousWingMesh.Face face : this.mesh.wings[w].faces) {
               int a = face.vertices[0] * 3;
               int b = face.vertices[1] * 3;
               int c = face.vertices[2] * 3;
               float nx = normals[normalIndex++];
               float ny = normals[normalIndex++];
               float nz = normals[normalIndex++];
               if (nx != 0.0F || ny != 0.0F || nz != 0.0F) {
                  matrix.normal().transform(this.temp.set(nx, ny, nz));
                  nx = this.temp.x;
                  ny = this.temp.y;
                  nz = this.temp.z;
                  if (batch != null) {
                     int back = DragonVertexBatch.normal(nx, ny, nz);
                     int front = DragonVertexBatch.normal(-nx, -ny, -nz);
                     emitBatch(batch, draw, a, face.back, 0, color, overlay, light, back);
                     emitBatch(batch, draw, b, face.back, 2, color, overlay, light, back);
                     emitBatch(batch, draw, c, face.back, 4, color, overlay, light, back);
                     emitBatch(batch, draw, c, face.back, 4, color, overlay, light, back);
                     emitBatch(batch, draw, a, face.front, 0, color, overlay, light, front);
                     emitBatch(batch, draw, c, face.front, 4, color, overlay, light, front);
                     emitBatch(batch, draw, b, face.front, 2, color, overlay, light, front);
                     emitBatch(batch, draw, b, face.front, 2, color, overlay, light, front);
                  } else {
                     emit(out, draw, a, face.back, 0, nx, ny, nz, light, overlay, red, green, blue, alpha);
                     emit(out, draw, b, face.back, 2, nx, ny, nz, light, overlay, red, green, blue, alpha);
                     emit(out, draw, c, face.back, 4, nx, ny, nz, light, overlay, red, green, blue, alpha);
                     emit(out, draw, c, face.back, 4, nx, ny, nz, light, overlay, red, green, blue, alpha);
                     emit(out, draw, a, face.front, 0, -nx, -ny, -nz, light, overlay, red, green, blue, alpha);
                     emit(out, draw, c, face.front, 4, -nx, -ny, -nz, light, overlay, red, green, blue, alpha);
                     emit(out, draw, b, face.front, 2, -nx, -ny, -nz, light, overlay, red, green, blue, alpha);
                     emit(out, draw, b, face.front, 2, -nx, -ny, -nz, light, overlay, red, green, blue, alpha);
                  }
               }
            }
         }

         if (batch != null) {
            DragonVertexBatch.flush(batch);
         }
      }

      private static void emitBatch(BufferBuilder batch, float[] p, int index, float[] uv, int corner, int color, int overlay, int light, int normal) {
         DragonVertexBatch.vertex(batch, p[index], p[index + 1], p[index + 2], uv[corner], uv[corner + 1], color, overlay, light, normal);
      }

      private static void emit(
         VertexConsumer out,
         float[] p,
         int index,
         float[] uv,
         int corner,
         float nx,
         float ny,
         float nz,
         int light,
         int overlay,
         float r,
         float g,
         float b,
         float alpha
      ) {
         // 1.21.1: 无 15 参 vertex(...)，改为 addVertex + setXxx 链式调用。
         out.addVertex(p[index], p[index + 1], p[index + 2])
            .setColor(r, g, b, alpha)
            .setUv(uv[corner], uv[corner + 1])
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(nx, ny, nz);
      }
   }

   static record Face(int[] vertices, float[] front, float[] back) {
   }

   static record Point(int[] anchors, float[] weights) {
   }

   static record Rig(String name, String parent, float[] pivot, float[] rotation) {
   }

   static record Wing(ContinuousWingMesh.Anchor[] anchors, ContinuousWingMesh.Point[] points, ContinuousWingMesh.Face[] faces) {
   }
}
