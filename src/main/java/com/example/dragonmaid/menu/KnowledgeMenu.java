package com.example.dragonmaid.menu;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.learning.KnowledgeMaterials;
import com.example.dragonmaid.learning.KnowledgeUpgrades;
import com.example.dragonmaid.network.AccessoryMenuAction;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import com.example.dragonmaid.compat.StackNbt;

public final class KnowledgeMenu extends AbstractContainerMenu {
    public static final int MATERIAL_SLOT = 0;
    public static final int UPGRADE_SLOT = 1;
    public static final int REGULAR_A_SLOT = 1;
    public static final int REGULAR_B_SLOT = 2;
    public static final int CURSE_SLOT = 3;
    public static final int PLAYER_START = 4;
    public static final int PLAYER_MAIN_END = 31;
    public static final int PLAYER_END = 40;
    private final DragonBaseEntity dragon;
    private final Player viewer;
    private final SimpleContainer input = new SimpleContainer(4);
    private final SimpleContainerData data = new SimpleContainerData(14);

    public KnowledgeMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, readDragon(inventory, buffer));
    }

    private static DragonBaseEntity readDragon(Inventory inventory, FriendlyByteBuf buffer) {
        int i = buffer.readVarInt();
        UUID uuid = buffer.readUUID();
        Entity entity = inventory.player.level().getEntity(i);
        if (entity instanceof DragonBaseEntity entitydragonbase && entity.getUUID().equals(uuid)) {
            return entitydragonbase;
        }

        return null;
    }

    public KnowledgeMenu(int id, Inventory inventory, DragonBaseEntity dragon) {
        super((MenuType)ModMenus.KNOWLEDGE.get(), id);
        this.dragon = dragon;
        this.viewer = inventory.player;
        this.addSlot(
            new Slot(this.input, 0, 10, 51) {
                public boolean mayPlace(ItemStack stack) {
                    return KnowledgeMenu.this.stillValid(KnowledgeMenu.this.viewer)
                        && KnowledgeMenu.this.canLearn()
                        && KnowledgeMaterials.accepts(stack, KnowledgeMenu.this.knowledgeLevel());
                }

                public boolean mayPickup(Player player) {
                    return KnowledgeMenu.this.stillValid(player);
                }
            }
        );
        this.addBookSlot(1, 10);
        this.addBookSlot(2, 34);
        this.addBookSlot(3, 70);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 34 + j * 18, 144 + i * 18));
            }
        }

        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(inventory, k, 34 + k * 18, 202));
        }

        this.addDataSlots(this.data);
        this.updateData();
    }

    private void addBookSlot(int index, int x) {
        this.addSlot(
            new Slot(this.input, index, x, 105) {
                public boolean isActive() {
                    return KnowledgeMenu.this.bookSlotEnabled(this.index) || this.hasItem();
                }

                public boolean mayPlace(ItemStack stack) {
                    return KnowledgeMenu.this.bookSlotEnabled(this.index)
                        && KnowledgeMenu.this.stillValid(KnowledgeMenu.this.viewer)
                        && KnowledgeMenu.matchesSlot(this.index, stack);
                }

                public boolean mayPickup(Player player) {
                    return KnowledgeMenu.this.stillValid(player);
                }

                public int getMaxStackSize() {
                    return 1;
                }
            }
        );
    }

    private boolean server() {
        return this.dragon != null && !this.dragon.level().isClientSide;
    }

    private void updateData() {
        if (this.server()) {
            KnowledgeUpgrades.Requirement knowledgeupgrades$requirement = KnowledgeUpgrades.nextRequirement(this.dragon);
            this.data.set(0, DragonForms.state(this.dragon).dragonmaid$getKnowledge());
            ItemStack itemstack = this.input.getItem(0);
            int i = KnowledgeMaterials.experience(itemstack, this.knowledgeLevel());
            this.data.set(1, Math.min(10001, i));
            this.data.set(2, DragonForms.state(this.dragon).dragonmaid$getKnowledgeLevel());
            this.data.set(3, KnowledgeUpgrades.learningLimit(this.dragon));
            this.data.set(4, knowledgeupgrades$requirement != null ? 1 : 0);
            this.data.set(5, knowledgeupgrades$requirement == null ? 0 : knowledgeupgrades$requirement.bookCount());
            this.data.set(6, knowledgeupgrades$requirement == null ? 0 : knowledgeupgrades$requirement.cursedBookCount());
            this.data
                .set(
                    7, (KnowledgeUpgrades.isNonCursedBook(this.input.getItem(1)) ? 1 : 0) + (KnowledgeUpgrades.isNonCursedBook(this.input.getItem(2)) ? 1 : 0)
                );
            this.data.set(8, KnowledgeUpgrades.isCursedBook(this.input.getItem(3)) ? 1 : 0);
            this.data.set(9, KnowledgeUpgrades.canUpgrade(this.dragon, this.input.getItem(1), this.input.getItem(2), this.input.getItem(3)) ? 1 : 0);
            this.data.set(10, knowledgeupgrades$requirement == null ? 0 : this.knowledgeLevel() + 1);
            this.data
                .set(11, knowledgeupgrades$requirement == null ? DragonAccessories.unlockedSlots(this.dragon) : Math.min(3, this.knowledgeLevel() + 1));
            int j = this.completeItemsToLearn(itemstack, i);
            this.data.set(12, j);
            this.data.set(13, j * i);
        }
    }

    public void broadcastChanges() {
        this.updateData();
        super.broadcastChanges();
    }

    public int knowledge() {
        return this.server() ? DragonForms.state(this.dragon).dragonmaid$getKnowledge() : this.data.get(0);
    }

    public int perItem() {
        return this.data.get(1);
    }

    public int knowledgeLevel() {
        return this.server() ? DragonForms.state(this.dragon).dragonmaid$getKnowledgeLevel() : Math.max(0, this.data.get(2));
    }

    public int learningLimit() {
        return this.server() ? KnowledgeUpgrades.learningLimit(this.dragon) : Math.max(300, this.data.get(3));
    }

    public boolean upgradeAvailable() {
        return this.server() ? KnowledgeUpgrades.available(this.dragon) : this.data.get(4) != 0;
    }

    public boolean canLearn() {
        return this.knowledge() < 10000;
    }

    public int previewCount() {
        return this.data.get(12);
    }

    public int previewExperience() {
        return this.data.get(13);
    }

    private int completeItemsToLearn(ItemStack material, int perItem) {
        return !material.isEmpty() && perItem > 0 && this.canLearn() ? Math.min(material.getCount(), (10000 - this.knowledge()) / perItem) : 0;
    }

    public int regularRequired() {
        KnowledgeUpgrades.Requirement knowledgeupgrades$requirement = this.server() ? KnowledgeUpgrades.nextRequirement(this.dragon) : null;
        return this.server() ? (knowledgeupgrades$requirement == null ? 0 : knowledgeupgrades$requirement.bookCount()) : this.data.get(5);
    }

    public int curseRequired() {
        KnowledgeUpgrades.Requirement knowledgeupgrades$requirement = this.server() ? KnowledgeUpgrades.nextRequirement(this.dragon) : null;
        return this.server() ? (knowledgeupgrades$requirement == null ? 0 : knowledgeupgrades$requirement.cursedBookCount()) : this.data.get(6);
    }

    public int regularPlaced() {
        return this.data.get(7);
    }

    public int cursePlaced() {
        return this.data.get(8);
    }

    public boolean upgradeReady() {
        return this.server()
            ? KnowledgeUpgrades.canUpgrade(this.dragon, this.input.getItem(1), this.input.getItem(2), this.input.getItem(3))
            : this.data.get(9) != 0;
    }

    public int nextLevel() {
        return this.data.get(10);
    }

    public int nextAccessorySlots() {
        return this.data.get(11);
    }

    public boolean bookSlotEnabled(int index) {
        if (this.upgradeAvailable()) {
            switch (index) {
                case 1:
                    if (this.regularRequired() > 0) {
                        return true;
                    }
                    break;
                case 2:
                    if (this.regularRequired() > 1) {
                        return true;
                    }
                    break;
                case 3:
                    if (this.curseRequired() > 0) {
                        return true;
                    }
            }
        }

        return false;
    }

    private static boolean matchesSlot(int index, ItemStack stack) {
        return index == 3 ? KnowledgeUpgrades.isCursedBook(stack) : KnowledgeUpgrades.isNonCursedBook(stack);
    }

    public DragonBaseEntity dragon() {
        return this.dragon;
    }

    public boolean stillValid(Player player) {
        return DragonForms.canManage(this.dragon, player);
    }

    private boolean current(Player player) {
        return player.containerMenu == this && this.stillValid(player);
    }

    private void bookFeedback(Player player, ItemStack stack) {
        if (!player.level().isClientSide && !stack.isEmpty()) {
            String s = switch (KnowledgeUpgrades.bookKind(stack)) {
                case UNKNOWN -> "upgrade_book_unknown";
                case UNSUPPORTED -> "upgrade_book_unsupported";
                case INVALID -> "upgrade_book_invalid";
                default -> "upgrade_book_wrong_type";
            };
            player.displayClientMessage(Component.translatable("label.dragonmaid." + s), true);
        }
    }

    public void clicked(int slot, int button, ClickType type, Player player) {
        if (this.current(player)) {
            if (slot >= 1 && slot <= 3) {
                ItemStack itemstack = type == ClickType.SWAP && button >= 0 && button < 9 ? player.getInventory().getItem(button) : this.getCarried();
                if (!itemstack.isEmpty() && this.bookSlotEnabled(slot) && !matchesSlot(slot, itemstack)) {
                    this.bookFeedback(player, itemstack);
                }
            }

            super.clicked(slot, button, type, player);
        }
    }

    public boolean canDragTo(Slot slot) {
        return slot.isActive() && this.stillValid(this.viewer) && super.canDragTo(slot);
    }

    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && this.stillValid(this.viewer) && super.canTakeItemForPickAll(stack, slot);
    }

    public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer serverplayer) || !this.current(player)) {
            return false;
        }

        if (button == 1) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) { serverPlayer.openMenu(this.dragon); }
            return true;
        } else if (button == 2) {
            return AccessoryMenuAction.open(serverplayer, this.dragon);
        } else if (button == 3) {
            return AccessoryMenuAction.transform(serverplayer, this.dragon);
        } else if (button == 4) {
            if (!KnowledgeUpgrades.tryUpgrade(this.dragon, this.input.getItem(1), this.input.getItem(2), this.input.getItem(3))) {
                return false;
            } else {
                this.input.setChanged();
                this.broadcastChanges();
                return true;
            }
        } else if (button == 0 && this.canLearn()) {
            ItemStack itemstack = this.input.getItem(0);
            int i = KnowledgeMaterials.experience(itemstack, this.knowledgeLevel());
            if (i <= 0) {
                return false;
            } else {
                int j = DragonForms.state(this.dragon).dragonmaid$getKnowledge();
                int k = this.completeItemsToLearn(itemstack, i);
                if (k == 0) {
                    return false;
                } else {
                    itemstack.shrink(k);
                    this.input.setChanged();
                    DragonForms.state(this.dragon).dragonmaid$setKnowledge(j + k * i);
                    this.broadcastChanges();
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    public ItemStack quickMoveStack(Player player, int index) {
        if (this.current(player) && index >= 0 && index < this.slots.size()) {
            Slot slot = (Slot)this.slots.get(index);
            if (slot.isActive() && slot.mayPickup(player) && slot.hasItem()) {
                ItemStack itemstack = slot.getItem();
                ItemStack itemstack1 = itemstack.copy();
                if (index < 4) {
                    if (!this.moveItemStackTo(itemstack, 4, 40, true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!KnowledgeMaterials.accepts(itemstack, this.knowledgeLevel()) && !KnowledgeUpgrades.acceptsBook(itemstack)) {
                    if (KnowledgeUpgrades.isBookCandidate(itemstack)
                        || itemstack.getItem() instanceof EnchantedBookItem
                        || StackNbt.hasTag(itemstack) && StackNbt.getTag(itemstack).contains("StoredEnchantments")) {
                        this.bookFeedback(player, itemstack);
                        return ItemStack.EMPTY;
                    }

                    if (index < 31) {
                        if (!this.moveItemStackTo(itemstack, 31, 40, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemstack, 4, 31, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    boolean flag = this.canLearn() && KnowledgeMaterials.accepts(itemstack, this.knowledgeLevel()) && this.moveItemStackTo(itemstack, 0, 1, false);
                    if (!flag && KnowledgeUpgrades.acceptsBook(itemstack) && this.upgradeAvailable()) {
                        flag = this.moveItemStackTo(itemstack, 1, 4, false);
                    }

                    if (!flag) {
                        return ItemStack.EMPTY;
                    }
                }

                if (itemstack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }

                slot.onTake(player, itemstack);
                this.broadcastChanges();
                return itemstack1;
            } else {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.input);
        }
    }
}
