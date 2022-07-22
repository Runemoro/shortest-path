package shortestpath;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

public class Path {
    @Getter
    private final List<WorldPoint> points;
    @Getter
    @Setter
    private int visitedIndex;

    public Path(List<WorldPoint> points) {
        this.points = points;
        this.visitedIndex = -1;
    }

    public Path(List<WorldPoint> points, int visitedIndex) {
        this.points = points;
        this.visitedIndex = visitedIndex;
    }
}
