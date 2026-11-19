package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.menu.DragonPanelInteraction;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.MultipartPartEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.2.7: 潜行右键自家龙（含多部件身体的任一段）打开"龙面板"。
 *
 * <p>IAF CE 2.2 把多部件身体换成了 NeoForge {@link PartEntity}
 * （{@code MultipartPartEntity extends PartEntity}），原先的
 * {@code DragonForms.multipartParent(EntityMutlipartPart)} 不再需要：父实体直接
 * 走 {@link PartEntity#getParent()}。判断条件与上游一致：创造模式 + 潜行 + 双手空。</p>
 */
@Mixin({Player.class})
public abstract class PlayerDragonPanelMixin {
    @Inject(method = {"interactOn"}, at = {@At("TAIL")}, cancellable = true)
    private void dragonmaid$handleCompanionClick(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> result) {
        if (result.getReturnValue() != InteractionResult.PASS) {
            return;
        }

        Player player = (Player) (Object) this;
        if (entity instanceof DragonBaseEntity dragon) {
            result.setReturnValue(DragonPanelInteraction.afterPass(dragon, player, InteractionResult.PASS));
            return;
        }

        if (entity instanceof MultipartPartEntity part
            && player.level().isClientSide()
            && player.isShiftKeyDown()
            && player.getMainHandItem().isEmpty()
            && player.getOffhandItem().isEmpty()
            && part.getParent() instanceof DragonBaseEntity dragon
            && !DragonForms.isHumanoid(dragon)) {
            result.setReturnValue(DragonPanelInteraction.afterPass(dragon, player, InteractionResult.PASS));
        }
    }
}
