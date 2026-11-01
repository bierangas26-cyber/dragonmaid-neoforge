package com.example.dragonmaid.client;

import java.util.Set;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

final class SleepPoseGrounding {
    private static final Set<String> SUPPORT = Set.of(
        "Head",
        "Body",
        "Chest",
        "Right Arm",
        "Left Arm",
        "arm_lower_right",
        "arm_lower_left",
        "hand_right",
        "hand_left",
        "Right Leg",
        "Left Leg",
        "leg_lower_right",
        "leg_lower_left",
        "foot_right",
        "foot_left"
    );
    private static final float CONTACT_Y = -0.005F;

    private SleepPoseGrounding() {
    }

    static void align(GeoBone root) {
        float f = lowestSupportPoint(root);
        if (Float.isFinite(f)) {
            root.setPosY(root.getPosY() + (-0.005F - f) * 16.0F);
        }

        root.resetStateChanges();
    }

    static float lowestSupportPoint(GeoBone root) {
        return lowest(root, new Matrix4f(), new Vector3f());
    }

    private static float lowest(GeoBone bone, Matrix4f parent, Vector3f vertex) {
        String s = bone.getName();
        if (!s.equals("Root") && !s.equals("Waist") && !SUPPORT.contains(s)) {
            return Float.POSITIVE_INFINITY;
        } else {
            Matrix4f matrix4f = new Matrix4f(parent)
                .translate(-bone.getPosX() / 16.0F, bone.getPosY() / 16.0F, bone.getPosZ() / 16.0F)
                .translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F)
                .rotateZ(bone.getRotZ())
                .rotateY(bone.getRotY())
                .rotateX(bone.getRotX())
                .scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ())
                .translate(-bone.getPivotX() / 16.0F, -bone.getPivotY() / 16.0F, -bone.getPivotZ() / 16.0F);
            float f = Float.POSITIVE_INFINITY;
            if (SUPPORT.contains(s) && !bone.isHidden()) {
                for (GeoCube geocube : bone.getCubes()) {
                    Vec3 vec3 = geocube.pivot();
                    Vec3 vec31 = geocube.rotation();
                    Matrix4f matrix4f1 = new Matrix4f(matrix4f)
                        .translate((float)vec3.x / 16.0F, (float)vec3.y / 16.0F, (float)vec3.z / 16.0F)
                        .rotateZ((float)vec31.z)
                        .rotateY((float)vec31.y)
                        .rotateX((float)vec31.x)
                        .translate((float)(-vec3.x) / 16.0F, (float)(-vec3.y) / 16.0F, (float)(-vec3.z) / 16.0F);

                    for (GeoQuad geoquad : geocube.quads()) {
                        if (geoquad != null) {
                            for (GeoVertex geovertex : geoquad.vertices()) {
                                matrix4f1.transformPosition(geovertex.position(), vertex);
                                f = Math.min(f, vertex.y);
                            }
                        }
                    }
                }
            }

            if (!bone.isHidingChildren()) {
                for (GeoBone geobone : bone.getChildBones()) {
                    f = Math.min(f, lowest(geobone, matrix4f, vertex));
                }
            }

            return f;
        }
    }
}
