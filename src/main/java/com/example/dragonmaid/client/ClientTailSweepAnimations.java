package com.example.dragonmaid.client;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.network.TailSweepMessage;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class ClientTailSweepAnimations {
    private static final Map<UUID, ClientTailSweepAnimations.Entry> ENTRIES = new HashMap<>();
    private static ClientLevel world;
    private static long lastTime = Long.MIN_VALUE;

    private ClientTailSweepAnimations() {
    }

    static void clear() {
        ENTRIES.clear();
        world = null;
        lastTime = Long.MIN_VALUE;
    }

    private static ClientLevel level() {
        ClientLevel clientlevel = Minecraft.getInstance().level;
        if (world != clientlevel || clientlevel != null && clientlevel.getGameTime() < lastTime) {
            ENTRIES.clear();
            world = clientlevel;
        }

        lastTime = clientlevel == null ? Long.MIN_VALUE : clientlevel.getGameTime();
        return clientlevel;
    }

    public static void accept(TailSweepMessage message) {
        ClientLevel clientlevel = level();
        if (clientlevel != null && clientlevel.dimension().location().equals(message.dimension())) {
            Entity entity = clientlevel.getEntity(message.entityId());
            if (entity == null || entity instanceof DragonBaseEntity && entity.getUUID().equals(message.uuid())) {
                ClientTailSweepAnimations.Entry clienttailsweepanimations$entry = ENTRIES.computeIfAbsent(
                    message.uuid(), ignored -> new ClientTailSweepAnimations.Entry()
                );
                long i = clienttailsweepanimations$entry.timeline.state() == null ? Long.MIN_VALUE : clienttailsweepanimations$entry.timeline.state().round();
                if (clienttailsweepanimations$entry.timeline.accept(message.state())) {
                    if (i != message.state().round()) {
                        clienttailsweepanimations$entry.seenEligible = false;
                        clienttailsweepanimations$entry.invalidated = false;
                    }

                    clienttailsweepanimations$entry.entityId = message.entityId();
                    clienttailsweepanimations$entry.seenAt = clientlevel.getGameTime();
                } else if (clienttailsweepanimations$entry.timeline.state() == null) {
                    ENTRIES.remove(message.uuid());
                }
            }
        }
    }

    static void tick() {
        ClientLevel clientlevel = level();
        if (clientlevel != null) {
            Iterator<Map.Entry<UUID, ClientTailSweepAnimations.Entry>> iterator = ENTRIES.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, ClientTailSweepAnimations.Entry> entry = iterator.next();
                ClientTailSweepAnimations.Entry clienttailsweepanimations$entry = entry.getValue();
                Entity entity = clientlevel.getEntity(clienttailsweepanimations$entry.entityId);
                if (clientlevel.getGameTime() - clienttailsweepanimations$entry.timeline.state().startedAt() <= 100L
                    && (entity == null || entity instanceof DragonBaseEntity && entity.getUUID().equals(entry.getKey()))) {
                    if (entity instanceof DragonBaseEntity entitydragonbase) {
                        updateSuppression(clienttailsweepanimations$entry, entitydragonbase);
                    } else if (clientlevel.getGameTime() - clienttailsweepanimations$entry.seenAt > 100L) {
                        iterator.remove();
                    }
                } else {
                    iterator.remove();
                }
            }
        }
    }

    private static boolean suppressed(DragonBaseEntity dragon) {
        return !DragonForms.isHumanoid(dragon)
            || dragon.isModelDead()
            || HumanoidLife.isInjured(dragon)
            || dragon.isSleeping()
            || dragon.isPassenger()
            || DragonForms.isSeated(dragon)
            || dragon.isInWaterOrBubble()
            || dragon.isInLava();
    }

    private static void updateSuppression(ClientTailSweepAnimations.Entry entry, DragonBaseEntity dragon) {
        if (suppressed(dragon)) {
            if (entry.seenEligible) {
                entry.invalidated = true;
            }
        } else {
            entry.seenEligible = true;
        }
    }

    static TailSweepTimeline.Frame frame(DragonBaseEntity dragon, float partial) {
        ClientLevel clientlevel = level();
        ClientTailSweepAnimations.Entry clienttailsweepanimations$entry = ENTRIES.get(dragon.getUUID());
        if (clientlevel != null
            && dragon.level() == clientlevel
            && clienttailsweepanimations$entry != null
            && clienttailsweepanimations$entry.entityId == dragon.getId()) {
            updateSuppression(clienttailsweepanimations$entry, dragon);
            return !clienttailsweepanimations$entry.invalidated && !suppressed(dragon)
                ? clienttailsweepanimations$entry.timeline.frame((double)clientlevel.getGameTime() + (double)partial)
                : null;
        } else {
            return null;
        }
    }

    static boolean ownsPose(DragonBaseEntity dragon) {
        return frame(dragon, 0.0F) != null;
    }

    static float bodyYaw(DragonBaseEntity dragon, float partial, float ordinaryYaw) {
        TailSweepTimeline.Frame tailsweeptimeline$frame = frame(dragon, partial);
        return tailsweeptimeline$frame == null
            ? ordinaryYaw
            : Mth.rotLerp(1.0F - tailsweeptimeline$frame.weight(), tailsweeptimeline$frame.yaw(), ordinaryYaw);
    }

    private static final class Entry {
        final TailSweepTimeline timeline = new TailSweepTimeline();
        int entityId;
        long seenAt;
        boolean seenEligible;
        boolean invalidated;

        private Entry() {
        }
    }
}
