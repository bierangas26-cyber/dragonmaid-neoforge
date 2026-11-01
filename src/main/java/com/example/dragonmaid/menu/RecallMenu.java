package com.example.dragonmaid.menu;

import com.example.dragonmaid.DragonMaid;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.RecallRosterMessage;
import com.example.dragonmaid.recall.RecallLedger;
import com.example.dragonmaid.recall.RecallService;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import com.example.dragonmaid.compat.net.PacketDistributor;

public final class RecallMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 6;
    public final BlockPos altar;
    private final Player owner;
    private int page;
    private int updateTicks;
    private RecallRosterMessage previous;
    private Component status = Component.empty();
    private boolean pending;
    private long nextRequest;
    public RecallRosterMessage roster;

    public RecallMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    public RecallMenu(int id, Inventory inventory, BlockPos altar) {
        super((MenuType)ModMenus.RECALL.get(), id);
        this.altar = altar.immutable();
        this.owner = inventory.player;
    }

    public boolean stillValid(Player player) {
        return player == this.owner
            && !player.isSpectator()
            && player.level().getBlockState(this.altar).is((Block)DragonMaid.DRAGON_RECALL_ALTAR.get())
            && player.distanceToSqr((double)this.altar.getX() + 0.5, (double)this.altar.getY() + 0.5, (double)this.altar.getZ() + 0.5) <= 64.0;
    }

    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    public void removed(Player player) {
        if (player instanceof ServerPlayer serverplayer) {
            RecallService.cancel(serverplayer);
        }

        super.removed(player);
    }

    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.owner instanceof ServerPlayer && this.updateTicks++ % 20 == 0) {
            this.refresh();
        }
    }

    public boolean clickMenuButton(Player player, int button) {
        if (player instanceof ServerPlayer && this.stillValid(player) && button >= 0 && button <= 2) {
            if (button == 0) {
                this.page = Math.max(0, this.page - 1);
            }

            if (button == 1) {
                this.page++;
            }

            this.refresh(true);
            return true;
        } else {
            return false;
        }
    }

    public void recall(ServerPlayer player, UUID dragon, int price) {
        if (player == this.owner && this.stillValid(player)) {
            if (this.pending || player.level().getGameTime() < this.nextRequest) {
                this.refresh(true);
            } else if (this.previous != null && !this.previous.rows().stream().noneMatch(row -> row.id().equals(dragon) && row.cost() == price)) {
                this.nextRequest = player.level().getGameTime() + 20L;
                RecallService.RequestResult recallservice$requestresult = RecallService.request(player, this.altar, dragon, price);
                this.status = recallservice$requestresult.message();
                this.pending = recallservice$requestresult.status() == RecallService.Status.PENDING;
                this.refresh(true);
            } else {
                this.refresh(true);
            }
        }
    }

    public void completed(Component result) {
        this.pending = false;
        this.status = result;
        this.refresh(true);
    }

    public void refresh() {
        this.refresh(false);
    }

    private void refresh(boolean force) {
        if (this.owner instanceof ServerPlayer serverplayer && this.stillValid(serverplayer)) {
            List<RecallLedger.Entry> list1 = RecallLedger.get(serverplayer.serverLevel()).owned(serverplayer.getUUID());
            int i = Math.max(1, (list1.size() + 6 - 1) / 6);
            this.page = Math.max(0, Math.min(this.page, i - 1));
            List<RecallRosterMessage.Row> list = list1.stream()
                .skip((long)this.page * 6L)
                .limit(6L)
                .map(entry -> RecallRosterMessage.Row.from(entry, RecallService.quoteDetails(serverplayer, this.altar, entry.dragon())))
                .toList();
            RecallRosterMessage recallrostermessage = new RecallRosterMessage(this.containerId, this.page, i, list, this.status, this.pending);
            if (force || !recallrostermessage.equals(this.previous)) {
                this.previous = recallrostermessage;
                DragonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverplayer), recallrostermessage);
            }

            return;
        }
    }
}
