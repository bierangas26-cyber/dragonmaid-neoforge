package com.example.dragonmaid.recall;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityEvent.EnteringSection;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class RecallTracking {
    public static final String GENERATION = "DragonMaidRecallGeneration";
    public static final String TOKEN = "DragonMaidRecallToken";
    public static final String DRAGON = "DragonMaidRecallDragon";
    private static final Map<MinecraftServer, Map<UUID, DragonBaseEntity>> DIRTY = new WeakHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Entity>> JOIN_RECOVERY = new WeakHashMap<>();

    private RecallTracking() {
    }

    public static boolean supported(Entity entity) {
        return entity instanceof DragonBaseEntity
            && (
                entity.getType() == IafEntities.FIRE_DRAGON.get()
                    || entity.getType() == IafEntities.ICE_DRAGON.get()
                    || entity.getType() == IafEntities.LIGHTNING_DRAGON.get()
            );
    }

    public static void stamp(Entity entity, RecallLedger.Entry entry) {
        stampData(entity.getPersistentData(), entry);
    }

    public static void stamp(CompoundTag snapshot, RecallLedger.Entry entry) {
        CompoundTag compoundtag = snapshot.getCompound("ForgeData");
        stampData(compoundtag, entry);
        snapshot.put("ForgeData", compoundtag);
    }

    private static void stampData(CompoundTag data, RecallLedger.Entry entry) {
        data.putLong("DragonMaidRecallGeneration", entry.generation());
        data.putUUID("DragonMaidRecallToken", entry.token());
        data.putUUID("DragonMaidRecallDragon", entry.dragon());
    }

    public static boolean matches(Entity entity, RecallLedger.Entry entry) {
        return matchesData(entity.getPersistentData(), entry);
    }

    public static boolean matches(CompoundTag snapshot, RecallLedger.Entry entry) {
        return matchesData(snapshot.getCompound("ForgeData"), entry);
    }

    private static boolean matchesData(CompoundTag data, RecallLedger.Entry entry) {
        return data.hasUUID("DragonMaidRecallToken")
            && data.getUUID("DragonMaidRecallToken").equals(entry.token())
            && data.getLong("DragonMaidRecallGeneration") == entry.generation()
            && data.hasUUID("DragonMaidRecallDragon")
            && data.getUUID("DragonMaidRecallDragon").equals(entry.dragon());
    }

    public static boolean authorized(Entity entity, UUID dragon) {
        if (!(entity.level() instanceof ServerLevel serverlevel)) {
            return true;
        } else {
            RecallLedger.Entry recallledger$entry = RecallLedger.get(serverlevel).entry(dragon);
            return recallledger$entry == null || recallledger$entry.state() != RecallLedger.State.DEAD && matches(entity, recallledger$entry);
        }
    }

    public static RecallLedger.Entry observe(DragonBaseEntity dragon) {
        return !dragon.isRemoved() && dragon.level() instanceof ServerLevel serverlevel && serverlevel.getEntity(dragon.getUUID()) == dragon
            ? RecallLedger.get(serverlevel).observe(dragon)
            : null;
    }

    public static void changed(DragonBaseEntity dragon) {
        if (dragon.level() instanceof ServerLevel serverlevel && supported(dragon)) {
            DIRTY.computeIfAbsent(serverlevel.getServer(), ignored -> new HashMap<>()).put(dragon.getUUID(), dragon);
        }
    }

    public static void retryCommit(Entity entity, UUID dragon) {
        if (entity.level() instanceof ServerLevel serverlevel) {
            RecallLedger.Transfer recallledger$transfer = RecallLedger.get(serverlevel).pending(dragon);
            if (recallledger$transfer != null && matches(entity, recallledger$transfer.after())) {
                JOIN_RECOVERY.computeIfAbsent(serverlevel.getServer(), ignored -> new HashMap<>()).put(dragon, entity);
            }
        }
    }

    public static void executed(DragonBaseEntity dragon) {
        if (dragon.level() instanceof ServerLevel serverlevel && supported(dragon)) {
            RecallLedger recallledger = RecallLedger.get(serverlevel);
            recallledger.observe(dragon);
            recallledger.markDead(dragon.getUUID());
            return;
        }
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel serverlevel
            && event.getEntity() instanceof DragonBaseEntity entitydragonbase
            && supported(entitydragonbase)
            && entitydragonbase.getOwnerUUID() != null) {
            RecallLedger recallledger = RecallLedger.get(serverlevel);
            RecallLedger.Entry recallledger$entry = recallledger.entry(entitydragonbase.getUUID());
            if (RecallDragonPetrification.isRestoring(entitydragonbase)) {
                return;
            }

            if (recallledger$entry != null
                && recallledger$entry.state() == RecallLedger.State.WORLD
                && recallledger$entry.generation() == 0L
                && !entitydragonbase.getPersistentData().hasUUID("DragonMaidRecallToken")) {
                stamp(entitydragonbase, recallledger$entry);
            }

            if (recallledger$entry == null || recallledger$entry.state() == RecallLedger.State.WORLD && matches(entitydragonbase, recallledger$entry)) {
                recallledger.observe(entitydragonbase);
                RecallLedger.Transfer recallledger$transfer = recallledger.pending(entitydragonbase.getUUID());
                if (recallledger$transfer != null
                    && !recallledger.isReserved(entitydragonbase.getUUID())
                    && matches(entitydragonbase, recallledger$transfer.after())) {
                    JOIN_RECOVERY.computeIfAbsent(serverlevel.getServer(), ignored -> new HashMap<>()).put(entitydragonbase.getUUID(), entitydragonbase);
                }

                return;
            }

            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public static void section(EnteringSection event) {
        if (event.didChunkChange() && event.getEntity() instanceof DragonBaseEntity entitydragonbase) {
            changed(entitydragonbase);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof DragonBaseEntity entitydragonbase && event.getLevel() instanceof ServerLevel) {
            observe(entitydragonbase);
        }
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void death(LivingDeathEvent event) {
        if (!event.isCanceled()
            && event.getEntity() instanceof DragonBaseEntity entitydragonbase
            && supported(entitydragonbase)
            && entitydragonbase.getOwnerUUID() != null
            && !HumanoidLife.isInjured(entitydragonbase)) {
            executed(entitydragonbase);
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (true) {
            Map<UUID, Entity> map = JOIN_RECOVERY.get(event.getServer());
            if (map != null && event.getServer().getTickCount() % 20 == 0) {
                map.entrySet().removeIf(task -> {
                    Entity entity = task.getValue();
                    UUID uuid = task.getKey();
                    if (entity.level() instanceof ServerLevel serverlevel1 && !entity.isRemoved()) {
                        RecallLedger recallledger = RecallLedger.get(serverlevel1);
                        RecallLedger.Transfer recallledger$transfer = recallledger.pending(uuid);
                        if (recallledger$transfer == null) {
                            return true;
                        }

                        if (recallledger.isReserved(uuid)) {
                            return false;
                        }

                        if (!matches(entity, recallledger$transfer.after())) {
                            return true;
                        }

                        if (!RecallService.safeToFinalizeRecovery(event.getServer(), recallledger$transfer)) {
                            return false;
                        }

                        try {
                            recallledger.commitTransfer(recallledger$transfer);
                            if (entity instanceof DragonBaseEntity entitydragonbase1) {
                                recallledger.observe(entitydragonbase1);
                            } else if (entity instanceof StoneStatueEntity entitystonestatue) {
                                RecallDragonPetrification.prepareRecalledStatue(entitystonestatue);
                            }

                            return true;
                        } catch (RuntimeException runtimeexception) {
                            return false;
                        }
                    }

                    return true;
                });
            }

            Map<UUID, DragonBaseEntity> map1 = DIRTY.remove(event.getServer());
            if (map1 != null) {
                for (DragonBaseEntity entitydragonbase : map1.values()) {
                    Level level1 = entitydragonbase.level();
                    if (level1 instanceof ServerLevel) {
                        ServerLevel serverlevel = (ServerLevel)level1;
                        if (serverlevel.getEntity(entitydragonbase.getUUID()) == entitydragonbase) {
                            RecallLedger.get(serverlevel).observe(entitydragonbase);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        DIRTY.remove(event.getServer());
        JOIN_RECOVERY.remove(event.getServer());
    }

    public static boolean rescueVoid(DragonBaseEntity dragon) {
        if (dragon.level() instanceof ServerLevel serverlevel
            && supported(dragon)
            && dragon.getOwnerUUID() != null
            && !DragonForms.state(dragon).dragonmaid$isExecuted()
            && (dragon.isAlive() || HumanoidLife.isInjured(dragon))) {
            RecallLedger recallledger = RecallLedger.get(serverlevel);
            if (!recallledger.reserve(dragon.getUUID())) {
                return true;
            }

            RecallLedger.Transfer recallledger$transfer = null;

            boolean flag;
            try {
                if (!HumanoidLife.isInjured(dragon)) {
                    dragon.setHealth(0.0F);
                    dragon.setModelDead(true);
                }

                dragon.ejectPassengers();
                dragon.stopRiding();
                dragon.setTarget(null);
                dragon.getNavigation().stop();
                recallledger.observe(dragon);
                CompoundTag compoundtag = new CompoundTag();
                if (dragon.save(compoundtag)) {
                    recallledger$transfer = recallledger.prepareTransfer(
                        dragon.getUUID(),
                        RecallLedger.State.VOID,
                        serverlevel.dimension().location().toString(),
                        dragon.blockPosition(),
                        dragon.getUUID(),
                        compoundtag
                    );
                    dragon.discard();
                    if (!dragon.isRemoved()) {
                        throw new IllegalStateException("Void source was not removed");
                    }

                    recallledger.commitTransfer(recallledger$transfer);
                    return true;
                }

                flag = true;
            } catch (RuntimeException runtimeexception1) {
                if (recallledger$transfer != null && recallledger.pending(dragon.getUUID()) != null) {
                    try {
                        if (dragon.isRemoved()) {
                            recallledger.commitTransfer(recallledger$transfer);
                        } else {
                            recallledger.rollbackTransfer(recallledger$transfer);
                        }
                    } catch (RuntimeException runtimeexception) {
                        runtimeexception1.addSuppressed(runtimeexception);
                    }
                }

                LogUtils.getLogger().error("Void rescue could not finish; dragon or pending snapshot retained", runtimeexception1);
                return true;
            } finally {
                recallledger.release(dragon.getUUID());
            }

            return flag;
        }

        return false;
    }
}
