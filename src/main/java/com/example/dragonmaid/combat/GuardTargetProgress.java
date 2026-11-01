package com.example.dragonmaid.combat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public final class GuardTargetProgress {
    public static final int CHECK_INTERVAL_TICKS = 10;
    public static final int STALLED_TICKS = 200;
    public static final int MAX_SEEK_TICKS = 600;
    public static final int RETRY_TICKS = 200;
    private static final int MAX_DEFERRED_TARGETS = 32;
    private static final double MOVEMENT_PROGRESS_SQR = 0.0625;
    private final Map<UUID, GuardTargetProgress.Deferred> deferred = new LinkedHashMap<>();
    private UUID observedTarget;
    private int startedSeeking;
    private int lastProgress;
    private int lastObservation;
    private Vec3 progressPosition;

    public GuardTargetProgress() {
    }

    public boolean shouldAbandon(UUID target, int tick, Vec3 position, boolean attackOpportunity, boolean navigating) {
        if (!target.equals(this.observedTarget) || tick < this.lastObservation || attackOpportunity) {
            this.observedTarget = target;
            this.startedSeeking = this.lastProgress = tick;
            this.progressPosition = position;
        } else if (navigating && this.progressPosition.distanceToSqr(position) >= 0.0625) {
            this.lastProgress = tick;
            this.progressPosition = position;
        }

        this.lastObservation = tick;
        return tick - this.lastProgress >= 200 || tick - this.startedSeeking >= 600;
    }

    public void defer(UUID target, int tick, GuardTargetProgress.ThreatState threats) {
        this.prune(tick);
        this.deferred.remove(target);
        this.deferred.put(target, new GuardTargetProgress.Deferred(tick, threats));

        while (this.deferred.size() > 32) {
            this.deferred.remove(this.deferred.keySet().iterator().next());
        }

        this.forgetTarget();
    }

    public boolean isDeferred(UUID target, int tick) {
        this.prune(tick);
        return this.deferred.containsKey(target);
    }

    public boolean maySelect(UUID target, int tick, boolean attackOpportunity, GuardTargetProgress.ThreatState threats) {
        this.prune(tick);
        GuardTargetProgress.Deferred guardtargetprogress$deferred = this.deferred.get(target);
        if (guardtargetprogress$deferred == null) {
            return true;
        } else if (!attackOpportunity && !threats.isNewerThan(guardtargetprogress$deferred.threats())) {
            return false;
        } else {
            this.deferred.remove(target);
            return true;
        }
    }

    private void prune(int tick) {
        Iterator<GuardTargetProgress.Deferred> iterator = this.deferred.values().iterator();

        while (iterator.hasNext()) {
            int i = iterator.next().since();
            if (tick < i || tick - i >= 200) {
                iterator.remove();
            }
        }
    }

    public void forgetTarget() {
        this.observedTarget = null;
        this.progressPosition = null;
    }

    public void reset() {
        this.forgetTarget();
        this.deferred.clear();
    }

    private static record Deferred(int since, GuardTargetProgress.ThreatState threats) {
    }

    public static record ThreatState(int selfHit, int ownerHit, int ownerAttack, long companionHit) {
        public static final GuardTargetProgress.ThreatState NONE = new GuardTargetProgress.ThreatState(-1, -1, -1);

        public ThreatState(int selfHit, int ownerHit, int ownerAttack) {
            this(selfHit, ownerHit, ownerAttack, -1L);
        }

        private boolean isNewerThan(GuardTargetProgress.ThreatState previous) {
            return this.selfHit >= 0 && this.selfHit != previous.selfHit
                || this.ownerHit >= 0 && this.ownerHit != previous.ownerHit
                || this.ownerAttack >= 0 && this.ownerAttack != previous.ownerAttack
                || this.companionHit >= 0L && this.companionHit != previous.companionHit;
        }
    }
}
