package com.example.dragonmaid.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.2: 创造模式背包里的 {@code CreativeModeInventoryScreen$SlotWrapper} 只是个套壳，
 * 真正的槽位在 private final {@code target} 字段里。GemModeInput 需要它来定位真实槽位。
 *
 * <p>1.21.1 校验（javap 实测）：{@code CreativeModeInventoryScreen$SlotWrapper} 存在，
 * 字段 {@code final Slot target} 仍在（官方名，无 SRG 混淆），因此 {@code remap = false}。
 */
@Mixin(remap = false, targets = {"net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper"})
public interface CreativeGemSlotAccessor {
    @Accessor(remap = false, value = "target")
    Slot dragonmaid$realGemSlot();
}
