package com.example.dragonmaid.recall;

import com.example.dragonmaid.mixin.RecallEntityManagerAccessor;
import com.example.dragonmaid.mixin.RecallServerLevelAccessor;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;

public final class RecallPersistence {
    private RecallPersistence() {
    }

    public static void saveEntityChunk(ServerLevel level, ChunkPos chunk) {
        RecallEntityManagerAccessor recallentitymanageraccessor = (RecallEntityManagerAccessor)((RecallServerLevelAccessor)level).dragonmaid$entityManager();
        if (!recallentitymanageraccessor.dragonmaid$storeChunk(chunk.toLong(), ignored -> {
        })) {
            throw new IllegalStateException("Recall destination entities are not loaded yet");
        } else {
            recallentitymanageraccessor.dragonmaid$permanentStorage().flush(true);
        }
    }

    static void checkpoint(ServerLevel storageLevel, RecallLedger.Entry entry) {
        ServerLevel serverlevel = storageLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(entry.dimension())));
        if (serverlevel == null) {
            throw new IllegalStateException("Recall destination dimension is missing");
        } else {
            Entity entity = serverlevel.getEntity(entry.holder());
            if (entity != null && RecallTracking.matches(entity, entry)) {
                saveEntityChunk(serverlevel, entity.chunkPosition());
                RecallEntityManagerAccessor recallentitymanageraccessor = (RecallEntityManagerAccessor)((RecallServerLevelAccessor)serverlevel)
                    .dragonmaid$entityManager();
                Object storage = recallentitymanageraccessor.dragonmaid$permanentStorage();
                SimpleRegionStorage simpleregionstorage = dragonmaid$simpleRegionStorage(storage);
                CompoundTag compoundtag;
                try {
                    // 1.21.1: EntityStorage is NOT a ChunkStorage; it owns a SimpleRegionStorage whose
                    // synchronize()/read() are public. Block until the entity chunk is really on disk.
                    simpleregionstorage.synchronize(false).join();
                    compoundtag = (CompoundTag)simpleregionstorage.read(entity.chunkPosition()).join()
                        .orElseThrow(() -> new IllegalStateException("Recall destination entity chunk was not saved"));
                } catch (Throwable throwable) {
                    throw new IllegalStateException("Recall destination entity chunk was not saved", throwable);
                }

                for (Tag tag : compoundtag.getList("Entities", 10)) {
                    CompoundTag compoundtag1 = (CompoundTag)tag;
                    if (compoundtag1.hasUUID("UUID") && compoundtag1.getUUID("UUID").equals(entry.holder()) && RecallTracking.matches(compoundtag1, entry)) {
                        return;
                    }
                }

                throw new IllegalStateException("Recall destination generation was not saved");
            } else {
                throw new IllegalStateException("Recall destination entity is not authoritative");
            }
        }
    }

    /**
     * Resolve the {@code SimpleRegionStorage} owned by a {@code EntityStorage} instance.
     * 1.21.1 removed the direct {@code IOWorker} field here (it now lives inside SimpleRegionStorage),
     * and the runtime field name is mapping-dependent — so the field is looked up by type name.
     */
    private static SimpleRegionStorage dragonmaid$simpleRegionStorage(Object storage) {
        for (Class<?> type = storage.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (SimpleRegionStorage.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return (SimpleRegionStorage)field.get(storage);
                    } catch (ReflectiveOperationException reflectiveoperationexception) {
                        throw new IllegalStateException("Recall region storage is not accessible", reflectiveoperationexception);
                    }
                }
            }
        }

        throw new IllegalStateException("Recall region storage field was not found in " + storage.getClass().getName());
    }
}
