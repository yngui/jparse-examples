package com.github.jparse.examples.ebnf;

import static com.github.jparse.Objects.requireNonNull;

public final class TerminalExpression extends Expression {

    private final String terminal;

    public TerminalExpression(String terminal) {
        this.terminal = requireNonNull(terminal);
    }

    public String getTerminal() {
        return terminal;
    }

    @Override
    public String toString() {
        return terminal.contains("\"") ? '\'' + terminal + '\'' : '"' + terminal + '"';
    }
}
