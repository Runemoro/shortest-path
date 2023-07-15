package shortestpath.pathfinder;

import net.runelite.api.coords.WorldPoint;

public class VisitedTiles {
    private static final int TOTAL_PLANES = 4;
    private static final int REGION_SIZE = 64;

    // TODO: what is the max number of regions?
    private static final int REGION_EXTENT_X = 192;
    private static final int REGION_EXTENT_Y = 256;
    private final VisitedRegion[] visitedRegions = new VisitedRegion[REGION_EXTENT_X * REGION_EXTENT_Y];

    public boolean get(WorldPoint point) {
        final int regionIndex = point.getRegionID();
        final VisitedRegion region = visitedRegions[regionIndex];

        if (region == null) {
            return false;
        }

        return region.get(point.getRegionX(), point.getRegionY(), point.getPlane());
    }
    public boolean set(WorldPoint point) {
        final int regionIndex = point.getRegionID();
        VisitedRegion region = visitedRegions[regionIndex];

        if (region == null) {
            region = new VisitedRegion();
            visitedRegions[regionIndex] = region;
        }

        return region.set(point.getRegionX(), point.getRegionY(), point.getPlane());
    }

    public void clear() {
        for (int i = 0; i < visitedRegions.length; ++i) {
            if (visitedRegions[i] != null) {
                visitedRegions[i] = null;
            }
        }
    }

    private class VisitedRegion {
        // This assumes a row is at most 64 tiles and fits in a long
        private final long[] planes = new long[TOTAL_PLANES * REGION_SIZE];

        // Sets a tile as visited in the tile bitset
        // Returns true if the tile is unique and hasn't been seen before or false if it was seen before
        public boolean set(int x, int y, int plane) {
            final int index = y + plane * REGION_SIZE;
            boolean unique = (planes[index] & (1L << x)) == 0;
            planes[index] |= 1L << x;
            return unique;
        }

        public boolean get(int x, int y, int plane) {
            return (planes[y + plane * REGION_SIZE] & (1L << x)) != 0;
        }
    }
}
