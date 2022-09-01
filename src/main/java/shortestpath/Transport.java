package shortestpath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * This class represents a travel point between two WorldPoints.
 */
public class Transport {
    /** The starting point of this transport */
    @Getter
    private final WorldPoint origin;

    /** The ending point of this transport */
    @Getter
    private final WorldPoint destination;

    /** The skill levels required to use this transport */
    private final int[] skillLevels = new int[Skill.values().length];

    /** The quest required to use this transport */
    @Getter
    private Quest quest;

    /** Whether the transport is an agility shortcut */
    @Getter
    private boolean isAgilityShortcut;

    /** Whether the transport is a crossbow grapple shortcut */
    @Getter
    private boolean isGrappleShortcut;

    /** Whether the transport is a boat */
    @Getter
    private boolean isBoat;

    /** Whether the transport is a fairy ring */
    @Getter
    private boolean isFairyRing;

    /** Whether the transport is a teleport */
    @Getter
    private boolean isTeleport;

    Transport(final WorldPoint origin, final WorldPoint destination) {
        this.origin = origin;
        this.destination = destination;
    }

    Transport(final WorldPoint origin, final WorldPoint destination, final boolean isFairyRing) {
        this(origin, destination);
        this.isFairyRing = isFairyRing;
    }

    Transport(final String line) {
        final String DELIM = " ";

        String[] parts = line.split("\t");

        String[] parts_origin = parts[0].split(DELIM);
        String[] parts_destination = parts[1].split(DELIM);

        origin = new WorldPoint(
            Integer.parseInt(parts_origin[0]),
            Integer.parseInt(parts_origin[1]),
            Integer.parseInt(parts_origin[2]));
        destination = new WorldPoint(
            Integer.parseInt(parts_destination[0]),
            Integer.parseInt(parts_destination[1]),
            Integer.parseInt(parts_destination[2]));

        if (parts.length >= 4 && !parts[3].isEmpty() && !parts[3].startsWith("\"")) {
            String[] skillRequirements = parts[3].split(";");

            for (String requirement : skillRequirements) {
                String[] levelAndSkill = requirement.split(DELIM);

                int level = Integer.parseInt(levelAndSkill[0]);
                String skillName = levelAndSkill[1];

                Skill[] skills = Skill.values();
                for (int i = 0; i < skills.length; i++) {
                    if (skills[i].getName().equals(skillName)) {
                        skillLevels[i] = level;
                        break;
                    }
                }
            }
        }
        if (parts.length >= 6 && !parts[5].isEmpty() && !parts[5].startsWith("\"")) {
            String questName = parts[5];

            for (Quest quest : Quest.values()) {
                if (quest.getName().equals(questName)) {
                    this.quest = quest;
                    break;
                }
            }
        }

        isAgilityShortcut = getRequiredLevel(Skill.AGILITY) > 1;
        isGrappleShortcut = isAgilityShortcut && (getRequiredLevel(Skill.RANGED) > 1 || getRequiredLevel(Skill.STRENGTH) > 1);
    }

    /** The skill level required to use this transport */
    public int getRequiredLevel(Skill skill) {
        return skillLevels[skill.ordinal()];
    }

    /** Whether the transport has a quest requirement */
    public boolean isQuestLocked() {
        return quest != null;
    }

    private static void addTransports(Map<WorldPoint, List<Transport>> transports, ShortestPathConfig config, String path, TransportType transportType) {
        try {
            String s = new String(Util.readAllBytes(ShortestPathPlugin.class.getResourceAsStream(path)), StandardCharsets.UTF_8);
            Scanner scanner = new Scanner(s);
            List<WorldPoint> fairyRings = new ArrayList<>();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }

                if (TransportType.FAIRY_RING.equals(transportType)) {
                    String[] wp = line.split("\t");
                    fairyRings.add(new WorldPoint(Integer.parseInt(wp[0]), Integer.parseInt(wp[1]), Integer.parseInt(wp[2])));
                } else {
                    Transport transport = new Transport(line);
                    transport.isBoat = TransportType.BOAT.equals(transportType);
                    transport.isTeleport = TransportType.TELEPORT.equals(transportType);
                    if (!config.useAgilityShortcuts() && transport.isAgilityShortcut) {
                        continue;
                    }
                    if (!config.useGrappleShortcuts() && transport.isGrappleShortcut) {
                        continue;
                    }
                    WorldPoint origin = transport.getOrigin();
                    transports.computeIfAbsent(origin, k -> new ArrayList<>()).add(transport);
                }
            }
            for (WorldPoint origin : fairyRings) {
                for (WorldPoint destination : fairyRings) {
                    if (origin.equals(destination)) {
                        continue;
                    }
                    transports.computeIfAbsent(origin.dx(-1), k -> new ArrayList<>()).add(new Transport(origin.dx(-1), destination, true));
                    transports.computeIfAbsent(origin.dx(1), k -> new ArrayList<>()).add(new Transport(origin.dx(1), destination, true));
                    transports.computeIfAbsent(origin.dy(-1), k -> new ArrayList<>()).add(new Transport(origin.dy(-1), destination, true));
                    transports.computeIfAbsent(origin.dy(1), k -> new ArrayList<>()).add(new Transport(origin.dy(1), destination, true));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashMap<WorldPoint, List<Transport>> fromResources(ShortestPathConfig config) {
        HashMap<WorldPoint, List<Transport>> transports = new HashMap<>();

        addTransports(transports, config, "/transports.txt", TransportType.TRANSPORT);

        if (config.useBoats()) {
            addTransports(transports, config, "/boats.txt", TransportType.BOAT);
        }

        if (config.useFairyRings()) {
            addTransports(transports, config, "/fairy_rings.txt", TransportType.FAIRY_RING);
        }

        if (config.useTeleports()) {
            addTransports(transports, config, "/teleports.txt", TransportType.TELEPORT);
        }

        return transports;
    }

    private enum TransportType {
        TRANSPORT,
        BOAT,
        FAIRY_RING,
        TELEPORT
    }
}
