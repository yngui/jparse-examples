package com.github.jparse.examples.ebnf;

import static java.util.Objects.requireNonNull;

public final class Identifier {

    private final String name;

    public Identifier(String name) {
        this.name = requireNonNull(name);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
