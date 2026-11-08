package com.example.dragonmaid.network;

import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.movement.PerchFlight;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;

public record PerchFlightInputMessage(boolean jump, boolean shift, boolean groundPress) {
    public static void encode(PerchFlightInputMessage m, FriendlyByteBuf out) {
        out.writeByte((m.jump ? 1 : 0) | (m.shift ? 2 : 0) | (m.groundPress ? 4 : 0));
    }

    public static PerchFlightInputMessage decode(FriendlyByteBuf in) {
        int flags = in.readUnsignedByte();
        return new PerchFlightInputMessage((flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0);
    }

    public static void handle(PerchFlightInputMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> PerchFlight.acceptInput(context.getSender(), message.jump, message.shift, message.groundPress));
        context.setPacketHandled(true);
    }
}
