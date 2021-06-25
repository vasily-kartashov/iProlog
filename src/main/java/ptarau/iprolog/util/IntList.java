package ptarau.iprolog.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class IntList {

    public static final IntList empty = null;
    private final int head;
    private final IntList tail;

    private IntList(final int X, final IntList Xs) {
        head = X;
        tail = Xs;
    }

    public static boolean isEmpty(final IntList Xs) {
        return null == Xs;
    }

    public static int head(final IntList Xs) {
        return Xs.head;
    }

    public static IntList tail(final IntList Xs) {
        return Xs.tail;
    }

    static IntList cons(final int X, final IntList Xs) {
        return new IntList(X, Xs);
    }

    public static IntList app(final int[] xs, final IntList Ys) {
        IntList Zs = Ys;
        for (int i = xs.length - 1; i >= 0; i--) {
            Zs = cons(xs[i], Zs);
        }
        return Zs;
    }

    static IntArrayList toInts(IntList Xs) {
        final IntArrayList is = new IntArrayList();
        while (!isEmpty(Xs)) {
            is.push(head(Xs));
            Xs = tail(Xs);
        }
        return is;
    }

    @Override
    public String toString() {
        return toInts(this).toString();
    }
}
