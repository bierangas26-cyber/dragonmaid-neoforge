package com.example.dragonmaid.client;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;

final class NativeMetalEquipment {
    private final Map<String, ArticulatedArmorMesh> pieces = new HashMap<>();

    NativeMetalEquipment(NativeMetalArmor material) {
        HumanoidModel<DragonBaseEntity> humanoidmodel = material.model(true);
        HumanoidModel<DragonBaseEntity> humanoidmodel1 = material.model(false);
        ArrayList<ArticulatedArmorMesh.Vertex> arraylist = new ArrayList<>(ArticulatedArmorMesh.capture(humanoidmodel.head));
        arraylist.addAll(ArticulatedArmorMesh.capture(humanoidmodel.hat));
        this.pieces.put("Head", ArticulatedArmorMesh.slice(arraylist, -100.0F, 100.0F, 0.0F));
        this.pieces.put("Body", ArticulatedArmorMesh.slice(ArticulatedArmorMesh.capture(humanoidmodel1.body), -100.0F, 100.0F, 0.0F));

        for (String s : new String[]{"right", "left"}) {
            boolean flag = s.equals("right");
            List<ArticulatedArmorMesh.Vertex> list = ArticulatedArmorMesh.capture(flag ? humanoidmodel1.rightArm : humanoidmodel1.leftArm);
            this.pieces.put(flag ? "Right Arm" : "Left Arm", ArticulatedArmorMesh.slice(list, -100.0F, 4.0F, 0.0F));
            this.pieces.put("arm_lower_" + s, ArticulatedArmorMesh.slice(list, 4.0F, 8.0F, 4.0F));
            this.pieces.put("hand_" + s, ArticulatedArmorMesh.slice(list, 8.0F, 100.0F, 8.0F));
        }

        this.pieces.putAll(LegArmorMesh.pieces(humanoidmodel, humanoidmodel1));
    }

    void render(String bone, PoseStack pose, VertexConsumer buffer, int light) {
        ArticulatedArmorMesh articulatedarmormesh = this.pieces.get(bone);
        if (articulatedarmormesh != null) {
            pose.pushPose();
            boolean flag = bone.equals("Right Arm") || bone.equals("Left Arm");
            boolean flag1 = bone.startsWith("arm_lower_") || bone.startsWith("hand_");
            if (flag || flag1) {
                boolean flag2 = bone.equals("Right Arm") || bone.endsWith("right");
                pose.translate((double)(flag2 ? 1 : -1) * (flag ? 0.25 : 0.75) / 16.0, 0.0, 0.0);
                pose.scale(0.75F, 1.0F, 1.0F);
            }

            articulatedarmormesh.render(pose, buffer, light);
            pose.popPose();
        }
    }
}
