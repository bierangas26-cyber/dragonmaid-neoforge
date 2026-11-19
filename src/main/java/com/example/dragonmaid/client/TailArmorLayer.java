package com.example.dragonmaid.client;

import com.example.dragonmaid.item.ServiceDragonArmorItem;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.render.entity.feature.DragonArmorFeatureRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

final class TailArmorLayer extends GeoRenderLayer<IceFemaleAnimatable> {
    private final IceFemaleRenderer parent;
    private final Map<GeoCube, GeoCube> shells = new WeakHashMap<>();

    TailArmorLayer(IceFemaleRenderer parent) {
        super(parent);
        this.parent = parent;
    }

    public void renderForBone(
        PoseStack pose,
        IceFemaleAnimatable animatable,
        GeoBone bone,
        RenderType renderType,
        MultiBufferSource buffers,
        VertexConsumer vertices,
        float partial,
        int light,
        int overlay
    ) {
        int i = TailArmorMesh.segment(bone.getName());
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)this.parent.getCurrentEntity();
        if (i >= 0 && !entitydragonbase.isInvisible() && entitydragonbase.dragonInventory != null && !bone.isHidden() && !bone.getCubes().isEmpty()) {
            // 1.2.7: 优先交给渲染器的装备队列（龙铠饰品与尾甲共用一套批处理）。
            if (!this.parent.queueArmor(pose, bone, true)) {
                this.drawArmor(pose, bone, buffers, light);
            }
        }
    }

    void drawArmor(PoseStack pose, GeoBone bone, MultiBufferSource buffers, int light) {
        int i = TailArmorMesh.segment(bone.getName());
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)this.parent.getCurrentEntity();
        // IAF CE 2.0: the old EnumDragonTextures.Armor enum (which carried three
        // per-dragon-type textures ICE/LIGHTNING/FIRE) is gone. CE keeps a single
        // texture per material+part (textures/entity/dragon_armor/armor_<part>_<material>.png)
        // and exposes DragonArmorFeatureRenderer.getArmorTexture(stack, slot).
        ResourceLocation resourcelocation = this.textureFor(entitydragonbase);
        if (resourcelocation == null) {
            return;
        }

        GeoCube geocube = this.shells.computeIfAbsent((GeoCube)bone.getCubes().get(0), key -> TailArmorMesh.bake(key, i));
        pose.pushPose();
        // 4.9.2: renderCube takes one packed colour int (was r,g,b,a floats)
        this.parent.renderCube(
            pose, geocube, buffers.getBuffer(RenderType.armorCutoutNoCull(resourcelocation)), light, OverlayTexture.NO_OVERLAY, -1
        );
        pose.popPose();
    }

    /** 1.2.7: 四套龙铠按 Style 分实例；非龙铠仍走 CE 的材质贴图。 */
    private ResourceLocation textureFor(DragonBaseEntity dragon) {
        ServiceDragonArmorItem.Style style = ServiceDragonArmorItem.styleOf(dragon.dragonInventory.getItem(4), dragon);
        if (style != null) {
            ResourceLocation styled = ServiceArmorResources.of(style).humanoidTailTexture(dragon);
            if (styled != null) {
                return styled;
            }
        }

        ItemStack tailStack = dragon.getItemBySlot(EquipmentSlot.FEET);
        return tailStack.isEmpty() ? null : DragonArmorFeatureRenderer.getArmorTexture(tailStack, EquipmentSlot.FEET);
    }
}
