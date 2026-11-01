package com.example.dragonmaid.compat.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class PacketDistributor {
    private PacketDistributor() { }

    public static final class Target {
        private final List<ServerPlayer> players = new ArrayList<>();
        private Entity trackEntity;
        private boolean trackSelf;

        void add(ServerPlayer player) { this.players.add(player); }
        void track(Entity entity, boolean self) { this.trackEntity = entity; this.trackSelf = self; }
        public List<ServerPlayer> players() { return this.players; }
        public Entity trackedEntity() { return this.trackEntity; }
        public boolean trackSelf() { return this.trackSelf; }
    }

    public interface TargetFactory {
        Target with(Supplier<?> source);
    }

    public static final TargetFactory PLAYER = source -> {
        Target target = new Target();
        if (source.get() instanceof ServerPlayer player) { target.add(player); }
        return target;
    };

    public static final TargetFactory TRACKING_ENTITY = source -> {
        Target target = new Target();
        if (source.get() instanceof Entity entity) { target.track(entity, false); }
        return target;
    };

    public static final TargetFactory TRACKING_ENTITY_AND_SELF = source -> {
        Target target = new Target();
        if (source.get() instanceof Entity entity) { target.track(entity, true); }
        return target;
    };
}
