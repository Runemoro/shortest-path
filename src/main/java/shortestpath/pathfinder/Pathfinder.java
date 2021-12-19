package shortestpath.pathfinder;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import shortestpath.ShortestPathConfig;
import shortestpath.ShortestPathPlugin;
import shortestpath.Util;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Pathfinder {
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);

    public final CollisionMap map;
    public final Map<WorldPoint, List<WorldPoint>> transports;

    public Pathfinder(CollisionMap map, Map<WorldPoint, List<WorldPoint>> transports) {
        this.map = map;
        this.transports = transports;
    }

    public static boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 ||
                WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    public class Path implements Runnable {
        private final Node start;
        public final WorldPoint target;
        private final boolean avoidWilderness;

        private final List<Node> boundary = new LinkedList<>();
        private final Set<WorldPoint> visited = new HashSet<>();

        public Node nearest;
        private List<WorldPoint> path;

        public boolean loading;

        public Path(WorldPoint start, WorldPoint target, boolean avoidWilderness) {
            this.target = target;
            this.start = new Node(start, null);
            this.avoidWilderness = avoidWilderness;
            this.nearest = null;
            this.loading = true;

            new Thread(this).start();
        }

        private void addNeighbors(Node node) {
            if (map.w(node.position.getX(), node.position.getY(), node.position.getPlane())) {
                addNeighbor(node, new WorldPoint(node.position.getX() - 1, node.position.getY(), node.position.getPlane()));
            }

            if (map.e(node.position.getX(), node.position.getY(), node.position.getPlane())) {
                addNeighbor(node, new WorldPoint(node.position.getX() + 1, node.position.getY(), node.position.getPlane()));
            }

            if (map.s(node.position.getX(), node.position.getY(), node.position.getPlane())) {
                addNeighbor(node, new WorldPoint(node.position.getX(), node.position.getY() - 1, node.position.getPlane()));
            }

            if (map.n(node.position.getX(), node.position.getY(), node.position.getPlane())) {
                addNeighbor(node, new WorldPoint(node.position.getX(), node.position.getY() + 1, node.position.getPlane()));
            }

            if (map.sw(node.position.getX(), node.position.getY(), node.position.getPlane())) {
                addNeighbor(node, new WorldPoint(node.position.getX() - 1, node.position.getY() - 1, node.position.getPlane()));
            }

            if (map.se(node.position.getX(), node.position.getY(), node.position.getPlane())) {
                addNeighbor(node, new WorldPoint(node.position.getX() + 1, node.position.getY() - 1, node.position.getPlane()));
            }

            if (map.nw(node.position.getX(), node.position.getY(), node.position.getPlane())) {
                addNeighbor(node, new WorldPoint(node.position.getX() - 1, node.position.getY() + 1, node.position.getPlane()));
            }

            if (map.ne(node.position.getX(), node.position.getY(), node.position.getPlane())) {
                addNeighbor(node, new WorldPoint(node.position.getX() + 1, node.position.getY() + 1, node.position.getPlane()));
            }

            for (WorldPoint transport : transports.getOrDefault(node.position, new ArrayList<>())) {
                addNeighbor(node, transport);
            }
        }

        public List<WorldPoint> currentBest() {
            return nearest == null ? null : nearest.path();
        }

        public List<WorldPoint> getPath() {
            return this.path;
        }

        private void addNeighbor(Node node, WorldPoint neighbor) {
            if (avoidWilderness && isInWilderness(neighbor)) {
                return;
            }

            if (!visited.add(neighbor)) {
                return;
            }

            boundary.add(new Node(neighbor, node));
        }

        @Override
        public void run() {
            boundary.add(start);

            int bestDistance = Integer.MAX_VALUE;

            while (!boundary.isEmpty()) {
                Node node = boundary.remove(0);

                if (node.position.equals(target)) {
                    this.path = node.path();
                }

                int distance = Math.max(Math.abs(node.position.getX() - target.getX()), Math.abs(node.position.getY() - target.getY()));
                if (nearest == null || distance < bestDistance) {
                    nearest = node;
                    bestDistance = distance;
                }

                addNeighbors(node);
            }

            if (nearest != null) {
                this.path = nearest.path();
            }

            this.loading = false;
        }
    }

    private static class Node {
        public final WorldPoint position;
        public final Node previous;

        public Node(WorldPoint position, Node previous) {
            this.position = position;
            this.previous = previous;
        }

        public List<WorldPoint> path() {
            List<WorldPoint> path = new LinkedList<>();
            Node node = this;

            while (node != null) {
                path.add(0, node.position);
                node = node.previous;
            }

            return new ArrayList<>(path);
        }
    }
}
