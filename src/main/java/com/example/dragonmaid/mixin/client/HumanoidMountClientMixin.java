package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.event.handler.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2.7: 骑人形龙时不要套用 IAF 的"龙骑乘"客户端表现。
 *
 * <p>IAF CE 2.2 把这三个行为搬成了 NeoForge 事件处理静态方法，签名与 1.20.1 完全不同，
 * 因此按等价语义重新落点：</p>
 * <ul>
 *   <li>上游 {@code shouldCancelRender} → {@code disablePlayerRenderWhenNeed(RenderPlayerEvent.Pre)}：
 *       人形龙只在一人称（且是自己）时隐藏玩家，其余情况保留玩家身体（人形骑乘姿势由
 *       {@code HumanoidRiderModelMixin} / {@code PlayerHumanoidRidingRendererMixin} 摆）。</li>
 *   <li>上游 {@code onCameraSetup} → {@code onCameraSetup(CalculateDetachedCameraDistanceEvent)}：直接跳过。</li>
 *   <li>上游 {@code onEntityMount}：IAF CE 2.2 已无此处理器，不做。</li>
 *   <li>上游 {@code onLivingUpdate} 里 redirect {@code Player.getVehicle()} → 同样在 IAF CE 2.2 的
 *       {@code onLivingUpdate(EntityTickEvent.Post)} 里 redirect 第一处 {@code Player.getVehicle()}，
 *       让龙飞控分支看到 null，从而不覆盖人形骑乘的输入。</li>
 * </ul>
 */
@Mixin(remap = false, value = {ClientEvents.class})
public abstract class HumanoidMountClientMixin {
    @Inject(remap = false, method = {"disablePlayerRenderWhenNeed"}, at = {@At("HEAD")}, cancellable = true)
    private static void dragonmaid$drawGroundRider(RenderPlayerEvent.Pre event, CallbackInfo ci) {
        Player rider = event.getEntity();
        if (DragonForms.isHumanoid(rider.getVehicle())) {
            Minecraft minecraft = Minecraft.getInstance();
            if (rider == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson()) {
                event.setCanceled(true);
            }

            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"onCameraSetup"}, at = {@At("HEAD")}, cancellable = true)
    private static void dragonmaid$keepGroundMountCamera(CalculateDetachedCameraDistanceEvent event, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof DragonBaseEntity dragon && DragonForms.isHumanoid(dragon)) {
            ci.cancel();
        }
    }

    @Redirect(
        remap = false,
        method = {"onLivingUpdate"},
        at = @At(
            value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/world/entity/player/Player;getVehicle()Lnet/minecraft/world/entity/Entity;"
        )
    )
    private static Entity dragonmaid$skipDragonFlightControls(Player player) {
        Entity mount = player.getVehicle();
        if (mount instanceof DragonBaseEntity dragon && DragonForms.isHumanoid(dragon)) {
            return null;
        }

        return mount;
    }
}
