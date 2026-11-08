package com.example.dragonmaid.client;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.util.RenderUtil;

final class IceFemaleRenderer extends GeoReplacedEntityRenderer<DragonBaseEntity, IceFemaleAnimatable> {
    private final FemaleDragonAssets assets;
    private final int stage;
    private Matrix4f mouthRenderInverse;

    IceFemaleRenderer(Context context, FemaleDragonAssets assets, int stage) {
        super(context, new IceFemaleModel(context, assets, stage), new IceFemaleAnimatable(assets));
        this.assets = assets;
        this.stage = stage;
        this.shadowRadius = 0.4F;
        this.addRenderLayer(new IceFemaleEquipmentLayer(this, context));
        this.addRenderLayer(new TailArmorLayer(this));
    }

    public ResourceLocation getTextureLocation(IceFemaleAnimatable animatable) {
        return this.getTextureLocation((DragonBaseEntity)this.getCurrentEntity());
    }

    /**
     * 1.2: 冰龙处于「霜尾」状态时改用 frosty_ 贴图（CE 2.0 的 variant 是颜色名，
     * 由 {@link FemaleDragonAssets#frostyTexture(int, DragonBaseEntity)} 解析）。
     */
    public ResourceLocation getTextureLocation(DragonBaseEntity dragon) {
        if (dragon == null) {
            return this.assets.texture(this.stage, 0);
        }

        return this.assets.species.equals("ice") && DragonForms.state(dragon).dragonmaid$hasFrostyTail()
            ? this.assets.frostyTexture(this.stage, dragon)
            : this.assets.texture(this.stage, dragon);
    }

    public void render(DragonBaseEntity dragon, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ((IceFemaleAnimatable)this.animatable).prepare(dragon);
        this.mouthRenderInverse = ClientMouthEffects.begin(dragon, pose);

        try {
            super.render(dragon, yaw, partial, pose, buffers, light);
        } finally {
            this.mouthRenderInverse = null;
            this.currentEntity = null;
        }
    }

    boolean clearWorldState() {
        this.currentEntity = null;
        this.mouthRenderInverse = null;
        boolean flag = ((IceFemaleAnimatable)this.animatable).clearWorldState();
        ((IceFemaleModel)this.model).clearWorldState();
        return flag;
    }

    // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
    // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
    void renderStatue(DragonBaseEntity dragon, float yaw, PoseStack pose, MultiBufferSource buffers, int light, ResourceLocation material) {
        BakedGeoModel bakedgeomodel = this.model.getBakedModel(this.model.getModelResource((IceFemaleAnimatable)this.animatable));
        List<GeoBone> list = this.model.getAnimationProcessor().getRegisteredBones().stream().map(bonex -> (GeoBone)bonex).toList();
        SharedAnimationPose sharedanimationpose = new SharedAnimationPose(list);
        float[] afloat = sharedanimationpose.buffer();
        float[] afloat1 = sharedanimationpose.buffer();
        sharedanimationpose.capture(afloat);
        ArrayList<boolean[]> arraylist = new ArrayList<>(list.size());

        for (GeoBone geobone : list) {
            arraylist.add(
                new boolean[]{
                    geobone.isHidden(),
                    geobone.isHidingChildren(),
                    geobone.isTrackingMatrices(),
                    geobone.hasRotationChanged(),
                    geobone.hasPositionChanged(),
                    geobone.hasScaleChanged()
                }
            );
            geobone.setTrackingMatrices(false);
        }

        DragonBaseEntity entitydragonbase = (DragonBaseEntity)this.currentEntity;
        Matrix4f matrix4f = this.mouthRenderInverse;
        // 1.2: 坐/睡/死亡姿势下要把手里的物品收起来（手部挂点不渲染手持物）
        IceFemaleModel humanoidModel = (IceFemaleModel)this.model;
        boolean previousStowed = humanoidModel.heldItemsStowed();
        this.currentEntity = dragon;
        this.mouthRenderInverse = null;
        PoseStack posestack = new PoseStack();
        posestack.last().pose().set(pose.last().pose());
        posestack.last().normal().set(pose.last().normal());
        posestack.pushPose();
        boolean flag1 = false /* VF: Semaphore variable */;

        try {
            flag1 = true;
            boolean flag = dragon.isSleeping() || DragonForms.isSeated(dragon) || dragon.isModelDead();
            humanoidModel.setHeldItemsStowed(dragon.isSleeping() || DragonForms.isSeated(dragon));
            String s = dragon.isModelDead()
                ? this.assets.sitPrefix + ".injury_breath"
                : (
                    dragon.isSleeping()
                        ? this.assets.sleepPrefix + ".sleep"
                        : (DragonForms.isSeated(dragon) ? this.assets.sitPrefix + ".sit" : "animation.dragon_shared.idle")
                );
            sharedanimationpose.sample(this.model.getAnimation((IceFemaleAnimatable)this.animatable, s), 0.0, afloat1);
            sharedanimationpose.apply(afloat1);
            this.model.getBone("sleep_eyelids").ifPresent(bonex -> bonex.setHidden(!flag));
            float f = DragonForms.modelScale(dragon);
            posestack.scale(f, f, f);
            posestack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
            posestack.translate(0.0F, 0.01F, 0.0F);
            MultiBufferSource multibuffersource = HumanoidStatueMaterials.wrap(buffers, material);
            MultiBufferSource multibuffersource1 = type -> untinted(multibuffersource.getBuffer(type));
            RenderType rendertype = RenderType.entityCutoutNoCull(this.getTextureLocation(dragon));
            VertexConsumer vertexconsumer = multibuffersource1.getBuffer(rendertype);

            for (GeoBone geobone1 : bakedgeomodel.topLevelBones()) {
                this.renderRecursively(
                    posestack,
                    (IceFemaleAnimatable)this.animatable,
                    geobone1,
                    rendertype,
                    multibuffersource1,
                    vertexconsumer,
                    false,
                    0.0F,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    -1
                );
            }

            flag1 = false;
        } finally {
            if (flag1) {
                humanoidModel.setHeldItemsStowed(previousStowed);
                posestack.popPose();
                this.currentEntity = entitydragonbase;
                this.mouthRenderInverse = matrix4f;
                sharedanimationpose.apply(afloat);
                int i = 0;
                Iterator iterator = list.iterator();

                while (true) {
                    if (!iterator.hasNext()) {
                        ;
                    } else {
                        GeoBone geobone2 = (GeoBone)iterator.next();
                        boolean[] aboolean = arraylist.get(i++);
                        geobone2.setHidden(aboolean[0]);
                        geobone2.setChildrenHidden(aboolean[1]);
                        geobone2.setTrackingMatrices(aboolean[2]);
                        geobone2.resetStateChanges();
                        if (aboolean[3]) {
                            geobone2.markRotationAsChanged();
                        }

                        if (aboolean[4]) {
                            geobone2.markPositionAsChanged();
                        }

                        if (aboolean[5]) {
                            geobone2.markScaleAsChanged();
                        }
                    }
                }
            }
        }

        humanoidModel.setHeldItemsStowed(previousStowed);
        posestack.popPose();
        this.currentEntity = entitydragonbase;
        this.mouthRenderInverse = matrix4f;
        sharedanimationpose.apply(afloat);
        int j = 0;

        for (GeoBone geobone3 : list) {
            boolean[] aboolean1 = arraylist.get(j++);
            geobone3.setHidden(aboolean1[0]);
            geobone3.setChildrenHidden(aboolean1[1]);
            geobone3.setTrackingMatrices(aboolean1[2]);
            geobone3.resetStateChanges();
            if (aboolean1[3]) {
                geobone3.markRotationAsChanged();
            }

            if (aboolean1[4]) {
                geobone3.markPositionAsChanged();
            }

            if (aboolean1[5]) {
                geobone3.markScaleAsChanged();
            }
        }
    }

    /**
     * 1.21.1 port: the VertexConsumer interface is now per-vertex
     * (addVertex / setColor / setUv / setUv1 / setUv2 / setNormal);
     * endVertex() and defaultColor()/unsetDefaultColor() no longer exist.
     * This wrapper keeps the same job: force full-white vertex colour.
     */
    private static VertexConsumer untinted(final VertexConsumer sink) {
        return new VertexConsumer() {
            @Override
            public VertexConsumer addVertex(float x, float y, float z) {
                sink.addVertex(x, y, z);
                return this;
            }

            @Override
            public VertexConsumer setColor(int r, int g, int b, int a) {
                sink.setColor(255, 255, 255, 255);
                return this;
            }

            @Override
            public VertexConsumer setUv(float u, float v) {
                sink.setUv(u, v);
                return this;
            }

            @Override
            public VertexConsumer setUv1(int u, int v) {
                sink.setUv1(u, v);
                return this;
            }

            @Override
            public VertexConsumer setUv2(int u, int v) {
                sink.setUv2(u, v);
                return this;
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) {
                sink.setNormal(x, y, z);
                return this;
            }
        };
    }

    public void applyRenderLayersForBone(
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
        if (this.mouthRenderInverse != null && bone.getName().equals("Head")) {
            pose.pushPose();
            RenderUtil.translateToPivotPoint(pose, bone);
            ClientMouthEffects.capture((DragonBaseEntity)this.getCurrentEntity(), this.mouthRenderInverse, pose, 0.0F, 0.09375F, -0.253125F, partial);
            pose.popPose();
            this.mouthRenderInverse = null;
        }

        super.applyRenderLayersForBone(pose, animatable, bone, renderType, buffers, vertices, partial, light, overlay);
    }

    public void preRender(
        PoseStack pose,
        IceFemaleAnimatable animatable,
        BakedGeoModel model,
        MultiBufferSource buffers,
        VertexConsumer vertices,
        boolean reRender,
        float partial,
        int light,
        int overlay,
        int colour
    ) {
        float f = DragonForms.modelScale((DragonBaseEntity)this.getCurrentEntity());
        this.scaleWidth = f;
        this.scaleHeight = f;
        super.preRender(pose, animatable, model, buffers, vertices, reRender, partial, light, overlay, colour);
    }

    protected void applyRotations(IceFemaleAnimatable animatable, PoseStack pose, float age, float yaw, float partial) {
        super.applyRotations(animatable, pose, age, ClientTailSweepAnimations.bodyYaw((DragonBaseEntity)this.getCurrentEntity(), partial, yaw), partial);
    }

    protected float getDeathMaxRotation(IceFemaleAnimatable animatable) {
        return 0.0F;
    }
}
