package com.example.dragonmaid.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.2.7: 调 {@code GridLayout.CellInhabitant#getLastOccupiedRow()}（包可见，需 Invoker）。
 */
@Mixin(targets = {"net.minecraft.client.gui.layouts.GridLayout$CellInhabitant"})
public interface StarterGridCellInvoker {
    @Invoker("getLastOccupiedRow")
    int dragonmaid$lastRow();
}
