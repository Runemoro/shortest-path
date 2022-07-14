package shortestpath.pathfinder;

import java.awt.Point;
import java.util.Map;

public class CollisionMap extends SplitFlagMap {
    public CollisionMap(int regionSize, Map<Position, byte[]> compressedRegions) {
        super(regionSize, compressedRegions, 2);
    }

    public boolean check(int x, int y, int z) {
        return get(x, y, z, 0);
    }

    public boolean checkDirection(int x, int y, int z, OrdinalDirection dir) {
        Point direction = dir.toPoint();
        return check(x + direction.x, y + direction.y, z);
    }
}
