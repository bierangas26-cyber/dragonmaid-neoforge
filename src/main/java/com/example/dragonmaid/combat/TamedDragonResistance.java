package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.compat.CompanionRelations;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafDamageTypes;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class TamedDragonResistance {
    private static final UUID NIL_UUID = new UUID(0L, 0L);

    private TamedDragonResistance() {
    }

    @SubscribeEvent(
        priority = EventPriority.LOW
    )
    public static void hurt(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof DragonBaseEntity entitydragonbase
            && entitydragonbase.level() instanceof ServerLevel
            && !(event.getNewDamage() <= 0.0F)
            && Float.isFinite(event.getNewDamage())) {
            double d0 = multiplier(entitydragonbase, event.getSource());
            if (d0 < 1.0) {
                event.setNewDamage((float)((double)event.getNewDamage() * d0));
            }

            return;
        }
    }

    public static double multiplier(DragonBaseEntity defender, DamageSource source) {
        UUID uuid = defender.getOwnerUUID();
        if (!defender.isTame()
            || uuid == null
            || NIL_UUID.equals(uuid)
            || !(source.getEntity() instanceof DragonBaseEntity entitydragonbase)
            || entitydragonbase.isTame()
            || entitydragonbase.getOwnerUUID() != null
            || entitydragonbase.level() != defender.level()
            || CompanionRelations.isAlly(defender, entitydragonbase)) {
            return 1.0;
        } else if (!source.is(IafDamageTypes.DRAGON_FIRE_TYPE)
            && !source.is(IafDamageTypes.DRAGON_ICE_TYPE)
            && !source.is(IafDamageTypes.DRAGON_LIGHTNING_TYPE)) {
            return source.is(DamageTypes.MOB_ATTACK) ? (Double)DragonMaidConfig.WILD_DRAGON_MELEE_DAMAGE_MULTIPLIER.get() : 1.0;
        } else {
            return (Double)DragonMaidConfig.WILD_DRAGON_BREATH_DAMAGE_MULTIPLIER.get();
        }
    }
}
