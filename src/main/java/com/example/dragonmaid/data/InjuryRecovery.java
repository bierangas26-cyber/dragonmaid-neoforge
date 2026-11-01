package com.example.dragonmaid.data;

import com.example.dragonmaid.item.ModItems;
import com.iafenvoy.uranus.object.item.FoodUtils;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InjuryRecovery {
    public static final int DAY_TICKS = 24000;
    public static final float DAILY_HEAL = 10.0F;
    public static final float MINIMUM_MEAL_HEAL = 32.0F;

    private InjuryRecovery() {
    }

    public static boolean fullyHealed(float health, float maximum) {
        return maximum > 0.0F && health >= maximum;
    }

    public static int nextTick(int elapsed) {
        return (Math.max(0, elapsed) % 24000 + 1) % 24000;
    }

    public static float mealHealing(float nativeAmount) {
        return Math.max(32.0F, nativeAmount);
    }

    public static void tick(DragonBaseEntity dragon) {
        DragonState dragonstate = DragonForms.state(dragon);
        if (!HumanoidLife.isInjured(dragon)) {
            dragonstate.dragonmaid$setRecoveryTicks(0);
        } else if (!finish(dragon)) {
            int i = nextTick(dragonstate.dragonmaid$getRecoveryTicks());
            dragonstate.dragonmaid$setRecoveryTicks(i);
            if (i == 0) {
                dragon.heal(10.0F);
                finish(dragon);
            }
        }
    }

    public static boolean finish(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide
            && !dragon.isRemoved()
            && HumanoidLife.isInjured(dragon)
            && dragon.getDeathStage() == 0
            && fullyHealed(dragon.getHealth(), dragon.getMaxHealth())) {
            dragon.setModelDead(false);
            DragonForms.state(dragon).dragonmaid$setRecoveryTicks(0);
            return true;
        } else {
            return false;
        }
    }

    public static boolean nativeInteraction(DragonBaseEntity dragon, Player player, InteractionHand hand) {
        if (dragon.isOwnedBy(player) && !player.isSpectator()) {
            ItemStack itemstack = player.getMainHandItem().isEmpty() ? player.getItemInHand(hand) : player.getMainHandItem();
            Item item = itemstack.getItem();
            return item == IafItems.SICKLY_DRAGON_MEAL.get()
                ? false
                : item instanceof com.iafenvoy.iceandfire.item.SummoningCrystalItem
                    || itemstack.is((Item)ModItems.TRINITY_DRAGON_GEM.get())
                    || item == IafItems.DRAGON_MEAL.get()
                    || item == IafItems.CREATIVE_DRAGON_MEAL.get()
                    || FoodUtils.getFoodPoints(itemstack, true, dragon.dragonType.piscivore()) > 0;
        } else {
            return false;
        }
    }
}
