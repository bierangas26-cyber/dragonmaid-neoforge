package com.example.dragonmaid.network;

import com.example.dragonmaid.combat.BreathFan;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.NetBuf;

public record BreathWaveMessage(ResourceLocation dimension, BreathFan fan, boolean ice, long gameTime, int sourceId, UUID sourceUuid) {
    public BreathWaveMessage(ResourceLocation dimension, BreathFan fan, boolean ice, long gameTime) {
        this(dimension, fan, ice, gameTime, -1, null);
    }

    public static void encode(BreathWaveMessage message, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(message.dimension);
        writeVector(buffer, message.fan.origin());
        writeVector(buffer, message.fan.forward());
        buffer.writeDouble(message.fan.range());
        buffer.writeDouble(message.fan.halfAngle());
        buffer.writeDouble(message.fan.speed());
        buffer.writeBoolean(message.ice);
        buffer.writeLong(message.gameTime);
        buffer.writeBoolean(message.sourceUuid != null);
        if (message.sourceUuid != null) {
            buffer.writeVarInt(message.sourceId);
            buffer.writeUUID(message.sourceUuid);
        }
    }

    public static BreathWaveMessage decode(FriendlyByteBuf buffer) {
        ResourceLocation resourcelocation = buffer.readResourceLocation();
        BreathFan breathfan = new BreathFan(readVector(buffer), readVector(buffer), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        boolean flag = buffer.readBoolean();
        long i = buffer.readLong();
        return buffer.isReadable() && buffer.readBoolean()
            ? new BreathWaveMessage(resourcelocation, breathfan, flag, i, buffer.readVarInt(), buffer.readUUID())
            : new BreathWaveMessage(resourcelocation, breathfan, flag, i);
    }

    private static void writeVector(FriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVector(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(BreathWaveMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> com.example.dragonmaid.compat.ClientBridge.call("com.example.dragonmaid.client.ClientBreathWaves", "accept", message));
        context.setPacketHandled(true);
    }
}
