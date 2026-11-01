package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.compat.HumanoidForge;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public final class HumanoidGoal extends Goal {
    private static final double MELEE_PURSUIT_RANGE = 4.0;
    private final DragonBaseEntity dragon;
    private final GuardEscort escort;
    private final HumanoidHome home;
    private final HumanoidTailSweep tailSweep;
    private final HumanoidIdleLook idleLook;
    private int meleeCooldown;
    private int tailSweepCooldown;
    private int breathCooldown;
    private int breathTicks;
    private int windupTicks;
    private int burstElapsed;
    private int chargeCooldown;
    private int clawTicks;
    private int lightningReserve = -1;
    private int lightningUseRemainder;
    private int lightningTicks;
    private int lightningAimTicks;
    private boolean lightningExhausted;
    private LivingEntity previousTarget;

    public HumanoidGoal(DragonBaseEntity dragon) {
        this.dragon = dragon;
        this.escort = new GuardEscort(dragon);
        this.home = new HumanoidHome(dragon);
        this.tailSweep = new HumanoidTailSweep(dragon);
        this.idleLook = new HumanoidIdleLook(dragon);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    public CompoundTag saveCombatState() {
        int i = this.windupTicks <= 0 && this.breathTicks <= 0 ? this.breathCooldown : Math.max(this.breathCooldown, this.recoveryTicks());
        int j = this.lightningReserve < 0 ? HumanoidCombatState.lightningCapacity(this.dragon.getDragonStage()) : this.lightningReserve;
        return new HumanoidCombatState(
                this.meleeCooldown, i, this.chargeCooldown, j, this.lightningExhausted, this.lightningUseRemainder, this.tailSweepCooldown
            )
            .save(this.dragon.getDragonStage());
    }

    public void loadCombatState(CompoundTag dragonMaid) {
        HumanoidCombatState humanoidcombatstate = HumanoidCombatState.read(dragonMaid, this.dragon.getDragonStage());
        this.meleeCooldown = humanoidcombatstate.meleeCooldown();
        this.tailSweepCooldown = humanoidcombatstate.tailSweepCooldown();
        this.breathCooldown = humanoidcombatstate.breathCooldown();
        this.chargeCooldown = humanoidcombatstate.chargeCooldown();
        this.lightningReserve = humanoidcombatstate.lightningReserve();
        this.lightningExhausted = humanoidcombatstate.lightningExhausted();
        this.lightningUseRemainder = humanoidcombatstate.lightningUseRemainder();
        this.windupTicks = this.breathTicks = this.burstElapsed = this.clawTicks = this.lightningTicks = this.lightningAimTicks = 0;
        this.previousTarget = null;
        this.tailSweep.cancel();
        this.escort.reset();
        this.dragon.setTarget(null);
        this.dragon.burningTarget = null;
        HumanoidCombat.stopBreath(this.dragon);
        DragonForms.state(this.dragon).dragonmaid$setAction(0);
    }

    public boolean canUse() {
        return DragonForms.isHumanoid(this.dragon);
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void stop() {
        this.idleLook.reset();
        this.tailSweep.cancel();
        this.home.reset();
        this.dragon.getNavigation().stop();
        HumanoidCombat.stopBreath(this.dragon);
        DragonForms.state(this.dragon).dragonmaid$setAction(0);
        this.dragon.burningTarget = null;
        this.cancelBurst();
        this.escort.reset();
        this.lightningAimTicks = 0;
        this.clawTicks = 0;
        this.previousTarget = null;
        BreathAnimationSync.flush(this.dragon);
    }

    private void cancelBurst() {
        BreathAnimationSync.phase(this.dragon, 4);
        if (this.windupTicks > 0 || this.breathTicks > 0) {
            this.breathCooldown = this.recoveryTicks();
        }

        this.windupTicks = 0;
        this.breathTicks = 0;
        this.burstElapsed = 0;
    }

    private void recoverLightning() {
        if (this.dragon instanceof LightningDragonEntity) {
            int i = BreathProfile.forStage(this.dragon.getDragonStage()).stage() * 20;
            int j = Math.min(i * 10, this.lightningReserve * 10 - this.lightningUseRemainder + 10);
            this.lightningReserve = (j + 9) / 10;
            this.lightningUseRemainder = this.lightningReserve * 10 - j;
            if (j == i * 10) {
                this.lightningExhausted = false;
            }
        }
    }

    private int recoveryTicks() {
        return DragonAccessories.breathRecoveryTicks(this.dragon, (Integer)DragonMaidConfig.BREATH_RECOVERY.get());
    }

    private boolean consumeLightning() {
        int i = DragonAccessories.enhancesBreath(this.dragon) ? 9 : 10;
        int j = Math.max(0, this.lightningReserve * 10 - this.lightningUseRemainder - i);
        this.lightningReserve = (j + 9) / 10;
        this.lightningUseRemainder = this.lightningReserve * 10 - j;
        return j == 0;
    }

    private void launchCharge(LivingEntity target) {
        if (this.dragon.getDragonStage() >= 5 && this.chargeCooldown == 0 && SafeDragonCharges.launch(this.dragon, target) != null) {
            this.chargeCooldown = (Integer)DragonMaidConfig.CHARGE_COOLDOWN.get();
        }
    }

    public void tick() {
        BreathAnimationSync.beginTick(this.dragon);

        try {
            this.tickCombat();
        } finally {
            BreathAnimationSync.flush(this.dragon);
        }
    }

    private void tickCombat() {
        if (this.lightningReserve < 0) {
            this.lightningReserve = BreathProfile.forStage(this.dragon.getDragonStage()).stage() * 20;
        }

        if (this.meleeCooldown > 0) {
            this.meleeCooldown--;
        }

        if (this.tailSweepCooldown > 0) {
            this.tailSweepCooldown--;
        }

        if (this.breathCooldown > 0) {
            this.breathCooldown--;
        }

        if (this.clawTicks > 0) {
            this.clawTicks--;
        }

        if (this.chargeCooldown > 0) {
            this.chargeCooldown--;
        }

        if (this.tailSweep.tick()) {
            this.idleLook.reset();
            this.home.reset();
            HumanoidCombat.stopBreath(this.dragon);
            this.recoverLightning();
        } else {
            if (HumanoidForge.isWorking(this.dragon)) {
                this.home.reset();
            }

            if (HumanoidForge.tryWork(this.dragon)) {
                this.idleLook.reset();
                this.cancelBurst();
                this.escort.reset();
                this.lightningAimTicks = 0;
                this.recoverLightning();
            } else if (!this.dragon.isModelDead() && this.dragon.getCommand() != 1 && !this.dragon.isSleeping() && !this.dragon.isPassenger()) {
                LivingEntity livingentity = GuardEscort.owner(this.dragon);
                if (livingentity != null) {
                    this.escort.tryTeleport(livingentity);
                } else {
                    this.escort.reset();
                }

                LivingEntity livingentity1 = this.dragon.getTarget();
                if (livingentity1 != null) {
                    this.home.reset();
                }

                if (livingentity1 != null
                    && (
                        this.dragon.getCommand() == 2 && livingentity == null
                            || !HumanoidTargetGoal.isEnemy(this.dragon, livingentity1)
                            || !GuardEscort.reachableTarget(this.dragon, livingentity1)
                    )) {
                    this.dragon.setTarget(null);
                    livingentity1 = null;
                }

                if (livingentity1 != null || this.dragon.getCommand() != 0) {
                    this.home.reset();
                }

                if (livingentity1 != this.previousTarget) {
                    this.escort.targetChanged();
                    this.previousTarget = livingentity1;
                }

                boolean flag = false;
                boolean flag1 = false;
                BreathProfile breathprofile = BreathProfile.forDragon(this.dragon);
                double d0 = HumanoidCombat.clawReach(this.dragon);
                boolean flag2 = false;
                if (livingentity1 != null) {
                    this.dragon.getLookControl().setLookAt(livingentity1, 30.0F, 30.0F);
                    HumanoidRoar.tryRoar(this.dragon);
                    if (HumanoidRoar.isRoaring(this.dragon)) {
                        this.idleLook.reset();
                        if (livingentity != null) {
                            this.escort.move(livingentity, livingentity1, breathprofile, false, d0);
                        } else {
                            this.dragon.getNavigation().stop();
                        }

                        HumanoidCombat.stopBreath(this.dragon);
                        this.recoverLightning();
                        DragonForms.state(this.dragon).dragonmaid$setAction(5);
                        return;
                    }

                    double d1 = (double)this.dragon.distanceTo(livingentity1);
                    boolean flag3 = this.dragon.hasLineOfSight(livingentity1);
                    boolean flag4 = breathprofile.inRange(this.dragon, livingentity1);
                    boolean flag5 = breathprofile.canHit(this.dragon, livingentity1);
                    boolean flag6 = this.dragon instanceof LightningDragonEntity
                        ? !this.lightningExhausted && this.lightningReserve > 0
                        : this.windupTicks > 0 || this.breathTicks > 0 || this.breathCooldown == 0;
                    if (flag6) {
                        if (!(this.dragon instanceof LightningDragonEntity)) {
                            if (this.breathTicks == 0 && this.windupTicks == 0 && this.breathCooldown == 0) {
                                this.windupTicks = (Integer)DragonMaidConfig.BREATH_WINDUP.get();
                                this.burstElapsed = 0;
                                BreathAnimationSync.begin(this.dragon, this.windupTicks, false);
                            }

                            if (flag5) {
                                int j = this.windupTicks > 0 ? this.windupTicks : (14 - this.burstElapsed % 14) % 14;
                                BreathAnimationSync.forecast(this.dragon, j);
                            }

                            if (this.windupTicks > 0) {
                                flag1 = true;
                                this.windupTicks--;
                                BreathAnimationSync.preparationRemaining(this.dragon, this.windupTicks);
                                if (this.windupTicks == 0) {
                                    this.breathTicks = breathprofile.duration();
                                    BreathAnimationSync.phase(this.dragon, 1);
                                }
                            } else if (this.breathTicks > 0 && flag5) {
                                boolean flag7 = this.burstElapsed % 14 == 0;
                                HumanoidCombat.breath(this.dragon, livingentity1, flag7);
                                if (flag7 && flag3 && flag4) {
                                    this.launchCharge(livingentity1);
                                }

                                this.burstElapsed++;
                                flag = true;
                                if (--this.breathTicks == 0) {
                                    this.breathCooldown = this.recoveryTicks();
                                    BreathAnimationSync.phase(this.dragon, 3);
                                }
                            } else {
                                flag1 = true;
                            }
                        } else {
                            int i = (Integer)DragonMaidConfig.BREATH_WINDUP.get();
                            BreathAnimationSync.begin(this.dragon, i, true);
                            this.lightningAimTicks = Math.min(i, this.lightningAimTicks + 1);
                            BreathAnimationSync.preparationRemaining(this.dragon, i - this.lightningAimTicks);
                            if (flag5 || livingentity != null && this.lightningAimTicks >= i) {
                                if (this.dragon.getDragonStage() >= 5 && flag3 && flag4 && this.chargeCooldown > 0) {
                                    BreathAnimationSync.forecast(this.dragon, this.chargeCooldown);
                                }

                                this.lightningAimTicks = i;
                                BreathAnimationSync.phase(this.dragon, 2);
                                HumanoidCombat.breath(this.dragon, livingentity1, this.lightningTicks++ % 10 == 0);
                                if (flag3 && flag4) {
                                    this.launchCharge(livingentity1);
                                }

                                flag = true;
                                if (this.consumeLightning()) {
                                    this.lightningExhausted = true;
                                    BreathAnimationSync.phase(this.dragon, 3);
                                }
                            } else {
                                flag1 = true;
                            }
                        }
                    } else {
                        this.lightningAimTicks = 0;
                    }

                    flag2 = !flag && !flag1 && d1 <= 4.0;
                    if (flag2 && flag3 && this.meleeCooldown == 0) {
                        if (d1 <= HumanoidTailSweep.reach(this.dragon) && this.tailSweepCooldown == 0 && this.tailSweep.tryStart(livingentity1)) {
                            this.idleLook.reset();
                            this.tailSweepCooldown = 120;
                            this.meleeCooldown = 24;
                            this.clawTicks = 0;
                            this.cancelBurst();
                            this.recoverLightning();
                            return;
                        }

                        if (d1 <= d0) {
                            HumanoidCombat.claw(this.dragon, d0);
                            this.meleeCooldown = 24;
                            this.clawTicks = 8;
                        }
                    }

                    if (livingentity == null) {
                        if (!flag && (!flag1 || !flag5) && (!flag3 || (flag2 ? !(d1 <= d0) : !flag5))) {
                            if (this.dragon.getNavigation().isDone() || this.dragon.tickCount % 10 == 0) {
                                this.dragon.getNavigation().moveTo(livingentity1, 1.1);
                            }
                        } else {
                            this.dragon.getNavigation().stop();
                        }
                    }
                } else {
                    this.cancelBurst();
                    this.lightningAimTicks = 0;
                    if (this.dragon.getCommand() == 0 && this.home.move()) {
                        this.idleLook.reset();
                        HumanoidCombat.stopBreath(this.dragon);
                        this.recoverLightning();
                        DragonForms.state(this.dragon).dragonmaid$setAction(0);
                        return;
                    }

                    if (this.dragon.getCommand() == 0 && this.dragon.getNavigation().isDone() && this.dragon.tickCount % 80 == 0) {
                        Vec3 vec3 = HumanoidWater.wander(this.dragon);
                        if (vec3 == null) {
                            vec3 = DefaultRandomPos.getPos(this.dragon, 8, 4);
                        }

                        vec3 = this.home.limitWander(vec3);
                        if (vec3 != null) {
                            this.dragon.getNavigation().moveTo(vec3.x, vec3.y, vec3.z, 0.8);
                        }
                    } else if (livingentity == null && this.dragon.getCommand() == 2) {
                        this.dragon.getNavigation().stop();
                    }
                }

                if (livingentity != null) {
                    this.escort.move(livingentity, livingentity1, breathprofile, flag2, d0);
                }

                if (!flag) {
                    HumanoidCombat.stopBreath(this.dragon);
                    this.recoverLightning();
                }

                DragonForms.state(this.dragon).dragonmaid$setAction(flag ? 2 : (flag1 ? 3 : (this.clawTicks > 0 ? 1 : 0)));
                this.idleLook.tick();
            } else {
                this.stop();
                this.recoverLightning();
            }
        }
    }
}
