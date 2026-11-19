package com.example.dragonmaid.item;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 龙血刀/长剑：行为完全委托给原刀（{@code originalSword}），只在配方产出时替换本体。 */
public final class DragonBloodBladeItem extends CompanionBladeItem {
    private final Supplier<Item> originalSword;

    DragonBloodBladeItem(Tier tier, boolean longsword, Supplier<Item> originalSword) {
        super(tier, longsword, longsword ? 0.6F : 0.0F, false);
        this.originalSword = originalSword;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        return this.originalSword.get().hurtEnemy(stack, victim, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, @Nullable List<Component> tooltip,
            TooltipFlag flags) {
        this.originalSword.get().appendHoverText(stack, context, tooltip, flags);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return this.originalSword.get().isFoil(stack);
    }
}
