package com.example.dragonmaid.petrification;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.item.ModItems;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import com.iafenvoy.iceandfire.item.component.StoneStatusComponent;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.world.DragonPosWorldData;
import com.mojang.logging.LogUtils;
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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import com.example.dragonmaid.compat.StackNbt;

public final class BasicDragonPetrification {
    public static final String MARKER = "DragonMaidPetrification";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<StoneStatueEntity, BasicDragonPetrification.Pending> PENDING = new WeakHashMap<>();
    private static final ThreadLocal<DragonBaseEntity> RESTORING = new ThreadLocal<>();

    private BasicDragonPetrification() {
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

            PENDING.put(entitystonestatue, new BasicDragonPetrification.Pending(entitydragonbase, uuid, compoundtag));
            return entitystonestatue;
        } else {
            return entitystonestatue;
        }
    }

    public static boolean addStatue(Level world, Entity entity) {
        BasicDragonPetrification.Pending basicdragonpetrification$pending = entity instanceof StoneStatueEntity entitystonestatue
            ? PENDING.remove(entitystonestatue)
            : null;
        if (basicdragonpetrification$pending == null) {
            return world.addFreshEntity(entity);
        } else {
            if (world instanceof ServerLevel serverlevel
                && basicdragonpetrification$pending.snapshot() != null
                && !basicdragonpetrification$pending.dragon().isRemoved()) {
                DragonBaseEntity entitydragonbase = basicdragonpetrification$pending.dragon();
                PetrificationLedger petrificationledger = PetrificationLedger.get(serverlevel);
                if (!petrificationledger.active(entitydragonbase.getUUID()) && petrificationledger.reserve(entitydragonbase.getUUID())) {
                    PetrificationLedger.Entry petrificationledger$entry = petrificationledger.entry(entitydragonbase.getUUID());
                    boolean flag = false;

                    boolean flag1;
                    try {
                        if (serverlevel.addFreshEntity(entity) && !entity.isRemoved()) {
                            petrificationledger.capture(
                                new PetrificationLedger.Entry(
                                    entitydragonbase.getUUID(),
                                    basicdragonpetrification$pending.token(),
                                    BuiltInRegistries.ENTITY_TYPE.getKey(entitydragonbase.getType()).toString(),
                                    basicdragonpetrification$pending.snapshot().copy(),
                                    true
                                )
                            );
                            entitydragonbase.ejectPassengers();
                            entitydragonbase.stopRiding();
                            entitydragonbase.remove(RemovalReason.DISCARDED);
                            flag = true;
                            DragonPosWorldData.get(serverlevel).removeDragon(entitydragonbase.getUUID());
                            return true;
                        }

                        return false;
                    } catch (RuntimeException runtimeexception) {
                        if (flag || entitydragonbase.isRemoved()) {
                            LOGGER.warn("Dragon {} transferred, but statue cleanup failed", entitydragonbase.getUUID(), runtimeexception);
                            return true;
                        }

                        petrificationledger.rollbackCapture(entitydragonbase.getUUID(), petrificationledger$entry);
                        if (!entity.isRemoved()) {
                            entity.discard();
                        }

                        LOGGER.error("Could not petrify dragon {}; preserving the original", entitydragonbase.getUUID(), runtimeexception);
                        flag1 = false;
                    } finally {
                        petrificationledger.release(entitydragonbase.getUUID());
                    }

                    return flag1;
                }

                return false;
            }

            return false;
        }
    }

    public static void preventDuplicateDragon(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel serverlevel
            && event.getEntity() instanceof DragonBaseEntity entitydragonbase
            && RESTORING.get() != entitydragonbase) {
            if (PetrificationLedger.get(serverlevel).active(entitydragonbase.getUUID())) {
                event.setCanceled(true);
            }

            return;
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
                CompoundTag compoundtag = statue.getTrappedTag().getCompound("DragonMaidPetrification");
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

                if (compoundtag.hasUUID("Dragon") && compoundtag.hasUUID("Token")) {
                    UUID uuid = compoundtag.getUUID("Dragon");
                    PetrificationLedger petrificationledger = PetrificationLedger.get(serverlevel);
                    PetrificationLedger.Entry petrificationledger$entry = petrificationledger.entry(uuid);
                    if (petrificationledger$entry != null
                        && petrificationledger$entry.active()
                        && petrificationledger$entry.token().equals(compoundtag.getUUID("Token"))) {
                        if (!petrificationledger.reserve(uuid)) {
                            return fail(player, "blocked");
                        }

                        DragonBaseEntity entitydragonbase = null;
                        boolean flag = false;

                        InteractionResult interactionresult;
                        try {
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

                            RESTORING.set(entitydragonbase1);

                            boolean flag1;
                            try {
                                flag1 = serverlevel.addFreshEntity(entitydragonbase1);
                            } finally {
                                RESTORING.remove();
                            }

                            if (!flag1 || entitydragonbase1.isRemoved()) {
                                return fail(player, "blocked");
                            }

                            petrificationledger.complete(petrificationledger$entry);
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
                            interactionresult = InteractionResult.CONSUME;
                        } catch (RuntimeException runtimeexception) {
                            if (!flag) {
                                LOGGER.error("Could not restore petrified dragon {}", uuid, runtimeexception);
                                return fail(player, "corrupt");
                            }

                            if (!statue.isRemoved()) {
                                statue.discard();
                            }

                            LOGGER.warn("Dragon {} restored; a post-restoration notification or cleanup failed", uuid, runtimeexception);
                            return InteractionResult.CONSUME;
                        } finally {
                            RESTORING.remove();
                            if (!flag && entitydragonbase != null && serverlevel.getEntity(uuid) == entitydragonbase) {
                                entitydragonbase.discard();
                            }

                            if (!flag && !petrificationledger.active(uuid)) {
                                petrificationledger.capture(petrificationledger$entry);
                            }

                            if (entitydragonbase != null && serverlevel.getEntity(uuid) != entitydragonbase) {
                                entitydragonbase.removeParts();
                            }

                            petrificationledger.release(uuid);
                        }

                        return interactionresult;
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
        if (player == null || player.isSpectator() || context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        } else if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (context.getLevel() instanceof ServerLevel serverlevel) {
            ItemStack itemstack = context.getItemInHand();
            if (!itemstack.isEmpty() && player.getItemInHand(context.getHand()) == itemstack) {
                // 1.21.1: read the statue payload from the STONE_STATUS data component instead of the
                // removed 1.20.1 "IAFStoneStatueNBT" item tag.
                StoneStatusComponent stoneStatus = itemstack.get(IafDataComponents.STONE_STATUS.get());
                CompoundTag compoundtag = stoneStatus == null ? new CompoundTag() : stoneStatus.nbt().copy();
                CompoundTag compoundtag1 = compoundtag.getCompound("StatueEntityTag").getCompound("DragonMaidPetrification");
                if (compoundtag1.hasUUID("Dragon") && compoundtag1.hasUUID("Token")) {
                    PetrificationLedger.Entry petrificationledger$entry = PetrificationLedger.get(serverlevel).entry(compoundtag1.getUUID("Dragon"));
                    if (petrificationledger$entry != null
                        && petrificationledger$entry.active()
                        && petrificationledger$entry.token().equals(compoundtag1.getUUID("Token"))) {
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
                            if (clearSpace(serverlevel, entitystonestatue, aabb) && serverlevel.addFreshEntity(entitystonestatue) && !entitystonestatue.isRemoved()) {
                                consumePlacedStatue(player, context.getHand(), itemstack);
                                return InteractionResult.CONSUME;
                            } else {
                                return fail(player, "blocked");
                            }
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
        } else {
            return InteractionResult.FAIL;
        }
    }

    public static void consumePlacedStatue(Player player, InteractionHand hand, ItemStack original) {
        ItemStack itemstack = original.copy();
        itemstack.shrink(1);
        player.setItemInHand(hand, itemstack);
        player.getInventory().setChanged();
    }

    public static void serverStopped(ServerStoppedEvent event) {
        PENDING.entrySet().removeIf(e -> {
            if (e.getValue().dragon().level() instanceof ServerLevel serverlevel && serverlevel.getServer() == event.getServer()) {
                return true;
            }

            return false;
        });
        RESTORING.remove();
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
