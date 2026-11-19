package com.example.dragonmaid.network;

import com.example.dragonmaid.client.PlayerKatanaPose;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.net.PacketDistributor;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;

/**
 * 1.2.7: 通知单个观察者播放"拔刀"第一人称/第三人称姿态。
 * 上游为这条消息单独开了一个 SimpleChannel（player_katana_draw, 协议 "1"）；
 * 本移植把所有消息收敛到 DragonNetwork.CHANNEL 的兼容适配层，id = 18。
 */
public record PlayerKatanaDrawMessage(int entityId, UUID uuid) {
    static void encode(PlayerKatanaDrawMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeUUID(message.uuid);
    }

    static PlayerKatanaDrawMessage decode(FriendlyByteBuf buffer) {
        return new PlayerKatanaDrawMessage(buffer.readVarInt(), buffer.readUUID());
    }

    static void handle(PlayerKatanaDrawMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PlayerKatanaDrawMessage.Client.accept(message)));
        context.setPacketHandled(true);
    }

    public static void send(ServerPlayer viewer, ServerPlayer wielder) {
        if (viewer == null || wielder == null) {
            return;
        }

        DragonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new PlayerKatanaDrawMessage(wielder.getId(), wielder.getUUID()));
    }

    private static final class Client {
        static void accept(PlayerKatanaDrawMessage message) {
            PlayerKatanaPose.startDraw(message.entityId, message.uuid);
        }
    }
}
