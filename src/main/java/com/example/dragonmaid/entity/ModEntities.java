package com.example.dragonmaid.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.example.dragonmaid.compat.RO;
import com.example.dragonmaid.compat.Reg;
import com.example.dragonmaid.combat.MiniatureLightningCharge;

public final class ModEntities {
    private static final Reg<EntityType<?>> TYPES = Reg.create(BuiltInRegistries.ENTITY_TYPE, "dragonmaid");
    public static final RO<EntityType<ReinforcedHornItemEntity>> REINFORCED_HORN = TYPES.register(
        "reinforced_dragon_horn",
        () -> Builder.of(ReinforcedHornItemEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .fireImmune()
                .clientTrackingRange(6)
                .updateInterval(20)
                .build("dragonmaid:reinforced_dragon_horn")
    );

    public static final RO<EntityType<MiniatureLightningCharge>> MINI_LIGHTNING = TYPES.register(
        "mini_lightning",
        () -> Builder.of(MiniatureLightningCharge::new, MobCategory.MISC)
                .sized(0.28F, 0.28F)
                .clientTrackingRange(10)
                .updateInterval(2)
                .build("dragonmaid:mini_lightning")
    );

    /** 1.2.7: 龙血三叉戟的飞行投射物（ThrownTrident 子类，服务端权威，port-combat 提供实体类）。 */
    public static final RO<EntityType<DragonTridentFlight>> DRAGON_TRIDENT = TYPES.register(
        "dragon_trident_flight",
        () -> Builder.of(DragonTridentFlight::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .noSummon()
                .clientTrackingRange(8)
                .updateInterval(1)
                .build("dragonmaid:dragon_trident_flight")
    );

    private ModEntities() {
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
