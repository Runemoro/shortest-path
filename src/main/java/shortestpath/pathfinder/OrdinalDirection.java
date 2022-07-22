package shortestpath.pathfinder;

import java.awt.Point;

public enum OrdinalDirection {
    NORTH {
        public Point toPoint() {
            return new Point(0, 1);
        }
    },
    EAST {
        public Point toPoint() {
            return new Point(1, 0);
        }
    },
    SOUTH {
        public Point toPoint() {
            return new Point(0, -1);
        }
    },
    WEST {
        public Point toPoint() {
            return new Point(-1, 0);
        }
    },
    NORTH_WEST {
        public Point toPoint() {
            return new Point(-1, 1);
        }
    },
    NORTH_EAST {
        public Point toPoint() {
            return new Point(1, 1);
        }
    },
    SOUTH_EAST {
        public Point toPoint() {
            return new Point(1, -1);
        }
    },
    SOUTH_WEST {
        public Point toPoint() {
            return new Point(-1, -1);
        }
    };

    public abstract Point toPoint();
}
