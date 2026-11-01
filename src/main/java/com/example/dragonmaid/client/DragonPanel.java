package com.example.dragonmaid.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class DragonPanel {
    private static final ResourceLocation DRAGON_GUI = ResourceLocation.fromNamespaceAndPath("iceandfire", "textures/gui/dragon.png");
    private static final int[][] GEAR_UV = new int[][]{{8, 54}, {8, 18}, {8, 36}, {153, 18}, {153, 36}};

    private DragonPanel() {
    }

    public static void background(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, -15724528);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, -11184811);
        g.fill(x + 2, y + 2, x + width - 2, y + height - 2, -3750202);
        g.fill(x + 2, y + 2, x + width - 2, y + 3, -1);
        g.fill(x + 2, y + 2, x + 3, y + height - 2, -1);
    }

    public static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, -13158601);
        g.fill(x, y, x + 17, y + 17, -1);
        g.fill(x, y, x + 16, y + 16, -7631989);
    }

    public static void gearSlot(GuiGraphics g, int x, int y, int index, boolean empty) {
        slot(g, x, y);
        if (empty && index >= 0 && index < GEAR_UV.length) {
            int[] aint = GEAR_UV[index];
            g.blit(DRAGON_GUI, x, y, (float)aint[0], (float)aint[1], 16, 16, 256, 256);
        }
    }

    public static void accessorySlot(GuiGraphics g, int x, int y, boolean empty) {
        slot(g, x, y);
        if (empty) {
            int i = -10329502;
            int j = -5395027;
            g.fill(x + 5, y + 2, x + 11, y + 3, i);
            g.fill(x + 4, y + 3, x + 5, y + 7, i);
            g.fill(x + 11, y + 3, x + 12, y + 7, i);
            g.fill(x + 5, y + 7, x + 6, y + 9, i);
            g.fill(x + 10, y + 7, x + 11, y + 9, i);
            g.fill(x + 6, y + 9, x + 10, y + 10, i);
            g.fill(x + 6, y + 10, x + 10, y + 13, i);
            g.fill(x + 7, y + 13, x + 9, y + 14, i);
            g.fill(x + 5, y + 3, x + 6, y + 6, j);
            g.fill(x + 10, y + 3, x + 11, y + 6, j);
            g.fill(x + 7, y + 11, x + 9, y + 12, j);
        }
    }

    public static void lockedSlot(GuiGraphics g, int x, int y) {
        slot(g, x, y);
        g.fill(x, y, x + 16, y + 16, -864849037);
        g.fill(x + 5, y + 3, x + 11, y + 5, -4473925);
        g.fill(x + 4, y + 4, x + 6, y + 9, -4473925);
        g.fill(x + 10, y + 4, x + 12, y + 9, -4473925);
        g.fill(x + 3, y + 8, x + 13, y + 14, -11184811);
        g.fill(x + 4, y + 8, x + 12, y + 13, -5592406);
        g.fill(x + 7, y + 10, x + 9, y + 12, -11184811);
    }

    public static void centered(GuiGraphics g, Font font, Component text, int centerX, int y, int maxWidth, int color) {
        float f = Math.min(1.0F, (float)maxWidth / (float)Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate((float)centerX, (float)y + (1.0F - f) * 4.0F, 0.0F);
        g.pose().scale(f, f, 1.0F);
        g.drawString(font, text, -font.width(text) / 2, 0, color, false);
        g.pose().popPose();
    }

    public static boolean hovering(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }
}
