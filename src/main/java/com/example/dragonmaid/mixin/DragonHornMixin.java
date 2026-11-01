package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.recall.RecallHorn;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonHornItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.example.dragonmaid.compat.StackNbt;

@Mixin(remap = false, value = {DragonHornItem.class})
public abstract class DragonHornMixin extends Item {
    protected DragonHornMixin(Properties properties) {
        super(properties);
    }

    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        RecallHorn.inventoryTick(stack, entity);
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Inject(remap = false, method = {"useOn"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$recallRelease(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult interactionresult = RecallHorn.release(context);
        if (interactionresult != null) {
            cir.setReturnValue(interactionresult);
        }
    }

    @Inject(remap = false, method = {"interactLivingEntity"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$noInjuredCapture(
        ItemStack stack, Player player, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (target instanceof DragonBaseEntity entitydragonbase
            && entitydragonbase.isOwnedBy(player)
            && !player.isSpectator()
            && hand == InteractionHand.MAIN_HAND
            && (entitydragonbase.isPassenger() || entitydragonbase.isVehicle())) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.dragonmaid.horn_dismount"), true);
            }

            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        InteractionResult interactionresult = RecallHorn.capture(stack, player, target, hand);
        if (interactionresult != null) {
            cir.setReturnValue(interactionresult);
        } else {
            if (target instanceof DragonBaseEntity entitydragonbase1 && HumanoidLife.isInjured(entitydragonbase1)) {
                if (!player.level().isClientSide) {
                    player.displayClientMessage(Component.translatable("message.dragonmaid.injury_no_horn"), true);
                }

                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
        }
    }

    @Unique
    private static InteractionResult dragonmaid$captureNativeSafely(ItemStack callbackStack, Player player, DragonBaseEntity dragon, InteractionHand hand) {
        if (!player.isSpectator()
            && hand == InteractionHand.MAIN_HAND
            && dragon.isOwnedBy(player)
            && dragon.level() == player.level()
            && dragon.isAlive()
            && !dragon.isModelDead()
            && !dragon.isPassenger()
            && !dragon.isVehicle()) {
            ItemStack itemstack = player.getItemInHand(hand);
            if (itemstack.getItem() instanceof DragonHornItem && itemstack.getCount() == 1 && ItemStack.isSameItemSameComponents(callbackStack, itemstack)) {
                CompoundTag compoundtag = StackNbt.getTag(itemstack) == null ? null : StackNbt.getTag(itemstack).copy();
                if (compoundtag == null || compoundtag.getCompound("EntityTag").isEmpty() && compoundtag.getString("DragonHornEntityID").isEmpty()) {
                    if (player.level().isClientSide) {
                        return InteractionResult.SUCCESS;
                    } else {
                        CompoundTag compoundtag1 = new CompoundTag();

                        try {
                            ResourceLocation resourcelocation = BuiltInRegistries.ENTITY_TYPE.getKey(dragon.getType());
                            if (resourcelocation == null
                                || !dragon.save(compoundtag1)
                                || !compoundtag1.hasUUID("UUID")
                                || !dragon.getUUID().equals(compoundtag1.getUUID("UUID"))
                                || player.getItemInHand(hand) != itemstack
                                || dragon.isRemoved()) {
                                return InteractionResult.FAIL;
                            }

                            CompoundTag compoundtag2 = compoundtag == null ? new CompoundTag() : compoundtag.copy();
                            compoundtag2.put("EntityTag", compoundtag1);
                            compoundtag2.putString("DragonHornEntityID", resourcelocation.toString());
                            StackNbt.setTag(itemstack, compoundtag2);
                            if (player.getItemInHand(hand) != itemstack) {
                                StackNbt.setTag(itemstack, compoundtag);
                                return InteractionResult.FAIL;
                            }

                            dragon.discard();
                            if (!dragon.isRemoved()) {
                                StackNbt.setTag(itemstack, compoundtag);
                                return InteractionResult.FAIL;
                            }
                        } catch (RuntimeException runtimeexception) {
                            if (!dragon.isRemoved()) {
                                StackNbt.setTag(itemstack, compoundtag);
                            }

                            LoggerFactory.getLogger("DragonMaid/Horn").error("Dragon horn capture failed", runtimeexception);
                            return dragon.isRemoved() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
                        }

                        player.swing(hand);
                        player.level().playSound(player, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.NEUTRAL, 3.0F, 0.75F);
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    return InteractionResult.FAIL;
                }
            } else {
                return InteractionResult.FAIL;
            }
        } else {
            return InteractionResult.FAIL;
        }
    }
}
