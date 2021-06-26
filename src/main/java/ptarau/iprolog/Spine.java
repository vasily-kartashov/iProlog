package ptarau.iprolog;

import ptarau.iprolog.util.MyIntList;

/**
 * runtime representation of an immutable list of goals
 * together with top of heap and trail pointers
 * and current clause tried out by head goal
 * as well as registers associated to it
 * <p>
 * note that parts of this immutable lists
 * are shared among alternative branches
 *
 * Spline is a runtime abstraction of a Clause.
 * It collects information needed for the execution of the goals
 * originating from it. Goal elements of this immutable list are shared
 * among alternative branches,
 */
final class Spine {

    /**
     * Head of the clause to which this corresponds
     */
    final int hd;

    /**
     * Base of the heap where the clause starts
     */
    final int base;

    /**
     * Immutable list of the locations of the goal elements
     * accumulated by unfolding clauses so far
     */
    final MyIntList goals;

    /**
     * Top of the trail as it was when the clause got unified
     */
    final int trailTop;

    /**
     * Index of the last clause that the top goal
     * of the spline has tried to match so far
     */
    int k;

    /**
     * Index elements based on regs
     */
    int[] xs;

    /**
     * array of  clauses known to be unifiable with top goal in gs
     */
    int[] cs;

    /**
     * creates a spine - as a snapshot of some runtime elements
     */
    Spine(final int[] gs0, final int base, final MyIntList goals, final int trailTop, final int k, final int[] cs) {
        this.hd = gs0[0];
        this.base = base;
        this.goals = MyIntList.app(gs0, goals).tail(); // prepends the goals of clause with head hs
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
        this.goals = MyIntList.empty;
        this.trailTop = trailTop;
        this.k = -1;
        this.cs = null;
    }
}
