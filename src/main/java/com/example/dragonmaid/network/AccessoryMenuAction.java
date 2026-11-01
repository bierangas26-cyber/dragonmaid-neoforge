package com.example.dragonmaid.network;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.menu.AccessoryMenu;
import com.example.dragonmaid.menu.KnowledgeMenu;
import com.example.dragonmaid.mixin.DragonMenuAccessor;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.screen.menu.DragonMenu;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.example.dragonmaid.compat.net.NetworkHooks;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;

public record AccessoryMenuAction(int menuId, int entityId, UUID uuid) {
    public static void encode(AccessoryMenuAction message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.menuId);
        buffer.writeVarInt(message.entityId);
        buffer.writeUUID(message.uuid);
    }

    public static AccessoryMenuAction decode(FriendlyByteBuf buffer) {
        return new AccessoryMenuAction(buffer.readVarInt(), buffer.readVarInt(), buffer.readUUID());
    }

    public static void handle(AccessoryMenuAction message, Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> message.openFor(context.getSender()));
        context.setPacketHandled(true);
    }

    public boolean openFor(ServerPlayer player) {
        if (player != null && player.containerMenu.containerId == this.menuId && player.containerMenu instanceof DragonMenu) {
            DragonBaseEntity entitydragonbase = ((DragonMenuAccessor)player.containerMenu).dragonmaid$getDragon();
            return DragonAccessories.canManage(entitydragonbase, player)
                    && entitydragonbase.getId() == this.entityId
                    && entitydragonbase.getUUID().equals(this.uuid)
                    && player.containerMenu.stillValid(player)
                ? open(player, entitydragonbase)
                : false;
        } else {
            return false;
        }
    }

    public static boolean open(ServerPlayer player, DragonBaseEntity dragon) {
        if (!DragonAccessories.canManage(dragon, player)) {
            return false;
        } else {
            NetworkHooks.openScreen(
                player,
                new SimpleMenuProvider((id, inventory, p) -> new AccessoryMenu(id, inventory, dragon), Component.translatable("menu.dragonmaid.accessories")),
                buffer -> {
                    buffer.writeVarInt(dragon.getId());
                    buffer.writeUUID(dragon.getUUID());
                }
            );
            if (player.containerMenu instanceof AccessoryMenu accessorymenu && accessorymenu.dragon() == dragon) {
                return true;
            }

            return false;
        }
    }

    public static boolean openKnowledge(ServerPlayer player, DragonBaseEntity dragon) {
        if (!DragonForms.canManage(dragon, player)) {
            return false;
        } else {
            NetworkHooks.openScreen(
                player,
                new SimpleMenuProvider((id, inventory, p) -> new KnowledgeMenu(id, inventory, dragon), Component.translatable("menu.dragonmaid.knowledge")),
                buffer -> {
                    buffer.writeVarInt(dragon.getId());
                    buffer.writeUUID(dragon.getUUID());
                }
            );
            if (player.containerMenu instanceof KnowledgeMenu knowledgemenu && knowledgemenu.dragon() == dragon) {
                return true;
            }

            return false;
        }
    }

    public static boolean transform(ServerPlayer player, DragonBaseEntity dragon) {
        if (player != null && DragonForms.canManage(dragon, player) && player.containerMenu.stillValid(player)) {
            AbstractContainerMenu abstractcontainermenu = player.containerMenu;
            DragonBaseEntity entitydragonbase = abstractcontainermenu instanceof DragonMenu
                ? ((DragonMenuAccessor)abstractcontainermenu).dragonmaid$getDragon()
                : (
                    abstractcontainermenu instanceof AccessoryMenu accessorymenu
                        ? accessorymenu.dragon()
                        : (abstractcontainermenu instanceof KnowledgeMenu knowledgemenu ? knowledgemenu.dragon() : null)
                );
            if (entitydragonbase == dragon && DragonForms.toggle(dragon, player)) {
                player.closeContainer();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
