package com.example.dragonmaid.item;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class GuardianTagItem extends DragonAccessoryItem {
    public GuardianTagItem() {
    }

    @Override
    public boolean preventsOwnerDamage(ItemStack stack, DragonBaseEntity dragon) {
        return true;
    }

    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dragonmaid.guardian_tag.protection").withStyle(ChatFormatting.GRAY));
    }
}
