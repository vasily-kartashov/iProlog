package ptarau.iprolog.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

final public class IMap {

    private final HashMap<Integer, IntMap> map = new HashMap<>(16);

    public static IntArrayList get(IMaps iMaps, List<IntMap> vmaps, int[] keys) {
        var tuples = IntStream.range(0, iMaps.size())
                .filter(i -> keys[i] != 0)
                .mapToObj(i -> new IntMapTuple(iMaps.get(i).get(keys[i]), vmaps.get(i)));
        var iterator = IntMap.intersect(tuples)
                .intStream()
                .map(i -> i - 1)
                .sorted()
                .iterator();
        return new IntArrayList(iterator);
    }

    final void put(int key, int val) {
        map.computeIfAbsent(key, k -> new IntMap()).add(val);
    }

    final IntMap get(int key) {
        return map.computeIfAbsent(key, k -> new IntMap());
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
