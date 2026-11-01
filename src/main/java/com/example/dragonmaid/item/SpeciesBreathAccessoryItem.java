package com.example.dragonmaid.item;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class SpeciesBreathAccessoryItem extends DragonAccessoryItem {
    private final SpeciesBreathAccessoryItem.Species species;
    private final String id;

    public SpeciesBreathAccessoryItem(SpeciesBreathAccessoryItem.Species species, String id) {
        this.species = species;
        this.id = id;
    }

    @Override
    public boolean canEquip(ItemStack stack, DragonBaseEntity dragon) {
        return switch (this.species) {
            case FIRE -> dragon instanceof FireDragonEntity;
            case ICE -> dragon instanceof IceDragonEntity;
            case LIGHTNING -> dragon instanceof LightningDragonEntity;
        };
    }

    @Override
    public double breathDamageBonus(ItemStack stack, DragonBaseEntity dragon) {
        return this.canEquip(stack, dragon) ? (this.species == SpeciesBreathAccessoryItem.Species.LIGHTNING ? 0.1 : 0.15) : 0.0;
    }

    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dragonmaid." + this.id + ".damage").withStyle(ChatFormatting.GRAY));
    }

    public static enum Species {
        FIRE,
        ICE,
        LIGHTNING;

        private Species() {
        }
    }
}
