package com.example.dragonmaid.mixin;

import com.example.dragonmaid.petrification.DragonPetrification;
import com.iafenvoy.iceandfire.item.StoneStatueItem;
import com.iafenvoy.iceandfire.item.component.StoneStatusComponent;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, value = {StoneStatueItem.class})
public abstract class StoneStatueItemMixin {
    public StoneStatueItemMixin() {
    }

    @Inject(remap = false, method = {"useOn"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$placeManagedDragonStatue(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        // 1.21.1: IAF CE 2.0 keeps the statue payload in IafDataComponents.STONE_STATUS
        // (StoneStatusComponent.nbt() == the StoneStatueEntity save data) instead of the
        // 1.20.1 "IAFStoneStatueNBT" item tag, so the legacy check never matched.
        StoneStatusComponent status = context.getItemInHand().get(IafDataComponents.STONE_STATUS.get());
        if (status != null && status.nbt().getCompound("StatueEntityTag").contains("DragonMaidPetrification")) {
            cir.setReturnValue(DragonPetrification.placeStatue(context));
        }
    }
}
