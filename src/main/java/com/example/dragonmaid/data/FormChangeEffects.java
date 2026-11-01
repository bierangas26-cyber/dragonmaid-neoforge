package com.example.dragonmaid.data;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

final class FormChangeEffects {
    private FormChangeEffects() {
    }

    static void emit(DragonBaseEntity dragon) {
        if (dragon.level() instanceof ServerLevel serverlevel) {
            boolean flag1 = dragon instanceof FireDragonEntity;
            boolean ice = dragon instanceof IceDragonEntity;
            double height = (double)Mth.clamp(dragon.getBbHeight(), 1.2F, 4.0F);
            double radius = (double)Mth.clamp(dragon.getBbWidth() * 0.45F, 0.45F, 1.8F);
            SimpleParticleType simpleparticletype = flag1 ? ParticleTypes.FLAME : (ice ? ParticleTypes.SNOWFLAKE : ParticleTypes.ELECTRIC_SPARK);
            SimpleParticleType simpleparticletype1 = flag1 ? ParticleTypes.LAVA : ParticleTypes.END_ROD;
            SimpleParticleType simpleparticletype2 = flag1 ? ParticleTypes.SMOKE : (ice ? ParticleTypes.CLOUD : ParticleTypes.ELECTRIC_SPARK);
            serverlevel.sendParticles(
                simpleparticletype, dragon.getX(), dragon.getY() + height * 0.5, dragon.getZ(), 32, radius, height * 0.3, radius, 0.045
            );
            serverlevel.sendParticles(
                simpleparticletype1,
                dragon.getX(),
                dragon.getY() + height * 0.45,
                dragon.getZ(),
                10,
                radius * 0.75,
                height * 0.25,
                radius * 0.75,
                0.025
            );
            serverlevel.sendParticles(
                simpleparticletype2, dragon.getX(), dragon.getY() + height * 0.4, dragon.getZ(), 6, radius * 0.8, height * 0.2, radius * 0.8, 0.015
            );
        }
    }
}
