package ptarau.iprolog.util;

import java.util.Arrays;

/**
 * Dynamic Stack for int data.
 */
public class IntStack {

    static final int SIZE = 16; // power of 2
    static final int MINSIZE = 1 << 15; // power of 2
    private int[] stack;
    private int top;

    public IntStack() {
        this(SIZE);
    }

    IntStack(final int size) {
        stack = new int[size];
        clear();
    }

    public final int getTop() {
        return top;
    }

    public final void clear() {
        top = -1;
    }

    public final boolean isEmpty() {
        return top < 0;
    }

    /**
     * Pushes an element - top is incremented first than the
     * element is assigned. This means top point to the last assigned
     * element - which can be returned with peek().
     */
    public final void push(final int i) {
        // IO.dump("push:"+i);
        if (++top >= stack.length) {
            expand();
        }
        stack[top] = i;
    }

    public final int pop() {
        final int r = stack[top--];
        shrink();
        return r;
    }

    public final int get(final int i) {
        return stack[i];
    }

    public final void set(final int i, final int val) {
        stack[i] = val;
    }

    public final int size() {
        return top + 1;
    }

    /**
     * dynamic array operation: doubles when full
     */
    private void expand() {
        final int l = stack.length;
        final int[] newstack = new int[l << 1];

        System.arraycopy(stack, 0, newstack, 0, l);
        stack = newstack;
    }

    /**
     * dynamic array operation: shrinks to 1/2 if more than than 3/4 empty
     */
    private void shrink() {
        int l = stack.length;
        if (l <= MINSIZE || top << 2 >= l)
            return;
        l = 1 + (top << 1); // still means shrink to at 1/2 or less of the heap
        if (top < MINSIZE) {
            l = MINSIZE;
        }

        final int[] newstack = new int[l];
        System.arraycopy(stack, 0, newstack, 0, top + 1);
        stack = newstack;
    }

    public int[] toArray() {
        final int[] array = new int[size()];
        if (size() > 0) {
            System.arraycopy(stack, 0, array, 0, size());
        }
        return array;
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
}
