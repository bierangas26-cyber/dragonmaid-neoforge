package com.example.dragonmaid.combat;

import com.example.dragonmaid.animation.TailSweepState;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.TailSweepMessage;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.example.dragonmaid.compat.net.PacketDistributor;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class TailSweepSync {
    private static final Map<DragonBaseEntity, TailSweepState> STATES = new WeakHashMap<>();
    private static long nextRound;

    private TailSweepSync() {
    }

    public static boolean isRunning(DragonBaseEntity dragon) {
        TailSweepState tailsweepstate = STATES.get(dragon);
        return tailsweepstate != null && tailsweepstate.running(dragon.level().getGameTime());
    }

    public static boolean isBusy(DragonBaseEntity dragon) {
        TailSweepState tailsweepstate = STATES.get(dragon);
        return tailsweepstate != null
            && tailsweepstate.cancelledAt() < 0L
            && dragon.level().getGameTime() >= tailsweepstate.startedAt()
            && (double)dragon.level().getGameTime() < (double)tailsweepstate.startedAt() + 24.0 + 4.0;
    }

    public static TailSweepState begin(DragonBaseEntity dragon, float yaw) {
        TailSweepState tailsweepstate = new TailSweepState(++nextRound, 1, dragon.level().getGameTime(), yaw, -1L);
        STATES.put(dragon, tailsweepstate);
        send(dragon, tailsweepstate);
        return tailsweepstate;
    }

    public static void cancel(DragonBaseEntity dragon) {
        TailSweepState tailsweepstate = STATES.get(dragon);
        if (tailsweepstate != null
            && tailsweepstate.cancelledAt() < 0L
            && !((double)dragon.level().getGameTime() >= (double)tailsweepstate.startedAt() + 24.0 + 4.0)) {
            TailSweepState tailsweepstate1 = new TailSweepState(
                tailsweepstate.round(), tailsweepstate.revision() + 1, tailsweepstate.startedAt(), tailsweepstate.yaw(), dragon.level().getGameTime()
            );
            STATES.put(dragon, tailsweepstate1);
            send(dragon, tailsweepstate1);
        }
    }

    private static void send(DragonBaseEntity dragon, TailSweepState state) {
        if (!dragon.level().isClientSide) {
            DragonNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> dragon), packet(dragon, state));
        }
    }

    private static TailSweepMessage packet(DragonBaseEntity dragon, TailSweepState state) {
        return new TailSweepMessage(dragon.level().dimension().location(), dragon.getId(), dragon.getUUID(), state);
    }

    @SubscribeEvent
    public static void tracking(StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverplayer && event.getTarget() instanceof DragonBaseEntity entitydragonbase) {
            TailSweepState tailsweepstate = STATES.get(entitydragonbase);
            if (tailsweepstate != null
                && tailsweepstate.cancelledAt() < 0L
                && (double)entitydragonbase.level().getGameTime() < (double)tailsweepstate.startedAt() + 24.0 + 4.0) {
                DragonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverplayer), packet(entitydragonbase, tailsweepstate));
            }

            return;
        }
    }

    @SubscribeEvent
    public static void leaving(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof DragonBaseEntity entitydragonbase) {
            STATES.remove(entitydragonbase);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        STATES.clear();
    }
}
