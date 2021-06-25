package ptarau.iprolog.util;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * derived from code at https://github.com/mikvor/hashmapTest
 */
public class IntMap extends Int2IntOpenHashMap {

    private final static int NO_VALUE = 0;
    private final static int FREE_KEY = 0;

    public IntMap() {
        super(1 << 2);
    }

    static void intersect0(final IntMap m, final IntMap[] maps, final IntMap[] vmaps, final IntArrayList r) {
        for (var key: m.keySet()) {
            if (key == FREE_KEY) {
                continue;
            }
            var match = true;
            for (int i = 1; i < maps.length; i++) {
                if (!maps[i].containsKey((int) key) && !vmaps[i].containsKey((int) key)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                r.push((int) key);
            }
        }
    }

    static IntArrayList intersect(final IntMap[] maps, final IntMap[] vmaps) {
        final IntArrayList r = new IntArrayList();

        intersect0(maps[0], maps, vmaps, r);
        intersect0(vmaps[0], maps, vmaps, r);
        return r;
    }

    public int get(int key) {
        return super.getOrDefault(key, NO_VALUE);
    }

    public final boolean add(final int key) {
        return NO_VALUE != put(key, 666);
    }
}
