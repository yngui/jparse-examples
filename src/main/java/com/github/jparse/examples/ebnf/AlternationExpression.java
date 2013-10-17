package com.github.jparse.examples.ebnf;

public final class AlternationExpression extends Expression {

    private final Expression expression1;
    private final Expression expression2;

    public AlternationExpression(Expression expression1, Expression expression2) {
        this.expression1 = expression1;
        this.expression2 = expression2;
    }

    public Expression getExpression1() {
        return expression1;
    }

    public Expression getExpression2() {
        return expression2;
    }

    @Override
    public String toString() {
        return '(' + expression1.toString() + ')' + '|' + '(' + expression2 + ')';
    }
}
