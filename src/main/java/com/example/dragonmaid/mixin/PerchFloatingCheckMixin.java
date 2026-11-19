package com.example.dragonmaid.mixin;

import com.example.dragonmaid.movement.HumanoidFlight;
import com.example.dragonmaid.movement.PerchFlight;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2: 被龙叼着/载着时允许"悬空移动"，不被原版的 floating 判定踢回去。
 *
 * <p>1.2.7 换了做法：不再是 {@code Redirect MinecraftServer#isFlightAllowed()}，而是
 * 在 {@code clientIsFloating / clientVehicleIsFloating} 被写入之后直接把标志翻回 false。
 * 这样不管服务端 {@code allow-flight} 配置如何、也不管原版是不是走了 vehicle 分支，都能生效。</p>
 *
 * <p>1.21.1 的字段与赋值点（已核对源码）：</p>
 * <ul>
 *   <li>{@code handleMovePlayer(ServerboundMovePlayerPacket)} 里
 *       {@code this.clientIsFloating = ...}（{@code noBlocksAround} 判定）。</li>
 *   <li>{@code handleMoveVehicle(ServerboundMoveVehiclePacket)} 里
 *       {@code this.clientVehicleIsFloating = ...}。</li>
 * </ul>
 */
@Mixin({ServerGamePacketListenerImpl.class})
abstract class PerchFloatingCheckMixin {
    @Shadow
    public ServerPlayer player;
    @Shadow
    private double lastGoodY;
    @Shadow
    private boolean clientIsFloating;
    @Shadow
    private boolean clientVehicleIsFloating;

    @Inject(
        method = {"handleMovePlayer"},
        at = {@At(
            value = "FIELD",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clientIsFloating:Z",
            opcode = 181,
            shift = Shift.AFTER
        )},
        require = 1,
        allow = 2
    )
    private void dragonmaid$allowCarriedMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (this.clientIsFloating
            && this.player.isVehicle()
            && PerchFlight.allowsFloating(this.player, packet.getY(this.lastGoodY) - this.lastGoodY)) {
            this.clientIsFloating = false;
        }
    }

    /** 1.2.7: 骑人形龙飞行时，载具移动包的 floating 判定同样要放过。 */
    @Inject(
        method = {"handleMoveVehicle"},
        at = {@At(
            value = "FIELD",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clientVehicleIsFloating:Z",
            opcode = 181,
            shift = Shift.AFTER
        )},
        require = 1,
        allow = 1
    )
    private void dragonmaid$allowHumanoidFlight(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        if (this.clientVehicleIsFloating
            && this.player.getVehicle() instanceof DragonBaseEntity dragon
            && HumanoidFlight.airborne(dragon)
            && HumanoidFlight.validPilot(dragon, this.player)) {
            this.clientVehicleIsFloating = false;
        }
    }
}
