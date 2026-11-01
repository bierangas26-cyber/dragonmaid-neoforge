package com.example.dragonmaid.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

/** 1.21 moved enchantments to a datapack registry; this resolves them from the running server. */
public final class Enchants {
    private Enchants() { }

    private static Registry<Enchantment> registry() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }

    public static boolean contains(ResourceLocation id) {
        Registry<Enchantment> registry = registry();
        return registry != null && registry.containsKey(id);
    }

    public static Enchantment get(ResourceLocation id) {
        Registry<Enchantment> registry = registry();
        return registry == null ? null : registry.get(id);
    }

    public static boolean isCurse(Enchantment enchantment) {
        Registry<Enchantment> registry = registry();
        if (registry == null || enchantment == null) {
            return false;
        }
        Holder<Enchantment> holder = registry.wrapAsHolder(enchantment);
        return holder.is(net.minecraft.tags.EnchantmentTags.CURSE);
    }
}
