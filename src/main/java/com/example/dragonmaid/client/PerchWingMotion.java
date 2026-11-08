package com.example.dragonmaid.client;

import com.example.dragonmaid.movement.PerchFlight;
import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.TabulaModel;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 1.2: 骑龙时让龙的翅膀拍动（把 6 帧 flyPoses 按时间插值到当前模型上）。
 *
 * <p>Citadel -> IAF CE：模型基类换成 {@code com.iafenvoy.uranus.client.model.TabulaModel} 与
 * {@code AdvancedModelBox}。SRG -> 官方名：{@code m_20202_ -> getVehicle()}、
 * {@code m_20096_ -> onGround()}、{@code m_19879_ -> getId()}、
 * {@code m_14036_ -> Mth.clamp}、{@code m_14177_ -> Mth.wrapDegrees}。
 */
public final class PerchWingMotion {
    private static final String[] WINGS = new String[]{
        "armL1",
        "armL2",
        "ClawL",
        "fingerL1.1",
        "fingerL2.1",
        "fingerL3.1",
        "fingerL4.1",
        "membraneL3",
        "membraneL4",
        "membraneL5",
        "membraneL6",
        "armR1",
        "armR2",
        "ClawR",
        "fingerR1.1",
        "fingerR2.1",
        "fingerR3.1",
        "fingerR4.1",
        "membraneR3",
        "membraneR4",
        "membraneR5",
        "membraneR6"
    };
    private static final Map<DragonBaseEntity, PerchWingMotion.Blend> BLENDS = new WeakHashMap<>();

    private PerchWingMotion() {
    }

    public static void apply(TabulaModel<?> model, @SuppressWarnings("rawtypes") TabulaModel[] flight, DragonBaseEntity dragon, float age) {
        if (dragon.getVehicle() instanceof Player carrier
            && PerchFlight.isCarrierDragon(carrier, dragon)
            && flight != null
            && flight.length == 6) {
            boolean active = !carrier.onGround() && PerchFlight.supportsAirMovement(carrier);
            PerchWingMotion.Blend blend = BLENDS.get(dragon);
            if (blend == null) {
                if (!active) {
                    return;
                }

                blend = new PerchWingMotion.Blend(age - 1.0F);
                BLENDS.put(dragon, blend);
            }

            float step = Mth.clamp((age - blend.age) / 4.0F, 0.0F, 1.0F);
            blend.age = age;
            blend.weight = Mth.clamp(blend.weight + (active ? step : -step), 0.0F, 1.0F);
            if (blend.weight == 0.0F) {
                BLENDS.remove(dragon);
                return;
            }

            float phase = (age % 30.0F + (float)Math.floorMod(dragon.getId(), 30)) % 30.0F / 5.0F;
            int index = (int)phase;
            float t = phase - (float)index;
            @SuppressWarnings("rawtypes")
            TabulaModel from = flight[(index + 5) % 6];
            TabulaModel to = flight[index];
            if (from != null && to != null) {
                for (String bone : WINGS) {
                    AdvancedModelBox live = model.getCube(bone);
                    AdvancedModelBox a = from.getCube(bone);
                    AdvancedModelBox b = to.getCube(bone);
                    if (live != null && a != null && b != null) {
                        live.rotateAngleX = angle(live.rotateAngleX, a.rotateAngleX, b.rotateAngleX, t, blend.weight);
                        live.rotateAngleY = angle(live.rotateAngleY, a.rotateAngleY, b.rotateAngleY, t, blend.weight);
                        live.rotateAngleZ = angle(live.rotateAngleZ, a.rotateAngleZ, b.rotateAngleZ, t, blend.weight);
                    }
                }
            }
        }
    }

    private static float angle(float base, float a, float b, float t, float weight) {
        float flightAngle = a + t * Mth.wrapDegrees((b - a) * (180.0F / (float)Math.PI)) * (float)(Math.PI / 180.0);
        return weight == 1.0F
            ? flightAngle
            : base + weight * Mth.wrapDegrees((flightAngle - base) * (180.0F / (float)Math.PI)) * (float)(Math.PI / 180.0);
    }

    private static final class Blend {
        float age;
        float weight;

        Blend(float age) {
            this.age = age;
        }
    }
}
