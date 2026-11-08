package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.DragonShield;
import com.example.dragonmaid.combat.HumanoidIdleLook;
import com.example.dragonmaid.combat.HumanoidMeleeTiming;
import com.example.dragonmaid.combat.HumanoidRoar;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

final class IceFemaleModel extends GeoModel<IceFemaleAnimatable> {
    private static final ResourceLocation[] SHARED_ANIMATIONS = new ResourceLocation[]{
        asset("animations/shared/dragon_shared.idle_walk.animation.json"),
        asset("animations/shared/dragon_shared.legacy_tail.animation.json"),
        asset("animations/shared/dragon_shared.breath_followup.animation.json"),
        asset("animations/breath/fire_ice/dragon_fire_ice.breath_first.animation.json"),
        asset("animations/shared/dragon_shared.tail_sweep.animation.json"),
        asset("animations/shared/dragon_shared.claw_combo.animation.json"),
        asset("animations/shared/dragon_shared.sword_combo.animation.json")
    };
    private static final String[] LOWER_ARMS = new String[]{"arm_lower_right", "arm_lower_left", "hand_right", "hand_left"};
    private static final String[] TAIL = new String[]{"tail_1", "tail_2", "tail_3", "tail_4", "tail_5", "tail_6"};
    private static final String[] BREATH_LEGS = new String[]{"Root", "Right Leg", "Left Leg", "leg_lower_right", "leg_lower_left", "foot_right", "foot_left"};
    private static final String[] WATER_BREATH_BONES = new String[]{
        "Waist",
        "Chest",
        "Head",
        "Right Arm",
        "Left Arm",
        "arm_lower_right",
        "arm_lower_left",
        "hand_right",
        "hand_left",
        "wing_right",
        "wing_tip_right",
        "wing_left",
        "wing_tip_left"
    };
    private final FemaleDragonAssets assets;
    private final int stage;
    private final ResourceLocation[] animationFallbacks;
    private final HumanoidModel<DragonBaseEntity> vanillaPose;
    private SharedAnimationPose rig;
    private float[] targetPose;
    private float[] walkPose;
    private float[] eyePose;
    private float[] legacyTailPose;
    private float[] legacyTailWalkPose;
    private final Map<DragonBaseEntity, SharedAnimationPose.Transition> transitions = new WeakHashMap<>();
    private final Map<DragonBaseEntity, BreathPoseLayer> breathLayers = new WeakHashMap<>();
    private final Map<DragonBaseEntity, SitTimeline> sitting = new WeakHashMap<>();
    private final Map<DragonBaseEntity, FemaleLifeTimeline> life = new WeakHashMap<>();
    private final Map<DragonBaseEntity, LightningBreathTimeline> lightningBreath = new WeakHashMap<>();
    private final Map<DragonBaseEntity, ForgeBreathTimeline> forgeBreath = new WeakHashMap<>();
    private final Map<DragonBaseEntity, IceFemaleModel.SitSupport> sitSupports = new WeakHashMap<>();
    /** 1.2: 坐/睡姿势下把手里拿的东西收起来（IceFemaleEquipmentLayer 每帧读取）。 */
    private boolean heldItemsStowed;
    private BakedGeoModel checkedFinModel;

    boolean heldItemsStowed() {
        return this.heldItemsStowed;
    }

    void setHeldItemsStowed(boolean stowed) {
        this.heldItemsStowed = stowed;
    }

    IceFemaleModel(Context context, FemaleDragonAssets assets, int stage) {
        this.assets = assets;
        this.stage = stage;
        ArrayList<ResourceLocation> arraylist = new ArrayList<>(List.of(SHARED_ANIMATIONS));
        arraylist.add(assets.sitResource);
        if (assets.lifeResource != null) {
            arraylist.add(assets.lifeResource);
        }

        if (assets.breathResource != null) {
            arraylist.add(assets.breathResource);
        }

        // 1.2: 水中姿势新增 swim_tail / water_idle 动画（冰龙复用 fire 的 water_idle）
        if (assets.species.equals("ice")) {
            arraylist.add(asset("animations/water/dragon_ice_stage" + (3 + FemaleDragonAssets.stageIndex(stage)) + ".swim_tail.animation.json"));
        }

        arraylist.add(asset("animations/water/dragon_" + this.waterIdleSpecies() + ".water_idle.animation.json"));
        this.animationFallbacks = arraylist.toArray(ResourceLocation[]::new);
        this.vanillaPose = HumanoidDragonRenderer.createPoseModel(context, !assets.male, false);
    }

    private String waterIdleSpecies() {
        return this.assets.species.equals("ice") ? "fire" : this.assets.species;
    }

    public BakedGeoModel getBakedModel(ResourceLocation location) {
        BakedGeoModel bakedgeomodel = super.getBakedModel(location);
        if (this.assets.species.equals("ice") && location.equals(this.assets.model(this.stage)) && bakedgeomodel != this.checkedFinModel) {
            bakedgeomodel.getBone("tail_6").ifPresent(bone -> {
                for (GeoCube geocube : bone.getCubes()) {
                    removeDuplicateFinFace(geocube);
                }
            });
            this.checkedFinModel = bakedgeomodel;
        }

        return bakedgeomodel;
    }

    private static void removeDuplicateFinFace(GeoCube cube) {
        if (cube.size().y() == 0.0 && cube.inflate() == 0.0 && !(cube.size().x() <= 0.0) && !(cube.size().z() <= 0.0)) {
            GeoQuad[] ageoquad = cube.quads();
            GeoQuad geoquad = null;
            int i = -1;

            for (int j = 0; j < ageoquad.length; j++) {
                if (ageoquad[j] != null) {
                    if (ageoquad[j].direction() == Direction.UP) {
                        geoquad = ageoquad[j];
                    }

                    if (ageoquad[j].direction() == Direction.DOWN) {
                        i = j;
                    }
                }
            }

            if (geoquad != null && i >= 0) {
                GeoQuad geoquad1 = ageoquad[i];
                if (geoquad.vertices().length == geoquad1.vertices().length) {
                    for (GeoVertex geovertex : geoquad.vertices()) {
                        boolean flag = false;

                        for (GeoVertex geovertex1 : geoquad1.vertices()) {
                            if (geovertex.position().equals(geovertex1.position())
                                && Float.compare(geovertex.texU(), geovertex1.texU()) == 0
                                && Float.compare(geovertex.texV(), geovertex1.texV()) == 0) {
                                flag = true;
                                break;
                            }
                        }

                        if (!flag) {
                            return;
                        }
                    }

                    ageoquad[i] = null;
                }
            }
        }
    }

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath("dragonmaid", path);
    }

    public ResourceLocation getModelResource(IceFemaleAnimatable animatable) {
        return this.assets.model(this.stage);
    }

    public ResourceLocation getTextureResource(IceFemaleAnimatable animatable) {
        return this.assets.texture(this.stage, 0);
    }

    public ResourceLocation getAnimationResource(IceFemaleAnimatable animatable) {
        return this.assets.sleepResource;
    }

    public ResourceLocation[] getAnimationResourceFallbacks(IceFemaleAnimatable animatable) {
        return this.animationFallbacks;
    }

    void clearWorldState() {
        this.heldItemsStowed = false;
        this.transitions.clear();
        this.breathLayers.clear();
        this.sitting.clear();
        this.life.clear();
        this.lightningBreath.clear();
        this.forgeBreath.clear();
        this.sitSupports.clear();
        this.rig = null;
        this.targetPose = this.walkPose = this.eyePose = this.legacyTailPose = this.legacyTailWalkPose = null;
    }

    public void setCustomAnimations(IceFemaleAnimatable animatable, long instanceId, AnimationState<IceFemaleAnimatable> state) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)state.getData(DataTickets.ENTITY);
        Collection<GeoBone> collection = this.getAnimationProcessor().getRegisteredBones();
        if (this.rig == null || !this.rig.matches(collection)) {
            this.rig = new SharedAnimationPose(collection);
            this.targetPose = this.rig.buffer();
            this.walkPose = this.rig.buffer();
            this.eyePose = this.rig.buffer();
            this.legacyTailPose = this.rig.buffer();
            this.legacyTailWalkPose = this.rig.buffer();
            this.transitions.clear();
            this.breathLayers.clear();
            this.sitting.clear();
            this.life.clear();
            this.lightningBreath.clear();
            this.forgeBreath.clear();
            this.sitSupports.clear();
        }

        LocomotionMotion locomotionmotion = HumanoidMotion.get(entitydragonbase);
        BreathTimeline.Frame breathtimeline$frame = ClientBreathAnimations.frame(entitydragonbase, state.getPartialTick());
        TailSweepTimeline.Frame tailsweeptimeline$frame = ClientTailSweepAnimations.frame(entitydragonbase, state.getPartialTick());
        double d0 = (double)((float)entitydragonbase.tickCount + state.getPartialTick());
        Animation animation = this.getAnimation(animatable, this.assets.sitPrefix + ".injury_enter");
        Animation animation1 = this.getAnimation(animatable, this.assets.sitPrefix + ".injury_breath");
        Animation animation2 = this.getAnimation(animatable, this.assets.sitPrefix + ".death");
        Animation animation3 = this.getAnimation(animatable, this.assets.sitPrefix + ".injury_exit");
        boolean flag = Boolean.TRUE.equals(state.getData(IceFemaleAnimatable.SLEEP_POSE));
        boolean flag1 = HumanoidWater.active(entitydragonbase);
        boolean flag2 = HumanoidWater.submerged(entitydragonbase);
        int i = DragonForms.state(entitydragonbase).dragonmaid$getAction();
        // 1.2: 近战动作（爪击/剑技）时间线 + 盾牌姿态
        long worldTick = entitydragonbase.level().getGameTime();
        HumanoidMeleeTiming humanoidmeleettiming = i == 1
            ? HumanoidMeleeTiming.decode(DragonForms.state(entitydragonbase).dragonmaid$getMeleeMotion(), worldTick)
            : null;
        BreathTimeline.Frame breathtimeline$frame1 = null;
        if (this.assets.breathResource == null) {
            boolean flag3 = tailsweeptimeline$frame != null
                || flag
                || entitydragonbase.isSleeping()
                || entitydragonbase.isModelDead()
                || entitydragonbase.isPassenger()
                || i == 1
                || i == 2
                || i == 3
                || i == 5
                || entitydragonbase.getAttackAnim(state.getPartialTick()) > 0.0F
                || HumanoidRoar.isRoaring(entitydragonbase);
            breathtimeline$frame1 = this.forgeBreath
                .computeIfAbsent(entitydragonbase, ignored -> new ForgeBreathTimeline())
                .update(d0, i == 4, flag3, locomotionmotion.generation());
            if (breathtimeline$frame1 != null) {
                breathtimeline$frame = breathtimeline$frame1;
            }
        }

        boolean flag8 = this.assets.breathResource == null && (i == 4 || breathtimeline$frame1 != null);
        LightningBreathTimeline.Frame lightningbreathtimeline$frame = null;
        if (this.assets.breathResource != null) {
            boolean flag4 = tailsweeptimeline$frame != null
                || flag
                || entitydragonbase.isSleeping()
                || entitydragonbase.isModelDead()
                || entitydragonbase.isPassenger()
                || !flag1 && DragonForms.isSeated(entitydragonbase) && i != 4
                || i == 1
                || i == 5
                || entitydragonbase.getAttackAnim(state.getPartialTick()) > 0.0F
                || HumanoidRoar.isRoaring(entitydragonbase);
            lightningbreathtimeline$frame = this.lightningBreath
                .computeIfAbsent(entitydragonbase, ignored -> new LightningBreathTimeline())
                .update(
                    d0,
                    i == 2 || i == 3 || i == 4,
                    flag4,
                    locomotionmotion.generation(),
                    this.getAnimation(animatable, this.assets.breathPrefix + ".breath_start").length(),
                    this.getAnimation(animatable, this.assets.breathPrefix + ".breath_loop").length(),
                    this.getAnimation(animatable, this.assets.breathPrefix + ".breath_end").length()
                );
        }

        boolean flag9 = lightningbreathtimeline$frame != null && lightningbreathtimeline$frame.active();
        Animation animation4 = this.getAnimation(animatable, this.assets.sitPrefix + ".sit_enter");
        Animation animation5 = this.getAnimation(animatable, this.assets.sitPrefix + ".sit");
        Animation animation6 = this.getAnimation(animatable, this.assets.sitPrefix + ".sit_exit");
        boolean flag5 = tailsweeptimeline$frame != null
            || flag
            || entitydragonbase.isSleeping()
            || entitydragonbase.isModelDead()
            || entitydragonbase.hurtTime > 0
            || !this.hasSitSupport(entitydragonbase)
            || entitydragonbase.isInWaterOrBubble()
            || entitydragonbase.isInLava()
            || entitydragonbase.isPassenger()
            || i != 0 && !flag8
            || entitydragonbase.getAttackAnim(state.getPartialTick()) > 0.0F
            || HumanoidRoar.isRoaring(entitydragonbase)
            || flag9
            || !flag8 && this.assets.breathResource == null && breathtimeline$frame != null && breathtimeline$frame.weight() > 0.0F
            || !DragonForms.isSeated(entitydragonbase) && locomotionmotion.weight(1.0F) > 0.1F;
        FemaleLifeTimeline.Frame femalelifetimeline$frame = this.life
            .computeIfAbsent(entitydragonbase, ignored -> new FemaleLifeTimeline())
            .update(
                d0,
                entitydragonbase.isModelDead(),
                HumanoidLife.isInjured(entitydragonbase),
                entitydragonbase.modelDeadProgress < 20.0F,
                locomotionmotion.generation(),
                animation.length(),
                animation1.length(),
                animation2.length(),
                animation3.length(),
                DragonForms.isSeated(entitydragonbase) && !flag5
            );
        boolean flag6 = femalelifetimeline$frame.phase() == FemaleLifeTimeline.Phase.RECOVERING;
        SitTimeline sittimeline = this.sitting.computeIfAbsent(entitydragonbase, ignored -> new SitTimeline());
        if (femalelifetimeline$frame.resumeSeated() || Boolean.TRUE.equals(state.getData(IceFemaleAnimatable.RESUME_SEATED))) {
            sittimeline.resumeLoop();
        }

        SitTimeline.Frame sittimeline$frame = sittimeline.update(
            d0,
            DragonForms.isSeated(entitydragonbase),
            flag5 || flag6,
            locomotionmotion.generation(),
            animation4.length(),
            animation5.length(),
            animation6.length()
        );
        this.heldItemsStowed = flag || sittimeline$frame.active();
        if (!flag) {
            animatable.rememberSeatedPose(
                entitydragonbase, sittimeline$frame.active() && sittimeline$frame.phase() != SitTimeline.Phase.EXIT, locomotionmotion.generation()
            );
        }

        this.getBone("sleep_eyelids").ifPresent(bonex -> bonex.setHidden(!flag && !sittimeline$frame.active() && !entitydragonbase.isModelDead() && !flag6));
        boolean flag7 = false;
        int j;
        if (flag) {
            this.rig.capture(this.targetPose);
            j = 2;
        } else if (entitydragonbase.isModelDead() || flag6) {
            Animation animation9 = switch (femalelifetimeline$frame.phase()) {
                case ENTER -> animation;
                case INJURED -> animation1;
                case RECOVERING -> animation3;
                case DYING -> animation2;
                default -> this.getAnimation(animatable, this.assets.sitPrefix + ".dead");
            };
            this.rig.sampleOnce(animation9, femalelifetimeline$frame.tick(), this.targetPose);
            j = -100 - femalelifetimeline$frame.revision();
        } else if (tailsweeptimeline$frame != null) {
            this.rig.sampleOnce(this.getAnimation(animatable, "animation.dragon_shared.tail_sweep"), tailsweeptimeline$frame.tick(), this.targetPose);
            if (tailsweeptimeline$frame.weight() < 1.0F) {
                this.rig.sample(this.getAnimation(animatable, "animation.dragon_shared.idle"), d0, this.walkPose);
                SharedAnimationPose.mix(this.walkPose, this.targetPose, tailsweeptimeline$frame.weight(), this.targetPose);
            }

            this.getBone("sleep_eyelids").ifPresent(bonex -> bonex.setHidden(true));
            j = Integer.MAX_VALUE;
        } else if (humanoidmeleettiming != null) {
            HumanoidMeleeTiming.Frame humanoidmeleettiming$frame = humanoidmeleettiming.frame(worldTick, state.getPartialTick());
            Animation humanoidmeleeclip = this.getAnimation(animatable, humanoidmeleettiming$frame.clip());
            if (humanoidmeleeclip != null) {
                this.rig.sampleOnce(humanoidmeleeclip, humanoidmeleettiming$frame.tick(), this.targetPose);
            }

            this.getBone("sleep_eyelids").ifPresent(bonex -> bonex.setHidden(true));
            j = 2147483646;
        } else if (sittimeline$frame.active()) {
            Animation animation7 = switch (sittimeline$frame.phase()) {
                case ENTER -> animation4;
                case LOOP -> animation5;
                case EXIT -> animation6;
                default -> throw new IllegalStateException("Inactive sitting pose");
            };
            if (sittimeline$frame.phase() == SitTimeline.Phase.LOOP) {
                this.rig.sample(animation7, sittimeline$frame.tick(), this.targetPose);
            } else {
                this.rig.sampleOnce(animation7, sittimeline$frame.tick(), this.targetPose);
            }

            if (flag8 && breathtimeline$frame1 != null) {
                this.breathLayers
                    .computeIfAbsent(entitydragonbase, ignored -> new BreathPoseLayer(this.rig))
                    .apply(
                        this.rig,
                        this.getAnimation(animatable, "animation.dragon_fire_ice.breath_first"),
                        this.getAnimation(animatable, "animation.dragon_shared.breath_followup"),
                        breathtimeline$frame1,
                        (double)((float)entitydragonbase.level().getGameTime() + state.getPartialTick()),
                        this.targetPose
                    );
                EntityModelData entitymodeldata = (EntityModelData)state.getData(DataTickets.ENTITY_MODEL_DATA);
                this.rig.add(this.targetPose, "Head", 0, entitymodeldata.headPitch() * (float) (Math.PI / 180.0) * breathtimeline$frame1.weight());
                this.rig.add(this.targetPose, "Head", 1, entitymodeldata.netHeadYaw() * (float) (Math.PI / 180.0) * breathtimeline$frame1.weight());
            } else if (!flag8
                && sittimeline$frame.phase() == SitTimeline.Phase.LOOP
                && HumanoidIdleLook.seatedGlance(entitydragonbase)
                && (breathtimeline$frame == null || breathtimeline$frame.weight() <= 0.0F)) {
                EntityModelData entitymodeldata0 = (EntityModelData)state.getData(DataTickets.ENTITY_MODEL_DATA);
                this.rig.add(this.targetPose, "Head", 0, Mth.clamp(entitymodeldata0.headPitch(), -25.0F, 25.0F) * (float) (Math.PI / 180.0));
                this.rig.add(this.targetPose, "Head", 1, Mth.clamp(entitymodeldata0.netHeadYaw(), -45.0F, 45.0F) * (float) (Math.PI / 180.0));
            }

            j = 100 + sittimeline$frame.revision();
        } else if (flag1) {
            boolean flag10 = HumanoidWater.canDive(entitydragonbase) && flag2 && !entitydragonbase.onGround();
            if (flag10) {
                this.rig.reset(this.targetPose);
                EntityModelData entitymodeldata2 = (EntityModelData)state.getData(DataTickets.ENTITY_MODEL_DATA);
                boolean idleSwimLook = DragonForms.state(entitydragonbase).dragonmaid$isIdleLooking()
                    && i == 0
                    && !flag9
                    && breathtimeline$frame == null
                    && !entitydragonbase.isBreathingFire()
                    && !HumanoidRoar.isRoaring(entitydragonbase)
                    && entitydragonbase.getAttackAnim(state.getPartialTick()) <= 0.0F;
                this.vanillaPose.attackTime = 0.0F;
                this.vanillaPose.young = false;
                this.vanillaPose.prepareMobModel(entitydragonbase, state.getLimbSwing(), state.getLimbSwingAmount(), state.getPartialTick());
                this.vanillaPose.swimAmount = 1.0F;
                this.vanillaPose
                    .setupAnim(
                        entitydragonbase,
                        state.getLimbSwing(),
                        state.getLimbSwingAmount(),
                        (float)d0,
                        -entitymodeldata2.netHeadYaw(),
                        -entitymodeldata2.headPitch()
                    );
                this.vanillaPose.head.xRot = (float) (-Math.PI / 4)
                    - (idleSwimLook ? entitymodeldata2.headPitch() * (float) (Math.PI / 180.0) : 0.0F);
                this.copyPose("Head", this.vanillaPose.head, 0.0F, 0.0F, 0.0F);
                this.copyPose("Body", this.vanillaPose.body, 0.0F, 0.0F, 0.0F);
                this.copyPose("Right Arm", this.vanillaPose.rightArm, -5.0F, 2.0F, 0.0F);
                this.copyPose("Left Arm", this.vanillaPose.leftArm, 5.0F, 2.0F, 0.0F);
                this.copyPose("Right Leg", this.vanillaPose.rightLeg, -1.9F, 12.0F, 0.0F);
                this.copyPose("Left Leg", this.vanillaPose.leftLeg, 1.9F, 12.0F, 0.0F);
                SwimPose.root(
                    this.rig,
                    this.targetPose,
                    idleSwimLook ? 0.0F : Mth.lerp(state.getPartialTick(), entitydragonbase.xRotO, entitydragonbase.getXRot())
                );
                Animation swimTailAnimation = this.getAnimation(animatable, "animation.dragon_ice.swim_tail");
                if (swimTailAnimation != null) {
                    this.rig.sample(swimTailAnimation, d0, this.walkPose);

                    for (String s : TAIL) {
                        this.rig.mixBone(s, this.walkPose, 1.0F, this.targetPose);
                    }
                }

                j = idleSwimLook ? 32 : 30;
            } else {
                boolean waterIdle = !entitydragonbase.onGround()
                    && i == 0
                    && !flag9
                    && breathtimeline$frame == null
                    && !entitydragonbase.isBreathingFire()
                    && !HumanoidRoar.isRoaring(entitydragonbase)
                    && entitydragonbase.getAttackAnim(state.getPartialTick()) <= 0.0F;
                Animation waterIdleAnimation = waterIdle
                    ? this.getAnimation(animatable, "animation.dragon_" + this.waterIdleSpecies() + ".water_idle")
                    : null;
                waterIdle = waterIdleAnimation != null;
                this.rig.sample(
                    waterIdle ? waterIdleAnimation : this.getAnimation(animatable, "animation.dragon_shared.idle"),
                    d0,
                    this.targetPose
                );
                if (waterIdle) {
                    this.getBone("sleep_eyelids").ifPresent(bonex -> bonex.setHidden(false));
                }

                float f = !flag9
                        && i != 1
                        && i != 5
                        && (breathtimeline$frame != null || i != 2 && i != 3 && i != 4)
                        && !(entitydragonbase.getAttackAnim(state.getPartialTick()) > 0.0F)
                        && !HumanoidRoar.isRoaring(entitydragonbase)
                    ? (breathtimeline$frame == null ? 0.0F : breathtimeline$frame.weight())
                    : 1.0F;
                if (f > 0.0F) {
                    this.restoreLegacyTailX(animatable, d0, 0.0, 0.0F, f);
                }

                EntityModelData entitymodeldata1 = (EntityModelData)state.getData(DataTickets.ENTITY_MODEL_DATA);
                this.rig.add(this.targetPose, "Head", 0, entitymodeldata1.headPitch() * (float) (Math.PI / 180.0));
                this.rig.add(this.targetPose, "Head", 1, entitymodeldata1.netHeadYaw() * (float) (Math.PI / 180.0));
                j = waterIdle ? 33 : 31;
            }

            for (int k = 0; k < TAIL.length; k++) {
                this.rig.add(this.targetPose, TAIL[k], 1, locomotionmotion.tailRadians(k, state.getPartialTick()));
            }

            EntityModelData entitymodeldata3 = (EntityModelData)state.getData(DataTickets.ENTITY_MODEL_DATA);
            if (flag9) {
                Animation animation10 = this.getAnimation(animatable, this.assets.breathPrefix + switch (lightningbreathtimeline$frame.phase()) {
                    case START -> ".breath_start";
                    case LOOP -> ".breath_loop";
                    case END -> ".breath_end";
                    default -> throw new IllegalStateException("Inactive lightning pose");
                });
                this.rig.sampleOnce(animation10, lightningbreathtimeline$frame.tick(), this.walkPose);

                for (String s : WATER_BREATH_BONES) {
                    this.rig.mixBone(s, this.walkPose, 1.0F, this.targetPose);
                }

                this.rig
                    .add(
                        this.targetPose,
                        "Head",
                        0,
                        entitymodeldata3.headPitch() * (float) (Math.PI / 180.0)
                            + (flag10 ? (float) (Math.PI / 2) + entitydragonbase.getXRot() * (float) (Math.PI / 180.0) : 0.0F)
                    );
                this.rig.add(this.targetPose, "Head", 1, entitymodeldata3.netHeadYaw() * (float) (Math.PI / 180.0));
            } else if (this.assets.breathResource == null && breathtimeline$frame != null && (i == 0 || i == 2 || i == 3 || i == 4)) {
                this.breathLayers
                    .computeIfAbsent(entitydragonbase, ignored -> new BreathPoseLayer(this.rig))
                    .apply(
                        this.rig,
                        this.getAnimation(animatable, "animation.dragon_fire_ice.breath_first"),
                        this.getAnimation(animatable, "animation.dragon_shared.breath_followup"),
                        breathtimeline$frame,
                        (double)((float)entitydragonbase.level().getGameTime() + state.getPartialTick()),
                        this.targetPose
                    );
                this.rig
                    .add(
                        this.targetPose,
                        "Head",
                        0,
                        (
                                entitymodeldata3.headPitch() * (float) (Math.PI / 180.0)
                                    + (flag10 ? (float) (Math.PI / 2) + entitydragonbase.getXRot() * (float) (Math.PI / 180.0) : 0.0F)
                            )
                            * breathtimeline$frame.weight()
                    );
                this.rig.add(this.targetPose, "Head", 1, entitymodeldata3.netHeadYaw() * (float) (Math.PI / 180.0) * breathtimeline$frame.weight());
            }
        } else if (flag9) {
            Animation animation8 = this.getAnimation(animatable, this.assets.breathPrefix + switch (lightningbreathtimeline$frame.phase()) {
                case START -> ".breath_start";
                case LOOP -> ".breath_loop";
                case END -> ".breath_end";
                default -> throw new IllegalStateException("Inactive lightning pose");
            });
            this.rig.sampleOnce(animation8, lightningbreathtimeline$frame.tick(), this.targetPose);
            EntityModelData entitymodeldata4 = (EntityModelData)state.getData(DataTickets.ENTITY_MODEL_DATA);
            this.rig.add(this.targetPose, "Head", 0, entitymodeldata4.headPitch() * (float) (Math.PI / 180.0));
            this.rig.add(this.targetPose, "Head", 1, entitymodeldata4.netHeadYaw() * (float) (Math.PI / 180.0));
            if (!HumanoidMotion.groundMotion(entitydragonbase)
                && (!DragonForms.isSeated(entitydragonbase) || !entitydragonbase.onGround() || entitydragonbase.isInWaterOrBubble() || entitydragonbase.isInLava())) {
                this.vanillaPose.attackTime = 0.0F;
                this.vanillaPose.young = false;
                this.vanillaPose.swimAmount = entitydragonbase.getSwimAmount(state.getPartialTick());
                this.vanillaPose.prepareMobModel(entitydragonbase, state.getLimbSwing(), state.getLimbSwingAmount(), state.getPartialTick());
                this.vanillaPose
                    .setupAnim(
                        entitydragonbase,
                        state.getLimbSwing(),
                        state.getLimbSwingAmount(),
                        (float)d0,
                        -entitymodeldata4.netHeadYaw(),
                        -entitymodeldata4.headPitch()
                    );

                for (String s2 : BREATH_LEGS) {
                    this.rig.resetBone(this.targetPose, s2);
                }

                this.copyPose("Right Leg", this.vanillaPose.rightLeg, -1.9F, 12.0F, 0.0F);
                this.copyPose("Left Leg", this.vanillaPose.leftLeg, 1.9F, 12.0F, 0.0F);
            } else {
                this.rig
                    .sample(this.getAnimation(animatable, "animation.dragon_shared.walk"), locomotionmotion.walkTick(state.getPartialTick()), this.walkPose);
                float f2 = locomotionmotion.weight(state.getPartialTick());

                for (String s3 : BREATH_LEGS) {
                    this.rig.mixBone(s3, this.walkPose, f2 * f2 * (3.0F - 2.0F * f2), this.targetPose);
                }
            }

            j = 400 + lightningbreathtimeline$frame.revision();
        } else {
            boolean flag11 = !HumanoidMotion.groundMotion(entitydragonbase);
            boolean flag12 = this.assets.breathResource == null && breathtimeline$frame != null && (i == 0 || i == 2 || i == 3 || i == 4);
            boolean flag13 = i != 0 && !flag12 || entitydragonbase.getAttackAnim(state.getPartialTick()) > 0.0F || HumanoidRoar.isRoaring(entitydragonbase);
            flag7 = !flag11 && !flag13;
            EntityModelData entitymodeldata5 = (EntityModelData)state.getData(DataTickets.ENTITY_MODEL_DATA);
            if (flag11) {
                this.rig.reset(this.targetPose);
                j = 1;
            } else {
                float f3 = locomotionmotion.weight(state.getPartialTick());
                if (f3 == 0.0F) {
                    this.rig.sample(this.getAnimation(animatable, "animation.dragon_shared.idle"), d0, this.targetPose);
                } else if (f3 == 1.0F) {
                    this.rig
                        .sample(this.getAnimation(animatable, "animation.dragon_shared.walk"), locomotionmotion.walkTick(state.getPartialTick()), this.walkPose);
                    System.arraycopy(this.walkPose, 0, this.targetPose, 0, this.targetPose.length);
                } else {
                    this.rig.sample(this.getAnimation(animatable, "animation.dragon_shared.idle"), d0, this.targetPose);
                    this.rig
                        .sample(this.getAnimation(animatable, "animation.dragon_shared.walk"), locomotionmotion.walkTick(state.getPartialTick()), this.walkPose);
                    SharedAnimationPose.mix(this.targetPose, this.walkPose, f3 * f3 * (3.0F - 2.0F * f3), this.targetPose);
                }

                for (int l = 0; l < TAIL.length; l++) {
                    this.rig.add(this.targetPose, TAIL[l], 1, locomotionmotion.tailRadians(l, state.getPartialTick()));
                }

                float f5 = flag13 ? 1.0F : (flag12 ? breathtimeline$frame.weight() : 0.0F);
                if (f5 > 0.0F) {
                    this.restoreLegacyTailX(animatable, d0, locomotionmotion.walkTick(state.getPartialTick()), f3, f5);
                }

                j = flag13 ? 10 + i : 0;
            }

            if (flag11 || flag13) {
                float f7 = entitydragonbase.getAttackAnim(state.getPartialTick());
                this.vanillaPose.attackTime = i == 1 && DragonForms.state(entitydragonbase).dragonmaid$isFastClaw()
                    ? Math.min(1.0F, f7 * 1.2F)
                    : f7;
                this.vanillaPose.young = false;
                this.vanillaPose.swimAmount = entitydragonbase.getSwimAmount(state.getPartialTick());
                float f4 = flag11 ? state.getLimbSwing() : 0.0F;
                float f6 = flag11 ? state.getLimbSwingAmount() : 0.0F;
                this.vanillaPose.prepareMobModel(entitydragonbase, f4, f6, state.getPartialTick());
                this.vanillaPose.setupAnim(entitydragonbase, f4, f6, (float)d0, -entitymodeldata5.netHeadYaw(), -entitymodeldata5.headPitch());
                this.copyPose("Head", this.vanillaPose.head, 0.0F, 0.0F, 0.0F);
                this.copyPose("Body", this.vanillaPose.body, 0.0F, 0.0F, 0.0F);
                this.copyPose("Right Arm", this.vanillaPose.rightArm, -5.0F, 2.0F, 0.0F);
                this.copyPose("Left Arm", this.vanillaPose.leftArm, 5.0F, 2.0F, 0.0F);

                for (String s1 : LOWER_ARMS) {
                    this.rig.resetBone(this.targetPose, s1);
                }

                if (flag11) {
                    this.copyPose("Right Leg", this.vanillaPose.rightLeg, -1.9F, 12.0F, 0.0F);
                    this.copyPose("Left Leg", this.vanillaPose.leftLeg, 1.9F, 12.0F, 0.0F);
                }
            }

            if (flag12 && !flag13) {
                this.breathLayers
                    .computeIfAbsent(entitydragonbase, ignored -> new BreathPoseLayer(this.rig))
                    .apply(
                        this.rig,
                        this.getAnimation(animatable, "animation.dragon_fire_ice.breath_first"),
                        this.getAnimation(animatable, "animation.dragon_shared.breath_followup"),
                        breathtimeline$frame,
                        (double)((float)entitydragonbase.level().getGameTime() + state.getPartialTick()),
                        this.targetPose
                    );
            }

            if (!flag13 && !flag11) {
                this.rig.add(this.targetPose, "Head", 0, entitymodeldata5.headPitch() * (float) (Math.PI / 180.0));
                this.rig.add(this.targetPose, "Head", 1, entitymodeldata5.netHeadYaw() * (float) (Math.PI / 180.0));
            } else if (flag12 && !flag13 && breathtimeline$frame.weight() > 0.0F) {
                this.rig.add(this.targetPose, "Head", 0, entitymodeldata5.headPitch() * (float) (Math.PI / 180.0) * breathtimeline$frame.weight());
                this.rig.add(this.targetPose, "Head", 1, entitymodeldata5.netHeadYaw() * (float) (Math.PI / 180.0) * breathtimeline$frame.weight());
            }
        }

        if (DragonShield.isRaised(entitydragonbase)
            && i == 0
            && humanoidmeleettiming == null
            && tailsweeptimeline$frame == null
            && !flag
            && !sittimeline$frame.active()
            && !flag6
            && !entitydragonbase.isModelDead()
            && !flag9
            && (breathtimeline$frame == null || breathtimeline$frame.weight() <= 0.0F)) {
            this.rig.set(this.targetPose, "Left Arm", 0, 0.72F);
            this.rig.set(this.targetPose, "Left Arm", 1, -0.52F);
            this.rig.set(this.targetPose, "Left Arm", 2, 0.12F);
            this.rig.set(this.targetPose, "arm_lower_left", 0, 0.35F);
            this.rig.set(this.targetPose, "arm_lower_left", 1, 0.0F);
            this.rig.set(this.targetPose, "arm_lower_left", 2, 0.0F);
            this.rig.resetBone(this.targetPose, "hand_left");
            j = 2147483645;
        }

        this.transitions
            .computeIfAbsent(entitydragonbase, ignored -> new SharedAnimationPose.Transition(this.targetPose.length))
            .apply(this.targetPose, j, d0, locomotionmotion.generation(), this.assets.male, j == Integer.MAX_VALUE ? tailsweeptimeline$frame.age() : 0.0);
        if (humanoidmeleettiming == null && tailsweeptimeline$frame == null && !flag1 && !flag && !sittimeline$frame.active() && !entitydragonbase.isModelDead() && !flag6 && !flag9) {
            BreathTimeline.EyeFrame breathtimeline$eyeframe = entitydragonbase.isModelDead()
                ? BreathTimeline.EyeFrame.OPEN
                : (this.assets.breathResource == null ? ClientBreathAnimations.eye(entitydragonbase, state.getPartialTick()) : BreathTimeline.EyeFrame.OPEN);
            if (flag7
                && breathtimeline$eyeframe.weight() <= 0.0F
                && (this.assets.breathResource != null || breathtimeline$frame == null || breathtimeline$frame.weight() <= 0.0F)) {
                float f1 = locomotionmotion.weight(state.getPartialTick());
                this.rig.sampleBone(this.getAnimation(animatable, "animation.dragon_shared.idle"), d0, "sleep_eyelids", this.eyePose);
                if (f1 != 0.0F) {
                    this.rig.mixBone("sleep_eyelids", this.walkPose, f1, this.eyePose);
                }

                this.getBone("sleep_eyelids").ifPresent(bonex -> bonex.setHidden(false));
                this.rig.eyelids(this.eyePose, 1.0F, this.targetPose);
            } else {
                this.getBone("sleep_eyelids").ifPresent(bonex -> bonex.setHidden(breathtimeline$eyeframe.weight() <= 0.0F));
                this.rig
                    .sampleOnce(
                        this.getAnimation(
                            animatable, breathtimeline$eyeframe.first() ? "animation.dragon_fire_ice.breath_first" : "animation.dragon_shared.breath_followup"
                        ),
                        breathtimeline$eyeframe.tick(),
                        this.eyePose
                    );
                this.rig.eyelids(this.eyePose, breathtimeline$eyeframe.weight(), this.targetPose);
            }
        }

        this.rig.apply(this.targetPose);
    }

    private void restoreLegacyTailX(IceFemaleAnimatable animatable, double idleTime, double walkTime, float walkWeight, float weight) {
        Animation animation = this.getAnimation(animatable, "animation.dragon_shared.idle_legacy_tail");
        Animation animation1 = this.getAnimation(animatable, "animation.dragon_shared.walk_legacy_tail");
        if (animation != null && animation1 != null) {
            if (walkWeight >= 1.0F) {
                this.rig.sample(animation1, walkTime, this.legacyTailPose);
            } else {
                this.rig.sample(animation, idleTime, this.legacyTailPose);
                if (walkWeight > 0.0F) {
                    this.rig.sample(animation1, walkTime, this.legacyTailWalkPose);
                    SharedAnimationPose.mix(
                        this.legacyTailPose, this.legacyTailWalkPose, walkWeight * walkWeight * (3.0F - 2.0F * walkWeight), this.legacyTailPose
                    );
                }
            }

            float f = Mth.clamp(weight, 0.0F, 1.0F);

            for (String s : TAIL) {
                this.rig.blendChannel(s, 0, this.legacyTailPose, f, this.targetPose);
            }
        }
    }

    private void copyPose(String name, ModelPart part, float x, float y, float z) {
        this.rig.set(this.targetPose, name, 0, -part.xRot);
        this.rig.set(this.targetPose, name, 1, -part.yRot);
        this.rig.set(this.targetPose, name, 2, part.zRot);
        this.rig.set(this.targetPose, name, 3, part.x - x);
        this.rig.set(this.targetPose, name, 4, -(part.y - y));
        this.rig.set(this.targetPose, name, 5, part.z - z);
    }

    private boolean hasSitSupport(DragonBaseEntity dragon) {
        if (dragon.onGround()) {
            return true;
        } else if (!DragonForms.isSeated(dragon)) {
            return false;
        } else {
            IceFemaleModel.SitSupport icefemalemodel$sitsupport = this.sitSupports.get(dragon);
            if (icefemalemodel$sitsupport == null || icefemalemodel$sitsupport.tick() != dragon.tickCount) {
                AABB aabb = dragon.getBoundingBox();
                AABB aabb1 = new AABB(
                    aabb.minX + 0.02, aabb.minY - 0.08, aabb.minZ + 0.02, aabb.maxX - 0.02, aabb.minY + 0.01, aabb.maxZ - 0.02
                );
                icefemalemodel$sitsupport = new IceFemaleModel.SitSupport(dragon.tickCount, dragon.level().getBlockCollisions(dragon, aabb1).iterator().hasNext());
                this.sitSupports.put(dragon, icefemalemodel$sitsupport);
            }

            return icefemalemodel$sitsupport.supported();
        }
    }

    private static record SitSupport(int tick, boolean supported) {
    }
}
