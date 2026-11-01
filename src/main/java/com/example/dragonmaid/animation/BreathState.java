package com.example.dragonmaid.animation;

public record BreathState(
    long round,
    int revision,
    int phase,
    boolean lightning,
    long prepareAt,
    int prepareTicks,
    long firstAt,
    long shotAt,
    int sequence,
    int shotType,
    long endedAt
) {
    public static final int PREPARE = 0;
    public static final int READY = 1;
    public static final int ACTIVE = 2;
    public static final int END = 3;
    public static final int CANCEL = 4;
    public static final int WAVE = 1;
    public static final int BALL = 2;
    public static final int FIRST = 4;

    public boolean running() {
        return this.phase < 3;
    }

    public static final class Round {
        private BreathState state;

        public Round(long id, long now, int duration, boolean lightning) {
            this.state = new BreathState(id, 1, 0, lightning, now, Math.max(1, duration), -1L, -1L, 0, 0, -1L);
        }

        public BreathState state() {
            return this.state;
        }

        public void preparationRemaining(int remaining, long now) {
            if (this.state.phase == 0) {
                long i = now - (long)(this.state.prepareTicks - Math.max(0, remaining));
                if (this.state.prepareAt != i) {
                    this.state = new BreathState(
                        this.state.round,
                        this.state.revision + 1,
                        this.state.phase,
                        this.state.lightning,
                        i,
                        this.state.prepareTicks,
                        this.state.firstAt,
                        this.state.shotAt,
                        this.state.sequence,
                        this.state.shotType,
                        this.state.endedAt
                    );
                }
            }
        }

        public void phase(int phase, long now) {
            if (this.state.running() && this.state.phase != phase) {
                this.state = new BreathState(
                    this.state.round,
                    this.state.revision + 1,
                    phase,
                    this.state.lightning,
                    this.state.prepareAt,
                    this.state.prepareTicks,
                    this.state.firstAt,
                    this.state.shotAt,
                    this.state.sequence,
                    this.state.shotType,
                    phase >= 3 ? now : -1L
                );
            }
        }

        public void shot(int type, long now) {
            if (this.state.running()) {
                boolean flag = !this.state.lightning && (type & 1) != 0 && this.state.firstAt < 0L;
                boolean flag1 = this.state.shotAt == now;
                int i = type | (flag ? 4 : 0) | (flag1 ? this.state.shotType : 0);
                this.state = new BreathState(
                    this.state.round,
                    this.state.revision + 1,
                    2,
                    this.state.lightning,
                    this.state.prepareAt,
                    this.state.prepareTicks,
                    flag ? now : this.state.firstAt,
                    now,
                    flag1 ? this.state.sequence : this.state.sequence + 1,
                    i,
                    -1L
                );
            }
        }
    }
}
