package com.example.dragonmaid.mixin;

import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(remap = false, value = {PersistentEntitySectionManager.class})
public interface RecallEntityManagerAccessor {
    @Invoker(remap = false, value = "storeChunkSections")
    boolean dragonmaid$storeChunk(long var1, Consumer<Entity> var3);

    @Accessor(remap = false, value = "permanentStorage")
    EntityPersistentStorage<Entity> dragonmaid$permanentStorage();
}
