package shortestpath.pathfinder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import shortestpath.WorldPointUtil;

public class Node {
    public final int packedPosition;
    public final Node previous;
    public final int cost;

    public Node(WorldPoint position, Node previous, int wait) {
        this.packedPosition = WorldPointUtil.packWorldPoint(position);
        this.previous = previous;
        this.cost = cost(previous, wait);
    }

    public Node(WorldPoint position, Node previous) {
        this(position, previous, 0);
    }

    public Node(int packedPosition, Node previous, int wait) {
        this.packedPosition = packedPosition;
        this.previous = previous;
        this.cost = cost(previous, wait);
    }

    public Node(int packedPosition, Node previous) {
        this(packedPosition, previous, 0);
    }

    public List<WorldPoint> getPath() {
        List<WorldPoint> path = new LinkedList<>();
        Node node = this;

        while (node != null) {
            WorldPoint position = WorldPointUtil.unpackWorldPoint(node.packedPosition);
            path.add(0, position);
            node = node.previous;
        }

        return new ArrayList<>(path);
    }

    public List<Integer> getPathPacked() {
        List<Integer> path = new LinkedList<>();
        Node node = this;

        while (node != null) {
            path.add(0, node.packedPosition);
            node = node.previous;
        }

        return new ArrayList<>(path);
    }

    private int cost(Node previous, int wait) {
        int previousCost = 0;
        int distance = 0;

        if (previous != null) {
            previousCost = previous.cost;
            distance = WorldPointUtil.distanceBetween(previous.packedPosition, packedPosition);
            final int previousPlane = WorldPointUtil.unpackWorldPlane(previous.packedPosition);
            final int currentPlane = WorldPointUtil.unpackWorldPlane(previous.packedPosition);
            boolean isTransport = distance > 1 || previousPlane != currentPlane;
            if (isTransport) {
                distance = wait;
            }
        }

        return previousCost + distance;
    }
}
