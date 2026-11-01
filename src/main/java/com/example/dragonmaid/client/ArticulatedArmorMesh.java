package com.example.dragonmaid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;

/**
 * 1.20.1 -> 1.21.1 vertex-API port.
 *
 * <p>1.21.1 replaced the "attribute by attribute" VertexConsumer
 * ({@code vertex/color/uv/overlayCoords/uv2/normal/endVertex}) with a "per vertex
 * call" one: {@code addVertex(x,y,z) / setColor(r,g,b,a) / setUv(u,v) /
 * setUv1 / setUv2 / setNormal(x,y,z)}. {@code endVertex()} no longer exists, so a
 * capturing consumer has to emit the previous vertex when the next
 * {@code addVertex} starts (plus one final flush).</p>
 *
 * <p>ModelPart.Cube.compile() in 1.21.1 emits exactly one
 * {@code addVertex(x, y, z, color, u, v, overlay, light, nx, ny, nz)} per vertex,
 * which routes through the six abstract methods below.</p>
 */
final class ArticulatedArmorMesh {
    final List<ArticulatedArmorMesh.Vertex> vertices;

    private ArticulatedArmorMesh(List<ArticulatedArmorMesh.Vertex> vertices) {
        this.vertices = List.copyOf(vertices);
    }

    static List<ArticulatedArmorMesh.Vertex> capture(ModelPart part) {
        part.x = part.y = part.z = part.xRot = part.yRot = part.zRot = 0.0F;
        ArticulatedArmorMesh.Capture capture = new ArticulatedArmorMesh.Capture();
        part.render(new PoseStack(), capture, 0, OverlayTexture.NO_OVERLAY);
        capture.flush();
        return List.copyOf(capture.vertices);
    }

    static ArticulatedArmorMesh slice(List<ArticulatedArmorMesh.Vertex> source, float fromPixels, float toPixels, float originPixels) {
        ArrayList<ArticulatedArmorMesh.Vertex> arraylist = new ArrayList<>();

        for (int i = 0; i < source.size(); i += 4) {
            List<ArticulatedArmorMesh.Vertex> list = new ArrayList<>(source.subList(i, i + 4));
            list = clip(list, fromPixels / 16.0F, true);
            list = clip(list, toPixels / 16.0F, false);
            if (list.size() == 4) {
                for (ArticulatedArmorMesh.Vertex vertex : list) {
                    arraylist.add(new ArticulatedArmorMesh.Vertex(
                            vertex.x, vertex.y - originPixels / 16.0F, vertex.z,
                            vertex.u, vertex.v, vertex.nx, vertex.ny, vertex.nz));
                }
            } else {
                for (int j = 1; j + 1 < list.size(); j++) {
                    for (int k : new int[]{0, j, j + 1, j + 1}) {
                        ArticulatedArmorMesh.Vertex vertex = list.get(k);
                        arraylist.add(new ArticulatedArmorMesh.Vertex(
                                vertex.x, vertex.y - originPixels / 16.0F, vertex.z,
                                vertex.u, vertex.v, vertex.nx, vertex.ny, vertex.nz));
                    }
                }
            }
        }

        return new ArticulatedArmorMesh(arraylist);
    }

    private static List<ArticulatedArmorMesh.Vertex> clip(List<ArticulatedArmorMesh.Vertex> polygon, float boundary, boolean lower) {
        ArrayList<ArticulatedArmorMesh.Vertex> arraylist = new ArrayList<>();
        if (polygon.isEmpty()) {
            return arraylist;
        } else {
            ArticulatedArmorMesh.Vertex previous = polygon.get(polygon.size() - 1);
            boolean inside = lower ? previous.y >= boundary : previous.y <= boundary;

            for (ArticulatedArmorMesh.Vertex vertex : polygon) {
                boolean inside1 = lower ? vertex.y >= boundary : vertex.y <= boundary;
                if (inside != inside1) {
                    arraylist.add(previous.between(vertex, (boundary - previous.y) / (vertex.y - previous.y)));
                }

                if (inside1) {
                    arraylist.add(vertex);
                }

                previous = vertex;
                inside = inside1;
            }

            return arraylist;
        }
    }

    void render(PoseStack pose, VertexConsumer output, int light) {
        this.render(pose, output, light, 1.0F, 1.0F, 1.0F);
    }

    void render(PoseStack pose, VertexConsumer output, int light, float red, float green, float blue) {
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        int color = packColor(red, green, blue);

        for (ArticulatedArmorMesh.Vertex vertex : this.vertices) {
            pose.last().pose().transformPosition(position.set(vertex.x, vertex.y, vertex.z));
            pose.last().normal().transform(normal.set(vertex.nx, vertex.ny, vertex.nz)).normalize();
            // 1.21.1: single per-vertex call, color is a packed ARGB int
            output.addVertex(position.x, position.y, position.z, color,
                    vertex.u, vertex.v, OverlayTexture.NO_OVERLAY, light,
                    normal.x, normal.y, normal.z);
        }
    }

    private static int packColor(float red, float green, float blue) {
        return 0xFF000000 | (toByte(red) << 16) | (toByte(green) << 8) | toByte(blue);
    }

    private static int toByte(float value) {
        int i = (int) (value * 255.0F + 0.5F);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    private static final class Capture implements VertexConsumer {
        final List<ArticulatedArmorMesh.Vertex> vertices = new ArrayList<>();
        float x;
        float y;
        float z;
        float u;
        float v;
        float nx;
        float ny;
        float nz;
        boolean pending;

        private Capture() {
        }

        void flush() {
            if (this.pending) {
                this.vertices.add(new ArticulatedArmorMesh.Vertex(this.x, this.y, this.z, this.u, this.v, this.nx, this.ny, this.nz));
                this.pending = false;
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.flush();
            this.x = x;
            this.y = y;
            this.z = z;
            this.pending = true;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.nx = x;
            this.ny = y;
            this.nz = z;
            return this;
        }
    }

    static record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        ArticulatedArmorMesh.Vertex between(ArticulatedArmorMesh.Vertex b, float t) {
            return new ArticulatedArmorMesh.Vertex(
                    this.x + (b.x - this.x) * t,
                    this.y + (b.y - this.y) * t,
                    this.z + (b.z - this.z) * t,
                    this.u + (b.u - this.u) * t,
                    this.v + (b.v - this.v) * t,
                    this.nx, this.ny, this.nz);
        }
    }
}
