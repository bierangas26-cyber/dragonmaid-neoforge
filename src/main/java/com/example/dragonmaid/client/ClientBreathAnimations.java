package com.example.dragonmaid.client;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.network.BreathAnimationMessage;
import com.example.dragonmaid.network.BreathForecastMessage;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

public final class ClientBreathAnimations {
    private static final Map<UUID, ClientBreathAnimations.Entry> ENTRIES = new HashMap<>();
    private static ClientLevel world;

    private ClientBreathAnimations() {
    }

    private static ClientLevel level() {
        ClientLevel clientlevel = Minecraft.getInstance().level;
        if (world != clientlevel) {
            ENTRIES.clear();
            world = clientlevel;
        }

        return clientlevel;
    }

    public static void accept(BreathAnimationMessage message) {
        ClientLevel clientlevel = level();
        if (clientlevel != null && clientlevel.dimension().location().equals(message.dimension())) {
            Entity entity = clientlevel.getEntity(message.entityId());
            if (entity == null || entity.getUUID().equals(message.uuid()) && entity instanceof DragonBaseEntity) {
                ClientBreathAnimations.Entry clientbreathanimations$entry = ENTRIES.computeIfAbsent(
                    message.uuid(), ignored -> new ClientBreathAnimations.Entry()
                );
                long i = clientbreathanimations$entry.timeline.state() == null ? -1L : clientbreathanimations$entry.timeline.state().round();
                if (clientbreathanimations$entry.timeline.accept(message.state())) {
                    if (i != message.state().round()) {
                        clientbreathanimations$entry.seenEligible = false;
                    }

                    clientbreathanimations$entry.entityId = message.entityId();
                    clientbreathanimations$entry.seenAt = clientlevel.getGameTime();
                }
            }
        }
    }

    public static void forecast(BreathForecastMessage message) {
        ClientLevel clientlevel = level();
        if (clientlevel != null && clientlevel.dimension().location().equals(message.dimension())) {
            ClientBreathAnimations.Entry clientbreathanimations$entry = ENTRIES.get(message.uuid());
            if (clientbreathanimations$entry != null && clientbreathanimations$entry.entityId == message.entityId()) {
                clientbreathanimations$entry.timeline
                    .forecast(message.round(), message.revision(), message.sequence(), message.fireAt(), (double)clientlevel.getGameTime());
            }
        }
    }

    static BreathTimeline.EyeFrame eye(DragonBaseEntity dragon, float partial) {
        ClientLevel clientlevel = level();
        ClientBreathAnimations.Entry clientbreathanimations$entry = ENTRIES.get(dragon.getUUID());
        return clientlevel != null
                && clientbreathanimations$entry != null
                && clientbreathanimations$entry.entityId == dragon.getId()
                && !suppressed(dragon)
                && DragonForms.state(dragon).dragonmaid$getAction() != 5
            ? clientbreathanimations$entry.timeline.eye((double)((float)clientlevel.getGameTime() + partial))
            : BreathTimeline.EyeFrame.OPEN;
    }

    static void tick() {
        ClientLevel clientlevel = level();
        if (clientlevel != null) {
            Iterator<Map.Entry<UUID, ClientBreathAnimations.Entry>> iterator = ENTRIES.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, ClientBreathAnimations.Entry> entry = iterator.next();
                ClientBreathAnimations.Entry clientbreathanimations$entry = entry.getValue();
                Entity entity = clientlevel.getEntity(clientbreathanimations$entry.entityId);
                if (entity == null) {
                    if (clientlevel.getGameTime() - clientbreathanimations$entry.seenAt > 100L) {
                        iterator.remove();
                    }
                } else {
                    if (entity instanceof DragonBaseEntity) {
                        DragonBaseEntity entitydragonbase = (DragonBaseEntity)entity;
                        if (entitydragonbase.getUUID().equals(entry.getKey())) {
                            clientbreathanimations$entry.seenAt = clientlevel.getGameTime();
                            updateSuppression(clientbreathanimations$entry, entitydragonbase, (double)clientlevel.getGameTime());
                            continue;
                        }
                    }

                    iterator.remove();
                }
            }
        }
    }

    private static boolean suppressed(DragonBaseEntity dragon) {
        return !DragonForms.isHumanoid(dragon) || dragon.isModelDead() || dragon.isSleeping() || dragon.isPassenger() || dragon.getCommand() == 1;
    }

    private static void updateSuppression(ClientBreathAnimations.Entry entry, DragonBaseEntity dragon, double now) {
        if (suppressed(dragon)) {
            if (entry.seenEligible) {
                entry.timeline.cancel(now);
            }
        } else {
            entry.seenEligible = true;
            if (DragonForms.state(dragon).dragonmaid$getAction() == 1 && !entry.timeline.state().running()) {
                entry.timeline.cancel(now);
            }
        }
    }

    static BreathTimeline.Frame frame(DragonBaseEntity dragon, float partial) {
        ClientLevel clientlevel = level();
        ClientBreathAnimations.Entry clientbreathanimations$entry = ENTRIES.get(dragon.getUUID());
        if (clientlevel != null && clientbreathanimations$entry != null && clientbreathanimations$entry.entityId == dragon.getId()) {
            double d0 = (double)((float)clientlevel.getGameTime() + partial);
            updateSuppression(clientbreathanimations$entry, dragon, d0);
            return clientbreathanimations$entry.timeline.frame(d0);
        } else {
            return null;
        }
    }

    private static final class Entry {
        final BreathTimeline timeline = new BreathTimeline();
        int entityId;
        long seenAt;
        boolean seenEligible;

        private Entry() {
        }
    }
}
