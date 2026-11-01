package com.example.dragonmaid.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;

/**
 * GeckoLib 4.9.x port helper for the mod's own bedrock .geo files.
 *
 * <p>Two 4.7 -> 4.9 changes bite here:</p>
 * <ol>
 *   <li>Parsing a raw {@code JsonObject} into GeckoLib's {@code Model} needs <b>GeckoLib's own Gson</b>:
 *       GeckoLib moved its fully-configured instance from {@code JsonUtil.GEO_GSON} (4.7) to
 *       {@code KeyFramesAdapter.GEO_GSON} (4.9). A plain {@code new GsonBuilder().create()} matches JSON
 *       keys against record component names, so {@code format_version} / {@code minecraft:geometry} /
 *       {@code description} all come back null and the loader NPEs.</li>
 *   <li>{@code BakedModelFactory#constructBone} dereferences {@code bone.pivot()}, {@code bone.rotation()}
 *       and {@code bone.cubes()} without null checks, while 4.7 defaulted missing values. The mod's geo
 *       files omit them on some bones (joint-only bones), so the defaults are filled in before parsing.</li>
 * </ol>
 */
final class GeoModelJson {
    /** GeckoLib's canonical Gson (model + keyframe type adapters). */
    static final Gson GSON = KeyFramesAdapter.GEO_GSON;

    private GeoModelJson() {
    }

    /** Deep-copies a bedrock geometry JSON and fills the bone defaults GeckoLib 4.9 no longer tolerates. */
    static JsonObject normalize(JsonObject geo) {
        JsonObject copy = geo.deepCopy();
        JsonArray geometries = copy.getAsJsonArray("minecraft:geometry");
        if (geometries == null) {
            return copy;
        }

        for (JsonElement geometry : geometries) {
            if (!geometry.isJsonObject()) {
                continue;
            }
            JsonArray bones = geometry.getAsJsonObject().getAsJsonArray("bones");
            if (bones == null) {
                continue;
            }
            for (JsonElement boneElement : bones) {
                if (!boneElement.isJsonObject()) {
                    continue;
                }
                JsonObject bone = boneElement.getAsJsonObject();
                if (!bone.has("pivot")) {
                    bone.add("pivot", vector(0.0, 0.0, 0.0));
                }
                if (!bone.has("rotation")) {
                    bone.add("rotation", vector(0.0, 0.0, 0.0));
                }
                if (!bone.has("cubes")) {
                    bone.add("cubes", new JsonArray());
                }
            }
        }

        return copy;
    }

    private static JsonArray vector(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
