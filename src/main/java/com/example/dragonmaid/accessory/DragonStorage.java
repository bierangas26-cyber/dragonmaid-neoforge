package com.example.dragonmaid.accessory;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.item.ItemStack;

public final class DragonStorage {
    public static final int CAPACITY = 27;

    private DragonStorage() {
    }

    // 1.2: 背包槽位 = 知识等级解锁的行数 * 9 + 饰品提供的额外槽位（上限 27）
    public static int unlockedSlots(DragonBaseEntity dragon) {
        if (dragon != null && dragon.isTame() && dragon.getOwnerUUID() != null) {
            int rows = Math.min(3, DragonAccessories.knowledgeLevel(dragon));
            int basic = Math.max(0, rows) * 9;
            return basic == 27 ? 27 : Math.min(27, basic + DragonAccessories.bonusStorageSlots(dragon));
        } else {
            return 0;
        }
    }

    public static DragonStorage.PickupMode pickupMode(DragonBaseEntity dragon) {
        return DragonForms.state(dragon).dragonmaid$getStorage().pickupMode();
    }

    public static void cyclePickupMode(DragonBaseEntity dragon) {
        DragonStorageInventory dragonstorageinventory = DragonForms.state(dragon).dragonmaid$getStorage();
        if (!dragon.level().isClientSide && unlockedSlots(dragon) > 0) {
            dragonstorageinventory.setPickupMode(dragonstorageinventory.pickupMode().next());
        }
    }

    public static boolean canPickup(DragonBaseEntity dragon, ItemStack incoming) {
        if (!incoming.isEmpty() && pickupMode(dragon) != DragonStorage.PickupMode.OFF) {
            DragonStorageInventory dragonstorageinventory = DragonForms.state(dragon).dragonmaid$getStorage();
            int i = unlockedSlots(dragon);
            boolean flag = dragonstorageinventory.pickupMode() == DragonStorage.PickupMode.ALL;
            boolean flag1 = false;

            for (int j = 0; j < i; j++) {
                ItemStack itemstack = dragonstorageinventory.getItem(j);
                if (itemstack.is(incoming.getItem())) {
                    flag = true;
                }

                if (itemstack.isEmpty()
                    || ItemStack.isSameItemSameComponents(itemstack, incoming)
                        && itemstack.getCount() < Math.min(dragonstorageinventory.getMaxStackSize(), itemstack.getMaxStackSize())) {
                    flag1 = true;
                }

                if (flag && flag1) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static boolean hasPickupRoom(DragonBaseEntity dragon) {
        DragonStorageInventory dragonstorageinventory = DragonForms.state(dragon).dragonmaid$getStorage();
        if (dragonstorageinventory.pickupMode() == DragonStorage.PickupMode.OFF) {
            return false;
        } else {
            int i = unlockedSlots(dragon);
            boolean flag = dragonstorageinventory.pickupMode() == DragonStorage.PickupMode.ALL;
            boolean flag1 = false;

            for (int j = 0; j < i; j++) {
                ItemStack itemstack = dragonstorageinventory.getItem(j);
                if (!itemstack.isEmpty()) {
                    flag = true;
                }

                if (itemstack.isEmpty() || itemstack.getCount() < Math.min(dragonstorageinventory.getMaxStackSize(), itemstack.getMaxStackSize())) {
                    flag1 = true;
                }

                if (flag && flag1) {
                    return true;
                }
            }

            return false;
        }
    }

    public static ItemStack insert(DragonBaseEntity dragon, ItemStack incoming) {
        if (!incoming.isEmpty() && !dragon.level().isClientSide) {
            int i = unlockedSlots(dragon);
            if (i == 0) {
                return incoming;
            } else {
                DragonStorageInventory dragonstorageinventory = DragonForms.state(dragon).dragonmaid$getStorage();
                ItemStack itemstack = incoming.copy();
                int j = Math.min(dragonstorageinventory.getMaxStackSize(), itemstack.getMaxStackSize());

                for (int k = 0; k < i && !itemstack.isEmpty(); k++) {
                    ItemStack itemstack1 = dragonstorageinventory.getItem(k);
                    if (!itemstack1.isEmpty() && ItemStack.isSameItemSameComponents(itemstack1, itemstack)) {
                        int l = Math.min(itemstack.getCount(), j - itemstack1.getCount());
                        if (l > 0) {
                            itemstack1.grow(l);
                            itemstack.shrink(l);
                            dragonstorageinventory.setChanged();
                        }
                    }
                }

                for (int i1 = 0; i1 < i && !itemstack.isEmpty(); i1++) {
                    if (dragonstorageinventory.getItem(i1).isEmpty()) {
                        dragonstorageinventory.setItem(i1, itemstack.split(j));
                    }
                }

                return itemstack.isEmpty() ? ItemStack.EMPTY : itemstack;
            }
        } else {
            return incoming;
        }
    }

    public static enum PickupMode {
        ALL,
        OFF,
        MATCHING;

        public DragonStorage.PickupMode next() {
            return switch (this) {
                case OFF -> MATCHING;
                case MATCHING -> ALL;
                case ALL -> OFF;
            };
        }
    }
}
