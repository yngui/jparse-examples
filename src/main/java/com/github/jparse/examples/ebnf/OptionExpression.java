package com.github.jparse.examples.ebnf;

public final class OptionExpression extends Expression {

    private final Expression expression;

    public OptionExpression(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return '(' + expression.toString() + ')' + '?';
    }
}
