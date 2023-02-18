package shortestpath.pathfinder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import shortestpath.Transport;

public class Pathfinder implements Runnable {
    @Getter
    private final WorldPoint start;
    @Getter
    private final WorldPoint target;
    private final PathfinderConfig config;

    private final Queue<Node> boundary = new PriorityQueue<>();
    private final Map<WorldPoint, Node> visited = new HashMap<>();

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

        Node n = visited.get(neighbor);
        if (n == null) {
            n = new Node(neighbor, node);
            visited.put(n.position, n);
        }

        int newCost = Node.cost(node, neighbor, wait);
        if (newCost < n.cost) {
            n.cost = newCost;
            n.previous = node;
            boundary.remove(n);
            boundary.add(n);
        }
    }

    private void addNeighbors(Node node) {
        for (WorldPoint neighbor : config.getMap().getNeighbors(node.position, config)) {
            addNeighbor(node, neighbor, 0);
        }

        for (Transport transport : config.getTransports().getOrDefault(node.position, new ArrayList<>())) {
            if (config.useTransport(transport)) {
                addNeighbor(node, transport.getDestination(), transport.getWait());
            }
        }
    }

    @Override
    public void run() {
        Node nearest = new Node(start, null);
        nearest.cost = 0;

        boundary.add(nearest);
        visited.put(nearest.position, nearest);

        int bestDistance = Integer.MAX_VALUE;
        long bestHeuristic = Integer.MAX_VALUE;
        Instant cutoffTime = Instant.now().plus(config.getCalculationCutoff());

        while (!boundary.isEmpty()) {
            Node node = boundary.poll();

            if (node.position.equals(target) || !config.isNear(start)) {
                path = node.getPath();
                break;
            }

            int distance = Node.distanceBetween(node.position, target);
            long heuristic = distance + Node.distanceBetween(node.position, target, 2);
            if (heuristic < bestHeuristic || (heuristic <= bestHeuristic && distance < bestDistance)) {
                path = node.getPath();
                bestDistance = distance;
                bestHeuristic = heuristic;
                cutoffTime = Instant.now().plus(config.getCalculationCutoff());
            }

            if (Instant.now().isAfter(cutoffTime)) {
                break;
            }

            addNeighbors(node);
        }

        done = true;
        boundary.clear();
        visited.clear();
    }
}
