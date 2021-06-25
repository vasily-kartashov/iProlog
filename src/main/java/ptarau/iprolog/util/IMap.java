package ptarau.iprolog.util;

import java.io.Serial;
import java.util.*;

public final class IMap<K> implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final HashMap<K, IntMap> map;

    IMap() {
        map = new HashMap<>();
    }

    public static IMap<Integer>[] create(final int l) {
        final IMap<Integer> first = new IMap<>();
        @SuppressWarnings("unchecked")
        final IMap<Integer>[] imaps = (IMap<Integer>[]) java.lang.reflect.Array.newInstance(first.getClass(), l);
        imaps[0] = first;
        for (int i = 1; i < l; i++) {
            imaps[i] = new IMap<>();
        }
        return imaps;
    }

    public static boolean put(final IMap<Integer>[] imaps, final int pos, final int key, final int val) {
        return imaps[pos].put(key, val);
    }

    public static int[] get(final IMap<Integer>[] iMaps, final IntMap[] vmaps, final int[] keys) {
        final int l = iMaps.length;
        final ArrayList<IntMap> ms = new ArrayList<>();
        final ArrayList<IntMap> vms = new ArrayList<>();

        for (int i = 0; i < l; i++) {
            final int key = keys[i];
            if (0 == key) {
                continue;
            }
            //Main.pp("i=" + i + " ,key=" + key);
            final IntMap m = iMaps[i].get(key);
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

        final IntStack cs = IntMap.intersect(ims, vims); // $$$ add vmaps here
        final int[] is = cs.toArray();
        for (int i = 0; i < is.length; i++) {
            is[i] = is[i] - 1;
        }
        java.util.Arrays.sort(is);
        return is;
    }

    public static String show(final IMap<Integer>[] imaps) {
        return Arrays.toString(imaps);
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
