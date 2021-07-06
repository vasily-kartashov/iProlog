package ptarau.iprolog.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

final public class IMap<K> {

    private final HashMap<K, IntMap> map;

    IMap() {
        map = new HashMap<>();
    }

    public static List<IMap<Integer>> create(final int l) {
        List<IMap<Integer>> maps = new ArrayList<>(l);
        for (int i = 0; i < l; i++) {
            maps.add(new IMap<>());
        }
        return maps;
    }

    public static boolean put(final List<IMap<Integer>> imaps, final int pos, final int key, final int val) {
        return imaps.get(pos).put(key, val);
    }

    public static IntArrayList get(final List<IMap<Integer>> iMaps, final List<IntMap> vmaps, final int[] keys) {
        var l = iMaps.size();
        var ms = new ArrayList<IntMap>();
        var vms = new ArrayList<IntMap>();

        for (int i = 0; i < l; i++) {
            var key = keys[i];
            if (key == 0) {
                continue;
            }
            var m = iMaps.get(i).get(key);
            ms.add(m);
            vms.add(vmaps.get(i));
        }
        var tuples = new ArrayList<IntMapTuple>(ms.size());
        for (int i = 0; i < ms.size(); i++) {
            tuples.add(new IntMapTuple(ms.get(i), vms.get(i)));
        }

        var iterator = IntMap.intersect(tuples)
                .intStream()
                .parallel()
                .map(i -> i - 1)
                .sorted()
                .iterator();
        return new IntArrayList(iterator);
    }

    public static String show(final List<IMap<Integer>> imaps) {
        return imaps.toString();
    }

    final boolean put(final K key, final int val) {
        IntMap vals = map.get(key);
        if (null == vals) {
            vals = new IntMap();
            map.put(key, vals);
        }
        return vals.add(val);
    }

    final IntMap get(final K key) {
        IntMap s = map.get(key);
        if (null == s) {
            s = new IntMap();
        }
        return s;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
