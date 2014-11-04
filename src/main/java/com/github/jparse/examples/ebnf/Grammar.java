package com.github.jparse.examples.ebnf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class Grammar {

    private final Collection<Rule> rules;

    public Grammar(Collection<Rule> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
    }

    public Collection<Rule> getRules() {
        return rules;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Rule rule : rules) {
            sb.append(rule);
        }
        return sb.toString();
    }
}
