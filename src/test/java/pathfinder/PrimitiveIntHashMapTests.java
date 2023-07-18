package pathfinder;

import net.runelite.api.coords.WorldPoint;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.Transport;
import shortestpath.WorldPointUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrimitiveIntHashMapTests {
    public static void main(String[] args) {
        HashMap<WorldPoint, List<Transport>> transports = Transport.loadAllFromResources();
        PrimitiveIntHashMap<List<Transport>> map = new PrimitiveIntHashMap<>(transports.size());
        for (Map.Entry<WorldPoint, List<Transport>> entry : transports.entrySet()) {
            int packedPoint = WorldPointUtil.packWorldPoint(entry.getKey());
            map.put(packedPoint, entry.getValue());
        }

        for (Map.Entry<WorldPoint, List<Transport>> entry : transports.entrySet()) {
            int packedPoint = WorldPointUtil.packWorldPoint(entry.getKey());
            if (map.get(packedPoint) != entry.getValue()) {
                System.err.println("Transport entry: " + entry.getKey() + " was not found in map");
            }
            assert map.get(packedPoint) == entry.getValue();
        }
    }
}
