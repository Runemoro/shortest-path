package shortestpath.pathfinder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import shortestpath.ShortestPathConfig;
import shortestpath.ShortestPathPlugin;
import shortestpath.Transport;
import shortestpath.WorldPointUtil;

public class PathfinderConfig {
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);

    private final SplitFlagMap mapData;
    private final ThreadLocal<CollisionMap> map;
    private final Map<WorldPoint, List<Transport>> allTransports;
    @Getter
    private Map<WorldPoint, List<Transport>> transports;

    // Copy of transports with packed positions for the hotpath; lists are not copied and are the same reference in both maps
    @Getter
    private Map<Integer, List<Transport>> transportsPacked;

    private final Client client;
    private final ShortestPathConfig config;
    private final ShortestPathPlugin plugin;

    @Getter
    private Duration calculationCutoff;
    @Getter
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
    private int recalculateDistance;
    private Map<Quest, QuestState> questStates = new HashMap<>();

    public PathfinderConfig(SplitFlagMap mapData, Map<WorldPoint, List<Transport>> transports, Client client,
                            ShortestPathConfig config, ShortestPathPlugin plugin) {
        this.mapData = mapData;
        this.map = ThreadLocal.withInitial(() -> new CollisionMap(this.mapData));
        this.allTransports = transports;
        this.transports = new HashMap<>();
        this.transportsPacked = new HashMap<>();
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        refresh();
    }

    public CollisionMap getMap() {
        return map.get();
    }

    public void refresh() {
        calculationCutoff = Duration.ofMillis(config.calculationCutoff() * Constants.GAME_TICK_LENGTH);
        recalculateDistance = config.recalculateDistance();
        avoidWilderness = config.avoidWilderness();
        useAgilityShortcuts = config.useAgilityShortcuts();
        useGrappleShortcuts = config.useGrappleShortcuts();
        useBoats = config.useBoats();
        useFairyRings = config.useFairyRings();
        useTeleports = config.useTeleports();

        if (GameState.LOGGED_IN.equals(client.getGameState())) {
            agilityLevel = client.getBoostedSkillLevel(Skill.AGILITY);
            rangedLevel = client.getBoostedSkillLevel(Skill.RANGED);
            strengthLevel = client.getBoostedSkillLevel(Skill.STRENGTH);
            prayerLevel = client.getBoostedSkillLevel(Skill.PRAYER);
            woodcuttingLevel = client.getBoostedSkillLevel(Skill.WOODCUTTING);

            refreshTransportData();
        }
    }

    private void refreshTransportData() {
        if (!Thread.currentThread().equals(client.getClientThread())) {
            return; // Has to run on the client thread; data will be refreshed when path finding commences
        }
        useFairyRings &= !QuestState.NOT_STARTED.equals(Quest.FAIRYTALE_II__CURE_A_QUEEN.getState(client));

        transports.clear();
        transportsPacked.clear();
        for (Map.Entry<WorldPoint, List<Transport>> entry : allTransports.entrySet()) {
            List<Transport> usableTransports = new ArrayList<>(entry.getValue().size());
            for (Transport transport : entry.getValue()) {
                if (transport.isQuestLocked()) {
                    try {
                        questStates.put(transport.getQuest(), transport.getQuest().getState(client));
                    } catch (NullPointerException ignored) {
                    }
                }

                if (useTransport(transport)) {
                    usableTransports.add(transport);
                }
            }

            WorldPoint point = entry.getKey();
            transports.put(point, usableTransports);
            transportsPacked.put(WorldPointUtil.packWorldPoint(point), usableTransports);
        }
    }

    public static boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 || WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    public static boolean isInWilderness(int packedPoint) {
        return WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_ABOVE_GROUND) == 0 || WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_UNDERGROUND) == 0;
    }

    public boolean avoidWilderness(WorldPoint position, WorldPoint neighbor, boolean targetInWilderness) {
        return avoidWilderness && !isInWilderness(position) && isInWilderness(neighbor) && !targetInWilderness;
    }

    public boolean avoidWilderness(int packedPosition, int packedNeightborPosition, boolean targetInWilderness) {
        return avoidWilderness && !isInWilderness(packedPosition) && isInWilderness(packedNeightborPosition) && !targetInWilderness;
    }

    public boolean isNear(WorldPoint location) {
        if (plugin.isStartPointSet() || client.getLocalPlayer() == null) {
            return true;
        }
        return recalculateDistance < 0 ||
            (client.isInInstancedRegion() ?
                WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()) :
                client.getLocalPlayer().getWorldLocation()).distanceTo2D(location) <= recalculateDistance;
    }

    private boolean useTransport(Transport transport) {
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
        final boolean isQuestLocked = transport.isQuestLocked();

        if (isAgilityShortcut) {
            if (!useAgilityShortcuts || agilityLevel < transportAgilityLevel) {
                return false;
            }

            if (isGrappleShortcut && (!useGrappleShortcuts || rangedLevel < transportRangedLevel || strengthLevel < transportStrengthLevel)) {
                return false;
            }
        }

        if (isBoat) {
            if (!useBoats) {
                return false;
            }

            if (isCanoe && woodcuttingLevel < transportWoodcuttingLevel) {
                return false;
            }
        }

        if (isFairyRing && !useFairyRings) {
            return false;
        }

        if (isTeleport && !useTeleports) {
            return false;
        }

        if (isPrayerLocked && prayerLevel < transportPrayerLevel) {
            return false;
        }

        if (isQuestLocked && !QuestState.FINISHED.equals(questStates.getOrDefault(transport.getQuest(), QuestState.NOT_STARTED))) {
            return false;
        }

        return true;
    }
}
