package com.example.dragonmaid.compat.jei;

import com.example.dragonmaid.client.AccessoryScreen;
import com.example.dragonmaid.client.DragonScreenTabs;
import com.example.dragonmaid.client.KnowledgeScreen;
import com.iafenvoy.iceandfire.screen.gui.DragonScreen;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class DragonMaidJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "dragon_sidebar");

    public DragonMaidJeiPlugin() {
    }

    public ResourceLocation getPluginUid() {
        return UID;
    }

    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(DragonScreen.class, new DragonMaidJeiPlugin.SidebarHandler());
        registration.addGuiContainerHandler(KnowledgeScreen.class, new DragonMaidJeiPlugin.SidebarHandler());
        registration.addGuiContainerHandler(AccessoryScreen.class, new DragonMaidJeiPlugin.SidebarHandler());
    }

    private static final class SidebarHandler<T extends AbstractContainerScreen<?>> implements IGuiContainerHandler<T> {
        private SidebarHandler() {
        }

        public List<Rect2i> getGuiExtraAreas(T screen) {
            int i = Integer.MAX_VALUE;
            int j = Integer.MAX_VALUE;
            int k = Integer.MIN_VALUE;
            int l = Integer.MIN_VALUE;

            for (GuiEventListener guieventlistener : screen.children()) {
                if (guieventlistener instanceof DragonScreenTabs.PageButton) {
                    DragonScreenTabs.PageButton dragonscreentabs$pagebutton = (DragonScreenTabs.PageButton)guieventlistener;
                    if (dragonscreentabs$pagebutton.visible) {
                        i = Math.min(i, dragonscreentabs$pagebutton.getX());
                        j = Math.min(j, dragonscreentabs$pagebutton.getY());
                        k = Math.max(k, dragonscreentabs$pagebutton.getX() + dragonscreentabs$pagebutton.getWidth());
                        l = Math.max(l, dragonscreentabs$pagebutton.getY() + dragonscreentabs$pagebutton.getHeight());
                    }
                }
            }

            return i == Integer.MAX_VALUE ? List.of() : List.of(new Rect2i(i, j, k - i, l - j));
        }
    }
}
