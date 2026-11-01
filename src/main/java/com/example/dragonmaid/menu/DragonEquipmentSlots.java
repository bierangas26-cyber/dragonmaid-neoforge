package com.example.dragonmaid.menu;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;

public final class DragonEquipmentSlots {
    private DragonEquipmentSlots() {
    }

    public static Slot gear(Container inventory, final int gearIndex, int x, int y, final BooleanSupplier valid) {
        return new Slot(inventory, gearIndex, x, y) {
            public boolean mayPlace(ItemStack stack) {
                return valid.getAsBoolean()
                    && !stack.isEmpty()
                    && (
                        gearIndex == 0
                            ? stack.getItem() instanceof BannerItem
                            : stack.getItem() instanceof DragonArmorItem itemdragonarmor && itemdragonarmor.dragonSlot.ordinal() == gearIndex - 1
                    );
            }

            public boolean mayPickup(Player player) {
                return valid.getAsBoolean();
            }
        };
    }

    public static Slot accessory(
        final Container inventory,
        final int accessoryIndex,
        int x,
        int y,
        final DragonBaseEntity dragon,
        final IntSupplier unlocked,
        final BooleanSupplier valid
    ) {
        return new Slot(inventory, accessoryIndex, x, y) {
            public boolean isActive() {
                return accessoryIndex < unlocked.getAsInt();
            }

            public boolean mayPlace(ItemStack stack) {
                return this.isActive() && valid.getAsBoolean() && DragonAccessories.accepts(stack, dragon, inventory, unlocked.getAsInt(), accessoryIndex);
            }

            public boolean mayPickup(Player player) {
                return this.isActive() && valid.getAsBoolean();
            }

            public int getMaxStackSize() {
                return 1;
            }

            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        };
    }
}
