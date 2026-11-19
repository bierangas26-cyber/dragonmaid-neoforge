package com.example.dragonmaid.movement;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.combat.AirTridentThrow;
import com.example.dragonmaid.combat.BreathProfile;
import com.example.dragonmaid.combat.GuardEscort;
import com.example.dragonmaid.combat.HumanoidTargetGoal;
import com.example.dragonmaid.combat.TailSweepSync;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidGroundMount;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.mixin.RiderJumpAccessor;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.2.7 humanoid dragon flight: autonomous escort flight plus ridden flight.
 *
 * <p>Ported from Dragon Maid Companion 1.2.7 (Forge 1.20.1). API mapping done for NeoForge 1.21.1:
 * {@code BlockPathTypes} -> {@code PathType}, {@code EntityGorgon} -> {@code GorgonEntity},
 * {@code EntityDragonBase} -> {@code DragonBaseEntity}.
 *
 * <p>Known downgrades (see PORT handoff doc):
 * <ul>
 *   <li>The rider takeoff is driven by {@code mixin/RiderJumpAccessor} exactly like upstream, with a
 *       defensive fallback to "sneak held on the ground" if that accessor is not applied (the mixin
 *       lives in another package, so it is guarded rather than assumed).</li>
 *   <li>{@code BreathProfile#canHitFrom}/{@code distanceToTargetSqr} (1.2.7 combat additions) are
 *       replaced by a plain range + corridor check; combat positioning still works but aims less
 *       precisely.</li>
 * </ul>
 */
public final class HumanoidFlight {
    private static final int TAKEOFF_TICKS = 12;
    private static final int TAKEOFF_HOLD_TICKS = 21;
    private static final int TOUCHDOWN_TICKS = 27;
    private static final int PLAN_INTERVAL = 8;
    private static final int CATCHUP_PLAN_INTERVAL = 4;
    private static final double CATCHUP_START_DISTANCE = 12.0;
    private static final double CATCHUP_STOP_DISTANCE = 8.0;
    private static final double CATCHUP_MIN_SPEED = 2.25;
    private static final double CATCHUP_MAX_SPEED = 4.0;
    private static final double CATCHUP_SPEED_ADVANTAGE = 0.55;
    private static final double FLIGHT_RESPONSE = 0.35;
    private static final int CATCHUP_BRAKE_TICKS = 5;
    private static final double CATCHUP_BRAKE_RESPONSE = 0.15;
    private static final int LEDGE_PATH_INTERVAL = 20;
    private static final int TAKEOFF_SEARCH_RADIUS = 16;
    private static final double INDOOR_LANDING_DISTANCE_SQR = 256.0;
    private static final int INDOOR_LANDING_RADIUS = 6;
    private static final double ESCORT_LIFTOFF_HEIGHT = 1.8;
    private static final double ESCORT_LIFT_CLEARANCE = 4.0;
    private static final double LEDGE_RISE = 1.25;
    private static final double LIFT_SPEED = 0.23;
    private static final double LAND_SPEED = 0.18;
    private static final double RIDDEN_ASCENT_SPEED = 0.46;
    private static final double RIDDEN_DESCENT_SPEED = 0.52;
    private static final double RIDDEN_VERTICAL_RESPONSE = 0.5;
    private static final double RIDDEN_VERTICAL_BRAKING = 0.6;
    private static final float BREATH_BODY_TURN = 20.0F;
    private static final float BREATH_HEAD_TURN = 30.0F;
    private static final int[] BYPASS_ANGLES = new int[]{45, -45, 90, -90, 135, -135};

    private final DragonBaseEntity dragon;
    private Vec3 waypoint;
    private Vec3 landing;
    private Vec3 takeoff;
    private Player orbitOwner;
    private Player landingNearShelterFor;
    private Vec3 ownerStillAnchor;
    private long ownerStillSince;
    private Player sampledOwner;
    private Vec3 sampledOwnerPosition;
    private long sampledOwnerTick;
    private boolean catchingUp;
    private double waypointBoostSpeed;
    private int catchupBrakeTicks;
    private long nextPlan;
    private long nextLedgePathCheck;
    private long nextTakeoffSearch;
    private Path takeoffPath;
    private Player walkingToTakeoffFor;
    private int ownerGroundTicks;
    private int blockedTicks;
    private int takeoffHoldTicks;
    private boolean requestedLanding;
    private boolean ridden;
    private boolean descend;
    private boolean breathe;
    private boolean strike;
    private boolean returningFromCombat;
    private boolean ledgeEscort;
    private boolean guardHeld;

    public HumanoidFlight(DragonBaseEntity dragon) {
        this.dragon = dragon;
    }

    public static HumanoidFlight of(DragonBaseEntity dragon) {
        return ((Access) dragon).dragonmaid$flightControl();
    }

    public static Phase phase(DragonBaseEntity dragon) {
        return Phase.decode(((Access) dragon).dragonmaid$flightData() & 7);
    }

    public static int phaseAge(DragonBaseEntity dragon) {
        int phaseStart = ((Access) dragon).dragonmaid$flightData() >>> 3;
        int elapsed = ((int) dragon.level().getGameTime() - phaseStart) & 32767;
        return elapsed > 16384 ? 0 : elapsed;
    }

    public static boolean airborne(DragonBaseEntity dragon) {
        return DragonForms.isHumanoid(dragon) && phase(dragon).airborne();
    }

    public static boolean hasBreathFacing(DragonBaseEntity dragon) {
        if (!autonomousInAir(dragon)) {
            return false;
        }

        int action = DragonForms.state(dragon).dragonmaid$getAction();
        return action == 2 || action == 3;
    }

    public static boolean autonomousInAir(DragonBaseEntity dragon) {
        return !dragon.isVehicle() && DragonForms.isHumanoid(dragon) && phase(dragon) == Phase.AIR;
    }

    public boolean allowsCombat() {
        return !this.requestedLanding && this.ownerGroundTicks < 6 && !this.returningFromCombat && !this.catchingUp;
    }

    public void glance(Vec3 target, boolean trackingEnemy) {
        boolean hovering = (this.waypoint == null || this.dragon.position().distanceToSqr(this.waypoint) < 0.25)
            && this.dragon.getDeltaMovement().lengthSqr() < 0.0025;
        aimAt(this.dragon, target, hovering ? (trackingEnemy ? 8.0F : 3.0F) : 0.0F, trackingEnemy ? 14.0F : 7.0F, false);
    }

    public static boolean aimBreath(DragonBaseEntity dragon, Vec3 target) {
        return aimAt(dragon, target, BREATH_BODY_TURN, BREATH_HEAD_TURN, true);
    }

    private static boolean aimAt(DragonBaseEntity dragon, Vec3 target, float bodySpeed, float headSpeed, boolean immediate) {
        Vec3 delta = target.subtract(dragon.getEyePosition());
        if (delta.horizontalDistanceSqr() < 1.0E-6) {
            Vec3 forward = dragon.position().add(Vec3.directionFromRotation(0.0F, dragon.getYHeadRot()).scale(0.001));
            dragon.getLookControl().setLookAt(forward.x, target.y, forward.z, headSpeed, headSpeed);
            return true;
        }

        float desired = (float) (Mth.atan2(delta.z, delta.x) * 180.0F / (float) Math.PI) - 90.0F;
        float facing = dragon.getYRot();
        float limit = (float) dragon.getMaxHeadYRot();
        float relative = Mth.wrapDegrees(desired - facing);
        float bodyTurn = relative - Mth.clamp(relative, -limit, limit);
        facing += Mth.clamp(bodyTurn, -bodySpeed, bodySpeed);
        dragon.setYRot(facing);
        dragon.setYBodyRot(facing);
        relative = Mth.wrapDegrees(desired - facing);
        float aim = facing + Mth.clamp(relative, -limit, limit);
        if (immediate) {
            dragon.setYHeadRot(aim);
        }

        Vec3 glance = dragon.position().add(Vec3.directionFromRotation(0.0F, aim).scale(delta.horizontalDistance()));
        dragon.getLookControl().setLookAt(glance.x, target.y, glance.z, headSpeed, headSpeed);
        return Math.abs(relative) <= limit + 0.01F;
    }

    public static boolean eligible(DragonBaseEntity dragon) {
        return DragonForms.isHumanoid(dragon)
            && dragon.getDragonStage() >= 4
            && dragon.isTame()
            && dragon.isAlive()
            && !dragon.isModelDead()
            && !dragon.isPassenger()
            && !dragon.isSleeping()
            && !dragon.isNoAi()
            && !HumanoidLife.isInjured(dragon)
            && !GorgonEntity.isStoneMob(dragon)
            && !dragon.isInWaterOrBubble()
            && !dragon.isInLava()
            && !dragon.isLeashed();
    }

    public static boolean validPilot(DragonBaseEntity dragon, Player player) {
        return validRider(dragon, player) && eligible(dragon);
    }

    public static boolean validRider(DragonBaseEntity dragon, Player player) {
        return player != null
            && dragon.getControllingPassenger() == player
            && player.getVehicle() == dragon
            && HumanoidGroundMount.canRemainMounted(dragon, player)
            && !dragon.isSleeping()
            && !HumanoidLife.isInjured(dragon);
    }

    public static boolean groundedForMelee(DragonBaseEntity dragon) {
        if (airborne(dragon) || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }

        return dragon.onGround()
            || dragon.getDeltaMovement().y <= 0.0
                && dragon.level()
                    .getBlockCollisions(dragon, dragon.getBoundingBox().deflate(1.0E-5, 0.0, 1.0E-5).move(0.0, -0.002, 0.0))
                    .iterator()
                    .hasNext();
    }

    public static boolean dismountInAir(Player player) {
        return player.getVehicle() instanceof DragonBaseEntity dragon && airborne(dragon) && !dragon.onGround();
    }

    public static double cruiseSpeed(DragonBaseEntity dragon) {
        return dragon.getAttributeValue(Attributes.MOVEMENT_SPEED) / 0.454;
    }

    public static boolean ownerFlying(DragonBaseEntity dragon, Player owner) {
        if (owner.getVehicle() instanceof DragonBaseEntity mount) {
            return mount != dragon && (airborne(mount) || mount.isFlying() || mount.isHovering());
        }

        return !owner.onGround() && !owner.isInWaterOrBubble() && !owner.isInLava()
            && (owner.getAbilities().flying || owner.isFallFlying()
                || PerchFlight.supportsAirMovement(owner) && PerchFlight.count(owner) == 3);
    }

    private static boolean ownerStillAloft(Player owner) {
        Entity support = owner.getVehicle() != null ? owner.getVehicle() : owner;
        return !support.onGround() && !support.isInWaterOrBubble() && !support.isInLava();
    }

    /** Reads the rider jump key through the accessor mixin, falling back to sneak if it is absent. */
    private static boolean riderJumping(Player pilot) {
        try {
            return ((RiderJumpAccessor) pilot).dragonmaid$riderJumping();
        } catch (ClassCastException | LinkageError ignored) {
            return pilot.isShiftKeyDown();
        }
    }

    /** Server-side request for other systems (network packet / mixin) to start flight explicitly. */
    public static void requestTakeoff(DragonBaseEntity dragon) {
        HumanoidFlight flight = of(dragon);
        if (phase(dragon) == Phase.GROUND && eligible(dragon) && dragon.isVehicle()) {
            flight.takeoffHoldTicks = TAKEOFF_HOLD_TICKS;
            flight.start(true);
        }
    }

    public void setDescend(boolean down) {
        this.descend = down;
    }

    public void setRiderAttacks(boolean breathe, boolean strike) {
        this.breathe = breathe;
        this.strike = strike;
    }

    public boolean wantsBreath() {
        return this.breathe;
    }

    public boolean wantsStrike() {
        return this.strike;
    }

    public void setRiderGuard(boolean held) {
        this.guardHeld = held;
    }

    public boolean wantsGuard() {
        return this.guardHeld;
    }

    public static boolean emptyHands(Player player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    public void clearRiderInput() {
        this.descend = false;
        this.breathe = false;
        this.strike = false;
        this.setRiderGuard(false);
    }

    public void reset() {
        this.clearTakeoffWalk();
        this.releaseOrbit();
        this.clearCatchup();
        this.ledgeEscort = false;
        this.landingNearShelterFor = null;
        if (phase(this.dragon) != Phase.GROUND) {
            this.change(Phase.GROUND);
            this.waypoint = null;
            this.landing = null;
            this.takeoff = null;
            this.requestedLanding = false;
            this.ridden = false;
            this.returningFromCombat = false;
            this.ownerGroundTicks = 0;
            this.blockedTicks = 0;
        }
    }

    public void tick() {
        Phase phase = phase(this.dragon);
        if (!this.dragon.isVehicle()) {
            this.clearRiderInput();
        }

        if (!eligible(this.dragon)) {
            this.takeoffHoldTicks = 0;
            this.reset();
            return;
        }

        if (phase == Phase.TOUCHDOWN) {
            if (!this.hasFooting()) {
                this.reset();
            } else if (phaseAge(this.dragon) >= TOUCHDOWN_TICKS) {
                Player shelterOwner = this.landingNearShelterFor;
                this.reset();
                if (shelterOwner != null
                    && GuardEscort.owner(this.dragon) == shelterOwner
                    && !GuardEscort.hasChain(this.dragon)
                    && !this.dragon.isOrderedToSit()
                    && !this.dragon.isVehicle()) {
                    PathNavigation navigation = this.dragon.getNavigation();
                    Path path = navigation.createPath(shelterOwner, 1);
                    if (path != null && path.canReach()) {
                        navigation.moveTo(path, 1.2);
                    }
                }
            }

            return;
        }

        Player pilot = this.dragon.getControllingPassenger() instanceof Player player ? player : null;
        if (phase == Phase.GROUND && this.dragon.isVehicle()) {
            this.tickRiddenTakeoff(pilot);
            return;
        }

        this.takeoffHoldTicks = 0;
        Player owner = GuardEscort.owner(this.dragon) instanceof Player player ? player : null;
        if (phase == Phase.GROUND) {
            if (this.dragon.getCommand() != 2) {
                this.clearTakeoffWalk();
            } else {
                this.tickGroundEscort(owner);
            }

            return;
        }

        if (pilot != null) {
            pilot.resetFallDistance();
        }

        this.dragon.resetFallDistance();
        if (phase == Phase.TAKEOFF && phaseAge(this.dragon) >= TAKEOFF_TICKS) {
            this.change(Phase.AIR);
            phase = Phase.AIR;
        }

        if (phase != Phase.TAKEOFF
            && (this.dragon.onGround() || phase == Phase.LANDING || this.ridden && this.descend)
            && this.hasFooting()) {
            this.change(Phase.TOUCHDOWN);
            this.releaseOrbit();
            this.clearCatchup();
            return;
        }

        if (this.ridden && !validPilot(this.dragon, pilot)) {
            this.ridden = false;
            this.requestedLanding = true;
            this.nextPlan = 0L;
        }

        if (this.ridden) {
            this.releaseOrbit();
            if (phase != Phase.TAKEOFF) {
                this.change(this.descend ? Phase.LANDING : Phase.AIR);
            }

            return;
        }

        this.tickAutonomous(owner, phase);
    }

    /** 1.2.7: rider holds jump on the ground for TAKEOFF_HOLD_TICKS to launch. */
    private void tickRiddenTakeoff(Player pilot) {
        this.clearTakeoffWalk();
        if (validPilot(this.dragon, pilot) && riderJumping(pilot) && this.dragon.onGround()) {
            if (++this.takeoffHoldTicks >= TAKEOFF_HOLD_TICKS) {
                this.takeoffHoldTicks = TAKEOFF_TICKS;
                if (!GuardEscort.hasChain(this.dragon)) {
                    this.start(true);
                }
            }
        } else {
            this.takeoffHoldTicks = 0;
        }
    }

    private void tickAutonomous(Player owner, Phase phase) {
        boolean follow = owner != null && !this.dragon.isOrderedToSit() && !GuardEscort.hasChain(this.dragon) && !this.requestedLanding;
        this.ledgeEscort = follow && owner.onGround() && (this.ledgeEscort || this.higherGround(owner));
        if (this.orbitOwner != null && (!follow || owner != this.orbitOwner)) {
            this.releaseOrbit();
        }

        boolean ownerAloft = follow && ownerStillAloft(owner);
        if (ownerAloft) {
            this.ownerGroundTicks = 0;
            if (phase == Phase.LANDING) {
                this.landingNearShelterFor = null;
                this.change(Phase.AIR);
            }
        } else {
            this.ownerGroundTicks++;
            this.catchupBrakeTicks = 0;
        }

        if (phase == Phase.TAKEOFF) {
            return;
        }

        long now = this.dragon.level().getGameTime();
        boolean shouldCatchUp = ownerAloft && owner != null
            && this.dragon.distanceToSqr(owner) > CATCHUP_START_DISTANCE * CATCHUP_START_DISTANCE;
        if (shouldCatchUp != this.catchingUp) {
            this.catchupBrakeTicks = !shouldCatchUp && ownerAloft && owner == this.sampledOwner && this.waypointBoostSpeed > 0.0
                ? CATCHUP_BRAKE_TICKS
                : 0;
            this.catchingUp = shouldCatchUp;
            this.nextPlan = 0L;
        }

        if (now >= this.nextPlan) {
            this.plan(owner, follow, ownerAloft, shouldCatchUp, now);
        }
    }

    private void plan(Player owner, boolean follow, boolean ownerAloft, boolean shouldCatchUp, long now) {
        this.nextPlan = now + PLAN_INTERVAL;
        this.waypointBoostSpeed = 0.0;
        boolean shouldLand = !follow || this.ownerGroundTicks >= 6;
        Vec3 anchor = follow ? owner.position() : this.dragon.position();

        if (ownerAloft) {
            long elapsed = now - this.sampledOwnerTick;
            if (shouldCatchUp) {
                Vec3 motion = owner == this.sampledOwner && elapsed > 0L && elapsed <= 16L
                    ? anchor.subtract(this.sampledOwnerPosition).scale(1.0 / (double) elapsed)
                    : owner.getDeltaMovement();
                this.nextPlan = now + CATCHUP_PLAN_INTERVAL;
                this.releaseOrbit();
                Vec3 destination = anchor.add(0.0, 0.8, 0.0);
                double ownerSpeed = motion.length();
                double speed = Mth.clamp(ownerSpeed + CATCHUP_SPEED_ADVANTAGE, CATCHUP_MIN_SPEED, CATCHUP_MAX_SPEED);
                Vec3 lead = motion.scale(Math.min(3.0, 9.0 / Math.max(ownerSpeed, 0.01)));
                this.waypoint = this.boostRoute(destination.add(lead), speed);
                if (this.waypoint != null) {
                    this.waypointBoostSpeed = speed;
                } else {
                    this.waypoint = this.route(destination);
                }
            }

            this.sampledOwner = owner;
            this.sampledOwnerPosition = anchor;
            this.sampledOwnerTick = now;
            if (shouldCatchUp) {
                return;
            }
        } else {
            this.clearCatchup();
        }

        double ownerHorizontalDistance = anchor.subtract(this.dragon.position()).horizontalDistanceSqr();
        boolean nearOwner = !follow || ownerHorizontalDistance <= 64.0;
        if (shouldLand
            && follow
            && ownerHorizontalDistance <= INDOOR_LANDING_DISTANCE_SQR
            && this.dragon.level().hasChunkAt(owner.blockPosition())
            && !this.exposed(owner.blockPosition())) {
            Vec3 approach = new Vec3(this.dragon.getX(), anchor.y, this.dragon.getZ());
            Vec3 point = this.findLanding(approach, Double.NEGATIVE_INFINITY, true);
            if (point != null) {
                this.beginLanding(point, this.indoorLandingApproach(point));
                this.landingNearShelterFor = owner;
                return;
            }
        }

        this.landingNearShelterFor = null;
        boolean aboveLedge = !this.ledgeEscort || this.dragon.getY() >= anchor.y + 0.5;
        if (shouldLand && nearOwner && aboveLedge) {
            Vec3 point = this.findLanding(anchor, this.ledgeEscort ? anchor.y - 1.0 : Double.NEGATIVE_INFINITY, false);
            if (point != null) {
                this.beginLanding(point, this.route(point));
                return;
            }
        }

        if (phase(this.dragon) == Phase.LANDING) {
            this.change(Phase.AIR);
        }

        if (!follow) {
            Vec3 lower = this.dragon.position().add(0.0, -3.0, 0.0);
            this.waypoint = !GuardEscort.hasChain(this.dragon) && this.corridor(lower) ? lower : null;
            return;
        }

        LivingEntity target = this.dragon.getTarget();
        double guardRadius = DragonMaidConfig.GUARD_COMBAT_RADIUS.get();
        boolean fighting = !shouldLand
            && target != null
            && HumanoidTargetGoal.isEnemy(this.dragon, target)
            && GuardEscort.reachableTarget(this.dragon, target);
        double returnRadius = this.returningFromCombat ? guardRadius - 1.5 : guardRadius;
        this.returningFromCombat = fighting && this.dragon.distanceToSqr(owner) > returnRadius * returnRadius;
        if (fighting && !this.returningFromCombat) {
            this.releaseOrbit();
            this.waypoint = this.combatWaypoint(target, anchor, guardRadius);
            return;
        }

        this.orbitOwner = owner;
        if (this.ownerStillAnchor == null || anchor.distanceToSqr(this.ownerStillAnchor) > 0.1225) {
            this.ownerStillAnchor = anchor;
            this.ownerStillSince = now;
        }

        Vec3 offset = EscortFlightOrbit.offset(
            owner,
            this.dragon.getUUID(),
            this.dragon.position().subtract(anchor),
            now,
            !shouldLand && !fighting && now - this.ownerStillSince >= 16L,
            this.dragon.getRandom()
        );
        double radius = offset.horizontalDistance();
        double approachRadius = shouldLand ? 6.0 : (this.returningFromCombat ? guardRadius - 2.0 : radius);
        if (radius > approachRadius) {
            offset = offset.scale(approachRadius / radius);
        }

        double altitude = this.returningFromCombat ? 0.6 : offset.y;
        if (shouldLand) {
            altitude = this.ledgeEscort && nearOwner ? 3.0 : Math.max(2.0, this.dragon.getY() - anchor.y);
        }

        Vec3 destination = anchor.add(offset.x, altitude, offset.z);
        if (this.ledgeEscort && nearOwner && this.dragon.getY() < anchor.y + 0.5) {
            Vec3 rise = this.dragon.position().add(0.0, Math.min(2.0, destination.y - this.dragon.getY()), 0.0);
            if (this.corridor(rise)) {
                this.waypoint = rise;
                return;
            }
        }

        this.waypoint = this.route(destination);
        if (this.waypoint == null) {
            this.waypoint = this.route(destination.add(0.0, 1.5, 0.0));
        }
    }

    private boolean higherGround(Player owner) {
        return owner.onGround() && owner.getY() - this.dragon.getY() > LEDGE_RISE && !owner.isInWaterOrBubble() && !owner.isInLava();
    }

    private void tickGroundEscort(Player owner) {
        if (owner == null || this.dragon.isOrderedToSit() || TailSweepSync.isBusy(this.dragon)
            || !this.dragon.level().hasChunkAt(owner.blockPosition())) {
            this.clearTakeoffWalk();
            return;
        }

        boolean flyingOwner = ownerFlying(this.dragon, owner);
        boolean wantsFlight = (flyingOwner || this.higherGround(owner)) && !GuardEscort.hasChain(this.dragon);
        if (!wantsFlight || !this.hasFooting()) {
            this.clearTakeoffWalk();
            return;
        }

        if (!flyingOwner && !this.needsLedgeFlight(owner)) {
            return;
        }

        if (this.escortTakeoffSpace(this.dragon.getBoundingBox(), this.exposed(owner.blockPosition()))) {
            this.clearTakeoffWalk();
            this.start(false);
            this.ledgeEscort = !flyingOwner && phase(this.dragon) == Phase.TAKEOFF;
            return;
        }

        if (this.walkingToTakeoffFor != owner) {
            this.clearTakeoffWalk();
            this.walkingToTakeoffFor = owner;
        }
    }

    public boolean walkToTakeoff(LivingEntity owner) {
        if (owner != this.walkingToTakeoffFor) {
            return false;
        }

        PathNavigation navigation = this.dragon.getNavigation();
        if (!navigation.isDone() && navigation.getPath() == this.takeoffPath) {
            return true;
        }

        long now = this.dragon.level().getGameTime();
        if (now < this.nextTakeoffSearch) {
            return false;
        }

        this.nextTakeoffSearch = now + 20L;
        Set<BlockPos> candidates = this.takeoffCandidates(this.exposed(owner.blockPosition()));
        if (candidates.isEmpty()) {
            return false;
        }

        Path path = navigation.createPath(candidates, 0);
        if (path != null && path.canReach() && navigation.moveTo(path, 1.1)) {
            this.takeoffPath = navigation.getPath();
            return true;
        }

        return false;
    }

    private Set<BlockPos> takeoffCandidates(boolean requireOpenSky) {
        Set<BlockPos> candidates = new HashSet<>();
        BlockPos start = this.dragon.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                for (int distance = 2; distance <= TAKEOFF_SEARCH_RADIUS; distance += 2) {
                    BlockPos column = start.offset(dx * distance, 0, dz * distance);
                    if (!this.dragon.level().hasChunkAt(column)) {
                        break;
                    }

                    BlockPos point = this.takeoffPoint(column, requireOpenSky);
                    if (point != null) {
                        candidates.add(point);
                        if (distance > 4) {
                            break;
                        }
                    }
                }
            }
        }

        return candidates;
    }

    private BlockPos takeoffPoint(BlockPos column, boolean requireOpenSky) {
        Level level = this.dragon.level();
        int top = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        int startY = column.getY();
        if (requireOpenSky && Math.abs(top - startY) > 3) {
            return null;
        }

        int minimumY = requireOpenSky ? top : startY - 3;
        for (int y = Math.min(top, startY + 3); y >= minimumY; y--) {
            BlockPos feet = new BlockPos(column.getX(), y, column.getZ());
            PathType type = HumanoidHazards.pathType(
                this.dragon, level, feet.getX(), y, feet.getZ(), WalkNodeEvaluator.getPathTypeStatic(this.dragon, feet)
            );
            if (type == PathType.WALKABLE) {
                Vec3 point = Vec3.atBottomCenterOf(feet);
                AABB box = this.dragon.getBoundingBox().move(point.subtract(this.dragon.position()));
                if (this.escortTakeoffSpace(box, requireOpenSky)) {
                    return feet;
                }
            }
        }

        return null;
    }

    private boolean escortTakeoffSpace(AABB feet, boolean requireOpenSky) {
        AABB clearance = feet.expandTowards(0.0, ESCORT_LIFT_CLEARANCE, 0.0);
        Vec3 center = feet.getCenter();
        return this.loaded(clearance)
            && (!requireOpenSky || this.exposed(BlockPos.containing(center.x, feet.minY, center.z)))
            && !this.touchesBlocks(clearance)
            && !this.dragon.level().containsAnyLiquid(clearance);
    }

    private boolean exposed(BlockPos position) {
        return this.dragon.level().getHeight(Types.MOTION_BLOCKING_NO_LEAVES, position.getX(), position.getZ()) <= position.getY();
    }

    private void clearTakeoffWalk() {
        if (this.walkingToTakeoffFor != null) {
            PathNavigation navigation = this.dragon.getNavigation();
            if (this.takeoffPath != null && navigation.getPath() == this.takeoffPath) {
                navigation.stop();
            }

            this.walkingToTakeoffFor = null;
            this.takeoffPath = null;
            this.nextTakeoffSearch = 0L;
        }
    }

    private boolean needsLedgeFlight(Player owner) {
        Level level = this.dragon.level();
        long now = level.getGameTime();
        if (now >= this.nextLedgePathCheck && this.dragon.onGround() && level.hasChunkAt(owner.blockPosition())) {
            this.nextLedgePathCheck = now + LEDGE_PATH_INTERVAL;
            Path path = this.dragon.getNavigation().createPath(owner, 0);
            return path == null || !path.canReach() || path.getEndNode() == null || (double) path.getEndNode().y < owner.getY() - 0.5;
        }

        return false;
    }

    private Vec3 combatWaypoint(LivingEntity target, Vec3 owner, double guardRadius) {
        BreathProfile profile = BreathProfile.forDragon(this.dragon);
        Vec3 origin = this.dragon.position();
        Vec3 eye = this.dragon.getEyePosition();
        Vec3 toEnemy = target.getBoundingBox().getCenter().subtract(eye);
        double distance = eye.distanceTo(target.getBoundingBox().getCenter());
        double near = Math.min(6.0, profile.range() * 0.8);
        Vec3 forward = new Vec3(toEnemy.x, 0.0, toEnemy.z).normalize();
        if (forward.lengthSqr() < 0.001) {
            forward = Vec3.directionFromRotation(0.0F, this.dragon.getYRot());
        }

        Vec3 side = new Vec3(-forward.z, 0.0, forward.x).scale((this.dragon.getId() & 1) == 0 ? 2.75 : -2.75);
        double limit = guardRadius * guardRadius;
        if (distance >= near && distance <= profile.range()) {
            return null;
        }

        Vec3 movement = distance < near
            ? forward.scale(-Mth.clamp(near + 0.8 - distance, 1.0, 3.0))
            : toEnemy.normalize().scale(Math.min(3.5, distance - profile.range() * 0.85));
        Vec3 next = this.route(origin.add(movement));
        if (next != null && next.distanceToSqr(owner) <= limit) {
            return next;
        }

        Vec3 rise = new Vec3(0.0, 1.5, 0.0);
        for (Vec3 offset : new Vec3[]{side, side.reverse(), rise, side.add(rise), side.reverse().add(rise), rise.scale(2.0)}) {
            Vec3 candidate = origin.add(offset);
            if (candidate.distanceToSqr(owner) <= limit && this.corridor(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private void releaseOrbit() {
        if (this.orbitOwner != null) {
            EscortFlightOrbit.release(this.orbitOwner, this.dragon.getUUID());
            this.orbitOwner = null;
            this.ownerStillAnchor = null;
        }
    }

    private void clearCatchup() {
        this.catchingUp = false;
        this.waypointBoostSpeed = 0.0;
        this.catchupBrakeTicks = 0;
        this.sampledOwner = null;
        this.sampledOwnerPosition = null;
    }

    private void start(boolean carrying) {
        if (!carrying && !this.hasFooting()) {
            return;
        }

        if (!this.corridor(this.dragon.position().add(0.0, 1.0, 0.0))) {
            return;
        }

        this.clearCatchup();
        this.ridden = carrying;
        this.requestedLanding = false;
        this.returningFromCombat = false;
        this.blockedTicks = 0;
        this.ownerGroundTicks = 0;
        this.nextPlan = 0L;
        this.landing = null;
        this.waypoint = null;
        this.takeoff = this.dragon.position().add(0.0, ESCORT_LIFTOFF_HEIGHT, 0.0);
        this.dragon.getNavigation().stop();
        this.dragon.setInSittingPose(false);
        this.dragon.setOrderedToSit(false);
        this.dragon.burningTarget = null;
        this.change(Phase.TAKEOFF);
    }

    public void travel(Vec3 input, boolean jumping) {
        Phase phase = phase(this.dragon);
        if (!this.dragon.isControlledByLocalInstance()) {
            return;
        }

        boolean manualFlight = phase != Phase.TAKEOFF && this.dragon.isVehicle();
        Vec3 velocity;
        if (phase == Phase.TAKEOFF) {
            velocity = new Vec3(0.0, LIFT_SPEED, 0.0);
            if (this.takeoff != null && !this.ridden && this.dragon.getY() >= this.takeoff.y) {
                velocity = Vec3.ZERO;
            }
        } else if (phase == Phase.LANDING && this.landing != null && this.waypoint != null && !this.dragon.isVehicle()) {
            Vec3 goal = this.waypoint;
            velocity = this.towards(goal, goal.distanceToSqr(this.landing) < 0.01 ? 0.28 : cruiseSpeed(this.dragon), LAND_SPEED);
            if (goal.subtract(this.dragon.position()).horizontalDistanceSqr() < 0.04 && goal.y < this.dragon.getY()) {
                velocity = new Vec3(velocity.x, Math.min(velocity.y, -0.035), velocity.z);
            }
        } else if (manualFlight) {
            Vec3 horizontal = new Vec3(input.x, 0.0, input.z);
            if (horizontal.lengthSqr() > 1.0) {
                horizontal = horizontal.normalize();
            }

            horizontal = horizontal.yRot(-this.dragon.getYRot() * (float) (Math.PI / 180.0)).scale(cruiseSpeed(this.dragon));
            velocity = new Vec3(
                horizontal.x,
                this.descend ? -RIDDEN_DESCENT_SPEED : (jumping ? RIDDEN_ASCENT_SPEED : 0.0),
                horizontal.z
            );
        } else if (this.waypointBoostSpeed > 0.0 && this.waypoint != null) {
            Vec3 offset = this.waypoint.subtract(this.dragon.position());
            double distance = offset.length();
            velocity = offset.scale(Math.min(0.55, this.waypointBoostSpeed / Math.max(distance, 0.01)));
        } else {
            velocity = this.waypoint == null ? Vec3.ZERO : this.towards(this.waypoint, cruiseSpeed(this.dragon), LIFT_SPEED);
        }

        Vec3 previous = this.dragon.getDeltaMovement();
        Vec3 step = previous.lerp(velocity, FLIGHT_RESPONSE);
        if (phase == Phase.AIR && !manualFlight && this.ownerGroundTicks == 0 && this.catchupBrakeTicks > 0) {
            double blend = 1.0 - (double) (this.catchupBrakeTicks--) / (double) CATCHUP_BRAKE_TICKS;
            double speed = step.length();
            double previousSpeed = previous.length();
            if (speed > 0.005 && speed < previousSpeed) {
                double response = Mth.lerp(blend, CATCHUP_BRAKE_RESPONSE, FLIGHT_RESPONSE);
                step = step.scale(Mth.lerp(response / FLIGHT_RESPONSE, previousSpeed, speed) / speed);
            }
        }

        if (manualFlight) {
            double response = velocity.y == 0.0 ? RIDDEN_VERTICAL_BRAKING : RIDDEN_VERTICAL_RESPONSE;
            step = new Vec3(step.x, Mth.lerp(response, previous.y, velocity.y), step.z);
        }

        if (step.lengthSqr() < 2.5E-5) {
            step = Vec3.ZERO;
        }

        if (!this.loaded(this.bodyBox().expandTowards(step))) {
            step = Vec3.ZERO;
        }

        if (step.lengthSqr() > 0.0 && this.dragon.isVehicle() && this.touchesBlocks(this.bodyBox().move(step))) {
            if (step.y > 0.0) {
                step = new Vec3(step.x, 0.0, step.z);
            }

            if (step.horizontalDistanceSqr() > 0.0 && this.touchesBlocks(this.bodyBox().move(step))) {
                step = new Vec3(0.0, step.y, 0.0);
            }
        }

        this.dragon.resetFallDistance();
        Vec3 start = this.dragon.position();
        this.dragon.setDeltaMovement(step);
        this.dragon.move(MoverType.SELF, step);
        Vec3 moved = this.dragon.position().subtract(start);
        this.dragon.setDeltaMovement(moved);
        if (!this.dragon.isVehicle()
            && moved.horizontalDistanceSqr() > 4.0E-4
            && !AirTridentThrow.playing(this.dragon)
            && !hasBreathFacing(this.dragon)) {
            float yaw = (float) Math.toDegrees(Math.atan2(-moved.x, moved.z));
            this.dragon.setYRot(this.dragon.getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - this.dragon.getYRot()), -22.0F, 22.0F));
            this.dragon.yBodyRot = this.dragon.getYRot();
        }

        if (this.dragon.level().isClientSide) {
            return;
        }

        if (step.lengthSqr() > 0.001 && moved.lengthSqr() < 1.0E-4) {
            if (++this.blockedTicks >= 3) {
                this.nextPlan = 0L;
            }

            if (phase == Phase.TAKEOFF && this.blockedTicks >= 3 && this.hasFooting()) {
                this.reset();
            }
        } else {
            this.blockedTicks = 0;
        }
    }

    private Vec3 towards(Vec3 point, double horizontalSpeed, double verticalSpeed) {
        Vec3 offset = point.subtract(this.dragon.position());
        double horizontal = Math.min(horizontalSpeed, offset.horizontalDistance() * 0.34);
        Vec3 flat = new Vec3(offset.x, 0.0, offset.z).normalize().scale(horizontal);
        return new Vec3(flat.x, Mth.clamp(offset.y * 0.34, -verticalSpeed, verticalSpeed), flat.z);
    }

    private Vec3 boostRoute(Vec3 goal, double speed) {
        Vec3 origin = this.dragon.position();
        Vec3 offset = goal.subtract(origin);
        Vec3 ahead = offset.normalize().scale(Math.min(offset.length(), speed * 4.0 + 2.0));
        AABB box = this.bodyBox();
        int segments = Mth.ceil(ahead.length() / 3.0);
        if (segments == 0) {
            return null;
        }

        Vec3 segment = ahead.scale(1.0 / (double) segments);
        for (int index = 0; index < segments; index++) {
            AABB swept = box.expandTowards(segment);
            if (!this.loaded(swept) || this.touchesBlocks(swept) || this.dragon.level().containsAnyLiquid(swept)) {
                return null;
            }

            box = box.move(segment);
        }

        return origin.add(ahead);
    }

    private Vec3 route(Vec3 goal) {
        Vec3 origin = this.dragon.position();
        Vec3 offset = goal.subtract(origin);
        double length = offset.length();
        Vec3 probe = origin.add(offset.scale(length > 4.0 ? 4.0 / length : 1.0));
        if (this.corridor(probe)) {
            return length > 4.0 ? probe : goal;
        }

        Vec3 up = origin.add(0.0, 1.5, 0.0);
        Vec3 raised = probe.add(0.0, 1.5, 0.0);
        if (this.corridor(up)) {
            if (this.corridor(raised)) {
                return raised;
            }

            if (goal.y > origin.y + 0.5) {
                return up;
            }
        }

        Vec3 direction = new Vec3(offset.x, 0.0, offset.z).normalize();
        if (direction.lengthSqr() < 0.01) {
            return this.corridor(up) ? up : null;
        }

        for (int angle : BYPASS_ANGLES) {
            Vec3 candidate = origin.add(direction.yRot((float) angle * (float) (Math.PI / 180.0)).scale(2.5));
            if (this.corridor(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean corridor(Vec3 end) {
        Vec3 start = this.dragon.position();
        int samples = Math.max(1, Mth.ceil(start.distanceTo(end) / 0.5));
        for (int step = 1; step <= samples; step++) {
            if (!this.clear(start.lerp(end, (double) step / (double) samples))) {
                return false;
            }
        }

        return true;
    }

    private void beginLanding(Vec3 point, Vec3 approach) {
        this.landing = point;
        this.waypoint = approach;
        this.change(Phase.LANDING);
        this.releaseOrbit();
    }

    private Vec3 indoorLandingApproach(Vec3 point) {
        if (this.corridor(point)) {
            return point;
        }

        Vec3 above = new Vec3(point.x, this.dragon.getY(), point.z);
        AABB descent = this.bodyBox().move(point.subtract(this.dragon.position())).expandTowards(0.0, above.y - point.y, 0.0);
        return this.loaded(descent)
                && !this.touchesBlocks(descent)
                && !this.dragon.level().containsAnyLiquid(descent)
                && this.corridor(above)
            ? above
            : null;
    }

    private Vec3 findLanding(Vec3 anchor, double minimumY, boolean indoorArrival) {
        BlockPos center = BlockPos.containing(anchor);
        Vec3 best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        int radius = indoorArrival ? INDOOR_LANDING_RADIUS : 2;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (!indoorArrival && !this.ridden && anchor.distanceToSqr(this.dragon.position()) > 1.0 && x * x + z * z >= 4) {
                    continue;
                }

                BlockPos top = center.offset(x, 1, z);
                if (!this.dragon.level().hasChunkAt(top) || indoorArrival && !this.exposed(top)) {
                    continue;
                }

                int drop = this.requestedLanding ? 18 : 3;
                for (int depth = 0; depth <= drop; depth++) {
                    BlockPos support = top.below(depth + 1);
                    if (this.dragon.level().isOutsideBuildHeight(support)) {
                        break;
                    }

                    BlockState state = this.dragon.level().getBlockState(support);
                    if (!state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES)) {
                        break;
                    }

                    VoxelShape shape = state.getCollisionShape(this.dragon.level(), support);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    double y = (double) support.getY() + shape.max(Axis.Y);
                    if (y < minimumY || indoorArrival && y > this.dragon.getY()) {
                        break;
                    }

                    Vec3 point = new Vec3((double) support.getX() + 0.5, y, (double) support.getZ() + 0.5);
                    BlockPos feet = BlockPos.containing(point.add(0.0, 0.01, 0.0));
                    AABB box = this.bodyBox().move(point.subtract(this.dragon.position()));
                    if (!this.loaded(box.inflate(1.0, 0.0, 1.0))) {
                        break;
                    }

                    PathType path = HumanoidHazards.pathType(
                        this.dragon,
                        this.dragon.level(),
                        feet.getX(),
                        feet.getY(),
                        feet.getZ(),
                        WalkNodeEvaluator.getPathTypeStatic(this.dragon, feet)
                    );
                    double score = point.distanceToSqr(this.dragon.position());
                    if (path != PathType.DAMAGE_OTHER
                        && path != PathType.DAMAGE_FIRE
                        && path != PathType.DANGER_OTHER
                        && path != PathType.DANGER_FIRE
                        && path != PathType.BLOCKED
                        && score < bestScore
                        && this.clear(point)
                        && this.dragon.level().getBlockCollisions(this.dragon, box.move(0.0, -0.01, 0.0)).iterator().hasNext()
                        && (!indoorArrival || this.indoorLandingApproach(point) != null)) {
                        best = point;
                        bestScore = score;
                    }

                    break;
                }
            }
        }

        return best;
    }

    private AABB bodyBox() {
        if (this.dragon.getFirstPassenger() instanceof Player pilot) {
            double seat = HumanoidGroundMount.seatHeight(this.dragon);
            AABB passenger = pilot.getBoundingBox()
                .move(this.dragon.getX() - pilot.getX(), this.dragon.getY() + seat - pilot.getY(), this.dragon.getZ() - pilot.getZ());
            return this.dragon.getBoundingBox().minmax(passenger);
        }

        return this.dragon.getBoundingBox();
    }

    private boolean clear(Vec3 point) {
        Vec3 offset = point.subtract(this.dragon.position());
        AABB box = this.bodyBox().move(offset);
        return this.loaded(box)
            && !this.touchesBlocks(box)
            && this.dragon.level().getEntityCollisions(this.dragon, this.dragon.getBoundingBox().move(offset)).isEmpty()
            && !this.dragon.level().containsAnyLiquid(box);
    }

    private boolean loaded(AABB box) {
        Level level = this.dragon.level();
        return box.minY >= (double) level.getMinBuildHeight()
            && box.maxY <= (double) level.getMaxBuildHeight()
            && level.getWorldBorder().isWithinBounds(box)
            && level.hasChunksAt(Mth.floor(box.minX), Mth.floor(box.minZ), Mth.floor(box.maxX), Mth.floor(box.maxZ));
    }

    private boolean touchesBlocks(AABB box) {
        return this.dragon.level().getBlockCollisions(this.dragon, box).iterator().hasNext();
    }

    private boolean hasFooting() {
        AABB box = this.dragon.getBoundingBox();
        AABB feet = new AABB(box.minX, box.minY - 0.02, box.minZ, box.maxX, box.minY, box.maxZ);
        return this.loaded(feet) && this.dragon.level().getBlockCollisions(this.dragon, feet).iterator().hasNext();
    }

    private void change(Phase next) {
        if (phase(this.dragon) != next) {
            int tick = (int) this.dragon.level().getGameTime() & 32767;
            ((Access) this.dragon).dragonmaid$flightData(tick << 3 | next.ordinal());
        }
    }

    public interface Access {
        int dragonmaid$flightData();

        void dragonmaid$flightData(int value);

        HumanoidFlight dragonmaid$flightControl();
    }

    public enum Phase {
        GROUND,
        TAKEOFF,
        AIR,
        LANDING,
        TOUCHDOWN;

        private static final Phase[] VALUES = values();

        public static Phase decode(int value) {
            return value >= 0 && value < VALUES.length ? VALUES[value] : GROUND;
        }

        public boolean airborne() {
            return this == TAKEOFF || this == AIR || this == LANDING;
        }
    }
}
