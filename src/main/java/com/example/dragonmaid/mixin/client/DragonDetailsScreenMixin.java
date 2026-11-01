package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.DragonPanel;
import com.example.dragonmaid.menu.DragonDetailsMenu;
import com.iafenvoy.iceandfire.screen.gui.DragonScreen;
import com.iafenvoy.iceandfire.screen.menu.DragonMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * IAF CE 2.0：GuiDragon → DragonScreen（renamed/moved）。
 * CE 的 DragonScreen.renderBg 直接用 ((DragonMenu)this.menu).getDragon() 取龙，
 * 不再有 CommonProxy.getReferencedMob()，因此 1.20.1 的 @Redirect 兼容块整体删除。
 */
@Mixin(remap = false, value = {DragonScreen.class})
public abstract class DragonDetailsScreenMixin extends AbstractContainerScreen<DragonMenu> {
    protected DragonDetailsScreenMixin(DragonMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(remap = false, method = {"renderBg"},
        at = {@At("TAIL")})
    private void dragonmaid$drawAccessorySlots(GuiGraphics graphics, float partial, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.menu instanceof DragonDetailsMenu) {
            graphics.drawManaged(() -> {
                for (int i = 41; i < 44; i++) {
                    Slot slot = ((DragonMenu)this.menu).getSlot(i);
                    int j = this.leftPos + slot.x;
                    int k = this.topPos + slot.y;
                    if (slot.isActive()) {
                        DragonPanel.accessorySlot(graphics, j, k, !slot.hasItem());
                    } else {
                        DragonPanel.lockedSlot(graphics, j, k);
                    }
                }
            });
        }
    }
}
