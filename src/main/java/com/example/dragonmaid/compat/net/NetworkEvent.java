package com.example.dragonmaid.compat.net;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NetworkEvent {
    private NetworkEvent() { }

    public static final class Context {
        private final IPayloadContext context;

        public Context(IPayloadContext context) { this.context = context; }

        public void enqueueWork(Runnable runnable) { this.context.enqueueWork(runnable); }

        public void setPacketHandled(boolean handled) { }

        public ServerPlayer getSender() {
            return this.context.player() instanceof ServerPlayer player ? player : null;
        }

        public IPayloadContext neoContext() { return this.context; }
    }
}
