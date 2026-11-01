package com.example.dragonmaid.movement;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.Nullable;

public final class HumanoidGroundNavigation extends GroundPathNavigation {
    private final HumanoidDoors doors = new HumanoidDoors();

    public HumanoidGroundNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    protected PathFinder createPathFinder(int maxNodes) {
        this.nodeEvaluator = new WalkNodeEvaluator() {
            public PathType getPathType(PathfindingContext context, int x, int y, int z) {
                BlockGetter world = context.level();
                PathType blockpathtypes = super.getPathType(context, x, y, z);
                return HumanoidHazards.pathType(
                    HumanoidGroundNavigation.this.mob,
                    world,
                    x,
                    y,
                    z,
                    HumanoidDoors.pathType(HumanoidGroundNavigation.this.mob, world, new BlockPos(x, y, z), blockpathtypes)
                );
            }
        };
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxNodes);
    }

    @Nullable
    protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean includeEntity, int accuracy, float range) {
        this.nodeEvaluator.setCanOpenDoors(HumanoidDoors.canOpen(this.mob));
        return super.createPath(targets, regionOffset, includeEntity, accuracy, range);
    }

    public void tick() {
        this.nodeEvaluator.setCanOpenDoors(HumanoidDoors.canOpen(this.mob));
        this.doors.beforeNavigation(this.mob, this);
        super.tick();
        this.doors.afterNavigation(this.mob, this, this.speedModifier);
    }
}
