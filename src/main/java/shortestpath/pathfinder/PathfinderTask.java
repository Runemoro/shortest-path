package shortestpath.pathfinder;

import java.util.*;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import shortestpath.Path;
import shortestpath.Transport;

public class PathfinderTask implements Runnable {
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);

    private final WorldPoint start;
    private final WorldPoint target;
    private final PathfinderConfig config;

    private List<Node> boundary = new LinkedList<>();
    private Set<WorldPoint> visited = new HashSet<>();

    private Path path;
    private boolean done = false;

    public PathfinderTask(PathfinderConfig config, WorldPoint start, WorldPoint target) {
        this.config = config;
        this.start = start;
        this.target = target;

        new Thread(this).start();
    }

    public static class PathfinderConfig {
        public CollisionMap map;
        public Map<WorldPoint, List<Transport>> transports;
        public boolean avoidWilderness = true;
        public boolean useAgilityShortcuts = false;
        public boolean useGrappleShortcuts = false;
        public int agilityLevel = 1;
        public int rangedLevel = 1;
        public int strengthLevel = 1;

        public PathfinderConfig(CollisionMap map) {
            this.map = map;
            this.transports = null;
        }

        public PathfinderConfig(CollisionMap map, Map<WorldPoint, List<Transport>> transports) {
            this.map = map;
            this.transports = transports;
        }
    }

    private static boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 || WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    public boolean isDone() {
        return this.done;
    }

    public WorldPoint getStart() {
        return this.start;
    }

    public WorldPoint getTarget() {
        return this.target;
    }

    public Path getPath() {
        return this.path;
    }

    private void addNeighbor(Node node, WorldPoint neighbor) {
        if (config.avoidWilderness && isInWilderness(neighbor) && !isInWilderness(node.position) && !isInWilderness(target)) {
            return;
        }
        if (!visited.add(neighbor)) {
            return;
        }
        boundary.add(new Node(neighbor, node));
    }

    private void addNeighbors(Node node) {
        for (OrdinalDirection direction : OrdinalDirection.values()) {
            if (config.map.checkDirection(node.position.getX(), node.position.getY(), node.position.getPlane(), direction)) {
                addNeighbor(node, new WorldPoint(node.position.getX() + direction.toPoint().x, node.position.getY() + direction.toPoint().y, node.position.getPlane()));
            }
        }

        for (Transport transport : config.transports.getOrDefault(node.position, new ArrayList<>())) {
            addNeighbor(node, transport.getDestination());
        }
    }

    @Override
    public void run() {
        boundary.add(new Node(start, null));

        int bestDistance = Integer.MAX_VALUE;
        while (!boundary.isEmpty()) {
            Node node = boundary.remove(0);

            if (node.position.equals(target)) {
                this.path = node.getPath();
                break;
            }

            int distance = node.position.distanceTo(target);
            if (this.path == null || distance < bestDistance) {
                this.path = node.getPath();
                bestDistance = distance;
            }

            addNeighbors(node);
        }

        this.done = true;
    }
}
