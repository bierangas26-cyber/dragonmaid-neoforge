package com.example.dragonmaid.client;

import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.LoggerFactory;

final class ContinuousWingResources extends SimplePreparableReloadListener<Map<ResourceLocation, ContinuousWingMesh>> {
   static final ContinuousWingResources INSTANCE = new ContinuousWingResources();
   private volatile Map<ResourceLocation, ContinuousWingMesh> meshes = Map.of();

   protected Map<ResourceLocation, ContinuousWingMesh> prepare(ResourceManager resources, ProfilerFiller profiler) {
      HashMap<ResourceLocation, ContinuousWingMesh> result = new HashMap<>();

      for (String species : List.of("ice", "fire", "lightning")) {
         for (String sex : List.of("female", "male")) {
            for (int stage = 3; stage <= 5; stage++) {
               ResourceLocation location = ResourceLocation.fromNamespaceAndPath("dragonmaid", "wing_membranes/" + species + "_" + sex + "_" + stage + ".json");
               if (!resources.getResource(location).isEmpty()) {
                  try (InputStream input = resources.getResourceOrThrow(location).open()) {
                     ContinuousWingMesh mesh = ContinuousWingMesh.parse(
                        JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject()
                     );
                     String model = stage > 3
                        ? "dragon_" + species + "_" + sex + "_stage" + stage
                        : (species.equals("ice") && sex.equals("female") ? "dragon_female.sleep" : "dragon_" + species + "_" + sex);
                     ContinuousWingMesh.require(
                        mesh.id.equals(species + "_" + sex + "_" + stage)
                           && mesh.model.equals(ResourceLocation.fromNamespaceAndPath("dragonmaid", "geo/" + model + ".geo.json")),
                        "model identity"
                     );

                     try (InputStream geometry = resources.getResourceOrThrow(mesh.model).open()) {
                        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(geometry.readAllBytes()));
                        ContinuousWingMesh.require(hash.equals(mesh.modelHash), "model resource changed");
                     }

                     result.put(mesh.model, mesh);
                  } catch (Exception var20) {
                     LoggerFactory.getLogger("DragonMaidWings").warn("Keeping original wings for {}: {}", location, var20.toString());
                  }
               }
            }
         }
      }

      return Map.copyOf(result);
   }

   protected void apply(Map<ResourceLocation, ContinuousWingMesh> prepared, ResourceManager resources, ProfilerFiller profiler) {
      this.meshes = prepared;
      ContinuousWingRenderType.clear();
   }

   ContinuousWingMesh get(ResourceLocation model) {
      return this.meshes.get(model);
   }
}
