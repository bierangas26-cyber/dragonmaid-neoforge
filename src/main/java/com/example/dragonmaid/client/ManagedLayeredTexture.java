package com.example.dragonmaid.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

final class ManagedLayeredTexture extends AbstractTexture {
    private final List<ResourceLocation> layers;
    private ManagedLayeredTexture.PendingImage pending;
    private boolean closed;

    ManagedLayeredTexture(List<String> layers) {
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("A layered texture needs a base image");
        } else {
            // 1.21.1: ResourceLocation's constructor is private -> use the parse factory
            this.layers = layers.stream().<ResourceLocation>map(ResourceLocation::parse).toList();
        }
    }

    public void load(ResourceManager resources) throws IOException {
        ManagedLayeredTexture.PendingImage managedlayeredtexture$pendingimage = new ManagedLayeredTexture.PendingImage(this.compose(resources));
        ManagedLayeredTexture.PendingImage managedlayeredtexture$pendingimage1;
        synchronized (this) {
            if (this.closed) {
                managedlayeredtexture$pendingimage.close();
                throw new IOException("Service layered texture already closed");
            }

            managedlayeredtexture$pendingimage1 = this.pending;
            this.pending = managedlayeredtexture$pendingimage;
        }

        if (managedlayeredtexture$pendingimage1 != null) {
            managedlayeredtexture$pendingimage1.close();
        }

        try {
            if (RenderSystem.isOnRenderThreadOrInit()) {
                this.upload(managedlayeredtexture$pendingimage);
            } else {
                RenderSystem.recordRenderCall(() -> this.upload(managedlayeredtexture$pendingimage));
            }
        } catch (Error | RuntimeException runtimeexception) {
            synchronized (this) {
                if (this.pending == managedlayeredtexture$pendingimage) {
                    this.pending = null;
                }
            }

            managedlayeredtexture$pendingimage.close();
            throw runtimeexception;
        }
    }

    private NativeImage compose(ResourceManager resources) throws IOException {
        NativeImage nativeimage = read(resources, this.layers.get(0));
        boolean flag = false;

        NativeImage nativeimage2;
        try {
            for (int i = 1; i < this.layers.size(); i++) {
                NativeImage nativeimage1 = read(resources, this.layers.get(i));

                try {
                    for (int j = 0; j < Math.min(nativeimage1.getHeight(), nativeimage.getHeight()); j++) {
                        for (int k = 0; k < Math.min(nativeimage1.getWidth(), nativeimage.getWidth()); k++) {
                            nativeimage.blendPixel(k, j, nativeimage1.getPixelRGBA(k, j));
                        }
                    }
                } catch (Throwable throwable1) {
                    if (nativeimage1 != null) {
                        try {
                            nativeimage1.close();
                        } catch (Throwable throwable) {
                            throwable1.addSuppressed(throwable);
                        }
                    }

                    throw throwable1;
                }

                if (nativeimage1 != null) {
                    nativeimage1.close();
                }
            }

            flag = true;
            nativeimage2 = nativeimage;
        } finally {
            if (!flag) {
                nativeimage.close();
            }
        }

        return nativeimage2;
    }

    private static NativeImage read(ResourceManager resources, ResourceLocation location) throws IOException {
        NativeImage nativeimage = null;

        try {
            try (InputStream inputstream = resources.getResourceOrThrow(location).open()) {
                nativeimage = NativeImage.read(inputstream);
            }

            return nativeimage;
        } catch (RuntimeException | Error | IOException ioexception) {
            if (nativeimage != null) {
                nativeimage.close();
            }

            throw ioexception;
        }
    }

    private void upload(ManagedLayeredTexture.PendingImage next) {
        NativeImage nativeimage = next.take();
        if (nativeimage != null) {
            synchronized (this) {
                try {
                    if (!this.closed && this.pending == next) {
                        this.pending = null;
                        RenderSystem.assertOnRenderThreadOrInit();
                        TextureUtil.prepareImage(this.getId(), nativeimage.getWidth(), nativeimage.getHeight());
                        nativeimage.upload(0, 0, 0, false);
                        return;
                    }
                } finally {
                    nativeimage.close();
                }
            }
        }
    }

    public void close() {
        ManagedLayeredTexture.PendingImage managedlayeredtexture$pendingimage;
        synchronized (this) {
            this.closed = true;
            managedlayeredtexture$pendingimage = this.pending;
            this.pending = null;
        }

        if (managedlayeredtexture$pendingimage != null) {
            managedlayeredtexture$pendingimage.close();
        }
    }

    private static final class PendingImage {
        private NativeImage image;

        PendingImage(NativeImage image) {
            this.image = image;
        }

        synchronized NativeImage take() {
            NativeImage nativeimage = this.image;
            this.image = null;
            return nativeimage;
        }

        void close() {
            NativeImage nativeimage = this.take();
            if (nativeimage != null) {
                nativeimage.close();
            }
        }
    }
}
