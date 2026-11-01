package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.data.InjuryRecovery;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, value = {DragonBaseEntity.class})
public abstract class DragonInjuryMixin extends TamableAnimal {
    protected DragonInjuryMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Inject(remap = false, method = {"die"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$fallInsteadOfDying(DamageSource source, CallbackInfo ci) {
        DragonBaseEntity entitydragonbase = (DragonBaseEntity)(Object)this;
        if (HumanoidLife.isInjured(entitydragonbase)
            || entitydragonbase.isTame() && !entitydragonbase.isModelDead() && !DragonForms.state(entitydragonbase).dragonmaid$isExecuted()) {
            this.setHealth(0.0F);
            entitydragonbase.setModelDead(true);
            entitydragonbase.ejectPassengers();
            entitydragonbase.setTarget(null);
            entitydragonbase.getNavigation().stop();
            this.dead = false;
            this.deathTime = 0;
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"tickDeath"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$preserveInjury(CallbackInfo ci) {
        if (HumanoidLife.isInjured((DragonBaseEntity)(Object)this)) {
            this.deathTime = 0;
            this.ejectPassengers();
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"hurt"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$zeroHealthProtection(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (HumanoidLife.isInjured((DragonBaseEntity)(Object)this) && this.getHealth() <= 0.0F) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(remap = false, method = {"hurt", "isAlliedTo"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;isModelDead()Z"
        ))
    private boolean dragonmaid$damageHealingDragon(DragonBaseEntity dragon) {
        return dragon.isModelDead() && (!HumanoidLife.isInjured(dragon) || !(this.getHealth() > 0.0F));
    }

    public void heal(float amount) {
        if (HumanoidLife.isInjured((DragonBaseEntity)(Object)this)) {
            if (!this.level().isClientSide) {
                float f = EventHooks.onLivingHeal(this, amount);
                if (f > 0.0F) {
                    this.setHealth(this.getHealth() + f);
                }
            }
        } else {
            super.heal(amount);
        }
    }

    public boolean canBeHitByProjectile() {
        return (!HumanoidLife.isInjured((DragonBaseEntity)(Object)this) || this.getHealth() > 0.0F) && super.canBeHitByProjectile();
    }

    @Inject(remap = false, method = {"growDragon"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$noGrowthWhileInjured(int days, CallbackInfo ci) {
        if (HumanoidLife.isInjured((DragonBaseEntity)(Object)this)) {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"tick"},
        at = {@At("TAIL")})
    private void dragonmaid$recover(CallbackInfo ci) {
        if (!this.level().isClientSide) {
            InjuryRecovery.tick((DragonBaseEntity)(Object)this);
        }
    }
}
