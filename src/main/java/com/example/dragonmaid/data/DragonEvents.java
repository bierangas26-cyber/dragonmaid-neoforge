package com.example.dragonmaid.data;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.accessory.DragonStorage;
import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.combat.CompanionDefense;
import com.example.dragonmaid.compat.HumanoidForge;
import com.example.dragonmaid.config.CompanionBalanceConfig;
import com.example.dragonmaid.inventory.DragonBagFeeding;
import com.example.dragonmaid.inventory.DragonBagPickupGoal;
import com.iafenvoy.iceandfire.data.DragonColor;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.event.GriefBreakBlockEvent;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = "dragonmaid")
public final class DragonEvents {
    private static final String LAST_MOLT = "DragonMaidLastGrowthDay";
    private static final Map<Player, DragonEvents.OwnerDamage> OWNER_DAMAGE = new WeakHashMap<>();

    private DragonEvents() {
    }

    @SubscribeEvent
    public static void mount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof Player && DragonForms.isHumanoid(event.getEntityBeingMounted())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    /**
     * IAF 2.1.13 的 GenericGriefEvent（自定义可取消 API 事件）在 CE 2.0 已删除；
     * 对应物是 CE 的 GriefBreakBlockEvent（Event + ICancellableEvent，getGriefer() 给破坏者）。
     */
    public static void grief(GriefBreakBlockEvent event) {
        if (event.getGriefer() instanceof DragonBaseEntity dragon && DragonForms.isHumanoid(dragon)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof DragonBaseEntity dragon) {
            if (!dragon.getPersistentData().contains(LAST_MOLT)) {
                dragon.getPersistentData().putInt(LAST_MOLT, dragon.getAgeInDays());
            }

            // 1.2: 已驯服龙进场时立即套用成长曲线（含迷你化移速修正）
            CompanionGrowth.refresh(dragon);
            MiniatureForm.refreshMovement(dragon);
        }
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof DragonBaseEntity dragon && !dragon.level().isClientSide) {
            // 1.2: 背包拾取 / 背包喂养逻辑挂在龙的 tick 上
            DragonBagPickupGoal.livingTick(dragon);
            DragonBagFeeding.tick(dragon);
            if (dragon.tickCount % 20 == 0) {
                CompanionGrowth.check(dragon);
                updateMolting(dragon);
            }

            // 1.2: 迷你化状态与需求不一致时每 20 tick 重试一次（上游在 DragonMixin，放在这里保证 data 层也能自愈）
            MiniatureForm.retry(dragon);

            if (DragonForms.isHumanoid(dragon) && dragon.getCommand() == 1 && dragon.isOrderedToSit()) {
                HumanoidForge.tryWork(dragon);
            }

            if (!dragon.isModelDead() && dragon.isAlive()) {
                if (dragon.isTame() && !DragonForms.state(dragon).dragonmaid$isLifeTracked()) {
                    DragonForms.state(dragon).dragonmaid$setLifeTracked(true);
                }

                if (dragon.isTame() && dragon.getOwner() instanceof Player owner && owner.level() == dragon.level()) {
                    CompanionDefense.tick(dragon);
                    if (!DragonForms.unlocked(dragon)) {
                        return;
                    }

                    DragonEvents.OwnerDamage damage = OWNER_DAMAGE.get(owner);
                    if (damage != null && dragon.level().getGameTime() - damage.time >= 0L && dragon.level().getGameTime() - damage.time <= 100L) {
                        Entity source = damage.attacker == null ? null : ((ServerLevel)dragon.level()).getEntity(damage.attacker);
                        DragonForms.state(dragon)
                            .dragonmaid$wakeForOwnerDamage(damage.time, source instanceof LivingEntity attacker ? attacker : null);
                    }

                    return;
                }

                return;
            }
        }
    }

    /** 1.2: 记录最近一次攻击主人的生物（供 CompanionDefense 唤醒/反击）。 */
    public static void recordOwnerDamage(Player player, net.minecraft.world.damagesource.DamageSource source) {
        if (!player.level().isClientSide) {
            OWNER_DAMAGE.put(
                player, new DragonEvents.OwnerDamage(player.level().getGameTime(), source.getEntity() instanceof LivingEntity attacker ? attacker.getUUID() : null)
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ownerDamaged(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && event.getNewDamage() > 0.0F) {
            recordOwnerDamage(player, event.getSource());
        }
    }

    /** 1.2: 成熟褪鳞按 companionGrowth/matureShedding 配置随机掉落，背包优先，溢出才掉地上。 */
    public static void updateMolting(DragonBaseEntity dragon) {
        int age = dragon.getAgeInDays();
        CompoundTag data = dragon.getPersistentData();
        int previous = data.contains(LAST_MOLT) ? data.getInt(LAST_MOLT) : age;
        data.putInt(LAST_MOLT, Math.max(previous, age));
        int fullGrowth = CompanionBalanceConfig.values().fullGrowthDays();
        if (CompanionGrowth.owned(dragon) && dragon.isAlive() && !dragon.isModelDead() && age > fullGrowth && age > previous) {
            long days = (long)age - (long)Math.max(fullGrowth, previous);
            int scales = CompanionGrowth.scales(days, DragonMaidConfig.MATURE_SCALES.get(), CompanionBalanceConfig.values().scaleChance(), dragon.getRandom());
            if (scales > 0) {
                // CE 2.0: DragonBaseEntity 不再有 getVariantScale(String)；变体颜色对象持有鳞片物品。
                DragonColor color = DragonColor.getById(dragon.getVariant());
                ItemStack excess = DragonStorage.insert(dragon, new ItemStack(color.getScaleItem(), scales));
                if (!excess.isEmpty()) {
                    dragon.spawnAtLocation(excess);
                }
            }
        }
    }

    private static record OwnerDamage(long time, UUID attacker) {
    }
}
