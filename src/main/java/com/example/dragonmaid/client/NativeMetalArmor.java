package com.example.dragonmaid.client;

import com.iafenvoy.iceandfire.data.DragonArmorMaterial;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import com.iafenvoy.iceandfire.render.model.armor.CopperArmorModel;
import com.iafenvoy.iceandfire.render.model.armor.SilverArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * IAF CE 2.0 port.
 *
 * <p>The old armor models were NOT deleted - they moved packages and lost the
 * "Model" prefix:</p>
 * <pre>
 *   com.github.alexthe666.iceandfire.client.model.armor.ModelCopperArmor
 *     -> com.iafenvoy.iceandfire.render.model.armor.CopperArmorModel
 *   ...ModelSilverArmor -> ...SilverArmorModel
 * </pre>
 * <p>{@code createMesh(CubeDeformation, float)} keeps the exact same signature, so this is
 * an import/class-name swap only. (Verified with javap against iceandfire-2.0.jar.)</p>
 *
 * <p>Second change: {@code DragonArmorItem#type} is now a
 * {@link DragonArmorMaterial} record instead of an enum, so the old
 * {@code switch} became identity comparisons.</p>
 */
enum NativeMetalArmor {
    SILVER("silver"),
    COPPER("copper");

    private final ResourceLocation outerTexture;
    private final ResourceLocation innerTexture;

    private NativeMetalArmor(String material) {
        // IAF CE 2.0 renamed the player-style metal armor textures:
        //   iae 1.20.1: textures/models/armor/armor_<material>_metal_layer_N.png
        //   CE 2.0   : textures/models/armor/<material>_layer_N.png
        // (verified against iceandfire-2.0.jar; the legacy names no longer exist -> missing texture)
        this.outerTexture = ResourceLocation.fromNamespaceAndPath("iceandfire", "textures/models/armor/" + material + "_layer_1.png");
        this.innerTexture = ResourceLocation.fromNamespaceAndPath("iceandfire", "textures/models/armor/" + material + "_layer_2.png");
    }

    static NativeMetalArmor of(ItemStack stack) {
        if (stack.getItem() instanceof DragonArmorItem item) {
            if (item.type == DragonArmorMaterial.SILVER) {
                return SILVER;
            }
            if (item.type == DragonArmorMaterial.COPPER) {
                return COPPER;
            }
        }
        return null;
    }

    ResourceLocation texture(int slot) {
        return slot == 4 ? this.innerTexture : this.outerTexture;
    }

    ResourceLocation texture(int slot, String bone) {
        return slot == 4 && LegArmorMesh.textureLayer(bone) == 1 ? this.outerTexture : this.texture(slot);
    }

    HumanoidModel<DragonBaseEntity> model(boolean inner) {
        CubeDeformation cubedeformation = new CubeDeformation(inner ? 0.38F : 0.45F);
        MeshDefinition meshdefinition = this == SILVER
            ? SilverArmorModel.createMesh(cubedeformation, 0.0F)
            : CopperArmorModel.createMesh(cubedeformation, 0.0F);
        return new HumanoidModel(LayerDefinition.create(meshdefinition, 64, 64).bakeRoot());
    }
}
