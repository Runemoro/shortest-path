package shortestpath.pathfinder;

import net.runelite.api.coords.WorldPoint;
import shortestpath.ShortestPathPlugin;

import java.util.*;

public class Pathfinder {
    private final CollisionMap map;
    private final Node start;
    private final WorldPoint target;
    private final List<Node> boundary = new LinkedList<>();
    private final Set<WorldPoint> visited = new HashSet<>();
    private final Map<WorldPoint, List<WorldPoint>> transports;
    private final boolean avoidWilderness;
    private Node nearest;

    public Pathfinder(CollisionMap map, Map<WorldPoint, List<WorldPoint>> transports, WorldPoint start, WorldPoint target, boolean avoidWilderness) {
        this.map = map;
        this.transports = transports;
        this.target = target;
        this.start = new Node(start, null);
        this.avoidWilderness = avoidWilderness;
        nearest = null;
    }

    public List<WorldPoint> find() {
        boundary.add(start);

        int bestDistance = Integer.MAX_VALUE;

        while (!boundary.isEmpty()) {
            Node node = boundary.remove(0);

            if (node.position.equals(target)) {
                return node.path();
            }

            int distance = Math.max(Math.abs(node.position.getX() - target.getX()), Math.abs(node.position.getY() - target.getY()));
            if (nearest == null || distance < bestDistance) {
                nearest = node;
                bestDistance = distance;
            }

            addNeighbors(node);
        }

        if (nearest != null) {
            return nearest.path();
        }

        return null;
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
        return nearest==null ? null : nearest.path();
    }

    private void addNeighbor(Node node, WorldPoint neighbor) {
        if (avoidWilderness && ShortestPathPlugin.isInWilderness(neighbor)) {
            return;
        }

        if (!visited.add(neighbor)) {
            return;
        }

        boundary.add(new Node(neighbor, node));
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
