package shortestpath.pathfinder;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import shortestpath.WorldPointUtil;

public class Pathfinder implements Runnable {
    private PathfinderStats stats;
    private volatile boolean done = false;
    private volatile boolean cancelled = false;

    @Getter
    private final WorldPoint start;
    @Getter
    private final WorldPoint target;

    private final int targetPacked;

    private final PathfinderConfig config;
    private final CollisionMap map;
    private final boolean targetInWilderness;

    // Capacities should be enough to store all nodes without requiring the queue to grow
    // They were found by checking the max queue size
    private final Deque<Node> boundary = new ArrayDeque<>(4096);
    private final Queue<Node> pending = new PriorityQueue<>(256);
    private final VisitedTiles visited;

    @SuppressWarnings("unchecked") // Casting EMPTY_LIST is safe here
    private List<WorldPoint> path = (List<WorldPoint>)Collections.EMPTY_LIST;
    private boolean pathNeedsUpdate = false;
    private Node bestLastNode;

    public Pathfinder(PathfinderConfig config, WorldPoint start, WorldPoint target) {
        stats = new PathfinderStats();
        this.config = config;
        this.map = config.getMap();
        this.start = start;
        this.target = target;
        visited = new VisitedTiles(map);
        targetPacked = WorldPointUtil.packWorldPoint(target);
        targetInWilderness = PathfinderConfig.isInWilderness(target);
    }

    public boolean isDone() {
        return done;
    }

    public void cancel() {
        cancelled = true;
    }

    public PathfinderStats getStats() {
        if (stats.started && stats.ended) {
            return stats;
        }

        // Don't give incomplete results
        return null;
    }

    public List<WorldPoint> getPath() {
        Node lastNode = bestLastNode; // For thread safety, read bestLastNode once
        if (lastNode == null) {
            return path;
        }

        if (pathNeedsUpdate) {
            path = lastNode.getPath();
            pathNeedsUpdate = false;
        }

        return path;
    }

    private Node addNeighbors(Node node) {
        List<Node> nodes = map.getNeighbors(node, visited, config);
        for (int i = 0; i < nodes.size(); ++i) {
            Node neighbor = nodes.get(i);
            if (neighbor.packedPosition == targetPacked) {
                return neighbor;
            }

            if (config.isAvoidWilderness() && config.avoidWilderness(node.packedPosition, neighbor.packedPosition, targetInWilderness)) {
                continue;
            }

            visited.set(neighbor.packedPosition);
            if (neighbor instanceof TransportNode) {
                pending.add(neighbor);
                ++stats.transportsChecked;
            } else {
                boundary.addLast(neighbor);
                ++stats.nodesChecked;
            }
        }

        return null;
    }

    @Override
    public void run() {
        stats.start();
        boundary.addFirst(new Node(start, null));

        int bestDistance = Integer.MAX_VALUE;
        long bestHeuristic = Integer.MAX_VALUE;
        long cutoffDurationMillis = config.getCalculationCutoffMillis();
        long cutoffTimeMillis = System.currentTimeMillis() + cutoffDurationMillis;

        while (!cancelled && (!boundary.isEmpty() || !pending.isEmpty())) {
            Node node = boundary.peekFirst();
            Node p = pending.peek();

            if (p != null && (node == null || p.cost < node.cost)) {
                boundary.addFirst(p);
                pending.poll();
            }

            node = boundary.removeFirst();

            if (node.packedPosition == targetPacked) {
                bestLastNode = node;
                pathNeedsUpdate = true;
                break;
            }

            int distance = WorldPointUtil.distanceBetween(node.packedPosition, targetPacked);
            long heuristic = distance + WorldPointUtil.distanceBetween(node.packedPosition, targetPacked, 2);
            if (heuristic < bestHeuristic || (heuristic <= bestHeuristic && distance < bestDistance)) {
                bestLastNode = node;
                pathNeedsUpdate = true;
                bestDistance = distance;
                bestHeuristic = heuristic;
                cutoffTimeMillis = System.currentTimeMillis() + cutoffDurationMillis;
            }

            if (System.currentTimeMillis() > cutoffTimeMillis) {
                break;
            }

            // Check if target was found without processing the queue to find it
            if ((p = addNeighbors(node)) != null) {
                bestLastNode = p;
                pathNeedsUpdate = true;
                break;
            }
        }

        done = !cancelled;

        boundary.clear();
        visited.clear();
        pending.clear();

        stats.end(); // Include cleanup in stats to get the total cost of pathfinding
    }

    public static class PathfinderStats {
        @Getter
        private int nodesChecked = 0, transportsChecked = 0;
        private long startNanos, endNanos;
        private volatile boolean started = false, ended = false;

        public int getTotalNodesChecked() {
            return nodesChecked + transportsChecked;
        }

        public long getElapsedTimeNanos() {
            return endNanos - startNanos;
        }

        private void start() {
            started = true;
            nodesChecked = 0;
            transportsChecked = 0;
            startNanos = System.nanoTime();
        }

        private void end() {
            endNanos = System.nanoTime();
            ended = true;
        }
    }
}
