package com.example.dragonmaid.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public final class TestExecutionBladeItem extends Item {
    public static final float TEST_DAMAGE = 9999.0F;

    public TestExecutionBladeItem() {
        super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level().isClientSide && !player.isSpectator() && target.isAttackable() && !target.skipAttackInteraction(player)) {
            if (target.hurt(player.damageSources().playerAttack(player), 9999.0F) && target instanceof LivingEntity livingentity) {
                player.setLastHurtMob(livingentity);
            }

            player.resetAttackStrengthTicker();
        }

        return true;
    }
}
