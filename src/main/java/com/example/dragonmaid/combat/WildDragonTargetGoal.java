package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;

public final class WildDragonTargetGoal extends Goal {
    private final DragonBaseEntity dragon;
    private DragonBaseEntity selected;
    private int nextScanTick;
    private int unseenTicks;

    public WildDragonTargetGoal(DragonBaseEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    private boolean available() {
        if (!DragonForms.isHumanoid(this.dragon) && WildDragonCombat.canAcquireTargets(this.dragon)) {
            if (this.dragon.getCommand() != 2) {
                return true;
            } else {
                LivingEntity livingentity = GuardEscort.owner(this.dragon);
                double d0 = (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get();
                return livingentity != null && this.dragon.distanceToSqr(livingentity) <= d0 * d0;
            }
        } else {
            return false;
        }
    }

    public boolean canUse() {
        if (!this.available()) {
            return false;
        } else {
            LivingEntity livingentity = this.dragon.getTarget();
            if (livingentity != null && HumanoidTargetGoal.isEnemy(this.dragon, livingentity)) {
                return false;
            } else if (this.dragon.tickCount < this.nextScanTick) {
                return false;
            } else {
                this.nextScanTick = this.dragon.tickCount + 10;
                LivingEntity livingentity1 = GuardEscort.owner(this.dragon);
                LivingEntity livingentity2 = (LivingEntity)(livingentity1 == null ? this.dragon : livingentity1);
                double d0 = WildDragonCombat.targetRange();
                this.selected = this.dragon
                    .level()
                    .getEntitiesOfClass(
                        DragonBaseEntity.class,
                        livingentity2.getBoundingBox().inflate(d0),
                        target -> WildDragonCombat.isHostileWildDragon(this.dragon, target)
                                && WildDragonCombat.distanceToSqr(this.dragon, livingentity2, target) <= d0 * d0
                                && WildDragonCombat.withinTargetRange(this.dragon, target)
                                && GuardEscort.reachableTarget(this.dragon, target)
                                && this.dragon.hasLineOfSight(target)
                    )
                    .stream()
                    .min(Comparator.comparingDouble(target -> WildDragonCombat.distanceToSqr(this.dragon, this.dragon, target)))
                    .orElse(null);
                return this.selected != null;
            }
        }
    }

    public boolean canContinueToUse() {
        return this.selected != null
            && this.dragon.getTarget() == this.selected
            && this.available()
            && WildDragonCombat.isHostileWildDragon(this.dragon, this.selected)
            && WildDragonCombat.withinTargetRange(this.dragon, this.selected)
            && GuardEscort.reachableTarget(this.dragon, this.selected)
            && this.unseenTicks <= 40;
    }

    public void start() {
        this.unseenTicks = 0;
        this.dragon.setTarget(this.selected);
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        this.unseenTicks = this.selected != null && this.dragon.hasLineOfSight(this.selected) ? 0 : this.unseenTicks + 1;
    }

    public void stop() {
        if (this.selected != null && this.dragon.getTarget() == this.selected) {
            this.dragon.setTarget(null);
            if (this.dragon.getCommand() == 2) {
                this.dragon.getNavigation().stop();
            }
        }

        this.selected = null;
        this.unseenTicks = 0;
    }
}
