package com.example.dragonmaid.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class TailSweepKnockback {
    public static final float MULTIPLIER = 0.55F;
    private static final ThreadLocal<TailSweepKnockback.Attempt> ATTEMPT = new ThreadLocal<>();

    private TailSweepKnockback() {
    }

    public static boolean hurt(LivingEntity victim, DamageSource source, float damage) {
        TailSweepKnockback.Attempt tailsweepknockback$attempt = ATTEMPT.get();
        ATTEMPT.set(new TailSweepKnockback.Attempt(victim));

        boolean flag;
        try {
            flag = victim.hurt(source, damage);
        } finally {
            if (tailsweepknockback$attempt == null) {
                ATTEMPT.remove();
            } else {
                ATTEMPT.set(tailsweepknockback$attempt);
            }
        }

        return flag;
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void knockback(LivingKnockBackEvent event) {
        TailSweepKnockback.Attempt tailsweepknockback$attempt = ATTEMPT.get();
        if (tailsweepknockback$attempt != null && !tailsweepknockback$attempt.adjusted && event.getEntity() == tailsweepknockback$attempt.victim) {
            tailsweepknockback$attempt.adjusted = true;
            event.setStrength(event.getStrength() * 0.55F);
        }
    }

    private static final class Attempt {
        final LivingEntity victim;
        boolean adjusted;

        Attempt(LivingEntity victim) {
            this.victim = victim;
        }
    }
}
