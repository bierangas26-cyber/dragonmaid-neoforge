package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.MiniatureAimMessage;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.example.dragonmaid.compat.net.PacketDistributor;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class MiniatureShoulder {
   private static final Map<Player, MiniatureShoulder.Nearby> NEARBY = new WeakHashMap<>();

   private MiniatureShoulder() {
   }

   public static LivingEntity target(DragonBaseEntity dragon, double range) {
      if (!(dragon.level() instanceof ServerLevel)) {
         return null;
      } else {
         LivingEntity selected = select(dragon, range);
         int id = selected == null ? -1 : selected.getId();
         MiniatureShoulder.AimHolder holder = (MiniatureShoulder.AimHolder)dragon;
         if (id != holder.dragonmaid$shoulderAim()) {
            holder.dragonmaid$shoulderAim(id);
            DragonNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> dragon), MiniatureAimMessage.of(dragon, id));
         }

         return selected;
      }
   }

   @SubscribeEvent
   public static void track(StartTracking event) {
      if (event.getEntity() instanceof ServerPlayer player
         && event.getTarget() instanceof DragonBaseEntity dragon
         && dragon.getVehicle() instanceof Player
         && ((MiniatureShoulder.AimHolder)dragon).dragonmaid$shoulderAim() >= 0) {
         DragonNetwork.CHANNEL
            .send(PacketDistributor.PLAYER.with(() -> player), MiniatureAimMessage.of(dragon, ((MiniatureShoulder.AimHolder)dragon).dragonmaid$shoulderAim()));
      }
   }

   public static void faceTarget(DragonBaseEntity dragon) {
      int id = ((MiniatureShoulder.AimHolder)dragon).dragonmaid$shoulderAim();
      if (id >= 0
         && MiniatureForm.isMatureMini(dragon)
         && !dragon.isSleeping()
         && dragon.getVehicle() instanceof Player
         && dragon.burningTarget == null
         && dragon.level().getEntity(id) instanceof LivingEntity enemy
         && enemy.isAlive()) {
         Vec3 delta = enemy.getBoundingBox().getCenter().subtract(dragon.getEyePosition());
         float yaw = (float)Math.toDegrees(Mth.atan2(-delta.x, delta.z));
         float pitch = (float)(-Math.toDegrees(Mth.atan2(delta.y, delta.horizontalDistance())));
         dragon.setYRot(Mth.approachDegrees(dragon.yRotO, yaw, 45.0F));
         dragon.setYHeadRot(dragon.getYRot());
         dragon.setYBodyRot(dragon.getYRot());
         dragon.setXRot(Mth.approachDegrees(dragon.xRotO, pitch, 30.0F));
      }
   }

   private static LivingEntity select(DragonBaseEntity dragon, double range) {
      if (dragon.level() instanceof ServerLevel level
         && dragon.getVehicle() instanceof Player owner
         && owner.isAlive()
         && !owner.isSpectator()
         && dragon.isOwnedBy(owner)
         && MiniatureForm.isMatureMini(dragon)
         && !dragon.isNoAi()
         && !dragon.isSleeping()
         && dragon.getCommand() != 1
         && !dragon.isOrderedToSit()
         && !GorgonEntity.isStoneMob(dragon)
         && !DragonForms.state(dragon).dragonmaid$isRecoveryRest()) {
         double reach = range * range;
         LivingEntity threat = owner.getLastHurtByMob();
         if (recent(owner, owner.getLastHurtByMobTimestamp()) && eligible(dragon, threat, reach)) {
            return threat;
         }

         threat = owner.getLastHurtMob();
         if (recent(owner, owner.getLastHurtMobTimestamp()) && eligible(dragon, threat, reach)) {
            return threat;
         }

         threat = CompanionDefense.recentAttacker(dragon);
         if (eligible(dragon, threat, reach)) {
            return threat;
         }

         long now = level.getGameTime();
         MiniatureShoulder.Nearby nearby = NEARBY.get(owner);
         if (nearby == null || now < nearby.sampled() || now - nearby.sampled() >= 10L || range > nearby.range()) {
            List<UUID> attackers = level.getEntitiesOfClass(
                  Mob.class, owner.getBoundingBox().inflate(range + 2.0), mobx -> mobx instanceof Enemy || threatensOwnerOrPet(mobx, owner)
               )
               .stream()
               .<UUID>map(Entity::getUUID)
               .toList();
            nearby = new MiniatureShoulder.Nearby(now, range, attackers);
            NEARBY.put(owner, nearby);
         }

         LivingEntity best = null;
         double bestDistance = reach;

         for (UUID id : nearby.threats()) {
            Entity var17 = level.getEntity(id);
            if (var17 instanceof Mob) {
               Mob mob = (Mob)var17;
               if ((threatensOwnerOrPet(mob, owner) || mob instanceof Enemy && DragonForms.state(dragon).dragonmaid$getKnowledgeLevel() >= 1)
                  && eligible(dragon, mob, bestDistance)) {
                  best = mob;
                  bestDistance = dragon.distanceToSqr(mob);
               }
            }
         }

         return best;
      }

      return null;
   }

   private static boolean recent(LivingEntity actor, int timestamp) {
      int age = actor.tickCount - timestamp;
      return age >= 0 && age <= 100;
   }

   private static boolean threatensOwnerOrPet(Mob mob, Player owner) {
      LivingEntity victim = mob.getTarget();
      return victim == owner || victim != null && victim.isAlive() && CompanionRelations.protectedOwners(victim).contains(owner.getUUID());
   }

   private static boolean eligible(DragonBaseEntity dragon, LivingEntity enemy, double distanceSqr) {
      return enemy != null
         && !enemy.isRemoved()
         && dragon.distanceToSqr(enemy) <= distanceSqr
         && HumanoidTargetGoal.isEnemy(dragon, enemy)
         && dragon.hasLineOfSight(enemy);
   }

   public interface AimHolder {
      int dragonmaid$shoulderAim();

      void dragonmaid$shoulderAim(int var1);
   }

   private static record Nearby(long sampled, double range, List<UUID> threats) {
   }
}
