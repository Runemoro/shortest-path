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
import shortestpath.ShortestPathPlugin;
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
    private final ShortestPathPlugin plugin;

    private boolean avoidWilderness;
    private boolean useAgilityShortcuts;
    private boolean useGrappleShortcuts;
    private boolean useBoats;
    private boolean useFairyRings;
    private boolean useTeleports;
    private int agilityLevel;
    private int rangedLevel;
    private int strengthLevel;
    private int prayerLevel;
    private int woodcuttingLevel;

    public PathfinderConfig(CollisionMap map, Map<WorldPoint, List<Transport>> transports, Client client,
                            ShortestPathConfig config, ShortestPathPlugin plugin) {
        this.map = map;
        this.transports = transports;
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        refresh();
    }

    public void refresh() {
        avoidWilderness = config.avoidWilderness();
        useAgilityShortcuts = config.useAgilityShortcuts();
        useGrappleShortcuts = config.useGrappleShortcuts();
        useBoats = config.useBoats();
        useFairyRings = config.useFairyRings();
        useTeleports = config.useTeleports();
        agilityLevel = client.getBoostedSkillLevel(Skill.AGILITY);
        rangedLevel = client.getBoostedSkillLevel(Skill.RANGED);
        strengthLevel = client.getBoostedSkillLevel(Skill.STRENGTH);
        prayerLevel = client.getBoostedSkillLevel(Skill.PRAYER);
        woodcuttingLevel = client.getBoostedSkillLevel(Skill.WOODCUTTING);
    }

    private boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 || WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    public boolean avoidWilderness(WorldPoint position, WorldPoint neighbor, WorldPoint target) {
        return avoidWilderness && !isInWilderness(position) && isInWilderness(neighbor) && !isInWilderness(target);
    }

    public boolean isNear(WorldPoint location) {
        if (plugin.isStartPointSet() || client.getLocalPlayer() == null) {
            return true;
        }
        return config.recalculateDistance() < 0 ||
               client.getLocalPlayer().getWorldLocation().distanceTo2D(location) <= config.recalculateDistance();
    }

    public boolean useTransport(Transport transport) {
        final int transportAgilityLevel = transport.getRequiredLevel(Skill.AGILITY);
        final int transportRangedLevel = transport.getRequiredLevel(Skill.RANGED);
        final int transportStrengthLevel = transport.getRequiredLevel(Skill.STRENGTH);
        final int transportPrayerLevel = transport.getRequiredLevel(Skill.PRAYER);
        final int transportWoodcuttingLevel = transport.getRequiredLevel(Skill.WOODCUTTING);

        final boolean isAgilityShortcut = transport.isAgilityShortcut();
        final boolean isGrappleShortcut = transport.isGrappleShortcut();
        final boolean isBoat = transport.isBoat();
        final boolean isFairyRing = transport.isFairyRing();
        final boolean isTeleport = transport.isTeleport();
        final boolean isCanoe = isBoat && transportWoodcuttingLevel > 1;
        final boolean isPrayerLocked = transportPrayerLevel > 1;

        if (isAgilityShortcut) {
            if (!useAgilityShortcuts || agilityLevel < transportAgilityLevel) {
                return false;
            }

            if (isGrappleShortcut) {
                return useGrappleShortcuts && rangedLevel >= transportRangedLevel && strengthLevel >= transportStrengthLevel;
            }

            return true;
        }

        if (isBoat) {
            if (isCanoe) {
                return useBoats && woodcuttingLevel >= transportWoodcuttingLevel;
            }
            return useBoats;
        }

        if (isFairyRing) {
            return useFairyRings;
        }

        if (isTeleport) {
            return useTeleports;
        }

        if (isPrayerLocked && prayerLevel < transportPrayerLevel) {
            return false;
        }

        return true;
    }
}
