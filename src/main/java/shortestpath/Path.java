package shortestpath;

import java.util.List;
import net.runelite.api.coords.WorldPoint;

public class Path {
    public final List<WorldPoint> points;
    public int visitedIndex;

    public Path(List<WorldPoint> points) {
        this.points = points;
        this.visitedIndex = -1;
    }

    public Path(List<WorldPoint> points, int visitedIndex) {
        this.points = points;
        this.visitedIndex = visitedIndex;
    }
}
