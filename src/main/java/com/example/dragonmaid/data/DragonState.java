package com.example.dragonmaid.data;

import com.example.dragonmaid.accessory.DragonAccessoryInventory;
import com.example.dragonmaid.accessory.DragonStorageInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface DragonState {
    DragonAccessoryInventory dragonmaid$getAccessories();

    DragonStorageInventory dragonmaid$getStorage();

    int dragonmaid$getKnowledgeLevel();

    void dragonmaid$setKnowledgeLevel(int var1);

    int dragonmaid$getKnowledge();

    void dragonmaid$setKnowledge(int var1);

    boolean dragonmaid$isHumanoid();

    void dragonmaid$setHumanoid(boolean var1);

    boolean dragonmaid$isIdleLooking();

    void dragonmaid$setIdleLooking(boolean var1);

    int dragonmaid$getAction();

    void dragonmaid$setAction(int var1);

    boolean dragonmaid$isExecuted();

    void dragonmaid$setExecuted(boolean var1);

    boolean dragonmaid$isLifeTracked();

    void dragonmaid$setLifeTracked(boolean var1);

    boolean dragonmaid$isRecoveryRest();

    boolean dragonmaid$isCombatInterruptedRest();

    void dragonmaid$setCombatInterruptedRest(boolean var1);

    int dragonmaid$getNativeBreathTicks();

    void dragonmaid$setNativeBreathTicks(int var1);

    int dragonmaid$getRecoveryTicks();

    void dragonmaid$setRecoveryTicks(int var1);

    void dragonmaid$wakeForOwnerDamage(long var1, LivingEntity var3);

    void dragonmaid$wakeForCompanionDamage(LivingEntity var1);

    void dragonmaid$refreshForm();

    boolean dragonmaid$canBreakBlock(BlockPos var1, BlockState var2);
}
