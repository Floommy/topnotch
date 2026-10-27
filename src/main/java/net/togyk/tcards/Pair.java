package net.togyk.tcards;

import java.util.Objects;

public class Pair<F, S> {
    public final F first;
    public final S second;

    protected Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair<>(first, second);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair<?, ?> p)) return false;
        return Objects.equals(first, p.first) && Objects.equals(second, p.second);
    }
    @Override public int hashCode() { return Objects.hash(first, second); }
    @Override public String toString() { return "(" + first + ", " + second + ")"; }
}
