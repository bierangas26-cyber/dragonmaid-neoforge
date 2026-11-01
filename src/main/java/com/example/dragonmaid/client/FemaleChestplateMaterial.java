package com.example.dragonmaid.client;

import com.iafenvoy.iceandfire.data.DragonArmorMaterial;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * IAF CE 2.0 port: the old nested enum {@code DragonArmorItem.DragonArmorType}
 * no longer exists; CE stores {@code com.iafenvoy.iceandfire.data.DragonArmorMaterial}
 * (a record with static singletons) on {@code DragonArmorItem#type}.
 *
 * <p>Old enum -> CE material mapping:</p>
 * <pre>
 *   IRON/GOLD/DIAMOND/SILVER/COPPER -> same name
 *   FIRE      -> DRAGON_STEEL_FIRE
 *   ICE       -> DRAGON_STEEL_ICE
 *   LIGHTNING -> DRAGON_STEEL_LIGHTNING
 *   (CE NETHERITE has no old counterpart -> null, no female chestplate variant)
 * </pre>
 */
enum FemaleChestplateMaterial {
    IRON("minecraft", "textures/models/armor/iron_layer_1.png", 32, 1.0F, 1.0F, 1.0F),
    GOLD("minecraft", "textures/models/armor/gold_layer_1.png", 32, 1.0F, 1.0F, 1.0F),
    DIAMOND("minecraft", "textures/models/armor/diamond_layer_1.png", 32, 1.0F, 1.0F, 1.0F),
    // IAF CE 2.0 renamed these textures: armor_<material>_metal_layer_1 -> <material>_layer_1
    SILVER("iceandfire", "textures/models/armor/silver_layer_1.png", 64, 1.0F, 1.0F, 1.0F),
    COPPER("iceandfire", "textures/models/armor/copper_layer_1.png", 64, 1.0F, 1.0F, 1.0F),
    FIRE("minecraft", "textures/models/armor/netherite_layer_1.png", 32, 1.0F, 0.55F, 0.4F),
    ICE("minecraft", "textures/models/armor/netherite_layer_1.png", 32, 0.55F, 0.8F, 1.0F),
    LIGHTNING("minecraft", "textures/models/armor/netherite_layer_1.png", 32, 1.0F, 0.6F, 1.0F);

    final ResourceLocation texture;
    final int textureWidth = 64;
    final int textureHeight;
    final float red;
    final float green;
    final float blue;

    private FemaleChestplateMaterial(String namespace, String path, int height, float red, float green, float blue) {
        this.texture = ResourceLocation.fromNamespaceAndPath(namespace, path);
        this.textureHeight = height;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    static FemaleChestplateMaterial of(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof DragonArmorItem item ? ofType(item.type) : null;
    }

    static FemaleChestplateMaterial ofType(DragonArmorMaterial type) {
        if (type == DragonArmorMaterial.IRON) {
            return IRON;
        } else if (type == DragonArmorMaterial.GOLD) {
            return GOLD;
        } else if (type == DragonArmorMaterial.DIAMOND) {
            return DIAMOND;
        } else if (type == DragonArmorMaterial.SILVER) {
            return SILVER;
        } else if (type == DragonArmorMaterial.COPPER) {
            return COPPER;
        } else if (type == DragonArmorMaterial.DRAGON_STEEL_FIRE) {
            return FIRE;
        } else if (type == DragonArmorMaterial.DRAGON_STEEL_ICE) {
            return ICE;
        } else if (type == DragonArmorMaterial.DRAGON_STEEL_LIGHTNING) {
            return LIGHTNING;
        }
        return null;
    }

    float normalizeU(float pixels) {
        return pixels / 64.0F;
    }

    float normalizeV(float pixels) {
        return pixels / (float)this.textureHeight;
    }

    String uvProfile() {
        return this.textureHeight == 64 ? "iaf_metal_64x64" : "minecraft_64x32";
    }
}
