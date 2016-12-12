grammar Filter;

filter
	:	expression EOF
	;

expression
	:	parenthesizedExpression
	|	function
	|	member
	|	expression ('^') expression
	|	'!' expression
	|	('+'|'-') expression
	|	expression ('*'|'/'|'%') expression
	|	expression ('+'|'-') expression
	|	expression ('<=' | '>=' | '>' | '<') expression
	|	expression ('==' | '!=') expression
	| 	expression '&&' expression
	|	expression '||' expression
	|	literal
	;

parenthesizedExpression
	:	'(' expression ')'
	;

member
	:	Identifier
	|	member '.' Identifier
	;

function
	:	Identifier '(' expressionList? ')'
	;

expressionList
	:	expression (',' expression)*
	;

literal
	:	IntegerLiteral			#Integer
	|	FloatingPointLiteral	#Double
	|	StringLiteral			#String
	|	BooleanLiteral			#Boolean
	|	'null'					#null
	;

BooleanLiteral
	:	'true' | 'false'
	;
	
Identifier
	:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
	;

IntegerLiteral
	:	'0'..'9'+
    ;

FloatingPointLiteral
	:	Digit+ '.' Digit* Exponent?
	|	' .' Digit+ Exponent?
	|	Digit+ Exponent
	;

StringLiteral
	:	'\'' ('\'\'' | (~'\'')*) '\''
	;

fragment
Digit : ('0'..'9') ;

fragment
Exponent : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

WS : [ \t]+ -> skip ;