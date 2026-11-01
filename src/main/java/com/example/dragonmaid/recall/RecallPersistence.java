package com.example.dragonmaid.recall;

import com.example.dragonmaid.mixin.RecallEntityManagerAccessor;
import com.example.dragonmaid.mixin.RecallEntityStorageAccessor;
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
                RecallEntityStorageAccessor recallentitystorageaccessor = (RecallEntityStorageAccessor)recallentitymanageraccessor.dragonmaid$permanentStorage();
                CompoundTag compoundtag;
                try {
                    compoundtag = (CompoundTag)((Optional)recallentitystorageaccessor.dragonmaid$entityWorker().loadAsync(entity.chunkPosition()).join())
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
}
