package ptarau.iprolog;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads chars from char streams using the current default encoding
 */
public class Tokenizer extends StreamTokenizer {

    public enum SourceType {
        RESOURCE, STRING
    }

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

    public static Tokenizer createTokenizer(String source, SourceType sourceType) {
        try {
            Reader reader = switch (sourceType) {
                case RESOURCE -> {
                    var resourceName = "/prolog/" + source + ".pl.nl";
                    var resource = Tokenizer.class.getResourceAsStream(resourceName);
                    assert resource != null;
                    yield new InputStreamReader(resource);
                }
                case STRING -> new StringReader(source);
            };
            return new Tokenizer(reader);
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse program", e);
        }
    }

    public static List<List<List<String>>> toSentences(String source, SourceType sourceType) {
        final List<List<List<String>>> Wsss = new ArrayList<>();
        List<List<String>> Wss = new ArrayList<>();
        List<String> Ws = new ArrayList<>();
        final Tokenizer tokenizer = createTokenizer(source, sourceType);
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

    public String getWord() {
        int c;
        try {
            c = nextToken();
            while (Character.isWhitespace(c) && c != TT_EOF) {
                c = nextToken();
            }
        } catch (IOException e) {
            throw new RuntimeException("Tokenizer error", e);
        }

        return switch (c) {
            case TT_WORD -> {
                final char first = sval.charAt(0);
                if (Character.isUpperCase(first) || '_' == first) {
                    yield "v:" + sval;
                } else {
                    try {
                        final int n = Integer.parseInt(sval);
                        if (Math.abs(n) < 1 << 28) {
                            yield "n:" + sval;
                        } else {
                            yield "c:" + sval;
                        }
                    } catch (final Exception e) {
                        yield "c:" + sval;
                    }
                }
            }
            case StreamTokenizer.TT_EOF -> null;
            default -> Character.toString((char) c);
        };
    }
}