package com.example.dragonmaid.accessory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class DragonStorageInventory extends SimpleContainer {
    public DragonStorageInventory() {
        super(27);
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
