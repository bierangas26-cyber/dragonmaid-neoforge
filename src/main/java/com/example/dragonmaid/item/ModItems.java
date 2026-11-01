package com.example.dragonmaid.item;

import com.example.dragonmaid.recall.RecallContent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
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

    public ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
