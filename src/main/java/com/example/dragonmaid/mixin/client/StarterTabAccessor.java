package com.example.dragonmaid.mixin.client;

import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.2.7: 取 {@code GridLayoutTab#layout}（1.21.1 仍为 {@code protected final GridLayout layout}）。
 */
@Mixin({GridLayoutTab.class})
public interface StarterTabAccessor {
    @Accessor("layout")
    GridLayout dragonmaid$layout();
}
