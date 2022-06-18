package shortestpath.pathfinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import shortestpath.ShortestPathConfig;
import shortestpath.Transport;

public class Pathfinder {
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);

    public final CollisionMap map;
    public final Map<WorldPoint, List<Transport>> transports;

    public Pathfinder(CollisionMap map, Map<WorldPoint, List<Transport>> transports) {
        this.map = map;
        this.transports = transports;
    }

    public static boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 || WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    public class Path implements Runnable {
        private final Node start;
        private final WorldPoint target;
        private final boolean avoidWilderness;
        private final boolean useAgilityShortcuts;
        private final boolean useGrappleShortcuts;
        private final int agilityLevel;
        private final int rangedLevel;
        private final int strengthLevel;

        private final List<Node> boundary = new LinkedList<>();
        private final Set<WorldPoint> visited = new HashSet<>();

        public Node nearest;
        private List<WorldPoint> path = new ArrayList<>();

        public boolean loading;

        public Path(WorldPoint start, WorldPoint target, ShortestPathConfig config, Client client) {
            this.target = target;
            this.start = new Node(start, null);
            this.avoidWilderness = config.avoidWilderness();
            this.useAgilityShortcuts = config.useAgilityShortcuts();
            this.useGrappleShortcuts = config.useGrappleShortcuts();
            this.agilityLevel = client.getBoostedSkillLevel(Skill.AGILITY);
            this.rangedLevel = client.getBoostedSkillLevel(Skill.RANGED);
            this.strengthLevel = client.getBoostedSkillLevel(Skill.STRENGTH);
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

            for (Transport transport : transports.getOrDefault(node.position, new ArrayList<>())) {
                if (canPlayerUseTransport(transport)) {
                    addNeighbor(node, transport.getOrigin());
                    addNeighbor(node, transport.getDestination());
                }
            }
        }

        public List<WorldPoint> currentBest() {
            return nearest == null ? null : nearest.path();
        }

        public List<WorldPoint> getPath() {
            return this.path;
        }

        public WorldPoint getStart() {
            return start.position;
        }

        public WorldPoint getTarget() {
            return target;
        }

        private void addNeighbor(Node node, WorldPoint neighbor) {
            if (avoidWilderness && isInWilderness(neighbor) && !isInWilderness(node.position) && !isInWilderness(target)) {
                return;
            }

            if (!visited.add(neighbor)) {
                return;
            }

            boundary.add(new Node(neighbor, node));
        }

        private boolean canPlayerUseTransport(Transport transport) {
            final int transportAgilityLevel = transport.getAgilityLevelRequired();
            final int transportRangedLevel = transport.getRangedLevelRequired();
            final int transportStrengthLevel = transport.getStrengthLevelRequired();

            final boolean isAgilityShortcut = transportAgilityLevel > 1;
            final boolean isGrappleShortcut = isAgilityShortcut && (transportRangedLevel > 1 || transportStrengthLevel > 1);

            if (!isAgilityShortcut) {
                return true;
            }

            if (!useAgilityShortcuts) {
                return false;
            }

            if (!useGrappleShortcuts && isGrappleShortcut) {
                return false;
            }

            if (useGrappleShortcuts && isGrappleShortcut && agilityLevel >= transportAgilityLevel &&
                rangedLevel >= transportRangedLevel && strengthLevel >= transportStrengthLevel) {
                return true;
            }

            return agilityLevel >= transportAgilityLevel;
        }

        @Override
        public void run() {
            boundary.add(start);

            int bestDistance = Integer.MAX_VALUE;

            while (!boundary.isEmpty()) {
                Node node = boundary.remove(0);

                if (node.position.equals(target)) {
                    this.path = node.path();
                    this.loading = false;
                    return;
                }

                int distance = node.position.distanceTo(target);
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
