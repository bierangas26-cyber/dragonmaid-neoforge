package com.example.dragonmaid.client;

import com.example.dragonmaid.item.TrinityDragonGemItem;
import com.example.dragonmaid.mixin.client.CreativeGemSlotAccessor;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.GemModeMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * 1.2: Shift + 右键切换「三位一体龙宝石」的模式。
 *
 * <p>1.20.1 里是 {@code ScreenEvent.MouseButtonPressed.Pre} / {@code MouseButtonReleased.Pre}；
 * 1.21.1 NeoForge 的事件类名不变，但 {@code Dist} / {@code EventBusSubscriber} / {@code EventPriority}
 * 全部搬到 {@code net.neoforged.*}（见 import）。SRG 名按 port 约定改为官方名：
 * {@code f_38840_ -> containerId}、{@code f_38839_ -> slots}、{@code m_142621_ -> getCarried()}、
 * {@code m_7993_ -> getItem()}、{@code f_40220_/f_40221_ -> x/y}、{@code f_40218_ -> container}、
 * {@code f_40219_ -> index}、{@code m_150661_ -> getContainerSlot()}、{@code f_36095_ -> inventoryMenu}。
 */
@EventBusSubscriber(modid = "dragonmaid", value = {Dist.CLIENT})
public final class GemModeInput {
    private static Screen consumed;

    private GemModeInput() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() == 1 && Screen.hasShiftDown() && event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            if (toggle(screen, event.getMouseX(), event.getMouseY())) {
                consumed = screen;
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() == 1 && consumed != null) {
            if (event.getScreen() == consumed) {
                event.setCanceled(true);
            }

            consumed = null;
        }
    }

    public static boolean toggle(AbstractContainerScreen<?> screen, double x, double y) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return false;
        }

        AbstractContainerMenu menu = screen.getMenu();
        if (menu.getCarried().getItem() instanceof TrinityDragonGemItem) {
            if (screen instanceof CreativeModeInventoryScreen) {
                TrinityDragonGemItem.toggleMode(menu.getCarried());
            } else {
                DragonNetwork.CHANNEL.sendToServer(new GemModeMessage(menu.containerId, -1, menu.getCarried().copy()));
            }

            return true;
        }

        Slot slot = null;

        for (Slot candidate : menu.slots) {
            if (candidate.isActive()
                && x >= (double)(screen.getGuiLeft() + candidate.x - 1)
                && x < (double)(screen.getGuiLeft() + candidate.x + 17)
                && y >= (double)(screen.getGuiTop() + candidate.y - 1)
                && y < (double)(screen.getGuiTop() + candidate.y + 17)) {
                slot = candidate;
                break;
            }
        }

        if (slot == null || !(slot.getItem().getItem() instanceof TrinityDragonGemItem)) {
            return false;
        }

        int id = slot.index;
        int menuId = menu.containerId;
        if (screen instanceof CreativeModeInventoryScreen) {
            Slot real = slot instanceof CreativeGemSlotAccessor wrapper ? wrapper.dragonmaid$realGemSlot() : slot;
            if (real.container != player.getInventory()) {
                ItemStack preview = slot.getItem().copy();
                TrinityDragonGemItem.toggleMode(preview);
                slot.set(preview);
                return true;
            }

            id = -1;

            for (Slot actual : player.inventoryMenu.slots) {
                if (actual.container == real.container && actual.getContainerSlot() == real.getContainerSlot()) {
                    id = actual.index;
                    break;
                }
            }

            if (id < 0) {
                return false;
            }

            menuId = player.inventoryMenu.containerId;
        }

        DragonNetwork.CHANNEL.sendToServer(new GemModeMessage(menuId, id, slot.getItem().copy()));
        return true;
    }
}
