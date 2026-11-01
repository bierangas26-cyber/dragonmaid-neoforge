package com.example.dragonmaid.client;

import com.example.dragonmaid.item.ModItems;
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
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
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

    @SubscribeEvent
    public static void chestplateResources(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(FemaleChestplateResources.INSTANCE);
        event.registerReloadListener(ServiceArmorResources.INSTANCE);
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
    }

    private static IceFemaleRenderer[] femaleStages(Context context, FemaleDragonAssets assets) {
        return new IceFemaleRenderer[]{
            new IceFemaleRenderer(context, assets, 3), new IceFemaleRenderer(context, assets, 4), new IceFemaleRenderer(context, assets, 5)
        };
    }
}
