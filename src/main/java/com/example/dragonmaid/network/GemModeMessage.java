package com.example.dragonmaid.network;

import com.example.dragonmaid.compat.StackCodec;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.item.TrinityDragonGemItem;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.2: 客户端请求切换 Trinity Dragon Gem 的 continuous/once 绑定模式（服务端校验槽位与预期物品）。
 * 1.21.1 没有 FriendlyByteBuf.writeItem，ItemStack 走 NBT（服务端解码时 registry access 可用）。
 */
public record GemModeMessage(int menuId, int slot, ItemStack expected) {
    public static void encode(GemModeMessage m, FriendlyByteBuf b) {
        b.writeVarInt(m.menuId);
        b.writeVarInt(m.slot);
        b.writeNbt(StackCodec.save(m.expected, new CompoundTag()));
    }

    public static GemModeMessage decode(FriendlyByteBuf b) {
        return new GemModeMessage(b.readVarInt(), b.readVarInt(), StackCodec.load(b.readNbt()));
    }

    public boolean apply(ServerPlayer player) {
        if (player != null && !player.isSpectator()) {
            AbstractContainerMenu menu = player.containerMenu;
            if (menu.containerId != this.menuId || !menu.stillValid(player)) {
                return false;
            } else if (this.slot == -1) {
                ItemStack carried = menu.getCarried();
                if (carried.getItem() instanceof TrinityDragonGemItem
                    && ItemStack.isSameItemSameComponents(carried, this.expected)
                    && carried.getCount() == this.expected.getCount()) {
                    TrinityDragonGemItem.clearDeadBinding(carried, player.serverLevel());
                    TrinityDragonGemItem.toggleMode(carried);
                    menu.setCarried(carried);
                    menu.broadcastChanges();
                    return true;
                } else {
                    return false;
                }
            } else if (this.slot >= 0 && this.slot < menu.slots.size()) {
                Slot target = menu.getSlot(this.slot);
                ItemStack stack = target.getItem();
                if (target.mayPickup(player)
                    && target.mayPlace(stack)
                    && stack.getItem() instanceof TrinityDragonGemItem
                    && ItemStack.isSameItemSameComponents(stack, this.expected)
                    && stack.getCount() == this.expected.getCount()) {
                    TrinityDragonGemItem.clearDeadBinding(stack, player.serverLevel());
                    TrinityDragonGemItem.toggleMode(stack);
                    target.setChanged();
                    menu.broadcastChanges();
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static void handle(GemModeMessage m, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> m.apply(context.getSender()));
        context.setPacketHandled(true);
    }
}
