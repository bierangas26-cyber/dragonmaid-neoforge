package com.example.dragonmaid.recall;

import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.item.DragonHornItem;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.LoggerFactory;
import com.example.dragonmaid.compat.StackNbt;

public final class RecallHorn {
    private static final String RECEIPT = "DragonMaidRecall";

    private RecallHorn() {
    }

    public static InteractionResult capture(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof DragonBaseEntity entitydragonbase) || !RecallTracking.supported(entitydragonbase)) {
            return null;
        }

        if (!player.isSpectator() && hand == InteractionHand.MAIN_HAND && entitydragonbase.isOwnedBy(player)) {
            ItemStack itemstack = player.getItemInHand(hand);
            if (!(itemstack.getItem() instanceof DragonHornItem) || itemstack.getCount() != 1 || !ItemStack.isSameItemSameComponents(stack, itemstack)) {
                return InteractionResult.FAIL;
            } else if (HumanoidLife.isInjured(entitydragonbase)) {
                if (!player.level().isClientSide) {
                    player.displayClientMessage(Component.translatable("message.dragonmaid.injury_no_horn"), true);
                }

                return InteractionResult.FAIL;
            } else if (!entitydragonbase.isAlive()
                || entitydragonbase.isModelDead()
                || entitydragonbase.isPassenger()
                || entitydragonbase.isVehicle()
                || itemstack.has(IafDataComponents.DRAGON_HORN.get())
                || StackNbt.hasTag(itemstack)
                    && (!StackNbt.getTag(itemstack).getCompound("EntityTag").isEmpty() || !StackNbt.getTag(itemstack).getString("DragonHornEntityID").isEmpty())) {
                return InteractionResult.FAIL;
            } else {
                if (!(player.level() instanceof ServerLevel serverlevel)) {
                    return InteractionResult.SUCCESS;
                }

                RecallLedger recallledger = RecallLedger.get(serverlevel);
                RecallLedger.Entry recallledger$entry = recallledger.observe(entitydragonbase);
                if (recallledger$entry != null
                    && recallledger$entry.state() == RecallLedger.State.WORLD
                    && RecallTracking.matches(entitydragonbase, recallledger$entry)
                    && recallledger.reserve(entitydragonbase.getUUID())) {
                    RecallLedger.Transfer recallledger$transfer = null;
                    CompoundTag compoundtag = StackNbt.getTag(itemstack) == null ? null : StackNbt.getTag(itemstack).copy();
                    boolean flag = false;

                    InteractionResult commitFailure;
                    try {
                        CompoundTag compoundtag1 = new CompoundTag();
                        if (entitydragonbase.save(compoundtag1)
                            && compoundtag1.hasUUID("UUID")
                            && entitydragonbase.getUUID().equals(compoundtag1.getUUID("UUID"))
                            && player.getItemInHand(hand) == itemstack
                            && !entitydragonbase.isRemoved()) {
                            recallledger$transfer = recallledger.prepareTransfer(
                                entitydragonbase.getUUID(),
                                RecallLedger.State.HORN,
                                serverlevel.dimension().location().toString(),
                                player.blockPosition(),
                                entitydragonbase.getUUID(),
                                compoundtag1
                            );
                            CompoundTag compoundtag2 = StackNbt.getOrCreateTag(itemstack);
                            compoundtag2.put("EntityTag", recallledger$transfer.after().snapshot().copy());
                            compoundtag2.putString("DragonHornEntityID", BuiltInRegistries.ENTITY_TYPE.getKey(entitydragonbase.getType()).toString());
                            writeReceipt(compoundtag2, recallledger$transfer.after());
                            flag = true;
                            if (player.getItemInHand(hand) != itemstack) {
                                throw new IllegalStateException("The held horn changed during capture");
                            }

                            entitydragonbase.discard();
                            if (!entitydragonbase.isRemoved()) {
                                throw new IllegalStateException("Dragon remained in world after horn capture");
                            }

                            recallledger.commitTransfer(recallledger$transfer);
                            player.swing(hand);
                            serverlevel.playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.NEUTRAL, 3.0F, 0.75F);
                            return InteractionResult.SUCCESS;
                        }

                        return InteractionResult.FAIL;
                    } catch (RuntimeException runtimeexception2) {
                        if (entitydragonbase.isRemoved()) {
                            if (recallledger$transfer != null && recallledger.pending(entitydragonbase.getUUID()) != null) {
                                try {
                                    recallledger.commitTransfer(recallledger$transfer);
                                } catch (RuntimeException runtimeexception1) {
                                    runtimeexception2.addSuppressed(runtimeexception1);
                                }
                            }

                            LoggerFactory.getLogger("DragonMaid/Recall")
                                .error("Horn capture encountered an error; stored snapshot retained", runtimeexception2);
                            return flag ? InteractionResult.SUCCESS : InteractionResult.FAIL;
                        }

                        if (recallledger$transfer != null && recallledger.pending(entitydragonbase.getUUID()) != null) {
                            try {
                                recallledger.rollbackTransfer(recallledger$transfer);
                                StackNbt.setTag(itemstack, compoundtag);
                            } catch (RuntimeException runtimeexception) {
                                runtimeexception2.addSuppressed(runtimeexception);
                            }
                        }

                        LoggerFactory.getLogger("DragonMaid/Recall").error("Horn capture failed; original retained", runtimeexception2);
                        commitFailure = InteractionResult.FAIL;
                    } finally {
                        recallledger.release(entitydragonbase.getUUID());
                    }

                    return commitFailure;
                } else {
                    return InteractionResult.FAIL;
                }
            }
        } else {
            return InteractionResult.FAIL;
        }
    }

    public static InteractionResult release(UseOnContext context) {
        ItemStack itemstack = context.getItemInHand();
        CompoundTag compoundtag = StackNbt.getTag(itemstack);
        if (compoundtag != null && !compoundtag.getCompound("EntityTag").isEmpty()) {
            EntityType<?> entitytype = (EntityType<?>)EntityType.byString(compoundtag.getString("DragonHornEntityID")).orElse(null);
            if (entitytype != null && supportedType(entitytype)) {
                if (context.getClickedFace() == Direction.UP && (context.getPlayer() == null || !context.getPlayer().isSpectator())) {
                    if (context.getLevel() instanceof ServerLevel serverlevel) {
                        RecallLedger.Entry recallledger$entry;
                        try {
                            recallledger$entry = observe(itemstack, serverlevel, context.getClickedPos());
                        } catch (RuntimeException runtimeexception2) {
                            LoggerFactory.getLogger("DragonMaid/Recall").warn("Horn release is waiting for storage", runtimeexception2);
                            if (context.getPlayer() != null) {
                                context.getPlayer().displayClientMessage(Component.translatable("message.dragonmaid.recall.busy"), true);
                            }

                            return InteractionResult.FAIL;
                        }

                        if (recallledger$entry == null) {
                            if (context.getPlayer() != null) {
                                context.getPlayer()
                                    .displayClientMessage(
                                        Component.translatable(
                                            !compoundtag.getCompound("EntityTag").isEmpty()
                                                ? "message.dragonmaid.recall.busy"
                                                : "message.dragonmaid.recall.horn_expired"
                                        ),
                                        true
                                    );
                            }

                            return InteractionResult.FAIL;
                        } else {
                            RecallLedger recallledger = RecallLedger.get(serverlevel);
                            if (!recallledger.reserve(recallledger$entry.dragon())) {
                                return InteractionResult.FAIL;
                            } else {
                                RecallLedger.Transfer recallledger$transfer = null;
                                DragonBaseEntity entitydragonbase = null;

                                InteractionResult interactionresult;
                                try {
                                    if (!(entitytype.create(serverlevel) instanceof DragonBaseEntity entitydragonbase1)) {
                                        return InteractionResult.FAIL;
                                    }

                                    entitydragonbase = entitydragonbase1;
                                    entitydragonbase1.load(recallledger$entry.snapshot().copy());
                                    entitydragonbase1.setUUID(recallledger$entry.dragon());
                                    entitydragonbase1.absMoveTo(
                                        (double)context.getClickedPos().getX() + 0.5,
                                        (double)(context.getClickedPos().getY() + 1),
                                        (double)context.getClickedPos().getZ() + 0.5,
                                        180.0F + context.getHorizontalDirection().toYRot(),
                                        0.0F
                                    );
                                    CompoundTag compoundtag1 = new CompoundTag();
                                    if (entitydragonbase1.save(compoundtag1)) {
                                        recallledger$transfer = recallledger.prepareTransfer(
                                            recallledger$entry.dragon(),
                                            RecallLedger.State.WORLD,
                                            serverlevel.dimension().location().toString(),
                                            entitydragonbase1.blockPosition(),
                                            entitydragonbase1.getUUID(),
                                            compoundtag1
                                        );
                                        RecallTracking.stamp(entitydragonbase1, recallledger$transfer.after());
                                        if (!serverlevel.addFreshEntity(entitydragonbase1)) {
                                            throw new IllegalStateException("Dragon release target was not accepted");
                                        }

                                        recallledger.commitTransfer(recallledger$transfer);
                                        clear(itemstack);
                                        return InteractionResult.SUCCESS;
                                    }

                                    interactionresult = InteractionResult.FAIL;
                                } catch (RuntimeException runtimeexception3) {
                                    if (recallledger$transfer != null && recallledger.pending(recallledger$entry.dragon()) != null) {
                                        boolean flag = entitydragonbase == null || entitydragonbase.isRemoved();
                                        if (!flag) {
                                            try {
                                                entitydragonbase.discard();
                                            } catch (RuntimeException runtimeexception1) {
                                                runtimeexception3.addSuppressed(runtimeexception1);
                                            }

                                            flag = entitydragonbase.isRemoved();
                                        }

                                        if (flag) {
                                            try {
                                                recallledger.rollbackTransfer(recallledger$transfer);
                                            } catch (RuntimeException runtimeexception) {
                                                runtimeexception3.addSuppressed(runtimeexception);
                                            }
                                        }
                                    }

                                    LoggerFactory.getLogger("DragonMaid/Recall").error("Horn release failed; escrow retained", runtimeexception3);
                                    return InteractionResult.FAIL;
                                } finally {
                                    recallledger.release(recallledger$entry.dragon());
                                }

                                return interactionresult;
                            }
                        }
                    } else {
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    return InteractionResult.FAIL;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static boolean supportedType(EntityType<?> type) {
        return type == IafEntities.FIRE_DRAGON.get() || type == IafEntities.ICE_DRAGON.get() || type == IafEntities.LIGHTNING_DRAGON.get();
    }

    static ServerLevel captureSource(ServerLevel level, RecallLedger.Transfer transfer) {
        ResourceLocation resourcelocation = ResourceLocation.tryParse(transfer.before().dimension());
        return resourcelocation == null ? null : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, resourcelocation));
    }

    static RecallHorn.CaptureRecovery recoverCapture(ServerLevel level, RecallLedger ledger, RecallLedger.Transfer transfer) {
        if (transfer.before().state() == RecallLedger.State.WORLD && transfer.after().state() == RecallLedger.State.HORN) {
            ServerLevel serverlevel = captureSource(level, transfer);
            if (serverlevel == null) {
                return RecallHorn.CaptureRecovery.WAIT;
            } else {
                for (ServerLevel serverlevel1 : level.getServer().getAllLevels()) {
                    Entity entity = serverlevel1.getEntity(transfer.before().dragon());
                    if (entity != null && !entity.isRemoved()) {
                        if (entity instanceof DragonBaseEntity && RecallTracking.matches(entity, transfer.before())) {
                            ledger.rollbackTransfer(transfer);
                            return RecallHorn.CaptureRecovery.ROLLED_BACK;
                        }

                        return RecallHorn.CaptureRecovery.WAIT;
                    }
                }

                return serverlevel.areEntitiesLoaded(new ChunkPos(transfer.before().pos()).toLong())
                    ? RecallHorn.CaptureRecovery.READY
                    : RecallHorn.CaptureRecovery.WAIT;
            }
        } else {
            return RecallHorn.CaptureRecovery.READY;
        }
    }

    public static void inventoryTick(ItemStack stack, Entity carrier) {
        if (carrier.level() instanceof ServerLevel serverlevel
            && serverlevel.getGameTime() % 20L == 0L
            && stack.getItem() instanceof DragonHornItem
            && StackNbt.hasTag(stack)
            && !StackNbt.getTag(stack).getCompound("EntityTag").isEmpty()) {
            try {
                observe(stack, serverlevel, carrier.blockPosition());
            } catch (RuntimeException runtimeexception) {
                LoggerFactory.getLogger("DragonMaid/Recall").warn("Horn discovery is waiting for storage", runtimeexception);
            }
        }
    }

    public static RecallLedger.Entry observe(ItemStack stack, ServerLevel level, BlockPos pos) {
        // 1.21.1 port: StackNbt.getTag() returns a COPY of CUSTOM_DATA (1.20.1 returned the live tag).
        // Everything written below (imported snapshot + DragonMaidRecall receipt) must be flushed back,
        // otherwise the horn receipt never persists and release() rejects the stored dragon.
        CompoundTag compoundtag = StackNbt.getOrCreateTag(stack);
        if (compoundtag == null) {
            return null;
        } else {
            CompoundTag compoundtag1 = compoundtag.getCompound("EntityTag");
            if (!compoundtag1.isEmpty() && compoundtag1.hasUUID("Owner") && compoundtag1.hasUUID("UUID")) {
                String s = compoundtag.getString("DragonHornEntityID");
                EntityType<?> entitytype = (EntityType<?>)EntityType.byString(s).orElse(null);
                if (entitytype != null && supportedType(entitytype)) {
                    UUID uuid = compoundtag.hasUUID("EntityUUID") ? compoundtag.getUUID("EntityUUID") : compoundtag1.getUUID("UUID");
                    RecallLedger recallledger = RecallLedger.get(level);
                    RecallLedger.Entry recallledger$entry = recallledger.entry(uuid);
                    if (recallledger$entry == null) {
                        CompoundTag compoundtag2 = compoundtag1.copy();
                        compoundtag2.putUUID("UUID", uuid);

                        try {
                            recallledger$entry = recallledger.importHorn(s, compoundtag2, level.dimension().location().toString(), pos);
                        } catch (RuntimeException runtimeexception1) {
                            LoggerFactory.getLogger("DragonMaid/Recall").warn("Legacy horn import is waiting for storage", runtimeexception1);
                            return null;
                        }

                        if (recallledger$entry != null) {
                            compoundtag.put("EntityTag", recallledger$entry.snapshot().copy());
                            writeReceipt(compoundtag, recallledger$entry);
                        }
                    }

                    CompoundTag compoundtag3 = compoundtag.getCompound("DragonMaidRecall");
                    RecallLedger.Transfer recallledger$transfer = recallledger.pending(uuid);
                    if (recallledger$transfer != null
                        && recallledger$transfer.before().state() == RecallLedger.State.HORN
                        && recallledger$transfer.after().state() == RecallLedger.State.WORLD
                        && compoundtag3.hasUUID("Token")
                        && recallledger$transfer.before().token().equals(compoundtag3.getUUID("Token"))
                        && compoundtag3.hasUUID("Dragon")
                        && uuid.equals(compoundtag3.getUUID("Dragon"))
                        && compoundtag3.getLong("Generation") == recallledger$transfer.before().generation()) {
                        return null;
                    } else if (recallledger$entry != null
                        && recallledger$entry.state() == RecallLedger.State.HORN
                        && recallledger.pending(uuid) == null
                        && compoundtag3.hasUUID("Token")
                        && recallledger$entry.token().equals(compoundtag3.getUUID("Token"))
                        && compoundtag3.getLong("Generation") == recallledger$entry.generation()
                        && compoundtag3.hasUUID("Dragon")
                        && uuid.equals(compoundtag3.getUUID("Dragon"))) {
                        return recallledger$entry;
                    } else {
                        RecallLedger.Transfer recallledger$transfer1 = recallledger.pending(uuid);
                        if (recallledger$entry == null
                            || recallledger$transfer1 == null
                            || recallledger$entry.state() != RecallLedger.State.HORN
                            || !compoundtag3.hasUUID("Token")
                            || !recallledger$entry.token().equals(compoundtag3.getUUID("Token"))
                            || !compoundtag3.hasUUID("Dragon")
                            || !uuid.equals(compoundtag3.getUUID("Dragon"))
                            || compoundtag3.getLong("Generation") != recallledger$entry.generation()) {
                            clear(stack);
                            return null;
                        } else if (recallledger.isReserved(uuid)) {
                            return null;
                        } else {
                            try {
                                RecallHorn.CaptureRecovery recallhorn$capturerecovery = recoverCapture(level, recallledger, recallledger$transfer1);
                                if (recallhorn$capturerecovery == RecallHorn.CaptureRecovery.WAIT) {
                                    return null;
                                } else if (recallhorn$capturerecovery == RecallHorn.CaptureRecovery.ROLLED_BACK) {
                                    clear(stack);
                                    return null;
                                } else {
                                    recallledger.commitTransfer(recallledger$transfer1);
                                    return recallledger.entry(uuid);
                                }
                            } catch (RuntimeException runtimeexception) {
                                LoggerFactory.getLogger("DragonMaid/Recall").warn("Horn recovery is waiting for storage", runtimeexception);
                                return null;
                            }
                        }
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    private static void writeReceipt(CompoundTag horn, RecallLedger.Entry entry) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putUUID("Dragon", entry.dragon());
        compoundtag.putLong("Generation", entry.generation());
        compoundtag.putUUID("Token", entry.token());
        horn.put("DragonMaidRecall", compoundtag);
    }

    public static void clear(ItemStack stack) {
        // 1.21.1 port: getTag() hands back a copy, so these removes() were silently discarded and a
        // stale horn could never be cleared. getOrCreateTag() writes every mutation back to the stack.
        CompoundTag compoundtag = StackNbt.getOrCreateTag(stack);
        compoundtag.remove("DragonHornEntityID");
        compoundtag.remove("EntityTag");
        compoundtag.remove("EntityUUID");
        compoundtag.remove("DragonMaidRecall");
    }

    static enum CaptureRecovery {
        ROLLED_BACK,
        READY,
        WAIT;

        private CaptureRecovery() {
        }
    }
}
