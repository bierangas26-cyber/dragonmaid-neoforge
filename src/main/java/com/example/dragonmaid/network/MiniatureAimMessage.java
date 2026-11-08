package com.example.dragonmaid.network;

import com.example.dragonmaid.combat.MiniatureShoulder;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;

public record MiniatureAimMessage(ResourceLocation dimension, int dragonId, UUID dragonUuid, int targetId) {
    public static MiniatureAimMessage of(DragonBaseEntity dragon, int targetId) {
        return new MiniatureAimMessage(dragon.level().dimension().location(), dragon.getId(), dragon.getUUID(), targetId);
    }

    public static void encode(MiniatureAimMessage m, FriendlyByteBuf out) {
        out.writeResourceLocation(m.dimension);
        out.writeVarInt(m.dragonId);
        out.writeUUID(m.dragonUuid);
        out.writeVarInt(m.targetId);
    }

    public static MiniatureAimMessage decode(FriendlyByteBuf in) {
        return new MiniatureAimMessage(in.readResourceLocation(), in.readVarInt(), in.readUUID(), in.readVarInt());
    }

    public static void handle(MiniatureAimMessage m, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MiniatureAimMessage.Client.apply(m)));
        context.setPacketHandled(true);
    }

    private static final class Client {
        static void apply(MiniatureAimMessage m) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null
                && level.dimension().location().equals(m.dimension)
                && level.getEntity(m.dragonId) instanceof DragonBaseEntity dragon
                && dragon.getUUID().equals(m.dragonUuid)) {
                ((MiniatureShoulder.AimHolder)dragon).dragonmaid$shoulderAim(m.targetId);
            }
        }
    }
}
