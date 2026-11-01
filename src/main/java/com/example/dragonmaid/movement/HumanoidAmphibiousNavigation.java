package com.example.dragonmaid.movement;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class HumanoidAmphibiousNavigation extends AmphibiousPathNavigation {
    private final HumanoidDoors doors = new HumanoidDoors();

    public HumanoidAmphibiousNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    protected PathFinder createPathFinder(int maxNodes) {
        this.nodeEvaluator = new AmphibiousNodeEvaluator(false) {
            public PathType getPathType(PathfindingContext context, int x, int y, int z) {
                BlockGetter world = context.level();
                PathType blockpathtypes = super.getPathType(context, x, y, z);
                return HumanoidHazards.pathType(
                    HumanoidAmphibiousNavigation.this.mob,
                    world,
                    x,
                    y,
                    z,
                    HumanoidDoors.pathType(HumanoidAmphibiousNavigation.this.mob, world, new BlockPos(x, y, z), blockpathtypes)
                );
            }
        };
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxNodes);
    }

    protected Vec3 getTempMobPos() {
        return this.mob.position();
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
