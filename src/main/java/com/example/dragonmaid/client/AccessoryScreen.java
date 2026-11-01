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

public final class AccessoryScreen extends AbstractContainerScreen<AccessoryMenu> {
    private Button knowledge;
    private Button transform;

    public AccessoryScreen(AccessoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 214;
    }

    protected void init() {
        super.init();
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
        this.updateButtons();
    }

    private void updateButtons() {
        this.knowledge.active = this.minecraft != null && DragonForms.canManage(((AccessoryMenu)this.menu).dragon(), this.minecraft.player);
        this.knowledge.setTooltip(this.knowledge.active ? null : Tooltip.create(Component.translatable("message.dragonmaid.cannot_manage")));
        DragonScreenTabs.updateTransform(this.transform, ((AccessoryMenu)this.menu).dragon());
    }

    protected void containerTick() {
        super.containerTick();
        this.updateButtons();
    }

    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        DragonPanel.background(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        g.fill(this.leftPos + 29, this.topPos + 17, this.leftPos + 144, this.topPos + 95, -13158601);
        g.fill(this.leftPos + 30, this.topPos + 18, this.leftPos + 143, this.topPos + 94, -15263977);
        DragonBaseEntity entitydragonbase = ((AccessoryMenu)this.menu).dragon();
        if (entitydragonbase != null) {
            g.enableScissor(this.leftPos + 30, this.topPos + 18, this.leftPos + 143, this.topPos + 94);
            int i = DragonForms.isHumanoid(entitydragonbase) ? 28 : Math.max(1, (int)(9.2F / Math.max(0.4F, entitydragonbase.getScale())));
            // 1.21.1: the method now takes the preview box (x0,y0,x1,y1) + absolute mouse coords
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                g,
                this.leftPos + 30,
                this.topPos + 18,
                this.leftPos + 143,
                this.topPos + 94,
                i,
                0.0F,
                (float)mouseX,
                (float)mouseY,
                entitydragonbase
            );
            g.disableScissor();
        }

        for (int l = 0; l < 5; l++) {
            Slot slot = (Slot)((AccessoryMenu)this.menu).slots.get(l);
            int j = this.leftPos + slot.x;
            int k = this.topPos + slot.y;
            DragonPanel.gearSlot(g, j, k, l, !slot.hasItem());
        }

        g.drawManaged(() -> {
            for (int i1 = 5; i1 < 35; i1++) {
                Slot slot1 = (Slot)((AccessoryMenu)this.menu).slots.get(i1);
                int j1 = this.leftPos + slot1.x;
                int k1 = this.topPos + slot1.y;
                if (!slot1.isActive()) {
                    DragonPanel.lockedSlot(g, j1, k1);
                } else if (i1 < 8) {
                    DragonPanel.accessorySlot(g, j1, k1, !slot1.hasItem());
                } else {
                    DragonPanel.slot(g, j1, k1);
                }
            }
        });
    }

    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 8, 6, 4210752, false);
        DragonPanel.centered(g, this.font, Component.translatable("label.dragonmaid.dragon_storage"), 88, 105, 160, 4210752);
        MutableComponent mutablecomponent = Component.literal(((AccessoryMenu)this.menu).storageSlots() + " / 27");
        if (((AccessoryMenu)this.menu).storageSlots() == 0) {
            mutablecomponent.append(" \u00b7 ").append(Component.translatable("label.dragonmaid.storage_locked"));
        }

        DragonPanel.centered(g, this.font, mutablecomponent, 88, 197, 160, 4210752);
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
            Slot slot1 = ((AccessoryMenu)this.menu).getSlot(8 + j);
            if (DragonPanel.hovering(mouseX, mouseY, this.leftPos + slot1.x, this.topPos + slot1.y)) {
                g.renderTooltip(this.font, Component.translatable("label.dragonmaid.storage_locked"), mouseX, mouseY);
            }
        }
    }
}
