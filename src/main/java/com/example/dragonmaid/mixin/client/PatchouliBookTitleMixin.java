package com.example.dragonmaid.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.gui.GuiBookEntryList;
import vazkii.patchouli.common.book.Book;

@Pseudo
@Mixin(remap = false, value = {GuiBook.class})
public abstract class PatchouliBookTitleMixin {
    private static final ResourceLocation DRAGON_MAID_GUIDE = ResourceLocation.fromNamespaceAndPath("dragonmaid", "dragon_maid_guide");
    private static final float MAX_TITLE_WIDTH = 112.0F;
    @Shadow(remap = false)
    @Final
    public Book book;

    public PatchouliBookTitleMixin() {
    }

    private boolean dragonmaid$isHeading(int x, int y) {
        if (!DRAGON_MAID_GUIDE.equals(this.book.id)) {
            return false;
        } else {
            return ((Object)this) instanceof GuiBookEntry
                ? x == 58 && (y == 0 || y == -3 || y == -6) || x == 0 && y == 0
                : ((Object)this) instanceof GuiBookEntryList && y == 18 && (x == 73 || x == 199);
        }
    }

    @Inject(remap = false, method = {"drawCenteredStringNoShadow(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;III)V"},
        at = {@At("HEAD")},
        cancellable = true,
        require = 0)
    private void dragonmaid$fitString(GuiGraphics graphics, String text, int x, int y, int color, CallbackInfo ci) {
        Font font = Minecraft.getInstance().font;
        int i = font.width(text);
        if (this.dragonmaid$isHeading(x, y) && !((float)i <= 112.0F)) {
            float f = 112.0F / (float)i;
            graphics.pose().pushPose();

            try {
                graphics.pose().translate((float)x, (float)y, 0.0F);
                graphics.pose().scale(f, f, 1.0F);
                graphics.drawString(font, text, -i / 2, 0, color, false);
            } finally {
                graphics.pose().popPose();
            }

            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"drawCenteredStringNoShadow(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/util/FormattedCharSequence;III)V"},
        at = {@At("HEAD")},
        cancellable = true,
        require = 0)
    private void dragonmaid$fitFormatted(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color, CallbackInfo ci) {
        Font font = Minecraft.getInstance().font;
        int i = font.width(text);
        if (this.dragonmaid$isHeading(x, y) && !((float)i <= 112.0F)) {
            float f = 112.0F / (float)i;
            graphics.pose().pushPose();

            try {
                graphics.pose().translate((float)x, (float)y, 0.0F);
                graphics.pose().scale(f, f, 1.0F);
                graphics.drawString(font, text, -i / 2, 0, color, false);
            } finally {
                graphics.pose().popPose();
            }

            ci.cancel();
        }
    }
}
