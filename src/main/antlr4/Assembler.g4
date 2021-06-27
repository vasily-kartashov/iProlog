grammar Assembler;

@header {
package ptarau.iprolog.antlr;
}

VAR         : [_A-Z][a-zA-Z_0-9]* ;
IF          : 'if' ;
AND         : 'and' ;
HOLDS       : 'holds' ;
LISTS       : 'lists' ;
INT         : '-'? [0-9]+ ;
DOUBLE      : '-'? [0-9]+'.'[0-9]+ ;
ATOM        : [a-z][a-zA-Z_0-9]* ;
STRING      :  '"' (~["])* '"' | '\'' (~['])* '\''
            {
                String s = getText();
                setText(s.substring(1, s.length() - 1));
            }
            ;
END         : '.' ;
WS          : [ \t\r\n]+ -> skip ;

program     : rule_* ;
rule_       : conj ( IF conj )? END ;
conj        : term ( AND term )* ;
term        : composite | vars | holds | lists ;
vars        : VAR* ;
composite   : ATOM argument* ;
holds       : VAR HOLDS term ;
lists       : VAR LISTS argument* ;
argument    : VAR | INT | DOUBLE | ATOM | STRING ;
