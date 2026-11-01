package com.example.dragonmaid.petrification;

import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class OwnedStoneProtection {
    private OwnedStoneProtection() {
    }

    public static boolean protects(StoneStatueEntity statue) {
        EntityType<?> entitytype = statue.getTrappedEntityType();
        if (entitytype != IafEntities.FIRE_DRAGON.get()
            && entitytype != IafEntities.ICE_DRAGON.get()
            && entitytype != IafEntities.LIGHTNING_DRAGON.get()) {
            return false;
        } else {
            CompoundTag compoundtag = statue.getTrappedTag();
            return hasOwner(compoundtag, "Owner") || hasOwner(compoundtag, "OwnerUUID");
        }
    }

    private static boolean hasOwner(CompoundTag data, String key) {
        return data.hasUUID(key) || data.contains(key, 8) && !data.getString(key).isBlank();
    }

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void preventMining(AttackEntityEvent event) {
        if (event.getTarget() instanceof StoneStatueEntity entitystonestatue && protects(entitystonestatue)) {
            event.setCanceled(true);
        }
    }
}
