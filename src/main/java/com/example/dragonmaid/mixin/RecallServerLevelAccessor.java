package com.example.dragonmaid.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(remap = false, value = {ServerLevel.class})
public interface RecallServerLevelAccessor {
    @Accessor(remap = false, value = "entityManager")
    PersistentEntitySectionManager<Entity> dragonmaid$entityManager();
}
