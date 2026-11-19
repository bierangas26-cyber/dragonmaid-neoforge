package com.example.dragonmaid.starter;

import com.example.dragonmaid.compat.StackNbt;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.recall.RecallHorn;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafItems;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import org.slf4j.LoggerFactory;

/**
 * 1.2.7 starting companion: a per-world SavedData that hands the chosen companion (or its egg) to the
 * owner on first login and never twice.
 *
 * <p>1.21.1 port notes: {@code SavedData#save} now takes a {@link Provider}; the storage is read
 * through {@link SavedData.Factory}; item NBT goes through {@code compat/StackNbt}; the dragon horn
 * comes from {@code IafItems} and the entity id from {@code BuiltInRegistries.ENTITY_TYPE}.
 */
@EventBusSubscriber(modid = "dragonmaid")
public final class StarterWorldData extends SavedData {
    public static final String ID = "dragonmaid_starting_companion";
    private static final String EGG_CLAIM = "DragonMaidStarterEgg";

    private static final SavedData.Factory<StarterWorldData> FACTORY =
        new SavedData.Factory<>(StarterWorldData::new, StarterWorldData::load);

    private StarterSettings settings = new StarterSettings();
    private UUID owner;
    private UUID dragon;
    private boolean delivered;

    /** Required by {@link SavedData.Factory}; an empty instance is replaced by {@link #load}. */
    public StarterWorldData() {
    }

    public StarterWorldData(StarterSettings settings, UUID owner) {
        this.settings = settings.copy();
        this.owner = owner;
        this.dragon = UUID.randomUUID();
        this.setDirty();
    }

    private static StarterWorldData load(CompoundTag tag, Provider registries) {
        StarterWorldData data = new StarterWorldData();
        data.settings = StarterSettings.load(tag.getCompound("Settings"));
        data.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        data.dragon = tag.hasUUID("Dragon") ? tag.getUUID("Dragon") : UUID.randomUUID();
        data.delivered = tag.getBoolean("Delivered");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, Provider registries) {
        tag.put("Settings", this.settings.save());
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }

        tag.putUUID("Dragon", this.dragon);
        tag.putBoolean("Delivered", this.delivered);
        return tag;
    }

    /** Called from the world-creation path before the level exists (upstream behaviour). */
    public static void writeNewWorld(Path root, StarterSettings settings, UUID owner) throws IOException {
        if (!settings.enabled) {
            return;
        }

        Path folder = root.resolve("data");
        Files.createDirectories(folder);
        Path path = folder.resolve(ID + ".dat");
        if (Files.exists(path)) {
            throw new IOException("Starting companion settings already exist");
        }

        CompoundTag envelope = new CompoundTag();
        envelope.put("data", new StarterWorldData(settings, owner).save(new CompoundTag(), null));
        NbtIo.writeCompressed(envelope, path);
    }

    @SubscribeEvent
    public static void login(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.server.overworld();
        StarterWorldData data = level.getDataStorage().get(FACTORY, ID);
        if (data == null || data.delivered || !data.settings.enabled || data.owner == null || !player.getUUID().equals(data.owner)) {
            return;
        }

        try {
            data.deliver(player);
        } catch (RuntimeException exception) {
            LoggerFactory.getLogger("DragonMaidStarter").error("Starting companion remains pending", exception);
        }
    }

    private void deliver(ServerPlayer player) {
        this.settings.normalize(StarterConfig.limits(false));
        this.settings.resolveRandomAppearance(this.dragon);
        if (this.settings.isEgg()) {
            this.deliverEgg(player);
            return;
        }

        for (ServerLevel level : player.server.getAllLevels()) {
            if (level.getEntity(this.dragon) != null) {
                this.complete();
                return;
            }
        }

        for (ItemStack stack : player.getInventory().items) {
            CompoundTag snapshot = StackNbt.getTagElement(stack, "EntityTag");
            if (snapshot != null && snapshot.hasUUID("UUID") && this.dragon.equals(snapshot.getUUID("UUID"))) {
                this.complete();
                return;
            }
        }

        ServerLevel level = player.serverLevel();
        DragonBaseEntity companion = this.settings.create(level, this.owner);
        companion.setUUID(this.dragon);
        boolean useHorn = this.settings.horn;
        if (!useHorn) {
            useHorn = !this.placeNearby(level, player, companion);
        }

        if (useHorn) {
            if (player.getInventory().getFreeSlot() < 0) {
                player.sendSystemMessage(Component.translatable("starter.dragonmaid.inventory_full"));
                return;
            }

            CompoundTag snapshot = new CompoundTag();
            if (!companion.save(snapshot)) {
                throw new IllegalStateException("Starting companion snapshot failed");
            }

            ItemStack horn = new ItemStack(IafItems.DRAGON_HORN.get());
            CompoundTag hornTag = StackNbt.getTag(horn);
            hornTag.put("EntityTag", snapshot);
            hornTag.putString("DragonHornEntityID", BuiltInRegistries.ENTITY_TYPE.getKey(companion.getType()).toString());
            StackNbt.setTag(horn, hornTag);
            if (RecallHorn.observe(horn, level, player.blockPosition()) == null) {
                throw new IllegalStateException("Starting companion horn could not be registered");
            }

            if (!player.getInventory().add(horn)) {
                throw new IllegalStateException("Starting companion inventory changed");
            }

            player.inventoryMenu.broadcastChanges();
            if (!this.settings.horn) {
                player.sendSystemMessage(Component.translatable("starter.dragonmaid.safe_horn"));
            }
        } else {
            companion.setYRot(player.getYRot());
            companion.yBodyRot = player.getYRot();
            companion.yHeadRot = player.getYRot();
            if (!level.addFreshEntity(companion)) {
                throw new IllegalStateException("Starting companion spawn cancelled");
            }
        }

        this.complete();
        player.sendSystemMessage(Component.translatable("starter.dragonmaid.arrived"));
    }

    /** Spiral search around the player for a safe spot; false when the horn has to be used instead. */
    private boolean placeNearby(ServerLevel level, ServerPlayer player, DragonBaseEntity companion) {
        BlockPos origin = player.blockPosition();
        for (int radius = 3; radius <= 9; radius += 2) {
            for (int side = 0; side < 8; side++) {
                for (int dy = 2; dy >= -3; dy--) {
                    double angle = (double) side * Math.PI / 4.0;
                    BlockPos pos = origin.offset(
                        (int) Math.round(Math.sin(angle) * (double) radius), dy, (int) Math.round(Math.cos(angle) * (double) radius)
                    );
                    if (!level.hasChunkAt(pos) || !level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below())) {
                        continue;
                    }

                    companion.setPos((double) pos.getX() + 0.5, (double) pos.getY(), (double) pos.getZ() + 0.5);
                    if (DragonForms.containedInLoadedWorld(companion, companion.getBoundingBox())
                        && level.noCollision(companion)
                        && !level.containsAnyLiquid(companion.getBoundingBox())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void deliverEgg(ServerPlayer player) {
        CompoundTag claims = player.getPersistentData();
        if (claims.hasUUID(EGG_CLAIM) && this.dragon.equals(claims.getUUID(EGG_CLAIM))) {
            this.complete();
            return;
        }

        if (player.getInventory().getFreeSlot() >= 0 && player.getInventory().add(this.settings.createEgg())) {
            claims.putUUID(EGG_CLAIM, this.dragon);
            this.complete();
            player.inventoryMenu.broadcastChanges();
            player.sendSystemMessage(Component.translatable("starter.dragonmaid.egg_arrived"));
        } else {
            player.sendSystemMessage(Component.translatable("starter.dragonmaid.inventory_full"));
        }
    }

    private void complete() {
        this.delivered = true;
        this.setDirty();
    }
}
