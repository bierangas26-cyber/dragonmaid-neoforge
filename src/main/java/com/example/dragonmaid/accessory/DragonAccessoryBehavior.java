package com.example.dragonmaid.accessory;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface DragonAccessoryBehavior {
    ResourceLocation OWNER_PROTECTION = ResourceLocation.fromNamespaceAndPath("dragonmaid", "owner_protection");
    ResourceLocation ARMOR = ResourceLocation.fromNamespaceAndPath("dragonmaid", "armor");
    ResourceLocation REGENERATION = ResourceLocation.fromNamespaceAndPath("dragonmaid", "regeneration");
    ResourceLocation BREATH_TIMING = ResourceLocation.fromNamespaceAndPath("dragonmaid", "breath_timing");
    ResourceLocation BREATH_DAMAGE = ResourceLocation.fromNamespaceAndPath("dragonmaid", "breath_damage");
    ResourceLocation STORAGE = ResourceLocation.fromNamespaceAndPath("dragonmaid", "storage");

    default boolean canEquip(ItemStack stack, DragonBaseEntity dragon) {
        return true;
    }

    default boolean preventsOwnerDamage(ItemStack stack, DragonBaseEntity dragon) {
        return false;
    }

    default double armorBonus(ItemStack stack, DragonBaseEntity dragon) {
        return 0.0;
    }

    default boolean regeneratesHealth(ItemStack stack, DragonBaseEntity dragon) {
        return false;
    }

    default boolean enhancesBreath(ItemStack stack, DragonBaseEntity dragon) {
        return false;
    }

    default double breathDamageBonus(ItemStack stack, DragonBaseEntity dragon) {
        return 0.0;
    }

    default int storageSlots(ItemStack stack, DragonBaseEntity dragon) {
        return 0;
    }

    default Set<ResourceLocation> exclusiveEffects(ItemStack stack, DragonBaseEntity dragon) {
        Set<ResourceLocation> set = new HashSet<>();
        if (this.preventsOwnerDamage(stack, dragon)) {
            set.add(OWNER_PROTECTION);
        }

        if (this.regeneratesHealth(stack, dragon)) {
            set.add(REGENERATION);
        }

        if (this.enhancesBreath(stack, dragon)) {
            set.add(BREATH_TIMING);
        }

        double d0 = this.breathDamageBonus(stack, dragon);
        if (Double.isFinite(d0) && d0 > 0.0) {
            set.add(BREATH_DAMAGE);
        }

        if (this.storageSlots(stack, dragon) > 0) {
            set.add(STORAGE);
        }

        return set;
    }
}
