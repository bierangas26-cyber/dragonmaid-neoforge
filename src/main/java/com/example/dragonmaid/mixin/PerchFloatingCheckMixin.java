package com.example.dragonmaid.mixin;

import com.example.dragonmaid.movement.PerchFlight;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ServerGamePacketListenerImpl.class})
abstract class PerchFloatingCheckMixin {
   @Shadow
   public ServerPlayer player;
   @Shadow
   private double lastGoodY;

   @Redirect(
      method = {"handleMovePlayer"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/MinecraftServer;isFlightAllowed()Z"
      ),
      require = 1,
      allow = 1
   )
   private boolean dragonmaid$allowCarriedMovement(MinecraftServer server, ServerboundMovePlayerPacket packet) {
      return server.isFlightAllowed() || this.player.isVehicle() && PerchFlight.allowsFloating(this.player, packet.getY(this.lastGoodY) - this.lastGoodY);
   }
}
