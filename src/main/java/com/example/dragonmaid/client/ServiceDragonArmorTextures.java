package com.example.dragonmaid.client;

import com.example.dragonmaid.item.ServiceDragonArmorItem;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.render.entity.feature.DragonArmorFeatureRenderer;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ServiceDragonArmorTextures {
    private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int CACHE_CAPACITY = 96;
    private static final long IDLE_NANOS = TimeUnit.SECONDS.toNanos(120L);
    private static final long MAINTENANCE_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final Set<ResourceLocation> RETIRED = new LinkedHashSet<>();
    private static final IdleResourceCache<List<String>, ResourceLocation> CACHE = new IdleResourceCache<>(96, IDLE_NANOS, RETIRED::add);
    private static long nextMaintenance;
    private static long generation;
    private static boolean renderingFrame;

    private ServiceDragonArmorTextures() {
    }

    public static boolean hasServiceArmor(DragonBaseEntity dragon) {
        if (dragon.dragonInventory == null) {
            return false;
        } else {
            for (EquipmentSlot equipmentslot : SLOTS) {
                if (ServiceDragonArmorItem.isService(dragon.getItemBySlot(equipmentslot))) {
                    return true;
                }
            }

            return false;
        }
    }

    public static ResourceLocation texture(DragonBaseEntity dragon) {
        RenderSystem.assertOnRenderThread();
        ArrayList<String> arraylist = new ArrayList<>(4);

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack itemstack = dragon.getItemBySlot(SLOTS[i]);
            // IAF CE 2.0: EnumDragonTextures.Armor (and its per-dragon-type
            // ICE/LIGHTNING/FIRE textures) is gone. CE keeps one texture per
            // material+part and returns null for an empty / non-dragon-armor slot;
            // skipping those layers is CE's own semantics (see
            // DragonArmorFeatureRenderer#render), not a downgrade.
            ResourceLocation resourcelocation = ServiceDragonArmorItem.isService(itemstack)
                ? ServiceArmorResources.INSTANCE.dragonTexture(dragon, i + 1)
                : DragonArmorFeatureRenderer.getArmorTexture(itemstack, SLOTS[i]);
            if (resourcelocation != null) {
                arraylist.add(resourcelocation.toString());
            }
        }

        if (arraylist.isEmpty()) {
            // nothing worn -> callers must skip drawing (same contract as CE's renderer)
            return null;
        }

        List<String> list = List.copyOf(arraylist);
        return CACHE.get(
            list,
            System.nanoTime(),
            () -> {
                HashCode hashcode = Hashing.sha256().hashString(String.join("\n", list), StandardCharsets.UTF_8);
                // 1.21.1: the ResourceLocation constructor is private now
                ResourceLocation resourcelocation1 = ResourceLocation.fromNamespaceAndPath(
                    "dragonmaid", "service_armor/composite/" + Long.toUnsignedString(generation, 16) + "/" + hashcode
                );
                if (!RETIRED.remove(resourcelocation1)) {
                    Minecraft.getInstance().getTextureManager().register(resourcelocation1, new ManagedLayeredTexture(list));
                }

                return resourcelocation1;
            }
        );
    }

    static void beginFrame() {
        renderingFrame = true;
    }

    static void endFrame() {
        renderingFrame = false;
        releaseRetired();
    }

    static void tick() {
        long i = System.nanoTime();
        if (nextMaintenance == 0L || i - nextMaintenance >= 0L) {
            nextMaintenance = i + MAINTENANCE_NANOS;
            CACHE.evictIdle(i);
            if (!renderingFrame) {
                releaseRetired();
            }
        }
    }

    static void clearCache() {
        Runnable runnable = () -> {
            CACHE.clear();
            generation++;
            nextMaintenance = 0L;
            if (!renderingFrame) {
                releaseRetired();
            }
        };
        if (RenderSystem.isOnRenderThread()) {
            runnable.run();
        } else {
            RenderSystem.recordRenderCall(runnable::run);
        }
    }

    private static void releaseRetired() {
        RenderSystem.assertOnRenderThread();
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        RETIRED.forEach(texturemanager::release);
        RETIRED.clear();
    }
}
