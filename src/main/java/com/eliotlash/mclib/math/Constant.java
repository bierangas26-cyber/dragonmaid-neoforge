package com.eliotlash.mclib.math;

public class Constant implements IValue {
    private final double value;

    public Constant(double value) { this.value = value; }

    @Override
    public double get() { return this.value; }
}
