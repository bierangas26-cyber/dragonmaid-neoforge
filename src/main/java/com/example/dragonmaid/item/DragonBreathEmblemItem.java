package com.example.dragonmaid.item;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class DragonBreathEmblemItem extends DragonAccessoryItem {
    public DragonBreathEmblemItem() {
    }

    @Override
    public boolean enhancesBreath(ItemStack stack, DragonBaseEntity dragon) {
        return true;
    }

    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        for (String s : List.of("fire_ice", "lightning")) {
            tooltip.add(Component.translatable("tooltip.dragonmaid.dragon_breath_emblem." + s).withStyle(ChatFormatting.GRAY));
        }
    }
}
