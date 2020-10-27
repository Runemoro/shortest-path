package shortestpath.pathfinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Pathfinder {
    private final CollisionMap map;
    private final Node start;
    private final Positon target;
    private final List<Node> boundary = new LinkedList<>();
    private final Set<Positon> visited = new HashSet<>();
    private final Map<Positon, List<Positon>> transports;

    public Pathfinder(CollisionMap map, Map<Positon, List<Positon>> transports, Positon start, Positon target) {
        this.map = map;
        this.transports = transports;
        this.target = target;
        this.start = new Node(start, null);
    }

    public List<Positon> find() {
        boundary.add(start);

        Node nearest = null;
        int bestDistance = Integer.MAX_VALUE;

        while (!boundary.isEmpty()) {
            Node node = boundary.remove(0);

            if (node.position.equals(target)) {
                return node.path();
            }

            int distance = Math.max(Math.abs(node.position.x - target.x), Math.abs(node.position.y - target.y));
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
        if (map.w(node.position.x, node.position.y, node.position.z)) {
            addNeighbor(node, new Positon(node.position.x - 1, node.position.y, node.position.z));
        }

        if (map.e(node.position.x, node.position.y, node.position.z)) {
            addNeighbor(node, new Positon(node.position.x + 1, node.position.y, node.position.z));
        }

        if (map.s(node.position.x, node.position.y, node.position.z)) {
            addNeighbor(node, new Positon(node.position.x, node.position.y - 1, node.position.z));
        }

        if (map.n(node.position.x, node.position.y, node.position.z)) {
            addNeighbor(node, new Positon(node.position.x, node.position.y + 1, node.position.z));
        }

        if (map.sw(node.position.x, node.position.y, node.position.z)) {
            addNeighbor(node, new Positon(node.position.x - 1, node.position.y - 1, node.position.z));
        }

        if (map.se(node.position.x, node.position.y, node.position.z)) {
            addNeighbor(node, new Positon(node.position.x + 1, node.position.y - 1, node.position.z));
        }

        if (map.nw(node.position.x, node.position.y, node.position.z)) {
            addNeighbor(node, new Positon(node.position.x - 1, node.position.y + 1, node.position.z));
        }

        if (map.ne(node.position.x, node.position.y, node.position.z)) {
            addNeighbor(node, new Positon(node.position.x + 1, node.position.y + 1, node.position.z));
        }

        for (Positon transport : transports.getOrDefault(node.position, new ArrayList<>())) {
            addNeighbor(node, transport);
        }
    }

    private void addNeighbor(Node node, Positon neighbor) {
        if (!visited.add(neighbor)) {
            return;
        }

        boundary.add(new Node(neighbor, node));
    }

    private static class Node {
        public final Positon position;
        public final Node previous;

        public Node(Positon position, Node previous) {
            this.position = position;
            this.previous = previous;
        }

        public List<Positon> path() {
            List<Positon> path = new LinkedList<>();
            Node node = this;

            while (node != null) {
                path.add(0, node.position);
                node = node.previous;
            }

            return new ArrayList<>(path);
        }
    }
}
