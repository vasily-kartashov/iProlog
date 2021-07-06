package ptarau.iprolog.util;

public record IntMapTuple(IntMap a, IntMap b) {
    public boolean containsKey(int key) {
        return a.containsKey(key) || b.containsKey(key);
    }
}
