package com.example.dragonmaid.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public final class KaishakuBladeItem extends Item {
    public KaishakuBladeItem() {
        super(new Properties().stacksTo(1));
    }

    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dragonmaid.kaishaku_blade").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dragonmaid.kaishaku_blade_final").withStyle(ChatFormatting.GRAY));
    }
}
