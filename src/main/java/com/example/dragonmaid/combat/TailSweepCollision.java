package com.example.dragonmaid.combat;

import java.util.Arrays;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TailSweepCollision {
    private TailSweepCollision() {
    }

    public static Vec3 contact(Vec3 start, Vec3 end, AABB box, double radius) {
        double[] adouble = new double[]{start.x, start.y, start.z};
        double[] adouble1 = new double[]{end.x - start.x, end.y - start.y, end.z - start.z};
        double[] adouble2 = new double[]{box.minX, box.minY, box.minZ};
        double[] adouble3 = new double[]{box.maxX, box.maxY, box.maxZ};
        double[] adouble4 = new double[8];
        int i = 2;
        adouble4[0] = 0.0;
        adouble4[1] = 1.0;

        for (int j = 0; j < 3; j++) {
            if (!(Math.abs(adouble1[j]) < 1.0E-12)) {
                double d0 = (adouble2[j] - adouble[j]) / adouble1[j];
                double d2 = (adouble3[j] - adouble[j]) / adouble1[j];
                if (d0 > 0.0 && d0 < 1.0) {
                    adouble4[i++] = d0;
                }

                if (d2 > 0.0 && d2 < 1.0) {
                    adouble4[i++] = d2;
                }
            }
        }

        Arrays.sort(adouble4, 0, i);
        double d13 = Double.POSITIVE_INFINITY;
        double d1 = 0.0;

        for (int k = 0; k < i - 1; k++) {
            double d3 = adouble4[k];
            double d4 = adouble4[k + 1];
            double d5 = (d3 + d4) * 0.5;
            double d6 = 0.0;
            double d7 = 0.0;

            for (int l = 0; l < 3; l++) {
                double d8 = adouble[l] + adouble1[l] * d5;
                double d10;
                if (d8 < adouble2[l]) {
                    d10 = adouble2[l];
                } else {
                    if (!(d8 > adouble3[l])) {
                        continue;
                    }

                    d10 = adouble3[l];
                }

                d6 += adouble1[l] * adouble1[l];
                d7 += adouble1[l] * (adouble[l] - d10);
            }

            double d14 = d6 > 1.0E-20 ? clamp(-d7 / d6, d3, d4) : d3;
            double d9 = 0.0;

            for (int i1 = 0; i1 < 3; i1++) {
                double d11 = adouble[i1] + adouble1[i1] * d14;
                double d12 = d11 - clamp(d11, adouble2[i1], adouble3[i1]);
                d9 += d12 * d12;
            }

            if (d9 < d13) {
                d13 = d9;
                d1 = d14;
            }
        }

        return Double.isFinite(radius) && !(radius < 0.0) && !(d13 > radius * radius + 1.0E-12)
            ? new Vec3(
                clamp(adouble[0] + adouble1[0] * d1, adouble2[0], adouble3[0]),
                clamp(adouble[1] + adouble1[1] * d1, adouble2[1], adouble3[1]),
                clamp(adouble[2] + adouble1[2] * d1, adouble2[2], adouble3[2])
            )
            : null;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
