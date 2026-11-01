package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryInventory;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.menu.DragonDetailsMenu;
import com.example.dragonmaid.menu.DragonEquipmentSlots;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.screen.menu.DragonMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.example.dragonmaid.compat.IafProxy;

@Mixin(remap = false, value = {DragonMenu.class})
public abstract class DragonDetailsMenuMixin extends AbstractContainerMenu implements DragonDetailsMenu {
    @Shadow(remap = false)
    @Final
    private Container dragonInventory;
    @Shadow(remap = false)
    @Final
    private DragonBaseEntity dragon;
    @Unique
    private Player dragonmaid$viewer;
    @Unique
    private DragonBaseEntity dragonmaid$clientHint;
    @Unique
    private Container dragonmaid$accessoryInventory;
    @Unique
    private SimpleContainerData dragonmaid$pageData;

    protected DragonDetailsMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(remap = false, method = {"<init>(ILnet/minecraft/world/Container;Lnet/minecraft/world/entity/player/Inventory;Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;)V"},
        at = {@At("TAIL")})
    private void dragonmaid$appendAccessories(int id, Container gear, Inventory inventory, DragonBaseEntity dragon, CallbackInfo ci) {
        this.dragonmaid$viewer = inventory.player;
        if (dragon == null
            && inventory.player.level().isClientSide
            && IafProxy.getReferencedMob() instanceof DragonBaseEntity entitydragonbase
            && inventory.player.level().getEntity(entitydragonbase.getId()) instanceof DragonBaseEntity entitydragonbase1
            && entitydragonbase1.getUUID().equals(entitydragonbase.getUUID())) {
            this.dragonmaid$clientHint = entitydragonbase1;
        }

        this.dragonmaid$accessoryInventory = dragon != null && !inventory.player.level().isClientSide
            ? DragonForms.state(dragon).dragonmaid$getAccessories()
            : new DragonAccessoryInventory();
        this.dragonmaid$pageData = new SimpleContainerData(2);

        for (int i = 0; i < 3; i++) {
            this.addSlot(
                DragonEquipmentSlots.accessory(
                    this.dragonmaid$accessoryInventory,
                    i,
                    153,
                    54 + i * 18,
                    this.dragonmaid$viewDragon(),
                    this::dragonmaid$unlockedSlots,
                    () -> this.stillValid(this.dragonmaid$viewer)
                )
            );
        }

        this.addDataSlots(this.dragonmaid$pageData);
        this.dragonmaid$syncPageData();
        this.dragonmaid$accessoryInventory.startOpen(inventory.player);
    }

    @Unique
    private void dragonmaid$syncPageData() {
        if (this.dragonmaid$pageData != null && this.dragon != null && !this.dragon.level().isClientSide) {
            this.dragonmaid$pageData.set(0, DragonAccessories.knowledgeLevel(this.dragon));
            this.dragonmaid$pageData.set(1, DragonAccessories.unlockedSlots(this.dragon));
        }
    }

    public void broadcastChanges() {
        this.dragonmaid$syncPageData();
        super.broadcastChanges();
    }

    @Override
    public DragonBaseEntity dragonmaid$viewDragon() {
        return this.dragon == null ? this.dragonmaid$clientHint : this.dragon;
    }

    @Override
    public Container dragonmaid$accessories() {
        return this.dragonmaid$accessoryInventory;
    }

    @Override
    public int dragonmaid$knowledgeLevel() {
        return this.dragon != null && !this.dragon.level().isClientSide
            ? DragonAccessories.knowledgeLevel(this.dragon)
            : (this.dragonmaid$pageData == null ? 0 : Math.max(0, this.dragonmaid$pageData.get(0)));
    }

    @Override
    public int dragonmaid$unlockedSlots() {
        return this.dragon != null && !this.dragon.level().isClientSide
            ? DragonAccessories.unlockedSlots(this.dragon)
            : (this.dragonmaid$pageData == null ? 0 : Math.max(0, Math.min(3, this.dragonmaid$pageData.get(1))));
    }

    @Inject(remap = false, method = {"stillValid"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$validateOwner(Player player, CallbackInfoReturnable<Boolean> cir) {
        DragonBaseEntity entitydragonbase = this.dragonmaid$viewDragon();
        cir.setReturnValue(
            DragonAccessories.canManage(entitydragonbase, player)
                && this.dragonInventory.stillValid(player)
                && (player.level().isClientSide || !entitydragonbase.hasInventoryChanged(this.dragonInventory))
        );
    }

    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot < 41 || slot >= 44 || ((Slot)this.slots.get(slot)).isActive()) {
            if (player.containerMenu == this && this.stillValid(player)) {
                super.clicked(slot, button, type, player);
            }
        }
    }

    public boolean canDragTo(Slot slot) {
        return slot.isActive() && this.stillValid(this.dragonmaid$viewer) && super.canDragTo(slot);
    }

    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && this.stillValid(this.dragonmaid$viewer) && super.canTakeItemForPickAll(stack, slot);
    }

    @Inject(remap = false, method = {"quickMoveStack"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$moveEquipment(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(this.dragonmaid$quickMove(player, index));
    }

    @Unique
    private ItemStack dragonmaid$quickMove(Player player, int index) {
        if (player.containerMenu == this && this.stillValid(player) && index >= 0 && index < this.slots.size()) {
            Slot slot = (Slot)this.slots.get(index);
            if (slot.isActive() && slot.mayPickup(player) && slot.hasItem()) {
                ItemStack itemstack = slot.getItem();
                ItemStack itemstack1 = itemstack.copy();
                if (index >= 5 && index < 41) {
                    boolean flag = false;

                    for (int i : new int[]{1, 2, 3, 4, 0}) {
                        if (!((Slot)this.slots.get(i)).hasItem() && ((Slot)this.slots.get(i)).mayPlace(itemstack)) {
                            flag = this.moveItemStackTo(itemstack, i, i + 1, false);
                            break;
                        }
                    }

                    if (!flag
                        && DragonAccessories.accepts(
                            itemstack, this.dragonmaid$viewDragon(), this.dragonmaid$accessoryInventory, this.dragonmaid$unlockedSlots(), -1
                        )) {
                        flag = this.moveItemStackTo(itemstack, 41, 41 + this.dragonmaid$unlockedSlots(), false);
                    }

                    if (!flag) {
                        flag = index < 32 ? this.moveItemStackTo(itemstack, 32, 41, false) : this.moveItemStackTo(itemstack, 5, 32, false);
                    }

                    if (!flag) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }

                if (itemstack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }

                slot.onTake(player, itemstack);
                return itemstack1;
            } else {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Inject(remap = false, method = {"removed"},
        at = {@At("TAIL")})
    private void dragonmaid$closeAccessories(Player player, CallbackInfo ci) {
        if (this.dragonmaid$accessoryInventory != null) {
            this.dragonmaid$accessoryInventory.stopOpen(player);
        }
    }
}
