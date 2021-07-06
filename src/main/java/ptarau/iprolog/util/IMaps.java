package ptarau.iprolog.util;

import java.util.ArrayList;

public class IMaps extends ArrayList<IMap> {

    public IMaps(int capacity) {
        super();
        for (int i = 0; i < capacity; i++) {
            add(new IMap());
        }
    }

    public void put(int pos, int key, int val) {
        get(pos).put(key, val);
    }
}
