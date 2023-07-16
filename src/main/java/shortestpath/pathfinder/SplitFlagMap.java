package shortestpath.pathfinder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import shortestpath.Util;

import static net.runelite.api.Constants.REGION_SIZE;

public abstract class SplitFlagMap {

    // Size is automatically chosen based on the max extents of the collision data
    private final FlagMap[] regionMaps;
    private final CollisionMap.RegionExtent regionExtents;
    private final int widthInclusive;
    private final int flagCount;

    public SplitFlagMap(Map<Integer, byte[]> compressedRegions, int flagCount) {
        this.flagCount = flagCount;

        regionExtents = CollisionMap.getRegionExtents();
        widthInclusive = regionExtents.getWidth() + 1;
        final int heightInclusive = regionExtents.getHeight() + 1;
        regionMaps = new FlagMap[widthInclusive * heightInclusive];

        for (Map.Entry<Integer, byte[]> entry : compressedRegions.entrySet()) {
            final int pos = entry.getKey();
            final int x = unpackX(pos);
            final int y = unpackY(pos);

            FlagMap map;
            try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(entry.getValue()))) {
                byte[] bytes = Util.readAllBytes(in);
                map = new FlagMap(bytes, this.flagCount);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            regionMaps[getIndex(x, y)] = map;
        }
    }

    public boolean get(int x, int y, int z, int flag) {
        final int index = getIndex(x / REGION_SIZE, y / REGION_SIZE);
        if (index < 0 || index >= regionMaps.length || regionMaps[index] == null) {
            return false;
        }

        return regionMaps[index].get(x, y, z, flag);
    }

    private int getIndex(int regionX, int regionY) {
        return (regionX - regionExtents.getMinX()) + (regionY - regionExtents.getMinY()) * widthInclusive;
    }

    public static int unpackX(int position) {
        return position & 0xFFFF;
    }

    public static int unpackY(int position) {
        return (position >> 16) & 0xFFFF;
    }

    public static int packPosition(int x, int y) {
        return (x & 0xFFFF) | ((y & 0xFFFF) << 16);
    }
}
