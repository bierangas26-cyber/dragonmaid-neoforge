package com.example.dragonmaid.learning;

public final class Knowledge {
    public static final int HUMAN_THRESHOLD = 300;
    public static final int MAX_KNOWLEDGE = 10000;
    public static final int LEARNING_MAX = 2500;
    private static final int[] THRESHOLDS = new int[]{300, 1000, 2500, 5500, 10000};

    private Knowledge() {
    }

    public static int clamp(int total) {
        return Math.max(0, Math.min(10000, total));
    }

    /** 1.2: 单次学习最多把知识推到 LEARNING_MAX（2500，即 3 级）。 */
    public static int batchLimit(int total) {
        if (total >= 2500) {
            return 2500;
        } else {
            for (int threshold : THRESHOLDS) {
                if (threshold > total) {
                    return Math.min(2500, threshold);
                }
            }

            return 2500;
        }
    }

    public static int level(int total) {
        int i = 0;

        for (int j : THRESHOLDS) {
            if (total >= j) {
                i++;
            }
        }

        return i;
    }

    public static int normalizeLevel(int total, int rank) {
        int i = Math.max(0, Math.min(5, rank));
        if (i > 1) {
            return i;
        } else {
            return clamp(total) >= 300 ? 1 : 0;
        }
    }

    public static int itemsToConsume(int total, int available, int perItem) {
        return itemsToConsume(total, available, perItem, 300);
    }

    public static int itemsToConsume(int total, int available, int perItem, int limit) {
        limit = clamp(limit);
        if (available > 0 && perItem > 0 && total < limit) {
            long i = (long)limit - (long)clamp(total);
            return (int)Math.min((long)available, (i + (long)perItem - 1L) / (long)perItem);
        } else {
            return 0;
        }
    }

    public static int afterLearning(int total, int consumed, int perItem) {
        return afterLearning(total, consumed, perItem, 300);
    }

    public static int afterLearning(int total, int consumed, int perItem, int limit) {
        limit = clamp(limit);
        return consumed > 0 && perItem > 0 && total < limit ? (int)Math.min((long)limit, (long)clamp(total) + (long)consumed * (long)perItem) : clamp(total);
    }
}
