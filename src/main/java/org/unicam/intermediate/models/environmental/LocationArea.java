package org.unicam.intermediate.models.environmental;

public class LocationArea {
    private double minX, maxX, minY, maxY;

    public LocationArea(double minX, double maxX, double minY, double maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public boolean contains(double lat, double lon) {
        return lon >= minX
                && lon <= maxX
                && lat >= minY
                && lat <= maxY;
    }
}
