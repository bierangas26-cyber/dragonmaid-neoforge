package com.example.dragonmaid.compat;

import com.example.dragonmaid.combat.GuardEscort;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.mixin.DragonForgeAccessor;
import com.example.dragonmaid.mixin.DragonForgeInputAccessor;
import com.iafenvoy.iceandfire.registry.IafBlocks;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import iafcompat.IafCompat;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeInputBlockEntity;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class ForgeCooperation {
    private static final Map<DragonForgeBlockEntity, ForgeCooperation.Batch> PENDING = new WeakHashMap<>();

    private ForgeCooperation() {
    }

    public static void fuelNative(DragonForgeInputBlockEntity input, DragonBaseEntity dragon) {
        input.onHitWithFlame(dragon);
        if (input.getLevel() instanceof ServerLevel serverlevel && dragon.level() == serverlevel) {
            DragonForgeBlockEntity tileentitydragonforge = ((DragonForgeInputAccessor)input).dragonmaid$getForgeCore();
            if (tileentitydragonforge != null) {
                record(tileentitydragonforge, input, dragon);
            }

            return;
        }
    }

    public static void fuelHuman(DragonForgeBlockEntity core, DragonBaseEntity dragon) {
        core.transferPower(1);
        if (core.getLevel() instanceof ServerLevel serverlevel
            && dragon.level() == serverlevel
            && dragon.burningTarget != null
            && serverlevel.hasChunkAt(dragon.burningTarget)) {
            if (serverlevel.getBlockEntity(dragon.burningTarget) instanceof DragonForgeInputBlockEntity tileentitydragonforgeinput) {
                record(core, tileentitydragonforgeinput, dragon);
            }

            return;
        }
    }

    private static void record(DragonForgeBlockEntity core, DragonForgeInputBlockEntity input, DragonBaseEntity dragon) {
        if (core.getLevel() instanceof ServerLevel serverlevel && input.getLevel() == serverlevel && dragon.level() == serverlevel) {
            long j = serverlevel.getGameTime();
            ForgeCooperation.Batch forgecooperation$batch = PENDING.get(core);
            if (forgecooperation$batch != null && forgecooperation$batch.tick == j && forgecooperation$batch.dragons.contains(dragon.getUUID())) {
                return;
            }

            if (serverlevel.hasChunkAt(core.getBlockPos())
                && serverlevel.getBlockEntity(core.getBlockPos()) == core
                && serverlevel.hasChunkAt(input.getBlockPos())
                && serverlevel.getBlockEntity(input.getBlockPos()) == input
                && !dragon.isRemoved()
                && dragon.isAlive()
                && !dragon.isModelDead()
                && !dragon.isBaby()
                && (!DragonForms.isHumanoid(dragon) || dragon.getDragonStage() >= 3)
                && dragon.burningTarget != null
                && dragon.burningTarget.equals(input.getBlockPos())
                && core.assembled()
                && core.canSmelt()) {
                boolean flag = dragon.isTame() || GuardEscort.hasChain(dragon);
                if (flag && (!DragonForms.isHumanoid(dragon) || dragon.isTame())) {
                    Block block = serverlevel.getBlockState(input.getBlockPos()).getBlock();
                    int i = block == IafBlocks.DRAGONFORGE_FIRE_INPUT.get()
                        ? 0
                        : (block == IafBlocks.DRAGONFORGE_ICE_INPUT.get() ? 1 : (block == IafBlocks.DRAGONFORGE_LIGHTNING_INPUT.get() ? 2 : -1));
                    if (i >= 0 && i == IafCompat.dragonTypeIndex(dragon) && i == core.getFireType(core.getBlockState().getBlock())) {
                        Vec3 vec3 = Vec3.atCenterOf(input.getBlockPos());
                        Vec3 vec31 = dragon.getEyePosition();
                        BlockHitResult blockhitresult = serverlevel.clip(
                            new ClipContext(vec31, vec3, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, dragon)
                        );
                        if (blockhitresult.getType() != Type.MISS && !blockhitresult.getBlockPos().equals(input.getBlockPos())) {
                            return;
                        }

                        if (forgecooperation$batch == null || forgecooperation$batch.tick != j) {
                            forgecooperation$batch = new ForgeCooperation.Batch(j, core);
                            PENDING.put(core, forgecooperation$batch);
                        }

                        forgecooperation$batch.dragons.add(dragon.getUUID());
                        return;
                    }

                    return;
                }

                return;
            }

            return;
        }
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        return a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b);
    }

    @SubscribeEvent
    public static void afterWorldTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverlevel) {
            Iterator iterator = PENDING.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<DragonForgeBlockEntity, ForgeCooperation.Batch> entry = (Entry<DragonForgeBlockEntity, ForgeCooperation.Batch>)iterator.next();
                DragonForgeBlockEntity tileentitydragonforge = entry.getKey();
                ForgeCooperation.Batch forgecooperation$batch = entry.getValue();
                if (tileentitydragonforge != null && tileentitydragonforge.getLevel() != null && !tileentitydragonforge.isRemoved()) {
                    if (tileentitydragonforge.getLevel() == serverlevel) {
                        iterator.remove();
                        if (forgecooperation$batch.tick == serverlevel.getGameTime()
                            && forgecooperation$batch.dragons.size() >= 2
                            && serverlevel.hasChunkAt(tileentitydragonforge.getBlockPos())
                            && serverlevel.getBlockEntity(tileentitydragonforge.getBlockPos()) == tileentitydragonforge
                            && tileentitydragonforge.assembled()
                            && tileentitydragonforge.canSmelt()
                            && ((DragonForgeAccessor)tileentitydragonforge).dragonmaid$getCookTime() > 0.0
                            && ((DragonForgeAccessor)tileentitydragonforge).dragonmaid$getCookTime() >= forgecooperation$batch.startCookTime
                            && sameStack(forgecooperation$batch.input, tileentitydragonforge.getItem(0))
                            && sameStack(forgecooperation$batch.blood, tileentitydragonforge.getItem(1))
                            && sameStack(forgecooperation$batch.output, tileentitydragonforge.getItem(2))) {
                            double d0 = tileentitydragonforge.getMaxCookTime() - 1.0;
                            double d1 = ((DragonForgeAccessor)tileentitydragonforge).dragonmaid$getCookTime();
                            if (d0 > d1) {
                                int j = forgecooperation$batch.dragons.size() - 1;
                                ((DragonForgeAccessor)tileentitydragonforge).dragonmaid$setCookTime(d1 + Math.min((double)j, d0 - d1));
                                tileentitydragonforge.setChanged();
                            }
                        }
                    }
                } else {
                    iterator.remove();
                }
            }
        }
    }

    private static final class Batch {
        final long tick;
        final double startCookTime;
        final ItemStack input;
        final ItemStack blood;
        final ItemStack output;
        final Set<UUID> dragons = new HashSet<>();

        Batch(long tick, DragonForgeBlockEntity core) {
            this.tick = tick;
            this.startCookTime = ((DragonForgeAccessor)core).dragonmaid$getCookTime();
            this.input = core.getItem(0).copy();
            this.blood = core.getItem(1).copy();
            this.output = core.getItem(2).copy();
        }
    }
}
