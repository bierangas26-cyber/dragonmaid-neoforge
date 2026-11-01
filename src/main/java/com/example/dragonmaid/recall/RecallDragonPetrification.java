package com.example.dragonmaid.recall;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.item.ModItems;
import com.example.dragonmaid.petrification.BasicDragonPetrification;
import com.example.dragonmaid.petrification.DragonDepetrifiedEvent;
import com.example.dragonmaid.petrification.PetrificationLedger;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import com.iafenvoy.iceandfire.item.component.StoneStatusComponent;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.world.DragonPosWorldData;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityEvent.EnteringSection;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import com.example.dragonmaid.compat.StackNbt;

public final class RecallDragonPetrification {
    public static final String MARKER = "DragonMaidPetrification";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<StoneStatueEntity, RecallDragonPetrification.Pending> PENDING = new WeakHashMap<>();
    private static final ThreadLocal<DragonBaseEntity> RESTORING = new ThreadLocal<>();
    private static final Map<StoneStatueEntity, ServerLevel> JOINED = new WeakHashMap<>();

    private RecallDragonPetrification() {
    }

    public static boolean isRestoring(DragonBaseEntity dragon) {
        return RESTORING.get() == dragon;
    }

    public static boolean protects(Entity entity) {
        if (entity instanceof DragonBaseEntity entitydragonbase && isEligibleDragon(entitydragonbase)) {
            return true;
        }

        return false;
    }

    public static boolean isEligibleDragon(DragonBaseEntity dragon) {
        boolean flag = dragon.isTame() && dragon.getOwnerUUID() != null;
        boolean flag1 = !dragon.isTame() && dragon.getOwnerUUID() == null && DragonForms.isHumanoid(dragon);
        return (flag || flag1) && !DragonForms.state(dragon).dragonmaid$isExecuted() && (!dragon.isModelDead() || HumanoidLife.isInjured(dragon));
    }

    public static StoneStatueEntity buildStatue(LivingEntity target) {
        StoneStatueEntity entitystonestatue = StoneStatueEntity.buildStatueEntity(target);
        if (protects(target) && target.level() instanceof ServerLevel) {
            DragonBaseEntity entitydragonbase = (DragonBaseEntity)target;
            UUID uuid = UUID.randomUUID();
            CompoundTag compoundtag = null;

            try {
                compoundtag = entitydragonbase.saveWithoutId(new CompoundTag());
                compoundtag.remove("Passengers");
                CompoundTag compoundtag1 = compoundtag.copy();
                CompoundTag compoundtag2 = new CompoundTag();
                compoundtag2.putUUID("Dragon", entitydragonbase.getUUID());
                compoundtag2.putUUID("Token", uuid);
                compoundtag2.putInt("Version", 1);
                compoundtag1.put("DragonMaidPetrification", compoundtag2);
                entitystonestatue.setTrappedTag(compoundtag1);
            } catch (RuntimeException runtimeexception) {
                LOGGER.error("Could not preserve dragon {} for petrification; keeping the dragon", entitydragonbase.getUUID(), runtimeexception);
            }

            PENDING.put(entitystonestatue, new RecallDragonPetrification.Pending(entitydragonbase, uuid, compoundtag));
            return entitystonestatue;
        } else {
            return entitystonestatue;
        }
    }

    public static boolean addStatue(Level world, Entity entity) {
        RecallDragonPetrification.Pending recalldragonpetrification$pending = entity instanceof StoneStatueEntity entitystonestatue
            ? PENDING.remove(entitystonestatue)
            : null;
        if (recalldragonpetrification$pending == null) {
            return world.addFreshEntity(entity);
        } else {
            if (world instanceof ServerLevel serverlevel
                && recalldragonpetrification$pending.snapshot() != null
                && !recalldragonpetrification$pending.dragon().isRemoved()) {
                DragonBaseEntity entitydragonbase = recalldragonpetrification$pending.dragon();
                PetrificationLedger petrificationledger = PetrificationLedger.get(serverlevel);
                if (!petrificationledger.active(entitydragonbase.getUUID()) && petrificationledger.reserve(entitydragonbase.getUUID())) {
                    PetrificationLedger.Entry petrificationledger$entry = petrificationledger.entry(entitydragonbase.getUUID());
                    RecallLedger recallledger = RecallLedger.get(serverlevel);
                    boolean flag = entitydragonbase.getOwnerUUID() != null;
                    boolean flag1 = false;
                    RecallLedger.Transfer recallledger$transfer = null;
                    boolean flag2 = false;

                    boolean suppressed;
                    try {
                        if (flag) {
                            RecallLedger.Entry recallledger$entry = recallledger.observe(entitydragonbase);
                            if (recallledger$entry == null
                                || recallledger$entry.state() != RecallLedger.State.WORLD
                                || !RecallTracking.matches(entitydragonbase, recallledger$entry)) {
                                return false;
                            }

                            flag1 = recallledger.reserve(entitydragonbase.getUUID());
                            if (!flag1) {
                                return false;
                            }

                            CompoundTag compoundtag = entity.saveWithoutId(new CompoundTag());
                            compoundtag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
                            recallledger$transfer = recallledger.prepareTransfer(
                                entitydragonbase.getUUID(),
                                RecallLedger.State.STONE,
                                serverlevel.dimension().location().toString(),
                                entity.blockPosition(),
                                entity.getUUID(),
                                compoundtag
                            );
                            stampRecallStatue((StoneStatueEntity)entity, recallledger$transfer.after());
                        }

                        petrificationledger.capture(
                            new PetrificationLedger.Entry(
                                entitydragonbase.getUUID(),
                                recalldragonpetrification$pending.token(),
                                BuiltInRegistries.ENTITY_TYPE.getKey(entitydragonbase.getType()).toString(),
                                recalldragonpetrification$pending.snapshot().copy(),
                                true
                            )
                        );
                        if (flag) {
                            petrificationledger.flush();
                        }

                        if (serverlevel.addFreshEntity(entity) && !entity.isRemoved()) {
                            if (recallledger$transfer != null) {
                                RecallPersistence.checkpoint(serverlevel, recallledger$transfer.after());
                            } else {
                                RecallPersistence.saveEntityChunk(serverlevel, entity.chunkPosition());
                                petrificationledger.flush();
                            }

                            entitydragonbase.ejectPassengers();
                            entitydragonbase.stopRiding();
                            entitydragonbase.remove(RemovalReason.DISCARDED);
                            if (!entitydragonbase.isRemoved()) {
                                throw new IllegalStateException("Petrified dragon was not removed");
                            }

                            if (recallledger$transfer != null) {
                                recallledger.commitTransfer(recallledger$transfer);
                            }

                            flag2 = true;
                            DragonPosWorldData.get(serverlevel).removeDragon(entitydragonbase.getUUID());
                            return true;
                        }

                        throw new IllegalStateException("Statue rejected");
                    } catch (RuntimeException runtimeexception4) {
                        if (flag2 || entitydragonbase.isRemoved()) {
                            if (recallledger$transfer != null && recallledger.pending(entitydragonbase.getUUID()) != null) {
                                try {
                                    recallledger.commitTransfer(recallledger$transfer);
                                } catch (RuntimeException runtimeexception3) {
                                    runtimeexception4.addSuppressed(runtimeexception3);
                                }
                            }

                            LOGGER.warn("Dragon {} was transferred, but petrification cleanup failed", entitydragonbase.getUUID(), runtimeexception4);
                            return true;
                        }

                        if (!entity.isRemoved()) {
                            try {
                                entity.discard();
                            } catch (RuntimeException runtimeexception2) {
                                runtimeexception4.addSuppressed(runtimeexception2);
                            }
                        }

                        if (entity.isRemoved()) {
                            petrificationledger.rollbackCapture(entitydragonbase.getUUID(), petrificationledger$entry);

                            try {
                                petrificationledger.flush();
                            } catch (RuntimeException runtimeexception1) {
                                runtimeexception4.addSuppressed(runtimeexception1);
                            }

                            if (recallledger$transfer != null && recallledger.pending(entitydragonbase.getUUID()) != null) {
                                try {
                                    recallledger.rollbackTransfer(recallledger$transfer);
                                } catch (RuntimeException runtimeexception) {
                                    runtimeexception4.addSuppressed(runtimeexception);
                                }
                            }

                            LOGGER.error("Could not transfer dragon {}; preserving the original", entitydragonbase.getUUID(), runtimeexception4);
                            return false;
                        }

                        LOGGER.error("Could not remove failed petrification target; preserving pending transfer", runtimeexception4);
                        suppressed = false;
                    } finally {
                        petrificationledger.release(entitydragonbase.getUUID());
                        if (flag1) {
                            recallledger.release(entitydragonbase.getUUID());
                        }
                    }

                    return suppressed;
                }

                return false;
            }

            return false;
        }
    }

    public static void preventDuplicateDragon(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel serverlevel) {
            if (event.getEntity() instanceof StoneStatueEntity entitystonestatue) {
                CompoundTag compoundtag = entitystonestatue.getTrappedTag().getCompound("DragonMaidPetrification");
                if (compoundtag.hasUUID("Dragon") && compoundtag.hasUUID("Token")) {
                    UUID uuid = compoundtag.getUUID("Dragon");
                    RecallLedger recallledger = RecallLedger.get(serverlevel);
                    RecallLedger.Entry recallledger$entry1 = recallledger.entry(uuid);
                    if (recallledger$entry1 != null) {
                        if (recallledger$entry1.state() != RecallLedger.State.STONE
                            || !entitystonestatue.getUUID().equals(recallledger$entry1.holder())
                            || !RecallTracking.matches(entitystonestatue, recallledger$entry1)) {
                            event.setCanceled(true);
                            return;
                        }

                        repairPreparedPetrification(entitystonestatue, recallledger$entry1);
                    }

                    PetrificationLedger.Entry petrificationledger$entry = PetrificationLedger.get(serverlevel).entry(uuid);
                    if (petrificationledger$entry != null
                        && petrificationledger$entry.active()
                        && petrificationledger$entry.token().equals(compoundtag.getUUID("Token"))) {
                        if (recallledger$entry1 == null && petrificationledger$entry.snapshot().hasUUID("Owner")) {
                            recallledger$entry1 = recallledger.observeStone(
                                uuid,
                                petrificationledger$entry.snapshot().getUUID("Owner"),
                                petrificationledger$entry.type(),
                                petrificationledger$entry.snapshot(),
                                entitystonestatue
                            );
                            stampRecallStatue(entitystonestatue, recallledger$entry1);
                        }

                        JOINED.put(entitystonestatue, serverlevel);
                    } else {
                        event.setCanceled(true);
                    }
                }
            } else {
                if (event.getEntity() instanceof DragonBaseEntity entitydragonbase && RESTORING.get() != entitydragonbase) {
                    PetrificationLedger petrificationledger = PetrificationLedger.get(serverlevel);
                    if (petrificationledger.active(entitydragonbase.getUUID())) {
                        RecallLedger.Entry recallledger$entry = RecallLedger.get(serverlevel).entry(entitydragonbase.getUUID());
                        if (recallledger$entry != null
                            && recallledger$entry.state() == RecallLedger.State.WORLD
                            && RecallTracking.matches(entitydragonbase, recallledger$entry)) {
                            petrificationledger.complete(petrificationledger.entry(entitydragonbase.getUUID()));
                            petrificationledger.flush();
                        } else {
                            event.setCanceled(true);
                        }
                    }

                    return;
                }
            }
        }
    }

    public static InteractionResult restore(Player player, InteractionHand hand, StoneStatueEntity statue) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            if (player.level() instanceof ServerLevel serverlevel
                && statue.level() == serverlevel
                && !statue.isRemoved()
                && !player.isSpectator()
                && player.getItemInHand(hand).is((Item)ModItems.PETRIFICATION_REMEDY.get())
                && inReach(player, statue)) {
                CompoundTag compoundtag1 = statue.getTrappedTag().getCompound("DragonMaidPetrification");
                if (!statue.getTrappedTag().contains("DragonMaidPetrification", 10)) {
                    return fail(
                        player,
                        statue.getTrappedEntityType() != IafEntities.FIRE_DRAGON.get()
                                && statue.getTrappedEntityType() != IafEntities.ICE_DRAGON.get()
                                && statue.getTrappedEntityType() != IafEntities.LIGHTNING_DRAGON.get()
                            ? "unsupported"
                            : "legacy"
                    );
                }

                if (compoundtag1.hasUUID("Dragon") && compoundtag1.hasUUID("Token")) {
                    UUID uuid = compoundtag1.getUUID("Dragon");
                    PetrificationLedger petrificationledger = PetrificationLedger.get(serverlevel);
                    PetrificationLedger.Entry petrificationledger$entry = petrificationledger.entry(uuid);
                    if (petrificationledger$entry != null
                        && petrificationledger$entry.active()
                        && petrificationledger$entry.token().equals(compoundtag1.getUUID("Token"))) {
                        RecallLedger recallledger = RecallLedger.get(serverlevel);
                        RecallLedger.Entry recallledger$entry = recallledger.entry(uuid);
                        if (recallledger$entry == null && petrificationledger$entry.snapshot().hasUUID("Owner")) {
                            recallledger$entry = recallledger.observeStone(
                                uuid,
                                petrificationledger$entry.snapshot().getUUID("Owner"),
                                petrificationledger$entry.type(),
                                petrificationledger$entry.snapshot(),
                                statue
                            );
                            stampRecallStatue(statue, recallledger$entry);
                        }

                        if (recallledger$entry != null && !validRecallStatue(statue, recallledger$entry)) {
                            return fail(player, "duplicate");
                        }

                        if (!petrificationledger.reserve(uuid)) {
                            return fail(player, "blocked");
                        }

                        DragonBaseEntity entitydragonbase = null;
                        boolean flag = false;
                        boolean flag1 = false;
                        boolean flag2 = false;
                        RecallLedger.Transfer recallledger$transfer = null;

                        InteractionResult targetGone;
                        try {
                            if (recallledger$entry != null) {
                                flag2 = recallledger.reserve(uuid);
                                if (!flag2) {
                                    return fail(player, "blocked");
                                }
                            }

                            for (ServerLevel serverlevel1 : serverlevel.getServer().getAllLevels()) {
                                if (serverlevel1.getEntity(uuid) != null) {
                                    return fail(player, "blocked");
                                }
                            }

                            EntityType<?> entitytype = (EntityType<?>)EntityType.byString(petrificationledger$entry.type()).orElse(null);
                            if (!((entitytype == null ? null : entitytype.create(serverlevel)) instanceof DragonBaseEntity entitydragonbase1)) {
                                return fail(player, "corrupt");
                            }

                            entitydragonbase = entitydragonbase1;
                            entitydragonbase1.load(petrificationledger$entry.snapshot().copy());
                            if (statue.getTrappedTag().contains("Hunger", 3)) {
                                entitydragonbase1.setHunger(statue.getTrappedTag().getInt("Hunger"));
                            }

                            if (!uuid.equals(entitydragonbase1.getUUID()) || !protects(entitydragonbase1)) {
                                return fail(player, "corrupt");
                            }

                            entitydragonbase1.setHealth(Math.min(entitydragonbase1.getMaxHealth(), petrificationledger$entry.snapshot().getFloat("Health")));
                            entitydragonbase1.moveTo(statue.getX(), statue.getY(), statue.getZ(), statue.getYRot(), 0.0F);
                            entitydragonbase1.yBodyRot = entitydragonbase1.yBodyRotO = entitydragonbase1.yHeadRot = entitydragonbase1.yHeadRotO = statue.getYRot();
                            entitydragonbase1.setDeltaMovement(Vec3.ZERO);
                            entitydragonbase1.fallDistance = 0.0F;
                            entitydragonbase1.refreshDimensions();
                            if (!clearSpace(serverlevel, entitydragonbase1, entitydragonbase1.getBoundingBox())) {
                                return fail(player, "blocked");
                            }

                            if (recallledger$entry != null) {
                                CompoundTag compoundtag = entitydragonbase1.saveWithoutId(new CompoundTag());
                                compoundtag.putString("id", petrificationledger$entry.type());
                                recallledger$transfer = recallledger.prepareTransfer(
                                    uuid,
                                    RecallLedger.State.WORLD,
                                    serverlevel.dimension().location().toString(),
                                    entitydragonbase1.blockPosition(),
                                    uuid,
                                    compoundtag
                                );
                                RecallTracking.stamp(entitydragonbase1, recallledger$transfer.after());
                            }

                            RESTORING.set(entitydragonbase1);

                            boolean flag4;
                            try {
                                flag1 = true;
                                flag4 = serverlevel.addFreshEntity(entitydragonbase1);
                            } finally {
                                RESTORING.remove();
                            }

                            if (!flag4 || entitydragonbase1.isRemoved()) {
                                return fail(player, "blocked");
                            }

                            if (recallledger$transfer != null) {
                                recallledger.commitTransfer(recallledger$transfer);
                                flag = true;
                            } else {
                                RecallPersistence.saveEntityChunk(serverlevel, entitydragonbase1.chunkPosition());
                            }

                            petrificationledger.complete(petrificationledger$entry);
                            petrificationledger.flush();
                            flag = true;
                            if (!player.getAbilities().instabuild) {
                                player.getItemInHand(hand).shrink(1);
                            }

                            statue.discard();
                            notifyRestored(player, entitydragonbase1);
                            DragonPosWorldData.get(serverlevel).addDragon(uuid, entitydragonbase1.blockPosition());
                            serverlevel.sendParticles(
                                ParticleTypes.HAPPY_VILLAGER,
                                entitydragonbase1.getX(),
                                entitydragonbase1.getY() + 0.7,
                                entitydragonbase1.getZ(),
                                12,
                                0.4,
                                0.5,
                                0.4,
                                0.0
                            );
                            serverlevel.playSound(null, entitydragonbase1.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
                            player.displayClientMessage(Component.translatable("message.dragonmaid.petrification.success"), true);
                            targetGone = InteractionResult.CONSUME;
                        } catch (RuntimeException runtimeexception3) {
                            if (!flag) {
                                LOGGER.error("Could not restore petrified dragon {}", uuid, runtimeexception3);
                                return fail(player, "corrupt");
                            }

                            if (!statue.isRemoved()) {
                                statue.discard();
                            }

                            LOGGER.warn("Dragon {} restored; a post-restoration notification or cleanup failed", uuid, runtimeexception3);
                            return InteractionResult.CONSUME;
                        } finally {
                            RESTORING.remove();
                            boolean flag3 = entitydragonbase == null || !flag1 || entitydragonbase.isRemoved();
                            if (!flag && !flag3) {
                                try {
                                    entitydragonbase.discard();
                                } catch (RuntimeException runtimeexception2) {
                                    LOGGER.error("Could not remove failed stone restoration target", runtimeexception2);
                                }

                                flag3 = entitydragonbase.isRemoved();
                            }

                            if (!flag) {
                                if (!petrificationledger.active(uuid)) {
                                    petrificationledger.capture(petrificationledger$entry);

                                    try {
                                        petrificationledger.flush();
                                    } catch (RuntimeException runtimeexception1) {
                                        LOGGER.error("Could not retain stone identity", runtimeexception1);
                                    }
                                }

                                if (flag3 && recallledger$transfer != null && recallledger.pending(uuid) != null) {
                                    try {
                                        recallledger.rollbackTransfer(recallledger$transfer);
                                    } catch (RuntimeException runtimeexception) {
                                        LOGGER.error("Could not roll back stone restoration", runtimeexception);
                                    }
                                }
                            }

                            if (entitydragonbase != null && !flag1) {
                                entitydragonbase.removeParts();
                            }

                            petrificationledger.release(uuid);
                            if (flag2) {
                                recallledger.release(uuid);
                            }
                        }

                        return targetGone;
                    }

                    return fail(player, "duplicate");
                }

                return fail(player, "corrupt");
            }

            return InteractionResult.FAIL;
        }
    }

    public static InteractionResult placeStatue(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        } else if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!(context.getLevel() instanceof ServerLevel serverlevel)) {
            return InteractionResult.FAIL;
        } else {
            ItemStack itemstack = context.getItemInHand();
            if (!itemstack.isEmpty() && player.getItemInHand(context.getHand()) == itemstack) {
                // 1.21.1: the statue payload is an IafDataComponents.STONE_STATUS component now.
                StoneStatusComponent stoneStatus = itemstack.get(IafDataComponents.STONE_STATUS.get());
                CompoundTag compoundtag = stoneStatus == null ? new CompoundTag() : stoneStatus.nbt().copy();
                CompoundTag compoundtag1 = compoundtag.getCompound("StatueEntityTag").getCompound("DragonMaidPetrification");
                if (compoundtag1.hasUUID("Dragon") && compoundtag1.hasUUID("Token")) {
                    PetrificationLedger.Entry petrificationledger$entry = PetrificationLedger.get(serverlevel).entry(compoundtag1.getUUID("Dragon"));
                    if (petrificationledger$entry != null
                        && petrificationledger$entry.active()
                        && petrificationledger$entry.token().equals(compoundtag1.getUUID("Token"))) {
                        RecallLedger recallledger = RecallLedger.get(serverlevel);
                        RecallLedger.Entry recallledger$entry = recallledger.entry(petrificationledger$entry.dragon());
                        if (recallledger$entry == null
                            || recallledger$entry.state() == RecallLedger.State.STONE && markerMatches(compoundtag1, recallledger$entry)) {
                            if (recallledger$entry != null) {
                                for (ServerLevel serverlevel1 : serverlevel.getServer().getAllLevels()) {
                                    Entity entity = serverlevel1.getEntity(recallledger$entry.holder());
                                    if (entity != null && !entity.isRemoved() && RecallTracking.matches(entity, recallledger$entry)) {
                                        return fail(player, "blocked");
                                    }
                                }
                            }

                            StoneStatueEntity entitystonestatue = (StoneStatueEntity)((EntityType)IafEntities.STONE_STATUE.get()).create(serverlevel);
                            if (entitystonestatue == null) {
                                return fail(player, "blocked");
                            } else {
                                entitystonestatue.readAdditionalSaveData(compoundtag);
                                entitystonestatue.setTrappedEntityTypeString(petrificationledger$entry.type());
                                float f = player.getYRot() + 180.0F;
                                entitystonestatue.moveTo(
                                    (double)context.getClickedPos().getX() + 0.5,
                                    (double)(context.getClickedPos().getY() + 1),
                                    (double)context.getClickedPos().getZ() + 0.5,
                                    f,
                                    0.0F
                                );
                                entitystonestatue.yBodyRot = entitystonestatue.yBodyRotO = entitystonestatue.yHeadRot = entitystonestatue.yHeadRotO = f;
                                entitystonestatue.setCrackAmount(0);
                                double d0 = (double)entitystonestatue.getTrappedWidth() / 2.0;
                                AABB aabb = new AABB(
                                    entitystonestatue.getX() - d0,
                                    entitystonestatue.getY(),
                                    entitystonestatue.getZ() - d0,
                                    entitystonestatue.getX() + d0,
                                    entitystonestatue.getY() + (double)entitystonestatue.getTrappedHeight(),
                                    entitystonestatue.getZ() + d0
                                );
                                if (!clearSpace(serverlevel, entitystonestatue, aabb)) {
                                    return fail(player, "blocked");
                                } else {
                                    if (recallledger$entry == null && petrificationledger$entry.snapshot().hasUUID("Owner")) {
                                        recallledger$entry = recallledger.observeStone(
                                            petrificationledger$entry.dragon(),
                                            petrificationledger$entry.snapshot().getUUID("Owner"),
                                            petrificationledger$entry.type(),
                                            petrificationledger$entry.snapshot(),
                                            entitystonestatue
                                        );
                                        stampRecallStatue(entitystonestatue, recallledger$entry);
                                        compoundtag1.putLong("RecallGeneration", recallledger$entry.generation());
                                        compoundtag1.putUUID("RecallToken", recallledger$entry.token());
                                        compoundtag.getCompound("StatueEntityTag").put("DragonMaidPetrification", compoundtag1);
                                        // 1.21.1: write the refreshed payload back into the data component.
                                        itemstack.set(
                                            IafDataComponents.STONE_STATUS.get(),
                                            new StoneStatusComponent(stoneStatus.isPlayer(), stoneStatus.entityType(), compoundtag)
                                        );
                                    }

                                    boolean flag = recallledger$entry != null && recallledger.reserve(petrificationledger$entry.dragon());
                                    if (recallledger$entry != null && !flag) {
                                        return fail(player, "blocked");
                                    } else {
                                        RecallLedger.Transfer recallledger$transfer = null;
                                        boolean flag1 = false;

                                        InteractionResult suppressed;
                                        try {
                                            if (recallledger$entry != null) {
                                                CompoundTag compoundtag2 = entitystonestatue.saveWithoutId(new CompoundTag());
                                                compoundtag2.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entitystonestatue.getType()).toString());
                                                recallledger$transfer = recallledger.prepareTransfer(
                                                    petrificationledger$entry.dragon(),
                                                    RecallLedger.State.STONE,
                                                    serverlevel.dimension().location().toString(),
                                                    entitystonestatue.blockPosition(),
                                                    entitystonestatue.getUUID(),
                                                    compoundtag2
                                                );
                                                stampRecallStatue(entitystonestatue, recallledger$transfer.after());
                                            }

                                            if (!serverlevel.addFreshEntity(entitystonestatue) || entitystonestatue.isRemoved()) {
                                                throw new IllegalStateException("Statue placement rejected");
                                            }

                                            if (recallledger$transfer != null) {
                                                recallledger.commitTransfer(recallledger$transfer);
                                            } else {
                                                RecallPersistence.saveEntityChunk(serverlevel, entitystonestatue.chunkPosition());
                                            }

                                            flag1 = true;
                                            BasicDragonPetrification.consumePlacedStatue(player, context.getHand(), itemstack);
                                            if (recallledger$entry != null) {
                                                recallledger.observeStone(
                                                    petrificationledger$entry.dragon(),
                                                    recallledger$entry.owner(),
                                                    petrificationledger$entry.type(),
                                                    petrificationledger$entry.snapshot(),
                                                    entitystonestatue
                                                );
                                                recallledger.flush();
                                            }

                                            return InteractionResult.CONSUME;
                                        } catch (RuntimeException runtimeexception2) {
                                            if (!flag1) {
                                                if (!entitystonestatue.isRemoved()) {
                                                    try {
                                                        entitystonestatue.discard();
                                                    } catch (RuntimeException runtimeexception1) {
                                                        runtimeexception2.addSuppressed(runtimeexception1);
                                                    }
                                                }

                                                if (entitystonestatue.isRemoved()
                                                    && recallledger$transfer != null
                                                    && recallledger.pending(petrificationledger$entry.dragon()) != null) {
                                                    try {
                                                        recallledger.rollbackTransfer(recallledger$transfer);
                                                    } catch (RuntimeException runtimeexception) {
                                                        runtimeexception2.addSuppressed(runtimeexception);
                                                    }
                                                }

                                                LOGGER.error("Could not place managed dragon statue", runtimeexception2);
                                                return fail(player, "blocked");
                                            }

                                            suppressed = InteractionResult.CONSUME;
                                        } finally {
                                            if (flag) {
                                                recallledger.release(petrificationledger$entry.dragon());
                                            }
                                        }

                                        return suppressed;
                                    }
                                }
                            }
                        } else {
                            return fail(player, "duplicate");
                        }
                    } else {
                        return fail(player, "duplicate");
                    }
                } else {
                    return fail(player, "corrupt");
                }
            } else {
                return InteractionResult.FAIL;
            }
        }
    }

    public static void stampRecallStatue(StoneStatueEntity statue, RecallLedger.Entry entry) {
        RecallTracking.stamp(statue, entry);
        CompoundTag compoundtag = statue.getTrappedTag().copy();
        CompoundTag compoundtag1 = compoundtag.getCompound("DragonMaidPetrification");
        compoundtag1.putLong("RecallGeneration", entry.generation());
        compoundtag1.putUUID("RecallToken", entry.token());
        compoundtag.put("DragonMaidPetrification", compoundtag1);
        statue.setTrappedTag(compoundtag);
    }

    private static boolean markerMatches(CompoundTag marker, RecallLedger.Entry entry) {
        return marker.hasUUID("RecallToken")
            && marker.getUUID("RecallToken").equals(entry.token())
            && marker.getLong("RecallGeneration") == entry.generation();
    }

    public static boolean validRecallStatue(StoneStatueEntity statue, RecallLedger.Entry entry) {
        if (statue.level() instanceof ServerLevel serverlevel
            && entry.state() == RecallLedger.State.STONE
            && statue.getUUID().equals(entry.holder())
            && RecallTracking.matches(statue, entry)) {
            CompoundTag compoundtag = statue.getTrappedTag().getCompound("DragonMaidPetrification");
            if (compoundtag.hasUUID("Dragon")
                && entry.dragon().equals(compoundtag.getUUID("Dragon"))
                && compoundtag.hasUUID("Token")
                && markerMatches(compoundtag, entry)) {
                PetrificationLedger.Entry petrificationledger$entry = PetrificationLedger.get(serverlevel).entry(entry.dragon());
                return petrificationledger$entry != null
                    && petrificationledger$entry.active()
                    && petrificationledger$entry.token().equals(compoundtag.getUUID("Token"))
                    && petrificationledger$entry.snapshot().hasUUID("Owner")
                    && entry.owner().equals(petrificationledger$entry.snapshot().getUUID("Owner"));
            }

            return false;
        }

        return false;
    }

    public static StoneStatueEntity createRecallStatue(ServerLevel level, RecallLedger.Entry entry) {
        if (entry.state() == RecallLedger.State.STONE && !entry.snapshot().isEmpty()) {
            StoneStatueEntity entitystonestatue = (StoneStatueEntity)((EntityType)IafEntities.STONE_STATUE.get()).create(level);
            if (entitystonestatue == null) {
                return null;
            } else {
                entitystonestatue.load(entry.snapshot().copy());
                entitystonestatue.setUUID(entry.holder());
                stampRecallStatue(entitystonestatue, entry);
                repairPreparedPetrification(entitystonestatue, entry);
                return validRecallStatue(entitystonestatue, entry) ? entitystonestatue : null;
            }
        } else {
            return null;
        }
    }

    private static void repairPreparedPetrification(StoneStatueEntity statue, RecallLedger.Entry entry) {
        if (statue.level() instanceof ServerLevel serverlevel) {
            RecallLedger.Transfer recallledger$transfer = RecallLedger.get(serverlevel).pending(entry.dragon());
            if (recallledger$transfer != null
                && recallledger$transfer.before().state() == RecallLedger.State.WORLD
                && recallledger$transfer.after().state() == RecallLedger.State.STONE) {
                CompoundTag compoundtag = statue.getTrappedTag();
                CompoundTag compoundtag1 = compoundtag.getCompound("DragonMaidPetrification");
                if (compoundtag1.hasUUID("Dragon")
                    && compoundtag1.hasUUID("Token")
                    && entry.dragon().equals(compoundtag1.getUUID("Dragon"))
                    && compoundtag.hasUUID("Owner")
                    && entry.owner().equals(compoundtag.getUUID("Owner"))) {
                    PetrificationLedger petrificationledger = PetrificationLedger.get(serverlevel);
                    PetrificationLedger.Entry petrificationledger$entry = petrificationledger.entry(entry.dragon());
                    if (petrificationledger$entry == null
                        || !petrificationledger$entry.active()
                        || !petrificationledger$entry.token().equals(compoundtag1.getUUID("Token"))) {
                        CompoundTag compoundtag2 = compoundtag.copy();
                        compoundtag2.remove("DragonMaidPetrification");
                        petrificationledger.capture(
                            new PetrificationLedger.Entry(entry.dragon(), compoundtag1.getUUID("Token"), entry.type(), compoundtag2, true)
                        );
                        petrificationledger.flush();
                    }
                }
            }
        }
    }

    public static void prepareRecalledStatue(StoneStatueEntity statue) {
        if (statue.level() instanceof ServerLevel serverlevel) {
            CompoundTag compoundtag1 = statue.getTrappedTag().getCompound("DragonMaidPetrification");
            if (compoundtag1.hasUUID("Dragon")) {
                RecallLedger recallledger = RecallLedger.get(serverlevel);
                RecallLedger.Entry recallledger$entry = recallledger.entry(compoundtag1.getUUID("Dragon"));
                if (recallledger$entry != null && validRecallStatue(statue, recallledger$entry)) {
                    PetrificationLedger petrificationledger = PetrificationLedger.get(serverlevel);
                    PetrificationLedger.Entry petrificationledger$entry = petrificationledger.entry(recallledger$entry.dragon());
                    CompoundTag compoundtag = petrificationledger$entry.snapshot().copy();
                    compoundtag.putInt("Hunger", 50);
                    petrificationledger.capture(
                        new PetrificationLedger.Entry(
                            petrificationledger$entry.dragon(), petrificationledger$entry.token(), petrificationledger$entry.type(), compoundtag, true
                        )
                    );
                    petrificationledger.flush();
                    recallledger.observeStone(
                        petrificationledger$entry.dragon(), recallledger$entry.owner(), petrificationledger$entry.type(), compoundtag, statue
                    );
                    recallledger.flush();
                }
            }
        }
    }

    public static void statueMoved(EnteringSection event) {
        if (event.didChunkChange()
            && event.getEntity() instanceof StoneStatueEntity entitystonestatue
            && entitystonestatue.level() instanceof ServerLevel serverlevel
            && entitystonestatue.getTrappedTag().contains("DragonMaidPetrification")) {
            JOINED.put(entitystonestatue, serverlevel);
        }
    }

    public static void finishStatueJoin(ServerTickEvent.Post event) {
        if (true) {
            for (Entry<StoneStatueEntity, ServerLevel> entry : new ArrayList<>(JOINED.entrySet())) {
                StoneStatueEntity entitystonestatue = entry.getKey();
                ServerLevel serverlevel = entry.getValue();
                if (serverlevel.getServer() == event.getServer()) {
                    JOINED.remove(entitystonestatue);
                    if (!entitystonestatue.isRemoved() && serverlevel.getEntity(entitystonestatue.getUUID()) == entitystonestatue) {
                        CompoundTag compoundtag = entitystonestatue.getTrappedTag().getCompound("DragonMaidPetrification");
                        if (compoundtag.hasUUID("Dragon")) {
                            RecallLedger recallledger = RecallLedger.get(serverlevel);
                            RecallLedger.Entry recallledger$entry = recallledger.entry(compoundtag.getUUID("Dragon"));
                            if (recallledger$entry != null
                                && !recallledger.isReserved(recallledger$entry.dragon())
                                && validRecallStatue(entitystonestatue, recallledger$entry)) {
                                try {
                                    RecallLedger.Transfer recallledger$transfer = recallledger.pending(recallledger$entry.dragon());
                                    if (recallledger$transfer != null) {
                                        if (!RecallService.safeToFinalizeRecovery(event.getServer(), recallledger$transfer)) {
                                            JOINED.put(entitystonestatue, serverlevel);
                                            continue;
                                        }

                                        recallledger.commitTransfer(recallledger$transfer);
                                    }

                                    PetrificationLedger.Entry petrificationledger$entry = PetrificationLedger.get(serverlevel)
                                        .entry(recallledger$entry.dragon());
                                    recallledger.observeStone(
                                        recallledger$entry.dragon(),
                                        recallledger$entry.owner(),
                                        petrificationledger$entry.type(),
                                        petrificationledger$entry.snapshot(),
                                        entitystonestatue
                                    );
                                    recallledger.flush();
                                } catch (RuntimeException runtimeexception) {
                                    LOGGER.error("Could not checkpoint a managed statue", runtimeexception);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void serverStopped(ServerStoppedEvent event) {
        JOINED.entrySet().removeIf(e -> e.getValue().getServer() == event.getServer());
        PENDING.entrySet().removeIf(e -> e.getValue().dragon().level().getServer() == event.getServer());
    }

    private static boolean clearSpace(ServerLevel level, Entity entity, AABB box) {
        return box.minY >= (double)level.getMinBuildHeight()
            && box.maxY <= (double)level.getMaxBuildHeight()
            && level.getWorldBorder().isWithinBounds(box)
            && !level.getBlockCollisions(entity, box.deflate(1.0E-4)).iterator().hasNext();
    }

    private static boolean inReach(Player player, StoneStatueEntity statue) {
        Vec3 vec3 = player.getEyePosition();
        AABB aabb = statue.getBoundingBox();
        double d0 = Math.max(aabb.minX, Math.min(aabb.maxX, vec3.x));
        double d1 = Math.max(aabb.minY, Math.min(aabb.maxY, vec3.y));
        double d2 = Math.max(aabb.minZ, Math.min(aabb.maxZ, vec3.z));
        return vec3.distanceToSqr(d0, d1, d2) <= 64.0;
    }

    private static void notifyRestored(Player player, DragonBaseEntity dragon) {
        try {
            NeoForge.EVENT_BUS.post(new DragonDepetrifiedEvent(player, dragon));
        } catch (RuntimeException runtimeexception) {
            LOGGER.warn("Dragon {} restored, but a rescue listener failed", dragon.getUUID(), runtimeexception);
        }
    }

    private static InteractionResult fail(Player player, String reason) {
        player.displayClientMessage(Component.translatable("message.dragonmaid.petrification." + reason), true);
        return InteractionResult.FAIL;
    }

    private static record Pending(DragonBaseEntity dragon, UUID token, CompoundTag snapshot) {
    }
}
