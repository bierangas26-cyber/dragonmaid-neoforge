package com.example.dragonmaid;

import com.example.dragonmaid.compat.RO;
import com.example.dragonmaid.compat.Reg;
import com.example.dragonmaid.config.GlobalBalanceConfig;
import com.example.dragonmaid.entity.ModEntities;
import com.example.dragonmaid.item.ModItems;
import com.example.dragonmaid.learning.KnowledgeUpgrades;
import com.example.dragonmaid.menu.ModMenus;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.particle.ModParticles;
import com.example.dragonmaid.recall.RecallContent;
import com.example.dragonmaid.recipe.ModRecipes;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(DragonMaid.MODID)
public class DragonMaid {
    public static final String MODID = "dragonmaid";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Reg<Block> BLOCKS = Reg.create(BuiltInRegistries.BLOCK, MODID);
    public static final Reg<CreativeModeTab> CREATIVE_MODE_TABS = Reg.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RO<Block> DRAGON_RECALL_ALTAR = RecallContent.block();
    public static final RO<CreativeModeTab> DRAGON_MAID_TAB = CREATIVE_MODE_TABS.register(
        "dragon_maid_tab",
        () -> CreativeModeTab.builder()
                .icon(() -> ModItems.DRAGON_MAID_GUIDE.get().getDefaultInstance())
                .title(Component.translatable("creativetab.dragonmaid.dragon_maid_tab"))
                .displayItems((parameters, output) -> {
                    output.accept((ItemLike)ModItems.DRAGON_MAID_GUIDE.get());
                    output.accept((ItemLike)ModItems.KAISHAKU_BLADE.get());
                    output.accept((ItemLike)ModItems.PETRIFICATION_REMEDY.get());
                    output.accept((ItemLike)ModItems.TRINITY_DRAGON_GEM.get());
                    output.accept((ItemLike)ModItems.REINFORCED_DRAGON_HORN.get());
                    output.accept((ItemLike)ModItems.DRAGON_RECALL_ALTAR.get());
                    output.accept((ItemLike)ModItems.GUARDIAN_TAG.get());
                    output.accept((ItemLike)ModItems.WARDING_EMBLEM.get());
                    output.accept((ItemLike)ModItems.GUARDIAN_PENDANT.get());
                    output.accept((ItemLike)ModItems.NATURAL_DRAGON_HEART_NECKLACE.get());
                    output.accept((ItemLike)ModItems.DRAGON_HEART_EMBLEM_NECKLACE.get());
                    output.accept((ItemLike)ModItems.DRAGON_BREATH_EMBLEM.get());
                    output.accept((ItemLike)ModItems.LIGHTNING_DISCHARGE_DEVICE.get());
                    output.accept((ItemLike)ModItems.STEAMING_DRINK.get());
                    output.accept((ItemLike)ModItems.ICY_DRINK.get());
                    output.accept((ItemLike)ModItems.ULTIMATE_DRAGON_BREATH_DEVICE.get());
                    output.accept((ItemLike)ModItems.SERVICE_DRAGON_ARMOR_HEAD.get());
                    output.accept((ItemLike)ModItems.SERVICE_DRAGON_ARMOR_NECK.get());
                    output.accept((ItemLike)ModItems.SERVICE_DRAGON_ARMOR_BODY.get());
                    output.accept((ItemLike)ModItems.SERVICE_DRAGON_ARMOR_TAIL.get());
                    output.accept((ItemLike)ModItems.MAJESTY_EYE_PENDANT.get());
                    output.accept((ItemLike)ModItems.THORNFEATHER_CHARM.get());
                    output.accept((ItemLike)ModItems.VENOMFANG_PENDANT.get());
                    output.accept((ItemLike)ModItems.FROSTY_TAIL_FIN.get());
                    output.accept((ItemLike)ModItems.WILD_PENDANT.get());
                    output.accept((ItemLike)ModItems.RENDING_CLAW.get());
                    output.accept((ItemLike)ModItems.GUARDIAN_THORNFEATHER_EMBLEM.get());
                    output.accept((ItemLike)ModItems.HUNTER_KING_PENDANT.get());
                    output.accept((ItemLike)ModItems.MINIATURE_CHARM.get());
                })
                .build()
    );

    public DragonMaid(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModEntities.register(modEventBus);
        ModParticles.TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        BLOCKS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        DragonNetwork.register();
        modEventBus.addListener(DragonNetwork::registerPayloads);
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.SERVER, DragonMaidConfig.SPEC, "dragonmaid-sync-server.toml");
        RecallContent.setup(modContainer);
        GlobalBalanceConfig.register();
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(KnowledgeUpgrades::registerDefaults);
        LOGGER.info("HELLO FROM COMMON SETUP");
        if (Config.logDirtBlock) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach(item -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(
        modid = "dragonmaid",
        bus = EventBusSubscriber.Bus.MOD,
        value = {Dist.CLIENT}
    )
    public static class ClientModEvents {
        public ClientModEvents() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            DragonMaid.LOGGER.info("HELLO FROM CLIENT SETUP");
            DragonMaid.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
