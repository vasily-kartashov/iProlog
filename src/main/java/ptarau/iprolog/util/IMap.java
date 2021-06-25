package ptarau.iprolog.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class IMap<K> {

    private final HashMap<K, IntMap> map;

    IMap() {
        map = new HashMap<>();
    }

    public static List<IMap<Integer>> create(final int l) {
        List<IMap<Integer>> maps = new ArrayList<>(l);
        for (int i = 0; i < l; i++) {
            maps.set(i, new IMap<>());
        }
        return maps;
    }

    public static boolean put(final List<IMap<Integer>> imaps, final int pos, final int key, final int val) {
        return imaps.get(pos).put(key, val);
    }

    public static int[] get(final List<IMap<Integer>> iMaps, final IntMap[] vmaps, final int[] keys) {
        final int l = iMaps.size();
        final ArrayList<IntMap> ms = new ArrayList<>();
        final ArrayList<IntMap> vms = new ArrayList<>();

        for (int i = 0; i < l; i++) {
            final int key = keys[i];
            if (0 == key) {
                continue;
            }
            //Main.pp("i=" + i + " ,key=" + key);
            final IntMap m = iMaps.get(i).get(key);
            //Main.pp("m=" + m);
            ms.add(m);
            vms.add(vmaps[i]);
        }
        final IntMap[] ims = new IntMap[ms.size()];
        final IntMap[] vims = new IntMap[vms.size()];

        for (int i = 0; i < ims.length; i++) {
            final IntMap im = ms.get(i);
            ims[i] = im;
            final IntMap vim = vms.get(i);
            vims[i] = vim;
        }

        final IntArrayList cs = IntMap.intersect(ims, vims); // $$$ add vmaps here
        final int[] is = cs.toArray(new int[] {});
        for (int i = 0; i < is.length; i++) {
            is[i] = is[i] - 1;
        }
        java.util.Arrays.sort(is);
        return is;
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
