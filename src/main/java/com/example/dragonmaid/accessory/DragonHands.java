package com.example.dragonmaid.accessory;

import com.example.dragonmaid.combat.SpecialWeapons;
import com.example.dragonmaid.compat.StackCodec;
import com.example.dragonmaid.entity.DragonTridentFlight;
import com.example.dragonmaid.compat.net.PacketDistributor;
import com.example.dragonmaid.network.DragonHandsMessage;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.recall.RecallTracking;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 1.2: 龙的双手持物容器（主手只收近战武器，副手收盾/可格挡物）。
 * 上游 Forge 1.20.1 的 ToolActions 在 1.21.1 已改名 ItemAbilities；
 * ItemStack.m_41739_/m_41712_（NBT 自存）已换成 compat.StackCodec（registry-aware codec）。
 */
@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class DragonHands extends SimpleContainer {
    public static final int MAIN = 0;
    public static final int OFF = 1;
    private static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("dragonmaid", "hand_weapons"));
    private static final TagKey<Item> SHIELDS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("dragonmaid", "hand_shields"));
    private final DragonBaseEntity dragon;
    private boolean loading;
    private ItemStack lastMain = ItemStack.EMPTY;
    private ItemStack lastOff = ItemStack.EMPTY;
    /** 1.2.7: the in-flight thrown trident (server) / its mirrored "stowed" view (client). */
    private DragonTridentFlight flight;
    private boolean clientTridentAway;
    private boolean lastTridentAway;

    public DragonHands(DragonBaseEntity dragon) {
        super(2);
        this.dragon = dragon;
    }

    public static DragonHands inventory(DragonBaseEntity dragon) {
        return ((DragonHands.Holder)dragon).dragonmaid$getHands();
    }

    public static ItemStack mainHand(DragonBaseEntity dragon) {
        ItemStack itemstack = inventory(dragon).getItem(0);
        return isMeleeWeapon(itemstack) ? itemstack : ItemStack.EMPTY;
    }

    public static ItemStack offHand(DragonBaseEntity dragon) {
        return inventory(dragon).getItem(1);
    }

    /** 1.2.7: hands are unlocked from knowledge level 2 (mirrors the accessory slots gate). */
    public static boolean unlocked(int knowledgeLevel) {
        return knowledgeLevel >= 2;
    }

    /** Main hand for attack purposes; empty while the trident is away in flight. */
    public static ItemStack availableMainHand(DragonBaseEntity dragon) {
        return inventory(dragon).tridentAway() ? ItemStack.EMPTY : mainHand(dragon);
    }

    public boolean tridentAway() {
        return this.dragon.level().isClientSide ? this.clientTridentAway : this.flight != null;
    }

    /** Client-side mirror of the server's trident state (driven by DragonHandsMessage). */
    public void applyFlightView(boolean away) {
        this.clientTridentAway = away;
    }

    public boolean ownsFlight(DragonTridentFlight candidate) {
        return this.flight == candidate;
    }

    public boolean trackFlight(DragonTridentFlight candidate) {
        if (!this.dragon.level().isClientSide && this.flight == null && !candidate.isRemoved() && candidate.validFor(this.dragon)) {
            this.flight = candidate;
            this.setChanged();
            return this.flight == candidate;
        }

        return false;
    }

    public void tickFlight() {
        if (this.flight != null && !this.flight.validFor(this.dragon)) {
            this.setChanged();
        }
    }

    public void finishFlight(DragonTridentFlight candidate) {
        if (this.flight == candidate) {
            this.clearFlight();
            this.setChanged();
        }
    }

    private void clearFlight() {
        DragonTridentFlight previous = this.flight;
        this.flight = null;
        if (previous != null && !previous.isRemoved()) {
            previous.discard();
        }
    }

    public static boolean accepts(int hand, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            return hand == 1
                ? stack.canPerformAction(ItemAbilities.SHIELD_BLOCK) || stack.is(SHIELDS) || stack.getItem() instanceof ShieldItem
                : hand == 0 && isMeleeWeapon(stack);
        }
    }

    private static boolean isMeleeWeapon(ItemStack stack) {
        if (!stack.isEmpty() && !(stack.getItem() instanceof ProjectileWeaponItem)) {
            UseAnim useanim = stack.getUseAnimation();
            return useanim != UseAnim.BOW && useanim != UseAnim.CROSSBOW
                ? stack.getItem() instanceof SwordItem
                    || stack.getItem() instanceof AxeItem
                    || SpecialWeapons.isTrident(stack)
                    || stack.is(net.minecraft.tags.ItemTags.SWORDS)
                    || stack.is(net.minecraft.tags.ItemTags.AXES)
                    || stack.is(WEAPONS)
                    || stack.canPerformAction(ItemAbilities.SWORD_SWEEP)
                : false;
        } else {
            return false;
        }
    }

    public static void stowOldWeapon(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide) {
            DragonHands dragonhands = inventory(dragon);
            ItemStack itemstack = dragonhands.getItem(0);
            if (!itemstack.isEmpty() && !isMeleeWeapon(itemstack)) {
                ItemStack itemstack1 = DragonStorage.insert(dragon, itemstack);
                if (itemstack1.getCount() != itemstack.getCount()) {
                    dragonhands.setItem(0, itemstack1);
                }
            }
        }
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof DragonBaseEntity entitydragonbase) {
            stowOldWeapon(entitydragonbase);
        }
    }

    public int getMaxStackSize() {
        return 1;
    }

    public void setChanged() {
        if (this.flight != null && !this.flight.validFor(this.dragon)) {
            this.clearFlight();
        }

        super.setChanged();
        // 1.20.1 Forge 的 isAddedToWorld() 在 1.21.1 更名为 isAddedToLevel()
        if (!this.loading && !this.dragon.level().isClientSide && this.dragon.isAddedToLevel()) {
            boolean away = this.tridentAway();
            boolean equipmentChanged = !ItemStack.matches(this.lastMain, this.getItem(0)) || !ItemStack.matches(this.lastOff, this.getItem(1));
            if (equipmentChanged || this.lastTridentAway != away) {
                this.lastMain = this.getItem(0).copy();
                this.lastOff = this.getItem(1).copy();
                this.lastTridentAway = away;
                DragonNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this.dragon), DragonHandsMessage.of(this.dragon));
                if (equipmentChanged) {
                    RecallTracking.changed(this.dragon);
                }
            }
        }
    }

    public CompoundTag saveHands() {
        CompoundTag compoundtag = new CompoundTag();
        if (!this.getItem(0).isEmpty()) {
            compoundtag.put("Main", StackCodec.save(this.getItem(0), new CompoundTag()));
        }

        if (!this.getItem(1).isEmpty()) {
            compoundtag.put("Off", StackCodec.save(this.getItem(1), new CompoundTag()));
        }

        return compoundtag;
    }

    public void loadHands(CompoundTag handItems) {
        this.clearFlight();
        this.loading = true;

        try {
            this.setItem(0, handItems.contains("Main") ? StackCodec.load(handItems.getCompound("Main")) : ItemStack.EMPTY);
            this.setItem(1, handItems.contains("Off") ? StackCodec.load(handItems.getCompound("Off")) : ItemStack.EMPTY);
        } finally {
            this.loading = false;
        }

        if (this.dragon.isAddedToLevel()) {
            this.setChanged();
        } else {
            this.lastMain = this.getItem(0).copy();
            this.lastOff = this.getItem(1).copy();
        }
    }

    public static void dropAll(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide) {
            DragonHands dragonhands = inventory(dragon);
            dragonhands.clearFlight();
            if (!dragonhands.isEmpty()) {
                for (int i = 0; i < dragonhands.getContainerSize(); i++) {
                    ItemStack itemstack = dragonhands.removeItemNoUpdate(i);
                    if (!itemstack.isEmpty()) {
                        dragon.spawnAtLocation(itemstack);
                    }
                }

                dragonhands.setChanged();
            }
        }
    }

    /** 1.2.7: leaving the level must not leave an orphaned trident entity behind. */
    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof DragonBaseEntity dragon) {
            inventory(dragon).clearFlight();
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverplayer
            && event.getTarget() instanceof DragonBaseEntity entitydragonbase
            && !inventory(entitydragonbase).isEmpty()) {
            DragonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverplayer), DragonHandsMessage.of(entitydragonbase));
        }
    }

    public interface Holder {
        DragonHands dragonmaid$getHands();
    }
}
