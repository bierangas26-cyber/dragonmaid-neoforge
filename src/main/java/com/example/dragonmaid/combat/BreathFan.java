package com.example.dragonmaid.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record BreathFan(Vec3 origin, Vec3 forward, double range, double halfAngle, double speed) {
    public Vec3 right() {
        Vec3 vec3 = this.forward.cross(new Vec3(0.0, 1.0, 0.0));
        return vec3.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : vec3.normalize();
    }

    public Vec3 up() {
        return this.right().cross(this.forward).normalize();
    }

    public double width(double distance) {
        return 0.3 + Math.tan(this.halfAngle) * distance;
    }

    public double height(double distance) {
        return 0.25 + this.width(distance) * 0.35;
    }

    public Vec3 center(double distance) {
        return this.origin.add(this.forward.scale(distance));
    }

    public AABB bounds(double back, double front) {
        return new AABB(this.center(back), this.center(front)).inflate(this.width(front) + 0.5);
    }

    public boolean intersects(AABB box, double back, double front) {
        Vec3 vec3 = box.getCenter().subtract(this.origin);
        double d0 = vec3.dot(this.forward);
        double d1 = projectedRadius(box, this.forward);
        if (!(d0 + d1 < back) && !(d0 - d1 > front) && !(d0 + d1 < 0.0)) {
            double d2 = Mth.clamp(d0, back, front);
            return Math.abs(vec3.dot(this.right())) <= this.width(d2) + projectedRadius(box, this.right())
                && Math.abs(vec3.dot(this.up())) <= this.height(d2) + projectedRadius(box, this.up());
        } else {
            return false;
        }
    }

    private static double projectedRadius(AABB box, Vec3 axis) {
        return (Math.abs(axis.x) * box.getXsize() + Math.abs(axis.y) * box.getYsize() + Math.abs(axis.z) * box.getZsize()) * 0.5;
    }
}
