package com.example.dragonmaid.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.example.dragonmaid.compat.RO;
import com.example.dragonmaid.compat.Reg;

public final class ModParticles {
    public static final Reg<ParticleType<?>> TYPES = Reg.create(BuiltInRegistries.PARTICLE_TYPE, "dragonmaid");
    public static final RO<SimpleParticleType> FIRE_BREATH = TYPES.register("fire_breath", () -> new SimpleParticleType(false));
    public static final RO<SimpleParticleType> FIRE_CORE = TYPES.register("fire_core", () -> new SimpleParticleType(false));
    public static final RO<SimpleParticleType> FIRE_SPARK = TYPES.register("fire_spark", () -> new SimpleParticleType(false));
    public static final RO<SimpleParticleType> ICE_BREATH = TYPES.register("ice_breath", () -> new SimpleParticleType(false));
    public static final RO<SimpleParticleType> FIRE_EMBER = TYPES.register("fire_ember", () -> new SimpleParticleType(false));
    public static final RO<SimpleParticleType> FIRE_SMOKE = TYPES.register("fire_smoke", () -> new SimpleParticleType(false));
    public static final RO<SimpleParticleType> ICE_MIST = TYPES.register("ice_mist", () -> new SimpleParticleType(false));
    public static final RO<SimpleParticleType> ICE_SHARD = TYPES.register("ice_shard", () -> new SimpleParticleType(false));
    public static final RO<SimpleParticleType> FIRE_WISP = TYPES.register("fire_wisp", () -> new SimpleParticleType(false));
    public static final RO<SimpleParticleType> FIRE_IMPACT = TYPES.register("fire_impact", () -> new SimpleParticleType(false));
    public static final RO<SimpleParticleType> ICE_IMPACT = TYPES.register("ice_impact", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
