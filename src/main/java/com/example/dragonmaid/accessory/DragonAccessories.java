package com.example.dragonmaid.accessory;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.data.InjuryRecovery;
import com.example.dragonmaid.item.DragonAccessoryItem;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DragonAccessories {
    private static final CopyOnWriteArrayList<DragonAccessories.AdapterEntry> ADAPTERS = new CopyOnWriteArrayList<>();
    private static final ResourceLocation ARMOR_ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "accessory_armor");
    public static final int REGENERATION_INTERVAL = 50;
    public static final int MAX_SLOTS = 3;

    private DragonAccessories() {
    }

    public static synchronized void registerAdapter(ResourceLocation id, DragonAccessoryAdapter adapter) {
        if (ADAPTERS.stream().anyMatch(entry -> entry.id().equals(id))) {
            throw new IllegalArgumentException("Duplicate dragon accessory adapter: " + id);
        } else {
            ADAPTERS.add(new DragonAccessories.AdapterEntry(id, adapter));
        }
    }

    private static DragonAccessoryBehavior behavior(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        } else {
            ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (resourcelocation != null && "dragonmaid".equals(resourcelocation.getNamespace())) {
                Item entry = stack.getItem();
                if (entry instanceof DragonAccessoryItem) {
                    return (DragonAccessoryItem)entry;
                }
            }

            for (DragonAccessories.AdapterEntry dragonaccessories$adapterentry : ADAPTERS) {
                DragonAccessoryBehavior dragonaccessorybehavior = dragonaccessories$adapterentry.adapter().resolve(stack);
                if (dragonaccessorybehavior != null) {
                    return dragonaccessorybehavior;
                }
            }

            return null;
        }
    }

    public static boolean accepts(ItemStack stack) {
        return behavior(stack) != null;
    }

    public static boolean accepts(ItemStack stack, DragonBaseEntity dragon) {
        return accepts(stack, dragon, -1);
    }

    public static boolean accepts(ItemStack stack, DragonBaseEntity dragon, int replacingSlot) {
        return dragon != null && accepts(stack, dragon, DragonForms.state(dragon).dragonmaid$getAccessories(), unlockedSlots(dragon), replacingSlot);
    }

    public static boolean accepts(ItemStack stack, DragonBaseEntity dragon, Container equipped, int activeSlots, int replacingSlot) {
        DragonAccessoryBehavior dragonaccessorybehavior = behavior(stack);
        if (dragon != null && equipped != null && dragonaccessorybehavior != null && dragonaccessorybehavior.canEquip(stack, dragon)) {
            int i = Math.max(0, Math.min(activeSlots, equipped.getContainerSize()));
            if (replacingSlot >= -1 && replacingSlot < i) {
                Set<ResourceLocation> set = dragonaccessorybehavior.exclusiveEffects(stack, dragon);
                if (set.isEmpty()) {
                    return true;
                } else {
                    for (int j = 0; j < i; j++) {
                        if (j != replacingSlot) {
                            ItemStack itemstack = equipped.getItem(j);
                            DragonAccessoryBehavior dragonaccessorybehavior1 = behavior(itemstack);
                            if (dragonaccessorybehavior1 != null
                                && dragonaccessorybehavior1.canEquip(itemstack, dragon)
                                && dragonaccessorybehavior1.exclusiveEffects(itemstack, dragon)
                                    .stream()
                                    .anyMatch(effect -> !DragonAccessoryBehavior.ARMOR.equals(effect) && set.contains(effect))) {
                                return false;
                            }
                        }
                    }

                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static int knowledgeLevel(DragonBaseEntity dragon) {
        return DragonForms.state(dragon).dragonmaid$getKnowledgeLevel();
    }

    public static int unlockedSlots(DragonBaseEntity dragon) {
        return Math.max(0, Math.min(3, knowledgeLevel(dragon)));
    }

    // 1.2: 背包额外槽位由饰品（effects.storageSlots）提供
    static int bonusStorageSlots(DragonBaseEntity dragon) {
        DragonAccessoryInventory.Effects effects = DragonForms.state(dragon).dragonmaid$getAccessories().effects;
        if (!dragon.level().isClientSide
            && (effects.dirty
                || effects.adapters != ADAPTERS.size()
                || effects.slots != unlockedSlots(dragon)
                || effects.stage != dragon.getDragonStage()
                || effects.eligible != effectsAllowed(dragon))) {
            refresh(dragon);
        }

        return effects.storageSlots;
    }

    public static boolean canManage(DragonBaseEntity dragon, Player player) {
        return dragon != null
            && player != null
            && player.isAlive()
            && !player.isSpectator()
            && !dragon.isRemoved()
            && dragon.level() == player.level()
            && dragon.isTame()
            && dragon.isOwnedBy(player)
            && dragon.distanceToSqr(player) < 64.0
            && (dragon.isAlive() && !dragon.isModelDead() || HumanoidLife.isInjured(dragon) && dragon.getDeathStage() == 0);
    }

    private static boolean effectsAllowed(DragonBaseEntity dragon) {
        return !dragon.isRemoved()
            && dragon.getDeathStage() == 0
            && !DragonForms.state(dragon).dragonmaid$isExecuted()
            && (dragon.isAlive() && !dragon.isModelDead() || HumanoidLife.isInjured(dragon));
    }

    public static void refresh(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide) {
            DragonAccessoryInventory dragonaccessoryinventory = DragonForms.state(dragon).dragonmaid$getAccessories();
            if (dragonaccessoryinventory != null) {
                DragonAccessoryInventory.Effects dragonaccessoryinventory$effects = dragonaccessoryinventory.effects;
                int i = dragon.getDragonStage();
                int j = unlockedSlots(dragon);
                boolean flag = effectsAllowed(dragon);
                if (dragonaccessoryinventory$effects.dirty
                    || dragonaccessoryinventory$effects.stage != i
                    || dragonaccessoryinventory$effects.slots != j
                    || dragonaccessoryinventory$effects.adapters != ADAPTERS.size()
                    || dragonaccessoryinventory$effects.eligible != flag) {
                    double d0 = 0.0;
                    boolean flag1 = false;
                    boolean flag2 = false;
                    double d1 = 0.0;
                    int k = 0;
                    Set<ResourceLocation> set = new HashSet<>();
                    if (flag) {
                        for (int l = 0; l < j; l++) {
                            ItemStack itemstack = dragonaccessoryinventory.getItem(l);
                            DragonAccessoryBehavior dragonaccessorybehavior = behavior(itemstack);
                            if (dragonaccessorybehavior != null && dragonaccessorybehavior.canEquip(itemstack, dragon)) {
                                set.addAll(dragonaccessorybehavior.activeEffects(itemstack, dragon));
                                double d2 = dragonaccessorybehavior.armorBonus(itemstack, dragon);
                                if (Double.isFinite(d2) && d2 > 0.0) {
                                    d0 = Math.min(Double.MAX_VALUE, d0 + d2);
                                }

                                flag1 |= dragonaccessorybehavior.regeneratesHealth(itemstack, dragon);
                                flag2 |= dragonaccessorybehavior.enhancesBreath(itemstack, dragon);
                                double d3 = dragonaccessorybehavior.breathDamageBonus(itemstack, dragon);
                                if (Double.isFinite(d3)) {
                                    d1 = Math.max(d1, d3);
                                }

                                k = Math.max(k, Math.min(27, dragonaccessorybehavior.storageSlots(itemstack, dragon)));
                            }
                        }
                    }

                    if (!flag1 || !dragonaccessoryinventory$effects.regenerates) {
                        dragonaccessoryinventory$effects.healingTicks = 0;
                    }

                    dragonaccessoryinventory$effects.armor = d0;
                    dragonaccessoryinventory$effects.regenerates = flag1;
                    dragonaccessoryinventory$effects.enhancesBreath = flag2;
                    dragonaccessoryinventory$effects.breathDamageBonus = d1;
                    dragonaccessoryinventory$effects.storageSlots = k;
                    dragonaccessoryinventory$effects.activeEffects = Set.copyOf(set);
                    if (dragon instanceof FireDragonEntity && set.contains(DragonAccessoryBehavior.FIRE_FANG) && dragon.hasEffect(MobEffects.POISON)) {
                        dragon.removeEffect(MobEffects.POISON);
                    }

                    DragonForms.state(dragon).dragonmaid$setFrostyTail(set.contains(DragonAccessoryBehavior.ICE_TAIL));
                    dragonaccessoryinventory$effects.stage = i;
                    dragonaccessoryinventory$effects.slots = j;
                    dragonaccessoryinventory$effects.adapters = ADAPTERS.size();
                    dragonaccessoryinventory$effects.eligible = flag;
                    dragonaccessoryinventory$effects.dirty = false;
                    MiniatureForm.equipmentChanged(dragon, set.contains(DragonAccessoryBehavior.MINIATURE));
                }

                AttributeInstance attributeinstance = dragon.getAttribute(Attributes.ARMOR);
                if (attributeinstance != null) {
                    AttributeModifier attributemodifier = attributeinstance.getModifier(ARMOR_ID);
                    if (attributemodifier != null
                        && (dragonaccessoryinventory$effects.armor <= 0.0 || attributemodifier.amount() != dragonaccessoryinventory$effects.armor)) {
                        attributeinstance.removeModifier(ARMOR_ID);
                        attributemodifier = null;
                    }

                    if (dragonaccessoryinventory$effects.armor > 0.0 && attributemodifier == null) {
                        attributeinstance.addTransientModifier(
                            new AttributeModifier(ARMOR_ID, dragonaccessoryinventory$effects.armor, Operation.ADD_VALUE)
                        );
                    }
                }
            }
        }
    }

    // 1.2: 查询饰品激活效果（战斗饰品用）
    public static boolean hasEffect(DragonBaseEntity dragon, ResourceLocation effect) {
        if (dragon != null && effect != null && !dragon.level().isClientSide) {
            DragonAccessoryInventory dragonaccessoryinventory = DragonForms.state(dragon).dragonmaid$getAccessories();
            if (dragonaccessoryinventory == null) {
                return false;
            } else {
                DragonAccessoryInventory.Effects dragonaccessoryinventory$effects = dragonaccessoryinventory.effects;
                if (dragonaccessoryinventory$effects.dirty
                    || dragonaccessoryinventory$effects.stage != dragon.getDragonStage()
                    || dragonaccessoryinventory$effects.slots != unlockedSlots(dragon)
                    || dragonaccessoryinventory$effects.adapters != ADAPTERS.size()
                    || dragonaccessoryinventory$effects.eligible != effectsAllowed(dragon)) {
                    refresh(dragon);
                }

                return dragonaccessoryinventory$effects.activeEffects.contains(effect);
            }
        } else {
            return false;
        }
    }

    public static boolean enhancesBreath(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide && DragonForms.isHumanoid(dragon)) {
            DragonAccessoryInventory.Effects dragonaccessoryinventory$effects = DragonForms.state(dragon).dragonmaid$getAccessories().effects;
            if (dragonaccessoryinventory$effects.dirty
                || dragonaccessoryinventory$effects.stage != dragon.getDragonStage()
                || dragonaccessoryinventory$effects.slots != unlockedSlots(dragon)
                || dragonaccessoryinventory$effects.adapters != ADAPTERS.size()
                || dragonaccessoryinventory$effects.eligible != effectsAllowed(dragon)) {
                refresh(dragon);
            }

            return dragonaccessoryinventory$effects.enhancesBreath;
        } else {
            return false;
        }
    }

    public static float breathDamageMultiplier(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide && DragonForms.isHumanoid(dragon) && dragon.isAlive() && !dragon.isModelDead()) {
            DragonAccessoryInventory.Effects dragonaccessoryinventory$effects = DragonForms.state(dragon).dragonmaid$getAccessories().effects;
            if (dragonaccessoryinventory$effects.dirty
                || dragonaccessoryinventory$effects.stage != dragon.getDragonStage()
                || dragonaccessoryinventory$effects.slots != unlockedSlots(dragon)
                || dragonaccessoryinventory$effects.adapters != ADAPTERS.size()
                || dragonaccessoryinventory$effects.eligible != effectsAllowed(dragon)) {
                refresh(dragon);
            }

            return (float)(1.0 + dragonaccessoryinventory$effects.breathDamageBonus);
        } else {
            return 1.0F;
        }
    }

    public static int breathRecoveryTicks(DragonBaseEntity dragon, int baseTicks) {
        return !(dragon instanceof LightningDragonEntity) && enhancesBreath(dragon) ? (int)(((long)baseTicks * 85L + 99L) / 100L) : baseTicks;
    }

    public static void serverTick(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide) {
            DragonAccessoryInventory dragonaccessoryinventory = DragonForms.state(dragon).dragonmaid$getAccessories();
            DragonAccessoryInventory.Effects dragonaccessoryinventory$effects = dragonaccessoryinventory.effects;
            if (dragonaccessoryinventory$effects.dirty
                || dragonaccessoryinventory$effects.stage != dragon.getDragonStage()
                || dragonaccessoryinventory$effects.slots != unlockedSlots(dragon)
                || dragonaccessoryinventory$effects.adapters != ADAPTERS.size()
                || dragonaccessoryinventory$effects.eligible != effectsAllowed(dragon)) {
                refresh(dragon);
            }

            if (!dragonaccessoryinventory$effects.regenerates || !dragonaccessoryinventory$effects.eligible || dragon.getHealth() >= dragon.getMaxHealth()) {
                dragonaccessoryinventory$effects.healingTicks = 0;
            } else if (++dragonaccessoryinventory$effects.healingTicks >= 50) {
                dragonaccessoryinventory$effects.healingTicks = 0;
                dragon.heal(HumanoidLife.isInjured(dragon) ? 0.1F : 1.0F);
                InjuryRecovery.finish(dragon);
            }
        }
    }

    public static boolean blocksOwnerDamage(DragonBaseEntity dragon, DamageSource source) {
        Entity entity = source.getEntity();
        if (dragon.isTame() && entity != null && dragon.getOwnerUUID() != null && dragon.getOwnerUUID().equals(entity.getUUID())) {
            DragonAccessoryInventory dragonaccessoryinventory = DragonForms.state(dragon).dragonmaid$getAccessories();

            for (int i = 0; i < unlockedSlots(dragon); i++) {
                ItemStack itemstack = dragonaccessoryinventory.getItem(i);
                DragonAccessoryBehavior dragonaccessorybehavior = behavior(itemstack);
                if (dragonaccessorybehavior != null
                    && dragonaccessorybehavior.canEquip(itemstack, dragon)
                    && dragonaccessorybehavior.preventsOwnerDamage(itemstack, dragon)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static void dropAll(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide) {
            DragonHands.dropAll(dragon);
            DragonAccessoryInventory dragonaccessoryinventory = DragonForms.state(dragon).dragonmaid$getAccessories();

            for (int i = 0; i < 5; i++) {
                ItemStack itemstack = dragonaccessoryinventory.removeItemNoUpdate(i);
                if (!itemstack.isEmpty()) {
                    dragon.spawnAtLocation(itemstack);
                }
            }

            dragonaccessoryinventory.setChanged();
            DragonStorageInventory dragonstorageinventory = DragonForms.state(dragon).dragonmaid$getStorage();

            for (int j = 0; j < dragonstorageinventory.getContainerSize(); j++) {
                ItemStack itemstack1 = dragonstorageinventory.removeItemNoUpdate(j);
                if (!itemstack1.isEmpty()) {
                    dragon.spawnAtLocation(itemstack1);
                }
            }

            dragonstorageinventory.setChanged();
        }
    }

    private static record AdapterEntry(ResourceLocation id, DragonAccessoryAdapter adapter) {
    }
}
