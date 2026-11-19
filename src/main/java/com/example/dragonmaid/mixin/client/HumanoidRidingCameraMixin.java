package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.HumanoidRidingPose;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2.7: 被龙抱着时把相机抬到"抱人姿态"的眼睛高度。
 * 1.21.1 的 {@code Camera.setup(BlockGetter, Entity, boolean, boolean, float)} 与
 * {@code protected void setPosition(Vec3)} 均与 1.20.1 一致。
 */
@Mixin({Camera.class})
abstract class HumanoidRidingCameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = {"setup"}, at = {@At("TAIL")})
    private void dragonmaid$carrierEyeLevel(BlockGetter level, Entity entity, boolean detached, boolean inverse, float partial, CallbackInfo ci) {
        if (!detached && entity instanceof AbstractClientPlayer player) {
            Vec3 eyes = HumanoidRidingPose.carrierEyes(player, partial);
            if (eyes != null) {
                this.setPosition(eyes);
            }
        }
    }
}
