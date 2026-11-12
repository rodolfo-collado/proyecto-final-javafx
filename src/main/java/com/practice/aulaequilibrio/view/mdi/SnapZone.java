package com.practice.aulaequilibrio.view.mdi;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

enum SnapZone {
    MAXIMIZED(0, 0, 1, 1),
    LEFT(0, 0, 0.5, 1),
    RIGHT(0.5, 0, 0.5, 1),
    TOP_LEFT(0, 0, 0.5, 0.5),
    TOP_RIGHT(0.5, 0, 0.5, 0.5),
    BOTTOM_LEFT(0, 0.5, 0.5, 0.5),
    BOTTOM_RIGHT(0.5, 0.5, 0.5, 0.5);

    private static final double EDGE = 24;
    private static final double CORNER = 84;

    private final double x;
    private final double y;
    private final double width;
    private final double height;

    SnapZone(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    Rectangle2D bounds(double desktopWidth, double desktopHeight) {
        return new Rectangle2D(x * desktopWidth, y * desktopHeight,
                width * desktopWidth, height * desktopHeight);
    }

    static SnapZone at(Point2D pointer, double width, double height) {
        double x = pointer.getX();
        double y = pointer.getY();
        if (x < 0 || x > width || y < 0 || y > height) {
            return null;
        }
        boolean left = x <= EDGE;
        boolean right = width - x <= EDGE;
        boolean top = y <= EDGE;
        boolean bottom = height - y <= EDGE;
        if ((left && y <= CORNER) || (top && x <= CORNER)) {
            return TOP_LEFT;
        }
        if ((right && y <= CORNER) || (top && width - x <= CORNER)) {
            return TOP_RIGHT;
        }
        if ((left && height - y <= CORNER) || (bottom && x <= CORNER)) {
            return BOTTOM_LEFT;
        }
        if ((right && height - y <= CORNER) || (bottom && width - x <= CORNER)) {
            return BOTTOM_RIGHT;
        }
        if (top) {
            return MAXIMIZED;
        }
        if (left) {
            return LEFT;
        }
        return right ? RIGHT : null;
    }
}
