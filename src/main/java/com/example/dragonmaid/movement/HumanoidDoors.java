package com.example.dragonmaid.movement;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class HumanoidDoors {
    private static final Map<ServerLevel, Map<BlockPos, HumanoidDoors.Opened>> OPENED = new WeakHashMap<>();
    private HumanoidDoors.Passage alignment;

    public HumanoidDoors() {
    }

    static boolean canOpen(Mob mob) {
        if (mob instanceof DragonBaseEntity entitydragonbase
            && DragonForms.isHumanoid(entitydragonbase)
            && entitydragonbase.isAlive()
            && !entitydragonbase.isModelDead()
            && DragonForms.state(entitydragonbase).dragonmaid$getKnowledgeLevel() >= 2) {
            return true;
        }

        return false;
    }

    static PathType pathType(Mob mob, BlockGetter view, BlockPos pos, PathType original) {
        if (original != PathType.DOOR_WOOD_CLOSED && original != PathType.DOOR_OPEN) {
            return original;
        } else {
            BlockState blockstate = view.getBlockState(pos);
            if (!(blockstate.getBlock() instanceof DoorBlock doorblock) || !doorblock.type().canOpenByHand()) {
                return original;
            }

            if (original == PathType.DOOR_WOOD_CLOSED && !canOpen(mob)) {
                return original;
            } else {
                BlockPos blockpos = blockstate.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
                return passage(mob, view, blockpos, canOpen(mob)) == null ? PathType.BLOCKED : original;
            }
        }
    }

    private static HumanoidDoors.Passage passage(Mob mob, BlockGetter view, BlockPos first, boolean allowOpening) {
        if (!mob.level().hasChunkAt(first)) {
            return null;
        } else {
            BlockState blockstate = view.getBlockState(first);
            if (blockstate.getBlock() instanceof DoorBlock doorblock
                && doorblock.type().canOpenByHand()
                && blockstate.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                && ((Boolean)blockstate.getValue(DoorBlock.OPEN) || allowOpening)) {
                Direction direction1 = (Direction)blockstate.getValue(DoorBlock.FACING);
                double d0 = (double)first.getX() + 0.5;
                double d1 = (double)first.getZ() + 0.5;
                if (fits(mob, view, first, null, d0, d1)) {
                    return new HumanoidDoors.Passage(first, null, d0, d1, direction1);
                }

                for (Direction direction : new Direction[]{direction1.getClockWise(), direction1.getCounterClockWise()}) {
                    BlockPos blockpos = first.relative(direction);
                    if (mob.level().hasChunkAt(blockpos)) {
                        BlockState blockstate1 = view.getBlockState(blockpos);
                        Block midX = blockstate1.getBlock();
                        if (midX instanceof DoorBlock) {
                            DoorBlock doorblock1 = (DoorBlock)midX;
                            if (doorblock1.type().canOpenByHand()
                                && blockstate1.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                                && ((Direction)blockstate1.getValue(DoorBlock.FACING)).getAxis() == direction1.getAxis()
                                && ((Boolean)blockstate1.getValue(DoorBlock.OPEN) || allowOpening)) {
                                double d3 = d0 + (double)direction.getStepX() * 0.5;
                                double d2 = d1 + (double)direction.getStepZ() * 0.5;
                                if (fits(mob, view, first, blockpos, d3, d2)) {
                                    return new HumanoidDoors.Passage(first, blockpos, d3, d2, direction1);
                                }
                            }
                        }
                    }
                }

                return null;
            }

            return null;
        }
    }

    private static boolean fits(Mob mob, BlockGetter view, BlockPos first, BlockPos second, double x, double z) {
        double d0 = (double)mob.getBbWidth() * 0.5;
        double d1 = 1.0E-5;
        AABB aabb = new AABB(
            x - d0 + d1, (double)first.getY() + d1, z - d0 + d1, x + d0 - d1, (double)((float)first.getY() + mob.getBbHeight()) - d1, z + d0 - d1
        );
        int i = Mth.floor(aabb.minX);
        int j = Mth.floor(aabb.maxX);
        int k = Mth.floor(aabb.minY);
        int l = Mth.floor(aabb.maxY);
        int i1 = Mth.floor(aabb.minZ);
        int j1 = Mth.floor(aabb.maxZ);
        CollisionContext collisioncontext = CollisionContext.of(mob);

        for (int k1 = i; k1 <= j; k1++) {
            for (int l1 = i1; l1 <= j1; l1++) {
                BlockPos blockpos = new BlockPos(k1, k, l1);
                if (!mob.level().hasChunkAt(blockpos)) {
                    return false;
                }

                for (int i2 = k; i2 <= l; i2++) {
                    BlockPos blockpos1 = new BlockPos(k1, i2, l1);
                    BlockState blockstate = view.getBlockState(blockpos1);
                    if (blockstate.getBlock() instanceof DoorBlock && (sameDoor(blockpos1, first) || sameDoor(blockpos1, second))) {
                        blockstate = (BlockState)blockstate.setValue(DoorBlock.OPEN, true);
                    }

                    VoxelShape voxelshape = blockstate.getCollisionShape(view, blockpos1, collisioncontext);

                    for (AABB aabb1 : voxelshape.toAabbs()) {
                        if (aabb1.move(blockpos1).intersects(aabb)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private static boolean sameDoor(BlockPos pos, BlockPos lower) {
        return lower != null
            && pos.getX() == lower.getX()
            && pos.getZ() == lower.getZ()
            && (pos.getY() == lower.getY() || pos.getY() == lower.getY() + 1);
    }

    void beforeNavigation(Mob mob, PathNavigation navigation) {
        this.alignment = null;
        if (canOpen(mob) && mob.level() instanceof ServerLevel serverlevel) {
            Path path = navigation.getPath();
            if (path != null && !path.isDone()) {
                int i = Math.max(0, path.getNextNodeIndex() - 1);
                int j = Math.min(path.getNodeCount(), path.getNextNodeIndex() + 3);

                for (int k = i; k < j; k++) {
                    BlockPos blockpos = path.getNodePos(k);

                    for (int l = 0; l <= 1; l++) {
                        BlockPos blockpos1 = blockpos.above(l);
                        if (serverlevel.hasChunkAt(blockpos1)) {
                            BlockState blockstate = serverlevel.getBlockState(blockpos1);
                            Block lower = blockstate.getBlock();
                            if (lower instanceof DoorBlock) {
                                DoorBlock doorblock = (DoorBlock)lower;
                                if (doorblock.type().canOpenByHand()) {
                                    BlockPos blockpos2 = blockstate.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? blockpos1.below() : blockpos1;
                                    if (!(mob.distanceToSqr(Vec3.atCenterOf(blockpos2)) > 6.25)) {
                                        HumanoidDoors.Passage humanoiddoors$passage = passage(mob, serverlevel, blockpos2, true);
                                        if (humanoiddoors$passage != null) {
                                            open(serverlevel, mob, humanoiddoors$passage.first());
                                            if (humanoiddoors$passage.second() != null) {
                                                open(serverlevel, mob, humanoiddoors$passage.second());
                                            }

                                            if (humanoiddoors$passage.second() != null) {
                                                this.alignment = humanoiddoors$passage;
                                            }

                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void afterNavigation(Mob mob, PathNavigation navigation, double speed) {
        if (this.alignment != null && canOpen(mob)) {
            Path path = navigation.getPath();
            if (path != null && !path.isDone()) {
                Vec3 vec3 = path.getNextEntityPos(mob);
                double d0 = this.alignment.facing().getAxis() == Axis.Z ? this.alignment.x() : vec3.x;
                double d1 = this.alignment.facing().getAxis() == Axis.X ? this.alignment.z() : vec3.z;
                mob.getMoveControl().setWantedPosition(d0, vec3.y, d1, speed);
            }
        }
    }

    private static void open(ServerLevel level, Mob mob, BlockPos pos) {
        if (level.hasChunkAt(pos)) {
            BlockState blockstate = level.getBlockState(pos);
            if (blockstate.getBlock() instanceof DoorBlock doorblock && doorblock.type().canOpenByHand() && !(Boolean)blockstate.getValue(DoorBlock.OPEN)) {
                doorblock.setOpen(mob, level, blockstate, pos, true);
                if (level.getBlockState(pos).getBlock() == doorblock && (Boolean)level.getBlockState(pos).getValue(DoorBlock.OPEN)) {
                    OPENED.computeIfAbsent(level, ignored -> new HashMap<>()).putIfAbsent(pos.immutable(), new HumanoidDoors.Opened(doorblock, level.getGameTime()));
                }

                return;
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 10 == 0 && !OPENED.isEmpty()) {
            Iterator<Entry<ServerLevel, Map<BlockPos, HumanoidDoors.Opened>>> iterator = OPENED.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<ServerLevel, Map<BlockPos, HumanoidDoors.Opened>> entry = iterator.next();
                ServerLevel serverlevel = entry.getKey();
                if (serverlevel.getServer() == event.getServer()) {
                    Iterator<Entry<BlockPos, HumanoidDoors.Opened>> iterator1 = entry.getValue().entrySet().iterator();

                    while (iterator1.hasNext()) {
                        Entry<BlockPos, HumanoidDoors.Opened> entry1 = iterator1.next();
                        BlockPos blockpos = entry1.getKey();
                        if (!serverlevel.hasChunkAt(blockpos)) {
                            iterator1.remove();
                        } else {
                            BlockState blockstate = serverlevel.getBlockState(blockpos);
                            if (blockstate.getBlock() == entry1.getValue().block()
                                && (Boolean)blockstate.getValue(DoorBlock.OPEN)
                                && !(Boolean)blockstate.getValue(DoorBlock.POWERED)) {
                                if (serverlevel.getGameTime() - entry1.getValue().since() >= 20L) {
                                    AABB aabb = new AABB(blockpos).expandTowards(0.0, 1.0, 0.0).inflate(0.65, 0.0, 0.65);
                                    if (serverlevel.getEntitiesOfClass(LivingEntity.class, aabb, e -> e.isAlive()).isEmpty()) {
                                        entry1.getValue().block().setOpen(null, serverlevel, blockstate, blockpos, false);
                                        iterator1.remove();
                                    }
                                }
                            } else {
                                iterator1.remove();
                            }
                        }
                    }

                    if (entry.getValue().isEmpty()) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private static record Opened(DoorBlock block, long since) {
    }

    private static record Passage(BlockPos first, BlockPos second, double x, double z, Direction facing) {
    }
}
