package com.example.dragonmaid.compat;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

/**
 * 人形近战动画事件（1.2.0 新增，供外部兼容 mod 监听）。
 *
 * <p>上游 1.2.0-beta.4 只有事件类本身，mod 内没有 post 点；这里保持同样的“纯 API 事件”语义。</p>
 */
public final class HumanoidMeleeAnimationEvent extends Event {
    private final DragonBaseEntity dragon;
    private final LivingEntity target;
    private final int cooldownTicks;

    public HumanoidMeleeAnimationEvent(DragonBaseEntity dragon, LivingEntity target, int cooldownTicks) {
        this.dragon = dragon;
        this.target = target;
        this.cooldownTicks = cooldownTicks;
    }

    public DragonBaseEntity getDragon() {
        return this.dragon;
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }
}
