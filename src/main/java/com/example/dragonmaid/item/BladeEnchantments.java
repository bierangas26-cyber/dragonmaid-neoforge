package com.example.dragonmaid.item;

import com.example.dragonmaid.combat.BladeCompat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 刀/长剑的四个附魔（1.2.7 上游）。
 *
 * <p><b>1.21.1 降级说明</b>：上游用 {@code EnchantmentCategory.create(...)} + {@code new Enchantment(...)}
 * 注册 4 个附魔；1.21.1 已删除 EnchantmentCategory、Enchantment 变为 final 且改为**数据驱动**
 * （data/&lt;ns&gt;/enchantment/*.json + 注册表 Holder）。因此这里只保留**数值/查询层**：
 * 4 张概率表、parryBonusTicks 公式、物品书 tooltip 描述；附魔本体与适用物品由数据包 JSON 提供，
 * 注册由数据层负责（本类的 {@link #register} 是空实现，仅为调用点保持兼容）。</p>
 */
@EventBusSubscriber(modid = "dragonmaid")
public final class BladeEnchantments {
    public static final ResourceLocation WINDSTEP_ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "windstep");
    public static final ResourceLocation SECOND_CHANCE_ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "second_chance");
    public static final ResourceLocation STEADY_GUARD_ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "steady_guard");
    public static final ResourceLocation DEFLECTION_ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "deflection");

    private static final float[] WINDSTEP_SPEED = new float[]{0.4F, 0.6F, 0.9F, 1.2F};
    private static final float[] SECOND_CHANCE_PROBABILITY = new float[]{0.0F, 0.125F, 0.25F, 0.35F};
    private static final float[] DEFLECTION_PROBABILITY = new float[]{0.0F, 0.15F, 0.3F, 0.5F};

    private BladeEnchantments() {
    }

    public static ResourceKey<Enchantment> windstep() {
        return ResourceKey.create(Registries.ENCHANTMENT, WINDSTEP_ID);
    }

    public static ResourceKey<Enchantment> secondChance() {
        return ResourceKey.create(Registries.ENCHANTMENT, SECOND_CHANCE_ID);
    }

    public static ResourceKey<Enchantment> steadyGuard() {
        return ResourceKey.create(Registries.ENCHANTMENT, STEADY_GUARD_ID);
    }

    public static ResourceKey<Enchantment> deflection() {
        return ResourceKey.create(Registries.ENCHANTMENT, DEFLECTION_ID);
    }

    /** 1.21.1：附魔由数据包注册，这里不再注册任何对象（保留签名以兼容调用点）。 */
    public static void register(IEventBus bus) {
    }

    public static float chargeMovement(ItemStack katana) {
        int level = BladeCompat.enchantLevel(katana, windstep());
        return WINDSTEP_SPEED[Mth.clamp(level, 0, WINDSTEP_SPEED.length - 1)];
    }

    public static boolean hasWindstep(ItemStack katana) {
        return BladeCompat.enchantLevel(katana, windstep()) > 0;
    }

    public static float counterChance(ItemStack longsword) {
        int level = BladeCompat.enchantLevel(longsword, secondChance());
        return SECOND_CHANCE_PROBABILITY[Mth.clamp(level, 0, SECOND_CHANCE_PROBABILITY.length - 1)];
    }

    public static int parryBonusTicks(ItemStack longsword) {
        return 5 * Mth.clamp(BladeCompat.enchantLevel(longsword, steadyGuard()), 0, 3);
    }

    public static float deflectionChance(ItemStack longsword) {
        int level = BladeCompat.enchantLevel(longsword, deflection());
        return DEFLECTION_PROBABILITY[Mth.clamp(level, 0, DEFLECTION_PROBABILITY.length - 1)];
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void describeBooks(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return;
        }
        ItemEnchantments enchantments = stack.getTagEnchantments();
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            ResourceLocation id = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
            if (id == null || !"dragonmaid".equals(id.getNamespace())) {
                continue;
            }
            String key = "enchantment." + id.getNamespace() + "." + id.getPath() + ".desc";
            List<Component> tooltip = event.getToolTip();
            boolean present = false;
            for (Component line : tooltip) {
                if (key.equals(line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable
                        ? translatable.getKey() : null)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
