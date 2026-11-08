package com.example.dragonmaid.accessory;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class DragonAccessoryInventory extends SimpleContainer {
    public static final int CAPACITY = 5;
    private final DragonBaseEntity owner;
    private boolean loading;
    final DragonAccessoryInventory.Effects effects = new DragonAccessoryInventory.Effects();

    public DragonAccessoryInventory() {
        this(null);
    }

    public DragonAccessoryInventory(DragonBaseEntity owner) {
        super(5);
        this.owner = owner;
    }

    public void setChanged() {
        super.setChanged();
        this.invalidateEffects();
    }

    public void invalidateEffects() {
        this.effects.dirty = true;
        if (this.owner != null && !this.loading) {
            DragonAccessories.refresh(this.owner);
        }
    }

    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack itemstack = super.removeItemNoUpdate(slot);
        if (!itemstack.isEmpty()) {
            this.invalidateEffects();
        }

        return itemstack;
    }

    public int getMaxStackSize() {
        return 1;
    }

    public void setItem(int slot, ItemStack stack) {
        if (stack.getCount() > 1) {
            stack = stack.copy();
            stack.setCount(1);
        }

        super.setItem(slot, stack);
    }

    public ListTag save() {
        ListTag listtag = new ListTag();

        for (int i = 0; i < 5; i++) {
            ItemStack itemstack = this.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundtag = com.example.dragonmaid.compat.StackCodec.save(itemstack, new CompoundTag());
                compoundtag.putByte("Slot", (byte)i);
                listtag.add(compoundtag);
            }
        }

        return listtag;
    }

    public void load(ListTag entries) {
        this.loading = true;

        try {
            this.clearContent();

            for (int i = 0; i < entries.size(); i++) {
                CompoundTag compoundtag = entries.getCompound(i);
                int j = compoundtag.getInt("Slot");
                if (j >= 0 && j < 5 && compoundtag.contains("Slot", 99)) {
                    this.setItem(j, com.example.dragonmaid.compat.StackCodec.load(compoundtag));
                }
            }
        } finally {
            this.loading = false;
            this.effects.healingTicks = 0;
            this.invalidateEffects();
        }
    }

    static final class Effects {
        boolean dirty = true;
        int stage = -1;
        int slots = -1;
        int adapters = -1;
        boolean eligible;
        double armor;
        boolean regenerates;
        boolean enhancesBreath;
        double breathDamageBonus;
        int storageSlots;
        int healingTicks;
        // 1.2: 饰品的激活效果集合（供 DragonAccessories.hasEffect 查询）
        Set<ResourceLocation> activeEffects = Set.of();

        Effects() {
        }
    }
}
