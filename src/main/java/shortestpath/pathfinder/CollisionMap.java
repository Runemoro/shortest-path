package shortestpath.pathfinder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;
import shortestpath.ShortestPathPlugin;
import shortestpath.Transport;
import shortestpath.Util;

public class CollisionMap extends SplitFlagMap {

    // Enum.values() makes copies every time which hurts performance in the hotpath
    private static final OrdinalDirection[] ORDINAL_VALUES = OrdinalDirection.values();
    private static RegionExtent regionExtents = new RegionExtent(0, 0, 0, 0);

    public CollisionMap(Map<Integer, byte[]> compressedRegions) {
        super(compressedRegions, 2);
    }

    public boolean n(int x, int y, int z) {
        return get(x, y, z, 0);
    }

    public boolean s(int x, int y, int z) {
        return n(x, y - 1, z);
    }

    public boolean e(int x, int y, int z) {
        return get(x, y, z, 1);
    }

    public boolean w(int x, int y, int z) {
        return e(x - 1, y, z);
    }

    private boolean ne(int x, int y, int z) {
        return n(x, y, z) && e(x, y + 1, z) && e(x, y, z) && n(x + 1, y, z);
    }

    private boolean nw(int x, int y, int z) {
        return n(x, y, z) && w(x, y + 1, z) && w(x, y, z) && n(x - 1, y, z);
    }

    private boolean se(int x, int y, int z) {
        return s(x, y, z) && e(x, y - 1, z) && e(x, y, z) && s(x + 1, y, z);
    }

    private boolean sw(int x, int y, int z) {
        return s(x, y, z) && w(x, y - 1, z) && w(x, y, z) && s(x - 1, y, z);
    }

    public boolean isBlocked(int x, int y, int z) {
        return !n(x, y, z) && !s(x, y, z) && !e(x, y, z) && !w(x, y, z);
    }

    private static WorldPoint pointFromOrdinal(WorldPoint start, OrdinalDirection direction) {
        return new WorldPoint(start.getX() + direction.x, start.getY() + direction.y, start.getPlane());
    }

    public List<Node> getNeighbors(Node node, PathfinderConfig config) {
        int x = node.position.getX();
        int y = node.position.getY();
        int z = node.position.getPlane();

        List<Node> neighbors = new ArrayList<>();

        // Transports are pre-filtered by PathfinderConfig.refreshTransportData
        // Thus any transports in the list are guaranteed to be valid per the user's settings
        for (Transport transport : config.getTransports().getOrDefault(node.position, (List<Transport>) Collections.EMPTY_LIST)) {
            neighbors.add(new TransportNode(transport.getDestination(), node, transport.getWait()));
        }

        boolean[] traversable;
        if (isBlocked(x, y, z)) {
            boolean westBlocked = isBlocked(x - 1, y, z);
            boolean eastBlocked = isBlocked(x + 1, y, z);
            boolean southBlocked = isBlocked(x, y - 1, z);
            boolean northBlocked = isBlocked(x, y + 1, z);
            boolean southWestBlocked = isBlocked(x - 1, y - 1, z);
            boolean southEastBlocked = isBlocked(x + 1, y - 1, z);
            boolean northWestBlocked = isBlocked(x - 1, y + 1, z);
            boolean northEastBlocked = isBlocked(x + 1, y + 1, z);
            traversable = new boolean[] {
                !westBlocked,
                !eastBlocked,
                !southBlocked,
                !northBlocked,
                !southWestBlocked && !westBlocked && !southBlocked,
                !southEastBlocked && !eastBlocked && !southBlocked,
                !northWestBlocked && !westBlocked && !northBlocked,
                !northEastBlocked && !eastBlocked && !northBlocked
            };
        } else {
            traversable = new boolean[] {
                w(x, y, z), e(x, y, z), s(x, y, z), n(x, y, z), sw(x, y, z), se(x, y, z), nw(x, y, z), ne(x, y, z)
            };
        }

        for (int i = 0; i < traversable.length; i++) {
            OrdinalDirection d = ORDINAL_VALUES[i];
            WorldPoint neighbor = pointFromOrdinal(node.position, d);
            if (traversable[i]) {
                neighbors.add(new Node(neighbor, node));
            } else if (Math.abs(d.x + d.y) == 1 && isBlocked(x + d.x, y + d.y, z)) {
                for (Transport transport : config.getTransports().getOrDefault(neighbor, (List<Transport>)Collections.EMPTY_LIST)) {
                    neighbors.add(new Node(transport.getOrigin(), node));
                }
            }
        }

        return neighbors;
    }

    public static CollisionMap fromResources() {
        Map<Integer, byte[]> compressedRegions = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(ShortestPathPlugin.class.getResourceAsStream("/collision-map.zip"))) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = 0;
            int maxY = 0;

            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String[] n = entry.getName().split("_");
                final int x = Integer.parseInt(n[0]);
                final int y = Integer.parseInt(n[1]);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);

                compressedRegions.put(
                        SplitFlagMap.packPosition(x, y),
                        Util.readAllBytes(in)
                );
            }

            regionExtents = new RegionExtent(minX, minY, maxX, maxY);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new CollisionMap(compressedRegions);
    }

    public static RegionExtent getRegionExtents() {
        return regionExtents;
    }

    @RequiredArgsConstructor
    @Getter
    public static class RegionExtent {
        public final int minX, minY, maxX, maxY;

        public int getWidth() {
            return maxX - minX;
        }

        public int getHeight() {
            return maxY - minY;
        }
    }
}
