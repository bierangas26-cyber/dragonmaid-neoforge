package com.example.dragonmaid.network;

import com.example.dragonmaid.accessory.MiniatureForm;
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

public record MiniatureSizeMessage(ResourceLocation dimension, int id, UUID uuid, boolean small, boolean worn) {
    public static MiniatureSizeMessage of(DragonBaseEntity dragon) {
        return new MiniatureSizeMessage(
            dragon.level().dimension().location(),
            dragon.getId(),
            dragon.getUUID(),
            MiniatureForm.isSmall(dragon),
            ((MiniatureForm.Holder)dragon).dragonmaid$miniWorn()
        );
    }

    public static void encode(MiniatureSizeMessage m, FriendlyByteBuf out) {
        out.writeResourceLocation(m.dimension);
        out.writeVarInt(m.id);
        out.writeUUID(m.uuid);
        out.writeBoolean(m.small);
        out.writeBoolean(m.worn);
    }

    public static MiniatureSizeMessage decode(FriendlyByteBuf in) {
        return new MiniatureSizeMessage(in.readResourceLocation(), in.readVarInt(), in.readUUID(), in.readBoolean(), in.readBoolean());
    }

    public static void handle(MiniatureSizeMessage m, Supplier<Context> ctx) {
        Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MiniatureSizeMessage.Client.accept(m)));
        context.setPacketHandled(true);
    }

    private static final class Client {
        static void accept(MiniatureSizeMessage m) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null
                && level.dimension().location().equals(m.dimension)
                && level.getEntity(m.id) instanceof DragonBaseEntity dragon
                && dragon.getUUID().equals(m.uuid)) {
                MiniatureForm.setState(dragon, m.small, m.worn);
            }
        }
    }
}
