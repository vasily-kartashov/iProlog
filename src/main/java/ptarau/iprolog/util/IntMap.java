package ptarau.iprolog.util;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.stream.Stream;

final public class IntMap extends Int2IntOpenHashMap {

    private final static int NO_VALUE = 0;
    private final static int FREE_KEY = 0;

    public IntMap() {
        super(16);
    }

    static void intersect0(IntMap m, Stream<IntMapTuple> tuples, IntArrayList r) {
        m.keySet().intStream()
                .filter(key -> key != FREE_KEY)
                .filter(key -> tuples.parallel().allMatch(tuple -> tuple.containsKey(key)))
                .forEach(r::push);
    }

    static IntArrayList intersect(Stream<IntMapTuple> tuples) {
        var r = new IntArrayList();
        var h = tuples.iterator().next();
        intersect0(h.a(), tuples, r);
        intersect0(h.b(), tuples, r);
        return r;
    }

    public int get(int key) {
        return super.getOrDefault(key, NO_VALUE);
    }

    public final boolean add(final int key) {
        return NO_VALUE != put(key, 666);
    }
}
