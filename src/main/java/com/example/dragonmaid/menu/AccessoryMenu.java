package com.example.dragonmaid.menu;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryInventory;
import com.example.dragonmaid.accessory.DragonHands;
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
    public static final int HAND_START = 71;
    public static final int HAND_END = 73;
    public static final int CYCLE_PICKUP = 4;
    public static final int SWITCH_INVENTORY = 5;
    private static final int[] EQUIP_ORDER = new int[]{71, 72, 0, 1, 2, 3, 4, 5, 6, 7};
    private final DragonBaseEntity dragon;
    private final Player viewer;
    private final Container gear;
    private final SimpleContainer accessories;
    private final SimpleContainer storage;
    private final SimpleContainerData data = new SimpleContainerData(4);

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
        boolean server = dragon != null && !inventory.player.level().isClientSide;
        if (server) {
            // 1.2: 打开界面时把旧版误存进主手的非近战物品挪回背包
            DragonHands.stowOldWeapon(dragon);
        }

        this.gear = server ? dragon.dragonInventory : new SimpleContainer(5);
        this.accessories = server ? DragonForms.state(dragon).dragonmaid$getAccessories() : new DragonAccessoryInventory();
        this.storage = server ? DragonForms.state(dragon).dragonmaid$getStorage() : new DragonStorageInventory();
        // 1.2: 装备槽移到右侧一列，背包从 82/74 起排，玩家背包可见（不再用 0,0 隐藏槽）
        int[] gearX = new int[]{154, 82, 100, 118, 136};

        for (int i = 0; i < 5; i++) {
            this.addSlot(DragonEquipmentSlots.gear(this.gear, i, gearX[i], 18, () -> this.stillValid(this.viewer)));
        }

        for (int i = 0; i < 3; i++) {
            this.addSlot(DragonEquipmentSlots.accessory(this.accessories, i, 82 + i * 18, 42, dragon, this::unlockedSlots, () -> this.stillValid(this.viewer)));
        }

        for (int i = 0; i < 27; i++) {
            final int storageIndex = i;
            this.addSlot(new Slot(this.storage, i, 82 + i % 9 * 18, 74 + i / 9 * 18) {
                public boolean isActive() {
                    return storageIndex < AccessoryMenu.this.storageSlots();
                }

                public boolean mayPlace(ItemStack stack) {
                    return storageIndex < AccessoryMenu.this.storageSlots() && AccessoryMenu.this.stillValid(AccessoryMenu.this.viewer);
                }

                public boolean mayPickup(Player player) {
                    return this.isActive() && AccessoryMenu.this.stillValid(player);
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(this.playerSlot(inventory, col + row * 9 + 9, 82 + col * 18, 150 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(this.playerSlot(inventory, col, 82 + col * 18, 208));
        }

        // 1.2: 双手槽（主手近战武器 / 副手盾），知识等级 >= 2 解锁
        Container hands = server ? DragonHands.inventory(dragon) : new SimpleContainer(2);

        for (int i = 0; i < 2; i++) {
            final int hand = i;
            this.addSlot(new Slot(hands, i, 208 + i * 18, 18) {
                public boolean isActive() {
                    return AccessoryMenu.this.knowledgeLevel() >= 2;
                }

                public boolean mayPlace(ItemStack stack) {
                    return this.isActive() && AccessoryMenu.this.stillValid(AccessoryMenu.this.viewer) && DragonHands.accepts(hand, stack);
                }

                public boolean mayPickup(Player player) {
                    return this.isActive() && AccessoryMenu.this.stillValid(player);
                }

                public int getMaxStackSize() {
                    return 1;
                }

                public int getMaxStackSize(ItemStack stack) {
                    return 1;
                }
            });
        }

        this.addDataSlots(this.data);
        this.updateData();
        this.gear.startOpen(this.viewer);
        this.accessories.startOpen(this.viewer);
        this.storage.startOpen(this.viewer);
    }

    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y);
    }

    private void updateData() {
        if (this.dragon != null && !this.dragon.level().isClientSide) {
            this.data.set(0, DragonAccessories.knowledgeLevel(this.dragon));
            this.data.set(1, DragonAccessories.unlockedSlots(this.dragon));
            this.data.set(2, DragonStorage.unlockedSlots(this.dragon));
            this.data.set(3, DragonStorage.pickupMode(this.dragon).ordinal());
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

    /** 1.2: 玩家背包恒显示；保留给 screen 判断"是否处于背包视图"（当前版本恒 true）。 */
    public boolean showingPlayer() {
        return true;
    }

    public int pickupMode() {
        return this.data.get(3);
    }

    public boolean stillValid(Player player) {
        return DragonAccessories.canManage(this.dragon, player) && (player.level().isClientSide || !this.dragon.hasInventoryChanged(this.gear));
    }

    private boolean current(Player player) {
        return player.containerMenu == this && this.stillValid(player);
    }

    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot < this.slots.size() && (slot < 0 || ((Slot)this.slots.get(slot)).isActive())) {
            if (this.current(player)) {
                super.clicked(slot, button, type, player);
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
        } else if (button == 2) {
            return AccessoryMenuAction.openKnowledge(serverplayer, this.dragon);
        } else if (button == SWITCH_INVENTORY) {
            // 1.2: 背包视图切换只影响客户端渲染，服务端无需任何状态变更
            return false;
        } else if (button == CYCLE_PICKUP && this.storageSlots() > 0) {
            DragonStorage.cyclePickupMode(this.dragon);
            this.updateData();
            return true;
        } else {
            return button == 3 && AccessoryMenuAction.transform(serverplayer, this.dragon);
        }
    }

    public ItemStack quickMoveStack(Player player, int index) {
        if (this.current(player) && index >= 0 && index < this.slots.size()) {
            Slot slot = (Slot)this.slots.get(index);
            if (slot.isActive() && slot.mayPickup(player) && slot.hasItem()) {
                ItemStack itemstack = slot.getItem();
                ItemStack itemstack1 = itemstack.copy();
                if (index >= 35 && index < 71) {
                    // 1.2: 玩家背包 Shift-点击优先填双手/装备槽，再进龙背包
                    boolean flag = false;

                    for (int i : EQUIP_ORDER) {
                        Slot destination = this.getSlot(i);
                        if (destination.isActive() && !destination.hasItem() && destination.mayPlace(itemstack)) {
                            destination.setByPlayer(itemstack.split(Math.min(itemstack.getCount(), destination.getMaxStackSize(itemstack))));
                            flag = true;
                            break;
                        }
                    }

                    if (!flag && !this.moveItemStackTo(itemstack, 8, 8 + this.storageSlots(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack, 35, 71, true)) {
                    return ItemStack.EMPTY;
                }

                if (itemstack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }

                slot.onTake(player, itemstack);
                return itemstack1;
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
