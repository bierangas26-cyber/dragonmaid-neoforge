package com.example.dragonmaid.movement;

import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public final class HumanoidMoveControl extends MoveControl {
    private final DragonBaseEntity dragon;
    private boolean wasInWater;

    public HumanoidMoveControl(DragonBaseEntity dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    public boolean steering() {
        return this.operation == MoveControl.Operation.MOVE_TO
            && !this.dragon.getNavigation().isDone()
            && this.dragon.canMove()
            && !this.dragon.isNoAi()
            && HumanoidWater.active(this.dragon);
    }

    public void tick() {
        if (!this.dragon.isInWater()) {
            if (this.wasInWater) {
                this.dragon.setXRot(0.0F);
            }

            this.wasInWater = false;
            super.tick();
        } else {
            this.wasInWater = true;
            this.dragon.setXxa(0.0F);
            this.dragon.setYya(0.0F);
            if (!this.steering()) {
                this.dragon.setSpeed(0.0F);
                this.dragon.setZza(0.0F);
                this.dragon.setXRot(this.rotlerp(this.dragon.getXRot(), 0.0F, 5.0F));
            } else {
                double d0 = this.wantedX - this.dragon.getX();
                double d1 = this.wantedZ - this.dragon.getZ();
                double d2 = Math.sqrt(d0 * d0 + d1 * d1);
                if (d2 > 0.05) {
                    float f = (float)(Mth.atan2(d1, d0) * 180.0F / (float)Math.PI) - 90.0F;
                    this.dragon.setYRot(this.rotlerp(this.dragon.getYRot(), f, 30.0F));
                    this.dragon.yBodyRot = this.dragon.getYRot();
                }

                this.dragon.setSpeed((float)Math.min(0.35, this.speedModifier * this.dragon.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                this.dragon.setZza(d2 > 0.05 ? 1.0F : 0.0F);
                float f1 = HumanoidWater.canDive(this.dragon) && HumanoidWater.submerged(this.dragon)
                    ? (float)(-Math.toDegrees(Math.atan2(this.wantedY - this.dragon.getY(), Math.max(d2, 0.1))))
                    : 0.0F;
                this.dragon.setXRot(this.rotlerp(this.dragon.getXRot(), Mth.clamp(f1, -60.0F, 60.0F), 5.0F));
            }
        }
    }
}
