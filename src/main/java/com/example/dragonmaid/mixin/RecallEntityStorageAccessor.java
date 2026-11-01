package com.example.dragonmaid.mixin;

import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(remap = false, value = {ChunkStorage.class})
public interface RecallEntityStorageAccessor {
    @Accessor(remap = false, value = "worker")
    IOWorker dragonmaid$entityWorker();
}
