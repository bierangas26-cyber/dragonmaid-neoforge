package iafcompat;

import com.iafenvoy.iceandfire.data.DragonType;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * IAF CE 2.0 兼容桥（task-3 P2）。
 *
 * <p>把 IAF 2.1.13（com.github.alexthe666）里被删除/改名的成员收敛到一处，供 restored/ 与
 * 155 个被 exclude 的源文件改写时调用，避免每处都写 CE 内部细节。</p>
 *
 * <p>依据：libs/iceandfire-2.0.jar 的 javap + _port_src/decompiled_iaf（590 个 CE 2.0 源文件）核对。
 * 见工程根目录 IAF-CE-MAPPING.md。</p>
 */
public final class IafCompat {
    private IafCompat() {
    }

    /**
     * IAF 2.1.13 的 `DragonType.getIntFromType()` 在 CE 2.0 已删除（DragonType 变成 record，只有 name()/colors()/...）。
     * 这里按实体类还原旧语义：0=fire / 1=ice / 2=lightning，未知返回 -1。
     */
    public static int dragonTypeIndex(DragonBaseEntity dragon) {
        if (dragon instanceof FireDragonEntity) {
            return 0;
        } else if (dragon instanceof IceDragonEntity) {
            return 1;
        } else {
            return dragon instanceof LightningDragonEntity ? 2 : -1;
        }
    }

    /** CE 的召唤水晶是 3 个物品（SUMMONING_CRYSTAL_FIRE/ICE/LIGHTNING），按龙种取。 */
    public static Item summoningCrystal(DragonBaseEntity dragon) {
        DragonType type = dragon.dragonType;
        return type == null ? ItemStack.EMPTY.getItem() : type.getCrystalItem();
    }

    /** 旧 IAF 的 isFood(ItemStack)：CE 由 Animal#isFood 提供（DragonBaseEntity 继承）。 */
    public static boolean isFood(DragonBaseEntity dragon, ItemStack stack) {
        return stack != null && !stack.isEmpty() && dragon.isFood(stack);
    }

    /** 1.20.1 的 isAiDisabled() 在 1.21.1 改名 isNoAi()，且只有 Mob 提供。 */
    public static boolean isAiDisabled(LivingEntity entity) {
        return entity instanceof Mob mob && mob.isNoAi();
    }

    /** 旧 DragonBaseEntity#openInventory(Player)：CE 走 MenuProvider#openMenu。 */
    public static void openDragonMenu(DragonBaseEntity dragon, Player player) {
        if (!player.level().isClientSide) {
            player.openMenu(dragon);
        }
    }

    /** 旧 Entity#fireTicks 字段（1.21.1 移除）的读取。 */
    public static int fireTicks(LivingEntity entity) {
        return entity.getRemainingFireTicks();
    }

    /** 旧 Entity#fireTicks 字段的写入。 */
    public static void setFireTicks(LivingEntity entity, int ticks) {
        entity.setRemainingFireTicks(ticks);
    }

    /** 一次性把旧 IAF 的"喂龙"判定收敛（供 HumanoidCombat/GuardEscort 等复用）。 */
    public static boolean canFeed(DragonBaseEntity dragon, Player player, InteractionHand hand, ItemStack stack) {
        return player != null && !player.isSpectator() && isFood(dragon, stack);
    }
}
