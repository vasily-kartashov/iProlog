package ptarau.iprolog.util;

import java.util.*;

/**
 * symbol table made of map + reverse map from ints to symbols
 */
public class Symbols {

    final private LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
    final private List<String> list = new ArrayList<>();

    /**
     * places an identifier in the symbol table
     * returns the index
     */
    public int add(String symbol) {
        return map.computeIfAbsent(symbol, s -> {
            list.add(s);
            return list.size() - 1;
        });
    }

    /**
     * returns the symbol associated to an integer index
     * in the symbol table
     */
    public String get(int i) {
        return list.get(i);
    }
}
