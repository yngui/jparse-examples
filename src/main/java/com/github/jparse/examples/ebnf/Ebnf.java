/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Igor Konev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.jparse.examples.ebnf;

import com.github.jparse.FluentParser;
import com.github.jparse.Function;
import com.github.jparse.Pair;
import com.github.jparse.ParseResult;
import com.github.jparse.Sequence;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

import static com.github.jparse.CharParsers.literal;
import static com.github.jparse.CharParsers.pattern;
import static com.github.jparse.Parsers.phrase;
import static com.github.jparse.Sequences.fromCharSequence;
import static com.github.jparse.StatefulParsers.log;
import static com.github.jparse.StatefulParsers.memo;
import static com.github.jparse.StatefulSequences.withMemo;

public final class Ebnf {

    private static final FluentParser<Character, Expression> quantExpr;
    private static final FluentParser<Character, Expression> concatExpr;
    private static final FluentParser<Character, Expression> altExpr;
    private static final FluentParser<Character, Grammar> grammar;

    static {
        Function<String, Identifier> newIdent = new Function<String, Identifier>() {
            @Override
            public Identifier apply(String arg) {
                return new Identifier(arg);
            }
        };
        Function<String, Expression> newTermExpr = new Function<String, Expression>() {
            @Override
            public Expression apply(String arg) {
                return new TerminalExpression(arg);
            }
        };
        Function<Identifier, Expression> newIdentExpr = new Function<Identifier, Expression>() {
            @Override
            public Expression apply(Identifier arg) {
                return new IdentifierExpression(arg);
            }
        };
        Function<Expression, Expression> newOptExpr = new Function<Expression, Expression>() {
            @Override
            public Expression apply(Expression arg) {
                return new OptionExpression(arg);
            }
        };
        Function<Expression, Expression> newRepExpr = new Function<Expression, Expression>() {
            @Override
            public Expression apply(Expression arg) {
                return new RepetitionExpression(arg);
            }
        };
        Function<Expression, Expression> newRep1Expr = new Function<Expression, Expression>() {
            @Override
            public Expression apply(Expression arg) {
                return new Repetition1Expression(arg);
            }
        };
        Function<Pair<Expression, Expression>, Expression> newAltExpr = new Function<Pair<Expression, Expression>,
                Expression>() {
            @Override
            public Expression apply(Pair<Expression, Expression> arg) {
                return new AlternationExpression(arg.getLeft(), arg.getRight());
            }
        };
        Function<Pair<Expression, Expression>, Expression> newConcatExpr = new Function<Pair<Expression, Expression>,
                Expression>() {
            @Override
            public Expression apply(Pair<Expression, Expression> arg) {
                return new ConcatenationExpression(arg.getLeft(), arg.getRight());
            }
        };
        Function<Pair<Identifier, Expression>, Rule> newRule = new Function<Pair<Identifier, Expression>, Rule>() {
            @Override
            public Rule apply(Pair<Identifier, Expression> arg) {
                return new Rule(arg.getLeft(), arg.getRight());
            }
        };
        Function<Collection<Rule>, Grammar> newGrammar = new Function<Collection<Rule>, Grammar>() {
            @Override
            public Grammar apply(Collection<Rule> arg) {
                return new Grammar(arg);
            }
        };
        FluentParser<Character, Expression> quantExprRef = new FluentParser<Character, Expression>() {
            @Override
            public ParseResult<Character, ? extends Expression> parse(Sequence<Character> sequence) {
                return quantExpr.parse(sequence);
            }
        };
        FluentParser<Character, Expression> concatExprRef = new FluentParser<Character, Expression>() {
            @Override
            public ParseResult<Character, ? extends Expression> parse(Sequence<Character> sequence) {
                return concatExpr.parse(sequence);
            }
        };
        FluentParser<Character, Expression> altExprRef = new FluentParser<Character, Expression>() {
            @Override
            public ParseResult<Character, ? extends Expression> parse(Sequence<Character> sequence) {
                return altExpr.parse(sequence);
            }
        };
        FluentParser<Character, ?> comments = log(pattern("/\\*.*?\\*/").rep().named("comments"));
        FluentParser<Character, Identifier> ident = log(
                comments.thenRight(pattern("^[A-Za-z][0-9A-Za-z_]*")).map(newIdent).named("ident"));
        FluentParser<Character, Expression> termExpr = log(comments.thenRight(literal("'").thenRight(pattern("[^']*"))
                .thenLeft(literal("'").asError())
                .orelse(literal("\"").thenRight(pattern("[^\"]*")).thenLeft(literal("\"").asError())))
                .map(newTermExpr).named("termExpr"));
        FluentParser<Character, Expression> identExpr = log(ident.map(newIdentExpr).named("identExpr"));
        FluentParser<Character, Expression> groupExpr = log(comments.thenRight(literal("("))
                .thenRight(altExprRef)
                .thenLeft(comments)
                .thenLeft(literal(")").asError()).named("groupExpr"));
        FluentParser<Character, Expression> optExpr = log(
                quantExprRef.thenLeft(comments).thenLeft(literal("?")).map(newOptExpr).named("optExpr"));
        FluentParser<Character, Expression> repExpr = log(
                quantExprRef.thenLeft(comments).thenLeft(literal("*")).map(newRepExpr).named("repExpr"));
        FluentParser<Character, Expression> rep1Expr = log(
                quantExprRef.thenLeft(comments).thenLeft(literal("+")).map(newRep1Expr).named("rep1Expr"));
        quantExpr = log(memo(optExpr.orelse(repExpr)
                .orelse(rep1Expr).orelse(termExpr.orelse(identExpr).orelse(groupExpr))).named("quantExpr"));
        concatExpr = log(
                memo(concatExprRef.thenLeft(comments).then(quantExpr).map(newConcatExpr).orelse(quantExpr)).named(
                        "concatExpr"));
        altExpr = log(memo(altExprRef.thenLeft(comments)
                .thenLeft(literal("|"))
                .then(concatExpr)
                .map(newAltExpr).orelse(concatExpr)).named("altExpr"));
        FluentParser<Character, Rule> rule = log(ident.thenLeft(comments)
                .thenLeft(literal(":").asError())
                .then(altExpr)
                .thenLeft(comments)
                .thenLeft(literal(";").asError())
                .map(newRule).named("rule"));
        grammar = log(
                rule.rep1().thenLeft(comments).thenLeft(pattern("\\s*")).map(newGrammar).asFailure().named("grammar"));
    }

    public static void main(String[] args) throws IOException {
        String sequence = readFully(new InputStreamReader(Ebnf.class.getResourceAsStream("grammar")));
        ParseResult<Character, ? extends Grammar> result = phrase(grammar).parse(withMemo(fromCharSequence(sequence)));
        if (result.isSuccess()) {
            Grammar grammar = result.getResult();
            System.out.println(grammar);
        } else {
            System.out.println(result.getMessage() + " at " + (sequence.length() - result.getRest().length()));
        }
    }

    private static String readFully(Reader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
        char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
        return sb.toString();
    }
}
