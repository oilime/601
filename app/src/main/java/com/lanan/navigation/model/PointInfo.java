package com.lanan.navigation.model;

@SuppressWarnings("unused")
public class PointInfo {

    private double lng;
    private double lat;

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public PointInfo(double lng, double lat) {
        super();
        this.lng = lng;
        this.lat = lat;
    }

    @Override
    public String toString() {
        return "PointInfo [lng=" + lng + ", lat=" + lat + "]";
    }
}