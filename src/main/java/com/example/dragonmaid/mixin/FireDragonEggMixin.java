package com.example.dragonmaid.mixin;

import com.example.dragonmaid.egg.FireEggHatching;
import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.registry.IafDragonTypes;
import com.iafenvoy.uranus.object.BlockUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2 上游 {@code mixin/FireDragonEggMixin} 的 IAF CE 2.2 移植：火龙蛋的性别由“灵魂火 + 维度”决定。
 *
 * <p>CE 2.2 与 1.20.1 的结构差异（javap 证据见报告）：
 * <ul>
 *   <li>火龙蛋的孵化判定从 {@code updateEggCondition} 内的
 *       {@code BlockState.isBurning(BlockGetter, BlockPos)} 搬进了 ticker lambda
 *       {@code DragonEggEntity.lambda$static$0}，且改为调用 uranus 的
 *       {@code BlockUtil.isBurning(BlockState)}（只认 FIRE / LAVA，灵魂火不算），
 *       因此“灵魂火也可孵化”的 Redirect 挂在 {@code lambda$static$0} 上。</li>
 *   <li>{@code updateEggCondition} 里只剩一处 {@code Level.setBlockAndUpdate(...)}
 *       （1.20.1 有两处，上游取 ordinal = 1），故此处不写 ordinal。</li>
 *   <li>{@code setGender} 的调用点仍在 {@code updateEggCondition}，接收者仍是
 *       {@code DragonBaseEntity}（{@code DragonType.createEntity} 的返回类型）。</li>
 * </ul>
 */
@Mixin(remap = false, value = {DragonEggEntity.class})
public abstract class FireDragonEggMixin {
    @Unique
    private FireEggHatching.ExpectedSex dragonmaid$hatchingSex;

    /**
     * CE 2.2 的火龙蛋 ticker："BlockUtil.isBurning(state)" 才计入孵化进度。
     * 上游 1.2 语义是灵魂火对火龙蛋也算有效孵化环境，这里补上（lambda$static$0 只注册给火龙蛋）。
     */
    @Redirect(
        remap = false,
        method = {"lambda$static$0"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/uranus/object/BlockUtil;isBurning(Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private static boolean dragonmaid$acceptSoulFire(BlockState state) {
        return BlockUtil.isBurning(state) || state.is(Blocks.SOUL_FIRE);
    }

    /** 在蛋方块被清掉（setBlockAndUpdate AIR）之前采样孵化环境，决定孵出时的性别。 */
    @Inject(
        remap = false,
        method = {"updateEggCondition"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private void dragonmaid$beforeHatchClear(CallbackInfo ci) {
        DragonEggEntity egg = (DragonEggEntity)(Object)this;
        if (egg.getEggType().getType() == IafDragonTypes.FIRE) {
            this.dragonmaid$hatchingSex = FireEggHatching.expected(egg);
        }
    }

    /** MALE → true，FEMALE → false，RANDOM（非灵魂火 / 其他维度）→ 保留 IAF 的随机值。 */
    @ModifyArg(
        remap = false,
        method = {"updateEggCondition"},
        index = 0,
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;setGender(Z)V"
        )
    )
    private boolean dragonmaid$hatchingGender(boolean vanillaRandom) {
        FireEggHatching.ExpectedSex expected = this.dragonmaid$hatchingSex;
        this.dragonmaid$hatchingSex = null;
        if (expected == FireEggHatching.ExpectedSex.MALE) {
            return true;
        } else {
            return expected == FireEggHatching.ExpectedSex.FEMALE ? false : vanillaRandom;
        }
    }
}
