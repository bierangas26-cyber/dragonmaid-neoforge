package com.example.dragonmaid.block;

import com.example.dragonmaid.recall.RecallContent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.2.7: the recall altar gained a floating gem block entity (client-side hover + player tracking).
 * Ported from 1.2.7; only container-type wiring changed (NeoForge DeferredRegister via compat/RO).
 */
public final class RecallAltarBlockEntity extends BlockEntity {
    private static final float GEM_HOVER_HEIGHT = 1.0F;
    private final AABB renderBounds;
    private int time;
    private float previousYaw;
    private float yaw;
    private float targetYaw;

    public RecallAltarBlockEntity(BlockPos pos, BlockState state) {
        super(RecallContent.altarEntity().get(), pos, state);
        this.renderBounds = new AABB(pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).move(0.0, 0.25, 0.0);
    }

    /**
     * 1.21.1/NeoForge port note: {@code BlockEntity#getRenderBoundingBox()} no longer exists on the
     * base class — the renderer owns {@code BlockEntityRenderer#getRenderBoundingBox(T)} — so the
     * bounds are exposed here and the renderer can forward to them.
     */
    public AABB renderBounds() {
        return this.renderBounds;
    }

    void animate(Level level) {
        Vec3 center = this.getBlockPos().getCenter();
        Player player = level.getNearestPlayer(center.x, center.y, center.z, 3.0, false);
        this.previousYaw = this.yaw;
        this.targetYaw = player == null
            ? wrapRadians(this.targetYaw + 0.02F)
            : (float) Mth.atan2(player.getZ() - center.z, player.getX() - center.x);
        this.yaw = wrapRadians(this.yaw + wrapRadians(this.targetYaw - this.yaw) * 0.4F);
        this.time++;
    }

    public float gemYaw(float partialTick) {
        return this.previousYaw + wrapRadians(this.yaw - this.previousYaw) * partialTick;
    }

    public float gemHeight(float partialTick) {
        return GEM_HOVER_HEIGHT + Mth.sin(((float) this.time + partialTick) * 0.1F) * 0.01F;
    }

    private static float wrapRadians(float angle) {
        if (angle >= (float) Math.PI) {
            return angle - (float) (Math.PI * 2);
        } else {
            return angle < (float) -Math.PI ? angle + (float) (Math.PI * 2) : angle;
        }
    }
}
