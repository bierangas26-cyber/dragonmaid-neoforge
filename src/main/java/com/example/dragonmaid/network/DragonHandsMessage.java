package com.example.dragonmaid.network;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.compat.DistExecutor;
import com.example.dragonmaid.compat.StackCodec;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;

/**
 * 1.2: 龙的双手持物同步（Forge 1.20.1 的 FriendlyByteBuf.writeItem 在 1.21.1 已移除，
 * ItemStack 是 registry-aware codec，因此这里改用 CUSTOM_DATA 兼容的 NBT 载荷：
 * 服务端用 registry access 编码，客户端在 handler 里用 ClientLevel.registryAccess() 解码）。
 */
public record DragonHandsMessage(ResourceLocation dimension, int entityId, UUID uuid, CompoundTag main, CompoundTag off) {
    public static DragonHandsMessage of(DragonBaseEntity dragon) {
        return new DragonHandsMessage(
            dragon.level().dimension().location(),
            dragon.getId(),
            dragon.getUUID(),
            StackCodec.save(DragonHands.mainHand(dragon).copy(), new CompoundTag()),
            StackCodec.save(DragonHands.offHand(dragon).copy(), new CompoundTag())
        );
    }

    public ItemStack mainStack(HolderLookup.Provider registries) {
        return ItemStack.parseOptional(registries, this.main);
    }

    public ItemStack offStack(HolderLookup.Provider registries) {
        return ItemStack.parseOptional(registries, this.off);
    }

    public static void encode(DragonHandsMessage message, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(message.dimension);
        buffer.writeVarInt(message.entityId);
        buffer.writeUUID(message.uuid);
        buffer.writeNbt(message.main);
        buffer.writeNbt(message.off);
    }

    public static DragonHandsMessage decode(FriendlyByteBuf buffer) {
        return new DragonHandsMessage(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readUUID(), buffer.readNbt(), buffer.readNbt());
    }

    public static void handle(DragonHandsMessage message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DragonHandsMessage.Client.accept(message)));
        context.setPacketHandled(true);
    }

    private static final class Client {
        static void accept(DragonHandsMessage message) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null || !level.dimension().location().equals(message.dimension)) {
                return;
            }

            if (level.getEntity(message.entityId) instanceof DragonBaseEntity dragon && dragon.getUUID().equals(message.uuid)) {
                DragonHands hands = DragonHands.inventory(dragon);
                hands.setItem(0, message.mainStack(level.registryAccess()));
                hands.setItem(1, message.offStack(level.registryAccess()));
            }
        }
    }
}
