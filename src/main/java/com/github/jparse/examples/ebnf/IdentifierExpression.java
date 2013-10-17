package com.github.jparse.examples.ebnf;

public final class IdentifierExpression extends Expression {

    private final Identifier identifier;

    public IdentifierExpression(Identifier identifier) {
        this.identifier = identifier;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return identifier.toString();
    }
}
