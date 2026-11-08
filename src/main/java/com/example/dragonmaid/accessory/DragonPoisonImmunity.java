package com.example.dragonmaid.accessory;

import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * 1.2: 火龙佩戴 fire_fang 饰品时免疫中毒。
 * Forge 的 MobEffectEvent.Applicable + Event.Result.DENY 在 NeoForge 1.21.1 改为
 * MobEffectEvent.Applicable.Result.DENY（独立嵌套枚举）。
 */
@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class DragonPoisonImmunity {
    private DragonPoisonImmunity() {
    }

    @SubscribeEvent
    public static void onApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(MobEffects.POISON)
            && event.getEntity() instanceof FireDragonEntity dragon
            && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.FIRE_FANG)) {
            // NeoForge 1.21.1 的三态枚举是 APPLY / DEFAULT / DO_NOT_APPLY（不再叫 DENY）
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
