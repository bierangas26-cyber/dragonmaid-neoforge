package com.example.dragonmaid.item;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.combat.BladeCombat;
import com.example.dragonmaid.combat.BladeCompat;
import com.example.dragonmaid.combat.LongswordParry;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 1.2.7 的刀（katana）/ 长剑（longsword）基类。
 *
 * <p>1.21.1 映射：攻击力/攻速不再走构造参数，改为 {@code Item.Properties#attributes} +
 * {@code SwordItem.createAttributes(tier, damage, speed)}；{@code MobType} 已删除，亡灵判定改
 * {@link EntityTypeTags#UNDEAD}；银锭修复 tag 由 {@code forge:ingots/silver} 改 {@code c:ingots/silver}。</p>
 */
public class CompanionBladeItem extends SwordItem {
    private static final TagKey<Item> SILVER_INGOTS =
        ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/silver"));
    private final boolean longsword;
    private final boolean silver;
    private final float guardReduction;

    public CompanionBladeItem(Tier tier, boolean longsword, float guardReduction, boolean silver) {
        super(tier, properties(tier, longsword));
        this.longsword = longsword;
        this.silver = silver;
        this.guardReduction = guardReduction;
    }

    private static Item.Properties properties(Tier tier, boolean longsword) {
        Item.Properties properties = new Item.Properties().attributes(
            SwordItem.createAttributes(tier, longsword ? 3 : 1, longsword ? -2.8F : -1.6F)
        );
        return tier == Tiers.NETHERITE ? properties.fireResistant() : properties;
    }

    public float guardReduction() {
        return this.guardReduction;
    }

    public boolean isLongsword() {
        return this.longsword;
    }

    public static float undeadBonus(ItemStack stack, LivingEntity target) {
        if (stack.getItem() instanceof CompanionBladeItem blade
            && blade.silver
            && target.getType().is(EntityTypeTags.UNDEAD)) {
            return 2.0F;
        }
        return 0.0F;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        if (this.silver) {
            tooltip.add(Component.translatable("silvertools.hurt").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack material) {
        return this.silver ? material.is(SILVER_INGOTS) : super.isValidRepairItem(stack, material);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return this.longsword ? UseAnim.BLOCK : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        ItemStack offhand = player.getOffhandItem();
        if (!this.longsword
            || (!LongswordParry.recovering(player)
                && (!DragonHands.accepts(1, offhand) || player.getCooldowns().isOnCooldown(offhand.getItem())))) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!this.longsword && !level.isClientSide && user instanceof Player player) {
            BladeCombat.chargeParticles(player, (long) (this.getUseDuration(stack, player) - remaining));
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (!this.longsword
            && user instanceof Player player
            && player.isAlive()
            && !player.isSpectator()
            && player.getMainHandItem() == stack) {
            int ticks = this.getUseDuration(stack, player) - remaining;
            if (level.isClientSide) {
                player.getCooldowns().addCooldown(this, 18);
                BladeCompat.clientReleaseBlade(player, stack, ticks);
            } else {
                BladeCombat.playerDraw(player, stack, ticks);
            }
        }
    }
}
