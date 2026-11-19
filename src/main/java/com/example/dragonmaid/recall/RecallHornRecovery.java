package com.example.dragonmaid.recall;

import com.example.dragonmaid.compat.StackNbt;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.mojang.logging.LogUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * 1.2.7 horn recovery transaction: cross-dimension dragon transfers through a reinforced horn are
 * two-phase, so a server restart / dimension unload between "dragon captured" and "dragon released"
 * must be recoverable. This keeps a {@link TicketType} region ticket alive on the destination chunk
 * until the transfer is verified, then commits or rolls it back and tells the player.
 *
 * <p>NeoForge 1.21.1 移植：{@code TickEvent.ServerTickEvent(Phase.END)} -> {@code ServerTickEvent.Post}；
 * {@code PlayerLoggedOutEvent}/{@code ServerStoppingEvent} 换包名；读取号角 NBT 走 compat/StackNbt
 * （1.21.1 的 ItemStack 标签是数据组件，原始 tag 是副本）。
 */
@EventBusSubscriber(modid = "dragonmaid")
public final class RecallHornRecovery {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TicketType<UUID> TICKET = TicketType.create("dragonmaid_horn_recovery", Comparator.<UUID>naturalOrder());
    private static final Map<Key, Request> REQUESTS = new HashMap<>();

    private RecallHornRecovery() {
    }

    static boolean request(ServerPlayer player, ItemStack horn) {
        CompoundTag tag = StackNbt.getTag(horn);
        CompoundTag receipt = tag == null ? new CompoundTag() : tag.getCompound("DragonMaidRecall");
        if (!receipt.hasUUID("Dragon")) {
            return false;
        }

        UUID dragon = receipt.getUUID("Dragon");
        RecallLedger ledger = RecallLedger.get(player.serverLevel());
        RecallLedger.Transfer transfer = ledger.pending(dragon);
        if (transfer == null) {
            return false;
        }

        RecallLedger.Entry chunkEntry;
        if (transfer.before().state() == RecallLedger.State.WORLD
            && transfer.after().state() == RecallLedger.State.HORN
            && RecallHorn.matchesReceipt(receipt, transfer.after())) {
            chunkEntry = transfer.before();
        } else {
            if (transfer.before().state() != RecallLedger.State.HORN
                || transfer.after().state() != RecallLedger.State.WORLD
                || !RecallHorn.matchesReceipt(receipt, transfer.before())) {
                return false;
            }

            chunkEntry = transfer.after();
        }

        MinecraftServer server = player.server;
        Key key = new Key(server, dragon);
        if (REQUESTS.containsKey(key) || ledger.isReserved(dragon)) {
            return true;
        }

        ResourceLocation dimension = ResourceLocation.tryParse(chunkEntry.dimension());
        ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null || !ledger.reserveRecovery(dragon)) {
            return false;
        }

        ChunkPos chunk = new ChunkPos(chunkEntry.pos());
        try {
            level.getChunkSource().addRegionTicket(TICKET, chunk, 2, dragon);
            REQUESTS.put(key, new Request(player, level, chunk, ledger, transfer, server.getTickCount() + RecallConfig.LOAD_TIMEOUT_TICKS.get()));
            return true;
        } catch (RuntimeException exception) {
            try {
                level.getChunkSource().removeRegionTicket(TICKET, chunk, 2, dragon);
            } finally {
                ledger.release(dragon);
            }

            throw exception;
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (REQUESTS.isEmpty() || event.getServer().getTickCount() % 5 != 0) {
            return;
        }

        Iterator<Map.Entry<Key, Request>> iterator = REQUESTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, Request> entry = iterator.next();
            Key key = entry.getKey();
            if (key.server() != event.getServer()) {
                continue;
            }

            Request request = entry.getValue();
            Result result = Result.FAILED;
            if (request.player().isRemoved()) {
                iterator.remove();
                finish(key, request);
                continue;
            }

            try {
                result = resolve(key, request);
                if (result == Result.WAIT && key.server().getTickCount() < request.deadline()) {
                    continue;
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Could not recover horn transfer for {}", key.dragon(), exception);
            }

            iterator.remove();
            finish(key, request);
            if (!request.player().isRemoved()) {
                request.player()
                    .displayClientMessage(
                        Component.translatable(
                            switch (result) {
                                case IN_HORN -> "message.dragonmaid.recall.horn_recovery_ready";
                                case IN_WORLD -> "message.dragonmaid.recall.horn_recovery_world";
                                default -> "message.dragonmaid.recall.horn_recovery_failed";
                            }
                        ),
                        true
                    );
            }
        }
    }

    private static Result resolve(Key key, Request request) {
        RecallLedger ledger = request.ledger();
        RecallLedger.Transfer transfer = request.transfer();
        if (ledger.pending(key.dragon()) != transfer) {
            return Result.FAILED;
        }

        if (!request.level().areEntitiesLoaded(request.chunk().toLong())) {
            return Result.WAIT;
        }

        if (transfer.after().state() == RecallLedger.State.HORN) {
            return switch (RecallHorn.recoverCapture(request.level(), ledger, transfer)) {
                case WAIT -> Result.WAIT;
                case ROLLED_BACK -> Result.IN_WORLD;
                case READY -> {
                    ledger.commitTransfer(transfer);
                    yield Result.IN_HORN;
                }
            };
        }

        Entity target = null;
        for (ServerLevel level : key.server().getAllLevels()) {
            Entity candidate = level.getEntity(key.dragon());
            if (candidate == null || candidate.isRemoved()) {
                continue;
            }

            if (target != null
                || level != request.level()
                || !(candidate instanceof DragonBaseEntity)
                || !RecallTracking.matches(candidate, transfer.after())) {
                return Result.WAIT;
            }

            target = candidate;
        }

        if (target != null) {
            ledger.commitTransfer(transfer);
            return Result.IN_WORLD;
        }

        ledger.rollbackTransfer(transfer);
        return Result.IN_HORN;
    }

    @SubscribeEvent
    public static void logout(PlayerLoggedOutEvent event) {
        REQUESTS.entrySet().removeIf(entry -> {
            if (entry.getValue().player() != event.getEntity()) {
                return false;
            }

            finish(entry.getKey(), entry.getValue());
            return true;
        });
    }

    @SubscribeEvent
    public static void stop(ServerStoppingEvent event) {
        REQUESTS.entrySet().removeIf(entry -> {
            if (entry.getKey().server() != event.getServer()) {
                return false;
            }

            finish(entry.getKey(), entry.getValue());
            return true;
        });
    }

    private static void finish(Key key, Request request) {
        try {
            request.level().getChunkSource().removeRegionTicket(TICKET, request.chunk(), 2, key.dragon());
        } finally {
            request.ledger().release(key.dragon());
        }
    }

    private record Key(MinecraftServer server, UUID dragon) {
    }

    private record Request(ServerPlayer player, ServerLevel level, ChunkPos chunk, RecallLedger ledger, RecallLedger.Transfer transfer, int deadline) {
    }

    private enum Result {
        WAIT,
        IN_HORN,
        IN_WORLD,
        FAILED
    }
}
