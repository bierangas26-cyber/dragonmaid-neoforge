package com.example.dragonmaid.item;

import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.entity.ReinforcedHornItemEntity;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonHornItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.example.dragonmaid.compat.StackNbt;
import net.minecraft.world.item.Item;

public final class ReinforcedDragonHornItem extends DragonHornItem {
    public ReinforcedDragonHornItem() {
    }

    public void onCraftedBy(ItemStack stack, Level level, Player player) {
    }

    public boolean isFireResistant() {
        return true;
    }

    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    public Entity createEntity(Level level, Entity original, ItemStack stack) {
        return original instanceof ItemEntity itementity ? ReinforcedHornItemEntity.fromDrop(level, itementity, stack) : null;
    }

    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.isSpectator()
            || hand != InteractionHand.MAIN_HAND
            || !(target instanceof DragonBaseEntity entitydragonbase)
            || !entitydragonbase.isOwnedBy(player)) {
            return InteractionResult.FAIL;
        }

        if (HumanoidLife.isInjured(entitydragonbase)) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.dragonmaid.injury_no_horn"), true);
            }

            return InteractionResult.FAIL;
        } else if (!entitydragonbase.isAlive() || entitydragonbase.isModelDead() || !StackNbt.getTag(player.getItemInHand(hand)).getCompound("EntityTag").isEmpty()) {
            return InteractionResult.FAIL;
        } else {
            return player.level().isClientSide ? InteractionResult.SUCCESS : super.interactLivingEntity(stack, player, target, hand);
        }
    }

    public InteractionResult useOn(UseOnContext context) {
        if ((context.getPlayer() == null || !context.getPlayer().isSpectator()) && context.getClickedFace() == Direction.UP) {
            return context.getLevel().isClientSide ? InteractionResult.SUCCESS : super.useOn(context);
        } else {
            return InteractionResult.FAIL;
        }
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.dragonmaid.reinforced_dragon_horn").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dragonmaid.reinforced_dragon_horn.retrieval").withStyle(ChatFormatting.GRAY));
    }
}
