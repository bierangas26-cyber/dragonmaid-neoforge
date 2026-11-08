package com.example.dragonmaid.data;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.advancement.DragonAdvancements;
import com.example.dragonmaid.advancement.HumanoidCompanions;
import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.DragonUtils;
import com.iafenvoy.iceandfire.registry.tag.IafBlockTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public final class DragonForms {
    public static final float SEATED_MODEL_DROP = 0.52F;

    private DragonForms() {
    }

    public static boolean isHumanoid(Entity entity) {
        if (entity instanceof DragonState state && state.dragonmaid$isHumanoid()) {
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

    /** 1.2: 迷你化形态按第一成长阶段的外观尺寸计算人形高度。 */
    public static float height(DragonBaseEntity dragon) {
        return height(dragon, MiniatureForm.isSmall(dragon));
    }

    private static float height(DragonBaseEntity dragon, boolean small) {
        float days = small ? 50.0F : (float)dragon.getAgeInDays();
        boolean male = dragon.isMale();
        float stageFour = male ? 2.0F : 1.85F;
        return days <= 75.0F
            ? Mth.lerp(Mth.clamp((days - 50.0F) / 25.0F, 0.0F, 1.0F), male ? 1.75F : 1.6F, stageFour)
            : Mth.lerp(Mth.clamp((days - 75.0F) / 50.0F, 0.0F, 1.0F), stageFour, male ? 2.5F : 2.3F);
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
        return humanoidDimensions(dragon, pose, MiniatureForm.isSmall(dragon));
    }

    public static EntityDimensions humanoidDimensions(DragonBaseEntity dragon, Pose pose, boolean small) {
        float standing = height(dragon, small);
        float actual = isSeated(dragon) ? standing - 0.52F * (standing / 2.0F) : (pose == Pose.CROUCHING ? standing * 0.85F : standing);
        return EntityDimensions.scalable(0.6F * standing / 1.8F, actual);
    }

    public static EntityDimensions dragonDimensions(DragonBaseEntity dragon) {
        return dragon.getType().getDimensions().scale(Math.min(MiniatureForm.visualSize(dragon) * 0.35F, 7.0F));
    }

    /** 1.2: 变形前的“已加载世界”检查（世界边界 + 高度上下限 + 区块已加载）。 */
    public static boolean containedInLoadedWorld(DragonBaseEntity dragon, AABB bounds) {
        Level level = dragon.level();
        WorldBorder border = level.getWorldBorder();
        return bounds.minY >= (double)level.getMinBuildHeight()
            && bounds.maxY <= (double)level.getMaxBuildHeight()
            && bounds.minX >= border.getMinX()
            && bounds.maxX <= border.getMaxX()
            && bounds.minZ >= border.getMinZ()
            && bounds.maxZ <= border.getMaxZ()
            && level.hasChunksAt(
                BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ), BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)
            );
    }

    public static boolean canExpandToDragon(DragonBaseEntity dragon, AABB bounds) {
        if (containedInLoadedWorld(dragon, bounds) && dragon.level().getEntityCollisions(dragon, bounds).isEmpty()) {
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

    /** 1.2: 变回龙形态时清掉与包围盒相交的树木（需要 canGrief 权限，掉落受 IAF 配置控制）。 */
    private static boolean clearIntersectingTrees(DragonBaseEntity dragon, AABB bounds) {
        Level level = dragon.level();
        List<BlockPos> trees = new ArrayList<>();
        List<BlockPos> allBlocks = new ArrayList<>();
        BlockCollisions<BlockPos> collisions = new BlockCollisions<BlockPos>(level, dragon, bounds, false, (posx, shape) -> posx.immutable());

        while (collisions.hasNext()) {
            BlockPos pos = (BlockPos)collisions.next();
            BlockState block = level.getBlockState(pos);
            if (!allBlocks.contains(pos)) {
                allBlocks.add(pos);
            }

            if ((block.is(BlockTags.LOGS) || block.is(BlockTags.LEAVES)) && !trees.contains(pos)) {
                trees.add(pos);
            }
        }

        if (trees.isEmpty()) {
            return true;
        } else if (!level.isClientSide
            && dragon.getDragonStage() >= 3
            && !dragon.isSleeping()
            && DragonUtils.canGrief(dragon)
            && EventHooks.canEntityGrief(level, dragon)) {
            for (BlockPos posx : allBlocks) {
                BlockState blockx = level.getBlockState(posx);
                if (!state(dragon).dragonmaid$canBreakBlock(posx, blockx) || !EventHooks.onEntityDestroyBlock(dragon, posx, blockx)) {
                    return false;
                }
            }

            for (BlockPos posxx : trees) {
                BlockState blockx = level.getBlockState(posxx);
                if (blockx.is(BlockTags.LOGS) || blockx.is(BlockTags.LEAVES)) {
                    if (!state(dragon).dragonmaid$canBreakBlock(posxx, blockx)) {
                        return false;
                    }

                    boolean drops = !blockx.is(IafBlockTags.DRAGON_BLOCK_BREAK_NO_DROPS)
                        && (double)dragon.getRandom().nextFloat() <= IafCommonConfig.INSTANCE.dragon.blockBreakingDropChance.getValue();
                    if (!level.destroyBlock(posxx, drops, dragon)) {
                        return false;
                    }
                }
            }

            return canExpandToDragon(dragon, bounds);
        } else {
            return false;
        }
    }

    /** 1.2: 变回龙形态时若原地被挡，在半径 4 格内找一处可站立的空地。 */
    private static Vec3 nearbyDragonPosition(DragonBaseEntity dragon, EntityDimensions dimensions) {
        Level level = dragon.level();
        Vec3 origin = dragon.position();

        for (int distanceSquared = 1; distanceSquared <= 16; distanceSquared++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (dx * dx + dz * dz == distanceSquared) {
                        Vec3 destination = origin.add((double)dx, 0.0, (double)dz);
                        AABB bounds = dimensions.makeBoundingBox(destination).deflate(0.01);
                        AABB path = dragon.getBoundingBox().expandTowards((double)dx, 0.0, (double)dz).deflate(0.01);
                        if (containedInLoadedWorld(dragon, bounds) && containedInLoadedWorld(dragon, path)) {
                            BlockPos groundPos = BlockPos.containing(destination.x, destination.y - 0.01, destination.z);
                            BlockState ground = level.getBlockState(groundPos);
                            if (!ground.is(BlockTags.LEAVES)
                                && !ground.is(BlockTags.LOGS)
                                && ground.isFaceSturdy(level, groundPos, Direction.UP)
                                && level.noCollision(dragon, path)
                                && level.noCollision(dragon, bounds)) {
                                return destination;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static boolean toggle(DragonBaseEntity dragon, Player player) {
        if (!canManage(dragon, player)) {
            return false;
        } else {
            boolean human = isHumanoid(dragon);
            if (!human && !unlocked(dragon)) {
                player.displayClientMessage(Component.translatable("message.dragonmaid.locked"), true);
                return false;
            } else if (dragon.onGround() && !dragon.isPassenger() && !dragon.isVehicle()) {
                EntityDimensions next = human ? dragonDimensions(dragon) : humanoidDimensions(dragon);
                AABB bounds = next.makeBoundingBox(dragon.position()).deflate(0.01);
                Vec3 destination = dragon.position();
                if (human && (!canExpandToDragon(dragon, bounds) || !clearIntersectingTrees(dragon, bounds))) {
                    destination = nearbyDragonPosition(dragon, next);
                }

                if (destination == null || !human && !dragon.level().noCollision(dragon, bounds)) {
                    player.displayClientMessage(Component.translatable(human ? "message.dragonmaid.dragon_form_blocked" : "message.dragonmaid.no_space"), true);
                    return false;
                } else {
                    if (human && !destination.equals(dragon.position())) {
                        dragon.getNavigation().stop();
                        dragon.setPos(destination);
                    }

                    state(dragon).dragonmaid$setHumanoid(!human);
                    dragon.setDeltaMovement(Vec3.ZERO);
                    FormChangeEffects.emit(dragon);
                    if (!human && player instanceof ServerPlayer serverPlayer) {
                        DragonAdvancements.award(serverPlayer, DragonAdvancements.CLOSER_COMPANION);
                        HumanoidCompanions.check(serverPlayer);
                    }

                    player.displayClientMessage(Component.translatable(human ? "message.dragonmaid.dragon_form" : "message.dragonmaid.human_form"), true);
                    return true;
                }
            } else {
                player.displayClientMessage(Component.translatable("message.dragonmaid.ground_required"), true);
                return false;
            }
        }
    }
}
