package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.StarterCompanionScreen;
import com.example.dragonmaid.client.StarterWorldOptions;
import com.example.dragonmaid.starter.StarterConfig;
import com.example.dragonmaid.starter.StarterSettings;
import com.example.dragonmaid.starter.StarterWorldData;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.2.7 "开局伙伴选择"：在新建世界界面追加一个入口按钮，并在世界目录创建完成后把
 * {@link StarterSettings} 落成 {@code data/dragonmaid_starting_companion.dat}。
 *
 * <p>1.21.1 的 {@link CreateWorldScreen} 结构（逐项核对过源码）：</p>
 * <ul>
 *   <li>字段 {@code private final TabManager tabManager}（1.20.1 的 {@code f_267424_}）仍在；
 *       {@code getCurrentTab()} 返回 {@link net.minecraft.client.gui.components.tabs.Tab}。</li>
 *   <li>标签页仍是 {@link GridLayoutTab}，其 {@code protected final GridLayout layout} 未变。</li>
 *   <li>"重新排布"方法为 {@code public void repositionElements()}（1.20.1 的 {@code m_267719_}）。</li>
 *   <li>{@code private Optional<LevelStorageAccess> createNewWorldDirectory()} 仍在，
 *       世界根目录用 {@code getLevelPath(LevelResource.ROOT)} 取（{@link StarterWorldData#writeNewWorld}
 *       会再拼一个 {@code data/}）。</li>
 * </ul>
 *
 * <p>与上游的差异（1.21.1 适配）：</p>
 * <ul>
 *   <li>{@code LevelResource.f_78182_} 不再靠 SRG 猜，直接写 {@code LevelResource.ROOT}。</li>
 *   <li>{@code Screen#width/height/minecraft/children()} 都还是同名成员，不做改动。</li>
 *   <li>注入点方法名沿用官方名，mixin 配置无 refmap（NeoForge 1.21.x 跑官方名）。</li>
 * </ul>
 */
@Mixin({CreateWorldScreen.class})
public abstract class StarterCreateWorldMixin extends Screen implements StarterWorldOptions {
    @Shadow
    @Final
    private TabManager tabManager;

    @Unique
    private StarterSettings dragonmaid$starterSettings;
    @Unique
    private Button dragonmaid$entry;
    @Unique
    private int dragonmaid$entryX = Integer.MIN_VALUE;
    @Unique
    private int dragonmaid$entryY;
    @Unique
    private int dragonmaid$entryWidth;

    @Shadow
    public abstract void repositionElements();

    protected StarterCreateWorldMixin(Component title) {
        super(title);
    }

    @Override
    public StarterSettings dragonmaid$starter() {
        if (this.dragonmaid$starterSettings == null) {
            this.dragonmaid$starterSettings = new StarterSettings();
            this.dragonmaid$starterSettings.normalize(StarterConfig.limits(true));
        }

        return this.dragonmaid$starterSettings;
    }

    @Override
    public void dragonmaid$starter(StarterSettings settings) {
        this.dragonmaid$starterSettings = settings.copy();
    }

    @Override
    public Button dragonmaid$starterEntry() {
        return this.dragonmaid$entry;
    }

    @Inject(method = {"init"}, at = {@At("TAIL")})
    private void dragonmaid$appendEntry(CallbackInfo ci) {
        this.dragonmaid$entry = null;
        if (!StarterConfig.showEntry()) {
            return;
        }

        if (this.tabManager.getCurrentTab() instanceof GridLayoutTab tab) {
            GridLayout layout = ((StarterTabAccessor) tab).dragonmaid$layout();
            int row = 0;

            for (Object cell : ((StarterGridAccessor) layout).dragonmaid$occupiedCells()) {
                row = Math.max(row, ((StarterGridCellInvoker) cell).dragonmaid$lastRow() + 1);
            }

            Button entry = Button
                .builder(Component.translatable("starter.dragonmaid.entry"), b -> this.minecraft.setScreen(new StarterCompanionScreen((CreateWorldScreen) (Object) this)))
                .width(210)
                .build();
            this.dragonmaid$entry = entry;
            this.dragonmaid$entryX = Integer.MIN_VALUE;
            layout.addChild(entry, row, 0);
            this.addRenderableWidget(entry);
            this.repositionElements();
        }
    }

    @Unique
    private boolean dragonmaid$free(int x, int y, int w) {
        if (x < 4 || x + w > this.width - 4 || y < 34 || y + 20 > this.height - 40) {
            return false;
        }

        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget
                && widget != this.dragonmaid$entry
                && widget.visible
                && x < widget.getX() + widget.getWidth()
                && x + w > widget.getX()
                && y < widget.getY() + widget.getHeight()
                && y + 20 > widget.getY()) {
                return false;
            }
        }

        return true;
    }

    @Inject(method = {"render"}, at = {@At("HEAD")})
    private void dragonmaid$avoidExternalButtons(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        Button entry = this.dragonmaid$entry;
        if (entry == null || !this.children().contains(entry)) {
            return;
        }

        if (entry.getX() == this.dragonmaid$entryX && entry.getY() == this.dragonmaid$entryY && entry.getWidth() == this.dragonmaid$entryWidth) {
            return;
        }

        if (!this.dragonmaid$free(entry.getX(), entry.getY(), entry.getWidth())) {
            boolean placed = false;

            for (int w : new int[]{210, 24}) {
                for (int y = 34; y <= this.height - 60 && !placed; y += 2) {
                    for (int x : new int[]{(this.width - w) / 2, 4, this.width - w - 4}) {
                        if (this.dragonmaid$free(x, y, w)) {
                            entry.setWidth(w);
                            entry.setX(x);
                            entry.setY(y);
                            entry.setMessage(w == 24 ? Component.literal("DM") : Component.translatable("starter.dragonmaid.entry"));
                            entry.setTooltip(Tooltip.create(Component.translatable("starter.dragonmaid.entry")));
                            placed = true;
                            break;
                        }
                    }
                }

                if (placed) {
                    break;
                }
            }
        }

        this.dragonmaid$entryX = entry.getX();
        this.dragonmaid$entryY = entry.getY();
        this.dragonmaid$entryWidth = entry.getWidth();
    }

    @Inject(method = {"createNewWorldDirectory"}, at = {@At("RETURN")}, cancellable = true)
    private void dragonmaid$saveStarter(CallbackInfoReturnable<Optional<LevelStorageAccess>> cir) {
        Optional<LevelStorageAccess> access = cir.getReturnValue();
        if (access.isEmpty() || !StarterConfig.showEntry()) {
            return;
        }

        try {
            StarterSettings settings = this.dragonmaid$starter().copy();
            settings.normalize(StarterConfig.limits(true));
            StarterWorldData.writeNewWorld(
                access.get().getLevelPath(LevelResource.ROOT),
                settings,
                this.minecraft.getUser().getProfileId()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save starting companion options", exception);
        }
    }
}
