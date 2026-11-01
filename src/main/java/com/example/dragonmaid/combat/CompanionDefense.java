package com.example.dragonmaid.combat;

import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class CompanionDefense {
    private static final int MEMORY_TICKS = 100;
    private static final int MAX_ATTACKERS = 16;
    private static final Map<Player, CompanionDefense.Alerts> ALERTS = new WeakHashMap<>();
    private static final Map<DragonBaseEntity, Long> NOTIFIED = new WeakHashMap<>();
    private static long revision;

    private CompanionDefense() {
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void companionDamaged(LivingDamageEvent.Pre event) {
        LivingEntity livingentity = event.getEntity();
        if (livingentity.level() instanceof ServerLevel serverlevel
            && !(livingentity instanceof Player)
            && !(event.getNewDamage() <= 0.0F)
            && event.getSource().getEntity() instanceof LivingEntity livingentity1
            && livingentity1 != livingentity) {
            for (UUID uuid : CompanionRelations.protectedOwners(livingentity)) {
                Player player = serverlevel.getPlayerByUUID(uuid);
                if (player != null
                    && player.isAlive()
                    && !player.isSpectator()
                    && livingentity1 != player
                    && !player.isAlliedTo(livingentity1)
                    && !CompanionRelations.protectedOwners(livingentity1).contains(uuid)) {
                    CompanionDefense.Alerts companiondefense$alerts = ALERTS.computeIfAbsent(player, ignored -> new CompanionDefense.Alerts());
                    long i = serverlevel.getGameTime();
                    companiondefense$alerts.attackers.values().removeIf(threatx -> !recent(threatx, serverlevel));
                    CompanionDefense.Threat companiondefense$threat = new CompanionDefense.Threat(
                        livingentity1.getUUID(), livingentity.getUUID(), serverlevel.dimension(), i, ++revision
                    );
                    companiondefense$alerts.attackers.remove(livingentity1.getUUID());
                    companiondefense$alerts.attackers.put(livingentity1.getUUID(), companiondefense$threat);

                    while (companiondefense$alerts.attackers.size() > 16) {
                        companiondefense$alerts.attackers.remove(companiondefense$alerts.attackers.keySet().iterator().next());
                    }

                    companiondefense$alerts.latest = companiondefense$threat;
                }
            }

            return;
        }
    }

    private static boolean recent(CompanionDefense.Threat threat, Level level) {
        long i = level.getGameTime() - threat.time;
        return threat.dimension.equals(level.dimension()) && i >= 0L && i <= 100L;
    }

    private static CompanionDefense.Alerts alerts(DragonBaseEntity dragon) {
        if (!(dragon.level() instanceof ServerLevel)) {
            return null;
        } else {
            if (dragon.getOwner() instanceof Player player && player.isAlive() && !player.isSpectator() && player.level() == dragon.level()) {
                return ALERTS.get(player);
            }

            return null;
        }
    }

    private static boolean valid(DragonBaseEntity dragon, CompanionDefense.Threat threat) {
        if (threat != null && recent(threat, dragon.level()) && dragon.level() instanceof ServerLevel serverlevel) {
            Entity entity = serverlevel.getEntity(threat.victim);
            if (entity == null) {
                return true;
            } else {
                if (entity instanceof LivingEntity livingentity && CompanionRelations.protectedOwners(livingentity).contains(dragon.getOwnerUUID())) {
                    return true;
                }

                return false;
            }
        } else {
            return false;
        }
    }

    public static long revision(DragonBaseEntity dragon, LivingEntity attacker) {
        CompanionDefense.Alerts companiondefense$alerts = alerts(dragon);
        CompanionDefense.Threat companiondefense$threat = companiondefense$alerts == null ? null : companiondefense$alerts.attackers.get(attacker.getUUID());
        return valid(dragon, companiondefense$threat) ? companiondefense$threat.revision : -1L;
    }

    public static boolean isThreat(DragonBaseEntity dragon, LivingEntity attacker) {
        if (dragon.getOwnerUUID() == null) {
            return false;
        } else {
            if (attacker instanceof Mob mob) {
                LivingEntity livingentity = mob.getTarget();
                if (livingentity != null
                    && livingentity.isAlive()
                    && livingentity.level() == dragon.level()
                    && CompanionRelations.protectedOwners(livingentity).contains(dragon.getOwnerUUID())) {
                    return true;
                }
            }

            return revision(dragon, attacker) >= 0L;
        }
    }

    public static LivingEntity recentAttacker(DragonBaseEntity dragon) {
        CompanionDefense.Alerts companiondefense$alerts = alerts(dragon);
        if (companiondefense$alerts != null && valid(dragon, companiondefense$alerts.latest)) {
            return ((ServerLevel)dragon.level()).getEntity(companiondefense$alerts.latest.attacker) instanceof LivingEntity livingentity ? livingentity : null;
        } else {
            return null;
        }
    }

    public static boolean canAssist(DragonBaseEntity dragon, LivingEntity attacker) {
        return attacker != null
            && !attacker.isRemoved()
            && HumanoidTargetGoal.isEnemy(dragon, attacker)
            && GuardEscort.reachableTarget(dragon, attacker)
            && (GuardEscort.owner(dragon) != null || dragon.distanceToSqr(attacker) <= 576.0);
    }

    public static boolean canRespond(DragonBaseEntity dragon) {
        return dragon.level() instanceof ServerLevel
            && dragon.isTame()
            && dragon.isAlive()
            && !dragon.isRemoved()
            && !dragon.isModelDead()
            && !dragon.isNoAi()
            && !dragon.isPassenger()
            && dragon.getControllingPassenger() == null
            && !GorgonEntity.isStoneMob(dragon)
            && !DragonForms.state(dragon).dragonmaid$isRecoveryRest()
            && dragon.getOwner() instanceof Player player
            && player.isAlive()
            && !player.isSpectator()
            && player.level() == dragon.level();
    }

    public static void tick(DragonBaseEntity dragon) {
        if (canRespond(dragon)) {
            CompanionDefense.Alerts companiondefense$alerts = alerts(dragon);
            if (companiondefense$alerts != null
                && companiondefense$alerts.latest != null
                && NOTIFIED.getOrDefault(dragon, -1L) != companiondefense$alerts.latest.revision) {
                if (!valid(dragon, companiondefense$alerts.latest)) {
                    NOTIFIED.put(dragon, companiondefense$alerts.latest.revision);
                } else {
                    LivingEntity livingentity = recentAttacker(dragon);
                    if (canAssist(dragon, livingentity)) {
                        NOTIFIED.put(dragon, companiondefense$alerts.latest.revision);
                        DragonForms.state(dragon).dragonmaid$wakeForCompanionDamage(livingentity);
                    }
                }
            }
        }
    }

    private static final class Alerts {
        final Map<UUID, CompanionDefense.Threat> attackers = new LinkedHashMap<>();
        CompanionDefense.Threat latest;

        private Alerts() {
        }
    }

    private static record Threat(UUID attacker, UUID victim, ResourceKey<Level> dimension, long time, long revision) {
    }
}
