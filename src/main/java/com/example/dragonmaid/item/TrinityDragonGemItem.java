package com.example.dragonmaid.item;

import com.iafenvoy.iceandfire.item.SummoningCrystalItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.example.dragonmaid.compat.StackNbt;

public final class TrinityDragonGemItem extends SummoningCrystalItem {
    public TrinityDragonGemItem() {
    }

    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean flagx = hasDragon(stack);
        tooltip.add(Component.translatable("tooltip.dragonmaid.trinity_dragon_gem." + (flagx ? "bound" : "unbound")).withStyle(ChatFormatting.GRAY));
        if (flagx && StackNbt.getTag(stack) != null) {
            String s = StackNbt.getTag(stack).getCompound("Dragon").getString("CustomName");
            if (!s.isBlank()) {
                tooltip.add(Component.literal(s).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
