package com.example.dragonmaid.item;

import com.example.dragonmaid.recall.RecallContent;
import com.iafenvoy.iceandfire.registry.IafItems;
import com.iafenvoy.iceandfire.registry.IafTiers;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.BuiltInRegistries;
import com.example.dragonmaid.compat.RO;
import com.example.dragonmaid.compat.Reg;

public class ModItems {
    public static final Reg<Item> ITEMS = Reg.create(BuiltInRegistries.ITEM, "dragonmaid");
    public static final RO<Item> TEST_EXECUTION_BLADE = ITEMS.register("test_execution_blade", TestExecutionBladeItem::new);
    public static final RO<Item> KAISHAKU_BLADE = ITEMS.register("kaishaku_blade", KaishakuBladeItem::new);
    public static final RO<PetrificationRemedyItem> PETRIFICATION_REMEDY = ITEMS.register("petrification_remedy", PetrificationRemedyItem::new);
    public static final RO<TrinityDragonGemItem> TRINITY_DRAGON_GEM = ITEMS.register("trinity_dragon_gem", TrinityDragonGemItem::new);
    public static final RO<ReinforcedDragonHornItem> REINFORCED_DRAGON_HORN = ITEMS.register(
        "reinforced_dragon_horn", ReinforcedDragonHornItem::new
    );
    public static final RO<GuardianTagItem> GUARDIAN_TAG = ITEMS.register("guardian_tag", GuardianTagItem::new);
    public static final RO<DragonBreathEmblemItem> DRAGON_BREATH_EMBLEM = ITEMS.register("dragon_breath_emblem", DragonBreathEmblemItem::new);
    public static final RO<SpeciesBreathAccessoryItem> LIGHTNING_DISCHARGE_DEVICE = ITEMS.register(
        "lightning_discharge_device", () -> new SpeciesBreathAccessoryItem(SpeciesBreathAccessoryItem.Species.LIGHTNING, "lightning_discharge_device")
    );
    public static final RO<SpeciesBreathAccessoryItem> STEAMING_DRINK = ITEMS.register(
        "steaming_drink", () -> new SpeciesBreathAccessoryItem(SpeciesBreathAccessoryItem.Species.FIRE, "steaming_drink")
    );
    public static final RO<SpeciesBreathAccessoryItem> ICY_DRINK = ITEMS.register(
        "icy_drink", () -> new SpeciesBreathAccessoryItem(SpeciesBreathAccessoryItem.Species.ICE, "icy_drink")
    );
    public static final RO<UltimateDragonBreathDeviceItem> ULTIMATE_DRAGON_BREATH_DEVICE = ITEMS.register(
        "ultimate_dragon_breath_device", UltimateDragonBreathDeviceItem::new
    );
    public static final RO<SustainingAccessoryItem> WARDING_EMBLEM = ITEMS.register(
        "warding_emblem", () -> new SustainingAccessoryItem(true, false)
    );
    public static final RO<SustainingAccessoryItem> NATURAL_DRAGON_HEART_NECKLACE = ITEMS.register(
        "natural_dragon_heart_necklace", () -> new SustainingAccessoryItem(false, true)
    );
    public static final RO<SustainingAccessoryItem> DRAGON_HEART_EMBLEM_NECKLACE = ITEMS.register(
        "dragon_heart_emblem_necklace", () -> new SustainingAccessoryItem(true, true)
    );
    public static final RO<ServiceDragonArmorItem> SERVICE_DRAGON_ARMOR_HEAD = ITEMS.register(
        "service_dragon_armor_head", () -> new ServiceDragonArmorItem(0)
    );
    public static final RO<ServiceDragonArmorItem> SERVICE_DRAGON_ARMOR_NECK = ITEMS.register(
        "service_dragon_armor_neck", () -> new ServiceDragonArmorItem(1)
    );
    public static final RO<ServiceDragonArmorItem> SERVICE_DRAGON_ARMOR_BODY = ITEMS.register(
        "service_dragon_armor_body", () -> new ServiceDragonArmorItem(2)
    );
    public static final RO<ServiceDragonArmorItem> SERVICE_DRAGON_ARMOR_TAIL = ITEMS.register(
        "service_dragon_armor_tail", () -> new ServiceDragonArmorItem(3)
    );
    // 1.2.7: 四套龙铠（各 head/neck/body/tail），对齐上游 ModItems.registerDragonAttire。
    private static final List<RO<ServiceDragonArmorItem>> KIMONO_ARMOR = registerDragonAttire("kimono", ServiceDragonArmorItem.Style.KIMONO);
    private static final List<RO<ServiceDragonArmorItem>> XIA_ARMOR = registerDragonAttire("xia", ServiceDragonArmorItem.Style.XIA);
    private static final List<RO<ServiceDragonArmorItem>> VIKING_ARMOR = registerDragonAttire("viking", ServiceDragonArmorItem.Style.VIKING);
    public static final RO<Item> ICON_NEW_JOURNEY = ITEMS.register("icon_new_journey", () -> new Item(new Properties()));
    public static final RO<Item> ICON_CLOSER_COMPANION = ITEMS.register("icon_closer_companion", () -> new Item(new Properties()));
    public static final RO<Item> ICON_MAID_FOR_HUNTING = ITEMS.register("icon_maid_for_hunting", () -> new Item(new Properties()));
    public static final RO<Item> ICON_TRINITY_OF_DRAGONS = ITEMS.register(
        "icon_trinity_of_dragons", () -> new Item(new Properties().rarity(Rarity.EPIC))
    );
    public static final RO<Item> DRAGON_MAID_GUIDE = ITEMS.register(
        "dragon_maid_guide", () -> new DragonMaidGuideItem(new Properties().stacksTo(1))
    );
    public static final RO<SustainingAccessoryItem> GUARDIAN_PENDANT = ITEMS.register(
        "guardian_pendant", () -> new SustainingAccessoryItem(true, false, true)
    );
    public static final RO<Item> DRAGON_RECALL_ALTAR = RecallContent.item();
    // 1.2: 战斗饰品（CombatAccessoryItem 的 Kind）
    public static final RO<CombatAccessoryItem> MAJESTY_EYE_PENDANT = ITEMS.register(
        "majesty_eye_pendant", () -> new CombatAccessoryItem(CombatAccessoryItem.Kind.MAJESTY)
    );
    public static final RO<CombatAccessoryItem> THORNFEATHER_CHARM = ITEMS.register(
        "thornfeather_charm", () -> new CombatAccessoryItem(CombatAccessoryItem.Kind.THORNFEATHER)
    );
    public static final RO<CombatAccessoryItem> VENOMFANG_PENDANT = ITEMS.register(
        "venomfang_pendant", () -> new CombatAccessoryItem(CombatAccessoryItem.Kind.VENOMFANG)
    );
    public static final RO<CombatAccessoryItem> FROSTY_TAIL_FIN = ITEMS.register(
        "frosty_tail_fin", () -> new CombatAccessoryItem(CombatAccessoryItem.Kind.FROSTY_TAIL)
    );
    public static final RO<CombatAccessoryItem> WILD_PENDANT = ITEMS.register(
        "wild_pendant", () -> new CombatAccessoryItem(CombatAccessoryItem.Kind.WILD)
    );
    public static final RO<CombatAccessoryItem> RENDING_CLAW = ITEMS.register(
        "rending_claw", () -> new CombatAccessoryItem(CombatAccessoryItem.Kind.RENDING)
    );
    public static final RO<CombatAccessoryItem> GUARDIAN_THORNFEATHER_EMBLEM = ITEMS.register(
        "guardian_thornfeather_emblem", () -> new CombatAccessoryItem(CombatAccessoryItem.Kind.GUARDIAN_THORNFEATHER)
    );
    public static final RO<CombatAccessoryItem> HUNTER_KING_PENDANT = ITEMS.register(
        "hunter_king_pendant", () -> new CombatAccessoryItem(CombatAccessoryItem.Kind.HUNTER_KING)
    );
    // 1.2.7: 翼卫传说（雷电龙 + 已驯服时的翼助推饰品）
    public static final RO<CombatAccessoryItem> WING_GUARD_LEGEND = ITEMS.register(
        "wing_guard_legend", () -> new CombatAccessoryItem(CombatAccessoryItem.Kind.WING_GUARD)
    );
    public static final RO<CombatAccessoryItem> MINIATURE_CHARM = ITEMS.register(
        "miniaturization_charm", () -> new CombatAccessoryItem(CombatAccessoryItem.Kind.MINI)
    );
    // 1.2.7: 24 件刀/太刀（12 组），由 port-combat 的 CompanionBladeItem 承载。
    private static final List<BladePair> CREATIVE_BLADES;
    private static final List<RO<ServiceDragonArmorItem>> FROSTSILVER_ARMOR = registerDragonAttire(
        "frostsilver", ServiceDragonArmorItem.Style.FROSTSILVER
    );

    private static List<RO<ServiceDragonArmorItem>> registerDragonAttire(String set, ServiceDragonArmorItem.Style style) {
        return List.of(
            ITEMS.register(set + "_dragon_armor_head", () -> new ServiceDragonArmorItem(style, 0)),
            ITEMS.register(set + "_dragon_armor_neck", () -> new ServiceDragonArmorItem(style, 1)),
            ITEMS.register(set + "_dragon_armor_body", () -> new ServiceDragonArmorItem(style, 2)),
            ITEMS.register(set + "_dragon_armor_tail", () -> new ServiceDragonArmorItem(style, 3))
        );
    }

    public static void addDragonAttireToTab(Output output) {
        KIMONO_ARMOR.forEach(item -> output.accept(item.get()));
        XIA_ARMOR.forEach(item -> output.accept(item.get()));
        VIKING_ARMOR.forEach(item -> output.accept(item.get()));
        FROSTSILVER_ARMOR.forEach(item -> output.accept(item.get()));
    }

    private static BladePair blades(String material, Supplier<Tier> tier, float guardReduction, boolean silver) {
        return new BladePair(
            ITEMS.register(material + "_jian", () -> new CompanionBladeItem(tier.get(), true, guardReduction, silver)),
            ITEMS.register(material + "_katana", () -> new CompanionBladeItem(tier.get(), false, 0.0F, silver))
        );
    }

    private static BladePair bloodBlades(String element, Supplier<Tier> tier, Supplier<Item> originalSword) {
        return new BladePair(
            ITEMS.register("dragonbone_jian_" + element, () -> new DragonBloodBladeItem(tier.get(), true, originalSword)),
            ITEMS.register("dragonbone_katana_" + element, () -> new DragonBloodBladeItem(tier.get(), false, originalSword))
        );
    }

    public static void addBladesToTab(Output output) {
        for (BladePair blades : CREATIVE_BLADES) {
            output.accept(blades.longsword().get());
        }

        for (BladePair blades : CREATIVE_BLADES) {
            output.accept(blades.katana().get());
        }
    }

    public ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    static {
        BladePair wooden = blades("wooden", () -> Tiers.WOOD, 0.25F, false);
        BladePair stone = blades("stone", () -> Tiers.STONE, 0.3F, false);
        BladePair copper = blades("copper", () -> IafTiers.COPPER_TOOL_MATERIAL, 0.3F, false);
        BladePair iron = blades("iron", () -> Tiers.IRON, 0.4F, false);
        BladePair golden = blades("golden", () -> Tiers.GOLD, 0.25F, false);
        BladePair diamond = blades("diamond", () -> Tiers.DIAMOND, 0.5F, false);
        BladePair netherite = blades("netherite", () -> Tiers.NETHERITE, 0.6F, false);
        BladePair dragonbone = blades("dragonbone", () -> IafTiers.DRAGONBONE_TOOL_MATERIAL, 0.6F, false);
        BladePair silver = blades("silver", () -> Tiers.STONE, 0.3F, true);
        BladePair fire = bloodBlades("fire", () -> IafTiers.BLOODED_DRAGONBONE_TOOL_MATERIAL, IafItems.DRAGONBONE_SWORD_FIRE);
        BladePair ice = bloodBlades("ice", () -> IafTiers.BLOODED_DRAGONBONE_TOOL_MATERIAL, IafItems.DRAGONBONE_SWORD_ICE);
        BladePair lightning = bloodBlades(
            "lightning", () -> IafTiers.BLOODED_DRAGONBONE_TOOL_MATERIAL, IafItems.DRAGONBONE_SWORD_LIGHTNING
        );
        CREATIVE_BLADES = List.of(wooden, stone, copper, silver, iron, golden, diamond, netherite, dragonbone, fire, ice, lightning);
    }

    private record BladePair(RO<? extends Item> longsword, RO<? extends Item> katana) {
    }
}
