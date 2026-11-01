package com.github.alexthe666.iceandfire.entity.props;

import java.util.Collections;
import java.util.List;
import net.minecraft.world.entity.Entity;

/** Compatibility stub: IAF CE dropped Citadel entity data, so these hooks are inert. */
public class EntityData {
    public final FrozenData frozenData = new FrozenData();
    public final ChainData chainData = new ChainData();

    public static class FrozenData {
        public void setFrozen(Entity entity, int ticks) { }
    }

    public static class ChainData {
        public List<Entity> getChainedTo() { return Collections.emptyList(); }
    }
}
