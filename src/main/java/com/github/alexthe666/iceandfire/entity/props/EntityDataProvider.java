package com.github.alexthe666.iceandfire.entity.props;

import java.util.Optional;
import net.minecraft.world.entity.Entity;

public final class EntityDataProvider {
    private static final EntityData EMPTY = new EntityData();

    private EntityDataProvider() { }

    public static Optional<EntityData> getCapability(Entity entity) {
        return Optional.of(EMPTY);
    }
}
