package com.example.dragonmaid.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;
import software.bernie.geckolib.util.RenderUtil;


final class FemaleChestplateMesh {
    private final float[] geometry;
    private final Map<String, float[]> uvProfiles;
    private final int cubeCount;

    private FemaleChestplateMesh(float[] geometry, Map<String, float[]> profiles, int cubes) {
        this.geometry = geometry;
        this.uvProfiles = Map.copyOf(profiles);
        this.cubeCount = cubes;
    }

    int cubeCount() {
        return this.cubeCount;
    }

    int vertexCount() {
        return this.geometry.length / 6;
    }

    static FemaleChestplateMesh load(JsonObject geo, JsonObject profiles) {
        require("dragonmaid.chestplate.uv-profiles.v1".equals(profiles.get("schema").getAsString()), "UV schema");
        require("1.12.0".equals(geo.get("format_version").getAsString()), "geo format");
        JsonArray jsonarray = geo.getAsJsonArray("minecraft:geometry");
        require(jsonarray.size() == 1, "single geometry");
        JsonArray jsonarray1 = jsonarray.get(0).getAsJsonObject().getAsJsonArray("bones");
        require(jsonarray1.size() == 1, "single Body root");
        JsonObject jsonobject = jsonarray1.get(0).getAsJsonObject();
        require("Body".equals(jsonobject.get("name").getAsString()), "Body root name");
        require(!jsonobject.has("parent"), "root parent absent");
        requireVector(jsonobject.getAsJsonArray("pivot"), new double[]{0.0, 24.0, 0.0}, "Body pivot");

        for (String s : new String[]{"rotation", "position"}) {
            if (jsonobject.has(s)) {
                requireVector(jsonobject.getAsJsonArray(s), new double[]{0.0, 0.0, 0.0}, "Body " + s);
            }
        }

        if (jsonobject.has("scale")) {
            requireVector(jsonobject.getAsJsonArray("scale"), new double[]{1.0, 1.0, 1.0}, "Body scale");
        }

        JsonArray jsonarray3 = jsonobject.getAsJsonArray("cubes");
        JsonArray jsonarray4 = profiles.getAsJsonArray("cube_order");
        require(jsonarray3.size() > 0 && jsonarray3.size() == jsonarray4.size(), "cube order length");
        HashSet<String> hashset = new HashSet<>();

        for (JsonElement jsonelement : jsonarray4) {
            require(hashset.add(jsonelement.getAsString()), "unique cube names");
        }

        float[] afloat = null;
        HashMap<String, float[]> hashmap = new HashMap<>();

        for (String s1 : new String[]{"iaf_metal_64x64", "minecraft_64x32"}) {
            JsonObject jsonobject1 = profiles.getAsJsonObject("profiles").getAsJsonObject(s1);
            int i = jsonobject1.get("texture_width").getAsInt();
            int j = jsonobject1.get("texture_height").getAsInt();
            require(i == 64 && j == (s1.equals("iaf_metal_64x64") ? 64 : 32), "profile dimensions");
            // GeckoLib 4.9 no longer tolerates missing bone pivot/rotation
            JsonObject jsonobject2 = GeoModelJson.normalize(geo);
            JsonObject jsonobject3 = jsonobject2.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            jsonobject3.getAsJsonObject("description").addProperty("texture_width", i);
            jsonobject3.getAsJsonObject("description").addProperty("texture_height", j);
            JsonArray jsonarray2 = jsonobject3.getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("cubes");
            JsonObject jsonobject4 = jsonobject1.getAsJsonObject("cube_faces");
            require(jsonobject4.size() == jsonarray4.size(), "profile cube count");

            for (int k = 0; k < jsonarray4.size(); k++) {
                JsonObject jsonobject5 = jsonobject4.getAsJsonObject(jsonarray4.get(k).getAsString());
                require(jsonobject5 != null && jsonobject5.size() == 6, "six cube faces");

                for (String s2 : new String[]{"north", "south", "east", "west", "up", "down"}) {
                    JsonObject jsonobject6 = jsonobject5.getAsJsonObject(s2);
                    require(jsonobject6 != null, "face " + s2);
                    validateUV(jsonobject6, i, j);
                }

                if (s1.equals("iaf_metal_64x64")) {
                    require(jsonarray3.get(k).getAsJsonObject().get("uv").equals(jsonobject5), "geo/default UV order " + k);
                }

                jsonarray2.get(k).getAsJsonObject().add("uv", jsonobject5.deepCopy());
            }

            BakedGeoModel bakedgeomodel = BakedModelFactory.getForNamespace("dragonmaid")
                // GeckoLib 4.9: Model has its own deserializer (format_version / minecraft:geometry keys)
                .constructGeoModel(GeometryTree.fromModel((Model)GeoModelJson.GSON.fromJson(jsonobject2, Model.class)));
            List<GeoCube> list = ((GeoBone)bakedgeomodel.topLevelBones().get(0)).getCubes();
            float[] afloat1 = new float[jsonarray3.size() * 6 * 4 * 6];
            float[] afloat2 = new float[jsonarray3.size() * 6 * 4 * 2];
            int i1 = 0;

            for (GeoCube geocube : list) {
                PoseStack posestack = new PoseStack();
                RenderUtil.translateToPivotPoint(posestack, geocube);
                RenderUtil.rotateMatrixAroundCube(posestack, geocube);
                RenderUtil.translateAwayFromPivotPoint(posestack, geocube);

                for (GeoQuad geoquad : geocube.quads()) {
                    require(geoquad != null && geoquad.vertices().length == 4, "complete quad");
                    Vector3f vector3f = posestack.last().normal().transform(new Vector3f(geoquad.normal()));
                    RenderUtil.fixInvertedFlatCube(geocube, vector3f);

                    for (GeoVertex geovertex : geoquad.vertices()) {
                        Vector3f vector3f1 = posestack.last().pose().transformPosition(new Vector3f(geovertex.position()));
                        int l = i1 * 6;
                        afloat1[l] = vector3f1.x;
                        afloat1[l + 1] = vector3f1.y;
                        afloat1[l + 2] = vector3f1.z;
                        afloat1[l + 3] = vector3f.x;
                        afloat1[l + 4] = vector3f.y;
                        afloat1[l + 5] = vector3f.z;
                        afloat2[i1 * 2] = geovertex.texU();
                        afloat2[i1 * 2 + 1] = geovertex.texV();
                        i1++;
                    }
                }
            }

            require(i1 * 6 == afloat1.length, "complete mesh");
            if (afloat == null) {
                afloat = afloat1;
            } else {
                require(Arrays.equals(afloat, afloat1), "UV profiles share identical geometry");
            }

            hashmap.put(s1, afloat2);
        }

        return new FemaleChestplateMesh(afloat, hashmap, jsonarray3.size());
    }

    private static void validateUV(JsonObject data, int width, int height) {
        JsonArray jsonarray = data.getAsJsonArray("uv");
        JsonArray jsonarray1 = data.getAsJsonArray("uv_size");
        require(jsonarray.size() == 2 && jsonarray1.size() == 2, "UV vector dimensions");
        require(!data.has("uv_rotation") || data.get("uv_rotation").getAsDouble() == 0.0, "v1 unrotated UV");

        for (int i = 0; i < 2; i++) {
            double d0 = jsonarray.get(i).getAsDouble();
            double d1 = jsonarray1.get(i).getAsDouble();
            double d2 = d0 + d1;
            require(
                Double.isFinite(d0) && Double.isFinite(d1) && Math.min(d0, d2) >= 0.0 && Math.max(d0, d2) <= (double)(i == 0 ? width : height),
                "UV endpoint bounds"
            );
        }
    }

    private static void requireVector(JsonArray actual, double[] expected, String label) {
        require(actual != null && actual.size() == expected.length, label);

        for (int i = 0; i < expected.length; i++) {
            require(actual.get(i).getAsDouble() == expected[i], label);
        }
    }

    private static void require(boolean valid, String detail) {
        if (!valid) {
            throw new IllegalArgumentException("Invalid female chestplate interface: " + detail);
        }
    }

    void render(PoseStack pose, VertexConsumer output, int light, FemaleChestplateMaterial material) {
        float[] afloat = this.uvProfiles.get(material.uvProfile());
        Vector3f vector3f = new Vector3f();
        Vector3f vector3f1 = new Vector3f();

        for (int i = 0; i < this.vertexCount(); i++) {
            int j = i * 6;
            pose.last().pose().transformPosition(vector3f.set(this.geometry[j], this.geometry[j + 1], this.geometry[j + 2]));
            pose.last().normal().transform(vector3f1.set(this.geometry[j + 3], this.geometry[j + 4], this.geometry[j + 5])).normalize();
            // 1.21.1: one call per vertex, colour is a packed ARGB int (was r,g,b,a floats)
            output.addVertex(
                vector3f.x,
                vector3f.y,
                vector3f.z,
                packColor(material.red, material.green, material.blue),
                afloat[i * 2],
                afloat[i * 2 + 1],
                OverlayTexture.NO_OVERLAY,
                light,
                vector3f1.x,
                vector3f1.y,
                vector3f1.z
            );
        }
    }

    private static int packColor(float red, float green, float blue) {
        return 0xFF000000 | (toByte(red) << 16) | (toByte(green) << 8) | toByte(blue);
    }

    private static int toByte(float value) {
        int i = (int) (value * 255.0F + 0.5F);
        return i < 0 ? 0 : Math.min(i, 255);
    }
}
