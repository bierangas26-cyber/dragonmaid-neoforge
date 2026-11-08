package com.example.dragonmaid.recipe;

import com.example.dragonmaid.compat.RO;
import com.example.dragonmaid.compat.Reg;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 1.2: 自定义合成原料“力量药水”（普通/喷溅/滞留，含 strong/long 变体）。
 * 1.21.1: Forge 的 AbstractIngredient/IIngredientSerializer 已移除，改用 NeoForge 的
 * ICustomIngredient + IngredientType（registry: neoforge:ingredient_serializer）。
 * 数据包 JSON 写法不变：{"type": "dragonmaid:strength_potion"}。
 */
public final class StrengthPotionIngredient implements ICustomIngredient {
    private static final List<Holder<Potion>> POTIONS = List.of(Potions.STRENGTH, Potions.LONG_STRENGTH, Potions.STRONG_STRENGTH);
    private static final List<Item> CARRIERS = List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);
    private static final StrengthPotionIngredient INSTANCE = new StrengthPotionIngredient();
    public static final MapCodec<StrengthPotionIngredient> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, StrengthPotionIngredient> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final IngredientType<StrengthPotionIngredient> TYPE = new IngredientType<>(CODEC, STREAM_CODEC);
    public static final Reg<IngredientType<?>> TYPES = Reg.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, "dragonmaid");
    public static final RO<IngredientType<?>> REGISTERED = TYPES.register("strength_potion", () -> TYPE);

    private StrengthPotionIngredient() {
    }

    public static Ingredient get() {
        return INSTANCE.toVanilla();
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        } else if (!CARRIERS.contains(stack.getItem())) {
            return false;
        } else {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            return contents != null && contents.potion().map(POTIONS::contains).orElse(false);
        }
    }

    @Override
    public Stream<ItemStack> getItems() {
        return CARRIERS.stream().flatMap(item -> POTIONS.stream().map(potion -> PotionContents.createItemStack(item, potion)));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
