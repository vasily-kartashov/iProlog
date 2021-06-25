package ptarau.iprolog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads chars from char streams using the current default encoding
 */
public class Tokenizer extends StreamTokenizer {

    public static final Logger logger = LoggerFactory.getLogger(Tokenizer.class);

    // reserved words - with syntactic function

    public final static String IF = "if";
    public final static String AND = "and";
    public final static String DOT = ".";
    public final static String HOLDS = "holds";
    public final static String LISTS = "lists"; // todo
    public final static String IS = "is"; // todo

    public Tokenizer(final Reader reader) {
        super(reader);
        resetSyntax();
        eolIsSignificant(false);
        ordinaryChar('.');
        ordinaryChars('!', '/'); // 33-47
        ordinaryChars(':', '@'); // 55-64
        ordinaryChars('[', '`'); // 91-96
        ordinaryChars('{', '~'); // 123-126
        wordChars('_', '_');
        wordChars('a', 'z');
        wordChars('A', 'Z');
        wordChars('0', '9');
        slashStarComments(true);
        slashSlashComments(true);
        ordinaryChar('%');
    }

    public static Tokenizer createTokenizer(String source, boolean fromFile) {
        try {
            Reader reader;
            if (fromFile) {
                var resourceName = "/prolog/" + source + ".pl.nl";
                var resource = Tokenizer.class.getResourceAsStream(resourceName);
                assert resource != null;
                reader = new InputStreamReader(resource);
            } else {
                reader = new StringReader(source);
            }
            return new Tokenizer(reader);
        } catch (Exception e) {
            logger.warn("Cannot parse program", e);
            throw new RuntimeException("Cannot parse program", e);
        }
    }

    public static List<List<List<String>>> toSentences(String source, boolean fromResource) {
        final List<List<List<String>>> Wsss = new ArrayList<>();
        List<List<String>> Wss = new ArrayList<>();
        List<String> Ws = new ArrayList<>();
        final Tokenizer tokenizer = createTokenizer(source, fromResource);
        String t;
        while (null != (t = tokenizer.getWord())) {
            switch (t) {
                case DOT -> {
                    Wss.add(Ws);
                    Wsss.add(Wss);
                    Wss = new ArrayList<>();
                    Ws = new ArrayList<>();
                }
                case ("c:" + IF), ("c:" + AND) -> {
                    Wss.add(Ws);
                    Ws = new ArrayList<>();
                }
                case ("c:" + HOLDS) -> {
                    final String w = Ws.get(0);
                    Ws.set(0, "h:" + w.substring(2));
                }
                case ("c:" + LISTS) -> {
                    final String w = Ws.get(0);
                    Ws.set(0, "l:" + w.substring(2));
                }
                case ("c:" + IS) -> {
                    final String w = Ws.get(0);
                    Ws.set(0, "f:" + w.substring(2));
                }
                default -> Ws.add(t);
            }
        }
        return Wsss;
    }

    public static void main(final String[] args) {
        Main.prettyPrint(toSentences("prog.nl", true));
    }

    public String getWord() {
        String t;

        int c;
        try {
            c = nextToken();
            while (Character.isWhitespace(c) && c != TT_EOF) {
                c = nextToken();
            }
        } catch (final IOException e) {
            return "*** tokenizer error:" + e;
        }

        switch (c) {
            case TT_WORD -> {
                final char first = sval.charAt(0);
                if (Character.isUpperCase(first) || '_' == first) {
                    t = "v:" + sval;
                } else {
                    try {
                        final int n = Integer.parseInt(sval);
                        if (Math.abs(n) < 1 << 28) {
                            t = "n:" + sval;
                        } else {
                            t = "c:" + sval;
                        }
                    } catch (final Exception e) {
                        t = "c:" + sval;
                    }
                }
            }
            case StreamTokenizer.TT_EOF -> {
                t = null;
            }
            default -> {
                t = "" + (char) c;
            }
        }
        return t;
    }
}