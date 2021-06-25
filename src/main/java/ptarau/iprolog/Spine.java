package ptarau.iprolog;

import ptarau.iprolog.util.IntList;

/**
 * runtime representation of an immutable list of goals
 * together with top of heap and trail pointers
 * and current clause tried out by head goal
 * as well as registers associated to it
 * <p>
 * note that parts of this immutable lists
 * are shared among alternative branches
 */
class Spine {

    final int hd; // head of the clause to which this corresponds
    final int base; // top of the heap when this was created
    final IntList goals; // goals - with the top one ready to unfold
    final int trailTop; // top of the trail when this was created
    int k;
    int[] xs; // index elements
    int[] cs; // array of  clauses known to be unifiable with top goal in gs

    /**
     * creates a spine - as a snapshot of some runtime elements
     */
    Spine(final int[] gs0, final int base, final IntList goals, final int trailTop, final int k, final int[] cs) {
        this.hd = gs0[0];
        this.base = base;
        this.goals = IntList.tail(IntList.app(gs0, goals)); // prepends the goals of clause with head hs
        this.trailTop = trailTop;
        this.k = k;
        this.cs = cs;
    }
    /**
     * creates a specialized spine returning an answer (with no goals left to solve)
     */
    Spine(final int hd, final int trailTop) {
        this.hd = hd;
        this.base = 0;
        this.goals = IntList.empty;
        this.trailTop = trailTop;
        this.k = -1;
        this.cs = null;
    }
}
