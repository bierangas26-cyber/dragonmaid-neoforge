package com.example.dragonmaid.item;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class SustainingAccessoryItem extends DragonAccessoryItem {
    private final boolean armor;
    private final boolean regeneration;
    private final boolean ownerProtection;

    public SustainingAccessoryItem(boolean armor, boolean regeneration) {
        this(armor, regeneration, false);
    }

    public SustainingAccessoryItem(boolean armor, boolean regeneration, boolean ownerProtection) {
        this.armor = armor;
        this.regeneration = regeneration;
        this.ownerProtection = ownerProtection;
    }

    @Override
    public double armorBonus(ItemStack stack, DragonBaseEntity dragon) {
        return this.armor ? (double)(Math.max(1, Math.min(5, dragon.getDragonStage())) + 1) : 0.0;
    }

    @Override
    public boolean regeneratesHealth(ItemStack stack, DragonBaseEntity dragon) {
        return this.regeneration;
    }

    @Override
    public boolean preventsOwnerDamage(ItemStack stack, DragonBaseEntity dragon) {
        return this.ownerProtection;
    }

    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (this.armor) {
            tooltip.add(line("armor"));
        }

        if (this.regeneration) {
            tooltip.add(line("regeneration"));
        }

        if (this.ownerProtection) {
            tooltip.add(Component.translatable("tooltip.dragonmaid.guardian_tag.protection").withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component line(String key) {
        return Component.translatable("tooltip.dragonmaid.accessory." + key).withStyle(ChatFormatting.GRAY);
    }
}
