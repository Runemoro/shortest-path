package shortestpath.pathfinder;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Locale;

import static net.runelite.api.Constants.MAX_Z;

public class FlagMap {
    public static final int PLANE_COUNT = 4;
    protected final BitSet flags;
    public final int minX;
    public final int minY;
    public final int maxX;
    public final int maxY;
    private final int width;
    private final int height;
    private final int flagCount;

    public FlagMap(int minX, int minY, int maxX, int maxY, int flagCount) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.flagCount = flagCount;
        width = (maxX - minX + 1);
        height = (maxY - minY + 1);
        flags = new BitSet(width * height * PLANE_COUNT * flagCount);
    }

    public FlagMap(byte[] bytes, int flagCount) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        minX = buffer.getInt();
        minY = buffer.getInt();
        maxX = buffer.getInt();
        maxY = buffer.getInt();
        this.flagCount = flagCount;
        width = (maxX - minX + 1);
        height = (maxY - minY + 1);
        flags = BitSet.valueOf(buffer);
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[16 + flags.size()];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putInt(minX);
        buffer.putInt(minY);
        buffer.putInt(maxX);
        buffer.putInt(maxY);
        buffer.put(flags.toByteArray());
        return bytes;
    }

    public boolean get(int x, int y, int z, int flag) {
        if (x < minX || x > maxX || y < minY || y > maxY || z < 0 || z >= MAX_Z) {
            return false;
        }

        return flags.get(index(x, y, z, flag));
    }

    public void set(int x, int y, int z, int flag, boolean value) {
        flags.set(index(x, y, z, flag), value);
    }

    private int index(int x, int y, int z, int flag) {
        if (x < minX || x > maxX || y < minY || y > maxY || z < 0 || z >= MAX_Z || flag < 0 || flag >= flagCount) {
            throw new IndexOutOfBoundsException(
                String.format(Locale.ENGLISH, "[%d,%d,%d,%d] when extents are [>=%d,>=%d,>=%d,>=%d] - [<=%d,<=%d,<%d,<%d]",
                        x, y, z, flag,
                        minX, minY, 0, 0,
                        maxX, maxY, MAX_Z, flagCount
                )
            );
        }

        return (z * width * height + (y - minY) * width + (x - minX)) * flagCount + flag;
    }
}
