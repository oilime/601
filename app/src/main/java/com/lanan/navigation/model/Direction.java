package com.lanan.navigation.model;

public class Direction {

    private double dis;
    private double angle;

    public double getDis() {
        return dis;
    }

    public void setDis(double dis) {
        this.dis = dis;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public Direction(double dis, double angle) {
        super();
        this.dis = dis;
        this.angle = angle;
    }

    public Direction() {
    }

    @Override
    public String toString() {
        return "Direction [dis=" + dis + ", angle=" + angle + "]";
    }
}