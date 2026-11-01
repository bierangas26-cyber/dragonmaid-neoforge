package com.example.dragonmaid.data;

import com.example.dragonmaid.advancement.DragonAdvancements;
import com.example.dragonmaid.advancement.HumanoidCompanions;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.DragonUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;

public final class DragonForms {
    public static final float SEATED_MODEL_DROP = 0.52F;

    private DragonForms() {
    }

    public static boolean isHumanoid(Entity entity) {
        if (entity instanceof DragonState dragonstate && dragonstate.dragonmaid$isHumanoid()) {
            return true;
        }

        return false;
    }

    public static DragonState state(DragonBaseEntity dragon) {
        return (DragonState)dragon;
    }

    public static boolean canManage(DragonBaseEntity dragon, Player player) {
        return player != null
            && player.isAlive()
            && !player.isSpectator()
            && dragon != null
            && dragon.isAlive()
            && !dragon.isModelDead()
            && !dragon.isRemoved()
            && dragon.level() == player.level()
            && dragon.isTame()
            && dragon.isOwnedBy(player)
            && dragon.distanceToSqr(player) < 64.0;
    }

    public static boolean unlocked(DragonBaseEntity dragon) {
        return dragon.isTame() && dragon.hasCustomName() && dragon.getDragonStage() >= 3 && state(dragon).dragonmaid$getKnowledge() >= 300;
    }

    public static float height(DragonBaseEntity dragon) {
        float f = (float)dragon.getAgeInDays();
        boolean flag = dragon.isMale();
        float f1 = flag ? 2.0F : 1.85F;
        return f <= 75.0F
            ? Mth.lerp(Mth.clamp((f - 50.0F) / 25.0F, 0.0F, 1.0F), flag ? 1.75F : 1.6F, f1)
            : Mth.lerp(Mth.clamp((f - 75.0F) / 25.0F, 0.0F, 1.0F), f1, flag ? 2.5F : 2.3F);
    }

    public static EntityDimensions humanoidDimensions(DragonBaseEntity dragon) {
        return humanoidDimensions(dragon, dragon.getPose());
    }

    public static boolean isSeated(DragonBaseEntity dragon) {
        return dragon.isOrderedToSit() && !dragon.isPassenger() && !dragon.isSleeping() && !dragon.isModelDead();
    }

    public static float modelScale(DragonBaseEntity dragon) {
        return height(dragon) / 2.0F;
    }

    public static EntityDimensions humanoidDimensions(DragonBaseEntity dragon, Pose pose) {
        float f = height(dragon);
        float f1 = isSeated(dragon) ? f - 0.52F * modelScale(dragon) : (pose == Pose.CROUCHING ? f * 0.85F : f);
        return EntityDimensions.scalable(0.6F * f / 1.8F, f1);
    }

    public static EntityDimensions dragonDimensions(DragonBaseEntity dragon) {
        return dragon.getType().getDimensions().scale(Math.min(dragon.getRenderSize() * 0.35F, 7.0F));
    }

    public static boolean canExpandToDragon(DragonBaseEntity dragon, AABB bounds) {
        if (dragon.level().getWorldBorder().isWithinBounds(bounds) && dragon.level().getEntityCollisions(dragon, bounds).isEmpty()) {
            BlockCollisions<BlockPos> blockcollisions = new BlockCollisions<BlockPos>(dragon.level(), dragon, bounds, false, (posx, shape) -> posx.immutable());
            if (!blockcollisions.hasNext()) {
                return true;
            } else if (dragon.getDragonStage() >= 3 && DragonUtils.canGrief(dragon) && EventHooks.canEntityGrief(dragon.level(), dragon)) {
                while (blockcollisions.hasNext()) {
                    BlockPos blockpos = (BlockPos)blockcollisions.next();
                    if (!state(dragon).dragonmaid$canBreakBlock(blockpos, dragon.level().getBlockState(blockpos))) {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean toggle(DragonBaseEntity dragon, Player player) {
        if (!canManage(dragon, player)) {
            return false;
        } else {
            boolean flag = isHumanoid(dragon);
            if (!flag && !unlocked(dragon)) {
                player.displayClientMessage(Component.translatable("message.dragonmaid.locked"), true);
                return false;
            } else if (dragon.onGround() && !dragon.isPassenger() && !dragon.isVehicle()) {
                EntityDimensions entitydimensions = flag ? dragonDimensions(dragon) : humanoidDimensions(dragon);
                AABB aabb = entitydimensions.makeBoundingBox(dragon.position()).deflate(0.01);
                if (flag ? canExpandToDragon(dragon, aabb) : dragon.level().noCollision(dragon, aabb)) {
                    state(dragon).dragonmaid$setHumanoid(!flag);
                    dragon.setDeltaMovement(Vec3.ZERO);
                    FormChangeEffects.emit(dragon);
                    if (!flag && player instanceof ServerPlayer serverplayer) {
                        DragonAdvancements.award(serverplayer, DragonAdvancements.CLOSER_COMPANION);
                        HumanoidCompanions.check(serverplayer);
                    }

                    player.displayClientMessage(Component.translatable(flag ? "message.dragonmaid.dragon_form" : "message.dragonmaid.human_form"), true);
                    return true;
                } else {
                    player.displayClientMessage(Component.translatable(flag ? "message.dragonmaid.dragon_form_blocked" : "message.dragonmaid.no_space"), true);
                    return false;
                }
            } else {
                player.displayClientMessage(Component.translatable("message.dragonmaid.ground_required"), true);
                return false;
            }
        }
    }
}
