package ptarau.iprolog.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * symbol table made of map + reverse map from ints to symbols
 */
public class SymbolMap {

    final private LinkedHashMap<String, Integer> symbolMap = new LinkedHashMap<>();
    final private List<String> symbolList = new ArrayList<>();

    /**
     * places an identifier in the symbol table
     * returns the index
     */
    public int add(String symbol) {
        return symbolMap.computeIfAbsent(symbol, s -> {
            symbolList.add(s);
            return symbolMap.size();
        });
    }

    /**
     * returns the symbol associated to an integer index
     * in the symbol table
     */
    public String get(int i) {
        return symbolList.get(i);
    }

    /**
     * returns the size of symbol list
     */
    public int size() {
        return symbolList.size();
    }
}
