package com.lanan.zigbeetransmission.dataclass;

public class LocationInfo {

    private double lng;
    private double lat;

    public LocationInfo() {
        super();
    }

    public LocationInfo(double x, double y) {
        this.lng = x;
        this.lat = y;
    }

    public double getLng() {
        return this.lng;
    }

    public double getLat() {
        return this.lat;
    }

    public void setLng(double x) {
        this.lng = x;
    }

    public void setLat(double y) {
        this.lat = y;
    }

    public void set(double x, double y) {
        this.lng = x;
        this.lat = y;
    }
}
