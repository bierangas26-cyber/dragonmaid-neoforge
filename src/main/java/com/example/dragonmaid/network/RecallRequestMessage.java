package com.example.dragonmaid.network;

import com.example.dragonmaid.menu.RecallMenu;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;

public record RecallRequestMessage(int menuId, UUID dragon, int quotedCost) {
    public static void encode(RecallRequestMessage message, FriendlyByteBuf out) {
        out.writeVarInt(message.menuId);
        out.writeUUID(message.dragon);
        out.writeVarInt(message.quotedCost);
    }

    public static RecallRequestMessage decode(FriendlyByteBuf in) {
        return new RecallRequestMessage(in.readVarInt(), in.readUUID(), in.readVarInt());
    }

    public static void handle(RecallRequestMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer serverplayer = context.getSender();
            if (serverplayer != null && serverplayer.containerMenu instanceof RecallMenu recallmenu && recallmenu.containerId == message.menuId) {
                recallmenu.recall(serverplayer, message.dragon, message.quotedCost);
            }
        });
        context.setPacketHandled(true);
    }
}
