add(0, X, X).
add(s(X), Y, s(Z)) :-
    add(X, Y, Z).
 
goal(R) :-
    add(s(s(0)), R, s(s(s(s(s(s(0))))))).
