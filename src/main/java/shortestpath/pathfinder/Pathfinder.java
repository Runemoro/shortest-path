package shortestpath.pathfinder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import shortestpath.Transport;

public class Pathfinder implements Runnable {
    @Getter
    private final WorldPoint start;
    @Getter
    private final WorldPoint target;
    private final PathfinderConfig config;

    private final List<Node> boundary = new LinkedList<>();
    private final Set<WorldPoint> visited = new HashSet<>();
    private final List<Node> pending = new ArrayList<Node>() {
        @Override
        public boolean add(Node n) {
            boolean result = super.add(n);
            sort(null);
            return result;
        }
    };

    @Getter
    private List<WorldPoint> path = new ArrayList<>();
    @Getter
    private boolean done = false;

    public Pathfinder(PathfinderConfig config, WorldPoint start, WorldPoint target) {
        this.config = config;
        this.start = start;
        this.target = target;
        this.config.refresh();

        new Thread(this).start();
    }

    private void addNeighbor(Node node, WorldPoint neighbor, int wait) {
        if (config.avoidWilderness(node.position, neighbor, target)) {
            return;
        }

        if (visited.add(neighbor)) {
            Node n = new Node(neighbor, node, target, wait);
            if (n.isTransport()) {
                pending.add(n);
            } else {
                boundary.add(n);
            }
        }
    }

    private void addNeighbors(Node node) {
        for (OrdinalDirection direction : OrdinalDirection.values()) {
            for (Transport transport : config.getTransports().getOrDefault(node.position.dx(direction.x).dy(direction.y), new ArrayList<>())) {
                WorldPoint origin = transport.getOrigin();
                if (config.useTransport(transport) && config.getMap().isBlocked(origin.getX(), origin.getY(), origin.getPlane())) {
                    addNeighbor(new Node(origin, node, target), transport.getDestination(), transport.getWait());
                }
            }
        }

        for (WorldPoint neighbor : config.getMap().getNeighbors(node.position)) {
            addNeighbor(node, neighbor, 0);
        }

        for (Transport transport : config.getTransports().getOrDefault(node.position, new ArrayList<>())) {
            if (config.useTransport(transport)) {
                addNeighbor(node, transport.getDestination(), transport.getWait());
            }
        }
    }

    private boolean isHeuristicBetter(long candidate, List<Node> data) {
        for (Node n : data) {
            if (n.heuristic <= candidate) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        boundary.add(new Node(start, null, target));

        Node nearest = boundary.get(0);
        long bestDistance = Integer.MAX_VALUE;
        Instant cutoffTime = Instant.now().plus(PathfinderConfig.CALCULATION_CUTOFF);

        while (!boundary.isEmpty()) {
            Node node = boundary.remove(0);

            if (pending.size() > 0) {
                Node p = pending.get(0);
                if (isHeuristicBetter(p.heuristic, boundary)) {
                    boundary.add(0, p);
                    pending.remove(p);
                }
            }

            if (node.position.equals(target) || !config.isNear(start)) {
                path = node.getPath();
                break;
            }

            long distance = node.heuristic;
            if (distance < bestDistance) {
                path = node.getPath();
                nearest = node;
                bestDistance = distance;
                cutoffTime = Instant.now().plus(PathfinderConfig.CALCULATION_CUTOFF);
            }

            if (Instant.now().isAfter(cutoffTime)) {
                path = nearest.getPath();
                break;
            }

            addNeighbors(node);
        }

        done = true;
        boundary.clear();
        visited.clear();
        pending.clear();
    }
}
