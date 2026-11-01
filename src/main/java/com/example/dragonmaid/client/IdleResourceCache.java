package com.example.dragonmaid.client;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class IdleResourceCache<K, V> {
    private final int capacity;
    private final long idleTime;
    private final Consumer<V> retire;
    private final LinkedHashMap<K, IdleResourceCache.Entry<V>> entries = new LinkedHashMap<>(16, 0.75F, true);

    IdleResourceCache(int capacity, long idleTime, Consumer<V> retire) {
        if (capacity >= 1 && idleTime >= 1L) {
            this.capacity = capacity;
            this.idleTime = idleTime;
            this.retire = Objects.requireNonNull(retire);
        } else {
            throw new IllegalArgumentException("Positive resource capacity and idle time required");
        }
    }

    V get(K key, long now, Supplier<V> create) {
        IdleResourceCache.Entry<V> entry = this.entries.get(key);
        if (entry != null) {
            entry.lastUsed = now;
            return entry.value;
        } else {
            V v = Objects.requireNonNull(create.get());
            this.entries.put(key, new IdleResourceCache.Entry<>(v, now));

            while (this.entries.size() > this.capacity) {
                this.removeOldest();
            }

            return v;
        }
    }

    void evictIdle(long now) {
        Iterator<Map.Entry<K, IdleResourceCache.Entry<V>>> iterator = this.entries.entrySet().iterator();

        while (iterator.hasNext()) {
            IdleResourceCache.Entry<V> entry = iterator.next().getValue();
            if (now - entry.lastUsed < this.idleTime) {
                break;
            }

            iterator.remove();
            this.retire.accept(entry.value);
        }
    }

    void clear() {
        for (IdleResourceCache.Entry<V> entry : this.entries.values()) {
            this.retire.accept(entry.value);
        }

        this.entries.clear();
    }

    int size() {
        return this.entries.size();
    }

    private void removeOldest() {
        Iterator<Map.Entry<K, IdleResourceCache.Entry<V>>> iterator = this.entries.entrySet().iterator();
        IdleResourceCache.Entry<V> entry = iterator.next().getValue();
        iterator.remove();
        this.retire.accept(entry.value);
    }

    private static final class Entry<V> {
        final V value;
        long lastUsed;

        Entry(V value, long now) {
            this.value = value;
            this.lastUsed = now;
        }
    }
}
