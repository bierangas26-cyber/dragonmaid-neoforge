package com.example.dragonmaid.client;

import com.example.dragonmaid.movement.PerchFlight;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.PerchFlightInputMessage;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * 1.2: 骑龙「栖息飞行」的客户端输入采样。
 *
 * <p>1.20.1 Forge 用 {@code MovementInputUpdateEvent}；1.21.1 NeoForge 同名事件在
 * {@code net.neoforged.neoforge.client.event} 下。{@code Input} 的 SRG 字段对应官方名：
 * {@code f_108572_ -> jumping}、{@code f_108573_ -> shiftKeyDown}；
 * {@code m_91080_ -> screen}、{@code m_20160_ -> isVehicle()}、{@code m_20197_ -> getPassengers()}。
 */
@EventBusSubscriber(modid = "dragonmaid", value = {Dist.CLIENT})
public final class PerchFlightInput {
    private static WeakReference<LocalPlayer> previousPlayer = new WeakReference<>(null);
    private static boolean previousShift;
    private static int sent = -1;

    private PerchFlightInput() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void read(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            Input input = event.getInput();
            boolean available = Minecraft.getInstance().screen == null;
            boolean jump = available && input.jumping;
            boolean shift = available && input.shiftKeyDown;
            if (previousPlayer.get() != player) {
                previousPlayer = new WeakReference<>(player);
                sent = -1;
                previousShift = shift;
            }

            boolean pressed = shift && !previousShift;
            previousShift = shift;
            if (player.isVehicle() && !player.getPassengers().stream().noneMatch(DragonBaseEntity.class::isInstance)) {
                int keys = (jump ? 1 : 0) | (shift ? 2 : 0);
                PerchFlight.setLocalInput(player, jump, shift);
                if (sent != keys || pressed) {
                    sent = keys;
                    DragonNetwork.CHANNEL.sendToServer(
                        new PerchFlightInputMessage(jump, shift, pressed && PerchFlight.hasFooting(player))
                    );
                }
            } else {
                sent = -1;
            }
        }
    }
}
