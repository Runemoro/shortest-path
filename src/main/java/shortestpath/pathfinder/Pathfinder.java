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

    private void addNeighbor(Node node, WorldPoint neighbor) {
        if (config.avoidWilderness(node.position, neighbor, target)) {
            return;
        }
        if (!visited.add(neighbor)) {
            return;
        }
        boundary.add(new Node(neighbor, node));
    }

    private void addNeighbors(Node node) {
        for (OrdinalDirection direction : OrdinalDirection.values()) {
            for (Transport transport : config.getTransports().getOrDefault(node.position.dx(direction.x).dy(direction.y), new ArrayList<>())) {
                if (config.useTransport(transport)) {
                    addNeighbor(new Node(transport.getOrigin(), node), transport.getDestination());
                }
            }
        }

        for (WorldPoint neighbor : config.getMap().getNeighbors(node.position)) {
            addNeighbor(node, neighbor);
        }

        for (Transport transport : config.getTransports().getOrDefault(node.position, new ArrayList<>())) {
            if (config.useTransport(transport)) {
                addNeighbor(node, transport.getDestination());
            }
        }
    }

    @Override
    public void run() {
        boundary.add(new Node(start, null));

        Node nearest = boundary.get(0);
        int bestDistance = Integer.MAX_VALUE;
        Instant cutoffTime = Instant.now().plus(PathfinderConfig.CALCULATION_CUTOFF);

        while (!boundary.isEmpty()) {
            Node node = boundary.remove(0);

            if (node.position.equals(target) || !config.isNear(start)) {
                path = node.getPath();
                break;
            }

            int distance = node.position.distanceTo(target);
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
    }
}
