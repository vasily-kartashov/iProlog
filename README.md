# Paul Tarau's iProlog

_License: Apache 2.0_

## Todo

- Replace `MyIntList` with IntArrayList from `Fastutil`
- Extract the logic of IntMap, IMap into a separate namespace, maybe there's a way to parallelize the algorithms
- Add code documentation
- Add unit tests
- Finish docker-compose to be able to build and run it without installing Java 16 or SWI-Prolog.
- Adding proper Prolog parser and skip the SWI dependency
- Can we use lazy streams instead of fully resolved list and arrays, thus only pulling the data that is really needed in each iteration?

## Usage

- ./scripts/build.sh
- maven package

## Documentation

- doc/paper.pdf (_A Hitchhiker's Guide to Reinventing a Prolog Machine_)
- [tutorial at VMSS'16](https://www.youtube.com/watch?v=SRYAMt8iQSw&t=82s)
for some motivations for this and justification of implementation choices a swi prolog script first compiles the code to a pl.nl file, than Main calls stuff in Engine which loads it in memory and runs it

## Notes

### primitive types:

* int
* ground
* var (U+V)
* ref
* array

see main code (~1000lines) in __Engine.java__

* __pl2nl.pl__ compiles a .pl file to its .nl equivalent, ready to run by
the java based runtime system
* _natint.pl_ emulates its work by compiling back .nl files to Prolog clauses

### Some out of the box thoughts:

- no symbol tables: a symbol is just a (small) ground array of ints, and instead of a symbol table we would have a "ground cache" - that helps with better memory usege and also speed
- when a non-ground compound tries to unify with a ground, the   ground term is expanded to the heap
- when a ground unifies with a ground - it's just pointer equality and when a var unifies with a ground, it just points to it - as if it were a constant

### Author

- Paul Tarau, August 2017


