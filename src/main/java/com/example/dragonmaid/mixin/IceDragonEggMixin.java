package com.example.dragonmaid.mixin;

import com.example.dragonmaid.egg.IceEggHatching;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.item.block.entity.EggInIceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.2 上游 {@code mixin/IceDragonEggMixin} 的 IAF CE 2.2 移植：被冻进冰里的冰龙蛋，性别由蛋下方的
 * 霜凌（DRAGON_ICE → 雄）/ 蓝冰（BLUE_ICE → 雌）决定。
 *
 * <p>CE 2.2 映射（javap 核对）：
 * <ul>
 *   <li>上游 {@code TileEntityEggInIce} → {@code com.iafenvoy.iceandfire.item.block.entity.EggInIceBlockEntity}</li>
 *   <li>上游 {@code EntityIceDragon} → {@code com.iafenvoy.iceandfire.entity.IceDragonEntity}</li>
 *   <li>{@code tickEgg} 仍是 {@code public static void tickEgg(Level, BlockPos, BlockState, EggInIceBlockEntity)}，
 *       内部仍只有一处 {@code IceDragonEntity.setGender(Z)}（接收者是 IceDragonEntity）。</li>
 *   <li>静态方法里的 @Redirect 需要静态 handler：签名 = 目标实例 + 目标参数 + 外层方法参数
 *       （与 port 里 DragonForgeBreathMixin 的 lambda Redirect 同一规则）。</li>
 * </ul>
 */
@Mixin(remap = false, value = {EggInIceBlockEntity.class})
public abstract class IceDragonEggMixin {
    @Redirect(
        remap = false,
        method = {"tickEgg"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/IceDragonEntity;setGender(Z)V"
        )
    )
    private static void dragonmaid$hatchGender(
        IceDragonEntity dragon,
        boolean vanillaRandom,
        Level level,
        BlockPos pos,
        BlockState state,
        EggInIceBlockEntity egg
    ) {
        boolean male = switch (IceEggHatching.expected(level, pos)) {
            case MALE -> true;
            case FEMALE -> false;
            case RANDOM -> vanillaRandom;
        };
        dragon.setGender(male);
    }
}
