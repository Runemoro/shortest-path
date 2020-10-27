package shortestpath.pathfinder;

import java.util.Objects;

public class Positon {
    public final int x;
    public final int y;
    public final int z;

    public Positon(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String toString() {
        return "("  + x + "," + y + "," + z + ')';
    }

    public boolean equals(Object o) {
        return o instanceof Positon && x == ((Positon) o).x && y == ((Positon) o).y && z == ((Positon) o).z;
    }

    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
