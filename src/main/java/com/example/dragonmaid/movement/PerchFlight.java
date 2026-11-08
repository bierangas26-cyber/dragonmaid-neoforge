package com.example.dragonmaid.movement;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * 1.2: 迷你龙“停在肩上”的飞行辅助（最多 3 只小龍提供缓降/滑翔/上升，Shift 下降 + 落地时下坐骑）。
 * 玩家侧的 Controls 接口由 mixin（PerchPlayerTravelMixin 等）实现。
 */
@EventBusSubscriber(modid = "dragonmaid")
public final class PerchFlight {
    private static final int JUMP = 1;
    private static final int DESCEND = 2;
    private static final double SLOW_FALL = -0.14;
    private static final double GLIDE = -0.02;
    private static final double POWERED_SPEED = 0.2;

    private PerchFlight() {
    }

    public static boolean isCarrierDragon(Player player, Entity passenger) {
        if (passenger instanceof DragonBaseEntity dragon
            && dragon.getVehicle() == player
            && player.getUUID().equals(dragon.getOwnerUUID())
            && MiniatureForm.isShoulderMini(dragon)) {
            return true;
        }

        return false;
    }

    public static int count(Player player) {
        if (player.isVehicle() && player.isAlive()) {
            int count = 0;

            for (Entity passenger : player.getPassengers()) {
                if (isCarrierDragon(player, passenger)) {
                    if (++count == 3) {
                        break;
                    }
                }
            }

            return count;
        } else {
            return 0;
        }
    }

    public static boolean controlsDismount(Player player, DragonBaseEntity dragon) {
        return isCarrierDragon(player, dragon)
            ? true
            : dragon.getVehicle() == player
                && player.getUUID().equals(dragon.getOwnerUUID())
                && (dragon.getDragonStage() == 1 || MiniatureForm.isSmall(dragon))
                && !DragonForms.isHumanoid(dragon)
                && count(player) > 0;
    }

    private static boolean canAssist(Player player) {
        return player.isAlive()
            && !player.isPassenger()
            && !player.isSleeping()
            && !player.isSpectator()
            && !player.getAbilities().flying
            && !player.isFallFlying()
            && !player.isSwimming()
            && !player.isInFluidType()
            && !player.onClimbable()
            && !player.hasEffect(MobEffects.LEVITATION)
            && !player.isAutoSpinAttack();
    }

    public static boolean supportsAirMovement(Player player) {
        return count(player) > 0 && !player.onGround() && canAssist(player);
    }

    public static void setLocalInput(Player player, boolean jump, boolean shift) {
        ((PerchFlight.Controls)player).dragonmaid$perchKeys((jump ? 1 : 0) | (shift ? 2 : 0));
    }

    public static void acceptInput(ServerPlayer player, boolean jump, boolean shift, boolean groundPress) {
        if (player != null) {
            int count = count(player);
            int old = ((PerchFlight.Controls)player).dragonmaid$perchKeys();
            setLocalInput(player, count > 0 && jump, count > 0 && shift);
            if (count != 0 && groundPress && shift && (old & 2) == 0 && !player.isPassenger() && !player.isSpectator() && hasFooting(player)) {
                List<Entity> dismount = player.getPassengers().stream().filter(passenger -> {
                    if (passenger instanceof DragonBaseEntity dragon && controlsDismount(player, dragon)) {
                        return true;
                    }

                    return false;
                }).toList();
                dismount.forEach(Entity::stopRiding);
                player.connection.send(new ClientboundSetPassengersPacket(player));
            }
        }
    }

    public static void beforeTravel(Player player) {
        if (!player.level().isClientSide || player.isLocalPlayer()) {
            int count = count(player);
            if (count == 0) {
                ((PerchFlight.Controls)player).dragonmaid$perchKeys(0);
            } else if (canAssist(player)) {
                player.resetFallDistance();
                if (!player.onGround()) {
                    int keys = ((PerchFlight.Controls)player).dragonmaid$perchKeys();
                    Vec3 velocity = player.getDeltaMovement();
                    double y;
                    if ((keys & 2) != 0) {
                        y = -0.2;
                    } else if (count < 3) {
                        y = Math.max(velocity.y, count == 2 ? -0.02 : -0.14);
                    } else if ((keys & 1) != 0) {
                        y = 0.2;
                    } else {
                        y = hasFooting(player) ? -0.001 : 0.0;
                    }

                    if (y != velocity.y) {
                        player.setDeltaMovement(velocity.x, y, velocity.z);
                    }
                }
            }
        }
    }

    public static boolean hasFooting(Player player) {
        AABB box = player.getBoundingBox();
        AABB feet = new AABB(box.minX, box.minY - 1.0E-6, box.minZ, box.maxX, box.minY, box.maxZ);
        return !player.level().noCollision(player, feet);
    }

    @SubscribeEvent
    public static void preventFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && count(player) > 0 && canAssist(player)) {
            player.resetFallDistance();
            event.setCanceled(true);
        }
    }

    public static boolean allowsFloating(ServerPlayer player, double deltaY) {
        if (Double.isFinite(deltaY) && !player.onGround() && canAssist(player)) {
            int count = count(player);
            if (count == 0) {
                return false;
            } else if (count < 3) {
                return deltaY < -0.005;
            } else {
                boolean ascending = (((PerchFlight.Controls)player).dragonmaid$perchKeys() & 3) == 1;
                return deltaY <= (ascending ? 0.21500000000000002 : 0.005);
            }
        } else {
            return false;
        }
    }

    public interface Controls {
        int dragonmaid$perchKeys();

        void dragonmaid$perchKeys(int var1);
    }
}
