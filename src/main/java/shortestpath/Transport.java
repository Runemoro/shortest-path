package shortestpath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import lombok.Getter;
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

    /** The agility level required to use this transport */
    @Getter
    private final int agilityLevelRequired;

    /** The ranged level required to use this transport */
    @Getter
    private final int rangedLevelRequired;

    /** The strength level required to use this transport */
    @Getter
    private final int strengthLevelRequired;

    /** Whether the transport is an agility shortcut */
    @Getter
    private final boolean isAgilityShortcut;

    /** Whether the transport is a crossbow grapple shortcut */
    @Getter
    private final boolean isGrappleShortcut;

    /** Whether the transport is a fairy ring */
    @Getter
    private final boolean isFairyRing;

    Transport(final WorldPoint origin, final WorldPoint destination,
              final int agilityLevelRequired, final int rangedLevelRequired, final int strengthLevelRequired,
              final boolean isFairyRing) {
        this.origin = origin;
        this.destination = destination;
        this.agilityLevelRequired = agilityLevelRequired;
        this.rangedLevelRequired = rangedLevelRequired;
        this.strengthLevelRequired = strengthLevelRequired;
        this.isAgilityShortcut = agilityLevelRequired > 1;
        this.isGrappleShortcut = isAgilityShortcut && (rangedLevelRequired > 1 || strengthLevelRequired > 1);
        this.isFairyRing = isFairyRing;
    }

    Transport(final WorldPoint origin, final WorldPoint destination, final boolean isFairyRing) {
        this(origin, destination, 0, 0, 0, isFairyRing);
    }

    Transport(final WorldPoint origin, final WorldPoint destination) {
        this(origin, destination, 0, 0, 0, false);
    }

    private static Transport fromLine(final String line) {
        final String DELIM = " ";

        String[] parts = line.split("\t");

        String[] parts_origin = parts[0].split(DELIM);
        String[] parts_destination = parts[1].split(DELIM);

        WorldPoint origin = new WorldPoint(
            Integer.parseInt(parts_origin[0]),
            Integer.parseInt(parts_origin[1]),
            Integer.parseInt(parts_origin[2]));
        WorldPoint destination = new WorldPoint(
            Integer.parseInt(parts_destination[0]),
            Integer.parseInt(parts_destination[1]),
            Integer.parseInt(parts_destination[2]));

        int agilityLevel = 0;
        int rangedLevel = 0;
        int strengthLevel = 0;

        if (parts.length >= 4 && !parts[3].startsWith("\"")) {
            String[] requirements = parts[3].split(";");

            if (requirements.length >= 1) {
                agilityLevel = Integer.parseInt(requirements[0].split(DELIM)[0]);
            }
            if (requirements.length >= 2) {
                rangedLevel = Integer.parseInt(requirements[1].split(DELIM)[0]);
            }
            if (requirements.length >= 3) {
                strengthLevel = Integer.parseInt(requirements[2].split(DELIM)[0]);
            }
        }

        return new Transport(origin, destination, agilityLevel, rangedLevel, strengthLevel, false);
    }

    public static HashMap<WorldPoint, List<Transport>> fromResources(ShortestPathConfig config) {
        HashMap<WorldPoint, List<Transport>> transports = new HashMap<>();

        try {
            String s = new String(Util.readAllBytes(ShortestPathPlugin.class.getResourceAsStream("/transports.txt")), StandardCharsets.UTF_8);
            Scanner scanner = new Scanner(s);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }

                Transport transport = Transport.fromLine(line);
                if (!config.useAgilityShortcuts() && transport.isAgilityShortcut) {
                    continue;
                }
                if (!config.useGrappleShortcuts() && transport.isGrappleShortcut) {
                    continue;
                }
                WorldPoint origin = transport.getOrigin();
                transports.computeIfAbsent(origin, k -> new ArrayList<>()).add(transport);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (config.useFairyRings()) {
            try {
                String s = new String(Util.readAllBytes(ShortestPathPlugin.class.getResourceAsStream("/fairy_rings.txt")), StandardCharsets.UTF_8);
                Scanner scanner = new Scanner(s);
                List<WorldPoint> fairyRings = new ArrayList<>();
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();

                    if (line.startsWith("#") || line.isEmpty()) {
                        continue;
                    }

                    String[] wp = line.split("\t");
                    fairyRings.add(new WorldPoint(Integer.parseInt(wp[0]), Integer.parseInt(wp[1]), Integer.parseInt(wp[2])));
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

        return transports;
    }
}
