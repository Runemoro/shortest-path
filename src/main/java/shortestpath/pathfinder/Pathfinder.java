package shortestpath.pathfinder;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import shortestpath.WorldPointUtil;

public class Pathfinder implements Runnable {
    private AtomicBoolean done = new AtomicBoolean();
    private AtomicBoolean cancelled = new AtomicBoolean();

    @Getter
    private final WorldPoint start;
    @Getter
    private final WorldPoint target;

    private final int startPacked;
    private final int targetPacked;

    private final PathfinderConfig config;
    private final CollisionMap map;
    private final boolean targetInWilderness;

    // Capacities should be enough to store all nodes without requiring the queue to grow
    // They were found by checking the max queue size
    private final Deque<Node> boundary = new ArrayDeque<>(4096);
    private final Queue<Node> pending = new PriorityQueue<>(256);
    private final VisitedTiles visited = new VisitedTiles();

    @SuppressWarnings("unchecked") // Casting EMPTY_LIST is safe here
    private List<WorldPoint> path = (List<WorldPoint>)Collections.EMPTY_LIST;
    private boolean pathNeedsUpdate = false;
    private Node bestLastNode;

    public Pathfinder(PathfinderConfig config, WorldPoint start, WorldPoint target) {
        this.config = config;
        this.map = config.getMap();
        this.start = start;
        this.target = target;
        startPacked = WorldPointUtil.packWorldPoint(start);
        targetPacked = WorldPointUtil.packWorldPoint(target);
        targetInWilderness = PathfinderConfig.isInWilderness(target);
        
        new Thread(this).start();
    }

    public boolean isDone() {
        return done.get();
    }

    public void cancel() {
        cancelled.set(true);
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

    private void addNeighbors(Node node) {
        List<Node> nodes = map.getNeighbors(node, config);
        for (int i = 0; i < nodes.size(); ++i) {
            Node neighbor = nodes.get(i);
            if (visited.get(neighbor.packedPosition) || (config.isAvoidWilderness() && config.avoidWilderness(node.packedPosition, neighbor.packedPosition, targetInWilderness))) {
                continue;
            }
            if (visited.set(neighbor.packedPosition)) {
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
        long cutoffDurationMillis = config.getCalculationCutoff().toMillis();
        long cutoffTimeMillis = System.currentTimeMillis() + cutoffDurationMillis;

        while (!cancelled.get() && (!boundary.isEmpty() || !pending.isEmpty())) {
            Node node = boundary.peekFirst();
            Node p = pending.peek();

            if (p != null && (node == null || p.cost < node.cost)) {
                boundary.addFirst(p);
                pending.poll();
            }

            node = boundary.removeFirst();

            if (node.packedPosition == targetPacked || !config.isNear(start)) {
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

            addNeighbors(node);
        }

        done.set(!cancelled.get());

        boundary.clear();
        visited.clear();
        pending.clear();
    }
}
