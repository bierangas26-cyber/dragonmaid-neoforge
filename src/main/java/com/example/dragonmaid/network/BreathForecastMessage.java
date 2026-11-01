package com.example.dragonmaid.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.NetBuf;

public record BreathForecastMessage(ResourceLocation dimension, int entityId, UUID uuid, long round, int revision, int sequence, long fireAt) {
    public static void encode(BreathForecastMessage m, FriendlyByteBuf b) {
        b.writeResourceLocation(m.dimension);
        b.writeVarInt(m.entityId);
        b.writeUUID(m.uuid);
        b.writeLong(m.round);
        b.writeVarInt(m.revision);
        b.writeVarInt(m.sequence);
        b.writeLong(m.fireAt);
    }

    public static BreathForecastMessage decode(FriendlyByteBuf b) {
        return new BreathForecastMessage(b.readResourceLocation(), b.readVarInt(), b.readUUID(), b.readLong(), b.readVarInt(), b.readVarInt(), b.readLong());
    }

    public static void handle(BreathForecastMessage m, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> com.example.dragonmaid.compat.ClientBridge.call("com.example.dragonmaid.client.ClientBreathAnimations", "forecast", m));
        context.setPacketHandled(true);
    }
}
