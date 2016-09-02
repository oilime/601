package com.lanan.zigbeetransmission.dataclass;

public class NavigationInfo {
    private double distance;
    private double angle;
    private int pos;
    private boolean arriveStatus;
    private boolean yawStatus;

    public NavigationInfo() {
        super();
    }

    public NavigationInfo(double dis, double angle, int pos, boolean arrived, boolean yaw) {
        this.distance = dis;
        this.angle = angle;
        this.pos = pos;
        this.arriveStatus = arrived;
        this.yawStatus = yaw;
    }

    public void setDistance(double dis) {
        this.distance = dis;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public void setArriveStatus(boolean status) {
        this.arriveStatus = status;
    }

    public void setYawStatus(boolean status) {
        this.yawStatus = status;
    }

    public double getDistance() {
        return this.distance;
    }

    public double getAngle() {
        return this.angle;
    }

    public int getPos() {
        return this.pos;
    }

    public boolean isArrived() {
        return this.arriveStatus;
    }

    public boolean isYawed() {
        return this.yawStatus;
    }
}
