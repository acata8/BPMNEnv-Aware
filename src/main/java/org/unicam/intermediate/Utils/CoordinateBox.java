package org.unicam.intermediate.Utils;

public class CoordinateBox {
    private double minX, maxX, minY, maxY;

    public CoordinateBox(double minX, double maxX, double minY, double maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public boolean contains(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }
}
