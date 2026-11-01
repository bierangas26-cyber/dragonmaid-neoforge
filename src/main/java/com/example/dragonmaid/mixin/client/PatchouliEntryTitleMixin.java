package com.example.dragonmaid.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.button.GuiButtonEntry;

@Pseudo
@Mixin(remap = false, value = {GuiButtonEntry.class})
public abstract class PatchouliEntryTitleMixin {
    private static final ResourceLocation DRAGON_MAID_GUIDE = ResourceLocation.fromNamespaceAndPath("dragonmaid", "dragon_maid_guide");
    private static final float MAX_ENTRY_WIDTH = 96.0F;
    @Shadow(remap = false)
    @Final
    private GuiBook parent;

    public PatchouliEntryTitleMixin() {
    }

    @Redirect(remap = false, method = {"renderWidget"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"
            ),
        require = 0)
    private int dragonmaid$fitEntryTitle(GuiGraphics graphics, Font font, Component text, int x, int y, int color, boolean shadow) {
        int i = font.width(text);
        if (DRAGON_MAID_GUIDE.equals(this.parent.book.id) && !((float)i <= 96.0F)) {
            float f = 96.0F / (float)i;
            graphics.pose().pushPose();

            int j;
            try {
                graphics.pose().translate((float)x, (float)y, 0.0F);
                graphics.pose().scale(f, f, 1.0F);
                j = graphics.drawString(font, text, 0, 0, color, shadow);
            } finally {
                graphics.pose().popPose();
            }

            return j;
        } else {
            return graphics.drawString(font, text, x, y, color, shadow);
        }
    }
}
