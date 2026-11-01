package com.example.dragonmaid.client;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;


public final class ServiceArmorResources extends SimplePreparableReloadListener<ServiceArmorResources.Snapshot> {
    public static final ServiceArmorResources INSTANCE = new ServiceArmorResources();
    public static final ResourceLocation CONTRACT = ResourceLocation.fromNamespaceAndPath("dragonmaid", "armor/service/service_armor_contract.json");
    private static final Map<Integer, List<String>> EXPECTED_SLOTS = Map.of(
        1,
        List.of("Head"),
        2,
        List.of("Right Arm", "Left Arm", "arm_lower_right", "arm_lower_left", "hand_right", "hand_left"),
        3,
        List.of("Body"),
        4,
        List.of("Right Leg", "Left Leg", "leg_lower_right", "leg_lower_left", "foot_right", "foot_left")
    );
    private static final Map<String, ServiceArmorResources.Binding> BINDINGS = Map.ofEntries(
        Map.entry("Root", new ServiceArmorResources.Binding(null, 0.0, 0.0, 0.0)),
        Map.entry("Waist", new ServiceArmorResources.Binding("Root", 0.0, 12.0, 0.0)),
        Map.entry("Head", new ServiceArmorResources.Binding("Waist", 0.0, 24.0, 0.0)),
        Map.entry("Body", new ServiceArmorResources.Binding("Waist", 0.0, 24.0, 0.0)),
        Map.entry("Right Arm", new ServiceArmorResources.Binding("Waist", -5.0, 22.0, 0.0)),
        Map.entry("Left Arm", new ServiceArmorResources.Binding("Waist", 5.0, 22.0, 0.0)),
        Map.entry("arm_lower_right", new ServiceArmorResources.Binding("Right Arm", -5.5, 18.0, 0.0)),
        Map.entry("arm_lower_left", new ServiceArmorResources.Binding("Left Arm", 5.5, 18.0, 0.0)),
        Map.entry("hand_right", new ServiceArmorResources.Binding("arm_lower_right", -5.5, 14.0, 0.0)),
        Map.entry("hand_left", new ServiceArmorResources.Binding("arm_lower_left", 5.5, 14.0, 0.0)),
        Map.entry("Right Leg", new ServiceArmorResources.Binding("Root", -1.9, 12.0, 0.0)),
        Map.entry("Left Leg", new ServiceArmorResources.Binding("Root", 1.9, 12.0, 0.0)),
        Map.entry("leg_lower_right", new ServiceArmorResources.Binding("Right Leg", -1.9, 6.0, 0.0)),
        Map.entry("leg_lower_left", new ServiceArmorResources.Binding("Left Leg", 1.9, 6.0, 0.0)),
        Map.entry("foot_right", new ServiceArmorResources.Binding("leg_lower_right", -1.9, 2.0, 0.0)),
        Map.entry("foot_left", new ServiceArmorResources.Binding("leg_lower_left", 1.9, 2.0, 0.0))
    );
    private static final List<String> DRAGON_PARTS = List.of("head", "neck", "body", "tail");
    private volatile ServiceArmorResources.Snapshot current = ServiceArmorResources.Snapshot.EMPTY;

    private ServiceArmorResources() {
    }

    protected ServiceArmorResources.Snapshot prepare(ResourceManager resources, ProfilerFiller profiler) {
        if (resources.getResource(CONTRACT).isEmpty()) {
            require(this.current == ServiceArmorResources.Snapshot.EMPTY, "previously loaded contract was removed: " + CONTRACT);
            return ServiceArmorResources.Snapshot.EMPTY;
        } else {
            try {
                JsonObject jsonobject = read(resources, CONTRACT);
                requireString(jsonobject, "schema", "dragonmaid.service-armor.v1");
                requireString(jsonobject, "binding", "existing_bone_final_space");
                requireString(jsonobject, "tail_mode", "existing_dynamic_overlay");
                Map<String, Integer> map = loadSlots(object(jsonobject, "slots"));
                HashMap<Boolean, ServiceArmorResources.HumanModel> hashmap = new HashMap<>();
                JsonObject jsonobject1 = object(jsonobject, "human");

                for (boolean flag : new boolean[]{false, true}) {
                    JsonObject jsonobject2 = object(jsonobject1, flag ? "male" : "female");
                    ResourceLocation resourcelocation = location(jsonobject2, "model");
                    ResourceLocation resourcelocation1 = location(jsonobject2, "texture");
                    validatePng(resources, resourcelocation1, 128, 128);
                    hashmap.put(
                        flag, new ServiceArmorResources.HumanModel(resourcelocation1, loadModel(read(resources, resourcelocation), resourcelocation, map))
                    );
                }

                HashMap<String, List<ResourceLocation>> hashmap1 = new HashMap<>();
                JsonObject jsonobject3 = object(jsonobject, "dragon");

                for (String s1 : List.of("fire", "ice", "lightning")) {
                    JsonObject jsonobject4 = object(jsonobject3, s1);
                    ArrayList<ResourceLocation> arraylist = new ArrayList<>();

                    for (String s : DRAGON_PARTS) {
                        ResourceLocation resourcelocation2 = location(jsonobject4, s);
                        validatePng(resources, resourcelocation2, 512, 256);
                        arraylist.add(resourcelocation2);
                    }

                    hashmap1.put(s1, List.copyOf(arraylist));
                }

                return new ServiceArmorResources.Snapshot(Map.copyOf(hashmap), Map.copyOf(map), Map.copyOf(hashmap1));
            } catch (RuntimeException | IOException ioexception) {
                throw new IllegalStateException("Cannot load service armor contract " + CONTRACT + ": " + ioexception.getMessage(), ioexception);
            }
        }
    }

    private static Map<String, Integer> loadSlots(JsonObject source) {
        require(source.keySet().equals(Set.of("1", "2", "3", "4")), "exact inventory slots 1..4");
        HashMap<String, Integer> hashmap = new HashMap<>();

        for (Entry<Integer, List<String>> entry : EXPECTED_SLOTS.entrySet()) {
            String s = Integer.toString(entry.getKey());
            JsonArray jsonarray = source.getAsJsonArray(s);
            require(jsonarray != null && jsonarray.size() == entry.getValue().size(), "slot " + s + " bone count");
            HashSet<String> hashset = new HashSet<>();

            for (JsonElement jsonelement : jsonarray) {
                String s1 = jsonelement.getAsString();
                require(hashset.add(s1), "duplicate slot bone " + s1);
                require(hashmap.put(s1, entry.getKey()) == null, "bone occurs in multiple slots: " + s1);
            }

            require(hashset.equals(Set.copyOf(entry.getValue())), "slot " + s + " existing bone names");
        }

        return hashmap;
    }

    private static Map<String, List<GeoCube>> loadModel(JsonObject geo, ResourceLocation source, Map<String, Integer> slots) {
        requireString(geo, "format_version", "1.12.0");
        JsonArray jsonarray = geo.getAsJsonArray("minecraft:geometry");
        require(jsonarray != null && jsonarray.size() == 1, source + " single geometry");
        JsonObject jsonobject = jsonarray.get(0).getAsJsonObject();
        JsonObject jsonobject1 = object(jsonobject, "description");
        require(
            jsonobject1.get("texture_width").getAsDouble() == 128.0 && jsonobject1.get("texture_height").getAsDouble() == 128.0,
            source + " logical texture must be 128x128"
        );
        JsonArray jsonarray1 = jsonobject.getAsJsonArray("bones");
        require(jsonarray1 != null, source + " bones");
        HashSet<String> hashset = new HashSet<>();

        for (JsonElement jsonelement : jsonarray1) {
            JsonObject jsonobject2 = jsonelement.getAsJsonObject();
            String s = jsonobject2.get("name").getAsString();
            require(hashset.add(s), source + " duplicate bone " + s);
            if (BINDINGS.containsKey(s)) {
                ServiceArmorResources.Binding servicearmorresources$binding = BINDINGS.get(s);
                if (servicearmorresources$binding.parent() == null) {
                    require(!jsonobject2.has("parent"), source + " Root has no parent");
                } else {
                    requireString(jsonobject2, "parent", servicearmorresources$binding.parent());
                }

                requireVector(
                    jsonobject2.getAsJsonArray("pivot"),
                    new double[]{servicearmorresources$binding.x(), servicearmorresources$binding.y(), servicearmorresources$binding.z()},
                    source + " " + s + " pivot"
                );

                for (String s1 : List.of("rotation", "position")) {
                    if (jsonobject2.has(s1)) {
                        requireVector(jsonobject2.getAsJsonArray(s1), new double[]{0.0, 0.0, 0.0}, source + " " + s + " " + s1);
                    }
                }

                if (jsonobject2.has("scale")) {
                    requireVector(jsonobject2.getAsJsonArray("scale"), new double[]{1.0, 1.0, 1.0}, source + " " + s + " scale");
                }

                require(!jsonobject2.has("neverRender") || !jsonobject2.get("neverRender").getAsBoolean(), source + " hidden equipment bone " + s);
            }

            JsonArray jsonarray2 = jsonobject2.getAsJsonArray("cubes");
            if (jsonarray2 != null && !jsonarray2.isEmpty()) {
                require(slots.containsKey(s), source + " clothing cubes on unsupported bone " + s);

                for (JsonElement jsonelement1 : jsonarray2) {
                    validateCube(jsonelement1.getAsJsonObject(), source + " " + s);
                }
            }
        }

        require(hashset.containsAll(BINDINGS.keySet()), source + " missing existing equipment bones or ancestors");
        BakedGeoModel bakedgeomodel = BakedModelFactory.getForNamespace("dragonmaid")
            // GeckoLib 4.9: Model has its own deserializer (format_version / minecraft:geometry keys)
            .constructGeoModel(GeometryTree.fromModel((Model)GeoModelJson.GSON.fromJson(GeoModelJson.normalize(geo), Model.class)));
        HashMap<String, List<GeoCube>> hashmap = new HashMap<>();

        for (GeoBone geobone : bakedgeomodel.topLevelBones()) {
            collectCubes(geobone, slots, hashmap);
        }

        return Map.copyOf(hashmap);
    }

    private static void collectCubes(GeoBone bone, Map<String, Integer> slots, Map<String, List<GeoCube>> result) {
        if (slots.containsKey(bone.getName())) {
            result.put(bone.getName(), List.copyOf(bone.getCubes()));
        }

        for (GeoBone geobone : bone.getChildBones()) {
            collectCubes(geobone, slots, result);
        }
    }

    private static void validateCube(JsonObject cube, String label) {
        finiteVector(cube.getAsJsonArray("origin"), 3, label + " cube origin");
        JsonArray jsonarray = cube.getAsJsonArray("size");
        finiteVector(jsonarray, 3, label + " cube size");

        for (JsonElement jsonelement : jsonarray) {
            require(jsonelement.getAsDouble() >= 0.0, label + " non-negative cube size");
        }

        for (String s : List.of("pivot", "rotation")) {
            if (cube.has(s)) {
                finiteVector(cube.getAsJsonArray(s), 3, label + " cube " + s);
            }
        }

        if (cube.has("inflate")) {
            require(Double.isFinite(cube.get("inflate").getAsDouble()), label + " cube inflate");
        }

        JsonElement jsonelement1 = cube.get("uv");
        require(jsonelement1 != null, label + " cube UV");
        if (jsonelement1.isJsonArray()) {
            JsonArray jsonarray2 = jsonelement1.getAsJsonArray();
            finiteVector(jsonarray2, 2, label + " box UV");
            double d0 = jsonarray2.get(0).getAsDouble();
            double d1 = jsonarray2.get(1).getAsDouble();
            double d2 = Math.floor(jsonarray.get(0).getAsDouble());
            double d3 = Math.floor(jsonarray.get(1).getAsDouble());
            double d4 = Math.floor(jsonarray.get(2).getAsDouble());
            require(d0 >= 0.0 && d1 >= 0.0 && d0 + 2.0 * (d2 + d4) <= 128.0 && d1 + d4 + d3 <= 128.0, label + " box UV bounds");
        } else {
            JsonObject jsonobject = jsonelement1.getAsJsonObject();
            require(
                !jsonobject.keySet().isEmpty() && Set.of("north", "south", "east", "west", "up", "down").containsAll(jsonobject.keySet()),
                label + " UV face names"
            );

            for (Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                JsonObject jsonobject1 = entry.getValue().getAsJsonObject();
                JsonArray jsonarray1 = jsonobject1.getAsJsonArray("uv");
                JsonArray jsonarray3 = jsonobject1.getAsJsonArray("uv_size");
                finiteVector(jsonarray1, 2, label + " " + entry.getKey() + " UV");
                finiteVector(jsonarray3, 2, label + " " + entry.getKey() + " UV size");

                for (int i = 0; i < 2; i++) {
                    double d6 = jsonarray1.get(i).getAsDouble();
                    double d7 = d6 + jsonarray3.get(i).getAsDouble();
                    require(Math.min(d6, d7) >= 0.0 && Math.max(d6, d7) <= 128.0, label + " " + entry.getKey() + " signed UV bounds");
                }

                if (jsonobject1.has("uv_rotation")) {
                    double d5 = jsonobject1.get("uv_rotation").getAsDouble();
                    require(d5 == 0.0 || d5 == 90.0 || d5 == 180.0 || d5 == 270.0, label + " UV rotation");
                }
            }
        }
    }

    private static JsonObject read(ResourceManager resources, ResourceLocation location) throws IOException {
        JsonObject jsonobject;
        try (InputStreamReader inputstreamreader = new InputStreamReader(resources.getResourceOrThrow(location).open(), StandardCharsets.UTF_8)) {
            jsonobject = JsonParser.parseReader(inputstreamreader).getAsJsonObject();
        }

        return jsonobject;
    }

    private static void validatePng(ResourceManager resources, ResourceLocation texture, int width, int height) throws IOException {
        try (InputStream inputstream = resources.getResourceOrThrow(texture).open()) {
            NativeImage nativeimage = NativeImage.read(inputstream);

            try {
                require(nativeimage.getWidth() == width && nativeimage.getHeight() == height, texture + " PNG must be " + width + "x" + height);
            } catch (Throwable throwable1) {
                if (nativeimage != null) {
                    try {
                        nativeimage.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }
                }

                throw throwable1;
            }

            if (nativeimage != null) {
                nativeimage.close();
            }
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        require(parent.has(key) && parent.get(key).isJsonObject(), "object " + key);
        return parent.getAsJsonObject(key);
    }

    private static ResourceLocation location(JsonObject parent, String key) {
        require(parent.has(key) && parent.get(key).isJsonPrimitive(), "resource " + key);
        // 1.21.1: the ResourceLocation constructor is private now
        ResourceLocation resourcelocation = ResourceLocation.parse(parent.get(key).getAsString());
        require("dragonmaid".equals(resourcelocation.getNamespace()), "service resource namespace " + resourcelocation);
        return resourcelocation;
    }

    private static void finiteVector(JsonArray actual, int count, String label) {
        require(actual != null && actual.size() == count, label);

        for (JsonElement jsonelement : actual) {
            require(Double.isFinite(jsonelement.getAsDouble()), label + " finite components");
        }
    }

    private static void requireVector(JsonArray actual, double[] expected, String label) {
        finiteVector(actual, expected.length, label);

        for (int i = 0; i < expected.length; i++) {
            require(actual.get(i).getAsDouble() == expected[i], label + " canonical existing rig");
        }
    }

    private static void requireString(JsonObject object, String key, String expected) {
        require(object.has(key) && expected.equals(object.get(key).getAsString()), key + " must be " + expected);
    }

    private static void require(boolean condition, String detail) {
        if (!condition) {
            throw new IllegalArgumentException("Invalid service armor: " + detail);
        }
    }

    protected void apply(ServiceArmorResources.Snapshot snapshot, ResourceManager resources, ProfilerFiller profiler) {
        this.current = snapshot;
        ServiceDragonArmorTextures.clearCache();
    }

    public List<GeoCube> forBone(boolean male, String bone) {
        ServiceArmorResources.HumanModel servicearmorresources$humanmodel = this.current.human().get(male);
        return servicearmorresources$humanmodel == null ? List.of() : servicearmorresources$humanmodel.cubes().getOrDefault(bone, List.of());
    }

    public int slotForBone(String bone) {
        return this.current.slots().getOrDefault(bone, 0);
    }

    public ResourceLocation humanTexture(boolean male) {
        ServiceArmorResources.HumanModel servicearmorresources$humanmodel = this.current.human().get(male);
        if (servicearmorresources$humanmodel == null) {
            throw new IllegalStateException("Missing service armor resources: " + CONTRACT);
        } else {
            return servicearmorresources$humanmodel.texture();
        }
    }

    public ResourceLocation dragonTexture(DragonBaseEntity dragon, int inventorySlot) {
        if (inventorySlot >= 1 && inventorySlot <= 4) {
            String s = dragon instanceof IceDragonEntity ? "ice" : (dragon instanceof LightningDragonEntity ? "lightning" : "fire");
            List<ResourceLocation> list = this.current.dragon().get(s);
            if (list == null) {
                throw new IllegalStateException("Missing service armor resources for " + s + ": " + CONTRACT);
            } else {
                return list.get(inventorySlot - 1);
            }
        } else {
            throw new IllegalArgumentException("Service armor inventory slot must be 1..4: " + inventorySlot);
        }
    }

    private static record Binding(String parent, double x, double y, double z) {
    }

    private static record HumanModel(ResourceLocation texture, Map<String, List<GeoCube>> cubes) {
    }

    static record Snapshot(Map<Boolean, ServiceArmorResources.HumanModel> human, Map<String, Integer> slots, Map<String, List<ResourceLocation>> dragon) {
        static final ServiceArmorResources.Snapshot EMPTY = new ServiceArmorResources.Snapshot(Map.of(), Map.of(), Map.of());
    }
}
