package com.example.dragonmaid.client;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

final class IceFemaleAnimatable implements GeoReplacedEntity {
    private static final boolean LIBRARY_INTERPOLATES = ((ModContainer)ModList.get().getModContainerById("geckolib").orElseThrow())
            .getModInfo()
            .getVersion()
            .compareTo(new DefaultArtifactVersion("4.8"))
        >= 0;
    static final DataTicket<Boolean> SLEEP_POSE = new DataTicket("dragonmaid_sleep_pose", Boolean.class);
    static final DataTicket<Boolean> RESUME_SEATED = new DataTicket("dragonmaid_resume_seated", Boolean.class);
    private final RawAnimation enter;
    private final RawAnimation sleep;
    private final RawAnimation exit;
    private final RawAnimation enterFromSit;
    private final RawAnimation exitToSit;
    private AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Level level;
    private final FemaleDragonAssets assets;
    private final Map<DragonBaseEntity, IceFemaleAnimatable.SeatedPose> seatedPoses = new WeakHashMap<>();

    IceFemaleAnimatable(FemaleDragonAssets assets) {
        this.assets = assets;
        this.enterFromSit = RawAnimation.begin().thenPlay(assets.seatedSleepEnter()).thenLoop(assets.sleepPrefix + ".sleep");
        this.sleep = RawAnimation.begin().thenLoop(assets.sleepPrefix + ".sleep");
        this.exitToSit = RawAnimation.begin().thenPlay(assets.seatedSleepExit());
        this.enter = assets.kneelingSleep()
            ? RawAnimation.begin()
                .thenPlay(assets.sitPrefix + ".sit_enter")
                .thenPlay(assets.sleepPrefix + ".sleep_enter")
                .thenLoop(assets.sleepPrefix + ".sleep")
            : RawAnimation.begin().thenPlay(assets.sleepPrefix + ".sleep_enter").thenLoop(assets.sleepPrefix + ".sleep");
        this.exit = assets.kneelingSleep()
            ? RawAnimation.begin().thenPlay(assets.sleepPrefix + ".sleep_exit").thenPlay(assets.sitPrefix + ".sit_exit")
            : RawAnimation.begin().thenPlay(assets.sleepPrefix + ".sleep_exit");
    }

    void rememberSeatedPose(DragonBaseEntity dragon, boolean seated, int generation) {
        this.seatedPoses.put(dragon, new IceFemaleAnimatable.SeatedPose(seated, dragon.tickCount, generation));
    }

    private boolean wasSeated(DragonBaseEntity dragon, int generation) {
        IceFemaleAnimatable.SeatedPose icefemaleanimatable$seatedpose = this.seatedPoses.get(dragon);
        return icefemaleanimatable$seatedpose != null
            && icefemaleanimatable$seatedpose.seated
            && icefemaleanimatable$seatedpose.generation == generation
            && dragon.tickCount >= icefemaleanimatable$seatedpose.tick
            && dragon.tickCount - icefemaleanimatable$seatedpose.tick <= 5;
    }

    void prepare(DragonBaseEntity dragon) {
        if (this.level != dragon.level()) {
            this.clearWorldState();
            this.level = dragon.level();
        }
    }

    boolean clearWorldState() {
        boolean flag = this.level != null;
        this.level = null;
        this.cache = GeckoLibUtil.createInstanceCache(this);
        this.seatedPoses.clear();
        return flag;
    }

    public EntityType<?> getReplacingEntityType() {
        return this.assets.entityType();
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public double getTick(Object entity) {
        // 1.21.1: Minecraft#getFrameTime() is gone -> DeltaTracker#getGameTimeDeltaPartialTick
        return ReplacedEntityAnimationClock.tick((double)((Entity)entity).tickCount, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), LIBRARY_INTERPOLATES);
    }

    public void registerControllers(ControllerRegistrar controllers) {
        IceFemaleAnimatable.SleepSequence icefemaleanimatable$sleepsequence = new IceFemaleAnimatable.SleepSequence();
        controllers.add(new AnimationController[]{new AnimationController(this, "sleep", 0, icefemaleanimatable$sleepsequence::animate)});
    }

    private static record SeatedPose(boolean seated, int tick, int generation) {
    }

    private final class SleepSequence {
        private boolean initialized;
        private boolean wasSleeping;
        private boolean exiting;
        private RawAnimation sleepingAnimation = IceFemaleAnimatable.this.enter;
        private RawAnimation wakingAnimation = IceFemaleAnimatable.this.exit;
        private boolean wakeToSeated;
        private int lastTick;
        private int motionGeneration = Integer.MIN_VALUE;

        private SleepSequence() {
        }

        PlayState animate(AnimationState<IceFemaleAnimatable> state) {
            DragonBaseEntity entitydragonbase = (DragonBaseEntity)state.getData(DataTickets.ENTITY);
            LocomotionMotion locomotionmotion = HumanoidMotion.get(entitydragonbase);
            if (this.motionGeneration != locomotionmotion.generation()) {
                this.initialized = false;
                this.exiting = false;
                this.motionGeneration = locomotionmotion.generation();
            }

            AnimationController<IceFemaleAnimatable> animationcontroller = state.getController();
            state.setData(IceFemaleAnimatable.SLEEP_POSE, false);
            state.setData(IceFemaleAnimatable.RESUME_SEATED, false);
            if (entitydragonbase.isModelDead()) {
                this.initialized = false;
                this.exiting = false;
                return PlayState.STOP;
            } else {
                if (this.initialized && (entitydragonbase.tickCount < this.lastTick || entitydragonbase.tickCount - this.lastTick > 5)) {
                    this.initialized = false;
                }

                this.lastTick = entitydragonbase.tickCount;
                boolean flag = entitydragonbase.isSleeping();
                if (flag
                    || entitydragonbase.hurtTime <= 0
                        && !(locomotionmotion.weight(1.0F) > 0.1F)
                        && DragonForms.state(entitydragonbase).dragonmaid$getAction() == 0) {
                    if (!this.initialized) {
                        this.initialized = true;
                        this.wasSleeping = flag;
                        this.exiting = false;
                        this.sleepingAnimation = flag && entitydragonbase.sleepProgress >= 20.0F
                            ? IceFemaleAnimatable.this.sleep
                            : (
                                IceFemaleAnimatable.this.wasSeated(entitydragonbase, locomotionmotion.generation())
                                    ? IceFemaleAnimatable.this.enterFromSit
                                    : IceFemaleAnimatable.this.enter
                            );
                        animationcontroller.forceAnimationReset();
                    } else if (flag != this.wasSleeping) {
                        this.wasSleeping = flag;
                        this.exiting = !flag;
                        this.sleepingAnimation = IceFemaleAnimatable.this.wasSeated(entitydragonbase, locomotionmotion.generation())
                            ? IceFemaleAnimatable.this.enterFromSit
                            : IceFemaleAnimatable.this.enter;
                        animationcontroller.forceAnimationReset();
                        if (this.exiting) {
                            this.wakeToSeated = DragonForms.isSeated(entitydragonbase);
                            this.wakingAnimation = this.wakeToSeated ? IceFemaleAnimatable.this.exitToSit : IceFemaleAnimatable.this.exit;
                            state.setData(IceFemaleAnimatable.SLEEP_POSE, true);
                            return state.setAndContinue(this.wakingAnimation);
                        }
                    }

                    if (flag) {
                        state.setData(IceFemaleAnimatable.SLEEP_POSE, true);
                        return state.setAndContinue(this.sleepingAnimation);
                    } else if (this.exiting && this.wakeToSeated != DragonForms.isSeated(entitydragonbase)) {
                        this.exiting = false;
                        animationcontroller.forceAnimationReset();
                        state.setData(IceFemaleAnimatable.RESUME_SEATED, DragonForms.isSeated(entitydragonbase));
                        return PlayState.STOP;
                    } else if (this.exiting && !animationcontroller.hasAnimationFinished()) {
                        state.setData(IceFemaleAnimatable.SLEEP_POSE, true);
                        return state.setAndContinue(this.wakingAnimation);
                    } else {
                        if (this.exiting && this.wakeToSeated) {
                            state.setData(IceFemaleAnimatable.RESUME_SEATED, true);
                        }

                        this.exiting = false;
                        return PlayState.STOP;
                    }
                } else {
                    this.initialized = true;
                    this.wasSleeping = false;
                    this.exiting = false;
                    animationcontroller.forceAnimationReset();
                    return PlayState.STOP;
                }
            }
        }
    }
}
