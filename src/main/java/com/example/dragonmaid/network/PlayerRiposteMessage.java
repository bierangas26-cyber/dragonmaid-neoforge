package com.example.dragonmaid.network;

import com.example.dragonmaid.client.PlayerBladeAnimation;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.net.PacketDistributor;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;

/**
 * 1.2.7: 长剑弹反反击的客户端表现（槽位 / 物品 / 使用序列 / 恢复 tick）。
 * 上游为这条消息单独开了一个 SimpleChannel（player_riposte, 协议 "1"）；
 * 本移植收敛到 DragonNetwork.CHANNEL，id = 19。
 */
public record PlayerRiposteMessage(int slot, int itemId, int useSequence, int recoveryTicks) {
    static void encode(PlayerRiposteMessage message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.slot);
        buffer.writeVarInt(message.itemId);
        buffer.writeVarInt(message.useSequence);
        buffer.writeVarInt(message.recoveryTicks);
    }

    static PlayerRiposteMessage decode(FriendlyByteBuf buffer) {
        return new PlayerRiposteMessage(
            buffer.readUnsignedByte(),
            buffer.readVarInt(),
            buffer.isReadable() ? buffer.readVarInt() : -1,
            buffer.isReadable() ? buffer.readVarInt() : 0
        );
    }

    static void handle(PlayerRiposteMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PlayerRiposteMessage.Client.accept(message)));
        context.setPacketHandled(true);
    }

    public static void send(ServerPlayer player, Item item, int useSequence, int recoveryTicks) {
        if (player == null || item == null) {
            return;
        }

        DragonNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new PlayerRiposteMessage(player.getInventory().selected, Item.getId(item), useSequence, recoveryTicks)
        );
    }

    private static final class Client {
        static void accept(PlayerRiposteMessage message) {
            PlayerBladeAnimation.riposte(message.slot, message.itemId, message.useSequence, message.recoveryTicks);
        }
    }
}
