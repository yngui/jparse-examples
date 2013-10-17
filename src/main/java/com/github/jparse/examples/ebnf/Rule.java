package com.github.jparse.examples.ebnf;

public final class Rule {

    private final Identifier identifier;
    private final Expression expression;

    public Rule(Identifier identifier, Expression expression) {
        this.identifier = identifier;
        this.expression = expression;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return identifier.toString() + ':' + expression + ';';
    }
}
