package pathfinder;

import net.runelite.api.coords.WorldPoint;
import org.junit.Assert;
import org.junit.Test;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.Transport;
import shortestpath.WorldPointUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrimitiveIntHashMapTests {
    @Test(expected=IllegalArgumentException.class)
    public void checkNullValueProhibited() {
        PrimitiveIntHashMap<Boolean> map = new PrimitiveIntHashMap<>(8);
        map.put(0, null);
    }

    @Test
    public void tryInsertTransports() {
        HashMap<WorldPoint, List<Transport>> transports = Transport.loadAllFromResources();
        PrimitiveIntHashMap<List<Transport>> map = new PrimitiveIntHashMap<>(transports.size());

        for (Map.Entry<WorldPoint, List<Transport>> entry : transports.entrySet()) {
            int packedPoint = WorldPointUtil.packWorldPoint(entry.getKey());
            map.put(packedPoint, entry.getValue());
        }

        for (Map.Entry<WorldPoint, List<Transport>> entry : transports.entrySet()) {
            int packedPoint = WorldPointUtil.packWorldPoint(entry.getKey());
            Assert.assertEquals("World Point " + entry.getKey() + " did not map to the correct value", entry.getValue(), map.get(packedPoint));
        }
    }

    @Test
    public void tryGrowMap() {
        PrimitiveIntHashMap<Integer> map = new PrimitiveIntHashMap<>(8);
        for (int i = 0; i < 1024; ++i) {
            map.put(i, i);
        }

        Assert.assertEquals(1024, map.size());

        for (int i = 0; i < 1024; ++i) {
            Assert.assertEquals(i, map.get(i).intValue());
        }
    }

    @Test
    public void checkNonexistentEntries() {
        PrimitiveIntHashMap<Integer> map = new PrimitiveIntHashMap<>(8);
        Assert.assertNull(map.get(667215));
    }

    @Test
    public void tryOverwriteValues() {
        final int keyStart = 9875643; // Use keys that aren't 0 or all low bits

        PrimitiveIntHashMap<Integer> map = new PrimitiveIntHashMap<>(2048);
        for (int i = 0; i < 1024; ++i) {
            map.put(i + keyStart, i);
        }

        // Overwrite values
        for (int i = 0; i < 1024; ++i) {
            map.put(i + keyStart, i + 1);
        }

        // Now check overwritten values stuck
        for (int i = 0; i < 1024; ++i) {
            Assert.assertEquals(i + 1, map.get(i + keyStart).intValue());
        }
    }

    @Test
    public void checkClearMap() {
        PrimitiveIntHashMap<Integer> map = new PrimitiveIntHashMap<>(8);
        for (int i = 0; i < 1024; ++i) {
            map.put(i, i);
        }

        Assert.assertEquals(1024, map.size());
        Assert.assertEquals(364, map.get(364).intValue());

        map.clear();
        Assert.assertEquals(0, map.size());
        Assert.assertNull(map.get(364));
    }

    @Test
    public void checkInsertOrderIrrelevant() {
        final int keyStart = 9875643; // Use keys that aren't 0 or all low bits
        PrimitiveIntHashMap<Integer> mapForward = new PrimitiveIntHashMap<>(8);
        PrimitiveIntHashMap<Integer> mapReversed = new PrimitiveIntHashMap<>(8);
        for (int i = 0; i < 1024; ++i) {
            mapForward.put(i + keyStart, i);
            mapReversed.put(1023 - i + keyStart, 1023 - i);
        }

        for (int i = 0; i < 1024; ++i) {
            Assert.assertEquals(mapForward.get(i + keyStart), mapReversed.get(i + keyStart));
        }
    }
}
