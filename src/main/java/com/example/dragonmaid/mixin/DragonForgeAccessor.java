package com.example.dragonmaid.mixin;

import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * IAF CE 2.0 把 DragonForgeBlockEntity.cookTime 改成了 private double，且没有公开 getter/setter
 * （1.20.1 旧 IAF 是可直接访问的字段）。本 mod 的"多龙协作加速"需要读写它，故用 @Accessor。
 *
 * 对应 1.20.1 行为：cookTime 公开字段读写 → dragonmaid$getCookTime()/dragonmaid$setCookTime(double)。
 * mixin 配置需要新增本条（见 mixins.json）。
 */
@Mixin(remap = false, value = {DragonForgeBlockEntity.class})
public interface DragonForgeAccessor {
    @Accessor(remap = false, value = "cookTime")
    double dragonmaid$getCookTime();

    @Accessor(remap = false, value = "cookTime")
    void dragonmaid$setCookTime(double cookTime);
}
