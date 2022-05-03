package shortestpath.pathfinder;

import net.runelite.api.coords.WorldPoint;
import shortestpath.ShortestPathPlugin;
import shortestpath.Transport;

import java.util.*;

public class Pathfinder {
    private final CollisionMap map;
    private final Node start;
    private final WorldPoint target;
    private final List<Node> boundary = new LinkedList<>();
    private final Set<WorldPoint> visited = new HashSet<>();
    private final Map<WorldPoint, List<Transport>> transports;
    private final boolean avoidWilderness;
    private Node nearest;
    private final int agilityLevel;

    public Pathfinder(CollisionMap map, Map<WorldPoint, List<Transport>> transports, WorldPoint start, WorldPoint target, boolean avoidWilderness, int agilityLevel) {
        this.map = map;
        this.transports = transports;
        this.target = target;
        this.start = new Node(start, null);
        this.avoidWilderness = avoidWilderness;
        nearest = null;
        this.agilityLevel = agilityLevel;
    }

    public List<WorldPoint> find() {
        boundary.add(start);

        int bestDistance = Integer.MAX_VALUE;

        while (!boundary.isEmpty()) {
            Node node = boundary.remove(0);

            // if we are on the target block, return a path to this block
            if (node.position.equals(target)) {
                return node.path();
            }

            // add all the neighbouring blocks to this node to the graph 
            addNeighbors(node);

            int distance = node.position.distanceTo2D(target);

            if (nearest == null || distance < bestDistance) {
                nearest = node;
                bestDistance = distance;
            }

            // Check this node for valid transports
            for (Transport transport : transports.getOrDefault(node.position, new ArrayList<>())) {
                if (canPlayerUseTransport(transport)) {
                    addNeighbor(node, transport.getOrigin());
                    addNeighbor(node, transport.getDestination());
                } else if (bestDistance == distance) {
                    // Player cannot use this tile, push out the bestDistance to allow selection of a new node
                    bestDistance += 1;
                }
            }
        }

        return currentBest();
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
    }

    private boolean canPlayerUseTransport(Transport transport) {
        // Currently, only supports agility level check
        return agilityLevel >= transport.getAgilityLevelRequired();
    }

    public List<WorldPoint> currentBest() {
        return nearest == null ? null : nearest.path();
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
