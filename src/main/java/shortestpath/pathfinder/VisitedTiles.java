package shortestpath.pathfinder;

import net.runelite.api.coords.WorldPoint;

import static net.runelite.api.Constants.MAX_Z;
import static net.runelite.api.Constants.REGION_SIZE;

public class VisitedTiles {
    private final CollisionMap.RegionExtent regionExtents;
    private final int widthInclusive;

    private final VisitedRegion[] visitedRegions;

    public VisitedTiles() {
        regionExtents = CollisionMap.getRegionExtents();
        widthInclusive = regionExtents.getWidth() + 1;
        final int heightInclusive = regionExtents.getHeight() + 1;

        visitedRegions = new VisitedRegion[widthInclusive * heightInclusive];
    }

    public boolean get(WorldPoint point) {
        final int regionIndex = getRegionIndex(point.getX() / REGION_SIZE, point.getY() / REGION_SIZE);
        if (regionIndex < 0 || regionIndex >= visitedRegions.length) {
            return true; // Region is out of bounds; report that it's been visited to avoid exploring it further
        }

        final VisitedRegion region = visitedRegions[regionIndex];
        if (region == null) {
            return false;
        }

        return region.get(point.getRegionX(), point.getRegionY(), point.getPlane());
    }

    public boolean set(WorldPoint point) {
        final int regionIndex = getRegionIndex(point.getX() / REGION_SIZE, point.getY() / REGION_SIZE);
        if (regionIndex < 0 || regionIndex >= visitedRegions.length) {
            return false; // Region is out of bounds; report that it's been visited to avoid exploring it further
        }

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

    private int getRegionIndex(int regionX, int regionY) {
        return (regionX - regionExtents.minX) + (regionY - regionExtents.minY) * widthInclusive;
    }

    private class VisitedRegion {
        // This assumes a row is at most 64 tiles and fits in a long
        private final long[] planes = new long[MAX_Z * REGION_SIZE];

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
