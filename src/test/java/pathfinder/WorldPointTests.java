package pathfinder;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import shortestpath.WorldPointUtil;

import java.util.ArrayList;
import java.util.List;

public class WorldPointTests {
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);

    public static void main(String[] args) {
        List<WorldPoint> testPoints = new ArrayList<>(10);
        testPoints.add(new WorldPoint(2900, 3500, 0));
        testPoints.add(new WorldPoint(3000, 3500, 0));
        testPoints.add(new WorldPoint(3600, 3500, 0));
        testPoints.add(new WorldPoint(2900, 3622, 0));
        testPoints.add(new WorldPoint(3000, 3622, 0));
        testPoints.add(new WorldPoint(3600, 3622, 0));
        testPoints.add(new WorldPoint(2900, 4300, 0));
        testPoints.add(new WorldPoint(3000, 4300, 0));
        testPoints.add(new WorldPoint(3600, 4300, 0));
        testPoints.add(new WorldPoint(3600, 4200, 1));

        for (WorldPoint point : testPoints) {
            final int areaDistance = WILDERNESS_ABOVE_GROUND.distanceTo(point);
            final int packedPoint = WorldPointUtil.packWorldPoint(point);
            final int worldUtilDistance = WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_ABOVE_GROUND);
            if (areaDistance == worldUtilDistance) {
                System.out.println(areaDistance + " == " + worldUtilDistance);
            } else {
                System.out.println(String.format("Error: %d != %d; Point: [%d,%d,%d]", areaDistance, worldUtilDistance, point.getX(), point.getY(), point.getPlane()));
            }
        }
    }
}
