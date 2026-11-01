package com.example.dragonmaid.menu;

import com.example.dragonmaid.recall.RecallContent;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.example.dragonmaid.compat.RO;
import com.example.dragonmaid.compat.Reg;

public final class ModMenus {
    public static final Reg<MenuType<?>> MENUS = Reg.create(BuiltInRegistries.MENU, "dragonmaid");
    public static final RO<MenuType<KnowledgeMenu>> KNOWLEDGE = MENUS.register("knowledge", () -> IMenuTypeExtension.create(KnowledgeMenu::new));
    public static final RO<MenuType<AccessoryMenu>> ACCESSORIES = MENUS.register("accessories", () -> IMenuTypeExtension.create(AccessoryMenu::new));
    public static final RO<MenuType<?>> RECALL = RecallContent.menu();

    private ModMenus() {
    }
}
