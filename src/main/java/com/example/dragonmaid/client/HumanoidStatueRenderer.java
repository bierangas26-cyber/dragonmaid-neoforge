package com.example.dragonmaid.client;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class HumanoidStatueRenderer {
    private static final ResourceLocation STONE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
    private static final ResourceLocation[] CRACKS = new ResourceLocation[10];
    private static final Map<StoneStatueEntity, HumanoidStatueRenderer.Cached> CACHE = new WeakHashMap<>();

    private HumanoidStatueRenderer() {
    }

    public static void clear() {
        CACHE.clear();
        HumanoidStatueMaterials.clear();
    }

    public static boolean render(StoneStatueEntity statue, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        CompoundTag compoundtag = statue.getTrappedTag();
        if (compoundtag.getCompound("DragonMaid").getBoolean("Humanoid")
            && (
                statue.getTrappedEntityType() == IafEntities.FIRE_DRAGON.get()
                    || statue.getTrappedEntityType() == IafEntities.ICE_DRAGON.get()
                    || statue.getTrappedEntityType() == IafEntities.LIGHTNING_DRAGON.get()
            )) {
            if (ClientRegistration.dragonRenderers.isEmpty()) {
                return true;
            } else {
                HumanoidStatueRenderer.Cached humanoidstatuerenderer$cached = CACHE.get(statue);
                if (humanoidstatuerenderer$cached == null || humanoidstatuerenderer$cached.source() != compoundtag) {
                    DragonBaseEntity entitydragonbase = null;

                    try {
                        entitydragonbase = (DragonBaseEntity)statue.getTrappedEntityType().create(statue.level());
                        if (entitydragonbase != null) {
                            entitydragonbase.load(compoundtag.copy());
                            entitydragonbase.setInvisible(false);
                            entitydragonbase.setNoAi(true);
                            entitydragonbase.setNoGravity(true);
                        }
                    } catch (RuntimeException runtimeexception1) {
                        LogUtils.getLogger().warn("Could not read humanoid statue {}", statue.getUUID(), runtimeexception1);
                        entitydragonbase = null;
                    }

                    humanoidstatuerenderer$cached = new HumanoidStatueRenderer.Cached(compoundtag, entitydragonbase);
                    CACHE.put(statue, humanoidstatuerenderer$cached);
                }

                if (humanoidstatuerenderer$cached.dragon() == null) {
                    return true;
                } else {
                    DragonBaseEntity entitydragonbase1 = humanoidstatuerenderer$cached.dragon();
                    entitydragonbase1.setPos(statue.position());
                    IceFemaleRenderer icefemalerenderer = ClientRegistration.dragonRenderers.get(FemaleDragonAssets.of(entitydragonbase1))[FemaleDragonAssets.stageIndex(
                        MiniatureForm.humanoidModelStage(entitydragonbase1)
                    )];
                    float f = Mth.rotLerp(partial, statue.yRotO, statue.getYRot());

                    try {
                        icefemalerenderer.renderStatue(entitydragonbase1, f, pose, buffers, light, STONE);
                        if (statue.getCrackAmount() > 0) {
                            int i = Mth.clamp(statue.getCrackAmount() - 1, 0, 9);
                            if (CRACKS[i] == null) {
                                CRACKS[i] = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/destroy_stage_" + i + ".png");
                            }

                            icefemalerenderer.renderStatue(entitydragonbase1, f, pose, buffers, light, CRACKS[i]);
                        }
                    } catch (RuntimeException runtimeexception) {
                        CACHE.put(statue, new HumanoidStatueRenderer.Cached(compoundtag, null));
                        LogUtils.getLogger().warn("Could not draw humanoid statue {}", statue.getUUID(), runtimeexception);
                    }

                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private static record Cached(CompoundTag source, DragonBaseEntity dragon) {
    }
}
