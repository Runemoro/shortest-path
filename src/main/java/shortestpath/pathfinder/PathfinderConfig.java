package shortestpath.pathfinder;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import shortestpath.ShortestPathConfig;
import shortestpath.Transport;

public class PathfinderConfig {
    public static final Duration CALCULATION_CUTOFF = Duration.ofSeconds(2);
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);

    @Getter
    private final CollisionMap map;
    @Getter
    private final Map<WorldPoint, List<Transport>> transports;
    private final Client client;
    private final ShortestPathConfig config;

    private boolean avoidWilderness;
    private boolean useAgilityShortcuts;
    private boolean useGrappleShortcuts;
    private int agilityLevel;
    private int rangedLevel;
    private int strengthLevel;

    public PathfinderConfig(CollisionMap map, Map<WorldPoint, List<Transport>> transports, Client client, ShortestPathConfig config) {
        this.map = map;
        this.transports = transports;
        this.client = client;
        this.config = config;
        refresh();
    }

    public void refresh() {
        avoidWilderness = config.avoidWilderness();
        useAgilityShortcuts = config.useAgilityShortcuts();
        useGrappleShortcuts = config.useGrappleShortcuts();
        agilityLevel = client.getBoostedSkillLevel(Skill.AGILITY);
        rangedLevel = client.getBoostedSkillLevel(Skill.RANGED);
        strengthLevel = client.getBoostedSkillLevel(Skill.STRENGTH);
    }

    private boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 || WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    public boolean avoidWilderness(WorldPoint position, WorldPoint neighbor, WorldPoint target) {
        return avoidWilderness && !isInWilderness(position) && isInWilderness(neighbor) && !isInWilderness(target);
    }

    public boolean useTransport(Transport transport) {
        final int transportAgilityLevel = transport.getAgilityLevelRequired();
        final int transportRangedLevel = transport.getRangedLevelRequired();
        final int transportStrengthLevel = transport.getStrengthLevelRequired();

        final boolean isAgilityShortcut = transportAgilityLevel > 1;
        final boolean isGrappleShortcut = isAgilityShortcut && (transportRangedLevel > 1 || transportStrengthLevel > 1);

        if (!isAgilityShortcut) {
            return true;
        }

        if (!useAgilityShortcuts) {
            return false;
        }

        if (!useGrappleShortcuts && isGrappleShortcut) {
            return false;
        }

        if (useGrappleShortcuts && isGrappleShortcut && agilityLevel >= transportAgilityLevel &&
                rangedLevel >= transportRangedLevel && strengthLevel >= transportStrengthLevel) {
            return true;
        }

        return agilityLevel >= transportAgilityLevel;
    }
}
