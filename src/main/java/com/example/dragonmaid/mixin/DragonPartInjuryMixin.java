package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.MultipartPartEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 1.21.1 / IAF CE 2.2 port.
 *
 * <p>IAF CE 2.2 changed {@code MultipartPartEntity} from a plain {@code Entity} into a
 * NeoForge {@link PartEntity}: the parent dragon is now held as a direct field
 * ({@code PartEntity#getParent()}) and the old synced {@code getParentId()} UUID was removed.
 * The mixin therefore uses the PartEntity API instead of shadowing methods that no longer
 * live on the target class.</p>
 */
@Mixin(remap = false, value = {MultipartPartEntity.class})
public abstract class DragonPartInjuryMixin extends Entity {
    protected DragonPartInjuryMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    public boolean canBeHitByProjectile() {
        Entity entity = (Object) this instanceof PartEntity<?> part ? part.getParent() : null;

        if (entity instanceof DragonBaseEntity dragon
            && HumanoidLife.isInjured(dragon)
            && dragon.getHealth() <= 0.0F) {
            return false;
        }

        return super.canBeHitByProjectile();
    }
}
