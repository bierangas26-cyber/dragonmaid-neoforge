package com.example.dragonmaid.combat;

import com.example.dragonmaid.animation.BreathState;
import com.example.dragonmaid.network.BreathAnimationMessage;
import com.example.dragonmaid.network.BreathForecastMessage;
import com.example.dragonmaid.network.DragonNetwork;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.example.dragonmaid.compat.net.PacketDistributor;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class BreathAnimationSync {
    private static final Map<DragonBaseEntity, BreathAnimationSync.Entry> STATES = new WeakHashMap<>();
    private static long nextRound;

    private BreathAnimationSync() {
    }

    public static void beginTick(DragonBaseEntity dragon) {
        BreathAnimationSync.Entry breathanimationsync$entry = STATES.get(dragon);
        if (breathanimationsync$entry != null) {
            breathanimationsync$entry.forecastValid = false;
        }
    }

    public static void forecast(DragonBaseEntity dragon, int ticksUntilFire) {
        BreathAnimationSync.Entry breathanimationsync$entry = STATES.get(dragon);
        if (breathanimationsync$entry != null && breathanimationsync$entry.round.state().running() && ticksUntilFire > 0 && ticksUntilFire <= 6) {
            if (breathanimationsync$entry.round.state().lightning() || breathanimationsync$entry.round.state().firstAt() >= 0L) {
                long i = dragon.level().getGameTime() + (long)ticksUntilFire;
                int j = breathanimationsync$entry.round.state().sequence() + 1;
                breathanimationsync$entry.forecastValid = true;
                if (breathanimationsync$entry.forecastAt != i || breathanimationsync$entry.forecastSequence != j) {
                    breathanimationsync$entry.forecastAt = i;
                    breathanimationsync$entry.forecastSequence = j;
                    breathanimationsync$entry.forecastRevision++;
                }
            }
        }
    }

    public static void begin(DragonBaseEntity dragon, int ticks, boolean lightning) {
        BreathAnimationSync.Entry breathanimationsync$entry = STATES.get(dragon);
        if (breathanimationsync$entry == null || !breathanimationsync$entry.round.state().running()) {
            STATES.put(dragon, new BreathAnimationSync.Entry(new BreathState.Round(++nextRound, dragon.level().getGameTime(), ticks, lightning)));
        }
    }

    public static void phase(DragonBaseEntity dragon, int phase) {
        BreathAnimationSync.Entry breathanimationsync$entry = STATES.get(dragon);
        if (breathanimationsync$entry != null) {
            breathanimationsync$entry.round.phase(phase, dragon.level().getGameTime());
        }
    }

    public static void preparationRemaining(DragonBaseEntity dragon, int remaining) {
        BreathAnimationSync.Entry breathanimationsync$entry = STATES.get(dragon);
        if (breathanimationsync$entry != null) {
            breathanimationsync$entry.round.preparationRemaining(remaining, dragon.level().getGameTime());
        }
    }

    public static void shot(DragonBaseEntity dragon, int type) {
        BreathAnimationSync.Entry breathanimationsync$entry = STATES.get(dragon);
        if (breathanimationsync$entry != null) {
            breathanimationsync$entry.round.shot(type, dragon.level().getGameTime());
        }
    }

    public static void flush(DragonBaseEntity dragon) {
        BreathAnimationSync.Entry breathanimationsync$entry = STATES.get(dragon);
        if (breathanimationsync$entry != null) {
            BreathState breathstate = breathanimationsync$entry.round.state();
            if ((!breathanimationsync$entry.forecastValid || !breathstate.running() || breathstate.sequence() >= breathanimationsync$entry.forecastSequence)
                && breathanimationsync$entry.forecastAt >= 0L) {
                breathanimationsync$entry.forecastAt = -1L;
                breathanimationsync$entry.forecastRevision++;
            }

            if (!breathstate.lightning() && breathstate.sequence() > breathanimationsync$entry.soundedSequence) {
                breathanimationsync$entry.soundedSequence = breathstate.sequence();
                if (breathstate.shotAt() == dragon.level().getGameTime()) {
                    dragon.playSound(SoundEvents.BLAZE_SHOOT, 1.5F, 0.95F + dragon.getRandom().nextFloat() * 0.1F);
                }
            }

            if (breathanimationsync$entry.sentRevision != breathstate.revision()) {
                breathanimationsync$entry.sentRevision = breathstate.revision();
                DragonNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> dragon), packet(dragon, breathanimationsync$entry));
            }

            if (breathanimationsync$entry.sentForecastRevision != breathanimationsync$entry.forecastRevision) {
                breathanimationsync$entry.sentForecastRevision = breathanimationsync$entry.forecastRevision;
                DragonNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> dragon), forecastPacket(dragon, breathanimationsync$entry));
            }
        }
    }

    private static BreathForecastMessage forecastPacket(DragonBaseEntity dragon, BreathAnimationSync.Entry entry) {
        return new BreathForecastMessage(
            dragon.level().dimension().location(),
            dragon.getId(),
            dragon.getUUID(),
            entry.round.state().round(),
            entry.forecastRevision,
            entry.forecastSequence,
            entry.forecastAt
        );
    }

    private static BreathAnimationMessage packet(DragonBaseEntity dragon, BreathAnimationSync.Entry entry) {
        return new BreathAnimationMessage(dragon.level().dimension().location(), dragon.getId(), dragon.getUUID(), entry.round.state());
    }

    @SubscribeEvent
    public static void tracking(StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverplayer && event.getTarget() instanceof DragonBaseEntity entitydragonbase) {
            BreathAnimationSync.Entry breathanimationsync$entry = STATES.get(entitydragonbase);
            if (breathanimationsync$entry != null) {
                DragonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverplayer), packet(entitydragonbase, breathanimationsync$entry));
                if (breathanimationsync$entry.forecastAt > entitydragonbase.level().getGameTime()) {
                    DragonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverplayer), forecastPacket(entitydragonbase, breathanimationsync$entry));
                }
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        STATES.clear();
    }

    private static final class Entry {
        BreathState.Round round;
        int sentRevision;
        int forecastRevision;
        int sentForecastRevision;
        int forecastSequence;
        int soundedSequence;
        long forecastAt = -1L;
        boolean forecastValid;

        Entry(BreathState.Round round) {
            this.round = round;
        }
    }
}
