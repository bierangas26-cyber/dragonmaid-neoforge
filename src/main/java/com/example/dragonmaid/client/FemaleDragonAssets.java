package com.example.dragonmaid.client;

import com.iafenvoy.iceandfire.data.DragonColor;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

enum FemaleDragonAssets {
    ICE(
        "geo/dragon_female.sleep.geo.json",
        "ice",
        new String[]{
            "ice_dragon_female_blue_3_128.png",
            "ice_dragon_female_white_3_128.png",
            "ice_dragon_female_sapphire_3_128.png",
            "ice_dragon_female_siliver_3_128.png"
        }
    ),
    FIRE(
        "geo/dragon_fire_female.geo.json",
        "fire",
        new String[]{
            "fire_dragon_female_red_3_128.png",
            "fire_dragon_female_green_3_128.layers.png",
            "fire_dragon_female_bronze_3_128.layers.png",
            "fire_dragon_female_gray_3_128.layers.png"
        }
    ),
    LIGHTNING(
        "geo/dragon_lightning_female.geo.json",
        "lightning",
        new String[]{
            "lightning_dragon_female_electric_3_128.layers.png",
            "lightning_dragon_female_amethyst_3_128.png",
            "lightning_dragon_female_copper_3_128.layers.png",
            "lightning_dragon_female_black_3_128.layers.png"
        }
    ),
    ICE_MALE("ice", new String[]{"blue", "white", "sapphire", "siliver"}),
    FIRE_MALE("fire", new String[]{"red", "green", "bronze", "gray"}),
    LIGHTNING_MALE("lightning", new String[]{"electric", "amethyst", "copper", "black"});

    final ResourceLocation model;
    final ResourceLocation sitResource;
    final ResourceLocation sleepResource;
    final ResourceLocation lifeResource;
    final ResourceLocation breathResource;
    final String sitPrefix;
    final String sleepPrefix;
    final String breathPrefix;
    final String species;
    final boolean male;
    private final ResourceLocation[] textures;
    private final ResourceLocation[] highTextures;
    private final ResourceLocation[] models;

    private FemaleDragonAssets(String model, String species, String[] textures) {
        this(model, species, textures, false);
    }

    private FemaleDragonAssets(String species, String[] colors) {
        this(
            "geo/dragon_" + species + "_male.geo.json",
            species,
            Arrays.stream(colors).map(color -> species + "_dragon_male_" + color + "_3_128.png").toArray(String[]::new),
            true
        );
    }

    private FemaleDragonAssets(String model, String species, String[] textures, boolean male) {
        this.male = male;
        this.species = species;
        String s = male ? "male" : "female";
        this.model = asset(model);
        this.models = new ResourceLocation[]{
            this.model, asset("geo/dragon_" + species + "_" + s + "_stage4.geo.json"), asset("geo/dragon_" + species + "_" + s + "_stage5.geo.json")
        };
        this.sitPrefix = "animation.dragon_" + species + "_" + s;
        this.sitResource = asset("animations/sit/" + species + "/" + s + "/dragon_" + species + "_" + s + ".sit.animation.json");
        this.lifeResource = asset("animations/life/" + species + "/" + s + "/dragon_" + species + "_" + s + ".life.animation.json");
        this.breathPrefix = "animation.dragon_lightning_female";
        this.breathResource = species.equals("lightning") ? asset("animations/breath/lightning/female/dragon_lightning_female.breath.animation.json") : null;
        String s1 = !male && !species.equals("lightning") ? "fire_ice" : species;
        this.sleepPrefix = "animation.dragon_" + s1 + "_" + s;
        this.sleepResource = asset("animations/sleep/" + s1 + "/" + s + "/dragon_" + s1 + "_" + s + ".sleep.animation.json");
        this.textures = Arrays.stream(textures).map(name -> asset("textures/entity/" + name)).toArray(ResourceLocation[]::new);
        this.highTextures = Arrays.stream(textures)
            .map(name -> asset("textures/entity/" + name.replace("_3_128", "_4_5_128")))
            .toArray(ResourceLocation[]::new);
    }

    static int stageIndex(int stage) {
        return Math.max(0, Math.min(2, stage - 3));
    }

    ResourceLocation model(int stage) {
        return this.models[stageIndex(stage)];
    }

    ResourceLocation texture(int stage, int variant) {
        ResourceLocation[] aresourcelocation = stageIndex(stage) == 0 ? this.textures : this.highTextures;
        return aresourcelocation[variant >= 0 && variant < aresourcelocation.length ? variant : 0];
    }

    ResourceLocation texture(int variant) {
        return this.textures[variant >= 0 && variant < this.textures.length ? variant : 0];
    }

    /**
     * IAF CE 2.0 port: {@code DragonBaseEntity#getVariant()} now returns the colour *name*
     * ("blue", "white", ...) instead of the old IAF variant index. Resolve it against this
     * enum constant's texture names (the file names carry the colour token).
     */
    ResourceLocation texture(int stage, DragonBaseEntity dragon) {
        String variant = dragon.getVariant();
        DragonColor color = variant == null ? null : DragonColor.getById(variant);
        String name = (color != null ? color.getName() : variant);
        if (name == null) {
            return this.texture(stage, 0);
        }

        String needle = "_" + name.toLowerCase(Locale.ROOT) + "_";
        ResourceLocation[] candidates = stageIndex(stage) == 0 ? this.textures : this.highTextures;

        for (int i = 0; i < candidates.length; i++) {
            String path = candidates[i].getPath();
            if (path.contains(needle) || (name.equalsIgnoreCase("silver") && path.contains("_siliver_"))) {
                return candidates[i];
            }
        }

        return this.texture(stage, 0);
    }

    boolean kneelingSleep() {
        return this == LIGHTNING;
    }

    String seatedSleepEnter() {
        return this.kneelingSleep() ? this.sleepPrefix + ".sleep_enter" : this.sitPrefix + ".sit_sleep_enter";
    }

    String seatedSleepExit() {
        return this.kneelingSleep() ? this.sleepPrefix + ".sleep_exit" : this.sitPrefix + ".sleep_sit_exit";
    }

    EntityType<?> entityType() {
        String s = this.species;

        return switch (s) {
            case "ice" -> (EntityType)IafEntities.ICE_DRAGON.get();
            case "fire" -> (EntityType)IafEntities.FIRE_DRAGON.get();
            default -> (EntityType)IafEntities.LIGHTNING_DRAGON.get();
        };
    }

    static FemaleDragonAssets of(DragonBaseEntity dragon) {
        if (dragon instanceof IceDragonEntity) {
            return dragon.isMale() ? ICE_MALE : ICE;
        } else if (dragon instanceof FireDragonEntity) {
            return dragon.isMale() ? FIRE_MALE : FIRE;
        } else {
            return dragon.isMale() ? LIGHTNING_MALE : LIGHTNING;
        }
    }

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath("dragonmaid", path);
    }
}
