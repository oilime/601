package com.lanan.navigation.model;

public class Direction {

    private double dis;
    private double angle;

    public double getDis() {
        return dis;
    }

    public double getAngle() {
        return angle;
    }

    @SuppressWarnings("unused")
    public Direction(double dis, double angle) {
        super();
        this.dis = dis;
        this.angle = angle;
    }

    @SuppressWarnings("unused")
    public Direction() {
    }

    @Override
    public String toString() {
        return "Direction [dis=" + dis + ", angle=" + angle + "]";
    }
}