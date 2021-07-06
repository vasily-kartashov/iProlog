package ptarau.iprolog;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptarau.iprolog.util.IMap;
import ptarau.iprolog.util.IMaps;
import ptarau.iprolog.util.MyIntList;
import ptarau.iprolog.util.IntMap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Integer.min;
import static ptarau.iprolog.Tokenizer.SourceType.RESOURCE;

/**
 * Implements execution mechanism
 */
class Engine {

    final static private Logger logger = LoggerFactory.getLogger(Engine.class);

    final static int MAXIND = 3; // number of index args
    final static int START_INDEX = 20;
    // switches off indexing for less then START_INDEX clauses e.g. <20
    /**
     * tags of our heap cells - that can also be seen as
     * instruction codes in a compiled implementation
     *
     * v - firs occurrences of variables
     * u - subsequent occurrences of variables
     * r - references pointing at arrays starting with their length marked with a:
     *
     */
    final private static int V = 0;
    final private static int U = 1;
    final private static int R = 2;
    final private static int C = 3;
    final private static int N = 4;
    final private static int A = 5;
    final private static int BAD = 7;
    final private static int MINSIZE = 1 << 15; // power of 2

    /**
     * trimmed down clauses ready to be quickly relocated to the heap
     */
    final List<Clause> clauses;
    final IntArrayList clauseIndex;
    /**
     * symbol table made of map + reverse map from ints to syms
     */

    final LinkedHashMap<String, Integer> symbolMap;
    final IMaps imaps;
    final List<IntMap> vmaps;
    final private List<String> symbolList;
    final private IntArrayList trail;
    final private IntArrayList unificationStack;
    final private Stack<Spine> spines = new Stack<>();
    Spine query;
    /**
     * runtime areas:
     * <p>
     * the heap contains code for and clauses their their copies
     * created during execution
     * <p>
     * the trail is an undo list for variable bindings
     * that facilitates retrying failed goals with alternative
     * matching clauses
     * <p>
     * the unification stack ustack helps handling term unification non-recursively
     * <p>
     * the spines stack contains abstractions of clauses and goals and performs the
     * functions of  both a choice-point stack and goal stack
     * <p>
     * imaps: contains indexes for up toMAXIND>0 arg positions (0 for pred symbol itself)
     * <p>
     * vmaps: contains clause numbers for which vars occur in indexed arg positions
     */

    private IntArrayList heap;
    private int top;

    // G - ground?

    /**
     * Builds a new engine from a natural-language style assembler.nl file
     */
    Engine(final String programName) {
        symbolMap = new LinkedHashMap<>();
        symbolList = new ArrayList<>();

        makeHeap();

        trail = new IntArrayList();
        unificationStack = new IntArrayList();

        clauses = loadProgram(programName);

        clauseIndex = indexClauses(clauses);

        query = init();

        vmaps = vcreate(MAXIND);
        imaps = index(clauses, vmaps);
    }

    /**
     * tags an integer value while flipping it into a negative
     * number to ensure that untagged cells are always negative and the tagged
     * ones are always positive - a simple way to ensure we do not mix them up
     * at runtime
     */
    private static int tag(final int t, final int w) {
        return -((w << 3) + t);
    }

    /**
     * removes tag after flipping sign
     */
    private static int detag(final int w) {
        return -w >> 3;
    }

    /**
     * extracts the tag of a cell
     */
    private static int tagOf(final int w) {
        return -w & 7;
    }

    /**
     * expands a "Xs lists .." statements to "Xs holds" statements
     */
    private static List<List<String>> maybeExpand(final List<String> Ws) {
        var W = Ws.get(0);
        if (W.length() < 2 || !"l:".equals(W.substring(0, 2))) {
            return null;
        }

        int l = Ws.size();
        var V = W.substring(2);
        return IntStream.range(1, l)
                .mapToObj(i -> {
                    var Vi = 1 == i ? V : V + "__" + (i - 1);
                    var Vii = V + "__" + i;
                    return List.of("h:" + Vi, "c:list", Ws.get(i), i == l - 1 ? "c:nil" : "v:" + Vii);
                })
                .collect(Collectors.toList());
    }

    /**
     * expands, if needed, "lists" statements in sequence of statements
     */
    private static List<List<String>> mapExpand(final List<List<String>> Wss) {
        List<List<String>> Rss = new ArrayList<>();
        for (var Ws : Wss) {
            var Hss = maybeExpand(Ws);
            if (Hss != null) {
                Rss.addAll(Hss);
            } else {
                Rss.add(new ArrayList<>(Ws));
            }
        }
        return Rss;
    }

    private static IntArrayList indexClauses(final List<Clause> clauses) {
        var l = clauses.size();
        var index = new IntArrayList(l);
        for (int i = 0; i < l; i++) {
            index.add(i);
        }
        return index;
    }

    /**
     * true if cell x is a variable
     * assumes that variables are tagged with 0 or 1
     */
    private static boolean isVAR(final int x) {
        return tagOf(x) < 2;
    }

    /**
     * relocates a variable or array reference cell by b
     * assumes var/ref codes V,U,R are 0,1,2
     */
    private static int relocate(final int b, final int cell) {
        return tagOf(cell) < 3 ? cell + b : cell;
    }

    public static List<IntMap> vcreate(int l) {
        List<IntMap> vss = new ArrayList<>(l);
        for (int i = 0; i < l; i++) {
            vss.add(new IntMap());
        }
        return vss;
    }

    static void put(IMaps imaps, List<IntMap> vss, int[] keys, int val) {
        for (int i = 0; i < imaps.size(); i++) {
            var key = keys[i];
            if (key != 0) {
                imaps.put(i, key, val);
            } else {
                vss.get(i).add(val);
            }
        }
    }

    /**
     * places an identifier in the symbol table
     */
    private int addSymbol(String symbol) {
        return symbolMap.computeIfAbsent(symbol, s -> {
            symbolList.add(s);
            return symbolMap.size();
        });
    }

    /**
     * returns the symbol associated to an integer index
     * in the symbol table
     */
    private String getSym(final int w) {
        if (w < 0 || w >= symbolList.size()) {
            return "BADSYMREF=" + w;
        }
        return symbolList.get(w);
    }

    private void makeHeap() {
        heap = new IntArrayList(MINSIZE);
        clear();
    }

    private int getTop() {
        return top;
    }

    private void setTop(int top) {
        this.top = top;
    }

    private void clear() {
        top = -1;
    }

    /**
     * Pushes an element - top is incremented first than the
     * element is assigned. This means top point to the last assigned
     * element - which can be returned with peek().
     */
    private void push(final int i) {
        top++;
        if (top < heap.size()) {
            heap.set(top, i);
        } else {
            heap.add(i);
        }
    }

    final int size() {
        return top + 1;
    }

    /**
     * loads a program from a .nl file of
     * "natural language" equivalents of Prolog/HiLog statements
     */
    List<Clause> loadProgram(final String programName) {
        List<List<List<String>>> Wsss = Tokenizer.toSentences(programName, RESOURCE);
        List<Clause> Cs = new ArrayList<>();

        for (final var Wss : Wsss) {
            logger.info("Sentence: {}", Wss);
            // clause starts here

            Map<String, IntArrayList> refs = new LinkedHashMap<>();
            var cs = new IntArrayList();
            var gs = new IntArrayList();

            var Rss = mapExpand(Wss);
            var k = 0;
            for (var ws : Rss) {

                // head or body element starts here

                final var l = ws.size();
                gs.push(tag(R, k++));
                cs.push(tag(A, l));

                for (String w : ws) {

                    // head or body subterm starts here

                    if (1 == w.length()) {
                        w = "c:" + w;
                    }

                    final var L = w.substring(2);

                    switch (w.charAt(0)) {
                        case 'c' -> {
                            cs.push(encode(C, L));
                            k++;
                        }
                        case 'n' -> {
                            cs.push(encode(N, L));
                            k++;
                        }
                        case 'v' -> {
                            var Is = refs.get(L);
                            if (null == Is) {
                                Is = new IntArrayList();
                                refs.put(L, Is);
                            }
                            Is.push(k);
                            cs.push(tag(BAD, k)); // just in case we miss this
                            k++;
                        }
                        case 'h' -> {
                            var Is = refs.get(L);
                            if (null == Is) {
                                Is = new IntArrayList();
                                refs.put(L, Is);
                            }
                            Is.push(k - 1);
                            cs.set(k - 1, tag(A, l - 1));
                            gs.popInt();
                        }
                        default -> Main.prettyPrint("FORGOTTEN=" + w);
                    } // end subterm
                } // end element
            } // end clause

            // linker

            for (IntArrayList Is : refs.values()) {
                // finding the A among refs
                int leader = -1;
                for (final int j : Is) {
                    if (A == tagOf(cs.getInt(j))) {
                        leader = j;

                        break;
                    }
                }
                if (-1 == leader) {
                    // for vars, first V others U
                    leader = Is.getInt(0);
                    for (final int i : Is) {
                        if (i == leader) {
                            cs.set(i, tag(V, i));
                        } else {
                            cs.set(i, tag(U, leader));
                        }

                    }
                } else {
                    for (final int i : Is) {
                        if (i == leader) {
                            continue;
                        }
                        cs.set(i, tag(R, leader));
                    }
                }
            }

            var neck = 1 == gs.size() ? cs.size() : detag(gs.getInt(1));
            var tgs = gs.toArray(new int[] {});
            Clause C = putClause(cs.toArray(new int[] {}), tgs, neck);
            Cs.add(C);
        }
        return Cs;
    }

    /*
     * encodes string constants into symbols while leaving
     * other data types untouched
     */
    private int encode(final int t, final String s) {
        int w;
        try {
            w = Integer.parseInt(s);
        } catch (final Exception e) {
            if (C == t) {
                w = addSymbol(s);
            } else
                //pp("bad in encode=" + t + ":" + s);
                return tag(BAD, 666);
        }
        return tag(t, w);
    }

    /**
     * returns the heap cell another cell points to
     */
    final int getRef(final int x) {
        return heap.getInt(detag(x));
    }

    /*
     * sets a heap cell to point to another one
     */
    private void setRef(final int w, final int r) {
        heap.set(detag(w), r);
    }

    /**
     * removes binding for variable cells
     * above savedTop
     */
    private void unwindTrail(final int savedTop) {
        while (savedTop < trail.size() - 1) {
            var href = trail.popInt();
            setRef(href, href);
        }
    }

    /**
     * scans reference chains starting from a variable
     * until it points to an unbound root variable or some
     * non-variable cell
     */
    private int dereference(int x) {
        while (isVAR(x)) {
            var r = getRef(x);
            if (r == x) {
                break;
            }
            x = r;
        }
        return x;
    }

    /**
     * raw display of a term - to be overridden
     */
    String showTerm(final int x) {
        return showTerm(exportTerm(x));
    }

    /**
     * raw display of a externalized term
     */
    String showTerm(final Object O) {
        if (O instanceof Object[]) {
            return Arrays.deepToString((Object[]) O);
        }
        return O.toString();
    }

    /**
     * builds an array of embedded arrays from a heap cell
     * representing a term for interaction with an external function
     * including a displayer
     */
    Object exportTerm(int x) {
        x = dereference(x);

        final var tag = tagOf(x);
        final var value = detag(x);

        return switch (tag) {
            case C -> getSym(value);
            case N -> value;
            case V -> "V" + value;
            case R -> {
                final int a = heap.getInt(value);
                if (A != tagOf(a)) {
                    yield "*** should be A, found=" + showCell(a);
                }
                final var n = detag(a);
                final var arr = new Object[n];
                final var k = value + 1;
                for (int i = 0; i < n; i++) {
                    final var j = k + i;
                    arr[i] = exportTerm(heap.getInt(j));
                }
                yield arr;
            }
            default -> "*BAD TERM*" + showCell(x);
        };
    }

    /**
     * raw display of a cell as tag : value
     */
    String showCell(int w) {
        final var tag = tagOf(w);
        final var value = detag(w);
        return switch (tag) {
            case V -> "v:" + value;
            case U -> "u:" + value;
            case N -> "n:" + value;
            case C -> "c:" + getSym(value);
            case R -> "r:" + value;
            case A -> "a:" + value;
            default -> "*BAD*=" + w;
        };
    }

    /**
     * a displayer for cells
     */

    String showCells(final int base, final int len) {
        final var builder = new StringBuilder();
        for (int k = 0; k < len; k++) {
            final var instr = heap.getInt(base + k);
            builder.append("[").append(base + k).append("]");
            builder.append(showCell(instr));
            builder.append(" ");
        }
        return builder.toString();
    }

    /**
     * unification algorithm for cells X1 and X2 on ustack that also takes care
     * to trail bindings below a given heap address "base"
     */
    private boolean unify(final int base) {
        while (!unificationStack.isEmpty()) {
            final int x1 = dereference(unificationStack.popInt());
            final int x2 = dereference(unificationStack.popInt());
            if (x1 != x2) {
                if (isVAR(x1)) { /* unb. var. v1 */
                    final int w1 = detag(x1);
                    final int w2 = detag(x2);
                    if (isVAR(x2) && w2 > w1) { /* unb. var. v2 */
                        heap.set(w2, x1);
                        if (w2 <= base) {
                            trail.push(x2);
                        }
                    } else { // x2 nonvar or older
                        heap.set(w1, x2);
                        if (w1 <= base) {
                            trail.push(x1);
                        }
                    }
                } else if (isVAR(x2)) { /* x1 is NONVAR */
                    final int w2 = detag(x2);
                    heap.set(w2, x1);
                    if (w2 <= base) {
                        trail.push(x2);
                    }
                } else {
                    final int t1 = tagOf(x1);
                    final int t2 = tagOf(x2);
                    if (R == t1 && R == t2) { // both should be R
                        final int w1 = detag(x1);
                        final int w2 = detag(x2);
                        if (!unifyTermArguments(w1, w2))
                            return false;
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean unifyTermArguments(final int w1, final int w2) {
        final int v1 = heap.getInt(w1);
        final int v2 = heap.getInt(w2);
        // both should be A
        final int n1 = detag(v1);
        final int n2 = detag(v2);
        if (n1 != n2)
            return false;
        final int b1 = 1 + w1;
        final int b2 = 1 + w2;
        for (int i = n1 - 1; i >= 0; i--) {
            final int i1 = b1 + i;
            final int i2 = b2 + i;
            final int u1 = heap.getInt(i1);
            final int u2 = heap.getInt(i2);
            if (u1 == u2) {
                continue;
            }
            unificationStack.push(u2);
            unificationStack.push(u1);
        }
        return true;
    }

    /**
     * places a clause built by the Toks reader on the heap
     */
    Clause putClause(final int[] cs, final int[] gs, final int neck) {
        var base = size();
        var b = tag(V, base);
        var len = cs.length;
        pushCells(b, 0, len, cs);
        for (int i = 0; i < gs.length; i++) {
            gs[i] = relocate(b, gs[i]);
        }
        var xs = getIndexables(gs[0]);
        return new Clause(len, gs, base, neck, xs);
    }

    /**
     * pushes slice[from,to] of array cs of cells to heap
     */
    private void pushCells(final int b, final int from, final int to, final int base) {
        for (int i = from; i < to; i++) {
            push(relocate(b, heap.getInt(base + i)));
        }
    }

    /**
     * pushes slice[from,to] of array cs of cells to heap
     */
    private void pushCells(final int b, final int from, final int to, final int[] cs) {
        for (int i = from; i < to; i++) {
            push(relocate(b, cs[i]));
        }
    }

    /**
     * copies and relocates head of clause at offset from heap to heap
     */
    private int pushHead(final int b, final Clause C) {
        pushCells(b, 0, C.neck(), C.base());
        var head = C.hgs()[0];
        return relocate(b, head);
    }

    /**
     * copies and relocates body of clause at offset from heap to heap
     * while also placing head as the first element of array gs that
     * when returned contains references to the toplevel spine of the clause
     */
    private int[] pushBody(final int b, final int head, final Clause C) {
        pushCells(b, C.neck(), C.len(), C.base());
        final var l = C.hgs().length;
        final var gs = new int[l];
        gs[0] = head;
        for (int k = 1; k < l; k++) {
            final var cell = C.hgs()[k];
            gs[k] = relocate(b, cell);
        }
        return gs;
    }

    /**
     * makes, if needed, registers associated to top goal of a Spine
     * these registers will be reused when matching with candidate clauses
     * note that xs contains dereferenced cells - this is done once for
     * each goal's toplevel subterms
     */
    private void makeIndexArgs(final Spine G, final int goal) {
        if (G.xs != null) {
            return;
        }
        final var p = 1 + detag(goal);
        final var n = Math.min(MAXIND, detag(getRef(goal)));
        final var xs = IntStream.range(0, n)
                .map(i -> cell2index(dereference(heap.getInt(p + i))))
                .toArray();
        G.xs = xs;
        if (imaps != null) {
            G.cs = IMap.get(imaps, vmaps, xs);
        }
    }

    private int[] getIndexables(final int ref) {
        final var p = 1 + detag(ref);
        final var n = detag(getRef(ref));
        return IntStream.range(0, min(n, MAXIND))
                .map(i -> cell2index(dereference(heap.getInt(p + i))))
                .toArray();
    }

    private int cell2index(final int cell) {
        return switch (tagOf(cell)) {
            case R -> getRef(cell);
            case C, N -> cell;
            default -> 0;
        };
    }

    /**
     * tests if the head of a clause, not yet copied to the heap
     * for execution could possibly match the current goal, an
     * abstraction of which has been placed in xs
     */
    private boolean match(final int[] xs, final Clause C0) {
        return IntStream.range(0, MAXIND)
                .allMatch(i -> {
                    var x = xs[i];
                    var y = C0.xs()[i];
                    return x == 0 || y == 0 || x == y;
                });
    }

    /**
     * transforms a spine containing references to choice point and
     * immutable list of goals into a new spine, by reducing the
     * first goal in the list with a clause that successfully
     * unifies with it - in which case places the goals of the
     * clause at the top of the new list of goals, in reverse order
     */
    private Spine unfold(final Spine G) {

        final int trailTop = trail.size() - 1;
        final int htop = getTop();
        final int base = htop + 1;

        final int goal = G.goals.head();

        makeIndexArgs(G, goal);

        final int last = G.cs.size();
        for (int k = G.k; k < last; k++) {
            final Clause C0 = clauses.get(G.cs.getInt(k));

            if (!match(G.xs, C0)) {
                continue;
            }

            final int base0 = base - C0.base();
            final int b = tag(V, base0);
            final int head = pushHead(b, C0);

            unificationStack.clear(); // set up unification stack

            unificationStack.push(head);
            unificationStack.push(goal);

            if (!unify(base)) {
                unwindTrail(trailTop);
                setTop(htop);
                continue;
            }
            final int[] gs = pushBody(b, head, C0);
            final MyIntList newGoals = MyIntList.app(gs, G.goals.tail()).tail();
            G.k = k + 1;
            if (MyIntList.isEmpty(newGoals)) {
                return answer(trailTop);
            } else {
                return new Spine(gs, base, G.goals.tail(), trailTop, 0, clauseIndex);
            }
        } // end for
        return null;
    }

    /**
     * extracts a query - by convention of the form
     * goal(Vars):-body to be executed by the engine
     */
    Clause getQuery() {
        return clauses.get(clauses.size() - 1);
    }

    /**
     * returns the initial spine built from the
     * query from which execution starts
     */
    Spine init() {
        var base = size();
        var G = getQuery();
        var Q = new Spine(G.hgs(), base, MyIntList.empty, trail.size() - 1, 0, clauseIndex);
        spines.push(Q);
        return Q;
    }

    /**
     * returns an answer as a Spine while recording in it
     * the top of the trail to allow the caller to retrieve
     * more answers by forcing backtracking
     */
    private Spine answer(final int trailTop) {
        return new Spine(spines.get(0).hd, trailTop);
    }

    /**
     * detects availability of alternative clauses for the
     * top goal of this spine
     */
    private boolean hasClauses(final Spine S) {
        return S.k < S.cs.size();
    }

    /**
     * true when there are no more goals left to solve
     */
    private boolean hasGoals(final Spine S) {
        return !MyIntList.isEmpty(S.goals);
    }

    /**
     * removes this spines for the spine stack and
     * resets trail and heap to where they where at its
     * creating time - while undoing variable binding
     * up to that point
     */
    private void popSpine() {
        final Spine G = spines.pop();
        unwindTrail(G.trailTop);
        setTop(G.base - 1);
    }

    /**
     * main interpreter loop: starts from a spine and works
     * though a stream of answers, returned to the caller one
     * at a time, until the spines stack is empty - when it
     * returns null
     */
    final Spine yield_() {
        while (!spines.isEmpty()) {
            final Spine G = spines.peek();
            if (!hasClauses(G)) {
                popSpine(); // no clauses left
                continue;
            }
            final Spine C = unfold(G);
            if (null == C) {
                popSpine(); // no matches
                continue;
            }
            if (hasGoals(C)) {
                spines.push(C);
                continue;
            }
            return C; // answer
        }
        return null;
    }

    // indexing extensions - ony active if START_INDEX clauses or more

    /**
     * retrieves an answers and ensure the engine can be resumed
     * by unwinding the trail of the query Spine
     * returns an external "human readable" representation of the answer
     */
    Object ask() {
        query = yield_();
        if (null == query) {
            return null;
        }
        final int res = answer(query.trailTop).hd;
        final Object R = exportTerm(res);
        unwindTrail(query.trailTop);
        return R;
    }

    /**
     * initiator and consumer of the stream of answers
     * generated by this engine
     */
    void run() {
        long ctr = 0L;
        for (; ; ctr++) {
            final Object A = ask();
            if (null == A) {
                break;
            }
            if (ctr < 5) Program.println("[" + ctr + "] " + "*** ANSWER=" + showTerm(A));
        }
        if (ctr > 5) Program.println("...");
        Program.println("TOTAL ANSWERS=" + ctr);
    }

    final IMaps index(List<Clause> clauses, List<IntMap> vmaps) {
        if (clauses.size() < START_INDEX) {
            return null;
        }

        var imaps = new IMaps(vmaps.size());
        for (int i = 0; i < clauses.size(); i++) {
            var c = clauses.get(i);
            put(imaps, vmaps, c.xs(), i + 1); // $$$ UGLY INC
        }
        Main.prettyPrint("INDEX");
        Main.prettyPrint(imaps.toString());
        Main.prettyPrint(vmaps.toString());
        Main.prettyPrint("");
        return imaps;
    }
}
