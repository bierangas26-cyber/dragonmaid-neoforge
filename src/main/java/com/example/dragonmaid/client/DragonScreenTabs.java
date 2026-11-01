package com.example.dragonmaid.client;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;

public final class DragonScreenTabs {
    public static final int WIDTH = 66;

    private DragonScreenTabs() {
    }

    public static int x(int panelLeft, int panelWidth) {
        return panelLeft + panelWidth + 2;
    }

    public static Button button(int x, int y, String label, OnPress action) {
        return new DragonScreenTabs.PageButton(x, y, Component.translatable(label), action);
    }

    public static Button currentPage(int x, int y, String label) {
        Button button = button(x, y, label, ignored -> {
        });
        button.active = false;
        return button;
    }

    public static void updateTransform(Button button, DragonBaseEntity dragon) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean flag = dragon != null && DragonForms.isHumanoid(dragon);
        button.setMessage(Component.translatable(flag ? "button.dragonmaid.to_dragon" : "button.dragonmaid.to_human"));
        boolean flag1 = DragonForms.canManage(dragon, minecraft.player);
        boolean flag2 = flag || dragon != null && DragonForms.unlocked(dragon);
        button.active = flag1 && flag2;
        button.setTooltip(
            !flag1
                ? Tooltip.create(Component.translatable("message.dragonmaid.cannot_manage"))
                : (!flag2 ? Tooltip.create(Component.translatable("message.dragonmaid.locked")) : null)
        );
    }

    public static class PageButton extends Button {
        public PageButton(int x, int y, Component label, OnPress action) {
            super(x, y, 66, 20, label, action, DEFAULT_NARRATION);
        }

        public void renderString(GuiGraphics g, Font font, int color) {
            DragonPanel.centered(g, font, this.getMessage(), this.getX() + this.width / 2, this.getY() + 6, this.width - 8, color);
        }
    }
}
