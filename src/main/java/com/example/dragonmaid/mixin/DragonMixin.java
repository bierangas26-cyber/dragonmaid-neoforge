package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryInventory;
import com.example.dragonmaid.accessory.DragonStorageInventory;
import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.combat.CompanionDefense;
import com.example.dragonmaid.combat.DragonCombatAccessories;
import com.example.dragonmaid.inventory.DragonBagPickupGoal;
import com.example.dragonmaid.combat.GuardEscort;
import com.example.dragonmaid.combat.HumanoidCombat;
import com.example.dragonmaid.combat.HumanoidGoal;
import com.example.dragonmaid.combat.HumanoidIdleLook;
import com.example.dragonmaid.combat.HumanoidRoar;
import com.example.dragonmaid.combat.HumanoidTailSweep;
import com.example.dragonmaid.combat.HumanoidTargetGoal;
import com.example.dragonmaid.combat.NativeCompanionTargetGoal;
import com.example.dragonmaid.combat.TailSweepSync;
import com.example.dragonmaid.combat.WildDragonCombat;
import com.example.dragonmaid.combat.WildDragonTargetGoal;
import com.example.dragonmaid.compat.HumanoidForge;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.data.HumanoidWater;
import com.example.dragonmaid.data.InjuryRecovery;
import com.example.dragonmaid.data.LifeStateMigration;
import com.example.dragonmaid.item.ModItems;
import com.example.dragonmaid.learning.Knowledge;
import com.example.dragonmaid.movement.HumanoidAmphibiousNavigation;
import com.example.dragonmaid.movement.HumanoidGroundNavigation;
import com.example.dragonmaid.movement.HumanoidHazards;
import com.example.dragonmaid.movement.HumanoidMoveControl;
import com.example.dragonmaid.network.ExecutionConfirmation;
import com.example.dragonmaid.network.FeedingEffectMessage;
import com.example.dragonmaid.recall.RecallTracking;
import com.example.dragonmaid.compat.IafCompatConfig;
import com.iafenvoy.iceandfire.data.DragonType;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import com.iafenvoy.iceandfire.registry.IafItems;
import com.iafenvoy.iceandfire.item.DragonHornItem;
import com.iafenvoy.uranus.object.entity.pathfinding.raycoms.PathingStuckHandler;
import com.iafenvoy.uranus.object.entity.pathfinding.raycoms.AdvancedPathNavigate.MovementType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, value = {DragonBaseEntity.class})
public abstract class DragonMixin extends TamableAnimal implements DragonState, MiniatureForm.Holder, DragonBagPickupGoal.Holder {
    @Unique
    private static final EntityDataAccessor<Integer> DRAGONMAID_KNOWLEDGE = SynchedEntityData.defineId(DragonBaseEntity.class, EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<Boolean> DRAGONMAID_HUMANOID = SynchedEntityData.defineId(DragonBaseEntity.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Integer> DRAGONMAID_ACTION = SynchedEntityData.defineId(DragonBaseEntity.class, EntityDataSerializers.INT);
    @Unique
    private static final int DRAGONMAID_IDLE_LOOK_BIT = 256;
    @Unique
    private static final int DRAGONMAID_FROSTY_TAIL_BIT = 512;
    @Unique
    private static final int DRAGONMAID_FAST_CLAW_BIT = 1024;
    @Unique
    private static final int DRAGONMAID_ACTION_FLAGS = 1792;
    @Unique
    private static final EntityDataAccessor<Boolean> DRAGONMAID_EXECUTED = SynchedEntityData.defineId(DragonBaseEntity.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Boolean> DRAGONMAID_LIFE_TRACKED = SynchedEntityData.defineId(
        DragonBaseEntity.class, EntityDataSerializers.BOOLEAN
    );
    @Unique
    private boolean dragonmaid$lastForm;
    @Unique
    private long dragonmaid$awakeUntil;
    @Unique
    private long dragonmaid$lastOwnerDamage = Long.MIN_VALUE;
    @Unique
    private boolean dragonmaid$loading;
    @Unique
    private boolean dragonmaid$recoveryRest;
    @Unique
    private boolean dragonmaid$combatInterruptedRest;
    @Unique
    private int dragonmaid$recoveryTicks;
    @Unique
    private HumanoidGoal dragonmaid$combatGoal;
    @Unique
    private final DragonAccessoryInventory dragonmaid$accessories = new DragonAccessoryInventory((DragonBaseEntity)(Object)this);
    @Unique
    private final DragonStorageInventory dragonmaid$storage = new DragonStorageInventory();
    @Unique
    private int dragonmaid$knowledgeLevel;
    @Unique
    private CompoundTag dragonmaid$knowledgeProgress = new CompoundTag();
    @Unique
    private boolean dragonmaid$small;
    @Unique
    private boolean dragonmaid$miniWorn;
    @Unique
    private DragonBagPickupGoal dragonmaid$pickupGoal;
            @Inject(remap = false, method = {"spawnItemCrackParticles"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$feedingAtMouth(Item item, CallbackInfo ci) {
        if (this.dragonmaid$isHumanoid()) {
            if (!this.level().isClientSide) {
                FeedingEffectMessage.send((DragonBaseEntity)(Object)this, item);
            }

            ci.cancel();
        }
    }

    protected DragonMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Shadow(remap = false)
    protected abstract void switchNavigator(int var1);

    @Shadow(remap = false)
    protected abstract boolean isBreakable(BlockPos var1, BlockState var2, float var3, DragonBaseEntity var4);

    @Override
    public boolean dragonmaid$canBreakBlock(BlockPos pos, BlockState state) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        float f = IafCompatConfig.dragonGriefing != 1 && entitydragonbase.getDragonStage() > 3 ? 5.0F : 2.0F;
        return this.isBreakable(pos, state, f, entitydragonbase);
    }

    @Inject(remap = false, method = {"hurt"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$preventOwnerDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (DragonAccessories.blocksOwnerDamage((DragonBaseEntity)(Object)this, source)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(remap = false, method = {"remove"},
        at = {@At("HEAD")})
    private void dragonmaid$dropAccessoriesOnRemoval(RemovalReason reason, CallbackInfo ci) {
        if (reason == RemovalReason.KILLED) {
            DragonAccessories.dropAll((DragonBaseEntity)(Object)this);
        }
    }

    @Inject(remap = false, method = {"hurt"},
        at = {@At("RETURN")})
    private void dragonmaid$wakeOnDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (!this.level().isClientSide
            && DragonForms.unlocked(entitydragonbase)
            && (Boolean)cir.getReturnValue()
            && !(amount <= 0.0F)
            && !entitydragonbase.isModelDead()
            && entitydragonbase.isAlive()) {
            this.dragonmaid$wakeForCombat(source.getEntity() instanceof LivingEntity livingentity ? livingentity : null);
        }
    }

    @Override
    public void dragonmaid$wakeForOwnerDamage(long timestamp, LivingEntity attacker) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (!this.level().isClientSide
            && DragonForms.unlocked(entitydragonbase)
            && !entitydragonbase.isModelDead()
            && entitydragonbase.isAlive()
            && timestamp != this.dragonmaid$lastOwnerDamage) {
            this.dragonmaid$lastOwnerDamage = timestamp;
            this.dragonmaid$wakeForCombat(attacker);
        }
    }

    @Override
    public void dragonmaid$wakeForCompanionDamage(LivingEntity attacker) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (CompanionDefense.canRespond(entitydragonbase)) {
            this.dragonmaid$wakeForCombat(attacker);
        }
    }

    @Unique
    private void dragonmaid$wakeForCombat(LivingEntity attacker) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        this.dragonmaid$awakeUntil = this.level().getGameTime() + 100L;
        entitydragonbase.setInSittingPose(false);
        entitydragonbase.sleepProgress = 0.0F;
        entitydragonbase.prevAnimationProgresses[1] = 0.0F;
        entitydragonbase.lookingForRoostAIFlag = false;
        if (this.dragonmaid$recoveryRest) {
            entitydragonbase.setTarget(null);
        } else {
            if (attacker != null
                && HumanoidTargetGoal.isEnemy(entitydragonbase, attacker)
                && (!this.dragonmaid$isHumanoid() || GuardEscort.reachableTarget(entitydragonbase, attacker))) {
                if (entitydragonbase.getCommand() == 1) {
                    entitydragonbase.setCommand(0);
                    this.dragonmaid$combatInterruptedRest = true;
                }

                entitydragonbase.setTarget(attacker);
            }
        }
    }

    @Inject(remap = false, method = {"setInSittingPose"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$stayAwakeInCombat(boolean sleeping, CallbackInfo ci) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (sleeping && this.dragonmaid$recoveryRest) {
            ci.cancel();
        } else {
            if (sleeping
                && !this.level().isClientSide
                && (DragonForms.unlocked(entitydragonbase) || entitydragonbase.isTame() && this.level().getGameTime() < this.dragonmaid$awakeUntil)
                && !entitydragonbase.isModelDead()
                && (
                    this.level().getGameTime() < this.dragonmaid$awakeUntil
                        || entitydragonbase.getTarget() != null && HumanoidTargetGoal.isEnemy(entitydragonbase, entitydragonbase.getTarget())
                )) {
                ci.cancel();
            }
        }
    }

    @Inject(remap = false, method = {"setBreathingFire"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$resetNativeBreathTimer(boolean breathing, CallbackInfo ci) {
        if (!breathing || !this.dragonmaid$recoveryRest && !HumanoidLife.isInjured((DragonBaseEntity)(Object)this)) {
            if (!breathing && this.dragonmaid$isHumanoid()) {
                this.setRemainingFireTicks(0);
            }
        } else {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"roar"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$humanRoar(CallbackInfo ci) {
        if (this.dragonmaid$isHumanoid()) {
            HumanoidRoar.tryRoar((DragonBaseEntity)(Object)this);
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"defineSynchedData"},
        at = {@At("TAIL")})
    private void dragonmaid$define(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(DRAGONMAID_KNOWLEDGE, 0);
        builder.define(DRAGONMAID_HUMANOID, false);
        builder.define(DRAGONMAID_ACTION, 0);
        builder.define(DRAGONMAID_EXECUTED, false);
        builder.define(DRAGONMAID_LIFE_TRACKED, false);
    }

    @Override
    public int dragonmaid$getKnowledge() {
        return (Integer)this.entityData.get(DRAGONMAID_KNOWLEDGE);
    }

    @Override
    public CompoundTag dragonmaid$getKnowledgeProgress() {
        return this.dragonmaid$knowledgeProgress;
    }

    @Override
    public int dragonmaid$getKnowledgeLevel() {
        return Knowledge.normalizeLevel(this.dragonmaid$getKnowledge(), this.dragonmaid$knowledgeLevel);
    }

    @Override
    public void dragonmaid$setKnowledgeLevel(int value) {
        this.dragonmaid$knowledgeLevel = Knowledge.normalizeLevel(this.dragonmaid$getKnowledge(), value);
        if (!this.dragonmaid$loading && this.dragonmaid$accessories != null) {
            this.dragonmaid$accessories.invalidateEffects();
        }

        if (!this.dragonmaid$loading) {
            RecallTracking.changed((DragonBaseEntity)(Object)this);
        }
    }

    @Override
    public void dragonmaid$setKnowledge(int value) {
        this.entityData.set(DRAGONMAID_KNOWLEDGE, Knowledge.clamp(value));
        this.dragonmaid$knowledgeLevel = Knowledge.normalizeLevel(this.dragonmaid$getKnowledge(), this.dragonmaid$knowledgeLevel);
        if (!this.dragonmaid$loading && this.dragonmaid$accessories != null) {
            DragonAccessories.refresh((DragonBaseEntity)(Object)this);
        }

        if (!this.dragonmaid$loading) {
            RecallTracking.changed((DragonBaseEntity)(Object)this);
        }
    }

    @Override
    public boolean dragonmaid$isHumanoid() {
        return (Boolean)this.entityData.get(DRAGONMAID_HUMANOID);
    }

    @Override
    public boolean dragonmaid$isIdleLooking() {
        return ((Integer)this.entityData.get(DRAGONMAID_ACTION) & 256) != 0;
    }

    @Override
    public void dragonmaid$setIdleLooking(boolean looking) {
        int i = (Integer)this.entityData.get(DRAGONMAID_ACTION);
        this.entityData.set(DRAGONMAID_ACTION, looking ? i | 256 : i & -257);
    }

    @Override
    public void dragonmaid$setHumanoid(boolean value) {
        if (this.dragonmaid$isHumanoid() != value) {
            this.entityData.set(DRAGONMAID_HUMANOID, value);
            this.dragonmaid$refreshForm();
            if (!this.dragonmaid$loading) {
                RecallTracking.changed((DragonBaseEntity)(Object)this);
            }
        }
    }

    @Override
    public boolean dragonmaid$hasFrostyTail() {
        return ((Integer)this.entityData.get(DRAGONMAID_ACTION) & DRAGONMAID_FROSTY_TAIL_BIT) != 0;
    }

    @Override
    public void dragonmaid$setFrostyTail(boolean equipped) {
        int state = (Integer)this.entityData.get(DRAGONMAID_ACTION);
        this.entityData.set(DRAGONMAID_ACTION, equipped ? state | DRAGONMAID_FROSTY_TAIL_BIT : state & ~DRAGONMAID_FROSTY_TAIL_BIT);
    }

    @Override
    public boolean dragonmaid$isFastClaw() {
        return ((Integer)this.entityData.get(DRAGONMAID_ACTION) & DRAGONMAID_FAST_CLAW_BIT) != 0;
    }

    @Override
    public int dragonmaid$getMeleeMotion() {
        return (Integer)this.entityData.get(DRAGONMAID_ACTION) >>> 11;
    }

    @Override
    public void dragonmaid$setMeleeMotion(int motion) {
        int old = (Integer)this.entityData.get(DRAGONMAID_ACTION);
        this.entityData.set(DRAGONMAID_ACTION, old & DRAGONMAID_ACTION_FLAGS | 1 | motion << 11);
    }

    @Override
    public boolean dragonmaid$small() {
        return this.dragonmaid$small;
    }

    @Override
    public boolean dragonmaid$miniWorn() {
        return this.dragonmaid$miniWorn;
    }

    @Override
    public boolean dragonmaid$readingSize() {
        return this.dragonmaid$loading;
    }

    @Override
    public void dragonmaid$sizeState(boolean small, boolean worn) {
        this.dragonmaid$small = small;
        this.dragonmaid$miniWorn = worn;
    }

    @Override
    public DragonBagPickupGoal dragonmaid$pickupGoal() {
        return this.dragonmaid$pickupGoal;
    }

    @Override
    public int dragonmaid$getAction() {
        return (Integer)this.entityData.get(DRAGONMAID_ACTION) & 0xFF;
    }

    @Override
    public void dragonmaid$setAction(int value) {
        int flags = (Integer)this.entityData.get(DRAGONMAID_ACTION) & DRAGONMAID_ACTION_FLAGS;
        if (!this.level().isClientSide) {
            if (value == 1 && DragonCombatAccessories.wild((DragonBaseEntity)(Object)this)) {
                flags |= DRAGONMAID_FAST_CLAW_BIT;
            } else {
                flags &= ~DRAGONMAID_FAST_CLAW_BIT;
            }
        }

        int motion = value == 1 ? this.dragonmaid$getMeleeMotion() << 11 : 0;
        this.entityData.set(DRAGONMAID_ACTION, motion | flags | value & 0xFF);
    }

    @Override
    public boolean dragonmaid$isExecuted() {
        return (Boolean)this.entityData.get(DRAGONMAID_EXECUTED);
    }

    @Override
    public void dragonmaid$setExecuted(boolean value) {
        boolean flag = value && !this.dragonmaid$isExecuted();
        if (flag && !this.dragonmaid$loading) {
            RecallTracking.executed((DragonBaseEntity)(Object)this);
        }

        this.entityData.set(DRAGONMAID_EXECUTED, value);
        if (flag && !this.dragonmaid$loading) {
            DragonAccessories.dropAll((DragonBaseEntity)(Object)this);
        }
    }

    @Override
    public DragonAccessoryInventory dragonmaid$getAccessories() {
        return this.dragonmaid$accessories;
    }

    @Override
    public DragonStorageInventory dragonmaid$getStorage() {
        return this.dragonmaid$storage;
    }

    @Override
    public boolean dragonmaid$isLifeTracked() {
        return (Boolean)this.entityData.get(DRAGONMAID_LIFE_TRACKED);
    }

    @Override
    public void dragonmaid$setLifeTracked(boolean value) {
        this.entityData.set(DRAGONMAID_LIFE_TRACKED, value);
    }

    @Override
    public boolean dragonmaid$isRecoveryRest() {
        return this.dragonmaid$recoveryRest;
    }

    @Override
    public boolean dragonmaid$isCombatInterruptedRest() {
        return this.dragonmaid$combatInterruptedRest;
    }

    @Override
    public void dragonmaid$setCombatInterruptedRest(boolean interrupted) {
        this.dragonmaid$combatInterruptedRest = interrupted;
    }

    @Override
    public int dragonmaid$getNativeBreathTicks() {
        return this.getRemainingFireTicks();
    }

    @Override
    public void dragonmaid$setNativeBreathTicks(int ticks) {
        this.setRemainingFireTicks(ticks);
    }

    @Override
    public int dragonmaid$getRecoveryTicks() {
        return this.dragonmaid$recoveryTicks;
    }

    @Override
    public void dragonmaid$setRecoveryTicks(int ticks) {
        this.dragonmaid$recoveryTicks = Math.max(0, ticks) % 24000;
    }

    @ModifyVariable(remap = false, method = {"setTarget"},
        at = @At("HEAD"),
        argsOnly = true)
    private LivingEntity dragonmaid$restWithoutTarget(LivingEntity target) {
        return !this.dragonmaid$recoveryRest && !HumanoidLife.isInjured((DragonBaseEntity)(Object)this) ? target : null;
    }

    public Component getDisplayName() {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        Component component = super.getDisplayName();
        return (Component)(this.dragonmaid$isLifeTracked() && entitydragonbase.isModelDead()
            ? component.copy().append(Component.translatable(this.dragonmaid$isExecuted() ? "label.dragonmaid.fully_dead" : "label.dragonmaid.injured"))
            : component);
    }

    @Override
    public void dragonmaid$refreshForm() {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        LivingEntity livingentity = !this.level().isClientSide && this.dragonmaid$combatInterruptedRest ? entitydragonbase.getTarget() : null;
        if (!this.level().isClientSide) {
            TailSweepSync.cancel(entitydragonbase);
        }

        this.dragonmaid$lastForm = this.dragonmaid$isHumanoid();
        // 1.21.1 / IAF CE 2.0: updateParts() 不再做 null 检查（headPart.copyPosition 直接 NPE）。
        // 客户端不管理 part 实体（part 的增删由服务端 removeParts/updateScale 后随实体同步下发），
        // 故这里只在服务端处理；且服务端重建必须用 updateScale()（对 null part 会创建），不能用 updateParts()。
        if (!this.level().isClientSide) {
            entitydragonbase.removeParts();
            if (!this.dragonmaid$isHumanoid()) {
                entitydragonbase.updateScale(entitydragonbase.getRenderSize() / 3.0F);
            }
        }

        entitydragonbase.setFlying(false);
        entitydragonbase.setHovering(false);
        entitydragonbase.setTackling(false);
        entitydragonbase.setBreathingFire(false);
        HumanoidCombat.stopBreath(entitydragonbase);
        entitydragonbase.burningTarget = null;
        entitydragonbase.setTarget(null);
        this.dragonmaid$setAction(0);
        this.navigation.stop();
        if (!this.level().isClientSide) {
            this.switchNavigator(0);
        }

        this.refreshDimensions();

        if (livingentity != null && HumanoidTargetGoal.isEnemy(entitydragonbase, livingentity)) {
            entitydragonbase.setTarget(livingentity);
        }
    }

    @Inject(remap = false, method = {"addAdditionalSaveData"},
        at = {@At("TAIL")})
    private void dragonmaid$save(CompoundTag tag, CallbackInfo ci) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putInt("Version", 6);
        compoundtag.putInt("Knowledge", this.dragonmaid$getKnowledge());
        compoundtag.putInt("KnowledgeLevel", this.dragonmaid$getKnowledgeLevel());
        compoundtag.put("KnowledgeProgress", this.dragonmaid$knowledgeProgress.copy());
        compoundtag.putBoolean("Miniature", this.dragonmaid$small);
        compoundtag.putBoolean("MiniWorn", this.dragonmaid$miniWorn);
        compoundtag.putString("PickupMode", this.dragonmaid$storage.pickupMode().name());
        compoundtag.putBoolean("Humanoid", this.dragonmaid$isHumanoid());
        compoundtag.putBoolean("Executed", this.dragonmaid$isExecuted());
        compoundtag.putBoolean("LifeTracked", this.dragonmaid$isLifeTracked());
        compoundtag.putBoolean("RecoveryRest", this.dragonmaid$recoveryRest);
        compoundtag.putBoolean("CombatInterruptedRest", this.dragonmaid$combatInterruptedRest);
        compoundtag.putInt("RecoveryTicks", this.dragonmaid$recoveryTicks);
        compoundtag.put("Accessories", this.dragonmaid$accessories.save());
        compoundtag.put("Storage", this.dragonmaid$storage.save());
        if (this.dragonmaid$combatGoal != null) {
            compoundtag.put("Combat", this.dragonmaid$combatGoal.saveCombatState());
        }

        tag.putBoolean("NoAI", ((DragonBaseEntity)(Object)this).isNoAi());
        tag.put("DragonMaid", compoundtag);
    }

    @Inject(remap = false, method = {"readAdditionalSaveData"},
        at = {@At("HEAD")})
    private void dragonmaid$beginLoad(CompoundTag tag, CallbackInfo ci) {
        this.dragonmaid$loading = true;
    }

    @Inject(remap = false, method = {"readAdditionalSaveData"},
        at = {@At("TAIL")})
    private void dragonmaid$load(CompoundTag tag, CallbackInfo ci) {
        CompoundTag compoundtag = tag.getCompound("DragonMaid");
        this.dragonmaid$accessories.load(compoundtag.getList("Accessories", 10));
        this.dragonmaid$storage.load(compoundtag.getList("Storage", 10));
        this.dragonmaid$setKnowledge(compoundtag.getInt("Knowledge"));
        this.dragonmaid$setKnowledgeLevel(
            compoundtag.contains("KnowledgeLevel", 99) ? compoundtag.getInt("KnowledgeLevel") : Knowledge.level(compoundtag.getInt("Knowledge"))
        );
        this.dragonmaid$knowledgeProgress = compoundtag.getCompound("KnowledgeProgress").copy();
        this.dragonmaid$small = compoundtag.getBoolean("Miniature");
        this.dragonmaid$miniWorn = compoundtag.getBoolean("MiniWorn");
        this.dragonmaid$storage.loadPickupMode(compoundtag.getString("PickupMode"));
        this.dragonmaid$setHumanoid(compoundtag.getBoolean("Humanoid"));
        this.dragonmaid$setExecuted(compoundtag.getBoolean("Executed"));
        boolean flag = LifeStateMigration.trackedOnLoad(compoundtag, ((DragonBaseEntity)(Object)this).getDeathStage());
        this.dragonmaid$setLifeTracked(flag);
        this.dragonmaid$recoveryRest = compoundtag.getBoolean("RecoveryRest") && ((DragonBaseEntity)(Object)this).getCommand() == 1;
        this.dragonmaid$combatInterruptedRest = compoundtag.getBoolean("CombatInterruptedRest") && ((DragonBaseEntity)(Object)this).getCommand() == 0;
        this.dragonmaid$setRecoveryTicks(compoundtag.getInt("RecoveryTicks"));
        if (this.dragonmaid$combatGoal != null) {
            this.dragonmaid$combatGoal.loadCombatState(compoundtag);
        }

        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (LifeStateMigration.restoreLegacyInjuryAi(tag, entitydragonbase.isTame(), entitydragonbase.getOwnerUUID() != null, entitydragonbase.getDeathStage())) {
            this.setNoAi(false);
        }

        this.dragonmaid$loading = false;
        this.dragonmaid$accessories.invalidateEffects();
    }

    @Inject(remap = false, method = {"setModelDead"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$trackNewInjury(boolean dead, CallbackInfo ci) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (!dead && !this.dragonmaid$loading && HumanoidLife.isInjured(entitydragonbase) && !InjuryRecovery.fullyHealed(this.getHealth(), this.getMaxHealth())) {
            ci.cancel();
        } else {
            if (!this.level().isClientSide && !this.dragonmaid$loading && dead && !entitydragonbase.isModelDead() && entitydragonbase.isTame()) {
                this.dragonmaid$setLifeTracked(true);
                this.dragonmaid$setExecuted(false);
                this.dragonmaid$recoveryRest = false;
                this.dragonmaid$combatInterruptedRest = false;
                this.dragonmaid$recoveryTicks = 0;
            } else if (!this.level().isClientSide
                && !this.dragonmaid$loading
                && !dead
                && !entitydragonbase.isRemoved()
                && entitydragonbase.getDeathStage() == 0
                && HumanoidLife.isInjured(entitydragonbase)) {
                entitydragonbase.setCommand(1);
                this.dragonmaid$recoveryRest = true;
                this.dragonmaid$awakeUntil = this.level().getGameTime() + 100L;
                entitydragonbase.setInSittingPose(false);
                entitydragonbase.sleepProgress = 0.0F;
                entitydragonbase.prevAnimationProgresses[1] = 0.0F;
                if (this.dragonmaid$isHumanoid()) {
                    entitydragonbase.modelDeadProgress = 0.0F;
                }

                this.dead = false;
                this.deathTime = 0;
                this.dragonmaid$recoveryTicks = 0;
                entitydragonbase.hurtTime = 0;
                entitydragonbase.lookingForRoostAIFlag = false;
                entitydragonbase.setFlying(false);
                entitydragonbase.setHovering(false);
                entitydragonbase.setTackling(false);
                entitydragonbase.setTarget(null);
                entitydragonbase.setLastHurtByMob(null);
                entitydragonbase.setBreathingFire(false);
                HumanoidCombat.stopBreath(entitydragonbase);
                entitydragonbase.burningTarget = null;
                this.dragonmaid$setAction(0);
                this.navigation.stop();
                this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
            }
        }
    }

    @Inject(remap = false, method = {"setCommand"},
        at = {@At("HEAD")})
    private void dragonmaid$releaseRecoveryRest(int command, CallbackInfo ci) {
        if (command != 1) {
            this.dragonmaid$recoveryRest = false;
        }

        if (!this.dragonmaid$loading) {
            this.dragonmaid$combatInterruptedRest = false;
        }
    }

    @Inject(remap = false, method = {"setAgeInTicks"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$freezeAgeTicks(int age, CallbackInfo ci) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (!this.dragonmaid$loading && HumanoidLife.isInjured(entitydragonbase) && age > entitydragonbase.getAgeInTicks()) {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"setAgeInDays"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$freezeAgeDays(int days, CallbackInfo ci) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (!this.dragonmaid$loading && HumanoidLife.isInjured(entitydragonbase) && days > entitydragonbase.getAgeInDays()) {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"refreshDirtyAttributes"},
        at = {@At("TAIL")})
    private void dragonmaid$accessoryArmorAfterNativeArmor(CallbackInfo ci) {
        if (!this.dragonmaid$loading && this.dragonmaid$accessories != null) {
            DragonAccessories.refresh((DragonBaseEntity)(Object)this);
        }
    }

    @Inject(remap = false, method = {"setAgeInDays"},
        at = {@At("RETURN")})
    private void dragonmaid$accessoryArmorAfterGrowth(int days, CallbackInfo ci) {
        if (!this.dragonmaid$loading && this.dragonmaid$accessories != null) {
            DragonAccessories.refresh((DragonBaseEntity)(Object)this);
        }
    }

    @Inject(remap = false, method = {"registerGoals"},
        at = {@At("TAIL")})
    private void dragonmaid$goals(CallbackInfo ci) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        this.dragonmaid$combatGoal = new HumanoidGoal(entitydragonbase);
        this.goalSelector.addGoal(-10, this.dragonmaid$combatGoal);
        this.targetSelector.addGoal(-10, new HumanoidTargetGoal(entitydragonbase));
        this.targetSelector.addGoal(4, new NativeCompanionTargetGoal(entitydragonbase));
        this.targetSelector.addGoal(4, new WildDragonTargetGoal(entitydragonbase));
        this.dragonmaid$pickupGoal = new DragonBagPickupGoal(entitydragonbase);
        this.goalSelector.addGoal(9, this.dragonmaid$pickupGoal);
    }

    @Inject(remap = false, method = {"tick"},
        at = {@At("HEAD")})
    private void dragonmaid$syncForm(CallbackInfo ci) {
        if (this.dragonmaid$lastForm != this.dragonmaid$isHumanoid()) {
            this.dragonmaid$refreshForm();
        }

        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (!this.level().isClientSide && this.dragonmaid$isIdleLooking() && !HumanoidIdleLook.eligible(entitydragonbase)) {
            this.dragonmaid$setIdleLooking(false);
        }

        if (!this.level().isClientSide
            && this.dragonmaid$combatInterruptedRest
            && !entitydragonbase.isRemoved()
            && !entitydragonbase.isNoAi()
            && !entitydragonbase.isPassenger()
            && entitydragonbase.getControllingPassenger() == null
            && !GorgonEntity.isStoneMob(entitydragonbase)) {
            LivingEntity livingentity = entitydragonbase.getTarget();
            if (entitydragonbase.getCommand() != 0) {
                this.dragonmaid$combatInterruptedRest = false;
            } else if (livingentity == null || !HumanoidTargetGoal.isEnemy(entitydragonbase, livingentity)) {
                boolean flag = entitydragonbase.getPassengers().stream().anyMatch(passenger -> {
                    if (passenger instanceof LivingEntity livingentity2 && HumanoidTargetGoal.isEnemy(entitydragonbase, livingentity2)) {
                        return true;
                    }

                    return false;
                });
                if (!flag) {
                    LivingEntity livingentity1 = this.dragonmaid$isHumanoid() ? null : CompanionDefense.recentAttacker(entitydragonbase);
                    boolean flag1 = CompanionDefense.canAssist(entitydragonbase, livingentity1);
                    boolean flag2 = flag1 && entitydragonbase.getAnimation() == DragonBaseEntity.ANIMATION_SHAKEPREY;
                    if (!flag2
                        && flag1
                        && WildDragonCombat.canAcquireTargets(entitydragonbase)
                        && entitydragonbase.canMove()
                        && entitydragonbase.hasLineOfSight(livingentity1)) {
                        entitydragonbase.setTarget(livingentity1);
                    } else if (!flag2) {
                        this.dragonmaid$combatInterruptedRest = false;
                        entitydragonbase.setTarget(null);
                        entitydragonbase.getNavigation().stop();
                        entitydragonbase.setCommand(1);
                    }
                }
            }
        }

        if (DragonForms.unlocked(entitydragonbase) && entitydragonbase.hurtTime > 0 && !entitydragonbase.isSleeping() && !entitydragonbase.isModelDead()) {
            entitydragonbase.sleepProgress = 0.0F;
            entitydragonbase.prevAnimationProgresses[1] = 0.0F;
        }
    }

    @Inject(remap = false, method = {"tick"},
        at = {@At("TAIL")})
    private void dragonmaid$clearStoppedBreath(CallbackInfo ci) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        DragonAccessories.serverTick(entitydragonbase);
        if (!this.level().isClientSide && TailSweepSync.isBusy(entitydragonbase) && !HumanoidTailSweep.canAct(entitydragonbase)) {
            TailSweepSync.cancel(entitydragonbase);
            if (this.dragonmaid$getAction() == 6) {
                this.dragonmaid$setAction(0);
            }
        }

        if (!this.level().isClientSide
            && this.dragonmaid$isHumanoid()
            && !HumanoidForge.isWorking(entitydragonbase)
            && (
                !entitydragonbase.isAlive()
                    || entitydragonbase.isModelDead()
                    || entitydragonbase.isNoAi()
                    || entitydragonbase.isSleeping()
                    || entitydragonbase.getCommand() == 1
                    || entitydragonbase.isPassenger()
                    || entitydragonbase.getTarget() == null
                    || !HumanoidTargetGoal.isEnemy(entitydragonbase, entitydragonbase.getTarget())
            )) {
            HumanoidCombat.stopBreath(entitydragonbase);
            if (!TailSweepSync.isBusy(entitydragonbase)) {
                this.dragonmaid$setAction(0);
            }
        }
    }

    @Inject(remap = false, method = {"updateBurnTarget"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$safeForge(CallbackInfo ci) {
        if (this.dragonmaid$isHumanoid() || this.dragonmaid$recoveryRest || HumanoidLife.isInjured((DragonBaseEntity)(Object)this)) {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"getDimensions"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (this.dragonmaid$isHumanoid()) {
            cir.setReturnValue(DragonForms.humanoidDimensions((DragonBaseEntity)(Object)this, pose));
        }
    }

    @Inject(remap = false, method = {"maxUpStep"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$step(CallbackInfoReturnable<Float> cir) {
        if (this.dragonmaid$isHumanoid()) {
            cir.setReturnValue(0.6F);
        }
    }

    @Inject(remap = false, method = {"removeParts"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$parts(CallbackInfo ci) {
        if (this.dragonmaid$isHumanoid()) {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"updateParts"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$noParts(CallbackInfo ci) {
        if (this.dragonmaid$isHumanoid()) {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"breakBlocks"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$noGrief(boolean force, CallbackInfo ci) {
        if (this.dragonmaid$isHumanoid()) {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"setFlying", "setHovering"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$noFlight(boolean value, CallbackInfo ci) {
        if (value && this.dragonmaid$isHumanoid()) {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"isAllowedToTriggerFlight"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$grounded(CallbackInfoReturnable<Boolean> cir) {
        if (this.dragonmaid$isHumanoid()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(remap = false, method = {"canAddPassenger"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$noRider(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
        if (this.dragonmaid$isHumanoid()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(remap = false, method = {"mobInteract"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$humanCommand(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (HumanoidLife.isInjured(entitydragonbase) && hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty() && !player.getOffhandItem().isEmpty()) {
            cir.setReturnValue(InteractionResult.PASS);
        } else if (this.dragonmaid$injuryInteraction(player, hand)) {
            cir.setReturnValue(InteractionResult.sidedSuccess(this.level().isClientSide));
        } else {
            ItemStack itemstack = player.getItemInHand(hand);
            if (itemstack.is((Item)ModItems.REINFORCED_DRAGON_HORN.get())) {
                cir.setReturnValue(itemstack.interactLivingEntity(player, entitydragonbase, hand));
            } else {
                if (this.dragonmaid$isHumanoid()
                    && entitydragonbase.isAlive()
                    && !entitydragonbase.isModelDead()
                    && entitydragonbase.isOwnedBy(player)
                    && !player.isSpectator()
                    && !player.isShiftKeyDown()
                    && player.getMainHandItem().isEmpty()
                    && player.getItemInHand(hand).isEmpty()) {
                    if (!player.getOffhandItem().isEmpty()) {
                        cir.setReturnValue(InteractionResult.PASS);
                        return;
                    }

                    if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide) {
                        int i = this.dragonmaid$combatInterruptedRest ? 1 : entitydragonbase.getCommand();
                        int j = Math.floorMod(i + 1, 3);
                        entitydragonbase.setCommand(j);

                        String s = switch (j) {
                            case 1 -> "rest";
                            case 2 -> "escort";
                            default -> "wander";
                        };
                        player.displayClientMessage(Component.translatable("message.dragonmaid.command." + s), true);
                    }

                    cir.setReturnValue(InteractionResult.sidedSuccess(this.level().isClientSide));
                }
            }
        }
    }

    @Redirect(remap = false, method = {"mobInteract"},
        at = @At(
            value = "INVOKE",
            ordinal = 0,
            target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;getCommand()I"
        ))
    private int dragonmaid$logicalCommandForStaff(DragonBaseEntity dragon) {
        return this.dragonmaid$combatInterruptedRest ? 1 : dragon.getCommand();
    }

    @Inject(remap = false, method = {"interactAt"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$protectInjured(Player player, Vec3 hit, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (HumanoidLife.isInjured(entitydragonbase)) {
            cir.setReturnValue(entitydragonbase.mobInteract(player, hand));
        }
    }

    @Redirect(remap = false, method = {"mobInteract"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/data/DragonType;getCrystalItem()Lnet/minecraft/world/item/Item;"
        ))
    private Item dragonmaid$universalCrystal(DragonType type, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getMainHandItem().isEmpty() ? player.getItemInHand(hand) : player.getMainHandItem();
        return !player.isSpectator() && itemstack.is((Item)ModItems.TRINITY_DRAGON_GEM.get()) ? itemstack.getItem() : type.getCrystalItem();
    }

    @Redirect(remap = false, method = {"mobInteract"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;isModelDead()Z"
        ))
    private boolean dragonmaid$allowInjuryFeeding(DragonBaseEntity dragon, Player player, InteractionHand hand) {
        return dragon.isModelDead() && (!HumanoidLife.isInjured(dragon) || !InjuryRecovery.nativeInteraction(dragon, player, hand));
    }

    @Redirect(remap = false, method = {"mobInteract"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;isFood(Lnet/minecraft/world/item/ItemStack;)Z"
        ))
    private boolean dragonmaid$noInjuredBreeding(DragonBaseEntity dragon, ItemStack stack) {
        return !HumanoidLife.isInjured(dragon) && dragon.isFood(stack);
    }

    @Inject(remap = false, method = {"mobInteract"},
        at = {@At("RETURN")})
    private void dragonmaid$finishFeeding(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        InjuryRecovery.finish((DragonBaseEntity)(Object)this);
    }

    @Redirect(remap = false, method = {"mobInteract"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;heal(F)V"
        ))
    private void dragonmaid$mealHealingFloor(DragonBaseEntity dragon, float amount, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getMainHandItem().isEmpty() ? player.getItemInHand(hand) : player.getMainHandItem();
        dragon.heal(itemstack.is((Item)IafItems.DRAGON_MEAL.get()) ? InjuryRecovery.mealHealing(amount) : amount);
    }

    @Unique
    private boolean dragonmaid$injuryInteraction(Player player, InteractionHand hand) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (!HumanoidLife.isInjured(entitydragonbase)) {
            return false;
        } else {
            ItemStack itemstack = player.getMainHandItem().isEmpty() ? player.getItemInHand(hand) : player.getMainHandItem();
            if (itemstack.is((Item)IafItems.SICKLY_DRAGON_MEAL.get())) {
                if (!this.level().isClientSide && entitydragonbase.isOwnedBy(player) && !player.isSpectator()) {
                    player.displayClientMessage(Component.translatable("message.dragonmaid.injury_no_sickly_meal"), true);
                }

                return true;
            } else if (InjuryRecovery.nativeInteraction(entitydragonbase, player, hand)) {
                return false;
            } else if (entitydragonbase.isOwnedBy(player)
                && !player.isSpectator()
                && player.isShiftKeyDown()
                && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().isEmpty()) {
                if (hand == InteractionHand.MAIN_HAND) {
                    player.openMenu(entitydragonbase);
                }

                return true;
            } else {
                if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND && !player.isSpectator()) {
                    if (!entitydragonbase.isOwnedBy(player)) {
                        player.displayClientMessage(Component.translatable("message.dragonmaid.execution_owner_only"), true);
                    } else if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
                        player.displayClientMessage(Component.translatable("message.dragonmaid.injury_alive"), true);
                    } else if (player.mayBuild() && player.getMainHandItem().is(HumanoidLife.EXECUTION_TOOLS) && player instanceof ServerPlayer serverplayer) {
                        if (entitydragonbase.getHealth() > 0.0F) {
                            player.displayClientMessage(Component.translatable("message.dragonmaid.execution_requires_zero_health"), true);
                        } else {
                            ExecutionConfirmation.request(serverplayer, entitydragonbase);
                        }
                    } else if (player.getItemInHand(hand).getItem() instanceof DragonHornItem) {
                        player.displayClientMessage(Component.translatable("message.dragonmaid.injury_no_horn"), true);
                    } else {
                        player.displayClientMessage(Component.translatable("message.dragonmaid.requires_execution"), true);
                    }
                }

                return true;
            }
        }
    }

    @Inject(remap = false, method = {"createNavigator(Lnet/minecraft/world/level/Level;Lcom/iafenvoy/uranus/object/entity/pathfinding/raycoms/AdvancedPathNavigate$MovementType;Lcom/iafenvoy/uranus/object/entity/pathfinding/raycoms/PathingStuckHandler;F)Lnet/minecraft/world/entity/ai/navigation/PathNavigation;"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$navigation(
        Level world, MovementType type, PathingStuckHandler handler, float width, CallbackInfoReturnable<PathNavigation> cir
    ) {
        if (this.dragonmaid$isHumanoid()) {
            if (HumanoidWater.canDive((DragonBaseEntity)(Object)this)) {
                this.navigation = new HumanoidAmphibiousNavigation(this, world);
                cir.setReturnValue(this.navigation);
                return;
            }

            GroundPathNavigation groundpathnavigation = new HumanoidGroundNavigation(this, world);
            groundpathnavigation.setCanFloat(true);
            this.navigation = groundpathnavigation;
            cir.setReturnValue(groundpathnavigation);
        }
    }

    @Inject(remap = false, method = {"switchNavigator"},
        at = {@At("TAIL")})
    private void dragonmaid$humanMoveControl(int type, CallbackInfo ci) {
        if (this.dragonmaid$isHumanoid()) {
            this.moveControl = new HumanoidMoveControl((DragonBaseEntity)(Object)this);
        }
    }

    public float getPathfindingMalus(PathType type) {
        float f = super.getPathfindingMalus(type);
        return this.dragonmaid$isHumanoid() ? HumanoidHazards.malus(this, type, f) : f;
    }

    @Inject(remap = false, method = {"travel"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$walk(Vec3 movement, CallbackInfo ci) {
        if (this.dragonmaid$isHumanoid()) {
            DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
            if (!HumanoidWater.travel(entitydragonbase, movement)) {
                super.travel(entitydragonbase.canMove() ? movement : Vec3.ZERO);
            }

            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"isInWater"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$water(CallbackInfoReturnable<Boolean> cir) {
        if (this.dragonmaid$isHumanoid()) {
            cir.setReturnValue(super.isInWater());
        }
    }

    @Inject(remap = false, method = {"getHeadPosition"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$head(CallbackInfoReturnable<Vec3> cir) {
        if (this.dragonmaid$isHumanoid()) {
            cir.setReturnValue(this.getEyePosition());
        }
    }
}
