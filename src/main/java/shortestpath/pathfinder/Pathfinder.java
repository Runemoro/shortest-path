package shortestpath.pathfinder;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public class Pathfinder implements Runnable {
    @Getter
    private final WorldPoint start;
    @Getter
    private final WorldPoint target;
    private final PathfinderConfig config;
    private final boolean targetInWilderness;

    // Capacities should be enough to store all nodes without requiring the queue to grow
    // They were found by checking the max queue size
    private final Deque<Node> boundary = new ArrayDeque<>(4096);
    private final Queue<Node> pending = new PriorityQueue<>(256);
    private final VisitedTiles visited = new VisitedTiles();

    @Getter
    private List<WorldPoint> path = new ArrayList<>();
    @Getter
    private boolean done = false;

    public Pathfinder(PathfinderConfig config, WorldPoint start, WorldPoint target) {
        this.config = config;
        this.start = start;
        this.target = target;
        this.config.refresh();
        targetInWilderness = PathfinderConfig.isInWilderness(target);

        new Thread(this).start();
    }

    private void addNeighbors(Node node) {
        for (Node neighbor : config.getMap().getNeighbors(node, config)) {
            if (!visited.get(node.position) && config.avoidWilderness(node.position, neighbor.position, targetInWilderness)) {
                continue;
            }
            if (visited.set(neighbor.position)) {
                if (neighbor instanceof TransportNode) {
                    pending.add(neighbor);
                } else {
                    boundary.addLast(neighbor);
                }
            }
        }
    }

    @Override
    public void run() {
        boundary.addFirst(new Node(start, null));

        int bestDistance = Integer.MAX_VALUE;
        long bestHeuristic = Integer.MAX_VALUE;
        Instant cutoffTime = Instant.now().plus(config.getCalculationCutoff());

        while (!boundary.isEmpty() || !pending.isEmpty()) {
            Node node = boundary.peekFirst();
            Node p = pending.peek();

            if (p != null && (node == null || p.cost < node.cost)) {
                boundary.addFirst(p);
                pending.poll();
            }

            node = boundary.removeFirst();

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
        pending.clear();
    }
}
