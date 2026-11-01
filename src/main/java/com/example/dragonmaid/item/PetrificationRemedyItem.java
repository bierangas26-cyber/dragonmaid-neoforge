package com.example.dragonmaid.item;

import com.example.dragonmaid.petrification.DragonPetrification;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public final class PetrificationRemedyItem extends Item {
    public PetrificationRemedyItem() {
        super(new Properties().stacksTo(16));
    }

    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return target instanceof StoneStatueEntity entitystonestatue ? DragonPetrification.restore(player, hand, entitystonestatue) : InteractionResult.PASS;
    }

    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dragonmaid.petrification_remedy").withStyle(ChatFormatting.GRAY));
    }
}
