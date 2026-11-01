package com.example.dragonmaid.compat.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CompatPayload(int id, byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CompatPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("dragonmaid", "compat"));

    public static final StreamCodec<FriendlyByteBuf, CompatPayload> CODEC = new StreamCodec<>() {
        @Override
        public CompatPayload decode(FriendlyByteBuf buf) {
            return new CompatPayload(buf.readVarInt(), buf.readByteArray());
        }

        @Override
        public void encode(FriendlyByteBuf buf, CompatPayload payload) {
            buf.writeVarInt(payload.id());
            buf.writeByteArray(payload.data());
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
