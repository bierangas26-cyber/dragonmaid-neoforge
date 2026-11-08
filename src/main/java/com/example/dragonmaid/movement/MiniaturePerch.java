package com.example.dragonmaid.movement;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** 1.2: 迷你龙坐在玩家肩上时的位置计算（第 1/2 只在两侧，第 3 只靠后偏高）。 */
public final class MiniaturePerch {
    private MiniaturePerch() {
    }

    public static void position(Player owner, DragonBaseEntity dragon) {
        if (MiniatureForm.isSmall(dragon) && dragon.getVehicle() == owner) {
            int index = 0;

            for (Entity passenger : owner.getPassengers()) {
                if (passenger == dragon) {
                    break;
                }

                if (passenger instanceof DragonBaseEntity) {
                    index++;
                }
            }

            float yaw = owner.yBodyRot * (float) (Math.PI / 180.0);
            double side = index == 0 ? 0.48 : (index == 1 ? -0.48 : 0.0);
            double back = index < 2 ? 0.05 : 0.36;
            double height = owner.isCrouching() ? 1.18 : 1.4;
            if (index == 2) {
                height += 0.15;
            }

            dragon.setPos(
                owner.getX() + (double)Mth.cos(yaw) * side + (double)Mth.sin(yaw) * back,
                owner.getY() + height,
                owner.getZ() + (double)Mth.sin(yaw) * side - (double)Mth.cos(yaw) * back
            );
            dragon.setYBodyRot(owner.getYHeadRot());
            dragon.setXRot(0.0F);
        }
    }
}
