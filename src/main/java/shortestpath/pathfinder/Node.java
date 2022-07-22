package shortestpath.pathfinder;

import net.runelite.api.coords.WorldPoint;
import shortestpath.Path;

import java.util.LinkedList;

public class Node {
    public final WorldPoint position;
    public final Node previous;

    public Node(WorldPoint position, Node previous) {
        this.position = position;
        this.previous = previous;
    }

    public Path getPath() {
        Path path = new Path(new LinkedList<>());

        Node nodeIterator = this;
        while (nodeIterator != null) {
            path.getPoints().add(0, nodeIterator.position);
            nodeIterator = nodeIterator.previous;
        }

        return path;
    }
}
