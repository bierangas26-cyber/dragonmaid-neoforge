package com.example.dragonmaid.gametest;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkChannel;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;

/**
 * Shared fixtures for the headless NeoForge GameTests.
 *
 * <p>Every test uses the empty 5x5x5 template {@code dragonmaid:gametest_empty}
 * (see {@code data/dragonmaid/structure/gametest_empty.nbt}), so the tests only own the
 * objects they create themselves and clean them up again.</p>
 */
public final class GameTestSupport {
    /** Structure template every test in this package runs inside. */
    public static final String TEMPLATE = "gametest_empty";

    /** Relative position at the centre of the 5x5x5 template. */
    public static final BlockPos CENTRE = new BlockPos(2, 2, 2);

    private GameTestSupport() {
    }

    /**
     * A {@link ServerPlayer} that is really registered with the level.
     *
     * <p>A plain {@code new ServerPlayer(...)} is not enough: dragon ownership resolves through
     * {@code OwnableEntity#getOwner() -> level().getPlayerByUUID(...)}, and an unregistered player
     * is invisible to that lookup, so every tamed-dragon interaction would be rejected.</p>
     */
    public static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = helper.absolutePos(CENTRE);
        player.teleportTo(helper.getLevel(), pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    /** Detach a mock player from the player list again (tests must not leak players). */
    public static void releasePlayer(GameTestHelper helper, ServerPlayer player) {
        if (player != null) {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    /** Spawn a tamed ice dragon owned by {@code owner} at a position relative to the template. */
    public static DragonBaseEntity spawnTamedDragon(GameTestHelper helper, ServerPlayer owner, BlockPos relative) {
        ServerLevel level = helper.getLevel();
        DragonBaseEntity dragon = IafEntities.ICE_DRAGON.get().create(level);
        if (dragon == null) {
            throw new IllegalStateException("could not create an ice dragon");
        }
        BlockPos pos = helper.absolutePos(relative);
        dragon.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        dragon.setTame(true, false);
        dragon.setOwnerUUID(owner.getUUID());
        if (!level.addFreshEntity(dragon)) {
            throw new IllegalStateException("the level refused the dragon entity");
        }
        return dragon;
    }

    /** Discard an entity without touching level state that a test still needs. */
    public static void discard(DragonBaseEntity dragon) {
        if (dragon != null && !dragon.isRemoved()) {
            dragon.discard();
        }
    }

    /**
     * Pretend that the NeoForge handshake negotiated {@code payloadId} for this connection.
     *
     * <p>A {@link ServerPlayer} created inside a game test never runs the configuration-phase
     * channel negotiation, so {@code NetworkRegistry.checkPacket} rejects every modded
     * client-bound payload ("Payload ... may not be sent to the client!") even though the same
     * call works on a real client.  Seeding the connection's payload setup with the channel makes
     * menu-opening paths ({@code NetworkHooks.openScreen}) behave like they do in production.</p>
     */
    public static void allowClientPayload(ServerPlayer player, ResourceLocation payloadId) {
        var connection = player.connection.getConnection();
        NetworkPayloadSetup previous = ChannelAttributes.getPayloadSetup(connection);
        Map<ConnectionProtocol, Map<ResourceLocation, NetworkChannel>> channels = new LinkedHashMap<>();
        if (previous != null) {
            channels.putAll(previous.channels());
        }
        channels.computeIfAbsent(ConnectionProtocol.PLAY, protocol -> new LinkedHashMap<>())
            .put(payloadId, new NetworkChannel(payloadId, "1"));
        ChannelAttributes.setPayloadSetup(connection, new NetworkPayloadSetup(channels));
    }

    /** Convenience wrapper for "put this stack in the first accessory slot". */
    public static void equip(Container container, int slot, ItemStack stack) {
        container.setItem(slot, stack);
    }
}
