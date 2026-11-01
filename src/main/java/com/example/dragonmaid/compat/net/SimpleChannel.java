package com.example.dragonmaid.compat.net;

import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SimpleChannel {
    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();
    private static final Map<Class<?>, Integer> IDS = new HashMap<>();
    private static boolean payloadRegistered;

    public <T> void registerMessage(int id, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<NetworkEvent.Context>> handler, Optional<NetworkDirection> direction) {
        registerMessage(id, type, encoder, decoder, handler);
    }

    @SuppressWarnings("unchecked")
    public <T> void registerMessage(int id, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        ENTRIES.put(id, new Entry(id, (BiConsumer<Object, FriendlyByteBuf>) encoder,
                (Function<FriendlyByteBuf, Object>) decoder,
                (BiConsumer<Object, Supplier<NetworkEvent.Context>>) handler));
        IDS.put(type, id);
    }

    public void send(PacketDistributor.Target target, Object message) {
        CompatPayload payload = encode(message);
        if (payload == null) { return; }
        if (target.trackedEntity() != null) {
            if (target.trackSelf()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(target.trackedEntity(), payload);
            } else {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(target.trackedEntity(), payload);
            }
        }
        for (ServerPlayer player : target.players()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public void sendToServer(Object message) {
        CompatPayload payload = encode(message);
        if (payload != null) { net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload); }
    }

    private CompatPayload encode(Object message) {
        Integer id = IDS.get(message.getClass());
        if (id == null) { return null; }
        // NOTE: id 已由 CompatPayload.CODEC 在 wire 层写出；这里 data 里**只放消息体**。
        // 旧实现多写了一次 id -> 解码端把 id 当第一个字段读掉，导致 ResourceLocation 等字段整体错位
        // （客户端日志: ResourceLocationException "Non [a-z0-9/._-] character ... minecraft:\u0013min"，0x13 就是 message id）。
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ENTRIES.get(id).encoder.accept(message, buf);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return new CompatPayload(id, data);
    }

    public static void bootstrap(RegisterPayloadHandlersEvent event) {
        if (payloadRegistered) { return; }
        payloadRegistered = true;
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(CompatPayload.TYPE, CompatPayload.CODEC, SimpleChannel::handle);
    }

    private static void handle(CompatPayload payload, IPayloadContext context) {
        Entry entry = ENTRIES.get(payload.id());
        if (entry == null) { return; }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
        Object message = entry.decoder.apply(buf);
        NetworkEvent.Context shim = new NetworkEvent.Context(context);
        entry.handler.accept(message, () -> shim);
    }

    private static final class Entry {
        final int id;
        final BiConsumer<Object, FriendlyByteBuf> encoder;
        final Function<FriendlyByteBuf, Object> decoder;
        final BiConsumer<Object, Supplier<NetworkEvent.Context>> handler;

        Entry(int id, BiConsumer<Object, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, Object> decoder,
                BiConsumer<Object, Supplier<NetworkEvent.Context>> handler) {
            this.id = id;
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
        }
    }
}
