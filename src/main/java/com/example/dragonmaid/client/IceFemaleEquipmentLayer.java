package com.example.dragonmaid.client;

import com.example.dragonmaid.item.ServiceDragonArmorItem;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

final class IceFemaleEquipmentLayer extends GeoRenderLayer<IceFemaleAnimatable> {
    private final IceFemaleRenderer parent;
    private final ItemInHandRenderer items;
    private final Map<String, IceFemaleEquipmentLayer.Piece> armor = new HashMap<>();
    private final Map<String, ArticulatedArmorMesh> legArmor = LegArmorMesh.vanilla();
    private final Map<NativeMetalArmor, NativeMetalEquipment> nativeArmor = new EnumMap<>(NativeMetalArmor.class);

    IceFemaleEquipmentLayer(IceFemaleRenderer parent, Context context) {
        super(parent);
        this.parent = parent;
        this.items = context.getItemInHandRenderer();
        this.piece("Head", 1, 0, 0, -4.0F, -8.0F, -4.0F, 8, 8, 8);
        this.piece("Body", 3, 16, 16, -4.0F, 0.0F, -2.0F, 8, 12, 4);
        this.piece("Right Arm", 2, 40, 16, -2.0F, -2.0F, -2.0F, 3, 6, 4);
        this.piece("Left Arm", 2, 40, 16, -1.0F, -2.0F, -2.0F, 3, 6, 4);

        for (String s : new String[]{"right", "left"}) {
            this.piece("arm_lower_" + s, 2, 40, 22, -1.5F, 0.0F, -2.0F, 3, 4, 4);
            this.piece("hand_" + s, 2, 40, 26, -1.5F, 0.0F, -2.0F, 3, 2, 4);
        }

        this.legArmor.keySet().forEach(bone -> this.armor.put(bone, new IceFemaleEquipmentLayer.Piece(4, null)));
    }

    private void piece(String bone, int slot, int u, int v, float x, float y, float z, int w, int h, int d) {
        MeshDefinition meshdefinition = new MeshDefinition();
        meshdefinition.getRoot()
            .addOrReplaceChild(
                "piece",
                CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, (float)w, (float)h, (float)d, new CubeDeformation(0.6F)),
                PartPose.ZERO
            );
        this.armor.put(bone, new IceFemaleEquipmentLayer.Piece(slot, LayerDefinition.create(meshdefinition, 64, 32).bakeRoot().getChild("piece")));
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
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)this.parent.getCurrentEntity();
        if (!entitydragonbase.isInvisible()) {
            IceFemaleEquipmentLayer.Piece icefemaleequipmentlayer$piece = this.armor.get(bone.getName());
            boolean flag = bone.getName().equals("Body") && entitydragonbase.getOffhandItem().getItem() instanceof BannerItem;
            if (icefemaleequipmentlayer$piece != null || flag) {
                ItemStack itemstack = icefemaleequipmentlayer$piece != null && entitydragonbase.dragonInventory != null
                    ? entitydragonbase.dragonInventory.getItem(icefemaleequipmentlayer$piece.slot())
                    : ItemStack.EMPTY;
                if (itemstack.isEmpty() && !flag) {
                    buffers.getBuffer(renderType);
                } else {
                    pose.pushPose();

                    try {
                        boolean flag1 = ServiceDragonArmorItem.isService(itemstack);
                        if (flag1 && !bone.isHidden()) {
                            ServiceArmorResources servicearmorresources = ServiceArmorResources.INSTANCE;
                            if (servicearmorresources.slotForBone(bone.getName()) != icefemaleequipmentlayer$piece.slot()) {
                                throw new IllegalStateException("Service armor slot mismatch: " + bone.getName());
                            }

                            VertexConsumer vertexconsumer = buffers.getBuffer(RenderType.armorCutoutNoCull(servicearmorresources.humanTexture(entitydragonbase.isMale())));

                            for (GeoCube geocube : servicearmorresources.forBone(entitydragonbase.isMale(), bone.getName())) {
                                pose.pushPose();

                                try {
                                    // 4.9.2: renderCube takes one packed colour int (was r,g,b,a floats)
                                    this.parent.renderCube(pose, geocube, vertexconsumer, light, OverlayTexture.NO_OVERLAY, -1);
                                } finally {
                                    pose.popPose();
                                }
                            }
                        }

                        FemaleChestplateMaterial femalechestplatematerial = !flag1
                                && icefemaleequipmentlayer$piece != null
                                && icefemaleequipmentlayer$piece.slot() == 3
                            ? FemaleChestplateMaterial.of(itemstack)
                            : null;
                        FemaleChestplateMesh femalechestplatemesh = FemaleChestplateResources.INSTANCE
                            .forEquipment(
                                entitydragonbase.isMale(),
                                icefemaleequipmentlayer$piece == null ? 0 : icefemaleequipmentlayer$piece.slot(),
                                bone.getName(),
                                femalechestplatematerial
                            );
                        if (femalechestplatemesh != null) {
                            femalechestplatemesh.render(
                                pose, buffers.getBuffer(RenderType.armorCutoutNoCull(femalechestplatematerial.texture)), light, femalechestplatematerial
                            );
                        }

                        RenderUtil.translateToPivotPoint(pose, bone);
                        pose.scale(-1.0F, -1.0F, 1.0F);
                        if (icefemaleequipmentlayer$piece != null && !itemstack.isEmpty() && !flag1) {
                            NativeMetalArmor nativemetalarmor = NativeMetalArmor.of(itemstack);
                            if (nativemetalarmor != null) {
                                this.nativeArmor
                                    .computeIfAbsent(nativemetalarmor, NativeMetalEquipment::new)
                                    .render(
                                        bone.getName(),
                                        pose,
                                        buffers.getBuffer(RenderType.armorCutoutNoCull(nativemetalarmor.texture(icefemaleequipmentlayer$piece.slot(), bone.getName()))),
                                        light
                                    );
                            } else if (!entitydragonbase.isMale()
                                && icefemaleequipmentlayer$piece.slot() == 3
                                && FemaleChestplateMaterial.of(itemstack) != null) {
                                VertexConsumer vertexconsumer2 = buffers.getBuffer(RenderType.armorCutoutNoCull(femalechestplatematerial.texture));
                                icefemaleequipmentlayer$piece.model()
                                    .render(
                                        pose,
                                        vertexconsumer2,
                                        light,
                                        OverlayTexture.NO_OVERLAY,
                                        packColor(femalechestplatematerial.red, femalechestplatematerial.green, femalechestplatematerial.blue)
                                    );
                            } else {
                                ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(itemstack.getItem());
                                String s = resourcelocation == null ? "" : resourcelocation.getPath();
                                String s1 = s.contains("gold")
                                    ? "gold"
                                    : (s.contains("diamond") ? "diamond" : (s.contains("dragonsteel") ? "netherite" : "iron"));
                                float f = 1.0F;
                                float f1 = 1.0F;
                                float f2 = 1.0F;
                                if (s.contains("fire")) {
                                    f1 = 0.55F;
                                    f2 = 0.4F;
                                } else if (s.contains("ice")) {
                                    f = 0.55F;
                                    f1 = 0.8F;
                                } else if (s.contains("lightning")) {
                                    f1 = 0.6F;
                                }

                                int i = icefemaleequipmentlayer$piece.slot() == 4 ? LegArmorMesh.textureLayer(bone.getName()) : 1;
                                VertexConsumer vertexconsumer1 = buffers.getBuffer(
                                    RenderType.armorCutoutNoCull(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/models/armor/" + s1 + "_layer_" + i + ".png"))
                                );
                                if (icefemaleequipmentlayer$piece.slot() == 4) {
                                    this.legArmor.get(bone.getName()).render(pose, vertexconsumer1, light, f, f1, f2);
                                } else {
                                    // 1.21.1 ModelPart.render: 5 args with a packed colour int
                                    icefemaleequipmentlayer$piece.model().render(pose, vertexconsumer1, light, OverlayTexture.NO_OVERLAY, packColor(f, f1, f2));
                                }
                            }
                        }

                        if (flag) {
                            CustomHeadLayer.translateToHead(pose, false);
                            this.items.renderItem(entitydragonbase, entitydragonbase.getOffhandItem(), ItemDisplayContext.HEAD, false, pose, buffers, light);
                        }
                    } finally {
                        pose.popPose();
                        buffers.getBuffer(renderType);
                    }
                }
            }
        }
    }

    private static int packColor(float red, float green, float blue) {
        return 0xFF000000 | (toByte(red) << 16) | (toByte(green) << 8) | toByte(blue);
    }

    private static int toByte(float value) {
        int i = (int) (value * 255.0F + 0.5F);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    private static record Piece(int slot, ModelPart model) {
    }
}
