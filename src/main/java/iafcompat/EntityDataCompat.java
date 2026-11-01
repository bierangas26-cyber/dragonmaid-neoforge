package iafcompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 替代 IAF 2.x（citadel）的 EntityData / EntityDataProvider（CE 2.0 已删除，官方只保留
 * uranus com.iafenvoy.uranus.data.EntityPropertyDelegate 的 int[] 槽，不适合本 mod 的复合数据）。
 *
 * <p>策略：服务端权威数据放 Entity#getPersistentData()（NeoForge，随实体存档、自动下发/同步由
 * 本 mod 的 16 个网络消息负责），冰冻状态用 1.21.1 原生 LivingEntity#setTicksFrozen。</p>
 *
 * <p>旧用法对照：
 * <pre>
 *   EntityDataProvider.getCapability(dragon).map(data -&gt; ...)   -&gt;  EntityDataCompat.tag(dragon)
 *   data.frozenData.setFrozen(enemy, stage * 40)                  -&gt;  EntityDataCompat.setFrozen(enemy, stage * 40)
 * </pre>
 */
public final class EntityDataCompat {
    public static final String ROOT = "dragonmaid";

    private EntityDataCompat() {
    }

    /** 本 mod 在实体 PersistentData 里的根 CompoundTag（不存在则创建）。 */
    public static CompoundTag tag(Entity entity) {
        CompoundTag root = entity.getPersistentData();
        if (!root.contains(ROOT)) {
            root.put(ROOT, new CompoundTag());
        }
        return root.getCompound(ROOT);
    }

    public static boolean hasTag(Entity entity) {
        return entity.getPersistentData().contains(ROOT);
    }

    public static void setTag(Entity entity, CompoundTag tag) {
        entity.getPersistentData().put(ROOT, tag);
    }

    /** IAF EntityData.frozenData.setFrozen(entity, ticks) 的替代（1.21.1 原生冰冻 tick）。 */
    public static void setFrozen(LivingEntity entity, int ticks) {
        entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), ticks));
    }

    public static boolean isFrozen(LivingEntity entity) {
        return entity.isFullyFrozen() || entity.getTicksFrozen() > 0;
    }

    public static void clearFrozen(LivingEntity entity) {
        entity.setTicksFrozen(0);
    }
}
