package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.screen.gui.DragonScreen;
import com.iafenvoy.iceandfire.screen.menu.DragonMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The dragon-details GUI previews the dragon through vanilla
 * {@code InventoryScreen.renderEntityInInventory}, whose scale parameter is tuned for the large
 * dragon model. Humanoid-form dragons render with a player-sized model, so they appeared tiny
 * (~10x20 px) in the preview box.
 *
 * This scales the preview up for humanoid dragons by the ratio between the dragon's hitbox height
 * and a player-sized model, so the humanoid fills the same box the dragon form fills.
 */
@Mixin(remap = false, value = {DragonScreen.class})
public abstract class DragonPreviewScaleMixin extends AbstractContainerScreen<DragonMenu> {
    protected DragonPreviewScaleMixin(DragonMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyArg(
        remap = false,
        method = {"renderBg"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V"
        ),
        index = 3
    )
    private float dragonmaid$scaleHumanoidPreview(float scale) {
        DragonBaseEntity dragon = this.menu == null ? null : this.menu.getDragon();
        if (dragon != null && DragonForms.isHumanoid(dragon)) {
            float factor = Mth.clamp(dragon.getBbHeight() / 1.9F, 1.0F, 12.0F);
            return scale * factor;
        }
        return scale;
    }
}
