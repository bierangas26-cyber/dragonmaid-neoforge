package com.example.dragonmaid.mixin.client;

import java.util.List;
import net.minecraft.client.gui.layouts.GridLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.2.7: 读 {@code GridLayout#cellInhabitants}，用来算出"开局伙伴"入口该追加到第几行。
 */
@Mixin({GridLayout.class})
public interface StarterGridAccessor {
    @Accessor("cellInhabitants")
    List<?> dragonmaid$occupiedCells();
}
