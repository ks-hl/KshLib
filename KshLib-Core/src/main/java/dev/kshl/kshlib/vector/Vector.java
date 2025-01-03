package dev.kshl.kshlib.vector;

import java.util.Objects;

public class Vector {
    private double x;
    private double y;
    private double z;

    public Vector() {
    }

    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static double square(double d) {
        return d * d;
    }

    public double length() {
        return Math.sqrt(square(getX()) + square(getY()) + square(getZ()));
    }

    public Vector divide(double by) {
        return multiply(1 / by);
    }

    public Vector multiply(double by) {
        return multiply(by, by, by);
    }

    public Vector multiply(double x, double y, double z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;

        return this;
    }

    public Vector multiply(Vector by) {
        return multiply(by.getX(), by.getY(), by.getZ());
    }

    public Vector add(Vector vector) {
        return add(vector.getX(), vector.getY(), vector.getZ());
    }

    public Vector add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getPitch() {
        double x2 = square(getX());
        double z2 = square(getZ());
        double xz = Math.sqrt(x2 + z2);
        return Math.atan(-getY() / xz);
    }

    public double getYaw() {
        double theta = Math.atan2(-getX(), getZ());
        return ((theta + Math.PI * 2) % (Math.PI * 2));
    }

    public double distance(Vector other) {
        return Math.sqrt(square(other.getX() - getX()) + square(other.getY() - getY()) + square(other.getZ() - getZ()));
    }

    public Vector crossProduct(Vector other) {
        double crossX = getY() * other.getZ() - getZ() * other.getY();
        double crossY = getZ() * other.getX() - getX() * other.getZ();
        double crossZ = getX() * other.getY() - getY() * other.getX();
        return new Vector(crossX, crossY, crossZ);
    }

    public double dotProduct(Vector other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ();
    }

    public void normalize() {
        double length = length();
        if (length != 0) {
            this.x /= length;
            this.y /= length;
            this.z /= length;
        }
    }

    public Vector rotate(Vector axis, double angle) {
        axis.normalize(); // Normalize the axis vector

        double sinAngle = Math.sin(angle);
        double cosAngle = Math.cos(angle);
        double oneMinusCosAngle = 1.0 - cosAngle;

        double xx = axis.getX() * axis.getX();
        double yy = axis.getY() * axis.getY();
        double zz = axis.getZ() * axis.getZ();
        double xy = axis.getX() * axis.getY();
        double yz = axis.getY() * axis.getZ();
        double zx = axis.getZ() * axis.getX();
        double xs = axis.getX() * sinAngle;
        double ys = axis.getY() * sinAngle;
        double zs = axis.getZ() * sinAngle;

        double m00 = xx + (1.0 - xx) * cosAngle;
        double m01 = xy * oneMinusCosAngle - zs;
        double m02 = zx * oneMinusCosAngle + ys;

        double m10 = xy * oneMinusCosAngle + zs;
        double m11 = yy + (1.0 - yy) * cosAngle;
        double m12 = yz * oneMinusCosAngle - xs;

        double m20 = zx * oneMinusCosAngle - ys;
        double m21 = yz * oneMinusCosAngle + xs;
        double m22 = zz + (1.0 - zz) * cosAngle;

        double newX = getX() * m00 + getY() * m01 + getZ() * m02;
        double newY = getX() * m10 + getY() * m11 + getZ() * m12;
        double newZ = getX() * m20 + getY() * m21 + getZ() * m22;

        return new Vector(newX, newY, newZ);
    }

    public Vector clone() {
        return new Vector(getX(), getY(), getZ());
    }


    @Override
    public String toString() {
        return String.format("[%.4f, %.4f, %.4f]", getX(), getY(), getZ());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Vector vector)) return false;

        return VectorMath.equals(getX(), vector.getX(), 1E-7)
                && VectorMath.equals(getY(), vector.getY(), 1E-7)
                && VectorMath.equals(getZ(), vector.getZ(), 1E-7);
    }

    @Override
    public int hashCode() {
        return Objects.hash((int) (getX() * 1E7), (int) (getY() * 1E7), (int) (getZ() * 1E7));
    }
}
