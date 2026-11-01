package com.example.dragonmaid.network;

import com.example.dragonmaid.animation.TailSweepState;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.NetBuf;

public record TailSweepMessage(ResourceLocation dimension, int entityId, UUID uuid, TailSweepState state) {
    public static void encode(TailSweepMessage message, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(message.dimension);
        buffer.writeVarInt(message.entityId);
        buffer.writeUUID(message.uuid);
        TailSweepState tailsweepstate = message.state;
        buffer.writeLong(tailsweepstate.round());
        buffer.writeVarInt(tailsweepstate.revision());
        buffer.writeLong(tailsweepstate.startedAt());
        buffer.writeFloat(tailsweepstate.yaw());
        buffer.writeLong(tailsweepstate.cancelledAt());
    }

    public static TailSweepMessage decode(FriendlyByteBuf buffer) {
        return new TailSweepMessage(
            buffer.readResourceLocation(),
            buffer.readVarInt(),
            buffer.readUUID(),
            new TailSweepState(buffer.readLong(), buffer.readVarInt(), buffer.readLong(), buffer.readFloat(), buffer.readLong())
        );
    }

    public static void handle(TailSweepMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> com.example.dragonmaid.compat.ClientBridge.call("com.example.dragonmaid.client.ClientTailSweepAnimations", "accept", message));
        context.setPacketHandled(true);
    }
}
