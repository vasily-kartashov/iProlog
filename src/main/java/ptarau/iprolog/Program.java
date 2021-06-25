package ptarau.iprolog;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class Program extends Engine implements Spliterator<Object> {

    Program(final String fileName) {
        super(fileName);
    }

    static void prettyPrint(final Object o) {
        Main.prettyPrint(o);
    }

    static void println(final Object o) {
        Main.prettyPrint(o);
    }

    static String maybeNull(final Object O) {
        if (null == O)
            return "$null";
        if (O instanceof Object[])
            return st0((Object[]) O);
        return O.toString();
    }

    static boolean isListCons(final Object name) {
        return ".".equals(name) || "[|]".equals(name) || "list".equals(name);
    }

    static boolean isOp(final Object name) {
        return "/".equals(name) || "-".equals(name) || "+".equals(name) || "=".equals(name);
    }

    static String st0(final Object[] args) {
        final StringBuilder buf = new StringBuilder();
        final String name = args[0].toString();
        if (args.length == 3 && isOp(name)) {
            buf.append("(");
            buf.append(maybeNull(args[0]));
            buf.append(" ").append(name).append(" ");
            buf.append(maybeNull(args[1]));
            buf.append(")");
        } else if (args.length == 3 && isListCons(name)) {
            buf.append('[');
            {
                buf.append(maybeNull(args[1]));
                Object tail = args[2];
                for (; ; ) {

                    if ("[]".equals(tail) || "nil".equals(tail)) {
                        break;
                    }
                    if (!(tail instanceof final Object[] list)) {
                        buf.append('|');
                        buf.append(maybeNull(tail));
                        break;
                    }
                    if (!(list.length == 3 && isListCons(list[0]))) {
                        buf.append('|');
                        buf.append(maybeNull(tail));
                        break;
                    } else {
                        //if (i > 1)
                        buf.append(',');
                        buf.append(maybeNull(list[1]));
                        tail = list[2];
                    }
                }
            }
            buf.append(']');
        } else if (args.length == 2 && "$VAR".equals(name)) {
            buf.append("_").append(args[1]);
        } else {
            final String qname = maybeNull(args[0]);
            buf.append(qname);
            buf.append("(");
            for (int i = 1; i < args.length; i++) {
                final Object O = args[i];
                buf.append(maybeNull(O));
                if (i < args.length - 1) {
                    buf.append(",");
                }
            }
            buf.append(")");
        }
        return buf.toString();
    }

    @Override
    String showTerm(final Object O) {
        if (O instanceof Object[])
            return st0((Object[]) O);
        return O.toString();
    }

    void prettyPrintCode() {
        prettyPrint("\nSYMS:");
        prettyPrint(syms);
        prettyPrint("\nCLAUSES:\n");

        for (int i = 0; i < clauses.size(); i++) {
            final Clause C = clauses.get(i);
            prettyPrint("[" + i + "]:" + showClause(C));
        }
        prettyPrint("");

    }

    String showClause(final Clause s) {
        final StringBuilder buf = new StringBuilder();
        final int l = s.hgs().length;
        buf.append("---base:[").append(s.base()).append("] neck: ").append(s.neck()).append("-----\n");
        buf.append(showCells(s.base(), s.len())); // TODO
        buf.append("\n");
        buf.append(showCell(s.hgs()[0]));

        buf.append(" :- [");
        for (int i = 1; i < l; i++) {

            final int e = s.hgs()[i];
            buf.append(showCell(e));
            if (i < l - 1) {
                buf.append(", ");
            }
        }

        buf.append("]\n");

        buf.append(showTerm(s.hgs()[0]));
        if (l > 1) {
            buf.append(" :- \n");
            for (int i = 1; i < l; i++) {
                final int e = s.hgs()[i];
                buf.append("  ");
                buf.append(showTerm(e));
                buf.append("\n");
            }
        } else {
            buf.append("\n");
        }
        return buf.toString();
    }

    public Stream<Object> stream() {
        return StreamSupport.stream(this, false);
    }

    @Override
    public boolean tryAdvance(Consumer<? super Object> action) {
        final Object R = ask();
        final boolean ok = null != R;
        if (ok) {
            action.accept(R);
        }
        return ok;
    }

    @Override
    public Spliterator<Object> trySplit() {
        return null;
    }

    @Override
    public int characteristics() {
        return (Spliterator.ORDERED | Spliterator.NONNULL) & ~Spliterator.SIZED;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }
}
