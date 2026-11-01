package com.example.dragonmaid.item;

import com.iafenvoy.iceandfire.data.DragonArmorMaterial;
import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import net.minecraft.world.item.ItemStack;

public final class ServiceDragonArmorItem extends DragonArmorItem {
    public ServiceDragonArmorItem(int dragonSlot) {
        super(DragonArmorMaterial.SILVER, requireSlot(dragonSlot));
    }

    private static DragonArmorPart requireSlot(int slot) {
        DragonArmorPart[] parts = DragonArmorPart.values();
        if (slot >= 0 && slot < parts.length) {
            return parts[slot];
        } else {
            throw new IllegalArgumentException("Dragon armor slot must be between 0 and 3: " + slot);
        }
    }

    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    public static boolean isService(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ServiceDragonArmorItem;
    }
}
