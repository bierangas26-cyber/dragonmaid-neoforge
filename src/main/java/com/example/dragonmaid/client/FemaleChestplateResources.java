package com.example.dragonmaid.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

final class FemaleChestplateResources extends SimplePreparableReloadListener<FemaleChestplateMesh> {
    static final FemaleChestplateResources INSTANCE = new FemaleChestplateResources();
    static final ResourceLocation CONTRACT = ResourceLocation.fromNamespaceAndPath("dragonmaid", "armor/female_chestplate_male_base_front_contract.json");
    static final ResourceLocation GEOMETRY = ResourceLocation.fromNamespaceAndPath("dragonmaid", "geo/female_chestplate_male_base_front.geo.json");
    static final ResourceLocation UV_PROFILES = ResourceLocation.fromNamespaceAndPath("dragonmaid", "armor/female_chestplate_male_base_front_uv.json");
    private volatile FemaleChestplateMesh current;

    FemaleChestplateResources() {
    }

    protected FemaleChestplateMesh prepare(ResourceManager resources, ProfilerFiller profiler) {
        if (resources.getResource(CONTRACT).isEmpty()) {
            return null;
        } else {
            JsonObject jsonobject = read(resources, CONTRACT, null);
            validateContract(jsonobject);
            return FemaleChestplateMesh.load(
                read(resources, GEOMETRY, jsonobject.get("geometry_sha256").getAsString()),
                read(resources, UV_PROFILES, jsonobject.get("uv_sha256").getAsString())
            );
        }
    }

    static void validateContract(JsonObject contract) {
        requireString(contract, "schema", "dragonmaid.chestplate.male-base-front.v1");
        requireString(contract, "render_mode", "additive_front");
        requireString(contract, "torso_source", "existing_male_body");
        require(contract.has("retain_existing_body") && contract.get("retain_existing_body").getAsBoolean(), "complete existing body retained");
        require(contract.has("body_inventory_slot") && contract.get("body_inventory_slot").getAsInt() == 3, "body slot 3");
        require(contract.has("shoulder_inventory_slot") && contract.get("shoulder_inventory_slot").getAsInt() == 2, "shoulder slot 2");
        require(contract.has("no_shoulders") && contract.get("no_shoulders").getAsBoolean(), "no shoulder addition");
        requireString(contract, "geometry", GEOMETRY.toString());
        requireString(contract, "uv_profiles", UV_PROFILES.toString());

        for (String s : new String[]{"geometry_sha256", "uv_sha256"}) {
            require(contract.has(s) && contract.get(s).getAsString().matches("(?i)[0-9a-f]{64}"), s);
        }
    }

    private static void requireString(JsonObject contract, String field, String expected) {
        require(contract.has(field) && expected.equals(contract.get(field).getAsString()), field);
    }

    private static void require(boolean valid, String field) {
        if (!valid) {
            throw new IllegalArgumentException("Invalid male-base chest-front contract: " + field);
        }
    }

    private static JsonObject read(ResourceManager resources, ResourceLocation location, String expectedHash) {
        try {
            JsonObject jsonobject;
            try (InputStream inputstream = resources.getResourceOrThrow(location).open()) {
                byte[] abyte = inputstream.readAllBytes();
                if (expectedHash != null) {
                    String s = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(abyte));
                    require(s.equalsIgnoreCase(expectedHash), "paired resource hash " + location);
                }

                jsonobject = JsonParser.parseString(new String(abyte, StandardCharsets.UTF_8)).getAsJsonObject();
            }

            return jsonobject;
        } catch (NoSuchAlgorithmException | IOException ioexception) {
            throw new IllegalStateException("Cannot load male-base chest-front " + location, ioexception);
        }
    }

    protected void apply(FemaleChestplateMesh mesh, ResourceManager resources, ProfilerFiller profiler) {
        this.current = mesh;
    }

    FemaleChestplateMesh forEquipment(boolean male, int slot, String bone, FemaleChestplateMaterial material) {
        return !male && slot == 3 && "Body".equals(bone) && material != null ? this.current : null;
    }
}
