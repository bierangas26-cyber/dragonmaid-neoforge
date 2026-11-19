package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.LongswordParry;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2.7: 把"正在处理的 useItem 包序号"暴露给 {@link LongswordParry}，
 * 用来判断某次格挡是不是同一个 use 序列（防止右键连点绕过前摇）。
 *
 * <p>1.21.1 的 {@code handleUseItem} 里 {@code ServerPlayerGameMode.useItem} 调用点与 1.20.1 相同。</p>
 */
@Mixin({ServerGamePacketListenerImpl.class})
abstract class LongswordUseSequenceMixin implements LongswordParry.UseSequence {
    @Unique
    private int dragonmaid$handlingUseSequence = -1;

    @Inject(
        method = {"handleUseItem"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItem(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
        )}
    )
    private void dragonmaid$beginItemUse(ServerboundUseItemPacket packet, CallbackInfo ci) {
        this.dragonmaid$handlingUseSequence = packet.getSequence();
    }

    @Inject(method = {"handleUseItem"}, at = {@At("RETURN")})
    private void dragonmaid$finishItemUse(ServerboundUseItemPacket packet, CallbackInfo ci) {
        this.dragonmaid$handlingUseSequence = -1;
    }

    @Override
    public int dragonmaid$useItemSequence() {
        return this.dragonmaid$handlingUseSequence;
    }
}
