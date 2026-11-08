package com.example.dragonmaid.inventory;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.accessory.DragonStorage;
import com.example.dragonmaid.combat.GuardEscort;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import com.iafenvoy.uranus.object.entity.pathfinding.raycoms.AdvancedPathNavigate;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 1.2: 龙自动把地上的掉落物捡进背包（DragonStorageInventory）的 AI goal。
 *
 * 移植说明（1.21.1）：
 * - 上游用 mixin/ItemPickupTargetAccessor 读 ItemEntity.target；1.21.1 的 ItemEntity 已有
 *   public getTarget()，因此不需要额外 mixin。
 * - AdvancedPathNavigate 仍在 com.iafenvoy.uranus... 包（IAF CE 依赖的 Uranus 库），getDestination() 在抽象父类上。
 * - 1.20.1 的 Forge 事件/API 全部对等替换。
 */
public final class DragonBagPickupGoal extends Goal {
    private static final int RANGE = 8;
    private static final int NEARBY_RANGE = 4;
    private static final int POST_COMBAT_TICKS = 60;
    private final DragonBaseEntity dragon;
    private ItemEntity target;
    private PathNavigation navigator;
    private BlockPos destination;
    private int nextScan;
    private int giveUpAt;
    private int skipUntil;
    private int failedId = -1;
    private int lootUntil;
    private boolean wasFighting;
    private boolean beforeReturn;

    public DragonBagPickupGoal(DragonBaseEntity dragon) {
        this.dragon = dragon;
    }

    private static boolean canCollect(DragonBaseEntity dragon) {
        return !dragon.level().isClientSide
            && dragon.isTame()
            && dragon.getOwnerUUID() != null
            && dragon.isAlive()
            && dragon.getHealth() > 0.0F
            && !dragon.isModelDead()
            && !dragon.isNoAi()
            && !dragon.isSleeping()
            && !dragon.isPassenger()
            && !dragon.isVehicle();
    }

    private static boolean idle(DragonBaseEntity dragon) {
        return canCollect(dragon)
            && dragon.canMove()
            && dragon.getCommand() != 1
            && !dragon.isLeashed()
            && dragon.getTarget() == null
            && dragon.burningTarget == null
            && !dragon.isBreathingFire()
            && !dragon.isTackling()
            && !dragon.lookingForRoostAIFlag
            && (
                dragon.getAnimation() == null
                    || dragon.getAnimation() == IAnimatedEntity.NO_ANIMATION
                    || dragon.getAnimation() == DragonBaseEntity.ANIMATION_SPEAK
            )
            && DragonForms.state(dragon).dragonmaid$getAction() == 0;
    }

    private static boolean available(DragonBaseEntity dragon, ItemEntity item) {
        if (item != null && item.isAlive() && item.level() == dragon.level() && !item.hasPickUpDelay()) {
            UUID uuid = item.getTarget();
            return (uuid == null || uuid.equals(dragon.getUUID())) && DragonStorage.canPickup(dragon, item.getItem());
        } else {
            return false;
        }
    }

    private static boolean touching(DragonBaseEntity dragon, ItemEntity item) {
        return dragon.getBoundingBox().inflate(2.0, 0.75, 2.0).intersects(item.getBoundingBox()) && dragon.hasLineOfSight(item);
    }

    public static int tryCollect(DragonBaseEntity dragon, ItemEntity item) {
        if (canCollect(dragon) && available(dragon, item) && touching(dragon, item)) {
            ItemStack itemstack = item.getItem();
            ItemStack itemstack1 = DragonStorage.insert(dragon, itemstack);
            int i = itemstack.getCount() - itemstack1.getCount();
            if (i <= 0) {
                return 0;
            } else {
                dragon.onItemPickup(item);
                dragon.take(item, i);
                if (itemstack1.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(itemstack1);
                }

                return i;
            }
        } else {
            return 0;
        }
    }

    public static void livingTick(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide) {
            DragonBagPickupGoal dragonbagpickupgoal = ((DragonBagPickupGoal.Holder)dragon).dragonmaid$pickupGoal();
            if (dragonbagpickupgoal != null) {
                dragonbagpickupgoal.observeCombat();
            }

            if (Math.floorMod(dragon.tickCount + dragon.getId(), 5) == 0 && canCollect(dragon) && DragonStorage.hasPickupRoom(dragon)) {
                for (ItemEntity itementity : dragon.level()
                    .getEntitiesOfClass(ItemEntity.class, dragon.getBoundingBox().inflate(2.0, 0.75, 2.0), candidate -> available(dragon, candidate))) {
                    tryCollect(dragon, itementity);
                }
            }
        }
    }

    private void observeCombat() {
        if (this.dragon.getTarget() != null) {
            this.wasFighting = true;
            this.lootUntil = 0;
        } else if (this.wasFighting) {
            this.wasFighting = false;
            this.lootUntil = this.dragon.tickCount + POST_COMBAT_TICKS;
            this.nextScan = 0;
        }
    }

    public static boolean deferEscort(DragonBaseEntity dragon, LivingEntity owner) {
        DragonBagPickupGoal dragonbagpickupgoal = ((DragonBagPickupGoal.Holder)dragon).dragonmaid$pickupGoal();
        return dragonbagpickupgoal != null && dragonbagpickupgoal.nearbyBeforeReturn(owner);
    }

    private boolean nearbyBeforeReturn(LivingEntity owner) {
        double d0 = (Double)DragonMaidConfig.GUARD_TELEPORT_DISTANCE.get();
        if (this.dragon.tickCount < this.lootUntil
            && idle(this.dragon)
            && this.dragon.getCommand() == 2
            && owner != null
            && owner.level() == this.dragon.level()
            && owner.isAlive()
            && !(this.dragon.distanceToSqr(owner) >= d0 * d0)
            && DragonStorage.hasPickupRoom(this.dragon)) {
            if (this.target == null || !available(this.dragon, this.target) || !this.ownsRoute() && !touching(this.dragon, this.target)) {
                if (this.beforeReturn) {
                    this.stop();
                }

                if (this.dragon.tickCount < this.nextScan) {
                    return false;
                } else {
                    this.nextScan = this.dragon.tickCount + 10;
                    if (GuardEscort.hasChain(this.dragon)) {
                        return false;
                    } else {
                        this.target = this.nearest(NEARBY_RANGE, owner, d0);
                        if (this.target == null) {
                            return false;
                        } else {
                            this.beforeReturn = true;
                            this.start();
                            return this.target != null && (this.ownsRoute() || touching(this.dragon, this.target));
                        }
                    }
                }
            } else {
                this.beforeReturn = true;
                return true;
            }
        } else {
            if (this.beforeReturn) {
                this.stop();
            }

            return false;
        }
    }

    public boolean canUse() {
        if (!this.beforeReturn
            || this.target == null
            || !idle(this.dragon)
            || this.dragon.tickCount >= this.lootUntil
            || !this.ownsRoute() && !touching(this.dragon, this.target)) {
            if (!idle(this.dragon) || !DragonStorage.hasPickupRoom(this.dragon) || !this.dragon.getNavigation().isDone()) {
                return false;
            } else if (this.dragon.tickCount < this.nextScan) {
                return false;
            } else {
                this.nextScan = this.dragon.tickCount + 10 + Math.floorMod(this.dragon.getId(), 4);
                if (GuardEscort.hasChain(this.dragon)) {
                    return false;
                } else {
                    boolean flag = this.dragon.getCommand() == 2;
                    LivingEntity livingentity = flag ? GuardEscort.owner(this.dragon) : null;
                    if (!flag || livingentity != null && !(this.dragon.distanceToSqr(livingentity) > 64.0)) {
                        this.target = this.nearest(RANGE, livingentity, 8.0);
                        return this.target != null;
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return true;
        }
    }

    private ItemEntity nearest(int range, LivingEntity owner, double ownerRange) {
        return this.dragon
            .level()
            .getEntitiesOfClass(
                ItemEntity.class,
                this.dragon.getBoundingBox().inflate((double)range, 3.0, (double)range),
                item -> (this.dragon.tickCount >= this.skipUntil || item.getId() != this.failedId)
                    && this.dragon.isWithinRestriction(item.blockPosition())
                    && (owner == null || item.distanceToSqr(owner) <= ownerRange * ownerRange)
                    && available(this.dragon, item)
            )
            .stream()
            .min(Comparator.comparingDouble(item -> this.dragon.distanceToSqr(item)))
            .orElse(null);
    }

    public void start() {
        if (!this.ownsRoute()) {
            if (this.target != null && idle(this.dragon) && (this.beforeReturn || this.dragon.getNavigation().isDone())) {
                this.giveUpAt = this.dragon.tickCount + 60;
                if (!touching(this.dragon, this.target)) {
                    PathNavigation pathnavigation = this.dragon.getNavigation();
                    if (!pathnavigation.moveTo(this.target.getX(), this.target.getY(), this.target.getZ(), 1.0)) {
                        this.skipUnreachable();
                    } else {
                        this.navigator = pathnavigation;
                        this.destination = destination(pathnavigation);
                    }
                }
            } else {
                this.target = null;
            }
        }
    }

    private static BlockPos destination(PathNavigation nav) {
        return nav instanceof AdvancedPathNavigate advancedpathnavigate ? advancedpathnavigate.getDestination() : nav.getTargetPos();
    }

    private boolean ownsRoute() {
        return this.navigator != null
            && this.navigator == this.dragon.getNavigation()
            && this.destination != null
            && this.destination.equals(destination(this.navigator));
    }

    public boolean canContinueToUse() {
        return this.target != null
            && idle(this.dragon)
            && (!this.beforeReturn || this.dragon.tickCount < this.lootUntil)
            && (this.ownsRoute() || touching(this.dragon, this.target));
    }

    public void tick() {
        if (this.target != null) {
            if (idle(this.dragon) && available(this.dragon, this.target) && (!this.beforeReturn || this.dragon.tickCount < this.lootUntil)) {
                if (touching(this.dragon, this.target)) {
                    tryCollect(this.dragon, this.target);
                    this.stop();
                } else if (this.dragon.tickCount >= this.giveUpAt || this.navigator == null || this.navigator.isDone()) {
                    this.skipUnreachable();
                }
            } else {
                this.stop();
            }
        }
    }

    private void skipUnreachable() {
        this.failedId = this.target.getId();
        this.skipUntil = this.dragon.tickCount + 80;
        this.stop();
    }

    public void stop() {
        if (this.ownsRoute()) {
            this.navigator.stop();
        }

        this.navigator = null;
        this.destination = null;
        this.target = null;
        this.beforeReturn = false;
    }

    public interface Holder {
        DragonBagPickupGoal dragonmaid$pickupGoal();
    }
}
