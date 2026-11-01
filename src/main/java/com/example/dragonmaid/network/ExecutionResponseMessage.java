package com.example.dragonmaid.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;

public record ExecutionResponseMessage(UUID token, boolean confirmed) {
    public static void encode(ExecutionResponseMessage message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.token);
        buffer.writeBoolean(message.confirmed);
    }

    public static ExecutionResponseMessage decode(FriendlyByteBuf buffer) {
        return new ExecutionResponseMessage(buffer.readUUID(), buffer.readBoolean());
    }

    public static void handle(ExecutionResponseMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                ExecutionConfirmation.respond(context.getSender(), message.token, message.confirmed);
            }
        });
        context.setPacketHandled(true);
    }
}
