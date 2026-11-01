package com.example.dragonmaid.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;

final class LegArmorMesh {
    private LegArmorMesh() {
    }

    static int textureLayer(String bone) {
        return !bone.equals("Right Leg") && !bone.equals("Left Leg") ? 1 : 2;
    }

    static Map<String, ArticulatedArmorMesh> vanilla() {
        ModelPart modelpart = LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.6F), 0.0F), 64, 32).bakeRoot();
        HumanoidModel<LivingEntity> humanoidmodel = new HumanoidModel(modelpart);
        return pieces(humanoidmodel, humanoidmodel);
    }

    static Map<String, ArticulatedArmorMesh> pieces(HumanoidModel<?> leggings, HumanoidModel<?> boots) {
        HashMap<String, ArticulatedArmorMesh> hashmap = new HashMap<>();

        for (String s : new String[]{"right", "left"}) {
            boolean flag = s.equals("right");
            List<ArticulatedArmorMesh.Vertex> list = ArticulatedArmorMesh.capture(flag ? leggings.rightLeg : leggings.leftLeg);
            List<ArticulatedArmorMesh.Vertex> list1 = ArticulatedArmorMesh.capture(flag ? boots.rightLeg : boots.leftLeg);
            hashmap.put(flag ? "Right Leg" : "Left Leg", ArticulatedArmorMesh.slice(list, -100.0F, 6.0F, 0.0F));
            hashmap.put("leg_lower_" + s, ArticulatedArmorMesh.slice(list1, 6.0F, 10.0F, 6.0F));
            hashmap.put("foot_" + s, ArticulatedArmorMesh.slice(list1, 10.0F, 100.0F, 10.0F));
        }

        return hashmap;
    }
}
