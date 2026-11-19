package com.example.dragonmaid.network;

import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.movement.HumanoidFlight;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.2.7: 骑乘飞行时的玩家输入（下降 / 吐息 / 直击 / 格挡）。
 * 上游用单字节位域打包 4 个布尔，节省带宽；4 参构造保留为兼容重载。
 */
public record HumanoidFlightInputMessage(int vehicle, boolean descend, boolean breathe, boolean strike, boolean guard) {
    public HumanoidFlightInputMessage(int vehicle, boolean descend, boolean breathe, boolean strike) {
        this(vehicle, descend, breathe, strike, false);
    }

    static void encode(HumanoidFlightInputMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.vehicle);
        buffer.writeByte((message.descend ? 1 : 0) | (message.breathe ? 2 : 0) | (message.strike ? 4 : 0) | (message.guard ? 8 : 0));
    }

    static HumanoidFlightInputMessage decode(FriendlyByteBuf buffer) {
        int vehicle = buffer.readVarInt();
        int flags = buffer.readUnsignedByte();
        return new HumanoidFlightInputMessage(vehicle, (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0, (flags & 8) != 0);
    }

    static void handle(HumanoidFlightInputMessage message, Supplier<Context> context) {
        Context event = context.get();
        event.enqueueWork(() -> message.applyFor(event.getSender()));
        event.setPacketHandled(true);
    }

    public boolean applyFor(ServerPlayer player) {
        if (player != null
            && player.getVehicle() instanceof DragonBaseEntity dragon
            && dragon.getId() == this.vehicle
            && HumanoidFlight.validRider(dragon, player)) {
            HumanoidFlight flight = HumanoidFlight.of(dragon);
            boolean descending = this.descend && HumanoidFlight.validPilot(dragon, player);
            flight.setDescend(descending);
            flight.setRiderAttacks(this.breathe, this.strike);
            boolean guarding = this.guard && HumanoidFlight.emptyHands(player);
            flight.setRiderGuard(guarding);
            return this.guard ? guarding : !this.descend || descending || this.breathe || this.strike;
        } else {
            return false;
        }
    }
}
