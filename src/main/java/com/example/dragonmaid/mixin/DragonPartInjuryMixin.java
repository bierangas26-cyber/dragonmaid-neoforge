package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.MultipartPartEntity;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(remap = false, value = {MultipartPartEntity.class})
public abstract class DragonPartInjuryMixin extends Entity {
    protected DragonPartInjuryMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow(remap = false)
    public abstract Entity getParent();

    @Shadow(remap = false)
    public abstract UUID getParentId();

    public boolean canBeHitByProjectile() {
        Entity entity = this.getParent();
        if (entity == null && this.level().isClientSide && this.getParentId() != null) {
            Iterator dragon = this.level()
                .getEntitiesOfClass(DragonBaseEntity.class, this.getBoundingBox().inflate(64.0), dragonx -> dragonx.getUUID().equals(this.getParentId()))
                .iterator();
            if (dragon.hasNext()) {
                DragonBaseEntity entitydragonbase = (DragonBaseEntity)dragon.next();
                entity = entitydragonbase;
            }
        }

        if (entity instanceof DragonBaseEntity entitydragonbase1 && HumanoidLife.isInjured(entitydragonbase1) && entitydragonbase1.getHealth() <= 0.0F) {
            return false;
        }

        return super.canBeHitByProjectile();
    }
}
