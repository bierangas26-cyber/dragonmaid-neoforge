package com.example.dragonmaid.client;

import java.util.ArrayDeque;
import java.util.List;

final class BreathImpactQueue<T> {
    private final ArrayDeque<BreathImpactQueue.Entry<T>> targets = new ArrayDeque<>();
    private final ArrayDeque<BreathImpactQueue.Entry<T>> surfaces = new ArrayDeque<>();

    BreathImpactQueue() {
    }

    void clear() {
        this.targets.clear();
        this.surfaces.clear();
    }

    int size() {
        return this.targets.size() + this.surfaces.size();
    }

    void add(T value, boolean target, long now) {
        if (this.size() >= 64) {
            if (!this.surfaces.isEmpty()) {
                this.surfaces.removeFirst();
            } else {
                if (!target) {
                    return;
                }

                this.targets.removeFirst();
            }
        }

        (target ? this.targets : this.surfaces).addLast(new BreathImpactQueue.Entry<>(value, now));
    }

    T poll(long now) {
        for (ArrayDeque<BreathImpactQueue.Entry<T>> arraydeque : List.of(this.targets, this.surfaces)) {
            while (!arraydeque.isEmpty()) {
                BreathImpactQueue.Entry<T> entry = arraydeque.removeFirst();
                if (now >= entry.at && now - entry.at <= 3L) {
                    return entry.value;
                }
            }
        }

        return null;
    }

    private static record Entry<T>(T value, long at) {
    }
}
