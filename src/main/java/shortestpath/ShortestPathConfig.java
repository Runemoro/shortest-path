package shortestpath;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("shortestPath")
public interface ShortestPathConfig extends Config {
    @ConfigItem(keyName = "drawTiles", name = "Draw path on tiles", description = "Whether the path should be drawn on the game tiles")
    default boolean drawTiles() {
        return true;
    }

    @ConfigItem(keyName = "drawTileNumbers", name = "Draw tile numbers on tiles", description = "Whether the tile number should be drawn on the game tiles")
    default boolean drawTileNumbers() {
        return true;
    }

    @ConfigItem(keyName = "drawMinimap", name = "Draw path on minimap", description = "Whether the path should be drawn on the minimap")
    default boolean drawMinimap() {
        return true;
    }

    @ConfigItem(keyName = "drawMap", name = "Draw path on map", description = "Whether the path should be drawn on the world map")
    default boolean drawMap() {
        return true;
    }

    @ConfigItem(keyName = "drawCollisionMap", name = "Draw collision map", description = "Whether the collision map should be drawn")
    default boolean drawCollisionMap() {
        return false;
    }

    @ConfigItem(keyName = "drawTransports", name = "Draw transports", description = "Whether transports should be drawn")
    default boolean drawTransports() {
        return false;
    }

    @ConfigItem(keyName = "recalculateDistance", name = "Recalculate distance", description = "Distance from the path the player should be for it to be recalculated")
    @Range(min = 1, max = 1000)
    default int recalculateDistance() {
        return 10;
    }

    @ConfigItem(keyName = "cancelInstead", name = "Cancel instead of recalculating", description = "Whether the path should be cancelled rather than recalculated when the distance limit is exceeded")
    default boolean cancelInstead() {
        return false;
    }

    @ConfigItem(keyName = "finishDistance", name = "Finish distance", description = "Distance from the target tile at which the path should be ended (-1 for never)")
    @Range(min = -1, max = 50)
    default int reachedDistance() {
        return 5;
    }

    @ConfigItem(keyName = "avoidWilderness", name = "Avoid wilderness", description = "Whether the wilderness should be avoided if possible (otherwise, will suggest using wilderness lever to travel from Edgeville to Ardougne)")
    default boolean avoidWilderness() {
        return true;
    }
}
