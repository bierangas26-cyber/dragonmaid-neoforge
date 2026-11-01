package com.example.dragonmaid.combat;

import net.minecraft.nbt.CompoundTag;

public record HumanoidCombatState(
    int meleeCooldown,
    int breathCooldown,
    int chargeCooldown,
    int lightningReserve,
    boolean lightningExhausted,
    int lightningUseRemainder,
    int tailSweepCooldown
) {
    public static final String KEY = "Combat";
    public static final int MAX_MELEE_COOLDOWN = 24;
    public static final int MAX_BREATH_COOLDOWN = 200;
    public static final int MAX_CHARGE_COOLDOWN = 1200;
    public static final int MAX_TAIL_SWEEP_COOLDOWN = 120;

    public HumanoidCombatState(int meleeCooldown, int breathCooldown, int chargeCooldown, int lightningReserve, boolean lightningExhausted) {
        this(meleeCooldown, breathCooldown, chargeCooldown, lightningReserve, lightningExhausted, 0, 0);
    }

    public HumanoidCombatState(
        int meleeCooldown, int breathCooldown, int chargeCooldown, int lightningReserve, boolean lightningExhausted, int lightningUseRemainder
    ) {
        this(meleeCooldown, breathCooldown, chargeCooldown, lightningReserve, lightningExhausted, lightningUseRemainder, 0);
    }

    public static int lightningCapacity(int stage) {
        return Math.max(3, Math.min(5, stage)) * 20;
    }

    public static HumanoidCombatState read(CompoundTag dragonMaid, int stage) {
        int i = lightningCapacity(stage);
        if (!dragonMaid.contains("Combat")) {
            return new HumanoidCombatState(0, 0, 0, i, false);
        } else {
            CompoundTag compoundtag = dragonMaid.getCompound("Combat");
            int j = bounded(compoundtag, "LightningReserve", i, 0);
            int k = j == 0 ? 0 : (compoundtag.contains("LightningUseRemainder") ? bounded(compoundtag, "LightningUseRemainder", 9, 9) : 0);
            boolean flag = j == 0 || (j < i || k > 0) && (!compoundtag.contains("LightningExhausted", 1) || compoundtag.getBoolean("LightningExhausted"));
            return new HumanoidCombatState(
                bounded(compoundtag, "MeleeCooldown", 24, 24),
                bounded(compoundtag, "BreathCooldown", 200, 200),
                bounded(compoundtag, "ChargeCooldown", 1200, 1200),
                j,
                flag,
                k,
                compoundtag.contains("TailSweepCooldown") ? bounded(compoundtag, "TailSweepCooldown", 120, 120) : 0
            );
        }
    }

    private static int bounded(CompoundTag state, String key, int maximum, int malformed) {
        return !state.contains(key, 3) ? malformed : Math.max(0, Math.min(maximum, state.getInt(key)));
    }

    public CompoundTag save(int stage) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putInt("Version", 3);
        compoundtag.putInt("MeleeCooldown", Math.max(0, Math.min(24, this.meleeCooldown)));
        compoundtag.putInt("BreathCooldown", Math.max(0, Math.min(200, this.breathCooldown)));
        compoundtag.putInt("ChargeCooldown", Math.max(0, Math.min(1200, this.chargeCooldown)));
        compoundtag.putInt("TailSweepCooldown", Math.max(0, Math.min(120, this.tailSweepCooldown)));
        int i = lightningCapacity(stage);
        int j = Math.max(0, Math.min(i, this.lightningReserve));
        compoundtag.putInt("LightningReserve", j);
        int k = j == 0 ? 0 : Math.max(0, Math.min(9, this.lightningUseRemainder));
        compoundtag.putInt("LightningUseRemainder", k);
        compoundtag.putBoolean("LightningExhausted", j == 0 || (j < i || k > 0) && this.lightningExhausted);
        return compoundtag;
    }
}
