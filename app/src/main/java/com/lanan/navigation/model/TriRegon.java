package com.lanan.navigation.model;

public class TriRegon {

    private PointInfo mO;
    private PointInfo mA;
    private PointInfo mB;

    public PointInfo getmO() {
        return mO;
    }

    public void setmO(PointInfo mO) {
        this.mO = mO;
    }

    public PointInfo getmA() {
        return mA;
    }

    public void setmA(PointInfo mA) {
        this.mA = mA;
    }

    public PointInfo getmB() {
        return mB;
    }

    public void setmB(PointInfo mB) {
        this.mB = mB;
    }

    public TriRegon() {
        super();
    }

    public TriRegon(double ax, double ay, double bx, double by, double ox, double oy) {
        mA = new PointInfo(ax, ay);
        mB = new PointInfo(bx, by);
        mO = new PointInfo(ox, oy);
    }

    @Override
    public String toString() {
        return "TriRegon [mO=" + mO + ", mA=" + mA + ", mB=" + mB + "]";
    }
}