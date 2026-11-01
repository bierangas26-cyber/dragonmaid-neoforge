package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.HumanoidRoar;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.joml.Matrix4f;

public final class HumanoidDragonRenderer extends LivingEntityRenderer<DragonBaseEntity, HumanoidModel<DragonBaseEntity>> {
    private Matrix4f mouthRenderInverse;
    private static final ResourceLocation PLACEHOLDER = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    private static final ResourceLocation FEMALE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/slim/alex.png");

    public HumanoidDragonRenderer(Context context, boolean slim) {
        super(context, createPoseModel(context, slim), 0.4F);
        this.addLayer(new HumanoidDragonRenderer.DragonArmorLayer(this, context));
        this.addLayer(new HumanoidBannerLayer(this, context));
        this.addLayer(
            new RenderLayer<DragonBaseEntity, HumanoidModel<DragonBaseEntity>>(this) {
                public void render(
                    PoseStack pose,
                    MultiBufferSource buffers,
                    int light,
                    DragonBaseEntity dragon,
                    float limb,
                    float amount,
                    float partial,
                    float age,
                    float yaw,
                    float pitch
                ) {
                    if (HumanoidDragonRenderer.this.mouthRenderInverse != null) {
                        pose.pushPose();
                        ((HumanoidModel)this.getParentModel()).head.translateAndRotate(pose);
                        ClientMouthEffects.capture(dragon, HumanoidDragonRenderer.this.mouthRenderInverse, pose, 0.0F, -0.09375F, -0.253125F, partial);
                        pose.popPose();
                        HumanoidDragonRenderer.this.mouthRenderInverse = null;
                    }
                }
            }
        );
    }

    public void render(DragonBaseEntity dragon, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        this.mouthRenderInverse = ClientMouthEffects.begin(dragon, pose);

        try {
            super.render(dragon, yaw, partial, pose, buffers, light);
        } finally {
            this.mouthRenderInverse = null;
        }
    }

    static HumanoidModel<DragonBaseEntity> createPoseModel(Context context, boolean slim) {
        return createPoseModel(context, slim, true);
    }

    static HumanoidModel<DragonBaseEntity> createPoseModel(Context context, boolean slim, final boolean commandSitting) {
        return new HumanoidModel<DragonBaseEntity>(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER)) {
            public void setupAnim(DragonBaseEntity dragon, float limb, float amount, float age, float yaw, float pitch) {
                if (dragon.isModelDead()) {
                    this.riding = false;
                    this.crouching = false;
                    this.attackTime = 0.0F;
                    this.swimAmount = 0.0F;
                    super.setupAnim(dragon, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
                    this.head.xRot = this.head.yRot = this.head.zRot = 0.0F;
                    this.rightArm.xRot = this.rightArm.yRot = this.rightArm.zRot = 0.0F;
                    this.leftArm.xRot = this.leftArm.yRot = this.leftArm.zRot = 0.0F;
                    this.rightLeg.xRot = this.rightLeg.yRot = this.rightLeg.zRot = 0.0F;
                    this.leftLeg.xRot = this.leftLeg.yRot = this.leftLeg.zRot = 0.0F;
                    this.body.yScale = HumanoidLife.isInjured(dragon) ? 1.0F + 0.008F * Mth.sin(age * 0.1F) : 1.0F;
                    this.hat.copyFrom(this.head);
                } else {
                    this.body.yScale = 1.0F;
                    this.riding = dragon.isPassenger() || commandSitting && DragonForms.isSeated(dragon);
                    this.crouching = !this.riding && dragon.getPose() == Pose.CROUCHING;
                    super.setupAnim(dragon, limb, amount, age, yaw, pitch);
                    if (!HumanoidWater.submerged(dragon)) {
                        int i = DragonForms.state(dragon).dragonmaid$getAction();
                        if (i == 1) {
                            this.rightArm.xRot = -1.5F;
                            this.leftArm.xRot = -0.4F;
                        }

                        if (i == 2) {
                            this.rightArm.xRot = -1.2F;
                            this.leftArm.xRot = -1.2F;
                        }

                        if (i == 3) {
                            this.rightArm.xRot = -0.65F;
                            this.leftArm.xRot = -0.65F;
                        }

                        if (i == 4) {
                            this.rightArm.xRot = -0.35F;
                            this.leftArm.xRot = -0.35F;
                        }

                        if (i == 5 || HumanoidRoar.isRoaring(dragon)) {
                            this.head.xRot = -0.4F;
                            this.rightArm.xRot = -0.75F;
                            this.leftArm.xRot = -0.75F;
                            this.rightArm.zRot = 0.35F;
                            this.leftArm.zRot = -0.35F;
                        }

                        this.hat.copyFrom(this.head);
                    }
                }
            }
        };
    }

    public ResourceLocation getTextureLocation(DragonBaseEntity dragon) {
        return dragon.isMale() ? PLACEHOLDER : FEMALE;
    }

    protected void scale(DragonBaseEntity dragon, PoseStack pose, float partial) {
        float f = DragonForms.modelScale(dragon);
        pose.scale(f, f, f);
    }

    // 1.21.1: LivingEntityRenderer#setupRotations gained a trailing "scale" parameter
    protected void setupRotations(DragonBaseEntity dragon, PoseStack pose, float age, float yaw, float partial, float scale) {
        super.setupRotations(dragon, pose, age, yaw, partial, scale);
        float f = dragon.isModelDead()
            ? 1.0F
            : Mth.clamp(Mth.lerp(partial, dragon.prevAnimationProgresses[1], dragon.sleepProgress) / 20.0F, 0.0F, 1.0F);
        if (DragonForms.isSeated(dragon)) {
            pose.translate(0.0F, -0.52F * DragonForms.modelScale(dragon) * (1.0F - f), 0.0F);
        }

        if (f > 0.0F) {
            pose.translate(0.0F, 0.25F * f, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F * f));
        }
    }

    private static final class DragonArmorLayer extends RenderLayer<DragonBaseEntity, HumanoidModel<DragonBaseEntity>> {
        private final HumanoidModel<DragonBaseEntity> armor;
        private final Map<NativeMetalArmor, HumanoidDragonRenderer.DragonArmorLayer.NativeModels> nativeArmor = new EnumMap<>(NativeMetalArmor.class);

        DragonArmorLayer(HumanoidDragonRenderer parent, Context context) {
            super(parent);
            this.armor = new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        }

        public void render(
            PoseStack pose,
            MultiBufferSource buffer,
            int light,
            DragonBaseEntity dragon,
            float limb,
            float amount,
            float partial,
            float age,
            float yaw,
            float pitch
        ) {
            if (!dragon.isInvisible()) {
                for (int i = 1; i <= 4; i++) {
                    ItemStack itemstack = dragon.dragonInventory.getItem(i);
                    if (!itemstack.isEmpty()) {
                        NativeMetalArmor nativemetalarmor = NativeMetalArmor.of(itemstack);
                        if (nativemetalarmor != null) {
                            HumanoidDragonRenderer.DragonArmorLayer.NativeModels humanoiddragonrenderer$dragonarmorlayer$nativemodels = this.nativeArmor
                                .computeIfAbsent(
                                    nativemetalarmor, keyx -> new HumanoidDragonRenderer.DragonArmorLayer.NativeModels(keyx.model(true), keyx.model(false))
                                );
                            HumanoidModel<DragonBaseEntity> humanoidmodel = i != 1 && i != 4
                                ? humanoiddragonrenderer$dragonarmorlayer$nativemodels.outer()
                                : humanoiddragonrenderer$dragonarmorlayer$nativemodels.inner();
                            ((HumanoidModel)this.getParentModel()).copyPropertiesTo(humanoidmodel);
                            humanoidmodel.setAllVisible(false);
                            switch (i) {
                                case 1:
                                    humanoidmodel.head.visible = true;
                                    humanoidmodel.hat.visible = true;
                                    break;
                                case 2:
                                    humanoidmodel.leftArm.visible = true;
                                    humanoidmodel.rightArm.visible = true;
                                    break;
                                case 3:
                                    humanoidmodel.body.visible = true;
                                    break;
                                case 4:
                                    humanoidmodel.leftLeg.visible = true;
                                    humanoidmodel.rightLeg.visible = true;
                            }

                            // 1.21.1: ModelPart#renderToBuffer/Model#renderToBuffer take one packed colour int
                            humanoidmodel.renderToBuffer(
                                pose,
                                buffer.getBuffer(RenderType.armorCutoutNoCull(nativemetalarmor.texture(i))),
                                light,
                                OverlayTexture.NO_OVERLAY,
                                -1
                            );
                        } else {
                            ((HumanoidModel)this.getParentModel()).copyPropertiesTo(this.armor);
                            this.armor.setAllVisible(false);
                            switch (i) {
                                case 1:
                                    this.armor.head.visible = true;
                                    break;
                                case 2:
                                    this.armor.leftArm.visible = true;
                                    this.armor.rightArm.visible = true;
                                    break;
                                case 3:
                                    this.armor.body.visible = true;
                                    break;
                                case 4:
                                    this.armor.leftLeg.visible = true;
                                    this.armor.rightLeg.visible = true;
                            }

                            ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(itemstack.getItem());
                            String s1 = resourcelocation == null ? "" : resourcelocation.getPath();
                            String s = s1.contains("gold")
                                ? "gold"
                                : (s1.contains("diamond") ? "diamond" : (s1.contains("dragonsteel") ? "netherite" : "iron"));
                            float f = 1.0F;
                            float f1 = 1.0F;
                            float f2 = 1.0F;
                            if (s1.contains("fire")) {
                                f1 = 0.55F;
                                f2 = 0.4F;
                            } else if (s1.contains("ice")) {
                                f = 0.55F;
                                f1 = 0.8F;
                            } else if (s1.contains("lightning")) {
                                f1 = 0.6F;
                            }

                            VertexConsumer vertexconsumer = buffer.getBuffer(
                                RenderType.armorCutoutNoCull(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/models/armor/" + s + "_layer_1.png"))
                            );
                            // 1.21.1: 8-arg renderToBuffer is gone -> 5 args with a packed ARGB colour
                            int packedColour = 0xFF000000
                                | ((int) (f * 255.0F) << 16)
                                | ((int) (f1 * 255.0F) << 8)
                                | (int) (f2 * 255.0F);
                            this.armor.renderToBuffer(pose, vertexconsumer, light, OverlayTexture.NO_OVERLAY, packedColour);
                        }
                    }
                }
            }
        }

        private static record NativeModels(HumanoidModel<DragonBaseEntity> inner, HumanoidModel<DragonBaseEntity> outer) {
        }
    }
}
