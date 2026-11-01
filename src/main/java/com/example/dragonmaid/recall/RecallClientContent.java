package com.example.dragonmaid.recall;

import com.example.dragonmaid.client.RecallScreen;
import com.example.dragonmaid.menu.ModMenus;
import com.example.dragonmaid.menu.RecallMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 1.21.1 / NeoForge 迁移：MenuScreens.register 在 1.21.1 变成 private，注册菜单界面必须走
 * RegisterMenuScreensEvent（IModBusEvent，MOD bus；1.20.1 Forge 的 FMLClientSetupEvent 里直接
 * MenuScreens.register 的写法已不成立）。
 *
 * 本类现在是自注册的 client/mod-bus 监听器：不再需要外部调用 setup()。
 * （ClientRegistration.setup 里原来的 RecallClientContent.setup() 调用应删除，
 *  连带 KNOWLEDGE/ACCESSORIES 两处 MenuScreens.register 也改成 RegisterMenuScreensEvent。）
 */
@EventBusSubscriber(
    modid = "dragonmaid",
    value = {Dist.CLIENT},
    bus = EventBusSubscriber.Bus.MOD
)
public final class RecallClientContent {
    private RecallClientContent() {
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        // ModMenus.RECALL 的类型是 RO<MenuType<?>>（非通配注册器的兼容垫片），这里还原成具体 RecallMenu 类型。
        event.register((MenuType<RecallMenu>)ModMenus.RECALL.get(), RecallScreen::new);
    }
}
