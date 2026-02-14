package pathfinder;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import shortestpath.ShortestPathPlugin;

import static org.junit.Assert.*;

public class HazardAvoidanceTest {
    @Test
    public void stormySeas() {
        // Center of Stormy Seas region (2750, 2820, 320, 200)
        assertTrue(ShortestPathPlugin.isInSailingHazard(new WorldPoint(2900, 2900, 0)));
    }

    @Test
    public void fetidWaters() {
        // Center of Fetid Waters region (2080, 2700, 480, 400)
        assertTrue(ShortestPathPlugin.isInSailingHazard(new WorldPoint(2300, 2900, 0)));
    }

    @Test
    public void icySeas() {
        // Center of Icy Seas region (1750, 3750, 1250, 350)
        assertTrue(ShortestPathPlugin.isInSailingHazard(new WorldPoint(2375, 3900, 0)));
    }

    @Test
    public void safeWaters() {
        // Tile well outside all hazard zones (near Lumbridge)
        assertFalse(ShortestPathPlugin.isInSailingHazard(new WorldPoint(3222, 3218, 0)));
    }

    @Test
    public void hazardEdge() {
        // Just inside Stormy Seas SW corner
        assertTrue(ShortestPathPlugin.isInSailingHazard(new WorldPoint(2750, 2820, 0)));
        // One tile outside
        assertFalse(ShortestPathPlugin.isInSailingHazard(new WorldPoint(2749, 2820, 0)));
    }

    @Test
    public void wildernessUnchanged() {
        // Verify wilderness check still works independently
        assertTrue(ShortestPathPlugin.isInWilderness(new WorldPoint(3100, 3600, 0)));
        assertFalse(ShortestPathPlugin.isInWilderness(new WorldPoint(3222, 3218, 0)));
    }
}
