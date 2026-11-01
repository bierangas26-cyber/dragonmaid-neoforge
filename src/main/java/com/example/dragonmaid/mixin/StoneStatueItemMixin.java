package com.example.dragonmaid.mixin;

import com.example.dragonmaid.petrification.DragonPetrification;
import com.iafenvoy.iceandfire.item.StoneStatueItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.example.dragonmaid.compat.StackNbt;

@Mixin(remap = false, value = {StoneStatueItem.class})
public abstract class StoneStatueItemMixin {
    public StoneStatueItemMixin() {
    }

    @Inject(remap = false, method = {"useOn"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$placeManagedDragonStatue(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        CompoundTag compoundtag = StackNbt.getTag(context.getItemInHand());
        if (compoundtag != null && compoundtag.getCompound("IAFStoneStatueNBT").getCompound("StatueEntityTag").contains("DragonMaidPetrification")) {
            cir.setReturnValue(DragonPetrification.placeStatue(context));
        }
    }
}
