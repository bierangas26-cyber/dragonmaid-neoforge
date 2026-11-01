package com.example.dragonmaid.client;

import com.example.dragonmaid.menu.RecallMenu;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.RecallRequestMessage;
import com.example.dragonmaid.network.RecallRosterMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;

public final class RecallScreen extends AbstractContainerScreen<RecallMenu> {
    private final List<Button> rows = new ArrayList<>();
    private Button previous;
    private Button next;
    private Button recall;
    private UUID selected;
    private int confirmedCost = -1;

    public RecallScreen(RecallMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = 234;
    }

    public static void receive(RecallRosterMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof RecallMenu recallmenu && recallmenu.containerId == message.menuId()) {
            recallmenu.roster = message;
            if (minecraft.screen instanceof RecallScreen recallscreen && recallscreen.menu == recallmenu) {
                recallscreen.updateRows();
            }
        }
    }

    protected void init() {
        super.init();
        this.rows.clear();

        for (int i = 0; i < 6; i++) {
            int j = i;
            this.rows.add((Button)this.addRenderableWidget(Button.builder(Component.empty(), b -> {
                if (((RecallMenu)this.menu).roster != null && j < ((RecallMenu)this.menu).roster.rows().size()) {
                    this.selected = ((RecallMenu)this.menu).roster.rows().get(j).id();
                    this.confirmedCost = -1;
                    this.updateRows();
                }
            }).bounds(this.leftPos + 8, this.topPos + 28 + i * 25, 120, 22).build()));
        }

        this.previous = (Button)this.addRenderableWidget(
            Button.builder(Component.literal("<"), b -> this.page(0)).bounds(this.leftPos + 8, this.topPos + 182, 22, 20).build()
        );
        this.next = (Button)this.addRenderableWidget(
            Button.builder(Component.literal(">"), b -> this.page(1)).bounds(this.leftPos + 106, this.topPos + 182, 22, 20).build()
        );
        this.addRenderableWidget(Button.builder(tr("refresh"), b -> this.page(2)).bounds(this.leftPos + 8, this.topPos + 207, 120, 20).build());
        this.recall = (Button)this.addRenderableWidget(
            Button.builder(tr("recall"), b -> this.confirm()).bounds(this.leftPos + 140, this.topPos + 182, 172, 20).build()
        );
        this.updateRows();
    }

    private void page(int action) {
        this.confirmedCost = -1;
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(((RecallMenu)this.menu).containerId, action);
        }
    }

    private RecallRosterMessage.Row selectedRow() {
        return ((RecallMenu)this.menu).roster == null
            ? null
            : ((RecallMenu)this.menu).roster.rows().stream().filter(row -> row.id().equals(this.selected)).findFirst().orElse(null);
    }

    private void confirm() {
        RecallRosterMessage.Row recallrostermessage$row = this.selectedRow();
        if (recallrostermessage$row != null && recallrostermessage$row.cost() >= 0 && !((RecallMenu)this.menu).roster.pending()) {
            if (this.confirmedCost != recallrostermessage$row.cost()) {
                this.confirmedCost = recallrostermessage$row.cost();
                this.updateRows();
            } else {
                DragonNetwork.CHANNEL
                    .sendToServer(new RecallRequestMessage(((RecallMenu)this.menu).containerId, recallrostermessage$row.id(), this.confirmedCost));
                this.confirmedCost = -1;
                this.recall.active = false;
            }
        }
    }

    private void updateRows() {
        if (!this.rows.isEmpty()) {
            RecallRosterMessage recallrostermessage = ((RecallMenu)this.menu).roster;
            if (recallrostermessage != null && this.selectedRow() == null) {
                this.selected = recallrostermessage.rows().isEmpty() ? null : recallrostermessage.rows().get(0).id();
                this.confirmedCost = -1;
            }

            for (int i = 0; i < this.rows.size(); i++) {
                Button button = this.rows.get(i);
                button.visible = recallrostermessage != null && i < recallrostermessage.rows().size();
                if (button.visible) {
                    RecallRosterMessage.Row recallrostermessage$row = recallrostermessage.rows().get(i);
                    button.setMessage(
                        Component.literal(
                            (recallrostermessage$row.id().equals(this.selected) ? "\u203a " : "")
                                + this.font.plainSubstrByWidth(displayName(recallrostermessage$row), 100)
                        )
                    );
                    button.setTooltip(Tooltip.create(Component.literal(displayName(recallrostermessage$row))));
                }
            }

            this.previous.active = recallrostermessage != null && recallrostermessage.page() > 0;
            this.next.active = recallrostermessage != null && recallrostermessage.page() + 1 < recallrostermessage.pages();
            RecallRosterMessage.Row recallrostermessage$row1 = this.selectedRow();
            if (recallrostermessage$row1 != null && recallrostermessage$row1.cost() != this.confirmedCost) {
                this.confirmedCost = -1;
            }

            this.recall.active = recallrostermessage$row1 != null && recallrostermessage$row1.cost() >= 0 && !recallrostermessage.pending();
            this.recall
                .setMessage(
                    recallrostermessage$row1 != null && this.confirmedCost >= 0
                        ? tr("confirm", this.font.plainSubstrByWidth(displayName(recallrostermessage$row1), 100))
                        : tr("recall")
                );
        }
    }

    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos - 2, this.topPos - 2, this.leftPos + this.imageWidth + 2, this.topPos + this.imageHeight + 2, -14210255);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -2764347);
        graphics.fill(this.leftPos + 133, this.topPos + 26, this.leftPos + 135, this.topPos + 202, -7632510);
    }

    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 9, 9, 3159613, false);
        RecallRosterMessage recallrostermessage = ((RecallMenu)this.menu).roster;
        if (recallrostermessage == null || recallrostermessage.rows().isEmpty()) {
            g.drawWordWrap(this.font, tr("empty"), 12, 38, 120, 5592405);
        }

        if (recallrostermessage != null) {
            g.drawCenteredString(this.font, tr("page", recallrostermessage.page() + 1, recallrostermessage.pages()), 68, 188, 15658734);
        }

        RecallRosterMessage.Row recallrostermessage$row = this.selectedRow();
        if (recallrostermessage$row != null) {
            g.drawString(this.font, this.font.plainSubstrByWidth(displayName(recallrostermessage$row), 170), 142, 29, 2236962, false);
            String s = "entity." + recallrostermessage$row.type().replace(':', '.');
            g.drawString(this.font, Component.translatable(s), 142, 45, 4473924, false);
            this.line(
                g,
                tr(recallrostermessage$row.male() ? "male" : "female")
                    .append(" \u00b7 ")
                    .append(tr(recallrostermessage$row.humanoid() ? "humanoid" : "dragon")),
                59
            );
            this.line(
                g,
                tr("state." + recallrostermessage$row.state().name().toLowerCase(Locale.ROOT))
                    .append(recallrostermessage$row.injured() ? Component.literal(" \u00b7 ").append(tr("state.injured")) : Component.empty()),
                73
            );
            this.line(g, tr("age", recallrostermessage$row.ageTicks() / 24000), 91);
            this.line(g, tr("knowledge", recallrostermessage$row.knowledgeLevel(), recallrostermessage$row.knowledge()), 105);
            g.drawString(this.font, "UID", 142, 118, 7829367, false);
            String s1 = recallrostermessage$row.id().toString();
            g.drawString(this.font, s1.substring(0, 18), 142, 128, 7829367, false);
            g.drawString(this.font, s1.substring(18), 142, 138, 7829367, false);
            if (recallrostermessage$row.cost() >= 0) {
                this.line(g, tr("cost", recallrostermessage$row.cost()), 153);
            }

            this.line(g, distance(recallrostermessage$row), 165);
        } else {
            this.line(g, tr("select"), 40);
        }

        if (recallrostermessage != null) {
            g.drawWordWrap(this.font, (FormattedText)(recallrostermessage.pending() ? tr("pending") : recallrostermessage.status()), 142, 209, 170, 5592405);
        }
    }

    private static String displayName(RecallRosterMessage.Row row) {
        return row.name().isBlank() ? tr("unnamed").getString() : row.name();
    }

    private static Component distance(RecallRosterMessage.Row row) {
        if (row.distance() == -1) {
            return tr(row.lastKnown() ? "distance_recorded_dimension" : "distance_dimension");
        } else {
            return row.distance() < 0 ? tr("distance_unknown") : tr(row.lastKnown() ? "distance_recorded" : "distance_current", row.distance());
        }
    }

    private void line(GuiGraphics graphics, Component text, int y) {
        graphics.drawString(this.font, this.font.plainSubstrByWidth(text.getString(), 170), 142, y, 4473924, false);
    }

    public void render(GuiGraphics graphics, int x, int y, float partialTick) {
        this.renderBackground(graphics, x, y, partialTick);
        super.render(graphics, x, y, partialTick);
        this.renderTooltip(graphics, x, y);
    }

    private static MutableComponent tr(String suffix, Object... args) {
        return Component.translatable("screen.dragonmaid.recall." + suffix, args);
    }
}
