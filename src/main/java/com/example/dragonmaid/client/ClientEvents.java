package com.example.dragonmaid.client;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.menu.DragonDetailsMenu;
import com.example.dragonmaid.mixin.client.RendererAccessor;
import com.example.dragonmaid.network.AccessoryMenuAction;
import com.example.dragonmaid.network.DragonNetwork;
import com.iafenvoy.iceandfire.screen.gui.DragonScreen;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.screen.menu.DragonMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Pre;
import net.neoforged.neoforge.client.event.ScreenEvent.Init.Post;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.example.dragonmaid.compat.IafProxy;

@EventBusSubscriber(
    modid = "dragonmaid",
    value = {Dist.CLIENT}
)
public final class ClientEvents {
    public ClientEvents() {
    }

    @SubscribeEvent
    public static void motion(ClientTickEvent.Post event) {
        // 1.21.1: ClientTickEvent is split into Pre/Post; this handler only runs on Post.
        {
            Minecraft minecraft = Minecraft.getInstance();
            HumanoidMotion.endTick(minecraft.level);
            ClientBreathAnimations.tick();
            ClientTailSweepAnimations.tick();
            ServiceDragonArmorTextures.tick();
            if (minecraft.screen instanceof DragonScreen guidragon) {
                for (GuiEventListener guieventlistener : guidragon.children()) {
                    if (guieventlistener instanceof ClientEvents.DragonButton clientevents$dragonbutton) {
                        clientevents$dragonbutton.update();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void logout(LoggingOut event) {
        clearWorldState();
    }

    @SubscribeEvent
    public static void unload(Unload event) {
        if (event.getLevel().isClientSide()) {
            clearWorldState();
        }
    }

    private static void clearWorldState() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(ClientEvents::clearWorldState);
        } else {
            ClientTailSweepAnimations.clear();
            IafProxy.clearReferencedMob();
            ClientRegistration.clearWorldState();
            ServiceDragonArmorTextures.clearCache();
        }
    }

    // 1.21.1: RenderTickEvent was replaced by RenderFrameEvent.Pre/Post
    @SubscribeEvent
    public static void renderFramePre(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
        ServiceDragonArmorTextures.beginFrame();
        HumanoidStatueMaterials.beginFrame();
    }

    @SubscribeEvent
    public static void renderFramePost(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
        ServiceDragonArmorTextures.endFrame();
        HumanoidStatueMaterials.endFrame();
    }

    private static DragonBaseEntity referencedClientDragon(DragonScreen screen) {
        ClientLevel clientlevel = Minecraft.getInstance().level;
        if (clientlevel == null) {
            return null;
        }

        // 1.21 IAF CE: the opened menu owns the dragon reference (1.20's IAF.PROXY field is gone).
        DragonBaseEntity menuDragon = screen.getMenu() instanceof DragonMenu menu
            ? IafProxy.getMenuDragon(menu)
            : null;
        if (menuDragon == null && IafProxy.getReferencedMob() instanceof DragonBaseEntity fallback) {
            menuDragon = fallback;
        }
        if (menuDragon == null) {
            return null;
        }

        DragonBaseEntity resolved = menuDragon;
        if (clientlevel.getEntity(menuDragon.getId()) instanceof DragonBaseEntity live
            && live.getUUID().equals(menuDragon.getUUID())) {
            resolved = live;
        }

        IafProxy.setReferencedMob(resolved);
        return resolved;
    }

    @SubscribeEvent
    public static void screen(Post event) {
        if (event.getScreen() instanceof DragonScreen guidragon) {
            int k = (guidragon.width - 176) / 2;
            int top = (guidragon.height - 214) / 2;
            int tabsX = DragonScreenTabs.x(k, 176);
            event.addListener(DragonScreenTabs.currentPage(tabsX, top + 8, "button.dragonmaid.equipment"));
            event.addListener(new ClientEvents.DragonButton(guidragon, tabsX, top + 32, ClientEvents.DragonPage.KNOWLEDGE));
            event.addListener(new ClientEvents.DragonButton(guidragon, tabsX, top + 56, ClientEvents.DragonPage.ACCESSORIES));
            event.addListener(new ClientEvents.DragonButton(guidragon, tabsX, top + 96, ClientEvents.DragonPage.TRANSFORM));
        }
    }

    public static void renderDetailsSlots(GuiGraphics graphics, DragonScreen screen, int left, int top) {
        if (screen.getMenu() instanceof DragonDetailsMenu dragondetailsmenu) {
            for (int k = 0; k < 3; k++) {
                int i = left + 153;
                int j = top + 54 + k * 18;
                if (k < dragondetailsmenu.dragonmaid$unlockedSlots()) {
                    DragonPanel.slot(graphics, i, j);
                } else {
                    DragonPanel.lockedSlot(graphics, i, j);
                }
            }
        }
    }

    @SubscribeEvent
    public static void detailsTooltip(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof DragonScreen guidragon && guidragon.getMenu() instanceof DragonDetailsMenu dragondetailsmenu) {
            int k = (guidragon.width - 176) / 2;
            int top = (guidragon.height - 214) / 2;

            for (int slot = dragondetailsmenu.dragonmaid$unlockedSlots(); slot < 3; slot++) {
                if (DragonPanel.hovering(event.getMouseX(), event.getMouseY(), k + 153, top + 54 + slot * 18)) {
                    event.getGuiGraphics()
                        .renderTooltip(
                            Minecraft.getInstance().font,
                            Component.translatable("label.dragonmaid.accessory_locked", new Object[]{slot + 1}),
                            event.getMouseX(),
                            event.getMouseY()
                        );
                }
            }

            return;
        }
    }

    @SubscribeEvent
    public static void render(Pre<?, ?> event) {
        if (event.getEntity() instanceof DragonBaseEntity entitydragonbase
            && DragonForms.isHumanoid(entitydragonbase)
            && !(event.getRenderer() instanceof HumanoidDragonRenderer)
            && !ClientRegistration.dragonRenderers.isEmpty()) {
            event.setCanceled(true);
            ((RendererAccessor)event.getRenderer()).dragonmaid$setShadowRadius(0.4F);
            int i = FemaleDragonAssets.stageIndex(entitydragonbase.getDragonStage());
            IceFemaleRenderer icefemalerenderer = ClientRegistration.dragonRenderers.get(FemaleDragonAssets.of(entitydragonbase))[i];
            icefemalerenderer.render(
                entitydragonbase,
                entitydragonbase.getYRot(),
                event.getPartialTick(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight()
            );
            return;
        }
    }

    private static final class DragonButton extends DragonScreenTabs.PageButton {
        private final DragonScreen screen;
        private final ClientEvents.DragonPage target;

        private DragonButton(DragonScreen screen, int x, int y, ClientEvents.DragonPage target) {
            super(
                x,
                y,
                Component.empty(),
                button -> {
                    DragonBaseEntity entitydragonbase = ClientEvents.referencedClientDragon(screen);
                    if (entitydragonbase != null) {
                        if (target == ClientEvents.DragonPage.ACCESSORIES) {
                            DragonNetwork.CHANNEL
                                .sendToServer(
                                    new AccessoryMenuAction(
                                        ((DragonMenu)screen.getMenu()).containerId, entitydragonbase.getId(), entitydragonbase.getUUID()
                                    )
                                );
                        } else {
                            DragonNetwork.CHANNEL
                                .sendToServer(
                                    new DragonNetwork.Action(
                                        ((DragonMenu)screen.getMenu()).containerId,
                                        entitydragonbase.getId(),
                                        entitydragonbase.getUUID(),
                                        target == ClientEvents.DragonPage.KNOWLEDGE
                                    )
                                );
                        }
                    }
                }
            );
            this.screen = screen;
            this.target = target;
            this.update();
        }

        private void update() {
            DragonBaseEntity entitydragonbase = ClientEvents.referencedClientDragon(this.screen);
            String s = this.target == ClientEvents.DragonPage.ACCESSORIES
                ? "button.dragonmaid.accessories"
                : (this.target == ClientEvents.DragonPage.KNOWLEDGE ? "button.dragonmaid.knowledge" : null);
            if (entitydragonbase != null && Minecraft.getInstance().player != null) {
                boolean flag = DragonForms.isHumanoid(entitydragonbase);
                this.setMessage(Component.translatable(s != null ? s : (flag ? "button.dragonmaid.to_dragon" : "button.dragonmaid.to_human")));
                boolean flag1 = this.target == ClientEvents.DragonPage.ACCESSORIES
                    ? DragonAccessories.canManage(entitydragonbase, Minecraft.getInstance().player)
                    : DragonForms.canManage(entitydragonbase, Minecraft.getInstance().player);
                this.active = flag1 && (this.target != ClientEvents.DragonPage.TRANSFORM || flag || DragonForms.unlocked(entitydragonbase));
                this.setTooltip(
                    !flag1
                        ? Tooltip.create(Component.translatable("message.dragonmaid.cannot_manage"))
                        : (
                            this.target == ClientEvents.DragonPage.TRANSFORM && !flag && !DragonForms.unlocked(entitydragonbase)
                                ? Tooltip.create(Component.translatable("message.dragonmaid.locked"))
                                : null
                        )
                );
            } else {
                this.active = false;
                this.setMessage(Component.translatable(s == null ? "button.dragonmaid.to_human" : s));
                this.setTooltip(Tooltip.create(Component.translatable("message.dragonmaid.wait_for_dragon")));
            }
        }
    }

    private static enum DragonPage {
        KNOWLEDGE,
        ACCESSORIES,
        TRANSFORM;

        private DragonPage() {
        }
    }
}
