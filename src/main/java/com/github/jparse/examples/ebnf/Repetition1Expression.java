package com.github.jparse.examples.ebnf;

public final class Repetition1Expression extends Expression {

    private final Expression expression;

    public Repetition1Expression(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return '(' + expression.toString() + ')' + '+';
    }
}
