package com.example.dragonmaid.item;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.SummoningCrystalItem;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * 1.2: 三龙宝石支持 continuous（持续绑定）/ once（一次召唤）两种模式。
 * 1.21.1: IAF CE 2.2 把宝石绑定放在 IafDataComponents.CRYSTAL_DRAGON_DATA（CompoundTag component），
 * 模式标记也存进同一个 compound（键名不能含 "Dragon"，否则会被 IAF 的 hasDragon 扫描误判）。
 */
public final class TrinityDragonGemItem extends SummoningCrystalItem {
    private static final String CONTINUOUS = "ContinuousBinding";

    private static CompoundTag crystal(ItemStack stack) {
        CompoundTag tag = stack.get(IafDataComponents.CRYSTAL_DRAGON_DATA.get());
        return tag == null ? new CompoundTag() : tag.copy();
    }

    private static void setCrystal(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) {
            stack.remove(IafDataComponents.CRYSTAL_DRAGON_DATA.get());
        } else {
            stack.set(IafDataComponents.CRYSTAL_DRAGON_DATA.get(), tag);
        }
    }

    public static boolean continuous(ItemStack stack) {
        return crystal(stack).getBoolean(CONTINUOUS);
    }

    public static void toggleMode(ItemStack stack) {
        CompoundTag tag = crystal(stack);
        tag.putBoolean(CONTINUOUS, !tag.getBoolean(CONTINUOUS));
        setCrystal(stack, tag);
    }

    public static void clearDeadBinding(ItemStack stack, ServerLevel level) {
        CompoundTag tag = crystal(stack);
        if (tag.contains("Dragon", 10)) {
            CompoundTag binding = tag.getCompound("Dragon");
            if (binding.hasUUID("DragonUUID") && GemBindingDeaths.get(level).contains(binding.getUUID("DragonUUID"))) {
                tag.remove("Dragon");
                setCrystal(stack, tag);
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null) {
            ItemStack stack = context.getItemInHand();
            clearDeadBinding(stack, level);
            CompoundTag saved = crystal(stack);
            boolean keep = saved.getBoolean(CONTINUOUS);
            InteractionResult result = super.useOn(context);
            if (!saved.isEmpty() && !hasDragon(stack)) {
                if (keep) {
                    CompoundTag binding = saved.getCompound("Dragon");
                    if (binding.hasUUID("DragonUUID")
                        && level.getEntity(binding.getUUID("DragonUUID")) instanceof DragonBaseEntity dragon
                        && !DragonForms.state(dragon).dragonmaid$isExecuted()
                        && dragon.isAlive()) {
                        setCrystal(stack, saved);
                        dragon.setCrystalBound(true);
                    } else {
                        CompoundTag tag = crystal(stack);
                        tag.putBoolean(CONTINUOUS, true);
                        setCrystal(stack, tag);
                    }
                } else {
                    CompoundTag tag = crystal(stack);
                    tag.putBoolean(CONTINUOUS, false);
                    setCrystal(stack, tag);
                }
            }

            return result;
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(
            Component.translatable("tooltip.dragonmaid.trinity_dragon_gem.mode." + (continuous(stack) ? "continuous" : "once"))
                .withStyle(ChatFormatting.GOLD)
        );
        tooltip.add(Component.translatable("tooltip.dragonmaid.trinity_dragon_gem.switch").withStyle(ChatFormatting.DARK_GRAY));
        boolean bound = hasDragon(stack);
        tooltip.add(Component.translatable("tooltip.dragonmaid.trinity_dragon_gem." + (bound ? "bound" : "unbound")).withStyle(ChatFormatting.GRAY));
        if (bound) {
            String name = crystal(stack).getCompound("Dragon").getString("CustomName");
            if (!name.isBlank()) {
                tooltip.add(Component.literal(name).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
