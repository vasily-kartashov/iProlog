package ptarau.iprolog;

/**
 * Representation of a clause
 *
 * len  - The length of the code of the clause,
 *        i.e. number of head cells the clause occupies
 * hgs  - The toplevel skeleton of a clause containing references
 *        to the location of its head and then body elements
 * base - The base of the heap where the cells for the clause start
 * neck - The length of the head and this the offset where the first body
 *        element starts (or the end of the clause if none)
 * xs   - The index vector containing dereferenced constants, numbers or
 *        array sizes as extracted from the outermost term of the head
 *        of the clause, with 0 values marking variable positions.
 */
record Clause(int len, int[] hgs, int base, int neck, int[] xs) {}
