package com.github.jparse.examples.ebnf;

public final class RepetitionExpression extends Expression {

    private final Expression expression;

    public RepetitionExpression(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return '(' + expression.toString() + ')' + '*';
    }
}
