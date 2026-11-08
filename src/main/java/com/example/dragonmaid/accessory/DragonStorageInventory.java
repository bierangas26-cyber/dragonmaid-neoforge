package com.example.dragonmaid.accessory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class DragonStorageInventory extends SimpleContainer {
    // 1.2: 拾取模式（全部 / 关闭 / 只拾取袋内已有种类）
    private DragonStorage.PickupMode mode = DragonStorage.PickupMode.OFF;
    private int revision;

    public DragonStorageInventory() {
        super(27);
    }

    public DragonStorage.PickupMode pickupMode() {
        return this.mode;
    }

    public void setPickupMode(DragonStorage.PickupMode mode) {
        this.mode = mode;
    }

    // 内容版本号：DragonBagFeeding 用它做"袋内没有可吃的东西"的缓存键
    public int revision() {
        return this.revision;
    }

    public void setChanged() {
        super.setChanged();
        this.revision++;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack itemstack = super.removeItemNoUpdate(slot);
        if (!itemstack.isEmpty()) {
            this.revision++;
        }

        return itemstack;
    }

    public void loadPickupMode(String saved) {
        for (DragonStorage.PickupMode dragonstorage$pickupmode : DragonStorage.PickupMode.values()) {
            if (dragonstorage$pickupmode.name().equals(saved)) {
                this.mode = dragonstorage$pickupmode;
                return;
            }
        }

        this.mode = DragonStorage.PickupMode.OFF;
    }

    public ListTag save() {
        ListTag listtag = new ListTag();

        for (int i = 0; i < this.getContainerSize(); i++) {
            if (!this.getItem(i).isEmpty()) {
                CompoundTag compoundtag = com.example.dragonmaid.compat.StackCodec.save(this.getItem(i), new CompoundTag());
                compoundtag.putInt("Slot", i);
                listtag.add(compoundtag);
            }
        }

        return listtag;
    }

    public void load(ListTag entries) {
        this.clearContent();

        for (int i = 0; i < entries.size(); i++) {
            CompoundTag compoundtag = entries.getCompound(i);
            int j = compoundtag.getInt("Slot");
            if (compoundtag.contains("Slot", 99) && j >= 0 && j < this.getContainerSize()) {
                this.setItem(j, com.example.dragonmaid.compat.StackCodec.load(compoundtag));
            }
        }
    }
}
