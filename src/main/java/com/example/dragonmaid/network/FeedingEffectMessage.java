package com.example.dragonmaid.network;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.PacketDistributor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.NetBuf;

public record FeedingEffectMessage(ResourceLocation dimension, int entityId, UUID uuid, int itemId) {
    public static void send(DragonBaseEntity dragon, Item item) {
        DragonNetwork.CHANNEL
            .send(
                PacketDistributor.TRACKING_ENTITY.with(() -> dragon),
                new FeedingEffectMessage(dragon.level().dimension().location(), dragon.getId(), dragon.getUUID(), Item.getId(item))
            );
    }

    public static void encode(FeedingEffectMessage message, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(message.dimension);
        buffer.writeVarInt(message.entityId);
        buffer.writeUUID(message.uuid);
        buffer.writeVarInt(message.itemId);
    }

    public static FeedingEffectMessage decode(FriendlyByteBuf buffer) {
        return new FeedingEffectMessage(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt());
    }

    public static void handle(FeedingEffectMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> com.example.dragonmaid.compat.ClientBridge.call("com.example.dragonmaid.client.ClientMouthEffects", "receive", message));
        context.setPacketHandled(true);
    }
}
