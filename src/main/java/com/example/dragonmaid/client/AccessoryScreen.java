package com.example.dragonmaid.client;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.menu.AccessoryMenu;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 1.2: 龙装备/仓库界面改为 252x232 布局（左侧人形预览、右侧玩家背包、底部手部槽位 71/72）。
 *
 * <p>1.20.1 -> 1.21.1 适配点：
 * <ul>
 *   <li>{@code InventoryScreen.renderEntityInInventoryFollowsMouse} 的签名从
 *       {@code (g, x, y, size, mouseOffsetX, mouseOffsetY, entity)} 变为
 *       {@code (g, x0, y0, x1, y1, size, yOffset, mouseX, mouseY, entity)}：
 *       这里用 1.2 画出的预览框 (8,18)-(72,127) 作为剪刀/定位框，鼠标传绝对坐标。</li>
 *   <li>菜单槽位布局与 1.2 一致：0-4 装备、5-7 饰品、8-34 仓库、35-70 玩家背包、71-72 双手。</li>
 * </ul>
 */
public final class AccessoryScreen extends AbstractContainerScreen<AccessoryMenu> {
    private static final String[] PICKUP_KEYS = new String[]{"all", "off", "matching"};
    private Button knowledge;
    private Button transform;
    private Button pickup;

    public AccessoryScreen(AccessoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 252;
        this.imageHeight = 232;
    }

    protected void init() {
        super.init();
        this.leftPos = Math.max(0, (this.width - this.imageWidth - 66 - 2) / 2);
        this.topPos = Math.max(0, (this.height - this.imageHeight) / 2);
        int i = DragonScreenTabs.x(this.leftPos, this.imageWidth);
        this.addRenderableWidget(
            DragonScreenTabs.button(
                i, this.topPos + 8, "button.dragonmaid.equipment", b -> this.minecraft.gameMode.handleInventoryButtonClick(((AccessoryMenu)this.menu).containerId, 1)
            )
        );
        this.knowledge = (Button)this.addRenderableWidget(
            DragonScreenTabs.button(
                i, this.topPos + 32, "button.dragonmaid.knowledge", b -> this.minecraft.gameMode.handleInventoryButtonClick(((AccessoryMenu)this.menu).containerId, 2)
            )
        );
        this.addRenderableWidget(DragonScreenTabs.currentPage(i, this.topPos + 56, "button.dragonmaid.accessories"));
        this.transform = (Button)this.addRenderableWidget(
            DragonScreenTabs.button(
                i, this.topPos + 96, "button.dragonmaid.to_human", b -> this.minecraft.gameMode.handleInventoryButtonClick(((AccessoryMenu)this.menu).containerId, 3)
            )
        );
        this.pickup = (Button)this.addRenderableWidget(
            DragonScreenTabs.button(
                this.leftPos + 136,
                this.topPos + 40,
                "button.dragonmaid.pickup.off",
                b -> this.minecraft.gameMode.handleInventoryButtonClick(((AccessoryMenu)this.menu).containerId, 4)
            )
        );
        this.pickup.setWidth(106);
        this.updateButtons();
    }

    private void updateButtons() {
        this.knowledge.active = this.minecraft != null && DragonForms.canManage(((AccessoryMenu)this.menu).dragon(), this.minecraft.player);
        this.knowledge.setTooltip(this.knowledge.active ? null : Tooltip.create(Component.translatable("message.dragonmaid.cannot_manage")));
        DragonScreenTabs.updateTransform(this.transform, ((AccessoryMenu)this.menu).dragon());
        this.pickup.active = ((AccessoryMenu)this.menu).storageSlots() > 0;
        int i = ((AccessoryMenu)this.menu).pickupMode();
        this.pickup.setMessage(Component.translatable("button.dragonmaid.pickup." + PICKUP_KEYS[i >= 0 && i < PICKUP_KEYS.length ? i : 1]));
    }

    protected void containerTick() {
        super.containerTick();
        this.updateButtons();
    }

    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        DragonPanel.background(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        g.fill(this.leftPos + 7, this.topPos + 17, this.leftPos + 73, this.topPos + 128, -13158601);
        g.fill(this.leftPos + 8, this.topPos + 18, this.leftPos + 72, this.topPos + 127, -15263977);
        DragonBaseEntity entitydragonbase = ((AccessoryMenu)this.menu).dragon();
        if (entitydragonbase != null) {
            g.enableScissor(this.leftPos + 8, this.topPos + 18, this.leftPos + 72, this.topPos + 127);
            int i = DragonForms.isHumanoid(entitydragonbase) ? 31 : Math.max(1, (int)(6.8F / Math.max(0.4F, entitydragonbase.getScale())));
            // 1.21.1: the method now takes the preview box (x0,y0,x1,y1) + absolute mouse coords
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                g,
                this.leftPos + 8,
                this.topPos + 18,
                this.leftPos + 72,
                this.topPos + 127,
                i,
                0.0F,
                (float)mouseX,
                (float)mouseY,
                entitydragonbase
            );
            g.disableScissor();
        }

        for (int j = 0; j < 5; j++) {
            Slot slot = ((AccessoryMenu)this.menu).getSlot(j);
            DragonPanel.gearSlot(g, this.leftPos + slot.x, this.topPos + slot.y, j, !slot.hasItem());
        }

        g.drawManaged(() -> {
            for (int k = 5; k < 35; k++) {
                Slot slot = ((AccessoryMenu)this.menu).getSlot(k);
                int l = this.leftPos + slot.x;
                int i1 = this.topPos + slot.y;
                if (!slot.isActive()) {
                    DragonPanel.lockedSlot(g, l, i1);
                } else if (k < 8) {
                    DragonPanel.accessorySlot(g, l, i1, !slot.hasItem());
                } else {
                    DragonPanel.slot(g, l, i1);
                }
            }

            for (int k = 35; k < 71; k++) {
                Slot slot = ((AccessoryMenu)this.menu).getSlot(k);
                DragonPanel.slot(g, this.leftPos + slot.x, this.topPos + slot.y);
            }

            for (int k = 71; k < 73; k++) {
                Slot slot = ((AccessoryMenu)this.menu).getSlot(k);
                int l = this.leftPos + slot.x;
                int i1 = this.topPos + slot.y;
                if (!slot.isActive()) {
                    DragonPanel.lockedSlot(g, l, i1);
                } else {
                    DragonPanel.slot(g, l, i1);
                    if (!slot.hasItem()) {
                        drawHandOutline(g, l, i1, k == 71);
                    }
                }
            }
        });
    }

    private static void drawHandOutline(GuiGraphics g, int x, int y, boolean main) {
        int i = -10329502;
        if (main) {
            for (int j = 2; j <= 12; j++) {
                g.fill(x + j, y + 14 - j, x + j + 2, y + 16 - j, i);
            }

            g.fill(x + 3, y + 8, x + 5, y + 10, i);
            g.fill(x + 7, y + 12, x + 9, y + 14, i);
        } else {
            g.fill(x + 3, y + 2, x + 13, y + 4, i);
            g.fill(x + 3, y + 4, x + 5, y + 10, i);
            g.fill(x + 11, y + 4, x + 13, y + 10, i);
            g.fill(x + 5, y + 10, x + 7, y + 12, i);
            g.fill(x + 9, y + 10, x + 11, y + 12, i);
            g.fill(x + 7, y + 12, x + 9, y + 14, i);
        }
    }

    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 8, 6, 4210752, false);
        Component component = Component.translatable("label.dragonmaid.dragon_storage")
            .append(" \u00b7 " + ((AccessoryMenu)this.menu).storageSlots() + " / 27");
        DragonPanel.centered(g, this.font, component, 162, 63, 160, 4210752);
        g.drawString(this.font, this.playerInventoryTitle, 82, 138, 4210752, false);
        DragonPanel.centered(
            g,
            this.font,
            Component.translatable("label.dragonmaid.accessory_knowledge", new Object[]{((AccessoryMenu)this.menu).knowledgeLevel()}),
            40,
            141,
            64,
            4210752
        );
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        this.renderTooltip(g, mouseX, mouseY);

        for (int i = ((AccessoryMenu)this.menu).unlockedSlots(); i < 3; i++) {
            Slot slot = ((AccessoryMenu)this.menu).getSlot(5 + i);
            if (DragonPanel.hovering(mouseX, mouseY, this.leftPos + slot.x, this.topPos + slot.y)) {
                g.renderTooltip(this.font, Component.translatable("label.dragonmaid.accessory_locked", new Object[]{i + 1}), mouseX, mouseY);
            }
        }

        for (int j = ((AccessoryMenu)this.menu).storageSlots(); j < 27; j++) {
            Slot slot = ((AccessoryMenu)this.menu).getSlot(8 + j);
            if (DragonPanel.hovering(mouseX, mouseY, this.leftPos + slot.x, this.topPos + slot.y)) {
                g.renderTooltip(this.font, Component.translatable("label.dragonmaid.storage_locked"), mouseX, mouseY);
            }
        }

        for (int k = 71; k < 73; k++) {
            Slot slot = ((AccessoryMenu)this.menu).getSlot(k);
            if (!slot.hasItem() && DragonPanel.hovering(mouseX, mouseY, this.leftPos + slot.x, this.topPos + slot.y)) {
                MutableComponent mutablecomponent = Component.translatable(k == 71 ? "label.dragonmaid.hand_weapon" : "label.dragonmaid.hand_shield");
                if (!slot.isActive()) {
                    mutablecomponent.append(" \u00b7 ").append(Component.translatable("label.dragonmaid.hands_locked"));
                }

                g.renderTooltip(this.font, mutablecomponent, mouseX, mouseY);
            }
        }
    }
}
