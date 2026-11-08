package com.example.dragonmaid.data;

import com.example.dragonmaid.accessory.DragonAccessoryInventory;
import com.example.dragonmaid.accessory.DragonStorageInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface DragonState {
    DragonAccessoryInventory dragonmaid$getAccessories();

    DragonStorageInventory dragonmaid$getStorage();

    int dragonmaid$getKnowledgeLevel();

    void dragonmaid$setKnowledgeLevel(int var1);

    int dragonmaid$getKnowledge();

    void dragonmaid$setKnowledge(int var1);

    /** 1.2: 分次学习时保存每个知识等级已累计的普通书/诅咒书数量（"<level>": {Regular,Curse}）。 */
    CompoundTag dragonmaid$getKnowledgeProgress();

    boolean dragonmaid$isHumanoid();

    void dragonmaid$setHumanoid(boolean var1);

    /** 1.2: Frosty Tail Fin 触发尾巴冰冻效果的状态位。 */
    boolean dragonmaid$hasFrostyTail();

    void dragonmaid$setFrostyTail(boolean var1);

    /** 1.2: Rending Claw 的快速爪击/近战动作编号（战斗动画同步用）。 */
    boolean dragonmaid$isFastClaw();

    int dragonmaid$getMeleeMotion();

    void dragonmaid$setMeleeMotion(int var1);

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
