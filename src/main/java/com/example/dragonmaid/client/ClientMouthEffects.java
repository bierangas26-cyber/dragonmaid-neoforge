package com.example.dragonmaid.client;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidWater;
import com.example.dragonmaid.network.FeedingEffectMessage;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class ClientMouthEffects {
    private static final Map<DragonBaseEntity, ArrayDeque<ClientMouthEffects.Meal>> MEALS = new WeakHashMap<>();
    private static final Map<DragonBaseEntity, Vec3> LIGHTNING = new WeakHashMap<>();
    private static final Map<DragonBaseEntity, ClientMouthEffects.Mouth> WATER_MOUTHS = new WeakHashMap<>();
    private static final Map<DragonBaseEntity, Long> FORGE_BRIDGES = new WeakHashMap<>();

    private ClientMouthEffects() {
    }

    public static void receive(FeedingEffectMessage message) {
        ClientLevel clientlevel = Minecraft.getInstance().level;
        if (clientlevel != null
            && clientlevel.dimension().location().equals(message.dimension())
            && clientlevel.getEntity(message.entityId()) instanceof DragonBaseEntity entitydragonbase
            && entitydragonbase.getUUID().equals(message.uuid())
            && DragonForms.isHumanoid(entitydragonbase)) {
            Item item = Item.byId(message.itemId());
            if (item != Items.AIR) {
                ArrayDeque<ClientMouthEffects.Meal> arraydeque = MEALS.computeIfAbsent(entitydragonbase, key -> new ArrayDeque<>());

                while (arraydeque.size() >= 8) {
                    arraydeque.removeFirst();
                }

                arraydeque.addLast(new ClientMouthEffects.Meal(item, clientlevel.getGameTime() + 10L));
            }
        }
    }

    static Matrix4f begin(DragonBaseEntity dragon, PoseStack pose) {
        LIGHTNING.remove(dragon);
        WATER_MOUTHS.remove(dragon);
        ArrayDeque<ClientMouthEffects.Meal> arraydeque = MEALS.get(dragon);
        if (arraydeque != null) {
            arraydeque.removeIf(meal -> meal.expires < dragon.level().getGameTime());
            if (arraydeque.isEmpty()) {
                MEALS.remove(dragon);
            }
        }

        return !MEALS.containsKey(dragon)
                && (!(dragon instanceof LightningDragonEntity entitylightningdragon) || !entitylightningdragon.hasLightningTarget())
                && !waterBreath(dragon)
            ? null
            : new Matrix4f(pose.last().pose()).invert();
    }

    private static boolean waterBreath(DragonBaseEntity dragon) {
        int i = DragonForms.state(dragon).dragonmaid$getAction();
        return Minecraft.getInstance().screen == null
            && dragon.level() == Minecraft.getInstance().level
            && !dragon.isInvisible()
            && dragon instanceof IceDragonEntity
            && DragonForms.isHumanoid(dragon)
            && HumanoidWater.submerged(dragon)
            && !dragon.onGround()
            && (i == 2 || i == 3 || i == 4);
    }

    static Vec3 waterMouth(DragonBaseEntity dragon) {
        ClientMouthEffects.Mouth clientmoutheffects$mouth = WATER_MOUTHS.get(dragon);
        long i = dragon.level().getGameTime();
        return waterBreath(dragon) && clientmoutheffects$mouth != null && i >= clientmoutheffects$mouth.tick() && i - clientmoutheffects$mouth.tick() <= 2L
            ? clientmoutheffects$mouth.position()
            : null;
    }

    static Vec3 offset(Matrix4fc inverse, Matrix4fc headPose, float x, float y, float z) {
        Vector3f vector3f = new Matrix4f(inverse).mul(headPose).transformPosition(new Vector3f(x, y, z));
        return new Vec3((double)vector3f.x, (double)vector3f.y, (double)vector3f.z);
    }

    static void capture(DragonBaseEntity dragon, Matrix4fc inverse, PoseStack headPose, float x, float y, float z, float partial) {
        Vec3 vec3 = offset(inverse, headPose.last().pose(), x, y, z);
        if (waterBreath(dragon)) {
            Vec3 vec31 = dragon.getPosition(partial).add(vec3);
            long i = dragon.level().getGameTime();
            WATER_MOUTHS.put(dragon, new ClientMouthEffects.Mouth(vec31, i));
            if (DragonForms.state(dragon).dragonmaid$getAction() == 4 && !Long.valueOf(i).equals(FORGE_BRIDGES.get(dragon))) {
                FORGE_BRIDGES.put(dragon, i);
                ClientBreathWaves.bridge((ClientLevel)dragon.level(), vec31, HumanoidWater.breathOrigin(dragon));
            }
        }

        if (dragon instanceof LightningDragonEntity entitylightningdragon && entitylightningdragon.hasLightningTarget()) {
            LIGHTNING.put(dragon, dragon.position().add(vec3));
        }

        ArrayDeque<ClientMouthEffects.Meal> arraydeque = MEALS.remove(dragon);
        if (arraydeque != null) {
            Vec3 vec32 = dragon.getPosition(partial).add(vec3);
            RandomSource randomsource = dragon.getRandom();

            for (ClientMouthEffects.Meal clientmoutheffects$meal : arraydeque) {
                if (clientmoutheffects$meal.expires >= dragon.level().getGameTime()) {
                    ItemParticleOption itemparticleoption = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(clientmoutheffects$meal.item));

                    for (int j = 0; j < 15; j++) {
                        dragon.level()
                            .addParticle(
                                itemparticleoption,
                                vec32.x,
                                vec32.y,
                                vec32.z,
                                randomsource.nextGaussian() * 0.07,
                                randomsource.nextGaussian() * 0.07,
                                randomsource.nextGaussian() * 0.07
                            );
                    }
                }
            }
        }
    }

    public static Vec3 lightningStart(LightningDragonEntity dragon) {
        Vec3 vec3 = LIGHTNING.remove(dragon);
        if (!DragonForms.isHumanoid(dragon)) {
            return dragon.getHeadPosition();
        } else {
            return vec3 != null ? vec3 : dragon.getEyePosition().add(dragon.getLookAngle().scale(0.25 * (double)DragonForms.modelScale(dragon)));
        }
    }

    private static record Meal(Item item, long expires) {
    }

    private static record Mouth(Vec3 position, long tick) {
    }
}
