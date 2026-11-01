package com.example.dragonmaid.client;

import com.example.dragonmaid.mixin.client.StatueCompositeRenderTypeAccessor;
import com.example.dragonmaid.mixin.client.StatueCompositeStateAccessor;
import com.example.dragonmaid.mixin.client.StatueTextureStateInvoker;
import com.iafenvoy.iceandfire.registry.IafRenderTypes;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public final class HumanoidStatueMaterials {
    private static final int CAPACITY = 96;
    private static final Map<HumanoidStatueMaterials.Key, HumanoidStatueMaterials.Material> CACHE = new LinkedHashMap<>(16, 0.75F, true);
    private static final Map<HumanoidStatueMaterials.Key, HumanoidStatueMaterials.Material> RETIRED = new LinkedHashMap<>();
    private static final Map<RenderType, Optional<ResourceLocation>> SOURCES = new LinkedHashMap<>(16, 0.75F, true);
    private static final Map<ResourceLocation, RenderType> PLAIN = new LinkedHashMap<>(16, 0.75F, true);
    private static long generation;
    private static boolean renderingFrame;

    private HumanoidStatueMaterials() {
    }

    public static MultiBufferSource wrap(MultiBufferSource buffers, ResourceLocation material) {
        return requested -> buffers.getBuffer(resolve(requested, material));
    }

    public static void beginFrame() {
        renderingFrame = true;
    }

    public static void endFrame() {
        renderingFrame = false;
        releaseRetired();
    }

    public static void clear() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(HumanoidStatueMaterials::clear);
        } else {
            RETIRED.putAll(CACHE);
            CACHE.clear();
            SOURCES.clear();
            PLAIN.clear();
            generation++;
            if (!renderingFrame) {
                releaseRetired();
            }
        }
    }

    private static RenderType resolve(RenderType requested, ResourceLocation material) {
        RenderSystem.assertOnRenderThreadOrInit();
        Optional<ResourceLocation> optional = SOURCES.get(requested);
        if (optional == null) {
            optional = sourceTexture(requested);
            SOURCES.put(requested, optional);
            trim(SOURCES);
        }

        if (optional.isEmpty()) {
            return plain(material);
        } else {
            HumanoidStatueMaterials.Key humanoidstatuematerials$key = new HumanoidStatueMaterials.Key(optional.get(), material, generation);
            HumanoidStatueMaterials.Material humanoidstatuematerials$material = CACHE.get(humanoidstatuematerials$key);
            if (humanoidstatuematerials$material != null) {
                return humanoidstatuematerials$material.renderType();
            } else {
                humanoidstatuematerials$material = RETIRED.remove(humanoidstatuematerials$key);
                if (humanoidstatuematerials$material == null) {
                    humanoidstatuematerials$material = compose(humanoidstatuematerials$key);
                }

                CACHE.put(humanoidstatuematerials$key, humanoidstatuematerials$material);

                while (CACHE.size() > 96) {
                    Iterator<Entry<HumanoidStatueMaterials.Key, HumanoidStatueMaterials.Material>> iterator = CACHE.entrySet().iterator();
                    Entry<HumanoidStatueMaterials.Key, HumanoidStatueMaterials.Material> entry = iterator.next();
                    iterator.remove();
                    RETIRED.put(entry.getKey(), entry.getValue());
                }

                return humanoidstatuematerials$material.renderType();
            }
        }
    }

    private static Optional<ResourceLocation> sourceTexture(RenderType requested) {
        if (requested instanceof StatueCompositeRenderTypeAccessor statuecompositerendertypeaccessor) {
            try {
                // mixin interfaces are not visible to javac (only at runtime) -> go through Object
                StatueCompositeStateAccessor statuecompositestateaccessor = (StatueCompositeStateAccessor)(Object)statuecompositerendertypeaccessor.dragonmaid$statueCompositeState();
                StatueTextureStateInvoker statuetexturestateinvoker = (StatueTextureStateInvoker)statuecompositestateaccessor.dragonmaid$statueTextureState();
                return statuetexturestateinvoker.dragonmaid$statueCutoutTexture();
            } catch (RuntimeException runtimeexception) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private static HumanoidStatueMaterials.Material compose(HumanoidStatueMaterials.Key key) {
        NativeImage nativeimage = null;
        DynamicTexture dynamictexture = null;

        HumanoidStatueMaterials.Material material;
        try {
            NativeImage nativeimage1 = read(key.source());

            HumanoidStatueMaterials.Material humanoidstatuematerials$material1;
            try {
                NativeImage nativeimage2 = read(key.material());

                try {
                    nativeimage = new NativeImage(nativeimage1.getWidth(), nativeimage1.getHeight(), false);

                    for (int i = 0; i < nativeimage.getHeight(); i++) {
                        for (int j = 0; j < nativeimage.getWidth(); j++) {
                            int k = nativeimage2.getPixelRGBA(j % nativeimage2.getWidth(), i % nativeimage2.getHeight());
                            int l = (nativeimage1.getPixelRGBA(j, i) >>> 24) * (k >>> 24) / 255;
                            nativeimage.setPixelRGBA(j, i, k & 16777215 | l << 24);
                        }
                    }

                    String s = Hashing.sha256().hashString(key.source() + "\n" + key.material(), StandardCharsets.UTF_8).toString();
                    // 1.21.1: the ResourceLocation constructor is private now
                    ResourceLocation resourcelocation = ResourceLocation.fromNamespaceAndPath(
                        "dragonmaid", "statue_material/" + Long.toUnsignedString(key.generation(), 16) + "/" + s
                    );
                    RenderType rendertype = materialType(resourcelocation, key.material());
                    dynamictexture = new DynamicTexture(nativeimage);
                    nativeimage = null;
                    Minecraft.getInstance().getTextureManager().register(resourcelocation, dynamictexture);
                    dynamictexture = null;
                    humanoidstatuematerials$material1 = new HumanoidStatueMaterials.Material(resourcelocation, rendertype);
                } catch (Throwable throwable2) {
                    if (nativeimage2 != null) {
                        try {
                            nativeimage2.close();
                        } catch (Throwable throwable1) {
                            throwable2.addSuppressed(throwable1);
                        }
                    }

                    throw throwable2;
                }

                if (nativeimage2 != null) {
                    nativeimage2.close();
                }
            } catch (Throwable throwable3) {
                if (nativeimage1 != null) {
                    try {
                        nativeimage1.close();
                    } catch (Throwable throwable) {
                        throwable3.addSuppressed(throwable);
                    }
                }

                throw throwable3;
            }

            if (nativeimage1 != null) {
                nativeimage1.close();
            }

            return humanoidstatuematerials$material1;
        } catch (RuntimeException | IOException ioexception) {
            material = new HumanoidStatueMaterials.Material(null, plain(key.material()));
        } finally {
            if (dynamictexture != null) {
                dynamictexture.close();
            }

            if (nativeimage != null) {
                nativeimage.close();
            }
        }

        return material;
    }

    private static NativeImage read(ResourceLocation location) throws IOException {
        NativeImage nativeimage = null;

        try {
            NativeImage nativeimage1;
            try (InputStream inputstream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location).open()) {
                nativeimage = NativeImage.read(inputstream);
                nativeimage1 = nativeimage;
            }

            return nativeimage1;
        } catch (RuntimeException | IOException ioexception) {
            if (nativeimage != null) {
                nativeimage.close();
            }

            throw ioexception;
        }
    }

    private static RenderType plain(ResourceLocation material) {
        RenderType rendertype = PLAIN.get(material);
        if (rendertype == null) {
            rendertype = materialType(material, material);
            PLAIN.put(material, rendertype);
            trim(PLAIN);
        }

        return rendertype;
    }

    private static RenderType materialType(ResourceLocation texture, ResourceLocation material) {
        return material.getPath().contains("destroy_stage_") ? IafRenderTypes.getStoneCrackRenderType(texture) : RenderType.entityCutoutNoCull(texture);
    }

    private static void releaseRetired() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(HumanoidStatueMaterials::releaseRetired);
        } else {
            for (HumanoidStatueMaterials.Material humanoidstatuematerials$material : RETIRED.values()) {
                if (humanoidstatuematerials$material.runtimeTexture() != null) {
                    Minecraft.getInstance().getTextureManager().release(humanoidstatuematerials$material.runtimeTexture());
                }
            }

            RETIRED.clear();
        }
    }

    private static <K, V> void trim(Map<K, V> cache) {
        while (cache.size() > 96) {
            Iterator<K> iterator = cache.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private static record Key(ResourceLocation source, ResourceLocation material, long generation) {
    }

    private static record Material(ResourceLocation runtimeTexture, RenderType renderType) {
    }
}
