package com.example.dragonmaid.recall;

import com.example.dragonmaid.combat.HumanoidCombat;
import com.example.dragonmaid.combat.TailSweepSync;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.petrification.PetrificationLedger;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import com.iafenvoy.iceandfire.world.DragonPosWorldData;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

@EventBusSubscriber(modid = "dragonmaid")
public final class RecallService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation ALTAR = ResourceLocation.fromNamespaceAndPath("dragonmaid", "dragon_recall_altar");
    private static final TicketType<UUID> LOAD_TICKET = TicketType.create("dragonmaid_recall", Comparator.<UUID>naturalOrder());
    private static final Map<UUID, RecallService.Pending> WAITING = new HashMap<>();
    private static BiConsumer<ServerPlayer, RecallService.RequestResult> completion = (player, result) -> {
    };

    private static boolean hasWorldSource(RecallLedger.Transfer transfer) {
        return (transfer.before().state() == RecallLedger.State.WORLD || transfer.before().state() == RecallLedger.State.STONE)
            && (transfer.after().state() == RecallLedger.State.WORLD || transfer.after().state() == RecallLedger.State.STONE);
    }

    public static boolean safeToFinalizeRecovery(MinecraftServer server, RecallLedger.Transfer transfer) {
        if (!hasWorldSource(transfer)) {
            return true;
        } else {
            RecallLedger.Entry recallledger$entry = transfer.before();
            ResourceKey<Level> resourcekey = dimension(recallledger$entry.dimension());
            ServerLevel serverlevel = resourcekey == null ? null : server.getLevel(resourcekey);
            if (serverlevel != null && serverlevel.areEntitiesLoaded(new ChunkPos(recallledger$entry.pos()).toLong())) {
                UUID uuid = recallledger$entry.state() == RecallLedger.State.STONE ? recallledger$entry.holder() : recallledger$entry.dragon();
                if (uuid == null) {
                    return false;
                } else {
                    for (ServerLevel serverlevel1 : server.getAllLevels()) {
                        Entity entity = serverlevel1.getEntity(uuid);
                        if (entity != null && !entity.isRemoved() && !RecallTracking.matches(entity, transfer.after())) {
                            return false;
                        }
                    }

                    return true;
                }
            } else {
                return false;
            }
        }
    }

    private static RecallService.RecoverySafety resolveWorldRecovery(MinecraftServer server, RecallLedger ledger, RecallLedger.Transfer transfer) {
        if (!hasWorldSource(transfer)) {
            return RecallService.RecoverySafety.READY;
        } else {
            ResourceKey<Level> resourcekey = dimension(transfer.after().dimension());
            ServerLevel serverlevel = resourcekey == null ? null : server.getLevel(resourcekey);
            if (serverlevel != null && serverlevel.areEntitiesLoaded(new ChunkPos(transfer.after().pos()).toLong())) {
                Entity entity = findLoaded(server, transfer.before());
                if (entity == null) {
                    return safeToFinalizeRecovery(server, transfer) ? RecallService.RecoverySafety.READY : RecallService.RecoverySafety.WAIT;
                } else if (findLoaded(server, transfer.after()) != null) {
                    return RecallService.RecoverySafety.WAIT;
                } else {
                    ledger.rollbackTransfer(transfer);
                    return RecallService.RecoverySafety.ROLLED_BACK;
                }
            } else {
                return RecallService.RecoverySafety.WAIT;
            }
        }
    }

    private static RecallService.RecoverySafety resolveVoidRecovery(MinecraftServer server, RecallLedger ledger, RecallLedger.Transfer transfer) {
        Entity entity = findLoaded(server, transfer.before());
        if (entity != null) {
            ledger.rollbackTransfer(transfer);
            return RecallService.RecoverySafety.ROLLED_BACK;
        } else {
            ResourceKey<Level> resourcekey = dimension(transfer.before().dimension());
            ServerLevel serverlevel = resourcekey == null ? null : server.getLevel(resourcekey);
            if (serverlevel != null && serverlevel.areEntitiesLoaded(new ChunkPos(transfer.before().pos()).toLong())) {
                for (ServerLevel serverlevel1 : server.getAllLevels()) {
                    Entity entity1 = serverlevel1.getEntity(transfer.before().dragon());
                    if (entity1 != null && !entity1.isRemoved()) {
                        return RecallService.RecoverySafety.WAIT;
                    }
                }

                return RecallService.RecoverySafety.READY;
            } else {
                return RecallService.RecoverySafety.WAIT;
            }
        }
    }

    private RecallService() {
    }

    public static void setCompletionListener(BiConsumer<ServerPlayer, RecallService.RequestResult> listener) {
        completion = Objects.requireNonNull(listener);
    }

    public static void cancel(ServerPlayer player) {
        for (RecallService.Pending recallservice$pending : new ArrayList<>(WAITING.values())) {
            if (recallservice$pending.server() == player.server && recallservice$pending.player().equals(player.getUUID())) {
                finishPending(recallservice$pending);
            }
        }
    }

    public static List<RecallLedger.Entry> query(ServerPlayer player, int page, int pageSize) {
        int i = Math.max(1, Math.min(64, pageSize));
        List<RecallLedger.Entry> list = RecallLedger.get(player.serverLevel()).owned(player.getUUID());
        long j = (long)Math.max(0, page) * (long)i;
        return j >= (long)list.size() ? List.of() : List.copyOf(list.subList((int)j, Math.min(list.size(), (int)j + i)));
    }

    public static int quote(ServerPlayer player, BlockPos altar, UUID dragon) {
        return quoteDetails(player, altar, dragon).cost();
    }

    public static RecallQuote quoteDetails(ServerPlayer player, BlockPos altar, UUID dragon) {
        RecallLedger.Entry recallledger$entry = RecallLedger.get(player.serverLevel()).entry(dragon);
        if (!owned(player, recallledger$entry)) {
            return RecallQuote.unknown(-1);
        } else {
            Entity entity = findLoaded(player.server, recallledger$entry);
            ResourceKey<Level> resourcekey = entity == null ? dimension(recallledger$entry.dimension()) : entity.level().dimension();
            if (resourcekey == null) {
                return RecallQuote.unknown(RecallCosts.isFree(player) ? 0 : -1);
            } else {
                BlockPos blockpos = entity == null ? recallledger$entry.pos() : entity.blockPosition();
                int i = RecallCosts.isFree(player) ? 0 : RecallCosts.quote(player.serverLevel(), altar, resourcekey, blockpos);
                return RecallQuote.at(i, altar, player.serverLevel().dimension(), resourcekey, blockpos, entity == null);
            }
        }
    }

    public static RecallService.RequestResult request(ServerPlayer player, BlockPos altar, UUID dragon, int quotedCost) {
        RecallService.RequestResult recallservice$requestresult = checkAltar(player, player.serverLevel(), altar);
        if (recallservice$requestresult != null) {
            return recallservice$requestresult;
        } else {
            RecallLedger recallledger = RecallLedger.get(player.serverLevel());
            RecallLedger.Entry recallledger$entry = recallledger.entry(dragon);
            if (!owned(player, recallledger$entry)) {
                return fail("not_owned");
            } else if (!WAITING.containsKey(dragon) && !WAITING.values().stream().anyMatch(p -> p.player().equals(player.getUUID()))) {
                int i = quote(player, altar, dragon);
                if (i < 0) {
                    return fail("unavailable");
                } else if (i != quotedCost) {
                    return fail("price_changed", i);
                } else if (RecallCosts.availablePoints(player) < (long)i) {
                    return fail("insufficient_xp", i);
                } else {
                    RecallLedger.Transfer recallledger$transfer = recallledger.pending(dragon);
                    boolean flag = recallledger$transfer == null ? recallledger.reserve(dragon) : recallledger.reserveRecovery(dragon);
                    if (!flag) {
                        return fail("busy");
                    } else {
                        boolean flag1 = false;

                        try {
                            if (recallledger$transfer != null
                                && recallledger$entry.state() == RecallLedger.State.VOID
                                && recallledger$transfer.before().state() == RecallLedger.State.WORLD) {
                                RecallService.RecoverySafety recallservice$recoverysafety = resolveVoidRecovery(
                                    player.server, recallledger, recallledger$transfer
                                );
                                if (recallservice$recoverysafety == RecallService.RecoverySafety.WAIT) {
                                    ResourceKey<Level> resourcekey2 = dimension(recallledger$transfer.before().dimension());
                                    ServerLevel serverlevel4 = resourcekey2 == null ? null : player.server.getLevel(resourcekey2);
                                    if (serverlevel4 == null) {
                                        return fail("missing");
                                    }

                                    ChunkPos chunkpos3 = new ChunkPos(recallledger$transfer.before().pos());
                                    serverlevel4.getChunkSource().addRegionTicket(LOAD_TICKET, chunkpos3, 2, dragon);
                                    WAITING.put(
                                        dragon,
                                        new RecallService.Pending(
                                            player.server,
                                            player.getUUID(),
                                            player.level().dimension(),
                                            altar.immutable(),
                                            dragon,
                                            i,
                                            serverlevel4,
                                            chunkpos3,
                                            player.server.getTickCount() + (Integer)RecallConfig.LOAD_TIMEOUT_TICKS.get(),
                                            recallledger$transfer,
                                            null,
                                            null
                                        )
                                    );
                                    flag1 = true;
                                    return result(RecallService.Status.PENDING, "loading");
                                }

                                if (recallservice$recoverysafety == RecallService.RecoverySafety.READY) {
                                    recallledger.commitTransfer(recallledger$transfer);
                                }

                                recallledger$transfer = null;
                                recallledger$entry = recallledger.entry(dragon);
                            }

                            if (recallledger$transfer != null && hasWorldSource(recallledger$transfer)) {
                                RecallService.RecoverySafety recallservice$recoverysafety1 = resolveWorldRecovery(
                                    player.server, recallledger, recallledger$transfer
                                );
                                if (recallservice$recoverysafety1 != RecallService.RecoverySafety.ROLLED_BACK) {
                                    if (recallservice$recoverysafety1 != RecallService.RecoverySafety.WAIT) {
                                        Entity entity1 = findLoaded(player.server, recallledger$entry);
                                        if (entity1 != null) {
                                            recallledger.commitTransfer(recallledger$transfer);
                                            recallledger$entry = recallledger.entry(dragon);
                                            return execute(player, altar, recallledger$entry, entity1, i, null);
                                        }

                                        return execute(player, altar, recallledger$entry, null, i, recallledger$transfer);
                                    }

                                    ResourceKey<Level> resourcekey1 = dimension(recallledger$transfer.before().dimension());
                                    ServerLevel serverlevel3 = resourcekey1 == null ? null : player.server.getLevel(resourcekey1);
                                    if (serverlevel3 == null) {
                                        return fail("missing");
                                    }

                                    ChunkPos chunkpos2 = new ChunkPos(recallledger$transfer.before().pos());
                                    serverlevel3.getChunkSource().addRegionTicket(LOAD_TICKET, chunkpos2, 2, dragon);
                                    ResourceKey<Level> resourcekey3 = dimension(recallledger$transfer.after().dimension());
                                    ServerLevel serverlevel1 = resourcekey3 == null ? null : player.server.getLevel(resourcekey3);
                                    if (serverlevel1 != null) {
                                        ChunkPos chunkpos4 = new ChunkPos(recallledger$transfer.after().pos());
                                        if (serverlevel1 == serverlevel3 && chunkpos4.equals(chunkpos2)) {
                                            serverlevel1 = null;
                                            chunkpos4 = null;
                                        } else {
                                            serverlevel1.getChunkSource().addRegionTicket(LOAD_TICKET, chunkpos4, 2, dragon);
                                        }

                                        WAITING.put(
                                            dragon,
                                            new RecallService.Pending(
                                                player.server,
                                                player.getUUID(),
                                                player.level().dimension(),
                                                altar.immutable(),
                                                dragon,
                                                i,
                                                serverlevel3,
                                                chunkpos2,
                                                player.server.getTickCount() + (Integer)RecallConfig.LOAD_TIMEOUT_TICKS.get(),
                                                recallledger$transfer,
                                                serverlevel1,
                                                chunkpos4
                                            )
                                        );
                                        flag1 = true;
                                        return result(RecallService.Status.PENDING, "loading");
                                    }

                                    serverlevel3.getChunkSource().removeRegionTicket(LOAD_TICKET, chunkpos2, 2, dragon);
                                    return fail("missing");
                                }

                                recallledger$transfer = null;
                                recallledger$entry = recallledger.entry(dragon);
                            }

                            if (recallledger$transfer != null
                                && recallledger$entry.state() == RecallLedger.State.HORN
                                && recallledger$transfer.before().state() == RecallLedger.State.WORLD) {
                                RecallHorn.CaptureRecovery recallhorn$capturerecovery = RecallHorn.recoverCapture(
                                    player.serverLevel(), recallledger, recallledger$transfer
                                );
                                if (recallhorn$capturerecovery == RecallHorn.CaptureRecovery.ROLLED_BACK) {
                                    recallledger$transfer = null;
                                    recallledger$entry = recallledger.entry(dragon);
                                } else if (recallhorn$capturerecovery == RecallHorn.CaptureRecovery.WAIT) {
                                    ServerLevel serverlevel2 = RecallHorn.captureSource(player.serverLevel(), recallledger$transfer);
                                    if (serverlevel2 == null) {
                                        return fail("missing");
                                    }

                                    ChunkPos chunkpos1 = new ChunkPos(recallledger$transfer.before().pos());
                                    serverlevel2.getChunkSource().addRegionTicket(LOAD_TICKET, chunkpos1, 2, dragon);
                                    WAITING.put(
                                        dragon,
                                        new RecallService.Pending(
                                            player.server,
                                            player.getUUID(),
                                            player.level().dimension(),
                                            altar.immutable(),
                                            dragon,
                                            i,
                                            serverlevel2,
                                            chunkpos1,
                                            player.server.getTickCount() + (Integer)RecallConfig.LOAD_TIMEOUT_TICKS.get(),
                                            recallledger$transfer,
                                            null,
                                            null
                                        )
                                    );
                                    flag1 = true;
                                    return result(RecallService.Status.PENDING, "loading");
                                }
                            }

                            if (recallledger$transfer != null
                                && (recallledger$entry.state() == RecallLedger.State.HORN || recallledger$entry.state() == RecallLedger.State.VOID)) {
                                recallledger.commitTransfer(recallledger$transfer);
                                recallledger$transfer = null;
                                recallledger$entry = recallledger.entry(dragon);
                            }

                            Entity entity = findLoaded(player.server, recallledger$entry);
                            if (entity != null && recallledger$transfer != null) {
                                recallledger.commitTransfer(recallledger$transfer);
                                recallledger$transfer = null;
                                recallledger$entry = recallledger.entry(dragon);
                            }

                            if (entity == null
                                && recallledger$entry.state() != RecallLedger.State.HORN
                                && recallledger$entry.state() != RecallLedger.State.VOID) {
                                ResourceKey<Level> resourcekey = dimension(recallledger$entry.dimension());
                                ServerLevel serverlevel = resourcekey == null ? null : player.server.getLevel(resourcekey);
                                if (serverlevel == null) {
                                    return fail("missing");
                                } else {
                                    ChunkPos chunkpos = new ChunkPos(recallledger$entry.pos());
                                    serverlevel.getChunkSource().addRegionTicket(LOAD_TICKET, chunkpos, 2, dragon);
                                    WAITING.put(
                                        dragon,
                                        new RecallService.Pending(
                                            player.server,
                                            player.getUUID(),
                                            player.level().dimension(),
                                            altar.immutable(),
                                            dragon,
                                            i,
                                            serverlevel,
                                            chunkpos,
                                            player.server.getTickCount() + (Integer)RecallConfig.LOAD_TIMEOUT_TICKS.get(),
                                            recallledger$transfer,
                                            null,
                                            null
                                        )
                                    );
                                    flag1 = true;
                                    return result(RecallService.Status.PENDING, "loading");
                                }
                            } else {
                                return execute(player, altar, recallledger$entry, entity, i, recallledger$transfer);
                            }
                        } catch (RuntimeException runtimeexception) {
                            LOGGER.error("Could not begin recall of {}", dragon, runtimeexception);
                            return fail("failed");
                        } finally {
                            if (!flag1) {
                                recallledger.release(dragon);
                            }
                        }
                    }
                }
            } else {
                return fail("busy");
            }
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!WAITING.isEmpty()) {
            for (RecallService.Pending recallservice$pending : new ArrayList<>(WAITING.values())) {
                if (recallservice$pending.server() == event.getServer()) {
                    ServerPlayer serverplayer = recallservice$pending.server().getPlayerList().getPlayer(recallservice$pending.player());
                    ServerLevel serverlevel = recallservice$pending.server().getLevel(recallservice$pending.destination());
                    RecallLedger recallledger = RecallLedger.get(recallservice$pending.source());
                    RecallService.RequestResult recallservice$requestresult = null;


                    try {
                        if (serverplayer != null && serverlevel != null && serverplayer.serverLevel() == serverlevel) {
                            recallservice$requestresult = checkAltar(serverplayer, serverlevel, recallservice$pending.altar());
                        } else {
                            recallservice$requestresult = fail("too_far");
                        }

                        RecallLedger.Entry recallledger$entry = recallledger.entry(recallservice$pending.dragon());
                        if (recallservice$requestresult == null && !owned(serverplayer, recallledger$entry)) {
                            recallservice$requestresult = fail("unavailable");
                        }

                        if (recallservice$requestresult == null) {
                            boolean flag = recallservice$pending.server().getTickCount() >= recallservice$pending.deadline();
                            RecallLedger.Transfer recallledger$transfer = recallledger.pending(recallservice$pending.dragon());
                            if (recallledger$transfer != null
                                && recallledger$entry.state() == RecallLedger.State.VOID
                                && recallledger$transfer.before().state() == RecallLedger.State.WORLD) {
                                RecallService.RecoverySafety recallservice$recoverysafety1 = resolveVoidRecovery(
                                    recallservice$pending.server(), recallledger, recallledger$transfer
                                );
                                if (recallservice$recoverysafety1 == RecallService.RecoverySafety.WAIT) {
                                    if (flag) {
                                        recallservice$requestresult = fail("missing");
                                    }
                                } else {
                                    if (recallservice$recoverysafety1 == RecallService.RecoverySafety.READY) {
                                        recallledger.commitTransfer(recallledger$transfer);
                                    }

                                    recallledger$entry = recallledger.entry(recallservice$pending.dragon());
                                    Entity entity3 = findLoaded(recallservice$pending.server(), recallledger$entry);
                                    recallservice$requestresult = execute(
                                        serverplayer, recallservice$pending.altar(), recallledger$entry, entity3, recallservice$pending.cost(), null
                                    );
                                }
                            } else if (recallledger$transfer != null && hasWorldSource(recallledger$transfer)) {
                                RecallService.RecoverySafety recallservice$recoverysafety = resolveWorldRecovery(
                                    recallservice$pending.server(), recallledger, recallledger$transfer
                                );
                                if (recallservice$recoverysafety != RecallService.RecoverySafety.WAIT) {
                                    recallledger$entry = recallledger.entry(recallservice$pending.dragon());
                                    Entity entity2 = findLoaded(recallservice$pending.server(), recallledger$entry);
                                    if (recallservice$recoverysafety == RecallService.RecoverySafety.READY && entity2 != null) {
                                        recallledger.commitTransfer(recallledger$transfer);
                                        recallledger$entry = recallledger.entry(recallservice$pending.dragon());
                                    }

                                    recallservice$requestresult = execute(
                                        serverplayer,
                                        recallservice$pending.altar(),
                                        recallledger$entry,
                                        entity2,
                                        recallservice$pending.cost(),
                                        recallservice$recoverysafety == RecallService.RecoverySafety.READY && entity2 == null ? recallledger$transfer : null
                                    );
                                } else if (flag) {
                                    recallservice$requestresult = fail("missing");
                                }
                            } else if (recallledger$transfer != null
                                && recallledger$entry.state() == RecallLedger.State.HORN
                                && recallledger$transfer.before().state() == RecallLedger.State.WORLD) {
                                RecallHorn.CaptureRecovery recallhorn$capturerecovery = RecallHorn.recoverCapture(
                                    recallservice$pending.source(), recallledger, recallledger$transfer
                                );
                                if (recallhorn$capturerecovery == RecallHorn.CaptureRecovery.WAIT) {
                                    if (flag) {
                                        recallservice$requestresult = fail("missing");
                                    }
                                } else {
                                    if (recallhorn$capturerecovery == RecallHorn.CaptureRecovery.READY) {
                                        recallledger.commitTransfer(recallledger$transfer);
                                    }

                                    recallledger$entry = recallledger.entry(recallservice$pending.dragon());
                                    Entity entity1 = findLoaded(recallservice$pending.server(), recallledger$entry);
                                    recallservice$requestresult = execute(
                                        serverplayer, recallservice$pending.altar(), recallledger$entry, entity1, recallservice$pending.cost(), null
                                    );
                                }
                            } else {
                                Entity entity = findLoaded(recallservice$pending.server(), recallledger$entry);
                                if (entity != null) {
                                    if (recallledger$transfer != null) {
                                        recallledger.commitTransfer(recallledger$transfer);
                                        recallledger$entry = recallledger.entry(recallservice$pending.dragon());
                                    }

                                    recallservice$requestresult = execute(
                                        serverplayer, recallservice$pending.altar(), recallledger$entry, entity, recallservice$pending.cost(), null
                                    );
                                } else if (flag) {
                                    boolean flag1 = recallservice$pending.source().isPositionEntityTicking(recallledger$entry.pos())
                                        && recallservice$pending.source().areEntitiesLoaded(recallservice$pending.chunk().toLong());
                                    boolean flag2 = recallledger$entry.state() == RecallLedger.State.STONE || recallservice$pending.recovery() != null;
                                    recallservice$requestresult = flag1 && flag2
                                        ? execute(
                                            serverplayer,
                                            recallservice$pending.altar(),
                                            recallledger$entry,
                                            null,
                                            recallservice$pending.cost(),
                                            recallservice$pending.recovery()
                                        )
                                        : fail("missing");
                                }
                            }
                        }
                    } catch (RuntimeException runtimeexception1) {
                        LOGGER.error("Recall request for {} failed", recallservice$pending.dragon(), runtimeexception1);
                        recallservice$requestresult = fail("failed");
                    }

                    if (recallservice$requestresult != null) {
                        finishPending(recallservice$pending);
                        if (serverplayer != null) {
                            serverplayer.displayClientMessage(recallservice$requestresult.message(), false);

                            try {
                                completion.accept(serverplayer, recallservice$requestresult);
                            } catch (RuntimeException runtimeexception) {
                                LOGGER.warn("Could not refresh recall screen", runtimeexception);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppingEvent event) {
        for (RecallService.Pending recallservice$pending : new ArrayList<>(WAITING.values())) {
            if (recallservice$pending.server() == event.getServer()) {
                finishPending(recallservice$pending);
            }
        }
    }

    private static void finishPending(RecallService.Pending pending) {
        WAITING.remove(pending.dragon());
        pending.source().getChunkSource().removeRegionTicket(LOAD_TICKET, pending.chunk(), 2, pending.dragon());
        if (pending.target() != null) {
            pending.target().getChunkSource().removeRegionTicket(LOAD_TICKET, pending.targetChunk(), 2, pending.dragon());
        }

        RecallLedger.get(pending.source()).release(pending.dragon());
    }

    private static RecallService.RequestResult execute(
        ServerPlayer player, BlockPos altar, RecallLedger.Entry entry, Entity source, int quotedCost, RecallLedger.Transfer recovery
    ) {
        RecallService.RequestResult recallservice$requestresult = executeTransfer(player, altar, entry, source, quotedCost, recovery);
        if (recallservice$requestresult.status() == RecallService.Status.SUCCESS) {
            try {
                player.serverLevel().playSound(null, altar, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.NEUTRAL, 3.0F, 0.75F);
            } catch (RuntimeException runtimeexception) {
                LOGGER.warn("Recall completed but its sound could not be played", runtimeexception);
            }
        }

        return recallservice$requestresult;
    }

    private static RecallService.RequestResult executeTransfer(
        ServerPlayer player, BlockPos altar, RecallLedger.Entry entry, Entity source, int quotedCost, RecallLedger.Transfer recovery
    ) {
        ServerLevel serverlevel = player.serverLevel();
        RecallLedger recallledger = RecallLedger.get(serverlevel);
        if (!owned(player, entry)) {
            return fail("not_owned");
        } else {
            int i = quote(player, altar, entry.dragon());
            if (i != quotedCost) {
                return fail("price_changed", i);
            } else if (RecallCosts.availablePoints(player) < (long)i) {
                return fail("insufficient_xp", i);
            } else if (source != null && !validSource(player, entry, source)) {
                return fail("unavailable");
            } else if (source == null && entry.state() == RecallLedger.State.WORLD && recovery == null) {
                return fail("missing");
            } else {
                Entity entity = source == null ? restoreCandidate(serverlevel, entry) : source;
                if (entity == null) {
                    return fail("corrupt");
                } else if (!validSource(player, entry, entity)) {
                    cleanCandidate(entity, source);
                    return fail("corrupt");
                } else {
                    entity.refreshDimensions();
                    final Vec3 vec3 = safeLanding(serverlevel, altar, entity);
                    if (vec3 == null) {
                        cleanCandidate(entity, source);
                        return fail("no_space");
                    } else {
                        Vec3 vec31;
                        float f;
                        float f1;
                        int j;
                        int k;
                        boolean flag9;
                        label615: {
                            vec31 = entity.position();
                            f = entity.getYRot();
                            f1 = entity.getXRot();
                            j = entity instanceof DragonBaseEntity entitydragonbase ? entitydragonbase.getHunger() : 50;
                            k = entity instanceof DragonBaseEntity entitydragonbase1 ? entitydragonbase1.getCommand() : 0;
                            if (entity instanceof DragonBaseEntity entitydragonbase2 && entitydragonbase2.isOrderedToSit()) {
                                flag9 = true;
                                break label615;
                            }

                            flag9 = false;
                        }

                        boolean flag1;
                        label610: {
                            flag1 = flag9;
                            if (entity instanceof DragonBaseEntity entitydragonbase3 && entitydragonbase3.isSleeping()) {
                                flag9 = true;
                                break label610;
                            }

                            flag9 = false;
                        }

                        boolean flag2;
                        label605: {
                            flag2 = flag9;
                            if (entity instanceof DragonBaseEntity entitydragonbase4
                                && DragonForms.state(entitydragonbase4).dragonmaid$isCombatInterruptedRest()) {
                                flag9 = true;
                                break label605;
                            }

                            flag9 = false;
                        }

                        boolean flag3;
                        label600: {
                            flag3 = flag9;
                            if (entity instanceof DragonBaseEntity entitydragonbase5 && entitydragonbase5.isBreathingFire()) {
                                flag9 = true;
                                break label600;
                            }

                            flag9 = false;
                        }

                        boolean flag4;
                        int l;
                        label594: {
                            flag4 = flag9;
                            l = entity instanceof DragonBaseEntity entitydragonbase6
                                ? DragonForms.state(entitydragonbase6).dragonmaid$getNativeBreathTicks()
                                : 0;
                            if (entity instanceof LightningDragonEntity entitylightningdragon && entitylightningdragon.hasLightningTarget()) {
                                flag9 = true;
                                break label594;
                            }

                            flag9 = false;
                        }

                        boolean flag5;
                        label589: {
                            flag5 = flag9;
                            if (entity instanceof DragonBaseEntity entitydragonbase7 && entitydragonbase7.isTackling()) {
                                flag9 = true;
                                break label589;
                            }

                            flag9 = false;
                        }

                        boolean flag6;
                        label584: {
                            flag6 = flag9;
                            if (entity instanceof DragonBaseEntity entitydragonbase8 && entitydragonbase8.isFlying()) {
                                flag9 = true;
                                break label584;
                            }

                            flag9 = false;
                        }

                        boolean flag7;
                        label579: {
                            flag7 = flag9;
                            if (entity instanceof DragonBaseEntity entitydragonbase9 && entitydragonbase9.isHovering()) {
                                flag9 = true;
                                break label579;
                            }

                            flag9 = false;
                        }

                        boolean flag8 = flag9;
                        int i1 = entity instanceof DragonBaseEntity entitydragonbase10 ? DragonForms.state(entitydragonbase10).dragonmaid$getAction() : 0;
                        BlockPos blockpos = entity instanceof DragonBaseEntity entitydragonbase11 ? entitydragonbase11.burningTarget : null;
                        LivingEntity livingentity = entity instanceof DragonBaseEntity entitydragonbase12 ? entitydragonbase12.getTarget() : null;
                        CompoundTag compoundtag1 = entity instanceof StoneStatueEntity entitystonestatue ? entitystonestatue.getTrappedTag().copy() : null;
                        RecallLedger.Transfer recallledger$transfer = recovery;
                        Entity entity1 = null;
                        final Entity[] aentity = new Entity[1];
                        boolean flag = false;

                        try {
                            try {
                                if (recallledger$transfer == null && source instanceof DragonBaseEntity entitydragonbase13) {
                                    entry = recallledger.observe(entitydragonbase13);
                                } else if (recallledger$transfer == null && source instanceof StoneStatueEntity entitystonestatue2) {
                                    PetrificationLedger.Entry petrificationledger$entry = PetrificationLedger.get(serverlevel).entry(entry.dragon());
                                    if (petrificationledger$entry == null || !petrificationledger$entry.active()) {
                                        return fail("unavailable");
                                    }

                                    entry = recallledger.observeStone(
                                        entry.dragon(),
                                        entry.owner(),
                                        petrificationledger$entry.type(),
                                        petrificationledger$entry.snapshot(),
                                        entitystonestatue2
                                    );
                                }

                                CompoundTag compoundtag2 = entity.saveWithoutId(new CompoundTag());
                                compoundtag2.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
                                compoundtag2.remove("Passengers");
                                if (entity instanceof DragonBaseEntity) {
                                    compoundtag2.putInt("Command", 1);
                                    compoundtag2.putBoolean("Sitting", true);
                                    compoundtag2.putBoolean("Sleeping", false);
                                    compoundtag2.putBoolean("FireBreathing", false);
                                    compoundtag2.putBoolean("Tackle", false);
                                    compoundtag2.putBoolean("Flying", false);
                                    compoundtag2.putBoolean("Hovering", false);
                                    CompoundTag compoundtag3 = compoundtag2.getCompound("DragonMaid");
                                    compoundtag3.putBoolean("CombatInterruptedRest", false);
                                    compoundtag2.put("DragonMaid", compoundtag3);
                                }

                                RecallLedger.State recallledger$state = entity instanceof StoneStatueEntity
                                    ? RecallLedger.State.STONE
                                    : RecallLedger.State.WORLD;
                                if (recallledger$transfer == null) {
                                    recallledger$transfer = recallledger.prepareTransfer(
                                        entry.dragon(),
                                        recallledger$state,
                                        serverlevel.dimension().location().toString(),
                                        BlockPos.containing(vec3),
                                        entity.getUUID(),
                                        compoundtag2
                                    );
                                } else if (entity instanceof DragonBaseEntity) {
                                    recallledger$transfer = recallledger.retargetRecovery(
                                        recallledger$transfer,
                                        serverlevel.dimension().location().toString(),
                                        BlockPos.containing(vec3),
                                        entity.getUUID(),
                                        compoundtag2
                                    );
                                } else {
                                    recallledger$transfer = recallledger.retargetRecovery(
                                        recallledger$transfer, serverlevel.dimension().location().toString(), BlockPos.containing(vec3), entity.getUUID()
                                    );
                                }

                                entity.ejectPassengers();
                                entity.stopRiding();
                                final float f2 = player.getYRot() + 180.0F;
                                if (source == null) {
                                    entity.moveTo(vec3.x, vec3.y, vec3.z, f2, 0.0F);
                                    stamp(entity, recallledger$transfer.after());
                                    settle(entity);
                                    aentity[0] = entity;
                                    if (!serverlevel.addFreshEntity(entity) || entity.isRemoved()) {
                                        throw new IllegalStateException("Target spawn rejected");
                                    }

                                    entity1 = entity;
                                } else if (source.level() == serverlevel) {
                                    source.teleportTo(vec3.x, vec3.y, vec3.z);
                                    source.setYRot(f2);
                                    source.setXRot(0.0F);
                                    stamp(source, recallledger$transfer.after());
                                    entity1 = source;
                                    settle(source);
                                } else {
                                    final RecallLedger.Entry recallledger$entry = recallledger$transfer.after();
                                    Entity entity2 = source.getType().create(serverlevel);
                                    if (entity2 == null) {
                                        throw new IllegalStateException("Unable to construct transfer target");
                                    }

                                    entity2.restoreFrom(source);
                                    RecallService.stamp(entity2, recallledger$entry);
                                    entity2.moveTo(vec3.x, vec3.y, vec3.z, f2, 0.0F);
                                    RecallService.settle(entity2);
                                    aentity[0] = entity2;
                                    source.remove(Entity.RemovalReason.CHANGED_DIMENSION);
                                    if (!serverlevel.addFreshEntity(entity2) || entity2.isRemoved()) {
                                        RecallService.cleanCandidate(entity2, null);
                                        throw new IllegalStateException("Dimension target rejected");
                                    }

                                    entity1 = entity2;
                                    if (entity1 == null || entity1.isRemoved()) {
                                        throw new IllegalStateException("Dimension travel denied");
                                    }

                                    if (source instanceof DragonBaseEntity entitydragonbase15) {
                                        entitydragonbase15.removeParts();
                                    }
                                }

                                if (entity1 instanceof DragonBaseEntity entitydragonbase16) {
                                    entitydragonbase16.setHunger(50);
                                } else if (entity1 instanceof StoneStatueEntity entitystonestatue3) {
                                    CompoundTag compoundtag = entitystonestatue3.getTrappedTag().copy();
                                    compoundtag.putInt("Hunger", 50);
                                    entitystonestatue3.setTrappedTag(compoundtag);
                                }

                                recallledger.commitTransfer(recallledger$transfer);
                                flag = true;
                                if (entity1 instanceof DragonBaseEntity entitydragonbase17) {
                                    try {
                                        TailSweepSync.cancel(entitydragonbase17);
                                    } catch (RuntimeException runtimeexception2) {
                                        LOGGER.warn("Recall completed but tail animation cancellation failed", runtimeexception2);
                                    }
                                }

                                if (i != 0) {
                                    player.giveExperiencePoints(-i);
                                }

                                if (entity1 instanceof DragonBaseEntity entitydragonbase18) {
                                    RecallTracking.observe(entitydragonbase18);
                                    if (source != null && source.level() instanceof ServerLevel serverlevel1 && serverlevel1 != serverlevel) {
                                        DragonPosWorldData.get(serverlevel1).removeDragon(entitydragonbase18.getUUID());
                                    }

                                    DragonPosWorldData.get(serverlevel).addDragon(entitydragonbase18.getUUID(), entitydragonbase18.blockPosition());
                                } else if (entity1 instanceof StoneStatueEntity entitystonestatue4) {
                                    RecallDragonPetrification.prepareRecalledStatue(entitystonestatue4);
                                }

                                return result(RecallService.Status.SUCCESS, "success");
                            } catch (RuntimeException runtimeexception3) {
                                LOGGER.error("Recall transfer for {} could not finish", entry.dragon(), runtimeexception3);
                                if (flag) {
                                    return result(RecallService.Status.SUCCESS, "success");
                                }

                                if (entity1 == null && aentity[0] != null && !aentity[0].isRemoved()) {
                                    entity1 = aentity[0];
                                }
                            }

                            if (source != null && source.isRemoved() && entity1 != null && !entity1.isRemoved()) {
                                RecallTracking.retryCommit(entity1, entry.dragon());
                                return fail("failed");
                            } else {
                                if (entity1 != null && entity1 != source) {
                                    try {
                                        entity1.discard();
                                    } catch (RuntimeException runtimeexception1) {
                                        LOGGER.error("Could not remove failed recall target; retaining pending transfer", runtimeexception1);
                                    }

                                    if (!entity1.isRemoved()) {
                                        return fail("failed");
                                    }

                                    cleanCandidate(entity1, source);
                                }

                                if (source != null && !source.isRemoved()) {
                                    source.moveTo(vec31.x, vec31.y, vec31.z, f, f1);
                                    if (source instanceof DragonBaseEntity entitydragonbase14) {
                                        entitydragonbase14.setHunger(j);
                                        entitydragonbase14.setCommand(k);
                                        entitydragonbase14.setOrderedToSit(flag1);
                                        entitydragonbase14.setInSittingPose(flag2);
                                        DragonForms.state(entitydragonbase14).dragonmaid$setCombatInterruptedRest(flag3);
                                        entitydragonbase14.setFlying(flag7);
                                        entitydragonbase14.setHovering(flag8);
                                        entitydragonbase14.setTackling(flag6);
                                        entitydragonbase14.setBreathingFire(flag4);
                                        DragonForms.state(entitydragonbase14).dragonmaid$setNativeBreathTicks(l);
                                        if (entitydragonbase14 instanceof LightningDragonEntity entitylightningdragon1) {
                                            entitylightningdragon1.setHasLightningTarget(flag5);
                                        }

                                        entitydragonbase14.burningTarget = blockpos;
                                        DragonForms.state(entitydragonbase14).dragonmaid$setAction(i1);
                                        entitydragonbase14.setTarget(livingentity);
                                    } else if (source instanceof StoneStatueEntity entitystonestatue1 && compoundtag1 != null) {
                                        entitystonestatue1.setTrappedTag(compoundtag1);
                                    }

                                    stamp(source, entry);
                                }

                                if (recovery == null && recallledger$transfer != null && recallledger.pending(entry.dragon()) != null) {
                                    try {
                                        recallledger.rollbackTransfer(recallledger$transfer);
                                    } catch (RuntimeException runtimeexception) {
                                        LOGGER.error("Recall rollback retained transaction for recovery", runtimeexception);
                                    }
                                }

                                return fail("failed");
                            }
                        } finally {
                            if (source == null && !flag && entity1 != entity) {
                                cleanCandidate(entity, null);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Entity restoreCandidate(ServerLevel level, RecallLedger.Entry entry) {
        if (entry.snapshot().isEmpty()) {
            return null;
        } else if (entry.state() == RecallLedger.State.STONE) {
            return RecallDragonPetrification.createRecallStatue(level, entry);
        } else {
            EntityType<?> entitytype = (EntityType<?>)EntityType.byString(entry.type()).orElse(null);
            if ((entitytype == null ? null : entitytype.create(level)) instanceof DragonBaseEntity entitydragonbase) {
                CompoundTag compoundtag = entry.snapshot().copy();
                compoundtag.remove("Passengers");
                entitydragonbase.load(compoundtag);
                RecallTracking.stamp(entitydragonbase, entry);
                entitydragonbase.setHealth(Math.min(entitydragonbase.getMaxHealth(), compoundtag.getFloat("Health")));
                entitydragonbase.refreshDimensions();
                return entitydragonbase;
            } else {
                return null;
            }
        }
    }

    private static boolean validSource(ServerPlayer player, RecallLedger.Entry entry, Entity entity) {
        if (entity.isRemoved() || !RecallTracking.matches(entity, entry)) {
            return false;
        } else if (entity instanceof DragonBaseEntity entitydragonbase) {
            return entry.dragon().equals(entitydragonbase.getUUID())
                && player.getUUID().equals(entitydragonbase.getOwnerUUID())
                && RecallTracking.supported(entitydragonbase)
                && !DragonForms.state(entitydragonbase).dragonmaid$isExecuted()
                && (entitydragonbase.isAlive() || HumanoidLife.isInjured(entitydragonbase));
        } else {
            if (entity instanceof StoneStatueEntity entitystonestatue && RecallDragonPetrification.validRecallStatue(entitystonestatue, entry)) {
                return true;
            }

            return false;
        }
    }

    private static void settle(Entity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        entity.setNoGravity(false);
        entity.clearFire();
        if (entity instanceof DragonBaseEntity entitydragonbase) {
            entitydragonbase.getNavigation().stop();
            entitydragonbase.setTarget(null);
            entitydragonbase.setFlying(false);
            entitydragonbase.setHovering(false);
            entitydragonbase.setTackling(false);
            HumanoidCombat.stopBreath(entitydragonbase);
            entitydragonbase.burningTarget = null;
            DragonForms.state(entitydragonbase).dragonmaid$setAction(0);
            entitydragonbase.setInSittingPose(false);
            entitydragonbase.setCommand(1);
            entitydragonbase.yBodyRot = entitydragonbase.yBodyRotO = entitydragonbase.yHeadRot = entitydragonbase.yHeadRotO = entitydragonbase.getYRot();
        }
    }

    private static void stamp(Entity entity, RecallLedger.Entry entry) {
        if (entity instanceof StoneStatueEntity entitystonestatue) {
            RecallDragonPetrification.stampRecallStatue(entitystonestatue, entry);
        } else {
            RecallTracking.stamp(entity, entry);
        }
    }

    private static void cleanCandidate(Entity candidate, Entity source) {
        if (candidate != null && candidate != source && candidate instanceof DragonBaseEntity entitydragonbase) {
            entitydragonbase.removeParts();
        }
    }

    private static Entity findLoaded(MinecraftServer server, RecallLedger.Entry entry) {
        UUID uuid = entry.state() == RecallLedger.State.STONE ? entry.holder() : entry.dragon();
        if (uuid == null) {
            return null;
        } else {
            for (ServerLevel serverlevel : server.getAllLevels()) {
                Entity entity = serverlevel.getEntity(uuid);
                if (entity != null && !entity.isRemoved()) {
                    if (RecallTracking.matches(entity, entry)) {
                        return entity;
                    }

                    // Old saves / RCON-summoned dragons may predate the recall token. Heal the token
                    // (never overwrite an existing one) and accept the entity for this recall.
                    if (entry.state() == RecallLedger.State.WORLD
                        && entity instanceof com.iafenvoy.iceandfire.entity.DragonBaseEntity
                        && !RecallTracking.hasToken(entity)) {
                        if (RecallTracking.stampIfMissing(entity, entry)) {
                            return entity;
                        }
                    }
                }
            }

            return null;
        }
    }

    private static ResourceKey<Level> dimension(String key) {
        ResourceLocation resourcelocation = ResourceLocation.tryParse(key);
        return resourcelocation == null ? null : ResourceKey.create(Registries.DIMENSION, resourcelocation);
    }

    private static boolean owned(ServerPlayer player, RecallLedger.Entry entry) {
        return player != null && entry != null && entry.state() != RecallLedger.State.DEAD && player.getUUID().equals(entry.owner());
    }

    private static RecallService.RequestResult checkAltar(ServerPlayer player, ServerLevel level, BlockPos altar) {
        if (player.isSpectator() || !player.isAlive() || player.serverLevel() != level || player.distanceToSqr(Vec3.atCenterOf(altar)) > 64.0) {
            return fail("too_far");
        } else {
            return level.hasChunkAt(altar)
                    && ALTAR.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(altar).getBlock()))
                    && RecallActivation.isActive(level, altar, player)
                ? null
                : fail("inactive");
        }
    }

    private static Vec3 safeLanding(ServerLevel level, BlockPos altar, Entity entity) {
        double d0 = entity instanceof StoneStatueEntity entitystonestatue ? (double)entitystonestatue.getTrappedWidth() : (double)entity.getBbWidth();
        double d4 = entity instanceof StoneStatueEntity entitystonestatue1 ? (double)entitystonestatue1.getTrappedHeight() : (double)entity.getBbHeight();
        if (Double.isFinite(d0) && Double.isFinite(d4) && !(d0 <= 0.0) && !(d4 <= 0.0) && !(d0 > 64.0) && !(d4 > 64.0)) {
            double d5 = d0 / 2.0;
            int i = Math.max(2, (int)Math.ceil(d5) + 1);
            int j = (Integer)RecallConfig.SEARCH_RADIUS.get();
            int[][] aint = new int[][]{{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
            int[] aint1 = new int[]{1, 0, 2, -1, 3, -2, 4, -3, 5, -4};

            for (int k = i; k <= j; k += 2) {
                for (int[] aint2 : aint) {
                    double d1 = (double)altar.getX() + 0.5 + (double)(aint2[0] * k);
                    double d2 = (double)altar.getZ() + 0.5 + (double)(aint2[1] * k);

                    for (int l : aint1) {
                        double d3 = (double)(altar.getY() + l);
                        if (level.areEntitiesLoaded(new ChunkPos(BlockPos.containing(d1, d3, d2)).toLong())) {
                            AABB aabb = new AABB(d1 - d5, d3, d2 - d5, d1 + d5, d3 + d4, d2 + d5);
                            if (!(aabb.minY <= (double)level.getMinBuildHeight())
                                && !(aabb.maxY >= (double)level.getMaxBuildHeight())
                                && level.getWorldBorder().isWithinBounds(aabb)
                                && level.hasChunksAt(
                                    BlockPos.containing(aabb.minX, aabb.minY - 1.0, aabb.minZ),
                                    BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ)
                                )
                                && supportedFloor(level, d1, d3, d2, d5)
                                && level.noCollision(entity, aabb.deflate(0.01))
                                && !level.containsAnyLiquid(aabb)) {
                                return new Vec3(d1, d3, d2);
                            }
                        }
                    }
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private static boolean supportedFloor(ServerLevel level, double x, double y, double z, double half) {
        double d0 = Math.max(0.0, half - 0.1);
        double[][] adouble = new double[][]{{0.0, 0.0}, {d0, d0}, {d0, -d0}, {-d0, d0}, {-d0, -d0}};

        for (double[] adouble1 : adouble) {
            BlockPos blockpos = BlockPos.containing(x + adouble1[0], y - 0.05, z + adouble1[1]);
            BlockState blockstate = level.getBlockState(blockpos);
            if (!blockstate.isFaceSturdy(level, blockpos, Direction.UP)
                || dangerous(blockstate)
                || !blockstate.getFluidState().isEmpty()
                || dangerous(level.getBlockState(blockpos.above()))) {
                return false;
            }
        }

        return true;
    }

    private static boolean dangerous(BlockState state) {
        ResourceLocation resourcelocation = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return state.is(BlockTags.FIRE)
            || state.is(Blocks.CACTUS)
            || state.is(Blocks.MAGMA_BLOCK)
            || state.is(Blocks.CAMPFIRE)
            || state.is(Blocks.SOUL_CAMPFIRE)
            || state.is(Blocks.SWEET_BERRY_BUSH)
            || state.is(Blocks.WITHER_ROSE)
            || resourcelocation != null && resourcelocation.getPath().contains("dragon_ice_spikes");
    }

    private static RecallService.RequestResult fail(String suffix, Object... args) {
        return result(RecallService.Status.FAILED, suffix, args);
    }

    private static RecallService.RequestResult result(RecallService.Status status, String suffix, Object... args) {
        return new RecallService.RequestResult(status, Component.translatable("message.dragonmaid.recall." + suffix, args));
    }

    private static record Pending(
        MinecraftServer server,
        UUID player,
        ResourceKey<Level> destination,
        BlockPos altar,
        UUID dragon,
        int cost,
        ServerLevel source,
        ChunkPos chunk,
        int deadline,
        RecallLedger.Transfer recovery,
        ServerLevel target,
        ChunkPos targetChunk
    ) {
    }

    private static enum RecoverySafety {
        READY,
        ROLLED_BACK,
        WAIT;

        private RecoverySafety() {
        }
    }

    public static record RequestResult(RecallService.Status status, Component message) {
    }

    public static enum Status {
        SUCCESS,
        PENDING,
        FAILED;

        private Status() {
        }
    }
}
