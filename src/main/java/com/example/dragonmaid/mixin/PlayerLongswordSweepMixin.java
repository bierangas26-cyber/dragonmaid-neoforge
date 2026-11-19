package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.BladeCombat;
import com.example.dragonmaid.combat.SpecialWeapons;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2.7: 玩家持长剑时用自定义横扫表现（粒子/轨迹），替换原版 sweepAttack。
 */
@Mixin({Player.class})
public abstract class PlayerLongswordSweepMixin {
    @Inject(method = {"sweepAttack"}, at = {@At("HEAD")}, cancellable = true)
    private void dragonmaid$longswordSweepVisual(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide() && SpecialWeapons.isLongsword(player.getMainHandItem())) {
            BladeCombat.playerLongswordSweepVisual(player);
            ci.cancel();
        }
    }
}
