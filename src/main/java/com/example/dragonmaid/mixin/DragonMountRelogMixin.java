package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.HumanoidGroundMount;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.2.7: 人形龙作为"地面坐骑"时，玩家掉线重登要能把人重新放回龙背上；
 * 并且当座位不可用、原版准备丢弃这条龙时，别把已驯服的自家龙 discard 掉。
 *
 * <p>1.21.1 的 {@code PlayerList.placeNewPlayer} 与原版一致：RootVehicle 还原里
 * {@code ServerPlayer.startRiding} 出现两次、{@code Entity.discard} 出现两次，
 * 所以两个 redirect 都用 {@code require = 2}。</p>
 */
@Mixin({PlayerList.class})
public abstract class DragonMountRelogMixin {
    @Redirect(
        method = {"placeNewPlayer"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;startRiding(Lnet/minecraft/world/entity/Entity;Z)Z"
        ),
        require = 2
    )
    private boolean dragonmaid$restoreRider(ServerPlayer player, Entity vehicle, boolean force) {
        return HumanoidGroundMount.restoreRider(player, vehicle, force);
    }

    @Redirect(
        method = {"placeNewPlayer"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;discard()V"),
        require = 2
    )
    private void dragonmaid$keepOwnedDragonIfSeatUnavailable(Entity vehicle) {
        if (!(vehicle instanceof DragonBaseEntity dragon) || !dragon.isTame() || dragon.getOwnerUUID() == null) {
            vehicle.discard();
        }
    }
}
