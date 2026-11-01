package com.example.dragonmaid.network;

import com.example.dragonmaid.recall.RecallLedger;
import com.example.dragonmaid.recall.RecallQuote;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.NetBuf;

public record RecallRosterMessage(int menuId, int page, int pages, List<RecallRosterMessage.Row> rows, Component status, boolean pending) {
    public static void encode(RecallRosterMessage message, FriendlyByteBuf out) {
        out.writeVarInt(message.menuId);
        out.writeVarInt(message.page);
        out.writeVarInt(message.pages);
        out.writeVarInt(message.rows.size());

        for (RecallRosterMessage.Row recallrostermessage$row : message.rows) {
            out.writeUUID(recallrostermessage$row.id);
            out.writeUtf(recallrostermessage$row.name, 128);
            out.writeUtf(recallrostermessage$row.type, 256);
            out.writeBoolean(recallrostermessage$row.male);
            out.writeVarInt(recallrostermessage$row.ageTicks);
            out.writeVarInt(recallrostermessage$row.knowledge);
            out.writeVarInt(recallrostermessage$row.knowledgeLevel);
            out.writeBoolean(recallrostermessage$row.humanoid);
            out.writeBoolean(recallrostermessage$row.injured);
            out.writeEnum(recallrostermessage$row.state);
            out.writeVarInt(recallrostermessage$row.cost);
            out.writeVarInt(recallrostermessage$row.distance);
            out.writeBoolean(recallrostermessage$row.lastKnown);
        }

        NetBuf.writeComponent(out, message.status);
        out.writeBoolean(message.pending);
    }

    public static RecallRosterMessage decode(FriendlyByteBuf in) {
        int i = in.readVarInt();
        int j = in.readVarInt();
        int k = in.readVarInt();
        int l = in.readVarInt();
        if (l >= 0 && l <= 6) {
            List<RecallRosterMessage.Row> list = new ArrayList<>(l);

            for (int i1 = 0; i1 < l; i1++) {
                list.add(
                    new RecallRosterMessage.Row(
                        in.readUUID(),
                        in.readUtf(128),
                        in.readUtf(256),
                        in.readBoolean(),
                        in.readVarInt(),
                        in.readVarInt(),
                        in.readVarInt(),
                        in.readBoolean(),
                        in.readBoolean(),
                        (RecallLedger.State)in.readEnum(RecallLedger.State.class),
                        in.readVarInt(),
                        in.readVarInt(),
                        in.readBoolean()
                    )
                );
            }

            return new RecallRosterMessage(i, j, k, List.copyOf(list), NetBuf.readComponent(in), in.readBoolean());
        } else {
            throw new IllegalArgumentException("Recall page too large");
        }
    }

    public static void handle(RecallRosterMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> com.example.dragonmaid.compat.ClientBridge.call("com.example.dragonmaid.client.RecallScreen", "receive", message));
        context.setPacketHandled(true);
    }

    public static record Row(
        UUID id,
        String name,
        String type,
        boolean male,
        int ageTicks,
        int knowledge,
        int knowledgeLevel,
        boolean humanoid,
        boolean injured,
        RecallLedger.State state,
        int cost,
        int distance,
        boolean lastKnown
    ) {
        public static RecallRosterMessage.Row from(RecallLedger.Entry e, int cost) {
            return from(e, RecallQuote.unknown(cost));
        }

        public static RecallRosterMessage.Row from(RecallLedger.Entry e, RecallQuote quote) {
            String s = e.name().length() > 128 ? e.name().substring(0, 128) : e.name();
            return new RecallRosterMessage.Row(
                e.dragon(),
                s,
                e.type(),
                e.male(),
                e.ageTicks(),
                e.knowledge(),
                e.knowledgeLevel(),
                e.humanoid(),
                e.injured(),
                e.state(),
                quote.cost(),
                quote.blocks(),
                quote.lastKnown()
            );
        }
    }
}
