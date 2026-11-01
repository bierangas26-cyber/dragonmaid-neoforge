package com.example.dragonmaid.advancement;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.advancements.AdvancementHolder;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class HumanoidCompanions {
    private static final Map<MinecraftServer, Set<UUID>> PENDING = new WeakHashMap<>();

    private HumanoidCompanions() {
    }

    public static void check(ServerPlayer player) {
        AdvancementHolder advancement = player.server.getAdvancements().get(DragonAdvancements.TRINITY_OF_DRAGONS);
        if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            int i = 0;

            for (ServerLevel serverlevel : player.server.getAllLevels()) {
                for (Entity entity : serverlevel.getAllEntities()) {
                    if (entity instanceof DragonBaseEntity) {
                        DragonBaseEntity entitydragonbase = (DragonBaseEntity)entity;
                        if (player.getUUID().equals(entitydragonbase.getOwnerUUID()) && eligible(entitydragonbase)) {
                            if (entitydragonbase instanceof FireDragonEntity) {
                                i |= 1;
                            } else if (entitydragonbase instanceof IceDragonEntity) {
                                i |= 2;
                            } else if (entitydragonbase instanceof LightningDragonEntity) {
                                i |= 4;
                            }

                            if (i == 7) {
                                DragonAdvancements.award(player, DragonAdvancements.TRINITY_OF_DRAGONS);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean eligible(DragonBaseEntity dragon) {
        return !dragon.isRemoved()
            && dragon.isTame()
            && dragon.getOwnerUUID() != null
            && DragonForms.isHumanoid(dragon)
            && !DragonForms.state(dragon).dragonmaid$isExecuted()
            && (HumanoidLife.isInjured(dragon) || dragon.isAlive() && !dragon.isModelDead());
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel serverlevel && event.getEntity() instanceof DragonBaseEntity entitydragonbase && eligible(entitydragonbase)
            )
         {
            synchronized (PENDING) {
                PENDING.computeIfAbsent(serverlevel.getServer(), ignored -> new HashSet<>()).add(entitydragonbase.getOwnerUUID());
                return;
            }
        }
    }

    @SubscribeEvent
    public static void finishJoins(ServerTickEvent.Post event) {
        if (true) {
            Set<UUID> set;
            synchronized (PENDING) {
                set = PENDING.remove(event.getServer());
            }

            if (set != null) {
                for (UUID uuid : set) {
                    ServerPlayer serverplayer = event.getServer().getPlayerList().getPlayer(uuid);
                    if (serverplayer != null) {
                        check(serverplayer);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        synchronized (PENDING) {
            PENDING.remove(event.getServer());
        }
    }
}
