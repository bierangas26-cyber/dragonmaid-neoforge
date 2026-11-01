package com.example.dragonmaid.advancement;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.advancements.AdvancementHolder;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class DragonAdvancements {
    public static final ResourceLocation WELCOME = id("root");
    public static final ResourceLocation CLOSER_COMPANION = id("closer_companion");
    public static final ResourceLocation MAID_FOR_HUNTING = id("maid_for_hunting");
    public static final ResourceLocation TRINITY_OF_DRAGONS = id("trinity_of_dragons");
    private static final String CRITERION = "completed";
    private static final Map<LivingEntity, DragonAdvancements.Hit> RECENT_HITS = new WeakHashMap<>();

    private DragonAdvancements() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("dragonmaid", path);
    }

    public static void award(ServerPlayer player, ResourceLocation id) {
        AdvancementHolder advancement = player.server.getAdvancements().get(id);
        if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            player.getAdvancements().award(advancement, "completed");
        }
    }

    @SubscribeEvent
    public static void login(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverplayer) {
            award(serverplayer, WELCOME);
            HumanoidCompanions.check(serverplayer);
        }
    }

    private static boolean eligible(DragonBaseEntity dragon) {
        return dragon.isTame() && dragon.getOwnerUUID() != null && DragonForms.isHumanoid(dragon);
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void damaged(LivingDamageEvent.Pre event) {
        LivingEntity livingentity = event.getEntity();
        if (livingentity.level() instanceof ServerLevel serverlevel
            && !(event.getNewDamage() <= 0.0F)
            && livingentity.getType().getCategory() == MobCategory.MONSTER) {
            Entity entity = event.getSource().getEntity();
            if (entity instanceof DragonBaseEntity entitydragonbase && eligible(entitydragonbase)) {
                RECENT_HITS.put(livingentity, new DragonAdvancements.Hit(entitydragonbase.getUUID(), entitydragonbase.getOwnerUUID(), serverlevel.getGameTime()));
                return;
            }

            if (entity != null) {
                RECENT_HITS.remove(livingentity);
            }

            return;
        }
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void died(LivingDeathEvent event) {
        LivingEntity livingentity = event.getEntity();
        if (livingentity.level() instanceof ServerLevel serverlevel) {
            DragonAdvancements.Hit dragonadvancements$hit = RECENT_HITS.remove(livingentity);
            if (livingentity.getType().getCategory() == MobCategory.MONSTER) {
                UUID uuid;
                label38: {
                    uuid = null;
                    Entity entity = event.getSource().getEntity();
                    if (entity instanceof DragonBaseEntity entitydragonbase && eligible(entitydragonbase)) {
                        uuid = entitydragonbase.getOwnerUUID();
                        break label38;
                    }

                    if (entity == null
                        && event.getSource().is(DamageTypes.ON_FIRE)
                        && dragonadvancements$hit != null
                        && serverlevel.getGameTime() - dragonadvancements$hit.time >= 0L
                        && serverlevel.getGameTime() - dragonadvancements$hit.time <= 100L
                        && livingentity.getLastHurtByMob() != null
                        && livingentity.getLastHurtByMob().getUUID().equals(dragonadvancements$hit.dragon)) {
                        uuid = dragonadvancements$hit.owner;
                    }
                }

                if (uuid != null) {
                    ServerPlayer serverplayer = serverlevel.getServer().getPlayerList().getPlayer(uuid);
                    if (serverplayer != null) {
                        award(serverplayer, MAID_FOR_HUNTING);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        RECENT_HITS.clear();
    }

    private static record Hit(UUID dragon, UUID owner, long time) {
    }
}
