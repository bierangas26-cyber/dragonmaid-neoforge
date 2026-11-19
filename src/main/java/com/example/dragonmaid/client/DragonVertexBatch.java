package com.example.dragonmaid.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 连续翼膜的顶点批量提交垫片。
 *
 * <p><b>降级说明（1.21.1）：</b>上游 1.2.7 走两条快速通道——直接写 {@code BufferBuilder} 的
 * 内部 {@code ByteBuffer}（1.20.1 的 {@code BufferBuilder#putBulkData(ByteBuffer)}，1.21.1 已删除），
 * 以及 Embeddium / Oculus 的 {@code VertexBufferWriter} / {@code IrisExtendedBufferBuilder}
 * （本仓 libs 无 sodium/iris，编译期不可见）。因此这里保留上游的公开 API 形状，
 * 但 {@link #target} 恒返回 {@code null}，让 {@code ContinuousWingMesh} 走
 * 「逐顶点 addVertex(...)」的通用分支（视觉一致，仅失去批量提交的性能优化）。
 */
final class DragonVertexBatch {
    private static final int STRIDE = 36;
    private static final int CAPACITY_BYTES = 36864;
    private static final int MAX_VERTICES = CAPACITY_BYTES / STRIDE;

    private DragonVertexBatch() {
    }

    /** 1.21.1 降级：不再启用直接顶点批量通道。 */
    static BufferBuilder target(VertexConsumer consumer) {
        return null;
    }

    /** 与 1.21.1 {@code VertexConsumer#setColor(int)} 一致的 ARGB 打包。 */
    static int color(float red, float green, float blue, float alpha) {
        return (int)(alpha * 255.0F) << 24
            | (int)(red * 255.0F) << 16
            | (int)(green * 255.0F) << 8
            | (int)(blue * 255.0F);
    }

    static int normal(float x, float y, float z) {
        return normalByte(x) | normalByte(y) << 8 | normalByte(z) << 16;
    }

    private static int normalByte(float value) {
        return (int)(Math.max(-1.0F, Math.min(1.0F, value)) * 127.0F) & 0xFF;
    }

    static int maxVertices() {
        return MAX_VERTICES;
    }

    /** 快速通道关闭时不会被调用；保留实现以便日后恢复批量提交。 */
    static void vertex(BufferBuilder builder, float x, float y, float z, float u, float v, int color, int overlay, int light, int normal) {
        builder.addVertex(x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(unpackNormal(normal, 0), unpackNormal(normal, 1), unpackNormal(normal, 2));
    }

    private static float unpackNormal(int packed, int component) {
        return (float)((byte)(packed >> (component * 8)) / 127.0F);
    }

    static void flush(BufferBuilder builder) {
        // 无缓冲，无需 flush。
    }

    static int noOverlay() {
        return OverlayTexture.NO_OVERLAY;
    }
}
