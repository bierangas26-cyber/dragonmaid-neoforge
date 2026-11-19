package com.example.dragonmaid.client;

import com.example.dragonmaid.item.ModItems;
import com.example.dragonmaid.item.ServiceDragonArmorItem;
import com.example.dragonmaid.menu.ModMenus;
import com.example.dragonmaid.particle.ModParticles;
import com.example.dragonmaid.recall.RecallClientContent;
import com.iafenvoy.iceandfire.item.DragonHornItem;
import com.iafenvoy.iceandfire.item.SummoningCrystalItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.LavaParticle.Provider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import software.bernie.geckolib.loading.math.MolangQueries;

@EventBusSubscriber(
    modid = "dragonmaid",
    value = {Dist.CLIENT},
    bus = Bus.MOD
)
public final class ClientRegistration {
    static final Map<FemaleDragonAssets, IceFemaleRenderer[]> dragonRenderers = new EnumMap<>(FemaleDragonAssets.class);

    public ClientRegistration() {
    }

    /**
     * 1.2.7: 四套龙铠（service/kimono/xia/viking/frostsilver）各有自己的 armor contract 与物种贴图；
     * 这里按 Style 逐套注册 reload listener（旧写法只注册 service 单例，其余四套会退化成 service 外观）。
     * 另加两条 1.2.7 新增资源：连续翼膜网格、第一人称刀动作 JSON。
     */
    @SubscribeEvent
    public static void chestplateResources(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(FemaleChestplateResources.INSTANCE);

        for (ServiceDragonArmorItem.Style style : ServiceDragonArmorItem.Style.values()) {
            event.registerReloadListener(ServiceArmorResources.of(style));
        }

        event.registerReloadListener(ContinuousWingResources.INSTANCE);
        event.registerReloadListener(PlayerBladeMotion.INSTANCE);
    }

    static void clearWorldState() {
        HumanoidStatueRenderer.clear();
        boolean flag = false;

        for (IceFemaleRenderer[] aicefemalerenderer : dragonRenderers.values()) {
            for (IceFemaleRenderer icefemalerenderer : aicefemalerenderer) {
                flag |= icefemalerenderer.clearWorldState();
            }
        }

        if (flag) {
            // 4.9.2 port: core.molang.MolangParser was removed (queries are now resolved per
            // AnimationState tick instead of being globally cached). The closest public API is
            // MolangQueries.clearActor(), which drops the cached actor so the next world starts
            // from the default query values instead of the last rendered entity's.
            MolangQueries.clearActor();
        }
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
            (ParticleType)ModParticles.FIRE_WISP.get(),
            s -> (t, l, x, y, z, dx, dy, dz) -> new FadingBreathParticle(l, x, y, z, dx, dy, dz, s, BreathParticleStyle.WISP)
        );
        event.registerSpriteSet(
            (ParticleType)ModParticles.FIRE_IMPACT.get(),
            s -> (t, l, x, y, z, dx, dy, dz) -> new FadingBreathParticle(l, x, y, z, dx, dy, dz, s, BreathParticleStyle.FIRE_IMPACT)
        );
        event.registerSpriteSet(
            (ParticleType)ModParticles.ICE_IMPACT.get(),
            s -> (t, l, x, y, z, dx, dy, dz) -> new FadingBreathParticle(l, x, y, z, dx, dy, dz, s, BreathParticleStyle.ICE_IMPACT)
        );
        event.registerSpriteSet((ParticleType)ModParticles.FIRE_EMBER.get(), s -> nativeFire(s, false));
        event.registerSpriteSet((ParticleType)ModParticles.FIRE_CORE.get(), s -> nativeFire(s, true));
        event.registerSpriteSet(
            (ParticleType)ModParticles.FIRE_SPARK.get(),
            s -> (t, l, x, y, z, dx, dy, dz) -> new FadingBreathParticle(l, x, y, z, dx, dy, dz, s, BreathParticleStyle.SPARK)
        );
        event.registerSpriteSet(
            (ParticleType)ModParticles.FIRE_SMOKE.get(),
            s -> (t, l, x, y, z, dx, dy, dz) -> new FadingBreathParticle(l, x, y, z, dx, dy, dz, s, BreathParticleStyle.SMOKE)
        );
        event.registerSpriteSet(
            (ParticleType)ModParticles.ICE_MIST.get(),
            s -> (t, l, x, y, z, dx, dy, dz) -> new FadingBreathParticle(l, x, y, z, dx, dy, dz, s, BreathParticleStyle.MIST)
        );
        event.registerSpriteSet(
            (ParticleType)ModParticles.ICE_SHARD.get(),
            s -> (t, l, x, y, z, dx, dy, dz) -> new FadingBreathParticle(l, x, y, z, dx, dy, dz, s, BreathParticleStyle.SHARD)
        );
        event.registerSpriteSet(
            (ParticleType)ModParticles.FIRE_BREATH.get(),
            sprites -> (type, level, x, y, z, dx, dy, dz) -> new FadingBreathParticle(level, x, y, z, dx, dy, dz, sprites, BreathParticleStyle.FIRE)
        );
        event.registerSpriteSet(
            (ParticleType)ModParticles.ICE_BREATH.get(),
            sprites -> (type, level, x, y, z, dx, dy, dz) -> new FadingBreathParticle(level, x, y, z, dx, dy, dz, sprites, BreathParticleStyle.ICE)
        );
        // 1.2.7: 太刀/长剑的弧形刀光。网络速度分量被复用成参数：
        //   dx = yaw（弧度）、dy = ±reach（负值表示太刀前倾弧）、dz = 半弧角/俯仰。
        event.registerSpriteSet(
            (ParticleType)ModParticles.BLADE_SWEEP.get(),
            sprites -> (type, level, x, y, z, yaw, radius, angleOrPitch) ->
                new BladeSweepParticle((ClientLevel)level, x, y, z, yaw, radius, angleOrPitch, sprites)
        );
    }

    private static ParticleProvider<SimpleParticleType> nativeFire(SpriteSet sprites, boolean stream) {
        Provider provider = new Provider(sprites);
        return (type, level, x, y, z, dx, dy, dz) -> {
            Particle particle = provider.createParticle(type, level, x, y, z, dx, dy, dz);
            if (particle != null && stream) {
                particle.setParticleSpeed(dx, dy + 0.04, dz);
                particle.setLifetime(BreathFxTuning.fireCoreLifetime() + level.random.nextInt(3));
            }

            return particle;
        };
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.KNOWLEDGE.get(), KnowledgeScreen::new);
        event.register(ModMenus.ACCESSORIES.get(), AccessoryScreen::new);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(
            () -> {
                // 1.21.1: menus are registered in screens(RegisterMenuScreensEvent); RecallClientContent self-registers too
                ItemProperties.register(
                    (Item)ModItems.REINFORCED_DRAGON_HORN.get(),
                    ResourceLocation.parse("iceorfire"),
                    (stack, level, entity, seed) -> (float)DragonHornItem.getDragonType(stack) / 4.0F
                );
                ItemProperties.register(
                    (Item)ModItems.TRINITY_DRAGON_GEM.get(),
                    ResourceLocation.parse("has_dragon"),
                    (stack, level, entity, seed) -> SummoningCrystalItem.hasDragon(stack) ? 1.0F : 0.0F
                );
            }
        );
    }

    @SubscribeEvent
    public static void renderers(AddLayers event) {
        clearWorldState();
        dragonRenderers.clear();

        for (FemaleDragonAssets femaledragonassets : FemaleDragonAssets.values()) {
            dragonRenderers.put(femaledragonassets, femaleStages(event.getContext(), femaledragonassets));
        }

        // 1.2.7: 玩家腰间的太刀刀鞘层（KatanaSheathLayer）。1.21.1 的 AddLayers 用 PlayerSkin.Model 枚举
        // （WIDE/SLIM）而不是 1.20.1 的 String skin，因此遍历 getSkins() 并逐个挂层。
        for (PlayerSkin.Model model : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new KatanaSheathLayer(playerRenderer));
            }
        }
    }

    private static IceFemaleRenderer[] femaleStages(Context context, FemaleDragonAssets assets) {
        return new IceFemaleRenderer[]{
            new IceFemaleRenderer(context, assets, 3), new IceFemaleRenderer(context, assets, 4), new IceFemaleRenderer(context, assets, 5)
        };
    }
}
