package shortestpath;
import net.runelite.api.coords.WorldPoint;


/**
 * This class represents a travel point between two WorldPoints. 
 */
public class Transport {
    /** The starting point of this transport */
    private final WorldPoint origin;
    /** The ending point of this transport */
    private final WorldPoint destination;
    /** The agility level required to use transport */
    private final int agilityLevelRequired;

    Transport(final String line) {
        String[] spaceSplit = line.split(" ");

        origin = new WorldPoint(Integer.parseInt(spaceSplit[0]), Integer.parseInt(spaceSplit[1]), Integer.parseInt(spaceSplit[2]));
        destination = new WorldPoint(Integer.parseInt(spaceSplit[3]), Integer.parseInt(spaceSplit[4]), Integer.parseInt(spaceSplit[5]));

        int agilityLevel = 1;

        if (line.contains("Agility")) {
            // Agility comments are in this for "NN Agility, <etc>" where NN is the level required
            String[] quoteSplit = line.split("\"");

            assert(quoteSplit.length == 2);

            // There is only one comment per line, always at the end
            String[] splitComment = quoteSplit[1].replace("\"", "").split(" ");
            String level = splitComment[0];

            try {
                agilityLevel = Integer.parseInt(level);
            } catch (Exception e) {
                System.out.println("e: " + e.getMessage());
            }
        }

        agilityLevelRequired = agilityLevel;
    }

    Transport(final WorldPoint origin, final WorldPoint destination, int agilityLevelRequired) {
        this.origin = origin;
        this.destination = destination;
        this.agilityLevelRequired = agilityLevelRequired;
    }

    Transport(final WorldPoint origin, final WorldPoint destination) {
        this(origin, destination, 0);
    }

    public WorldPoint getOrigin() {
        return origin;
    }

    public WorldPoint getDestination() {
        return destination;
    }

    public int getAgilityLevelRequired() {
        return agilityLevelRequired;
    }
}