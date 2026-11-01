package com.example.dragonmaid.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.NetBuf;

public record ExecutionPromptMessage(UUID dragon, UUID token, Component name) {
    public static void encode(ExecutionPromptMessage message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.dragon);
        buffer.writeUUID(message.token);
        NetBuf.writeComponent(buffer, message.name);
    }

    public static ExecutionPromptMessage decode(FriendlyByteBuf buffer) {
        return new ExecutionPromptMessage(buffer.readUUID(), buffer.readUUID(), NetBuf.readComponent(buffer));
    }

    public static void handle(ExecutionPromptMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> com.example.dragonmaid.compat.ClientBridge.call("com.example.dragonmaid.client.ExecutionPrompt", "open", message));
        context.setPacketHandled(true);
    }
}
