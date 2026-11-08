package com.example.dragonmaid.inventory;

import com.example.dragonmaid.accessory.DragonStorage;
import com.example.dragonmaid.accessory.DragonStorageInventory;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.InjuryRecovery;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafItems;
import com.iafenvoy.uranus.object.item.FoodUtils;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * 1.2: 龙的背包（DragonStorageInventory）自动进食。
 * 移植说明：Forge/1.20.1 IAF 的 EntityDragonBase.eatFoodBonus(ItemStack) 在 IAF CE 2.2 已删除，
 * 这里按 TODO 注释掉（进食粒子/声音/饥饿恢复由下面代码自行完成，不影响可玩性）。
 */
public final class DragonBagFeeding {
    private static final int HUNGER_THRESHOLD = 80;
    private static final int COMBAT_INTERVAL = 20;
    private static final String LAST_BITE = "DragonMaidPackLastBite";
    private static final String CALM_DELAY = "DragonMaidPackCalmDelay";
    private static final Map<DragonStorageInventory, DragonBagFeeding.Checked> NO_FOOD = new WeakHashMap<>();

    private DragonBagFeeding() {
    }

    public static void tick(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide
            && dragon.isTame()
            && dragon.getOwnerUUID() != null
            && dragon.isAlive()
            && !dragon.isModelDead()
            && !(dragon.getHealth() <= 0.0F)
            && needsFood(dragon)) {
            int i = DragonStorage.unlockedSlots(dragon);
            if (i != 0) {
                DragonStorageInventory dragonstorageinventory = DragonForms.state(dragon).dragonmaid$getStorage();
                DragonBagFeeding.Checked dragonbagfeeding$checked = NO_FOOD.get(dragonstorageinventory);
                boolean flag = dragon.getHunger() < 100;
                if (dragonbagfeeding$checked == null
                    || dragonbagfeeding$checked.slots() != i
                    || dragonbagfeeding$checked.revision() != dragonstorageinventory.revision()
                    || dragonbagfeeding$checked.canGainHunger() != flag) {
                    long j = dragon.level().getGameTime();
                    if (!coolingDown(dragon, j)) {
                        for (int k = 0; k < i; k++) {
                            ItemStack itemstack = dragonstorageinventory.getItem(k);
                            if (!itemstack.isEmpty()
                                && !itemstack.is(IafItems.CREATIVE_DRAGON_MEAL.get())
                                && !itemstack.is(IafItems.SICKLY_DRAGON_MEAL.get())) {
                                boolean flag1 = itemstack.is(IafItems.DRAGON_MEAL.get());
                                int l = flag1 ? 20 : FoodUtils.getFoodPoints(itemstack, true, dragon.dragonType.piscivore());
                                if (l > 0 && (flag || flag1 || l >= 10)) {
                                    ItemStack itemstack1 = dragonstorageinventory.removeItem(k, 1);
                                    CompoundTag compoundtag = dragon.getPersistentData();
                                    compoundtag.putLong(LAST_BITE, j);
                                    compoundtag.putInt(CALM_DELAY, 80 + dragon.getRandom().nextInt(41));
                                    if (flag1) {
                                        dragon.growDragon(1);
                                    }

                                    dragon.setHunger(dragon.getHunger() + l);
                                    if (flag1) {
                                        float f = Math.min(dragon.getHealth(), (float)((int)(dragon.getMaxHealth() / 2.0F)));
                                        dragon.heal(InjuryRecovery.mealHealing(f));
                                    } else if (l >= 10) {
                                        dragon.setHealth(Math.min(dragon.getMaxHealth(), (float)((int)(dragon.getHealth() + (float)(l / 10)))));
                                    }

                                    feedback(dragon, itemstack1, flag1);
                                    // 1.21.1 说明：IAF 1.20.1 的 eatFoodBonus(ItemStack) 本身就是空实现
                                    // （源码注释 "// FIXME :: ?"），上游 1.2 的调用等价于 no-op；这里无需替代实现。
                                    // 进食音效/粒子由上面的 feedback() 完成。
                                    return;
                                }
                            }
                        }

                        NO_FOOD.put(dragonstorageinventory, new DragonBagFeeding.Checked(dragonstorageinventory.revision(), i, flag));
                    }
                }
            }
        }
    }

    private static boolean coolingDown(DragonBaseEntity dragon, long now) {
        CompoundTag compoundtag = dragon.getPersistentData();
        if (!compoundtag.contains(LAST_BITE)) {
            return false;
        } else {
            long j = now - compoundtag.getLong(LAST_BITE);
            if (j < 0L || j >= (long)compoundtag.getInt(CALM_DELAY)) {
                return false;
            } else if (j < (long)COMBAT_INTERVAL) {
                return true;
            } else {
                LivingEntity livingentity = dragon.getTarget();
                if (livingentity == null || !livingentity.isAlive() || livingentity.level() != dragon.level()) {
                    return true;
                } else {
                    if (livingentity instanceof DragonBaseEntity entitydragonbase && entitydragonbase.isModelDead()) {
                        return true;
                    }

                    return false;
                }
            }
        }
    }

    private static void feedback(DragonBaseEntity dragon, ItemStack bite, boolean dragonMeal) {
        if (!dragon.isSilent()) {
            Vec3 vec3 = dragon.getHeadPosition();
            dragon.level()
                .playSound(
                    null,
                    BlockPos.containing(vec3.x, vec3.y, vec3.z),
                    SoundEvents.GENERIC_EAT,
                    dragon.getSoundSource(),
                    1.0F,
                    dragon.getVoicePitch()
                );
        }

        dragon.spawnItemCrackParticles(bite.getItem());
        if (dragonMeal) {
            dragon.spawnItemCrackParticles(Items.BONE);
            dragon.spawnItemCrackParticles(Items.BONE_MEAL);
        }
    }

    private static boolean needsFood(DragonBaseEntity dragon) {
        return dragon.getHunger() < HUNGER_THRESHOLD || dragon.getHealth() < dragon.getMaxHealth();
    }

    private static record Checked(int revision, int slots, boolean canGainHunger) {
    }
}
