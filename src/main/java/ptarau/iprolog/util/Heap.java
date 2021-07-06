package ptarau.iprolog.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class Heap {

    final private static int MIN_SIZE = 1 << 15;

    final private IntArrayList heap = new IntArrayList(MIN_SIZE);
    private int top = -1;

    public int getTop() {
        return top;
    }

    public int get(int i) {
        return heap.getInt(i);
    }

    public void set(int i, int v) {
        heap.set(i, v);
    }

    public void setTop(int top) {
        this.top = top;
    }

    /**
     * Pushes an element - top is incremented first than the
     * element is assigned. This means top point to the last assigned
     * element - which can be returned with peek().
     */
    public void push(final int i) {
        top++;
        if (top < heap.size()) {
            heap.set(top, i);
        } else {
            heap.add(i);
        }
    }

    public int size() {
        return top + 1;
    }
}
