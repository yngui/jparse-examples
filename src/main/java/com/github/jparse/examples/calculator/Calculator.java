package com.github.jparse.examples.calculator;

import com.github.jparse.FluentParser;
import com.github.jparse.Function;
import com.github.jparse.Pair;
import com.github.jparse.ParseContext;
import com.github.jparse.ParseResult;
import com.github.jparse.Sequence;
import com.github.jparse.Sequences;

import java.math.BigDecimal;

import static com.github.jparse.CharParsers.literal;
import static com.github.jparse.CharParsers.pattern;

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
            public ParseResult<Character, BigDecimal> parse(Sequence<Character> sequence, ParseContext context) {
                return multiplicationOrDivision.parse(sequence, context);
            }
        };
        FluentParser<Character, BigDecimal> additionOrSubtractionRef = new FluentParser<Character, BigDecimal>() {
            @Override
            public ParseResult<Character, BigDecimal> parse(Sequence<Character> sequence, ParseContext context) {
                return additionOrSubtraction.parse(sequence, context);
            }
        };

        FluentParser<Character, BigDecimal> number = pattern("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?").map(newBigDecimal)
                .memo()
                .log("number");
        FluentParser<Character, BigDecimal> grouping = literal("(").asError()
                .thenRight(additionOrSubtractionRef)
                .thenLeft(literal(")").asError())
                .memo()
                .log("grouping");
        FluentParser<Character, BigDecimal> numberOrGrouping = number.orelse(grouping)
                .memo()
                .log("numberOrGrouping")
                .cast();

        FluentParser<Character, BigDecimal> multiplication = multiplicationOrDivisionRef.thenLeft(literal("*"))
                .then(numberOrGrouping)
                .map(multiply)
                .memo()
                .log("multiplication");
        FluentParser<Character, BigDecimal> division = multiplicationOrDivisionRef.thenLeft(literal("/"))
                .then(numberOrGrouping)
                .map(divide)
                .memo()
                .log("division");
        multiplicationOrDivision = multiplication.orelse(division)
                .orelse(numberOrGrouping)
                .memo()
                .log("multiplicationOrDivision")
                .cast();

        FluentParser<Character, BigDecimal> addition = additionOrSubtractionRef.thenLeft(literal("+"))
                .then(multiplicationOrDivisionRef)
                .map(add)
                .memo()
                .log("addition");
        FluentParser<Character, BigDecimal> subtraction = additionOrSubtractionRef.thenLeft(literal("-"))
                .then(multiplicationOrDivisionRef)
                .map(subtract)
                .memo()
                .log("subtraction");
        additionOrSubtraction = addition.orelse(subtraction)
                .orelse(multiplicationOrDivisionRef)
                .memo()
                .log("additionOrSubtraction")
                .cast();

        expr = additionOrSubtraction.asFailure();
    }

    public static void main(String[] args) {
        String sequence = "1+(2-3)*4";
        ParseResult<Character, BigDecimal> result = expr.phrase()
                .parse(Sequences.forCharSequence(sequence), new ParseContext());
        if (result.isSuccess()) {
            System.out.println(result.getResult());
        } else {
            System.out.println(result.getMessage() + " at " + (sequence.length() - result.getRest().length()));
        }
    }
}
