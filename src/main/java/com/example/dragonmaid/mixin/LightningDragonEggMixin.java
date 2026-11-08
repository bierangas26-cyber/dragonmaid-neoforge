package com.example.dragonmaid.mixin;

import com.example.dragonmaid.egg.LightningEggHatching;
import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.registry.IafDragonTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2 上游 {@code mixin/LightningDragonEggMixin} 的 IAF CE 2.2 移植：电龙蛋孵化时读取红石信号
 * （蛋所在格与正下方取较强值；1..7 → 雄，8..15 → 雌，0 → 随机）。
 *
 * <p>CE 2.2 映射（javap 核对）：
 * <ul>
 *   <li>{@code updateEggCondition} 里只有一处 {@code Level.setBlockAndUpdate(...)}（上游 ordinal = 1 已不适用），
 *       在它之前采样信号即可保住“破壳前环境仍可调整”的语义。</li>
 *   <li>{@code setGender} 调用点唯一，接收者 {@code DragonBaseEntity}。</li>
 *   <li>注意：CE 2.2 的孵化条件本身改成了“淋雨 + 露天地”（{@code lambda$static$2}），红石信号不再参与
 *       孵化判定，只按 1.2 的规则决定性别；这与 port 的 Patchouli 手册描述一致。</li>
 * </ul>
 */
@Mixin(remap = false, value = {DragonEggEntity.class})
public abstract class LightningDragonEggMixin {
    /** -1 = 本次破壳没有采样到电龙蛋；1..15 = 红石信号强度。 */
    @Unique
    private int dragonmaid$hatchingSignal = -1;

    @Inject(
        remap = false,
        method = {"updateEggCondition"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private void dragonmaid$beforeLightningHatchClear(CallbackInfo ci) {
        DragonEggEntity egg = (DragonEggEntity)(Object)this;
        if (egg.getEggType().getType() == IafDragonTypes.LIGHTNING) {
            this.dragonmaid$hatchingSignal = LightningEggHatching.signal(egg.level(), egg.blockPosition());
        }
    }

    @ModifyArg(
        remap = false,
        method = {"updateEggCondition"},
        index = 0,
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;setGender(Z)V"
        )
    )
    private boolean dragonmaid$lightningHatchingGender(boolean vanillaRandom) {
        int signal = this.dragonmaid$hatchingSignal;
        this.dragonmaid$hatchingSignal = -1;
        return signal > 0 ? signal < 8 : vanillaRandom;
    }
}
