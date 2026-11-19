package com.example.dragonmaid.item;

import com.iafenvoy.iceandfire.data.DragonArmorMaterial;
import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * 1.2.7: four decorative dragon armor sets on top of the original silver "service" armor.
 * Upstream enum order is SERVICE, KIMONO, XIA, VIKING, FROSTSILVER.
 *
 * <p>IAF-CE port note: upstream constructor was {@code (DragonArmorType, int slot)}; CE splits that
 * into {@link DragonArmorMaterial} + {@link DragonArmorPart}, and this repo already maps the old
 * 0..3 slot index onto {@code DragonArmorPart.values()}.
 */
public final class ServiceDragonArmorItem extends DragonArmorItem {
    private final Style style;

    public ServiceDragonArmorItem(int dragonSlot) {
        this(Style.SERVICE, dragonSlot);
    }

    public ServiceDragonArmorItem(Style style, int dragonSlot) {
        super(DragonArmorMaterial.SILVER, requireSlot(dragonSlot));
        this.style = style == null ? Style.SERVICE : style;
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

    public Style style() {
        return this.style;
    }

    public static boolean isService(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ServiceDragonArmorItem;
    }

    public static Style styleOf(ItemStack stack) {
        return stack.getItem() instanceof ServiceDragonArmorItem armor ? armor.style : null;
    }

    /** Specialty armor only fits the species it was made for (viking = fire, frostsilver = ice). */
    public static Style styleOf(ItemStack stack, DragonBaseEntity dragon) {
        Style style = styleOf(stack);
        if (style == null) {
            return null;
        }

        return switch (style) {
            case VIKING -> dragon instanceof FireDragonEntity ? style : null;
            case FROSTSILVER -> dragon instanceof IceDragonEntity ? style : null;
            default -> style;
        };
    }

    public static boolean isSpecialtyArmor(ItemStack stack) {
        return stack.getItem() instanceof ServiceDragonArmorItem armor && armor.style != Style.SERVICE;
    }

    public enum Style {
        SERVICE,
        KIMONO,
        XIA,
        VIKING,
        FROSTSILVER
    }
}
