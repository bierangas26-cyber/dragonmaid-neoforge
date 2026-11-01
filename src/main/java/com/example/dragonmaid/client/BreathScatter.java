package com.example.dragonmaid.client;

final class BreathScatter {
    static final int COLUMNS = 9;
    static final int ROWS = 5;
    static final int CELLS = 45;

    private BreathScatter() {
    }

    static int count(int quality) {
        return quality == 0 ? 54 : (quality == 1 ? 30 : 12);
    }

    static BreathScatter.Sample sample(long seed, int tick, int index) {
        long i = seed + (long)tick * 1009L + (long)index * 37L;
        double d0 = fract((double)index * 0.6180339887498949 + unit(seed + (long)tick * 101L));
        double d1 = (d0 * 2.0 - 1.0) * 0.97;
        int j = index % 6;
        double d2 = (unit(i + 7L) * 2.0 - 1.0) * Math.sqrt(1.0 - d1 * d1);
        if (j == 0 || j == 2) {
            d2 *= 0.48;
        }

        if (j == 4 || j == 5) {
            double d3 = d0 * Math.PI * 2.0;
            double d4 = 0.7 + unit(i + 23L) * 0.25;
            d1 = Math.cos(d3) * d4;
            d2 = Math.sin(d3) * d4;
        }

        return new BreathScatter.Sample(d1, d2 * 0.96, unit(i + 19L), j);
    }

    static BreathScatter.Sample fireSample(long seed, int tick, int index) {
        BreathScatter.Sample breathscatter$sample = sample(seed, tick, index);
        if (breathscatter$sample.layer == 0 || breathscatter$sample.layer == 2) {
            return new BreathScatter.Sample(
                breathscatter$sample.side * 0.68, breathscatter$sample.height * 0.9, breathscatter$sample.depth, breathscatter$sample.layer
            );
        } else {
            return breathscatter$sample.layer == 1
                ? new BreathScatter.Sample(
                    breathscatter$sample.side * 0.68, breathscatter$sample.height * 0.48 * 0.9, breathscatter$sample.depth, breathscatter$sample.layer
                )
                : breathscatter$sample;
        }
    }

    static int cell(double side, double height) {
        int i = Math.max(0, Math.min(8, (int)((side + 1.0) * 0.5 * 9.0)));
        int j = Math.max(0, Math.min(4, (int)((height + 1.0) * 0.5 * 5.0)));
        return j * 9 + i;
    }

    private static double fract(double n) {
        return n - Math.floor(n);
    }

    private static double unit(long n) {
        n = (n ^ n >>> 30) * -4658895280553007687L;
        n = (n ^ n >>> 27) * -7723592293110705685L;
        return (double)((n ^ n >>> 31) >>> 11) * 1.110223E-16F;
    }

    static record Sample(double side, double height, double depth, int layer) {
    }
}
