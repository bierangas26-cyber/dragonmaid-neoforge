package com.example.dragonmaid.client;

import com.example.dragonmaid.item.ServiceDragonArmorItem;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.TabulaModel;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.util.RenderUtil;

public final class DragonArmorOrnaments {
   private DragonArmorOrnaments() {
   }

   static DragonArmorOrnaments.Mesh bake(JsonObject source, ResourceLocation texture) {
      JsonObject geometry = source.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
      JsonObject description = geometry.getAsJsonObject("description");
      if (description.get("texture_width").getAsInt() == 128 && description.get("texture_height").getAsInt() == 128) {
         for (JsonElement entry : geometry.getAsJsonArray("bones")) {
            JsonObject bone = entry.getAsJsonObject();
            if (bone.has("cubes")) {
               for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                  ServiceArmorResources.validateCube(cube.getAsJsonObject(), texture.toString());
               }
            }
         }

         BakedGeoModel baked = BakedModelFactory.getForNamespace("dragonmaid")
            .constructGeoModel(GeometryTree.fromModel((Model)KeyFramesAdapter.GEO_GSON.fromJson(source, Model.class)));
         ArrayList<DragonArmorOrnaments.Attachment> head = new ArrayList<>();
         ArrayList<DragonArmorOrnaments.Attachment> body = new ArrayList<>();

         for (GeoBone root : baked.topLevelBones()) {
            collect(root, head, body);
         }

         return new DragonArmorOrnaments.Mesh(texture, List.copyOf(head), List.copyOf(body));
      } else {
         throw new IllegalArgumentException("Dragon armor ornament UVs must use a 128x128 texture: " + texture);
      }
   }

   private static void collect(GeoBone bone, List<DragonArmorOrnaments.Attachment> head, List<DragonArmorOrnaments.Attachment> body) {
      if (!bone.getCubes().isEmpty()) {
         ArrayList<DragonArmorOrnaments.Face> faces = new ArrayList<>();
         PoseStack local = new PoseStack();

         for (GeoCube cube : bone.getCubes()) {
            local.pushPose();
            RenderUtil.translateToPivotPoint(local, cube);
            RenderUtil.rotateMatrixAroundCube(local, cube);
            RenderUtil.translateAwayFromPivotPoint(local, cube);

            for (GeoQuad quad : cube.quads()) {
               if (quad != null) {
                  Vector3f normal = local.last().normal().transform(new Vector3f(quad.normal()));
                  RenderUtil.fixInvertedFlatCube(cube, normal);
                  GeoVertex[] vertices = new GeoVertex[quad.vertices().length];

                  for (int i = 0; i < vertices.length; i++) {
                     GeoVertex vertex = quad.vertices()[i];
                     Vector3f point = local.last().pose().transformPosition(new Vector3f(vertex.position()));
                     point.set(-point.x + bone.getPivotX() / 16.0F, -point.y + bone.getPivotY() / 16.0F, point.z - bone.getPivotZ() / 16.0F);
                     vertices[i] = new GeoVertex(point, vertex.texU(), vertex.texV());
                  }

                  faces.add(new DragonArmorOrnaments.Face(vertices, normal.set(-normal.x, -normal.y, normal.z)));
               }
            }

            local.popPose();
         }

         String var19 = bone.getName();

         List<DragonArmorOrnaments.Attachment> slot = switch (var19) {
            case "Head", "HornL", "HornR" -> head;
            case "BodyUpper", "BodyLower", "armL1", "armL2", "armR1", "armR2", "WingExistingPositiveX1", "WingExistingPositiveX2" -> body;
            default -> throw new IllegalArgumentException("Unknown dragon armor ornament bone: " + bone.getName());
         };
         slot.add(new DragonArmorOrnaments.Attachment(bone.getName(), List.copyOf(faces)));
      }

      for (GeoBone child : bone.getChildBones()) {
         collect(child, head, body);
      }
   }

   public static void render(DragonBaseEntity dragon, AdvancedEntityModel<DragonBaseEntity> model, PoseStack pose, MultiBufferSource buffers, int light) {
      if (model instanceof TabulaModel tabula && !dragon.isInvisible()) {
         renderSlot(dragon, tabula, EquipmentSlot.HEAD, pose, buffers, light);
         renderSlot(dragon, tabula, EquipmentSlot.LEGS, pose, buffers, light);
         return;
      }
   }

   private static void renderSlot(DragonBaseEntity dragon, TabulaModel model, EquipmentSlot slot, PoseStack pose, MultiBufferSource buffers, int light) {
      ServiceDragonArmorItem.Style style = ServiceDragonArmorItem.styleOf(dragon.getItemBySlot(slot), dragon);
      if (style != null) {
         DragonArmorOrnaments.Mesh mesh = ServiceArmorResources.of(style).ornaments(dragon);
         if (mesh != null) {
            List<DragonArmorOrnaments.Attachment> attachments = slot == EquipmentSlot.HEAD ? mesh.head() : mesh.body();
            if (!attachments.isEmpty()) {
               VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(mesh.texture()));

               for (DragonArmorOrnaments.Attachment attachment : attachments) {
                  AdvancedModelBox bone = resolveBone(model, attachment.bone(), dragon instanceof LightningDragonEntity);
                  if (bone != null) {
                     pose.pushPose();

                     try {
                        if (applyBonePose(pose, bone, false)) {
                           draw(attachment, pose.last(), consumer, light);
                        }
                     } finally {
                        pose.popPose();
                     }
                  }
               }
            }
         }
      }
   }

   private static AdvancedModelBox resolveBone(TabulaModel model, String name, boolean lightning) {
      if (!lightning) {
         return model.getCube(name);
      } else {
         return switch (name) {
            case "armL1" -> lightningArm(model, true);
            case "armL2" -> lightningForewing(lightningArm(model, true));
            case "WingExistingPositiveX1" -> lightningArm(model, false);
            case "WingExistingPositiveX2" -> lightningForewing(lightningArm(model, false));
            default -> model.getCube(name);
         };
      }
   }

   private static AdvancedModelBox lightningArm(TabulaModel model, boolean positive) {
      AdvancedModelBox body = model.getCube("BodyUpper");
      if (body == null) {
         return null;
      } else {
         ObjectListIterator var3 = body.childModels.iterator();

         while (var3.hasNext()) {
            BasicModelPart child = (BasicModelPart)var3.next();
            if (child instanceof AdvancedModelBox arm
               && (positive ? arm.defaultPositionX > 0.0F : arm.defaultPositionX < 0.0F)
               && ("armL1".equals(arm.boxName) || "armR1".equals(arm.boxName))) {
               return arm;
            }
         }

         return null;
      }
   }

   private static AdvancedModelBox lightningForewing(AdvancedModelBox arm) {
      if (arm == null) {
         return null;
      } else {
         ObjectListIterator var1 = arm.childModels.iterator();

         while (var1.hasNext()) {
            BasicModelPart child = (BasicModelPart)var1.next();
            if (child instanceof AdvancedModelBox wing && ("armL2".equals(wing.boxName) || "armR2".equals(wing.boxName))) {
               return wing;
            }
         }

         return null;
      }
   }

   private static boolean applyBonePose(PoseStack pose, AdvancedModelBox bone, boolean parent) {
      if (bone.showModel && (bone.getParent() == null || applyBonePose(pose, bone.getParent(), true))) {
         bone.translateAndRotate(pose);
         if (parent && !bone.scaleChildren) {
            pose.scale(1.0F / Math.max(bone.scaleX, 1.0E-4F), 1.0F / Math.max(bone.scaleY, 1.0E-4F), 1.0F / Math.max(bone.scaleZ, 1.0E-4F));
         }

         return true;
      } else {
         return false;
      }
   }

   private static void draw(DragonArmorOrnaments.Attachment attachment, Pose pose, VertexConsumer consumer, int light) {
      for (DragonArmorOrnaments.Face face : attachment.faces()) {
         Vector3f normal = face.normal();

         for (GeoVertex vertex : face.vertices()) {
            Vector3f point = vertex.position();
            // 1.21.1: addVertex + setXxx（原 vertex/color/uv/overlayCoords/uv2/normal/endVertex）。
            consumer.addVertex(pose.pose(), point.x, point.y, point.z)
               .setColor(255, 255, 255, 255)
               .setUv(vertex.texU(), vertex.texV())
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(light)
               .setNormal(pose, normal.x, normal.y, normal.z);
         }
      }
   }

   private static record Attachment(String bone, List<DragonArmorOrnaments.Face> faces) {
   }

   private static record Face(GeoVertex[] vertices, Vector3f normal) {
   }

   static record Mesh(ResourceLocation texture, List<DragonArmorOrnaments.Attachment> head, List<DragonArmorOrnaments.Attachment> body) {
   }
}
