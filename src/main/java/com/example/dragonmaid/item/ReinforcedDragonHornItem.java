package com.example.dragonmaid.item;

import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.entity.ReinforcedHornItemEntity;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonHornItem;
import com.iafenvoy.iceandfire.item.component.DragonHornComponent;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;

/**
 * Reinforced dragon horn: captures/releases a tamed dragon.
 *
 * 1.21 port note: IAF CE 2.0 moved the stored dragon from the legacy {@code EntityTag} compound
 * into a {@link DragonHornComponent} data component, and the vanilla {@code DragonHornItem}
 * gates every behaviour on {@code stack.is(IafItems.DRAGON_HORN)} — which never matches this
 * custom horn item. The capture/release logic therefore has to live here and talk to the
 * component API directly (the 1.20.1 {@code StackNbt EntityTag} checks are dead code now).
 */
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

    /** Capture: store the owned dragon inside the horn component, then remove it from the world. */
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.isSpectator()
            || hand != InteractionHand.MAIN_HAND
            || !(target instanceof DragonBaseEntity dragon)
            || !dragon.isOwnedBy(player)) {
            return InteractionResult.FAIL;
        }

        if (HumanoidLife.isInjured(dragon)) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.dragonmaid.injury_no_horn"), true);
            }

            return InteractionResult.FAIL;
        }

        if (!dragon.isAlive() || dragon.isModelDead() || dragon.isPassenger() || dragon.isVehicle()) {
            return InteractionResult.FAIL;
        }

        // Already holding a dragon (1.21 component instead of the old EntityTag compound).
        if (stack.has(IafDataComponents.DRAGON_HORN.get())) {
            return InteractionResult.FAIL;
        }

        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        CompoundTag entityData = new CompoundTag();
        dragon.save(entityData);
        stack.set(
            IafDataComponents.DRAGON_HORN.get(),
            new DragonHornComponent(BuiltInRegistries.ENTITY_TYPE.getKey(dragon.getType()), dragon.getUUID(), entityData)
        );
        player.swing(hand);
        dragon.remove(Entity.RemovalReason.DISCARDED);
        return InteractionResult.SUCCESS;
    }

    /** Release: rebuild the stored dragon from the horn component on top of the clicked block. */
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSpectator()) {
            return InteractionResult.FAIL;
        }

        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        if (!stack.has(IafDataComponents.DRAGON_HORN.get())) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        DragonHornComponent component = stack.get(IafDataComponents.DRAGON_HORN.get());
        if (component == null) {
            return InteractionResult.PASS;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(component.entityType()).orElse(null);
        if (type == null) {
            return InteractionResult.FAIL;
        }

        Entity entity = type.create(level);
        if (!(entity instanceof DragonBaseEntity dragon)) {
            return InteractionResult.FAIL;
        }

        dragon.load(component.entityData());
        if (component.entityUuid() != null) {
            entity.setUUID(component.entityUuid());
        }

        BlockPos pos = context.getClickedPos();
        entity.absMoveTo(
            pos.getX() + 0.5D,
            pos.getY() + 1.0D,
            pos.getZ() + 0.5D,
            180.0F + context.getHorizontalDirection().toYRot(),
            0.0F
        );

        if (level.addFreshEntity(entity)) {
            stack.remove(IafDataComponents.DRAGON_HORN.get());
        }

        return InteractionResult.SUCCESS;
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.dragonmaid.reinforced_dragon_horn").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dragonmaid.reinforced_dragon_horn.retrieval").withStyle(ChatFormatting.GRAY));
    }
}
