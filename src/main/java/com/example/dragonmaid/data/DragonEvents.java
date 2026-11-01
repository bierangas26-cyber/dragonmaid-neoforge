package com.example.dragonmaid.data;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.combat.CompanionDefense;
import com.example.dragonmaid.compat.HumanoidForge;
import com.iafenvoy.iceandfire.data.DragonColor;
import com.iafenvoy.iceandfire.event.GriefBreakBlockEvent;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
    modid = "dragonmaid"
)
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
        if (!event.getLevel().isClientSide
            && event.getEntity() instanceof DragonBaseEntity entitydragonbase
            && !entitydragonbase.getPersistentData().contains("DragonMaidLastGrowthDay")) {
            entitydragonbase.getPersistentData().putInt("DragonMaidLastGrowthDay", entitydragonbase.getAgeInDays());
        }
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof DragonBaseEntity entitydragonbase && !entitydragonbase.level().isClientSide) {
            if (entitydragonbase.tickCount % 20 == 0) {
                updateMolting(entitydragonbase);
            }

            if (DragonForms.isHumanoid(entitydragonbase) && entitydragonbase.getCommand() == 1 && entitydragonbase.isOrderedToSit()) {
                HumanoidForge.tryWork(entitydragonbase);
            }

            if (!entitydragonbase.isModelDead() && entitydragonbase.isAlive()) {
                if (entitydragonbase.isTame() && !DragonForms.state(entitydragonbase).dragonmaid$isLifeTracked()) {
                    DragonForms.state(entitydragonbase).dragonmaid$setLifeTracked(true);
                }

                if (entitydragonbase.isTame() && entitydragonbase.getOwner() instanceof Player player && player.level() == entitydragonbase.level()) {
                    CompanionDefense.tick(entitydragonbase);
                    if (!DragonForms.unlocked(entitydragonbase)) {
                        return;
                    }

                    DragonEvents.OwnerDamage dragonevents$ownerdamage = OWNER_DAMAGE.get(player);
                    if (dragonevents$ownerdamage != null
                        && entitydragonbase.level().getGameTime() - dragonevents$ownerdamage.time >= 0L
                        && entitydragonbase.level().getGameTime() - dragonevents$ownerdamage.time <= 100L) {
                        Entity entity = dragonevents$ownerdamage.attacker == null
                            ? null
                            : ((ServerLevel)entitydragonbase.level()).getEntity(dragonevents$ownerdamage.attacker);
                        DragonForms.state(entitydragonbase)
                            .dragonmaid$wakeForOwnerDamage(dragonevents$ownerdamage.time, entity instanceof LivingEntity livingentity ? livingentity : null);
                    }

                    return;
                }

                return;
            }

            return;
        }
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void ownerDamaged(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && event.getNewDamage() > 0.0F) {
            OWNER_DAMAGE.put(
                player,
                new DragonEvents.OwnerDamage(
                    player.level().getGameTime(), event.getSource().getEntity() instanceof LivingEntity livingentity ? livingentity.getUUID() : null
                )
            );
        }
    }

    public static void updateMolting(DragonBaseEntity dragon) {
        int i = dragon.getAgeInDays();
        CompoundTag compoundtag = dragon.getPersistentData();
        int j = compoundtag.contains("DragonMaidLastGrowthDay") ? compoundtag.getInt("DragonMaidLastGrowthDay") : i;
        compoundtag.putInt("DragonMaidLastGrowthDay", i);
        if (dragon.isTame() && dragon.isAlive() && !dragon.isModelDead() && i > 125 && i > j) {
            long k = (long)i - (long)Math.max(125, j);
            int l = (int)Math.min(64L, k * (long)((Integer)DragonMaidConfig.MATURE_SCALES.get()).intValue());
            if (l > 0) {
                // CE 2.0: DragonBaseEntity 不再有 getVariantScale(String)；变体颜色对象持有鳞片物品。
                DragonColor dragoncolor = DragonColor.getById(dragon.getVariant());
                dragon.spawnAtLocation(new ItemStack(dragoncolor.getScaleItem(), l));
            }
        }
    }

    private static record OwnerDamage(long time, UUID attacker) {
    }
}
