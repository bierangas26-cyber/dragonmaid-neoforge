package com.example.dragonmaid.compat.net;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

public final class NetworkHooks {
    private NetworkHooks() { }

    public static void openScreen(ServerPlayer player, MenuProvider provider) {
        player.openMenu(provider);
    }

    public static void openScreen(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> writer) {
        player.openMenu(provider, writer);
    }

    public static void openScreen(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        player.openMenu(provider, buf -> buf.writeBlockPos(pos));
    }
}
