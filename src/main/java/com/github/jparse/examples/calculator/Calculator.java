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

package com.github.jparse.examples.calculator;

import com.github.jparse.FluentParser;
import com.github.jparse.Function;
import com.github.jparse.Pair;
import com.github.jparse.ParseResult;
import com.github.jparse.Sequence;

import java.math.BigDecimal;

import static com.github.jparse.CharParsers.literal;
import static com.github.jparse.CharParsers.pattern;
import static com.github.jparse.Sequences.fromCharSequence;
import static com.github.jparse.Sequences.withMemo;

public final class Calculator {

    private static final FluentParser<Character, BigDecimal> multiplicationOrDivision;
    private static final FluentParser<Character, BigDecimal> additionOrSubtraction;
    private static final FluentParser<Character, BigDecimal> expr;

    static {
        Function<String, BigDecimal> newBigDecimal = new Function<String, BigDecimal>() {
            @Override
            public BigDecimal apply(String arg) {
                return new BigDecimal(arg);
            }
        };
        Function<Pair<BigDecimal, BigDecimal>, BigDecimal> add = new Function<Pair<BigDecimal, BigDecimal>,
                BigDecimal>() {
            @Override
            public BigDecimal apply(Pair<BigDecimal, BigDecimal> arg) {
                return arg.getLeft().add(arg.getRight());
            }
        };
        Function<Pair<BigDecimal, BigDecimal>, BigDecimal> subtract = new Function<Pair<BigDecimal, BigDecimal>,
                BigDecimal>() {
            @Override
            public BigDecimal apply(Pair<BigDecimal, BigDecimal> arg) {
                return arg.getLeft().subtract(arg.getRight());
            }
        };
        Function<Pair<BigDecimal, BigDecimal>, BigDecimal> multiply = new Function<Pair<BigDecimal, BigDecimal>,
                BigDecimal>() {
            @Override
            public BigDecimal apply(Pair<BigDecimal, BigDecimal> arg) {
                return arg.getLeft().multiply(arg.getRight());
            }
        };
        Function<Pair<BigDecimal, BigDecimal>, BigDecimal> divide = new Function<Pair<BigDecimal, BigDecimal>,
                BigDecimal>() {
            @Override
            public BigDecimal apply(Pair<BigDecimal, BigDecimal> arg) {
                return arg.getLeft().divide(arg.getRight());
            }
        };
        FluentParser<Character, BigDecimal> multiplicationOrDivisionRef = new FluentParser<Character, BigDecimal>() {
            @Override
            public ParseResult<Character, ? extends BigDecimal> parse(Sequence<Character> sequence) {
                return multiplicationOrDivision.parse(sequence);
            }
        };
        FluentParser<Character, BigDecimal> additionOrSubtractionRef = new FluentParser<Character, BigDecimal>() {
            @Override
            public ParseResult<Character, ? extends BigDecimal> parse(Sequence<Character> sequence) {
                return additionOrSubtraction.parse(sequence);
            }
        };
        FluentParser<Character, BigDecimal> number = pattern("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?").map(newBigDecimal)
                .memo()
                .named("number")
                .log();
        FluentParser<Character, BigDecimal> grouping = literal("(").asError()
                .thenRight(additionOrSubtractionRef)
                .thenLeft(literal(")").asError())
                .memo()
                .named("grouping")
                .log();
        FluentParser<Character, BigDecimal> numberOrGrouping = number.orelse(grouping)
                .memo()
                .named("numberOrGrouping")
                .log();
        FluentParser<Character, BigDecimal> multiplication = multiplicationOrDivisionRef.thenLeft(literal("*"))
                .then(numberOrGrouping)
                .map(multiply)
                .memo()
                .named("multiplication")
                .log();
        FluentParser<Character, BigDecimal> division = multiplicationOrDivisionRef.thenLeft(literal("/"))
                .then(numberOrGrouping)
                .map(divide)
                .memo()
                .named("division")
                .log();
        multiplicationOrDivision = multiplication.orelse(division)
                .orelse(numberOrGrouping)
                .memo()
                .named("multiplicationOrDivision")
                .log();
        FluentParser<Character, BigDecimal> addition = additionOrSubtractionRef.thenLeft(literal("+"))
                .then(multiplicationOrDivisionRef)
                .map(add)
                .memo()
                .named("addition")
                .log();
        FluentParser<Character, BigDecimal> subtraction = additionOrSubtractionRef.thenLeft(literal("-"))
                .then(multiplicationOrDivisionRef)
                .map(subtract)
                .memo()
                .named("subtraction")
                .log();
        additionOrSubtraction = addition.orelse(subtraction)
                .orelse(multiplicationOrDivisionRef)
                .memo()
                .named("additionOrSubtraction")
                .log();
        expr = additionOrSubtraction.asFailure();
    }

    public static void main(String[] args) {
        String sequence = "1+(2-3)*4";
        ParseResult<Character, ? extends BigDecimal> result = expr.phrase().parse(withMemo(fromCharSequence(sequence)));
        if (result.isSuccess()) {
            System.out.println(result.getResult());
        } else {
            System.out.println(result.getMessage() + " at " + (sequence.length() - result.getRest().length()));
        }
    }
}
