package es.upm.pproject.sokoban.model.dto;

import java.io.Serializable;

public final class Pair implements Comparable<Pair>, Serializable {
    private static final long serialVersionUID = 1L;

    private final int x;
    private final int y;

    public Pair(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Pair)) return false;
        Pair pair = (Pair) object;
        return x == pair.x && y == pair.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public int compareTo(Pair pair) {
        if (this.y != pair.y) return Integer.compare(this.y, pair.y);
        return Integer.compare(this.x, pair.x);
    }
}
