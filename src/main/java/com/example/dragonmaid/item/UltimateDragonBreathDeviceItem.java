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

public final class UltimateDragonBreathDeviceItem extends DragonAccessoryItem {
    public UltimateDragonBreathDeviceItem() {
    }

    @Override
    public boolean enhancesBreath(ItemStack stack, DragonBaseEntity dragon) {
        return true;
    }

    @Override
    public double breathDamageBonus(ItemStack stack, DragonBaseEntity dragon) {
        if (dragon instanceof LightningDragonEntity) {
            return 0.1;
        } else {
            return !(dragon instanceof FireDragonEntity) && !(dragon instanceof IceDragonEntity) ? 0.0 : 0.15;
        }
    }

    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        for (String s : List.of("fire_ice", "lightning")) {
            tooltip.add(Component.translatable("tooltip.dragonmaid.ultimate_dragon_breath_device." + s).withStyle(ChatFormatting.GRAY));
        }
    }
}
