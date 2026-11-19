package com.example.dragonmaid.movement;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 1.2.7 escort formation bookkeeping. Each escorting dragon reserves one slot in a ring around the
 * owner so that several escorts do not stack on the same point. Ported 1:1 from 1.2.7
 * (Forge 1.20.1 -> NeoForge 1.21.1, no API change beyond package names).
 */
final class EscortFlightOrbit {
    private static final int POSITIONS_PER_RING = 8;
    private static final int RESERVATION_TICKS = 80;
    private static final double SECTOR = Math.PI / 4;
    private static final Map<Player, Formation> FORMATIONS = new WeakHashMap<>();

    private EscortFlightOrbit() {
    }

    static Vec3 offset(Player owner, UUID dragon, Vec3 relative, long time, boolean hovering, RandomSource random) {
        Formation formation = FORMATIONS.computeIfAbsent(owner, ignored -> new Formation());
        Reservation reservation = formation.reserve(dragon, relative, time);
        if (!hovering) {
            reservation.nextShift = 0L;
        } else if (reservation.nextShift == 0L) {
            reservation.schedule(time, random);
        } else if (time >= reservation.nextShift) {
            reservation.shift(time, random);
        }

        int ring = reservation.slot / 8;
        double radius = (double) (5 + ring * 4) + reservation.radiusShift;
        double angle = angleOf(reservation.slot) + reservation.angleShift;
        double height = 0.6 + (double) ring * 1.4 + 0.35 * (double) (reservation.slot % 2) + reservation.heightShift;
        return new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    private static double angleOf(int slot) {
        return ((double) (slot % 8) + 0.5 * (double) (slot / 8 % 2)) * (Math.PI / 4);
    }

    static void release(Player owner, UUID dragon) {
        Formation formation = FORMATIONS.get(owner);
        if (formation != null) {
            formation.reservations.remove(dragon);
            if (formation.reservations.isEmpty()) {
                FORMATIONS.remove(owner);
            }
        }
    }

    private static final class Formation {
        private final Map<UUID, Reservation> reservations = new HashMap<>();
        private long nextPrune;

        Reservation reserve(UUID dragon, Vec3 relative, long time) {
            if (time >= this.nextPrune) {
                this.reservations.values().removeIf(reservation -> time >= reservation.expires);
                this.nextPrune = time + 20L;
            }

            Reservation existing = this.reservations.get(dragon);
            if (existing != null) {
                existing.expires = time + 80L;
                return existing;
            }

            BitSet occupied = new BitSet();
            for (Reservation reservation : this.reservations.values()) {
                occupied.set(reservation.slot);
            }

            int first = occupied.nextClearBit(0);
            int ring = first / 8;
            int end = (ring + 1) * 8;
            double currentAngle = Math.atan2(relative.z, relative.x);
            int slot = first;
            double closest = Double.POSITIVE_INFINITY;

            for (int candidate = first; candidate < end; candidate = occupied.nextClearBit(candidate + 1)) {
                double difference = Math.abs(Mth.wrapDegrees(Math.toDegrees(currentAngle - angleOf(candidate))));
                if (difference < closest) {
                    closest = difference;
                    slot = candidate;
                }
            }

            Reservation reservation = new Reservation(slot, time + 80L);
            this.reservations.put(dragon, reservation);
            return reservation;
        }
    }

    private static final class Reservation {
        private final int slot;
        private long expires;
        private long nextShift;
        private double angleShift;
        private double radiusShift;
        private double heightShift;

        Reservation(int slot, long expires) {
            this.slot = slot;
            this.expires = expires;
        }

        void schedule(long time, RandomSource random) {
            this.nextShift = time + 100L + (long) random.nextInt(121);
        }

        void shift(long time, RandomSource random) {
            this.angleShift = (random.nextDouble() - 0.5) * 0.24;
            this.radiusShift = random.nextDouble() - 0.5;
            this.heightShift = (random.nextDouble() - 0.5) * 0.8;
            this.schedule(time, random);
        }
    }
}
