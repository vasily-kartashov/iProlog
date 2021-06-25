package ptarau.iprolog;

/**
 * Representation of a clause
 *
 * len  - head+goals pointing to cells in cs
 * hgs  - heap where this starts
 * base - length of heap slice
 * neck - first after the end of the head
 * xs   - indexables in head
 */
record Clause(int len, int[] hgs, int base, int neck, int[] xs) {}
