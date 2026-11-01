package com.example.dragonmaid.client;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.menu.KnowledgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class KnowledgeScreen extends AbstractContainerScreen<KnowledgeMenu> {
    private Button learn;
    private Button upgrade;
    private Button transform;

    public KnowledgeScreen(KnowledgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 228;
        this.imageHeight = 230;
        this.inventoryLabelX = 34;
        this.inventoryLabelY = 132;
    }

    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth - 66 - 2) / 2;
        this.learn = (Button)this.addRenderableWidget(
            Button.builder(Component.translatable("button.dragonmaid.learn"), b -> this.minecraft.gameMode.handleInventoryButtonClick(((KnowledgeMenu)this.menu).containerId, 0))
                .bounds(this.leftPos + 36, this.topPos + 49, 77, 20)
                .build()
        );
        this.upgrade = (Button)this.addRenderableWidget(
            Button.builder(
                    Component.translatable("button.dragonmaid.upgrade"), b -> this.minecraft.gameMode.handleInventoryButtonClick(((KnowledgeMenu)this.menu).containerId, 4)
                )
                .bounds(this.leftPos + 102, this.topPos + 103, 118, 20)
                .build()
        );
        int i = DragonScreenTabs.x(this.leftPos, this.imageWidth);
        this.addRenderableWidget(
            DragonScreenTabs.button(
                i, this.topPos + 8, "button.dragonmaid.equipment", b -> this.minecraft.gameMode.handleInventoryButtonClick(((KnowledgeMenu)this.menu).containerId, 1)
            )
        );
        this.addRenderableWidget(DragonScreenTabs.currentPage(i, this.topPos + 32, "button.dragonmaid.knowledge"));
        this.addRenderableWidget(
            DragonScreenTabs.button(
                i, this.topPos + 56, "button.dragonmaid.accessories", b -> this.minecraft.gameMode.handleInventoryButtonClick(((KnowledgeMenu)this.menu).containerId, 2)
            )
        );
        this.transform = (Button)this.addRenderableWidget(
            DragonScreenTabs.button(
                i, this.topPos + 96, "button.dragonmaid.to_human", b -> this.minecraft.gameMode.handleInventoryButtonClick(((KnowledgeMenu)this.menu).containerId, 3)
            )
        );
        this.updateButtons();
    }

    private void updateButtons() {
        boolean flag = this.minecraft != null && DragonForms.canManage(((KnowledgeMenu)this.menu).dragon(), this.minecraft.player);
        this.learn.active = flag && ((KnowledgeMenu)this.menu).previewCount() > 0;
        String s = !((KnowledgeMenu)this.menu).canLearn()
            ? "label.dragonmaid.learning_max"
            : (
                ((KnowledgeMenu)this.menu).getSlot(0).hasItem()
                        && ((KnowledgeMenu)this.menu).perItem() > 0
                        && ((KnowledgeMenu)this.menu).previewCount() == 0
                    ? "label.dragonmaid.learning_no_room"
                    : this.learningMaterialsHint()
            );
        this.learn.setTooltip(Tooltip.create(Component.translatable(s)));
        this.upgrade.visible = ((KnowledgeMenu)this.menu).knowledgeLevel() > 0;
        this.upgrade.active = this.upgrade.visible && flag && ((KnowledgeMenu)this.menu).upgradeReady();
        Component component = ((KnowledgeMenu)this.menu).knowledgeLevel() == 0
            ? Component.translatable("label.dragonmaid.knowledge_first_step")
            : (
                !((KnowledgeMenu)this.menu).upgradeAvailable()
                    ? Component.translatable("label.dragonmaid.knowledge_max")
                    : (
                        ((KnowledgeMenu)this.menu).knowledge() < ((KnowledgeMenu)this.menu).learningLimit()
                            ? Component.translatable(
                                "label.dragonmaid.upgrade_missing_knowledge",
                                new Object[]{((KnowledgeMenu)this.menu).learningLimit() - ((KnowledgeMenu)this.menu).knowledge()}
                            )
                            : (!((KnowledgeMenu)this.menu).upgradeReady() ? Component.translatable("label.dragonmaid.upgrade_missing_books") : null)
                    )
            );
        this.upgrade.setTooltip(component == null ? null : Tooltip.create(component));
        DragonScreenTabs.updateTransform(this.transform, ((KnowledgeMenu)this.menu).dragon());
    }

    private String learningMaterialsHint() {
        return ((KnowledgeMenu)this.menu).knowledgeLevel() >= 2
            ? "label.dragonmaid.learning_materials_curse"
            : (((KnowledgeMenu)this.menu).knowledgeLevel() >= 1 ? "label.dragonmaid.learning_materials_enchanted" : "label.dragonmaid.learning_materials");
    }

    protected void containerTick() {
        super.containerTick();
        this.updateButtons();
    }

    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        DragonPanel.background(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        for (Slot slot : ((KnowledgeMenu)this.menu).slots) {
            if (slot.isActive()) {
                DragonPanel.slot(g, this.leftPos + slot.x, this.topPos + slot.y);
            } else {
                DragonPanel.lockedSlot(g, this.leftPos + slot.x, this.topPos + slot.y);
            }
        }

        g.fill(this.leftPos + 8, this.topPos + 43, this.leftPos + 220, this.topPos + 47, -11184811);
        int i = (int)(
            210L
                * (long)Math.min(((KnowledgeMenu)this.menu).learningLimit(), ((KnowledgeMenu)this.menu).knowledge())
                / (long)Math.max(1, ((KnowledgeMenu)this.menu).learningLimit())
        );
        g.fill(this.leftPos + 9, this.topPos + 44, this.leftPos + 9 + i, this.topPos + 46, -13272983);
    }

    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(
            this.font,
            Component.translatable("label.dragonmaid.accessory_level", new Object[]{((KnowledgeMenu)this.menu).knowledgeLevel()}),
            8,
            20,
            4210752,
            false
        );
        g.drawString(
            this.font,
            ((KnowledgeMenu)this.menu).upgradeAvailable()
                ? Component.translatable(
                    "label.dragonmaid.progress", new Object[]{((KnowledgeMenu)this.menu).knowledge(), ((KnowledgeMenu)this.menu).learningLimit()}
                )
                : Component.translatable("label.dragonmaid.knowledge_total", new Object[]{((KnowledgeMenu)this.menu).knowledge()}),
            8,
            32,
            4210752,
            false
        );
        if (((KnowledgeMenu)this.menu).getSlot(0).hasItem()) {
            g.drawString(
                this.font,
                Component.translatable(
                    "label.dragonmaid.learning_preview",
                    new Object[]{((KnowledgeMenu)this.menu).previewExperience(), ((KnowledgeMenu)this.menu).previewCount()}
                ),
                118,
                55,
                2646093,
                false
            );
        }

        g.drawString(
            this.font,
            ((KnowledgeMenu)this.menu).upgradeAvailable()
                ? Component.translatable(
                    "label.dragonmaid.knowledge_next",
                    new Object[]{((KnowledgeMenu)this.menu).nextLevel(), ((KnowledgeMenu)this.menu).nextAccessorySlots()}
                )
                : Component.translatable("label.dragonmaid.knowledge_max"),
            8,
            75,
            4210752,
            false
        );
        if (((KnowledgeMenu)this.menu).knowledgeLevel() == 0) {
            g.drawString(this.font, Component.translatable("label.dragonmaid.knowledge_first_step"), 8, 89, 4210752, false);
            g.drawWordWrap(this.font, Component.translatable("label.dragonmaid.knowledge_first_form"), 102, 104, 118, 4210752);
        } else if (((KnowledgeMenu)this.menu).upgradeAvailable()) {
            g.drawString(
                this.font,
                Component.translatable(
                    "label.dragonmaid.upgrade_regular_count",
                    new Object[]{((KnowledgeMenu)this.menu).regularPlaced(), ((KnowledgeMenu)this.menu).regularRequired()}
                ),
                8,
                89,
                ((KnowledgeMenu)this.menu).regularPlaced() >= ((KnowledgeMenu)this.menu).regularRequired() ? 2646093 : 4210752,
                false
            );
            if (((KnowledgeMenu)this.menu).curseRequired() > 0) {
                g.drawString(
                    this.font,
                    Component.translatable(
                        "label.dragonmaid.upgrade_curse_count",
                        new Object[]{((KnowledgeMenu)this.menu).cursePlaced(), ((KnowledgeMenu)this.menu).curseRequired()}
                    ),
                    134,
                    89,
                    ((KnowledgeMenu)this.menu).cursePlaced() >= ((KnowledgeMenu)this.menu).curseRequired() ? 2646093 : 4210752,
                    false
                );
            }
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        this.renderTooltip(g, mouseX, mouseY);

        for (int i = 1; i <= 3; i++) {
            Slot slot = ((KnowledgeMenu)this.menu).getSlot(i);
            if (!slot.hasItem() && DragonPanel.hovering(mouseX, mouseY, this.leftPos + slot.x, this.topPos + slot.y)) {
                g.renderTooltip(
                    this.font,
                    Component.translatable(
                        ((KnowledgeMenu)this.menu).knowledgeLevel() == 0
                            ? "label.dragonmaid.knowledge_first_step"
                            : (
                                !((KnowledgeMenu)this.menu).upgradeAvailable()
                                    ? "label.dragonmaid.knowledge_max"
                                    : (
                                        !((KnowledgeMenu)this.menu).bookSlotEnabled(i)
                                            ? "label.dragonmaid.upgrade_slot_locked"
                                            : (i == 3 ? "label.dragonmaid.upgrade_curse_slot" : "label.dragonmaid.upgrade_regular_slot")
                                    )
                            )
                    ),
                    mouseX,
                    mouseY
                );
            }
        }

        Slot slot1 = ((KnowledgeMenu)this.menu).getSlot(0);
        if (!slot1.hasItem() && DragonPanel.hovering(mouseX, mouseY, this.leftPos + slot1.x, this.topPos + slot1.y)) {
            g.renderTooltip(this.font, Component.translatable(this.learningMaterialsHint()), mouseX, mouseY);
        }
    }
}
