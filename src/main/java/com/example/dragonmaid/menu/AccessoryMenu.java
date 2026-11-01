package com.example.dragonmaid.menu;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryInventory;
import com.example.dragonmaid.accessory.DragonStorage;
import com.example.dragonmaid.accessory.DragonStorageInventory;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.network.AccessoryMenuAction;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AccessoryMenu extends AbstractContainerMenu {
    public static final int GEAR_START = 0;
    public static final int GEAR_END = 5;
    public static final int ACCESSORY_START = 5;
    public static final int ACCESSORY_SLOTS = 3;
    public static final int ACCESSORY_END = 8;
    public static final int STORAGE_START = 8;
    public static final int STORAGE_END = 35;
    public static final int PLAYER_START = 35;
    public static final int PLAYER_MAIN_END = 62;
    public static final int PLAYER_END = 71;
    private final DragonBaseEntity dragon;
    private final Player viewer;
    private final Container gear;
    private final SimpleContainer accessories;
    private final SimpleContainer storage;
    private final SimpleContainerData data = new SimpleContainerData(3);

    public AccessoryMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, readDragon(inventory, buffer));
    }

    private static DragonBaseEntity readDragon(Inventory inventory, FriendlyByteBuf buffer) {
        int i = buffer.readVarInt();
        UUID uuid = buffer.readUUID();
        if (inventory.player.level().getEntity(i) instanceof DragonBaseEntity entitydragonbase && entitydragonbase.getUUID().equals(uuid)) {
            return entitydragonbase;
        }

        return null;
    }

    public AccessoryMenu(int id, Inventory inventory, DragonBaseEntity dragon) {
        super((MenuType)ModMenus.ACCESSORIES.get(), id);
        this.dragon = dragon;
        this.viewer = inventory.player;
        boolean flag = dragon != null && !inventory.player.level().isClientSide;
        this.gear = flag ? dragon.dragonInventory : new SimpleContainer(5);
        this.accessories = flag ? DragonForms.state(dragon).dragonmaid$getAccessories() : new DragonAccessoryInventory();
        this.storage = flag ? DragonForms.state(dragon).dragonmaid$getStorage() : new DragonStorageInventory();
        int[] aint = new int[]{90, 18, 36, 54, 72};

        for (int i = 0; i < 5; i++) {
            this.addSlot(DragonEquipmentSlots.gear(this.gear, i, 8, aint[i], () -> this.stillValid(this.viewer)));
        }

        for (int k = 0; k < 3; k++) {
            this.addSlot(DragonEquipmentSlots.accessory(this.accessories, k, 152, 28 + k * 18, dragon, this::unlockedSlots, () -> this.stillValid(this.viewer)));
        }

        for (int l = 0; l < 27; l++) {
            final int j = l;
            this.addSlot(new Slot(this.storage, l, 8 + l % 9 * 18, 117 + l / 9 * 18) {
                public boolean isActive() {
                    return j < AccessoryMenu.this.storageSlots();
                }

                public boolean mayPlace(ItemStack stack) {
                    return this.isActive() && AccessoryMenu.this.stillValid(AccessoryMenu.this.viewer);
                }

                public boolean mayPickup(Player player) {
                    return this.isActive() && AccessoryMenu.this.stillValid(player);
                }
            });
        }

        for (int i1 = 0; i1 < 3; i1++) {
            for (int k1 = 0; k1 < 9; k1++) {
                this.addSlot(hiddenPlayerSlot(inventory, k1 + i1 * 9 + 9));
            }
        }

        for (int j1 = 0; j1 < 9; j1++) {
            this.addSlot(hiddenPlayerSlot(inventory, j1));
        }

        this.addDataSlots(this.data);
        this.updateData();
        this.gear.startOpen(this.viewer);
        this.accessories.startOpen(this.viewer);
        this.storage.startOpen(this.viewer);
    }

    private static Slot hiddenPlayerSlot(Inventory inventory, int index) {
        return new Slot(inventory, index, 0, 0) {
            public boolean isActive() {
                return false;
            }
        };
    }

    private void updateData() {
        if (this.dragon != null && !this.dragon.level().isClientSide) {
            this.data.set(0, DragonAccessories.knowledgeLevel(this.dragon));
            this.data.set(1, DragonAccessories.unlockedSlots(this.dragon));
            this.data.set(2, DragonStorage.unlockedSlots(this.dragon));
        }
    }

    public void broadcastChanges() {
        this.updateData();
        super.broadcastChanges();
    }

    public int knowledgeLevel() {
        return this.dragon != null && !this.dragon.level().isClientSide ? DragonAccessories.knowledgeLevel(this.dragon) : Math.max(0, this.data.get(0));
    }

    public int unlockedSlots() {
        return this.dragon != null && !this.dragon.level().isClientSide
            ? DragonAccessories.unlockedSlots(this.dragon)
            : Math.max(0, Math.min(3, this.data.get(1)));
    }

    public int storageSlots() {
        return this.dragon != null && !this.dragon.level().isClientSide
            ? DragonStorage.unlockedSlots(this.dragon)
            : Math.max(0, Math.min(27, this.data.get(2)));
    }

    public DragonBaseEntity dragon() {
        return this.dragon;
    }

    public Container gear() {
        return this.gear;
    }

    public SimpleContainer accessories() {
        return this.accessories;
    }

    public SimpleContainer storage() {
        return this.storage;
    }

    public boolean stillValid(Player player) {
        return DragonAccessories.canManage(this.dragon, player) && (player.level().isClientSide || !this.dragon.hasInventoryChanged(this.gear));
    }

    private boolean current(Player player) {
        return player.containerMenu == this && this.stillValid(player);
    }

    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot < 35) {
            if (slot < 5 || slot >= 35 || ((Slot)this.slots.get(slot)).isActive()) {
                if (this.current(player)) {
                    super.clicked(slot, button, type, player);
                }
            }
        }
    }

    public boolean canDragTo(Slot slot) {
        return slot.isActive() && this.stillValid(this.viewer) && super.canDragTo(slot);
    }

    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && this.stillValid(this.viewer) && super.canTakeItemForPickAll(stack, slot);
    }

    public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer serverplayer) || !this.current(player)) {
            return false;
        }

        if (button == 1) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) { serverPlayer.openMenu(this.dragon); }
            return true;
        } else {
            return button == 2
                ? AccessoryMenuAction.openKnowledge(serverplayer, this.dragon)
                : button == 3 && AccessoryMenuAction.transform(serverplayer, this.dragon);
        }
    }

    public ItemStack quickMoveStack(Player player, int index) {
        if (this.current(player) && index >= 0 && index < this.slots.size()) {
            Slot slot = (Slot)this.slots.get(index);
            if (slot.isActive() && slot.mayPickup(player) && slot.hasItem()) {
                ItemStack itemstack = slot.getItem();
                ItemStack itemstack1 = itemstack.copy();
                if (!this.moveItemStackTo(itemstack, 35, 71, true)) {
                    return ItemStack.EMPTY;
                } else {
                    if (itemstack.isEmpty()) {
                        slot.setByPlayer(ItemStack.EMPTY);
                    } else {
                        slot.setChanged();
                    }

                    slot.onTake(player, itemstack);
                    return itemstack1;
                }
            } else {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    public void removed(Player player) {
        super.removed(player);
        this.gear.stopOpen(player);
        this.accessories.stopOpen(player);
        this.storage.stopOpen(player);
    }
}
