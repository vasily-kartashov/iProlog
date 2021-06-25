package ptarau.iprolog;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import ptarau.iprolog.util.IMap;
import ptarau.iprolog.util.MyIntList;
import ptarau.iprolog.util.IntMap;

import java.util.*;

/**
 * Implements execution mechanism
 */
class Engine {

    final static int MAXIND = 3; // number of index args
    final static int START_INDEX = 20;
    // switches off indexing for less then START_INDEX clauses e.g. <20
    /**
     * tags of our heap cells - that can also be seen as
     * instruction codes in a compiled implementation
     */
    final private static int V = 0;
    final private static int U = 1;
    final private static int R = 2;
    final private static int C = 3;
    final private static int N = 4;
    final private static int A = 5;
    final private static int BAD = 7;
    static final int MINSIZE = 1 << 15; // power of 2
    /**
     * trimmed down clauses ready to be quickly relocated to the heap
     */
    final Clause[] clauses;
    final int[] cls;
    /**
     * symbol table made of map + reverse map from ints to syms
     */

    final LinkedHashMap<String, Integer> syms;
    final List<IMap<Integer>> imaps;
    final IntMap[] vmaps;
    final private List<String> slist;
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

    private int[] heap;
    private int top;

    // G - ground?

    /**
     * Builds a new engine from a natural-language style assembler.nl file
     */
    Engine(final String fname) {
        syms = new LinkedHashMap<>();
        slist = new ArrayList<>();

        makeHeap();

        trail = new IntArrayList();
        unificationStack = new IntArrayList();

        clauses = dload(fname);

        cls = toNums(clauses);

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
        final String W = Ws.get(0);
        if (W.length() < 2 || !"l:".equals(W.substring(0, 2))) {
            return null;
        }

        final int l = Ws.size();
        final List<List<String>> Rss = new ArrayList<>();
        final String V = W.substring(2);
        for (int i = 1; i < l; i++) {
            var Vi = 1 == i ? V : V + "__" + (i - 1);
            var Vii = V + "__" + i;
            var Rs = List.of("h:" + Vi, "c:list", Ws.get(i), i == l - 1 ? "c:nil" : "v:" + Vii);
            Rss.add(Rs);
        }
        return Rss;
    }

    /**
     * expands, if needed, "lists" statements in sequence of statements
     */
    private static List<List<String>> mapExpand(final List<List<String>> Wss) {
        final List<List<String>> Rss = new ArrayList<>();
        for (final var Ws : Wss) {
            var Hss = maybeExpand(Ws);
            if (null == Hss) {
                var ws = new ArrayList<String>(Ws.size());
                ws.addAll(Ws);
                Rss.add(ws);
            } else {
                Rss.addAll(Hss);
            }
        }
        return Rss;
    }

    private static int[] toNums(final Clause[] clauses) {
        final int l = clauses.length;
        final int[] cls = new int[l];
        for (int i = 0; i < l; i++) {
            cls[i] = i;
        }
        return cls;
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

    public static IntMap[] vcreate(final int l) {
        final IntMap[] vss = new IntMap[l];
        for (int i = 0; i < l; i++) {
            vss[i] = new IntMap();
        }
        return vss;
    }

    static void put(final List<IMap<Integer>> imaps, final IntMap[] vss, final int[] keys, final int val) {
        for (int i = 0; i < imaps.size(); i++) {
            final int key = keys[i];
            if (key != 0) {
                IMap.put(imaps, i, key, val);
            } else {
                vss[i].add(val);
            }
        }
    }

    /**
     * places an identifier in the symbol table
     */
    private int addSym(final String sym) {
        Integer I = syms.get(sym);
        if (null == I) {
            I = syms.size();
            syms.put(sym, I);
            slist.add(sym);
        }
        return I;
    }

    /**
     * returns the symbol associated to an integer index
     * in the symbol table
     */
    private String getSym(final int w) {
        if (w < 0 || w >= slist.size())
            return "BADSYMREF=" + w;
        return slist.get(w);
    }

    private void makeHeap() {
        makeHeap(MINSIZE);
    }

    private void makeHeap(final int size) {
        heap = new int[size];
        clear();
    }

    private int getTop() {
        return top;
    }

    private int setTop(final int top) {
        return this.top = top;
    }

    private void clear() {
        top = -1;
    }

    /**
     * Pushes an element - top is incremented frirst than the
     * element is assigned. This means top point to the last assigned
     * element - which can be returned with peek().
     */
    private void push(final int i) {
        heap[++top] = i;
    }

    final int size() {
        return top + 1;
    }

    /**
     * dynamic array operation: doubles when full
     */
    private void expand() {
        final int l = heap.length;
        final int[] newstack = new int[l << 1];

        System.arraycopy(heap, 0, newstack, 0, l);
        heap = newstack;
    }

    private void ensureSize(final int more) {
        if (1 + top + more >= heap.length) {
            expand();
        }
    }

    /**
     * loads a program from a .nl file of
     * "natural language" equivalents of Prolog/HiLog statements
     */
    Clause[] dload(final String s) {
        final boolean fromFile = true;
        final List<List<List<String>>> Wsss = Tokenizer.toSentences(s, fromFile);

        final List<Clause> Cs = new ArrayList<>();

        for (final var Wss : Wsss) {
            // clause starts here

            final LinkedHashMap<String, IntArrayList> refs = new LinkedHashMap<>();
            final IntArrayList cs = new IntArrayList();
            final IntArrayList gs = new IntArrayList();

            var Rss = mapExpand(Wss);
            int k = 0;
            for (var ws : Rss) {

                // head or body element starts here

                final int l = ws.size();
                gs.push(tag(R, k++));
                cs.push(tag(A, l));

                for (String w : ws) {

                    // head or body subterm starts here

                    if (1 == w.length()) {
                        w = "c:" + w;
                    }

                    final String L = w.substring(2);

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
                            IntArrayList Is = refs.get(L);
                            if (null == Is) {
                                Is = new IntArrayList();
                                refs.put(L, Is);
                            }
                            Is.push(k);
                            cs.push(tag(BAD, k)); // just in case we miss this
                            k++;
                        }
                        case 'h' -> {
                            IntArrayList Is = refs.get(L);
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

            final int neck = 1 == gs.size() ? cs.size() : detag(gs.getInt(1));
            final int[] tgs = gs.toArray(new int[] {});

            final Clause C = putClause(cs.toArray(new int[] {}), tgs, neck);

            Cs.add(C);

        } // end clause set

        final int ccount = Cs.size();
        final Clause[] cls = new Clause[ccount];
        for (int i = 0; i < ccount; i++) {
            cls[i] = Cs.get(i);
        }
        return cls;
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
                w = addSym(s);
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
        return heap[detag(x)];
    }

    /*
     * sets a heap cell to point to another one
     */
    private void setRef(final int w, final int r) {
        heap[detag(w)] = r;
    }

    /**
     * removes binding for variable cells
     * above savedTop
     */
    private void unwindTrail(final int savedTop) {
        while (savedTop < trail.size() - 1) {
            final int href = trail.popInt();
            // assert href is var

            setRef(href, href);
        }
    }

    /**
     * scans reference chains starting from a variable
     * until it points to an unbound root variable or some
     * non-variable cell
     */
    private int deref(int x) {
        while (isVAR(x)) {
            final int r = getRef(x);
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
        if (O instanceof Object[])
            return Arrays.deepToString((Object[]) O);
        return O.toString();
    }

    /**
     * builds an array of embedded arrays from a heap cell
     * representing a term for interaction with an external function
     * including a displayer
     */
    Object exportTerm(int x) {
        x = deref(x);

        final int t = tagOf(x);
        final int w = detag(x);

        Object res;
        switch (t) {
            case C -> res = getSym(w);
            case N -> res = w;
            case V ->
                    //case U:
                    res = "V" + w;
            case R -> {

                final int a = heap[w];
                if (A != tagOf(a))
                    return "*** should be A, found=" + showCell(a);
                final int n = detag(a);
                final Object[] arr = new Object[n];
                final int k = w + 1;
                for (int i = 0; i < n; i++) {
                    final int j = k + i;
                    arr[i] = exportTerm(heap[j]);
                }
                res = arr;
            }
            default -> res = "*BAD TERM*" + showCell(x);
        }
        return res;
    }

    /**
     * raw display of a cell as tag : value
     */
    final String showCell(final int w) {
        final int t = tagOf(w);
        final int val = detag(w);
        return switch (t) {
            case V -> "v:" + val;
            case U -> "u:" + val;
            case N -> "n:" + val;
            case C -> "c:" + getSym(val);
            case R -> "r:" + val;
            case A -> "a:" + val;
            default -> "*BAD*=" + w;
        };
    }

    /**
     * a displayer for cells
     */

    String showCells(final int base, final int len) {
        final StringBuilder buf = new StringBuilder();
        for (int k = 0; k < len; k++) {
            final int instr = heap[base + k];

            buf.append("[").append(base + k).append("]");
            buf.append(showCell(instr));
            buf.append(" ");
        }
        return buf.toString();
    }

    /**
     * unification algorithm for cells X1 and X2 on ustack that also takes care
     * to trail bindigs below a given heap address "base"
     */
    private boolean unify(final int base) {
        while (!unificationStack.isEmpty()) {
            final int x1 = deref(unificationStack.popInt());
            final int x2 = deref(unificationStack.popInt());
            if (x1 != x2) {
                final int t1 = tagOf(x1);
                final int t2 = tagOf(x2);
                final int w1 = detag(x1);
                final int w2 = detag(x2);

                if (isVAR(x1)) { /* unb. var. v1 */
                    if (isVAR(x2) && w2 > w1) { /* unb. var. v2 */
                        heap[w2] = x1;
                        if (w2 <= base) {
                            trail.push(x2);
                        }
                    } else { // x2 nonvar or older
                        heap[w1] = x2;
                        if (w1 <= base) {
                            trail.push(x1);
                        }
                    }
                } else if (isVAR(x2)) { /* x1 is NONVAR */
                    heap[w2] = x1;
                    if (w2 <= base) {
                        trail.push(x2);
                    }
                } else if (R == t1 && R == t2) { // both should be R
                    if (!unify_args(w1, w2))
                        return false;
                } else
                    return false;
            }
        }
        return true;
    }

    private boolean unify_args(final int w1, final int w2) {
        final int v1 = heap[w1];
        final int v2 = heap[w2];
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
            final int u1 = heap[i1];
            final int u2 = heap[i2];
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
        final int base = size();
        final int b = tag(V, base);
        final int len = cs.length;
        pushCells(b, 0, len, cs);
        for (int i = 0; i < gs.length; i++) {
            gs[i] = relocate(b, gs[i]);
        }
        final int[] xs = getIndexables(gs[0]);
        return new Clause(len, gs, base, neck, xs);
    }

    /**
     * pushes slice[from,to] of array cs of cells to heap
     */
    private void pushCells(final int b, final int from, final int to, final int base) {
        ensureSize(to - from);
        for (int i = from; i < to; i++) {
            push(relocate(b, heap[base + i]));
        }
    }

    /**
     * pushes slice[from,to] of array cs of cells to heap
     */
    private void pushCells(final int b, final int from, final int to, final int[] cs) {
        ensureSize(to - from);
        for (int i = from; i < to; i++) {
            push(relocate(b, cs[i]));
        }
    }

    /**
     * copies and relocates head of clause at offset from heap to heap
     */
    private int pushHead(final int b, final Clause C) {
        pushCells(b, 0, C.neck(), C.base());
        final int head = C.hgs()[0];
        return relocate(b, head);
    }

    /**
     * copies and relocates body of clause at offset from heap to heap
     * while also placing head as the first element of array gs that
     * when returned contains references to the toplevel spine of the clause
     */
    private int[] pushBody(final int b, final int head, final Clause C) {
        pushCells(b, C.neck(), C.len(), C.base());
        final int l = C.hgs().length;
        final int[] gs = new int[l];
        gs[0] = head;
        for (int k = 1; k < l; k++) {
            final int cell = C.hgs()[k];
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
        if (null != G.xs) {
            return;
        }

        final int p = 1 + detag(goal);
        final int n = Math.min(MAXIND, detag(getRef(goal)));

        final int[] xs = new int[MAXIND];

        for (int i = 0; i < n; i++) {
            final int cell = deref(heap[p + i]);
            xs[i] = cell2index(cell);
        }

        G.xs = xs;

        if (null == imaps) {
            return;
        }
        G.cs = IMap.get(imaps, vmaps, xs);
    }

    private int[] getIndexables(final int ref) {
        final int p = 1 + detag(ref);
        final int n = detag(getRef(ref));
        final int[] xs = new int[MAXIND];
        for (int i = 0; i < MAXIND && i < n; i++) {
            final int cell = deref(heap[p + i]);
            xs[i] = cell2index(cell);
        }
        return xs;
    }

    private int cell2index(final int cell) {
        int x = 0;
        final int t = tagOf(cell);
        switch (t) {
            case R -> x = getRef(cell);
            case C, N -> x = cell;
            // 0 otherwise - assert: tagging with R,C,N <>0
        }
        return x;
    }

    /**
     * tests if the head of a clause, not yet copied to the heap
     * for execution could possibly match the current goal, an
     * abstraction of which has been placed in xs
     */
    private boolean match(final int[] xs, final Clause C0) {
        for (int i = 0; i < MAXIND; i++) {
            final int x = xs[i];
            final int y = C0.xs()[i];
            if (0 == x || 0 == y) {
                continue;
            }
            if (x != y)
                return false;
        }
        return true;
    }

    /**
     * transforms a spine containing references to choice point and
     * immutable list of goals into a new spine, by reducing the
     * first goal in the list with a clause that successfully
     * unifies with it - in which case places the goals of the
     * clause at the top of the new list of goals, in reverse order
     */
    private Spine unfold(final Spine G) {

        final int ttop = trail.size() - 1;
        final int htop = getTop();
        final int base = htop + 1;

        final int goal = G.goals.head();

        makeIndexArgs(G, goal);

        final int last = G.cs.length;
        for (int k = G.k; k < last; k++) {
            final Clause C0 = clauses[G.cs[k]];

            if (!match(G.xs, C0))
                continue;

            final int base0 = base - C0.base();
            final int b = tag(V, base0);
            final int head = pushHead(b, C0);

            unificationStack.clear(); // set up unification stack

            unificationStack.push(head);
            unificationStack.push(goal);

            if (!unify(base)) {
                unwindTrail(ttop);
                setTop(htop);
                continue;
            }
            final int[] gs = pushBody(b, head, C0);
            final MyIntList newgs = MyIntList.app(gs, G.goals.tail()).tail();
            G.k = k + 1;
            if (!MyIntList.isEmpty(newgs))
                return new Spine(gs, base, G.goals.tail(), ttop, 0, cls);
            else
                return answer(ttop);
        } // end for
        return null;
    }

    /**
     * extracts a query - by convention of the form
     * goal(Vars):-body to be executed by the engine
     */
    Clause getQuery() {
        return clauses[clauses.length - 1];
    }

    /**
     * returns the initial spine built from the
     * query from which execution starts
     */
    Spine init() {
        final int base = size();

        final Clause G = getQuery();
        final Spine Q = new Spine(G.hgs(), base, MyIntList.empty, trail.size() - 1, 0, cls);
        spines.push(Q);
        return Q;
    }

    /**
     * returns an answer as a Spine while recording in it
     * the top of the trail to allow the caller to retrieve
     * more answers by forcing backtracking
     */
    private Spine answer(final int ttop) {
        return new Spine(spines.get(0).hd, ttop);
    }

    /**
     * detects availability of alternative clauses for the
     * top goal of this spine
     */
    private boolean hasClauses(final Spine S) {
        return S.k < S.cs.length;
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
        if (null == query)
            return null;
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

    final List<IMap<Integer>> index(final Clause[] clauses, final IntMap[] vmaps) {
        if (clauses.length < START_INDEX)
            return null;

        final List<IMap<Integer>> imaps = IMap.create(vmaps.length);
        for (int i = 0; i < clauses.length; i++) {
            final Clause c = clauses[i];
            put(imaps, vmaps, c.xs(), i + 1); // $$$ UGLY INC
        }
        Main.prettyPrint("INDEX");
        Main.prettyPrint(IMap.show(imaps));
        Main.prettyPrint(Arrays.toString(vmaps));
        Main.prettyPrint("");
        return imaps;
    }
}
