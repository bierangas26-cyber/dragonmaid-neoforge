package com.example.dragonmaid.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.NetBuf;

public record BreathImpactMessage(ResourceLocation dimension, long waveId, long gameTime, boolean ice, List<Vec3> points) {
    public BreathImpactMessage(ResourceLocation dimension, long waveId, long gameTime, boolean ice, List<Vec3> points) {
        points = List.copyOf(points);
        if (points.size() > 6) {
            throw new IllegalArgumentException("Too many impact points");
        } else {
            this.dimension = dimension;
            this.waveId = waveId;
            this.gameTime = gameTime;
            this.ice = ice;
            this.points = points;
        }
    }

    public static void encode(BreathImpactMessage m, FriendlyByteBuf b) {
        b.writeResourceLocation(m.dimension);
        b.writeLong(m.waveId);
        b.writeLong(m.gameTime);
        b.writeBoolean(m.ice);
        b.writeVarInt(m.points.size());

        for (Vec3 vec3 : m.points) {
            b.writeDouble(vec3.x);
            b.writeDouble(vec3.y);
            b.writeDouble(vec3.z);
        }
    }

    public static BreathImpactMessage decode(FriendlyByteBuf b) {
        ResourceLocation resourcelocation = b.readResourceLocation();
        long i = b.readLong();
        long j = b.readLong();
        boolean flag = b.readBoolean();
        int k = b.readVarInt();
        if (k >= 0 && k <= 6) {
            List<Vec3> list = new ArrayList<>(k);

            for (int l = 0; l < k; l++) {
                list.add(new Vec3(b.readDouble(), b.readDouble(), b.readDouble()));
            }

            return new BreathImpactMessage(resourcelocation, i, j, flag, list);
        } else {
            throw new IllegalArgumentException("Invalid impact count");
        }
    }

    public static void handle(BreathImpactMessage m, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> com.example.dragonmaid.compat.ClientBridge.call("com.example.dragonmaid.client.ClientBreathWaves", "impact", m));
        context.setPacketHandled(true);
    }
}
