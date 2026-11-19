package com.example.dragonmaid.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.2.7: 读取 {@code LivingEntity#jumping}，用于判断骑手是否在按跳跃（骑乘直击/起飞）。
 */
@Mixin({LivingEntity.class})
public interface RiderJumpAccessor {
    @Accessor("jumping")
    boolean dragonmaid$riderJumping();
}
