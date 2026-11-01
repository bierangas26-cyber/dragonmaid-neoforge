package com.example.dragonmaid.client;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.loading.json.raw.FaceUV.Rotation;
import software.bernie.geckolib.loading.object.BakedModelFactory.VertexSet;

final class TailArmorMesh {
    private static final int[][] ISLANDS = new int[][]{
        {75, 35, 4, 5, 6}, {95, 33, 3, 4, 8}, {72, 21, 3, 3, 9}, {94, 20, 2, 2, 8}, {94, 9, 2, 2, 8}, {42, 17, 4, 2, 8}
    };
    static final float SHELL = 0.0075F;

    private TailArmorMesh() {
    }

    static int segment(String name) {
        return switch (name) {
            case "tail_1" -> 0;
            case "tail_2" -> 1;
            case "tail_3" -> 2;
            case "tail_4" -> 3;
            case "tail_5" -> 4;
            case "tail_6" -> 5;
            default -> -1;
        };
    }

    static GeoCube bake(GeoCube source, int segment) {
        Vector3f vector3f = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f vector3f1 = new Vector3f(Float.NEGATIVE_INFINITY);

        for (GeoQuad geoquad : source.quads()) {
            if (geoquad != null) {
                for (GeoVertex geovertex : geoquad.vertices()) {
                    vector3f.min(geovertex.position());
                    vector3f1.max(geovertex.position());
                }
            }
        }

        Vector3f vector3f3 = new Vector3f(vector3f).add(vector3f1).mul(0.5F);
        VertexSet vertexset = new VertexSet(Vec3.ZERO, new Vec3(1.0, 1.0, 1.0), 0.0);
        GeoQuad[] ageoquad = new GeoQuad[source.quads().length];

        for (int i1 = 0; i1 < ageoquad.length; i1++) {
            GeoQuad geoquad1 = source.quads()[i1];
            if (geoquad1 != null) {
                Direction direction = switch (geoquad1.direction()) {
                    case NORTH -> Direction.UP;
                    case SOUTH -> Direction.DOWN;
                    case UP -> Direction.SOUTH;
                    case DOWN -> Direction.NORTH;
                    default -> geoquad1.direction();
                };
                float[] afloat = rectangle(segment, direction);
                GeoQuad geoquad2 = GeoQuad.build(
                    vertexset.verticesForQuad(direction, true, false),
                    afloat[0],
                    afloat[1],
                    afloat[2],
                    afloat[3],
                    Rotation.NONE,
                    256.0F,
                    128.0F,
                    false,
                    direction
                );
                GeoVertex[] ageovertex = new GeoVertex[4];

                for (int i = 0; i < 4; i++) {
                    Vector3f vector3f2 = geoquad1.vertices()[i].position();
                    int j = vector3f2.x > vector3f3.x ? 1 : 0;
                    int k = vector3f2.y > vector3f3.y ? 1 : 0;
                    int l = vector3f2.z > vector3f3.z ? 1 : 0;
                    GeoVertex geovertex1 = corner(geoquad2, j, 1 - l, k);
                    ageovertex[i] = new GeoVertex(
                        new Vector3f(vector3f2).add((float)(2 * j - 1) * 0.0075F, (float)(2 * k - 1) * 0.0075F, (float)(2 * l - 1) * 0.0075F),
                        geovertex1.texU(),
                        geovertex1.texV()
                    );
                }

                ageoquad[i1] = new GeoQuad(ageovertex, new Vector3f(geoquad1.normal()), geoquad1.direction());
            }
        }

        return new GeoCube(ageoquad, source.pivot(), source.rotation(), source.size(), source.inflate() + 0.0075F, source.mirror());
    }

    private static GeoVertex corner(GeoQuad quad, int x, int y, int z) {
        for (GeoVertex geovertex : quad.vertices()) {
            Vector3f vector3f = geovertex.position();
            if (vector3f.x == (float)x && vector3f.y == (float)y && vector3f.z == (float)z) {
                return geovertex;
            }
        }

        throw new IllegalStateException("Tail armor UV corner outside native face");
    }

    private static float[] rectangle(int segment, Direction face) {
        int[] aint = ISLANDS[segment];
        int i = aint[0];
        int j = aint[1];
        int k = aint[2];
        int l = aint[3];
        int i1 = aint[4];

        return switch (face) {
            case NORTH -> new float[]{(float)(i + i1), (float)(j + i1), (float)k, (float)l};
            case SOUTH -> new float[]{(float)(i + i1 + k + i1), (float)(j + i1), (float)k, (float)l};
            case UP -> new float[]{(float)(i + i1), (float)j, (float)k, (float)i1};
            case DOWN -> new float[]{(float)(i + i1 + k), (float)(j + i1), (float)k, (float)(-i1)};
            case WEST -> new float[]{(float)(i + i1 + k), (float)(j + i1), (float)i1, (float)l};
            case EAST -> new float[]{(float)i, (float)(j + i1), (float)i1, (float)l};
            default -> throw new IncompatibleClassChangeError();
        };
    }
}
