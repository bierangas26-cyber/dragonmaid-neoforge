package com.example.dragonmaid.network;

import com.example.dragonmaid.animation.BreathState;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.NetBuf;

public record BreathAnimationMessage(ResourceLocation dimension, int entityId, UUID uuid, BreathState state) {
    public static void encode(BreathAnimationMessage m, FriendlyByteBuf b) {
        b.writeResourceLocation(m.dimension);
        b.writeVarInt(m.entityId);
        b.writeUUID(m.uuid);
        BreathState breathstate = m.state;
        b.writeLong(breathstate.round());
        b.writeVarInt(breathstate.revision());
        b.writeVarInt(breathstate.phase());
        b.writeBoolean(breathstate.lightning());
        b.writeLong(breathstate.prepareAt());
        b.writeVarInt(breathstate.prepareTicks());
        b.writeLong(breathstate.firstAt());
        b.writeLong(breathstate.shotAt());
        b.writeVarInt(breathstate.sequence());
        b.writeVarInt(breathstate.shotType());
        b.writeLong(breathstate.endedAt());
    }

    public static BreathAnimationMessage decode(FriendlyByteBuf b) {
        return new BreathAnimationMessage(
            b.readResourceLocation(),
            b.readVarInt(),
            b.readUUID(),
            new BreathState(
                b.readLong(),
                b.readVarInt(),
                b.readVarInt(),
                b.readBoolean(),
                b.readLong(),
                b.readVarInt(),
                b.readLong(),
                b.readLong(),
                b.readVarInt(),
                b.readVarInt(),
                b.readLong()
            )
        );
    }

    public static void handle(BreathAnimationMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> com.example.dragonmaid.compat.ClientBridge.call("com.example.dragonmaid.client.ClientBreathAnimations", "accept", message));
        context.setPacketHandled(true);
    }
}
