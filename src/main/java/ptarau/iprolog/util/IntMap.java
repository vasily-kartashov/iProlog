package ptarau.iprolog.util;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.List;

/**
 * derived from code at https://github.com/mikvor/hashmapTest
 */
final public class IntMap extends Int2IntOpenHashMap {

    private final static int NO_VALUE = 0;
    private final static int FREE_KEY = 0;

    public IntMap() {
        super(1 << 2);
    }

    static void intersect0(IntMap m, List<IntMapTuple> tuples, final IntArrayList r) {
        for (var key: m.keys) {
            if (key == FREE_KEY) {
                continue;
            }
            var match = tuples.stream().parallel()
                    .allMatch(tuple -> tuple.containsKey(key));
            if (match) {
                r.push((int) key);
            }
        }
    }

    static IntArrayList intersect(List<IntMapTuple> tuples) {
        var r = new IntArrayList();
        intersect0(tuples.get(0).a(), tuples, r);
        intersect0(tuples.get(0).b(), tuples, r);
        return r;
    }

    public int get(int key) {
        return super.getOrDefault(key, NO_VALUE);
    }

    public final boolean add(final int key) {
        return NO_VALUE != put(key, 666);
    }
}
