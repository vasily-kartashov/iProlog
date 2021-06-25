package ptarau.iprolog.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class MyIntList {

    public static final MyIntList empty = null;
    private final int head;
    private final MyIntList tail;

    private MyIntList(final int X, final MyIntList Xs) {
        head = X;
        tail = Xs;
    }

    public static boolean isEmpty(final MyIntList Xs) {
        return null == Xs;
    }

    public int head() {
        return head;
    }

    public MyIntList tail() {
        return tail;
    }

    static MyIntList cons(final int X, final MyIntList Xs) {
        return new MyIntList(X, Xs);
    }

    public static MyIntList app(final int[] xs, final MyIntList Ys) {
        MyIntList Zs = Ys;
        for (int i = xs.length - 1; i >= 0; i--) {
            Zs = cons(xs[i], Zs);
        }
        return Zs;
    }

    static IntArrayList toInts(MyIntList Xs) {
        final IntArrayList is = new IntArrayList();
        while (!isEmpty(Xs)) {
            is.push(Xs.head());
            Xs = Xs.tail();
        }
        return is;
    }

    @Override
    public String toString() {
        return toInts(this).toString();
    }
}
