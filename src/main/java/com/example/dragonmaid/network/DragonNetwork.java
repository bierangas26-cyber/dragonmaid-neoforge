package com.example.dragonmaid.network;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.mixin.DragonMenuAccessor;
import com.example.dragonmaid.recall.RecallContent;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.screen.menu.DragonMenu;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.example.dragonmaid.compat.net.NetworkDirection;
import com.example.dragonmaid.compat.net.NetworkRegistry;
import com.example.dragonmaid.compat.net.NetworkEvent.Context;
import com.example.dragonmaid.compat.net.SimpleChannel;

public final class DragonNetwork {
    private static final String VERSION = "15";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath("dragonmaid", "main"), () -> "15", "15"::equals, "15"::equals
    );

    private DragonNetwork() {
    }

    public static String protocolVersion() {
        return "15";
    }

    public static void register() {
        RecallContent.network();
        CHANNEL.registerMessage(
            9,
            TailSweepMessage.class,
            TailSweepMessage::encode,
            TailSweepMessage::decode,
            TailSweepMessage::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            8,
            AccessoryMenuAction.class,
            AccessoryMenuAction::encode,
            AccessoryMenuAction::decode,
            AccessoryMenuAction::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            7,
            FeedingEffectMessage.class,
            FeedingEffectMessage::encode,
            FeedingEffectMessage::decode,
            FeedingEffectMessage::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            6,
            BreathImpactMessage.class,
            BreathImpactMessage::encode,
            BreathImpactMessage::decode,
            BreathImpactMessage::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            5,
            BreathForecastMessage.class,
            BreathForecastMessage::encode,
            BreathForecastMessage::decode,
            BreathForecastMessage::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            4,
            BreathAnimationMessage.class,
            BreathAnimationMessage::encode,
            BreathAnimationMessage::decode,
            BreathAnimationMessage::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            0,
            DragonNetwork.Action.class,
            DragonNetwork.Action::encode,
            DragonNetwork.Action::decode,
            DragonNetwork.Action::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            1,
            BreathWaveMessage.class,
            BreathWaveMessage::encode,
            BreathWaveMessage::decode,
            BreathWaveMessage::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            2,
            ExecutionPromptMessage.class,
            ExecutionPromptMessage::encode,
            ExecutionPromptMessage::decode,
            ExecutionPromptMessage::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            3,
            ExecutionResponseMessage.class,
            ExecutionResponseMessage::encode,
            ExecutionResponseMessage::decode,
            ExecutionResponseMessage::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    public static record Action(int menuId, int entityId, UUID uuid, boolean learn) {
        private static void encode(DragonNetwork.Action message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.menuId);
            buffer.writeVarInt(message.entityId);
            buffer.writeUUID(message.uuid);
            buffer.writeBoolean(message.learn);
        }

        private static DragonNetwork.Action decode(FriendlyByteBuf buffer) {
            return new DragonNetwork.Action(buffer.readVarInt(), buffer.readVarInt(), buffer.readUUID(), buffer.readBoolean());
        }

        private static void handle(DragonNetwork.Action message, Supplier<Context> supplier) {
            Context context = supplier.get();
            context.enqueueWork(() -> message.applyFor(context.getSender()));
            context.setPacketHandled(true);
        }

        public boolean applyFor(ServerPlayer player) {
            if (player != null && !player.isSpectator() && player.containerMenu.containerId == this.menuId && player.containerMenu instanceof DragonMenu) {
                DragonBaseEntity entitydragonbase = ((DragonMenuAccessor)player.containerMenu).dragonmaid$getDragon();
                if (DragonForms.canManage(entitydragonbase, player)
                    && entitydragonbase.getId() == this.entityId
                    && entitydragonbase.getUUID().equals(this.uuid)
                    && player.containerMenu.stillValid(player)) {
                    return this.learn ? AccessoryMenuAction.openKnowledge(player, entitydragonbase) : AccessoryMenuAction.transform(player, entitydragonbase);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }


    /** NeoForge: register the single adapter payload that carries all legacy channel messages. */
    public static void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        com.example.dragonmaid.compat.net.SimpleChannel.bootstrap(event);
    }
}
