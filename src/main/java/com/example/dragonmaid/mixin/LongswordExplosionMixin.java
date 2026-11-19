package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.BladeCombat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.2.7: 长剑格挡可以把爆炸伤害按"爆心"重新算（近距离挡下）。
 *
 * <p>1.21.1 的 {@code Explosion} 没有 {@code getPosition()}，爆心是三个私有字段
 * {@code x / y / z}，这里直接 shadow 出来。</p>
 */
@Mixin({Explosion.class})
public abstract class LongswordExplosionMixin {
    @Shadow
    @Final
    private double x;
    @Shadow
    @Final
    private double y;
    @Shadow
    @Final
    private double z;

    @Redirect(
        method = {"explode"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
        )
    )
    private boolean dragonmaid$guardFromBlastCenter(Entity entity, DamageSource source, float amount) {
        return BladeCombat.hurtFromExplosion(entity, source, amount, new Vec3(this.x, this.y, this.z));
    }
}
