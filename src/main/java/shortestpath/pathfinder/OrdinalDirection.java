package shortestpath.pathfinder;

import java.awt.*;

public enum OrdinalDirection {
    NORTH,
    EAST,
    SOUTH,
    WEST,
    NORTH_WEST,
    NORTH_EAST,
    SOUTH_EAST,
    SOUTH_WEST;

    public Point toPoint() {
        switch (this) {
            case NORTH:
                return new Point(0, 1);
            case EAST:
                return new Point(1, 0);
            case SOUTH:
                return new Point(0, -1);
            case WEST:
                return new Point(-1, 0);
            case NORTH_WEST:
                return new Point(-1, 1);
            case NORTH_EAST:
                return new Point(1, 1);
            case SOUTH_EAST:
                return new Point(1, -1);
            case SOUTH_WEST:
                return new Point(-1, -1);
            default:
                return new Point(0, 0);
        }
    }
}
